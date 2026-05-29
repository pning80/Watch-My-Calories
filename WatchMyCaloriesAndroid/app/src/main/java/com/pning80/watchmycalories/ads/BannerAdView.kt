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

// Google-provided test ad units — visible "Test Ad" placeholder creative. We
// don't want this rendered in the user-facing UI even in debug builds because
// it looks like a bug. Render nothing while the project is still on the
// placeholder unit ID; flip on once real AdMob units are wired in AdManager.
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
