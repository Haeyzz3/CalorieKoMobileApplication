package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.FoodItem
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.ui.screens.LogMealPhase
import com.calorieko.app.ui.screens.LoggedDish
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class LogMealViewModel(
    private val foodDao: FoodDao,
    private val mealLogDao: MealLogDao,
    private val mealLogItemDao: MealLogItemDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val CONFIDENCE_THRESHOLD = 0.70f

    // ── UI States ──

    private val _phase = MutableStateFlow(LogMealPhase.SCANNING)
    val phase: StateFlow<LogMealPhase> = _phase.asStateFlow()

    private val _weight = MutableStateFlow(0)
    val weight: StateFlow<Int> = _weight.asStateFlow()

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

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _showCandidateSelection = MutableStateFlow(false)
    val showCandidateSelection: StateFlow<Boolean> = _showCandidateSelection.asStateFlow()

    private val _candidate1 = MutableStateFlow<Pair<FoodItem, Float>?>(null)
    val candidate1: StateFlow<Pair<FoodItem, Float>?> = _candidate1.asStateFlow()

    private val _candidate2 = MutableStateFlow<Pair<FoodItem, Float>?>(null)
    val candidate2: StateFlow<Pair<FoodItem, Float>?> = _candidate2.asStateFlow()

    private val _pendingDishName = MutableStateFlow("")
    val pendingDishName: StateFlow<String> = _pendingDishName.asStateFlow()

    private val _pendingConfidence = MutableStateFlow(0f)
    val pendingConfidence: StateFlow<Float> = _pendingConfidence.asStateFlow()

    private val _pendingCaloriesEst = MutableStateFlow(0f)
    val pendingCaloriesEst: StateFlow<Float> = _pendingCaloriesEst.asStateFlow()

    private val _loggedDishes = MutableStateFlow<List<LoggedDish>>(emptyList())
    val loggedDishes: StateFlow<List<LoggedDish>> = _loggedDishes.asStateFlow()

    private val _mealType = MutableStateFlow(getDefaultMealType())
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    private var weightStabilizationJob: Job? = null

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

    fun updateRealWeight(realWeight: Int) {
        _weight.value = realWeight
        _weightStable.value = false
        weightStabilizationJob?.cancel()
        weightStabilizationJob = viewModelScope.launch {
            delay(1000)
            if (_weight.value == realWeight) {
                _weightStable.value = true
            }
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
        if (results.isEmpty() || !_weightStable.value || currentWeight <= 0) return

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

            if (top1.second >= 0.80f) {
                setDishReady(food1.nameEn, top1.second, food1.caloriesPer100g * currentWeight / 100f)
            } else if (top2 != null && (top1.second - top2.second) <= 0.30f && top1.second > 0.10f) {
                val food2 = withContext(Dispatchers.IO) { foodDao.getFoodByMlLabel(top2.first) }
                if (food2 != null) {
                    _candidate1.value = Pair(food1, top1.second)
                    _candidate2.value = Pair(food2, top2.second)
                    _showCandidateSelection.value = true
                } else {
                    setDishReady(food1.nameEn, top1.second, food1.caloriesPer100g * currentWeight / 100f)
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

    private fun setDishReady(name: String, confidence: Float, calEst: Float) {
        _pendingDishName.value = name
        _pendingConfidence.value = confidence
        _pendingCaloriesEst.value = calEst
        _phase.value = LogMealPhase.DISH_READY
    }

    fun onCandidateSelected(food: FoodItem, confidence: Float) {
        setDishReady(food.nameEn, confidence, food.caloriesPer100g * _weight.value / 100f)
        _showCandidateSelection.value = false
    }

    fun cancelCandidateSelection() {
        _showCandidateSelection.value = false
        resetScanningVariables()
    }

    fun logCurrentDish() {
        val dishName = _pendingDishName.value
        val confidence = _pendingConfidence.value
        val currentWeight = _weight.value

        viewModelScope.launch {
            val food = withContext(Dispatchers.IO) { foodDao.getFoodByName(dishName) }
            if (food != null) {
                val w = currentWeight.toFloat()
                val dish = LoggedDish(
                    dishNameEn = dishName,
                    weightGrams = w,
                    confidence = confidence,
                    foodId = food.foodId,
                    calories = food.caloriesPer100g * w / 100f,
                    protein = food.proteinPer100g * w / 100f,
                    carbs = food.carbsPer100g * w / 100f,
                    fat = food.fatPer100g * w / 100f,
                    fiber = food.fiberPer100g * w / 100f,
                    sugar = food.sugarPer100g * w / 100f,
                    saturatedFat = food.saturatedFatPer100g * w / 100f,
                    polyunsaturatedFat = food.polyunsaturatedFatPer100g * w / 100f,
                    monounsaturatedFat = food.monounsaturatedFatPer100g * w / 100f,
                    transFat = food.transFatPer100g * w / 100f,
                    cholesterol = food.cholesterolPer100g * w / 100f,
                    sodium = food.sodiumPer100g * w / 100f,
                    potassium = food.potassiumPer100g * w / 100f,
                    vitaminA = food.vitaminAPer100g * w / 100f,
                    vitaminC = food.vitaminCPer100g * w / 100f,
                    calcium = food.calciumPer100g * w / 100f,
                    iron = food.ironPer100g * w / 100f
                )
                _loggedDishes.update { it + dish }
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
        _candidate1.value = null
        _candidate2.value = null
        _showCandidateSelection.value = false
        _phase.value = LogMealPhase.SCANNING
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

    fun confirmMeal(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            onComplete()
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                persistMeal(uid, _mealType.value, _loggedDishes.value)
            }
            onComplete()
        }
    }

    private suspend fun persistMeal(uid: String, mealType: String, dishes: List<LoggedDish>) {
        val now = System.currentTimeMillis()

        val mealLogId = mealLogDao.insertMealLog(
            MealLogEntity(uid = uid, mealType = mealType, timestamp = now)
        )

        val items = dishes.map { d ->
            MealLogItemEntity(
                mealLogId = mealLogId,
                foodId = d.foodId,
                dishName = d.dishNameEn,
                weightGrams = d.weightGrams,
                calories = d.calories,
                protein = d.protein,
                carbs = d.carbs,
                fiber = d.fiber,
                sugar = d.sugar,
                fat = d.fat,
                saturatedFat = d.saturatedFat,
                polyunsaturatedFat = d.polyunsaturatedFat,
                monounsaturatedFat = d.monounsaturatedFat,
                transFat = d.transFat,
                cholesterol = d.cholesterol,
                sodium = d.sodium,
                potassium = d.potassium,
                vitaminA = d.vitaminA,
                vitaminC = d.vitaminC,
                calcium = d.calcium,
                iron = d.iron
            )
        }
        mealLogItemDao.insertItems(items)

        val today = LocalDate.now().toEpochDay()
        val existing = dailyNutritionSummaryDao.getSummaryForDate(uid, today)
        val mealCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()

        val updated = (existing ?: DailyNutritionSummaryEntity(uid = uid, dateEpochDay = today)).let { s ->
            s.copy(
                id = s.id,
                totalCalories = s.totalCalories + mealCalories,
                totalProtein = s.totalProtein + dishes.sumOf { it.protein.toDouble() }.toFloat(),
                totalCarbs = s.totalCarbs + dishes.sumOf { it.carbs.toDouble() }.toFloat(),
                totalFiber = s.totalFiber + dishes.sumOf { it.fiber.toDouble() }.toFloat(),
                totalSugar = s.totalSugar + dishes.sumOf { it.sugar.toDouble() }.toFloat(),
                totalFat = s.totalFat + dishes.sumOf { it.fat.toDouble() }.toFloat(),
                totalSaturatedFat = s.totalSaturatedFat + dishes.sumOf { it.saturatedFat.toDouble() }.toFloat(),
                totalPolyunsaturatedFat = s.totalPolyunsaturatedFat + dishes.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat(),
                totalMonounsaturatedFat = s.totalMonounsaturatedFat + dishes.sumOf { it.monounsaturatedFat.toDouble() }.toFloat(),
                totalTransFat = s.totalTransFat + dishes.sumOf { it.transFat.toDouble() }.toFloat(),
                totalCholesterol = s.totalCholesterol + dishes.sumOf { it.cholesterol.toDouble() }.toFloat(),
                totalSodium = s.totalSodium + dishes.sumOf { it.sodium.toDouble() }.toFloat(),
                totalPotassium = s.totalPotassium + dishes.sumOf { it.potassium.toDouble() }.toFloat(),
                totalVitaminA = s.totalVitaminA + dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
                totalVitaminC = s.totalVitaminC + dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
                totalCalcium = s.totalCalcium + dishes.sumOf { it.calcium.toDouble() }.toFloat(),
                totalIron = s.totalIron + dishes.sumOf { it.iron.toDouble() }.toFloat(),
                breakfastCalories = s.breakfastCalories + if (mealType == "Breakfast") mealCalories else 0f,
                lunchCalories = s.lunchCalories + if (mealType == "Lunch") mealCalories else 0f,
                dinnerCalories = s.dinnerCalories + if (mealType == "Dinner") mealCalories else 0f,
                snacksCalories = s.snacksCalories + if (mealType == "Snacks") mealCalories else 0f
            )
        }
        dailyNutritionSummaryDao.upsertSummary(updated)
    }

    companion object {
        fun provideFactory(
            foodDao: FoodDao,
            mealLogDao: MealLogDao,
            mealLogItemDao: MealLogItemDao,
            dailyNutritionSummaryDao: DailyNutritionSummaryDao,
            auth: FirebaseAuth
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LogMealViewModel::class.java)) {
                    return LogMealViewModel(foodDao, mealLogDao, mealLogItemDao, dailyNutritionSummaryDao, auth) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
