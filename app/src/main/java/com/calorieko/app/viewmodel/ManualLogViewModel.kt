package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.model.FoodItem
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

class ManualLogViewModel(
    private val foodDao: FoodDao,
    private val auth: FirebaseAuth,
    private val mealRepository: MealRepository
) : ViewModel() {

    // ── Dish catalogue ──

    private val _allDishes = MutableStateFlow<List<FoodItem>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredDishes = MutableStateFlow<List<FoodItem>>(emptyList())
    val filteredDishes: StateFlow<List<FoodItem>> = _filteredDishes.asStateFlow()

    // ── Selection state ──

    private val _selectedDish = MutableStateFlow<FoodItem?>(null)
    val selectedDish: StateFlow<FoodItem?> = _selectedDish.asStateFlow()

    private val _manualWeightText = MutableStateFlow("")
    val manualWeightText: StateFlow<String> = _manualWeightText.asStateFlow()

    // ── Logged dishes ──

    private val _loggedDishes = MutableStateFlow<List<LoggedDish>>(emptyList())
    val loggedDishes: StateFlow<List<LoggedDish>> = _loggedDishes.asStateFlow()

    private val _mealType = MutableStateFlow(getDefaultMealType())
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    private val _showSummary = MutableStateFlow(false)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()

    // ── One-shot events ──

    private val _events = Channel<ManualLogEvent>(Channel.BUFFERED)
    val events: Flow<ManualLogEvent> = _events.receiveAsFlow()

    // ── Init ──

    init {
        viewModelScope.launch {
            val foods = withContext(Dispatchers.IO) { foodDao.getAllFoods() }
            _allDishes.value = foods
            _filteredDishes.value = foods
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

    fun selectDish(dish: FoodItem) {
        _selectedDish.value = dish
        _manualWeightText.value = ""
    }

    fun clearSelectedDish() {
        _selectedDish.value = null
        _manualWeightText.value = ""
    }

    fun updateWeightText(text: String) {
        _manualWeightText.value = text
    }

    /**
     * Computes all 17 nutrients from the selected dish's per-100g values
     * scaled by the manually entered weight, then appends to loggedDishes.
     * Resets selection state for multi-dish entry.
     */
    fun addDish() {
        val food = _selectedDish.value ?: return
        val w = _manualWeightText.value.toFloatOrNull() ?: return
        if (w <= 0f) return

        val dish = LoggedDish(
            dishNameEn = food.nameEn,
            weightGrams = w,
            confidence = 1.0f, // Manual entry = 100% confidence
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

        // Reset for next dish
        _selectedDish.value = null
        _manualWeightText.value = ""
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

    fun confirmMeal() {
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

    companion object {
        fun provideFactory(
            foodDao: FoodDao,
            auth: FirebaseAuth,
            mealRepository: MealRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManualLogViewModel::class.java)) {
                    return ManualLogViewModel(foodDao, auth, mealRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
