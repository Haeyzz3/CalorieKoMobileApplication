package com.calorieko.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen

@Composable
fun NutrientDisclaimerDialog(
    onDismiss: () -> Unit,
    onLearnMore: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "About Nutritional Data",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Nutritional values shown are approximate estimates derived from USDA FoodData Central, calculated from raw ingredient weights before cooking. Actual nutrient content may vary based on cooking method, local ingredient varieties, and natural differences between individual ingredients.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.DarkGray
                )
                if (onLearnMore != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onDismiss()
                            onLearnMore()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Learn how we calculate →",
                            color = CalorieKoGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = CalorieKoGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.DarkGray
    )
}

@Composable
fun NutrientDisclaimerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Gray
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Nutrition Disclaimer",
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}
