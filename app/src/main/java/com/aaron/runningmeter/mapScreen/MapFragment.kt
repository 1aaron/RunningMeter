package com.aaron.runningmeter.mapScreen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aaron.runningmeter.R
import com.aaron.runningmeter.databinding.MapFragmentBinding
import com.aaron.runningmeter.extensions.showLocationPermissionDialog
import com.aaron.runningmeter.services.LocationService
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
    private var mInterstitialAd: InterstitialAd? = null
    private final var TAG = "MapFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adRequest = AdRequest.Builder().build()
        //TODO: Change this id for test or real one
        InterstitialAd.load(requireContext(),Globals.ANNOUNCEMENT_ID,adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }
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

    @SuppressLint("MissingPermission")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mapView = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapView.getMapAsync(this)
        binder.fab.setOnClickListener {
            if (it.tag == viewModel.stoppedTag) {
                if (verifyPermissionStatus()) {
                    //TODO: remove when uploading
                    val testDeviceIds = listOf("FA681621979806E37E3B213A1F514285")
                    val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
                    MobileAds.setRequestConfiguration(configuration)
                    //TODO: until here
                    mInterstitialAd?.show(requireActivity())
                    gMap.clear()
                    gMap.isMyLocationEnabled = true
                    val locationManager: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(requireActivity())
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
                    Toast.makeText(context,getString(R.string.saving),Toast.LENGTH_SHORT).show()
                    viewModel.saveRoute(textAlias.text.toString()) {
                        aliasDialog.dismiss()
                        viewModel.setMarkers(gMap)
                        mInterstitialAd?.show(requireActivity())
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
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadCastReceiver, IntentFilter(Globals.NEW_LOCATION_INTENT_FILTER))
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadCastReceiver, IntentFilter(Globals.TIME_INTENT_FILTER))
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadCastReceiver)
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
        return if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showLocationPermissionDialog() {
                    askLocationPermissions()
                }
            } else {
                askLocationPermissions()
            }
            false
        } else {
            true
        }
    }

    private fun askLocationPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_DENIED
                    || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
                    requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                } else
                    requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
            else -> {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
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
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadCastReceiver)
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
        if (requestCode != 3)
            if (verifyPermissionStatus()) {
                binder.fab.performClick()
            }
    }

    @SuppressLint("MissingPermission")
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

    val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionResults ->
            var accepted = true
           for ((key, value) in permissionResults) {
               Log.e(TAG, key)
               if (!value) accepted = value
           }
            if (accepted) {
                Log.e(TAG,"permissions accepted")
            } else {
                Toast.makeText(requireContext(),getString(R.string.accept_permisses),Toast.LENGTH_SHORT).show()
            }
        }
}
