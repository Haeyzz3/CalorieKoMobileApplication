package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single raw ingredient with USDA-sourced nutritional data.
 *
 * All nutrient values are per 100g of the raw ingredient.
 * Seeded from `raw_ingredients.json` (Phase 1 output).
 */
@Entity(tableName = "RAW_INGREDIENTS_TABLE")
data class RawIngredientEntity(
    @PrimaryKey
    @ColumnInfo(name = "ingredient_key") val ingredientKey: String,

    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "sub_category") val subCategory: String,
    @ColumnInfo(name = "fdc_id") val fdcId: Int,
    @ColumnInfo(name = "data_source") val dataSource: String,

    // Nutrients per 100g raw
    @ColumnInfo(name = "calories") val calories: Float = 0f,
    @ColumnInfo(name = "protein") val protein: Float = 0f,
    @ColumnInfo(name = "carbs") val carbs: Float = 0f,
    @ColumnInfo(name = "fat") val fat: Float = 0f,
    @ColumnInfo(name = "fiber") val fiber: Float = 0f,
    @ColumnInfo(name = "sugar") val sugar: Float = 0f,
    @ColumnInfo(name = "sodium") val sodium: Float = 0f,
    @ColumnInfo(name = "potassium") val potassium: Float = 0f,
    @ColumnInfo(name = "vitamin_a") val vitaminA: Float = 0f,
    @ColumnInfo(name = "vitamin_c") val vitaminC: Float = 0f,
    @ColumnInfo(name = "calcium") val calcium: Float = 0f,
    @ColumnInfo(name = "iron") val iron: Float = 0f,

    // Substitution control: false = ingredient never appears as a swap candidate
    @ColumnInfo(name = "is_substitutable", defaultValue = "1") val isSubstitutable: Boolean = true,

    // Transparency note for proxy-sourced ingredients (empty = direct USDA match)
    @ColumnInfo(name = "nutrient_proxy_note", defaultValue = "") val nutrientProxyNote: String = "",
)
