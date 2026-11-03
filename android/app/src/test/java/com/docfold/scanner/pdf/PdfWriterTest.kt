package com.docfold.scanner.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Writes real PDFs (plain and AES-256 protected) to app/build/pdf-test/ so they can also be checked with an
 * independent reader. The structure mirrors PdfExporter: catalog, pages tree, one JPEG XObject per page.
 */
class PdfWriterTest {
    private fun jpeg(w: Int, h: Int, color: Color): ByteArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color.WHITE; g.fillRect(0, 0, w, h)
        g.color = color; g.fillRect(w / 8, h / 8, w / 2, h / 10)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(img, "jpg", it) }.toByteArray()
    }

    private fun write(password: String?, file: File) {
        file.parentFile.mkdirs()
        file.outputStream().use { out ->
            val w = PdfWriter(out, password)
            val catalog = w.reserve(); val pages = w.reserve(); val font = w.reserve()
            w.obj(font, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
            val kids = ArrayList<Int>()
            listOf(Color(0xD7262E), Color(0x1B3F8F)).forEachIndexed { i, c ->
                val bytes = jpeg(600, 800, c)
                val (iw, ih, comps) = PdfExporter.jpegInfo(bytes)!!
                assertEquals(600, iw); assertEquals(800, ih); assertEquals(3, comps)
                val im = w.reserve()
                w.stream(im, "<< /Type /XObject /Subtype /Image /Width $iw /Height $ih /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode >>", bytes)
                val text = PdfWriter.literal(PdfExporter.winAnsi("Docfold page ${i + 1} – café"))
                val content = "q 595 0 0 793 0 0 cm /Im0 Do Q\nBT /F1 18 Tf 3 Tr 1 0 0 1 72 700 Tm $text Tj ET\n"
                val cn = w.reserve()
                w.stream(cn, "<< >>", content.toByteArray(Charsets.ISO_8859_1))
                val pn = w.reserve()
                w.obj(pn, "<< /Type /Page /Parent $pages 0 R /MediaBox [0 0 595 793] /Resources << /XObject << /Im0 $im 0 R >> /Font << /F1 $font 0 R >> >> /Contents $cn 0 R >>")
                kids += pn
            }
            w.obj(pages, "<< /Type /Pages /Kids [${kids.joinToString(" ") { "$it 0 R" }}] /Count ${kids.size} >>")
            w.obj(catalog, "<< /Type /Catalog /Pages $pages 0 R${w.catalogExtras} >>")
            val info = w.reserve()
            w.obj(info, "<< /Title ${w.string(PdfExporter.winAnsi("Test doc"))} /Producer ${w.string(PdfExporter.winAnsi("Docfold"))} >>")
            w.finish(catalog, info)
        }
    }

    @Test fun writesPlainAndEncryptedPdfs() {
        val dir = File("build/pdf-test")
        write(null, File(dir, "plain.pdf"))
        write("s3cret-pass", File(dir, "locked.pdf"))
        val plain = File(dir, "plain.pdf").readBytes().toString(Charsets.ISO_8859_1)
        assertTrue(plain.startsWith("%PDF-1.7"))
        assertTrue(plain.contains("/Count 2"))
        assertTrue(plain.trimEnd().endsWith("%%EOF"))
        val locked = File(dir, "locked.pdf").readBytes().toString(Charsets.ISO_8859_1)
        assertTrue(locked.contains("/V 5 /R 6"))
        assertTrue(!locked.contains("Docfold page"))
    }

    @Test fun formatsNumbers() {
        assertEquals("12.5", PdfExporter.f(12.5f))
        assertEquals("0", PdfExporter.f(-0.00001f))
        assertEquals("595.28", PdfExporter.f(595.28f))
    }
}
