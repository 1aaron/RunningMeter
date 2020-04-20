package com.aaron.grainchaintest.mapScreen

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.aaron.grainchaintest.models.GCTestDB
import com.aaron.grainchaintest.models.Route
import com.aaron.grainchaintest.utils.Globals
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.aaron.grainchaintest.R

interface MapFragmentViewModelInterface {
    var locations: ArrayList<Location>
    var stoppedTag: String
    var runningTag: String
    var polilyne: PolylineOptions
    fun reviewPermissions(): Boolean
    fun paintRoute(inMap: GoogleMap)
    fun saveRoute(alias: String, completion: () -> Unit)
    fun setMarkers(map: GoogleMap)
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

    override fun paintRoute(inMap: GoogleMap) {
        polilyne = PolylineOptions()
        locations.map { location ->
            polilyne.add(LatLng(location.latitude,location.longitude))
        }
        val lastPoint = polilyne.points.last()
        inMap.clear()
        inMap.addPolyline(polilyne)
        inMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPoint,24f))
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
            val route = Route(0,alias,0f,0,locations,locations.first(),locations.last())
            db.routeDao().addRoute(route)
            completion()
        }
    }

    override fun setMarkers(map: GoogleMap) {
        var initialMarker = MarkerOptions()
        var finishMarker = MarkerOptions()
        val initialLoc = locations.first()
        val lastLoc = locations.last()
        initialMarker.position(LatLng(initialLoc.latitude,initialLoc.longitude))
        initialMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)))
        map.addMarker(initialMarker)

        finishMarker.position(LatLng(lastLoc.latitude,lastLoc.longitude))
        finishMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)))
        map.addMarker(finishMarker)
    }
}