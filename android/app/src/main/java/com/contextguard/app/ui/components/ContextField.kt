package com.contextguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.contextguard.app.theme.MotionTokens
import com.contextguard.app.theme.isReducedMotion
import com.contextguard.app.theme.ActLime
import com.contextguard.app.theme.AskAmber
import com.contextguard.app.theme.ContourBorder
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import com.contextguard.app.theme.StopCoral
import com.contextguard.app.theme.WarnOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual states for ContextGuard's signature visual motif: The Context Field.
 */
enum class ContextFieldMode {
    IDLE,
    PROTECTION_ACTIVE,
    ANALYZING,
    ACT,
    ASK,
    WARN,
    STOP
}

/**
 * THE CONTEXT FIELD
 *
 * An original animated visual motif representing a signal passing through a field of uncertainty.
 * - Several fine, concentric, offset contours surround a central aperture.
 * - Small, restrained signal points move along the contours.
 * - Geometry responds dynamically to actual runtime safety state:
 *     IDLE: Quiet, nearly static breathing field.
 *     PROTECTION_ACTIVE: Subtle synchronized orbit indicating live monitoring.
 *     ANALYZING: Directional wavefront sweeping through contours as stages execute.
 *     ASK: Softly amber field with a visible unresolved angular gap.
 *     WARN: Focused orange signal highlighting specific evidence sector.
 *     STOP: Geometry contracts around a clear coral focal core in defensive lockdown.
 *     ACT: Symmetrical alignment settled into a calm harmonic lime accent.
 */
