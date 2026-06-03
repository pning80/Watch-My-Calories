package com.pning80.watchmycalories.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light scheme (unchanged) ───────────────────────────────────────────────
val CwPrimaryLight = Color(0xFF2E6B4F)
val CwSecondaryLight = Color(0xFFD9F2DB)
val CwAccent = Color(0xFFFF9E1C)
val CwBackgroundLight = Color(0xFFFAFAFA)
val CwSurfaceLight = Color(0xFFFFFFFF)
val CwTextPrimaryLight = Color(0xFF1A1A1A)
// Neutral light surface ladder + green tint — mirrors the dark scheme's
// anti-M3-purple guard (D-011) which the light scheme was missing. Without
// these, light-mode cards (which use colorScheme.surfaceContainer) fell back
// to M3's default lavender (~#F1ECF1) instead of iOS's neutral white.
val CwSurfaceTintLight = Color(0xFF66CC99)
val CwSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val CwSurfaceContainerLowLight = Color(0xFFFFFFFF)
val CwSurfaceContainerLight = Color(0xFFFFFFFF)
val CwSurfaceContainerHighLight = Color(0xFFF2F2F7)
val CwSurfaceContainerHighestLight = Color(0xFFEDEDF2)
val CwSurfaceVariantLight = Color(0xFFF2F2F7)

// ── Dark scheme — mirrors iOS dark mode (D-011) ────────────────────────────
// Aligned to iOS's two-tone neutral dark for visual parity:
//   - Background is true black (#000) like iOS `systemBackground`; surface is
//     the neutral #1C1C1E (`secondarySystemBackground`). The earlier green
//     tint read as "off" next to iOS, so it's removed.
//   - Primary mint and tertiary orange match iOS exactly (#66CC99 / #FF9E1C),
//     restoring the vivid "energetic orange" the softened tone had drained.
//   - Text is pure white (#FFFFFF) like iOS `label` for crisp contrast.
//   - Secondary keeps the pale sage #B8D5C2: iOS reuses a too-dark light-theme
//     container (#264D33) in dark mode, which reads poorly as a badge fill —
//     this is the one intentional improvement retained over iOS.
//   - surfaceTint stays brand mint so any tonal-elevation fallback picks up a
//     whisper of green rather than M3's default purple; the visible surfaces
//     all resolve from the explicit neutral surfaceContainer slots below.
val CwPrimaryDark = Color(0xFF66CC99)
val CwOnPrimaryDark = Color(0xFF003822)
val CwPrimaryContainerDark = Color(0xFF1F4A35)
val CwOnPrimaryContainerDark = Color(0xFFA8EFC8)

val CwSecondaryDark = Color(0xFFB8D5C2)
val CwOnSecondaryDark = Color(0xFF22382A)
val CwSecondaryContainerDark = Color(0xFF2F4A39)
val CwOnSecondaryContainerDark = Color(0xFFCFEAD8)

val CwAccentDark = Color(0xFFFF9E1C)
val CwOnTertiaryDark = Color(0xFF4A2A00)
val CwTertiaryContainerDark = Color(0xFF5C3A00)
val CwOnTertiaryContainerDark = Color(0xFFFFDDB0)

val CwBackgroundDark = Color(0xFF000000)
val CwSurfaceDark = Color(0xFF1C1C1E)
val CwTextPrimaryDark = Color(0xFFFFFFFF)

val CwSurfaceTintDark = Color(0xFF66CC99)
val CwSurfaceContainerLowestDark = Color(0xFF000000)
val CwSurfaceContainerLowDark = Color(0xFF161618)
val CwSurfaceContainerDark = Color(0xFF1C1C1E)
val CwSurfaceContainerHighDark = Color(0xFF2C2C2E)
val CwSurfaceContainerHighestDark = Color(0xFF363638)
val CwSurfaceVariantDark = Color(0xFF3A3A3C)
val CwOnSurfaceVariantDark = Color(0xFFC7C7CC)
val CwOutlineDark = Color(0xFF8E8E93)
val CwOutlineVariantDark = Color(0xFF3A3A3C)
