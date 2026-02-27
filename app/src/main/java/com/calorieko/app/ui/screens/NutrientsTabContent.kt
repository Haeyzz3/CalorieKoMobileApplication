package com.calorieko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NutrientRow(
    val name: String,
    val total: Int,
    val goal: Int,
    val unit: String
)

@Composable
fun NutrientsTabContent() {
    val nutrients = listOf(
        NutrientRow("Protein", 0, 109, "g"),
        NutrientRow("Carbohydrates", 0, 271, "g"),
        NutrientRow("Fiber", 0, 38, "g"),
        NutrientRow("Sugar", 0, 81, "g"),
        NutrientRow("Fat", 0, 72, "g"),
        NutrientRow("Saturated Fat", 0, 24, "g"),
        NutrientRow("Polyunsaturated Fat", 0, 0, "g"),
        NutrientRow("Monounsaturated Fat", 0, 0, "g"),
        NutrientRow("Trans Fat", 0, 0, "g"),
        NutrientRow("Cholesterol", 0, 300, "mg"),
        NutrientRow("Sodium", 0, 2300, "mg"),
        NutrientRow("Potassium", 0, 3500, "mg"),
        NutrientRow("Vitamin A", 0, 100, "%"),
        NutrientRow("Vitamin C", 0, 100, "%"),
        NutrientRow("Calcium", 0, 100, "%"),
        NutrientRow("Iron", 0, 100, "%")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name column (takes remaining space)
            Spacer(modifier = Modifier.weight(1f))
            // Total
            Text(
                text = "Total",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.width(60.dp)
            )
            // Goal
            Text(
                text = "Goal",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.width(60.dp)
            )
            // Left
            Text(
                text = "Left",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.width(70.dp)
            )
        }

        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

        // --- Nutrient Rows ---
        nutrients.forEach { nutrient ->
            NutrientItemRow(nutrient)
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }
}

@Composable
private fun NutrientItemRow(nutrient: NutrientRow) {
    val left = nutrient.goal - nutrient.total
    val leftText = if (nutrient.unit == "%") {
        "$left %"
    } else {
        "$left ${nutrient.unit}"
    }
    val goalText = if (nutrient.unit == "%") {
        nutrient.goal.toFormattedString()
    } else {
        nutrient.goal.toFormattedString()
    }
    val progress = if (nutrient.goal > 0) {
        (nutrient.total.toFloat() / nutrient.goal).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name
            Text(
                text = nutrient.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF424242),
                modifier = Modifier.weight(1f)
            )
            // Total
            Text(
                text = nutrient.total.toString(),
                fontSize = 14.sp,
                color = Color(0xFF616161),
                modifier = Modifier.width(60.dp)
            )
            // Goal
            Text(
                text = goalText,
                fontSize = 14.sp,
                color = Color(0xFF616161),
                modifier = Modifier.width(60.dp)
            )
            // Left
            Text(
                text = leftText,
                fontSize = 14.sp,
                color = Color(0xFF616161),
                modifier = Modifier.width(70.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1565C0))
                )
            }
        }
    }
}

private fun Int.toFormattedString(): String {
    return String.format(java.util.Locale.US, "%,d", this)
}
