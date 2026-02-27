package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Denormalized daily nutrition aggregate — upserted whenever a meal is
 * logged or deleted. Provides O(1) reads for Dashboard progress rings,
 * NutrientsTabContent, CaloriesTabContent donut chart, and MacrosTabContent.
 *
 * Unique index on (uid, dateEpochDay) ensures one row per user per day.
 */
@Entity(
    tableName = "daily_nutrition_summary_table",
    indices = [Index(value = ["uid", "date_epoch_day"], unique = true)]
)
data class DailyNutritionSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "uid") val uid: String,

    /** LocalDate.toEpochDay() for fast date-based indexing */
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,

    // ── Totals (all meals combined) ──
    @ColumnInfo(name = "total_calories") val totalCalories: Float = 0f,
    @ColumnInfo(name = "total_protein") val totalProtein: Float = 0f,
    @ColumnInfo(name = "total_carbs") val totalCarbs: Float = 0f,
    @ColumnInfo(name = "total_fiber") val totalFiber: Float = 0f,
    @ColumnInfo(name = "total_sugar") val totalSugar: Float = 0f,
    @ColumnInfo(name = "total_fat") val totalFat: Float = 0f,
    @ColumnInfo(name = "total_saturated_fat") val totalSaturatedFat: Float = 0f,
    @ColumnInfo(name = "total_polyunsaturated_fat") val totalPolyunsaturatedFat: Float = 0f,
    @ColumnInfo(name = "total_monounsaturated_fat") val totalMonounsaturatedFat: Float = 0f,
    @ColumnInfo(name = "total_trans_fat") val totalTransFat: Float = 0f,
    @ColumnInfo(name = "total_cholesterol") val totalCholesterol: Float = 0f,
    @ColumnInfo(name = "total_sodium") val totalSodium: Float = 0f,
    @ColumnInfo(name = "total_potassium") val totalPotassium: Float = 0f,
    @ColumnInfo(name = "total_vitamin_a") val totalVitaminA: Float = 0f,
    @ColumnInfo(name = "total_vitamin_c") val totalVitaminC: Float = 0f,
    @ColumnInfo(name = "total_calcium") val totalCalcium: Float = 0f,
    @ColumnInfo(name = "total_iron") val totalIron: Float = 0f,

    // ── Per-meal-type calorie breakdowns (for CaloriesTab donut chart) ──
    @ColumnInfo(name = "breakfast_calories") val breakfastCalories: Float = 0f,
    @ColumnInfo(name = "lunch_calories") val lunchCalories: Float = 0f,
    @ColumnInfo(name = "dinner_calories") val dinnerCalories: Float = 0f,
    @ColumnInfo(name = "snacks_calories") val snacksCalories: Float = 0f
)
