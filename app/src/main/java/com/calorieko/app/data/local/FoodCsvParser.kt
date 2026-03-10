package com.calorieko.app.data.local

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
                        ironPer100g = tokens[20].trim().toFloatOrNull() ?: 0f
                    )
                    dishes.add(item)
                }
            }
        }
        return dishes
    }
}
