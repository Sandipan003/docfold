package com.docfold.scanner.ui.screens

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.docfold.scanner.data.Page
import com.docfold.scanner.data.PageFilter
import com.docfold.scanner.imaging.DeviceProfile
import com.docfold.scanner.imaging.ImageIO
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.LocalSnackbar
import com.docfold.scanner.ui.Screen
import com.docfold.scanner.ui.components.PageImage
import com.docfold.scanner.ui.theme.AirmailBlue
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.LocalInks
import com.docfold.scanner.ui.theme.Mono
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Panel { None, Filters }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(vm: AppViewModel, docId: String, pageId: String) {
    val docs by vm.store.docs.collectAsState()
    val doc = docs?.firstOrNull { it.id == docId }
    if (doc == null || doc.pages.isEmpty()) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val start = doc.pages.indexOfFirst { it.id == pageId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { doc.pages.size }
    val page = doc.pages[pager.currentPage.coerceIn(0, doc.pages.lastIndex)]
    val ink = LocalInks.current[doc.color]
    var panel by remember { mutableStateOf(Panel.None) }
    var confirmDelete by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf<Bitmap?>(null) }
    var readingText by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val side = with(LocalDensity.current) { (max(config.screenWidthDp, config.screenHeightDp).dp.toPx()).roundToInt() }
        .coerceAtMost(if (DeviceProfile.lowRam) 1400 else 2000)

    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbar.current) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton({ vm.back() }) { Icon(DocIcons.Back, "Back to pages") } },
                title = { Text("Page ${pager.currentPage + 1} of ${doc.pages.size}", style = MaterialTheme.typography.headlineSmall) },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.navigationBarsPadding()) {
                if (placing != null) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton({ placing = null }, Modifier.weight(1f).height(54.dp)) { Text("Cancel") }
                        Button({ signBox?.let { box -> vm.sign(doc.id, page, placing!!, box) }; placing = null }, Modifier.weight(1.3f).height(54.dp)) {
                            Icon(DocIcons.Check, null); Spacer(Modifier.width(8.dp)); Text("Place signature")
                        }
                    }
                } else Column {
                    AnimatedVisibility(panel == Panel.Filters) { FilterPanel(vm, docId, page) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToolButton(DocIcons.Crop, "Crop") { panel = Panel.None; vm.open(Screen.Crop(docId, page.id)) }
                        ToolButton(DocIcons.Contrast, "Filters", active = panel == Panel.Filters) { panel = if (panel == Panel.Filters) Panel.None else Panel.Filters }
                        ToolButton(DocIcons.RotateRight, "Rotate") { vm.rotate(docId, setOf(page.id), 90) }
                        ToolButton(DocIcons.Sign, "Sign") { panel = Panel.None; drawing = true }
                        ToolButton(DocIcons.Text, "Get text") { panel = Panel.None; readingText = true }
                        ToolButton(DocIcons.Delete, "Delete") { panel = Panel.None; confirmDelete = true }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.surfaceContainer)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = placing == null, key = { doc.pages[it].id }, beyondViewportPageCount = 0) { i ->
                val p = doc.pages[i]
                PageImage(vm.store.pageFile(docId, p.id), p, side, Modifier.fillMaxSize().padding(18.dp))
            }
            placing?.let { sig -> SignaturePlacer(vm, docId, page, sig) }
            if (doc.pages.size > 1 && placing == null) {
                Text(
                    "Swipe for other pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                )
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this page?") },
        text = { Text("Page ${pager.currentPage + 1} will be removed from “${doc.name}”.") },
        confirmButton = { TextButton({ confirmDelete = false; vm.deletePages(docId, setOf(page.id)) }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
    )
    if (drawing) SignaturePad(vm, onDismiss = { drawing = false }) { drawing = false; placing = it }
    if (readingText) TextSheet(vm, docId, page) { readingText = false }
}

/** Where the signature sits on the page, as fractions of the upright page. Written by [SignaturePlacer]. */
private var signBox: RectF? = null

@Composable
private fun SignaturePlacer(vm: AppViewModel, docId: String, page: Page, sig: Bitmap) {
    val (w0, h0) = remember(page.id, page.rev) { ImageIO.bounds(vm.store.pageFile(docId, page.id)) }
    val pageW = if (page.rotation % 180 == 0) w0 else h0
    val pageH = if (page.rotation % 180 == 0) h0 else w0
    var box by remember { mutableStateOf(IntSize.Zero) }
    var cx by remember { mutableFloatStateOf(0.62f) }
    var cy by remember { mutableFloatStateOf(0.82f) }
    var wf by remember { mutableFloatStateOf(0.36f) }
    val image = remember(sig) { sig.asImageBitmap() }
    Box(Modifier.fillMaxSize().padding(18.dp).onSizeChanged { box = it }) {
        if (box == IntSize.Zero || pageW <= 0) return@Box
        val s = min(box.width / pageW.toFloat(), box.height / pageH.toFloat())
        val iw = pageW * s; val ih = pageH * s
        val ix = (box.width - iw) / 2; val iy = (box.height - ih) / 2
        val sw = wf * iw; val sh = sw * sig.height / sig.width
        val left = ix + cx * iw - sw / 2; val top = iy + cy * ih - sh / 2
        signBox = RectF((left - ix) / iw, (top - iy) / ih, (left - ix + sw) / iw, (top - iy + sh) / ih)
        val density = LocalDensity.current
        Box(
            Modifier.fillMaxSize().pointerInput(iw, ih) {
                detectTransformGestures { _, pan, zoom, _ ->
                    wf = (wf * zoom).coerceIn(0.12f, 0.9f)
                    cx = (cx + pan.x / iw).coerceIn(0.05f, 0.95f)
                    cy = (cy + pan.y / ih).coerceIn(0.05f, 0.95f)
                }
            },
        ) {
            with(density) {
                Image(
                    image, "Your signature",
                    Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }.size(sw.toDp(), sh.toDp())
                        .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)),
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.small, modifier = Modifier.align(Alignment.TopCenter)) {
            Text("Drag to move · pinch to resize", color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun FilterPanel(vm: AppViewModel, docId: String, page: Page) {
    // Previews start from the original capture (with the page's crop), exactly as the real filter will.
    val hasSrc = vm.store.srcFile(docId, page.id).exists()
    val src = if (hasSrc) vm.store.srcFile(docId, page.id) else vm.store.pageFile(docId, page.id)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PageFilter.entries.forEach { f ->
            val active = page.filter == f
            Column(
                Modifier.weight(1f).clickable(role = Role.RadioButton) { if (!active) vm.editPage(docId, page.id, f, page.crop) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(86.dp)
                        .border(if (active) 2.5.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .padding(4.dp),
                ) {
                    PageImage(src, page, side = 220, filter = f, crop = if (hasSrc) page.crop else null)
                }
                Spacer(Modifier.height(4.dp))
                Text(f.label, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignaturePad(vm: AppViewModel, onDismiss: () -> Unit, onDone: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var blue by remember { mutableStateOf(false) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    val saved = remember { vm.savedSignature() }
    val color = if (blue) AirmailBlue else Color(0xFF14161A)
    val widthPx = with(LocalDensity.current) { 3.dp.toPx() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetGesturesEnabled = false) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sign here", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                IconButton(onDismiss) { Icon(DocIcons.Close, "Close") }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(210.dp).background(Color.White, MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .onSizeChanged { padSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { current = listOf(it) },
                            onDrag = { ch, _ -> ch.consume(); current = current + ch.position },
                            onDragEnd = { if (current.size > 1) strokes.add(current); current = emptyList() },
                        )
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(Color(0xFFDADAD3), Offset(24f, size.height * 0.72f), Offset(size.width - 24f, size.height * 0.72f), 2f)
                    (strokes + listOf(current)).forEach { pts ->
                        if (pts.size < 2) return@forEach
                        val p = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                        drawPath(p, color, style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                if (strokes.isEmpty() && current.isEmpty()) {
                    Text("Draw your signature with your finger", color = Color(0xFF8E9199), modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!blue, { blue = false }, { Text("Black ink") })
                FilterChip(blue, { blue = true }, { Text("Blue ink") })
                Spacer(Modifier.weight(1f))
                TextButton({ strokes.clear() }, enabled = strokes.isNotEmpty()) { Text("Clear") }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                {
                    val bmp = renderSignature(strokes, padSize, color, widthPx)
                    if (bmp != null) { vm.saveSignature(bmp); onDone(bmp) }
                },
                Modifier.fillMaxWidth().height(56.dp), enabled = strokes.isNotEmpty(),
            ) { Text("Use this signature", style = MaterialTheme.typography.titleMedium) }
            if (saved != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton({ onDone(saved) }, Modifier.fillMaxWidth().height(52.dp)) { Text("Use my saved signature") }
            }
        }
    }
}

/** Renders the strokes, cropped to their ink, at twice screen resolution on a transparent bitmap. */
private fun renderSignature(strokes: List<List<Offset>>, pad: IntSize, color: Color, width: Float): Bitmap? {
    val pts = strokes.flatten()
    if (pts.isEmpty() || pad == IntSize.Zero) return null
    val m = width * 2
    val l = pts.minOf { it.x } - m; val t = pts.minOf { it.y } - m
    val r = pts.maxOf { it.x } + m; val b = pts.maxOf { it.y } + m
    val k = 2f
    val bmp = Bitmap.createBitmap(((r - l) * k).roundToInt().coerceAtLeast(1), ((b - t) * k).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE; strokeWidth = width * k
        strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
        this.color = android.graphics.Color.argb((color.alpha * 255).roundToInt(), (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt())
    }
    strokes.forEach { s ->
        val p = android.graphics.Path()
        s.forEachIndexed { i, o -> val x = (o.x - l) * k; val y = (o.y - t) * k; if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
        canvas.drawPath(p, paint)
    }
    return bmp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSheet(vm: AppViewModel, docId: String, page: Page, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(page.id, page.rev) {
        val r = runCatching { vm.readText(docId, page) }.getOrNull()
        if (r == null) failed = true else text = r
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Text on this page", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            val t = text
            when {
                failed -> Text("Text reading needs a one-time download from Google Play services. Connect to the internet and try again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                t == null -> { Text("Reading…", style = Mono.medium); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                t.isBlank() -> Text("No text found on this page.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                        SelectionContainer {
                            Text(t, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(14.dp).fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button({
                            val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                            cm?.setPrimaryClip(ClipData.newPlainText("Docfold text", t))
                            vm.notify("Text copied")
                        }, Modifier.weight(1f).height(52.dp)) { Icon(DocIcons.Copy, null); Spacer(Modifier.width(8.dp)); Text("Copy") }
                        OutlinedButton({
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t), "Share text"))
                        }, Modifier.weight(1f).height(52.dp)) { Icon(DocIcons.Share, null); Spacer(Modifier.width(8.dp)); Text("Share") }
                    }
                }
            }
        }
    }
}
