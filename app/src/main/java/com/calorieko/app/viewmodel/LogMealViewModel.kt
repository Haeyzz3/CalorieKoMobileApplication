package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.local.RecipeNutritionCalculator
import com.calorieko.app.data.local.IngredientNutritionBreakdown
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.LogMealPhase
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.repository.MealRepository
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
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


/** One-shot navigation/UI events emitted by LogMealViewModel. */
sealed interface LogMealEvent {
    data object MealConfirmed : LogMealEvent
}

class LogMealViewModel(
    private val foodDao: FoodDao,
    private val rawIngredientDao: RawIngredientDao,
    private val auth: FirebaseAuth,
    private val mealRepository: MealRepository,
    private val calculator: RecipeNutritionCalculator
) : ViewModel() {

    // ── UI States ──

    private val _phase = MutableStateFlow(LogMealPhase.MODE_SELECTION)
    val phase: StateFlow<LogMealPhase> = _phase.asStateFlow()

    private val _weight = MutableStateFlow(0f)
    val weight: StateFlow<Float> = _weight.asStateFlow()

    private val _weightStable = MutableStateFlow(false)
    val weightStable: StateFlow<Boolean> = _weightStable.asStateFlow()

    private val _latestResults = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val latestResults: StateFlow<List<Pair<String, Float>>> = _latestResults.asStateFlow()

    private val _topLabel = MutableStateFlow("")
    val topLabel: StateFlow<String> = _topLabel.asStateFlow()

    private val _topConfidence = MutableStateFlow(0f)
    val topConfidence: StateFlow<Float> = _topConfidence.asStateFlow()

    private val _currentDetectedFood = MutableStateFlow<FoodItem?>(null)
    val currentDetectedFood: StateFlow<FoodItem?> = _currentDetectedFood.asStateFlow()

    private val _showUnsupportedBanner = MutableStateFlow(false)
    val showUnsupportedBanner: StateFlow<Boolean> = _showUnsupportedBanner.asStateFlow()

    private val _showLogFailedBanner = MutableStateFlow(false)
    val showLogFailedBanner: StateFlow<Boolean> = _showLogFailedBanner.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _isScaleConnected = MutableStateFlow(false)
    val isScaleConnected: StateFlow<Boolean> = _isScaleConnected.asStateFlow()

    private val _showCandidateSelection = MutableStateFlow(false)
    val showCandidateSelection: StateFlow<Boolean> = _showCandidateSelection.asStateFlow()

    private val _candidate1 = MutableStateFlow<Pair<FoodItem, Float>?>(null)
    val candidate1: StateFlow<Pair<FoodItem, Float>?> = _candidate1.asStateFlow()

    private val _candidate2 = MutableStateFlow<Pair<FoodItem, Float>?>(null)
    val candidate2: StateFlow<Pair<FoodItem, Float>?> = _candidate2.asStateFlow()

    private val _pendingFoodId = MutableStateFlow(0)

    private val _pendingDishName = MutableStateFlow("")
    val pendingDishName: StateFlow<String> = _pendingDishName.asStateFlow()

    private val _pendingDishNameEn = MutableStateFlow("")

    private val _pendingConfidence = MutableStateFlow(0f)
    val pendingConfidence: StateFlow<Float> = _pendingConfidence.asStateFlow()

    private val _pendingCaloriesEst = MutableStateFlow(0f)
    val pendingCaloriesEst: StateFlow<Float> = _pendingCaloriesEst.asStateFlow()

    private val _loggedDishes = MutableStateFlow<List<LoggedDish>>(emptyList())
    val loggedDishes: StateFlow<List<LoggedDish>> = _loggedDishes.asStateFlow()

    private val _mealType = MutableStateFlow(getDefaultMealType())
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    private var weightStabilizationJob: Job? = null
    private var stabilizationTargetWeight = 0f
    private val WEIGHT_TOLERANCE = 3.0f // Allow +/- 3 grams of noise


    // ── Confirm guard (prevents duplicate submissions) ──

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    // ── One-shot events ──

    private val _events = Channel<LogMealEvent>(Channel.BUFFERED)
    val events: Flow<LogMealEvent> = _events.receiveAsFlow()

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

    fun updateRealWeight(realWeight: Float) {
        _weight.value = realWeight

        // If the new weight jumps outside our tolerance band, reset the timer
        if (abs(realWeight - stabilizationTargetWeight) > WEIGHT_TOLERANCE) {
            _weightStable.value = false
            stabilizationTargetWeight = realWeight // set the new target

            weightStabilizationJob?.cancel()
            weightStabilizationJob = viewModelScope.launch {
                delay(1500) // 1.5 seconds of staying within the tolerance band

                // Once stable, snap the display weight to the target
                // to stop the UI from jittering while they wait to capture.
                _weight.value = stabilizationTargetWeight
                _weightStable.value = true
            }
        }
    }

    fun updateScaleConnectionStatus(connected: Boolean) {
        _isScaleConnected.value = connected
        if (!connected) {
            _weight.value = 0f
            _weightStable.value = false
            stabilizationTargetWeight = 0f
            weightStabilizationJob?.cancel()
        }
    }

    fun onFrameAnalyzed(results: List<Pair<String, Float>>) {
        if (results.isNotEmpty() && _phase.value == LogMealPhase.SCANNING && !_isProcessing.value && !_showCandidateSelection.value) {
            _latestResults.value = results
            val newTopLabel = results[0].first
            _topLabel.value = newTopLabel
            _topConfidence.value = results[0].second

            viewModelScope.launch {
                val food = if (newTopLabel.isNotEmpty() && newTopLabel != "negative") {
                    withContext(Dispatchers.IO) { foodDao.getFoodByMlLabel(newTopLabel) }
                } else {
                    null
                }
                _currentDetectedFood.value = food
            }
        }
    }

    fun hideUnsupportedBanner() {
        _showUnsupportedBanner.value = false
    }

    fun processCapture() {
        val results = _latestResults.value
        val currentWeight = _weight.value
        if (results.isEmpty() || !_weightStable.value || currentWeight <= 0f) return

        val top1 = results.getOrNull(0)
        val top2 = results.getOrNull(1)

        if (top1 != null && top1.first == "negative" && top1.second >= CONFIDENCE_THRESHOLD) {
            triggerUnsupportedBanner()
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val food1 = top1?.let { withContext(Dispatchers.IO) { foodDao.getFoodByMlLabel(it.first) } }

            if (food1 == null) {
                triggerUnsupportedBanner()
                _isProcessing.value = false
                return@launch
            }

            // Use the new calculator for the calorie estimate
            val calEst = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(food1.mlLabel, currentWeight).calories
            }

            if (top1.second >= 0.80f) {
                setDishReady(food1.foodId, food1.nameEn, top1.second, calEst)
            } else if (top2 != null && (top1.second - top2.second) <= 0.30f && top1.second > 0.10f) {
                val food2 = withContext(Dispatchers.IO) { foodDao.getFoodByMlLabel(top2.first) }
                if (food2 != null) {
                    _candidate1.value = Pair(food1, top1.second)
                    _candidate2.value = Pair(food2, top2.second)
                    _showCandidateSelection.value = true
                } else {
                    setDishReady(food1.foodId, food1.namePh, food1.nameEn, top1.second, calEst)
                }
            } else {
                triggerUnsupportedBanner()
            }
            _isProcessing.value = false
        }
    }

    private fun triggerUnsupportedBanner() {
        _showUnsupportedBanner.value = true
        viewModelScope.launch {
            delay(5000)
            _showUnsupportedBanner.value = false
        }
    }

    private fun setDishReady(foodId: Int, namePh: String, nameEn: String, confidence: Float, calEst: Float) {
        _pendingFoodId.value = foodId
        _pendingDishName.value = namePh
        _pendingDishNameEn.value = nameEn
        _pendingConfidence.value = confidence
        _pendingCaloriesEst.value = calEst
        _phase.value = LogMealPhase.DISH_READY
    }

    fun onCandidateSelected(food: FoodItem, confidence: Float) {
        viewModelScope.launch {
            val calEst = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(food.mlLabel, _weight.value).calories
            }
            setDishReady(food.foodId, food.namePh, food.nameEn, confidence, calEst)
            _showCandidateSelection.value = false
        }
    }

    fun cancelCandidateSelection() {
        _showCandidateSelection.value = false
        resetScanningVariables()
    }

    fun logCurrentDish() {
        val foodId = _pendingFoodId.value
        val dishName = _pendingDishName.value
        val confidence = _pendingConfidence.value
        val currentWeight = _weight.value

        viewModelScope.launch {
            val food = withContext(Dispatchers.IO) { foodDao.getFoodById(foodId) }
            if (food != null) {
                val w = currentWeight
                // Use the new calculator to compute all nutrients at once
                val nutrients = withContext(Dispatchers.IO) {
                    calculator.calculatePortionNutrition(food.mlLabel, w)
                }
                val dish = LoggedDish(
                    dishNameEn = _pendingDishNameEn.value,
                    dishNamePh = dishName,
                    weightGrams = w,
                    confidence = confidence,
                    foodId = food.foodId,
                    dishLabel = food.mlLabel,
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
            } else {
                triggerLogFailedBanner()
            }
            resetScanningVariables()
        }
    }

    fun cancelDishReady() {
        resetScanningVariables()
    }

    private fun resetScanningVariables() {
        _latestResults.value = emptyList()
        _topLabel.value = ""
        _topConfidence.value = 0f
        _pendingFoodId.value = 0
        _candidate1.value = null
        _candidate2.value = null
        _showCandidateSelection.value = false
        _phase.value = LogMealPhase.SCANNING
    }

    fun hideLogFailedBanner() {
        _showLogFailedBanner.value = false
    }

    private fun triggerLogFailedBanner() {
        _showLogFailedBanner.value = true
        viewModelScope.launch {
            delay(5000)
            _showLogFailedBanner.value = false
        }
    }

    fun setPhase(newPhase: LogMealPhase) {
        _phase.value = newPhase
    }

    fun removeDish(index: Int) {
        _loggedDishes.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun updateMealType(type: String) {
        _mealType.value = type
    }

    fun confirmMeal() {
        // Guard: prevent duplicate submissions from rapid taps
        if (_isConfirming.value) return
        _isConfirming.value = true

        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            viewModelScope.launch { _events.send(LogMealEvent.MealConfirmed) }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mealRepository.saveMeal(uid, _mealType.value, _loggedDishes.value)
            }
            _events.send(LogMealEvent.MealConfirmed)
        }
    }

    // ── Ingredient Breakdown & Substitution ──

    /** Resolves a foodId to its ML label for calculator lookups. */
    suspend fun getMlLabelForDish(foodId: Int): String? {
        return withContext(Dispatchers.IO) {
            foodDao.getFoodById(foodId)?.mlLabel
        }
    }

    /** Returns per-ingredient nutrition breakdown for a dish. */
    suspend fun getIngredientBreakdown(dishLabel: String): Map<String, IngredientNutritionBreakdown> {
        return calculator.getIngredientBreakdown(dishLabel)
    }

    /** Returns same-subcategory substitution candidates for an ingredient. */
    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<RawIngredientEntity> {
        return calculator.getSubstitutesForIngredient(ingredientKey)
    }

    /**
     * Applies an ingredient substitution to a specific logged dish and
     * recalculates its nutrition values.
     * @param dishIndex index in the logged dishes list
     * @param substitutions map of originalIngredientKey → newIngredientKey
     */
    fun applySubstitutionToDish(dishIndex: Int, substitutions: Map<String, String>) {
        viewModelScope.launch {
            val dish = _loggedDishes.value.getOrNull(dishIndex) ?: return@launch
            if (dish.dishLabel.isEmpty()) return@launch
            val nutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(dish.dishLabel, dish.weightGrams, substitutions)
            }
            _loggedDishes.update { list ->
                list.toMutableList().also {
                    it[dishIndex] = dish.copy(
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
                }
            }
        }
    }

    /** Formats an ingredient_key into a readable display name. */
    fun formatIngredientName(key: String): String {
        return key.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.70f

        fun provideFactory(
            foodDao: FoodDao,
            rawIngredientDao: RawIngredientDao,
            auth: FirebaseAuth,
            mealRepository: MealRepository,
            calculator: RecipeNutritionCalculator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogMealViewModel::class.java)) {
                    return LogMealViewModel(foodDao, rawIngredientDao, auth, mealRepository, calculator) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
