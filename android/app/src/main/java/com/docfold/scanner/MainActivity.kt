package com.docfold.scanner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.docfold.scanner.ads.Ads
import com.docfold.scanner.ui.AppRoot
import com.docfold.scanner.ui.AppViewModel

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Set by the launcher shortcut; AppRoot opens the scanner once it is on screen. */
    val pendingScan = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppRoot(vm, this) }
        if (savedInstanceState == null) handle(intent)
        Ads.gatherConsent(this)
        pushShortcut()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Images or a PDF shared from another app become a new document; the shortcut starts a scan. */
    private fun handle(intent: Intent?) {
        when (intent?.action) {
            ACTION_SCAN -> pendingScan.value = true
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { route(listOf(it), intent.type) }
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { route(it, intent.type) }
        }
    }

    private fun route(uris: List<Uri>, type: String?) {
        if (uris.isEmpty()) return
        vm.finishIntroIfNeeded()
        val pdf = type == "application/pdf" || contentResolver.getType(uris.first()) == "application/pdf"
        if (pdf) vm.addPdf(null, uris.first()) else vm.addImages(null, uris)
    }

    private fun pushShortcut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        runCatching {
            ShortcutManagerCompat.pushDynamicShortcut(
                this,
                ShortcutInfoCompat.Builder(this, "scan")
                    .setShortLabel(getString(R.string.shortcut_scan_short))
                    .setLongLabel(getString(R.string.shortcut_scan_long))
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_scan))
                    .setIntent(Intent(this, MainActivity::class.java).setAction(ACTION_SCAN))
                    .build(),
            )
        }
    }

    companion object {
        const val ACTION_SCAN = "com.docfold.scanner.action.SCAN"
    }
}
