package com.calorieko.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── DATA MODELS ─────────────────────────────────────────

data class Recipe(
    val id: String,
    val name: String,
    val calories: Int,
    val sodium: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val ingredients: List<String>,
    val category: String, // "ready" or "almost"
    val missingIngredients: List<String> = emptyList()
)

data class PlannedMeal(
    val day: Int,
    val recipe: Recipe
)

// ─── RECIPE DATA (from Figma) ────────────────────────────

val ALL_RECIPES = listOf(
    Recipe("1", "Pork Adobo", 380, 890, 32, 12, 24,
        listOf("Pork", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves"), "ready"),
    Recipe("2", "Chicken Adobo", 320, 820, 38, 10, 18,
        listOf("Chicken", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves"), "ready"),
    Recipe("3", "Sinigang na Baboy", 420, 650, 28, 35, 16,
        listOf("Pork", "Tamarind", "Tomatoes", "Onions", "Kangkong"), "almost",
        listOf("Kangkong")),
    Recipe("4", "Tinola", 280, 520, 30, 18, 12,
        listOf("Chicken", "Ginger", "Green Papaya", "Fish Sauce", "Chili Leaves"), "almost",
        listOf("Green Papaya", "Chili Leaves")),
    Recipe("5", "Sisig", 450, 980, 35, 8, 32,
        listOf("Pork", "Onions", "Chili", "Calamansi", "Soy Sauce"), "ready"),
    Recipe("6", "Law-uy", 340, 580, 22, 42, 10,
        listOf("Squash", "String Beans", "Corn", "Ginger", "Salt"), "almost",
        listOf("Squash")),
    Recipe("7", "Bicol Express", 520, 720, 28, 14, 38,
        listOf("Pork", "Coconut Milk", "Chili", "Shrimp Paste", "Ginger"), "almost",
        listOf("Coconut Milk", "Shrimp Paste")),
    Recipe("8", "Pinakbet", 260, 680, 18, 28, 12,
        listOf("Squash", "Eggplant", "String Beans", "Tomatoes", "Shrimp Paste"), "almost",
        listOf("Eggplant", "Shrimp Paste"))
)

val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// ─── PANTRY SCREEN ───────────────────────────────────────

@Composable
fun PantryScreen(
    onNavigate: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf("pantry") }
    var searchQuery by remember { mutableStateOf("") }
    var pantryIngredients by remember {
        mutableStateOf(
            listOf("Pork", "Chicken", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves", "Onions", "Chili", "Calamansi")
        )
    }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }

    // Filter recipes
    val readyRecipes = ALL_RECIPES.filter { it.category == "ready" }
    val almostReadyRecipes = ALL_RECIPES.filter { it.category == "almost" }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabChange = {
                activeTab = it
                onNavigate(it)
            })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .padding(paddingValues)
            ) {
                // ── Header (bg-white px-6 py-5 shadow-sm) ──
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            text = "Pantry & Meal Plan",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937) // gray-800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Discover what you can cook today",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280) // gray-500
                        )
                    }
                }

                // ── Scrollable Content ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ── Search & Inventory Input ──
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(400, easing = EaseOutCubic)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Search bar with add button
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Add ingredients (e.g., Pork, Vinegar, Law-uy mix)",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                if (searchQuery
                                                        .trim()
                                                        .isNotEmpty() && !pantryIngredients.contains(
                                                        searchQuery.trim()
                                                    )
                                                ) {
                                                    pantryIngredients =
                                                        pantryIngredients + searchQuery.trim()
                                                    searchQuery = ""
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add ingredient",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedBorderColor = Color(0xFF4CAF50)
                                ),
                                singleLine = true
                            )

                            // ── My Pantry section ──
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "My Pantry",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151) // gray-700
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (pantryIngredients.isEmpty()) {
                                        Text(
                                            text = "No ingredients added yet. Start by adding what you have!",
                                            fontSize = 14.sp,
                                            color = Color(0xFF9CA3AF),
                                            fontStyle = FontStyle.Italic
                                        )
                                    } else {
                                        // FlowRow of ingredient chips ─ approximated with wrapping
                                        PantryChipsGrid(
                                            ingredients = pantryIngredients,
                                            onRemove = { ingredient ->
                                                pantryIngredients = pantryIngredients.filter { it != ingredient }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── What Can I Cook? ──
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(400, delayMillis = 150, easing = EaseOutCubic)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "What Can I Cook?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )

                            if (pantryIngredients.isEmpty()) {
                                // Empty state
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF3F4F6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp),
                                                tint = Color(0xFF9CA3AF)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No Ingredients Yet",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF1F2937)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Add ingredients to your pantry to discover recipes you can make!",
                                            fontSize = 14.sp,
                                            color = Color(0xFF4B5563),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // ── Ready to Cook (green dot + horizontal scroll) ──
                                if (readyRecipes.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                            )
                                            Text(
                                                "Ready to Cook",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF374151)
                                            )
                                            Text(
                                                "(${readyRecipes.size})",
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(end = 8.dp)
                                        ) {
                                            items(readyRecipes) { recipe ->
                                                ReadyRecipeCard(
                                                    recipe = recipe,
                                                    onClick = { selectedRecipe = recipe }
                                                )
                                            }
                                        }
                                    }
                                }

                                // ── Almost Ready (orange dot + horizontal scroll) ──
                                if (almostReadyRecipes.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF9800))
                                            )
                                            Text(
                                                "Almost Ready",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF374151)
                                            )
                                            Text(
                                                "(${almostReadyRecipes.size})",
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(end = 8.dp)
                                        ) {
                                            items(almostReadyRecipes) { recipe ->
                                                AlmostReadyRecipeCard(
                                                    recipe = recipe,
                                                    onClick = { selectedRecipe = recipe }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Meal Plan Calendar ──
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(400, delayMillis = 300, easing = EaseOutCubic)
                        )
                    ) {
                        MealPlanCalendar(
                            recipes = readyRecipes + almostReadyRecipes
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Recipe Detail Modal (overlay) ──
            if (selectedRecipe != null) {
                RecipeDetailModal(
                    recipe = selectedRecipe!!,
                    onClose = { selectedRecipe = null }
                )
            }
        }
    }
}

