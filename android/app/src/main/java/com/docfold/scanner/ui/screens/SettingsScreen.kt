package com.docfold.scanner.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.docfold.scanner.BuildConfig
import com.docfold.scanner.Links
import com.docfold.scanner.MainActivity
import com.docfold.scanner.ads.Ads
import com.docfold.scanner.ui.AppViewModel
import com.docfold.scanner.ui.components.AirmailBand
import com.docfold.scanner.ui.theme.DocIcons
import com.docfold.scanner.ui.theme.Mono
import com.docfold.scanner.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, activity: MainActivity) {
    val context = LocalContext.current
    val docs by vm.store.docs.collectAsState()
    fun open(url: String) = try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: ActivityNotFoundException) { vm.notify("No browser found.") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton({ vm.back() }) { Icon(DocIcons.Back, "Back") } },
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Section("Appearance")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(vm.themeMode == m, { vm.setTheme(m) }, SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)) {
                        Text(when (m) { ThemeMode.System -> "Phone"; ThemeMode.Light -> "Light"; ThemeMode.Dark -> "Dark" })
                    }
                }
            }

            Section("Your documents")
            Item(DocIcons.Shield, "Stored only on this phone", "${plural(docs.orEmpty().size, "document")} · ${formatSize(vm.storageBytes())}. No account, no cloud. Uninstalling the app deletes them.")
            Item(DocIcons.Info, "Replay the introduction", null) { vm.replayIntro() }

            Section("Privacy")
            if (Ads.privacyOptionsRequired) Item(DocIcons.Settings, "Ad privacy choices", "Change what you agreed to for ads") { Ads.showPrivacyOptions(activity) }
            Item(DocIcons.Lock, "Privacy policy", "What Docfold does and doesn't collect") { open(Links.PRIVACY_POLICY) }

            Section("Docfold")
            Item(DocIcons.Star, "Rate Docfold", "It helps other people find a free scanner") { open("market://details?id=${context.packageName}") }
            Item(DocIcons.Share, "Tell a friend", null) {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                    "Docfold — scan pages to PDF, free and offline: https://play.google.com/store/apps/details?id=${context.packageName}"), "Share Docfold"))
            }
            Item(DocIcons.Info, "Licences", "Archivo and Martian Mono fonts (SIL Open Font License). Scanning and text reading by Google ML Kit.")

            Spacer(Modifier.padding(top = 24.dp))
            AirmailBand()
            Text("Docfold ${BuildConfig.VERSION_NAME}", style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp))
}

@Composable
private fun Item(icon: ImageVector, title: String, detail: String?, onClick: (() -> Unit)? = null) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (onClick != null) Icon(DocIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(start = 62.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
