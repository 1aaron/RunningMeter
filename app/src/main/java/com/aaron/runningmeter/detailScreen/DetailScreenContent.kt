package com.aaron.runningmeter.detailScreen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.aaron.runningmeter.ui.theme.RunningMeterTheme
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import com.aaron.runningmeter.models.Route
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.util.Locale

@Preview(showSystemUi = true, name = "Light Mode")
@Composable
fun DetailScreenPreviewLight() {
    DetailScreenPreviewContent()
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun DetailScreenPreviewDark() {
    DetailScreenPreviewContent()
}

@Composable
private fun DetailScreenPreviewContent() {
    val dummyRoute = Route(
        id = 1,
        alias = "Evening Jog",
        time = 1850,
        distance = 3.2,
        date = "2023-10-25",
        speed = null
    )
    val dummyLocations = listOf(
        Locations(id = 0L, routeId = 0L, latitude = 37.7749, longitude = -122.4194),
        Locations(id = 0L, routeId = 0L, latitude = 37.7750, longitude = -122.4195)
    )
    RunningMeterTheme {
        DetailScreenContent(
            route = dummyRoute,
            locations = dummyLocations,
            onDelete = {},
            onShare = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenContent(
    route: Route,
    locations: List<Locations>,
    onDelete: () -> Unit,
    onShare: (Bitmap) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    var googleMapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Adjust camera to fit all locations when they are available and the map is ready
    LaunchedEffect(locations, mapLoaded) {
        if (locations.isNotEmpty() && mapLoaded) {
            val builder = LatLngBounds.Builder()
            locations.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            val bounds = builder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true },
            uiSettings = MapUiSettings(
                zoomGesturesEnabled = false,
                scrollGesturesEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = false
            ),
        ) {
            MapEffect(Unit) { map ->
                googleMapInstance = map
            }

            if (locations.isNotEmpty()) {
                Polyline(
                    points = locations.map { LatLng(it.latitude, it.longitude) },
                    color = Color.Blue,
                    width = 10f
                )
                Marker(
                    state = rememberUpdatedMarkerState(
                        position = LatLng(
                            locations.first().latitude,
                            locations.first().longitude
                        )
                    ),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)
                )
                Marker(
                    state = rememberUpdatedMarkerState(
                        position = LatLng(
                            locations.last().latitude,
                            locations.last().longitude
                        )
                    ),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Share FAB
            FloatingActionButton(
                onClick = {
                    googleMapInstance?.snapshot { bitmap ->
                        bitmap?.let { onShare(it) }
                    }
                },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share))
            }
            // Delete FAB
            FloatingActionButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
            }

            // Info footer
            Row(
                modifier = Modifier
                    //.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        id = R.string.distance,
                        route.distance ?: 0.0
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                val hours = (route.time ?: 0) / 3600
                val minutes = ((route.time ?: 0) % 3600) / 60
                val seconds = (route.time ?: 0) % 60
                val timeFormatted = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )
                Text(
                    text = stringResource(id = R.string.timeData, timeFormatted),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(id = R.string.confirm_delete)) },
            text = { Text(stringResource(id = R.string.delete_confirmation_message)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun StatItem(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
