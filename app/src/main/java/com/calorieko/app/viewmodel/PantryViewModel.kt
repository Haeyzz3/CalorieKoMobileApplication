package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.local.RecipeNutritionCalculator
import com.calorieko.app.data.local.IngredientNutritionBreakdown
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.NutritionResult
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.repository.NutritionalValuesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Rich ingredient info for display in the Recipe Detail bottom sheet.
 */
data class IngredientInfo(
    val name: String,
    val type: String,           // "core" or "optional"
    val category: String,       // "protein", "produce", "seasoning", "pantry_staple"
    val portionQuantity: String, // e.g. "5 cups", "" if not specified
    val preparationMethod: String, // e.g. "sliced", "" if not specified
    val step: Int                // 1-based step number
)

/**
 * Result of the core-aware recipe matching engine.
 */
data class DishResult(
    val dishLabel: String,
    val dishName: String,
    val ingredients: List<String>,
    val ingredientDetails: List<IngredientInfo> = emptyList(),
    val missingCoreIngredients: List<String>,
    val missingOptionalIngredients: List<String>,
    val coreMatchedCount: Int = 0,
    val coreTotalCount: Int = 0,
    // Core energy
    val calories: Int = 0,
    // Macros
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    // Minerals & vitamins
    val sodium: Int = 0,
    val potassium: Float = 0f,
    val vitaminA: Float = 0f,
    val vitaminC: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    // Source attribution
    val dataSource: String = "USDA_FDC"
)

