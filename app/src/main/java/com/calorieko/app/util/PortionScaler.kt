package com.calorieko.app.util

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Utility for scaling recipe portion strings by a multiplier.
 *
 * Handles mixed fractions ("1 1/2 cups"), simple fractions ("1/3 cup"),
 * integers/decimals ("5 pcs"), and qualitative descriptions ("enough to cover").
 * Qualitative portions bypass scaling and are returned unchanged.
 */
object PortionScaler {

    // Common fractions for "pretty" output (sorted by value for nearest-match)
    private val FRACTION_TABLE = listOf(
        1f / 8f to "1/8",
        1f / 4f to "1/4",
        1f / 3f to "1/3",
        3f / 8f to "3/8",
        1f / 2f to "1/2",
        5f / 8f to "5/8",
        2f / 3f to "2/3",
        3f / 4f to "3/4",
        7f / 8f to "7/8"
    )

    // Qualitative keywords that indicate non-numeric portions
    private val QUALITATIVE_KEYWORDS = listOf(
        "enough", "to taste", "as needed", "to cover", "pinch", "dash"
    )

    // Regex patterns (ordered by specificity)
    // Mixed fraction: "1 1/2 cups", "2 3/4 tbsp"
    private val MIXED_FRACTION_REGEX =
        Regex("""^(\d+)\s+(\d+)/(\d+)\s*(.*)$""")

    // Simple fraction: "1/3 cup", "1/2 tsp"
    private val SIMPLE_FRACTION_REGEX =
        Regex("""^(\d+)/(\d+)\s*(.*)$""")

    // Decimal or integer: "5 pcs", "2.5 cups", "250"
    private val DECIMAL_REGEX =
        Regex("""^([\d.]+)\s*(.*)$""")

    /**
     * Scales a portion string by the given multiplier.
     * Returns the original string unchanged if:
     * - multiplier is 1.0
     * - the portion is qualitative (e.g., "enough to cover")
     * - the portion cannot be parsed
     */
    fun scale(portionOriginal: String, multiplier: Float): String {
        if (multiplier == 1f) return portionOriginal
        if (portionOriginal.isBlank()) return portionOriginal
        if (isQualitative(portionOriginal)) return portionOriginal

        val parsed = parseQuantity(portionOriginal) ?: return portionOriginal
        val scaled = parsed.value * multiplier
        return formatQuantity(scaled, parsed.unit)
    }

    /**
     * Returns true if the portion string is qualitative (non-numeric).
     */
    fun isQualitative(portion: String): Boolean {
        val lower = portion.lowercase().trim()
        return QUALITATIVE_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Parses a portion string into a numeric value and unit.
     * Supports mixed fractions, simple fractions, and decimal/integer values.
     */
    private fun parseQuantity(portion: String): ParsedQuantity? {
        val trimmed = portion.trim()

        // Try mixed fraction first: "1 1/2 cups"
        MIXED_FRACTION_REGEX.matchEntire(trimmed)?.let { match ->
            val whole = match.groupValues[1].toFloatOrNull() ?: return null
            val num = match.groupValues[2].toFloatOrNull() ?: return null
            val den = match.groupValues[3].toFloatOrNull() ?: return null
            if (den == 0f) return null
            val unit = match.groupValues[4].trim()
            return ParsedQuantity(whole + num / den, unit)
        }

        // Try simple fraction: "1/3 cup"
        SIMPLE_FRACTION_REGEX.matchEntire(trimmed)?.let { match ->
            val num = match.groupValues[1].toFloatOrNull() ?: return null
            val den = match.groupValues[2].toFloatOrNull() ?: return null
            if (den == 0f) return null
            val unit = match.groupValues[3].trim()
            return ParsedQuantity(num / den, unit)
        }

        // Try decimal/integer: "5 pcs", "2.5 cups"
        DECIMAL_REGEX.matchEntire(trimmed)?.let { match ->
            val value = match.groupValues[1].toFloatOrNull() ?: return null
            val unit = match.groupValues[2].trim()
            return ParsedQuantity(value, unit)
        }

        return null
    }

    /**
     * Formats a scaled numeric quantity back into a human-readable string.
     *
     * Rules:
     * - Whole numbers → "6"
     * - Clean fractions → "1/2", "1 1/4"
     * - Unclean fractions → "~1.2" (approximate, rounded to 1 decimal)
     */
    private fun formatQuantity(value: Float, unit: String): String {
        val wholePart = floor(value).toInt()
        val fractionalPart = value - wholePart

        val formatted = when {
            // Essentially zero fractional part → whole number
            fractionalPart < 0.02f -> "$wholePart"

            // Essentially a whole number (fractional part close to 1)
            fractionalPart > 0.98f -> "${wholePart + 1}"

            // Try to match to a known fraction
            else -> {
                val matchedFraction = findClosestFraction(fractionalPart)
                if (matchedFraction != null) {
                    if (wholePart > 0) "$wholePart $matchedFraction"
                    else matchedFraction
                } else {
                    // No clean fraction — show approximate decimal
                    val rounded = (value * 10).roundToInt() / 10f
                    val display = if (rounded == rounded.toInt().toFloat()) {
                        "${rounded.toInt()}"
                    } else {
                        "%.1f".format(rounded)
                    }
                    "~$display"
                }
            }
        }

        return if (unit.isNotBlank()) "$formatted $unit" else formatted
    }

    /**
     * Finds the closest fraction from the lookup table within a tolerance.
     * Returns the fraction string (e.g., "1/4") or null if no match.
     */
    private fun findClosestFraction(fractional: Float): String? {
        val tolerance = 0.04f
        return FRACTION_TABLE
            .minByOrNull { abs(it.first - fractional) }
            ?.takeIf { abs(it.first - fractional) < tolerance }
            ?.second
    }

    private data class ParsedQuantity(val value: Float, val unit: String)
}
