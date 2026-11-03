package com.docfold.scanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docfold.scanner.data.Page
import com.docfold.scanner.data.PageFilter
import com.docfold.scanner.ui.theme.AirmailBlue
import com.docfold.scanner.ui.theme.AirmailRed
import com.docfold.scanner.ui.theme.ArchivoCondensed
import com.docfold.scanner.ui.theme.LocalDark
import com.docfold.scanner.ui.theme.MartianMono
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** A rectangle with postage-stamp perforations bitten out of every edge. */
class PerforatedShape(private val hole: Dp = 3.dp, private val pitch: Dp = 9.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { hole.toPx() }
        val p = with(density) { pitch.toPx() }
        val body = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val holes = Path()
        fun edge(length: Float, place: (Float) -> Offset) {
            val n = max(1, floor(length / p).toInt())
            val step = length / n
            for (i in 0..n) holes.addOval(Rect(place(i * step), r))
        }
        edge(size.width) { Offset(it, 0f) }
        edge(size.width) { Offset(it, size.height) }
        edge(size.height) { Offset(0f, it) }
        edge(size.height) { Offset(size.width, it) }
        return Outline.Generic(Path().apply { op(body, holes, PathOperation.Difference) })
    }
}

/**
 * Stamp paper drawn from a cached perforated path, with a soft offset shadow drawn from the same path.
 * Never use a perforated outline with elevation: Android tessellates shadows for concave outlines on the
 * render thread, which takes seconds per frame and freezes the app.
 */
fun Modifier.perforatedPaper(paper: Color, edge: Color, hole: Dp = 3.dp, pitch: Dp = 9.dp, shadow: Boolean = true): Modifier =
    drawWithCache {
        val path = (PerforatedShape(hole, pitch).createOutline(size, layoutDirection, this) as Outline.Generic).path
        val lift = 1.5.dp.toPx()
        onDrawBehind {
            if (shadow) {
                translate(0f, lift * 1.6f) { drawPath(path, Color.Black.copy(alpha = 0.05f)) }
                translate(0f, lift) { drawPath(path, Color.Black.copy(alpha = 0.08f)) }
            }
            drawPath(path, paper)
            if (edge != Color.Transparent) drawPath(path, edge, style = Stroke(1f))
        }
    }

/**
 * A page drawn as a postage stamp: perforated paper, the page image inside an ink frame, and its number set
 * like a stamp's value in the bottom margin. Edits show as marks beside the number, not as chips.
 */
@Composable
fun PageStamp(
    file: File,
    page: Page,
    number: Int,
    ink: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    // Stamps stay paper-light in the dark theme too, a step above the ground so they never sink into it.
    val paper = if (LocalDark.current) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier
            .aspectRatio(0.74f)
            .perforatedPaper(if (selected) ink else paper, MaterialTheme.colorScheme.outlineVariant)
            .padding(start = 7.dp, top = 7.dp, end = 7.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .background(if (selected) paper else ink)
                    .padding(3.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                PageImage(file, page, side = 360, contentScale = ContentScale.Fit)
            }
            Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = if (selected) Color.White else ink,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = ArchivoCondensed, fontSize = 20.sp),
                )
                Spacer(Modifier.weight(1f))
                EditMarks(page, if (selected) Color.White else ink)
            }
        }
    }
}

/** Small engraved marks: a corner for crop, a dot for a filter, a tick for rotation. */
@Composable
private fun EditMarks(page: Page, color: Color) {
    Canvas(Modifier.width(30.dp).height(12.dp)) {
        var x = size.width
        val h = size.height
        val w = 1.4.dp.toPx()
        if (page.rotation != 0) {
            x -= h
            drawArc(color, -90f, 270f, false, topLeft = Offset(x, 1f), size = Size(h - 2f, h - 2f), style = Stroke(w))
            x -= 4.dp.toPx()
        }
        if (page.filter != PageFilter.Original) {
            x -= h * 0.6f
            drawCircle(color, h * 0.28f, Offset(x + h * 0.3f, h / 2))
            x -= 4.dp.toPx()
        }
        if (page.crop != null) {
            x -= h
            drawLine(color, Offset(x + 2f, 0f), Offset(x + 2f, h - 2f), w)
            drawLine(color, Offset(x + 2f, h - 2f), Offset(x + h, h - 2f), w)
        }
    }
}

/** Draws a page image upright (its rotation applied), fitted or cropped into its box. */
@Composable
fun PageImage(
    file: File, page: Page, side: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit,
    filter: PageFilter? = null, crop: List<Float>? = null,
) {
    val img by Images.load(file, "${file.path}#${page.rev}", side, filter, crop)
    val bmp = img
    if (bmp == null) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
        return
    }
    val crop = contentScale == ContentScale.Crop
    Canvas(modifier.fillMaxSize().then(if (crop) Modifier.clipToBounds() else Modifier)) {
        val turned = page.rotation % 180 != 0
        val dw = if (turned) bmp.height else bmp.width
        val dh = if (turned) bmp.width else bmp.height
        val s = if (crop) max(size.width / dw, size.height / dh) else min(size.width / dw, size.height / dh)
        val w = (bmp.width * s).roundToInt()
        val h = (bmp.height * s).roundToInt()
        translate(size.width / 2, size.height / 2) {
            rotate(page.rotation.toFloat(), pivot = Offset.Zero) {
                drawImage(bmp, dstOffset = IntOffset(-w / 2, -h / 2), dstSize = IntSize(w, h), filterQuality = FilterQuality.Medium)
            }
        }
    }
}