// ─── PANTRY CHIPS GRID ───────────────────────────────────
// Custom wrapping layout (replaces FlowRow for BOM compatibility)

@Composable
fun PantryChipsGrid(
    ingredients: List<String>,
    onRemove: (String) -> Unit
) {
    WrappingRow(
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) {
        ingredients.forEach { ingredient ->
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFE8F5E9), // green-50
                border = BorderStroke(2.dp, Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ingredient,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151) // gray-700
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove $ingredient",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onRemove(ingredient) },
                        tint = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
fun WrappingRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()

        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        var currentX = 0
        var currentY = 0
        var rowHeight = 0

        val positions = placeables.map { placeable ->
            if (currentX + placeable.width > constraints.maxWidth && currentX > 0) {
                currentX = 0
                currentY += rowHeight + vSpacingPx
                rowHeight = 0
            }
            val pos = Pair(currentX, currentY)
            rowHeight = maxOf(rowHeight, placeable.height)
            currentX += placeable.width + hSpacingPx
            pos
        }

        val totalHeight = if (placeables.isEmpty()) 0 else currentY + rowHeight

        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(positions[index].first, positions[index].second)
            }
        }
    }
}

// ─── READY RECIPE CARD ───────────────────────────────────
// Matches Figma: w-48 bg-white rounded-xl border-2 border-green-500 p-4

