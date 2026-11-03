package com.docfold.scanner.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.docfold.scanner.data.PageFilter
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Document clean-up filters. Each one first divides the page by an estimate of its own paper colour, which
 * flattens shadows and uneven light, then shapes the tones. Pixels are processed in horizontal strips so a
 * full page needs only a few hundred KB of working memory on top of the bitmap itself.
 */
object Filters {
    private const val GRID = 40
    private const val STRIP = 48

    fun apply(input: Bitmap, filter: PageFilter): Bitmap {
        if (filter == PageFilter.Original) return input
        val bmp = if (input.isMutable && input.config == Bitmap.Config.ARGB_8888) input
        else input.copy(Bitmap.Config.ARGB_8888, true).also { input.recycle() }
        val w = bmp.width
        val h = bmp.height
        val cell = max(8, ceil(max(w, h) / GRID.toFloat()).toInt())
        val gw = (w + cell - 1) / cell
        val gh = (h + cell - 1) / cell
        val bg = paperGrid(bmp, cell, gw, gh)

        val rowBg = FloatArray(gw * 3)
        val x0 = IntArray(w); val x1 = IntArray(w); val xt = FloatArray(w)
        for (x in 0 until w) {
            val fx = (x + 0.5f) / cell - 0.5f
            val i = fx.toInt().coerceIn(0, gw - 1)
            x0[x] = i; x1[x] = min(i + 1, gw - 1); xt[x] = (fx - i).coerceIn(0f, 1f)
        }
        val buf = IntArray(w * STRIP)
        var y = 0
        while (y < h) {
            val rows = min(STRIP, h - y)
            bmp.getPixels(buf, 0, w, 0, y, w, rows)
            for (r in 0 until rows) {
                val fy = (y + r + 0.5f) / cell - 0.5f
                val j0 = fy.toInt().coerceIn(0, gh - 1)
                val j1 = min(j0 + 1, gh - 1)
                val ty = (fy - j0).coerceIn(0f, 1f)
                for (k in 0 until gw * 3) {
                    val a = bg[j0 * gw * 3 + k]; val b = bg[j1 * gw * 3 + k]
                    rowBg[k] = a + (b - a) * ty
                }
                val off = r * w
                for (x in 0 until w) {
                    val c = buf[off + x]
                    val t = xt[x]; val i0 = x0[x] * 3; val i1 = x1[x] * 3
                    val br = rowBg[i0] + (rowBg[i1] - rowBg[i0]) * t
                    val bgG = rowBg[i0 + 1] + (rowBg[i1 + 1] - rowBg[i0 + 1]) * t
                    val bb = rowBg[i0 + 2] + (rowBg[i1 + 2] - rowBg[i0 + 2]) * t
                    val nr = norm((c shr 16) and 0xff, br)
                    val ng = norm((c shr 8) and 0xff, bgG)
                    val nb = norm(c and 0xff, bb)
                    buf[off + x] = when (filter) {
                        PageFilter.Magic -> rgb(deepen(nr), deepen(ng), deepen(nb))
                        PageFilter.Gray -> deepen(luma(nr, ng, nb)).let { rgb(it, it, it) }
                        else -> ink(luma(nr, ng, nb)).let { rgb(it, it, it) }
                    }
                }
            }
            bmp.setPixels(buf, 0, w, 0, y, w, rows)
            y += rows
        }
        return bmp
    }

