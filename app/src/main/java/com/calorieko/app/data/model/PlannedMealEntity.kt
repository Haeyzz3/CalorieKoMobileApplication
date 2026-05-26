package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "PLANNED_MEALS_TABLE",
    primaryKeys = ["uid", "day_index", "week_start_date", "meal_slot", "dish_label"]
)
data class PlannedMealEntity(
    @ColumnInfo(name = "uid") val uid: String = "",
    @ColumnInfo(name = "day_index") val dayIndex: Int,        // 0=Mon, 6=Sun
    @ColumnInfo(name = "dish_label") val dishLabel: String,
    @ColumnInfo(name = "week_start_date") val weekStartDate: String,  // ISO format, e.g. "2026-03-16"
    @ColumnInfo(name = "meal_slot") val mealSlot: String,      // "Breakfast", "Lunch", "Dinner", "Snack"
    @ColumnInfo(name = "substitutions_json", defaultValue = "")
    val substitutionsJson: String = "",  // JSON: {"black_pepper": "thyme", ...}

    @ColumnInfo(name = "scaled_servings", defaultValue = "0")
    val scaledServings: Int = 0,  // 0 = use original servings (no override)

    @ColumnInfo(name = "tweaks_json", defaultValue = "")
    val tweaksJson: String = "",   // JSON: {"garlic": 2.0, "onion": 0.5}

    /** Persistent status: "planned", "logged", "skipped", "missed" */
    @ColumnInfo(name = "status", defaultValue = "planned")
    val status: String = "planned"
)
