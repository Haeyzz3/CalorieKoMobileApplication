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

        // ═══════════════════════════════════════════════════════════════
        // NDAP + Broca-Tanhauser Method
        // Standard protocol used by Filipino Registered Nutritionist-
        // Dietitians (NDAP — National Dietetic Association of the Philippines).
        //
        // Step 1: Compute Desirable Body Weight (DBW) via modified Broca
        //         formula adjusted for smaller Asian frames (10% deduction).
        // Step 2: Map the user's activity level to an NDAP kcal/kg factor.
        // Step 3: Branch on goal to determine final Total Energy Allowance.
        // ═══════════════════════════════════════════════════════════════

        // --- 1. Broca-Tanhauser: Desirable Body Weight (DBW) ---
        // DBW = (Height in cm − 100) × 0.90
        val dbw = (heightCm - 100) * 0.90

        // --- 2. NDAP Activity Factor (kcal per kg of DBW) ---
        // Backward-compatible: accepts both new NDAP IDs and legacy IDs.
        val activityKcalPerKg = when (activityLevel.lowercase().trim()) {
            "sedentary", "not_very_active"    -> 30.0  // Office worker, student, driver
            "light", "lightly_active"         -> 35.0  // Teacher, nurse, housewife with chores
            "moderate", "active"              -> 40.0  // Farmer, manual laborer, regular athlete
            "vigorous", "very_active"         -> 45.0  // Logger, construction worker, athlete in training
            else                              -> 30.0  // Conservative default
        }

        // --- 3. Total Energy Allowance (TEA) — Goal-Aware Branching ---
        // "General Health" → pure NDAP (no adjustment)
        // "Gain Muscle"    → NDAP baseline + 300 kcal surplus
        // "Weight Control" → NDAP baseline − 500 kcal deficit (floor 1200)
        val ndapBaseline = dbw * activityKcalPerKg

        var finalCalories = ndapBaseline.toInt()
        var proteinPct = 0.15
        var carbsPct = 0.60
        var fatsPct = 0.25

        when (goal.lowercase().trim()) {
            "lose_weight", "weight_loss", "weight", "weight control" -> {
                finalCalories = (ndapBaseline - 500).toInt().coerceAtLeast(1200) // 1200 floor safety
                proteinPct = 0.25
                carbsPct = 0.45
                fatsPct = 0.30
            }
            "gain_muscle" -> {
                finalCalories = (ndapBaseline + 300).toInt()
                proteinPct = 0.25
                carbsPct = 0.55
                fatsPct = 0.20
            }
            else -> {
                // "General Health & Wellness" or "Maintain" → pure NDAP TEA
                finalCalories = ndapBaseline.toInt()
                proteinPct = 0.15
                carbsPct = 0.60
                fatsPct = 0.25
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
