package com.aaron.grainchaintest.utils

import android.Manifest

object Globals {
    const val NEW_LOCATION_INTENT_FILTER = "com.aaron.grainchaintest.newLocation"
    const val LOCATION_INTENT_KEY = "LOCATIONS"
    val PERMISSIONS_TO_ASK = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.FOREGROUND_SERVICE,//be sure to be the previous to the last one
        Manifest.permission.ACCESS_BACKGROUND_LOCATION //be sure is the last
    )

}