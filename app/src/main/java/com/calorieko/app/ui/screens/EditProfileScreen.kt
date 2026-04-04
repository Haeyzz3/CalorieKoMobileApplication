package com.calorieko.app.ui.screens


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.viewmodel.EditProfileViewModel


// ─── Brand colors (matching ProfileScreen) ───
private val CalorieKoOrange = Color(0xFFFDB05E)
private val CalorieKoDeepOrange = Color(0xFFFF9800)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF6B7280)
private val FieldBg = Color(0xFFF9FAFB)
private val FieldBorder = Color(0xFFE5E7EB)
private val PageBg = Color(0xFFF8F9FA)

// ─── Goal option for the selector ───
private data class GoalOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // ── Collect ViewModel State ──
    val name by viewModel.name.collectAsState()
    val age by viewModel.age.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val sex by viewModel.sex.collectAsState()
    val selectedGoal by viewModel.selectedGoal.collectAsState()
    val selectedActivityLevel by viewModel.selectedActivityLevel.collectAsState()
    val existingPhotoUrl by viewModel.existingPhotoUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ── Local UI State (photo picker) ──
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // ── Handle one-shot events ──
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProfileViewModel.Event.SaveSuccess -> onSave()
                is EditProfileViewModel.Event.SaveError -> { /* Could show a toast here */ }
            }
        }
    }


    val goals = remember {
        listOf(
            GoalOption(
                "gain_muscle",
                "Gain Muscle",
                "Build lean muscle through optimized protein and nutrition",
                Icons.Default.FitnessCenter,
                Color(0xFFEF4444),
                Color(0xFFFEE2E2)
            ),
            GoalOption(
                "weight_loss",
                "Weight Control",
                "Achieve and maintain your ideal weight",
                Icons.Default.MonitorWeight,
                Color(0xFF2563EB),
                Color(0xFFDBEAFE)
            ),
            GoalOption(
                "general",
                "General Health & Wellness",
                "Build healthy eating habits",
                Icons.Default.Favorite,
                Color(0xFF16A34A),
                Color(0xFFDCFCE7)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ═══════════════════════ HEADER ═══════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                    )
                )
        ) {
            Column {
                // Top bar with back arrow
                TopAppBar(
                    title = {
                        Text(
                            "Edit Profile",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                // Avatar section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Replace the existing avatar Surface with this:
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(100.dp)
                                .clickable { photoPickerLauncher.launch("image/*") } // Triggers the gallery
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (existingPhotoUrl.isNotEmpty()) {
                                val bitmap = remember(existingPhotoUrl) {
                                    if (existingPhotoUrl.startsWith(ImageUtils.BASE64_PREFIX)) {
                                        ImageUtils.decodeBase64ToBitmap(existingPhotoUrl)
                                    } else null
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Current Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Show existing profile photo from backend (fallback)
                                    AsyncImage(
                                        model = existingPhotoUrl,
                                        contentDescription = "Current Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = CalorieKoGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        // Camera badge
                        Surface(
                            shape = CircleShape,
                            color = CalorieKoOrange,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Tap to change photo",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ═══════════════════════ FORM ═══════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ─── Personal Info Section ───
            SectionTitle("Personal Information")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditField(
                        label = "Full Name",
                        value = name,
                        onValueChange = { viewModel.updateName(it) },
                        icon = Icons.Default.Person,
                        iconColor = CalorieKoGreen,
                        iconBg = Color(0xFFECFDF5)
                    )
                }
            }

            // ─── Sex Selection ───
            SectionTitle("Sex")

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SexOptionButton(
                    text = "Male",
                    isSelected = sex == "Male",
                    onClick = { viewModel.updateSex("Male") },
                    modifier = Modifier.weight(1f)
                )
                SexOptionButton(
                    text = "Female",
                    isSelected = sex == "Female",
                    onClick = { viewModel.updateSex("Female") },
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── Baseline Metrics Section ───
            SectionTitle("Baseline Metrics")

            // Weight & Height row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EditMetricCard(
                    label = "Weight",
                    value = weight,
                    onValueChange = { viewModel.updateWeight(it) },
                    unit = "kg",
                    icon = Icons.Default.MonitorWeight,
                    iconColor = CalorieKoGreen,
                    iconBg = Color(0xFFECFDF5),
                    modifier = Modifier.weight(1f)
                )
                EditMetricCard(
                    label = "Height",
                    value = height,
                    onValueChange = { viewModel.updateHeight(it) },
                    unit = "cm",
                    icon = Icons.Default.Straighten,
                    iconColor = Color(0xFF2563EB),
                    iconBg = Color(0xFFEFF6FF),
                    modifier = Modifier.weight(1f)
                )
            }

            // Age card (full width)
            EditMetricCard(
                label = "Age",
                value = age,
                onValueChange = { viewModel.updateAge(it) },
                unit = "years",
                icon = Icons.Default.Cake,
                iconColor = Color(0xFF9333EA),
                iconBg = Color(0xFFFAF5FF),
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Number
            )

            // ─── Activity Level Section ───
            SectionTitle("Activity Level")

            Text(
                "What is your baseline activity level?",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(start = 4.dp)
            )

            Text(
                "Not including workouts - we count that separately.",
                fontSize = 12.sp,
                color = TextGray.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityLevelOption(
                    title = "Not Very Active",
                    description = "Spend most of the day sitting (e.g. bankteller, desk job)",
                    isSelected = selectedActivityLevel == "not_very_active",
                    onClick = { viewModel.updateActivityLevel("not_very_active") }
                )
                ActivityLevelOption(
                    title = "Lightly Active",
                    description = "Spend a good part of the day on your feet (e.g. teacher, salesperson)",
                    isSelected = selectedActivityLevel == "lightly_active",
                    onClick = { viewModel.updateActivityLevel("lightly_active") }
                )
                ActivityLevelOption(
                    title = "Active",
                    description = "Spend a good part of the day doing some physical activity (e.g. food server, postal carrier)",
                    isSelected = selectedActivityLevel == "active",
                    onClick = { viewModel.updateActivityLevel("active") }
                )
                ActivityLevelOption(
                    title = "Very Active",
                    description = "Spend most of the day doing heavy physical activity (e.g. bike messenger, carpenter)",
                    isSelected = selectedActivityLevel == "very_active",
                    onClick = { viewModel.updateActivityLevel("very_active") }
                )
            }

            // ─── Health Goal Section ───
            SectionTitle("Health Goal")

            Text(
                "Select your primary health focus",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goals.forEach { goal ->
                    GoalOptionCard(
                        goal = goal,
                        isSelected = selectedGoal == goal.id,
                        onClick = { viewModel.updateGoal(goal.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Save Button (Orange Gradient) ───
            Button(
                onClick = {
                    viewModel.saveProfile(context, selectedImageUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading // Disable button while loading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isLoading) listOf(Color.Gray, Color.LightGray)
                                else listOf(CalorieKoOrange, CalorieKoDeepOrange)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isLoading) {
                            Text(
                                "Saving...",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Save Changes",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ─── Cancel link ───
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextGray
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════ HELPER COMPOSABLES ═══════════════════════

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CalorieKoGreen,
                unfocusedBorderColor = FieldBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = FieldBg,
                cursorColor = CalorieKoGreen
            )
        )
    }
}

@Composable
private fun EditMetricCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Label row with icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
            }

            // Input field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                suffix = {
                    Text(
                        unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalorieKoGreen
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalorieKoGreen,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = FieldBg,
                    cursorColor = CalorieKoGreen
                )
            )
        }
    }
}

@Composable
private fun SexOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CalorieKoGreen else Color(0xFFE5E7EB),
        animationSpec = tween(300),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CalorieKoGreen.copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(300),
        label = "bg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        color = bgColor,
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isSelected) CalorieKoGreen else TextGray
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(CalorieKoGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLevelOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CalorieKoGreen else Color(0xFFE5E7EB),
        animationSpec = tween(300),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CalorieKoGreen.copy(alpha = 0.05f) else Color.White,
        animationSpec = tween(300),
        label = "bg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) CalorieKoGreen.copy(alpha = 0.15f)
                        else Color(0xFFF3F4F6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = if (isSelected) CalorieKoGreen else TextGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )
            }

            // Radio-style indicator
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CalorieKoGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .then(
                            Modifier.background(Color.Transparent)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(2.dp, Color(0xFFD1D5DB)),
                        color = Color.Transparent,
                        modifier = Modifier.size(24.dp)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun GoalOptionCard(
    goal: GoalOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) goal.color else Color(0xFFE5E7EB),
        animationSpec = tween(300),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) goal.bgColor.copy(alpha = 0.5f) else Color.White,
        animationSpec = tween(300),
        label = "bg"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "check"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(goal.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    goal.icon,
                    contentDescription = null,
                    tint = goal.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark
                )
                Text(
                    goal.subtitle,
                    fontSize = 12.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )
            }

            // Checkmark
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(goal.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
