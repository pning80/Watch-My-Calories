package com.pning80.watchmycalories.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pning80.watchmycalories.utils.AccessibilityTags

/**
 * Adaptive banner ad, styled to match iOS `BannerAdView.swift`:
 *  - inset 16dp from the screen edges, 12dp rounded corners, a soft shadow
 *    (iOS BannerAdView.swift:18-20), height capped at 90dp (line 122)
 *  - adaptive width sized to the *actual* laid-out width, so AdMob picks the
 *    right creative + row height for every placement.
 *
 * @param insetHorizontal pass `false` when the caller's container already
 *   supplies the 16dp horizontal page margin (Dashboard's LazyColumn
 *   contentPadding, the Settings/ManualEntry/ScannedMenus scrolls, the
 *   Log Food / Scan Menu sheets). Edge-to-edge callers (History, About) keep
 *   the default `true` so the banner still sits 16dp off the screen edges,
 *   matching iOS where every banner is inset.
 */
@Composable
fun BannerAdView(insetHorizontal: Boolean = true) {
    // Gates mirror iOS BannerAdView (BannerAdView.swift:12) exactly:
    // `!isUITestingMode && adManager.canRequestAds`. No test-ID suppression —
    // iOS renders Google's test creative in DEBUG, and so does Android. Release
    // resolves ADMOB_BANNER_ID to the production ID via the committed
    // `Ads/AdMob-Android.properties` + release-buildType override.
    val canRequestAds by AdManager.canRequestAds.collectAsState()
    if (AdManager.disableForUITesting || !canRequestAds) return

    val hInset = if (insetHorizontal) 16.dp else 0.dp
    // Padding is applied to BoxWithConstraints, so `maxWidth` is the post-inset
    // width the AdView actually occupies — exactly what the adaptive sizer needs.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hInset)
    ) {
        val adWidthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 90.dp) // iOS caps banner height at 90 (BannerAdView.swift:122)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .testTag(AccessibilityTags.Ads.BANNER),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
                    )
                    adUnitId = AdManager.BANNER_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
