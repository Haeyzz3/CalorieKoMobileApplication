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
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.ui.theme.*

private data class NutrientRow(
    val name: String,
    val total: Int,
    val goal: Int,
    val unit: String
)

@Composable
fun NutrientsTabContent(
    viewMode: String,
    daySummary: DailyNutritionSummaryEntity?,
    targets: com.calorieko.app.data.repository.NutritionalTarget?,
    weekDaySummaries: List<DailyNutritionSummaryEntity?>
) {
    if (viewMode == "day") {
        NutrientsDayView(
            daySummary = daySummary,
            targets = targets
        )
    } else {
        NutrientsWeekView(
            weekDaySummaries = weekDaySummaries,
            targets = targets
        )
    }
}

// =====================================================
// DAY VIEW
// =====================================================

@Composable
private fun NutrientsDayView(
    daySummary: DailyNutritionSummaryEntity?,
    targets: com.calorieko.app.data.repository.NutritionalTarget?
) {
    val nutrients = listOf(
        NutrientRow("Protein",             daySummary?.totalProtein?.toInt() ?: 0,            targets?.targetProtein ?: 150, "g"),
        NutrientRow("Carbohydrates",       daySummary?.totalCarbs?.toInt() ?: 0,              targets?.targetCarbs ?: 200,   "g"),
        NutrientRow("Fiber",               daySummary?.totalFiber?.toInt() ?: 0,              targets?.targetFiber ?: 38,     "g"),
        NutrientRow("Sugar",               daySummary?.totalSugar?.toInt() ?: 0,              targets?.targetSugar ?: 50,     "g"),
        NutrientRow("Fat",                 daySummary?.totalFat?.toInt() ?: 0,                targets?.targetFats ?: 65,    "g"),
        NutrientRow("Saturated Fat",       daySummary?.totalSaturatedFat?.toInt() ?: 0,       targets?.targetSaturatedFat ?: 20, "g"),
        NutrientRow("Polyunsaturated Fat", daySummary?.totalPolyunsaturatedFat?.toInt() ?: 0, targets?.targetPolyunsaturatedFat ?: 0, "g"),
        NutrientRow("Monounsaturated Fat", daySummary?.totalMonounsaturatedFat?.toInt() ?: 0, targets?.targetMonounsaturatedFat ?: 0, "g"),
        NutrientRow("Trans Fat",           daySummary?.totalTransFat?.toInt() ?: 0,           targets?.targetTransFat ?: 0, "g"),
        NutrientRow("Cholesterol",         daySummary?.totalCholesterol?.toInt() ?: 0,        targets?.targetCholesterol ?: 300, "mg"),
        NutrientRow("Sodium",              daySummary?.totalSodium?.toInt() ?: 0,             targets?.targetSodium ?: 2300,  "mg"),
        NutrientRow("Potassium",           daySummary?.totalPotassium?.toInt() ?: 0,          targets?.targetPotassium ?: 3500, "mg"),
        NutrientRow("Vitamin A",           daySummary?.totalVitaminA?.toInt() ?: 0,           targets?.targetVitaminA ?: 900,           "µg"),
        NutrientRow("Vitamin C",           daySummary?.totalVitaminC?.toInt() ?: 0,           targets?.targetVitaminC ?: 90,            "mg"),
        NutrientRow("Calcium",             daySummary?.totalCalcium?.toInt() ?: 0,            targets?.targetCalcium ?: 1000,          "mg"),
        NutrientRow("Iron",                daySummary?.totalIron?.toInt() ?: 0,               targets?.targetIron ?: 18,            "mg")
    )

    NutrientTable(
        valueColumnHeader = "Total",
        nutrients = nutrients
    )
}

// =====================================================
// WEEK VIEW
// =====================================================

