package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.Midnight
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SoftWhite

/**
 * Tactile Primary Action Component.
 *
 * Implements:
 * 1. Spring-damped physical compression on press (0.96 scale).
 * 2. Subtle, cinematic Violet-to-Cyan radiant light shift on interaction.
 * 3. High-legibility editorial typography and informative micro-copy.
 */
@Composable
fun TactilePrimaryButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press scale compression
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactileScale"
    )

    // Dynamic Spectral Light Shift: Violet -> Cyan on interaction
    val startGradientColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF5DF0FF) else if (isPrimary) ElectricViolet else ElevatedSurface,
        animationSpec = tween(180),
        label = "lightShiftStart"
    )
    val endGradientColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF2FD5F6) else if (isPrimary) Color(0xFF6E4FFF) else DeepSurface,
        animationSpec = tween(180),
        label = "lightShiftEnd"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) IonCyan else if (isPrimary) ContourBorderActive else ContourBorder,
        animationSpec = tween(180),
        label = "lightShiftBorder"
    )

    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(startGradientColor, endGradientColor))
    } else {
        Brush.horizontalGradient(listOf(ElevatedSurface, DeepSurface))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPrimary) Midnight.copy(alpha = 0.4f) else DeepSurface
                    )
                    .border(
                        1.dp,
                        if (isPrimary) SoftWhite.copy(alpha = 0.35f) else ContourBorderActive,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isPrimary) SoftWhite else IonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SoftWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = subtitle,
                    color = if (isPrimary) SoftWhite.copy(alpha = 0.85f) else MutedText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
