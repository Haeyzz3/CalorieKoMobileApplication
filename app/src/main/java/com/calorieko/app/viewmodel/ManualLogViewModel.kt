package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
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

class ManualLogViewModel(
    private val dishRecipeDao: DishRecipeDao,
    private val auth: FirebaseAuth,
    private val mealRepository: MealRepository,
    private val calculator: RecipeNutritionCalculator
) : ViewModel() {

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
     * Computes all 12 nutrients via [RecipeNutritionCalculator] for the
     * selected dish, scaled by the manually entered cooked weight.
     * Resets selection state for multi-dish entry.
     */
    fun addDish() {
        val recipe = _selectedDish.value ?: return
        val w = _manualWeightText.value.toFloatOrNull() ?: return
        if (w <= 0f) return

        viewModelScope.launch {
            val nutrients = withContext(Dispatchers.IO) {
                calculator.calculatePortionNutrition(recipe.dishLabel, w)
            }
            val dish = LoggedDish(
                dishNameEn = recipe.nameEn,
                weightGrams = w,
                confidence = 1.0f, // Manual entry = 100% confidence
                foodId = 0,        // No legacy food_id needed for new calculator path
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

    fun confirmMeal() {
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

    companion object {
        fun provideFactory(
            dishRecipeDao: DishRecipeDao,
            auth: FirebaseAuth,
            mealRepository: MealRepository,
            calculator: RecipeNutritionCalculator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ManualLogViewModel::class.java)) {
                    return ManualLogViewModel(dishRecipeDao, auth, mealRepository, calculator) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
