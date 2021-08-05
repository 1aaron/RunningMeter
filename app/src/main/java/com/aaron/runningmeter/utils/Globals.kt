package com.aaron.runningmeter.utils

import android.Manifest

object Globals {
    const val TIMER_KEY = "TIME"
    const val TEST_ANNOUNCEMENT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val ANNOUNCEMENT_ID = "ca-app-pub-4258059828543306/6057962808"
    const val TIME_INTENT_FILTER = "com.aaron.runningmeter.timeEnd"
    const val NEW_LOCATION_INTENT_FILTER = "com.aaron.runningmeter.newLocation"
    const val LOCATION_INTENT_KEY = "LOCATIONS"
    val PERMISSIONS_TO_ASK = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

}