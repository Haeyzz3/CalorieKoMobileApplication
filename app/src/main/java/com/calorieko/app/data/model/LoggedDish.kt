package com.calorieko.app.data.model

/** A dish that has been recognized and queued for logging. */
data class LoggedDish(
    val dishNameEn: String,
    val dishNamePh: String = "",
    val weightGrams: Float,
    val confidence: Float,
    val foodId: Int,
    val dishLabel: String = "",  // ML/recipe key for ingredient lookups
    // Pre-computed nutrients
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val saturatedFat: Float,
    val polyunsaturatedFat: Float,
    val monounsaturatedFat: Float,
    val transFat: Float,
    val cholesterol: Float,
    val sodium: Float,
    val potassium: Float,
    val vitaminA: Float,
    val vitaminC: Float,
    val calcium: Float,
    val iron: Float
)