@Composable
private fun NutrientsWeekView(
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    targets: com.calorieko.app.data.repository.NutritionalTarget?
) {
    // Compute daily averages across the 7-day week (divide by 7 always)
    fun avgOf(selector: (DailyNutritionSummaryEntity) -> Float): Int {
        val sum = weekDaySummaries.sumOf { (selector(it ?: return@sumOf 0.0) ).toDouble() }
        return (sum / 7).toInt()
    }

    val nutrients = listOf(
        NutrientRow("Protein",             avgOf { it.totalProtein },            targets?.targetProtein ?: 150, "g"),
        NutrientRow("Carbohydrates",       avgOf { it.totalCarbs },              targets?.targetCarbs ?: 200,   "g"),
        NutrientRow("Fiber",               avgOf { it.totalFiber },              targets?.targetFiber ?: 38,     "g"),
        NutrientRow("Sugar",               avgOf { it.totalSugar },              targets?.targetSugar ?: 50,     "g"),
        NutrientRow("Fat",                 avgOf { it.totalFat },                targets?.targetFats ?: 65,    "g"),
        NutrientRow("Saturated Fat",       avgOf { it.totalSaturatedFat },       targets?.targetSaturatedFat ?: 20, "g"),
        NutrientRow("Polyunsaturated Fat", avgOf { it.totalPolyunsaturatedFat }, targets?.targetPolyunsaturatedFat ?: 0, "g"),
        NutrientRow("Monounsaturated Fat", avgOf { it.totalMonounsaturatedFat }, targets?.targetMonounsaturatedFat ?: 0, "g"),
        NutrientRow("Trans Fat",           avgOf { it.totalTransFat },           targets?.targetTransFat ?: 0, "g"),
        NutrientRow("Cholesterol",         avgOf { it.totalCholesterol },        targets?.targetCholesterol ?: 300, "mg"),
        NutrientRow("Sodium",              avgOf { it.totalSodium },             targets?.targetSodium ?: 2300,  "mg"),
        NutrientRow("Potassium",           avgOf { it.totalPotassium },          targets?.targetPotassium ?: 3500, "mg"),
        NutrientRow("Vitamin A",           avgOf { it.totalVitaminA },           targets?.targetVitaminA ?: 900,           "µg"),
        NutrientRow("Vitamin C",           avgOf { it.totalVitaminC },           targets?.targetVitaminC ?: 90,            "mg"),
        NutrientRow("Calcium",             avgOf { it.totalCalcium },            targets?.targetCalcium ?: 1000,          "mg"),
        NutrientRow("Iron",                avgOf { it.totalIron },               targets?.targetIron ?: 18,            "mg")
    )

    NutrientTable(
        valueColumnHeader = "Avg",
        nutrients = nutrients
    )
}

// =====================================================
// SHARED TABLE
// =====================================================

@Composable
private fun NutrientTable(
    valueColumnHeader: String,
    nutrients: List<NutrientRow>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- Header Row ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = valueColumnHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "Left",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubtleText,
                        modifier = Modifier.width(70.dp)
                    )
                }

                HorizontalDivider(color = DividerGray, thickness = 1.dp)

                // --- Nutrient Rows ---
                nutrients.forEach { nutrient ->
                    NutrientItemRow(nutrient)
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                }
            }
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
    val goalText = nutrient.goal.toFormattedString()
    val progress = if (nutrient.goal > 0) {
        (nutrient.total.toFloat() / nutrient.goal).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Color the progress bar green normally, orange if over 80%
    val progressColor = if (progress > 0.8f) CalorieKoOrange else CalorieKoGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                color = DarkText,
                modifier = Modifier.weight(1f)
            )
            // Total / Avg
            Text(
                text = nutrient.total.toString(),
                fontSize = 14.sp,
                color = SubtleText,
                modifier = Modifier.width(60.dp)
            )
            // Goal
            Text(
                text = goalText,
                fontSize = 14.sp,
                color = SubtleText,
                modifier = Modifier.width(60.dp)
            )
            // Left
            Text(
                text = leftText,
                fontSize = 14.sp,
                color = if (left < 0) CalorieKoOrange else SubtleText,
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
                .background(DividerGray)
        ) {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(progressColor)
                )
            }
        }
    }
}

private fun Int.toFormattedString(): String {
    return String.format(java.util.Locale.US, "%,d", this)
}
