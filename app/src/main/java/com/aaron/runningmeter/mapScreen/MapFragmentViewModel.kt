package com.aaron.runningmeter.mapScreen

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.aaron.runningmeter.models.GCTestDB
import com.aaron.runningmeter.models.Route
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface MapFragmentViewModelInterface {
    var locations: ArrayList<Location>
    var stoppedTag: String
    var runningTag: String
    var polilyne: PolylineOptions
    var seconds: Int
    fun reviewPermissions(): Boolean
    fun paintRoute(inMap: GoogleMap)
    fun saveRoute(alias: String, completion: () -> Unit)
    fun setMarkers(map: GoogleMap)
    fun getDistance(): Double
    fun getTimeStamp(): String
}

class MapFragmentViewModel(application: Application) : AndroidViewModel(application), MapFragmentViewModelInterface {

    private val myApp = application
    private val _index = MutableLiveData<Int>()
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override var locations = arrayListOf<Location>()
    override var stoppedTag = "STOPPED"
    override var runningTag = "RUNNING"
    override var polilyne = PolylineOptions()
    override var seconds = 0
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun paintRoute(inMap: GoogleMap) {
        polilyne = PolylineOptions().color(Color.BLUE)
        locations.map { location ->
            polilyne.add(LatLng(location.latitude,location.longitude))
        }
        val lastPoint = polilyne.points.last()
        inMap.clear()
        inMap.addPolyline(polilyne)
        inMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPoint,18f))
    }

    override fun reviewPermissions(): Boolean {
        var permissionsAccepted = true
        var permissionsToCheck = Globals.PERMISSIONS_TO_ASK.copyOf()
        if (Build.VERSION.SDK_INT < 29)
            permissionsToCheck = permissionsToCheck.dropLast(1).toTypedArray()
        if (Build.VERSION.SDK_INT < 28)
            permissionsToCheck = permissionsToCheck.dropLast(1).toTypedArray()
        for (permission in permissionsToCheck) {
            if (ContextCompat.checkSelfPermission(myApp, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsAccepted = false
                return permissionsAccepted
            }
        }
        return permissionsAccepted
    }

    override fun saveRoute(alias: String, completion: () -> Unit) {
        uiScope.launch {
            val db = GCTestDB.getAppDataBase(myApp.applicationContext)
            var distance = 0.0
            for(x in 1 until locations.size) {
                val beginLocation = locations[x - 1]
                val endLocation = locations[x]
                distance += beginLocation.distanceTo(endLocation)
            }
            distance /= 1000
            val df = DecimalFormat("#.###")
            df.roundingMode = RoundingMode.CEILING
            distance = df.format(distance).toDouble()
            //TODO: future updates -> calculate speed
            val route = Route(0,alias,distance,seconds,dateFormatter.format(Date()),0.0)
            val idInserted = db.routeDao().addRoute(route)
            for (location in locations) {
                val loToSave = Locations(0,idInserted,location.latitude,location.longitude)
                db.locationsDao().addLocation(loToSave)
            }
            completion()
        }
    }

    override fun setMarkers(map: GoogleMap) {
        if (locations.isNotEmpty()) {
            var initialMarker = MarkerOptions()
            var finishMarker = MarkerOptions()
            val initialLoc = locations.first()
            val lastLoc = locations.last()
            initialMarker.position(LatLng(initialLoc.latitude, initialLoc.longitude))
            initialMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)))
            map.addMarker(initialMarker)

            finishMarker.position(LatLng(lastLoc.latitude, lastLoc.longitude))
            finishMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)))
            map.addMarker(finishMarker)
        }
    }

    override fun getDistance(): Double {
        var distance = 0.0
        for(x in 1 until locations.size) {
            val beginLocation = locations[x - 1]
            val endLocation = locations[x]
            distance += beginLocation.distanceTo(endLocation)
        }
        distance /= 1000
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.CEILING
        distance = df.format(distance).toDouble()
        return distance
    }

    override fun getTimeStamp(): String {
        val hours: Int = (seconds) / 3600
        var reminder = (seconds) % 3600
        val minutes = reminder / 60
        reminder %= 60
        return "${String.format("%02d",hours)}:${String.format("%02d",minutes)}:${String.format("%02d",reminder)}"
    }
}
