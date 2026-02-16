package com.calorieko.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Matches Figma: fixed bottom-0, bg-white, border-t, shadow-lg
@Composable
fun BottomNavigation(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    val tabs = listOf(
        NavTab("home", "Home", Icons.Default.Home),
        NavTab("pantry", "Pantry", Icons.AutoMirrored.Filled.MenuBook),
        NavTab("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp),
        NavTab("profile", "Profile", Icons.Default.Person),
        NavTab("settings", "Settings", Icons.Default.Settings)
    )

    Surface(
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 0.dp
    ) {
        // border-t border-gray-200
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE5E7EB)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    NavTabItem(
                        tab = tab,
                        isActive = activeTab == tab.id,
                        onClick = { onTabChange(tab.id) }
                    )
                }
            }
        }
    }
}

data class NavTab(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NavTabItem(
    tab: NavTab,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Animated color transitions (matching Figma: active=#4CAF50, inactive=gray-500)
    val iconColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF4CAF50) else Color(0xFF6B7280),
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "navIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF4CAF50) else Color(0xFF6B7280),
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "navLabelColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            fontSize = 12.sp,
            color = labelColor,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}