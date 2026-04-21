package com.calorieko.app.data.model

/**
 * Lightweight container for 12 nutrient values.
 *
 * Used as the return type for all [RecipeNutritionCalculator][com.calorieko.app.data.local.RecipeNutritionCalculator]
 * methods. Values are in their standard units:
 *
 * - calories: kcal
 * - protein, carbs, fat, fiber, sugar: grams (g)
 * - sodium, potassium, calcium: milligrams (mg)
 * - vitaminA: micrograms (µg RAE)
 * - vitaminC: milligrams (mg)
 * - iron: milligrams (mg)
 */
data class NutritionResult(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    val vitaminA: Float = 0f,
    val vitaminC: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
) {
    /** Returns a new result with all values scaled by the given [factor]. */
    operator fun times(factor: Float): NutritionResult = NutritionResult(
        calories = calories * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
        fiber = fiber * factor,
        sugar = sugar * factor,
        sodium = sodium * factor,
        potassium = potassium * factor,
        vitaminA = vitaminA * factor,
        vitaminC = vitaminC * factor,
        calcium = calcium * factor,
        iron = iron * factor,
    )

    /** Returns a new result adding another result's values. */
    operator fun plus(other: NutritionResult): NutritionResult = NutritionResult(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
        fiber = fiber + other.fiber,
        sugar = sugar + other.sugar,
        sodium = sodium + other.sodium,
        potassium = potassium + other.potassium,
        vitaminA = vitaminA + other.vitaminA,
        vitaminC = vitaminC + other.vitaminC,
        calcium = calcium + other.calcium,
        iron = iron + other.iron,
    )

    companion object {
        val ZERO = NutritionResult()
    }
}
