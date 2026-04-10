package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FOOD_TABLE")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "food_id") val foodId: Int = 0,

    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_ph") val namePh: String,
    @ColumnInfo(name = "category") val category: String,

    @ColumnInfo(name = "ml_label") val mlLabel: String,

    // Core Energy (kcal)
    @ColumnInfo(name = "calories_per_100g") val caloriesPer100g: Float = 0f,

    // Macros (g)
    @ColumnInfo(name = "protein_per_100g") val proteinPer100g: Float = 0f, // in g
    @ColumnInfo(name = "carbs_per_100g") val carbsPer100g: Float = 0f, // in g
    @ColumnInfo(name = "fiber_per_100g") val fiberPer100g: Float = 0f, // in g
    @ColumnInfo(name = "sugar_per_100g") val sugarPer100g: Float = 0f, // in g
    @ColumnInfo(name = "fat_per_100g") val fatPer100g: Float = 0f, // in g

    // Fat Details (g and mg)
    @ColumnInfo(name = "saturated_fat_per_100g") val saturatedFatPer100g: Float = 0f, // in g
    @ColumnInfo(name = "polyunsaturated_fat_per_100g") val polyunsaturatedFatPer100g: Float = 0f, // in g
    @ColumnInfo(name = "monounsaturated_fat_per_100g") val monounsaturatedFatPer100g: Float = 0f, // in g
    @ColumnInfo(name = "trans_fat_per_100g") val transFatPer100g: Float = 0f, // in g
    @ColumnInfo(name = "cholesterol_per_100g") val cholesterolPer100g: Float = 0f, // in mg

    // Minerals & Vitamins (mg and µg)
    @ColumnInfo(name = "sodium_per_100g") val sodiumPer100g: Float = 0f, // in mg
    @ColumnInfo(name = "potassium_per_100g") val potassiumPer100g: Float = 0f, // in mg
    @ColumnInfo(name = "vitamin_a_per_100g") val vitaminAPer100g: Float = 0f, // in µg
    @ColumnInfo(name = "vitamin_c_per_100g") val vitaminCPer100g: Float = 0f, // in mg
    @ColumnInfo(name = "calcium_per_100g") val calciumPer100g: Float = 0f, // in mg
    @ColumnInfo(name = "iron_per_100g") val ironPer100g: Float = 0f, // in mg

    // Source Attribution
    @ColumnInfo(name = "data_source") val dataSource: String = "DOST_FNRI_MENU_GUIDE"
)