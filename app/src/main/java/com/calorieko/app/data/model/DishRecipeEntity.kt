package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a dish recipe with pre-computed per-serving nutrition.
 *
 * Contains metadata (names, category, cooking method), yield factor,
 * and per-serving nutrient values computed from raw ingredient summation.
 * Seeded from `dish_recipes.json` (Phase 1 output).
 */
@Entity(tableName = "DISH_RECIPES_TABLE")
data class DishRecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "dish_label") val dishLabel: String,

    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_ph") val namePh: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "cooking_method") val cookingMethod: String,
    @ColumnInfo(name = "servings") val servings: Int,
    @ColumnInfo(name = "total_raw_weight_g") val totalRawWeightG: Float,
    @ColumnInfo(name = "dish_yield_factor") val dishYieldFactor: Float,
    @ColumnInfo(name = "cooked_weight_g") val cookedWeightG: Float,
    @ColumnInfo(name = "per_serving_weight_g") val perServingWeightG: Float,
    @ColumnInfo(name = "ingredient_count") val ingredientCount: Int,

    // FNRI serving size description (e.g., "1 1/2 cups", "3 matchbox size chicken + 1 cup vegetables")
    @ColumnInfo(name = "serving_size_description", defaultValue = "") val servingSizeDescription: String = "",

    // Pre-computed per-serving nutrients (from assemble_dishes.py)
    @ColumnInfo(name = "cal_per_serving") val calPerServing: Float = 0f,
    @ColumnInfo(name = "protein_per_serving") val proteinPerServing: Float = 0f,
    @ColumnInfo(name = "carbs_per_serving") val carbsPerServing: Float = 0f,
    @ColumnInfo(name = "fat_per_serving") val fatPerServing: Float = 0f,
    @ColumnInfo(name = "fiber_per_serving") val fiberPerServing: Float = 0f,
    @ColumnInfo(name = "sugar_per_serving") val sugarPerServing: Float = 0f,
    @ColumnInfo(name = "sodium_per_serving") val sodiumPerServing: Float = 0f,
    @ColumnInfo(name = "potassium_per_serving") val potassiumPerServing: Float = 0f,
    @ColumnInfo(name = "vitamin_a_per_serving") val vitaminAPerServing: Float = 0f,
    @ColumnInfo(name = "vitamin_c_per_serving") val vitaminCPerServing: Float = 0f,
    @ColumnInfo(name = "calcium_per_serving") val calciumPerServing: Float = 0f,
    @ColumnInfo(name = "iron_per_serving") val ironPerServing: Float = 0f,
)
