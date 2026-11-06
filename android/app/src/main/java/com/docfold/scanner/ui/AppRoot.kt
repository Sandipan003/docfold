package com.docfold.scanner.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.docfold.scanner.MainActivity
import com.docfold.scanner.ads.Ads
import com.docfold.scanner.scan.Scanner
import com.docfold.scanner.ui.screens.AccessSheet
import com.docfold.scanner.ui.screens.CropScreen
import com.docfold.scanner.ui.screens.DocumentScreen
import com.docfold.scanner.ui.screens.HomeScreen
import com.docfold.scanner.ui.screens.IntroScreen
import com.docfold.scanner.ui.screens.PageScreen
import com.docfold.scanner.ui.screens.SettingsScreen
import com.docfold.scanner.ui.theme.DocfoldTheme
import com.docfold.scanner.ui.theme.Mono
import com.google.android.play.core.review.ReviewManagerFactory

/** Ways to add pages, shared by every screen. A null docId means "into a new document". */
class AddActions(
    val scan: (String?) -> Unit,
    val photos: (String?) -> Unit,
    val pdf: (String?) -> Unit,
    val camera: (String?) -> Unit,
)

val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun AppRoot(vm: AppViewModel, activity: MainActivity) {
    DocfoldTheme(vm.themeMode) {
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            vm.noticeFlow.collect { n ->
                val r = snackbar.showSnackbar(n.text, n.actionLabel, withDismissAction = false, duration = if (n.action != null) SnackbarDuration.Long else SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) n.action?.invoke()
            }
        }

        val access = rememberAccess(vm, activity)
        var target by rememberSaveable { mutableStateOf<String?>(null) }
        var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok) cameraUri?.let { vm.addImages(target, listOf(it), cropNew = true) }
        }
        val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
            vm.addImages(target, Scanner.pages(r.resultCode, r.data))
        }
        // No cap of our own: the system photo picker's limit applies.
        val photosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { vm.addImages(target, it) }
        val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.addPdf(target, it) } }

        val actions = remember {
            val camera: (String?) -> Unit = { id ->
                target = id
                val uri = vm.cameraFile()
                cameraUri = uri
                runCatching { cameraLauncher.launch(uri) }.onFailure { vm.notify("No camera app found on this phone.") }
            }
            AddActions(
                scan = { id ->
                    target = id
                    access.camera {
                        Scanner.start(activity, scanLauncher) {
                            // Old or low-memory phones: Google's scanner isn't available, so use the camera + our own crop.
                            camera(id)
                        }
                    }
                },
                photos = { id ->
                    target = id
                    photosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                pdf = { id -> target = id; pdfLauncher.launch(arrayOf("application/pdf")) },
                camera = { id -> access.camera { camera(id) } },
            )
        }

        val pendingScan by activity.pendingScan
        LaunchedEffect(pendingScan) {
            if (pendingScan) { activity.pendingScan.value = false; vm.finishIntroIfNeeded(); actions.scan(null) }
        }

        BackHandler(enabled = vm.stack.size > 1) { leaving(vm, activity) }

        CompositionLocalProvider(LocalSnackbar provides snackbar, LocalAccess provides access) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                var lastDepth by remember { mutableIntStateOf(vm.stack.size) }
                val depth = vm.stack.size
                AnimatedContent(
                    targetState = vm.screen,
                    transitionSpec = {
                        val forward = depth >= lastDepth
                        val d = tween<Float>(260)
                        (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(d))
                            .togetherWith(slideOutHorizontally(tween(300)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(180)))
                    },
                    label = "screens",
                ) { s ->
                    when (s) {
                        Screen.Intro -> IntroScreen(onDone = { vm.finishIntro(); if (it) actions.scan(null) })
                        Screen.Home -> HomeScreen(vm, actions)
                        is Screen.Document -> DocumentScreen(vm, s.docId, actions, activity, onBack = { leaving(vm, activity) })
                        is Screen.PageView -> PageScreen(vm, s.docId, s.pageId)
                        is Screen.Crop -> CropScreen(vm, s.docId, s.pageId, s.isNew)
                        Screen.Settings -> SettingsScreen(vm, activity)
                    }
                }
                LaunchedEffect(depth) { lastDepth = depth }
                vm.busy?.let { BusyOverlay(it) }
                AccessSheet(access)
            }
        }
    }
}

/** Leaving a document is the app's natural break: the place for an in-app review ask or an interstitial. */
private fun leaving(vm: AppViewModel, activity: MainActivity) {
    val wasDoc = vm.screen is Screen.Document
    vm.back()
    if (!wasDoc) return
    if (vm.exportsDone >= 3 && !vm.reviewAsked) {
        vm.reviewAsked = true
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnSuccessListener { manager.launchReviewFlow(activity, it) }
    } else {
        Ads.maybeShowAtBreak(activity)
    }
}

@Composable
private fun BusyOverlay(b: Busy) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.padding(32.dp).widthIn(max = 360.dp)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(b.label, style = MaterialTheme.typography.headlineSmall)
                if (b.total > 0) {
                    LinearProgressIndicator(progress = { b.done / b.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("${b.done} of ${b.total}", style = Mono.medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
