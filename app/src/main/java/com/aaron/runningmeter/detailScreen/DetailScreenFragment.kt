package com.aaron.runningmeter.detailScreen

import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Locations
import com.aaron.runningmeter.models.Route
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.io.OutputStream
import java.util.Locale

class DetailScreenFragment(val route: Route) : Fragment() {

    private lateinit var viewModel: DetailScreenViewModelInterface

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DetailScreenViewModel::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val locations by viewModel.locations.observeAsState(emptyList())
                    
                    LaunchedEffect(Unit) {
                        viewModel.load(route) {}
                    }

                    DetailScreenContent(
                        route = route,
                        locations = locations,
                        onDelete = {
                            viewModel.deleteRoute {
                                activity?.supportFragmentManager?.popBackStack()
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

    private fun shareImage(image: Bitmap) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "route")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        
        activity?.let { activity ->
            activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )?.let { uri ->
                try {
                    val outstream: OutputStream? = activity.contentResolver.openOutputStream(uri)
                    outstream?.use {
                        image.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                } catch (e: Exception) {
                    System.err.println(e.toString())
                }

                share.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(share, getString(R.string.shareImage)))
            }
        }
    }
}

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
                    state = rememberMarkerState(position = LatLng(locations.first().latitude, locations.first().longitude)),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.walk_marker)
                )
                Marker(
                    state = rememberMarkerState(position = LatLng(locations.last().latitude, locations.last().longitude)),
                    icon = BitmapDescriptorFactory.fromResource(R.drawable.flag_checkered)
                )
            }
        }

        // Overlays
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.8f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(id = R.string.routeName, route.alias ?: ""),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
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

@Preview(showSystemUi = true, name = "Light Mode")
@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun DetailScreenPreview() {
    val dummyRoute = Route(
        id = 1,
        alias = "Morning Run",
        distance = 5.2,
        time = 1850,
        date = "2023-10-25"
    )
    val dummyLocations = listOf(
        Locations(1, 1, 37.422, -122.084),
        Locations(2, 1, 37.423, -122.085),
        Locations(3, 1, 37.424, -122.086)
    )
    MaterialTheme {
        DetailScreenContent(
            route = dummyRoute,
            locations = dummyLocations,
            onDelete = {},
            onShare = {}
        )
    }
}
