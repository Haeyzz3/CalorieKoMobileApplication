package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "DISH_INGREDIENTS_TABLE",
    primaryKeys = ["dish_label", "ingredient_name"]
)
data class DishIngredient(
    @ColumnInfo(name = "dish_label") val dishLabel: String,
    @ColumnInfo(name = "ingredient_name") val ingredientName: String,
    @ColumnInfo(name = "ingredient_type") val ingredientType: String = "core",           // "core" or "optional"
    @ColumnInfo(name = "ingredient_category") val ingredientCategory: String = "pantry_staple"  // "protein", "produce", "seasoning", "pantry_staple"
)
