package com.aaron.runningmeter.detailScreen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(locations, mapLoaded) {
        if (locations.isNotEmpty() && mapLoaded) {
            val builder = LatLngBounds.Builder()
            locations.forEach {
                builder.include(LatLng(it.latitude, it.longitude))
            }
            val bounds = builder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true }
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
                    state = rememberUpdatedMarkerState(position = LatLng(locations.first().latitude, locations.first().longitude)),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)
                )
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(locations.last().latitude, locations.last().longitude)),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(text = stringResource(id = R.string.distance, route.distance ?: 0.0))
            
            val hours: Int = (route.time ?: 1) / 3600
            var reminder = (route.time ?: 1) % 3600
            val minutes = reminder / 60
            reminder %= 60
            val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, reminder)
            StatItem(text = stringResource(id = R.string.timeData, timeFormatted))
        }

        FloatingActionButton(
            onClick = {
                googleMapInstance?.snapshot { bitmap ->
                    bitmap?.let { onShare(it) }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
        }

        FloatingActionButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFFF44336),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
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
