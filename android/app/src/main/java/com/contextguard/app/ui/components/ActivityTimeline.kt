package com.contextguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.ActLimeContainer
import com.contextguard.app.theme.AskAmber
import com.contextguard.app.theme.AskAmberContainer
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.StopCoralContainer
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange
import com.contextguard.app.theme.WarnOrangeContainer
import com.contextguard.app.ui.viewmodel.InterventionType

enum class EventOrigin {
    REAL_MONITORING,
    NOTIFICATION,
    USER_CHECK,
    DEMO_FIXTURE
}

data class TimelineEvent(
    val id: String,
    val sourceApp: String,
    val category: String,
    val origin: EventOrigin,
    val intervention: InterventionType,
    val riskScore: Float,
    val timestampMs: Long,
    val evidenceSummary: String,
    val candidateAction: String? = null,
    val latencyMs: Long? = null
)

/**
 * Refined, compact activity timeline.
 * Displays real monitoring events, user checks, and demo fixtures with clear differentiation.
 */
@Composable
fun ActivityTimeline(
    events: List<TimelineEvent>,
    isSetupRequired: Boolean,
    isModelReady: Boolean,
    onSetupClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (events.isEmpty()) {
            EmptyActivityView(
                isSetupRequired = isSetupRequired,
                isModelReady = isModelReady,
                onSetupClicked = onSetupClicked
            )
        } else {
            events.forEachIndexed { index, event ->
                ActivityTimelineRow(event = event)
                if (index < events.size - 1) {
                    HorizontalDivider(
                        color = ContourBorder.copy(alpha = 0.5f),
                        thickness = 0.75.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineRow(
    event: TimelineEvent,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val interventionColor = when (event.intervention) {
        InterventionType.ACT -> ActLime
        InterventionType.ASK -> AskAmber
        InterventionType.WARN -> WarnOrange
        InterventionType.STOP -> StopCoral
    }

    val interventionContainer = when (event.intervention) {
        InterventionType.ACT -> ActLimeContainer
        InterventionType.ASK -> AskAmberContainer
        InterventionType.WARN -> WarnOrangeContainer
        InterventionType.STOP -> StopCoralContainer
    }

    val (originLabel, originColor) = when (event.origin) {
        EventOrigin.REAL_MONITORING -> "MONITORED" to IonCyan
        EventOrigin.NOTIFICATION -> "NOTIFICATION" to ElectricViolet
        EventOrigin.USER_CHECK -> "MANUAL CHECK" to SignalLime
        EventOrigin.DEMO_FIXTURE -> "DEMO SCENARIO" to AskAmber
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedSurface.copy(alpha = 0.55f))
            .border(1.dp, if (isExpanded) ContourBorderActive else ContourBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row: App Name + Origin Badge + Intervention Pill + Relative Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Source App & Category
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.sourceApp,
                            color = SoftWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(originColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .border(0.6.dp, originColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = originLabel,
                                color = originColor,
                                fontSize = 8.5.sp,
                                fontFamily = TechnicalMono,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = event.category,
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Intervention Pill
                Box(
                    modifier = Modifier
                        .background(interventionContainer, RoundedCornerShape(6.dp))
                        .border(1.dp, interventionColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = event.intervention.name,
                        color = interventionColor,
                        fontSize = 11.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Relative Time
                Text(
                    text = formatRelativeTime(event.timestampMs),
                    color = SubtleText,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand details",
                    tint = SubtleText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expanded Evidence Summary
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(DeepSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "EVIDENCE RATIONALE",
                    color = IonCyan,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = event.evidenceSummary,
                    color = SoftWhite,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                HorizontalDivider(color = ContourBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (event.candidateAction != null) {
                        Text(
                            text = "ACTION: ${event.candidateAction}",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono
                        )
                    }
                    Text(
                        text = "RISK SCORE: ${"%.2f".format(event.riskScore)}",
                        color = interventionColor,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (event.latencyMs != null) {
                        Text(
                            text = "${event.latencyMs}ms",
                            color = SubtleText,
                            fontSize = 10.sp,
                            fontFamily = TechnicalMono
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Empty State line illustration of the Context Field.
 */
@Composable
fun EmptyActivityView(
    isSetupRequired: Boolean,
    isModelReady: Boolean,
    onSetupClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyContourLoop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EmptyContourRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepSurface.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .border(1.dp, ContourBorder, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Animated Line Illustration of the Context Field
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(90.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Outer faint contour line
                drawCircle(
                    color = ElectricViolet.copy(alpha = 0.25f),
                    radius = 38.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), rotation)
                    )
                )

                // Middle offset contour line
                drawCircle(
                    color = IonCyan.copy(alpha = 0.35f),
                    radius = 24.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), -rotation * 0.7f)
                    )
                )

                // Inner focal anchor
                drawCircle(
                    color = SignalLime.copy(alpha = 0.6f),
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Nothing to review yet.",
                color = SoftWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "ContextGuard operates silently in on-device memory. Live intercepted actions and manual checks will appear here with zero cloud transmission.",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        // Next Step / Limitation Guidance
        if (isSetupRequired) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, IonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Setup required",
                            tint = IonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Next step: Enable Monitoring",
                            color = SoftWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Turn on Screen or Notification Monitoring in Android Settings so ContextGuard can inspect risks before you act.",
                        color = MutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = onSetupClicked,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Configure Protection Permissions",
                            color = com.contextguard.app.theme.Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (!isModelReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "On-Device Model Notice: 37-feature extractor active. Heuristic safety rules protecting device.",
                    color = SubtleText,
                    fontSize = 10.5.sp,
                    fontFamily = TechnicalMono
                )
            }
        }
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val diffMs = System.currentTimeMillis() - timestampMs
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
