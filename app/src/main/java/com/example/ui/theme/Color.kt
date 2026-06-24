package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// "Sophisticated Dark" Theme Palette
val SophisticatedDarkBg = Color(0xFF1A1C1E)      // Deep charcoal background
val SophisticatedDarkCard = Color(0xFF2D3033)    // Sleek medium charcoal for cards
val SophisticatedBorder = Color(0xFF43474E)      // Border grey / separator
val SophisticatedLightBlue = Color(0xFFD0E4FF)   // Soft ice-blue for titles/primary
val SophisticatedLimeAccent = Color(0xFFB4FF9A)  // Luminous pastel green / charging completion
val SophisticatedMutedText = Color(0xFF8E9196)   // Subtle gray-blue for descriptions
val SophisticatedWhite = Color(0xFFE2E2E6)       // Soft white for primary text

// We map existing color names to preserve existing codebase logic but transform the entire aesthetic
val CyberBlack = SophisticatedDarkBg
val CyberDarkCard = SophisticatedDarkCard
val CyberCyan = SophisticatedLightBlue
val CyberBlue = Color(0xFF38495A)                // Elegant dark-slate-blue for secondary actions/buttons
val CyberGreen = SophisticatedLimeAccent
val CyberOrange = Color(0xFFF2BF94)              // Muted warm peach for moderate status / temp
val CyberPink = Color(0xFFFFB4AB)                // Soft elegant rose for alerts / deletes
val CyberLightGray = SophisticatedMutedText
val CyberWhite = SophisticatedWhite

val PrimaryDark = SophisticatedLightBlue
val SecondaryDark = SophisticatedBorder
val BackgroundDark = SophisticatedDarkBg
val SurfaceDark = SophisticatedDarkCard

val PrimaryLight = Color(0xFF00677C)
val SecondaryLight = Color(0xFF005F9E)
val BackgroundLight = Color(0xFFF6FAFD)
val SurfaceLight = Color(0xFFFFFFFF)
