package com.calorieko.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorieko.app.ui.theme.CalorieKoGreen

@Composable
fun BottomNavigation(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp) // Slightly more padding
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icons matching dashboard.tsx
            NavUserItem("home", "Home", Icons.Default.Home, activeTab, onTabChange)
            NavUserItem("pantry", "Pantry", Icons.Default.MenuBook, activeTab, onTabChange)
            NavUserItem("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp, activeTab, onTabChange)
            NavUserItem("profile", "Profile", Icons.Default.Person, activeTab, onTabChange)
            NavUserItem("settings", "Settings", Icons.Default.Settings, activeTab, onTabChange)
        }
    }
}

@Composable
fun NavUserItem(
    id: String,
    label: String,
    icon: ImageVector,
    activeTab: String,
    onClick: (String) -> Unit
) {
    val isActive = activeTab == id
    val color = if (isActive) CalorieKoGreen else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick(id) }
            .padding(4.dp) // Touch target padding
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = color,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}