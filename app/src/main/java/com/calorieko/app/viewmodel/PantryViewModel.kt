package com.calorieko.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/**
 * Result of the Cosine Similarity recipe matching engine.
 */
data class DishResult(
    val dishLabel: String,
    val dishName: String,           // Human-readable, derived from dishLabel
    val ingredients: List<String>,
    val missingIngredients: List<String>,
    val similarityScore: Float,     // 0.0 to 1.0
    val calories: Int = 0,
    val sodium: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0
)

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val pantryDao = database.pantryDao()
    private val mealPlanDao = database.mealPlanDao()
    private val foodDao = database.foodDao()

    // --- Cosine Similarity threshold for "Almost Ready" ---
    companion object {
        /** Minimum cosine similarity score for a dish to appear in "Almost Ready" */
        const val ALMOST_READY_THRESHOLD = 0.6f
    }

    // --- Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Pantry items (reactive from Room) ---
    val pantryItems: StateFlow<List<String>> = pantryDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- All unique ingredients for autocomplete ---
    private val _allIngredients = MutableStateFlow<List<String>>(emptyList())

    // --- Autocomplete suggestions (filtered) ---
    private val _autocompleteSuggestions = MutableStateFlow<List<String>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<String>> = _autocompleteSuggestions.asStateFlow()

    // --- Recipe matching results ---
    private val _readyToCookDishes = MutableStateFlow<List<DishResult>>(emptyList())
    val readyToCookDishes: StateFlow<List<DishResult>> = _readyToCookDishes.asStateFlow()

    private val _almostReadyDishes = MutableStateFlow<List<DishResult>>(emptyList())
    val almostReadyDishes: StateFlow<List<DishResult>> = _almostReadyDishes.asStateFlow()

    // --- Meal Plan ---
    private val _currentWeekStart = MutableStateFlow(getWeekStartDate())

    val plannedMeals: StateFlow<List<PlannedMealEntity>> = mealPlanDao
        .getMealsForWeek(getWeekStartDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weeklyCalories = MutableStateFlow(0)
    val weeklyCalories: StateFlow<Int> = _weeklyCalories.asStateFlow()

    private val _avgDailySodium = MutableStateFlow(0)
    val avgDailySodium: StateFlow<Int> = _avgDailySodium.asStateFlow()

    // --- Cache for dish nutritional data ---
    private val _dishNutritionCache = mutableMapOf<String, DishNutritionInfo>()

    private data class DishNutritionInfo(
        val calories: Int, val sodium: Int, val protein: Int, val carbs: Int, val fats: Int
    )

    init {
        // Load all unique ingredients for autocomplete
        viewModelScope.launch(Dispatchers.IO) {
            _allIngredients.value = pantryDao.getAllUniqueIngredients()
        }

        // React to pantry changes → recompute recipe matches
        viewModelScope.launch {
            pantryItems.collect { items ->
                withContext(Dispatchers.IO) {
                    recomputeRecipeMatches(items)
                }
            }
        }

        // React to planned meals changes → recompute weekly stats
        viewModelScope.launch {
            plannedMeals.collect { meals ->
                withContext(Dispatchers.IO) {
                    recomputeWeeklyStats(meals)
                }
            }
        }
    }

    // ============================================================
    // Pantry Actions
    // ============================================================

    fun addIngredient(name: String) {
        val trimmed = name.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.insertItem(PantryItem(ingredientName = trimmed))
        }
    }

    fun removeIngredient(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.deleteItem(name)
        }
    }

    // ============================================================
    // Search & Autocomplete
    // ============================================================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _autocompleteSuggestions.value = emptyList()
            return
        }
        val lowerQuery = query.lowercase()
        val currentPantry = pantryItems.value
        _autocompleteSuggestions.value = _allIngredients.value.filter { ingredient ->
            ingredient.contains(lowerQuery) && ingredient !in currentPantry
        }.take(10)
    }

    // ============================================================
    // Cosine Similarity Engine
    // ============================================================

    /**
     * Recomputes recipe matches using Cosine Similarity whenever the pantry changes.
     *
     * Formula: cosine_sim = matched_count / (√pantry_size × √total_ingredients)
     *
     * - Score = 1.0 → "Ready to Cook"
     * - Score >= ALMOST_READY_THRESHOLD (and < 1.0) → "Almost Ready"
     */
    private suspend fun recomputeRecipeMatches(pantryItems: List<String>) {
        if (pantryItems.isEmpty()) {
            _readyToCookDishes.value = emptyList()
            _almostReadyDishes.value = emptyList()
            return
        }

        val matchInfoList = pantryDao.getDishMatchCounts(pantryItems)
        val pantrySize = pantryItems.size

        val ready = mutableListOf<DishResult>()
        val almostReady = mutableListOf<DishResult>()

        for (info in matchInfoList) {
            val score = info.matched_count.toFloat() /
                    (sqrt(pantrySize.toFloat()) * sqrt(info.total_ingredients.toFloat()))

            if (score >= ALMOST_READY_THRESHOLD) {
                val allIngredients = pantryDao.getIngredientsForDish(info.dish_label)
                val missing = if (score < 1.0f) {
                    pantryDao.getMissingIngredients(info.dish_label, pantryItems)
                } else {
                    emptyList()
                }
                val nutrition = getDishNutrition(info.dish_label)

                val result = DishResult(
                    dishLabel = info.dish_label,
                    dishName = formatDishName(info.dish_label),
                    ingredients = allIngredients,
                    missingIngredients = missing,
                    similarityScore = score,
                    calories = nutrition.calories,
                    sodium = nutrition.sodium,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fats = nutrition.fats
                )

                if (score >= 1.0f) {
                    ready.add(result)
                } else {
                    almostReady.add(result)
                }
            }
        }

        _readyToCookDishes.value = ready.sortedByDescending { it.similarityScore }
        _almostReadyDishes.value = almostReady.sortedByDescending { it.similarityScore }
    }

    // ============================================================
    // Meal Plan Actions
    // ============================================================

    fun addMealToPlan(dayIndex: Int, dishLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mealPlanDao.insertMeal(
                PlannedMealEntity(
                    dayIndex = dayIndex,
                    dishLabel = dishLabel,
                    weekStartDate = _currentWeekStart.value
                )
            )
        }
    }

    fun removeMealFromPlan(dayIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            mealPlanDao.removeMeal(dayIndex, _currentWeekStart.value)
        }
    }

    // ============================================================
    // Weekly Stats
    // ============================================================

    private suspend fun recomputeWeeklyStats(meals: List<PlannedMealEntity>) {
        if (meals.isEmpty()) {
            _weeklyCalories.value = 0
            _avgDailySodium.value = 0
            return
        }

        var totalCalories = 0
        var totalSodium = 0

        for (meal in meals) {
            val nutrition = getDishNutrition(meal.dishLabel)
            totalCalories += nutrition.calories
            totalSodium += nutrition.sodium
        }

        _weeklyCalories.value = totalCalories
        _avgDailySodium.value = totalSodium / meals.size
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Gets nutritional data for a dish by looking up the FOOD_TABLE via ml_label.
     * Results are cached to avoid repeated DB lookups.
     */
    private suspend fun getDishNutrition(dishLabel: String): DishNutritionInfo {
        _dishNutritionCache[dishLabel]?.let { return it }

        val foodItem = foodDao.getFoodByMlLabel(dishLabel)
        val info = if (foodItem != null) {
            DishNutritionInfo(
                calories = foodItem.caloriesPer100g.toInt(),
                sodium = foodItem.sodiumPer100g.toInt(),
                protein = foodItem.proteinPer100g.toInt(),
                carbs = foodItem.carbsPer100g.toInt(),
                fats = foodItem.fatPer100g.toInt()
            )
        } else {
            // Dish exists in ingredients table but not in food table — no nutrition data available
            DishNutritionInfo(0, 0, 0, 0, 0)
        }

        _dishNutritionCache[dishLabel] = info
        return info
    }

    /**
     * Converts a dish label like "sinigang_pork" to "Sinigang Pork".
     */
    private fun formatDishName(label: String): String {
        return label.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Returns the Monday of the current week as an ISO date string.
     */
    private fun getWeekStartDate(): String {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Formats an ingredient name for display: "cooking_oil" → "Cooking Oil".
     */
    fun formatIngredientName(name: String): String {
        return name.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
