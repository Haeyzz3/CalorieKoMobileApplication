package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.local.RecipeNutritionCalculator
import com.calorieko.app.data.model.DishRecipeEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.model.NutritionResult
import com.calorieko.app.data.repository.MealRepository
import com.calorieko.app.data.repository.PantryRepository
import com.google.firebase.auth.FirebaseAuth
import com.calorieko.app.util.RecipeCustomizationRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import kotlin.math.abs

/** One-shot navigation/UI events emitted by ManualLogViewModel. */
sealed interface ManualLogEvent {
    data object MealConfirmed : ManualLogEvent
}

/** Data class for a planned dish to quick-log. */
data class QuickLogDishEntry(
    val dishLabel: String,
    val substitutionsJson: String = "",
    val scaledServings: Int = 0,
    val tweaksJson: String = ""
)

enum class PlannedWeightMethod {
    SMART_SCALE,
    MANUAL
}

fun canConfirmPlannedQuickLog(requiredCount: Int, dishes: List<LoggedDish>): Boolean =
    requiredCount > 0 &&
        dishes.size == requiredCount &&
        dishes.all { it.weightGrams > 0f }

/**
 * Represents a pantry ingredient that was used in a confirmed meal.
 * Shown after meal confirmation so the user can opt in to removing it.
 */
