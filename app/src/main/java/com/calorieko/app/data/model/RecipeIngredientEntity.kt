package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity linking a dish to one of its raw ingredients with a gram weight.
 *
 * Composite PK: (dish_label, ingredient_key, step) — allows the same ingredient
 * to appear multiple times in a single dish (e.g., salt added in step 1 and step 2).
 *
 * Foreign-keyed to both [DishRecipeEntity] and [RawIngredientEntity] with CASCADE delete.
 * Seeded from `recipe_ingredients.json` (Phase 1 output).
 */
@Entity(
    tableName = "RECIPE_INGREDIENTS_TABLE",
    primaryKeys = ["dish_label", "ingredient_key", "step"],
    foreignKeys = [
        ForeignKey(
            entity = DishRecipeEntity::class,
            parentColumns = ["dish_label"],
            childColumns = ["dish_label"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RawIngredientEntity::class,
            parentColumns = ["ingredient_key"],
            childColumns = ["ingredient_key"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dish_label"),
        Index("ingredient_key")
    ]
)
data class RecipeIngredientEntity(
    @ColumnInfo(name = "dish_label") val dishLabel: String,
    @ColumnInfo(name = "ingredient_key") val ingredientKey: String,
    @ColumnInfo(name = "ingredient_type") val ingredientType: String,       // "core" or "optional"
    @ColumnInfo(name = "ingredient_category") val ingredientCategory: String, // "protein", "produce", "seasoning", "pantry_staple"
    @ColumnInfo(name = "raw_weight_grams") val rawWeightGrams: Float,
    @ColumnInfo(name = "portion_original") val portionOriginal: String,     // Original portion string for display
    @ColumnInfo(name = "preparation_method") val preparationMethod: String,
    @ColumnInfo(name = "step") val step: Int,
)
