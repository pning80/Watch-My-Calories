package com.pning80.watchmycalories.ads

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView as GmsNativeAdView
import com.pning80.watchmycalories.utils.AccessibilityTags

// Safety net mirroring `BannerAdView.TEST_AD_UNIT_PREFIXES`. If we ever hand
// `NATIVE_UNIT_ID` a Google test ID — either in debug or in a release built
// without local.properties prod IDs — skip the load entirely so users never
// see Google's literal "TEST AD" creative inside a real surface.
private val TEST_AD_UNIT_PREFIXES = listOf("ca-app-pub-3940256099942544/")

/**
 * Composable wrapper around the GMS [NativeAdView][GmsNativeAdView] that mirrors
 * iOS `NativeAdContentView` (NativeAdView.swift): media + "Ad" badge + icon +
 * headline + body + advertiser/star-rating/store row + branded CTA button,
 * rounded 16dp corners, theme-driven surface background.
 *
 * Previously rendered headline + CTA only — see PR I in the AdMob parity audit.
 */
@Composable
fun NativeAdView() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Reactive gate — mirrors `BannerAdView`. When PR H lands consent-gated
    // SDK init, `canRequestAds` will be false until UMP + MobileAds.initialize
    // resolve, so the AdLoader stays parked behind this check until the
    // recomposition triggered by the StateFlow update.
    val canRequestAds by AdManager.canRequestAds.collectAsState()

    // Theme tokens captured into argb for the AndroidView factory. Reading them
    // here keeps the programmatic AdView layout in sync with light/dark theme
    // swaps without each child View pulling at runtime.
    val surfaceArgb = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
    val outlineArgb = MaterialTheme.colorScheme.outline.toArgb()

    DisposableEffect(canRequestAds) {
        // Local-scope reference so the AdLoader callback and the onDispose
        // handler can clean up the *actually-loaded* ad even if the surrounding
        // composable has already left the tree (in which case the MutableState
        // setter is a no-op and `nativeAd` is null). Without this, a late
        // callback would leak the NativeAd — fixed per PR #31 review.
        var loadedAd: NativeAd? = null

        // Mirror iOS NativeAdLoader.loadAd guards plus the test-prefix guard.
        if (
            AdManager.disableForUITesting ||
            !canRequestAds ||
            TEST_AD_UNIT_PREFIXES.any { AdManager.NATIVE_UNIT_ID.startsWith(it) }
        ) {
            return@DisposableEffect onDispose { loadedAd?.destroy() }
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .setClickToExpandRequested(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            // iOS pre-installs a 16:9 height constraint on the media view; ask
            // GMS for the same aspect ratio so the card doesn't visibly "pop"
            // as the media reflows on first frame.
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
            .build()
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_UNIT_ID)
            .forNativeAd { ad ->
                loadedAd = ad
                nativeAd = ad
            }
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
        onDispose { loadedAd?.destroy() }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag(AccessibilityTags.Ads.NATIVE),
            factory = { ctx ->
                buildNativeAdView(
                    context = ctx,
                    surfaceArgb = surfaceArgb,
                    onSurfaceArgb = onSurfaceArgb,
                    onSurfaceVariantArgb = onSurfaceVariantArgb,
                    primaryArgb = primaryArgb,
                    onPrimaryArgb = onPrimaryArgb,
                    outlineArgb = outlineArgb,
                )
            },
            update = { adView -> bindNativeAd(adView, ad) }
        )
    }
}

private fun dp(context: android.content.Context, value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics).toInt()

