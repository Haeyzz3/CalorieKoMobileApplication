package com.calorieko.app.util

/**
 * Shared utility for formatting duration values consistently across the app.
 *
 * All screens (DashboardScreen, ActivityDetailsScreen, NutritionDetailsScreen,
 * LogWorkoutScreen) must use these functions so that the same activity
 * always displays the same human-readable time string.
 */
object DurationFormatter {

    /**
     * Formats a duration given in **seconds** into a human-readable string.
     *
     * Examples:
     *  - 52  → "0m 52s"
     *  - 352 → "5m 52s"
     *  - 3661 → "1h 1m 1s"
     *
     * @param totalSeconds Duration in seconds. Negative values are clamped to 0.
     */
    fun formatSeconds(totalSeconds: Long): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        val seconds = clamped % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            else -> "${minutes}m ${seconds}s"
        }
    }

    /**
     * Formats a duration given in **seconds** into a digital clock string (MM:SS or HH:MM:SS).
     *
     * Examples:
     *  - 52   → "00:52"
     *  - 352  → "05:52"
     *  - 3661 → "01:01:01"
     *
     * @param totalSeconds Duration in seconds. Negative values are clamped to 0.
     */
    fun formatDigital(totalSeconds: Long): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val hours = clamped / 3600
        val minutes = (clamped % 3600) / 60
        val seconds = clamped % 60

        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Parses a stored `weightOrDuration` string (e.g. "05:52", "5m 52s", "30 min")
     * back into total seconds. Returns null if the string cannot be parsed.
     *
     * Supports the following formats:
     *  - "HH:MM:SS" → hours * 3600 + minutes * 60 + seconds
     *  - "MM:SS"    → minutes * 60 + seconds
     *  - "Xm Ys"   → X * 60 + Y
     *  - "Xh Ym Zs" → X * 3600 + Y * 60 + Z
     *  - "X min"    → X * 60  (legacy manual workouts)
     */
    fun parseToSeconds(durationString: String?): Long? {
        if (durationString.isNullOrBlank()) return null
        val trimmed = durationString.trim()

        // Try HH:MM:SS or MM:SS
        val colonParts = trimmed.split(":")
        if (colonParts.size == 3) {
            val h = colonParts[0].toLongOrNull() ?: return null
            val m = colonParts[1].toLongOrNull() ?: return null
            val s = colonParts[2].toLongOrNull() ?: return null
            return h * 3600 + m * 60 + s
        }
        if (colonParts.size == 2) {
            val m = colonParts[0].toLongOrNull() ?: return null
            val s = colonParts[1].toLongOrNull() ?: return null
            return m * 60 + s
        }

        // Try "Xh Ym Zs" / "Xm Ys" pattern
        val hMatch = Regex("(\\d+)h").find(trimmed)
        val mMatch = Regex("(\\d+)m").find(trimmed)
        val sMatch = Regex("(\\d+)s").find(trimmed)
        if (mMatch != null || sMatch != null || hMatch != null) {
            val h = hMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
            val m = mMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
            val s = sMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
            return h * 3600 + m * 60 + s
        }

        // Try "X min" (legacy manual workout format)
        val minMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*min", RegexOption.IGNORE_CASE).find(trimmed)
        if (minMatch != null) {
            val minutes = minMatch.groupValues[1].toDoubleOrNull() ?: return null
            return (minutes * 60).toLong()
        }

        return null
    }
}
