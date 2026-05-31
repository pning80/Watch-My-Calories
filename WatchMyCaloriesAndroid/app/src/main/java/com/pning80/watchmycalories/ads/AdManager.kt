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

    // Replace with actual AdMob App ID / Unit IDs for release
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

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
