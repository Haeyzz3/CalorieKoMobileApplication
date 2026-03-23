package com.calorieko.app.data.repository

import android.content.Context
import com.calorieko.app.data.model.UserProfile
import java.io.BufferedReader
import java.io.InputStreamReader

data class NutritionalTarget(
    val targetCalories: Int,
    val targetProtein: Int,
    val targetCarbs: Int,
    val targetFats: Int,
    val targetSodium: Int,
    val targetFiber: Int,
    val targetSugar: Int,
    val targetSaturatedFat: Int,
    val targetPolyunsaturatedFat: Int,
    val targetMonounsaturatedFat: Int,
    val targetTransFat: Int,
    val targetCholesterol: Int,
    val targetPotassium: Int,
    val targetVitaminA: Int,
    val targetVitaminC: Int,
    val targetCalcium: Int,
    val targetIron: Int
)

class NutritionalValuesRepository(private val context: Context) {

    fun getTargetsForUser(profile: UserProfile): NutritionalTarget {
        return getTargets(
            age = profile.age,
            sex = profile.sex,
            heightCm = profile.height,
            weightKg = profile.weight,
            activityLevel = profile.activityLevel,
            goal = profile.goal
        )
    }

    fun getTargets(
        age: Int, 
        sex: String, 
        heightCm: Double, 
        weightKg: Double, 
        activityLevel: String, 
        goal: String
    ): NutritionalTarget {
        var rowFiber = 25.0
        var rowSodium = 2000.0
        var rowPotassium = 3510.0
        var rowVitaminA = 700.0
        var rowVitaminC = 1000.0
        var rowCalcium = 1000.0
        var rowIron = 45.0

        // Parse CSV exclusively for micronutrients based on Age and Sex
        try {
            val inputStream = context.assets.open("nutrient_daily_values.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var isFirstLine = true

            // Read line by line
            for (line in reader.lineSequence()) {
                if (isFirstLine) {
                    isFirstLine = false
                    continue
                }
                if (line.isBlank()) continue

                val tokens = line.split(",")
                if (tokens.size >= 21) {
                    val ageMin = tokens[0].toIntOrNull() ?: 0
                    val ageMax = tokens[1].toIntOrNull() ?: 999
                    val gender = tokens[2].trim()

                    if (age in ageMin..ageMax && gender.equals(sex, ignoreCase = true)) {
                        rowFiber = tokens[8].toDoubleOrNull() ?: rowFiber
                        rowSodium = tokens[15].toDoubleOrNull() ?: rowSodium
                        rowPotassium = tokens[16].toDoubleOrNull() ?: rowPotassium
                        rowVitaminA = tokens[17].toDoubleOrNull() ?: rowVitaminA
                        rowVitaminC = tokens[18].toDoubleOrNull() ?: rowVitaminC
                        rowCalcium = tokens[19].toDoubleOrNull() ?: rowCalcium
                        rowIron = tokens[20].toDoubleOrNull() ?: rowIron
                        break
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallbacks act as safety nets if CSV breaks
        }

        // --- 1. Mifflin-St Jeor Logic ---
        val bmr = if (sex.equals("Male", ignoreCase = true)) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
        }

        // --- 2. Activity Level Multiplier ---
        val activityMultiplier = when (activityLevel.lowercase().trim()) {
            "not_very_active" -> 1.2
            "lightly_active" -> 1.375
            "active" -> 1.55
            "very_active" -> 1.725
            else -> 1.2 // defaults conservative
        }

        val tdee = bmr * activityMultiplier

        // --- 3. Health Goals (Calorie shift & Macro Splits) ---
        var finalCalories = tdee.toInt()
        var proteinPct = 0.30
        var carbsPct = 0.40
        var fatsPct = 0.30
        
        when (goal.lowercase().trim()) {
            "lose_weight", "weight_loss", "weight", "weight control" -> {
                finalCalories = (tdee - 500).toInt().coerceAtLeast(1200) // 1200 floor safety
                proteinPct = 0.35 // Higher protein to preserve muscle mass
                carbsPct = 0.35
                fatsPct = 0.30
            }
            "gain_muscle" -> {
                finalCalories = (tdee + 300).toInt()
                proteinPct = 0.30 
                carbsPct = 0.45 // Higher carbs to fuel muscle synthesis routines
                fatsPct = 0.25
            }
            else -> {
                // "General Health" or "Maintain"
                finalCalories = tdee.toInt()
                proteinPct = 0.30
                carbsPct = 0.40
                fatsPct = 0.30
            }
        }

        // --- 4. Sub-Macronutrient Dynamic Parsing ---
        val targetProtein = ((finalCalories * proteinPct) / 4).toInt()
        val targetCarbs = ((finalCalories * carbsPct) / 4).toInt()
        val targetFats = ((finalCalories * fatsPct) / 9).toInt()

        val targetSugar = ((finalCalories * 0.10) / 4).toInt()
        val targetSatFat = ((finalCalories * 0.10) / 9).toInt()
        val targetTransFat = ((finalCalories * 0.01) / 9).toInt()
        
        val remainderFat = (targetFats - targetSatFat - targetTransFat).coerceAtLeast(0)
        val targetPolyFat = remainderFat / 2
        val targetMonoFat = remainderFat - targetPolyFat

        // --- 5. Return Complete Formatted Target ---
        return NutritionalTarget(
            targetCalories = finalCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFats = targetFats,
            targetSodium = rowSodium.toInt(), // Strictly unscaled (2000mg limit typically)
            targetFiber = rowFiber.toInt(),
            targetSugar = targetSugar,
            targetSaturatedFat = targetSatFat,
            targetPolyunsaturatedFat = targetPolyFat,
            targetMonounsaturatedFat = targetMonoFat,
            targetTransFat = targetTransFat,
            targetCholesterol = 300,
            targetPotassium = rowPotassium.toInt(),
            targetVitaminA = rowVitaminA.toInt(),
            targetVitaminC = rowVitaminC.toInt(),
            targetCalcium = rowCalcium.toInt(),
            targetIron = rowIron.toInt()
        )
    }
}
