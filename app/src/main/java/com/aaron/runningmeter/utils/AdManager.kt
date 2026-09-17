package com.aaron.runningmeter.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "AdManager"

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, Globals.ANNOUNCEMENT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.e(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                setListeners()
            }
        })
    }

    private fun setListeners() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.e(TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.e(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }
    }

    fun showAd(activity: Activity) {
        mInterstitialAd?.show(activity)
    }
}
