package com.docfold.scanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.docfold.scanner.ui.theme.AirmailBlue
import com.docfold.scanner.ui.theme.AirmailRed
import com.docfold.scanner.ui.theme.ArchivoCondensed
import kotlin.math.min
import kotlin.math.sin

// Engraved-plate illustrations, drawn with strokes and hatching on a 320-unit square so they stay crisp on
// every screen and cost nothing in APK size. Each scene loops one small motion; with system animations
// off, Compose freezes them at rest.

private val InkLine = Color(0xFF14161A)
private val Paper = Color(0xFFFFFFFF)
private const val U = 320f

private fun DrawScope.fit(block: DrawScope.() -> Unit) {
    val s = min(size.width, size.height) / U
    translate((size.width - U * s) / 2, (size.height - U * s) / 2) { scale(s, s, Offset.Zero) { block() } }
}

/** Parallel engraving lines clipped to [path]. */
private fun DrawScope.hatch(path: Path, color: Color = InkLine, gap: Float = 6f, width: Float = 1.1f, slope: Float = 1f) {
    val b = path.getBounds()
    clipPath(path) {
        var x = b.left - b.height * slope
        while (x < b.right) {
            drawLine(color, Offset(x, b.bottom), Offset(x + b.height * slope, b.top), width)
            x += gap
        }
    }
}

private fun rectPath(r: Rect) = Path().apply { addRect(r) }

/** A sheet of paper with ruled "text" lines and an ink outline. */
private fun DrawScope.sheet(r: Rect, lines: Int = 6, heading: Boolean = true, lineColor: Color = Color(0xFFB8BBC2)) {
    drawRect(Paper, r.topLeft, r.size)
    drawRect(InkLine, r.topLeft, r.size, style = Stroke(2.4f))
    val pad = r.width * 0.12f
    var y = r.top + pad + 4f
    if (heading) {
        drawLine(InkLine, Offset(r.left + pad, y), Offset(r.left + r.width * 0.55f, y), 5f, StrokeCap.Round)
        y += 14f
    }
    val step = (r.bottom - pad - y) / lines
    repeat(lines) { i ->
        val end = if (i == lines - 1) 0.5f else if (i % 3 == 1) 0.82f else 0.9f
        drawLine(lineColor, Offset(r.left + pad, y), Offset(r.left + pad + (r.width - 2 * pad) * end, y), 3f, StrokeCap.Round)
        y += step
    }
}

private fun DrawScope.stamp(r: Rect, ink: Color, value: String, measurer: androidx.compose.ui.text.TextMeasurer) {
    val outline = PerforatedShape().createOutline(r.size, LayoutDirection.Ltr, this)
    translate(r.left, r.top) {
        drawOutline(outline, Paper)
        val inner = Rect(7f, 7f, r.width - 7f, r.height - 24f)
        drawRect(ink, inner.topLeft, inner.size)
        hatch(rectPath(inner), Color.White.copy(alpha = 0.35f), gap = 5f, width = 1f)
        sheet(Rect(inner.left + 7f, inner.top + 7f, inner.right - 7f, inner.bottom - 7f), lines = 3, heading = false)
        // 22 scene units tall: measured in unscaled pixels, the surrounding scene transform scales it with the art.
        val t = measurer.measure(value, TextStyle(fontFamily = ArchivoCondensed, fontSize = (22f / density / fontScale).sp, color = ink))
        drawText(t, topLeft = Offset(8f, r.height - 4f - t.size.height))
        drawOutline(outline, InkLine, style = Stroke(1.2f))
    }
}