/**
 * The airmail edge around a whole envelope card: alternating red and blue slanted bars just inside the card's
 * shape, drawn over its content from a cached path.
 */
fun Modifier.airmailEdge(shape: Shape, band: Dp = 3.5.dp): Modifier = composed {
    val blue = if (LocalDark.current) Color(0xFF5B7BD6) else AirmailBlue
    drawWithCache {
        val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val b = band.toPx()
        val bar = 10.dp.toPx()
        val gap = 4.dp.toPx()
        val bars = Path()
        val reds = Path()
        var i = 0
        fun along(length: Float, place: Path.(Float) -> Unit) {
            var d = -b
            while (d < length) {
                val target = if (i % 2 == 0) reds else bars
                target.place(d)
                d += bar + gap
                i++
            }
        }
        // top and bottom run left→right, sides run top→bottom; each bar is slanted by the band width.
        along(size.width) { x -> moveTo(x, b); lineTo(x + b, 0f); lineTo(x + b + bar, 0f); lineTo(x + bar, b); close() }
        along(size.width) { x -> moveTo(x, size.height); lineTo(x + b, size.height - b); lineTo(x + b + bar, size.height - b); lineTo(x + bar, size.height); close() }
        along(size.height) { y -> moveTo(0f, y + b); lineTo(b, y); lineTo(b, y + bar); lineTo(0f, y + bar + b); close() }
        along(size.height) { y -> moveTo(size.width - b, y + b); lineTo(size.width, y); lineTo(size.width, y + bar); lineTo(size.width - b, y + bar + b); close() }
        onDrawWithContent {
            drawContent()
            clipPath(clip) {
                drawPath(reds, AirmailRed)
                drawPath(bars, blue)
            }
        }
    }
}

/** The airmail border: red and blue chevrons on the envelope's paper. */
@Composable
fun AirmailBand(modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val paper = MaterialTheme.colorScheme.background
    val blue = if (LocalDark.current) Color(0xFF5B7BD6) else AirmailBlue
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        val stripe = h * 2.2f
        val gap = h * 0.9f
        var x = -h
        var i = 0
        drawRect(paper)
        while (x < size.width + h) {
            val p = Path().apply {
                moveTo(x, h); lineTo(x + h, 0f); lineTo(x + h + stripe, 0f); lineTo(x + stripe, h); close()
            }
            drawPath(p, if (i % 2 == 0) AirmailRed else blue)
            x += stripe + gap
            i++
        }
    }
}

/**
 * A rubber postmark: two rings, text set around the top arc, a date across the middle. Drawn in ink with a
 * little transparency so it reads as stamped on, never printed.
 */
@Composable
fun Postmark(top: String, middle: String, bottom: String, color: Color, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.size(size)) { drawPostmark(measurer, top, middle, bottom, color) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPostmark(
    measurer: TextMeasurer, top: String, middle: String, bottom: String, color: Color,
) {
    val c = color.copy(alpha = 0.78f)
    val r = size.minDimension / 2
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(c, r - 1.dp.toPx(), center, style = Stroke(1.6.dp.toPx()))
    drawCircle(c, r * 0.62f, center, style = Stroke(1.dp.toPx()))
    val small = androidx.compose.ui.text.TextStyle(fontFamily = MartianMono, fontSize = (r * 0.2f / density).sp, color = c)
    fun arc(text: String, startDeg: Float, sweepDeg: Float, radius: Float) {
        val chars = text.toCharArray()
        if (chars.isEmpty()) return
        val step = sweepDeg / chars.size
        chars.forEachIndexed { i, ch ->
            val a = (startDeg + step * (i + 0.5f)) * PI.toFloat() / 180f
            val pos = Offset(center.x + radius * cos(a), center.y + radius * sin(a))
            val layout = measurer.measure(ch.toString(), small)
            rotate(startDeg + step * (i + 0.5f) + 90f, pos) {
                drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
            }
        }
    }
    arc(top, -160f, 140f, r * 0.8f)
    val bottomChars = bottom.reversed()
    // Bottom text runs left-to-right along the lower arc.
    val stepB = 120f / max(1, bottom.length)
    bottomChars.forEachIndexed { i, ch ->
        val deg = 30f + stepB * (i + 0.5f)
        val a = deg * PI.toFloat() / 180f
        val pos = Offset(center.x + r * 0.8f * cos(a), center.y + r * 0.8f * sin(a))
        val layout = measurer.measure(ch.toString(), small)
        rotate(deg - 90f, pos) {
            drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
        }
    }
    val mid = measurer.measure(
        middle,
        androidx.compose.ui.text.TextStyle(fontFamily = ArchivoCondensed, fontSize = (r * 0.3f / density).sp, color = c, textAlign = TextAlign.Center),
    )
    drawText(mid, topLeft = Offset(center.x - mid.size.width / 2f, center.y - mid.size.height / 2f))
}

/** The one celebratory moment: a postmark lands on the screen with a short overshoot, then lifts away. */
@Composable
fun BoxScope.PostmarkStamp(top: String, middle: String, bottom: String, onDone: () -> Unit) {
    val scale = remember { Animatable(1.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(90))
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        kotlinx.coroutines.delay(1100)
        alpha.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        onDone()
    }
    Postmark(
        top, middle, bottom, AirmailRed,
        Modifier.align(Alignment.Center)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
            .rotate(-14f),
        size = 190.dp,
    )
}