@Suppress("LongMethod")
private fun buildNativeAdView(
    context: android.content.Context,
    surfaceArgb: Int,
    onSurfaceArgb: Int,
    onSurfaceVariantArgb: Int,
    primaryArgb: Int,
    onPrimaryArgb: Int,
    outlineArgb: Int,
): GmsNativeAdView {
    val adView = GmsNativeAdView(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 16f).toFloat()
            setColor(surfaceArgb)
        }
        clipToOutline = true
    }

    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    adView.addView(
        column,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
    )

    // --- Media area: MediaView + "Ad" badge overlay ---
    val mediaContainer = FrameLayout(context)
    column.addView(
        mediaContainer,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
    )

    val mediaView = MediaView(context)
    mediaContainer.addView(
        mediaView,
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
    )
    adView.mediaView = mediaView

    val adBadge = TextView(context).apply {
        text = "Ad"
        setTextColor(onPrimaryArgb)
        textSize = 11f
        gravity = Gravity.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(dp(context, 6f), dp(context, 2f), dp(context, 6f), dp(context, 2f))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 4f).toFloat()
            setColor(primaryArgb)
        }
    }
    val badgeMargin = dp(context, 8f)
    mediaContainer.addView(
        adBadge,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START,
        ).apply { setMargins(badgeMargin, badgeMargin, 0, 0) },
    )

    // --- Header row: icon + (headline / body) ---
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.TOP
        setPadding(dp(context, 16f), dp(context, 12f), dp(context, 16f), 0)
    }
    column.addView(
        headerRow,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
    )

    val iconView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 8f).toFloat()
            setColor(AndroidColor.TRANSPARENT)
        }
    }
    val iconSize = dp(context, 40f)
    headerRow.addView(
        iconView,
        LinearLayout.LayoutParams(iconSize, iconSize).apply { marginEnd = dp(context, 12f) },
    )
    adView.iconView = iconView

    val titleColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    headerRow.addView(
        titleColumn,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
    )

    val headlineView = TextView(context).apply {
        textSize = 16f
        setTextColor(onSurfaceArgb)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    titleColumn.addView(headlineView)
    adView.headlineView = headlineView

    val bodyView = TextView(context).apply {
        textSize = 13f
        setTextColor(onSurfaceVariantArgb)
        maxLines = 3
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, dp(context, 4f), 0, 0)
    }
    titleColumn.addView(bodyView)
    adView.bodyView = bodyView

    // --- Info row: advertiser / star rating / store ---
    val infoRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(context, 16f), dp(context, 8f), dp(context, 16f), 0)
    }
    column.addView(
        infoRow,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
    )

    fun infoLabel(): TextView = TextView(context).apply {
        textSize = 11f
        setTextColor(outlineArgb)
        setPadding(0, 0, dp(context, 8f), 0)
    }

    val advertiserView = infoLabel()
    infoRow.addView(advertiserView)
    adView.advertiserView = advertiserView

    val starRatingView = infoLabel()
    infoRow.addView(starRatingView)
    adView.starRatingView = starRatingView

    val storeView = infoLabel()
    infoRow.addView(storeView)
    adView.storeView = storeView

    // --- CTA button (left-aligned, branded) ---
    val ctaButton = Button(context).apply {
        setTextColor(onPrimaryArgb)
        textSize = 14f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 8f).toFloat()
            setColor(primaryArgb)
        }
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(context, 16f), dp(context, 8f), dp(context, 16f), dp(context, 8f))
        isClickable = false
    }
    val ctaParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        setMargins(dp(context, 16f), dp(context, 12f), dp(context, 16f), dp(context, 12f))
    }
    column.addView(ctaButton, ctaParams)
    adView.callToActionView = ctaButton

    return adView
}

private fun bindNativeAd(adView: GmsNativeAdView, ad: NativeAd) {
    (adView.headlineView as? TextView)?.text = ad.headline
    (adView.bodyView as? TextView)?.text = ad.body
    adView.bodyView?.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE

    // Icon — hide the slot when absent so the headline column slides left.
    val icon = ad.icon
    val iconView = adView.iconView as? ImageView
    if (icon != null && iconView != null) {
        iconView.setImageDrawable(icon.drawable)
        iconView.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    // Advertiser / rating / store.
    (adView.advertiserView as? TextView)?.text = ad.advertiser
    adView.advertiserView?.visibility =
        if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE

    val rating = ad.starRating
    val starText = adView.starRatingView as? TextView
    if (rating != null && starText != null) {
        starText.text = "★ ${"%.1f".format(rating)}"
        starText.visibility = View.VISIBLE
    } else {
        starText?.visibility = View.GONE
    }

    (adView.storeView as? TextView)?.text = ad.store
    adView.storeView?.visibility =
        if (ad.store.isNullOrBlank()) View.GONE else View.VISIBLE

    // CTA.
    val cta = ad.callToAction
    val ctaButton = adView.callToActionView as? Button
    if (cta.isNullOrBlank() || ctaButton == null) {
        ctaButton?.visibility = View.GONE
    } else {
        ctaButton.text = cta
        ctaButton.visibility = View.VISIBLE
    }

    adView.setNativeAd(ad)
}
