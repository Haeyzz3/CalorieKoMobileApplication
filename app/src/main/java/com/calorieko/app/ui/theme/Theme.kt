package com.calorieko.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 1. Define Dark Mode Colors
private val DarkColorScheme = darkColorScheme(
    primary = CalorieKoLightGreen,
    secondary = CalorieKoLightOrange,
    tertiary = CalorieKoGreen
)

// 2. Define Light Mode Colors
private val LightColorScheme = lightColorScheme(
    primary = CalorieKoGreen,
    secondary = CalorieKoOrange,
    tertiary = CalorieKoLightGreen,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Black,
    onSurface = Black,
)

// --- RENAMED TO MATCH YOUR ACTIVITIES ---
@Composable
fun CalorieKoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}