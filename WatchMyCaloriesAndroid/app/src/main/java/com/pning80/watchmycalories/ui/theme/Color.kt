package com.pning80.watchmycalories.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light scheme (unchanged) ───────────────────────────────────────────────
val CwPrimaryLight = Color(0xFF2E6B4F)
val CwSecondaryLight = Color(0xFFD9F2DB)
// Selected-segment / secondaryContainer fill for LIGHT. The light scheme never
// set secondaryContainer, so M3 derived its default lavender — which surfaced as
// purple selected SegmentedButtons (ManualEntry meal picker, onboarding) in light
// while dark used CwSecondaryContainerDark green. Pale green + dark-green text
// mirrors the dark scheme's brand intent.
val CwSecondaryContainerLight = Color(0xFFD9F2DB)
val CwOnSecondaryContainerLight = Color(0xFF1F4A35)
val CwAccent = Color(0xFFFF9E1C)
// Fat macro color — iOS uses `Color.secondary` (the system GRAY label color),
// NOT the brand sage. Ported as colorScheme.secondary (sage) by mistake, which
// made Fat green instead of gray everywhere (hero breakdown, macro bars, History
// chips). iOS systemGray works in both themes. Shared light/dark.
val CwMacroFat = Color(0xFF8E8E93)
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
//   - Secondary is iOS forest #264D33 (matches `cwSecondary` dark exactly).
//     This is the fill behind the kcal pill, the meal/avatar tile, the hero
//     ring track, and the dashboard empty-state glyph — all of which iOS draws
//     in forest, so converging the token brings every one of them into parity
//     in a single stroke (D-011 revised 2026-06-02). The earlier pale-sage
//     #B8D5C2 leaked into all five surfaces under a sign-off that only covered
//     the Remaining badge; that readability tweak now lives in the dedicated,
//     badge-only `CwRemainingBadgeDark` below instead of bending the shared token.
//   - surfaceTint stays brand mint so any tonal-elevation fallback picks up a
//     whisper of green rather than M3's default purple; the visible surfaces
//     all resolve from the explicit neutral surfaceContainer slots below.
val CwPrimaryDark = Color(0xFF66CC99)
val CwOnPrimaryDark = Color(0xFF003822)
val CwPrimaryContainerDark = Color(0xFF1F4A35)
val CwOnPrimaryContainerDark = Color(0xFFA8EFC8)

val CwSecondaryDark = Color(0xFF264D33)
val CwOnSecondaryDark = Color(0xFFB8D5C2)

// Remaining-stat badge fill (dark only). iOS draws this badge in forest
// `cwSecondary` (#264D33), but on the true-black dark surface a forest circle
// nearly vanishes; the pale sage reads as a distinct chip. This is the sole
// retained departure from iOS's dark secondary — scoped to the one badge so it
// no longer leaks into the ring track, pills, tiles, or empty-state glyph
// (D-011 revised 2026-06-02). Light mode uses `colorScheme.secondary` directly
// (iOS's pale `#D9F2DB` already matches), so no light counterpart is needed.
val CwRemainingBadgeDark = Color(0xFFB8D5C2)
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
