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

@Composable
fun BannerAdView() {
    // Gates mirror iOS BannerAdView (BannerAdView.swift:12) exactly:
    // `!isUITestingMode && adManager.canRequestAds`. No test-ID suppression —
    // iOS renders Google's test creative in DEBUG, and so does Android, so the
    // ad slot is verifiable in dev. Release resolves ADMOB_BANNER_ID to the
    // production ID via the committed `Ads/AdMob-Android.properties` +
    // release-buildType override, so real users get real ads. (The earlier
    // test-prefix guard was an Android-only divergence — removed for parity.)
    val canRequestAds by AdManager.canRequestAds.collectAsState()
    if (AdManager.disableForUITesting || !canRequestAds) return
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag(AccessibilityTags.Ads.BANNER),
        factory = { context ->
            AdView(context).apply {
                // Full-width adaptive anchored banner — mirrors iOS
                // `currentOrientationAnchoredAdaptiveBanner(width:)`
                // (BannerAdView.swift:112). The previous fixed AdSize.BANNER
                // (320x50dp) rendered narrow and centered with side margins on
                // phones wider than 320dp. Adaptive sizes to the device width.
                val density = context.resources.displayMetrics.density
                val adWidthDp = (context.resources.displayMetrics.widthPixels / density).toInt()
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
                )
                adUnitId = AdManager.BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
