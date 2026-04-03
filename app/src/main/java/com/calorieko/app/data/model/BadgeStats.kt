package com.calorieko.app.data.model

/**
 * Holds aggregate badge-tracking counts fetched from DAOs.
 * Used by ProfileViewModel to compute badge progress.
 */
data class BadgeStats(
    val totalMeals: Int = 0,
    val totalWorkouts: Int = 0,
    val totalPhotos: Int = 0
)
