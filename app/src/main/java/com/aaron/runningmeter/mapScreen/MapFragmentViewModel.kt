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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrackingState {
    STOPPED, RUNNING
}

interface MapFragmentViewModelInterface {
    var locations: ArrayList<Location>
    val locationsFlow: StateFlow<List<Location>>
    val secondsFlow: StateFlow<Int>
    val distanceFlow: StateFlow<Double>
    val trackingState: StateFlow<TrackingState>
    val userLocation: StateFlow<Location?>
    val isMyLocationEnabled: StateFlow<Boolean>
    
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
    fun setTrackingState(state: TrackingState)
    fun updateSeconds(seconds: Int)
    fun updateLocations(locations: ArrayList<Location>)
    fun clearData()
}

class MapFragmentViewModel(application: Application) : AndroidViewModel(application), MapFragmentViewModelInterface {

    private val myApp = application
    private val _index = MutableLiveData<Int>()
    private var currentPosMarker: Marker? = null

    private val _locationsFlow = MutableStateFlow<List<Location>>(emptyList())
    override val locationsFlow: StateFlow<List<Location>> = _locationsFlow

    private val _secondsFlow = MutableStateFlow(0)
    override val secondsFlow: StateFlow<Int> = _secondsFlow

    private val _distanceFlow = MutableStateFlow(0.0)
    override val distanceFlow: StateFlow<Double> = _distanceFlow

    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    override val trackingState: StateFlow<TrackingState> = _trackingState

    private val _userLocation = MutableStateFlow<Location?>(null)
    override val userLocation: StateFlow<Location?> = _userLocation

    private val _isMyLocationEnabled = MutableStateFlow(false)
    override val isMyLocationEnabled: StateFlow<Boolean> = _isMyLocationEnabled

    override var locations = arrayListOf<Location>()
    override var stoppedTag = "STOPPED"
    override var runningTag = "RUNNING"
    override var polilyne = PolylineOptions()
    override var seconds = 0
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun setTrackingState(state: TrackingState) {
        _trackingState.value = state
    }

    override fun clearData() {
        locations.clear()
        _locationsFlow.value = emptyList()
        _secondsFlow.value = 0
        _distanceFlow.value = 0.0
        seconds = 0
    }

    override fun updateSeconds(seconds: Int) {
        this.seconds = seconds
        _secondsFlow.value = seconds
    }

    override fun updateLocations(locations: ArrayList<Location>) {
        this.locations = locations
        _locationsFlow.value = locations.toList()
        _distanceFlow.value = getDistance()
        if (locations.isNotEmpty()) {
            _userLocation.value = locations.last()
        }
    }

    fun updateUserLocation(location: Location) {
        _userLocation.value = location
    }

    fun setMyLocationEnabled(enabled: Boolean) {
        _isMyLocationEnabled.value = enabled
    }

    override fun paintRoute(inMap: GoogleMap) {
        polilyne = PolylineOptions().color(Color.BLUE)
        locations.map { location ->
            polilyne.add(LatLng(location.latitude,location.longitude))
        }
        val lastPoint = polilyne.points.last()
        inMap.clear()
        inMap.addPolyline(polilyne)
        inMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPoint,18f))

        currentPosMarker?.remove()
        val currentMarker = MarkerOptions()
        currentMarker.position(LatLng(locations.last().latitude, locations.last().longitude))
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
            val route = Route(0,alias,distance,seconds,dateFormatter.format(Date()),0.0)
            route.id = db.routeDao().addRoute(route)
            val locationsToSave = arrayListOf(Locations(0,route.id,locations[0].latitude,locations[0].longitude))
            for(x in 1 until locations.size) {
                val beginLocation = locations[x - 1]
                val endLocation = locations[x]
                locationsToSave.add(Locations(0,route.id,endLocation.latitude,endLocation.longitude))
                distance += beginLocation.distanceTo(endLocation)
            }
            distance /= 1000
            val df = DecimalFormat("#.###")
            df.roundingMode = RoundingMode.CEILING
            distance = df.format(distance).toDouble()
            //TODO: future updates -> calculate speed
            route.distance = distance
            db.routeDao().updateRoute(route)
            db.locationsDao().addLocations(locationsToSave)
            completion()
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
