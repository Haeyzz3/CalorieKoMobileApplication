package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Represents a dish for display in the Explore screen.
 * Enriched with ingredient names and data source info beyond what DishRecipeEntity alone provides.
 */
data class ExploreDish(
    val dishLabel: String,
    val nameEn: String,
    val namePh: String,
    val category: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val sodium: Int,
    val dataSource: String,
    val ingredientCount: Int = 0,
    val ingredientNames: List<String> = emptyList(),
    val servings: Int = 1,
    val perServingWeightG: Float = 0f,
    val servingSizeDescription: String = ""
)

/**
 * ViewModel for the Explore Dishes screen.
 * Provides dish browsing, search, source filtering, and quick add-to-pantry.
 */
class ExploreViewModel(
    private val auth: FirebaseAuth,
    private val dishRecipeDao: DishRecipeDao,
    private val pantryDao: PantryDao,
    private val rawIngredientDao: RawIngredientDao,
    private val foodDao: FoodDao,
    private val firestoreSyncRepo: FirestoreSyncRepository,
    private val appContext: Context
) : ViewModel() {

    private val uid: String get() = auth.currentUser?.uid ?: ""

    // --- Factory ---
    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            dishRecipeDao: DishRecipeDao,
            pantryDao: PantryDao,
            rawIngredientDao: RawIngredientDao,
            foodDao: FoodDao,
            firestoreSyncRepo: FirestoreSyncRepository,
            appContext: Context
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
                    return ExploreViewModel(auth, dishRecipeDao, pantryDao, rawIngredientDao, foodDao, firestoreSyncRepo, appContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // --- All dishes loaded from FOOD_TABLE ---
    private val _allDishes = MutableStateFlow<List<ExploreDish>>(emptyList())

    // --- Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Category filter ---
    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    // --- Filtered + grouped dishes (derived state) ---
    val filteredDishes: StateFlow<Map<String, List<ExploreDish>>> = combine(
        _allDishes,
        _searchQuery,
        _categoryFilter
    ) { dishes, query, category ->
        dishes
            .filter { dish ->
                // Exclude non-food labels
                dish.dishLabel != "negative"
            }
            .filter { dish ->
                // Search filter — matches dish name, Filipino name, label, AND ingredient names
                if (query.isBlank()) true
                else dish.nameEn.contains(query, ignoreCase = true) ||
                     dish.namePh.contains(query, ignoreCase = true) ||
                     dish.dishLabel.contains(query, ignoreCase = true) ||
                     dish.ingredientNames.any { it.contains(query, ignoreCase = true) }
            }
            .filter { dish ->
                // Category filter
                when (category) {
                    "ALL" -> true
                    else -> dish.category == category
                }
            }
            .groupBy { it.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Current pantry items (reactive, for showing "already in pantry" state) ---
    val pantryItems: StateFlow<List<String>> = pantryDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Loading state ---
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Snackbar events (one-shot) ---
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        loadAllDishes()
    }

    private fun normalizePantryKey(value: String): String = value.trim().lowercase()

    private fun loadAllDishes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            // 1. Load System B dishes (USDA-verified, full ingredient-level nutrition)
            val recipes = dishRecipeDao.getAllDishRecipes()
            val systemBLabels = recipes.map { it.dishLabel }.toSet()
            val dishes = recipes.map { recipe ->
                val ingredientKeys = pantryDao.getIngredientsForDish(recipe.dishLabel)
                // Resolve authoritative display names from RAW_INGREDIENTS_TABLE
                val displayNames = rawIngredientDao.getDisplayNamesForKeys(ingredientKeys)
                val nameMap = displayNames.associate { it.ingredient_key to it.display_name }
                val resolvedNames = ingredientKeys.map { nameMap[it] ?: it }
                ExploreDish(
                    dishLabel = recipe.dishLabel,
                    nameEn = recipe.nameEn,
                    namePh = recipe.namePh,
                    category = recipe.category,
                    calories = recipe.calPerServing.toInt(),
                    protein = recipe.proteinPerServing.toInt(),
                    carbs = recipe.carbsPerServing.toInt(),
                    fats = recipe.fatPerServing.toInt(),
                    sodium = recipe.sodiumPerServing.toInt(),
                    dataSource = "USDA_FDC",
                    ingredientCount = resolvedNames.size,
                    ingredientNames = resolvedNames,
                    servings = recipe.servings,
                    perServingWeightG = recipe.perServingWeightG,
                    servingSizeDescription = recipe.servingSizeDescription
                )
            }

            // 2. Load admin-added dishes from FOOD_TABLE that DON'T exist in System B.
            //    These are researcher-added via the admin panel and synced to mobile.
            //    They show flat per-100g nutrition (no ingredient breakdown).
            val allFoodItems = foodDao.getAllFoods()
            val adminOnlyDishes = allFoodItems
                .filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }
                .map { food ->
                    ExploreDish(
                        dishLabel = food.mlLabel,
                        nameEn = food.nameEn,
                        namePh = food.namePh,
                        category = food.category,
                        calories = food.caloriesPer100g.toInt(),
                        protein = food.proteinPer100g.toInt(),
                        carbs = food.carbsPer100g.toInt(),
                        fats = food.fatPer100g.toInt(),
                        sodium = food.sodiumPer100g.toInt(),
                        dataSource = food.dataSource,
                        ingredientCount = 0,
                        ingredientNames = emptyList(),
                        servings = 1,
                        perServingWeightG = 100f
                    )
                }

            _allDishes.value = dishes + adminOnlyDishes
            _isLoading.value = false
        }
    }

    // --- Actions ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    /**
     * Adds all CORE ingredients for a dish to the user's pantry.
     * Skips ingredients already in the pantry (INSERT IGNORE).
     * Uses the same lowercase normalization as PantryViewModel.addIngredient().
     */
    fun addCoreIngredientsToPantry(dishLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = pantryDao.getIngredientDetailsForDish(dishLabel)
            val coreIngredients = details
                .filter { it.ingredient_type == "core" }
                .map { normalizePantryKey(it.ingredient_name) }
                .distinct()

            // Check which ingredients are actually new (not already in pantry)
            val currentPantry = pantryItems.value.map { normalizePantryKey(it) }.toSet()
            val newIngredients = coreIngredients.filter { it !in currentPantry }

            for (ingredient in newIngredients) {
                pantryDao.insertItem(PantryItem(ingredientName = ingredient))
            }
            if (uid.isNotEmpty() && newIngredients.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPantryItemsBatch(uid, newIngredients) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }

            // Emit snackbar feedback
            val message = if (newIngredients.isEmpty()) {
                "All ${coreIngredients.size} core ingredients already in Pantry"
            } else {
                "Added ${newIngredients.size} core ingredient${if (newIngredients.size > 1) "s" else ""} to Pantry"
            }
            _snackbarEvent.emit(message)
        }
    }

    /**
     * Adds a single ingredient to the user's pantry by its ingredient_key.
     * Skips if already present (INSERT IGNORE).
     */
    fun addSingleIngredientToPantry(ingredientKey: String, displayName: String = "") {
        val normalized = normalizePantryKey(ingredientKey)
        if (normalized.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.insertItem(PantryItem(ingredientName = normalized))
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPantryItem(uid, normalized) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            _snackbarEvent.emit("Added ${displayName.ifBlank { formatName(normalized) }} to Pantry")
        }
    }

    /**
     * Removes a single ingredient from the user's pantry by its ingredient_key.
     */
    fun removeSingleIngredientFromPantry(ingredientKey: String, displayName: String = "") {
        val normalized = normalizePantryKey(ingredientKey)
        if (normalized.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.deleteItem(normalized)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePantryItem(uid, normalized) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            _snackbarEvent.emit("Removed ${displayName.ifBlank { formatName(normalized) }} from Pantry")
        }
    }

    /**
     * Removes all distinct CORE ingredients for a dish from the user's pantry.
     */
    fun removeCoreIngredientsFromPantry(dishLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val coreIngredients = pantryDao.getIngredientDetailsForDish(dishLabel)
                .filter { it.ingredient_type == "core" }
                .map { normalizePantryKey(it.ingredient_name) }
                .distinct()

            if (coreIngredients.isEmpty()) return@launch

            val currentPantry = pantryItems.value.map { normalizePantryKey(it) }.toSet()
            val ingredientsToRemove = coreIngredients.filter { it in currentPantry }
            if (ingredientsToRemove.isEmpty()) {
                _snackbarEvent.emit("No core ingredients to remove from Pantry")
                return@launch
            }

            pantryDao.deleteItems(ingredientsToRemove)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try {
                        ingredientsToRemove.forEach { firestoreSyncRepo.deletePantryItem(uid, it) }
                    } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            _snackbarEvent.emit("Removed ${ingredientsToRemove.size} core ingredient${if (ingredientsToRemove.size > 1) "s" else ""} from Pantry")
        }
    }

    /**
     * Returns the full DishRecipeEntity for a dish (for use in the detail bottom sheet).
     */
    suspend fun getDishRecipe(dishLabel: String): DishRecipeEntity? {
        return dishRecipeDao.getByDishLabel(dishLabel)
    }

    /**
     * Returns ingredient details for a dish (for use in the detail bottom sheet).
     */
    suspend fun getIngredientDetails(dishLabel: String): List<IngredientInfo> {
        val details = pantryDao.getIngredientDetailsForDish(dishLabel)
        // Resolve authoritative display names from RAW_INGREDIENTS_TABLE
        val keys = details.map { it.ingredient_name }.distinct()
        val displayNames = rawIngredientDao.getDisplayNamesForKeys(keys)
        val nameMap = displayNames.associate { it.ingredient_key to it.display_name }
        return details.map { detail ->
            IngredientInfo(
                name = nameMap[detail.ingredient_name] ?: detail.ingredient_name,
                ingredientKey = detail.ingredient_name,
                type = detail.ingredient_type,
                category = detail.ingredient_category,
                portionQuantity = detail.portion_quantity,
                preparationMethod = detail.preparation_method,
                step = detail.step
            )
        }
    }

    /**
     * Formats a name like "sinigang_pork" to "Sinigang Pork".
     */
    fun formatName(label: String): String {
        return label.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Returns a user-friendly display label for the nutritional data source.
     *
     * Since all ingredient nutrition now comes from USDA FoodData Central
     * (via the raw_ingredients.json pipeline), this always returns USDA
     * regardless of the legacy `dataSource` field on FoodItem.
     */
    fun getSourceDisplayLabel(source: String): String = when (source) {
        "COMMUNITY" -> "CalorieKo Community Database"
        "DOST_FNRI_FCT" -> "DOST-FNRI Food Composition Table"
        else -> "USDA FoodData Central"
    }

    /**
     * Returns a short badge label for a data source key.
     */
    fun getSourceBadgeLabel(source: String): String = when (source) {
        "COMMUNITY" -> "Community"
        "DOST_FNRI_FCT" -> "DOST-FNRI"
        else -> "USDA"
    }

    /**
     * Returns the URL for the nutritional data source (USDA).
     */
    fun getSourceUrl(source: String): String = when (source) {
        "COMMUNITY" -> ""
        else -> "https://fdc.nal.usda.gov/food-search"
    }

    /**
     * Returns the proof document info for a specific dish's nutritional data.
     *
     * Since all dishes now use USDA FoodData Central for ingredient-level
     * nutrition, only single-ingredient dishes (like eggs and chicken parts)
     * that map directly to a specific USDA FDC entry have a direct proof URL.
     */
    fun getDishProofDocument(mlLabel: String, @Suppress("UNUSED_PARAMETER") dataSource: String): DishProofDocument {
        // Single-ingredient dishes with direct USDA nutrient detail pages
        // Note: egg dishes removed — they are multi-ingredient recipes, not single-FDC-entry items
        val usdaUrls = mapOf(
            "chicken_drumstick" to "https://fdc.nal.usda.gov/food-details/171126/nutrients",
            "chicken_thigh" to "https://fdc.nal.usda.gov/food-details/171127/nutrients",
            "chicken_wing" to "https://fdc.nal.usda.gov/food-details/172830/nutrients",
            "chicken_breast" to "https://fdc.nal.usda.gov/food-details/171125/nutrients"
        )

        val url = usdaUrls[mlLabel] ?: ""
        return if (url.isNotEmpty()) DishProofDocument(ProofType.URL, url)
        else DishProofDocument(ProofType.NONE, "")
    }

    /**
     * Returns the recipe source document for a dish, if available.
     *
     * These are DOST-FNRI Menu Guide PDFs that document the original
     * recipe (ingredients, portions, preparation method). Even though
     * the nutritional values are now computed from USDA data, these PDFs
     * remain valuable as the provenance for the recipe itself.
     */
    fun getRecipeSourceDocument(mlLabel: String): DishProofDocument {
        // Dishes with FNRI recipe source PDFs in assets/sources/
        // mackerel_fried, milkfish_fried, rice_well_milled removed (no applicable FNRI PDF)
        // linatan, humba_pork, lawuy added (FNRI PDFs placed in assets/sources/)
        val fnriDishes = setOf(
            "chicken_tinola", "chopseuy", "egg_ampalaya",
            "galunggong_grilled", "kinilaw_tuna",
            "menudo", "sinabawang_bangus", "pinakbet",
            "sinigang_pork", "sinuglaw_pork",
            "tilapia_fried", "kwekwek", "udong",
            "linatan", "humba_pork", "lawuy"
        )

        return if (mlLabel in fnriDishes) {
            DishProofDocument(ProofType.PDF_ASSET, "sources/$mlLabel.pdf")
        } else {
            DishProofDocument(ProofType.NONE, "")
        }
    }
}

/**
 * Represents proof document info for a dish.
 */
data class DishProofDocument(
    val type: ProofType,
    val path: String // URL for browser, or asset path for PDF
)

enum class ProofType { URL, PDF_ASSET, NONE }

