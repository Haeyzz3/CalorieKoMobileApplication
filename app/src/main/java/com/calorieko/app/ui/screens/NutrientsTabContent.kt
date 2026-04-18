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
    val total: Float,
    val goal: Float,
    val unit: String
)

@Composable
fun NutrientsTabContent(
    viewMode: String,
    daySummary: DailyNutritionSummaryEntity?,
    targets: com.calorieko.app.data.repository.NutritionalTarget?,
    weekDaySummaries: List<DailyNutritionSummaryEntity?>,
    weekDayLabels: List<String>
) {
    if (viewMode == "day") {
        NutrientsDayView(
            daySummary = daySummary,
            targets = targets
        )
    } else {
        NutrientsWeekView(
            weekDaySummaries = weekDaySummaries,
            targets = targets,
            weekDayLabels = weekDayLabels
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
        NutrientRow("Protein",             daySummary?.totalProtein ?: 0f,            targets?.targetProtein?.toFloat() ?: 150f, "g"),
        NutrientRow("Carbohydrates",       daySummary?.totalCarbs ?: 0f,              targets?.targetCarbs?.toFloat() ?: 200f,   "g"),
        NutrientRow("Fiber",               daySummary?.totalFiber ?: 0f,              targets?.targetFiber?.toFloat() ?: 38f,     "g"),
        NutrientRow("Sugar",               daySummary?.totalSugar ?: 0f,              targets?.targetSugar?.toFloat() ?: 50f,     "g"),
        NutrientRow("Fat",                 daySummary?.totalFat ?: 0f,                targets?.targetFats?.toFloat() ?: 65f,    "g"),
        NutrientRow("Sodium",              daySummary?.totalSodium ?: 0f,             targets?.targetSodium?.toFloat() ?: 2300f,  "mg"),
        NutrientRow("Potassium",           daySummary?.totalPotassium ?: 0f,          targets?.targetPotassium?.toFloat() ?: 3500f, "mg"),
        NutrientRow("Vitamin A",           daySummary?.totalVitaminA ?: 0f,           targets?.targetVitaminA?.toFloat() ?: 900f,           "µg"),
        NutrientRow("Vitamin C",           daySummary?.totalVitaminC ?: 0f,           targets?.targetVitaminC?.toFloat() ?: 90f,            "mg"),
        NutrientRow("Calcium",             daySummary?.totalCalcium ?: 0f,            targets?.targetCalcium?.toFloat() ?: 1000f,          "mg"),
        NutrientRow("Iron",                daySummary?.totalIron ?: 0f,               targets?.targetIron?.toFloat() ?: 18f,            "mg")
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
    targets: com.calorieko.app.data.repository.NutritionalTarget?,
    weekDayLabels: List<String>
) {
    // Compute daily averages across the 7-day week (divide by 7 always)
    fun avgOf(selector: (DailyNutritionSummaryEntity) -> Float): Float {
        val sum = weekDaySummaries.sumOf { (selector(it ?: return@sumOf 0.0) ).toDouble() }
        return (sum / 7).toFloat()
    }

    val nutrients = listOf(
        NutrientRow("Protein",             avgOf { it.totalProtein },            targets?.targetProtein?.toFloat() ?: 150f, "g"),
        NutrientRow("Carbohydrates",       avgOf { it.totalCarbs },              targets?.targetCarbs?.toFloat() ?: 200f,   "g"),
        NutrientRow("Fiber",               avgOf { it.totalFiber },              targets?.targetFiber?.toFloat() ?: 38f,     "g"),
        NutrientRow("Sugar",               avgOf { it.totalSugar },              targets?.targetSugar?.toFloat() ?: 50f,     "g"),
        NutrientRow("Fat",                 avgOf { it.totalFat },                targets?.targetFats?.toFloat() ?: 65f,    "g"),
        NutrientRow("Sodium",              avgOf { it.totalSodium },             targets?.targetSodium?.toFloat() ?: 2300f,  "mg"),
        NutrientRow("Potassium",           avgOf { it.totalPotassium },          targets?.targetPotassium?.toFloat() ?: 3500f, "mg"),
        NutrientRow("Vitamin A",           avgOf { it.totalVitaminA },           targets?.targetVitaminA?.toFloat() ?: 900f,           "µg"),
        NutrientRow("Vitamin C",           avgOf { it.totalVitaminC },           targets?.targetVitaminC?.toFloat() ?: 90f,            "mg"),
        NutrientRow("Calcium",             avgOf { it.totalCalcium },            targets?.targetCalcium?.toFloat() ?: 1000f,          "mg"),
        NutrientRow("Iron",                avgOf { it.totalIron },               targets?.targetIron?.toFloat() ?: 18f,            "mg")
    )

    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NutrientsWeeklyChart(weekDaySummaries, weekDayLabels)
        NutrientTable(
            valueColumnHeader = "Avg",
            nutrients = nutrients
        )
    }
}

@Composable
private fun NutrientsWeeklyChart(
    summaries: List<DailyNutritionSummaryEntity?>,
    labels: List<String>
) {
    // Find the max calories to scale the bars
    val maxCals = summaries.maxOfOrNull { it?.totalCalories ?: 0f }?.coerceAtLeast(100f) ?: 100f
    
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp).fillMaxWidth()
        ) {
            androidx.compose.material3.Text(
                "Weekly Calorie Intake",
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = com.calorieko.app.ui.theme.DarkText
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = androidx.compose.ui.Alignment.Bottom
            ) {
                // Ignore the last label which is "Avg"
                val chartLabels = labels.take(7)
                chartLabels.forEachIndexed { i, label ->
                    val cals = summaries.getOrNull(i)?.totalCalories ?: 0f
                    val heightRatio = (cals / maxCals).coerceIn(0f, 1f)
                    
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(heightRatio)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(com.calorieko.app.ui.theme.CalorieKoGreen)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = label, // Mon, Tue, etc.
                            fontSize = 11.sp,
                            color = com.calorieko.app.ui.theme.SubtleText
                        )
                    }
                }
            }
        }
    }
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
        "${left.toFormattedString()} %"
    } else {
        "${left.toFormattedString()} ${nutrient.unit}"
    }
    val goalText = nutrient.goal.toFormattedString()
    val progress = if (nutrient.goal > 0f) {
        (nutrient.total / nutrient.goal).coerceIn(0f, 1f)
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
                text = "~${nutrient.total.toFormattedString()}",
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

private fun Float.toFormattedString(): String {
    return String.format(java.util.Locale.US, "%,.1f", this)
}
