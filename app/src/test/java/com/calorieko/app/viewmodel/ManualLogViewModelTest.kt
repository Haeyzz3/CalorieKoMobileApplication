package com.calorieko.app.viewmodel

import com.calorieko.app.data.model.LoggedDish
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLogViewModelTest {

    @Test
    fun canConfirmPlannedQuickLog_requiresEveryPlannedDishToHavePositiveWeight() {
        val dishes = listOf(
            loggedDish(weightGrams = 120f),
            loggedDish(weightGrams = 0f)
        )

        assertFalse(canConfirmPlannedQuickLog(requiredCount = 2, dishes = dishes))
    }

    @Test
    fun canConfirmPlannedQuickLog_rejectsMissingPlannedDish() {
        val dishes = listOf(loggedDish(weightGrams = 120f))

        assertFalse(canConfirmPlannedQuickLog(requiredCount = 2, dishes = dishes))
    }

    @Test
    fun canConfirmPlannedQuickLog_allowsCompletePositiveWeights() {
        val dishes = listOf(
            loggedDish(weightGrams = 120f),
            loggedDish(weightGrams = 85f)
        )

        assertTrue(canConfirmPlannedQuickLog(requiredCount = 2, dishes = dishes))
    }

    private fun loggedDish(weightGrams: Float): LoggedDish =
        LoggedDish(
            dishNameEn = "Test Dish",
            dishNamePh = "Test Dish",
            weightGrams = weightGrams,
            confidence = 1.0f,
            foodId = 0,
            dishLabel = "test_dish",
            calories = 0f,
            protein = 0f,
            carbs = 0f,
            fat = 0f,
            fiber = 0f,
            sugar = 0f,
            saturatedFat = 0f,
            polyunsaturatedFat = 0f,
            monounsaturatedFat = 0f,
            transFat = 0f,
            cholesterol = 0f,
            sodium = 0f,
            potassium = 0f,
            vitaminA = 0f,
            vitaminC = 0f,
            calcium = 0f,
            iron = 0f
        )
}
