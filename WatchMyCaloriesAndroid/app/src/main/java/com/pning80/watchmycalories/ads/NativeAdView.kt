package com.pning80.watchmycalories.ads

import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView as GmsNativeAdView
import com.pning80.watchmycalories.utils.AccessibilityTags

@Composable
fun NativeAdView() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_UNIT_ID)
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag(AccessibilityTags.Ads.NATIVE),
            factory = { ctx ->
                // Basic NativeAdView Layout logic without explicit XML
                val adView = GmsNativeAdView(ctx)

                // Simple programmatic view setup for Native Ad parity
                val linearLayout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

                val headlineView = TextView(ctx).apply {
                    textSize = 16f
                    setTextColor(android.graphics.Color.BLACK)
                }
                linearLayout.addView(headlineView)
                adView.headlineView = headlineView

                val callToActionView = Button(ctx).apply {
                    tag = AccessibilityTags.Ads.VIEW_RESULTS_BUTTON
                }
                linearLayout.addView(callToActionView)
                adView.callToActionView = callToActionView
                
                adView.addView(linearLayout)
                
                // Populate the ad view
                (adView.headlineView as TextView).text = ad.headline
                if (ad.callToAction == null) {
                    adView.callToActionView?.visibility = android.view.View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = android.view.View.VISIBLE
                    (adView.callToActionView as Button).text = ad.callToAction
                }
                
                adView.setNativeAd(ad)
                adView
            }
        )
    }
}
