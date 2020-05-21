package com.aaron.runningmeter.mapScreen

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aaron.runningmeter.R
import com.aaron.runningmeter.databinding.MapFragmentBinding
import com.aaron.runningmeter.services.LocationService
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Task


/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapFragmentViewModelInterface
    private lateinit var binder: MapFragmentBinding
    private var gpsService: LocationService? = null
    private lateinit var gMap: GoogleMap
    val PERMISSIONS_CHECK_CODE = 1
    private lateinit var mInterstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mInterstitialAd = InterstitialAd(context)
        mInterstitialAd.adUnitId = Globals.TEST_ANNOUNCEMENT_ID //TODO: CHANGE FOR REAL ONE
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
            mInterstitialAd.loadAd(AdRequest.Builder().build())
            if (it.tag == viewModel.stoppedTag) {
                if (verifyPermissionStatus()) {
                    //TODO: REVIEW GPS SETTINGS
                    gMap.clear()
                    val locationManager: LocationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        prepareLocationService()
                        setTrackingState()
                    } else {
                        reviewGPSSettings()
                    }
                }
            } else {
                binder.fab.setImageResource(R.drawable.walk)
                it.tag = viewModel.stoppedTag
                disconnectLocationService()
                presentAliasDialog()
            }
        }
    }

    fun setTrackingState() {
        binder.fab.tag = viewModel.runningTag
        binder.fab.setImageResource(R.drawable.stop)
        Toast.makeText(context,getString(R.string.beginRoute),Toast.LENGTH_SHORT).show()
    }

    fun reviewGPSSettings() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(activity!!)
            .checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                prepareLocationService()
                setTrackingState()
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.SUCCESS -> {
                        Log.e("CORRECTO","correcto!!!")
                    }
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            startIntentSenderForResult(resolvable.resolution.intentSender,
                                LocationRequest.PRIORITY_HIGH_ACCURACY,null,0,
                                0,0,null)
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                        } catch (e: ClassCastException) {
                            e.printStackTrace()
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> { }
                }
            }
        }

    }

    fun presentAliasDialog() {
        val textAlias = EditText(context)
        val aliasDialog = AlertDialog.Builder(context)
            .setView(textAlias)
            .setPositiveButton(getString(R.string.save),null)
            .create()
        aliasDialog.setCanceledOnTouchOutside(false)
        aliasDialog.setTitle(getString(R.string.nameDialogTitle))
        aliasDialog.setOnShowListener {
            aliasDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (textAlias.text.isNotEmpty()) {
                    viewModel.saveRoute(textAlias.text.toString()) {
                        aliasDialog.dismiss()
                        viewModel.setMarkers(gMap)
                        mInterstitialAd.show()
                    }
                } else {
                    Toast.makeText(context,getString(R.string.nameDialogTitle),Toast.LENGTH_SHORT).show()
                }
            }
        }
        aliasDialog.show()
    }

    private fun disconnectLocationService() {
        gpsService?.stopTracking()
        try {
            activity?.let {
                val application = it.application
                val gpsIntent = Intent(application, LocationService::class.java)
                application.stopService(gpsIntent)
                application.unbindService(serviceConnection)
            }
        }catch (e: Exception) {
            e.printStackTrace()
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
                binder.txtDistance.text = getString(R.string.distance, viewModel.getDistance().toString())
            }
            if (intent.action == Globals.TIME_INTENT_FILTER) {
                intent.extras?.get(Globals.TIMER_KEY)?.let {
                    val seconds = it as Int
                    viewModel.seconds = seconds
                    binder.txtTime.text = getString(R.string.timeData, viewModel.getTimeStamp())
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            disconnectLocationService()
            activity?.let {
                it.unregisterReceiver(broadCastReceiver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            LocationRequest.PRIORITY_HIGH_ACCURACY -> {
                if (resultCode == Activity.RESULT_OK) {
                    prepareLocationService()
                    setTrackingState()
                } else {
                    Toast.makeText(context,getString(R.string.errorGPS),Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}