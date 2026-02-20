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
    @ColumnInfo(name = "calories_per_100g") val caloriesPer100g: Float,
    @ColumnInfo(name = "sodium_per_100g") val sodiumPer100g: Float,

    @ColumnInfo(name = "category") val category: String
)
