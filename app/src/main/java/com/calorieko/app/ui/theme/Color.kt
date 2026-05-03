package com.calorieko.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors
val CalorieKoGreen = Color(0xFF4CAF50)
val CalorieKoLightGreen = Color(0xFF66BB6A)
val CalorieKoDarkGreen = Color(0xFF388E3C)
val CalorieKoOrange = Color(0xFFFF9800)
val CalorieKoLightOrange = Color(0xFFFFB74D)
val CalorieKoDarkOrange = Color(0xFFF57C00)

// UI Surface & Text Colors
val IceGray = Color(0xFFF0F2F5)
val DarkText = Color(0xFF1F2937)
val SubtleText = Color(0xFF6B7280)
val DividerGray = Color(0xFFE5E7EB)

// Macro Nutrient Colors — Each must be visually distinct from ring colors (Eaten/Burned/Sodium)
val MacroProtein = Color(0xFF3B82F6)   // Royal Blue — industry standard for protein
val MacroCarbs = Color(0xFFF59E0B)     // Warm Amber — universally recognized "energy"
val MacroFat = Color(0xFF8B5CF6)       // Vivid Purple — distinct from Burned ring (red)

// Ring Colors — Each must be visually distinct from each other AND from macro bars
val RingEaten = Color(0xFF4CAF50)      // Green — matches CalorieKoGreen
val RingBurned = Color(0xFFEF5350)     // Coral Red — distinct from purple Fats
val RingSodium = Color(0xFF06B6D4)     // Cyan/Teal — distinct from amber Carbs

// Standard Colors
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)