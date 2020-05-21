package com.aaron.runningmeter.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService: Service() {
    private val locationinterval: Long = 2000
    private val fastestLocationInterval: Long = 1000
    private val NOTIFICATION_ID = 1000
    private val channelID = "locationsChannelID"
    private val channelName = "locationChannel"
    private var locationRequest: LocationRequest? = null
    private var locationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private val binder = LocationServiceBinder()
    private var locations = arrayListOf<Location>()
    private var job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var stopped = true
    var seconds = 0
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val loc = locationResult.lastLocation
                locations.add(loc)
                val intent = Intent(Globals.NEW_LOCATION_INTENT_FILTER)
                intent.putExtra(Globals.LOCATION_INTENT_KEY, locations)
                sendBroadcast(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun startTimer() {
        seconds = 0
        stopped = false
        uiScope.launch {
            while (!stopped) {
                delay(1000)
                seconds += 1
                val intent = Intent(Globals.TIME_INTENT_FILTER)
                intent.putExtra(Globals.TIMER_KEY, seconds)
                sendBroadcast(intent)
            }
        }
    }

    fun startTracking() {
        initializeLocationManager()
        startTimer()
        try {
            locationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (ex: SecurityException) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (ex: IllegalArgumentException) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    fun stopTimer() {
        stopped = true
        val intent = Intent(Globals.TIME_INTENT_FILTER)
        intent.putExtra(Globals.TIMER_KEY, seconds)
        sendBroadcast(intent)
        seconds = 0
    }

    override fun onDestroy() {
        locationClient?.removeLocationUpdates(locationCallback)
        stopForeground(true)
        super.onDestroy()
    }

    fun stopTracking() {
        stopTimer()
        onDestroy()
    }

    private fun initializeLocationManager() {
        locations.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID,getNotification())
        }
        if (locationRequest == null) {
            locationRequest = createLocationRequest()
        }
        locationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create()?.apply {
            interval = locationinterval
            fastestInterval = fastestLocationInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getNotification(): Notification? {
        val channel =
            NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager?.createNotificationChannel(channel)
        val builder: Notification.Builder =
            Notification.Builder(applicationContext, channelID).setAutoCancel(true)
        return builder.build()
    }

    inner class LocationServiceBinder: Binder() {
        fun getService(): LocationService = this@LocationService
    }
}