package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElectricVioletSubtle
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono

enum class HomeNavTab {
    PROTECT,
    ACTIVITY,
    PRIVACY,
    SETTINGS
}

data class BottomNavItem(
    val tab: HomeNavTab,
    val label: String,
    val icon: ImageVector
)

/**
 * Carefully considered 4-item navigation bar:
 * - Protect
 * - Activity
 * - Privacy
 * - Settings
 *
 * Avoids bulky floating navigation bars; uses clean hairline geometry with animated selection indicators.
 */
@Composable
fun SpectralBottomNavigation(
    selectedTab: HomeNavTab,
    onTabSelected: (HomeNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        BottomNavItem(HomeNavTab.PROTECT, "Protect", Icons.Default.Security),
        BottomNavItem(HomeNavTab.ACTIVITY, "Activity", Icons.Default.History),
        BottomNavItem(HomeNavTab.PRIVACY, "Privacy", Icons.Default.Lock),
        BottomNavItem(HomeNavTab.SETTINGS, "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepSurface.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = ContourBorderActive.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = selectedTab == item.tab

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) SoftWhite else SubtleText,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "navIconTint"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) IonCyan else SubtleText,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "navLabelColor"
                )
                val containerBg by animateColorAsState(
                    targetValue = if (isSelected) ElectricVioletSubtle else Color.Transparent,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "navContainerBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) ContourBorderActive else Color.Transparent,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "navBorderColor"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(item.tab) }
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.label,
                            color = labelColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = if (isSelected) TechnicalMono else androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}
