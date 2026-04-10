package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents a dish for display in the Explore screen.
 * Enriched with ingredient count and data source info beyond what FoodItem alone provides.
 */
data class ExploreDish(
    val mlLabel: String,
    val nameEn: String,
    val namePh: String,
    val category: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val sodium: Int,
    val dataSource: String,
    val ingredientCount: Int = 0
)

/**
 * ViewModel for the Explore Dishes screen.
 * Provides dish browsing, search, source filtering, and quick add-to-pantry.
 */
class ExploreViewModel(
    private val auth: FirebaseAuth,
    private val foodDao: FoodDao,
    private val pantryDao: PantryDao,
    private val firestoreSyncRepo: FirestoreSyncRepository
) : ViewModel() {

    private val uid: String get() = auth.currentUser?.uid ?: ""

    // --- Factory ---
    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            foodDao: FoodDao,
            pantryDao: PantryDao,
            firestoreSyncRepo: FirestoreSyncRepository
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
                    return ExploreViewModel(auth, foodDao, pantryDao, firestoreSyncRepo) as T
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

    // --- Source filter ---
    private val _sourceFilter = MutableStateFlow("ALL")
    val sourceFilter: StateFlow<String> = _sourceFilter.asStateFlow()

    // --- Filtered + grouped dishes (derived state) ---
    val filteredDishes: StateFlow<Map<String, List<ExploreDish>>> = combine(
        _allDishes,
        _searchQuery,
        _sourceFilter
    ) { dishes, query, source ->
        dishes
            .filter { dish ->
                // Exclude non-food ML labels
                dish.mlLabel != "negative"
            }
            .filter { dish ->
                // Search filter
                if (query.isBlank()) true
                else dish.nameEn.contains(query, ignoreCase = true) ||
                     dish.namePh.contains(query, ignoreCase = true) ||
                     dish.mlLabel.contains(query, ignoreCase = true)
            }
            .filter { dish ->
                // Source filter
                when (source) {
                    "ALL" -> true
                    else -> dish.dataSource == source
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

    init {
        loadAllDishes()
    }

    private fun loadAllDishes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val foods = foodDao.getAllFoods()
            val dishes = foods.map { food ->
                val ingredientCount = pantryDao.getIngredientsForDish(food.mlLabel).size
                ExploreDish(
                    mlLabel = food.mlLabel,
                    nameEn = food.nameEn,
                    namePh = food.namePh,
                    category = food.category,
                    calories = food.caloriesPer100g.toInt(),
                    protein = food.proteinPer100g.toInt(),
                    carbs = food.carbsPer100g.toInt(),
                    fats = food.fatPer100g.toInt(),
                    sodium = food.sodiumPer100g.toInt(),
                    dataSource = food.dataSource,
                    ingredientCount = ingredientCount
                )
            }

            _allDishes.value = dishes
            _isLoading.value = false
        }
    }

    // --- Actions ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSourceFilter(source: String) {
        _sourceFilter.value = source
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
                .map { it.ingredient_name.trim().lowercase() }
                .distinct()

            for (ingredient in coreIngredients) {
                pantryDao.insertItem(PantryItem(ingredientName = ingredient))
            }
            if (uid.isNotEmpty() && coreIngredients.isNotEmpty()) {
                firestoreSyncRepo.syncPantryItemsBatch(uid, coreIngredients)
            }
        }
    }

    /**
     * Returns the full FoodItem for a dish (for use in the detail bottom sheet).
     */
    suspend fun getFoodItem(mlLabel: String): FoodItem? {
        return foodDao.getFoodByMlLabel(mlLabel)
    }

    /**
     * Returns ingredient details for a dish (for use in the detail bottom sheet).
     */
    suspend fun getIngredientDetails(dishLabel: String): List<IngredientInfo> {
        return pantryDao.getIngredientDetailsForDish(dishLabel).map { detail ->
            IngredientInfo(
                name = detail.ingredient_name,
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
     * Returns a user-friendly display label for a data source key.
     */
    fun getSourceDisplayLabel(source: String): String {
        return when (source) {
            "DOST_FNRI_MENU_GUIDE" -> "DOST-FNRI Menu Guide"
            "DOST_FNRI_FCT" -> "DOST-FNRI FCT"
            "USDA_FNDDS" -> "USDA FNDDS"
            else -> source
        }
    }

    /**
     * Returns a short badge label for a data source key.
     */
    fun getSourceBadgeLabel(source: String): String {
        return when (source) {
            "DOST_FNRI_MENU_GUIDE" -> "FNRI"
            "DOST_FNRI_FCT" -> "FCT"
            "USDA_FNDDS" -> "USDA"
            else -> source
        }
    }

    /**
     * Returns the URL for a data source key.
     */
    fun getSourceUrl(source: String): String {
        return when (source) {
            "DOST_FNRI_MENU_GUIDE" -> "https://www.fnri.dost.gov.ph/index.php/tools-and-standard/fnri-menu-guide-calendar"
            "DOST_FNRI_FCT" -> "https://i.fnri.dost.gov.ph/login/fct"
            "USDA_FNDDS" -> "https://fdc.nal.usda.gov/food-search?type=Survey%20(FNDDS)"
            else -> ""
        }
    }
}
