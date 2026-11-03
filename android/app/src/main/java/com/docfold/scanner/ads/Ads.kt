package com.docfold.scanner.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.docfold.scanner.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * AdMob, with consent first (Google's UMP form for EEA/UK and US-state privacy laws).
 *
 * Placement rules, chosen so ads never get in the way of the work:
 *  - an adaptive banner at the bottom of the browsing screens only, never over the scanner or editors;
 *  - one interstitial at a natural break — leaving a document after exporting it — at most every
 *    [INTERSTITIAL_GAP_MS], never right after launch and never twice in a row.
 */
object Ads {
    private const val INTERSTITIAL_GAP_MS = 150_000L
    private val started = AtomicBoolean(false)
    private lateinit var consent: ConsentInformation

    /** True once the SDK may serve ads (consent gathered or not required). */
    var ready by mutableStateOf(false)
        private set

    /** Whether Settings must show a "Privacy options" entry (required by Google when consent applies). */
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShown = SystemClock.elapsedRealtime() // counts app start as "just shown"
    private var breakPending = false

    fun gatherConsent(activity: Activity) {
        consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                refreshPrivacyFlag()
                if (consent.canRequestAds()) start(activity)
            }
        }, {
            // Offline or blocked: fall back to whatever consent the user gave last time.
            if (consent.canRequestAds()) start(activity)
        })
        // Returning users who already answered can start loading right away.
        if (consent.canRequestAds()) start(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { refreshPrivacyFlag() }
    }

    private fun refreshPrivacyFlag() {
        privacyOptionsRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        val app = activity.applicationContext
        // Initialising on a background thread keeps the first frame fast on low-end phones.
        thread(name = "ads-init") {
            MobileAds.initialize(app) {
                activity.runOnUiThread {
                    ready = true
                    preloadInterstitial(app)
                }
            }
        }
    }

    private fun preloadInterstitial(context: Context) {
        if (!ready || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context, BuildConfig.AD_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
            override fun onAdFailedToLoad(error: LoadAdError) { loadingInterstitial = false }
        })
    }

    /** Call after a successful export or share. */
    fun markNaturalBreak() { breakPending = true }

    /** Call when the user leaves a document. Shows an interstitial only if a break is pending and the gap has passed. */
    fun maybeShowAtBreak(activity: Activity) {
        if (!breakPending) return
        val ad = interstitial
        if (!ready || ad == null) { preloadInterstitial(activity.applicationContext); return }
        if (SystemClock.elapsedRealtime() - lastShown < INTERSTITIAL_GAP_MS) return
        breakPending = false
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadInterstitial(activity.applicationContext) }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { preloadInterstitial(activity.applicationContext) }
        }
        lastShown = SystemClock.elapsedRealtime()
        ad.show(activity)
    }
}

/**
 * Anchored adaptive banner. Takes no space until an ad has actually loaded, so offline users and
 * users without consent never see an empty grey strip.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!Ads.ready) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val size = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        var loaded by remember { mutableStateOf(false) }
        val view = remember(size) {
            AdView(context).apply {
                setAdSize(size)
                adUnitId = BuildConfig.AD_BANNER_ID
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                adListener = object : AdListener() {
                    override fun onAdLoaded() { loaded = true }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(view) { onDispose { view.destroy() } }
        val h = with(LocalDensity.current) { size.getHeightInPixels(context).toDp() }
        // A rule and a gap keep the ad visibly apart from the app's own buttons (AdMob accidental-click policy).
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
            if (loaded) {
                androidx.compose.material3.HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            }
            Box(Modifier.fillMaxWidth().height(if (loaded) h else 0.dp)) {
                AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
