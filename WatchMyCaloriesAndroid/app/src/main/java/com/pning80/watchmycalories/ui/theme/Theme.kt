package com.pning80.watchmycalories.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CwPrimaryDark,
    onPrimary = CwOnPrimaryDark,
    primaryContainer = CwPrimaryContainerDark,
    onPrimaryContainer = CwOnPrimaryContainerDark,
    secondary = CwSecondaryDark,
    onSecondary = CwOnSecondaryDark,
    secondaryContainer = CwSecondaryContainerDark,
    onSecondaryContainer = CwOnSecondaryContainerDark,
    tertiary = CwAccentDark,
    onTertiary = CwOnTertiaryDark,
    tertiaryContainer = CwTertiaryContainerDark,
    onTertiaryContainer = CwOnTertiaryContainerDark,
    background = CwBackgroundDark,
    onBackground = CwTextPrimaryDark,
    surface = CwSurfaceDark,
    onSurface = CwTextPrimaryDark,
    surfaceVariant = CwSurfaceVariantDark,
    onSurfaceVariant = CwOnSurfaceVariantDark,
    surfaceTint = CwSurfaceTintDark,
    surfaceContainerLowest = CwSurfaceContainerLowestDark,
    surfaceContainerLow = CwSurfaceContainerLowDark,
    surfaceContainer = CwSurfaceContainerDark,
    surfaceContainerHigh = CwSurfaceContainerHighDark,
    surfaceContainerHighest = CwSurfaceContainerHighestDark,
    outline = CwOutlineDark,
    outlineVariant = CwOutlineVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = CwPrimaryLight,
    secondary = CwSecondaryLight,
    tertiary = CwAccent,
    background = CwBackgroundLight,
    surface = CwSurfaceLight,
    onPrimary = CwTextPrimaryLight,
    onSecondary = CwTextPrimaryLight,
    onBackground = CwTextPrimaryLight,
    onSurface = CwTextPrimaryLight,
    // Neutral surface ladder + green tint so light-mode cards render white
    // (iOS parity), not M3's default lavender. Mirrors the dark scheme.
    surfaceTint = CwSurfaceTintLight,
    surfaceVariant = CwSurfaceVariantLight,
    surfaceContainerLowest = CwSurfaceContainerLowestLight,
    surfaceContainerLow = CwSurfaceContainerLowLight,
    surfaceContainer = CwSurfaceContainerLight,
    surfaceContainerHigh = CwSurfaceContainerHighLight,
    surfaceContainerHighest = CwSurfaceContainerHighestLight,
)

@Composable
fun WatchMyCaloriesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Edge-to-edge is enabled in MainActivity via enableEdgeToEdge(). We only
            // sync the status-bar icon color to the active theme so icons stay legible
            // against whatever background sits under the transparent status bar.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
