package com.contextguard.app.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// CREATIVE DIRECTION: SPECTRAL SIGNAL
// Primary Palette Tokens
// ============================================================================
val Ink = Color(0xFF080A12)              // Deepest backdrop
val Midnight = Color(0xFF0D1120)         // Cinematic container background
val DeepSurface = Color(0xFF13192B)      // Sub-layer surface
val ElevatedSurface = Color(0xFF1A2238)  // Primary elevated card
val GlassSurface = Color(0xCC1A2238)     // 80% opacity glass panel
val GlassSurfaceSubtle = Color(0x8013192B)// 50% opacity subtle layer

// Brand Identifiers
val ElectricViolet = Color(0xFF8B70FF)   // Primary brand identity (Signal core)
val ElectricVioletGlow = Color(0x408B70FF)
val ElectricVioletMuted = Color(0xFF5B45C6)
val ElectricVioletSubtle = Color(0x1F8B70FF)

val IonCyan = Color(0xFF45E4FF)          // Detection, perception & evidence
val IonCyanGlow = Color(0x4045E4FF)
val IonCyanMuted = Color(0xFF1FA8C7)
val IonCyanSubtle = Color(0x1A45E4FF)

val SignalLime = Color(0xFFD8FF63)       // Success, resolution & verified states
val SignalLimeGlow = Color(0x40D8FF63)
val SignalLimeSubtle = Color(0x22D8FF63)

val SoftWhite = Color(0xFFF4F6FF)        // High-legibility editorial white
val MutedText = Color(0xFF9CA8C2)        // Secondary technical labels
val SubtleText = Color(0xFF5D6B88)       // Tertiary captions & timestamps
val ContourBorder = Color(0xFF232D48)    // Fine geometry contour lines
val ContourBorderActive = Color(0x668B70FF)// Active state contour highlight

// ============================================================================
// Intervention Color Palette (Semantic, Distinct Roles)
// ============================================================================
val ActLime = Color(0xFFC9F77A)          // ACT: Calm harmonic resolution
val ActLimeDark = Color(0xFF1E3812)
val ActLimeContainer = Color(0x24C9F77A)
val ActLimeText = Color(0xFFD8FF63)

val AskAmber = Color(0xFFFFD166)         // ASK: Unresolved ambiguity / gap
val AskAmberDark = Color(0xFF3D2E0B)
val AskAmberContainer = Color(0x24FFD166)
val AskAmberText = Color(0xFFFFE082)

val WarnOrange = Color(0xFFFFAA65)       // WARN: Focused evidence alert
val WarnOrangeDark = Color(0xFF3D200E)
val WarnOrangeContainer = Color(0x24FFAA65)
val WarnOrangeText = Color(0xFFFFCC99)

val StopCoral = Color(0xFFFF667D)        // STOP: Contracted protective barrier
val StopCoralDark = Color(0xFF3D0F18)
val StopCoralContainer = Color(0x28FF667D)
val StopCoralText = Color(0xFFFF94A4)

// ============================================================================
// Secondary Light Theme Tokens (Porcelain & Subtle Violet)
// ============================================================================
val PorcelainBg = Color(0xFFF5F7FC)
val PorcelainElevated = Color(0xFFFFFFFF)
val PorcelainSurface = Color(0xFFEAE8FB)
val PorcelainBorder = Color(0xFFD6D3F3)
val PorcelainTextPrimary = Color(0xFF080A12)
val PorcelainTextSecondary = Color(0xFF5E6A85)

// ============================================================================
// Compatibility Aliases (Seamless integration with existing screens)
// ============================================================================
val BackgroundDark = Ink
val BackgroundObsidian = Midnight
val SurfaceDark = DeepSurface
val SurfaceCard = ElevatedSurface
val SurfaceGlassHigh = ElevatedSurface
val SurfaceBorder = ContourBorder
val SurfaceBorderSubtle = ContourBorderActive

val CyanAccent = IonCyan
val CyanAccentGlow = IonCyanGlow
val CyanAccentMuted = IonCyanMuted
val IndigoAccent = ElectricViolet
val VioletAccent = ElectricViolet

val ActGreen = ActLime
val ActGreenDark = ActLimeDark
val ActGreenContainer = ActLimeContainer
val ActGreenText = ActLimeText

val AskYellow = AskAmber
val AskYellowDark = AskAmberDark
val AskYellowContainer = AskAmberContainer
val AskYellowText = AskAmberText

val SurfaceGlass = ElevatedSurface

val StopRed = StopCoral
val StopRedDark = StopCoralDark
val StopRedContainer = StopCoralContainer
val StopRedText = StopCoralText

val TextPrimary = SoftWhite
val TextSecondary = MutedText
val TextTertiary = SubtleText
val DividerColor = ContourBorder

// ============================================================================
// Spectral Signal Dynamic Gradients
// ============================================================================
val HeroGradient = Brush.verticalGradient(
    colors = listOf(Midnight, Ink)
)

val CardGlowGradient = Brush.horizontalGradient(
    colors = listOf(Color(0x1A8B70FF), Color(0x1045E4FF))
)

val AccentGradient = Brush.horizontalGradient(
    colors = listOf(ElectricViolet, IonCyan)
)

val SignalApertureGradient = Brush.radialGradient(
    colors = listOf(Color(0x338B70FF), Color(0x000D1120))
)

val SafeFieldGradient = Brush.horizontalGradient(
    colors = listOf(SignalLime, IonCyan)
)
