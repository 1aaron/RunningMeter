package com.aaron.runningmeter.mapScreen

import android.app.Application
import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import com.aaron.runningmeter.models.GCTestDB
import com.aaron.runningmeter.models.Route
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.launch
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapFragmentViewModel(application: Application) : AndroidViewModel(application) {

     private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
     val shortDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
     private val myApp = application
     private var currentPosMarker: Marker? = null
     private var lastLocation: Location? = null

     var stoppedTag = "STOPPED"
     var runningTag = "RUNNING"
     private var polilyne = PolylineOptions().color(Color.BLUE)
     var seconds = 0
     var distance = 0.0

    var currentRoute: Route? = null

    fun clearData() {
        lastLocation = null
        currentRoute = null
        polilyne.points.clear()
        distance = 0.0
        seconds = 0
    }

    fun paintRoute(inMap: GoogleMap, locations: List<Locations>) {
        if (locations.isEmpty())
            return
        polilyne.points.clear()
        val lastLocation = locations.last()
        locations.forEach { polilyne.add(LatLng(it.latitude,it.longitude)) }
        inMap.clear()
        inMap.addPolyline(polilyne)
        inMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polilyne.points.last(),18f))

        currentPosMarker?.remove()
        val currentMarker = MarkerOptions()
        currentMarker.position(LatLng(lastLocation.latitude, lastLocation.longitude))
        currentMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)))
        currentPosMarker = inMap.addMarker(currentMarker)
    }

    fun saveRoute(alias: String, completion: () -> Unit) {
        viewModelScope.launch {
            val db = GCTestDB.getAppDataBase(myApp.applicationContext)
            //TODO: future updates -> calculate speed
            db.routeDao().updateValues(currentRoute!!.id,dateFormatter.format(Date()),alias)
            completion()
        }
    }

    fun initRoute(completion: () -> Unit) {
        val db = GCTestDB.getAppDataBase(myApp.applicationContext)
        val route = Route(0,dateFormatter.format(Date()),0.0,0,dateFormatter.format(Date()),0.0)
        viewModelScope.launch(Dispatchers.IO) {
            route.id = db.routeDao().addRoute(route)
            withContext(Dispatchers.Main) {
                Log.e("mapVM","roteID: ${route.id}")
                currentRoute = route
                completion()
            }
        }
    }

    fun setMarkers(map: GoogleMap) {
        val db = GCTestDB.getAppDataBase(myApp.applicationContext)
        viewModelScope.launch {
            val locations = db.locationsDao().getLocationsForRoute(currentRoute!!.id)
            if (locations.isNotEmpty()) {
                val initialMarker = MarkerOptions()
                val finishMarker = MarkerOptions()
                val initialLoc = locations.first()
                val lastLoc = locations.last()
                currentPosMarker?.remove()
                initialMarker.position(LatLng(initialLoc.latitude, initialLoc.longitude))
                initialMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)))

                finishMarker.position(LatLng(lastLoc.latitude, lastLoc.longitude))
                finishMarker.icon((BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)))
                withContext(Dispatchers.Main) {
                    map.addMarker(initialMarker)
                    map.addMarker(finishMarker)
                }
            }
        }
    }

    fun getTimeStamp(): String {
        val hours: Int = (seconds) / 3600
        var reminder = (seconds) % 3600
        val minutes = reminder / 60
        reminder %= 60
        return "${String.format("%02d",hours)}:${String.format("%02d",minutes)}:${String.format("%02d",reminder)}"
    }
}
