package com.contextguard.app.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Dark Theme Palette (Graphite & Glass Surfaces)
val BackgroundDark = Color(0xFF090D14)
val BackgroundObsidian = Color(0xFF06090E)
val SurfaceDark = Color(0xFF111722)
val SurfaceCard = Color(0xFF161E2E)
val SurfaceGlass = Color(0xCC1A2336)
val SurfaceGlassHigh = Color(0xE6202B42)
val SurfaceBorder = Color(0xFF26334A)
val SurfaceBorderSubtle = Color(0x2638BDF8)

// Brand & Accent Colors
val CyanAccent = Color(0xFF00E5FF)
val CyanAccentGlow = Color(0xFF38BDF8)
val CyanAccentMuted = Color(0xFF00838F)
val IndigoAccent = Color(0xFF6366F1)
val VioletAccent = Color(0xFF8B5CF6)

// Safety Intervention Colors & Backgrounds
val ActGreen = Color(0xFF00E676)
val ActGreenDark = Color(0xFF1B5E20)
val ActGreenContainer = Color(0x1F00E676)
val ActGreenText = Color(0xFF69F0AE)

val AskYellow = Color(0xFFFFD600)
val AskYellowDark = Color(0xFFF57F17)
val AskYellowContainer = Color(0x1FFFD600)
val AskYellowText = Color(0xFFFFEA00)

val WarnOrange = Color(0xFFFF9100)
val WarnOrangeDark = Color(0xFFE65100)
val WarnOrangeContainer = Color(0x1FFF9100)
val WarnOrangeText = Color(0xFFFFB74D)

val StopRed = Color(0xFFFF1744)
val StopRedDark = Color(0xFFB71C1C)
val StopRedContainer = Color(0x28FF1744)
val StopRedText = Color(0xFFFF5252)

// Typography & State
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

val DividerColor = Color(0xFF1E293B)

// Gradients
val HeroGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF090D14))
)

val CardGlowGradient = Brush.horizontalGradient(
    colors = listOf(Color(0x1A00E5FF), Color(0x086366F1))
)

val AccentGradient = Brush.horizontalGradient(
    colors = listOf(CyanAccent, IndigoAccent)
)
