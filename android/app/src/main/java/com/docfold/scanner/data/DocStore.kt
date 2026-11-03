package com.docfold.scanner.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docfold.scanner.imaging.DeviceProfile
import com.docfold.scanner.imaging.Filters
import com.docfold.scanner.imaging.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * All documents, stored as plain folders — no database:
 *
 *   files/docs/<docId>/doc.json          name, dates, colour, page order + per-page edits
 *   files/docs/<docId>/pages/<id>.jpg     the page as it looks now
 *   files/docs/<docId>/pages/<id>.src.jpg the original capture, only once the page has been edited
 *   files/docs/<docId>/thumbs/<id>.jpg    small preview for lists
 */
class DocStore(private val context: Context) {
    private val root = File(context.filesDir, "docs").apply { mkdirs() }
    private val lock = Mutex()
    private val _docs = MutableStateFlow<List<Doc>?>(null)

    /** Null until the first load finishes; newest first. */
    val docs: StateFlow<List<Doc>?> = _docs.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(2)

    fun dir(docId: String) = File(root, docId)
    fun pageFile(docId: String, pageId: String) = File(dir(docId), "pages/$pageId.jpg")
    fun srcFile(docId: String, pageId: String) = File(dir(docId), "pages/$pageId.src.jpg")
    fun thumbFile(docId: String, pageId: String) = File(dir(docId), "thumbs/$pageId.jpg")

    fun get(docId: String): Doc? = _docs.value?.firstOrNull { it.id == docId }

    suspend fun load() = withContext(io) {
        val list = root.listFiles()?.mapNotNull { d -> read(d) }?.sortedByDescending { it.updated } ?: emptyList()
        _docs.value = list
    }

    private fun read(d: File): Doc? = runCatching {
        val f = File(d, "doc.json")
        if (!f.exists()) return null
        Doc.fromJson(JSONObject(f.readText())).copy(sizeBytes = d.walkTopDown().filter { it.isFile }.sumOf { it.length() })
    }.getOrNull()

    private fun write(doc: Doc): Doc {
        val d = dir(doc.id).apply { mkdirs() }
        val tmp = File(d, "doc.json.tmp")
        tmp.writeText(doc.toJson().toString())
        if (!tmp.renameTo(File(d, "doc.json"))) {
            File(d, "doc.json").writeText(tmp.readText()); tmp.delete()
        }
        val sized = doc.copy(sizeBytes = d.walkTopDown().filter { it.isFile }.sumOf { it.length() })
        _docs.update { list ->
            val rest = list.orEmpty().filterNot { it.id == doc.id }
            (listOf(sized) + rest).sortedByDescending { it.updated }
        }
        return sized
    }

    private fun newId() = buildString { repeat(10) { append("abcdefghijkmnpqrstuvwxyz23456789"[Random.nextInt(32)]) } }

