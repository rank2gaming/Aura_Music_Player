package com.rank2gaming.aura.utils

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {

    // TEST IDs provided in prompt
    private const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var mInterstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    /**
     * Initialize the Mobile Ads SDK.
     * Should be called in Application class or SplashActivity.
     */
    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    /**
     * Loads a Banner Ad into the provided AdView.
     * Handles visibility: GONE on failure, VISIBLE on success.
     */
    fun loadBanner(adView: AdView?) {
        if (adView == null) return

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                adView.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                adView.visibility = View.GONE
            }
        }
    }

    /**
     * Pre-loads an Interstitial Ad.
     * Call this early (e.g., in Splash onCreate).
     */
    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    isInterstitialLoading = false
                }
            })
    }

    /**
     * Checks if the interstitial is ready and shows it.
     * @param activity The current activity.
     * @param onNextAction Callback to run after ad closes or if ad fails/isn't ready.
     */
    fun showInterstitial(activity: Activity, onNextAction: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    onNextAction()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onNextAction()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            // Ad wasn't ready, proceed immediately
            onNextAction()
        }
    }

    /**
     * Check if an interstitial is currently loaded in memory.
     */
    fun isInterstitialLoaded(): Boolean {
        return mInterstitialAd != null
    }
}