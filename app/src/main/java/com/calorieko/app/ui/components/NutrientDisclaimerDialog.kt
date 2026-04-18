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
    onDismiss: () -> Unit
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
            Text(
                text = "While weight measurements are precise, nutritional values are estimates based on standard food databases and natural variations in ingredients. These figures should be used as a general guide for your daily intake.",
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
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
