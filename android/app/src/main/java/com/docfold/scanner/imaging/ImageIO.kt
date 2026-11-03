package com.docfold.scanner.imaging

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Sizes the app works at, picked once from the phone's memory so 2 GB devices never run out. */
object DeviceProfile {
    var maxPageSide = 2600; private set
    var lowRam = false; private set

    fun init(context: Context) {
        val am = context.getSystemService<ActivityManager>() ?: return
        lowRam = am.isLowRamDevice || am.memoryClass <= 128
        maxPageSide = if (lowRam) 2000 else 2600
    }
}

object ImageIO {
    const val THUMB_SIDE = 420
    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    fun bounds(file: File): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, o)
        return o.outWidth to o.outHeight
    }

    /** Largest power-of-two sample size that keeps the long side at or above [target]. */
    fun sampleSize(w: Int, h: Int, target: Int): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= target) s *= 2
        return s
    }

    fun decodeFile(file: File, targetLongSide: Int): Bitmap? {
        val (w, h) = bounds(file)
        if (w <= 0 || h <= 0) return null
        val o = BitmapFactory.Options().apply { inSampleSize = sampleSize(w, h, targetLongSide) }
        return BitmapFactory.decodeFile(file.path, o)
    }

    /** Decodes [uri] upright (EXIF applied) with its long side at most [maxSide]. */
    fun decodeUri(context: Context, uri: Uri, maxSide: Int = DeviceProfile.maxPageSide): Bitmap? {
        val cr = context.contentResolver
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decode always returns null; the answer is in o.outWidth/outHeight.
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(o.outWidth, o.outHeight, maxSide) }
        val raw = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val degrees = cr.openInputStream(uri)?.use(::exifRotation) ?: 0
        return fit(rotate(raw, degrees), maxSide)
    }

    private fun exifRotation(stream: InputStream): Int = runCatching {
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
    }.getOrDefault(0)

    /** Returns a rotated copy and recycles the input (or returns the input when nothing changes). */
    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return src
        val m = Matrix().apply { postRotate(d.toFloat()) }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out !== src) src.recycle()
        return out
    }

    /** Scales down so the long side is at most [maxSide]; recycles the input when a copy is made. */
    fun fit(src: Bitmap, maxSide: Int): Bitmap {
        val long = max(src.width, src.height)
        if (long <= maxSide) return src
        val s = maxSide.toFloat() / long
        val w = (src.width * s).roundToInt().coerceAtLeast(1)
        val h = (src.height * s).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, Matrix().apply { setScale(w / src.width.toFloat(), h / src.height.toFloat()) }, filterPaint)
        src.recycle()
        return out
    }

    fun writeJpeg(bitmap: Bitmap, file: File, quality: Int = 90) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    fun writeThumb(bitmap: Bitmap, file: File) {
        val long = max(bitmap.width, bitmap.height)
        val s = THUMB_SIDE.toFloat() / long
        val thumb = if (s < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * s).roundToInt().coerceAtLeast(1), (bitmap.height * s).roundToInt().coerceAtLeast(1), true) else bitmap
        writeJpeg(thumb, file, 82)
        if (thumb !== bitmap) thumb.recycle()
    }
}
