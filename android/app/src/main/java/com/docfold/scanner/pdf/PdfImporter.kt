package com.docfold.scanner.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.docfold.scanner.data.DocStore
import com.docfold.scanner.imaging.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Turns each page of an existing PDF into a Docfold page, which is how PDFs get merged and rearranged. */
object PdfImporter {
    class Locked : Exception("This PDF is password-protected")

    /** Returns the number of pages imported. Throws [Locked] for encrypted PDFs. */
    suspend fun import(context: Context, store: DocStore, docId: String, uri: Uri, onProgress: (Int, Int) -> Unit): Int =
        withContext(Dispatchers.IO) {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext 0
            val renderer = try { PdfRenderer(pfd) } catch (e: SecurityException) { pfd.close(); throw Locked() }
            var count = 0
            renderer.use { r ->
                val target = min(DeviceProfile.maxPageSide, 2200)
                for (i in 0 until r.pageCount) {
                    coroutineContext.ensureActive()
                    r.openPage(i).use { page ->
                        val s = target / max(page.width, page.height).toFloat()
                        val bmp = Bitmap.createBitmap((page.width * s).roundToInt().coerceAtLeast(1), (page.height * s).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        store.addBitmap(docId, bmp)
                    }
                    count++
                    onProgress(i + 1, r.pageCount)
                }
            }
            count
        }
}
