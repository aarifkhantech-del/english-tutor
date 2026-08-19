package com.vocalbharat.app.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Clean Light Canvas & Surfaces
val LightBackground = Color(0xFFF8FAFC)
val LightBackgroundGradientStart = Color(0xFFEEF2FF)
val LightBackgroundGradientEnd = Color(0xFFF8FAFC)

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightCard = Color(0xFFFFFFFF)
val SurfaceLightBorder = Color(0xFFE2E8F0)
val SurfaceGlassBorder = Color(0x334F46E5)

// Backward compatible aliases
val DarkBackground = LightBackground
val DarkBackgroundEnd = Color(0xFFF1F5F9)
val SurfaceDark = SurfaceLight
val SurfaceDarkCard = SurfaceLightCard
val SurfaceBorder = SurfaceLightBorder

// ── Unified Brand Accent (Help & Support red-orange) ──────────────────────────
// All screens, buttons, and active states use this palette for consistency
val AppAccent      = Color(0xFFFF5252)   // Primary red-orange
val AppAccentEnd   = Color(0xFFFF8F00)   // Gradient end (warm amber)
val AppAccentMid   = Color(0xFFFF6D2E)   // Mid-point (used in radial gradients)
val AppAccentGlow  = Color(0x33FF5252)   // 20% opacity glow

// Legacy aliases — kept for backward compatibility, now point to AppAccent
val PrimaryIndigo  = AppAccent
val PrimaryViolet  = AppAccentEnd
val PrimaryLight   = Color(0xFFFF7043)
val PrimaryBlue    = AppAccent

val AccentCyan   = Color(0xFF0284C7)
val AccentTeal   = Color(0xFF0D9488)
val AccentMint   = Color(0xFF059669)
val AccentPurple = Color(0xFF7C3AED)
val WarningAmber = Color(0xFFD97706)
val RecordingRed = Color(0xFFE11D48)

// Text Colors
val TextPrimary   = Color(0xFF0F172A)
val TextSecondary = Color(0xFF334155)
val TextMuted     = Color(0xFF64748B)
val TextDim       = Color(0xFF94A3B8)


