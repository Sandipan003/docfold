package com.docfold.scanner.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.docfold.scanner.data.PageFilter
import com.docfold.scanner.imaging.Filters
import com.docfold.scanner.imaging.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Page images for the UI: decoded off the main thread at the size they are shown, two at a time, and kept in a
 * memory cache sized to the phone (1/8 of the app's heap) so scrolling back is instant without risking OOM.
 */
object Images {
    @OptIn(ExperimentalCoroutinesApi::class)
    val decoder = Dispatchers.IO.limitedParallelism(2)

    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    fun evict(prefix: String) {
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    /** Loads [file] with its long side near [side] px. [key] must change whenever the file's pixels change. */
    @Composable
    fun load(file: File, key: String, side: Int, filter: PageFilter? = null, crop: List<Float>? = null): State<ImageBitmap?> {
        val cacheKey = "$key@$side${filter?.key.orEmpty()}${crop?.hashCode() ?: ""}"
        // Keeps showing the previous image while a new version loads, so edits never flash blank.
        return produceState(cache.get(cacheKey), cacheKey) {
            cache.get(cacheKey)?.let { value = it; return@produceState }
            value = withContext(decoder) {
                runCatching {
                    var bmp: Bitmap = ImageIO.decodeFile(file, side) ?: return@runCatching null
                    bmp = ImageIO.fit(bmp, side)
                    if (crop != null) bmp = Filters.warp(bmp, crop, side).also { bmp.recycle() }
                    if (filter != null) bmp = Filters.apply(bmp, filter)
                    bmp.prepareToDraw()
                    bmp.asImageBitmap().also { cache.put(cacheKey, it) }
                }.getOrNull()
            }
        }
    }
}
