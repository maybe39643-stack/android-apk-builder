package com.filemanager.vip.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdManager - Handles all AdMob ads with TEST ad unit IDs.
 * Replace test IDs with real ones before production release.
 */
object AdManager : LifecycleObserver {

    private const val TAG = "AdManager"

    // ============ TEST AD UNIT IDs (Google's official test IDs) ============
    private const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoaded = false
    private var isShowingAppOpen = false

    private var onRewardedCallback: (() -> Unit)? = null
    private var firstLaunchDone = false
    private var rewardedFailed = false
    private var adsInitialized = false

    /** Tracks the currently visible activity (set from MainActivity) */
    @Volatile
    private var currentActivity: Activity? = null

    /** Call once in Application class or MainActivity */
    fun init(context: Context) {
        if (adsInitialized) return
        adsInitialized = true
        try {
            // Set test device configuration
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
            )
            
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds init status: $status")
                // Load ads regardless - they'll be ready when init completes
                loadInterstitial(context)
                loadRewarded(context)
                loadAppOpen(context)
            }
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (e: Exception) {
            Log.e(TAG, "AdMob init error: ${e.message}")
        }
    }

    /** Register the current activity so app-open ads can show correctly */
    fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    // ==================== BANNER ====================
    fun loadBanner(activity: Activity, container: ViewGroup) {
        try {
            val adView = AdView(activity)
            adView.adUnitId = BANNER_ID
            adView.setAdSize(AdSize.BANNER)
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Banner failed: ${error.message} (code: ${error.code})")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Banner loaded OK")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner impression")
                }
            }
            
            val request = AdRequest.Builder().build()
            container.removeAllViews()
            container.addView(adView)
            adView.loadAd(request)
        } catch (e: Exception) {
            Log.e(TAG, "Banner load error: ${e.message}")
        }
    }

    // ==================== INTERSTITIAL ====================
    private fun loadInterstitial(context: Context) {
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        Log.d(TAG, "Interstitial loaded OK")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        Log.d(TAG, "Interstitial failed: ${error.message} (code: ${error.code})")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Interstitial load error: ${e.message}")
        }
    }

    fun showInterstitial(activity: Activity, onComplete: (() -> Unit)? = null) {
        try {
            val ad = interstitialAd ?: run {
                loadInterstitial(activity)
                onComplete?.invoke()
                return
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onComplete?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onComplete?.invoke()
                }
            }
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Interstitial show error: ${e.message}")
            onComplete?.invoke()
        }
    }

    // ==================== REWARDED ====================
    private fun loadRewarded(context: Context) {
        try {
            RewardedAd.load(
                context,
                REWARDED_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        rewardedFailed = false
                        Log.d(TAG, "Rewarded loaded OK")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        rewardedFailed = true
                        Log.d(TAG, "Rewarded failed: ${error.message} (code: ${error.code})")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded load error: ${e.message}")
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        try {
            val ad = rewardedAd
            if (ad == null) {
                loadRewarded(activity)
                onReward.invoke() // give reward anyway if not loaded (VIP unlock demo)
                return
            }
            onRewardedCallback = onReward
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewarded(activity)
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "Reward earned: ${rewardItem.amount} ${rewardItem.type}")
                onRewardedCallback?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show error: ${e.message}")
            onReward.invoke() // Give reward on error
        }
    }

    // ==================== APP OPEN ====================
    private fun loadAppOpen(context: Context) {
        try {
            AppOpenAd.load(
                context,
                APP_OPEN_ID,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        appOpenLoaded = true
                        Log.d(TAG, "App Open loaded OK")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenLoaded = false
                        Log.d(TAG, "App Open failed: ${error.message} (code: ${error.code})")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "App Open load error: ${e.message}")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        // Never show an app-open ad on cold start - only on subsequent foregrounds
        if (!firstLaunchDone) {
            firstLaunchDone = true
            return
        }
        val activity = currentActivity ?: return
        if (appOpenLoaded && !isShowingAppOpen) {
            val ad = appOpenAd ?: return
            isShowingAppOpen = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    appOpenLoaded = false
                    isShowingAppOpen = false
                    loadAppOpen(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    appOpenLoaded = false
                    isShowingAppOpen = false
                    loadAppOpen(activity.applicationContext)
                }
            }
            try {
                ad.show(activity)
            } catch (e: Exception) {
                isShowingAppOpen = false
                appOpenAd = null
                appOpenLoaded = false
                loadAppOpen(activity.applicationContext)
            }
        }
    }

    fun isRewardedReady(): Boolean = rewardedAd != null
}
