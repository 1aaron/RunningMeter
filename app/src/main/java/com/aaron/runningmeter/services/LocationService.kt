package com.aaron.runningmeter.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aaron.runningmeter.activities.MainActivity
import com.aaron.runningmeter.models.GCTestDB
import com.aaron.runningmeter.models.Locations
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
    private var job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var stopped = true
    var seconds = 0
    private var routeId: Long = 0
    var distance = 0.0
    private var lastLocation: Location? = null

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    companion object {
        var isAttached = false
    }

    override fun onCreate() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val loc = locationResult.lastLocation
                calculateDistance(loc)
                val intent = Intent(Globals.NEW_LOCATION_INTENT_FILTER)
                intent.putExtra(Globals.LOCATION_INTENT_KEY, loc)
                val db = GCTestDB.getAppDataBase(applicationContext)
                uiScope.launch {
                    val locationToSave = Locations(0,routeId,loc.latitude,loc.longitude)
                    db.locationsDao().addLocation(locationToSave)
                    db.routeDao().updateValues(id = routeId,distance = distance, time = seconds, speed = 0.0)
                }
                LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
            }
        }
    }

    private fun calculateDistance(newLocation: Location) {
        var distanceCalculating = 0.0
        lastLocation?.let {
            distanceCalculating = it.distanceTo(newLocation).toDouble()
            distance += distanceCalculating
        }
        lastLocation = newLocation
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
                LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
            }
        }
    }

    fun startTracking(routeId: Long) {
        this.routeId = routeId
        distance = 0.0
        initializeLocationManager()
        startTimer()
        try {
            locationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isAttached = true
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
        LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
    }

    override fun onDestroy() {
        locationClient?.removeLocationUpdates(locationCallback)
        stopForeground(true)
        isAttached = false
        super.onDestroy()
    }

    fun stopTracking() {
        stopTimer()
        onDestroy()
    }

    private fun initializeLocationManager() {
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
    private fun getNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val channel =
            NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH)
        getSystemService(
            NotificationManager::class.java
        ).apply {
            createNotificationChannel(channel)
        }
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, channelID)
        builder.apply {
            setContentIntent(pendingIntent)
        }
        return builder.build()
    }

    inner class LocationServiceBinder: Binder() {
        fun getService(): LocationService = this@LocationService
    }
}