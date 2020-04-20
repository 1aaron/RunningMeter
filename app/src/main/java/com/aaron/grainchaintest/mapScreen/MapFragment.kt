package com.aaron.grainchaintest.mapScreen

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aaron.grainchaintest.R
import com.aaron.grainchaintest.databinding.MapFragmentBinding
import com.aaron.grainchaintest.services.LocationService
import com.aaron.grainchaintest.utils.Globals
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapFragmentViewModel
    private lateinit var binder: MapFragmentBinding
    private var gpsService: LocationService? = null
    private lateinit var gMap: GoogleMap
    val PERMISSIONS_CHECK_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapFragmentViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binder = MapFragmentBinding.inflate(inflater)
        return binder.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mapView = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapView.getMapAsync(this)
        binder.fab.setOnClickListener {
            if (verifyPermissionStatus()) {
                prepareLocationService()
                Toast.makeText(context,"clicked walk",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it.registerReceiver(broadCastReceiver, IntentFilter(Globals.NEW_LOCATION_INTENT_FILTER))
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            it.unregisterReceiver(broadCastReceiver)
        }
    }
    fun prepareLocationService() {
        activity?.let {
            val application = it.application
            val gpsIntent = Intent(application, LocationService::class.java)
            application.startService(gpsIntent)
            application.bindService(gpsIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun verifyPermissionStatus(): Boolean {
        if (!viewModel.reviewPermissions()) {
            requestPermissions(Globals.PERMISSIONS_TO_ASK,PERMISSIONS_CHECK_CODE)
            return false
        }
        return true
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val name = className.className
            if (name.endsWith("LocationService")) {
                gpsService = (service as LocationService.LocationServiceBinder).getService()
                gpsService?.startTracking()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            if (className.className == "LocationService") {
                gpsService?.stopTracking()
                gpsService = null
            }
        }
    }

    private val broadCastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e("broadcast",intent.action ?: intent.toString())

        }
    }

    override fun onDestroyView() {
        activity?.let {
            it.unregisterReceiver(broadCastReceiver)
        }
        super.onDestroyView()
    }

    companion object {
        private var INSTANCE: MapFragment? = null

        @JvmStatic
        fun getInstance(): MapFragment {
            return INSTANCE
                ?: synchronized(this) {
                INSTANCE
                    ?: MapFragment()
                        .also { INSTANCE = it }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (verifyPermissionStatus()) {
            binder.fab.performClick()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
    }
}