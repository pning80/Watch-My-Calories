package com.pning80.watchmycalories.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pning80.watchmycalories.utils.AccessibilityTags

// Safety net against Google's literal "TEST AD" creative ever reaching real
// users. Triggers in two situations:
//   - Debug builds, where `defaultConfig` in app/build.gradle.kts pins
//     ADMOB_BANNER_ID to the test ID so dev installs + parity tests don't
//     fire real AdMob impressions.
//   - Release builds where both `Ads/AdMob-Android.properties` (committed
//     prod source) and `local.properties` (per-dev override) are missing
//     the ADMOB_* keys — the build script falls back silently to the test
//     ID, and this guard keeps the misconfigured release from serving
//     Google's "TEST AD" creative.
// Mirrors the companion `AdManager.isTestUnit` check that gates the
// interstitial flow; with the committed properties file present, release
// builds naturally resolve to production IDs and render real ads.
private val TEST_AD_UNIT_PREFIXES = listOf("ca-app-pub-3940256099942544/")

@Composable
fun BannerAdView() {
    // Gates mirror iOS BannerAdView (BannerAdView.swift:13):
    // `!isUITestingMode && adManager.canRequestAds`. Without these the ad
    // request would race ahead of the consent-gated MobileAds.initialize.
    val canRequestAds by AdManager.canRequestAds.collectAsState()
    if (AdManager.disableForUITesting || !canRequestAds) return
    if (TEST_AD_UNIT_PREFIXES.any { AdManager.BANNER_UNIT_ID.startsWith(it) }) return
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag(AccessibilityTags.Ads.BANNER),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
