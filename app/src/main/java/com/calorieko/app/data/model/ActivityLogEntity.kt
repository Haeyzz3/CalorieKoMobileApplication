package com.calorieko.app.data.model


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log_table")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uid: String, // The user who logged this
    val type: String, // "meal" or "workout"
    val name: String, // e.g., "Chicken Adobo" or "Morning Walk"
    val timeString: String, // e.g., "8:30 AM"
    val weightOrDuration: String, // e.g., "250g" or "30 min"

    // Nutrition / Burn Data
    val calories: Int,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val sodium: Int = 0,

    val timestamp: Long // Used to filter by "today"
)