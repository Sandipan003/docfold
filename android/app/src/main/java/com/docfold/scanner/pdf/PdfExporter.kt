package com.docfold.scanner.pdf

import android.graphics.Bitmap
import android.graphics.Rect
import com.docfold.scanner.data.Doc
import com.docfold.scanner.data.DocStore
import com.docfold.scanner.data.Page
import com.docfold.scanner.imaging.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

enum class PaperSize(val label: String, val w: Float, val h: Float) {
    Auto("Fit page", 0f, 0f),
    A4("A4", 595.28f, 841.89f),
    Letter("Letter", 612f, 792f),
    Legal("Legal", 612f, 1008f),
}

enum class Margin(val label: String, val pt: Float) { None("None", 0f), Small("Small", 18f), Normal("Normal", 36f) }

/** Image quality per page. [maxSide] null = embed the stored JPEG untouched. */
enum class Quality(val label: String, val maxSide: Int?, val jpeg: Int) {
    Original("full", null, 0),
    High("high", 2200, 85),
    Balanced("standard", 1700, 72),
    Small("compact", 1250, 60),
    Smaller("compact", 1050, 54),
    Tiny("very small", 900, 48),
    Tinier("very small", 770, 43),
    Micro("tiny", 650, 38),
    Minute("tiny", 550, 34),
    Nano("smallest", 460, 30),
}

enum class Layout(val label: String) {
    OnePerPage("1 per page"),
    TwoPerPage("2 per page"),
    IdCard("ID card"),
}

data class ExportOptions(
    val paper: PaperSize = PaperSize.Auto,
    val margin: Margin = Margin.None,
    val quality: Quality = Quality.Balanced,
    val layout: Layout = Layout.OnePerPage,
    val password: String? = null,
    val searchable: Boolean = false,
    /** A size the PDF must fit under, when the user picked one. */
    val limitBytes: Long? = null,
)

/** A recognised line of text in image pixel coordinates (origin top-left). */
data class TextLine(val text: String, val box: Rect)

class PdfExporter(private val store: DocStore) {

