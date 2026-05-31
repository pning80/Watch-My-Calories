package com.pning80.watchmycalories.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light scheme (unchanged) ───────────────────────────────────────────────
val CwPrimaryLight = Color(0xFF2E6B4F)
val CwSecondaryLight = Color(0xFFD9F2DB)
val CwAccent = Color(0xFFFF9E1C)
val CwBackgroundLight = Color(0xFFFAFAFA)
val CwSurfaceLight = Color(0xFFFFFFFF)
val CwTextPrimaryLight = Color(0xFF1A1A1A)

// ── Dark scheme — M3 expressive dark palette with brand-tinted elevation ───
// Rationale:
//   - Background is a soft near-black with a faint green tint (not pure #000)
//     so shadows render and cards visibly lift; reduces halation vs OLED black.
//   - Surface is a hair lighter than background so cards have natural lift.
//   - Primary is desaturated from the light scheme's mint per Material's
//     dark-mode guidance ("use lighter, less saturated tones").
//   - Secondary flips from a too-dark forest (#264D33 was a light-theme
//     container color reused in dark mode) to a pale sage that reads as a
//     badge fill.
//   - Tertiary (accent) is softened from full-saturation #FF9E1C.
//   - surfaceTint overrides the M3 default purple — every elevated component
//     (TopAppBar, DropdownMenu, Dialog, BottomSheet) now picks up a brand
//     mint cast from tonal elevation overlays instead of off-brand lavender.
//   - Full M3 surfaceContainer slots are defined so Card / Sheet / Menu
//     fills resolve from the brand palette instead of M3 defaults.
val CwPrimaryDark = Color(0xFF7FD9A8)
val CwOnPrimaryDark = Color(0xFF003822)
val CwPrimaryContainerDark = Color(0xFF1F4A35)
val CwOnPrimaryContainerDark = Color(0xFFA8EFC8)

val CwSecondaryDark = Color(0xFFB8D5C2)
val CwOnSecondaryDark = Color(0xFF22382A)
val CwSecondaryContainerDark = Color(0xFF2F4A39)
val CwOnSecondaryContainerDark = Color(0xFFCFEAD8)

val CwAccentDark = Color(0xFFFFB95A)
val CwOnTertiaryDark = Color(0xFF4A2A00)
val CwTertiaryContainerDark = Color(0xFF5C3A00)
val CwOnTertiaryContainerDark = Color(0xFFFFDDB0)

val CwBackgroundDark = Color(0xFF0F1411)
val CwSurfaceDark = Color(0xFF1A211C)
val CwTextPrimaryDark = Color(0xFFE2E6E3)

val CwSurfaceTintDark = Color(0xFF66CC99)
val CwSurfaceContainerLowestDark = Color(0xFF0B0F0C)
val CwSurfaceContainerLowDark = Color(0xFF161C18)
val CwSurfaceContainerDark = Color(0xFF1F2722)
val CwSurfaceContainerHighDark = Color(0xFF252D28)
val CwSurfaceContainerHighestDark = Color(0xFF2B342E)
val CwSurfaceVariantDark = Color(0xFF3A4540)
val CwOnSurfaceVariantDark = Color(0xFFB8C3BC)
val CwOutlineDark = Color(0xFF8A968E)
val CwOutlineVariantDark = Color(0xFF3A4540)
