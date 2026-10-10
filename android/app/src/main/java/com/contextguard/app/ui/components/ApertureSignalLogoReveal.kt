package com.contextguard.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.contextguard.app.theme.ElectricViolet
import com.contextguard.app.theme.IonCyan
import com.contextguard.app.theme.SignalLime
import com.contextguard.app.theme.SoftWhite
import kotlinx.coroutines.launch

/**
 * ContextGuard Aperture Signal: Engineered Logo-Reveal Animation.
 *
 * CHOREOGRAPHY SPECIFICATION:
 * 1. Signal Ingestion (0-650ms): Offset contours draw in along concentric arcs.
 *    - Wing Alpha (Electric Violet) sweeps 0° -> 270°.
 *    - Wing Beta (Ion Cyan) sweeps 0° -> 240° with a 150ms phase delay.
 * 2. Approach Alignment (600-1100ms):
 *    - Contours rotate toward alignment, decelerating into intentional equilibrium,
 *      preserving the deliberate 45° action-gating discontinuity gap.
 * 3. Discontinuity Gate & Core Lock-In (1100-1600ms):
 *    - Central action-gating focal core and observer anchor lock in with an engineered spring settle.
 *
 * Used exclusively during onboarding (WelcomeScreen) and deliberate brand introductions.
 */
@Composable
fun ApertureSignalLogoReveal(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    autoPlay: Boolean = true,
    onRevealFinished: () -> Unit = {}
) {
    // Animation drivers
    val alphaSweepProgress = remember { Animatable(0f) }
    val betaSweepProgress = remember { Animatable(0f) }
    val alphaRotation = remember { Animatable(18f) }
    val betaRotation = remember { Animatable(-22f) }
    val coreScale = remember { Animatable(0f) }
    val coreAlpha = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    suspend fun playReveal() {
        // Reset state
        alphaSweepProgress.snapTo(0f)
        betaSweepProgress.snapTo(0f)
        alphaRotation.snapTo(18f)
        betaRotation.snapTo(-22f)
        coreScale.snapTo(0f)
        coreAlpha.snapTo(0f)

        // Stage 1: Contour Draw-in (Offset intake signals)
        val alphaDrawJob = scope.launch {
            alphaSweepProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        val betaDrawJob = scope.launch {
            kotlinx.coroutines.delay(120)
            betaSweepProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: Approach Alignment (Engineering convergence)
        kotlinx.coroutines.delay(550)
        val alphaAlignJob = scope.launch {
            alphaRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
        }
        val betaAlignJob = scope.launch {
            betaRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
        }

        // Stage 3: Core Lock-in & Anchor Point Settle
        kotlinx.coroutines.delay(450)
        coreAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350)
        )
        coreScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        alphaDrawJob.join()
        betaDrawJob.join()
        alphaAlignJob.join()
        betaAlignJob.join()

        onRevealFinished()
    }

    LaunchedEffect(autoPlay) {
        if (autoPlay) {
            playReveal()
        } else {
            // Settle directly into final resting state
            alphaSweepProgress.snapTo(1f)
            betaSweepProgress.snapTo(1f)
            alphaRotation.snapTo(0f)
            betaRotation.snapTo(0f)
            coreScale.snapTo(1f)
            coreAlpha.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch { playReveal() }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val s = minOf(this.size.width, this.size.height) / 100f
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // -------------------------------------------------------------
            // Wing Alpha: Outer Signal Contour (Electric Violet Gradient)
            // Mean Radius = 33, Stroke Width = 10, Total Sweep = 270°
            // -------------------------------------------------------------
            val alphaMidR = 33f * s
            val alphaStrokeWidth = 10f * s
            val alphaSweep = 270f * alphaSweepProgress.value

            if (alphaSweep > 0.1f) {
                rotate(degrees = alphaRotation.value, pivot = Offset(cx, cy)) {
                    val brushAlpha = Brush.linearGradient(
                        colors = listOf(Color(0xFFA28DFF), ElectricViolet, Color(0xFF6E4FFF)),
                        start = Offset(cx - 38f * s, cy - 38f * s),
                        end = Offset(cx + 38f * s, cy + 38f * s)
                    )
                    drawArc(
                        brush = brushAlpha,
                        startAngle = -10f,
                        sweepAngle = alphaSweep,
                        useCenter = false,
                        topLeft = Offset(cx - alphaMidR, cy - alphaMidR),
                        size = Size(alphaMidR * 2f, alphaMidR * 2f),
                        style = Stroke(width = alphaStrokeWidth, cap = StrokeCap.Butt)
                    )
                }
            }

            // -------------------------------------------------------------
            // Wing Beta: Inner Offset Signal Contour (Ion Cyan Gradient)
            // Mean Radius = 19, Stroke Width = 8, Total Sweep = 240°
            // -------------------------------------------------------------
            val betaMidR = 19f * s
            val betaStrokeWidth = 8f * s
            val betaSweep = 240f * betaSweepProgress.value

            if (betaSweep > 0.1f) {
                rotate(degrees = betaRotation.value, pivot = Offset(cx, cy)) {
                    val brushBeta = Brush.linearGradient(
                        colors = listOf(Color(0xFF5BF0FF), IonCyan),
                        start = Offset(cx - 23f * s, cy - 23f * s),
                        end = Offset(cx + 23f * s, cy + 23f * s)
                    )
                    drawArc(
                        brush = brushBeta,
                        startAngle = 50f,
                        sweepAngle = betaSweep,
                        useCenter = false,
                        topLeft = Offset(cx - betaMidR, cy - betaMidR),
                        size = Size(betaMidR * 2f, betaMidR * 2f),
                        style = Stroke(width = betaStrokeWidth, cap = StrokeCap.Butt)
                    )
                }
            }

            // -------------------------------------------------------------
            // Central Aperture Focal Core (Action-Gating Quadrant)
            // Radius = 9 * scale, Angle = 135° to 225° (90° sweep)
            // -------------------------------------------------------------
            if (coreAlpha.value > 0.01f && coreScale.value > 0.01f) {
                val coreR = 9f * s * coreScale.value
                drawArc(
                    color = SignalLime.copy(alpha = coreAlpha.value),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(cx - coreR, cy - coreR),
                    size = Size(coreR * 2f, coreR * 2f)
                )

                // ---------------------------------------------------------
                // Observer Anchor Pinpoint
                // Radius = 2.8 * scale, Center offset at (+5, +5)
                // ---------------------------------------------------------
                val anchorR = 2.8f * s * coreScale.value
                val anchorCenter = Offset(cx + 5f * s, cy + 5f * s)
                drawCircle(
                    color = SoftWhite.copy(alpha = coreAlpha.value),
                    radius = anchorR,
                    center = anchorCenter
                )
            }
        }
    }
}
