package com.calorieko.app.data.local

import com.calorieko.app.data.model.DishIngredient
import com.calorieko.app.data.model.FoodItem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object FoodCsvParser {
    fun parse(inputStream: InputStream): List<FoodItem> {
        val dishes = mutableListOf<FoodItem>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var isFirstLine = true
        
        reader.useLines { lines ->
            lines.forEach { line ->
                if (isFirstLine) {
                    isFirstLine = false
                    return@forEach
                }
                
                // Assuming it's simple CSV, no quoted commas.
                val tokens = line.split(",")
                if (tokens.size >= 21) {
                    val item = FoodItem(
                        mlLabel = tokens[0].trim(),
                        nameEn = tokens[1].trim(),
                        namePh = tokens[2].trim(),
                        category = tokens[3].trim(),
                        caloriesPer100g = tokens[4].trim().toFloatOrNull() ?: 0f,
                        proteinPer100g = tokens[5].trim().toFloatOrNull() ?: 0f,
                        carbsPer100g = tokens[6].trim().toFloatOrNull() ?: 0f,
                        fatPer100g = tokens[7].trim().toFloatOrNull() ?: 0f,
                        fiberPer100g = tokens[8].trim().toFloatOrNull() ?: 0f,
                        sugarPer100g = tokens[9].trim().toFloatOrNull() ?: 0f,
                        saturatedFatPer100g = tokens[10].trim().toFloatOrNull() ?: 0f,
                        polyunsaturatedFatPer100g = tokens[11].trim().toFloatOrNull() ?: 0f,
                        monounsaturatedFatPer100g = tokens[12].trim().toFloatOrNull() ?: 0f,
                        transFatPer100g = tokens[13].trim().toFloatOrNull() ?: 0f,
                        cholesterolPer100g = tokens[14].trim().toFloatOrNull() ?: 0f,
                        sodiumPer100g = tokens[15].trim().toFloatOrNull() ?: 0f,
                        potassiumPer100g = tokens[16].trim().toFloatOrNull() ?: 0f,
                        vitaminAPer100g = tokens[17].trim().toFloatOrNull() ?: 0f,
                        vitaminCPer100g = tokens[18].trim().toFloatOrNull() ?: 0f,
                        calciumPer100g = tokens[19].trim().toFloatOrNull() ?: 0f,
                        ironPer100g = tokens[20].trim().toFloatOrNull() ?: 0f,
                        dataSource = tokens.getOrNull(21)?.trim() ?: "DOST_FNRI_MENU_GUIDE"
                    )
                    dishes.add(item)
                }
            }
        }
        return dishes
    }

    /**
     * Parses dish_ingredients.csv.
     * Expected format: ml_label,ingredient_name,ingredient_type,ingredient_category,portion_quantity,preparation_method,step
     * (with a header row).
     */
    fun parseDishIngredients(inputStream: InputStream): List<DishIngredient> {
        val ingredients = mutableListOf<DishIngredient>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var isFirstLine = true

        reader.useLines { lines ->
            lines.forEach { line ->
                if (isFirstLine) {
                    isFirstLine = false
                    return@forEach
                }

                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) return@forEach

                val tokens = parseCsvLine(trimmedLine)
                if (tokens.size >= 7) {
                    ingredients.add(
                        DishIngredient(
                            dishLabel = tokens[0].trim(),
                            ingredientName = tokens[1].trim(),
                            ingredientType = tokens[2].trim(),
                            ingredientCategory = tokens[3].trim(),
                            portionQuantity = tokens[4].trim(),
                            preparationMethod = tokens[5].trim(),
                            step = tokens[6].trim().toIntOrNull() ?: 1
                        )
                    )
                } else if (tokens.size >= 4) {
                    // Fallback for rows with 4-6 columns (missing portion/prep/step)
                    ingredients.add(
                        DishIngredient(
                            dishLabel = tokens[0].trim(),
                            ingredientName = tokens[1].trim(),
                            ingredientType = tokens[2].trim(),
                            ingredientCategory = tokens[3].trim(),
                            portionQuantity = tokens.getOrNull(4)?.trim() ?: "",
                            preparationMethod = tokens.getOrNull(5)?.trim() ?: "",
                            step = tokens.getOrNull(6)?.trim()?.toIntOrNull() ?: 1
                        )
                    )
                } else if (tokens.size >= 2) {
                    // Legacy 2-column fallback
                    ingredients.add(
                        DishIngredient(
                            dishLabel = tokens[0].trim(),
                            ingredientName = tokens[1].trim()
                        )
                    )
                }
            }
        }
        return ingredients
    }

    /**
     * Parses a single CSV line with minimal quote-awareness.
     * Handles fields wrapped in double-quotes (e.g., `"cut into 1"""`),
     * where `""` inside quotes represents a literal double-quote character.
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> {
                    inQuotes = true
                }
                c == '"' && inQuotes -> {
                    // Check for escaped quote ("")
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip the second quote
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
