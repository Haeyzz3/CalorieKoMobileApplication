package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.local.RecipeNutritionCalculator
import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.repository.MealRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

/** One-shot navigation/UI events emitted by ManualLogViewModel. */
sealed interface ManualLogEvent {
    data object MealConfirmed : ManualLogEvent
}

/** Data class for a planned dish to quick-log. */
data class QuickLogDishEntry(
    val dishLabel: String,
    val substitutionsJson: String = ""
)

/**
 * Shared bridge for passing multi-dish quick-log data across navigation.
 * Set before navigating to the quick-log screen, read on arrival, then cleared.
 */
object QuickLogBridge {
    var pendingMealSlot: String = ""
    var pendingDishes: List<QuickLogDishEntry> = emptyList()

    fun clear() {
        pendingMealSlot = ""
        pendingDishes = emptyList()
    }
}

class ManualLogViewModel(
    private val dishRecipeDao: DishRecipeDao,
    private val rawIngredientDao: RawIngredientDao,
    private val auth: FirebaseAuth,
    private val mealRepository: MealRepository,
    private val calculator: RecipeNutritionCalculator
) : ViewModel() {

    // --- Display name cache: ingredient_key → display_name from RAW_INGREDIENTS_TABLE ---
    private val _displayNameCache = mutableMapOf<String, String>()

    // ── Dish catalogue ──

    private val _allDishes = MutableStateFlow<List<DishRecipeEntity>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredDishes = MutableStateFlow<List<DishRecipeEntity>>(emptyList())
    val filteredDishes: StateFlow<List<DishRecipeEntity>> = _filteredDishes.asStateFlow()

    // ── Selection state ──

    private val _selectedDish = MutableStateFlow<DishRecipeEntity?>(null)
    val selectedDish: StateFlow<DishRecipeEntity?> = _selectedDish.asStateFlow()

    private val _manualWeightText = MutableStateFlow("")
    val manualWeightText: StateFlow<String> = _manualWeightText.asStateFlow()

    private val _servingQuantityText = MutableStateFlow("1")
    val servingQuantityText: StateFlow<String> = _servingQuantityText.asStateFlow()

    // ── Logged dishes ──

    private val _loggedDishes = MutableStateFlow<List<LoggedDish>>(emptyList())
    val loggedDishes: StateFlow<List<LoggedDish>> = _loggedDishes.asStateFlow()

    private val _mealType = MutableStateFlow(getDefaultMealType())
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    private val _showSummary = MutableStateFlow(false)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()

    // ── Confirm guard (prevents duplicate submissions) ──

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    // ── One-shot events ──

    private val _events = Channel<ManualLogEvent>(Channel.BUFFERED)
    val events: Flow<ManualLogEvent> = _events.receiveAsFlow()

    // ── Init ──

    init {
        viewModelScope.launch {
            val dishes = withContext(Dispatchers.IO) { dishRecipeDao.getAllDishRecipes() }
            _allDishes.value = dishes
            _filteredDishes.value = dishes
        }
    }

    // ── Functions ──

    private fun getDefaultMealType(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 10 -> "Breakfast"
            hour < 14 -> "Lunch"
            hour < 17 -> "Snacks"
            else -> "Dinner"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val all = _allDishes.value
        if (query.isBlank()) {
            _filteredDishes.value = all
        } else {
            val lower = query.lowercase()
            _filteredDishes.value = all.filter {
                it.nameEn.lowercase().contains(lower) ||
                it.namePh.lowercase().contains(lower) ||
                it.category.lowercase().contains(lower)
            }
        }
    }

    fun selectDish(dish: DishRecipeEntity) {
        _selectedDish.value = dish
        _manualWeightText.value = defaultWeightText(dish)
        _servingQuantityText.value = "1"
    }

    fun clearSelectedDish() {
        _selectedDish.value = null
        _manualWeightText.value = ""
        _servingQuantityText.value = "1"
    }

    fun updateWeightText(text: String) {
        _manualWeightText.value = text
    }

    fun updateServingQuantityText(text: String) {
        _servingQuantityText.value = text
    }

    /**
     * Computes all 12 nutrients via [RecipeNutritionCalculator] for the
     * selected dish, scaled by the manually entered cooked weight.
     * Resets selection state for multi-dish entry.
     */
    fun addDish() {
        val recipe = _selectedDish.value ?: return
        val unitWeight = _manualWeightText.value.toFloatOrNull() ?: return
        val quantity = _servingQuantityText.value.toFloatOrNull() ?: return
        if (unitWeight <= 0f || quantity <= 0f) return

        val totalWeight = unitWeight * quantity

        viewModelScope.launch {
            val nutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(recipe.dishLabel, unitWeight) * quantity
            }
            val dish = LoggedDish(
                dishNameEn = recipe.nameEn,
                dishNamePh = recipe.namePh,
                weightGrams = totalWeight,
                servingQuantity = quantity,
                confidence = 1.0f, // Manual entry = 100% confidence
                foodId = 0,        // No legacy food_id needed for new calculator path
                dishLabel = recipe.dishLabel,
                calories = nutrients.calories,
                protein = nutrients.protein,
                carbs = nutrients.carbs,
                fat = nutrients.fat,
                fiber = nutrients.fiber,
                sugar = nutrients.sugar,
                // Secondary fat macros zeroed — removed from UI per earlier decision
                saturatedFat = 0f,
                polyunsaturatedFat = 0f,
                monounsaturatedFat = 0f,
                transFat = 0f,
                cholesterol = 0f,
                sodium = nutrients.sodium,
                potassium = nutrients.potassium,
                vitaminA = nutrients.vitaminA,
                vitaminC = nutrients.vitaminC,
                calcium = nutrients.calcium,
                iron = nutrients.iron
            )
            _loggedDishes.update { it + dish }

            // Reset for next dish
            _selectedDish.value = null
            _manualWeightText.value = ""
            _servingQuantityText.value = "1"
        }
    }

    fun removeDish(index: Int) {
        _loggedDishes.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun updateMealType(type: String) {
        _mealType.value = type
    }

    fun setShowSummary(show: Boolean) {
        _showSummary.value = show
    }

    /**
     * Quick-log shortcut from planned meals: pre-selects the dish,
     * calculates one standard serving, and shows the summary.
     * Now supports substitutions from the meal plan.
     */
    fun quickLogFromPlan(dishLabel: String, mealSlot: String, substitutionsJson: String = "") {
        // Override meal type immediately to prevent time-based mismatch
        _mealType.value = mealSlot

        viewModelScope.launch {
            val recipe = withContext(Dispatchers.IO) {
                dishRecipeDao.getByDishLabel(dishLabel)
            }

            if (recipe == null) {
                // Fallback: create minimal entry so UI doesn't freeze
                val displayName = dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
                _loggedDishes.update { it + LoggedDish(
                    dishNameEn = displayName,
                    dishNamePh = displayName,
                    weightGrams = 100f,
                    servingQuantity = 1f,
                    confidence = 0.5f,
                    foodId = 0,
                    dishLabel = dishLabel,
                    calories = 0f, protein = 0f, carbs = 0f, fat = 0f,
                    fiber = 0f, sugar = 0f, saturatedFat = 0f,
                    polyunsaturatedFat = 0f, monounsaturatedFat = 0f,
                    transFat = 0f, cholesterol = 0f, sodium = 0f,
                    potassium = 0f, vitaminA = 0f, vitaminC = 0f,
                    calcium = 0f, iron = 0f
                ) }
                _showSummary.value = true
                return@launch
            }

            val subs = parseSubstitutionsJson(substitutionsJson)

            val nutrients = withContext(Dispatchers.IO) {
                if (subs.isNotEmpty()) {
                    calculator.calculateWithSubstitution(dishLabel, subs)
                } else {
                    calculator.calculatePerServingNutrition(dishLabel)
                }
            }

            // Standard serving weight = cooked_weight / servings
            val servingWeight = if (recipe.servings > 0) recipe.cookedWeightG / recipe.servings else recipe.cookedWeightG

            val dish = LoggedDish(
                dishNameEn = recipe.nameEn,
                dishNamePh = recipe.namePh,
                weightGrams = servingWeight,
                servingQuantity = 1f,
                confidence = 1.0f,
                foodId = 0,
                dishLabel = recipe.dishLabel,
                calories = nutrients.calories,
                protein = nutrients.protein,
                carbs = nutrients.carbs,
                fat = nutrients.fat,
                fiber = nutrients.fiber,
                sugar = nutrients.sugar,
                saturatedFat = 0f,
                polyunsaturatedFat = 0f,
                monounsaturatedFat = 0f,
                transFat = 0f,
                cholesterol = 0f,
                sodium = nutrients.sodium,
                potassium = nutrients.potassium,
                vitaminA = nutrients.vitaminA,
                vitaminC = nutrients.vitaminC,
                calcium = nutrients.calcium,
                iron = nutrients.iron,
                substitutionsJson = substitutionsJson,
                requiresWeightConfirmation = substitutionsJson.isNotBlank()
            )
            _loggedDishes.update { it + dish }
            _showSummary.value = true
        }
    }

    /**
     * Quick-log shortcut for an entire meal slot (multiple dishes at once).
     * Pre-loads all dishes with substitution-aware nutrition and shows summary.
     */
    fun quickLogSlotFromPlan(mealSlot: String, dishEntries: List<QuickLogDishEntry>) {
        // Override meal type immediately to prevent time-based mismatch
        _mealType.value = mealSlot

        viewModelScope.launch {
            val loggedDishList = mutableListOf<LoggedDish>()
            for (entry in dishEntries) {
                val recipe = withContext(Dispatchers.IO) {
                    dishRecipeDao.getByDishLabel(entry.dishLabel)
                }

                if (recipe == null) {
                    // Fallback: create minimal entry so no dish is silently skipped
                    val displayName = entry.dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
                    loggedDishList.add(LoggedDish(
                        dishNameEn = displayName,
                        dishNamePh = displayName,
                        weightGrams = 100f,
                        servingQuantity = 1f,
                        confidence = 0.5f,
                        foodId = 0,
                        dishLabel = entry.dishLabel,
                        calories = 0f, protein = 0f, carbs = 0f, fat = 0f,
                        fiber = 0f, sugar = 0f, saturatedFat = 0f,
                        polyunsaturatedFat = 0f, monounsaturatedFat = 0f,
                        transFat = 0f, cholesterol = 0f, sodium = 0f,
                        potassium = 0f, vitaminA = 0f, vitaminC = 0f,
                        calcium = 0f, iron = 0f
                    ))
                    continue
                }

                val subs = parseSubstitutionsJson(entry.substitutionsJson)
                val nutrients = withContext(Dispatchers.IO) {
                    if (subs.isNotEmpty()) {
                        calculator.calculateWithSubstitution(entry.dishLabel, subs)
                    } else {
                        calculator.calculatePerServingNutrition(entry.dishLabel)
                    }
                }

                val servingWeight = if (recipe.servings > 0)
                    recipe.cookedWeightG / recipe.servings else recipe.cookedWeightG

                loggedDishList.add(LoggedDish(
                    dishNameEn = recipe.nameEn,
                    dishNamePh = recipe.namePh,
                    weightGrams = servingWeight,
                    servingQuantity = 1f,
                    confidence = 1.0f,
                    foodId = 0,
                    dishLabel = recipe.dishLabel,
                    calories = nutrients.calories,
                    protein = nutrients.protein,
                    carbs = nutrients.carbs,
                    fat = nutrients.fat,
                    fiber = nutrients.fiber,
                    sugar = nutrients.sugar,
                    saturatedFat = 0f,
                    polyunsaturatedFat = 0f,
                    monounsaturatedFat = 0f,
                    transFat = 0f,
                    cholesterol = 0f,
                    sodium = nutrients.sodium,
                    potassium = nutrients.potassium,
                    vitaminA = nutrients.vitaminA,
                    vitaminC = nutrients.vitaminC,
                    calcium = nutrients.calcium,
                    iron = nutrients.iron,
                    substitutionsJson = entry.substitutionsJson,
                    requiresWeightConfirmation = entry.substitutionsJson.isNotBlank()
                ))
            }
            _loggedDishes.update { it + loggedDishList }
            _showSummary.value = true
        }
    }

    /**
     * Applies an actual cooked weight to a planned/customized dish and recalculates
     * its nutrition using the substitutions saved with that planned meal.
     */
    fun confirmLoggedDishWeight(index: Int, weightGrams: Float) {
        if (weightGrams <= 0f) return
        val dish = _loggedDishes.value.getOrNull(index) ?: return
        if (dish.dishLabel.isBlank()) return

        viewModelScope.launch {
            val substitutions = parseSubstitutionsJson(dish.substitutionsJson)
            val nutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(dish.dishLabel, weightGrams, substitutions)
            }

            _loggedDishes.update { list ->
                list.mapIndexed { i, current ->
                    if (i == index) {
                        current.copy(
                            weightGrams = weightGrams,
                            calories = nutrients.calories,
                            protein = nutrients.protein,
                            carbs = nutrients.carbs,
                            fat = nutrients.fat,
                            fiber = nutrients.fiber,
                            sugar = nutrients.sugar,
                            sodium = nutrients.sodium,
                            potassium = nutrients.potassium,
                            vitaminA = nutrients.vitaminA,
                            vitaminC = nutrients.vitaminC,
                            calcium = nutrients.calcium,
                            iron = nutrients.iron,
                            requiresWeightConfirmation = false
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun updateLoggedDishQuantity(index: Int, quantity: Float) {
        if (quantity <= 0f) return
        val dish = _loggedDishes.value.getOrNull(index) ?: return
        if (dish.dishLabel.isBlank()) return

        viewModelScope.launch {
            val substitutions = parseSubstitutionsJson(dish.substitutionsJson)
            val recipe = withContext(Dispatchers.IO) {
                dishRecipeDao.getByDishLabel(dish.dishLabel)
            } ?: return@launch
            val servingWeight = if (recipe.perServingWeightG > 0f) {
                recipe.perServingWeightG
            } else if (recipe.servings > 0) {
                recipe.cookedWeightG / recipe.servings
            } else {
                recipe.cookedWeightG
            }
            val currentUnitWeight = if (dish.servingQuantity > 0f && dish.weightGrams > 0f) {
                dish.weightGrams / dish.servingQuantity
            } else {
                servingWeight
            }
            val totalWeight = currentUnitWeight * quantity
            val nutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(dish.dishLabel, totalWeight, substitutions)
            }

            _loggedDishes.update { list ->
                list.mapIndexed { i, current ->
                    if (i == index) {
                        current.copy(
                            weightGrams = totalWeight,
                            servingQuantity = quantity,
                            calories = nutrients.calories,
                            protein = nutrients.protein,
                            carbs = nutrients.carbs,
                            fat = nutrients.fat,
                            fiber = nutrients.fiber,
                            sugar = nutrients.sugar,
                            sodium = nutrients.sodium,
                            potassium = nutrients.potassium,
                            vitaminA = nutrients.vitaminA,
                            vitaminC = nutrients.vitaminC,
                            calcium = nutrients.calcium,
                            iron = nutrients.iron
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    // ── Ingredient Breakdown & Substitution (shared with AI flow) ──

    /** Returns per-ingredient nutrition breakdown for a dish. */
    suspend fun getIngredientBreakdown(dishLabel: String): Map<String, com.calorieko.app.data.local.IngredientNutritionBreakdown> {
        return calculator.getIngredientBreakdown(dishLabel)
    }

    /** Returns substitution candidates for an ingredient. */
    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<com.calorieko.app.data.model.RawIngredientEntity> {
        return calculator.getSubstitutesForIngredient(ingredientKey)
    }

    /** Applies substitutions to a dish and recalculates its nutrition. */
    fun applySubstitutionToDish(dishIndex: Int, substitutions: Map<String, String>) {
        val current = _loggedDishes.value.toMutableList()
        val dish = current.getOrNull(dishIndex) ?: return

        viewModelScope.launch {
            val newNutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(dish.dishLabel, dish.weightGrams, substitutions)
            }
            val updated = dish.copy(
                calories = newNutrients.calories,
                protein = newNutrients.protein,
                carbs = newNutrients.carbs,
                fat = newNutrients.fat,
                fiber = newNutrients.fiber,
                sugar = newNutrients.sugar,
                sodium = newNutrients.sodium,
                potassium = newNutrients.potassium,
                vitaminA = newNutrients.vitaminA,
                vitaminC = newNutrients.vitaminC,
                calcium = newNutrients.calcium,
                iron = newNutrients.iron
            )
            current[dishIndex] = updated
            _loggedDishes.value = current
        }
    }

    /**
     * Formats an ingredient key for display.
     *
     * Resolves the authoritative display name from [_displayNameCache]
     * (sourced from RAW_INGREDIENTS_TABLE) if available.
     * Falls back to naive formatting for user-typed free-form ingredients.
     */
    fun formatIngredientName(key: String): String {
        // Populate cache on first call if empty
        if (_displayNameCache.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val allRaw = rawIngredientDao.getAllRawIngredients()
                allRaw.forEach { _displayNameCache[it.ingredientKey] = it.displayName }
            }
        }
        _displayNameCache[key]?.let { return it }
        return key.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun confirmMeal() {
        if (_loggedDishes.value.any { it.requiresWeightConfirmation }) return

        // Guard: prevent duplicate submissions from rapid taps
        if (_isConfirming.value) return
        _isConfirming.value = true

        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            viewModelScope.launch { _events.send(ManualLogEvent.MealConfirmed) }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mealRepository.saveMeal(uid, _mealType.value, _loggedDishes.value)
            }
            _events.send(ManualLogEvent.MealConfirmed)
        }
    }

    // ── Substitution Helpers ──

    /**
     * Parses a substitutions JSON string into a Map<String, String>.
     * Returns empty map if the string is blank or malformed.
     */
    private fun parseSubstitutionsJson(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key -> map[key] = obj.getString(key) }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun defaultWeightText(dish: DishRecipeEntity): String {
        val defaultWeight = when {
            dish.perServingWeightG > 0f -> dish.perServingWeightG
            dish.servings > 0 -> dish.cookedWeightG / dish.servings
            else -> 0f
        }
        return if (defaultWeight > 0f) {
            String.format(java.util.Locale.US, "%.0f", defaultWeight)
        } else {
            ""
        }
    }

    companion object {
        /** Sentinel value for removed ingredients (matches PantryViewModel.REMOVED_INGREDIENT). */
        const val REMOVED_INGREDIENT = "__REMOVED__"

        fun provideFactory(
            dishRecipeDao: DishRecipeDao,
            rawIngredientDao: RawIngredientDao,
            auth: FirebaseAuth,
            mealRepository: MealRepository,
            calculator: RecipeNutritionCalculator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManualLogViewModel::class.java)) {
                    return ManualLogViewModel(dishRecipeDao, rawIngredientDao, auth, mealRepository, calculator) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
