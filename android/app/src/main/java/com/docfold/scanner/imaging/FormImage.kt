package com.docfold.scanner.imaging

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** A page turned into the exact photo or signature image an online form asks for: fixed pixels, capped file size. */
object FormImage {
    private const val BEST = 95
    private const val WORST = 10

    /** A finished JPEG, and whether it came in under the size asked for. */
    class Result(val bytes: ByteArray, val fits: Boolean)

    /** The centred part of a [srcW] x [srcH] image with the shape of [w] x [h], as left, top, width, height. */
    fun centerCrop(srcW: Int, srcH: Int, w: Int, h: Int): IntArray {
        val scale = minOf(srcW / w.toFloat(), srcH / h.toFloat())
        val cw = (w * scale).roundToInt().coerceIn(1, srcW)
        val ch = (h * scale).roundToInt().coerceIn(1, srcH)
        return intArrayOf((srcW - cw) / 2, (srcH - ch) / 2, cw, ch)
    }

    /** The highest JPEG quality whose [size] fits [maxBytes], or the lowest quality when nothing does. */
    fun bestQuality(maxBytes: Long, size: (Int) -> Long): Int {
        var low = WORST
        var high = BEST
        var best = WORST
        while (low <= high) {
            val q = (low + high) / 2
            if (size(q) <= maxBytes) { best = q; low = q + 1 } else high = q - 1
        }
        return best
    }

    /** Forms can set a minimum size too. A JPEG comment block reaches it without touching the picture. */
    fun padTo(jpeg: ByteArray, minBytes: Long): ByteArray {
        var missing = minBytes - jpeg.size
        if (missing <= 0) return jpeg
        val out = ByteArrayOutputStream(minBytes.toInt() + 4)
        out.write(jpeg, 0, 2) // the start-of-image marker has to stay first
        while (missing > 0) {
            val block = missing.coerceIn(4, 65_537).toInt() // 2 marker bytes, then a 2-byte length that counts itself
            out.write(0xFF); out.write(0xFE)
            out.write((block - 2) shr 8); out.write((block - 2) and 0xFF)
            out.write(ByteArray(block - 4))
            missing -= block
        }
        out.write(jpeg, 2, jpeg.size - 2)
        return out.toByteArray()
    }

    fun render(src: File, rotation: Int, width: Int, height: Int, minBytes: Long, maxBytes: Long): Result? {
        val upright = ImageIO.rotate(ImageIO.decodeFile(src, max(width, height) * 2) ?: return null, rotation)
        val (x, y, cw, ch) = centerCrop(upright.width, upright.height, width, height).toList()
        val cropped = Bitmap.createBitmap(upright, x, y, cw, ch)
        val sized = Bitmap.createScaledBitmap(cropped, width, height, true)
        fun encode(q: Int) = ByteArrayOutputStream().also { sized.compress(Bitmap.CompressFormat.JPEG, q, it) }.toByteArray()
        val bytes = padTo(encode(bestQuality(maxBytes) { encode(it).size.toLong() }), minBytes)
        listOf(upright, cropped, sized).distinct().forEach { it.recycle() }
        return Result(bytes, bytes.size <= maxBytes)
    }
}
