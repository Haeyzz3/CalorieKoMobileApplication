package com.calorieko.app.ui.screens


// Make sure to add these imports at the top:
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.calorieko.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.auth.FirebaseAuth
import com.calorieko.app.ui.components.BottomNavigation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import java.util.Locale

// --- Data Models ---
data class UserData(
    val name: String = "Juan Dela Cruz",
    val memberSince: String = "January 2025",
    val streak: Int = 7,
    val level: Int = 5,
    val age: Int = 25,
    val height: Double = 170.0, // cm
    val weight: Double = 70.0,  // kg
    val sex: String = "Male",
    val activityLevel: String = "lightly_active",
    val goal: String = "general"
)

data class Badge(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val colorBg: Color,
    val colorIcon: Color,
    val earned: Boolean,
    val progress: Int,
    val max: Int
)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    onEditProfile: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf("profile") }
    val scrollState = rememberScrollState()

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val fullName = currentUser?.displayName ?: "User"
    val profileImageUrl = currentUser?.photoUrl

    // 1. Initialize Database Access
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val userDao = db.userDao()

    // 2. Change Mock Data to a MutableState
    var userData by remember { mutableStateOf(UserData(name = fullName)) }

    // 3. Fetch Real Data on Load
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            withContext(Dispatchers.IO) {
                val profile = userDao.getUser(uid)
                if (profile != null) {
                    // Update the UI state with the real database values
                    userData = userData.copy(
                        name = profile.name,
                        age = profile.age,
                        height = profile.height,
                        weight = profile.weight,
                        sex = profile.sex.ifEmpty { "Male" },
                        activityLevel = profile.activityLevel.ifEmpty { "lightly_active" },
                        goal = profile.goal.ifEmpty { "general" }
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 1. Header Section (Pass the image URL here)
            ProfileHeader(user = userData, profileImageUrl = profileImageUrl)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 2. Baseline Metrics Grid
                BaselineMetricsGrid(userData)

                // 3. Health Goals Section
                HealthGoalsSection(userData.goal, onEditProfile = onEditProfile)

                // 4. Milestones & Badges
                MilestonesSection(userData.streak)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- 1. Header with Animated Streak Ring ---
// --- 1. Header with Animated Streak Ring ---
@Composable
fun ProfileHeader(user: UserData, profileImageUrl: android.net.Uri? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                )
            )
            .padding(top = 32.dp, bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated Streak Ring Profile
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                // Animation State
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    animatedProgress.animateTo(
                        targetValue = user.streak / 30f,
                        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
                    )
                }

                Canvas(modifier = Modifier.size(130.dp)) {
                    // Background Ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Progress Ring
                    drawArc(
                        color = Color(0xFFFFD700), // Gold
                        startAngle = -90f,
                        sweepAngle = animatedProgress.value * 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Avatar
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Check if Google Image exists, otherwise show default icon
                        if (profileImageUrl != null) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Level Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent, // Gradient handled by box
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFC107), Color(0xFFFF9800))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(
                                "Level ${user.level}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Info
            Text(
                text = user.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                Text(
                    "Member since ${user.memberSince}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Streak Chip
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Text("${user.streak} Day Streak!", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// --- 2. Baseline Metrics Grid ---
@Composable
fun BaselineMetricsGrid(user: UserData) {
    // Calculate BMI
    val heightInMeters = user.height / 100
    val bmi = user.weight / (heightInMeters * heightInMeters)
    val bmiRounded = String.format(Locale.US, "%.1f", bmi)

    val bmiInfo = when {
        bmi < 18.5 -> Pair("Underweight", Color(0xFF2563EB)) // Blue
        bmi < 25 -> Pair("Normal", Color(0xFF16A34A)) // Green
        bmi < 30 -> Pair("Overweight", Color(0xFFEA580C)) // Orange
        else -> Pair("Obese", Color(0xFFDC2626)) // Red
    }

    Column {
        Text(
            "Baseline Metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Weight
            MetricCard(
                title = "Current Weight",
                value = "${user.weight}",
                unit = "kilograms",
                icon = Icons.Default.MonitorWeight,
                iconColor = CalorieKoGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
            // Height
            MetricCard(
                title = "Height",
                value = "${user.height}",
                unit = "centimeters",
                icon = Icons.Default.Straighten, // Or Height icon if available in extended
                iconColor = Color(0xFF2563EB),
                bgColor = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Age
            MetricCard(
                title = "Age",
                value = "${user.age}",
                unit = "years old",
                icon = Icons.Default.Cake,
                iconColor = Color(0xFF9333EA),
                bgColor = Color(0xFFFAF5FF),
                modifier = Modifier.weight(1f)
            )
            // BMI
            MetricCard(
                title = "BMI",
                value = bmiRounded,
                unit = bmiInfo.first,
                unitColor = bmiInfo.second,
                icon = Icons.Default.MonitorHeart,
                iconColor = Color(0xFFEA580C),
                bgColor = Color(0xFFFFF7ED),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Level Card (full width)
        val activityLabel = when (user.activityLevel) {
            "not_very_active" -> "Not Very Active"
            "lightly_active" -> "Lightly Active"
            "active" -> "Active"
            "very_active" -> "Very Active"
            else -> "Not Set"
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFF7ED), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsRun,
                        null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Activity Level",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563)
                    )
                    Text(
                        activityLabel,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    unitColor: Color = Color.Gray
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(bgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(
                unit,
                fontSize = 12.sp,
                fontWeight = if (unitColor != Color.Gray) FontWeight.Bold else FontWeight.Normal,
                color = unitColor
            )
        }
    }
}

// --- 3. Health Goals Section ---
@Composable
fun HealthGoalsSection(goalCode: String, onEditProfile: () -> Unit = {}) {
    val goalInfo = when (goalCode) {
        "gain_muscle" -> Triple("Gain Muscle", Icons.Default.FitnessCenter, Color(0xFFEF4444))
        "weight_loss" -> Triple("Weight Control", Icons.Default.MonitorWeight, Color(0xFF2563EB))
        "diabetes" -> Triple("Diabetes Management", Icons.Default.MonitorHeart, Color(0xFF9333EA))
        "fitness" -> Triple("Fitness & Performance", Icons.Default.DirectionsRun, Color(0xFF9333EA))
        else -> Triple("General Health & Wellness", Icons.Default.TrackChanges, Color(0xFF16A34A))
    }

    val description = when(goalCode) {
        "gain_muscle" -> "Building lean muscle through optimized protein intake and strength-focused nutrition."
        "weight_loss" -> "Achieving sustainable weight loss through calorie management and portion control."
        "diabetes" -> "Maintaining stable blood sugar levels through carbohydrate tracking and meal timing."
        "fitness" -> "Optimizing nutrition for peak athletic performance and recovery."
        else -> "Building healthy eating habits and maintaining overall wellness."
    }

    Column {
        Text(
            "Health Goals",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(goalInfo.third.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(goalInfo.second, null, tint = goalInfo.third, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Primary Focus", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Text(goalInfo.first, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description Box
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Button
                OutlinedButton(
                    onClick = { onEditProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, CalorieKoGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CalorieKoGreen)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 4. Milestones Section ---
@Composable
fun MilestonesSection(currentStreak: Int) {
    // State for badge detail dialog
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    // Mock Badges
    val badges = remember { listOf(
        Badge(1, "Sodium Defender", "3 Days Under Limit", Icons.Default.WaterDrop, Color(0xFFDBEAFE), Color(0xFF2563EB), true, 3, 3),
        Badge(2, "Scale Pro", "10 Meals Logged", Icons.Default.MonitorWeight, Color(0xFFDCFCE7), Color(0xFF16A34A), true, 10, 10),
        Badge(3, "Streak Master", "7 Day Streak", Icons.Default.LocalFireDepartment, Color(0xFFFFEDD5), Color(0xFFEA580C), true, 7, 7),
        Badge(4, "Photo Logger", "15 Photos Taken", Icons.Default.CameraAlt, Color(0xFFF3E8FF), Color(0xFF9333EA), false, 8, 15),
        Badge(5, "Workout Warrior", "5 Workouts Logged", Icons.Default.Bolt, Color(0xFFFEF3C7), Color(0xFFD97706), false, 2, 5),
        Badge(6, "Health Champion", "30 Day Streak", Icons.Default.MilitaryTech, Color(0xFFFCE7F3), Color(0xFFDB2777), false, 7, 30)
    )}

    val earned = badges.filter { it.earned }
    val inProgress = badges.filter { !it.earned }

    // Badge Detail Dialog
    selectedBadge?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            onDismiss = { selectedBadge = null }
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Milestones & Achievements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${earned.size}/${badges.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
            }
        }

        // Earned Badges Grid
        if (earned.isNotEmpty()) {
            Text("Earned Badges", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                earned.forEach { badge ->
                    EarnedBadgeCard(
                        badge = badge,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // In Progress List
        if (inProgress.isNotEmpty()) {
            Text("In Progress", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                inProgress.forEach { badge ->
                    InProgressBadgeCard(
                        badge = badge,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Motivation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Keep Going! ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You're making great progress! Complete more activities to unlock new badges and climb the leaderboard.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EarnedBadgeCard(badge: Badge, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(badge.colorBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, null, tint = badge.colorIcon, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(badge.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), maxLines = 1)
            Text(badge.description, fontSize = 10.sp, color = Color(0xFF6B7280), maxLines = 1)

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(50)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFF15803D), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Earned", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
            }
        }
    }
}

@Composable
fun InProgressBadgeCard(badge: Badge, onClick: () -> Unit = {}) {
    // Animate Progress Bar
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = badge.progress.toFloat() / badge.max.toFloat(),
            animationSpec = tween(durationMillis = 1000, delayMillis = 200, easing = LinearOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(badge.colorBg.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, null, tint = badge.colorIcon, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(badge.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                Text(badge.description, fontSize = 12.sp, color = Color(0xFF6B7280))

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5E7EB))) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.value)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("${badge.progress}/${badge.max} completed", fontSize = 11.sp, color = Color(0xFF4B5563))
            }
        }
    }
}

// ═══════════════════════ BADGE DETAIL DIALOG ═══════════════════════
@Composable
fun BadgeDetailDialog(badge: Badge, onDismiss: () -> Unit) {
    val progressFraction = badge.progress.toFloat() / badge.max.toFloat()
    val progressPercent = (progressFraction * 100).toInt()

    // Animated progress for the dialog
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(badge.id) {
        animatedProgress.animateTo(
            targetValue = progressFraction,
            animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
        )
    }

    // Detailed descriptions per badge
    val detailDescription = when (badge.id) {
        1 -> "Stay under your daily sodium limit to protect your heart health. Each day you stay under the recommended intake counts toward this badge."
        2 -> "Log your meals using the smart scale to track accurate nutrition data. The more meals you log, the better your insights become."
        3 -> "Maintain a daily streak by logging at least one meal every day. Consistency is the key to achieving your health goals!"
        4 -> "Take photos of your meals for AI-powered food recognition. This helps build a visual diary of your nutrition journey."
        5 -> "Log your workouts to see how exercise impacts your daily calorie balance. Every workout brings you closer to your fitness goals."
        6 -> "The ultimate achievement! Maintain a 30-day streak to prove your dedication to a healthier lifestyle."
        else -> "Complete the challenge to earn this badge."
    }

    // How to earn / next step tip
    val tip = if (badge.earned) {
        " Congratulations! You've earned this badge through your dedication and consistency."
    } else {
        when (badge.id) {
            4 -> "📸 Tip: Use the camera button on the Log Meal screen to snap a photo of your next meal!"
            5 -> "💪 Tip: Head to Log Workout and record your next exercise session."
            6 -> "🔥 Tip: Keep your daily streak alive! Log at least one meal each day to build toward 30 days."
            else -> "Keep going! You're ${badge.max - badge.progress} away from earning this badge."
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Badge Icon (large) ──
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (badge.earned) badge.colorBg else badge.colorBg.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        badge.icon,
                        contentDescription = null,
                        tint = badge.colorIcon,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Badge Name ──
                Text(
                    badge.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Status chip ──
                Surface(
                    color = if (badge.earned) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (badge.earned) Icons.Default.EmojiEvents else Icons.Default.TrackChanges,
                            null,
                            tint = if (badge.earned) Color(0xFF15803D) else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (badge.earned) "Badge Earned!" else "In Progress",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.earned) Color(0xFF15803D) else Color(0xFFD97706)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Description ──
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        detailDescription,
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Progress Section ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Progress",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            "${badge.progress}/${badge.max} ($progressPercent%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = badge.colorIcon
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress.value)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(badge.colorIcon, badge.colorIcon.copy(alpha = 0.7f))
                                    ),
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Tip / Encouragement ──
                Surface(
                    color = if (badge.earned) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        tip,
                        fontSize = 13.sp,
                        color = if (badge.earned) Color(0xFF065F46) else Color(0xFF92400E),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Close Button ──
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = CalorieKoGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Got it!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}