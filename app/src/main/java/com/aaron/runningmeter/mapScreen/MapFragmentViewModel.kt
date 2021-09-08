package com.aaron.runningmeter.mapScreen

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.aaron.runningmeter.models.GCTestDB
import com.aaron.runningmeter.models.Route
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val shortDateFormatter: SimpleDateFormat
    fun reviewPermissions(): Boolean
    fun clearData()
    fun paintRoute(inMap: GoogleMap)
    fun initRoute()
    fun saveRoute(alias: String, completion: () -> Unit)
    fun setMarkers(map: GoogleMap)
    fun getDistance(): Double
    fun getTimeStamp(): String
}

class MapFragmentViewModel(application: Application) : AndroidViewModel(application), MapFragmentViewModelInterface {

    private val myApp = application
    private val _index = MutableLiveData<Int>()
    private var currentPosMarker: Marker? = null

    override var locations = arrayListOf<Location>()
    override var stoppedTag = "STOPPED"
    override var runningTag = "RUNNING"
    override var polilyne = PolylineOptions().color(Color.BLUE)
    override var seconds = 0
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    override val shortDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var currentRoute: Route? = null

    override fun clearData() {
        currentRoute = null
        polilyne = PolylineOptions().color(Color.BLUE)
    }

    override fun paintRoute(inMap: GoogleMap) {
        if(locations.isEmpty())
            return
        val lastPoint = locations.last()
        polilyne.add(LatLng(lastPoint.latitude,lastPoint.longitude))
        inMap.clear()
        inMap.addPolyline(polilyne)
        inMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polilyne.points.last(),18f))

        currentPosMarker?.remove()
        val currentMarker = MarkerOptions()
        currentMarker.position(LatLng(lastPoint.latitude, lastPoint.longitude))
        currentMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)))
        inMap.addMarker(currentMarker)
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
        viewModelScope.launch {
            val db = GCTestDB.getAppDataBase(myApp.applicationContext)
            var distance = 0.0
            currentRoute.apply {
                this?.alias = alias
                this?.distance = distance
                this?.time = seconds
                this?.date = dateFormatter.format(Date())
            }
            val locationsToSave = arrayListOf(Locations(0,currentRoute!!.id,locations[0].latitude,locations[0].longitude))
            for(x in 1 until locations.size) {
                val beginLocation = locations[x - 1]
                val endLocation = locations[x]
                locationsToSave.add(Locations(0,currentRoute!!.id,endLocation.latitude,endLocation.longitude))
                distance += beginLocation.distanceTo(endLocation)
            }
            distance /= 1000
            val df = DecimalFormat("#.###")
            df.roundingMode = RoundingMode.CEILING
            distance = df.format(distance).toDouble()
            //TODO: future updates -> calculate speed
            currentRoute?.distance = distance
            db.routeDao().updateRoute(currentRoute!!)
            db.locationsDao().addLocations(locationsToSave)
            completion()
        }
    }

    override fun initRoute() {
        val db = GCTestDB.getAppDataBase(myApp.applicationContext)
        val route = Route(0,"",0.0,0,dateFormatter.format(Date()),0.0)
        viewModelScope.launch(Dispatchers.IO) {
            route.id = db.routeDao().addRoute(route)
            withContext(Dispatchers.Main) {
                Log.e("mapVM","roteID: ${route.id}")
                currentRoute = route
            }
        }
    }

    override fun setMarkers(map: GoogleMap) {
        if (locations.isNotEmpty()) {
            var initialMarker = MarkerOptions()
            var finishMarker = MarkerOptions()
            val initialLoc = locations.first()
            val lastLoc = locations.last()
            currentPosMarker?.remove()
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
