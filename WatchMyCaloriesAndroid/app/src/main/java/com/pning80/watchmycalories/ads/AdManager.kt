package com.pning80.watchmycalories.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.pning80.watchmycalories.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Mirror of iOS `AdManager.swift`. The two key invariants this enforces:
 *
 *  1. `MobileAds.initialize(...)` runs only **after** UMP consent has been
 *     gathered (or definitively declined). Calling it unconditionally on
 *     app launch — as the previous Android implementation did — violates
 *     Google's policy for EEA users.
 *
 *  2. `canRequestAds` starts `false` and flips to `true` only after the
 *     SDK reports init success and UMP says ads can be requested. Ad
 *     surfaces (`BannerAdView`, `NativeAdView`) gate their load() calls
 *     on this flag — same as iOS `NativeAdLoader.loadAd` guards on
 *     `AdManager.shared.canRequestAds`.
 */
object AdManager {
    private const val TAG = "AdManager"

    var isInitialized = false
        private set

    // Starts false; the consent + SDK-init pipeline (enableAds → gatherConsent →
    // startSDK) flips it true once the system is actually ready to serve ads.
    var canRequestAds = MutableStateFlow(false)
        private set

    var isPrivacyOptionsRequired = MutableStateFlow(false)
        private set

    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L // 1 min cooldown

    // Test hook: when true, all entry points are no-ops so instrumentation
    // doesn't trip into the real UMP flow or have a fullscreen interstitial
    // tear down the compose tree. Set by TestSeed when EXTRA_UI_TESTING is
    // passed; mirrors iOS `AdManager.isUITestingMode`.
    var disableForUITesting = false

    private var interstitialAd: InterstitialAd? = null
    var isInterstitialReady = false
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

    /**
     * Gathers UMP consent and starts the SDK. Idempotent — repeat calls
     * after [isInitialized] are no-ops. Mirrors iOS `AdManager.enableAds()`.
     *
     * Intended to be called from [android.app.Activity] context once the user
     * has completed onboarding, not from `onCreate`. iOS triggers the
     * equivalent from `WatchMyCaloriesApp.task` keyed on the
     * `hasCompletedOnboarding` flag.
     */
    suspend fun enableAds(activity: Activity) {
        if (isInitialized || disableForUITesting) return
        gatherConsent(activity)
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        if (consentInfo.canRequestAds()) {
            startSDK(activity)
        }
        isPrivacyOptionsRequired.value =
            consentInfo.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private suspend fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        // Step 1: request consent info update. UMP callbacks are one-shot,
        // so wrap each in suspendCancellableCoroutine. Errors are logged and
        // swallowed — same shape as iOS which catches and logs without
        // raising; the downstream `canRequestAds()` check governs whether
        // we actually serve ads.
        suspendCancellableCoroutine<Unit> { cont ->
            consentInfo.requestConsentInfoUpdate(
                activity, params,
                { if (cont.isActive) cont.resume(Unit) },
                { error ->
                    Log.w(TAG, "requestConsentInfoUpdate failed: ${error.message}")
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }

        // Step 2: load + show the consent form if required by the user's
        // region. No-op for users where consent isn't required (e.g. US).
        suspendCancellableCoroutine<Unit> { cont ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                if (formError != null) {
                    Log.w(TAG, "loadAndShowConsentFormIfRequired error: ${formError.message}")
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private fun startSDK(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) {
            isInitialized = true
            canRequestAds.value = true
            loadInterstitial(context)
        }
    }

    /**
     * Re-open the UMP privacy options form. Used by the Settings screen to
     * let users revisit their consent after the initial flow. Mirrors iOS
     * `presentPrivacyOptionsForm`.
     */
    fun presentPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "showPrivacyOptionsForm error: ${formError.message}")
            }
            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            isPrivacyOptionsRequired.value =
                consentInfo.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            val canRequest = consentInfo.canRequestAds()
            canRequestAds.value = canRequest
            if (canRequest && !isInitialized) {
                startSDK(activity)
            }
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
