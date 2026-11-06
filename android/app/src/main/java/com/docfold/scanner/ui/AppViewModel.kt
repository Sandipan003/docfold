package com.docfold.scanner.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docfold.scanner.DocfoldApp
import com.docfold.scanner.data.Doc
import com.docfold.scanner.data.Page
import com.docfold.scanner.data.PageFilter
import com.docfold.scanner.imaging.ImageIO
import com.docfold.scanner.ocr.Ocr
import com.docfold.scanner.pdf.ExportOptions
import com.docfold.scanner.pdf.Margin
import com.docfold.scanner.pdf.PaperSize
import com.docfold.scanner.pdf.PdfImporter
import com.docfold.scanner.pdf.Quality
import com.docfold.scanner.ui.components.Images
import com.docfold.scanner.ui.screens.formatSize
import com.docfold.scanner.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Intro : Screen
    data object Home : Screen
    data class Document(val docId: String) : Screen
    data class PageView(val docId: String, val pageId: String) : Screen
    data class Crop(val docId: String, val pageId: String, val isNew: Boolean = false) : Screen
    data object Settings : Screen
}

/** A long task the user waits on, shown as a calm full-screen progress card. */
data class Busy(val label: String, val done: Int = 0, val total: Int = 0)

/** One-off messages for the snackbar; [actionLabel] + [action] make it actionable. */
data class Notice(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

/** Counts what passes through, so a saved PDF can report its real size. */
private class CountingStream(stream: OutputStream) : FilterOutputStream(stream) {
    var count = 0L
        private set

    override fun write(b: Int) { out.write(b); count++ }
    override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    val store = (app as DocfoldApp).store
    val exporter = (app as DocfoldApp).exporter
    private val prefs = app.getSharedPreferences("docfold", Context.MODE_PRIVATE)

    val stack = mutableStateListOf<Screen>(if (prefs.getBoolean("introDone", false)) Screen.Home else Screen.Intro)
    val screen get() = stack.last()

    var busy by mutableStateOf<Busy?>(null)
        private set
    var themeMode by mutableStateOf(ThemeMode.entries[prefs.getInt("theme", 0).coerceIn(0, 2)])
        private set
    /** Set after an export so the document screen can play the postmark. */
    var postmarkFor by mutableStateOf<String?>(null)

    private val notices = Channel<Notice>(Channel.BUFFERED)
    val noticeFlow = notices.receiveAsFlow()

    init {
        viewModelScope.launch { store.load() }
    }

    // ── navigation ──────────────────────────────────────────────────────────────────────────────────

    fun open(s: Screen) { stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun replace(s: Screen) { stack[stack.lastIndex] = s }

    fun finishIntro() {
        prefs.edit { putBoolean("introDone", true) }
        stack.clear(); stack.add(Screen.Home)
    }

    fun replayIntro() { stack.add(Screen.Intro) }

    fun finishIntroIfNeeded() { if (screen == Screen.Intro) finishIntro() }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit { putInt("theme", mode.ordinal) }
    }

    fun notify(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        notices.trySend(Notice(text, actionLabel, action))
    }

    private fun work(label: String, block: suspend () -> Unit): Job = viewModelScope.launch {
        busy = Busy(label)
        try { block() } catch (e: Exception) {
            notify(e.message ?: "Something went wrong. Please try again.")
        } finally { busy = null }
    }

    private fun progress(label: String): (Int, Int) -> Unit = { d, t -> busy = Busy(label, d, t) }

    // ── adding pages ────────────────────────────────────────────────────────────────────────────────

    /** Adds images to [docId] (or a new document) and opens it. */
    fun addImages(docId: String?, uris: List<Uri>, cropNew: Boolean = false) {
        if (uris.isEmpty()) return
        work("Adding pages") {
            val doc = docId?.let { store.get(it) } ?: store.create()
            val added = try { store.addImages(doc.id, uris, progress("Adding pages")) } finally { discardScannerCopies(uris) }
            val result = added ?: return@work
            if (result.pages.size == doc.pages.size) {
                notify(if (uris.size == 1) "Couldn't read that image." else "Couldn't read those images.")
                if (docId == null) store.deleteDoc(doc.id)
                return@work
            }
            val addedCount = result.pages.size - doc.pages.size
            if (addedCount < uris.size) notify("Added $addedCount of ${uris.size} pages. ${uris.size - addedCount} couldn't be read.")
            showDoc(doc.id, newDoc = docId == null)
            if (cropNew) open(Screen.Crop(doc.id, result.pages.last().id, isNew = true))
        }
    }

    fun addPdf(docId: String?, uri: Uri) {
        work("Opening PDF") {
            val doc = docId?.let { store.get(it) } ?: store.create(nameFromUri(uri))
            try {
                val n = PdfImporter.import(ctx, store, doc.id, uri, progress("Reading PDF pages"))
                if (n == 0) notify("That PDF has no pages.")
            } catch (e: PdfImporter.Locked) {
                notify("This PDF has a password. Open it in another app first, then share the pages here.")
                if (docId == null && store.get(doc.id)?.pages.isNullOrEmpty()) store.deleteDoc(doc.id)
                return@work
            }
            showDoc(doc.id, newDoc = docId == null)
        }
    }

    /**
     * Google's scanner hands back pages as files in our cache and leaves them there. Once copied into the document
     * they are duplicates, so remove them. Only file:// Uris inside our own cache are touched.
     */
    private suspend fun discardScannerCopies(uris: List<Uri>) = withContext(Dispatchers.IO) {
        val cache = ctx.cacheDir.canonicalPath + File.separator
        uris.filter { it.scheme == "file" }.mapNotNull { it.path?.let(::File) }
            .filter { runCatching { it.canonicalPath.startsWith(cache) }.getOrDefault(false) }
            .forEach { it.delete() }
    }

    private fun showDoc(docId: String, newDoc: Boolean) {
        val target = Screen.Document(docId)
        if (screen != target) {
            if (newDoc && screen !is Screen.Home) stack.removeAll { it is Screen.Document }
            open(target)
        }
    }

    private fun nameFromUri(uri: Uri): String? = runCatching {
        ctx.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0)?.substringBeforeLast('.') else null
        }
    }.getOrNull()

    fun cameraFile(): Uri {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3_600_000) it.delete() }
        val f = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    }

    // ── arranging ───────────────────────────────────────────────────────────────────────────────────

    fun rename(docId: String, name: String) = viewModelScope.launch { store.rename(docId, name) }
    fun reorder(docId: String, order: List<String>) = viewModelScope.launch { store.reorder(docId, order) }
    fun rotate(docId: String, ids: Set<String>, delta: Int) = viewModelScope.launch { store.rotate(docId, ids, delta) }
    fun duplicate(docId: String, ids: Set<String>) = viewModelScope.launch { store.duplicatePages(docId, ids) }

    fun deletePages(docId: String, ids: Set<String>) = viewModelScope.launch {
        store.deletePages(docId, ids)
        notify(if (ids.size == 1) "Page deleted" else "${ids.size} pages deleted")
    }

    fun extract(docId: String, ids: Set<String>) = work("Making a new document") {
        val src = store.get(docId) ?: return@work
        val made = store.extract(docId, ids, "${src.name} (part)", move = true) ?: return@work
        notify("Moved ${ids.size} page${if (ids.size == 1) "" else "s"} to a new document", "Open") { open(Screen.Document(made.id)) }
    }

    fun merge(ids: List<String>) = work("Merging documents") {
        val first = store.get(ids.first()) ?: return@work
        val made = store.merge(ids, "${first.name} + ${ids.size - 1} more") ?: return@work
        open(Screen.Document(made.id))
    }

    /** Deleting a document waits for the snackbar, so Undo is instant and nothing is lost by accident. */
    val hiddenDocs = mutableStateListOf<String>()
    fun deleteDoc(docId: String) {
        hiddenDocs += docId
        if (screen == Screen.Document(docId)) back()
        val name = store.get(docId)?.name ?: "Document"
        viewModelScope.launch {
            var undone = false
            notices.send(Notice("“$name” deleted", "Undo") { undone = true; hiddenDocs -= docId })
            kotlinx.coroutines.delay(5_000)
            if (!undone) { store.deleteDoc(docId); Images.evict(store.dir(docId).path) }
            hiddenDocs -= docId
        }
    }

    fun editPage(docId: String, pageId: String, filter: PageFilter, crop: List<Float>?) = work("Applying") {
        store.editPage(docId, pageId, filter, crop)
    }

    /** Burns [signature] into the page at [box] (fractions of the upright page). */
    fun sign(docId: String, page: Page, signature: Bitmap, box: RectF) = work("Signing page") {
        withContext(Dispatchers.IO) {
            val decoded = ImageIO.decodeFile(store.pageFile(docId, page.id), Int.MAX_VALUE) ?: return@withContext
            val upright = ImageIO.rotate(decoded, page.rotation)
            val bmp = if (upright.isMutable) upright else upright.copy(Bitmap.Config.ARGB_8888, true).also { upright.recycle() }
            val dst = RectF(box.left * bmp.width, box.top * bmp.height, box.right * bmp.width, box.bottom * bmp.height)
            Canvas(bmp).drawBitmap(signature, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            store.replacePixels(docId, page.id, bmp)
        }
        notify("Signed")
    }

    // ── signature ───────────────────────────────────────────────────────────────────────────────────

    private val signatureFile get() = File(ctx.filesDir, "signature.png")
    fun savedSignature(): Bitmap? = signatureFile.takeIf { it.exists() }?.let { android.graphics.BitmapFactory.decodeFile(it.path) }
    fun saveSignature(bmp: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        signatureFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ── text ────────────────────────────────────────────────────────────────────────────────────────

    suspend fun readText(docId: String, page: Page): String? = withContext(Dispatchers.IO) {
        if (!Ocr.ensureReady(ctx)) return@withContext null
        val bmp = ImageIO.decodeFile(store.pageFile(docId, page.id), 2000) ?: return@withContext ""
        val upright = ImageIO.rotate(bmp, page.rotation)
        try { Ocr.text(upright) } finally { upright.recycle() }
    }

    // ── export ──────────────────────────────────────────────────────────────────────────────────────

    private fun fileName(doc: Doc, ext: String) =
        doc.name.replace(':', '.').replace(Regex("[\\\\/*?\"<>|\\n]"), " ").trim().take(80).ifEmpty { "Docfold" } + ".$ext"

    /**
     * Writes the PDF and returns its size in bytes. With a size limit the size estimate isn't trusted: it is written
     * to a scratch file and stepped down a quality at a time until it truly fits, or there is nothing smaller.
     */
    private suspend fun writePdf(doc: Doc, opts: ExportOptions, out: OutputStream): Long {
        if (opts.searchable && !Ocr.ensureReady(ctx)) notify("Text recognition needs a one-time download. Saved without searchable text.")
        val searchable = opts.searchable && Ocr.ensureReady(ctx)
        suspend fun render(o: ExportOptions, to: OutputStream) = exporter.write(
            doc, o.copy(searchable = searchable), to,
            ocr = if (searchable) { b -> Ocr.lines(b) } else null,
            progress = progress("Writing page"),
        )
        val limit = opts.limitBytes
        if (limit == null) return CountingStream(out).also { render(opts, it) }.count
        val scratch = File(ctx.cacheDir, "fit.pdf")
        try {
            var quality = opts.quality
            while (true) {
                withContext(Dispatchers.IO) { scratch.outputStream().buffered().use { render(opts.copy(quality = quality), it) } }
                val smaller = Quality.entries.getOrNull(quality.ordinal + 1)
                if (scratch.length() <= limit || smaller == null) break
                quality = smaller
            }
            withContext(Dispatchers.IO) { scratch.inputStream().use { it.copyTo(out) } }
            return scratch.length()
        } finally {
            scratch.delete()
        }
    }

    private fun warnIfOverLimit(opts: ExportOptions, bytes: Long) {
        val limit = opts.limitBytes ?: return
        if (bytes > limit) notify("This PDF is ${formatSize(bytes)}, the smallest Docfold can make it. Try removing pages.")
    }

    /** Straight into Downloads/Docfold, no picker. Hands the new file's Uri and size to [onSaved]. */
    fun savePdfToDownloads(doc: Doc, opts: ExportOptions, onSaved: (Uri, Long) -> Unit) = work("Sealing your PDF") {
        val (uri, bytes) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) savePdfWithMediaStore(doc, opts) else savePdfAsFile(doc, opts)
        sealed(doc, adBreak = opts.limitBytes == null)
        warnIfOverLimit(opts, bytes)
        onSaved(uri, bytes)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun savePdfWithMediaStore(doc: Doc, opts: ExportOptions): Pair<Uri, Long> {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName(doc, "pdf"))
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Docfold")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = withContext(Dispatchers.IO) { resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) }
            ?: error("Couldn't create the file in Downloads.")
        var bytes = 0L
        try {
            withContext(Dispatchers.IO) { resolver.openOutputStream(uri)?.use { bytes = writePdf(doc, opts, it) } ?: error("Couldn't write the file.") }
            withContext(Dispatchers.IO) { resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { resolver.delete(uri, null, null) }
            throw e
        }
        return uri to bytes
    }

    /** Android 7–9 has no MediaStore.Downloads: a plain file, which the storage permission allows. */
    @Suppress("DEPRECATION")
    private suspend fun savePdfAsFile(doc: Doc, opts: ExportOptions): Pair<Uri, Long> = withContext(Dispatchers.IO) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Docfold").apply { mkdirs() }
        val name = fileName(doc, "pdf")
        var file = File(dir, name)
        var copy = 1
        while (file.exists()) file = File(dir, "${name.removeSuffix(".pdf")} (${copy++}).pdf")
        val bytes = try {
            file.outputStream().use { writePdf(doc, opts, it) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        MediaScannerConnection.scanFile(ctx, arrayOf(file.path), arrayOf("application/pdf"), null)
        FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file) to bytes
    }

    /** When saving wasn't allowed: write to a place the user picked with the system file picker. */
    fun savePdfTo(doc: Doc, opts: ExportOptions, uri: Uri, onSaved: (Long) -> Unit) = work("Sealing your PDF") {
        val bytes = withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { writePdf(doc, opts, it) } } ?: 0L
        sealed(doc, adBreak = opts.limitBytes == null)
        warnIfOverLimit(opts, bytes)
        onSaved(bytes)
    }

    /** Writes into the app cache for the share sheet; returns a content Uri. */
    fun sharePdf(doc: Doc, opts: ExportOptions, onReady: (Uri) -> Unit) = work("Preparing to share") {
        val f = exportFile(fileName(doc, "pdf"))
        val bytes = withContext(Dispatchers.IO) { f.outputStream().use { writePdf(doc, opts, it) } }
        sealed(doc, adBreak = opts.limitBytes == null)
        warnIfOverLimit(opts, bytes)
        onReady(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f))
    }

    /** A print-ready A4 copy in the app cache, for Android's print screen. */
    fun printPdf(doc: Doc, onReady: (File) -> Unit) = work("Preparing to print") {
        val f = exportFile(fileName(doc, "pdf"))
        withContext(Dispatchers.IO) { f.outputStream().use { writePdf(doc, ExportOptions(paper = PaperSize.A4, margin = Margin.Small, quality = Quality.High), it) } }
        onReady(f)
    }

    fun formImageName(doc: Doc, width: Int, height: Int) = fileName(doc, "").removeSuffix(".") + " ${width}x$height.jpg"

    fun saveBytes(uri: Uri, bytes: ByteArray) = work("Saving") {
        withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
        notify("Saved · ${formatSize(bytes.size.toLong())}")
    }

    fun shareBytes(name: String, bytes: ByteArray, onReady: (Uri) -> Unit) = work("Preparing to share") {
        val f = exportFile(name)
        withContext(Dispatchers.IO) { f.writeBytes(bytes) }
        onReady(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f))
    }

    fun shareImages(doc: Doc, onReady: (List<Uri>) -> Unit) = work("Preparing images") {
        val base = fileName(doc, "").removeSuffix(".")
        val uris = doc.pages.mapIndexed { i, p ->
            val f = exportFile("$base ${i + 1}.jpg")
            withContext(Dispatchers.IO) { f.outputStream().use { exporter.writeJpeg(doc.id, p, it) } }
            busy = Busy("Preparing images", i + 1, doc.pages.size)
            FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        }
        sealed(doc)
        onReady(uris)
    }

    /** Android 10+: each page into Pictures/Docfold, visible in the gallery. */
    fun saveImagesToGallery(doc: Doc) = work("Saving images") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@work
        val base = fileName(doc, "").removeSuffix(".")
        doc.pages.forEachIndexed { i, p ->
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$base ${i + 1}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Docfold")
            }
            withContext(Dispatchers.IO) {
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext
                ctx.contentResolver.openOutputStream(uri)?.use { exporter.writeJpeg(doc.id, p, it) }
            }
            busy = Busy("Saving images", i + 1, doc.pages.size)
        }
        sealed(doc, postmark = false)
        notify("${doc.pages.size} image${if (doc.pages.size == 1) "" else "s"} saved to Pictures › Docfold")
    }

    private fun exportFile(name: String): File {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000) it.delete() }
        return File(dir, name)
    }

    var exportsDone by mutableStateOf(prefs.getInt("exports", 0))
        private set

    /** [adBreak] is false after a size-limited export: those users are mid-form and on a deadline, so no interstitial. */
    private suspend fun sealed(doc: Doc, postmark: Boolean = true, adBreak: Boolean = true) {
        store.markSent(doc.id)
        exportsDone += 1
        prefs.edit { putInt("exports", exportsDone) }
        if (postmark) postmarkFor = doc.id
        if (adBreak) com.docfold.scanner.ads.Ads.markNaturalBreak()
    }

    fun storageBytes(): Long = store.docs.value.orEmpty().sumOf { it.sizeBytes }

    // ── folders ─────────────────────────────────────────────────────────────────────────────────────

    fun folders(): List<String> = store.docs.value.orEmpty().map { it.folder }.filter { it.isNotEmpty() }.distinct().sorted()

    fun moveToFolder(ids: Collection<String>, folder: String) = viewModelScope.launch {
        store.setFolder(ids, folder)
        notify(if (folder.isBlank()) "Taken out of its folder" else "Moved to ${folder.trim()}")
    }

    // ── backup ──────────────────────────────────────────────────────────────────────────────────────

    fun backupName() = "Docfold backup " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()) + ".zip"

    fun backupTo(uri: Uri) = work("Backing up") {
        val out = withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri) } ?: error("Couldn't write the backup file.")
        out.use { store.backup(it, progress("Backing up")) }
        notify("Backup saved. Keep it somewhere safe, off this phone.")
    }

    fun restoreFrom(uri: Uri) = work("Restoring") {
        val input = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri) } ?: error("Couldn't open that file.")
        val (added, here) = input.use { store.restore(it) }
        notify(
            when {
                added == 0 && here > 0 -> "Those documents are already on this phone."
                added == 0 -> "No documents found in that backup."
                here > 0 -> "Restored ${added} document${if (added == 1) "" else "s"}. $here already here."
                else -> "Restored ${added} document${if (added == 1) "" else "s"}."
            },
        )
    }

    // ── export choices ──────────────────────────────────────────────────────────────────────────────

    /** The last Save as PDF choices, so a regular job needs no setting up. Layout and password stay per document. */
    fun exportChoice(key: String): String? = prefs.getString("export.$key", null)

    fun rememberExport(choices: Map<String, String>) = prefs.edit { choices.forEach { (key, value) -> putString("export.$key", value) } }

    /** Android 13+ has no permission for saving files, so the answer to Docfold's own question is kept here. */
    var savingAllowed by mutableStateOf(prefs.getBoolean("savingAllowed", false))
        private set

    fun allowSaving(allowed: Boolean) {
        savingAllowed = allowed
        prefs.edit { putBoolean("savingAllowed", allowed) }
    }

    var reviewAsked: Boolean
        get() = prefs.getBoolean("reviewAsked", false)
        set(v) = prefs.edit { putBoolean("reviewAsked", v) }
}
