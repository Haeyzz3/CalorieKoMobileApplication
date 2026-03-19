package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PANTRY_TABLE")
data class PantryItem(
    @PrimaryKey
    @ColumnInfo(name = "ingredient_name") val ingredientName: String
)
