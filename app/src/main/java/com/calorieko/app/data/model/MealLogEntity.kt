package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Parent entity representing a single meal-logging session.
 *
 * One MealLogEntity contains one or more [MealLogItemEntity] children,
 * each representing a dish recognized by the AI and weighed by the IoT scale.
 */
@Entity(tableName = "meal_log_table")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "meal_log_id") val mealLogId: Long = 0,

    @ColumnInfo(name = "uid") val uid: String,

    /** "Breakfast", "Lunch", "Dinner", or "Snacks" */
    @ColumnInfo(name = "meal_type") val mealType: String,

    /** Epoch millis when the meal was logged */
    @ColumnInfo(name = "timestamp") val timestamp: Long,

    /** Optional user notes */
    @ColumnInfo(name = "notes") val notes: String? = null
)
