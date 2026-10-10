package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange

/**
 * Representation of an individual protection capability.
 */
data class CapabilityStatus(
    val title: String,
    val detail: String,
    val isActive: Boolean,
    val isPaused: Boolean,
    val icon: ImageVector,
    val onClick: () -> Unit = {}
)

/**
 * Active Protection Visualization:
 * Displays the animated Context Field as the central visual anchor with subtle,
 * elegant connection lines leading into it from the three real capabilities:
 * 1. Screen Monitoring
 * 2. Notification Monitoring
 * 3. Local Risk Analysis
 *
 * When a service is disabled, its connection visibly becomes inactive (dashed, dimmed).
 */
@Composable
fun ProtectionCapabilityConnector(
    fieldMode: ContextFieldMode,
    screenCapability: CapabilityStatus,
    notificationCapability: CapabilityStatus,
    localModelCapability: CapabilityStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepSurface.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .border(1.dp, ContourBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Satellite: Local Risk Analysis
        CapabilityBadge(
            capability = localModelCapability,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Center Area: Animated Context Field with dynamic connection lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            // Visual connection lines between satellites and Context Field
            ConnectionLinesCanvas(
                isScreenActive = screenCapability.isActive && !screenCapability.isPaused,
                isNotificationActive = notificationCapability.isActive && !notificationCapability.isPaused,
                isLocalModelActive = localModelCapability.isActive,
                modifier = Modifier.matchParentSize()
            )

            // The signature Context Field
            ContextField(
                mode = fieldMode,
                size = 150.dp,
                showApertureCore = true
            )
        }

        // Bottom Satellite Row: Screen Guard (Left) & Notification Guard (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CapabilityBadge(
                capability = screenCapability,
                modifier = Modifier.weight(1f)
            )
            CapabilityBadge(
                capability = notificationCapability,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectionLinesCanvas(
    isScreenActive: Boolean,
    isNotificationActive: Boolean,
    isLocalModelActive: Boolean,
    modifier: Modifier = Modifier
) {
    val screenColor by animateColorAsState(
        targetValue = if (isScreenActive) IonCyan else ContourBorder.copy(alpha = 0.35f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "screenConnColor"
    )
    val notifColor by animateColorAsState(
        targetValue = if (isNotificationActive) ElectricViolet else ContourBorder.copy(alpha = 0.35f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "notifConnColor"
    )
    val modelColor by animateColorAsState(
        targetValue = if (isLocalModelActive) SignalLime else ContourBorder.copy(alpha = 0.35f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "modelConnColor"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val fieldRadius = 60.dp.toPx()

        // 1. Top connection: Local Risk Analysis into top aperture
        val topStart = Offset(cx, 4.dp.toPx())
        val topEnd = Offset(cx, cy - fieldRadius)
        val pathTop = Path().apply {
            moveTo(topStart.x, topStart.y)
            lineTo(topEnd.x, topEnd.y)
        }
        drawPath(
            path = pathTop,
            color = modelColor,
            style = Stroke(
                width = if (isLocalModelActive) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (isLocalModelActive) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                cap = StrokeCap.Round
            )
        )
        // Anchor nodes
        drawCircle(
            color = modelColor,
            radius = if (isLocalModelActive) 3.5.dp.toPx() else 2.5.dp.toPx(),
            center = topEnd
        )

        // 2. Bottom-left connection: Screen Monitoring
        val leftStart = Offset(size.width * 0.25f, size.height - 4.dp.toPx())
        val leftEnd = Offset(cx - fieldRadius * 0.707f, cy + fieldRadius * 0.707f)
        val pathLeft = Path().apply {
            moveTo(leftStart.x, leftStart.y)
            cubicTo(
                leftStart.x, cy + 20.dp.toPx(),
                leftEnd.x - 10.dp.toPx(), leftEnd.y + 10.dp.toPx(),
                leftEnd.x, leftEnd.y
            )
        }
        drawPath(
            path = pathLeft,
            color = screenColor,
            style = Stroke(
                width = if (isScreenActive) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (isScreenActive) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                cap = StrokeCap.Round
            )
        )
        drawCircle(
            color = screenColor,
            radius = if (isScreenActive) 3.5.dp.toPx() else 2.5.dp.toPx(),
            center = leftEnd
        )

        // 3. Bottom-right connection: Notification Monitoring
        val rightStart = Offset(size.width * 0.75f, size.height - 4.dp.toPx())
        val rightEnd = Offset(cx + fieldRadius * 0.707f, cy + fieldRadius * 0.707f)
        val pathRight = Path().apply {
            moveTo(rightStart.x, rightStart.y)
            cubicTo(
                rightStart.x, cy + 20.dp.toPx(),
                rightEnd.x + 10.dp.toPx(), rightEnd.y + 10.dp.toPx(),
                rightEnd.x, rightEnd.y
            )
        }
        drawPath(
            path = pathRight,
            color = notifColor,
            style = Stroke(
                width = if (isNotificationActive) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (isNotificationActive) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                cap = StrokeCap.Round
            )
        )
        drawCircle(
            color = notifColor,
            radius = if (isNotificationActive) 3.5.dp.toPx() else 2.5.dp.toPx(),
            center = rightEnd
        )
    }
}

@Composable
private fun CapabilityBadge(
    capability: CapabilityStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        capability.isActive && !capability.isPaused -> SignalLime
        capability.isActive && capability.isPaused -> WarnOrange
        else -> SubtleText
    }

    val statusText = when {
        capability.isActive && !capability.isPaused -> "ACTIVE"
        capability.isActive && capability.isPaused -> "PAUSED"
        else -> "STANDBY"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(
                width = 1.dp,
                color = if (capability.isActive && !capability.isPaused) ContourBorderActive else ContourBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = capability.onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DeepSurface)
                .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = capability.icon,
                contentDescription = capability.title,
                tint = if (capability.isActive && !capability.isPaused) SoftWhite else SubtleText,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = capability.title,
                    color = SoftWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Text(
                text = capability.detail,
                color = MutedText,
                fontSize = 10.sp,
                fontFamily = TechnicalMono,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .border(0.75.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 9.sp,
                fontFamily = TechnicalMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