@Composable
fun ContextField(
    mode: ContextFieldMode,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    showApertureCore: Boolean = true,
    focusedEvidenceAngle: Float? = null
) {
    val reducedMotion = isReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "ContextFieldLoop")

    // Continuous orbital rotation angle (0 to 360 degrees) - paused if reduced motion
    val animatedOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    ContextFieldMode.ANALYZING -> 2400
                    ContextFieldMode.PROTECTION_ACTIVE -> 14000
                    ContextFieldMode.IDLE -> 36000
                    else -> 28000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    val orbitAngle = if (reducedMotion) 0f else animatedOrbitAngle

    // Breathing pulse for aperture and contour glow
    val animatedBreathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    val breathingPulse = if (reducedMotion) 1.0f else animatedBreathingPulse

    // Sweep phase for active analysis sweep
    val animatedAnalysisSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AnalysisSweep"
    )

    val analysisSweep = if (reducedMotion) 90f else animatedAnalysisSweep

    // Color interpolation across states
    val targetPrimaryColor = when (mode) {
        ContextFieldMode.IDLE -> ElectricViolet
        ContextFieldMode.PROTECTION_ACTIVE -> ElectricViolet
        ContextFieldMode.ANALYZING -> IonCyan
        ContextFieldMode.ACT -> ActLime
        ContextFieldMode.ASK -> AskAmber
        ContextFieldMode.WARN -> WarnOrange
        ContextFieldMode.STOP -> StopCoral
    }

    val targetSecondaryColor = when (mode) {
        ContextFieldMode.IDLE -> ContourBorder
        ContextFieldMode.PROTECTION_ACTIVE -> IonCyan
        ContextFieldMode.ANALYZING -> ElectricViolet
        ContextFieldMode.ACT -> SignalLime
        ContextFieldMode.ASK -> AskAmber.copy(alpha = 0.4f)
        ContextFieldMode.WARN -> WarnOrange.copy(alpha = 0.5f)
        ContextFieldMode.STOP -> StopCoral.copy(alpha = 0.6f)
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = if (reducedMotion) tween(0) else tween(MotionTokens.DurationMajorTransition),
        label = "PrimaryColor"
    )

    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = if (reducedMotion) tween(0) else tween(MotionTokens.DurationMajorTransition),
        label = "SecondaryColor"
    )

    // Contraction scale: STOP contracts inward, ACT expands slightly into symmetry
    val contractionScale by animateFloatAsState(
        targetValue = when (mode) {
            ContextFieldMode.STOP -> 0.78f
            ContextFieldMode.ACT -> 1.05f
            ContextFieldMode.ASK -> 0.94f
            ContextFieldMode.WARN -> 0.96f
            ContextFieldMode.ANALYZING -> 1.02f
            else -> 1.00f
        },
        animationSpec = if (reducedMotion) tween(0) else tween(MotionTokens.DurationMajorTransition, easing = FastOutSlowInEasing),
        label = "ContractionScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cWidth = this.size.width
            val cHeight = this.size.height
            val center = Offset(cWidth / 2f, cHeight / 2f)
            val maxRadius = (minOf(cWidth, cHeight) / 2f) * 0.92f

            // Radii multipliers for 5 concentric contours
            val baseRadiiFactors = floatArrayOf(0.24f, 0.42f, 0.60f, 0.78f, 0.96f)

            // 1. Aperture Soft Radial Ambient Glow
            val apertureGlowRadius = maxRadius * 0.45f * breathingPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (mode == ContextFieldMode.STOP) 0.35f else 0.22f),
                        primaryColor.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = apertureGlowRadius
                ),
                radius = apertureGlowRadius,
                center = center
            )

            // 2. Draw Concentric Offset Contours
            for (i in baseRadiiFactors.indices) {
                val factor = baseRadiiFactors[i]
                val contourRadius = maxRadius * factor * contractionScale

                // Asymmetrical contour offset based on depth index
                val xOffset = when (mode) {
                    ContextFieldMode.STOP, ContextFieldMode.ACT -> 0f
                    else -> sin(Math.toRadians((orbitAngle * (i + 1) * 0.3).toDouble())).toFloat() * (4f - i)
                }
                val yOffset = when (mode) {
                    ContextFieldMode.STOP, ContextFieldMode.ACT -> 0f
                    else -> cos(Math.toRadians((orbitAngle * (i + 1) * 0.3).toDouble())).toFloat() * (3f - i)
                }
                val contourCenter = Offset(center.x + xOffset, center.y + yOffset)

                val strokeWidth = when (i) {
                    0 -> 1.8f
                    1 -> 1.5f
                    2 -> 1.3f
                    3 -> 1.1f
                    else -> 0.9f
                }

                when (mode) {
                    ContextFieldMode.ASK -> {
                        // ASK: Visible unresolved gap in contours (e.g. 55-degree missing arc)
                        val startAngle = (orbitAngle * 0.5f + i * 35f) % 360f
                        val sweepAngle = 360f - 55f // 55 degree gap symbolizing missing context
                        drawArc(
                            color = if (i == 1 || i == 2) primaryColor.copy(alpha = 0.75f) else ContourBorder.copy(alpha = 0.45f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(contourCenter.x - contourRadius, contourCenter.y - contourRadius),
                            size = Size(contourRadius * 2, contourRadius * 2),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = if (i == 4) PathEffect.dashPathEffect(floatArrayOf(6f, 8f)) else null
                            )
                        )
                    }

                    ContextFieldMode.WARN -> {
                        // WARN: Focused orange highlighted sector
                        drawCircle(
                            color = ContourBorder.copy(alpha = 0.4f),
                            radius = contourRadius,
                            center = contourCenter,
                            style = Stroke(width = strokeWidth)
                        )
                        // Intense evidence arc
                        val evidenceSectorStart = (i * 45f + 180f) % 360f
                        drawArc(
                            color = primaryColor.copy(alpha = 0.85f),
                            startAngle = evidenceSectorStart,
                            sweepAngle = 75f,
                            useCenter = false,
                            topLeft = Offset(contourCenter.x - contourRadius, contourCenter.y - contourRadius),
                            size = Size(contourRadius * 2, contourRadius * 2),
                            style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                        )
                    }

                    ContextFieldMode.STOP -> {
                        // STOP: Contracted dense defensive rings
                        drawCircle(
                            color = if (i <= 2) primaryColor.copy(alpha = 0.85f - (i * 0.2f)) else ContourBorder.copy(alpha = 0.35f),
                            radius = contourRadius,
                            center = contourCenter,
                            style = Stroke(
                                width = if (i == 0) strokeWidth * 2.4f else strokeWidth,
                                pathEffect = if (i == 3) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
                            )
                        )
                    }

                    ContextFieldMode.ACT -> {
                        // ACT: Harmonious aligned concentric lime rings
                        drawCircle(
                            color = if (i == 1 || i == 3) primaryColor.copy(alpha = 0.75f) else ContourBorder.copy(alpha = 0.5f),
                            radius = contourRadius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    ContextFieldMode.ANALYZING -> {
                        // ANALYZING: Directional sweep wave moving through contours
                        drawCircle(
                            color = ContourBorder.copy(alpha = 0.35f),
                            radius = contourRadius,
                            center = contourCenter,
                            style = Stroke(width = strokeWidth)
                        )
                        // Dynamic sweep arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColor.copy(alpha = 0.2f),
                                    primaryColor.copy(alpha = 0.9f),
                                    secondaryColor
                                ),
                                center = contourCenter
                            ),
                            startAngle = analysisSweep - 45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(contourCenter.x - contourRadius, contourCenter.y - contourRadius),
                            size = Size(contourRadius * 2, contourRadius * 2),
                            style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round)
                        )
                    }

                    else -> {
                        // IDLE & PROTECTION_ACTIVE: Fine concentric rings with quiet offset
                        val ringColor = if (i == 1 && mode == ContextFieldMode.PROTECTION_ACTIVE) {
                            secondaryColor.copy(alpha = 0.65f)
                        } else if (i == 2) {
                            primaryColor.copy(alpha = 0.45f)
                        } else {
                            ContourBorder.copy(alpha = 0.4f)
                        }

                        drawCircle(
                            color = ringColor,
                            radius = contourRadius,
                            center = contourCenter,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = if (i == 4) PathEffect.dashPathEffect(floatArrayOf(5f, 7f)) else null
                            )
                        )
                    }
                }

                // 3. Small, Restrained Signal Points Orbiting along the Contours
                val speedMultiplier = when (i) {
                    0 -> 1.0f
                    1 -> -0.7f
                    2 -> 1.3f
                    3 -> -0.9f
                    else -> 0.5f
                }

                val particleAngleDeg = when (mode) {
                    ContextFieldMode.STOP -> (i * 72f) // Stationary locked positions
                    ContextFieldMode.ACT -> (i * 72f + 45f) // Balanced nodes
                    ContextFieldMode.ANALYZING -> (analysisSweep * 1.2f + i * 40f)
                    else -> (orbitAngle * speedMultiplier + i * 65f)
                }

                val particleAngleRad = Math.toRadians(particleAngleDeg.toDouble())
                val particleX = contourCenter.x + (contourRadius * cos(particleAngleRad)).toFloat()
                val particleY = contourCenter.y + (contourRadius * sin(particleAngleRad)).toFloat()

                val particleRadius = when {
                    mode == ContextFieldMode.STOP && i == 0 -> 4.0f
                    mode == ContextFieldMode.ANALYZING -> 3.2f
                    i == 1 -> 2.8f
                    else -> 2.0f
                }

                val particleColor = when {
                    mode == ContextFieldMode.STOP -> StopCoral
                    mode == ContextFieldMode.ACT -> SignalLime
                    mode == ContextFieldMode.WARN -> WarnOrange
                    mode == ContextFieldMode.ASK -> AskAmber
                    i == 1 -> IonCyan
                    else -> ElectricViolet
                }

                // Draw Signal Point
                drawCircle(
                    color = particleColor,
                    radius = particleRadius,
                    center = Offset(particleX, particleY)
                )

                // Very small subtle particle glow
                drawCircle(
                    color = particleColor.copy(alpha = 0.35f),
                    radius = particleRadius * 2.2f,
                    center = Offset(particleX, particleY)
                )
            }

            // 3.5 Signature Animation 3: Evidence Focus Vector
            if (focusedEvidenceAngle != null) {
                val angleRad = Math.toRadians(focusedEvidenceAngle.toDouble())
                val targetRadius = maxRadius * 0.82f * contractionScale
                val targetX = center.x + (targetRadius * cos(angleRad)).toFloat()
                val targetY = center.y + (targetRadius * sin(angleRad)).toFloat()

                drawLine(
                    color = IonCyan.copy(alpha = 0.85f),
                    start = center,
                    end = Offset(targetX, targetY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = IonCyan,
                    radius = 4.5.dp.toPx(),
                    center = Offset(targetX, targetY)
                )

                drawCircle(
                    color = IonCyan.copy(alpha = 0.35f),
                    radius = 9.dp.toPx(),
                    center = Offset(targetX, targetY),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 4. Central Aperture Core
            if (showApertureCore) {
                val coreRadius = maxRadius * 0.14f * (if (mode == ContextFieldMode.STOP) 0.82f else 1.0f)
                // Aperture outer border
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = coreRadius,
                    center = center,
                    style = Stroke(width = 1.8f)
                )
                // Inner aperture lens
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.95f),
                            secondaryColor.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius * 0.85f
                    ),
                    radius = coreRadius * 0.85f,
                    center = center
                )
                // Center pinpoint
                drawCircle(
                    color = SoftWhite,
                    radius = 2.2f,
                    center = center
                )
            }
        }
    }
}
