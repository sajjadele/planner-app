package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light Mode Palette — Unified soft purple ──
val LightBackground = Color(0xFFF0E6FF)          // main bg & header
val LightSurface = Color(0xFFFFFFFF)              // card white
val LightSurfaceVariant = Color(0xFFF8F4FF)       // subtle card alt
val LightTextPrimary = Color(0xFF211A2D)           // deep indigo
val LightTextSecondary = Color(0xFF5B5270)         // muted indigo
val LightTextTertiary = Color(0xFF8B85A0)          // faint
val LightBorder = Color(0xFFDDD6E8)                // borders
val AccentPurple = Color(0xFF8A56EC)                // FAB, active day, checkboxes
val AccentPurpleLight = Color(0xFFF3EDFF)           // light chip bg

// ── Dark Mode Palette — Midnight Blue + Cyan ──
val DarkBackground = Color(0xFF0F172A)              // midnight slate
val DarkSurface = Color(0xFF1E293B)                 // card surfaces
val DarkSurfaceVariant = Color(0xFF263548)           // elevated surfaces
val DarkTextPrimary = Color(0xFFF8FAFC)              // off-white
val DarkTextSecondary = Color(0xFFCBD5E1)            // light slate
val DarkTextTertiary = Color(0xFF64748B)              // muted slate
val DarkBorder = Color(0xFF334155)                   // borders
val AccentCyan = Color(0xFF38BDF8)                   // FAB, active day, progress
val AccentCyanBg = Color(0xFF1E3A5F)                  // chip bg for cyan

// Shared accent colors
val AccentFire = Color(0xFFF97316)
val AccentBlue = Color(0xFF3B82F6)
val AccentGreen = Color(0xFF22C55E)
val AccentRed = Color(0xFFEF4444)

// ── Neumorphic Shadow Colors ──
val LightShadowAmbient = Color.White
val LightShadowSpot = Color.Black.copy(alpha = 0.06f)
val DarkShadowAmbient = Color.White.copy(alpha = 0.04f)
val DarkShadowSpot = Color.Black.copy(alpha = 0.60f)

// ── Day selector ──
val LightDayUnselectedBg = Color(0xFFF0E6FF)
val LightDayUnselectedText = Color(0xFF211A2D)
val LightDaySelectedBg = AccentPurple
val LightDaySelectedText = Color.White

val DarkDayUnselectedBg = Color(0xFF1E293B)
val DarkDayUnselectedText = Color(0xFF64748B)
val DarkDaySelectedBg = AccentCyan
val DarkDaySelectedText = Color(0xFF0F172A)

// ── Chip / Tag colors ──
val DarkLifeAreaSelectedBg = AccentCyan
val DarkLifeAreaUnselectedBg = Color(0xFF1E293B)
val DarkLifeAreaUnselectedText = Color(0xFF64748B)

// ══════════════════════════════════════════════════
// Backward-compatible aliases — all point to new light-mode values
// ══════════════════════════════════════════════════
// Old → New
val SurfaceWhite = LightSurface               // 0xFFFFFFFF
val SurfaceLight = LightSurfaceVariant        // 0xFFF8F4FF
val NeumorphicBackground = LightBackground    // 0xFFF0E6FF
val NeumorphicLight = Color(0xFFF5F7FA)
val NeumorphicDark = Color(0xFFD1D5DB)
val TextPrimary = LightTextPrimary            // 0xFF211A2D
val TextSecondary = LightTextSecondary         // 0xFF5B5270
val TextTertiary = LightTextTertiary           // 0xFF8B85A0
val BorderLight = LightBorder                 // 0xFFDDD6E8

// Gradient — keep for now, screens will migrate to theme background
val GradientTop = LightBackground             // 0xFFF0E6FF
val GradientBottom = LightBackground          // 0xFFF0E6FF
val DarkGradientTop = DarkBackground          // 0xFF0F172A
val DarkGradientBottom = DarkBackground       // 0xFF0F172A

// Header — light mode
val HeaderTitleColor = LightTextPrimary       // 0xFF211A2D
val HeaderDateColor = LightTextSecondary      // 0xFF5B5270
val HeaderIconBgLight = LightBackground       // 0xFFF0E6FF
val HeaderIconTint = LightTextPrimary         // 0xFF211A2D

// Header — dark mode
val DarkHeaderTitleColor = DarkTextPrimary    // 0xFFF8FAFC
val DarkHeaderDateColor = DarkTextSecondary   // 0xFFCBD5E1
val DarkHeaderIconBg = Color(0xFF1E293B)
val DarkHeaderIconTint = DarkTextPrimary      // 0xFFF8FAFC

// Day selector — light
val DayUnselectedBg = LightDayUnselectedBg
val DayUnselectedText = LightDayUnselectedText
val DaySelectedBg = LightDaySelectedBg
val DaySelectedText = LightDaySelectedText

// Life area — light
val LifeAreaSelectedBg = Color(0xFF8A56EC)
val LifeAreaSelectedText = Color.White
val LifeAreaUnselectedBg = Color(0xFFF3EDFF)
val LifeAreaUnselectedText = Color(0xFF5B5270)

// Misc old refs
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val DarkAccentPurple = AccentCyan
val DarkSurfaceWhite = DarkSurface