data class PantryDeductionItem(
    val ingredientKey: String,
    val displayName: String,
    val usedInDishes: List<String>
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
    private val foodDao: FoodDao,
    private val auth: FirebaseAuth,
    private val mealRepository: MealRepository,
    private val calculator: RecipeNutritionCalculator,
    private val pantryDao: PantryDao,
    private val pantryRepository: PantryRepository,
    private val appContext: Context
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

    // ── Logged dishes ──

    private val _loggedDishes = MutableStateFlow<List<LoggedDish>>(emptyList())
    val loggedDishes: StateFlow<List<LoggedDish>> = _loggedDishes.asStateFlow()

    private val _mealType = MutableStateFlow(getDefaultMealType())
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    private val _showSummary = MutableStateFlow(false)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()

    private val _plannedQuickLogEntries = MutableStateFlow<List<QuickLogDishEntry>>(emptyList())
    val plannedQuickLogEntries: StateFlow<List<QuickLogDishEntry>> = _plannedQuickLogEntries.asStateFlow()

    private val _plannedWeightMethod = MutableStateFlow<PlannedWeightMethod?>(null)
    val plannedWeightMethod: StateFlow<PlannedWeightMethod?> = _plannedWeightMethod.asStateFlow()

    private val _plannedWeightIndex = MutableStateFlow(0)
    val plannedWeightIndex: StateFlow<Int> = _plannedWeightIndex.asStateFlow()

    private val _isPlannedQuickLog = MutableStateFlow(false)
    val isPlannedQuickLog: StateFlow<Boolean> = _isPlannedQuickLog.asStateFlow()

    private val _currentPlannedRecipe = MutableStateFlow<DishRecipeEntity?>(null)
    val currentPlannedRecipe: StateFlow<DishRecipeEntity?> = _currentPlannedRecipe.asStateFlow()

    private val _plannedManualWeightText = MutableStateFlow("")
    val plannedManualWeightText: StateFlow<String> = _plannedManualWeightText.asStateFlow()

    private val _plannedScaleWeight = MutableStateFlow(0f)
    val plannedScaleWeight: StateFlow<Float> = _plannedScaleWeight.asStateFlow()

    private val _plannedScaleWeightStable = MutableStateFlow(false)
    val plannedScaleWeightStable: StateFlow<Boolean> = _plannedScaleWeightStable.asStateFlow()

    private val _pantryDeductionItems = MutableStateFlow<List<PantryDeductionItem>>(emptyList())
    val pantryDeductionItems: StateFlow<List<PantryDeductionItem>> = _pantryDeductionItems.asStateFlow()

    private val _showPantryDeduction = MutableStateFlow(false)
    val showPantryDeduction: StateFlow<Boolean> = _showPantryDeduction.asStateFlow()

    // ── Confirm guard (prevents duplicate submissions) ──

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    private val _isAddingDish = MutableStateFlow(false)
    val isAddingDish: StateFlow<Boolean> = _isAddingDish.asStateFlow()

    private var plannedWeightStabilizationJob: Job? = null
    private var plannedStabilizationTargetWeight = 0f
    private val plannedWeightTolerance = 3.0f

    // ── One-shot events ──

    private val _events = Channel<ManualLogEvent>(Channel.BUFFERED)
    val events: Flow<ManualLogEvent> = _events.receiveAsFlow()

    // ── Init ──

    init {
        viewModelScope.launch {
            val dishes = withContext(Dispatchers.IO) { dishRecipeDao.getAllDishRecipes() }
            val systemBLabels = dishes.map { it.dishLabel }.toSet()

            // Merge admin-added dishes from FOOD_TABLE that don't exist in System B.
            // Synthesize DishRecipeEntity objects so they integrate into the existing
            // search/filter/select flow without changing the UI layer.
            val adminDishes = withContext(Dispatchers.IO) {
                foodDao.getAllFoods()
                    .filter { it.mlLabel !in systemBLabels && it.mlLabel != "negative" }
                    .map { food ->
                        DishRecipeEntity(
                            dishLabel = food.mlLabel,
                            nameEn = food.nameEn,
                            namePh = food.namePh,
                            category = food.category,
                            cookingMethod = "",
                            servings = 1,
                            totalRawWeightG = 100f,
                            dishYieldFactor = 1f,
                            cookedWeightG = 100f,
                            perServingWeightG = 100f,
                            ingredientCount = 0,
                            calPerServing = food.caloriesPer100g,
                            proteinPerServing = food.proteinPer100g,
                            carbsPerServing = food.carbsPer100g,
                            fatPerServing = food.fatPer100g,
                            sodiumPerServing = food.sodiumPer100g
                        )
                    }
            }

            _allDishes.value = dishes + adminDishes
            recomputeFilteredDishes()
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
        recomputeFilteredDishes()
    }

    private fun recomputeFilteredDishes() {
        val all = _allDishes.value
        val loggedLabels = _loggedDishes.value
            .mapNotNull { it.dishLabel.takeIf { label -> label.isNotBlank() } }
            .toSet()
        val available = all.filterNot { it.dishLabel in loggedLabels }
        val query = _searchQuery.value
        _filteredDishes.value = if (query.isBlank()) {
            available
        } else {
            val lower = query.lowercase()
            available.filter {
                it.nameEn.lowercase().contains(lower) ||
                it.namePh.lowercase().contains(lower) ||
                it.category.lowercase().contains(lower)
            }
        }
    }

    fun selectDish(dish: DishRecipeEntity) {
        if (isDishAlreadyLogged(dish.dishLabel)) return
        _selectedDish.value = dish
        _manualWeightText.value = defaultWeightText(dish)
    }

    fun clearSelectedDish() {
        _selectedDish.value = null
        _manualWeightText.value = ""
    }

    fun updateWeightText(text: String) {
        _manualWeightText.value = text
    }

    /**
     * Computes all 12 nutrients via [RecipeNutritionCalculator] for the
     * selected dish, scaled by the manually entered cooked weight.
     * Resets selection state for multi-dish entry.
     */
    fun addDish() {
        if (_isAddingDish.value) return
        val recipe = _selectedDish.value ?: return
        val weightGrams = _manualWeightText.value.toFloatOrNull() ?: return
        if (weightGrams <= 0f) return
        if (isDishAlreadyLogged(recipe.dishLabel)) {
            clearSelectedDish()
            recomputeFilteredDishes()
            return
        }

        _isAddingDish.value = true
        viewModelScope.launch {
            // Fix: Use DishRecipeDao existence check (not calories == 0f) to determine
            // whether this dish has System B (USDA) ingredient-level nutrition data.
            val hasSystemBData = withContext(Dispatchers.IO) {
                dishRecipeDao.getByDishLabel(recipe.dishLabel) != null
            }

            val nutrients = if (hasSystemBData) {
                // System B path: full ingredient-level nutrition via RecipeNutritionCalculator
                withContext(Dispatchers.IO) {
                    calculator.calculatePortionNutrition(recipe.dishLabel, weightGrams)
                }
            } else {
                // System A path: scale flat per-100g values by weight
                // (admin-added dishes without USDA ingredient data)
                val scale = weightGrams / 100f
                NutritionResult(
                    calories = recipe.calPerServing * scale,
                    protein = recipe.proteinPerServing * scale,
                    carbs = recipe.carbsPerServing * scale,
                    fat = recipe.fatPerServing * scale,
                    fiber = 0f,
                    sugar = 0f,
                    sodium = recipe.sodiumPerServing * scale,
                    potassium = 0f,
                    vitaminA = 0f,
                    vitaminC = 0f,
                    calcium = 0f,
                    iron = 0f
                )
            }

            val dish = LoggedDish(
                dishNameEn = recipe.nameEn,
                dishNamePh = recipe.namePh,
                weightGrams = weightGrams,
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
            _loggedDishes.update { current ->
                if (current.any { it.dishLabel == recipe.dishLabel }) current else current + dish
            }

            // Reset for next dish
            _selectedDish.value = null
            _manualWeightText.value = ""
            recomputeFilteredDishes()
        }.invokeOnCompletion {
            _isAddingDish.value = false
        }
    }

    fun removeDish(index: Int) {
        _loggedDishes.update { list -> list.filterIndexed { i, _ -> i != index } }
        recomputeFilteredDishes()
    }

    private fun isDishAlreadyLogged(dishLabel: String): Boolean =
        dishLabel.isNotBlank() && _loggedDishes.value.any { it.dishLabel == dishLabel }

    fun updateMealType(type: String) {
        _mealType.value = type
    }

    fun setShowSummary(show: Boolean) {
        _showSummary.value = show
    }

    /**
     * Quick-log shortcut from planned meals. The dish remains pending until
     * the user enters the actual consumed weight.
     */
    fun quickLogFromPlan(
        dishLabel: String,
        mealSlot: String,
        substitutionsJson: String = "",
        scaledServings: Int = 0,
        tweaksJson: String = ""
    ) {
        initializePlannedQuickLog(
            mealSlot = mealSlot,
            dishEntries = listOf(
                QuickLogDishEntry(
                    dishLabel = dishLabel,
                    substitutionsJson = substitutionsJson,
                    scaledServings = scaledServings,
                    tweaksJson = tweaksJson
                )
            )
        )
    }

    /**
     * Quick-log shortcut for an entire meal slot. Dishes remain pending until
     * each planned dish receives an actual consumed weight.
     */
    fun quickLogSlotFromPlan(mealSlot: String, dishEntries: List<QuickLogDishEntry>) {
        initializePlannedQuickLog(mealSlot = mealSlot, dishEntries = dishEntries)
    }

    private fun initializePlannedQuickLog(mealSlot: String, dishEntries: List<QuickLogDishEntry>) {
        _mealType.value = mealSlot
        _plannedQuickLogEntries.value = dishEntries
        _plannedWeightMethod.value = null
        _plannedWeightIndex.value = 0
        _isPlannedQuickLog.value = dishEntries.isNotEmpty()
        _loggedDishes.value = emptyList()
        _showSummary.value = false
        _selectedDish.value = null
        _manualWeightText.value = ""
        _plannedManualWeightText.value = ""
        resetPlannedScaleState()
        recomputeFilteredDishes()
        refreshCurrentPlannedRecipe()
    }

    private fun refreshCurrentPlannedRecipe() {
        val entry = _plannedQuickLogEntries.value.getOrNull(_plannedWeightIndex.value)
        if (entry == null) {
            _currentPlannedRecipe.value = null
            return
        }

        viewModelScope.launch {
            _currentPlannedRecipe.value = withContext(Dispatchers.IO) {
                dishRecipeDao.getByDishLabel(entry.dishLabel)
            }
        }
    }

    fun selectPlannedWeightMethod(method: PlannedWeightMethod) {
        _plannedWeightMethod.value = method
        _plannedManualWeightText.value = ""
        resetPlannedScaleState()
    }

    fun setCurrentPlannedManualWeight(text: String) {
        _plannedManualWeightText.value = text
    }

    fun updatePlannedScaleConnectionStatus(connected: Boolean) {
        if (!connected) {
            resetPlannedScaleState()
        }
    }

    fun updatePlannedScaleWeight(realWeight: Float) {
        _plannedScaleWeight.value = realWeight

        if (abs(realWeight - plannedStabilizationTargetWeight) > plannedWeightTolerance) {
            _plannedScaleWeightStable.value = false
            plannedStabilizationTargetWeight = realWeight

            plannedWeightStabilizationJob?.cancel()
            plannedWeightStabilizationJob = viewModelScope.launch {
                delay(1500)
                _plannedScaleWeight.value = plannedStabilizationTargetWeight
                _plannedScaleWeightStable.value = true
            }
        }
    }

    fun logCurrentPlannedDishWithManualWeight() {
        val weightGrams = _plannedManualWeightText.value.toFloatOrNull() ?: return
        if (weightGrams <= 0f) return
        logCurrentPlannedDish(weightGrams)
    }

    fun logCurrentPlannedDishWithScaleWeight(weightGrams: Float) {
        if (weightGrams <= 0f) return
        logCurrentPlannedDish(weightGrams)
    }

    private fun logCurrentPlannedDish(weightGrams: Float) {
        val entry = _plannedQuickLogEntries.value.getOrNull(_plannedWeightIndex.value) ?: return
        viewModelScope.launch {
            val loggedDish = withContext(Dispatchers.IO) {
                buildLoggedDishFromPlannedEntry(entry, weightGrams)
            }
            _loggedDishes.update { it + loggedDish }
            advancePlannedWeightStep()
        }
    }

    private suspend fun buildLoggedDishFromPlannedEntry(
        entry: QuickLogDishEntry,
        cookedWeightGrams: Float
    ): LoggedDish {
        val recipe = dishRecipeDao.getByDishLabel(entry.dishLabel)
        val substitutions = parseSubstitutionsJson(entry.substitutionsJson)
        val tweaks = parseTweaksJson(entry.tweaksJson)
        val substitutionsJson = substitutionsToJson(substitutions)
        val nutrients = if (recipe != null) {
            val calculated = if (tweaks.isNotEmpty()) {
                calculator.calculatePortionNutrition(entry.dishLabel, cookedWeightGrams, substitutions, tweaks)
            } else if (substitutions.isNotEmpty()) {
                calculator.calculatePortionNutrition(entry.dishLabel, cookedWeightGrams, substitutions)
            } else {
                calculator.calculatePortionNutrition(entry.dishLabel, cookedWeightGrams)
            }

            if ((substitutions.isNotEmpty() || tweaks.isNotEmpty()) && calculated == NutritionResult.ZERO) {
                calculator.calculatePortionNutrition(entry.dishLabel, cookedWeightGrams)
            } else {
                calculated
            }
        } else {
            NutritionResult.ZERO
        }

        val displayName = recipe?.namePh?.ifBlank { recipe.nameEn }
            ?: entry.dishLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
        val englishName = recipe?.nameEn?.ifBlank { displayName } ?: displayName
        val filipinoName = recipe?.namePh?.ifBlank { displayName } ?: displayName

        return LoggedDish(
            dishNameEn = englishName,
            dishNamePh = filipinoName,
            weightGrams = cookedWeightGrams,
            confidence = 1.0f,
            foodId = 0,
            dishLabel = recipe?.dishLabel ?: entry.dishLabel,
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
            tweaksJson = entry.tweaksJson
        )
    }

    private fun advancePlannedWeightStep() {
        val nextIndex = _plannedWeightIndex.value + 1
        _plannedManualWeightText.value = ""
        resetPlannedScaleState()

        if (nextIndex >= _plannedQuickLogEntries.value.size) {
            _plannedWeightIndex.value = nextIndex
            _currentPlannedRecipe.value = null
            _showSummary.value = true
        } else {
            _plannedWeightIndex.value = nextIndex
            refreshCurrentPlannedRecipe()
        }
    }

    private fun resetPlannedScaleState() {
        plannedWeightStabilizationJob?.cancel()
        plannedStabilizationTargetWeight = 0f
        _plannedScaleWeight.value = 0f
        _plannedScaleWeightStable.value = false
    }

    fun canConfirmCurrentPlannedQuickLog(): Boolean =
        !_isPlannedQuickLog.value ||
            canConfirmPlannedQuickLog(_plannedQuickLogEntries.value.size, _loggedDishes.value)


    // ── Ingredient Breakdown & Substitution (shared with AI flow) ──

    /** Returns per-ingredient nutrition breakdown for a dish. */
    suspend fun getIngredientBreakdown(
        dishLabel: String,
        substitutions: Map<String, String> = emptyMap()
    ): Map<String, com.calorieko.app.data.local.IngredientNutritionBreakdown> {
        return calculator.getIngredientBreakdown(
            dishLabel,
            RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        )
    }

    /** Returns substitution candidates for an ingredient. */
    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<com.calorieko.app.data.model.RawIngredientEntity> {
        return calculator.getSubstitutesForIngredient(ingredientKey)
    }

    /** Applies substitutions to a dish and recalculates its nutrition. */
    fun applySubstitutionToDish(dishIndex: Int, substitutions: Map<String, String>) {
        if (_isPlannedQuickLog.value) return
        val dish = _loggedDishes.value.getOrNull(dishIndex) ?: return
        val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        val tweaks = parseTweaksJson(dish.tweaksJson)
            .filterKeys { sanitizedSubstitutions[it] != REMOVED_INGREDIENT }
        applyCustomizationsToDish(dishIndex, sanitizedSubstitutions, tweaks)
    }

    fun removeIngredientFromDish(dishIndex: Int, ingredientKey: String) {
        if (_isPlannedQuickLog.value) return
        if (RecipeCustomizationRules.isProtectedBaseIngredient(ingredientKey)) return
        updateDishCustomizations(dishIndex) { current, tweaks ->
            current[ingredientKey] = REMOVED_INGREDIENT
            tweaks.remove(ingredientKey)
        }
    }

    fun removeSubstitutionFromDish(dishIndex: Int, ingredientKey: String) {
        if (_isPlannedQuickLog.value) return
        updateDishCustomizations(dishIndex) { current, _ ->
            current.remove(ingredientKey)
        }
    }

    fun applyIngredientTweakToDish(
        dishIndex: Int,
        ingredientKey: String,
        multiplier: Float
    ) {
        if (_isPlannedQuickLog.value) return
        updateDishCustomizations(dishIndex) { substitutions, tweaks ->
            if (substitutions[ingredientKey] != REMOVED_INGREDIENT) {
                if (!multiplier.isFinite() || multiplier <= 0f || multiplier == 1f) {
                    tweaks.remove(ingredientKey)
                } else {
                    tweaks[ingredientKey] = multiplier
                }
            }
        }
    }

    fun resetIngredientTweakFromDish(dishIndex: Int, ingredientKey: String) {
        applyIngredientTweakToDish(dishIndex, ingredientKey, 1f)
    }

    fun clearIngredientTweaksFromDish(dishIndex: Int) {
        if (_isPlannedQuickLog.value) return
        val dish = _loggedDishes.value.getOrNull(dishIndex) ?: return
        applyCustomizationsToDish(
            dishIndex = dishIndex,
            substitutions = parseSubstitutionsJson(dish.substitutionsJson),
            tweaks = emptyMap()
        )
    }

    private fun updateDishCustomizations(
        dishIndex: Int,
        transform: (MutableMap<String, String>, MutableMap<String, Float>) -> Unit
    ) {
        val dish = _loggedDishes.value.getOrNull(dishIndex) ?: return
        val substitutions = parseSubstitutionsJson(dish.substitutionsJson).toMutableMap()
        val tweaks = parseTweaksJson(dish.tweaksJson).toMutableMap()
        transform(substitutions, tweaks)
        val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        substitutions.clear()
        substitutions.putAll(sanitizedSubstitutions)
        tweaks.keys
            .filter { substitutions[it] == REMOVED_INGREDIENT }
            .forEach { tweaks.remove(it) }
        applyCustomizationsToDish(dishIndex, substitutions, tweaks)
    }

    private fun applyCustomizationsToDish(
        dishIndex: Int,
        substitutions: Map<String, String>,
        tweaks: Map<String, Float>
    ) {
        viewModelScope.launch {
            val dish = _loggedDishes.value.getOrNull(dishIndex) ?: return@launch
            if (dish.dishLabel.isBlank()) return@launch

            val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
            val normalizedTweaks = normalizeTweaks(tweaks)
                .filterKeys { sanitizedSubstitutions[it] != REMOVED_INGREDIENT }
            val newNutrients = withContext(Dispatchers.IO) {
                calculateLoggedDishNutrition(dish, sanitizedSubstitutions, normalizedTweaks)
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
                iron = newNutrients.iron,
                substitutionsJson = substitutionsToJson(sanitizedSubstitutions),
                tweaksJson = tweaksToJson(normalizedTweaks)
            )
            _loggedDishes.update { list ->
                list.toMutableList().also { current ->
                    if (dishIndex in current.indices) current[dishIndex] = updated
                }
            }
        }
    }

    private suspend fun calculateLoggedDishNutrition(
        dish: LoggedDish,
        substitutions: Map<String, String>,
        tweaks: Map<String, Float>
    ): NutritionResult {
        val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        val calculated = when {
            tweaks.isNotEmpty() -> calculator.calculatePortionNutrition(
                dish.dishLabel,
                dish.weightGrams,
                sanitizedSubstitutions,
                tweaks
            )
            sanitizedSubstitutions.isNotEmpty() -> calculator.calculatePortionNutrition(
                dish.dishLabel,
                dish.weightGrams,
                sanitizedSubstitutions
            )
            else -> calculator.calculatePortionNutrition(dish.dishLabel, dish.weightGrams)
        }

        return if ((sanitizedSubstitutions.isNotEmpty() || tweaks.isNotEmpty()) && calculated == NutritionResult.ZERO) {
            calculator.calculatePortionNutrition(dish.dishLabel, dish.weightGrams)
        } else {
            calculated
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
        if (_isPlannedQuickLog.value && !canConfirmCurrentPlannedQuickLog()) return

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
            val deductionItems = computePantryOverlap()
            if (deductionItems.isNotEmpty()) {
                _pantryDeductionItems.value = deductionItems
                _showPantryDeduction.value = true
            } else {
                _events.send(ManualLogEvent.MealConfirmed)
            }
        }
    }

    // --- Pantry Deduction ---

    private suspend fun computePantryOverlap(): List<PantryDeductionItem> {
        val pantryItems = withContext(Dispatchers.IO) { pantryDao.getAllItemsList() }.toSet()
        if (pantryItems.isEmpty()) return emptyList()

        val usedIngredients = linkedMapOf<String, Pair<String, MutableList<String>>>()

        for (dish in _loggedDishes.value) {
            if (dish.dishLabel.isBlank()) continue

            val substitutions = parseSubstitutionsJson(dish.substitutionsJson)
            val breakdown = withContext(Dispatchers.IO) {
                calculator.getIngredientBreakdown(dish.dishLabel, substitutions)
            }
            if (breakdown.isEmpty()) continue

            val dishDisplayName = dish.dishNamePh.ifBlank { dish.dishNameEn }
                .ifBlank { formatIngredientName(dish.dishLabel) }

            for ((_, info) in breakdown) {
                if (info.isRemoved) continue

                val effectiveKey = info.replacementIngredientKey ?: info.ingredientKey
                if (effectiveKey in pantryItems) {
                    val existing = usedIngredients.getOrPut(effectiveKey) {
                        info.displayName to mutableListOf()
                    }
                    existing.second.add(dishDisplayName)
                }
            }
        }

        return usedIngredients.map { (key, value) ->
            PantryDeductionItem(
                ingredientKey = key,
                displayName = value.first,
                usedInDishes = value.second.distinct()
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    fun confirmPantryDeduction(selectedKeys: Set<String>) {
        if (selectedKeys.isEmpty()) {
            skipPantryDeduction()
            return
        }

        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: ""
            withContext(Dispatchers.IO) {
                pantryRepository.removeIngredients(uid, selectedKeys.toList())
            }
            finishPantryDeduction()
            _events.send(ManualLogEvent.MealConfirmed)
        }
    }

    fun skipPantryDeduction() {
        viewModelScope.launch {
            finishPantryDeduction()
            _events.send(ManualLogEvent.MealConfirmed)
        }
    }

    private fun finishPantryDeduction() {
        _showPantryDeduction.value = false
        _pantryDeductionItems.value = emptyList()
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
            RecipeCustomizationRules.sanitizeSubstitutions(map)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseTweaksJson(json: String): Map<String, Float> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, Float>()
            obj.keys().forEach { key -> map[key] = obj.getDouble(key).toFloat() }
            normalizeTweaks(map)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun substitutionsToJson(substitutions: Map<String, String>): String {
        val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        if (sanitizedSubstitutions.isEmpty()) return ""
        return org.json.JSONObject(sanitizedSubstitutions as Map<*, *>).toString()
    }

    private fun tweaksToJson(tweaks: Map<String, Float>): String {
        val normalized = normalizeTweaks(tweaks)
        if (normalized.isEmpty()) return ""

        val obj = org.json.JSONObject()
        normalized.forEach { (key, value) ->
            obj.put(key, value.toDouble())
        }
        return obj.toString()
    }

    private fun normalizeTweaks(tweaks: Map<String, Float>): Map<String, Float> {
        return tweaks.filterValues { it.isFinite() && it > 0f && it != 1f }
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
            foodDao: FoodDao,
            auth: FirebaseAuth,
            mealRepository: MealRepository,
            calculator: RecipeNutritionCalculator,
            pantryDao: PantryDao,
            pantryRepository: PantryRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManualLogViewModel::class.java)) {
                    return ManualLogViewModel(
                        dishRecipeDao,
                        rawIngredientDao,
                        foodDao,
                        auth,
                        mealRepository,
                        calculator,
                        pantryDao,
                        pantryRepository,
                        appContext
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
