package com.aaron.runningmeter.mapScreen

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaron.runningmeter.R
import com.aaron.runningmeter.ui.theme.RunningMeterTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.util.Locale

@Composable
fun MapScreenContent(
    trackingState: TrackingState,
    locations: List<Location>,
    seconds: Int,
    distance: Double,
    userLocation: Location?,
    isMyLocationEnabled: Boolean,
    onFabClick: () -> Unit,
    showAliasDialog: Boolean,
    onDismissAliasDialog: () -> Unit,
    onSaveRoute: (String) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    var hasCentredOnInitialLocation by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation) {
        if (userLocation != null && !hasCentredOnInitialLocation && locations.isEmpty()) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(userLocation.latitude, userLocation.longitude),
                18f
            )
            hasCentredOnInitialLocation = true
        }
    }
    
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
            properties = MapProperties(isMyLocationEnabled = isMyLocationEnabled),
            uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    RunningMeterTheme {
        MapScreenContent(
            trackingState = TrackingState.STOPPED,
            locations = emptyList(),
            seconds = 1850,
            distance = 5.2,
            userLocation = null,
            isMyLocationEnabled = false,
            onFabClick = {},
            showAliasDialog = false,
            onDismissAliasDialog = {},
            onSaveRoute = {}
        )
    }
}
