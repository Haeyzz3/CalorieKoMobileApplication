package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import java.util.Locale

@Composable
fun ActivityDetailsScreen(activity: ActivityLogEntity, onBack: () -> Unit) {
    
    // Decode the path string back into Mapbox Points
    val points = remember(activity.encodedPath) {
        activity.encodedPath?.split("|")?.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) Point.fromLngLat(parts[1].toDouble(), parts[0].toDouble()) else null
        } ?: emptyList()
    }

    val formatTime = { seconds: Long -> 
        if (seconds < 3600) "%02d:%02d".format(seconds / 60, seconds % 60)
        else "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60) 
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF121212)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) 
                    }
                    Text(
                        text = if (!activity.activityTag.isNullOrBlank()) "[${activity.activityTag}] ${activity.name}" else activity.name, 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // User Header
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("CalorieKo Athlete", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(activity.timeString, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Text(
                text = activity.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (!activity.notes.isNullOrBlank()) {
                Text(
                    text = activity.notes,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                Column {
                    Text("Distance", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f km", activity.distanceKm ?: 0.0),
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
                // Pace
                Column {
                    Text("Pace", color = Color.Gray, fontSize = 12.sp)
                    val p = activity.pace ?: 0.0
                    val paceStr = if (p > 0 && p < 60) {
                        val pMin = p.toInt()
                        val pSec = ((p - pMin) * 60).toInt()
                        String.format(Locale.US, "%d:%02d /km", pMin, pSec)
                    } else "-:--"
                    Text(text = paceStr, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                // Time
                Column {
                    Text("Time", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = formatTime(activity.movingTimeSeconds ?: 0L),
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Map View
            if (points.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            com.mapbox.maps.MapView(ctx).apply {
                                mapboxMap.loadStyle(
                                    when (activity.mapType) {
                                        "Standard" -> Style.MAPBOX_STREETS
                                        "Terrain" -> Style.OUTDOORS
                                        else -> Style.DARK
                                    }
                                )
                                val polylineManager = annotations.createPolylineAnnotationManager()
                                polylineManager.create(
                                    PolylineAnnotationOptions()
                                        .withPoints(points)
                                        .withLineColor("#F97316")
                                        .withLineWidth(5.0)
                                )
                                // Center camera on route
                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(points[points.size / 2])
                                        .zoom(13.5)
                                        .build()
                                )
                                // Disable interactions for the static post look
                                setGesturesEnabled(false) 
                            }
                        }
                    )
                }
            }

            // Photo View
            if (!activity.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = activity.photoUri,
                    contentDescription = "Activity Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }
        }
    }
}

// Extension to disable gestures easily
fun com.mapbox.maps.MapView.setGesturesEnabled(enabled: Boolean) {
    this.gestures.apply {
        pitchEnabled = enabled
        scrollEnabled = enabled
        doubleTapToZoomInEnabled = enabled
        pinchToZoomEnabled = enabled
    }
}
