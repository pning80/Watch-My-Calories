package com.pning80.caloriewatcherandroid.ui.theme

import androidx.compose.ui.graphics.Color

// iOS Design System Colors
val CWPrimary = Color(0xFF2E6B4F)   // Deep Forest Green
val CWSecondary = Color(0xFFD9F2DB) // Pale Mint
val CWAccent = Color(0xFFFF9E1C)    // Energetic Orange
val CWBackground = Color(0xFFFAFAFA) // Soft Off-White
val CWSurface = Color(0xFFFFFFFF)   // White
val CWTextPrimary = Color(0xFF1A1A1A)

// Legacy / Helper Colors (Mapping to new system where appropriate)
val OrganicGreen = CWPrimary
val MintSage = CWSecondary
val BurntOrange = CWAccent
val SoftOffWhite = CWBackground
val OrganicSurface = CWSurface

val Green40 = CWPrimary
val Green80 = CWSecondary
val Green10 = CWSecondary.copy(alpha = 0.5f) // Subtle background tint
val Green20 = CWSecondary.copy(alpha = 0.8f) // Slightly darker background tint

val GreenGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)
