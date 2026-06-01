package com.pning80.watchmycalories.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.UserMessagingPlatform
import com.pning80.watchmycalories.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow

object AdManager {
    var isInitialized = false
        private set

    // Control toggles
    var canRequestAds = MutableStateFlow(true)
    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L // 1 min cooldown

    // Test hook: when true, showInterstitialIfReady is a no-op so the compose
    // tree is never destroyed mid-test by an interstitial fullscreen takeover.
    // Set by TestSeed when EXTRA_UI_TESTING is passed.
    var disableForUITesting = false

    private var interstitialAd: InterstitialAd? = null
    var isInterstitialReady = false
        private set

    var isPrivacyOptionsRequired = MutableStateFlow(false)
        private set

    // Ad unit IDs are sourced from BuildConfig. Debug builds always resolve
    // to Google's published test IDs (pinned in `defaultConfig` of
    // app/build.gradle.kts), so parity tests + dev installs never fire real
    // AdMob impressions. Release builds override with production IDs from
    // the committed `Ads/AdMob-Android.properties` file, with
    // `local.properties` taking precedence for per-dev overrides. App ID
    // lives in `AndroidManifest.xml` via the same mechanism
    // (`${ADMOB_APP_ID}` manifestPlaceholder). Mirrors the iOS pattern in
    // `AdManager.swift` (DEBUG → hardcoded test IDs, RELEASE → Info.plist
    // env vars sourced from `Ads/AdMob-iOS.xcconfig`).
    val BANNER_UNIT_ID: String = BuildConfig.ADMOB_BANNER_ID
    val NATIVE_UNIT_ID: String = BuildConfig.ADMOB_NATIVE_ID
    val INTERSTITIAL_UNIT_ID: String = BuildConfig.ADMOB_INTERSTITIAL_ID

    // Safety net — any of the three unit IDs starting with Google's test-unit
    // prefix means either a debug build or a misconfigured release (no
    // production IDs in `local.properties`). Surfaces that consult this flag
    // suppress rendering / loading rather than serve Google's literal "TEST
    // AD" creative to real users. The banner has its own copy of this guard
    // in `BannerAdView.kt`; this companion covers the interstitial.
    private val TEST_AD_UNIT_PREFIX = "ca-app-pub-3940256099942544/"
    private fun isTestUnit(id: String): Boolean = id.startsWith(TEST_AD_UNIT_PREFIX)

    fun initialize(context: Context) {
        if (isInitialized) return
        
        // Request consent info
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == 
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        MobileAds.initialize(context) {
            isInitialized = true
            loadInterstitial(context)
        }
    }

    fun presentPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            // Update required status after potential changes
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == 
                com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }
    }

    fun loadInterstitial(context: Context) {
        if (!canRequestAds.value) return
        // Don't fetch test creative — see TEST_AD_UNIT_PREFIX above.
        if (isTestUnit(INTERSTITIAL_UNIT_ID)) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialReady = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialReady = false
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit) {
        if (disableForUITesting) {
            onDismissed()
            return
        }
        // Belt-and-braces against a stale test-creative ad that somehow got
        // loaded — if isTestUnit returns true, loadInterstitial's guard above
        // should have already prevented this, but skip showing regardless.
        if (isTestUnit(INTERSTITIAL_UNIT_ID)) {
            onDismissed()
            return
        }
        val now = System.currentTimeMillis()
        if (interstitialAd != null && isInterstitialReady && (now - lastInterstitialTime > INTERSTITIAL_COOLDOWN_MS)) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isInterstitialReady = false
                    lastInterstitialTime = System.currentTimeMillis()
                    loadInterstitial(activity) // reload
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    isInterstitialReady = false
                    onDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onDismissed()
        }
    }
}
