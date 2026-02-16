package com.calorieko.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import kotlinx.coroutines.launch

// --- Data Models ---
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
    val dayIndex: Int, // 0 = Mon, 6 = Sun
    val recipe: Recipe
)

// --- Mock Data ---
val ALL_RECIPES = listOf(
    Recipe("1", "Pork Adobo", 380, 890, 32, 12, 24, listOf("Pork", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves"), "ready"),
    Recipe("2", "Chicken Adobo", 320, 820, 38, 10, 18, listOf("Chicken", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves"), "ready"),
    Recipe("3", "Sinigang na Baboy", 420, 650, 28, 35, 16, listOf("Pork", "Tamarind", "Tomatoes", "Onions", "Kangkong"), "almost", listOf("Kangkong")),
    Recipe("4", "Tinola", 280, 520, 30, 18, 12, listOf("Chicken", "Ginger", "Green Papaya", "Fish Sauce", "Chili Leaves"), "almost", listOf("Green Papaya", "Chili Leaves")),
    Recipe("5", "Sisig", 450, 980, 35, 8, 32, listOf("Pork", "Onions", "Chili", "Calamansi", "Soy Sauce"), "ready"),
    Recipe("6", "Law-uy", 340, 580, 22, 42, 10, listOf("Squash", "String Beans", "Corn", "Ginger", "Salt"), "almost", listOf("Squash")),
    Recipe("7", "Bicol Express", 520, 720, 28, 14, 38, listOf("Pork", "Coconut Milk", "Chili", "Shrimp Paste", "Ginger"), "almost", listOf("Coconut Milk", "Shrimp Paste")),
    Recipe("8", "Pinakbet", 260, 680, 18, 28, 12, listOf("Squash", "Eggplant", "String Beans", "Tomatoes", "Shrimp Paste"), "almost", listOf("Eggplant", "Shrimp Paste"))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PantryScreen(onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("pantry") }
    var searchQuery by remember { mutableStateOf("") }
    var pantryIngredients by remember { mutableStateOf(listOf("Pork", "Chicken", "Soy Sauce", "Vinegar", "Garlic", "Bay Leaves", "Onions", "Chili", "Calamansi")) }
    var plannedMeals by remember { mutableStateOf<List<PlannedMeal>>(emptyList()) }

    // Bottom Sheet State for Recipe Details
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Keyboard Controller
    val keyboardController = LocalSoftwareKeyboardController.current

    // Logic: Filter Recipes
    val readyRecipes = ALL_RECIPES.filter { it.category == "ready" }
    val almostReadyRecipes = ALL_RECIPES.filter { it.category == "almost" }

    fun handleAddIngredient() {
        if (searchQuery.isNotBlank() && !pantryIngredients.contains(searchQuery.trim())) {
            pantryIngredients = pantryIngredients + searchQuery.trim()
            searchQuery = ""
            keyboardController?.hide()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                Surface(color = Color.White, shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Text("Pantry & Meal Plan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text("Discover what you can cook today", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            // Search & Inventory
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Search Bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Add ingredients (e.g., Pork, Vinegar)", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { handleAddIngredient() }),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { handleAddIngredient() },
                                modifier = Modifier.size(32.dp).background(CalorieKoGreen, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pantry Chips
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("My Pantry", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                            Spacer(modifier = Modifier.height(12.dp))

                            // ADD THIS BLOCK
                            SimpleFlowRow(
                                horizontalGap = 8.dp,
                                verticalGap = 8.dp
                            ) {
                                pantryIngredients.forEach { ingredient ->
                                    Surface(
                                        color = Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ingredient, fontSize = 13.sp, color = Color(0xFF1F2937))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                null,
                                                tint = Color(0xFF6B7280),
                                                modifier = Modifier.size(14.dp).clickable {
                                                    pantryIngredients = pantryIngredients - ingredient
                                                }
                                            )
                                        }
                                    }
                                }
                                if (pantryIngredients.isEmpty()) {
                                    Text(
                                        "No ingredients added yet.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recipe Suggestions
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("What Can I Cook?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (readyRecipes.isNotEmpty()) {
                        RecipeRow("Ready to Cook", readyRecipes, CalorieKoGreen) { selectedRecipe = it }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (almostReadyRecipes.isNotEmpty()) {
                        RecipeRow("Almost Ready", almostReadyRecipes, CalorieKoOrange) { selectedRecipe = it }
                    }

                    if (readyRecipes.isEmpty() && almostReadyRecipes.isEmpty()) {
                        EmptyStateCard()
                    }
                }
            }

            // Meal Plan Calendar
            item {
                MealPlanCalendarSection(
                    plannedMeals = plannedMeals,
                    onAddMeal = { day, recipe ->
                        // Logic handled by dialog usually, keeping simple for prototype
                        plannedMeals = plannedMeals + PlannedMeal(day, recipe)
                    },
                    onRemoveMeal = { day ->
                        plannedMeals = plannedMeals.filter { it.dayIndex != day }
                    },
                    allRecipes = readyRecipes + almostReadyRecipes
                )
            }
        }
    }

    // Recipe Detail Modal
    if (selectedRecipe != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedRecipe = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            RecipeDetailContent(
                recipe = selectedRecipe!!,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedRecipe = null }
                },
                onAddToPlan = {
                    // In a real app, this would open a day selector
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedRecipe = null }
                }
            )
        }
    }
}

// --- Recipe Row Component ---
@Composable
fun RecipeRow(title: String, recipes: List<Recipe>, color: Color, onClick: (Recipe) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
            Spacer(modifier = Modifier.width(4.dp))
            Text("(${recipes.size})", fontSize = 12.sp, color = Color.Gray)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recipes) { recipe ->
                RecipeCard(recipe, color, onClick)
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe, color: Color, onClick: (Recipe) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick(recipe) }
            .border(2.dp, color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    recipe.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f)
                )
                if (recipe.category == "ready") {
                    Box(modifier = Modifier.size(24.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(4.dp)) {
                        Text("Missing", fontSize = 10.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("${recipe.calories} kcal", fontSize = 12.sp, color = Color(0xFF4B5563))

            if (recipe.missingIngredients.isNotEmpty()) {
                Text(
                    "Need: ${recipe.missingIngredients.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = CalorieKoOrange,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text("Sodium: ${recipe.sodium}mg", fontSize = 12.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(64.dp).background(Color(0xFFFFF7ED), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Restaurant, null, tint = CalorieKoOrange, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Recipes Match Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text("Add more ingredients to unlock recipe suggestions!", fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center)
        }
    }
}

// --- Meal Plan Calendar Section ---
@Composable
fun MealPlanCalendarSection(
    plannedMeals: List<PlannedMeal>,
    onAddMeal: (Int, Recipe) -> Unit,
    onRemoveMeal: (Int) -> Unit,
    allRecipes: List<Recipe>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var recipeToAdd by remember { mutableStateOf<Recipe?>(null) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weeklyCalories = plannedMeals.sumOf { it.recipe.calories }
    val weeklySodium = if (plannedMeals.isNotEmpty()) plannedMeals.sumOf { it.recipe.sodium } / plannedMeals.size else 0

    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Meal Plan Calendar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                Text("This Week", fontSize = 14.sp, color = Color.Gray)
                IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            }
        }

        // Stats
        if (plannedMeals.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Weekly Total", fontSize = 12.sp, color = Color.Gray)
                        Text("$weeklyCalories kcal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avg. Sodium/Day", fontSize = 12.sp, color = Color.Gray)
                        Text("$weeklySodium mg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    }
                }
            }
        }

        // Calendar Grid
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Days Header
                Row(modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFF3F4F6))) {
                    days.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                // Grid Body
                Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    days.indices.forEach { index ->
                        val meal = plannedMeals.find { it.dayIndex == index }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(0.5.dp, Color(0xFFF3F4F6))
                                .clickable {
                                    if (meal != null) {
                                        onRemoveMeal(index)
                                    }
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (meal != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (meal.recipe.category == "ready") Color(0xFFECFDF5) else Color(0xFFFFEDD5)
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(modifier = Modifier.padding(4.dp)) {
                                        Text(meal.recipe.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${meal.recipe.calories}", fontSize = 9.sp, color = Color.DarkGray)
                                    }
                                }
                            } else {
                                Text("${index + 1}", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Add
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Add to Calendar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allRecipes.take(5)) { recipe ->
                        SuggestionChip(
                            onClick = {
                                recipeToAdd = recipe
                                showAddDialog = true
                            },
                            label = { Text(recipe.name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (recipe.category == "ready") Color(0xFFECFDF5) else Color(0xFFFFEDD5),
                                labelColor = Color(0xFF1F2937)
                            ),
                            border = BorderStroke(1.dp, if (recipe.category == "ready") CalorieKoGreen else CalorieKoOrange)
                        )
                    }
                }
            }
        }
    }

    // Add Meal Dialog
    if (showAddDialog && recipeToAdd != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Plan Meal") },
            text = {
                Column {
                    Text("Select a day to cook ${recipeToAdd?.name}:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CalorieKoGreen.copy(alpha = 0.1f), CircleShape)
                                    .clickable {
                                        onAddMeal(index, recipeToAdd!!)
                                        showAddDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.first().toString(), fontWeight = FontWeight.Bold, color = CalorieKoGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

// --- Recipe Detail Content (BottomSheet) ---
@Composable
fun RecipeDetailContent(recipe: Recipe, onClose: () -> Unit, onAddToPlan: () -> Unit) {
    val caloriePercent = (recipe.calories / 2000f)
    val sodiumPercent = (recipe.sodium / 2300f)
    val sodiumColor = if (recipe.sodium <= 500) Color(0xFF16A34A) else if (recipe.sodium <= 800) Color(0xFFCA8A04) else Color(0xFFEA580C)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(recipe.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Spacer(modifier = Modifier.height(8.dp))
                if (recipe.category == "ready") {
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                        Text("✓ Ready to Cook", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = CalorieKoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(50)) {
                        Text("Missing Ingredients", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = CalorieKoOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nutrition Cards
        Text("Nutrition Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Calories
            NutritionCard(
                value = "${recipe.calories}",
                unit = "kcal",
                subtext = "${(caloriePercent * 100).toInt()}% of daily",
                progress = caloriePercent,
                color = CalorieKoGreen,
                bgColor = Color(0xFFECFDF5),
                modifier = Modifier.weight(1f)
            )
            // Sodium
            NutritionCard(
                value = "${recipe.sodium}",
                unit = "mg",
                subtext = "${(sodiumPercent * 100).toInt()}% of limit",
                progress = sodiumPercent,
                color = sodiumColor,
                bgColor = if (recipe.sodium <= 500) Color(0xFFECFDF5) else if (recipe.sodium <= 800) Color(0xFFFEF9C3) else Color(0xFFFFF7ED),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Macros
        Text("Macronutrients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroRow("Protein", "${recipe.protein}g", Color(0xFF3B82F6), "P")
            MacroRow("Carbohydrates", "${recipe.carbs}g", Color(0xFFEAB308), "C")
            MacroRow("Fats", "${recipe.fats}g", Color(0xFFA855F7), "F")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ingredients List
        Text("Ingredients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recipe.ingredients.forEach { ingredient ->
                val isMissing = recipe.missingIngredients.contains(ingredient)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isMissing) Color(0xFFFFF7ED) else Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                        .border(1.dp, if (isMissing) Color(0xFFFFEDD5) else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(20.dp).background(if (isMissing) CalorieKoOrange else CalorieKoGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (isMissing) Icons.Rounded.Warning else Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(ingredient, color = if (isMissing) CalorieKoOrange else Color(0xFF374151), fontWeight = if (isMissing) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddToPlan,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add to Meal Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NutritionCard(value: String, unit: String, subtext: String, progress: Float, color: Color, bgColor: Color, modifier: Modifier) {
    // Animate Bar
    val animatedProgress = animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "bar")

    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 14.sp, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(subtext, fontSize = 11.sp, color = Color(0xFF4B5563))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White, CircleShape)) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.value.coerceIn(0f, 1f)).background(color, CircleShape))
            }
        }
    }
}

@Composable
fun MacroRow(name: String, value: String, color: Color, label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontSize = 14.sp, color = Color(0xFF374151))
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
    }
}