package com.contextguard.app.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * ContextGuard Motion Token System (Spectral Signal Motion Language)
 *
 * Core Philosophy:
 * Motion communicates Attention, State Change, Evidence, Uncertainty,
 * User Choice, and System Readiness.
 *
 * Timing Hierarchies:
 * - Fast interaction feedback: 120-180ms
 * - Standard state transitions: 200-320ms
 * - Major visual transitions: 350-550ms
 * - Success settle: 500ms harmonic ease
 */
object MotionTokens {

    // Duration Tokens (milliseconds)
    const val DurationFeedbackFast = 150
    const val DurationStateStandard = 260
    const val DurationMajorTransition = 420
    const val DurationHarmonicSettle = 500

    // Easing Curves
    val EaseEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EaseStandard: Easing = FastOutSlowInEasing
    val EaseDecelerate: Easing = LinearOutSlowInEasing
    val EaseLinear: Easing = LinearEasing

    // Standard Animation Specs
    fun <T> fastFeedback(delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = DurationFeedbackFast, delayMillis = delayMillis, easing = EaseStandard)

    fun <T> standardTransition(delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = DurationStateStandard, delayMillis = delayMillis, easing = EaseEmphasized)

    fun <T> majorTransition(delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = DurationMajorTransition, delayMillis = delayMillis, easing = EaseEmphasized)

    fun <T> harmonicSettle(delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = DurationHarmonicSettle, delayMillis = delayMillis, easing = EaseDecelerate)

    // Tactile Spring for Precision Press
    fun <T> tactileSpring(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    /**
     * Checks if reduced motion is enabled at the system level via Settings.Global.
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * CompositionLocal providing reduced motion preference to Compose trees.
 */
val LocalReducedMotion = compositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isReducedMotion(): Boolean {
    val context = LocalContext.current
    return LocalReducedMotion.current || MotionTokens.isReducedMotionEnabled(context)
}
