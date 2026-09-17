package com.aaron.runningmeter.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.aaron.runningmeter.R
import com.aaron.runningmeter.ListScreen.ListFragmentViewModel
import com.aaron.runningmeter.ListScreen.RouteListScreen
import com.aaron.runningmeter.detailScreen.DetailScreenContent
import com.aaron.runningmeter.detailScreen.DetailScreenViewModel
import com.aaron.runningmeter.extensions.showLocationPermissionDialog
import com.aaron.runningmeter.mapScreen.MapFragmentViewModel
import com.aaron.runningmeter.mapScreen.MapScreenContent
import com.aaron.runningmeter.mapScreen.TrackingState
import com.aaron.runningmeter.ui.theme.RunningMeterTheme
import androidx.activity.result.IntentSenderRequest
import com.aaron.runningmeter.models.Route
import com.aaron.runningmeter.services.LocationService
import com.aaron.runningmeter.utils.Globals
import com.aaron.runningmeter.utils.AdManager
import com.aaron.runningmeter.utils.StorageUtils
import com.google.android.gms.ads.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import androidx.core.content.ContextCompat
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var gpsService: LocationService? = null
    private lateinit var adManager: AdManager
    private lateinit var mapViewModel: MapFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)

        // Inicialización de componentes desacoplados
        MobileAds.initialize(this) {}
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) {}
        adManager = AdManager(this)

        setContent {
            RunningMeterTheme {
                val navController = rememberNavController()
                
                mapViewModel = viewModel<MapFragmentViewModel>()
                
                LaunchedEffect(Unit) {
                    mapViewModel.setMyLocationEnabled(checkPermissions())
                    if (checkPermissions()) {
                        fetchLastLocation(mapViewModel)
                    }
                }
                val listViewModel = viewModel<ListFragmentViewModel>()
                val detailViewModel = viewModel<DetailScreenViewModel>()

                val trackingState by mapViewModel.trackingState.collectAsState()
                val locations by mapViewModel.locationsFlow.collectAsState()
                val seconds by mapViewModel.secondsFlow.collectAsState()
                val distance by mapViewModel.distanceFlow.collectAsState()
                val userLocation by mapViewModel.userLocation.collectAsState()
                val isMyLocationEnabled by mapViewModel.isMyLocationEnabled.collectAsState()

                var showAliasDialog by remember { mutableStateOf(false) }
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                val topBarTitle = when {
                    currentRoute == "map" -> stringResource(id = R.string.app_name)
                    currentRoute == "list" -> stringResource(id = R.string.list_tab_text)
                    currentRoute?.startsWith("detail") == true -> {
                        val routeJson = currentBackStackEntry?.arguments?.getString("routeJson")
                        val decodedJson = URLDecoder.decode(routeJson ?: "", StandardCharsets.UTF_8.toString())
                        val route = Gson().fromJson(decodedJson, Route::class.java)
                        route.alias ?: "Route"
                    }
                    else -> stringResource(id = R.string.app_name)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(topBarTitle) },
                            navigationIcon = {
                                if (currentRoute?.startsWith("detail") == true) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    },
                    bottomBar = {
                        if (currentRoute == "map" || currentRoute == "list") {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                                    label = { Text("Map") },
                                    selected = currentRoute == "map",
                                    onClick = {
                                        if (currentRoute != "map") {
                                            navController.navigate("map") {
                                                popUpTo("map") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = "Routes") },
                                    label = { Text("Routes") },
                                    selected = currentRoute == "list",
                                    onClick = {
                                        if (currentRoute != "list") {
                                            navController.navigate("list") {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "map",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("map") {
                            MapScreenContent(
                                trackingState = trackingState,
                                locations = locations,
                                seconds = seconds,
                                distance = distance,
                                userLocation = userLocation,
                                isMyLocationEnabled = isMyLocationEnabled,
                                onFabClick = {
                                    if (trackingState == TrackingState.STOPPED) {
                                        handleStartClick()
                                    } else {
                                        if (mapViewModel.locations.isNotEmpty()) {
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
                        composable("list") {
                            val routes by listViewModel.routes?.observeAsState(emptyList()) ?: remember { mutableStateOf(emptyList()) }
                            
                            LaunchedEffect(Unit) {
                                listViewModel.load {}
                            }

                            RouteListScreen(
                                routes = routes,
                                onRouteClick = { route ->
                                    val routeJson = Gson().toJson(route)
                                    val encodedJson = URLEncoder.encode(routeJson, StandardCharsets.UTF_8.toString())
                                    navController.navigate("detail/$encodedJson")
                                }
                            )
                        }
                        composable(
                            route = "detail/{routeJson}",
                            arguments = listOf(navArgument("routeJson") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val routeJson = backStackEntry.arguments?.getString("routeJson")
                            val decodedJson = URLDecoder.decode(routeJson ?: "", StandardCharsets.UTF_8.toString())
                            val route = Gson().fromJson(decodedJson, Route::class.java)
                            
                            val detailLocations by detailViewModel.locations.observeAsState(emptyList())
                            
                            LaunchedEffect(route) {
                                detailViewModel.load(route) {}
                            }

                            DetailScreenContent(
                                route = route,
                                locations = detailLocations,
                                onDelete = {
                                    detailViewModel.deleteRoute {
                                        navController.popBackStack()
                                    }
                                },
                                onShare = { snapshot ->
                                    shareImage(snapshot)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleStartClick() {
        if (verifyPermissionStatus()) {
            mapViewModel.clearData()
            adManager.loadAd()
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                prepareLocationService()
                mapViewModel.setTrackingState(TrackingState.RUNNING)
                Toast.makeText(this, getString(R.string.beginRoute), Toast.LENGTH_SHORT).show()
            } else {
                reviewGPSSettings()
            }
        }
    }

    private fun handleStopClick() {
        mapViewModel.setTrackingState(TrackingState.STOPPED)
        disconnectLocationService()
    }

    private fun prepareLocationService() {
        val gpsIntent = Intent(application, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(gpsIntent)
        } else {
            startService(gpsIntent)
        }
        application.bindService(gpsIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun disconnectLocationService() {
        gpsService?.stopTracking()
        try {
            val gpsIntent = Intent(application, LocationService::class.java)
            application.stopService(gpsIntent)
            application.unbindService(serviceConnection)
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
                    val locationsList = it as ArrayList<Location>
                    mapViewModel.updateLocations(locationsList)
                }
            }
            if (intent.action == Globals.TIME_INTENT_FILTER) {
                intent.extras?.get(Globals.TIMER_KEY)?.let {
                    val secondsCount = it as Int
                    mapViewModel.updateSeconds(secondsCount)
                }
            }
        }
    }

    private fun verifyPermissionStatus(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showLocationPermissionDialog(layoutInflater) {
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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_DENIED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
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
        for ((_, value) in permissionResults) {
            if (!value) accepted = false
        }
        if (accepted) {
            mapViewModel.setMyLocationEnabled(true)
            fetchLastLocation(mapViewModel)
            handleStartClick()
        } else {
            Toast.makeText(this, getString(R.string.accept_permisses), Toast.LENGTH_SHORT).show()
        }
    }

    private fun reviewGPSSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
                prepareLocationService()
                mapViewModel.setTrackingState(TrackingState.RUNNING)
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvable = exception as ResolvableApiException
                            val intentSenderRequest = IntentSenderRequest.Builder(resolvable.resolution.intentSender).build()
                            gpsSettingsLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private val gpsSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            prepareLocationService()
            mapViewModel.setTrackingState(TrackingState.RUNNING)
        }
    }

    private fun saveRoute(alias: String) {
        Toast.makeText(this, getString(R.string.saving), Toast.LENGTH_SHORT).show()
        mapViewModel.saveRoute(alias) {
            adManager.showAd(this)
        }
    }

    private fun shareImage(image: Bitmap) {
        StorageUtils.saveRouteImageToGallery(this, image)?.let { uri ->
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            startActivity(Intent.createChooser(share, getString(R.string.shareImage)))
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchLastLocation(viewModel: MapFragmentViewModel) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.updateUserLocation(it)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadCastReceiver, IntentFilter(Globals.NEW_LOCATION_INTENT_FILTER))
        LocalBroadcastManager.getInstance(this).registerReceiver(broadCastReceiver, IntentFilter(Globals.TIME_INTENT_FILTER))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadCastReceiver)
    }

    override fun onDestroy() {
        disconnectLocationService()
        super.onDestroy()
    }
}
