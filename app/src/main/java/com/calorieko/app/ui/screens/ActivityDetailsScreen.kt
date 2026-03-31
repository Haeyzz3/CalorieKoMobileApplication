package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.calorieko.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(activity: ActivityLogEntity, onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember { auth.currentUser }
    var userName by remember { mutableStateOf("CalorieKo athlete") }

// 2. Fetch the user's name from the local database when the screen loads
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val userProfile = userDao.getUserProfile(uid) // Pass the required UID

            if (userProfile != null) {
                withContext(Dispatchers.Main) {
                    if (userProfile.name.isNotEmpty()) {
                        userName = userProfile.name
                    }
                }
            }
        }
    }

    // Decode the path string back into Mapbox Points
    val points = remember(activity.encodedPath) {
        activity.encodedPath?.split("|")?.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) Point.fromLngLat(parts[1].toDouble(), parts[0].toDouble()) else null
        } ?: emptyList()
    }

    // Format Time Duration
    val formatDuration = { seconds: Long ->
        if (seconds < 3600) "%d:%02d".format(seconds / 60, seconds % 60)
        else "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }

    // Format highly professional Date & Time (e.g., "March 16, 2026 at 11:53 AM")
    val displayDate = remember(activity.timestamp) {
        val sdf = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        sdf.format(Date(activity.timestamp))
    }

    // Clean up title and tags (Removes redundant brackets if they were saved in the DB)
    val cleanTitle = activity.name.replace(Regex("^\\[.*?]\\s*"), "")
    val cleanTag = activity.activityTag?.replace("[", "")?.replace("]", "")?.trim()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    scrolledContainerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                // User Profile Header
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2A2A2A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = displayDate, color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                }
            }

            item {
                // Title and Professional Tag Pill
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = cleanTitle,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp
                    )

                    // Pill Badge (Only shows if tag exists, with brackets cleanly removed)
                    if (!cleanTag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = CalorieKoOrange,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = cleanTag,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Private Notes
                    if (!activity.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = activity.notes,
                            color = Color(0xFFE0E0E0),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            item {
                // Main Stats Row (Clean, whitespace-driven layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Distance
                    StatBlock(
                        label = "Distance",
                        value = String.format(Locale.US, "%.2f", activity.distanceKm ?: 0.0),
                        unit = "km"
                    )

                    // Pace
                    val p = activity.pace ?: 0.0
                    val paceStr = if (p > 0 && p < 60) {
                        val pMin = p.toInt()
                        val pSec = ((p - pMin) * 60).toInt()
                        String.format(Locale.US, "%d:%02d", pMin, pSec)
                    } else "-:--"

                    StatBlock(
                        label = "Pace",
                        value = paceStr,
                        unit = "/km"
                    )

                    // Time
                    StatBlock(
                        label = "Time",
                        value = formatDuration(activity.movingTimeSeconds ?: 0L),
                        unit = ""
                    )
                }
            }

            // Map View - Edge to Edge
            if (points.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp) // Taller map for premium feel
                            .background(Color(0xFF1E1E1E))
                    ) {
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
                                            .withLineColor("#F97316") // CalorieKo Orange
                                            .withLineWidth(6.0)
                                    )

                                    if (points.isNotEmpty()) {
                                        mapboxMap.setCamera(
                                            CameraOptions.Builder()
                                                .center(points[points.size / 2])
                                                .zoom(13.5)
                                                .build()
                                        )
                                    }
                                    // Disable interactions for the static post look
                                    setGesturesEnabled(false)
                                }
                            }
                        )
                    }
                }
            }

            // Photo View - Edge to Edge
            if (!activity.photoUri.isNullOrBlank()) {
                item {
                    val bitmap = if (activity.photoUri.startsWith(ImageUtils.BASE64_PREFIX)) {
                        ImageUtils.decodeBase64ToBitmap(activity.photoUri)
                    } else null

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Activity Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 350.dp, max = 500.dp)
                        )
                    } else {
                        AsyncImage(
                            model = activity.photoUri,
                            contentDescription = "Activity Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 350.dp, max = 500.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


// Reusable Stat Block using Baseline Alignment to stop units from "floating"
@Composable
fun StatBlock(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, color = Color(0xFFAAAAAA), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Row {
            Text(
                text = value,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline() // Aligns text cleanly to the bottom baseline
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp)
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

