package com.calorieko.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A detailed nutrient breakdown grid showing all 13 additional nutrients
 * beyond the primary calories/protein/carbs/fat.
 */
@Composable
fun ExpandableNutrientGrid(
    fiber: Float,
    sugar: Float,
    saturatedFat: Float,
    polyunsaturatedFat: Float,
    monounsaturatedFat: Float,
    transFat: Float,
    cholesterol: Float,
    sodium: Float,
    potassium: Float,
    vitaminA: Float,
    vitaminC: Float,
    calcium: Float,
    iron: Float
) {
    fun Float.fmt() = String.format(java.util.Locale.US, "%.1f", this)
    val nutrients = listOf(
        Triple("Fiber",               "${fiber.fmt()}g",              Color(0xFF10B981)),
        Triple("Sugar",               "${sugar.fmt()}g",              Color(0xFFF59E0B)),
        Triple("Saturated Fat",       "${saturatedFat.fmt()}g",       Color(0xFFEF4444)),
        Triple("Polyunsat. Fat",      "${polyunsaturatedFat.fmt()}g", Color(0xFF6366F1)),
        Triple("Monounsat. Fat",      "${monounsaturatedFat.fmt()}g", Color(0xFF8B5CF6)),
        Triple("Trans Fat",           "${transFat.fmt()}g",           Color(0xFFDC2626)),
        Triple("Cholesterol",         "${cholesterol.fmt()}mg",       Color(0xFFEC4899)),
        Triple("Sodium",              "${sodium.fmt()}mg",            Color(0xFFF97316)),
        Triple("Potassium",           "${potassium.fmt()}mg",         Color(0xFF14B8A6)),
        Triple("Vitamin A",           "${vitaminA.fmt()}µg",          Color(0xFFEAB308)),
        Triple("Vitamin C",           "${vitaminC.fmt()}mg",          Color(0xFF22C55E)),
        Triple("Calcium",             "${calcium.fmt()}mg",           Color(0xFF0EA5E9)),
        Triple("Iron",                "${iron.fmt()}mg",              Color(0xFF78716C))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Display in 2-column rows
        nutrients.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { (name, value, dotColor) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(dotColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            name,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                    }
                }
                // If odd number, fill remaining space
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun NutrientChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
        Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
    }
}
