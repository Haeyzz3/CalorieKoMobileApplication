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
            weightKg = profile.weight,
            goal = profile.goal
        )
    }

    fun getTargets(age: Int, sex: String, weightKg: Double, goal: String): NutritionalTarget {
        var rowWeight = 60.5
        var rowCalories = 2530.0
        var rowProtein = 71.0
        var rowFiber = 25.0
        var rowSodium = 2000.0
        var rowPotassium = 3510.0
        var rowVitaminA = 700.0
        var rowVitaminC = 1000.0
        var rowCalcium = 1000.0
        var rowIron = 45.0

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
                        rowWeight = tokens[3].toDoubleOrNull() ?: rowWeight
                        rowCalories = tokens[4].toDoubleOrNull() ?: rowCalories
                        rowProtein = tokens[5].toDoubleOrNull() ?: rowProtein
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
            // In case of error, fallback values are already set based on a 19-29 male profile roughly
        }

        val scalingFactor = if (rowWeight > 0.0) weightKg / rowWeight else 1.0

        val scaledCalories = (rowCalories * scalingFactor).toInt()
        val finalCalories = when (goal.lowercase().trim()) {
            "lose_weight", "weight_loss", "weight" -> (scaledCalories - 500).coerceAtLeast(1200)
            "gain_muscle" -> scaledCalories + 300
            else -> scaledCalories
        }

        // Exact midpoints as approved
        val targetFats = ((finalCalories * 0.25) / 9).toInt()
        val targetCarbs = ((finalCalories * 0.60) / 4).toInt()
        val targetSugar = ((finalCalories * 0.10) / 4).toInt()
        
        val targetSatFat = ((finalCalories * 0.10) / 9).toInt()
        val targetTransFat = ((finalCalories * 0.01) / 9).toInt()
        val remainderFat = (targetFats - targetSatFat - targetTransFat).coerceAtLeast(0)
        val targetPolyFat = remainderFat / 2
        val targetMonoFat = remainderFat - targetPolyFat

        return NutritionalTarget(
            targetCalories = finalCalories,
            targetProtein = (rowProtein * scalingFactor).toInt(),
            targetCarbs = targetCarbs,
            targetFats = targetFats,
            targetSodium = (rowSodium * scalingFactor).toInt(),
            targetFiber = (rowFiber * scalingFactor).toInt(),
            targetSugar = targetSugar,
            targetSaturatedFat = targetSatFat,
            targetPolyunsaturatedFat = targetPolyFat,
            targetMonounsaturatedFat = targetMonoFat,
            targetTransFat = targetTransFat,
            targetCholesterol = 300,
            targetPotassium = (rowPotassium * scalingFactor).toInt(),
            targetVitaminA = (rowVitaminA * scalingFactor).toInt(),
            targetVitaminC = (rowVitaminC * scalingFactor).toInt(),
            targetCalcium = (rowCalcium * scalingFactor).toInt(),
            targetIron = (rowIron * scalingFactor).toInt()
        )
    }
}