@Composable
fun ReadyRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color(0xFF4CAF50)),
        modifier = Modifier.width(192.dp) // w-48
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)), // green-100
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 12.sp)
                }
            }
            Column {
                Text(
                    text = "${recipe.calories} kcal",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Sodium: ${recipe.sodium}mg",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

// ─── ALMOST READY RECIPE CARD ────────────────────────────
// Matches Figma: w-48 bg-white rounded-xl border-2 border-orange-500 p-4

@Composable
fun AlmostReadyRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color(0xFFFF9800)),
        modifier = Modifier.width(192.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFFFF7ED), // orange-100
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Missing",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "${recipe.calories} kcal",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Need: ${recipe.missingIngredients.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── MEAL PLAN CALENDAR ──────────────────────────────────

@Composable
fun MealPlanCalendar(recipes: List<Recipe>) {
    var plannedMeals by remember { mutableStateOf(listOf<PlannedMeal>()) }

    // Weekly totals
    val weeklyCalories = plannedMeals.sumOf { it.recipe.calories }
    val weeklySodium = plannedMeals.sumOf { it.recipe.sodium }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Meal Plan Calendar",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous week
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous week",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4B5563)
                        )
                    }
                }
                Text(
                    "This Week",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
                // Next week
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next week",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4B5563)
                        )
                    }
                }
            }
        }

        // ── Weekly Summary (only when meals planned) ──
        if (plannedMeals.isNotEmpty()) {
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
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column {
                        Text("Weekly Total", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$weeklyCalories",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                " kcal",
                                fontSize = 14.sp,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                    Column {
                        Text("Avg. Sodium/Day", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${Math.round(weeklySodium.toFloat() / plannedMeals.size)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                " mg",
                                fontSize = 14.sp,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }
        }

        // ── Calendar Grid ──
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                // Day headers row
                Row(modifier = Modifier.fillMaxWidth()) {
                    DAYS.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 0.5.dp,
                                    color = Color(0xFFF3F4F6)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // Day cells
                Row(modifier = Modifier.fillMaxWidth()) {
                    DAYS.forEachIndexed { index, _ ->
                        val meal = plannedMeals.find { it.day == index }?.recipe

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .border(0.5.dp, Color(0xFFF3F4F6))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (meal != null) {
                                        // Remove meal on tap
                                        plannedMeals = plannedMeals.filter { it.day != index }
                                    }
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9CA3AF)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (meal != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (meal.category == "ready")
                                            Color(0xFFE8F5E9) else Color(0xFFFFF7ED),
                                        border = BorderStroke(
                                            2.dp,
                                            if (meal.category == "ready")
                                                Color(0xFF4CAF50) else Color(0xFFFF9800)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(4.dp)) {
                                            Text(
                                                text = meal.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1F2937),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${meal.calories} kcal",
                                                fontSize = 9.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Tap to add",
                                        fontSize = 9.sp,
                                        color = Color(0xFFD1D5DB),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Quick Add Section ──
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Quick Add to Calendar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recipes.take(5)) { recipe ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (recipe.category == "ready")
                                Color(0xFFE8F5E9) else Color(0xFFFFF7ED),
                            border = BorderStroke(
                                2.dp,
                                if (recipe.category == "ready")
                                    Color(0xFF4CAF50) else Color(0xFFFF9800)
                            ),
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // Add to first available day
                                val firstAvailable = (0..6).firstOrNull { day ->
                                    plannedMeals.none { it.day == day }
                                }
                                if (firstAvailable != null) {
                                    plannedMeals = plannedMeals + PlannedMeal(firstAvailable, recipe)
                                }
                            }
                        ) {
                            Text(
                                text = recipe.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 Tap recipes to add to the first available day",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

// ─── RECIPE DETAIL MODAL ─────────────────────────────────

@Composable
fun RecipeDetailModal(recipe: Recipe, onClose: () -> Unit) {
    // Calculate % of daily values (2000 kcal, 2300mg sodium)
    val caloriePercent = Math.round(recipe.calories.toFloat() / 2000f * 100)
    val sodiumPercent = Math.round(recipe.sodium.toFloat() / 2300f * 100)

    val sodiumStatus = when {
        recipe.sodium <= 500 -> "Low"
        recipe.sodium <= 800 -> "Moderate"
        else -> "High"
    }
    val sodiumStatusColor = when {
        recipe.sodium <= 500 -> Color(0xFF16A34A)  // green-600
        recipe.sodium <= 800 -> Color(0xFFCA8A04)  // yellow-600
        else -> Color(0xFFEA580C)                   // orange-600
    }

    // Full-screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom sheet card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* prevent close through card */ }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = recipe.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onClose() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF4B5563)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status badge
                    Surface(
                        color = if (recipe.category == "ready")
                            Color(0xFFDCFCE7) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (recipe.category == "ready") "✓ Ready to Cook"
                            else "Missing Ingredients",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (recipe.category == "ready")
                                Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // ── Scrollable Content ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Nutrition Overview ──
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Nutrition Overview",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Calories card (green gradient)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                "${recipe.calories}",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "kcal",
                                                fontSize = 14.sp,
                                                color = Color(0xFF4B5563),
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${caloriePercent}% of daily goal",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4B5563)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Progress bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(
                                                        (caloriePercent.coerceAtMost(100) / 100f)
                                                    )
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xFF4CAF50))
                                            )
                                        }
                                    }
                                }
                            }

                            // Sodium card (orange gradient)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                "${recipe.sodium}",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "mg",
                                                fontSize = 14.sp,
                                                color = Color(0xFF4B5563),
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row {
                                            Text(
                                                "${sodiumPercent}% of limit · ",
                                                fontSize = 12.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                            Text(
                                                sodiumStatus,
                                                fontSize = 12.sp,
                                                color = sodiumStatusColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(
                                                        (sodiumPercent.coerceAtMost(100) / 100f)
                                                    )
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xFFFF9800))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Macronutrients ──
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Macronutrients",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )

                        // Protein
                        MacroRow(
                            letter = "P",
                            label = "Protein",
                            value = "${recipe.protein}g",
                            bgColor = Color(0xFFDBEAFE), // blue-100
                            textColor = Color(0xFF2563EB) // blue-600
                        )
                        // Carbs
                        MacroRow(
                            letter = "C",
                            label = "Carbohydrates",
                            value = "${recipe.carbs}g",
                            bgColor = Color(0xFFFEF9C3), // yellow-100
                            textColor = Color(0xFFCA8A04) // yellow-600
                        )
                        // Fats
                        MacroRow(
                            letter = "F",
                            label = "Fats",
                            value = "${recipe.fats}g",
                            bgColor = Color(0xFFF3E8FF), // purple-100
                            textColor = Color(0xFF9333EA) // purple-600
                        )
                    }

                    // ── Ingredients ──
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ingredients",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )

                        recipe.ingredients.forEach { ingredient ->
                            val isMissing = recipe.missingIngredients.contains(ingredient)

                            Surface(
                                color = if (isMissing) Color(0xFFFFF7ED) else Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isMissing) BorderStroke(1.dp, Color(0xFFFF9800))
                                else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isMissing) Color(0xFFFF9800)
                                                else Color(0xFF4CAF50)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isMissing) "!" else "✓",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = ingredient,
                                        fontSize = 14.sp,
                                        color = if (isMissing) Color(0xFFFF9800) else Color(0xFF374151),
                                        fontWeight = if (isMissing) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // ── PhilFCT Note ──
                    Surface(
                        color = Color(0xFFEFF6FF), // blue-50
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Note: Nutritional values are estimated based on the Philippine Food Composition Tables (PhilFCT) for standard serving sizes.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A5F), // blue-800
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // ── Footer: Add to Meal Plan ──
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            "Add to Meal Plan",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── MACRO ROW (for recipe detail) ───────────────────────

@Composable
fun MacroRow(
    letter: String,
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF374151)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
    }
}
