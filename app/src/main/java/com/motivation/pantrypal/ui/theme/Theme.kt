package com.motivation.pantrypal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Supports light/dark mode as required by the non-functional requirements.
private val LightColors = lightColorScheme(
    primary = PantryOrange,
    secondary = PantryTeal,
    tertiary = PantryNavy,
    background = PantrySurfaceLight,
    error = PantryDanger
)

private val DarkColors = darkColorScheme(
    primary = PantryOrange,
    secondary = PantryTeal,
    tertiary = PantryNavy,
    background = PantrySurfaceDark,
    error = PantryDanger
)

@Composable
fun PantryPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = PantryTypography, content = content)
}
