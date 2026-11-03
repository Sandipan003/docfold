package com.docfold.scanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.docfold.scanner.data.Page
import com.docfold.scanner.data.PageFilter
import com.docfold.scanner.imaging.ImageIO
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.components.PageImage
import com.docfold.scanner.ui.theme.AirmailRed
import com.docfold.scanner.ui.theme.DocIcons
import kotlin.math.hypot
import kotlin.math.min

private val FULL = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
private val INSET = listOf(0.06f, 0.06f, 0.94f, 0.06f, 0.94f, 0.94f, 0.06f, 0.94f)

/**
 * Four-corner crop over the page's original capture. Corners are dragged on the page as it is shown (rotated);
 * they are stored in the capture's own orientation so the perspective fix and the rotation never fight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(vm: AppViewModel, docId: String, pageId: String, isNew: Boolean) {
    val docs by vm.store.docs.collectAsState()
    val page = docs?.firstOrNull { it.id == docId }?.pages?.firstOrNull { it.id == pageId }
    if (page == null) { LaunchedEffect(Unit) { vm.back() }; return }
    val src = remember(page.rev) { vm.store.srcFile(docId, pageId).takeIf { it.exists() } ?: vm.store.pageFile(docId, pageId) }
    val (sw, sh) = remember(src, page.rev) { ImageIO.bounds(src) }
    val k = page.rotation / 90
    // Displayed corners, clockwise from the top-left as the user sees them.
    val pts = remember(page.rev) {
        val q = page.crop ?: if (isNew) INSET else FULL
        val shown = MutableList(4) { Offset.Zero }
        for (i in 0 until 4) shown[(i + k) % 4] = toShown(Offset(q[i * 2], q[i * 2 + 1]), page.rotation)
        mutableStateListOf<Offset>().apply { addAll(shown) }
    }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var active by remember { mutableIntStateOf(-1) }

    fun apply() {
        val q = FloatArray(8)
        for (i in 0 until 4) {
            val s = toSource(pts[(i + k) % 4], page.rotation)
            q[i * 2] = s.x.coerceIn(0f, 1f); q[i * 2 + 1] = s.y.coerceIn(0f, 1f)
        }
        val full = q.toList().zip(FULL).all { (a, b) -> kotlin.math.abs(a - b) < 0.004f }
        val filter = if (isNew && page.filter == PageFilter.Original) PageFilter.Magic else page.filter
        vm.editPage(docId, pageId, filter, if (full) null else q.toList())
        vm.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton({ vm.back() }) { Icon(DocIcons.Close, "Cancel crop") } },
                title = { Text("Crop", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.navigationBarsPadding()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Drag the red corners onto the corners of the paper.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton({
                            val full = MutableList(4) { Offset.Zero }
                            for (i in 0 until 4) full[(i + k) % 4] = toShown(Offset(FULL[i * 2], FULL[i * 2 + 1]), page.rotation)
                            pts.clear(); pts.addAll(full)
                        }, Modifier.weight(1f).height(54.dp)) {
                            Icon(DocIcons.Reset, null); Spacer(Modifier.width(8.dp)); Text("Reset")
                        }
                        Button(::apply, Modifier.weight(1.2f).height(54.dp)) {
                            Icon(DocIcons.Check, null); Spacer(Modifier.width(8.dp)); Text("Apply crop")
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(Color(0xFF1A1D24)).padding(28.dp).onSizeChanged { box = it }) {
            if (box == IntSize.Zero || sw <= 0) return@Box
            val turned = page.rotation % 180 != 0
            val dw = if (turned) sh else sw
            val dh = if (turned) sw else sh
            val s = min(box.width / dw.toFloat(), box.height / dh.toFloat())
            val iw = dw * s; val ih = dh * s
            val ox = (box.width - iw) / 2; val oy = (box.height - ih) / 2
            PageImage(src, Page(pageId, rotation = page.rotation, rev = page.rev), side = 1600, modifier = Modifier.fillMaxSize())
            Canvas(
                Modifier.fillMaxSize().pointerInput(iw, ih) {
                    val grab = 40.dp.toPx()
                    detectDragGestures(
                        onDragStart = { p ->
                            active = pts.indices.minByOrNull { i -> hypot(ox + pts[i].x * iw - p.x, oy + pts[i].y * ih - p.y) }
                                ?.takeIf { i -> hypot(ox + pts[i].x * iw - p.x, oy + pts[i].y * ih - p.y) < grab * 1.6f } ?: -1
                        },
                        onDrag = { ch, d ->
                            if (active >= 0) {
                                ch.consume()
                                val c = pts[active]
                                pts[active] = Offset((c.x + d.x / iw).coerceIn(0f, 1f), (c.y + d.y / ih).coerceIn(0f, 1f))
                            }
                        },
                        onDragEnd = { active = -1 },
                    )
                },
            ) {
                val screen = pts.map { Offset(ox + it.x * iw, oy + it.y * ih) }
                // Dim everything outside the quad.
                val outside = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    moveTo(screen[0].x, screen[0].y); screen.drop(1).forEach { lineTo(it.x, it.y) }; close()
                }
                drawPath(outside, Color.Black.copy(alpha = 0.5f))
                val quad = Path().apply { moveTo(screen[0].x, screen[0].y); screen.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                drawPath(quad, AirmailRed, style = Stroke(2.5.dp.toPx()))
                screen.forEachIndexed { i, p ->
                    drawCircle(Color.White, (if (i == active) 16 else 13).dp.toPx(), p)
                    drawCircle(AirmailRed, (if (i == active) 12 else 9).dp.toPx(), p)
                }
            }
            // Corners sit near the screen edges, where Android's back swipe lives: claim those spots for dragging.
            val density = LocalDensity.current
            pts.forEach { p ->
                Box(
                    Modifier.offset { IntOffset((ox + p.x * iw).roundToInt() - 32.dp.roundToPx(), (oy + p.y * ih).roundToInt() - 32.dp.roundToPx()) }
                        .size(64.dp).systemGestureExclusion(),
                )
            }
        }
    }
}

/** Capture fraction → shown fraction for a clockwise rotation. */
private fun toShown(p: Offset, rotation: Int) = when (rotation) {
    90 -> Offset(1 - p.y, p.x)
    180 -> Offset(1 - p.x, 1 - p.y)
    270 -> Offset(p.y, 1 - p.x)
    else -> p
}

private fun toSource(p: Offset, rotation: Int) = when (rotation) {
    90 -> Offset(p.y, 1 - p.x)
    180 -> Offset(1 - p.x, 1 - p.y)
    270 -> Offset(1 - p.y, p.x)
    else -> p
}
