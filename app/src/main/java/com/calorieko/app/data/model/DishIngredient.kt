package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "DISH_INGREDIENTS_TABLE",
    primaryKeys = ["dish_label", "ingredient_name"]
)
data class DishIngredient(
    @ColumnInfo(name = "dish_label") val dishLabel: String,
    @ColumnInfo(name = "ingredient_name") val ingredientName: String
)
