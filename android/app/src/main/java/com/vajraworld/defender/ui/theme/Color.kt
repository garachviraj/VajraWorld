package com.vajraworld.defender.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// VAJRAWORLD GUARDIAN — MASTER COLOR SYSTEM (Section 4)
// Aerospace SOC / Deep Dark Control Room Identity
// =====================================================================

// Base Palette (Dark Cockpit)
val Bg0 = Color(0xFF070A0F)          // Deepest cockpit space
val Bg1 = Color(0xFF0B1018)          // Primary screen background
val Surface0 = Color(0xFF101722)     // Primary card & panel surface
val Surface1 = Color(0xFF151D29)     // Elevated surface / nested card
val Surface2 = Color(0xFF1A2432)     // Focused / hover surface
val BorderColor = Color(0xFF263244)  // Technical hairline grid / border
val BorderSubtle = Color(0xFF1E293B) // Soft separator line

// High-Contrast Technical Typography
val TextPrimary = Color(0xFFF3F7FB)   // Crisp primary reading text
val TextSecondary = Color(0xFF98A7BA) // Clean technical label / subtitle
val TextMuted = Color(0xFF617084)     // Timestamp / muted metadata
val TextWhite = Color(0xFFFFFFFF)

// Semantic Cyber Threat States (Section 4)
val Healthy = Color(0xFF32D583)       // Emerald nominal / protected state
val HealthyBg = Color(0x1F32D583)     // 12% alpha soft green glow
val HealthyBorder = Color(0x4D32D583) // 30% alpha green border

val Info = Color(0xFF4DA3FF)          // Tech / Brand telemetry blue
val InfoBg = Color(0x1F4DA3FF)        // 12% alpha soft blue glow
val InfoBorder = Color(0x4D4DA3FF)    // 30% alpha blue border

val Warning = Color(0xFFF5B942)       // Elevated amber caution
val WarningBg = Color(0x1FF5B942)     // 12% alpha soft amber glow
val WarningBorder = Color(0x4DF5B942) // 30% alpha amber border

val High = Color(0xFFFF7A45)          // High severity orange
val HighBg = Color(0x1FFF7A45)
val HighBorder = Color(0x4DFF7A45)

val Critical = Color(0xFFFF4D5F)      // Critical attack ruby red
val CriticalBg = Color(0x1FFF4D5F)    // 12% alpha soft red glow
val CriticalBorder = Color(0x4DFF4D5F)// 30% alpha red border

// Radar Ring Geometry Accents
val RadarRingColor = Color(0x1F4DA3FF)
val RadarSweepGlow = Color(0x264DA3FF)
val UncertaintyHaloColor = Color(0x33F5B942) // Translucent amber band for uncertainty

// Backward Compatibility Aliases for Existing Screen Logic
val BgLight = Bg1
val SurfaceWhite = Surface0
val SurfaceSecondary = Surface1
val SurfaceElevated = Surface2
val BorderLight = BorderColor
val DividerColor = BorderColor

val BrandBlue = Info
val BrandBlueLight = InfoBg
val BrandBlueHover = Color(0xFF2B8EF0)
val AccentCyan = Color(0xFF38BDF8)
val AccentCyanLight = Color(0x1F38BDF8)
val PurpleAccent = Color(0xFF818CF8)
val PurpleAccentLight = Color(0x1F818CF8)

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
