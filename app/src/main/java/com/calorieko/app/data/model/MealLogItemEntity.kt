package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Child entity representing a single recognized dish within a meal session.
 *
 * All nutrition values are **pre-computed at log time** using the formula:
 *   `nutrientPer100g × weightGrams / 100`
 * so that downstream queries never need to join back to [FoodItem].
 *
 * Foreign-keyed to [MealLogEntity] with CASCADE delete.
 */
@Entity(
    tableName = "meal_log_item_table",
    foreignKeys = [
        ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["meal_log_id"],
            childColumns = ["meal_log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meal_log_id")]
)
data class MealLogItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "meal_log_item_id") val mealLogItemId: Long = 0,

    /** FK → meal_log_table.meal_log_id */
    @ColumnInfo(name = "meal_log_id") val mealLogId: Long,

    /** FK reference to FOOD_TABLE.food_id (not enforced to keep flexibility) */
    @ColumnInfo(name = "food_id") val foodId: Int,

    /** Snapshot of the dish name at log time */
    @ColumnInfo(name = "dish_name") val dishName: String,

    /** Actual weight in grams from the IoT scale */
    @ColumnInfo(name = "weight_grams") val weightGrams: Float,

    // ── Core Energy ──
    @ColumnInfo(name = "calories") val calories: Float = 0f,

    // ── Macros ──
    @ColumnInfo(name = "protein") val protein: Float = 0f,
    @ColumnInfo(name = "carbs") val carbs: Float = 0f,
    @ColumnInfo(name = "fiber") val fiber: Float = 0f,
    @ColumnInfo(name = "sugar") val sugar: Float = 0f,
    @ColumnInfo(name = "fat") val fat: Float = 0f,

    // ── Fat Details ──
    @ColumnInfo(name = "saturated_fat") val saturatedFat: Float = 0f,
    @ColumnInfo(name = "polyunsaturated_fat") val polyunsaturatedFat: Float = 0f,
    @ColumnInfo(name = "monounsaturated_fat") val monounsaturatedFat: Float = 0f,
    @ColumnInfo(name = "trans_fat") val transFat: Float = 0f,
    @ColumnInfo(name = "cholesterol") val cholesterol: Float = 0f,

    // ── Minerals & Vitamins ──
    @ColumnInfo(name = "sodium") val sodium: Float = 0f,
    @ColumnInfo(name = "potassium") val potassium: Float = 0f,
    @ColumnInfo(name = "vitamin_a") val vitaminA: Float = 0f,
    @ColumnInfo(name = "vitamin_c") val vitaminC: Float = 0f,
    @ColumnInfo(name = "calcium") val calcium: Float = 0f,
    @ColumnInfo(name = "iron") val iron: Float = 0f
)
