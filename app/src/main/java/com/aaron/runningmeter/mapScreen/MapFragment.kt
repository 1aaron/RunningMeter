package com.aaron.runningmeter.mapScreen

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aaron.runningmeter.R
import com.aaron.runningmeter.extensions.showLocationPermissionDialog
import com.aaron.runningmeter.services.LocationService
import com.aaron.runningmeter.utils.Globals
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Task
import com.google.maps.android.compose.*
import java.util.Locale

class MapFragment : Fragment() {

    private lateinit var viewModel: MapFragmentViewModelInterface
    private var gpsService: LocationService? = null
    private var mInterstitialAd: InterstitialAd? = null
    private val CLASS_TAG = "MapFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val trackingState by viewModel.trackingState.collectAsState()
                    val locations by viewModel.locationsFlow.collectAsState()
                    val seconds by viewModel.secondsFlow.collectAsState()
                    val distance by viewModel.distanceFlow.collectAsState()
                    
                    var showAliasDialog by remember { mutableStateOf(false) }

                    MapScreenContent(
                        trackingState = trackingState,
                        locations = locations,
                        seconds = seconds,
                        distance = distance,
                        onFabClick = {
                            if (trackingState == TrackingState.STOPPED) {
                                handleStartClick()
                            } else {
                                if (viewModel.locations.isNotEmpty()) {
                                    showAliasDialog = true
                                }
                                handleStopClick()
                            }
                        },
                        showAliasDialog = showAliasDialog,
                        onDismissAliasDialog = { showAliasDialog = false },
                        onSaveRoute = { alias ->
                            saveRoute(alias)
                            showAliasDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun handleStartClick() {
        if (verifyPermissionStatus()) {
            viewModel.clearData()
            loadAdd()
            val locationManager: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                prepareLocationService()
                viewModel.setTrackingState(TrackingState.RUNNING)
                Toast.makeText(context, getString(R.string.beginRoute), Toast.LENGTH_SHORT).show()
            } else {
                reviewGPSSettings()
            }
        }
    }

    private fun handleStopClick() {
        viewModel.setTrackingState(TrackingState.STOPPED)
        disconnectLocationService()
    }

    private fun loadAdd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), Globals.ANNOUNCEMENT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(CLASS_TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.e(CLASS_TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                setListeners()
            }
        })
    }

    private fun setListeners() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.e(CLASS_TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(CLASS_TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.e(CLASS_TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }
    }

    private fun prepareLocationService() {
        activity?.let {
            val application = it.application
            val gpsIntent = Intent(application, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(gpsIntent)
            } else {
                requireActivity().startService(gpsIntent)
            }
            application.bindService(gpsIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
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
        } catch (e: Exception) {
            e.printStackTrace()
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
                    viewModel.updateLocations(locations)
                }
            }
            if (intent.action == Globals.TIME_INTENT_FILTER) {
                intent.extras?.get(Globals.TIMER_KEY)?.let {
                    val seconds = it as Int
                    viewModel.updateSeconds(seconds)
                }
            }
        }
    }

    private fun verifyPermissionStatus(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showLocationPermissionDialog {
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
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_DENIED
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

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResults ->
        var accepted = true
        for ((key, value) in permissionResults) {
            if (!value) accepted = value
        }
        if (accepted) {
            handleStartClick()
        } else {
            Toast.makeText(requireContext(), getString(R.string.accept_permisses), Toast.LENGTH_SHORT).show()
        }
    }

    private fun reviewGPSSettings() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
                prepareLocationService()
                viewModel.setTrackingState(TrackingState.RUNNING)
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            startIntentSenderForResult(resolvable.resolution.intentSender,
                                LocationRequest.PRIORITY_HIGH_ACCURACY, null, 0,
                                0, 0, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun saveRoute(alias: String) {
        Toast.makeText(context, getString(R.string.saving), Toast.LENGTH_SHORT).show()
        viewModel.saveRoute(alias) {
            mInterstitialAd?.show(requireActivity())
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadCastReceiver, IntentFilter(Globals.NEW_LOCATION_INTENT_FILTER))
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadCastReceiver, IntentFilter(Globals.TIME_INTENT_FILTER))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadCastReceiver)
    }

    override fun onDestroy() {
        disconnectLocationService()
        super.onDestroy()
    }
}

@Composable
fun MapScreenContent(
    trackingState: TrackingState,
    locations: List<Location>,
    seconds: Int,
    distance: Double,
    onFabClick: () -> Unit,
    showAliasDialog: Boolean,
    onDismissAliasDialog: () -> Unit,
    onSaveRoute: (String) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    
    LaunchedEffect(locations, trackingState) {
        if (locations.isNotEmpty()) {
            if (trackingState == TrackingState.RUNNING) {
                val lastLoc = locations.last()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lastLoc.latitude, lastLoc.longitude), 18f)
                )
            } else {
                val builder = LatLngBounds.Builder()
                locations.forEach {
                    builder.include(LatLng(it.latitude, it.longitude))
                }
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = trackingState == TrackingState.RUNNING)
        ) {
            if (locations.isNotEmpty()) {
                Polyline(
                    points = locations.map { LatLng(it.latitude, it.longitude) },
                    color = Color.Blue,
                    width = 10f
                )
                if (trackingState == TrackingState.RUNNING) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(locations.last().latitude, locations.last().longitude)),
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)
                    )
                } else {
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(locations.first().latitude, locations.first().longitude)),
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)
                    )
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(locations.last().latitude, locations.last().longitude)),
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)
                    )
                }
            }
        }

        // Stats Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(text = stringResource(id = R.string.distance, String.format(Locale.getDefault(), "%.3f", distance)))
            
            val hours: Int = seconds / 3600
            val minutes: Int = (seconds % 3600) / 60
            val reminder: Int = seconds % 60
            val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, reminder)
            StatItem(text = stringResource(id = R.string.timeData, timeFormatted))
        }

        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                painter = painterResource(id = if (trackingState == TrackingState.STOPPED) R.drawable.walk else R.drawable.stop),
                contentDescription = if (trackingState == TrackingState.STOPPED) "Start" else "Stop",
                tint = Color.White
            )
        }

        if (showAliasDialog) {
            var alias by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismissAliasDialog,
                title = { Text(stringResource(id = R.string.nameDialogTitle)) },
                text = {
                    TextField(
                        value = alias,
                        onValueChange = { alias = it },
                        placeholder = { Text("Enter route name") }
                    )
                },
                confirmButton = {
                    Button(onClick = { if (alias.isNotEmpty()) onSaveRoute(alias) }) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            )
        }
    }
}

@Composable
fun StatItem(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.8f),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 19.sp,
            color = Color.Black
        )
    }
}

@Preview(showSystemUi = true, name = "Light Mode")
@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun MapScreenPreview() {
    MaterialTheme {
        MapScreenContent(
            trackingState = TrackingState.STOPPED,
            locations = emptyList(),
            seconds = 1850,
            distance = 5.2,
            onFabClick = {},
            showAliasDialog = false,
            onDismissAliasDialog = {},
            onSaveRoute = {}
        )
    }
}