    fun defaultName(now: Long = System.currentTimeMillis()): String =
        "Scan " + SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(now))

    private fun nextColor(): Int {
        val used = _docs.value.orEmpty().take(5).map { it.color }
        return (0 until STAMP_COLORS).firstOrNull { it !in used } ?: Random.nextInt(STAMP_COLORS)
    }

    // ── Creating ────────────────────────────────────────────────────────────────────────────────────

    suspend fun create(name: String? = null): Doc = lock.withLock {
        withContext(io) {
            val now = System.currentTimeMillis()
            write(Doc(newId(), name ?: defaultName(now), now, now, nextColor(), emptyList()))
        }
    }

    /** Imports images in order. [onProgress] gets (done, total). Unreadable images are skipped. */
    suspend fun addImages(docId: String, uris: List<Uri>, onProgress: (Int, Int) -> Unit = { _, _ -> }): Doc? = lock.withLock {
        withContext(io) {
            val doc = get(docId) ?: return@withContext null
            val added = ArrayList<Page>()
            uris.forEachIndexed { i, uri ->
                ImageIO.decodeUri(context, uri)?.let { bmp -> added += savePage(docId, bmp) }
                onProgress(i + 1, uris.size)
            }
            write(doc.copy(pages = doc.pages + added, updated = System.currentTimeMillis()))
        }
    }

    suspend fun addBitmap(docId: String, bitmap: Bitmap): Doc? = lock.withLock {
        withContext(io) {
            val doc = get(docId) ?: return@withContext null
            val page = savePage(docId, bitmap)
            write(doc.copy(pages = doc.pages + page, updated = System.currentTimeMillis()))
        }
    }

    /** Writes the page image + thumbnail and recycles [bmp]. */
    private fun savePage(docId: String, bmp: Bitmap): Page {
        val id = newId()
        pageFile(docId, id).parentFile?.mkdirs()
        thumbFile(docId, id).parentFile?.mkdirs()
        ImageIO.writeJpeg(bmp, pageFile(docId, id), 88)
        ImageIO.writeThumb(bmp, thumbFile(docId, id))
        bmp.recycle()
        return Page(id)
    }

    // ── Arranging ───────────────────────────────────────────────────────────────────────────────────

    private suspend fun mutate(docId: String, block: (Doc) -> Doc): Doc? = lock.withLock {
        withContext(io) {
            val doc = get(docId) ?: return@withContext null
            write(block(doc).copy(updated = System.currentTimeMillis()))
        }
    }

    suspend fun rename(docId: String, name: String) = mutate(docId) { it.copy(name = name.trim().ifEmpty { it.name }) }

    suspend fun reorder(docId: String, order: List<String>) = mutate(docId) { doc ->
        val byId = doc.pages.associateBy { it.id }
        val sorted = order.mapNotNull { byId[it] }
        doc.copy(pages = sorted + doc.pages.filter { it.id !in order })
    }

    suspend fun rotate(docId: String, pageIds: Set<String>, delta: Int) = mutate(docId) { doc ->
        doc.copy(pages = doc.pages.map { p -> if (p.id in pageIds) p.copy(rotation = ((p.rotation + delta) % 360 + 360) % 360) else p })
    }

    suspend fun deletePages(docId: String, pageIds: Set<String>) = mutate(docId) { doc ->
        pageIds.forEach { id -> pageFile(docId, id).delete(); srcFile(docId, id).delete(); thumbFile(docId, id).delete() }
        doc.copy(pages = doc.pages.filterNot { it.id in pageIds })
    }

    suspend fun duplicatePages(docId: String, pageIds: Set<String>) = mutate(docId) { doc ->
        val out = ArrayList<Page>()
        doc.pages.forEach { p ->
            out += p
            if (p.id in pageIds) {
                val id = newId()
                pageFile(docId, p.id).copyTo(pageFile(docId, id))
                thumbFile(docId, p.id).takeIf { it.exists() }?.copyTo(thumbFile(docId, id))
                srcFile(docId, p.id).takeIf { it.exists() }?.copyTo(srcFile(docId, id))
                out += p.copy(id = id)
            }
        }
        doc.copy(pages = out)
    }

    /** Re-renders a page from its original capture with a new crop and/or filter. */
    suspend fun editPage(docId: String, pageId: String, filter: PageFilter, crop: List<Float>?) = mutate(docId) { doc ->
        val page = doc.pages.first { it.id == pageId }
        val src = srcFile(docId, pageId)
        val out = pageFile(docId, pageId)
        val pristine = filter == PageFilter.Original && crop == null
        if (!src.exists()) {
            if (pristine) return@mutate doc
            out.renameTo(src)
        }
        if (pristine) {
            src.copyTo(out, overwrite = true); src.delete()
        } else {
            var bmp = ImageIO.decodeFile(src, DeviceProfile.maxPageSide) ?: return@mutate doc
            if (crop != null) bmp = Filters.warp(bmp, crop).also { bmp.recycle() }
            bmp = Filters.apply(bmp, filter)
            ImageIO.writeJpeg(bmp, out, 88)
            ImageIO.writeThumb(bmp, thumbFile(docId, pageId))
            bmp.recycle()
        }
        if (pristine) ImageIO.decodeFile(out, ImageIO.THUMB_SIDE * 2)?.let { ImageIO.writeThumb(it, thumbFile(docId, pageId)); it.recycle() }
        doc.copy(pages = doc.pages.map { if (it.id == pageId) page.copy(filter = filter, crop = crop, rev = page.rev + 1) else it })
    }

    /**
     * Replaces a page's pixels outright (e.g. after signing). [bmp] must already be upright: the result
     * becomes the new original, with rotation, crop and filter reset.
     */
    suspend fun replacePixels(docId: String, pageId: String, bmp: Bitmap) = mutate(docId) { doc ->
        ImageIO.writeJpeg(bmp, pageFile(docId, pageId), 88)
        ImageIO.writeThumb(bmp, thumbFile(docId, pageId))
        bmp.recycle()
        srcFile(docId, pageId).delete()
        doc.copy(pages = doc.pages.map { if (it.id == pageId) it.copy(rotation = 0, filter = PageFilter.Original, crop = null, rev = it.rev + 1) else it })
    }

    suspend fun markSent(docId: String) = mutate(docId) { it.copy(sentAt = System.currentTimeMillis()) }

    // ── Whole documents ─────────────────────────────────────────────────────────────────────────────

    suspend fun deleteDoc(docId: String) = lock.withLock {
        withContext(io) {
            dir(docId).deleteRecursively()
            _docs.update { it.orEmpty().filterNot { d -> d.id == docId } }
        }
    }

    /** Copies pages from several documents, in the given order, into a new document. */
    suspend fun merge(docIds: List<String>, name: String): Doc? = copyInto(name, docIds.flatMap { id -> get(id)?.pages.orEmpty().map { id to it } })

    /** Moves or copies [pageIds] of [docId] into a new document. */
    suspend fun extract(docId: String, pageIds: Set<String>, name: String, move: Boolean): Doc? {
        val doc = get(docId) ?: return null
        val made = copyInto(name, doc.pages.filter { it.id in pageIds }.map { docId to it })
        if (move && made != null) deletePages(docId, pageIds)
        return made
    }

    private suspend fun copyInto(name: String, pages: List<Pair<String, Page>>): Doc? = lock.withLock {
        withContext(io) {
            if (pages.isEmpty()) return@withContext null
            val now = System.currentTimeMillis()
            val target = Doc(newId(), name, now, now, nextColor(), emptyList())
            val copied = pages.map { (from, p) ->
                val id = newId()
                pageFile(from, p.id).copyTo(pageFile(target.id, id))
                thumbFile(from, p.id).takeIf { it.exists() }?.copyTo(thumbFile(target.id, id))
                srcFile(from, p.id).takeIf { it.exists() }?.copyTo(srcFile(target.id, id))
                p.copy(id = id)
            }
            write(target.copy(pages = copied))
        }
    }

    companion object {
        const val STAMP_COLORS = 6
    }
}
