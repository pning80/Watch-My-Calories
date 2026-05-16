package com.pning80.watchmycalories.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CwPrimaryDark,
    secondary = CwSecondaryDark,
    tertiary = CwAccent,
    background = CwBackgroundDark,
    surface = CwSurfaceDark,
    onPrimary = CwTextPrimaryDark,
    onSecondary = CwTextPrimaryDark,
    onBackground = CwTextPrimaryDark,
    onSurface = CwTextPrimaryDark
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
    onSurface = CwTextPrimaryLight
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
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
