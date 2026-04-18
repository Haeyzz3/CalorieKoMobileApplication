package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.components.SimpleFlowRow
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.viewmodel.DishResult
import com.calorieko.app.viewmodel.IngredientInfo
import com.calorieko.app.viewmodel.PantryViewModel
import com.calorieko.app.viewmodel.ProofType
import com.calorieko.app.viewmodel.DishProofDocument
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

// --- Common Ingredients for Quick-Add ---
val COMMON_INGREDIENTS = listOf(
    "chicken_egg", "garlic", "onion_bombay", "salt_iodized", "cooking_oil", "black_pepper",
    "vinegar_cane", "tomato", "pork_liempo", "chicken_breast", "water", "sugar_white",
    "soy_sauce", "ginger", "calamansi_juice"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PantryScreen(viewModel: PantryViewModel, onNavigate: (String) -> Unit) {
    var activeTab by remember { mutableStateOf("pantry") }

    // Collect ViewModel state
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pantryIngredients by viewModel.pantryItems.collectAsState()
    val readyRecipes by viewModel.readyToCookDishes.collectAsState()
    val almostReadyRecipes by viewModel.almostReadyDishes.collectAsState()
    val autocompleteSuggestions by viewModel.autocompleteSuggestions.collectAsState()
    val plannedMeals by viewModel.plannedMeals.collectAsState()
    val weeklyCalories by viewModel.weeklyCalories.collectAsState()
    val avgDailySodium by viewModel.avgDailySodium.collectAsState()
    val pantryByCategory by viewModel.pantryItemsByCategory.collectAsState()

    // Bottom Sheet State for Recipe Details
    val selectedRecipe = remember { mutableStateOf<DishResult?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Keyboard Controller
    val keyboardController = LocalSoftwareKeyboardController.current

    // Clear Pantry Confirmation Dialog state
    val showClearPantryDialog = remember { mutableStateOf(false) }

    fun handleAddIngredient() {
        if (searchQuery.isNotBlank()) {
            viewModel.addIngredient(searchQuery)
            viewModel.updateSearchQuery("")
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
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Add ingredients (e.g., egg, garlic)", color = Color.Gray) },
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

                    // Autocomplete Dropdown
                    if (autocompleteSuggestions.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                        ) {
                            Column {
                                autocompleteSuggestions.forEach { suggestion ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addIngredient(suggestion)
                                                viewModel.updateSearchQuery("")
                                                keyboardController?.hide()
                                            },
                                        color = Color.Transparent
                                    ) {
                                        Text(
                                            text = viewModel.formatIngredientName(suggestion),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            fontSize = 14.sp,
                                            color = Color(0xFF374151)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick-Add Common Ingredients
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Quick Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(COMMON_INGREDIENTS.filter { it !in pantryIngredients }) { ingredient ->
                                    SuggestionChip(
                                        onClick = { viewModel.addIngredient(ingredient) },
                                        label = { Text(viewModel.formatIngredientName(ingredient), fontSize = 12.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFECFDF5),
                                            labelColor = Color(0xFF374151)
                                        ),
                                        border = BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.3f)),
                                        icon = {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = CalorieKoGreen)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                     // Pantry Chips — Grouped by Category
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("My Pantry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${pantryIngredients.size} items", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (pantryIngredients.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier.clickable { showClearPantryDialog.value = true },
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (pantryIngredients.isEmpty()) {
                                Text(
                                    "No ingredients added yet. Add some to get recipe suggestions!",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            } else {
                                val categoryOrder = listOf(
                                    "protein" to Pair("\uD83E\uDD69 Protein", Color(0xFFFEE2E2)),
                                    "produce" to Pair("\uD83E\uDD6C Produce", Color(0xFFDCFCE7)),
                                    "seasoning" to Pair("\uD83E\uDDC2 Seasonings & Sauces", Color(0xFFFEF9C3)),
                                    "pantry_staple" to Pair("\uD83C\uDFE1 Pantry Staples", Color(0xFFDBEAFE))
                                )

                                categoryOrder.forEach { (categoryKey, labelAndColor) ->
                                    val (label, chipBgColor) = labelAndColor
                                    val items = pantryByCategory[categoryKey] ?: emptyList()
                                    if (items.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${items.size})", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        SimpleFlowRow(
                                            horizontalGap = 8.dp,
                                            verticalGap = 8.dp
                                        ) {
                                            items.forEach { ingredient ->
                                                Surface(
                                                    color = chipBgColor,
                                                    shape = RoundedCornerShape(50),
                                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(viewModel.formatIngredientName(ingredient), fontSize = 13.sp, color = Color(0xFF374151))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            Icons.Default.Close,
                                                            null,
                                                            tint = Color(0xFF9CA3AF),
                                                            modifier = Modifier.size(14.dp).clickable {
                                                                viewModel.removeIngredient(ingredient)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Explore All Dishes CTA
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clickable { onNavigate("explore") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍽️", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Explore All Dishes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                "Browse 24 supported dishes, view ingredients & sources",
                                fontSize = 12.sp,
                                color = Color(0xFF3B82F6),
                                lineHeight = 16.sp
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Explore",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Recipe Suggestions
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("What Can I Cook?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (readyRecipes.isNotEmpty()) {
                        RecipeRow("Ready to Cook", readyRecipes, CalorieKoGreen) { selectedRecipe.value = it }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (almostReadyRecipes.isNotEmpty()) {
                        RecipeRow("Almost Ready", almostReadyRecipes, CalorieKoOrange) { selectedRecipe.value = it }
                    }

                    if (readyRecipes.isEmpty() && almostReadyRecipes.isEmpty()) {
                        EmptyStateCard()
                    }
                }
            }

            // Meal Plan Calendar
            item {
                MealPlanCalendarSection(
                    viewModel = viewModel,
                    plannedMeals = plannedMeals,
                    weeklyCalories = weeklyCalories,
                    avgDailySodium = avgDailySodium,
                    allRecipes = readyRecipes + almostReadyRecipes
                )
            }
        }
    }

    // Recipe Detail Modal
    if (selectedRecipe.value != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedRecipe.value = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            RecipeDetailContent(
                recipe = selectedRecipe.value!!,
                viewModel = viewModel,
                plannedMeals = plannedMeals,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedRecipe.value = null }
                },
                onAddToPlan = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedRecipe.value = null }
                }
            )
        }
    }

    // Clear All Pantry Confirmation Dialog
    if (showClearPantryDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearPantryDialog.value = false },
            title = { Text("Clear Pantry") },
            text = { Text("Are you sure you want to remove all ${pantryIngredients.size} ingredients from your pantry?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllPantryItems()
                    showClearPantryDialog.value = false
                }) {
                    Text("Clear All", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearPantryDialog.value = false }) { Text("Cancel") } }
        )
    }
}

// --- Recipe Row Component ---
@Composable
fun RecipeRow(title: String, recipes: List<DishResult>, color: Color, onClick: (DishResult) -> Unit) {
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
fun RecipeCard(recipe: DishResult, color: Color, onClick: (DishResult) -> Unit) {
    val isReady = recipe.missingCoreIngredients.isEmpty()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(220.dp)
            .height(200.dp)
            .clickable { onClick(recipe) }
            .border(2.dp, color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Emoji + Badge
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getDishEmoji(recipe.dishLabel), fontSize = 28.sp)
                }

                if (isReady) {
                    Box(modifier = Modifier.size(24.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(4.dp)) {
                        Text("Missing", fontSize = 10.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                recipe.dishName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${recipe.calories} kcal", fontSize = 12.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                Text(" • ", fontSize = 12.sp, color = Color.LightGray)
                Text("${recipe.sodium}mg Na", fontSize = 12.sp, color = Color(0xFF9CA3AF))
            }

            // Core ingredient match info
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${recipe.coreMatchedCount}/${recipe.coreTotalCount} Core Ingredients",
                fontSize = 11.sp,
                color = if (isReady) CalorieKoGreen else CalorieKoOrange,
                fontWeight = FontWeight.Medium
            )

            if (recipe.missingCoreIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Need: ${recipe.missingCoreIngredients.joinToString(", ") { it.replace("_", " ") }}",
                    fontSize = 12.sp,
                    color = CalorieKoOrange,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (recipe.missingOptionalIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Optional: ${recipe.missingOptionalIngredients.joinToString(", ") { it.replace("_", " ") }}",
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    viewModel: PantryViewModel,
    plannedMeals: List<PlannedMealEntity>,
    weeklyCalories: Int,
    avgDailySodium: Int,
    allRecipes: List<DishResult>
) {
    // --- Dialog states ---
    val showAddDialog = remember { mutableStateOf(false) }
    val recipeToAdd = remember { mutableStateOf<DishResult?>(null) }
    val selectedDayIndex = remember { mutableIntStateOf(-1) }
    val showSlotPicker = remember { mutableStateOf(false) }

    // Meal Detail Dialog (tapping populated cell)
    val showMealDetail = remember { mutableStateOf(false) }
    val detailDayIndex = remember { mutableIntStateOf(-1) }
    val detailSlot = remember { mutableStateOf("") }

    // "Add Dish to this slot" flow from Meal Detail Dialog
    val showAddDishToSlot = remember { mutableStateOf(false) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val mealSlots = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val slotEmojis = mapOf("Breakfast" to "☀️", "Lunch" to "🌤️", "Dinner" to "🌙", "Snack" to "🍿")
    val slotColors = mapOf(
        "Breakfast" to Color(0xFFFFF7ED),
        "Lunch" to Color(0xFFECFDF5),
        "Dinner" to Color(0xFFEDE9FE),
        "Snack" to Color(0xFFFEF9C3)
    )

    // Clear Week/Day dialog states
    val showClearWeekDialog = remember { mutableStateOf(false) }
    val showClearDayDialog = remember { mutableStateOf(false) }
    val clearDayIndex = remember { mutableIntStateOf(-1) }

    // Sodium warning threshold (mg)
    val sodiumWarning = avgDailySodium > 2000

    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Meal Plan Calendar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (plannedMeals.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.clickable { showClearWeekDialog.value = true },
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Clear Week", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("This Week", fontSize = 14.sp, color = Color.Gray)
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
                        Text(
                            "$avgDailySodium mg",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sodiumWarning) CalorieKoOrange else Color(0xFF1F2937)
                        )
                        if (sodiumWarning) {
                            Text("⚠ Above recommended", fontSize = 10.sp, color = CalorieKoOrange)
                        }
                    }
                }
            }
        }

        // Calendar Grid — multi-dish per slot
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Days Header
                Row(modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFF3F4F6))) {
                    Box(
                        modifier = Modifier.width(56.dp).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("", fontSize = 10.sp)
                    }
                    days.forEachIndexed { dayIdx, day ->
                        val dayHasMeals = plannedMeals.any { it.dayIndex == dayIdx }
                        Text(
                            text = day,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp)
                                .clickable {
                                    if (dayHasMeals) {
                                        clearDayIndex.intValue = dayIdx
                                        showClearDayDialog.value = true
                                    }
                                },
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dayHasMeals) Color(0xFF374151) else Color(0xFF6B7280)
                        )
                    }
                }
                // Slot rows
                mealSlots.forEach { slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFF3F4F6)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot label column
                        Box(
                            modifier = Modifier.width(56.dp).padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(slotEmojis[slot] ?: "", fontSize = 12.sp)
                                Text(
                                    slot.take(3),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                        // Day cells
                        days.indices.forEach { dayIdx ->
                            val slotMeals = plannedMeals.filter { it.dayIndex == dayIdx && it.mealSlot == slot }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .border(0.5.dp, Color(0xFFF3F4F6))
                                    .clickable {
                                        // Open Meal Detail Dialog (works for both empty and populated cells)
                                        detailDayIndex.intValue = dayIdx
                                        detailSlot.value = slot
                                        showMealDetail.value = true
                                    }
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (slotMeals.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = slotColors[slot] ?: Color(0xFFECFDF5)
                                        ),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(2.dp).fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (slotMeals.size <= 2) {
                                                // Show individual emojis
                                                Text(
                                                    slotMeals.joinToString("") { getDishEmoji(it.dishLabel) },
                                                    fontSize = 14.sp
                                                )
                                            } else {
                                                // Show first emoji + count
                                                Text(
                                                    "${getDishEmoji(slotMeals.first().dishLabel)} +${slotMeals.size - 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF374151)
                                                )
                                            }
                                            Text(
                                                "${slotMeals.size} dish${if (slotMeals.size > 1) "es" else ""}",
                                                fontSize = 7.sp,
                                                color = Color(0xFF6B7280),
                                                lineHeight = 8.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Add
        if (allRecipes.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Add to Calendar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allRecipes.take(5)) { recipe ->
                            SuggestionChip(
                                onClick = {
                                    recipeToAdd.value = recipe
                                    showAddDialog.value = true
                                },
                                label = { Text("${getDishEmoji(recipe.dishLabel)} ${recipe.dishName}") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (recipe.missingCoreIngredients.isEmpty()) Color(0xFFECFDF5) else Color(0xFFFFEDD5),
                                    labelColor = Color(0xFF1F2937)
                                ),
                                border = BorderStroke(1.dp, if (recipe.missingCoreIngredients.isEmpty()) CalorieKoGreen else CalorieKoOrange)
                            )
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Meal Detail Dialog (tapping a populated cell)
    // ================================================================
    if (showMealDetail.value) {
        val dayIdx = detailDayIndex.intValue
        val slot = detailSlot.value
        val slotMeals = plannedMeals.filter { it.dayIndex == dayIdx && it.mealSlot == slot }

        AlertDialog(
            onDismissRequest = { showMealDetail.value = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(slotEmojis[slot] ?: "", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("${days[dayIdx]} — $slot", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (slotMeals.isNotEmpty()) {
                            Text("${slotMeals.size} dish${if (slotMeals.size > 1) "es" else ""}", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Text("No dishes planned", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            text = {
                Column {
                    if (slotMeals.isEmpty()) {
                        // Empty state
                        Text(
                            "No dishes added to this meal yet. Tap below to add one!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Dish list with individual remove buttons
                        slotMeals.forEach { meal ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = slotColors[slot] ?: Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(getDishEmoji(meal.dishLabel), fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        viewModel.formatIngredientName(meal.dishLabel),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF374151),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.removeDishFromSlot(dayIdx, slot, meal.dishLabel)
                                            if (slotMeals.size <= 1) {
                                                showMealDetail.value = false
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove dish",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Dish button
                    if (allRecipes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMealDetail.value = false
                                    showAddDishToSlot.value = true
                                },
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = CalorieKoGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Dish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CalorieKoGreen)
                            }
                        }
                    }

                    // Clear Entire Meal button (only show when there are dishes)
                    if (slotMeals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.clearMealSlot(dayIdx, slot)
                                    showMealDetail.value = false
                                },
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🗑", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Entire Meal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMealDetail.value = false }) { Text("Cancel") } }
        )
    }

    // ================================================================
    // "Add Dish to this slot" — recipe picker (from Meal Detail Dialog)
    // ================================================================
    if (showAddDishToSlot.value && allRecipes.isNotEmpty()) {
        val dayIdx = detailDayIndex.intValue
        val slot = detailSlot.value
        val existingDishLabels = plannedMeals
            .filter { it.dayIndex == dayIdx && it.mealSlot == slot }
            .map { it.dishLabel }
            .toSet()
        AlertDialog(
            onDismissRequest = { showAddDishToSlot.value = false },
            title = { Text("Add Dish to ${days[dayIdx]} $slot") },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    allRecipes.forEach { recipe ->
                        val alreadyAdded = recipe.dishLabel in existingDishLabels
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!alreadyAdded) {
                                        viewModel.addMealToPlan(dayIdx, recipe.dishLabel, slot)
                                        showAddDishToSlot.value = false
                                    }
                                },
                            color = if (alreadyAdded) Color(0xFFECFDF5) else Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(12.dp),
                            border = if (alreadyAdded) BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.3f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(getDishEmoji(recipe.dishLabel), fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        recipe.dishName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (alreadyAdded) Color(0xFF9CA3AF) else Color(0xFF374151)
                                    )
                                    Text("${recipe.calories} kcal", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (alreadyAdded) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Added", fontSize = 9.sp, color = CalorieKoGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddDishToSlot.value = false }) { Text("Cancel") } }
        )
    }

    // ================================================================
    // Quick Add: Step 1 — Pick a day
    // ================================================================
    if (showAddDialog.value && recipeToAdd.value != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog.value = false },
            title = { Text("Plan Meal") },
            text = {
                Column {
                    Text("Select a day to cook ${recipeToAdd.value?.dishName}:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CalorieKoGreen.copy(alpha = 0.1f), CircleShape)
                                    .clickable {
                                        selectedDayIndex.intValue = index
                                        showAddDialog.value = false
                                        showSlotPicker.value = true
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
            dismissButton = { TextButton(onClick = { showAddDialog.value = false }) { Text("Cancel") } }
        )
    }

    // ================================================================
    // Quick Add: Step 2 — Pick a meal slot
    // ================================================================
    if (showSlotPicker.value && recipeToAdd.value != null) {
        val dishToAdd = recipeToAdd.value!!.dishLabel
        AlertDialog(
            onDismissRequest = { showSlotPicker.value = false },
            title = { Text("Choose Meal Slot") },
            text = {
                Column {
                    Text("Add ${recipeToAdd.value?.dishName} on ${days[selectedDayIndex.intValue]} as:")
                    Spacer(modifier = Modifier.height(16.dp))
                    mealSlots.forEach { slot ->
                        val alreadyInSlot = plannedMeals.any {
                            it.dayIndex == selectedDayIndex.intValue && it.mealSlot == slot && it.dishLabel == dishToAdd
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!alreadyInSlot) {
                                        viewModel.addMealToPlan(
                                            selectedDayIndex.intValue,
                                            dishToAdd,
                                            slot
                                        )
                                    }
                                    showSlotPicker.value = false
                                },
                            color = slotColors[slot] ?: Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp),
                            border = if (alreadyInSlot) BorderStroke(1.5.dp, CalorieKoGreen) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(slotEmojis[slot] ?: "", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    slot,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (alreadyInSlot) Color(0xFF9CA3AF) else Color(0xFF374151),
                                    modifier = Modifier.weight(1f)
                                )
                                if (alreadyInSlot) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Added", fontSize = 9.sp, color = CalorieKoGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSlotPicker.value = false }) { Text("Cancel") } }
        )
    }

    // ================================================================
    // Clear Week Confirmation Dialog
    // ================================================================
    if (showClearWeekDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearWeekDialog.value = false },
            title = { Text("Clear Week") },
            text = { Text("Are you sure you want to remove all ${plannedMeals.size} planned dishes for this week?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearMealWeek()
                    showClearWeekDialog.value = false
                }) {
                    Text("Clear Week", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearWeekDialog.value = false }) { Text("Cancel") } }
        )
    }

    // ================================================================
    // Clear Day Confirmation Dialog
    // ================================================================
    if (showClearDayDialog.value && clearDayIndex.intValue >= 0) {
        val dayName = days[clearDayIndex.intValue]
        val dayMealCount = plannedMeals.count { it.dayIndex == clearDayIndex.intValue }
        AlertDialog(
            onDismissRequest = { showClearDayDialog.value = false },
            title = { Text("Clear $dayName") },
            text = { Text("Are you sure you want to remove all $dayMealCount planned dish${if (dayMealCount > 1) "es" else ""} for $dayName?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearMealDay(clearDayIndex.intValue)
                    showClearDayDialog.value = false
                }) {
                    Text("Clear Day", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDayDialog.value = false }) { Text("Cancel") } }
        )
    }
}

// --- Recipe Detail Content (BottomSheet) ---
@Composable
fun RecipeDetailContent(recipe: DishResult, viewModel: PantryViewModel, plannedMeals: List<PlannedMealEntity>, onClose: () -> Unit, onAddToPlan: () -> Unit) {
    val isReady = recipe.missingCoreIngredients.isEmpty()

    // Use user's actual targets from ViewModel
    val userCalorieTarget by viewModel.userCalorieTarget.collectAsState()
    val userSodiumLimit by viewModel.userSodiumLimit.collectAsState()

    val caloriePercent = if (userCalorieTarget > 0) (recipe.calories / userCalorieTarget.toFloat()) else 0f
    val sodiumPercent = if (userSodiumLimit > 0) (recipe.sodium / userSodiumLimit.toFloat()) else 0f
    val sodiumColor = if (recipe.sodium <= 500) Color(0xFF16A34A) else if (recipe.sodium <= 800) Color(0xFFCA8A04) else Color(0xFFEA580C)

    // Combine all missing for convenience
    val allMissing = recipe.missingCoreIngredients + recipe.missingOptionalIngredients

    // Full nutrient toggle state
    var showFullNutrients by remember { mutableStateOf(false) }

    // Add-to-plan dialog state
    val showPlanDialog = remember { mutableStateOf(false) }
    val selectedDayForPlan = remember { mutableIntStateOf(-1) }
    val showSlotPickerForPlan = remember { mutableStateOf(false) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val mealSlots = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val slotEmojis = mapOf("Breakfast" to "☀️", "Lunch" to "🌤️", "Dinner" to "🌙", "Snack" to "🍿")
    val slotColors = mapOf(
        "Breakfast" to Color(0xFFFFF7ED),
        "Lunch" to Color(0xFFECFDF5),
        "Dinner" to Color(0xFFEDE9FE),
        "Snack" to Color(0xFFFEF9C3)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getDishEmoji(recipe.dishLabel), fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(recipe.dishName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(4.dp))
                    // Core ingredient match info
                    Text(
                        "${recipe.coreMatchedCount}/${recipe.coreTotalCount} Core Ingredients",
                        fontSize = 12.sp,
                        color = if (isReady) CalorieKoGreen else CalorieKoOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isReady) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                            Text("✓ Ready to Cook", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = CalorieKoGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(50)) {
                            Text("Missing Core Ingredients", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = CalorieKoOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nutrition Cards
        if (recipe.calories > 0) {
            Text("Nutrition Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Text("Values per 100 grams", fontSize = 12.sp, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NutritionCard(
                    value = "${recipe.calories}",
                    unit = "kcal",
                    subtext = "${(caloriePercent * 100).toInt()}% of daily target",
                    progress = caloriePercent,
                    color = CalorieKoGreen,
                    bgColor = Color(0xFFECFDF5),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                NutritionCard(
                    value = "${recipe.sodium}",
                    unit = "Sod. (mg)",
                    subtext = "${(sodiumPercent * 100).toInt()}% of limit",
                    progress = sodiumPercent,
                    color = sodiumColor,
                    bgColor = if (recipe.sodium <= 500) Color(0xFFECFDF5) else if (recipe.sodium <= 800) Color(0xFFFEF9C3) else Color(0xFFFFF7ED),
                    modifier = Modifier.weight(1f).fillMaxHeight()
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

            Spacer(modifier = Modifier.height(16.dp))

            // View Full Nutrients Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullNutrients = !showFullNutrients },
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (showFullNutrients) "Hide Full Nutrients" else "View Full Nutrients (12)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showFullNutrients) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Full Nutrients Expandable Section
            AnimatedVisibility(
                visible = showFullNutrients,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Energy
                        NutrientCategoryHeader("⚡ Energy")
                        NutrientDetailRow("Calories", "${recipe.calories} kcal")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Macronutrients
                        NutrientCategoryHeader("🥩 Macronutrients")
                        NutrientDetailRow("Protein", "${recipe.protein} g")
                        NutrientDetailRow("Carbohydrates", "${recipe.carbs} g")
                        NutrientDetailRow("Total Fat", "${recipe.fats} g")
                        NutrientDetailRow("Dietary Fiber", "${formatNutrientValue(recipe.fiber)} g")
                        NutrientDetailRow("Sugar", "${formatNutrientValue(recipe.sugar)} g")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Minerals
                        NutrientCategoryHeader("⛏️ Minerals")
                        NutrientDetailRow("Sodium", "${recipe.sodium} mg")
                        NutrientDetailRow("Potassium", "${formatNutrientValue(recipe.potassium)} mg")
                        NutrientDetailRow("Calcium", "${formatNutrientValue(recipe.calcium)} mg")
                        NutrientDetailRow("Iron", "${formatNutrientValue(recipe.iron)} mg")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vitamins
                        NutrientCategoryHeader("💊 Vitamins")
                        NutrientDetailRow("Vitamin A", "${formatNutrientValue(recipe.vitaminA)} µg")
                        NutrientDetailRow("Vitamin C", "${formatNutrientValue(recipe.vitaminC)} mg")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Ingredients List
        Text("Ingredients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recipe.ingredientDetails.forEach { detail ->
                val isMissingCore = recipe.missingCoreIngredients.contains(detail.name)
                val isMissingOptional = recipe.missingOptionalIngredients.contains(detail.name)
                val isMissing = isMissingCore || isMissingOptional
                val bgColor = when {
                    isMissingCore -> Color(0xFFFFF7ED)
                    isMissingOptional -> Color(0xFFFEFCE8)
                    else -> Color(0xFFF9FAFB)
                }
                val borderColor = when {
                    isMissingCore -> Color(0xFFFFEDD5)
                    isMissingOptional -> Color(0xFFFEF9C3)
                    else -> Color.Transparent
                }
                val iconColor = when {
                    isMissingCore -> CalorieKoOrange
                    isMissingOptional -> Color(0xFFCA8A04)
                    else -> CalorieKoGreen
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(20.dp).background(iconColor, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (isMissing) Icons.Rounded.Warning else Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Ingredient name with optional preparation method
                        val displayName = viewModel.formatIngredientName(detail.name)
                        val nameWithPrep = if (detail.preparationMethod.isNotBlank()) {
                            "$displayName, ${detail.preparationMethod}"
                        } else {
                            displayName
                        }
                        Text(
                            nameWithPrep,
                            fontSize = 14.sp,
                            color = if (isMissingCore) CalorieKoOrange else if (isMissingOptional) Color(0xFFCA8A04) else Color(0xFF374151),
                            fontWeight = if (isMissing) FontWeight.Medium else FontWeight.Normal
                        )
                        // Portion quantity
                        if (detail.portionQuantity.isNotBlank()) {
                            Text(
                                detail.portionQuantity,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    // Core / Optional badge
                    if (isMissingCore) {
                        Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(4.dp)) {
                            Text("Core", fontSize = 9.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    } else if (isMissingOptional) {
                        Surface(color = Color(0xFFFEF9C3), shape = RoundedCornerShape(4.dp)) {
                            Text("Optional", fontSize = 9.sp, color = Color(0xFFCA8A04), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Source Attribution Card (Interactive)
        val context = LocalContext.current
        val sourceLabel = when (recipe.dataSource) {
            "DOST_FNRI_MENU_GUIDE" -> "DOST-FNRI Menu Guide"
            "DOST_FNRI_FCT" -> "DOST-FNRI FCT"
            "USDA_FNDDS" -> "USDA FNDDS"
            else -> recipe.dataSource
        }
        val (sourceTextColor, sourceBgColor) = when (recipe.dataSource) {
            "DOST_FNRI_MENU_GUIDE" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
            "DOST_FNRI_FCT" -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))
            "USDA_FNDDS" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
            else -> Pair(Color(0xFF374151), Color(0xFFF3F4F6))
        }
        val proofDoc = remember(recipe.dishLabel, recipe.dataSource) {
            getDishProofDocument(recipe.dishLabel, recipe.dataSource)
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = sourceBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = sourceTextColor.copy(alpha = 0.15f), shape = CircleShape) {
                        Text("📊", modifier = Modifier.padding(8.dp), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nutritional data sourced from",
                            fontSize = 11.sp,
                            color = sourceTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            sourceLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = sourceTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Primary: View Source Document
                    if (proofDoc.type != ProofType.NONE) {
                        val proofLabel = when (proofDoc.type) {
                            ProofType.URL -> "View on USDA"
                            ProofType.PDF_ASSET -> "View Source PDF"
                            else -> ""
                        }
                        Surface(
                            onClick = {
                                when (proofDoc.type) {
                                    ProofType.URL -> {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(proofDoc.path))
                                        context.startActivity(intent)
                                    }
                                    ProofType.PDF_ASSET -> {
                                        openPdfFromAssets(context, proofDoc.path)
                                    }
                                    else -> {}
                                }
                            },
                            color = sourceTextColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    proofLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Secondary: Visit Database
                    Surface(
                        onClick = {
                            val url = getSourceDatabaseUrl(recipe.dataSource)
                            if (url.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        color = sourceTextColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .then(
                                if (proofDoc.type != ProofType.NONE) Modifier.weight(1f)
                                else Modifier.fillMaxWidth()
                            )
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Visit Database",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = sourceTextColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showPlanDialog.value = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add to Meal Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Step 1: Add to Plan — Pick a day (from Detail Sheet)
    if (showPlanDialog.value) {
        AlertDialog(
            onDismissRequest = { showPlanDialog.value = false },
            title = { Text("Plan Meal") },
            text = {
                Column {
                    Text("Select a day to cook ${recipe.dishName}:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CalorieKoGreen.copy(alpha = 0.1f), CircleShape)
                                    .clickable {
                                        selectedDayForPlan.intValue = index
                                        showPlanDialog.value = false
                                        showSlotPickerForPlan.value = true
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
            dismissButton = { TextButton(onClick = { showPlanDialog.value = false }) { Text("Cancel") } }
        )
    }

    // Step 2: Pick a meal slot (from Detail Sheet)
    if (showSlotPickerForPlan.value) {
        AlertDialog(
            onDismissRequest = { showSlotPickerForPlan.value = false },
            title = { Text("Choose Meal Slot") },
            text = {
                Column {
                    Text("Add ${recipe.dishName} on ${days[selectedDayForPlan.intValue]} as:")
                    Spacer(modifier = Modifier.height(16.dp))
                    mealSlots.forEach { slot ->
                        val alreadyInSlot = plannedMeals.any {
                            it.dayIndex == selectedDayForPlan.intValue && it.mealSlot == slot && it.dishLabel == recipe.dishLabel
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!alreadyInSlot) {
                                        viewModel.addMealToPlan(
                                            selectedDayForPlan.intValue,
                                            recipe.dishLabel,
                                            slot
                                        )
                                    }
                                    showSlotPickerForPlan.value = false
                                    onAddToPlan()
                                },
                            color = slotColors[slot] ?: Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp),
                            border = if (alreadyInSlot) BorderStroke(1.5.dp, CalorieKoGreen) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(slotEmojis[slot] ?: "", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    slot,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (alreadyInSlot) Color(0xFF9CA3AF) else Color(0xFF374151),
                                    modifier = Modifier.weight(1f)
                                )
                                if (alreadyInSlot) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Added", fontSize = 9.sp, color = CalorieKoGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSlotPickerForPlan.value = false }) { Text("Cancel") } }
        )
    }
}

// --- Utility: Map dish labels to emojis ---
fun getDishEmoji(dishLabel: String): String {
    return when {
        dishLabel.contains("sinigang") -> "🥘"
        dishLabel.contains("tinola") || dishLabel.contains("chicken") -> "🍗"
        dishLabel.contains("adobo") -> "🍲"
        dishLabel.contains("pinakbet") || dishLabel.contains("chopseuy") -> "🥗"
        dishLabel.contains("egg") -> "🍳"
        dishLabel.contains("fish") || dishLabel.contains("bangus") || dishLabel.contains("galunggong") || dishLabel.contains("tilapya") || dishLabel.contains("milkfish") || dishLabel.contains("mackerel") -> "🐟"
        dishLabel.contains("pork") || dishLabel.contains("menudo") || dishLabel.contains("sisig") -> "🥩"
        dishLabel.contains("rice") -> "🍚"
        dishLabel.contains("udong") || dishLabel.contains("noodle") -> "🍜"
        dishLabel.contains("kinilaw") -> "🥗"
        dishLabel.contains("tinapa") -> "🐟"
        else -> "🍽️"
    }
}

@Composable
fun NutritionCard(value: String, unit: String, subtext: String, progress: Float, color: Color, bgColor: Color, modifier: Modifier) {
    // Animate Bar
    val animatedProgress = animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "bar")

    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 14.sp, color = Color(0xFF4B5563), modifier = Modifier.padding(bottom = 4.dp))
            }
            Column {
                Text(subtext, fontSize = 11.sp, color = Color(0xFF4B5563))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White, CircleShape)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.value.coerceIn(0f, 1f)).background(color, CircleShape))
                }
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

// --- Full Nutrient Detail Components ---

@Composable
fun NutrientCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF374151),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun NutrientDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF6B7280))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
    }
}

/**
 * Formats a float nutrient value to one decimal place, removing trailing ".0" for whole numbers.
 */
fun formatNutrientValue(value: Float): String {
    return if (value == value.toInt().toFloat()) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

/**
 * Returns a DishProofDocument for a given dish, matching the logic in ExploreViewModel.
 * Used by RecipeDetailContent in PantryScreen.
 */
private fun getDishProofDocument(mlLabel: String, dataSource: String): DishProofDocument {
    val usdaUrls = mapOf(
        "egg_sunny" to "https://fdc.nal.usda.gov/food-details/2707158/nutrients",
        "egg_boiled" to "https://fdc.nal.usda.gov/food-details/173424/nutrients",
        "egg_fried" to "https://fdc.nal.usda.gov/food-details/2707200/nutrients",
        "chicken_drumstick" to "https://fdc.nal.usda.gov/food-details/171126/nutrients",
        "chicken_thigh" to "https://fdc.nal.usda.gov/food-details/171127/nutrients",
        "chicken_wings" to "https://fdc.nal.usda.gov/food-details/172830/nutrients",
        "chicken_breast" to "https://fdc.nal.usda.gov/food-details/171125/nutrients"
    )

    return when (dataSource) {
        "USDA_FNDDS" -> {
            val url = usdaUrls[mlLabel] ?: ""
            if (url.isNotEmpty()) DishProofDocument(ProofType.URL, url)
            else DishProofDocument(ProofType.NONE, "")
        }
        "DOST_FNRI_MENU_GUIDE", "DOST_FNRI_FCT" -> {
            DishProofDocument(ProofType.PDF_ASSET, "sources/$mlLabel.pdf")
        }
        else -> DishProofDocument(ProofType.NONE, "")
    }
}

/**
 * Returns the general database URL for a data source key.
 */
private fun getSourceDatabaseUrl(source: String): String {
    return when (source) {
        "DOST_FNRI_MENU_GUIDE" -> "https://www.fnri.dost.gov.ph/index.php/tools-and-standard/fnri-menu-guide-calendar"
        "DOST_FNRI_FCT" -> "https://i.fnri.dost.gov.ph/login/fct"
        "USDA_FNDDS" -> "https://fdc.nal.usda.gov/food-search?type=Survey%20(FNDDS)"
        else -> ""
    }
}

/**
 * Copies a PDF from the app's assets to the cache directory
 * and opens it with an external PDF viewer via FileProvider.
 */
private fun openPdfFromAssets(context: android.content.Context, assetPath: String) {
    try {
        val fileName = assetPath.substringAfterLast("/")

        val cacheDir = File(context.cacheDir, "source_pdfs")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val outFile = File(cacheDir, fileName)

        context.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}