    /** Per cell, the average colour of its brightest pixels: the paper, ignoring the ink on it. Then smoothed. */
    private fun paperGrid(bmp: Bitmap, cell: Int, gw: Int, gh: Int): FloatArray {
        val w = bmp.width
        val h = bmp.height
        val grid = FloatArray(gw * gh * 3)
        val strip = IntArray(w * cell)
        for (gy in 0 until gh) {
            val y = gy * cell
            val rows = min(cell, h - y)
            bmp.getPixels(strip, 0, w, 0, y, w, rows)
            for (gx in 0 until gw) {
                val xs = gx * cell
                val xe = min(xs + cell, w)
                var maxL = 0
                var r = 0
                while (r < rows) {
                    var x = xs
                    while (x < xe) {
                        val c = strip[r * w + x]
                        val l = luma((c shr 16) and 0xff, (c shr 8) and 0xff, c and 0xff)
                        if (l > maxL) maxL = l
                        x += 2
                    }
                    r += 2
                }
                val floor = maxL - 28
                var sr = 0L; var sg = 0L; var sb = 0L; var n = 0
                r = 0
                while (r < rows) {
                    var x = xs
                    while (x < xe) {
                        val c = strip[r * w + x]
                        val cr = (c shr 16) and 0xff; val cg = (c shr 8) and 0xff; val cb = c and 0xff
                        if (luma(cr, cg, cb) >= floor) { sr += cr; sg += cg; sb += cb; n++ }
                        x += 2
                    }
                    r += 2
                }
                val i = (gy * gw + gx) * 3
                if (n == 0) n = 1
                grid[i] = sr / n.toFloat(); grid[i + 1] = sg / n.toFloat(); grid[i + 2] = sb / n.toFloat()
            }
        }
        // Each cell already ignores its ink (brightest-pixel average), so no dilation is needed: lifting cells to
        // their brightest neighbour would overestimate the paper inside shadows and leave them grey.
        // Two light blurs remove seams between cells.
        var a = grid
        repeat(2) {
            val b = FloatArray(a.size)
            for (gy in 0 until gh) for (gx in 0 until gw) for (ch in 0 until 3) {
                var s = 0f
                for (dy in -1..1) for (dx in -1..1) {
                    val yy = (gy + dy).coerceIn(0, gh - 1); val xx = (gx + dx).coerceIn(0, gw - 1)
                    s += a[(yy * gw + xx) * 3 + ch]
                }
                b[(gy * gw + gx) * 3 + ch] = max(24f, s / 9f)
            }
            a = b
        }
        return a
    }

    private fun norm(v: Int, paper: Float) = min(255, (v * 262f / paper).roundToInt()) // slight lift: paper reads pure white
    private fun luma(r: Int, g: Int, b: Int) = (r * 77 + g * 150 + b * 29) shr 8
    private fun deepen(v: Int) = (255 - (255 - v) * 1.35f).roundToInt().coerceIn(0, 255)
    private fun ink(l: Int): Int {
        val t = ((l - 150) / 60f).coerceIn(0f, 1f)
        return (t * t * (3 - 2 * t) * 255).roundToInt()
    }
    private fun rgb(r: Int, g: Int, b: Int) = (0xff shl 24) or (r shl 16) or (g shl 8) or b

    /**
     * Perspective-corrects the quad (fractions of [src], clockwise from top-left) into a flat rectangle.
     * Android's Matrix handles the full 4-point projective transform, so no OpenCV is needed.
     */
    fun warp(src: Bitmap, quad: List<Float>, maxSide: Int = DeviceProfile.maxPageSide): Bitmap {
        val p = FloatArray(8) { i -> quad[i] * if (i % 2 == 0) src.width else src.height }
        fun d(a: Int, b: Int) = hypot(p[a * 2] - p[b * 2], p[a * 2 + 1] - p[b * 2 + 1])
        var ow = max(d(0, 1), d(3, 2))
        var oh = max(d(0, 3), d(1, 2))
        val s = min(1f, maxSide / max(ow, oh))
        ow = max(1f, ow * s); oh = max(1f, oh * s)
        val m = Matrix()
        m.setPolyToPoly(p, 0, floatArrayOf(0f, 0f, ow, 0f, ow, oh, 0f, oh), 0, 4)
        val out = Bitmap.createBitmap(ow.roundToInt(), oh.roundToInt(), Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        return out
    }
}