/** Chapter 1 — a pile of everyday paper, and the phone that will carry it. */
@Composable
fun PaperPileScene(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "pile")
    val sway by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "sway")
    Canvas(modifier) {
        fit {
            // receipt with a torn edge
            rotate(-12f + sway, Offset(90f, 170f)) {
                val r = Rect(48f, 70f, 132f, 250f)
                val p = Path().apply {
                    moveTo(r.left, r.top); lineTo(r.right, r.top); lineTo(r.right, r.bottom)
                    var x = r.right; var up = true
                    while (x > r.left) { x -= 8f; lineTo(x.coerceAtLeast(r.left), if (up) r.bottom - 7f else r.bottom); up = !up }
                    close()
                }
                drawPath(p, Paper); drawPath(p, InkLine, style = Stroke(2.4f))
                for (i in 0 until 9) {
                    val y = r.top + 20f + i * 17f
                    drawLine(Color(0xFFB8BBC2), Offset(r.left + 10f, y), Offset(r.left + 44f, y), 3f, StrokeCap.Round)
                    drawLine(InkLine, Offset(r.right - 26f, y), Offset(r.right - 10f, y), 3f, StrokeCap.Round)
                }
            }
            // form
            rotate(7f - sway * 0.6f, Offset(200f, 160f)) {
                sheet(Rect(130f, 52f, 262f, 228f), lines = 8)
                drawRect(AirmailRed, Offset(222f, 64f), Size(26f, 26f), style = Stroke(2.4f))
            }
            // ID card
            rotate(-4f + sway * 0.8f, Offset(170f, 255f)) {
                val card = Rect(104f, 214f, 236f, 296f)
                drawRoundRect(Paper, card.topLeft, card.size, CornerRadius(10f))
                drawRect(AirmailBlue, Offset(card.left, card.top + 12f), Size(card.width, 12f))
                val face = Path().apply { addOval(Rect(card.left + 14f, card.top + 32f, card.left + 50f, card.top + 72f)) }
                hatch(face, AirmailBlue, gap = 4f)
                drawPath(face, InkLine, style = Stroke(2f))
                for (i in 0 until 3) drawLine(InkLine, Offset(card.left + 62f, card.top + 40f + i * 12f), Offset(card.right - 16f - i * 14f, card.top + 40f + i * 12f), 3f, StrokeCap.Round)
                drawRoundRect(InkLine, card.topLeft, card.size, CornerRadius(10f), style = Stroke(2.4f))
            }
        }
    }
}

/** Chapter 2 — the phone finds the page edges and a light bar sweeps it flat. */
@Composable
fun ScanScene(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    Canvas(modifier) {
        fit {
            // the page on the table, seen in perspective
            val page = Path().apply { moveTo(78f, 92f); lineTo(246f, 80f); lineTo(266f, 266f); lineTo(58f, 258f); close() }
            drawPath(page, Paper)
            hatch(Path().apply { moveTo(58f, 258f); lineTo(266f, 266f); lineTo(270f, 280f); lineTo(54f, 272f); close() }, InkLine, gap = 5f)
            drawPath(page, InkLine, style = Stroke(2.4f))
            for (i in 0 until 7) {
                val f = i / 7f
                drawLine(Color(0xFFB8BBC2), Offset(92f - f * 14f, 120f + f * 120f), Offset(222f + f * 24f, 110f + f * 128f), 3f, StrokeCap.Round)
            }
            // phone frame
            val phone = Rect(40f, 34f, 284f, 300f)
            drawRoundRect(InkLine, phone.topLeft, phone.size, CornerRadius(30f), style = Stroke(5f))
            drawLine(InkLine, Offset(140f, 46f), Offset(184f, 46f), 5f, StrokeCap.Round)
            // detected corners
            listOf(Offset(78f, 92f), Offset(246f, 80f), Offset(266f, 266f), Offset(58f, 258f)).forEach {
                drawCircle(AirmailRed, 7f, it)
                drawCircle(Paper, 3f, it)
            }
            drawPath(page, AirmailRed.copy(alpha = 0.9f), style = Stroke(2f))
            // light bar
            val y = 86f + sweep * 178f
            drawRect(AirmailRed.copy(alpha = 0.16f), Offset(56f, y - 18f), Size(212f, 18f))
            drawLine(AirmailRed, Offset(56f, y), Offset(268f, y), 3f)
        }
    }
}

