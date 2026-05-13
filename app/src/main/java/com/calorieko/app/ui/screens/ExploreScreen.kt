package com.calorieko.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen
import com.calorieko.app.ui.theme.MacroFat
import com.calorieko.app.viewmodel.ExploreDish
import com.calorieko.app.viewmodel.ExploreViewModel
import com.calorieko.app.viewmodel.IngredientInfo
import com.calorieko.app.viewmodel.ProofType
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// --- Category display metadata ---
private val CATEGORY_ORDER = listOf(
    "Main Dish", "Fish", "Soup", "Vegetable Dish", "Egg", "Noodles", "Rice", "Street Food"
)

private val CATEGORY_EMOJIS = mapOf(
    "Main Dish" to "🥩",
    "Fish" to "🐟",
    "Soup" to "🥘",
    "Vegetable Dish" to "🥬",
    "Egg" to "🍳",
    "Noodles" to "🍜",
    "Rice" to "🍚",
    "Street Food" to "🍢"
)



@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val filteredDishes by viewModel.filteredDishes.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Bottom Sheet for Dish Detail
    val selectedDish = remember { mutableStateOf<ExploreDish?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // Compute total dish count
    val totalDishCount = filteredDishes.values.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Explore Dishes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        Text(
                            if (isLoading) "Loading dishes..." else "$totalDishCount supported dishes",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF374151))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Search Bar + Source Filters
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Search Bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search dishes or ingredients...", color = Color.Gray, fontSize = 14.sp) },
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
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.updateSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            CategoryFilterChip("ALL", "All", "🍽️", categoryFilter) { viewModel.setCategoryFilter(it) }
                        }
                        items(CATEGORY_ORDER.size) { index ->
                            val cat = CATEGORY_ORDER[index]
                            val emoji = CATEGORY_EMOJIS[cat] ?: "🍽️"
                            CategoryFilterChip(cat, cat, emoji, categoryFilter) { viewModel.setCategoryFilter(it) }
                        }
                    }
                }
            }

            // Loading State
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CalorieKoGreen, strokeWidth = 3.dp)
                    }
                }
            }

            // Empty State
            if (!isLoading && filteredDishes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No dishes found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Text(
                            "Try adjusting your search or filter",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Category-Grouped Dish Sections
            if (!isLoading) {
                val sortedCategories = CATEGORY_ORDER.filter { it in filteredDishes.keys } +
                    filteredDishes.keys.filter { it !in CATEGORY_ORDER }

                sortedCategories.forEach { category ->
                    val dishes = filteredDishes[category] ?: return@forEach

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            // Category Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    CATEGORY_EMOJIS[category] ?: "🍽️",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    category,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        "${dishes.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Horizontal Dish Cards
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(dishes) { dish ->
                                    ExploreDishCard(
                                        dish = dish,
                                        viewModel = viewModel,
                                        onClick = { selectedDish.value = dish }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Data Transparency Footer
            if (!isLoading && filteredDishes.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                null,
                                tint = Color(0xFF0369A1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Data Transparency",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0C4A6E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "All nutritional values are sourced from official food composition databases. " +
                                    "Each dish's source is shown on its detail card. " +
                                    "Tap any dish to view the full attribution.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF0369A1),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dish Detail Bottom Sheet
    if (selectedDish.value != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDish.value = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            ExploreDishDetailContent(
                dish = selectedDish.value!!,
                viewModel = viewModel,
                pantryItems = pantryItems,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedDish.value = null }
                },
                onAddToPantry = {
                    viewModel.addCoreIngredientsToPantry(selectedDish.value!!.dishLabel)
                },
                onAddSingleIngredient = { ingredientKey ->
                    viewModel.addSingleIngredientToPantry(ingredientKey)
                }
            )
        }
    }
}

// --- Category Filter Chip ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChip(
    value: String,
    label: String,
    emoji: String,
    currentFilter: String,
    onSelect: (String) -> Unit
) {
    val isSelected = currentFilter == value
    FilterChip(
        selected = isSelected,
        onClick = { onSelect(value) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
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

// --- Dietary Badge Helper ---
private data class DietaryBadge(val label: String, val textColor: Color, val bgColor: Color)

private fun getDietaryBadges(dish: ExploreDish): List<DietaryBadge> {
    val badges = mutableListOf<DietaryBadge>()
    // Thresholds are per 100g, tuned for Filipino dishes
    if (dish.protein >= 15) badges.add(DietaryBadge("High Protein", Color(0xFF1D4ED8), Color(0xFFDBEAFE)))
    if (dish.calories <= 120) badges.add(DietaryBadge("Low Cal", Color(0xFF16A34A), Color(0xFFDCFCE7)))
    if (dish.sodium <= 200) badges.add(DietaryBadge("Low Sodium", Color(0xFF0891B2), Color(0xFFCFFAFE)))
    if (dish.fats <= 3) badges.add(DietaryBadge("Low Fat", Color(0xFF7C3AED), Color(0xFFEDE9FE)))
    return badges.take(2) // Max 2 badges to avoid clutter on small cards
}

// --- Explore Dish Card ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreDishCard(
    dish: ExploreDish,
    viewModel: ExploreViewModel,
    onClick: () -> Unit
) {
    val dietaryBadges = remember(dish) { getDietaryBadges(dish) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Emoji + Source Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getDishEmoji(dish.dishLabel), fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dish Name (Filipino — primary)
            Text(
                dish.namePh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // English name
            Text(
                dish.nameEn,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontStyle = FontStyle.Italic
            )

            // Dietary Quick Badges
            if (dietaryBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dietaryBadges.forEach { badge ->
                        Surface(
                            color = badge.bgColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                badge.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badge.textColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom: Calories + Ingredient count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${dish.calories} kcal",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.Medium
                )
                if (dish.ingredientCount > 0) {
                    Text(" • ", fontSize = 12.sp, color = Color.LightGray)
                    Text(
                        "${dish.ingredientCount} ingr.",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

// --- Explore Dish Detail Bottom Sheet Content ---
@Composable
private fun ExploreDishDetailContent(
    dish: ExploreDish,
    viewModel: ExploreViewModel,
    pantryItems: List<String>,
    onClose: () -> Unit,
    onAddToPantry: () -> Unit,
    onAddSingleIngredient: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var ingredientDetails by remember { mutableStateOf<List<IngredientInfo>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var addedToPantry by remember { mutableStateOf(false) }

    // Load ingredient details on first composition
    LaunchedEffect(dish.dishLabel) {
        isLoadingDetails = true
        ingredientDetails = withContext(Dispatchers.IO) {
            viewModel.getIngredientDetails(dish.dishLabel)
        }
        isLoadingDetails = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getDishEmoji(dish.dishLabel), fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(dish.namePh, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Text(dish.nameEn, fontSize = 13.sp, color = Color(0xFF9CA3AF), fontStyle = FontStyle.Italic)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(50)) {
                        Text(
                            dish.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Nutrition Summary
        Text("Per Serving Nutrition", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        if (dish.perServingWeightG > 0f) {
            Text(
                "≈ ${dish.perServingWeightG.toInt()}g per serving",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }
        if (dish.servingSizeDescription.isNotBlank()) {
            Text(
                "1 serving \u2248 ${dish.servingSizeDescription}",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientQuickStat("Calories", "${dish.calories}", "kcal", CalorieKoGreen)
                NutrientQuickStat("Protein", "${dish.protein}", "g", Color(0xFF3B82F6))
                NutrientQuickStat("Carbs", "${dish.carbs}", "g", Color(0xFFEAB308))
                NutrientQuickStat("Fat", "${dish.fats}", "g", MacroFat)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sodium badge
        Surface(
            color = if (dish.sodium <= 500) Color(0xFFECFDF5) else if (dish.sodium <= 800) Color(0xFFFEF9C3) else Color(0xFFFFF7ED),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sodium: ${dish.sodium}mg per serving",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dish.sodium <= 500) Color(0xFF16A34A) else if (dish.sodium <= 800) Color(0xFFCA8A04) else Color(0xFFEA580C)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Source Attribution Card (Interactive)
        val context = LocalContext.current
        val proofDoc = remember(dish.dishLabel, dish.dataSource) {
            viewModel.getDishProofDocument(dish.dishLabel, dish.dataSource)
        }
        val recipeSourceDoc = remember(dish.dishLabel) {
            viewModel.getRecipeSourceDocument(dish.dishLabel)
        }

        // Determine source-based colors
        val isCommunity = dish.dataSource == "COMMUNITY"
        val isUsda = dish.dataSource.startsWith("USDA")
        val sourceTextColor = if (isCommunity) Color(0xFF7C3AED) else if (isUsda) Color(0xFF2E7D32) else Color(0xFF0369A1)
        val sourceBgColor = if (isCommunity) Color(0xFFF3F0FF) else if (isUsda) Color(0xFFE8F5E9) else Color(0xFFF0F9FF)
        val sourceIcon = if (isCommunity) Icons.Default.Description else Icons.Default.VerifiedUser

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = sourceBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Source info header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        sourceIcon,
                        null,
                        tint = sourceTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nutritional data sourced from",
                            fontSize = 11.sp,
                            color = sourceTextColor.copy(alpha = 0.7f)
                        )
                        Text(
                            viewModel.getSourceDisplayLabel(dish.dataSource),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = sourceTextColor
                        )
                    }
                }

                // Community dishes: flat nutrition notice, no USDA proof buttons
                if (isCommunity) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Per 100g flat nutrition \u2014 no ingredient breakdown available",
                        fontSize = 11.sp,
                        color = sourceTextColor.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                }

                // Action row — only for non-Community dishes
                if (!isCommunity) {

                Spacer(modifier = Modifier.height(12.dp))

                // Action row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Primary: View USDA proof (for single-ingredient dishes)
                    if (isUsda && proofDoc.type != ProofType.NONE) {
                        Surface(
                            onClick = {
                                when (proofDoc.type) {
                                    ProofType.URL -> {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(proofDoc.path))
                                        context.startActivity(intent)
                                    }
                                    else -> {}
                                }
                            },
                            color = sourceTextColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "View on USDA",
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
                            val url = viewModel.getSourceUrl(dish.dataSource)
                            if (url.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        color = sourceTextColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .then(
                                if (isUsda && proofDoc.type != ProofType.NONE) Modifier.weight(1f)
                                else Modifier.fillMaxWidth()
                            )
                            .height(38.dp)
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
                } // end if (!isCommunity)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (dish.ingredientCount > 0) {
            Text("Ingredients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        }
        if (isLoadingDetails) {
            Text("Loading ingredients...", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else if (ingredientDetails.isEmpty()) {
            // Store-bought dishes have no recipe — show contextual message
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Store-Bought Item",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0369A1)
                        )
                        Text(
                            "This is a pre-cooked item with no recipe. Nutritional values are sourced directly from the USDA for this product.",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ingredientDetails.forEach { detail ->
                    val isInPantry = detail.ingredientKey in pantryItems
                    val isCore = detail.type == "core"
                    val bgColor = if (isInPantry) Color(0xFFECFDF5) else Color(0xFFF9FAFB)
                    val iconColor = if (isInPantry) CalorieKoGreen else Color(0xFF9CA3AF)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .then(
                                if (!isInPantry) Modifier.clickable {
                                    onAddSingleIngredient(detail.ingredientKey)
                                } else Modifier
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp).background(iconColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isInPantry) Icons.Default.Check else Icons.Default.Add,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = detail.name
                            val nameWithPrep = if (detail.preparationMethod.isNotBlank()) {
                                "$displayName, ${detail.preparationMethod}"
                            } else {
                                displayName
                            }
                            Text(
                                nameWithPrep,
                                fontSize = 14.sp,
                                color = Color(0xFF374151),
                                fontWeight = if (isCore) FontWeight.Medium else FontWeight.Normal
                            )
                            if (detail.portionQuantity.isNotBlank()) {
                                Text(detail.portionQuantity, fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                            // Tap hint for missing ingredients
                            if (!isInPantry) {
                                Text("Tap to add to pantry", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                        // Core / Optional badge
                        Surface(
                            color = if (isCore) Color(0xFFECFDF5) else Color(0xFFFEF9C3),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (isCore) "Core" else "Optional",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCore) CalorieKoGreen else Color(0xFFCA8A04),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // "In Pantry" indicator
                        if (isInPantry) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✓", fontSize = 12.sp, color = CalorieKoGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Add Core Ingredients to Pantry Button
        if (ingredientDetails.isNotEmpty()) {
            val coreCount = ingredientDetails.count { it.type == "core" }
            val coreInPantryCount = ingredientDetails.count { it.type == "core" && it.ingredientKey in pantryItems }
            val missingCoreCount = coreCount - coreInPantryCount
            val allCoreInPantry = missingCoreCount == 0

            Surface(
                onClick = {
                    if (!allCoreInPantry && !addedToPantry) {
                        onAddToPantry()
                        addedToPantry = true
                    }
                },
                color = if (allCoreInPantry || addedToPantry) Color(0xFFECFDF5) else CalorieKoGreen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (allCoreInPantry || addedToPantry) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = CalorieKoGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (addedToPantry) "Added to Pantry!" else "All Core Ingredients in Pantry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalorieKoGreen
                            )
                        }
                    } else {
                        Text(
                            "Add $missingCoreCount Core Ingredient${if (missingCoreCount > 1) "s" else ""} to Pantry",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- Nutrient Quick Stat ---
@Composable
private fun NutrientQuickStat(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, fontSize = 10.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(bottom = 2.dp))
        }
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}

/**
 * Copies a PDF from the app's assets to the cache directory
 * and opens it with an external PDF viewer via FileProvider.
 */
private fun openPdfFromAssets(context: android.content.Context, assetPath: String) {
    try {
        // Extract filename from path (e.g., "sources/menudo.pdf" → "menudo.pdf")
        val fileName = assetPath.substringAfterLast("/")

        // Copy asset to cache directory
        val cacheDir = File(context.cacheDir, "source_pdfs")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val outFile = File(cacheDir, fileName)

        context.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Create content URI via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )

        // Launch PDF viewer
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
