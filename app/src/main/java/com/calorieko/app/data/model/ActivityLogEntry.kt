package com.calorieko.app.data.model

/**
 * UI model representing a single entry in the daily activity feed.
 * Used by DashboardViewModel to merge meal logs and workout logs
 * into a unified, time-sorted list.
 */
data class ActivityLogEntry(
    val id: String,
    val type: String, // "meal" or "workout"
    val time: String,
    val name: String,
    val details: ActivityDetails,
    val timestamp: Long = 0L
)

data class ActivityDetails(
    val weight: String? = null,
    val calories: Int,
    val sodium: Int? = null,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val duration: String? = null
)
