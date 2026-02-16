package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ─── DATA MODELS ─────────────────────────────────────────

data class Badge(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val earned: Boolean,
    val progress: Int,
    val goal: Int
)

data class GoalInfo(
    val name: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val description: String
)

// ─── PROFILE SCREEN ──────────────────────────────────────

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("profile") }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Mock user data
    val userName = "Juan Dela Cruz"
    val memberSince = "January 2025"
    val currentStreak = 7
    val level = 5
    val weight = 70f
    val height = 170f
    val age = 25
    val goal = "hypertension"

    // Calculate BMI
    val heightInMeters = height / 100f
    val bmi = weight / (heightInMeters * heightInMeters)
    val bmiRounded = String.format("%.1f", bmi)
    val bmiCategory = when {
        bmi < 18.5f -> "Underweight" to Color(0xFF2563EB)
        bmi < 25f -> "Normal" to Color(0xFF16A34A)
        bmi < 30f -> "Overweight" to Color(0xFFEA580C)
        else -> "Obese" to Color(0xFFDC2626)
    }

    // Goal info
    val goalInfo = when (goal) {
        "hypertension" -> GoalInfo(
            "Hypertension Management", Icons.Default.Favorite,
            Color(0xFFFEF2F2), Color(0xFFDC2626),
            "Managing sodium intake and tracking blood pressure through balanced nutrition."
        )
        "weight_loss" -> GoalInfo(
            "Weight Control", Icons.Default.FitnessCenter,
            Color(0xFFEFF6FF), Color(0xFF2563EB),
            "Achieving sustainable weight loss through calorie management and portion control."
        )
        "diabetes" -> GoalInfo(
            "Diabetes Management", Icons.Default.MonitorHeart,
            Color(0xFFF5F3FF), Color(0xFF7C3AED),
            "Maintaining stable blood sugar levels through carbohydrate tracking and meal timing."
        )
        else -> GoalInfo(
            "General Health & Wellness", Icons.Default.CheckCircle,
            Color(0xFFF0FDF4), Color(0xFF16A34A),
            "Building healthy eating habits and maintaining overall wellness."
        )
    }

    // Badges
    val badges = listOf(
        Badge(1, "Sodium Defender", "3 Days Under Limit", Icons.Default.WaterDrop,
            Color(0xFFDBEAFE), Color(0xFF2563EB), true, 3, 3),
        Badge(2, "Scale Pro", "10 Meals Logged", Icons.Default.FitnessCenter,
            Color(0xFFDCFCE7), Color(0xFF16A34A), true, 10, 10),
        Badge(3, "Streak Master", "7 Day Streak", Icons.Default.LocalFireDepartment,
            Color(0xFFFFEDD5), Color(0xFFEA580C), true, 7, 7),
        Badge(4, "Photo Logger", "15 Photos Taken", Icons.Default.CameraAlt,
            Color(0xFFEDE9FE), Color(0xFF7C3AED), false, 8, 15),
        Badge(5, "Workout Warrior", "5 Workouts Logged", Icons.Default.Bolt,
            Color(0xFFFEF3C7), Color(0xFFD97706), false, 2, 5),
        Badge(6, "Health Champion", "30 Day Streak", Icons.Default.EmojiEvents,
            Color(0xFFFCE7F3), Color(0xFFDB2777), false, 7, 30)
    )

    val earnedBadges = badges.filter { it.earned }
    val inProgressBadges = badges.filter { !it.earned }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ═══════════════════════════════════════════════
            // HEADER SECTION (Green gradient)
            // ═══════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                        )
                    )
                    .padding(top = 32.dp, bottom = 48.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture with Streak Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(128.dp)
                    ) {
                        // Streak ring (gold arc)
                        val streakProgress = currentStreak / 30f
                        Canvas(modifier = Modifier.size(128.dp)) {
                            // Background ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = size.minDimension / 2f - 4f,
                                style = Stroke(width = 8f)
                            )
                            // Progress arc
                            drawArc(
                                color = Color(0xFFFFD700),
                                startAngle = -90f,
                                sweepAngle = 360f * streakProgress,
                                useCenter = false,
                                style = Stroke(width = 8f, cap = StrokeCap.Round)
                            )
                        }

                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }

                        // Level badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFBBF24), Color(0xFFEAB308))
                                        ),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "Level $level",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User name
                    Text(
                        userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Member since
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            "Member since $memberSince",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak pill
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFCD34D)
                            )
                            Text(
                                "$currentStreak Day Streak!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════
            // CONTENT SECTIONS
            // ═══════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-24).dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── 1. Baseline Metrics Grid ──
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, easing = EaseOutCubic)
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Baseline Metrics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 2×2 Grid
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.FitnessCenter,
                                    iconBg = Color(0xFFF0FDF4),
                                    iconColor = Color(0xFF4CAF50),
                                    label = "Current Weight",
                                    value = "${weight.roundToInt()}",
                                    unit = "kilograms"
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Straighten,
                                    iconBg = Color(0xFFEFF6FF),
                                    iconColor = Color(0xFF2563EB),
                                    label = "Height",
                                    value = "${height.roundToInt()}",
                                    unit = "centimeters"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Cake,
                                    iconBg = Color(0xFFF5F3FF),
                                    iconColor = Color(0xFF7C3AED),
                                    label = "Age",
                                    value = "$age",
                                    unit = "years old"
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.MonitorHeart,
                                    iconBg = Color(0xFFFFF7ED),
                                    iconColor = Color(0xFFEA580C),
                                    label = "BMI",
                                    value = bmiRounded,
                                    unit = bmiCategory.first,
                                    unitColor = bmiCategory.second
                                )
                            }
                        }
                    }
                }

                // ── 2. Health Goals Section ──
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 100, easing = EaseOutCubic)
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Health Goals",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                // Goal icon + name
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(goalInfo.bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            goalInfo.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = goalInfo.iconColor
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Primary Focus",
                                            fontSize = 14.sp,
                                            color = Color(0xFF4B5563)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            goalInfo.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Goal description
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        goalInfo.description,
                                        fontSize = 14.sp,
                                        color = Color(0xFF374151),
                                        modifier = Modifier.padding(16.dp),
                                        lineHeight = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Edit Profile button
                                OutlinedButton(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Edit Profile",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. Milestones & Achievements ──
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 200, easing = EaseOutCubic)
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Section header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Milestones & Achievements",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFD97706)
                                )
                                Text(
                                    "${earnedBadges.size}/${badges.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                            }
                        }

                        // ── Earned Badges (3-column grid) ──
                        if (earnedBadges.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Earned Badges",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // 3 badges per row
                                val rows = earnedBadges.chunked(3)
                                rows.forEach { rowBadges ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowBadges.forEach { badge ->
                                            EarnedBadgeCard(
                                                badge = badge,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // Fill remaining slots
                                        repeat(3 - rowBadges.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // ── In Progress Badges ──
                        if (inProgressBadges.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "In Progress",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                inProgressBadges.forEach { badge ->
                                    InProgressBadgeCard(badge = badge)
                                }
                            }
                        }

                        // ── Motivation Card ──
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Column {
                                        Text(
                                            "Keep Going! 🎉",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
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
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── METRIC CARD ─────────────────────────────────────────

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    label: String,
    value: String,
    unit: String,
    unitColor: Color = Color(0xFF6B7280)
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                }
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                unit,
                fontSize = 12.sp,
                fontWeight = if (unitColor != Color(0xFF6B7280)) FontWeight.Medium else FontWeight.Normal,
                color = unitColor
            )
        }
    }
}

// ─── EARNED BADGE CARD ───────────────────────────────────

@Composable
fun EarnedBadgeCard(badge: Badge, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(badge.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = badge.iconColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                badge.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                badge.description,
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // "Earned" badge
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF15803D)
                    )
                    Text(
                        "Earned",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

// ─── IN-PROGRESS BADGE CARD ──────────────────────────────

@Composable
fun InProgressBadgeCard(badge: Badge) {
    val progressPercent = badge.progress.toFloat() / badge.goal.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(badge.bgColor.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = badge.iconColor.copy(alpha = 0.6f)
                )
            }

            // Name + progress bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    badge.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    badge.description,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE5E7EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${badge.progress}/${badge.goal} completed",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}