/** Chapter 3 — a sheet of page-stamps; one lifts and slides into a new place. */
@Composable
fun StampSheetScene(inks: List<Color>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val t = rememberInfiniteTransition(label = "stamps")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse), label = "move")
    Canvas(modifier) {
        fit {
            val w = 82f; val h = 108f; val gx = 98f; val gy = 126f
            val ox = 13f; val oy = 36f
            val lift = (sin(p * Math.PI).toFloat()).coerceAtLeast(0f)
            val values = listOf("01", "02", "03", "04", "05", "06")
            for (i in 0 until 6) {
                if (i == 1) continue
                val col = i % 3; val row = i / 3
                stamp(Rect(ox + col * gx, oy + row * gy, ox + col * gx + w, oy + row * gy + h), inks[i % inks.size], values[i], measurer)
            }
            // the lifted stamp travels from slot 2 to slot 5
            val from = Offset(ox + gx, oy); val to = Offset(ox + 2 * gx, oy + gy)
            val pos = Offset(from.x + (to.x - from.x) * p, from.y + (to.y - from.y) * p - lift * 16f)
            rotate(-6f * lift, Offset(pos.x + w / 2, pos.y + h / 2)) {
                for (k in 1..3) drawRect(InkLine.copy(alpha = 0.06f * lift), Offset(pos.x + k * 2f, pos.y + k * 3.5f), Size(w, h))
                stamp(Rect(pos.x, pos.y, pos.x + w, pos.y + h), inks[1 % inks.size], "02", measurer)
            }
        }
    }
}

/** Chapter 4 — the envelope, sealed, with the postmark landing. */
@Composable
fun EnvelopeScene(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val t = rememberInfiniteTransition(label = "env")
    val land by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "land")
    Canvas(modifier) {
        fit { envelope(measurer, postmark = land) }
    }
}

/** A quiet, static envelope for empty states. */
@Composable
fun EmptyEnvelope(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) { fit { envelope(measurer, postmark = -1f) } }
}

private fun DrawScope.envelope(measurer: androidx.compose.ui.text.TextMeasurer, postmark: Float) {
    val body = Rect(24f, 82f, 296f, 250f)
    for (k in 1..3) drawRect(InkLine.copy(alpha = 0.045f), Offset(body.left + k * 1.5f, body.top + k * 2.5f), body.size)
    drawRect(Paper, body.topLeft, body.size)
    // chevron border
    val stripe = 16f; val band = 8f
    fun border(x0: Float, y0: Float, x1: Float, y1: Float) {
        val len = if (y0 == y1) x1 - x0 else y1 - y0
        var d = 0f; var i = 0
        while (d < len) {
            val c = if (i % 2 == 0) AirmailRed else AirmailBlue
            if (y0 == y1) drawRect(c, Offset(x0 + d, y0), Size(min(stripe * 0.6f, len - d), band))
            else drawRect(c, Offset(x0, y0 + d), Size(band, min(stripe * 0.6f, len - d)))
            d += stripe; i++
        }
    }
    border(body.left, body.top, body.right, body.top)
    border(body.left, body.bottom - band, body.right, body.bottom - band)
    border(body.left, body.top, body.left, body.bottom)
    border(body.right - band, body.top, body.right - band, body.bottom)
    // flap
    val flap = Path().apply { moveTo(body.left + band, body.top + band); lineTo(body.center.x, body.top + 92f); lineTo(body.right - band, body.top + band) }
    drawPath(flap, InkLine, style = Stroke(2.2f))
    // address lines
    for (i in 0 until 3) drawLine(InkLine, Offset(96f, 186f + i * 18f), Offset(226f - i * 24f, 186f + i * 18f), 3.4f, StrokeCap.Round)
    // stamp
    stamp(Rect(220f, 100f, 278f, 176f), AirmailRed, "01", measurer)
    drawRect(InkLine, body.topLeft, body.size, style = Stroke(2.6f))
    if (postmark >= 0f) {
        val k = (postmark * 3f).coerceAtMost(1f)
        val s = 1.5f - 0.5f * k
        val a = if (postmark > 0.8f) (1f - postmark) * 5f else k
        val c = Offset(210f, 150f)
        rotate(-16f, c) {
            scale(s, s, c) {
                drawCircle(AirmailBlue.copy(alpha = 0.75f * a), 44f, c, style = Stroke(3f))
                drawCircle(AirmailBlue.copy(alpha = 0.75f * a), 28f, c, style = Stroke(2f))
                for (i in 0 until 4) drawLine(AirmailBlue.copy(alpha = 0.6f * a), Offset(c.x + 50f, c.y - 18f + i * 12f), Offset(c.x + 120f, c.y - 18f + i * 12f), 2.4f)
            }
        }
    }
}
