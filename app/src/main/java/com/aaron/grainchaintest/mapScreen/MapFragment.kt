package com.aaron.grainchaintest.mapScreen

import android.app.AlertDialog
import android.content.*
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aaron.grainchaintest.R
import com.aaron.grainchaintest.databinding.MapFragmentBinding
import com.aaron.grainchaintest.services.LocationService
import com.aaron.grainchaintest.utils.Globals
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapFragmentViewModelInterface
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
        binder.fab.tag = viewModel.stoppedTag
        return binder.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mapView = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapView.getMapAsync(this)
        binder.fab.setOnClickListener {
            if (it.tag == viewModel.stoppedTag) {
                if (verifyPermissionStatus()) {
                    prepareLocationService()
                    it.tag = viewModel.runningTag
                    binder.fab.setImageResource(R.drawable.stop)
                    Toast.makeText(context,"begin route",Toast.LENGTH_SHORT).show()
                }
            } else {
                binder.fab.setImageResource(R.drawable.walk)
                it.tag = viewModel.stoppedTag
                disconnectLocationService()
                presentAliasDialog()
            }
        }
    }

    fun presentAliasDialog() {
        val textAlias = EditText(context)
        val aliasDialog = AlertDialog.Builder(context)
            .setView(textAlias)
            .setPositiveButton("Save",null)
            .create()
        aliasDialog.setCanceledOnTouchOutside(false)
        aliasDialog.setTitle("Set a name for route")
        aliasDialog.setOnShowListener {
            aliasDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (textAlias.text.isNotEmpty()) {
                    viewModel.saveRoute(textAlias.text.toString()) {
                        aliasDialog.dismiss()
                        viewModel.setMarkers(gMap)
                    }
                } else {
                    Toast.makeText(context,"Add a name for route",Toast.LENGTH_SHORT).show()
                }
            }
        }
        aliasDialog.show()
    }

    private fun disconnectLocationService() {
        gpsService?.stopTracking()
        activity?.let {
            val application = it.application
            val gpsIntent = Intent(application, LocationService::class.java)
            application.stopService(gpsIntent)
            application.unbindService(serviceConnection)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it.registerReceiver(broadCastReceiver, IntentFilter(Globals.NEW_LOCATION_INTENT_FILTER))
            it.registerReceiver(broadCastReceiver, IntentFilter(Globals.TIME_INTENT_FILTER))
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            it.unregisterReceiver(broadCastReceiver)
        }
    }
    private fun prepareLocationService() {
        activity?.let {
            val application = it.application
            val gpsIntent = Intent(application, LocationService::class.java)
            application.startService(gpsIntent)
            application.bindService(gpsIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun verifyPermissionStatus(): Boolean {
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
            if (intent.action == Globals.NEW_LOCATION_INTENT_FILTER) {
                intent.extras?.get(Globals.LOCATION_INTENT_KEY)?.let {
                    val locations = it as ArrayList<Location>
                    viewModel.locations = locations
                }
                viewModel.paintRoute(inMap = gMap)
            }
            if (intent.action == Globals.TIME_INTENT_FILTER) {
                intent.extras?.get(Globals.TIMER_KEY)?.let {
                    val seconds = it as Int
                    viewModel.seconds = seconds
                }
            }
        }
    }

    override fun onDestroy() {
        disconnectLocationService()
        activity?.let {
            it.unregisterReceiver(broadCastReceiver)
        }
        super.onDestroy()
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