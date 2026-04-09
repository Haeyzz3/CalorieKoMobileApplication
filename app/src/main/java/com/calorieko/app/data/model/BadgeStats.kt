package com.calorieko.app.data.model

/**
 * Holds aggregate badge-tracking counts fetched from DAOs.
 * Used by ProfileViewModel to compute badge progress and leveled milestones.
 */
data class BadgeStats(
    val totalMeals: Int = 0,
    val totalWorkouts: Int = 0,
    val totalPhotos: Int = 0
)

/**
 * Represents a leveled badge milestone.
 * The badge progresses through multiple levels with increasing thresholds.
 * Level 0 = not yet started. Level 1 = first threshold reached, etc.
 */
data class LeveledBadge(
    val id: Int,
    val name: String,
    val baseDescription: String,
    val currentLevel: Int,           // 0 = locked, 1/2/3 = earned tiers
    val currentProgress: Int,        // user's current count toward next level
    val nextLevelThreshold: Int,     // threshold to reach the next level
    val maxLevel: Int = 3,           // max achievable level
    val levelThresholds: List<Int>   // e.g. [5, 10, 15]
) {
    val isMaxed: Boolean get() = currentLevel >= maxLevel
    val progressFraction: Float get() =
        if (isMaxed) 1f
        else (currentProgress.toFloat() / nextLevelThreshold.toFloat()).coerceIn(0f, 1f)
    val levelLabel: String get() = when (currentLevel) {
        0 -> "Locked"
        1 -> "Bronze"
        2 -> "Silver"
        3 -> "Gold"
        else -> "Legendary"
    }
}
