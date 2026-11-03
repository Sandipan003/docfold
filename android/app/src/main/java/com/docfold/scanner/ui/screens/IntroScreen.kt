package com.docfold.scanner.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.docfold.scanner.ui.theme.LocalDark
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.docfold.scanner.ui.components.AirmailBand
import com.docfold.scanner.ui.components.EnvelopeScene
import com.docfold.scanner.ui.components.PaperPileScene
import com.docfold.scanner.ui.components.perforatedPaper
import com.docfold.scanner.ui.components.ScanScene
import com.docfold.scanner.ui.components.StampSheetScene
import com.docfold.scanner.ui.theme.AirmailRed
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.LocalInks
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private class Chapter(val field: Color, val title: String, val body: String)

private val chapters = listOf(
    Chapter(Color(0xFF2F4FD8), "Paper,\nmeet phone.", "Forms, IDs, receipts, notes — Docfold turns them into clean PDFs you can send in a minute."),
    Chapter(Color(0xFFF2B53A), "Point.\nIt scans itself.", "Edges, tilt and shadows are fixed for you. Scan as many pages as you like in one go."),
    Chapter(Color(0xFF12825A), "Arrange\nlike stamps.", "Hold a page and drag it into place. Crop, rotate, or sign it with your finger."),
    Chapter(AirmailRed, "Sealed on\nyour phone.", "No account. No watermark. No limits. Nothing leaves your phone until you send it."),
)

/** The first-run story: four chapters, each a saturated field with an engraved scene and one plain sentence. */
@Composable
fun IntroScreen(onDone: (scanNow: Boolean) -> Unit) {
    val pager = rememberPagerState { chapters.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == chapters.lastIndex
    val inks = LocalInks.current.all
    // White status-bar icons on the saturated fields (dark on the light saffron one); restored on leaving.
    val view = LocalView.current
    val dark = LocalDark.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = pager.currentPage == 1
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? android.app.Activity)?.window ?: return@onDispose
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val fieldHeight = maxHeight * 0.58f
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                val c = chapters[i]
                val shift = pagerOffset(pager, i)
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(fieldHeight).background(c.field)) {
                        // The scene drifts slower than the page: a little depth for free.
                        Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .graphicsLayer { translationX = shift * size.width * 0.35f; alpha = 1f - shift.absoluteValue * 0.6f }) {
                            when (i) {
                                0 -> PaperPileScene(Modifier.fillMaxSize())
                                1 -> ScanScene(Modifier.fillMaxSize())
                                2 -> StampSheetScene(inks, Modifier.fillMaxSize())
                                else -> EnvelopeScene(Modifier.fillMaxSize())
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = 24.dp).padding(top = 26.dp).graphicsLayer { translationX = shift * size.width * 0.15f }) {
                        Text(c.title, style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(14.dp))
                        Text(c.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.statusBarsPadding()) {
                AirmailBand()
                if (!last) TextButton({ onDone(false) }, Modifier.align(Alignment.End).padding(end = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = if (pager.currentPage == 1) Color(0xFF14161A) else Color.White)) {
                    Text("Skip", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                chapters.forEachIndexed { i, c ->
                    val w by animateDpAsState(if (i == pager.currentPage) 44.dp else 22.dp, label = "dot")
                    val col by animateColorAsState(if (i == pager.currentPage) c.field else MaterialTheme.colorScheme.outlineVariant, label = "dotc")
                    Box(Modifier.width(w).height(26.dp).perforatedPaper(col, Color.Transparent, 2.2.dp, 7.5.dp, shadow = false))
                }
            }
            Button(
                { if (last) onDone(true) else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.fillMaxWidth().height(60.dp),
            ) {
                if (last) { Icon(DocIcons.Scan, null, Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)) }
                Text(if (last) "Scan my first page" else "Next", style = MaterialTheme.typography.titleMedium)
            }
            if (last) TextButton({ onDone(false) }, Modifier.fillMaxWidth().height(48.dp)) { Text("I'll look around first") }
            else Spacer(Modifier.height(48.dp))
        }
    }
}

private fun pagerOffset(state: PagerState, page: Int): Float =
    ((state.currentPage - page) + state.currentPageOffsetFraction)
