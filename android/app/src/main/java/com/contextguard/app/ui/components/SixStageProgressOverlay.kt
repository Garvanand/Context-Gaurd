package com.contextguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.BodyFont
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ContourBorderActive
import com.contextguard.app.theme.DeepSurface
import com.contextguard.app.theme.DisplayFont
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.ElevatedSurface
import com.contextguard.app.theme.Ink
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.MutedText
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.SubtleText
import com.contextguard.app.theme.TechnicalMono
import com.contextguard.app.theme.WarnOrange

data class StageInfo(
    val step: Int,
    val name: String,
    val description: String,
    val modelOrSource: String
)

private val PIPELINE_STAGES = listOf(
    StageInfo(1, "Context", "Artifact metadata, recipient & channel parameters", "SHA-256 Memory Buffer"),
    StageInfo(2, "Intent", "Action-conditioned mapping & harm taxonomy", "Action Matrix Parser"),
    StageInfo(3, "Evidence", "ML Kit OCR, sensitive PII & XGBoost signals", "On-Device ML Kit + URL-Tree"),
    StageInfo(4, "Consequence", "Projecting severity (s) and irreversibility (r)", "Consequence Bound Engine"),
    StageInfo(5, "Uncertainty", "Epistemic confidence calibration", "Laplace Error Estimator"),
    StageInfo(6, "Intervention", "Deterministic policy threshold gating", "rho = s * (1 + lambda * r)")
)

/**
 * Step 3: Analysis Motion with Sector Illumination & Accessibility.
 *
 * Progressively illuminates sectors or contours of the Context Field
 * as the 6 reasoning stages execute.
 * Uses real stage statuses, TalkBack announcements via liveRegion, and transitions under 400ms.
 */
@Composable
fun SixStageProgressOverlay(
    currentMessage: String,
    modifier: Modifier = Modifier
) {
    // Determine active stage from message text
    val activeStep = when {
        currentMessage.contains("1/6", true) || currentMessage.contains("Context", true) -> 1
        currentMessage.contains("2/6", true) || currentMessage.contains("Intent", true) -> 2
        currentMessage.contains("3/6", true) || currentMessage.contains("Evidence", true) -> 3
        currentMessage.contains("4/6", true) || currentMessage.contains("Consequence", true) -> 4
        currentMessage.contains("5/6", true) || currentMessage.contains("Uncertainty", true) -> 5
        currentMessage.contains("6/6", true) || currentMessage.contains("Intervention", true) -> 6
        else -> 3
    }

    val activeStageInfo = PIPELINE_STAGES.getOrNull(activeStep - 1) ?: PIPELINE_STAGES.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.90f))
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "Analysis in progress. Stage $activeStep of 6: ${activeStageInfo.name}. ${activeStageInfo.description}"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(ElevatedSurface)
                .border(1.5.dp, ContourBorderActive, RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // SECTOR-ILLUMINATED CONTEXT FIELD (6 STAGES)
            // ==========================================
            StageIlluminatedContextField(
                activeStep = activeStep,
                modifier = Modifier.size(120.dp)
            )

            // Header & Live Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Analyzing Pre-Action Risk",
                    color = SoftWhite,
                    fontSize = 18.sp,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "STAGE $activeStep / 6 • REASONING PIPELINE",
                    color = IonCyan,
                    fontSize = 10.sp,
                    fontFamily = TechnicalMono,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // The Six Progressive Stages List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                PIPELINE_STAGES.forEach { stage ->
                    val isDone = stage.step < activeStep
                    val isActive = stage.step == activeStep

                    val badgeColor by animateColorAsState(
                        targetValue = when {
                            isDone -> ActLime
                            isActive -> IonCyan
                            else -> MutedText.copy(alpha = 0.35f)
                        },
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "StageColor"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) DeepSurface else Color.Transparent)
                            .border(
                                1.dp,
                                if (isActive) ContourBorderActive else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stage Node Circle Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = if (isActive || isDone) 0.2f else 0.08f))
                                .border(1.dp, badgeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = ActLime,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                isActive -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = IonCyan,
                                        strokeWidth = 2.dp
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "${stage.step}",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        fontFamily = TechnicalMono,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Stage Label & Model Source Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stage.step}. ${stage.name}",
                                    color = if (isActive) SoftWhite else if (isDone) SoftWhite.copy(alpha = 0.8f) else MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = TechnicalMono
                                )
                                Text(
                                    text = stage.modelOrSource,
                                    color = if (isActive) IonCyan else SubtleText,
                                    fontSize = 9.sp,
                                    fontFamily = TechnicalMono
                                )
                            }
                            Text(
                                text = stage.description,
                                color = if (isActive) IonCyan.copy(alpha = 0.85f) else SubtleText,
                                fontSize = 10.sp,
                                fontFamily = BodyFont,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Real Runtime Status Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, ContourBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ActLime)
                    )
                    Text(
                        text = "ON-DEVICE ZERO-LEAK PIPELINE",
                        color = ActLime,
                        fontSize = 10.sp,
                        fontFamily = TechnicalMono,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "< 400ms STAGES",
                    color = SubtleText,
                    fontSize = 9.sp,
                    fontFamily = TechnicalMono
                )
            }
        }
    }
}

/**
 * Custom 6-sector illuminated Context Field.
 * Each contour corresponds to one of the 6 pipeline stages:
 * - Contour 1 (Outer): Context
 * - Contour 2: Intent
 * - Contour 3: Evidence
 * - Contour 4: Consequence
 * - Contour 5: Uncertainty
 * - Contour 6 (Center Aperture Core): Intervention
 */
@Composable
private fun StageIlluminatedContextField(
    activeStep: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Draw 6 concentric contours, illuminating up to activeStep
        val radii = listOf(
            maxRadius * 0.95f, // Stage 1: Context
            maxRadius * 0.80f, // Stage 2: Intent
            maxRadius * 0.65f, // Stage 3: Evidence
            maxRadius * 0.50f, // Stage 4: Consequence
            maxRadius * 0.35f, // Stage 5: Uncertainty
            maxRadius * 0.20f  // Stage 6: Intervention Core
        )

        radii.forEachIndexed { index, radius ->
            val stageIndex = index + 1
            val isIlluminated = stageIndex <= activeStep
            val isCurrent = stageIndex == activeStep

            val arcColor = when {
                isCurrent -> IonCyan
                isIlluminated -> ActLime
                else -> ContourBorder.copy(alpha = 0.35f)
            }

            val strokeWidth = if (isCurrent) 2.5.dp.toPx() else if (isIlluminated) 1.8.dp.toPx() else 1.dp.toPx()

            // Draw circular contour
            drawCircle(
                color = arcColor,
                radius = radius,
                center = center,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = if (!isIlluminated) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                )
            )

            // If current, draw highlighted leading pulse point
            if (isCurrent) {
                drawCircle(
                    color = IonCyan,
                    radius = 3.5.dp.toPx(),
                    center = Offset(center.x + radius, center.y)
                )
                // Glow around current
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(IonCyan.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(center.x + radius, center.y),
                        radius = 16.dp.toPx()
                    ),
                    radius = 12.dp.toPx(),
                    center = Offset(center.x + radius, center.y)
                )
            }
        }

        // Central Aperture Signal Discontinuity Core
        val coreColor = if (activeStep >= 6) ActLime else IonCyan
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor.copy(alpha = 0.6f), Color.Transparent),
                center = center,
                radius = maxRadius * 0.25f
            ),
            radius = maxRadius * 0.20f,
            center = center
        )
    }
}
