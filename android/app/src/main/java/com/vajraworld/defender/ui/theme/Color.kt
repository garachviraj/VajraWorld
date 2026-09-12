package com.vajraworld.defender.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// VAJRAWORLD GUARDIAN — MASTER COLOR SYSTEM (Section 4)
// Executive Cyber Defense SOC / Light Theme Palette
// =====================================================================

// Base Palette (Executive Light Mode)
val Bg0 = Color(0xFFFFFFFF)          // Crisp pure white header, footer, topbar
val Bg1 = Color(0xFFF8FAFC)          // Soft modern cockpit background (slate-50)
val Surface0 = Color(0xFFFFFFFF)     // Primary card & panel surface (crisp white)
val Surface1 = Color(0xFFF1F5F9)     // Secondary container / elevated surface (slate-100)
val Surface2 = Color(0xFFE2E8F0)     // Interactive chip / badge / hover surface (slate-200)
val BorderColor = Color(0xFFCBD5E1)  // Clean technical hairline border (slate-300)
val BorderSubtle = Color(0xFFE2E8F0) // Soft divider / hairline separator (slate-200)

// High-Contrast Technical Typography (Executive Light Mode)
val TextPrimary = Color(0xFF0F172A)   // Deep slate-900 for high-contrast readability
val TextSecondary = Color(0xFF334155) // Slate-700 technical label / subtitle
val TextMuted = Color(0xFF64748B)     // Slate-500 timestamp / muted metadata
val TextWhite = Color(0xFFFFFFFF)

// Semantic Cyber Threat States (Vibrant on Light Surfaces)
val Healthy = Color(0xFF059669)       // Deep Emerald 600 nominal / protected state
val HealthyBg = Color(0xFFECFDF5)     // 100% visible soft mint background
val HealthyBorder = Color(0xFFA7F3D0) // Soft emerald border

val Info = Color(0xFF0284C7)          // Ocean/Brand Blue 600
val InfoBg = Color(0xFFF0F9FF)        // Soft sky blue background
val InfoBorder = Color(0xFFBAE6FD)    // Soft sky border

val Warning = Color(0xFFD97706)       // Amber 600 elevated caution
val WarningBg = Color(0xFFFFFBEB)     // Soft amber background
val WarningBorder = Color(0xFFFDE68A) // Soft amber border

val High = Color(0xFFEA580C)          // Orange 600 high severity
val HighBg = Color(0xFFFFF7ED)        // Soft orange background
val HighBorder = Color(0xFFFED7AA)    // Soft orange border

val Critical = Color(0xFFDC2626)      // Red 600 critical attack ruby red
val CriticalBg = Color(0xFFFEF2F2)    // Soft ruby background
val CriticalBorder = Color(0xFFFECACA)// Soft ruby border

// Radar Ring Geometry Accents (Light Mode)
val RadarRingColor = Color(0xFFCBD5E1) // Crisp slate ring lines
val RadarSweepGlow = Color(0x330284C7) // 20% blue sweep glow
val UncertaintyHaloColor = Color(0x33D97706) // 20% amber band for uncertainty

// Backward Compatibility Aliases for Existing Screen Logic
val BgLight = Bg1
val SurfaceWhite = Surface0
val SurfaceSecondary = Surface1
val SurfaceElevated = Surface2
val BorderLight = BorderColor
val DividerColor = BorderSubtle

val BrandBlue = Info
val BrandBlueLight = InfoBg
val BrandBlueHover = Color(0xFF0369A1)
val AccentCyan = Color(0xFF0284C7)
val AccentCyanLight = Color(0xFFE0F2FE)
val PurpleAccent = Color(0xFF7C3AED)
val PurpleAccentLight = Color(0xFFF3E8FF)

val SafeGreen = Healthy
val SafeGreenBg = HealthyBg
val SafeGreenBorder = HealthyBorder

val WarningAmber = Warning
val WarningAmberBg = WarningBg
val WarningAmberBorder = WarningBorder

val ThreatRed = Critical
val ThreatRedBg = CriticalBg
val ThreatRedBorder = CriticalBorder

val BgDark = Bg0
val CardDark = Surface0
val BorderDark = BorderColor
