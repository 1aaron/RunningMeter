package com.aaron.runningmeter.ListScreen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Route

@Composable
fun RouteListScreen(
    routes: List<Route>,
    onRouteClick: (Route) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.list_background))
    ) {
        if (routes.isEmpty()) {
            EmptyStateView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routes) { route ->
                    RouteItemCard(route = route, onClick = { onRouteClick(route) })
                }
            }
        }
    }
}

@Composable
fun RouteItemCard(route: Route, onClick: () -> Unit) {
    val hours: Int = (route.time ?: 1) / 3600
    var reminder = (route.time ?: 1) % 3600
    val minutes = reminder / 60
    reminder %= 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, reminder)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.mapa_marcador),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.Gray)) {
                            append("Route: ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.DarkGray)) {
                            append(route.alias ?: "Route")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append("Time: ")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.DarkGray)) {
                                append(timeFormatted)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = route.date ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(id = R.string.empty_routes_text),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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
fun RouteListPreview() {
    val dummyRoutes = listOf(
        Route(id = 1, alias = "Evening Jog", time = 1850, date = "2023-10-25"),
        Route(id = 2, alias = "Morning Run", time = 3600, date = "2023-10-26"),
        Route(id = 3, alias = "Short Walk", time = 600, date = "2023-10-27")
    )
    MaterialTheme {
        RouteListScreen(routes = dummyRoutes, onRouteClick = {})
    }
}