    /**
     * Writes [doc] as a PDF to [out]. [ocr] (when searchable) returns lines for a bitmap; the bitmap is
     * recycled by the exporter afterwards. [progress] receives (pagesDone, total).
     */
    suspend fun write(
        doc: Doc,
        opts: ExportOptions,
        out: OutputStream,
        ocr: (suspend (Bitmap) -> List<TextLine>)? = null,
        progress: (Int, Int) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        val w = PdfWriter(out, opts.password)
        val catalog = w.reserve()
        val pagesNum = w.reserve()
        val searchable = opts.searchable && ocr != null
        val font = if (searchable) w.reserve() else 0
        if (searchable) w.obj(font, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")

        val groups = when (opts.layout) {
            Layout.OnePerPage -> doc.pages.map { listOf(it) }
            else -> doc.pages.chunked(2)
        }
        val kids = ArrayList<Int>()
        var done = 0
        for (group in groups) {
            coroutineContext.ensureActive()
            val placed = group.map { page ->
                val img = embedImage(w, doc.id, page, opts.quality)
                val lines = if (searchable) runOcr(doc.id, page, img, ocr!!) else emptyList()
                done++
                progress(done, doc.pages.size)
                Triple(page, img, lines)
            }
            val (sw, sh, slots) = sheet(opts, placed.map { (p, img, _) -> displayed(img, p.rotation) })
            val content = StringBuilder()
            placed.forEachIndexed { i, (page, img, lines) ->
                val m = placeMatrix(img, page.rotation, slots[i], opts.layout == Layout.IdCard)
                content.append("q ${m.joinToString(" ") { f(it) }} cm /Im$i Do Q\n")
                if (lines.isNotEmpty()) content.append(textLayer(m, img, lines))
            }
            val contentNum = w.reserve()
            w.stream(contentNum, "<< >>", content.toString().toByteArray(Charsets.ISO_8859_1))
            val pageNum = w.reserve()
            val xobjects = placed.indices.joinToString(" ") { "/Im$it ${placed[it].second.num} 0 R" }
            val fontRes = if (searchable) " /Font << /F1 $font 0 R >>" else ""
            w.obj(pageNum, "<< /Type /Page /Parent $pagesNum 0 R /MediaBox [0 0 ${f(sw)} ${f(sh)}] " +
                "/Resources << /XObject << $xobjects >>$fontRes >> /Contents $contentNum 0 R >>")
            kids += pageNum
        }
        w.obj(pagesNum, "<< /Type /Pages /Kids [${kids.joinToString(" ") { "$it 0 R" }}] /Count ${kids.size} >>")
        w.obj(catalog, "<< /Type /Catalog /Pages $pagesNum 0 R${w.catalogExtras} >>")
        val info = w.reserve()
        val date = "D:" + SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(Date())
        w.obj(info, "<< /Title ${w.string(winAnsi(doc.name))} /Producer ${w.string(winAnsi("Docfold"))} " +
            "/Creator ${w.string(winAnsi("Docfold Doc Scanner"))} /CreationDate ${w.string(winAnsi(date))} >>")
        w.finish(catalog, info)
    }

    /** Estimated PDF size in bytes, measured on up to three sample pages. */
    suspend fun estimate(doc: Doc, quality: Quality): Long = withContext(Dispatchers.IO) {
        if (doc.pages.isEmpty()) return@withContext 0L
        val overhead = 1500L + doc.pages.size * 450L
        if (quality.maxSide == null) return@withContext overhead + doc.pages.sumOf { store.pageFile(doc.id, it.id).length() }
        val n = doc.pages.size
        val samples = listOf(0, n / 2, n - 1).distinct().map { doc.pages[it] }
        var bytes = 0L
        var originals = 0L
        for (p in samples) {
            coroutineContext.ensureActive()
            bytes += encode(store.pageFile(doc.id, p.id), quality)?.size ?: 0
            originals += store.pageFile(doc.id, p.id).length()
        }
        // Scale by the original sizes of all pages, so a page much denser than the samples is still counted.
        val ratio = if (originals > 0) bytes.toDouble() / originals else 1.0
        overhead + (doc.pages.sumOf { store.pageFile(doc.id, it.id).length() } * ratio).toLong()
    }

    /** The best quality whose estimate fits [limitBytes], or the smallest one when nothing fits. */
    suspend fun qualityFor(doc: Doc, limitBytes: Long): Pair<Quality, Long> {
        var last = Quality.Tiny to 0L
        for (q in Quality.entries) {
            val e = estimate(doc, q)
            last = q to e
            if (e <= limitBytes * 0.92) return last
        }
        return last
    }

    /** Writes one page as an upright JPEG (rotation applied). */
    suspend fun writeJpeg(docId: String, page: Page, out: OutputStream) = withContext(Dispatchers.IO) {
        val file = store.pageFile(docId, page.id)
        if (page.rotation == 0) {
            file.inputStream().use { it.copyTo(out) }
        } else {
            val bmp = ImageIO.rotate(ImageIO.decodeFile(file, Int.MAX_VALUE) ?: return@withContext, page.rotation)
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bmp.recycle()
        }
    }

    // ── internals ───────────────────────────────────────────────────────────────────────────────────

    private class Img(val num: Int, val w: Int, val h: Int)

    private fun embedImage(w: PdfWriter, docId: String, page: Page, quality: Quality): Img {
        val file = store.pageFile(docId, page.id)
        var bytes = if (quality.maxSide == null) file.readBytes() else encode(file, quality)
        var info = bytes?.let(::jpegInfo)
        if (info == null) { // unreadable or exotic JPEG: re-encode through Android so the PDF stays valid
            bytes = encode(file, Quality.High)
            info = bytes?.let(::jpegInfo) ?: error("Page image is damaged")
        }
        val (iw, ih, comps) = info
        val cs = when (comps) { 1 -> "/DeviceGray"; 4 -> "/DeviceCMYK /Decode [1 0 1 0 1 0 1 0]"; else -> "/DeviceRGB" }
        val num = w.reserve()
        w.stream(num, "<< /Type /XObject /Subtype /Image /Width $iw /Height $ih /ColorSpace $cs /BitsPerComponent 8 /Filter /DCTDecode >>", bytes!!)
        return Img(num, iw, ih)
    }

    private fun encode(file: File, q: Quality): ByteArray? {
        val side = q.maxSide ?: return file.readBytes()
        val bmp = ImageIO.fit(ImageIO.decodeFile(file, side) ?: return null, side)
        val bos = ByteArrayOutputStream(256 * 1024)
        bmp.compress(Bitmap.CompressFormat.JPEG, q.jpeg, bos)
        bmp.recycle()
        return bos.toByteArray()
    }

    private suspend fun runOcr(docId: String, page: Page, img: Img, ocr: suspend (Bitmap) -> List<TextLine>): List<TextLine> {
        val bmp = ImageIO.decodeFile(store.pageFile(docId, page.id), 1800) ?: return emptyList()
        val sx = img.w / bmp.width.toFloat()
        val sy = img.h / bmp.height.toFloat()
        val lines = runCatching { ocr(bmp) }.getOrDefault(emptyList())
        bmp.recycle()
        return lines.map { l ->
            TextLine(l.text, Rect((l.box.left * sx).toInt(), (l.box.top * sy).toInt(), (l.box.right * sx).toInt(), (l.box.bottom * sy).toInt()))
        }
    }

    private fun displayed(img: Img, rotation: Int) = if (rotation % 180 == 0) img.w.toFloat() to img.h.toFloat() else img.h.toFloat() to img.w.toFloat()

    private data class Slot(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Sheet size and one slot per image on it. */
    private fun sheet(o: ExportOptions, dims: List<Pair<Float, Float>>): Triple<Float, Float, List<Slot>> {
        val m = o.margin.pt
        if (o.layout == Layout.OnePerPage) {
            val (dw, dh) = dims[0]
            if (o.paper == PaperSize.Auto) {
                // Long side matches A4's long side, so every page prints at a natural size.
                val s = 841.89f / max(dw, dh)
                val pw = dw * s; val ph = dh * s
                return Triple(pw + 2 * m, ph + 2 * m, listOf(Slot(m, m, pw, ph)))
            }
            val land = dw > dh
            val sw = if (land) o.paper.h else o.paper.w
            val sh = if (land) o.paper.w else o.paper.h
            return Triple(sw, sh, listOf(Slot(m, m, sw - 2 * m, sh - 2 * m)))
        }
        val paper = if (o.paper == PaperSize.Auto) PaperSize.A4 else o.paper
        val sw = paper.w; val sh = paper.h
        val mm = max(m, 24f)
        val gap = 18f
        val slotH = (sh - 2 * mm - gap) / 2
        val top = Slot(mm, mm + slotH + gap, sw - 2 * mm, slotH)
        val bottom = Slot(mm, mm, sw - 2 * mm, slotH)
        return Triple(sw, sh, listOf(top, bottom).take(dims.size))
    }

    /** The `cm` matrix that draws the image (unit square) rotated and fitted into [slot]. */
    private fun placeMatrix(img: Img, rotation: Int, slot: Slot, idCard: Boolean): FloatArray {
        val (dw, dh) = displayed(img, rotation)
        val boxW: Float
        val boxH: Float
        if (idCard) { // ISO/IEC 7810 ID-1: 85.6 × 54 mm, printed at real size like a photocopy
            boxW = if (dw >= dh) ID_LONG else ID_SHORT
            boxH = if (dw >= dh) ID_SHORT else ID_LONG
        } else { boxW = slot.w; boxH = slot.h }
        val s = min(min(boxW / dw, boxH / dh), min(slot.w / dw, slot.h / dh))
        val w = dw * s; val h = dh * s
        val x = slot.x + (slot.w - w) / 2
        val y = slot.y + (slot.h - h) / 2
        return when (((rotation % 360) + 360) % 360) {
            90 -> floatArrayOf(0f, -h, w, 0f, x, y + h)
            180 -> floatArrayOf(-w, 0f, 0f, -h, x + w, y + h)
            270 -> floatArrayOf(0f, h, -w, 0f, x + w, y)
            else -> floatArrayOf(w, 0f, 0f, h, x, y)
        }
    }

    /**
     * Invisible (render mode 3) text over each recognised line, so the PDF is searchable and copyable.
     * Strings inside a content stream stay plain: the stream as a whole is what gets encrypted.
     */
    private fun textLayer(m: FloatArray, img: Img, lines: List<TextLine>): String {
        val sb = StringBuilder("q ${m.joinToString(" ") { f(it) }} cm ${f(1f / img.w)} 0 0 ${f(1f / img.h)} 0 0 cm\nBT 3 Tr\n")
        for (l in lines) {
            val bytes = winAnsi(l.text)
            if (bytes.isEmpty()) continue
            val height = l.box.height().toFloat().coerceAtLeast(4f)
            val size = height * 0.82f
            val natural = bytes.sumOf { HELVETICA.getOrElse((it.toInt() and 0xff) - 32) { 556 } } * size / 1000f
            val scale = if (natural > 0) 100f * l.box.width() / natural else 100f
            val baseline = img.h - l.box.bottom + height * 0.2f
            sb.append("/F1 ${f(size)} Tf ${f(scale)} Tz 1 0 0 1 ${f(l.box.left.toFloat())} ${f(baseline)} Tm ${PdfWriter.literal(bytes)} Tj\n")
        }
        sb.append("ET Q\n")
        return sb.toString()
    }

    companion object {
        private const val ID_LONG = 242.65f
        private const val ID_SHORT = 153.07f

        fun f(v: Float): String {
            val s = String.format(Locale.ROOT, "%.4f", v).trimEnd('0').trimEnd('.')
            return if (s == "-0") "0" else s
        }

        /** Width, height and colour components from a JPEG's SOF marker, without decoding pixels. */
        fun jpegInfo(b: ByteArray): Triple<Int, Int, Int>? {
            if (b.size < 4 || (b[0].toInt() and 0xff) != 0xFF || (b[1].toInt() and 0xff) != 0xD8) return null
            var i = 2
            while (i + 9 < b.size) {
                if ((b[i].toInt() and 0xff) != 0xFF) { i++; continue }
                val mk = b[i + 1].toInt() and 0xff
                if (mk == 0xFF) { i++; continue }
                if (mk == 0x01 || mk in 0xD0..0xD8) { i += 2; continue }
                val len = ((b[i + 2].toInt() and 0xff) shl 8) or (b[i + 3].toInt() and 0xff)
                if (mk in 0xC0..0xCF && mk != 0xC4 && mk != 0xC8 && mk != 0xCC) {
                    val h = ((b[i + 5].toInt() and 0xff) shl 8) or (b[i + 6].toInt() and 0xff)
                    val w = ((b[i + 7].toInt() and 0xff) shl 8) or (b[i + 8].toInt() and 0xff)
                    val c = b[i + 9].toInt() and 0xff
                    return if (w > 0 && h > 0) Triple(w, h, c) else null
                }
                i += 2 + len
            }
            return null
        }

        /** Encodes to WinAnsi (the encoding of the built-in Helvetica); anything else becomes '?'. */
        fun winAnsi(s: String): ByteArray {
            val out = ByteArrayOutputStream(s.length)
            for (ch in s) {
                val c = ch.code
                out.write(
                    when {
                        c in 32..126 || c in 160..255 -> c
                        else -> when (ch) {
                            '€' -> 0x80; '‚' -> 0x82; '„' -> 0x84; '…' -> 0x85; '•' -> 0x95; '–' -> 0x96; '—' -> 0x97
                            '‘' -> 0x91; '’' -> 0x92; '“' -> 0x93; '”' -> 0x94; '™' -> 0x99; '\t' -> 32
                            else -> '?'.code
                        }
                    }
                )
            }
            return out.toByteArray()
        }

        /** Helvetica advance widths for ASCII 32–126 (Adobe AFM), in 1/1000 em. */
        private val HELVETICA = intArrayOf(
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584,
        )
    }
}
