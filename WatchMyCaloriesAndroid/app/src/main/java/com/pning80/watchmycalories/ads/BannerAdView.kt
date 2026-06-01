package com.pning80.watchmycalories.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pning80.watchmycalories.utils.AccessibilityTags

// Safety net against Google's literal "TEST AD" creative ever reaching real
// users. Triggers in two situations:
//   - Debug builds, where ADMOB_BANNER_ID falls back to the test ID. The
//     banner stays hidden in dev so the UI doesn't look broken.
//   - Release builds where local.properties was missing the ADMOB_* keys —
//     `app/build.gradle.kts` falls back silently to the same test ID, and
//     this guard keeps a misconfigured release from serving test creative.
// Mirrors the companion `AdManager.isTestUnit` check that gates the
// interstitial flow; once production IDs are wired in local.properties for
// release, both branches naturally render real ads.
private val TEST_AD_UNIT_PREFIXES = listOf("ca-app-pub-3940256099942544/")

@Composable
fun BannerAdView() {
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
