package com.calorieko.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen

/**
 * Static informational screen explaining how nutritional values are calculated.
 *
 * Accessible from:
 * - Settings → About → "How We Calculate" row
 * - NutrientDisclaimerDialog → "Learn more" link
 *
 * No ViewModel needed — all content is hardcoded informational text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowWeCalculateScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                TopAppBar(
                    title = {
                        Text(
                            "How We Calculate",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1F2937)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intro text
            Text(
                text = "This page explains exactly how CalorieKo computes the nutritional values you see throughout the app.",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // ── Section 1: Data Source (expanded by default) ──
            ExpandableSection(
                icon = Icons.Default.Storage,
                iconColor = Color(0xFF3B82F6),
                title = "Data Source",
                subtitle = "Where do the numbers come from?",
                initiallyExpanded = true
            ) {
                BodyText(
                    "All nutritional values in CalorieKo are sourced from the "
                )
                BoldBodyText("USDA FoodData Central (FDC)")
                BodyText(
                    " database, maintained by the United States Department of Agriculture. " +
                    "This is one of the most widely used and peer-reviewed food composition " +
                    "databases in the world."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BodyText(
                    "Each of the 88 ingredients in our system is individually mapped to a " +
                    "specific USDA FDC entry by its unique FDC ID, ensuring full traceability " +
                    "back to the original laboratory-analyzed data."
                )
            }

            // ── Section 2: Calculation Method ──
            ExpandableSection(
                icon = Icons.Default.Calculate,
                iconColor = CalorieKoGreen,
                title = "Calculation Method",
                subtitle = "How do we compute a dish's nutrition?"
            ) {
                BoldBodyText("For multi-ingredient dishes")
                BodyText(" (e.g., Sinigang, Menudo, Tinola):")
                Spacer(modifier = Modifier.height(6.dp))
                BodyText(
                    "Each dish has a defined recipe with exact ingredient weights in grams. " +
                    "The app calculates the total batch nutrition by summing each ingredient's contribution:"
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormulaText("nutrient = (ingredient_weight_g ÷ 100) × USDA_per_100g_value")
                Spacer(modifier = Modifier.height(8.dp))
                BodyText(
                    "The total is then divided by the number of servings to get a per-serving value. " +
                    "When you weigh your portion on the smart scale, the app proportionally scales " +
                    "the nutrients based on your actual cooked weight relative to the total batch weight."
                )
                Spacer(modifier = Modifier.height(12.dp))
                BoldBodyText("For single-ingredient dishes")
                BodyText(" (e.g., Egg Sunny Side Up, Chicken Wing):")
                Spacer(modifier = Modifier.height(6.dp))
                BodyText(
                    "Nutrients are taken directly from the corresponding USDA FDC entry for that " +
                    "food item, then scaled to the weighed portion."
                )
            }

            // ── Section 3: Key Assumptions ──
            ExpandableSection(
                icon = Icons.Default.Info,
                iconColor = Color(0xFFF59E0B),
                title = "Key Assumptions",
                subtitle = "What assumptions are made?"
            ) {
                BulletPoint(
                    title = "Raw-basis nutrients",
                    body = "All USDA values used are for raw, uncooked ingredients. Cooking can " +
                           "affect some nutrients — for example, vitamin C may decrease with prolonged heat exposure."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Yield factors",
                    body = "Each cooking method has an estimated weight-loss factor (e.g., frying " +
                           "reduces weight by ~20%, simmering by ~15%). This is used to relate your " +
                           "cooked portion weight back to the raw nutrient totals."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Portion conversions",
                    body = "When original recipes specify volumes (e.g., \"2 cups\"), these are " +
                           "converted to gram weights using standard density approximations for each ingredient."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Standard piece weights",
                    body = "Count-based portions (e.g., \"3 pcs eggs\") use standard weights " +
                           "(1 medium egg ≈ 50g, 1 galunggong ≈ 80g)."
                )
            }

            // ── Section 4: Limitations ──
            ExpandableSection(
                icon = Icons.Default.Visibility,
                iconColor = Color(0xFFEF4444),
                title = "Limitations",
                subtitle = "What can affect accuracy?"
            ) {
                BulletPoint(
                    title = "Cooking variations",
                    body = "The same dish cooked at different temperatures or durations will have " +
                           "different nutrient retention rates."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Ingredient differences",
                    body = "Filipino produce varieties may differ slightly from the USDA reference " +
                           "values, which are based on US market samples."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Portion estimation",
                    body = "Volume-to-weight conversions introduce an estimated ±5–15% margin compared " +
                           "to direct weighing."
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    title = "Natural variation",
                    body = "Two tomatoes from the same market can have naturally different vitamin C " +
                           "content. This is inherent to all food composition databases."
                )
            }

            // ── Section 5: Recipe Sources ──
            ExpandableSection(
                icon = Icons.Default.Description,
                iconColor = Color(0xFF8B5CF6),
                title = "Recipe Sources",
                subtitle = "Where do the recipes come from?"
            ) {
                BodyText("The original recipes for 17 dishes are based on the ")
                BoldBodyText("DOST-FNRI Pinggang Pinoy Menu Guide")
                BodyText(
                    ", a publication by the Philippine Department of Science and Technology – " +
                    "Food and Nutrition Research Institute. These recipe PDFs document the original " +
                    "ingredient lists and portions."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BodyText(
                    "You can view the original FNRI recipe PDF for applicable dishes on each " +
                    "dish's detail page in the Explore screen."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BodyText(
                    "Newer dishes added after the initial release follow the same methodology: " +
                    "ingredient lists are defined, mapped to USDA FDC entries, and computed using " +
                    "the same summation formula described above."
                )
            }

            // ── Footer: USDA Link ──
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CalorieKoGreen.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = CalorieKoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "USDA FoodData Central",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalorieKoGreen
                        )
                        Text(
                            text = "fdc.nal.usda.gov",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "The authoritative source for all ingredient nutritional data used in this app.",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Bottom padding
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Reusable Components ──

/**
 * Expandable card section with an icon, title, subtitle, and collapsible content.
 */
@Composable
private fun ExpandableSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            // Header (always visible, clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    // Thin divider
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = Color(0xFFE5E7EB)
                    ) {}
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

/** Standard body text in the content area. */
@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF4B5563),
        lineHeight = 21.sp
    )
}

/** Bold inline text for emphasis within body content. */
@Composable
private fun BoldBodyText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF374151),
        lineHeight = 21.sp
    )
}

/** Monospace formula display block. */
@Composable
private fun FormulaText(formula: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF3F4F6)
    ) {
        Text(
            text = formula,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF374151),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Bullet point with a bold title and regular body. */
@Composable
private fun BulletPoint(title: String, body: String) {
    Text(
        text = buildAnnotatedString {
            append("•  ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF374151))) {
                append("$title: ")
            }
            withStyle(SpanStyle(color = Color(0xFF4B5563))) {
                append(body)
            }
        },
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}
