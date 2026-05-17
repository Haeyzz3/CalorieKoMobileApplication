package com.calorieko.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.local.IngredientNutritionBreakdown
import androidx.compose.runtime.LaunchedEffect
import com.calorieko.app.ui.components.BottomNavigation
import com.calorieko.app.ui.components.SimpleFlowRow
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.CalorieKoOrange
import com.calorieko.app.ui.theme.MacroFat
import com.calorieko.app.viewmodel.DishProofDocument
import com.calorieko.app.viewmodel.DishResult
import com.calorieko.app.viewmodel.PantryViewModel
import com.calorieko.app.viewmodel.PantryUiEvent
import com.calorieko.app.viewmodel.ProofType
import com.calorieko.app.viewmodel.WeekInfo
import com.calorieko.app.util.PortionScaler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

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
    val storeBoughtRecipes by viewModel.storeBoughtDishes.collectAsState()
    val autocompleteSuggestions by viewModel.autocompleteSuggestions.collectAsState()
    val plannedMeals by viewModel.plannedMeals.collectAsState()
    val weeklyCalories by viewModel.weeklyCalories.collectAsState()
    val avgDailySodium by viewModel.avgDailySodium.collectAsState()
    val pantryByCategory by viewModel.pantryItemsByCategory.collectAsState()
    val allBrowsableIngredients by viewModel.allBrowsableIngredients.collectAsState()

    // Bottom Sheet State for Recipe Details
    val selectedRecipe = remember { mutableStateOf<DishResult?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PantryUiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Ingredient Browser Sheet state
    val showIngredientBrowser = remember { mutableStateOf(false) }
    val browserSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keyboard Controller
    val keyboardController = LocalSoftwareKeyboardController.current

    // Clear Pantry Confirmation Dialog state
    val showClearPantryDialog = remember { mutableStateOf(false) }

    // Single ingredient removal confirmation state (null = no dialog)
    var ingredientPendingRemoval by remember { mutableStateOf<String?>(null) }

    // Collapsible pantry categories — tracks which categories are expanded
    val allPantryCategoryKeys = listOf("protein", "produce", "seasoning", "pantry_staple", "grain_starch")
    var expandedPantryCategories by remember { mutableStateOf(allPantryCategoryKeys.toSet()) }

    // Auto-expand/collapse based on item count threshold
    LaunchedEffect(pantryIngredients.size) {
        expandedPantryCategories = if (pantryIngredients.size <= 12) {
            allPantryCategoryKeys.toSet()
        } else {
            emptySet()
        }
    }

    fun handleAddIngredient() {
        if (searchQuery.isNotBlank()) {
            viewModel.addIngredient(searchQuery)
            viewModel.updateSearchQuery("")
            keyboardController?.hide()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Browse All Ingredients CTA
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showIngredientBrowser.value = true },
                        border = BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CalorieKoGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\uD83E\uDDFE", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Browse All Ingredients",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    "View categories, nutrition info & batch-add to pantry",
                                    fontSize = 12.sp,
                                    color = Color(0xFF16A34A),
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Browse",
                                tint = CalorieKoGreen,
                                modifier = Modifier.size(20.dp)
                            )
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
                                    "pantry_staple" to Pair("\uD83C\uDFE1 Pantry Staples", Color(0xFFDBEAFE)),
                                    "grain_starch" to Pair("\uD83C\uDF3E Grains & Starches", Color(0xFFFFF7ED))
                                )

                                categoryOrder.forEach { (categoryKey, labelAndColor) ->
                                    val (label, chipBgColor) = labelAndColor
                                    val items = pantryByCategory[categoryKey] ?: emptyList()
                                    if (items.isNotEmpty()) {
                                        val isExpanded = categoryKey in expandedPantryCategories

                                        // Clickable category header row
                                        Surface(
                                            color = chipBgColor.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedPantryCategories = if (isExpanded) {
                                                        expandedPantryCategories - categoryKey
                                                    } else {
                                                        expandedPantryCategories + categoryKey
                                                    }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    shape = RoundedCornerShape(50)
                                                ) {
                                                    Text(
                                                        "${items.size}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                    tint = Color(0xFF9CA3AF),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Expandable chip area
                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
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
                                                                        ingredientPendingRemoval = ingredient
                                                                    }
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
                                "Browse all supported dishes, view ingredients & sources",
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

                    if (storeBoughtRecipes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        RecipeRow("Store-Bought Items", storeBoughtRecipes, Color(0xFF0284C7)) { selectedRecipe.value = it }
                    }

                    if (readyRecipes.isEmpty() && almostReadyRecipes.isEmpty() && storeBoughtRecipes.isEmpty()) {
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
                    allRecipes = readyRecipes + almostReadyRecipes,
                    storeBoughtRecipes = storeBoughtRecipes
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

    // Single Ingredient Removal Confirmation Dialog
    if (ingredientPendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { ingredientPendingRemoval = null },
            title = { Text("Remove Ingredient") },
            text = {
                Text("Remove ${viewModel.formatIngredientName(ingredientPendingRemoval!!)} from your pantry?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeIngredient(ingredientPendingRemoval!!)
                    ingredientPendingRemoval = null
                }) {
                    Text("Remove", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ingredientPendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ingredient Browser Bottom Sheet
    if (showIngredientBrowser.value) {
        ModalBottomSheet(
            onDismissRequest = { showIngredientBrowser.value = false },
            sheetState = browserSheetState,
            containerColor = Color.White
        ) {
            IngredientBrowserSheet(
                allIngredients = allBrowsableIngredients,
                currentPantryItems = pantryIngredients,
                formatName = { viewModel.formatIngredientName(it) },
                onApply = { selectedKeys ->
                    viewModel.batchUpdatePantry(selectedKeys)
                    showIngredientBrowser.value = false
                },
                onDismiss = { showIngredientBrowser.value = false }
            )
        }
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

private fun DishResult.primaryDishName(): String {
    return dishNamePh.ifBlank { dishNameEn.ifBlank { dishName } }
}

private fun DishResult.secondaryDishName(): String {
    val primary = primaryDishName()
    return dishNameEn.trim().takeIf {
        it.isNotBlank() && !it.equals(primary, ignoreCase = true)
    } ?: ""
}

private fun DishResult.inlineDishName(): String {
    val secondary = secondaryDishName()
    return if (secondary.isNotBlank()) {
        "${primaryDishName()} ($secondary)"
    } else {
        primaryDishName()
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
            .height(216.dp)
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
                recipe.primaryDishName(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val secondaryDishName = recipe.secondaryDishName()
            if (secondaryDishName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    secondaryDishName,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${recipe.calories} kcal", fontSize = 12.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                Text(" • ", fontSize = 12.sp, color = Color.LightGray)
                Text("${recipe.sodium}mg Na", fontSize = 12.sp, color = Color(0xFF9CA3AF))
            }

            // Core ingredient match info (or store-bought label)
            Spacer(modifier = Modifier.height(4.dp))
            if (recipe.coreTotalCount > 0) {
                Text(
                    "${recipe.coreMatchedCount}/${recipe.coreTotalCount} Core Ingredients",
                    fontSize = 11.sp,
                    color = if (isReady) CalorieKoGreen else CalorieKoOrange,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    "🛒 Store-Bought Item",
                    fontSize = 11.sp,
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Medium
                )
            }

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

private sealed interface MealPlanCopySource {
    data class Week(val sourceWeekStart: String) : MealPlanCopySource
    data class MealSlot(
        val sourceWeekStart: String,
        val dayIndex: Int,
        val mealSlot: String,
        val dishCount: Int
    ) : MealPlanCopySource
    data class Dish(val meal: PlannedMealEntity) : MealPlanCopySource
}

// --- Meal Plan Calendar Section ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanCalendarSection(
    viewModel: PantryViewModel,
    plannedMeals: List<PlannedMealEntity>,
    weeklyCalories: Int,
    avgDailySodium: Int,
    allRecipes: List<DishResult>,
    storeBoughtRecipes: List<DishResult> = emptyList()
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
    var isTodayPillSelected by remember { mutableStateOf(false) }

    // "View Recipe" bottom sheet state (from Meal Detail Dialog)
    var viewRecipeDishResult = remember { mutableStateOf<DishResult?>(null) }
    var showRecipeSheet = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Nutrition data for dishes in the open slot (loaded when dialog opens)
    var slotNutritionMap = remember { mutableStateOf<Map<String, PantryViewModel.CompactDishNutrition>>(emptyMap()) }
    var slotDishResults = remember { mutableStateOf<Map<String, DishResult>>(emptyMap()) }

    // Load nutrition when meal detail dialog opens
    LaunchedEffect(showMealDetail.value, detailDayIndex.intValue, detailSlot.value) {
        if (showMealDetail.value) {
            val meals = plannedMeals.filter {
                it.dayIndex == detailDayIndex.intValue && it.mealSlot == detailSlot.value
            }
            val nutritionMap = mutableMapOf<String, PantryViewModel.CompactDishNutrition>()
            val dishResultMap = mutableMapOf<String, DishResult>()
            withContext(Dispatchers.IO) {
                meals.forEach { meal ->
                    nutritionMap[meal.dishLabel] = viewModel.getCompactNutrition(
                        meal.dishLabel,
                        meal.substitutionsJson,
                        meal.scaledServings,
                        meal.tweaksJson
                    )
                    viewModel.getDishResultByLabel(
                        meal.dishLabel,
                        meal.substitutionsJson,
                        meal.scaledServings,
                        meal.tweaksJson
                    )?.let { result ->
                        dishResultMap[meal.dishLabel] = result
                    }
                }
            }
            slotNutritionMap.value = nutritionMap
            slotDishResults.value = dishResultMap
        } else {
            slotNutritionMap.value = emptyMap()
            slotDishResults.value = emptyMap()
        }
    }

    // Sodium warning threshold (mg)
    val sodiumWarning = avgDailySodium > 2000

    // Collect navigation state from ViewModel
    val displayedMonth by viewModel.displayedMonth.collectAsState()
    val weeksInMonth by viewModel.weeksInMonth.collectAsState()
    val currentWeekStart by viewModel.currentWeekStart.collectAsState()
    val weekDayDates by viewModel.weekDayDates.collectAsState()
    val todayColumnIndex by viewModel.todayColumnIndex.collectAsState()

    var copySource by remember { mutableStateOf<MealPlanCopySource?>(null) }
    var copyTargetWeekStart by remember { mutableStateOf(currentWeekStart) }
    var copyTargetDayIndex by remember { mutableIntStateOf(-1) }
    var copyTargetSlot by remember { mutableStateOf<String?>(null) }
    var copyTargetWeekMeals by remember { mutableStateOf<List<PlannedMealEntity>>(emptyList()) }
    val showCopyWeekDialog = remember { mutableStateOf(false) }

    // Fallback day names if weekDayDates hasn't loaded yet
    val days = if (weekDayDates.isNotEmpty()) weekDayDates else listOf(
        "Mon" to 0, "Tue" to 0, "Wed" to 0, "Thu" to 0, "Fri" to 0, "Sat" to 0, "Sun" to 0
    )

    // Check if the entire displayed week is editable (has at least one editable day)
    val hasEditableDay = days.indices.any { viewModel.isDayEditable(it) }

    // Combined recipe list for calendar pickers (recipes + store-bought)
    val allAvailableRecipes = allRecipes + storeBoughtRecipes

    fun openCopyTargetPicker(source: MealPlanCopySource) {
        val todayWeek = viewModel.getCurrentWeekStartDate()
        val maxWeek = viewModel.getMaxPlanningWeekStartPublic()
        val displayedWeekHasEditableDay = currentWeekStart in todayWeek..maxWeek &&
            days.indices.any { viewModel.isDayEditableForWeek(it, currentWeekStart) }
        copySource = source
        copyTargetWeekStart = if (displayedWeekHasEditableDay) currentWeekStart else todayWeek
        copyTargetDayIndex = -1
        copyTargetSlot = null
    }

    LaunchedEffect(copySource, copyTargetWeekStart, currentWeekStart, plannedMeals) {
        copyTargetWeekMeals = if (copySource == null) {
            emptyList()
        } else if (copyTargetWeekStart == currentWeekStart) {
            plannedMeals
        } else {
            withContext(Dispatchers.IO) {
                viewModel.getPlannedMealsForWeekSnapshot(copyTargetWeekStart)
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Section Title
        Text(
            "Meal Plan Calendar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── Month Header ── (◀ April 2026 ▶ + Today pill)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        isTodayPillSelected = false
                        viewModel.navigateMonth(-1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = Color(0xFF6B7280)
                    )
                }
                // Clickable month+year — tapping the year shows a year picker dropdown
                Box {
                    var showYearPicker by remember { mutableStateOf(false) }
                    Text(
                        "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.clickable { showYearPicker = true }
                    )
                    DropdownMenu(
                        expanded = showYearPicker,
                        onDismissRequest = { showYearPicker = false }
                    ) {
                        val currentYear = java.time.Year.now().value
                        // Show a reasonable range: 2020 through current year + 2
                        (2020..(currentYear + 2)).forEach { year ->
                            DropdownMenuItem(
                                text = { Text("$year", fontSize = 15.sp) },
                                onClick = {
                                    isTodayPillSelected = false
                                    viewModel.navigateToYear(year)
                                    showYearPicker = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        isTodayPillSelected = false
                        viewModel.navigateMonth(1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = Color(0xFF6B7280)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.clickable {
                        isTodayPillSelected = true
                        viewModel.navigateToToday()
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isTodayPillSelected) CalorieKoGreen else Color(0xFFE5E7EB)
                    )
                ) {
                    Text(
                        "Today",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTodayPillSelected) CalorieKoGreen else Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Week Scrubber ── (horizontally scrollable week pills)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            items(weeksInMonth) { weekInfo ->
                WeekPill(
                    weekInfo = weekInfo,
                    isSelected = weekInfo.weekStartDate == currentWeekStart,
                    onClick = {
                        if (!weekInfo.isBeyondHorizon) {
                            isTodayPillSelected = false
                            viewModel.selectWeek(weekInfo.weekStartDate)
                        }
                    }
                )
            }
        }

        // ── Action Buttons Row ── (Clear Week, Copy to Next Week)
        if (hasEditableDay) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy to Next Week
                if (plannedMeals.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.clickable { showCopyWeekDialog.value = true },
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy to Next Week", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3B82F6))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Clear Week
                if (plannedMeals.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.clickable { showClearWeekDialog.value = true },
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Clear Week", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
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
                // Days Header with date numbers + today highlight
                Row(modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFF3F4F6))) {
                    Box(
                        modifier = Modifier.width(56.dp).padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("", fontSize = 10.sp)
                    }
                    days.forEachIndexed { dayIdx, (dayName, dateNum) ->
                        val isToday = todayColumnIndex == dayIdx
                        val isDayEditable = viewModel.isDayEditable(dayIdx)
                        val dayHasMeals = plannedMeals.any { it.dayIndex == dayIdx }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isToday) Modifier.background(CalorieKoGreen.copy(alpha = 0.06f))
                                    else Modifier
                                )
                                .alpha(if (isDayEditable) 1f else 0.45f)
                                .padding(vertical = 6.dp)
                                .clickable {
                                    if (dayHasMeals && isDayEditable) {
                                        clearDayIndex.intValue = dayIdx
                                        showClearDayDialog.value = true
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isToday) CalorieKoGreen else if (dayHasMeals) Color(0xFF374151) else Color(0xFF6B7280)
                            )
                            // Date number
                            if (dateNum > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .then(
                                            if (isToday) Modifier
                                                .size(20.dp)
                                                .background(CalorieKoGreen, CircleShape)
                                            else Modifier.size(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dateNum",
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) Color.White else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
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
                            val isToday = todayColumnIndex == dayIdx
                            val isDayEditable = viewModel.isDayEditable(dayIdx)
                            val slotMeals = plannedMeals.filter { it.dayIndex == dayIdx && it.mealSlot == slot }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .then(
                                        if (isToday) Modifier.background(CalorieKoGreen.copy(alpha = 0.03f))
                                        else Modifier
                                    )
                                    .border(0.5.dp, Color(0xFFF3F4F6))
                                    .alpha(if (isDayEditable) 1f else 0.45f)
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

        // Quick Add — only show when the displayed week has editable days
        if (allAvailableRecipes.isNotEmpty() && hasEditableDay) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Add to Calendar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allAvailableRecipes.take(5)) { recipe ->
                            val isStoreBought = recipe.coreTotalCount == 0
                            SuggestionChip(
                                onClick = {
                                    recipeToAdd.value = recipe
                                    showAddDialog.value = true
                                },
                                label = { Text("${getDishEmoji(recipe.dishLabel)} ${recipe.inlineDishName()}") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = when {
                                        isStoreBought -> Color(0xFFF0F9FF)
                                        recipe.missingCoreIngredients.isEmpty() -> Color(0xFFECFDF5)
                                        else -> Color(0xFFFFEDD5)
                                    },
                                    labelColor = Color(0xFF1F2937)
                                ),
                                border = BorderStroke(1.dp, when {
                                    isStoreBought -> Color(0xFF0284C7)
                                    recipe.missingCoreIngredients.isEmpty() -> CalorieKoGreen
                                    else -> CalorieKoOrange
                                })
                            )
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Meal Detail Dialog (tapping a populated cell) — Enhanced with nutrition
    // ================================================================
    if (showMealDetail.value) {
        val dayIdx = detailDayIndex.intValue
        val slot = detailSlot.value
        val slotMeals = plannedMeals.filter { it.dayIndex == dayIdx && it.mealSlot == slot }
        val isEditable = viewModel.isDayEditable(dayIdx)
        val nutrition = slotNutritionMap.value
        val slotDishDisplayResults = slotDishResults.value

        // Compute meal totals from loaded nutrition
        val totalCalories = slotMeals.sumOf { nutrition[it.dishLabel]?.calories ?: 0 }
        val totalProtein = slotMeals.sumOf { nutrition[it.dishLabel]?.protein ?: 0 }
        val totalCarbs = slotMeals.sumOf { nutrition[it.dishLabel]?.carbs ?: 0 }
        val totalFats = slotMeals.sumOf { nutrition[it.dishLabel]?.fats ?: 0 }

        Dialog(onDismissRequest = { showMealDetail.value = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(slotEmojis[slot] ?: "", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${days[dayIdx].first} ${days[dayIdx].second} \u2014 $slot",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            if (slotMeals.isNotEmpty() && nutrition.isNotEmpty()) {
                                Text(
                                    "${slotMeals.size} dish${if (slotMeals.size > 1) "es" else ""} \u00B7 $totalCalories kcal total",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else if (slotMeals.isNotEmpty()) {
                                Text("${slotMeals.size} dish${if (slotMeals.size > 1) "es" else ""}", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Text("No dishes planned", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (slotMeals.isEmpty()) {
                            item {
                                Text(
                                    if (isEditable) "No dishes added to this meal yet. Tap below to add one!"
                                    else "No dishes were planned for this meal.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            if (nutrition.isNotEmpty() && slotMeals.size > 1) {
                                item(key = "meal-total-macros") {
                                    Surface(
                                        color = Color(0xFFF9FAFB),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$totalCalories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CalorieKoGreen)
                                                Text("kcal", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${totalProtein}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                                Text("protein", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${totalCarbs}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                                Text("carbs", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("${totalFats}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MacroFat)
                                                Text("fat", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }

                            items(slotMeals, key = { it.dishLabel }) { meal ->
                                val dishNutrition = nutrition[meal.dishLabel]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = slotColors[slot] ?: Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val dishResult = slotDishDisplayResults[meal.dishLabel]
                                            val primaryDishName = dishResult?.primaryDishName()
                                                ?: viewModel.formatIngredientName(meal.dishLabel)
                                            val secondaryDishName = dishResult?.secondaryDishName().orEmpty()
                                            Text(getDishEmoji(meal.dishLabel), fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    primaryDishName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF374151),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (secondaryDishName.isNotBlank()) {
                                                    Text(
                                                        secondaryDishName,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF9CA3AF),
                                                        fontStyle = FontStyle.Italic,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (meal.substitutionsJson.isNotEmpty() || meal.scaledServings > 0 || meal.tweaksJson.isNotEmpty()) {
                                                Surface(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Text(
                                                        "customized",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (isEditable) {
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

                                        if (dishNutrition != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${dishNutrition.calories} kcal \u00B7 ${dishNutrition.protein}g P \u00B7 ${dishNutrition.carbs}g C \u00B7 ${dishNutrition.fats}g F",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280),
                                                modifier = Modifier.padding(start = 30.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.padding(start = 30.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "View Recipe \u25B8",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CalorieKoGreen,
                                                modifier = Modifier.clickable {
                                                    scope.launch {
                                                        val result = withContext(Dispatchers.IO) {
                                                            viewModel.getDishResultByLabel(
                                                                meal.dishLabel,
                                                                meal.substitutionsJson,
                                                                meal.scaledServings,
                                                                meal.tweaksJson
                                                            )
                                                        }
                                                        if (result != null) {
                                                            viewRecipeDishResult.value = result
                                                            showMealDetail.value = false
                                                            showRecipeSheet.value = true
                                                        }
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "Copy Dish",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF3B82F6),
                                                modifier = Modifier.clickable {
                                                    showMealDetail.value = false
                                                    openCopyTargetPicker(MealPlanCopySource.Dish(meal))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (slotMeals.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMealDetail.value = false
                                        openCopyTargetPicker(
                                            MealPlanCopySource.MealSlot(
                                                sourceWeekStart = currentWeekStart,
                                                dayIndex = dayIdx,
                                                mealSlot = slot,
                                                dishCount = slotMeals.size
                                            )
                                        )
                                    },
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Meal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3B82F6))
                                }
                            }
                        }

                        if (allAvailableRecipes.isNotEmpty() && isEditable) {
                            if (slotMeals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
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

                        if (slotMeals.isNotEmpty() && isEditable) {
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
                                    Text("Clear Entire Meal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                                }
                            }
                        }

                        TextButton(
                            onClick = { showMealDetail.value = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    copySource?.let { source ->
        MealPlanCopyTargetDialog(
            source = source,
            targetWeekStart = copyTargetWeekStart,
            targetDayIndex = copyTargetDayIndex,
            targetSlot = copyTargetSlot,
            plannedMealsForTargetWeek = copyTargetWeekMeals,
            onTargetWeekChange = {
                copyTargetWeekStart = it
                copyTargetDayIndex = -1
                copyTargetSlot = null
            },
            onTargetDayChange = {
                copyTargetDayIndex = it
                copyTargetSlot = null
            },
            onTargetSlotChange = { copyTargetSlot = it },
            onConfirm = {
                when (source) {
                    is MealPlanCopySource.Week -> Unit
                    is MealPlanCopySource.MealSlot -> viewModel.copyMealSlot(
                        sourceWeekStart = source.sourceWeekStart,
                        sourceDayIndex = source.dayIndex,
                        sourceMealSlot = source.mealSlot,
                        targetWeekStart = copyTargetWeekStart,
                        targetDayIndex = copyTargetDayIndex,
                        targetMealSlot = copyTargetSlot.orEmpty()
                    )
                    is MealPlanCopySource.Dish -> viewModel.copySingleDish(
                        sourceMeal = source.meal,
                        targetWeekStart = copyTargetWeekStart,
                        targetDayIndex = copyTargetDayIndex,
                        targetMealSlot = copyTargetSlot.orEmpty()
                    )
                }
                copySource = null
            },
            onDismiss = { copySource = null },
            viewModel = viewModel
        )
    }

    // ================================================================
    // "View Recipe" Bottom Sheet (from Meal Detail Dialog)
    // ================================================================
    if (showRecipeSheet.value && viewRecipeDishResult.value != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showRecipeSheet.value = false
                viewRecipeDishResult.value = null
            },
            containerColor = Color.White
        ) {
            RecipeDetailContent(
                recipe = viewRecipeDishResult.value!!,
                viewModel = viewModel,
                plannedMeals = plannedMeals,
                onClose = {
                    showRecipeSheet.value = false
                    viewRecipeDishResult.value = null
                },
                onAddToPlan = {
                    showRecipeSheet.value = false
                    viewRecipeDishResult.value = null
                },
                isViewOnly = true
            )
        }
    }


    // ================================================================
    // "Add Dish to this slot" — recipe picker (from Meal Detail Dialog)
    // ================================================================
    if (showAddDishToSlot.value && allAvailableRecipes.isNotEmpty()) {
        val dayIdx = detailDayIndex.intValue
        val slot = detailSlot.value
        val existingDishLabels = plannedMeals
            .filter { it.dayIndex == dayIdx && it.mealSlot == slot }
            .map { it.dishLabel }
            .toSet()
        AlertDialog(
            onDismissRequest = { showAddDishToSlot.value = false },
            title = { Text("Add Dish to ${days[dayIdx].first} ${days[dayIdx].second} $slot") },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    allAvailableRecipes.forEach { recipe ->
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
                                    val secondaryDishName = recipe.secondaryDishName()
                                    Text(
                                        recipe.primaryDishName(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (alreadyAdded) Color(0xFF9CA3AF) else Color(0xFF374151)
                                    )
                                    if (secondaryDishName.isNotBlank()) {
                                        Text(
                                            secondaryDishName,
                                            fontSize = 11.sp,
                                            color = Color(0xFF9CA3AF),
                                            fontStyle = FontStyle.Italic,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
                    Text("Select a day to cook ${recipeToAdd.value?.inlineDishName().orEmpty()}:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { index, (dayName, dateNum) ->
                            val editable = viewModel.isDayEditable(index)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (editable) CalorieKoGreen.copy(alpha = 0.1f) else Color(0xFFF3F4F6),
                                        CircleShape
                                    )
                                    .alpha(if (editable) 1f else 0.4f)
                                    .clickable {
                                        if (editable) {
                                            selectedDayIndex.intValue = index
                                            showAddDialog.value = false
                                            showSlotPicker.value = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dayName.first().toString(), fontWeight = FontWeight.Bold, color = if (editable) CalorieKoGreen else Color.Gray)
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
                    Text("Add ${recipeToAdd.value?.inlineDishName().orEmpty()} on ${days[selectedDayIndex.intValue].first} ${days[selectedDayIndex.intValue].second} as:")
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
    // Copy Week Confirmation Dialog
    // ================================================================
    if (showCopyWeekDialog.value) {
        val targetWeekStart = LocalDate.parse(currentWeekStart)
            .plusWeeks(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        AlertDialog(
            onDismissRequest = { showCopyWeekDialog.value = false },
            title = { Text("Copy to Next Week?") },
            text = {
                Column {
                    Text("Source: ${mealPlanWeekRangeLabel(currentWeekStart)}")
                    Text("Target: ${mealPlanWeekRangeLabel(targetWeekStart)}")
                    Text("${plannedMeals.size} dish${if (plannedMeals.size == 1) "" else "es"} will be copied.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Existing meals in the target week will be replaced.", color = Color(0xFFDC2626))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.copyCurrentWeekToNextReplacing()
                    showCopyWeekDialog.value = false
                }) {
                    Text("Copy & Replace", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showCopyWeekDialog.value = false }) { Text("Cancel") } }
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
        val dayName = "${days[clearDayIndex.intValue].first} ${days[clearDayIndex.intValue].second}"
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

@Composable
private fun MealPlanCopyTargetDialog(
    source: MealPlanCopySource,
    targetWeekStart: String,
    targetDayIndex: Int,
    targetSlot: String?,
    plannedMealsForTargetWeek: List<PlannedMealEntity>,
    onTargetWeekChange: (String) -> Unit,
    onTargetDayChange: (Int) -> Unit,
    onTargetSlotChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: PantryViewModel
) {
    val mealSlots = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val slotEmojis = mapOf("Breakfast" to "B", "Lunch" to "L", "Dinner" to "D", "Snack" to "S")
    val slotColors = mapOf(
        "Breakfast" to Color(0xFFFFF7ED),
        "Lunch" to Color(0xFFECFDF5),
        "Dinner" to Color(0xFFEDE9FE),
        "Snack" to Color(0xFFFEF9C3)
    )
    val todayWeek = viewModel.getCurrentWeekStartDate()
    val maxWeek = viewModel.getMaxPlanningWeekStartPublic()
    val canGoBack = targetWeekStart > todayWeek
    val nextWeek = LocalDate.parse(targetWeekStart).plusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    val canGoForward = nextWeek <= maxWeek
    val targetWeekDayDates = viewModel.computeWeekDayDatesPublic(targetWeekStart)
    val selectedSlotMeals = if (targetDayIndex >= 0 && targetSlot != null) {
        plannedMealsForTargetWeek.filter { it.dayIndex == targetDayIndex && it.mealSlot == targetSlot }
    } else {
        emptyList()
    }
    val confirmEnabled = targetDayIndex in 0..6 && targetSlot != null &&
        viewModel.isDayEditableForWeek(targetDayIndex, targetWeekStart)

    val sourceSummary = when (source) {
        is MealPlanCopySource.Week -> "Week of ${mealPlanWeekRangeLabel(source.sourceWeekStart)}"
        is MealPlanCopySource.MealSlot -> {
            val dishCount = source.dishCount
            "${mealPlanDayLabel(source.sourceWeekStart, source.dayIndex)} ${source.mealSlot} - $dishCount dish${if (dishCount == 1) "" else "es"}"
        }
        is MealPlanCopySource.Dish -> {
            val customized = source.meal.substitutionsJson.isNotEmpty() ||
                source.meal.scaledServings > 0 ||
                source.meal.tweaksJson.isNotEmpty()
            buildString {
                append(viewModel.formatIngredientName(source.meal.dishLabel))
                if (customized) append(" - customized")
            }
        }
    }

    val conflictText = when (source) {
        is MealPlanCopySource.Week -> ""
        is MealPlanCopySource.MealSlot -> if (selectedSlotMeals.isNotEmpty()) {
            "This will replace ${selectedSlotMeals.size} existing dish${if (selectedSlotMeals.size == 1) "" else "es"}."
        } else {
            "This will copy the meal into an empty slot."
        }
        is MealPlanCopySource.Dish -> {
            val sameDishExists = selectedSlotMeals.any { it.dishLabel == source.meal.dishLabel }
            when {
                sameDishExists -> "This will replace the existing copy of this dish."
                selectedSlotMeals.isNotEmpty() -> "This will add this dish to the selected meal."
                else -> "This will copy the dish into an empty slot."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (source is MealPlanCopySource.MealSlot) "Copy Meal" else "Copy Dish") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(sourceSummary, fontSize = 13.sp, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val prev = LocalDate.parse(targetWeekStart)
                                    .minusWeeks(1)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                                if (prev >= todayWeek) onTargetWeekChange(prev)
                            },
                            enabled = canGoBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous week",
                                tint = if (canGoBack) Color(0xFF374151) else Color(0xFFD1D5DB)
                            )
                        }
                        Text(
                            mealPlanWeekRangeLabel(targetWeekStart),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                if (canGoForward) onTargetWeekChange(nextWeek)
                            },
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next week",
                                tint = if (canGoForward) Color(0xFF374151) else Color(0xFFD1D5DB)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Target day", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    targetWeekDayDates.forEachIndexed { index, (dayName, dateNum) ->
                        val editable = viewModel.isDayEditableForWeek(index, targetWeekStart)
                        val selected = targetDayIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.alpha(if (editable) 1f else 0.4f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        when {
                                            selected -> CalorieKoGreen
                                            editable -> CalorieKoGreen.copy(alpha = 0.1f)
                                            else -> Color(0xFFF3F4F6)
                                        },
                                        CircleShape
                                    )
                                    .clickable {
                                        if (editable) onTargetDayChange(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayName.first().toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = when {
                                        selected -> Color.White
                                        editable -> CalorieKoGreen
                                        else -> Color.Gray
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$dateNum", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Target meal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                Spacer(modifier = Modifier.height(6.dp))
                mealSlots.forEach { slot ->
                    val slotMeals = plannedMealsForTargetWeek.filter {
                        it.dayIndex == targetDayIndex && it.mealSlot == slot
                    }
                    val selected = targetSlot == slot
                    val canSelectSlot = targetDayIndex >= 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .alpha(if (canSelectSlot) 1f else 0.45f)
                            .clickable {
                                if (canSelectSlot) onTargetSlotChange(slot)
                            },
                        color = slotColors[slot] ?: Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(10.dp),
                        border = if (selected) BorderStroke(1.5.dp, CalorieKoGreen) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(slotEmojis[slot] ?: "", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(slot, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (slotMeals.isNotEmpty()) {
                                Text(
                                    "${slotMeals.size} dish${if (slotMeals.size == 1) "" else "es"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }

                if (targetSlot != null && conflictText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(conflictText, fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (confirmEnabled) onConfirm()
                },
                enabled = confirmEnabled
            ) {
                Text("Copy")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun mealPlanDayLabel(weekStartDate: String, dayIndex: Int): String {
    val date = LocalDate.parse(weekStartDate).plusDays(dayIndex.toLong())
    return date.format(DateTimeFormatter.ofPattern("MMM d"))
}

private fun mealPlanWeekRangeLabel(weekStartDate: String): String {
    val start = LocalDate.parse(weekStartDate)
    val end = start.plusDays(6)
    val startFormatter = DateTimeFormatter.ofPattern("MMM d")
    return if (start.month == end.month) {
        "${start.format(startFormatter)}-${end.dayOfMonth}"
    } else {
        "${start.format(startFormatter)}-${end.format(startFormatter)}"
    }
}

// --- Week Pill Composable (for the week scrubber) ---
@Composable
fun WeekPill(
    weekInfo: WeekInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> CalorieKoGreen.copy(alpha = 0.12f)
        weekInfo.isPast -> Color(0xFFF3F4F6)
        weekInfo.isBeyondHorizon -> Color(0xFFF9FAFB)
        else -> Color.White
    }
    val borderColor = when {
        isSelected -> CalorieKoGreen
        else -> Color(0xFFE5E7EB)
    }
    val textColor = when {
        weekInfo.isBeyondHorizon -> Color(0xFFD1D5DB)
        weekInfo.isPast -> Color(0xFF9CA3AF)
        isSelected -> CalorieKoGreen
        else -> Color(0xFF374151)
    }

    Surface(
        modifier = Modifier
            .clickable(enabled = !weekInfo.isBeyondHorizon) { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                weekInfo.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Density dots
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                val dotCount = when {
                    weekInfo.mealCount == 0 -> 0
                    weekInfo.mealCount <= 4 -> 1
                    weekInfo.mealCount <= 10 -> 2
                    else -> 3
                }
                repeat(dotCount) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                if (isSelected) CalorieKoGreen else Color(0xFF9CA3AF),
                                CircleShape
                            )
                    )
                }
                // Show a faint dot placeholder when no meals
                if (dotCount == 0) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFFE5E7EB), CircleShape)
                    )
                }
            }
            // Current week indicator
            if (weekInfo.isCurrentWeek) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("This week", fontSize = 8.sp, color = CalorieKoGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Recipe Detail Content (BottomSheet) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailContent(recipe: DishResult, viewModel: PantryViewModel, plannedMeals: List<PlannedMealEntity>, onClose: () -> Unit, onAddToPlan: () -> Unit, isViewOnly: Boolean = false) {
    val isReady = recipe.missingCoreIngredients.isEmpty()
    val scope = rememberCoroutineScope()

    // Use user's actual targets from ViewModel
    val userCalorieTarget by viewModel.userCalorieTarget.collectAsState()
    val userSodiumLimit by viewModel.userSodiumLimit.collectAsState()

    // --- Substitution state ---
    val allSubstitutions by viewModel.activeSubstitutions.collectAsState()
    val allSubNutrition by viewModel.substitutedNutrition.collectAsState()
    val dishSubs = if (isViewOnly) {
        recipe.appliedSubstitutions
    } else {
        allSubstitutions[recipe.dishLabel] ?: emptyMap()
    }
    val subNutrition = if (isViewOnly) null else allSubNutrition[recipe.dishLabel]
    val hasSubstitutions = dishSubs.isNotEmpty()

    // --- Serving scaling state ---
    val scaledServingsMap by viewModel.scaledServings.collectAsState()
    val targetServings = if (isViewOnly) {
        recipe.appliedScaledServings.takeIf { it > 0 } ?: recipe.originalServings
    } else {
        scaledServingsMap[recipe.dishLabel] ?: recipe.originalServings
    }
    val multiplier = targetServings.toFloat() / recipe.originalServings.toFloat().coerceAtLeast(1f)
    val isScaled = targetServings != recipe.originalServings
    val maxServings = maxOf(recipe.originalServings * 4, 20)

    // --- Individual ingredient tweak state ---
    val allTweaks by viewModel.ingredientTweaks.collectAsState()
    val dishTweaks = if (isViewOnly) {
        recipe.appliedTweaks
    } else {
        allTweaks[recipe.dishLabel] ?: emptyMap()
    }
    val hasTweaks = dishTweaks.isNotEmpty()
    val tweakedNutritionMap by viewModel.tweakedNutrition.collectAsState()
    val tweakedNutrition = if (isViewOnly) null else tweakedNutritionMap[recipe.dishLabel]
    val tweakedWeightMap by viewModel.tweakedPerServingWeight.collectAsState()
    val tweakedPerServingWeight = if (isViewOnly && recipe.appliedTweakedPerServingWeightG > 0f) {
        recipe.appliedTweakedPerServingWeightG
    } else if (isViewOnly) {
        null
    } else {
        tweakedWeightMap[recipe.dishLabel]
    }

    // Substitution picker state
    var substitutionTarget by remember { mutableStateOf<String?>(null) }  // ingredientKey being substituted
    var substitutionCandidates by remember { mutableStateOf<List<RawIngredientEntity>>(emptyList()) }
    var isLoadingCandidates by remember { mutableStateOf(false) }

    // Effective nutrition: tweaks > subs > original
    val effectiveCalories = when {
        isViewOnly -> recipe.calories
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.calories.toInt()
        hasSubstitutions && subNutrition != null -> subNutrition.calories.toInt()
        else -> recipe.calories
    }
    val effectiveProtein = when {
        isViewOnly -> recipe.protein
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.protein.toInt()
        hasSubstitutions && subNutrition != null -> subNutrition.protein.toInt()
        else -> recipe.protein
    }
    val effectiveCarbs = when {
        isViewOnly -> recipe.carbs
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.carbs.toInt()
        hasSubstitutions && subNutrition != null -> subNutrition.carbs.toInt()
        else -> recipe.carbs
    }
    val effectiveFats = when {
        isViewOnly -> recipe.fats
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.fat.toInt()
        hasSubstitutions && subNutrition != null -> subNutrition.fat.toInt()
        else -> recipe.fats
    }
    val effectiveSodium = when {
        isViewOnly -> recipe.sodium
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.sodium.toInt()
        hasSubstitutions && subNutrition != null -> subNutrition.sodium.toInt()
        else -> recipe.sodium
    }
    val effectiveFiber = when {
        isViewOnly -> recipe.fiber
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.fiber
        hasSubstitutions && subNutrition != null -> subNutrition.fiber
        else -> recipe.fiber
    }
    val effectiveSugar = when {
        isViewOnly -> recipe.sugar
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.sugar
        hasSubstitutions && subNutrition != null -> subNutrition.sugar
        else -> recipe.sugar
    }
    val effectivePotassium = when {
        isViewOnly -> recipe.potassium
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.potassium
        hasSubstitutions && subNutrition != null -> subNutrition.potassium
        else -> recipe.potassium
    }
    val effectiveVitaminA = when {
        isViewOnly -> recipe.vitaminA
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.vitaminA
        hasSubstitutions && subNutrition != null -> subNutrition.vitaminA
        else -> recipe.vitaminA
    }
    val effectiveVitaminC = when {
        isViewOnly -> recipe.vitaminC
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.vitaminC
        hasSubstitutions && subNutrition != null -> subNutrition.vitaminC
        else -> recipe.vitaminC
    }
    val effectiveCalcium = when {
        isViewOnly -> recipe.calcium
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.calcium
        hasSubstitutions && subNutrition != null -> subNutrition.calcium
        else -> recipe.calcium
    }
    val effectiveIron = when {
        isViewOnly -> recipe.iron
        hasTweaks && tweakedNutrition != null -> tweakedNutrition.iron
        hasSubstitutions && subNutrition != null -> subNutrition.iron
        else -> recipe.iron
    }

    val caloriePercent = if (userCalorieTarget > 0) (effectiveCalories / userCalorieTarget.toFloat()) else 0f
    val sodiumPercent = if (userSodiumLimit > 0) (effectiveSodium / userSodiumLimit.toFloat()) else 0f
    val sodiumColor = if (effectiveSodium <= 500) Color(0xFF16A34A) else if (effectiveSodium <= 800) Color(0xFFCA8A04) else Color(0xFFEA580C)

    // Combine all missing for convenience
    val allMissing = recipe.missingCoreIngredients + recipe.missingOptionalIngredients

    // Full nutrient toggle state
    var showFullNutrients by remember { mutableStateOf(false) }

    // Add-to-plan dialog state
    val showPlanDialog = remember { mutableStateOf(false) }
    val selectedDayForPlan = remember { mutableIntStateOf(-1) }
    val showSlotPickerForPlan = remember { mutableStateOf(false) }
    val mealSlots = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val slotEmojis = mapOf("Breakfast" to "☀️", "Lunch" to "🌤️", "Dinner" to "🌙", "Snack" to "🍿")
    val slotColors = mapOf(
        "Breakfast" to Color(0xFFFFF7ED),
        "Lunch" to Color(0xFFECFDF5),
        "Dinner" to Color(0xFFEDE9FE),
        "Snack" to Color(0xFFFEF9C3)
    )

    // --- Target week for planning (may differ from calendar's selected week) ---
    val calendarWeekStart by viewModel.currentWeekStart.collectAsState()
    var targetWeekStart by remember { mutableStateOf(calendarWeekStart) }
    var targetWeekDayDates by remember { mutableStateOf(viewModel.computeWeekDayDatesPublic(calendarWeekStart)) }
    var targetWeekMeals by remember { mutableStateOf(plannedMeals) }

    // Refresh day dates and meals when target week changes
    LaunchedEffect(targetWeekStart) {
        targetWeekDayDates = viewModel.computeWeekDayDatesPublic(targetWeekStart)
        targetWeekMeals = if (targetWeekStart == calendarWeekStart) {
            plannedMeals
        } else {
            withContext(Dispatchers.IO) {
                viewModel.getPlannedMealsForWeekSnapshot(targetWeekStart)
            }
        }
    }

    // Format target week range label for display
    val targetWeekLabel = remember(targetWeekStart) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
        val start = java.time.LocalDate.parse(targetWeekStart)
        val end = start.plusDays(6)
        "${start.format(formatter)} \u2013 ${end.format(formatter)}"
    }

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
                    Text(recipe.primaryDishName(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    val secondaryDishName = recipe.secondaryDishName()
                    if (secondaryDishName.isNotBlank()) {
                        Text(
                            secondaryDishName,
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF),
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (isViewOnly) {
                        // In view-only mode (calendar), show a simple "Planned Dish" badge
                        Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(50)) {
                            Text("\uD83D\uDCC5 Planned Dish", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF7C3AED), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (recipe.coreTotalCount == 0) {
                        // Store-bought item — no ingredients
                        Surface(color = Color(0xFFF0F9FF), shape = RoundedCornerShape(50)) {
                            Text("🛒 Store-Bought Item", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Core ingredient match info (only relevant when browsing recipes)
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
            }
            IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Serving Size Scaling Card ──
        if (recipe.coreTotalCount > 0 && !isViewOnly) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isScaled) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)),
                border = BorderStroke(1.dp, if (isScaled) CalorieKoGreen.copy(alpha = 0.3f) else Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Servings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                            Text(
                                "Original: ${recipe.originalServings} serving${if (recipe.originalServings > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { if (targetServings > 1) viewModel.setTargetServings(recipe.dishLabel, targetServings - 1) },
                                color = if (targetServings > 1) CalorieKoGreen.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("\u2013", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (targetServings > 1) CalorieKoGreen else Color(0xFFD1D5DB))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "$targetServings",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isScaled) CalorieKoGreen else Color(0xFF374151)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Surface(
                                onClick = { if (targetServings < maxServings) viewModel.setTargetServings(recipe.dishLabel, targetServings + 1) },
                                color = if (targetServings < maxServings) CalorieKoGreen.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (targetServings < maxServings) CalorieKoGreen else Color(0xFFD1D5DB))
                                }
                            }
                        }
                    }
                    // Total recipe weight (tweak-aware)
                    val totalWeightPerServing = if (hasTweaks && tweakedPerServingWeight != null) tweakedPerServingWeight else recipe.perServingWeightG
                    if (totalWeightPerServing > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalWeightText = "Total recipe: \u2248 ${(totalWeightPerServing * targetServings).toInt()}g" +
                            if (hasTweaks) " (est.)" else ""
                        Text(
                            totalWeightText,
                            fontSize = 12.sp,
                            color = when {
                                hasTweaks -> Color(0xFF7C3AED)
                                isScaled -> CalorieKoGreen
                                else -> Color(0xFF6B7280)
                            },
                            fontWeight = if (isScaled || hasTweaks) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    // Reset link
                    if (isScaled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Reset to original",
                            fontSize = 11.sp,
                            color = CalorieKoGreen,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { viewModel.resetTargetServings(recipe.dishLabel) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Read-only Servings Info (for planned dish view) ──
        if (recipe.coreTotalCount > 0 && isViewOnly) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isScaled) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)),
                border = BorderStroke(1.dp, if (isScaled) CalorieKoGreen.copy(alpha = 0.3f) else Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Servings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                            if (isScaled) {
                                Text(
                                    "Scaled from ${recipe.originalServings} to $targetServings serving${if (targetServings > 1) "s" else ""}",
                                    fontSize = 11.sp, color = CalorieKoGreen
                                )
                            } else {
                                Text(
                                    "${recipe.originalServings} serving${if (recipe.originalServings > 1) "s" else ""}",
                                    fontSize = 11.sp, color = Color(0xFF6B7280)
                                )
                            }
                        }
                        // Serving count badge
                        Surface(
                            color = if (isScaled) CalorieKoGreen else Color(0xFF374151),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$targetServings", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    // Total recipe weight
                    val viewOnlyWeight = if (hasTweaks && tweakedPerServingWeight != null) tweakedPerServingWeight else recipe.perServingWeightG
                    if (viewOnlyWeight > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Total recipe: \u2248 ${(viewOnlyWeight * targetServings).toInt()}g" +
                                if (hasTweaks) " (est.)" else "",
                            fontSize = 12.sp,
                            color = when {
                                hasTweaks -> Color(0xFF7C3AED)
                                isScaled -> CalorieKoGreen
                                else -> Color(0xFF6B7280)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Nutrition Cards
        if (recipe.calories > 0) {
            // Substitution Active Banner (not shown in view-only mode since subs are pre-applied)
            if (hasSubstitutions && !isViewOnly) {
                Surface(
                    color = Color(0xFFF0F9FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${dishSubs.size} Substitution${if (dishSubs.size > 1) "s" else ""} Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0C4A6E)
                            )
                            Text(
                                "Nutrition values updated below",
                                fontSize = 11.sp,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Surface(
                            onClick = { viewModel.clearSubstitutions(recipe.dishLabel) },
                            color = Color(0xFF0284C7).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ingredient Tweaks Active Banner
            if (hasTweaks && !isViewOnly) {
                Surface(
                    color = Color(0xFFF5F3FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uD83C\uDF9A\uFE0F", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${dishTweaks.size} Ingredient${if (dishTweaks.size > 1) "s" else ""} Tweaked",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5B21B6)
                            )
                            Text(
                                "Per-serving nutrition recalculated",
                                fontSize = 11.sp,
                                color = Color(0xFF7C3AED)
                            )
                        }
                        Surface(
                            onClick = { viewModel.clearIngredientTweaks(recipe.dishLabel) },
                            color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Per Serving Nutrition", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            // Per-serving weight: show tweaked estimate with (est.) when tweaks active
            val displayPerServingWeight = if (hasTweaks && tweakedPerServingWeight != null) {
                tweakedPerServingWeight
            } else {
                recipe.perServingWeightG
            }
            if (displayPerServingWeight > 0f) {
                Text(
                    if (hasTweaks) "\u2248 ${displayPerServingWeight.toInt()}g per serving (est.)"
                    else "\u2248 ${displayPerServingWeight.toInt()}g per serving",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasTweaks) Color(0xFF7C3AED) else Color(0xFF6B7280)
                )
            }
            Text(
                when {
                    hasTweaks -> "Updated with ingredient adjustments"
                    hasSubstitutions -> "Updated with substitutions"
                    recipe.servingSizeDescription.isNotBlank() -> "1 serving \u2248 ${recipe.servingSizeDescription}"
                    else -> "Values per single serving"
                },
                fontSize = 12.sp,
                color = when {
                    hasTweaks -> Color(0xFF7C3AED)
                    hasSubstitutions -> Color(0xFF0284C7)
                    else -> Color(0xFF9CA3AF)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NutritionCard(
                    value = "$effectiveCalories",
                    unit = "kcal",
                    subtext = "${(caloriePercent * 100).toInt()}% of daily target",
                    progress = caloriePercent,
                    color = CalorieKoGreen,
                    bgColor = Color(0xFFECFDF5),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                NutritionCard(
                    value = "$effectiveSodium",
                    unit = "Sod. (mg)",
                    subtext = "${(sodiumPercent * 100).toInt()}% of limit",
                    progress = sodiumPercent,
                    color = sodiumColor,
                    bgColor = if (effectiveSodium <= 500) Color(0xFFECFDF5) else if (effectiveSodium <= 800) Color(0xFFFEF9C3) else Color(0xFFFFF7ED),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Macros
            Text("Macronutrients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroRow("Protein", "${effectiveProtein}g", Color(0xFF3B82F6), "P")
                MacroRow("Carbohydrates", "${effectiveCarbs}g", Color(0xFFEAB308), "C")
                MacroRow("Fats", "${effectiveFats}g", Color(0xFFA855F7), "F")
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
                        NutrientDetailRow("Calories", "$effectiveCalories kcal")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Macronutrients
                        NutrientCategoryHeader("🥩 Macronutrients")
                        NutrientDetailRow("Protein", "$effectiveProtein g")
                        NutrientDetailRow("Carbohydrates", "$effectiveCarbs g")
                        NutrientDetailRow("Total Fat", "$effectiveFats g")
                        NutrientDetailRow("Dietary Fiber", "${formatNutrientValue(effectiveFiber)} g")
                        NutrientDetailRow("Sugar", "${formatNutrientValue(effectiveSugar)} g")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Minerals
                        NutrientCategoryHeader("⛏️ Minerals")
                        NutrientDetailRow("Sodium", "$effectiveSodium mg")
                        NutrientDetailRow("Potassium", "${formatNutrientValue(effectivePotassium)} mg")
                        NutrientDetailRow("Calcium", "${formatNutrientValue(effectiveCalcium)} mg")
                        NutrientDetailRow("Iron", "${formatNutrientValue(effectiveIron)} mg")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vitamins
                        NutrientCategoryHeader("💊 Vitamins")
                        NutrientDetailRow("Vitamin A", "${formatNutrientValue(effectiveVitaminA)} µg")
                        NutrientDetailRow("Vitamin C", "${formatNutrientValue(effectiveVitaminC)} mg")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nutrition Methodology Disclaimer
            Surface(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("ⓘ", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "How we calculate nutrition",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Values are computed by summing each raw ingredient\u2019s USDA-verified nutrients based on recipe weights before cooking. Actual values may vary based on cooking method, ingredient freshness, and brand-specific differences.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Load per-ingredient nutrition breakdown
        var ingredientBreakdown by remember { mutableStateOf<Map<String, IngredientNutritionBreakdown>>(emptyMap()) }
        var expandedIngredient by remember { mutableStateOf<String?>(null) }

        // Cache whether each ingredient has substitution alternatives (loaded on expand)
        var ingredientHasAlternatives by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

        LaunchedEffect(recipe.dishLabel, dishSubs) {
            ingredientBreakdown = withContext(Dispatchers.IO) {
                viewModel.getIngredientBreakdown(recipe.dishLabel, dishSubs)
            }
        }

        // Pre-check substitution candidates when an ingredient is expanded
        LaunchedEffect(expandedIngredient) {
            val key = expandedIngredient ?: return@LaunchedEffect
            if (ingredientHasAlternatives.containsKey(key)) return@LaunchedEffect
            val hasAlts = withContext(Dispatchers.IO) {
                viewModel.getSubstitutesForIngredient(key).isNotEmpty()
            }
            ingredientHasAlternatives = ingredientHasAlternatives + (key to hasAlts)
        }

        // Ingredients List (hidden for store-bought items)
        if (recipe.coreTotalCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ingredients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                if (!hasSubstitutions && !hasTweaks && !isViewOnly) {
                    Text("Tap to customize & adjust", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // Store-bought info card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛒", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Store-Bought Item", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0369A1))
                        Text(
                            "This is a pre-cooked item with no recipe. Nutritional values are sourced directly from the USDA.",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recipe.ingredientDetails.forEach { detail ->
                val isMissingCore = recipe.missingCoreIngredients.contains(detail.name)
                val isMissingOptional = recipe.missingOptionalIngredients.contains(detail.name)
                val isMissing = isMissingCore || isMissingOptional

                // Check if this ingredient has been substituted or removed
                val substitutedWith = dishSubs[detail.ingredientKey]
                val replacementKey = detail.replacementIngredientKey
                    ?: substitutedWith?.takeUnless { it == PantryViewModel.REMOVED_INGREDIENT }
                val isRemoved = detail.isRemoved || substitutedWith == PantryViewModel.REMOVED_INGREDIENT
                val isSubstituted = replacementKey != null && !isRemoved
                val effectiveDisplayName = when {
                    isRemoved -> detail.name
                    isSubstituted -> detail.replacementName ?: viewModel.formatIngredientName(replacementKey!!)
                    else -> detail.name
                }
                val isOptional = detail.type == "optional"

                val bgColor = when {
                    isRemoved -> Color(0xFFF9FAFB)
                    isSubstituted -> Color(0xFFF0F9FF)  // Light blue for substituted
                    isMissingCore -> Color(0xFFFFF7ED)
                    isMissingOptional -> Color(0xFFFEFCE8)
                    else -> Color(0xFFF9FAFB)
                }
                val borderColor = when {
                    isRemoved -> Color(0xFFE5E7EB)
                    isSubstituted -> Color(0xFFBAE6FD)
                    isMissingCore -> Color(0xFFFFEDD5)
                    isMissingOptional -> Color(0xFFFEF9C3)
                    else -> Color.Transparent
                }
                val iconColor = when {
                    isRemoved -> Color(0xFFD1D5DB)
                    isSubstituted -> Color(0xFF0284C7)
                    isMissingCore -> CalorieKoOrange
                    isMissingOptional -> Color(0xFFCA8A04)
                    else -> CalorieKoGreen
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Toggle nutrition detail on tap; also load substitutes
                                if (expandedIngredient == detail.ingredientKey) {
                                    expandedIngredient = null
                                } else {
                                    expandedIngredient = detail.ingredientKey
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(20.dp).background(iconColor, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(
                                when {
                                    isRemoved -> Icons.Rounded.RemoveCircleOutline
                                    isSubstituted -> Icons.Default.SwapHoriz
                                    isMissing -> Icons.Rounded.Warning
                                    else -> Icons.Default.Check
                                },
                                null, tint = Color.White, modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Ingredient name (show substituted name if active, strikethrough if removed)
                            val displayName = effectiveDisplayName
                            val nameWithPrep = if (!isSubstituted && !isRemoved && detail.preparationMethod.isNotBlank()) {
                                "$displayName, ${detail.preparationMethod}"
                            } else {
                                displayName
                            }
                            Text(
                                nameWithPrep,
                                fontSize = 14.sp,
                                color = when {
                                    isRemoved -> Color(0xFF6B7280)
                                    isSubstituted -> Color(0xFF0C4A6E)
                                    isMissingCore -> CalorieKoOrange
                                    isMissingOptional -> Color(0xFFCA8A04)
                                    else -> Color(0xFF374151)
                                },
                                fontWeight = if (isSubstituted || isMissing) FontWeight.Medium else FontWeight.Normal,
                                textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                            )
                            // Show original ingredient name if substituted
                            if (isSubstituted) {
                                Text(
                                    "Replaced ${detail.name}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0369A1)
                                )
                            } else if (isRemoved) {
                                Text(
                                    if (isViewOnly) "Removed from customized recipe" else "Removed from recipe",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            } else if (detail.portionQuantity.isNotBlank()) {
                                // Portion quantity (scaled by serving multiplier × ingredient tweak)
                                val ingredientTweakMultiplier = dishTweaks[detail.ingredientKey] ?: 1f
                                val combinedMultiplier = multiplier * ingredientTweakMultiplier
                                val isTweaked = ingredientTweakMultiplier != 1f
                                val scaledPortion = PortionScaler.scale(detail.portionQuantity, combinedMultiplier)
                                Text(
                                    scaledPortion,
                                    fontSize = 12.sp,
                                    color = when {
                                        isTweaked && isScaled -> Color(0xFF0D9488)
                                        isTweaked -> Color(0xFF7C3AED)
                                        isScaled -> CalorieKoGreen
                                        else -> Color(0xFF6B7280)
                                    },
                                    fontWeight = if (isScaled || isTweaked) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                        // Badges
                        if (isViewOnly && (isRemoved || isSubstituted)) {
                            Surface(
                                color = if (isRemoved) Color(0xFFFEE2E2) else Color(0xFFBAE6FD),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (isRemoved) "Removed" else "Swapped",
                                    fontSize = 9.sp,
                                    color = if (isRemoved) Color(0xFFDC2626) else Color(0xFF0C4A6E),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isRemoved) {
                            Surface(
                                onClick = { viewModel.removeSubstitution(recipe.dishLabel, detail.ingredientKey) },
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Undo", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else if (isSubstituted) {
                            Surface(
                                onClick = { viewModel.removeSubstitution(recipe.dishLabel, detail.ingredientKey) },
                                color = Color(0xFFBAE6FD),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Undo", fontSize = 9.sp, color = Color(0xFF0C4A6E), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else if (isMissingCore) {
                            Surface(color = Color(0xFFFFEDD5), shape = RoundedCornerShape(4.dp)) {
                                Text("Core", fontSize = 9.sp, color = CalorieKoOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else if (isMissingOptional) {
                            Surface(color = Color(0xFFFEF9C3), shape = RoundedCornerShape(4.dp)) {
                                Text("Optional", fontSize = 9.sp, color = Color(0xFFCA8A04), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    // Expandable per-ingredient nutrition detail
                    val breakdown = ingredientBreakdown[detail.ingredientKey]
                    AnimatedVisibility(
                        visible = expandedIngredient == detail.ingredientKey,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6).copy(alpha = 0.5f))
                                .padding(start = 44.dp, end = 12.dp, top = 4.dp, bottom = 10.dp)
                        ) {
                            if (breakdown != null) {
                                val bkTweakMult = dishTweaks[detail.ingredientKey] ?: 1f
                                val bkCombinedMult = multiplier * bkTweakMult
                                val bkIsTweaked = bkTweakMult != 1f
                                val bkHighlightColor = when {
                                    bkIsTweaked && isScaled -> Color(0xFF0D9488)
                                    bkIsTweaked -> Color(0xFF7C3AED)
                                    isScaled -> CalorieKoGreen
                                    else -> Color(0xFF6B7280)
                                }
                                Text(
                                    "${(breakdown.rawWeightGrams * bkCombinedMult).toInt()}g raw",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = bkHighlightColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("${(breakdown.calories * bkCombinedMult).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                        Text("kcal", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                    }
                                    Column {
                                        Text("${(breakdown.protein * bkCombinedMult).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                        Text("protein", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                    }
                                    Column {
                                        Text("${(breakdown.carbs * bkCombinedMult).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEAB308))
                                        Text("carbs", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                    }
                                    Column {
                                        Text("${(breakdown.fat * bkCombinedMult).toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                                        Text("fats", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                    }
                                    Column {
                                        Text("${(breakdown.sodium * bkCombinedMult).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                        Text("mg sod.", fontSize = 9.sp, color = Color(0xFF9CA3AF))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // --- Ingredient Tweak Stepper ---
                                if (!isViewOnly && !isRemoved && !PortionScaler.isQualitative(detail.portionQuantity)) {
                                    val tweakSteps = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f)
                                    val tweakLabels = listOf("\u00BC\u00d7", "\u00BD\u00d7", "1\u00d7", "1\u00BD\u00d7", "2\u00d7", "3\u00d7", "4\u00d7")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Adjust amount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6B7280)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        tweakSteps.forEachIndexed { index, step ->
                                            val isActive = bkTweakMult == step
                                            Surface(
                                                onClick = {
                                                    viewModel.setIngredientTweak(
                                                        recipe.dishLabel,
                                                        detail.ingredientKey,
                                                        step
                                                    )
                                                },
                                                color = when {
                                                    isActive && step != 1f -> Color(0xFF7C3AED)
                                                    isActive -> Color(0xFF374151)
                                                    else -> Color(0xFFE5E7EB)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    tweakLabels[index],
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) Color.White else Color(0xFF6B7280),
                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Action buttons row (hidden in view-only mode)
                            if (!isViewOnly) {
                            val hasAlts = ingredientHasAlternatives[detail.ingredientKey]

                            if (!isRemoved && !isSubstituted) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Swap action (conditional on availability)
                                    when {
                                        hasAlts == null -> {
                                            // Still loading
                                            Text("Checking alternatives...", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                        }
                                        hasAlts -> {
                                            // Has alternatives — show swap button
                                            Surface(
                                                onClick = {
                                                    scope.launch {
                                                        isLoadingCandidates = true
                                                        substitutionTarget = detail.ingredientKey
                                                        val candidates = withContext(Dispatchers.IO) {
                                                            viewModel.getSubstitutesForIngredient(detail.ingredientKey)
                                                        }
                                                        substitutionCandidates = candidates
                                                        isLoadingCandidates = false
                                                        if (candidates.isEmpty()) {
                                                            substitutionTarget = null
                                                        }
                                                    }
                                                },
                                                color = Color(0xFF0284C7).copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Swap ingredient", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                        else -> {
                                            // No alternatives — show disabled text
                                            Row(
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Info, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("No alternatives available", fontSize = 11.sp, color = Color(0xFFD1D5DB), fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                // Remove action (only for optional ingredients)
                                if (isOptional) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        onClick = {
                                            viewModel.removeIngredient(recipe.dishLabel, detail.ingredientKey)
                                            expandedIngredient = null
                                        },
                                        color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.RemoveCircleOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove ingredient", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            } // end !isViewOnly
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
            "USDA_FDC" -> "USDA FoodData Central"
            else -> recipe.dataSource
        }
        val (sourceTextColor, sourceBgColor) = when (recipe.dataSource) {
            "DOST_FNRI_MENU_GUIDE" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
            "DOST_FNRI_FCT" -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))
            "USDA_FNDDS", "USDA_FDC" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
            else -> Pair(Color(0xFF374151), Color(0xFFF3F4F6))
        }
        val proofDoc = remember(recipe.dishLabel, recipe.dataSource) {
            getDishProofDocument(recipe.dishLabel, recipe.dataSource)
        }
        val recipeSourceDoc = remember(recipe.dishLabel) {
            getRecipeSourceDocument(recipe.dishLabel)
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

                // ── Recipe Source (FNRI) — shown only for dishes with FNRI PDFs ──
                if (recipeSourceDoc.type == ProofType.PDF_ASSET) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(sourceTextColor.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            tint = sourceTextColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Recipe sourced from",
                                fontSize = 11.sp,
                                color = sourceTextColor.copy(alpha = 0.7f)
                            )
                            Text(
                                "DOST-FNRI Menu Guide",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = sourceTextColor
                            )
                        }
                        Surface(
                            onClick = {
                                openPdfFromAssets(context, recipeSourceDoc.path)
                            },
                            color = sourceTextColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "View PDF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = sourceTextColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // FNRI website link
                    Surface(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.fnri.dost.gov.ph/index.php/tools-and-standard/fnri-menu-guide-calendar"))
                            context.startActivity(intent)
                        },
                        color = sourceTextColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "View on FNRI",
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

        if (!isViewOnly) {
            Button(
                onClick = {
                    targetWeekStart = calendarWeekStart // reset to calendar's selected week on each open
                    showPlanDialog.value = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CalorieKoGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add to Meal Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Planning: $targetWeekLabel",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Step 1: Add to Plan — Pick a day with week navigator (from Detail Sheet)
    if (showPlanDialog.value) {
        val todayWeek = viewModel.getCurrentWeekStartDate()
        val maxWeek = viewModel.getMaxPlanningWeekStartPublic()
        val canGoBack = targetWeekStart > todayWeek
        val canGoForward = run {
            val nextWeek = java.time.LocalDate.parse(targetWeekStart)
                .plusWeeks(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            nextWeek <= maxWeek
        }

        AlertDialog(
            onDismissRequest = { showPlanDialog.value = false },
            title = { Text("Plan Meal") },
            text = {
                Column {
                    // Week navigator: ◀ Apr 28 – May 4 ▶
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val prev = java.time.LocalDate.parse(targetWeekStart)
                                        .minusWeeks(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    if (prev >= todayWeek) targetWeekStart = prev
                                },
                                enabled = canGoBack,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous week",
                                    tint = if (canGoBack) Color(0xFF374151) else Color(0xFFD1D5DB)
                                )
                            }
                            Text(
                                targetWeekLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    val next = java.time.LocalDate.parse(targetWeekStart)
                                        .plusWeeks(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    if (next <= maxWeek) targetWeekStart = next
                                },
                                enabled = canGoForward,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next week",
                                    tint = if (canGoForward) Color(0xFF374151) else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }

                    Text("Select a day for ${recipe.inlineDishName()}:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Day circles with date numbers + editability
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        targetWeekDayDates.forEachIndexed { index, (dayName, dateNum) ->
                            val editable = viewModel.isDayEditableForWeek(index, targetWeekStart)
                            val isToday = run {
                                val dayDate = java.time.LocalDate.parse(targetWeekStart).plusDays(index.toLong())
                                dayDate == java.time.LocalDate.now()
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.alpha(if (editable) 1f else 0.4f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            when {
                                                isToday -> CalorieKoGreen
                                                editable -> CalorieKoGreen.copy(alpha = 0.1f)
                                                else -> Color(0xFFF3F4F6)
                                            },
                                            CircleShape
                                        )
                                        .clickable {
                                            if (editable) {
                                                selectedDayForPlan.intValue = index
                                                showPlanDialog.value = false
                                                showSlotPickerForPlan.value = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayName.first().toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = when {
                                            isToday -> Color.White
                                            editable -> CalorieKoGreen
                                            else -> Color.Gray
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "$dateNum",
                                    fontSize = 9.sp,
                                    color = if (isToday) CalorieKoGreen else Color(0xFF9CA3AF),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlanDialog.value = false }) { Text("Cancel") } }
        )
    }

    // Step 2: Pick a meal slot (from Detail Sheet) — uses target week meals for duplicate check
    if (showSlotPickerForPlan.value) {
        val dayIdx = selectedDayForPlan.intValue
        val (selectedDayName, selectedDateNum) = if (dayIdx in targetWeekDayDates.indices) {
            targetWeekDayDates[dayIdx]
        } else "Day" to 0

        AlertDialog(
            onDismissRequest = { showSlotPickerForPlan.value = false },
            title = { Text("Choose Meal Slot") },
            text = {
                Column {
                    Text("Add ${recipe.inlineDishName()} on $selectedDayName $selectedDateNum as:")
                    Spacer(modifier = Modifier.height(16.dp))
                    mealSlots.forEach { slot ->
                        val alreadyInSlot = targetWeekMeals.any {
                            it.dayIndex == dayIdx && it.mealSlot == slot && it.dishLabel == recipe.dishLabel
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!alreadyInSlot) {
                                        viewModel.addMealToPlan(
                                            dayIdx,
                                            recipe.dishLabel,
                                            slot,
                                            targetWeekStart,
                                            dishSubs,
                                            scaledServings = if (isScaled) targetServings else 0,
                                            tweaks = dishTweaks
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

    // --- Substitution Picker Bottom Sheet ---
    if (substitutionTarget != null && substitutionCandidates.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = {
                substitutionTarget = null
                substitutionCandidates = emptyList()
            },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Swap Ingredient", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text(
                            "Replace ${viewModel.formatIngredientName(substitutionTarget!!)} with:",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Alternative candidates
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    substitutionCandidates.forEach { candidate ->
                        val isCurrentSub = dishSubs[substitutionTarget] == candidate.ingredientKey
                        Surface(
                            onClick = {
                                viewModel.applySubstitution(
                                    recipe.dishLabel,
                                    substitutionTarget!!,
                                    candidate.ingredientKey
                                )
                                substitutionTarget = null
                                substitutionCandidates = emptyList()
                            },
                            color = if (isCurrentSub) Color(0xFFE0F2FE) else Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isCurrentSub) 1.5.dp else 1.dp,
                                color = if (isCurrentSub) Color(0xFF0284C7) else Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            candidate.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrentSub) Color(0xFF0C4A6E) else Color(0xFF1F2937)
                                        )
                                        if (isCurrentSub) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color(0xFF0284C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Active",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Nutrition comparison (per 100g)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "${candidate.calories.toInt()} kcal",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4B5563),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "P: ${candidate.protein.toInt()}g",
                                            fontSize = 12.sp,
                                            color = Color(0xFF3B82F6)
                                        )
                                        Text(
                                            "F: ${candidate.fat.toInt()}g",
                                            fontSize = 12.sp,
                                            color = Color(0xFFA855F7)
                                        )
                                    }
                                    Text(
                                        "per 100g raw",
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                TextButton(
                    onClick = {
                        substitutionTarget = null
                        substitutionCandidates = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        }
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
 *
 * Note: egg dishes removed — they are multi-ingredient recipes, not single-FDC-entry items.
 * All dishes now use USDA_FDC as dataSource.
 */
private fun getDishProofDocument(mlLabel: String, dataSource: String): DishProofDocument {
    val usdaUrls = mapOf(
        "chicken_drumstick" to "https://fdc.nal.usda.gov/food-details/171126/nutrients",
        "chicken_thigh" to "https://fdc.nal.usda.gov/food-details/171127/nutrients",
        "chicken_wings" to "https://fdc.nal.usda.gov/food-details/172830/nutrients",
        "chicken_breast" to "https://fdc.nal.usda.gov/food-details/171125/nutrients"
    )

    return when (dataSource) {
        "USDA_FNDDS", "USDA_FDC" -> {
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
 * Returns the recipe source document for a dish, if available.
 * Matches the logic in ExploreViewModel.getRecipeSourceDocument().
 *
 * These are DOST-FNRI Menu Guide PDFs that document the original
 * recipe (ingredients, portions, preparation method). Even though
 * the nutritional values are now computed from USDA data, these PDFs
 * remain valuable as the provenance for the recipe itself.
 */
private fun getRecipeSourceDocument(mlLabel: String): DishProofDocument {
    val fnriDishes = setOf(
        "chicken_tinola", "chopseuy", "egg_ampalaya",
        "galunggong_grilled", "kinilaw_tuna",
        "menudo", "sinabawang_bangus", "pinakbet",
        "sinigang_pork", "sinuglaw_pork",
        "tilapia_fried", "tinapa_ginisa", "kwekwek", "udong",
        "linatan", "humba_pork", "lawuy"
    )

    return if (mlLabel in fnriDishes) {
        DishProofDocument(ProofType.PDF_ASSET, "sources/$mlLabel.pdf")
    } else {
        DishProofDocument(ProofType.NONE, "")
    }
}

/**
 * Returns the general database URL for a data source key.
 */
private fun getSourceDatabaseUrl(source: String): String {
    return when (source) {
        "DOST_FNRI_MENU_GUIDE" -> "https://www.fnri.dost.gov.ph/index.php/tools-and-standard/fnri-menu-guide-calendar"
        "DOST_FNRI_FCT" -> "https://i.fnri.dost.gov.ph/login/fct"
        "USDA_FNDDS", "USDA_FDC" -> "https://fdc.nal.usda.gov/food-search"
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

// ============================================================
// Ingredient Browser — Category metadata
// ============================================================

private val INGREDIENT_CATEGORY_ORDER = listOf(
    "all" to Pair("All", "\uD83D\uDCCB"),
    "protein" to Pair("Protein", "\uD83E\uDD69"),
    "produce" to Pair("Produce", "\uD83E\uDD6C"),
    "seasoning" to Pair("Seasonings & Sauces", "\uD83E\uDDC2"),
    "pantry_staple" to Pair("Pantry Staples", "\uD83C\uDFE1"),
    "grain_starch" to Pair("Grains & Starches", "\uD83C\uDF3E")
)

private val INGREDIENT_CATEGORY_COLORS = mapOf(
    "protein" to Color(0xFFFEE2E2),
    "produce" to Color(0xFFDCFCE7),
    "seasoning" to Color(0xFFFEF9C3),
    "pantry_staple" to Color(0xFFDBEAFE),
    "grain_starch" to Color(0xFFFFF7ED)
)

// ============================================================
// Ingredient Browser Sheet
// ============================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IngredientBrowserSheet(
    allIngredients: List<RawIngredientEntity>,
    currentPantryItems: List<String>,
    formatName: (String) -> String,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Local mutable selection state — initialized from current pantry
    var selectedKeys by remember(currentPantryItems) {
        mutableStateOf(currentPantryItems.toSet())
    }

    var selectedCategory by remember { mutableStateOf("all") }
    var browserSearchQuery by remember { mutableStateOf("") }

    // Compute filtered list
    val filteredIngredients = remember(allIngredients, selectedCategory, browserSearchQuery) {
        allIngredients.filter { ingredient ->
            val matchesCategory = selectedCategory == "all" || ingredient.category == selectedCategory
            val matchesSearch = browserSearchQuery.isBlank() ||
                ingredient.displayName.contains(browserSearchQuery, ignoreCase = true) ||
                ingredient.ingredientKey.contains(browserSearchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Compute diff for the bottom action bar
    val currentPantrySet = remember(currentPantryItems) { currentPantryItems.toSet() }
    val toAddCount = selectedKeys.count { it !in currentPantrySet }
    val toRemoveCount = currentPantrySet.count { it !in selectedKeys }
    val hasChanges = toAddCount > 0 || toRemoveCount > 0

    // Expanded nutrition detail state
    var expandedIngredientKey by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Browse Ingredients",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    "${allIngredients.size} ingredients available · Nutrition per 100g",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF3F4F6), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Search Bar ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF9FAFB),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = browserSearchQuery,
                    onValueChange = { browserSearchQuery = it },
                    placeholder = { Text("Search ingredients...", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    modifier = Modifier.weight(1f)
                )
                if (browserSearchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { browserSearchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Category Filter Chips ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(INGREDIENT_CATEGORY_ORDER) { (categoryKey, labelAndEmoji) ->
                val (label, emoji) = labelAndEmoji
                val isSelected = selectedCategory == categoryKey
                val count = if (categoryKey == "all") allIngredients.size
                    else allIngredients.count { it.category == categoryKey }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = categoryKey },
                    label = {
                        Text(
                            "$emoji $label ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalorieKoGreen.copy(alpha = 0.15f),
                        selectedLabelColor = CalorieKoGreen,
                        containerColor = Color.White,
                        labelColor = Color(0xFF6B7280)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFFE5E7EB),
                        selectedBorderColor = CalorieKoGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Ingredient List ──
        if (filteredIngredients.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("\uD83D\uDD0D", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No ingredients match your search",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Text(
                    "Try adjusting your search or category filter",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(filteredIngredients, key = { it.ingredientKey }) { ingredient ->
                    val isChecked = ingredient.ingredientKey in selectedKeys
                    val isInPantry = ingredient.ingredientKey in currentPantrySet
                    val isExpanded = expandedIngredientKey == ingredient.ingredientKey

                    IngredientBrowserRow(
                        ingredient = ingredient,
                        isChecked = isChecked,
                        isInPantry = isInPantry,
                        isExpanded = isExpanded,
                        formatName = formatName,
                        onToggle = {
                            selectedKeys = if (isChecked) selectedKeys - ingredient.ingredientKey
                                           else selectedKeys + ingredient.ingredientKey
                        },
                        onExpandToggle = {
                            expandedIngredientKey = if (isExpanded) null else ingredient.ingredientKey
                        }
                    )
                }
            }
        }

        // ── Sticky Bottom Action Bar ──
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Diff summary
                Column {
                    Text(
                        "${selectedKeys.size} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                    if (hasChanges) {
                        val parts = mutableListOf<String>()
                        if (toAddCount > 0) parts.add("$toAddCount to add")
                        if (toRemoveCount > 0) parts.add("$toRemoveCount to remove")
                        Text(
                            parts.joinToString(" · "),
                            fontSize = 12.sp,
                            color = if (toRemoveCount > 0) CalorieKoOrange else CalorieKoGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "No changes",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Apply button
                Button(
                    onClick = { onApply(selectedKeys) },
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalorieKoGreen,
                        disabledContainerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        if (hasChanges) "Apply Changes" else "No Changes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================
// Ingredient Browser — Single Row
// ============================================================

@Composable
private fun IngredientBrowserRow(
    ingredient: RawIngredientEntity,
    isChecked: Boolean,
    isInPantry: Boolean,
    isExpanded: Boolean,
    formatName: (String) -> String,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val categoryColor = INGREDIENT_CATEGORY_COLORS[ingredient.category] ?: Color(0xFFF3F4F6)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) categoryColor.copy(alpha = 0.5f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 0.dp else 0.5.dp),
        border = if (isChecked) BorderStroke(1.dp, CalorieKoGreen.copy(alpha = 0.4f))
                 else BorderStroke(0.5.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onToggle() }
    ) {
        Column {
            // Main row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CalorieKoGreen,
                        uncheckedColor = Color(0xFFD1D5DB),
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(40.dp)
                )

                // Name + category badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ingredient.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isInPantry && isChecked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "In Pantry",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CalorieKoGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Compact nutrition summary
                    Text(
                        "${ingredient.calories.toInt()} kcal · " +
                        "${ingredient.protein.toInt()}g P · " +
                        "${ingredient.carbs.toInt()}g C · " +
                        "${ingredient.fat.toInt()}g F · " +
                        "${ingredient.sodium.toInt()}mg Na",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expand/collapse button
                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded nutrition detail
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Nutritional Values per 100g",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Nutrient grid — 3 columns
                        val nutrients = listOf(
                            "Calories" to "${ingredient.calories.toInt()} kcal",
                            "Protein" to "${String.format("%.1f", ingredient.protein)}g",
                            "Carbs" to "${String.format("%.1f", ingredient.carbs)}g",
                            "Fat" to "${String.format("%.1f", ingredient.fat)}g",
                            "Fiber" to "${String.format("%.1f", ingredient.fiber)}g",
                            "Sugar" to "${String.format("%.1f", ingredient.sugar)}g",
                            "Sodium" to "${String.format("%.0f", ingredient.sodium)}mg",
                            "Potassium" to "${String.format("%.0f", ingredient.potassium)}mg",
                            "Vitamin A" to "${String.format("%.0f", ingredient.vitaminA)} IU",
                            "Vitamin C" to "${String.format("%.1f", ingredient.vitaminC)}mg",
                            "Calcium" to "${String.format("%.0f", ingredient.calcium)}mg",
                            "Iron" to "${String.format("%.2f", ingredient.iron)}mg"
                        )

                        // Display in rows of 3
                        nutrients.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                row.forEach { (label, value) ->
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontSize = 10.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
                                        Text(value, fontSize = 12.sp, color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                // Fill remaining columns if row has < 3 items
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Proxy transparency note (if applicable)
                        if (ingredient.nutrientProxyNote.isNotBlank()) {
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        ingredient.nutrientProxyNote,
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Dynamic data source badge
                        val badgeText: String
                        val badgeColor: Color
                        val badgeTextColor: Color
                        when {
                            ingredient.fdcId > 0 -> {
                                badgeText = "USDA FDC"
                                badgeColor = Color(0xFFE8F5E9)
                                badgeTextColor = Color(0xFF2E7D32)
                            }
                            ingredient.dataSource == "KNORR_LABEL" -> {
                                badgeText = "Product Label"
                                badgeColor = Color(0xFFE3F2FD)
                                badgeTextColor = Color(0xFF1565C0)
                            }
                            else -> {
                                badgeText = ""
                                badgeColor = Color.Transparent
                                badgeTextColor = Color.Transparent
                            }
                        }

                        if (badgeText.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (ingredient.fdcId > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "FDC ID: ${ingredient.fdcId}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF)
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