class PantryViewModel(
    private val auth: FirebaseAuth,
    private val pantryDao: PantryDao,
    private val mealPlanDao: MealPlanDao,
    private val dishRecipeDao: DishRecipeDao,
    private val rawIngredientDao: RawIngredientDao,
    private val calculator: RecipeNutritionCalculator,
    private val firestoreSyncRepo: FirestoreSyncRepository,
    private val userDao: UserDao,
    private val nutritionalValuesRepo: NutritionalValuesRepository,
    private val appContext: Context
) : ViewModel() {

    private val uid: String get() = auth.currentUser?.uid ?: ""

    // --- Factory ---
    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            pantryDao: PantryDao,
            mealPlanDao: MealPlanDao,
            dishRecipeDao: DishRecipeDao,
            rawIngredientDao: RawIngredientDao,
            calculator: RecipeNutritionCalculator,
            firestoreSyncRepo: FirestoreSyncRepository,
            userDao: UserDao,
            nutritionalValuesRepo: NutritionalValuesRepository,
            appContext: Context
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
                    return PantryViewModel(auth, pantryDao, mealPlanDao, dishRecipeDao, rawIngredientDao, calculator, firestoreSyncRepo, userDao, nutritionalValuesRepo, appContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
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
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int,
        val fiber: Float,
        val sugar: Float,
        val sodium: Int,
        val potassium: Float,
        val vitaminA: Float,
        val vitaminC: Float,
        val calcium: Float,
        val iron: Float
    )

    // --- User's actual daily calorie target and sodium limit ---
    private val _userCalorieTarget = MutableStateFlow(2000)
    val userCalorieTarget: StateFlow<Int> = _userCalorieTarget.asStateFlow()

    private val _userSodiumLimit = MutableStateFlow(2000)
    val userSodiumLimit: StateFlow<Int> = _userSodiumLimit.asStateFlow()

    // --- Pantry items grouped by category for UI ---
    private val _pantryItemsByCategory = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val pantryItemsByCategory: StateFlow<Map<String, List<String>>> = _pantryItemsByCategory.asStateFlow()

    // --- All browsable ingredients for the Ingredient Browser ---
    private val _allBrowsableIngredients = MutableStateFlow<List<RawIngredientEntity>>(emptyList())
    val allBrowsableIngredients: StateFlow<List<RawIngredientEntity>> = _allBrowsableIngredients.asStateFlow()

    init {
        // Defense-in-depth: ensure reference data is seeded before any queries.
        // Covers edge cases where db.clearAllTables() was called (e.g., wipe operations)
        // and the async FoodDatabaseCallback re-seed hasn't completed yet.
        viewModelScope.launch(Dispatchers.IO) {
            // No-op: legacy ensureReferenceDataSeeded removed since we now
            // use DISH_RECIPES_TABLE which is seeded via FoodDatabaseCallback
        }

        // Load all unique ingredients for autocomplete
        viewModelScope.launch(Dispatchers.IO) {
            _allIngredients.value = pantryDao.getAllUniqueIngredients()
        }

        // Load all browsable ingredients for the Ingredient Browser (excluding store_bought)
        viewModelScope.launch(Dispatchers.IO) {
            _allBrowsableIngredients.value = rawIngredientDao.getAllBrowsable()
        }

        // Load user's actual nutritional targets
        viewModelScope.launch(Dispatchers.IO) {
            loadUserNutritionalTargets()
        }

        // React to pantry changes → recompute recipe matches + category grouping
        viewModelScope.launch {
            pantryItems.collect { items ->
                withContext(Dispatchers.IO) {
                    recomputeRecipeMatches(items)
                    recomputePantryCategories(items)
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

    /**
     * Loads the user's actual calorie target and sodium limit from their profile
     * via the Mifflin-St Jeor calculation in NutritionalValuesRepository.
     */
    private suspend fun loadUserNutritionalTargets() {
        val currentUid = uid
        if (currentUid.isEmpty()) return

        val profile = userDao.getUserProfile(currentUid) ?: return
        val targets = nutritionalValuesRepo.getTargetsForUser(profile)

        _userCalorieTarget.value = targets.targetCalories
        _userSodiumLimit.value = targets.targetSodium
    }

    // ============================================================
    // Pantry Actions
    // ============================================================

    fun addIngredient(name: String) {
        val trimmed = name.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.insertItem(PantryItem(ingredientName = trimmed))
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPantryItem(uid, trimmed) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun removeIngredient(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.deleteItem(name)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePantryItem(uid, name) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    /**
     * Applies batch pantry changes from the Ingredient Browser.
     * Computes the diff between current pantry and the selected set,
     * then adds new items and removes deselected items in batch.
     *
     * @param selectedKeys The full set of ingredient_keys the user has checked
     */
    fun batchUpdatePantry(selectedKeys: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPantry = pantryItems.value.toSet()

            val toAdd = selectedKeys - currentPantry
            val toRemove = currentPantry - selectedKeys

            // Batch insert new items
            if (toAdd.isNotEmpty()) {
                pantryDao.insertAll(toAdd.map { PantryItem(ingredientName = it) })
            }

            // Batch remove deselected items
            if (toRemove.isNotEmpty()) {
                pantryDao.deleteItems(toRemove.toList())
            }

            // Single Firestore sync pass for all changes
            if (uid.isNotEmpty() && (toAdd.isNotEmpty() || toRemove.isNotEmpty())) {
                withTimeoutOrNull(5_000L) {
                    try {
                        toAdd.forEach { firestoreSyncRepo.syncPantryItem(uid, it) }
                        toRemove.forEach { firestoreSyncRepo.deletePantryItem(uid, it) }
                    } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
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
    // Core-Aware Recipe Matching Engine
    // ============================================================

    /**
     * Recomputes recipe matches using a core-ingredient-based algorithm.
     *
     * Classification:
     * - "Ready to Cook" = all core ingredients present (optional may be missing)
     * - "Almost Ready" = at least 1 core ingredient present, but not all
     * - Hidden = 0 core ingredients present (filtered out by SQL HAVING clause)
     *
     * Sorting: by core_matched / core_total ratio (descending)
     */
    private suspend fun recomputeRecipeMatches(pantryItems: List<String>) {
        if (pantryItems.isEmpty()) {
            _readyToCookDishes.value = emptyList()
            _almostReadyDishes.value = emptyList()
            return
        }

        val matchInfoList = pantryDao.getDishMatchCounts(pantryItems)

        val ready = mutableListOf<DishResult>()
        val almostReady = mutableListOf<DishResult>()

        for (info in matchInfoList) {
            val allIngredients = pantryDao.getIngredientsForDish(info.dish_label)
            val details = pantryDao.getIngredientDetailsForDish(info.dish_label)
            val missingWithType = if (info.core_matched < info.core_total || info.matched_count < info.total_ingredients) {
                pantryDao.getMissingIngredients(info.dish_label, pantryItems)
            } else {
                emptyList()
            }

            val missingCore = missingWithType.filter { it.ingredient_type == "core" }.map { it.ingredient_name }
            val missingOptional = missingWithType.filter { it.ingredient_type == "optional" }.map { it.ingredient_name }

            val nutrition = getDishNutrition(info.dish_label)

            val ingredientInfoList = details.map { detail ->
                IngredientInfo(
                    name = detail.ingredient_name,
                    type = detail.ingredient_type,
                    category = detail.ingredient_category,
                    portionQuantity = detail.portion_quantity,
                    preparationMethod = detail.preparation_method,
                    step = detail.step
                )
            }

            val result = DishResult(
                dishLabel = info.dish_label,
                dishName = formatDishName(info.dish_label),
                ingredients = allIngredients,
                ingredientDetails = ingredientInfoList,
                missingCoreIngredients = missingCore,
                missingOptionalIngredients = missingOptional,
                coreMatchedCount = info.core_matched,
                coreTotalCount = info.core_total,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fats = nutrition.fats,
                fiber = nutrition.fiber,
                sugar = nutrition.sugar,
                sodium = nutrition.sodium,
                potassium = nutrition.potassium,
                vitaminA = nutrition.vitaminA,
                vitaminC = nutrition.vitaminC,
                calcium = nutrition.calcium,
                iron = nutrition.iron
            )

            if (info.core_matched >= info.core_total) {
                // All core ingredients present → Ready to Cook
                ready.add(result)
            } else {
                // Some core ingredients present → Almost Ready
                almostReady.add(result)
            }
        }

        // Sort by core completion ratio (descending)
        val coreRatio: (DishResult) -> Float = { it.coreMatchedCount.toFloat() / it.coreTotalCount.toFloat() }
        _readyToCookDishes.value = ready.sortedByDescending(coreRatio)
        _almostReadyDishes.value = almostReady.sortedByDescending(coreRatio)
    }

    /**
     * Groups pantry items by their ingredient category from the dish_ingredients table.
     * Items not found in the table are placed in "pantry_staple" by default.
     */
    private suspend fun recomputePantryCategories(items: List<String>) {
        if (items.isEmpty()) {
            _pantryItemsByCategory.value = emptyMap()
            return
        }

        val categoryMappings = pantryDao.getCategoriesForIngredients(items)
        val categoryMap = categoryMappings.associate { it.ingredient_name to it.ingredient_category }

        val grouped = items.groupBy { ingredient ->
            categoryMap[ingredient] ?: "pantry_staple"
        }

        _pantryItemsByCategory.value = grouped
    }

    // ============================================================
    // Meal Plan Actions
    // ============================================================

    fun addMealToPlan(dayIndex: Int, dishLabel: String, mealSlot: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val meal = PlannedMealEntity(
                dayIndex = dayIndex,
                dishLabel = dishLabel,
                weekStartDate = _currentWeekStart.value,
                mealSlot = mealSlot
            )
            mealPlanDao.insertMeal(meal)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPlannedMeal(uid, meal) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun removeDishFromSlot(dayIndex: Int, mealSlot: String, dishLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mealPlanDao.removeDish(dayIndex, _currentWeekStart.value, mealSlot, dishLabel)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePlannedMeal(uid, dayIndex, _currentWeekStart.value, mealSlot, dishLabel) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun clearMealSlot(dayIndex: Int, mealSlot: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mealPlanDao.clearSlot(dayIndex, _currentWeekStart.value, mealSlot)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePlannedMealSlot(uid, dayIndex, _currentWeekStart.value, mealSlot) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun clearMealDay(dayIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            mealPlanDao.clearDay(dayIndex, _currentWeekStart.value)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.clearDayPlannedMeals(uid, dayIndex, _currentWeekStart.value) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun clearMealWeek() {
        viewModelScope.launch(Dispatchers.IO) {
            val week = _currentWeekStart.value
            mealPlanDao.clearWeek(week)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.clearWeekPlannedMeals(uid, week) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun clearAllPantryItems() {
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.clearAllItems()
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.clearPantryItems(uid) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
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
     * Gets nutritional data for a dish by looking up the DISH_RECIPES_TABLE.
     * Returns per-serving nutrient values.
     * Results are cached to avoid repeated DB lookups.
     */
    private suspend fun getDishNutrition(dishLabel: String): DishNutritionInfo {
        _dishNutritionCache[dishLabel]?.let { return it }

        val recipe = dishRecipeDao.getByDishLabel(dishLabel)
        val info = if (recipe != null) {
            DishNutritionInfo(
                calories = recipe.calPerServing.toInt(),
                protein = recipe.proteinPerServing.toInt(),
                carbs = recipe.carbsPerServing.toInt(),
                fats = recipe.fatPerServing.toInt(),
                fiber = recipe.fiberPerServing,
                sugar = recipe.sugarPerServing,
                sodium = recipe.sodiumPerServing.toInt(),
                potassium = recipe.potassiumPerServing,
                vitaminA = recipe.vitaminAPerServing,
                vitaminC = recipe.vitaminCPerServing,
                calcium = recipe.calciumPerServing,
                iron = recipe.ironPerServing
            )
        } else {
            // Dish exists in ingredients table but not in recipes table — no nutrition data available
            DishNutritionInfo(0, 0, 0, 0, 0f, 0f, 0, 0f, 0f, 0f, 0f, 0f)
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

    // ============================================================
    // Ingredient Substitution
    // ============================================================

    /**
     * Active substitutions per dish. Key = dishLabel, Value = map of originalKey → replacementKey.
     * Ephemeral — not persisted, resets when user closes the detail sheet.
     */
    private val _activeSubstitutions = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val activeSubstitutions: StateFlow<Map<String, Map<String, String>>> = _activeSubstitutions.asStateFlow()

    /**
     * Recalculated per-serving nutrition for dishes with active substitutions.
     * Key = dishLabel, Value = NutritionResult with the substituted values.
     */
    private val _substitutedNutrition = MutableStateFlow<Map<String, NutritionResult>>(emptyMap())
    val substitutedNutrition: StateFlow<Map<String, NutritionResult>> = _substitutedNutrition.asStateFlow()

    /**
     * Returns all substitute candidates for an ingredient (same sub_category).
     * Returns empty list if the ingredient has no alternatives.
     */
    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<RawIngredientEntity> {
        val ingredient = rawIngredientDao.getByKey(ingredientKey) ?: return emptyList()
        return rawIngredientDao.getSubstituteCandidates(ingredient.subCategory, ingredientKey)
    }

    /**
     * Returns per-ingredient nutrition breakdown for a dish.
     * Each entry shows how many calories/protein/fat/carbs that ingredient contributes.
     */
    suspend fun getIngredientBreakdown(dishLabel: String): Map<String, IngredientNutritionBreakdown> {
        return calculator.getIngredientBreakdown(dishLabel)
    }

    /**
     * Applies a substitution: replaces [originalKey] with [newKey] in the dish recipe
     * and recalculates nutrition using the dynamic path.
     */
    fun applySubstitution(dishLabel: String, originalKey: String, newKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update the substitution map
            val currentSubs = _activeSubstitutions.value.toMutableMap()
            val dishSubs = (currentSubs[dishLabel] ?: emptyMap()).toMutableMap()
            dishSubs[originalKey] = newKey
            currentSubs[dishLabel] = dishSubs
            _activeSubstitutions.value = currentSubs

            // Recalculate with substitutions
            val result = calculator.calculateWithSubstitution(dishLabel, dishSubs)
            val currentNutrition = _substitutedNutrition.value.toMutableMap()
            currentNutrition[dishLabel] = result
            _substitutedNutrition.value = currentNutrition
        }
    }

    /**
     * Removes a specific substitution for an ingredient in a dish.
     * If no more substitutions remain, removes the dish entry entirely.
     */
    fun removeSubstitution(dishLabel: String, originalKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSubs = _activeSubstitutions.value.toMutableMap()
            val dishSubs = (currentSubs[dishLabel] ?: emptyMap()).toMutableMap()
            dishSubs.remove(originalKey)

            if (dishSubs.isEmpty()) {
                currentSubs.remove(dishLabel)
                val currentNutrition = _substitutedNutrition.value.toMutableMap()
                currentNutrition.remove(dishLabel)
                _substitutedNutrition.value = currentNutrition
            } else {
                currentSubs[dishLabel] = dishSubs
                val result = calculator.calculateWithSubstitution(dishLabel, dishSubs)
                val currentNutrition = _substitutedNutrition.value.toMutableMap()
                currentNutrition[dishLabel] = result
                _substitutedNutrition.value = currentNutrition
            }

            _activeSubstitutions.value = currentSubs
        }
    }

    /**
     * Clears all substitutions for a dish, resetting nutrition to the original values.
     */
    fun clearSubstitutions(dishLabel: String) {
        val currentSubs = _activeSubstitutions.value.toMutableMap()
        currentSubs.remove(dishLabel)
        _activeSubstitutions.value = currentSubs

        val currentNutrition = _substitutedNutrition.value.toMutableMap()
        currentNutrition.remove(dishLabel)
        _substitutedNutrition.value = currentNutrition
    }
}
