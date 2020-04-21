package com.aaron.grainchaintest.detailScreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aaron.grainchaintest.R
import com.aaron.grainchaintest.models.GCTestDB
import com.aaron.grainchaintest.models.Locations
import com.aaron.grainchaintest.models.Route
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


interface DetailScreenViewModelInterface {
    val route: Route
    fun load(route: Route, completion: () -> Unit)
    fun paintRoute(inMap: GoogleMap)
    fun deleteRoute(completion: () -> Unit)
}

class DetailScreenViewModel(application: Application) : AndroidViewModel(application), DetailScreenViewModelInterface {
    private val myApp = application
    private lateinit var db: GCTestDB
    private lateinit var locations: List<Locations>
    override lateinit var route: Route
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val initialMarker = MarkerOptions()
    private val finishMarker = MarkerOptions()

    override fun load(route: Route, completion: () -> Unit) {
        this.route = route
        db = GCTestDB.getAppDataBase(myApp.applicationContext)
        uiScope.launch {
            locations = db.locationsDao().getLocationsForRoute(route.id)
            completion()
        }
    }

    override fun paintRoute(inMap: GoogleMap) {
        val polilyne = PolylineOptions()
        locations.map { location ->
            polilyne.add(LatLng(location.latitude,location.longitude))
        }
        inMap.clear()
        inMap.addPolyline(polilyne)
        setMarkers(inMap)
        val builder = LatLngBounds.Builder()
        builder.include(initialMarker.position)
        builder.include(finishMarker.position)
        val bounds = builder.build()
        val padding = 100
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        inMap.animateCamera(cu)
    }

    override fun deleteRoute(completion: () -> Unit) {
        uiScope.launch {
            db.locationsDao().removeLocationsForRoute(route.id)
            db.routeDao().deleteRoute(route)
            completion()
        }
    }

    private fun setMarkers(map: GoogleMap) {
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
