package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = TribePrimaryDark,
    onPrimary = TribeOnPrimaryContainer,
    primaryContainer = TribeSecondary,
    onPrimaryContainer = Color.White,
    secondary = TribeSecondaryDark,
    onSecondary = Color.Black,
    background = TribeDarkBackground,
    surface = TribeDarkSurface,
    surfaceVariant = TribeDarkSurfaceVariant,
    onBackground = TribeDarkTextPrimary,
    onSurface = TribeDarkTextPrimary,
    onSurfaceVariant = TribeDarkTextSecondary,
    outline = TribeDarkOutline,
    outlineVariant = TribeDarkOutline.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = TribePrimary,
    onPrimary = TribeOnPrimary,
    primaryContainer = TribePrimaryContainer,
    onPrimaryContainer = TribeOnPrimaryContainer,
    secondary = TribeSecondary,
    onSecondary = Color.White,
    background = TribeLightBackground,
    surface = TribeLightSurface,
    surfaceVariant = TribeLightSurfaceVariant,
    onBackground = TribeLightTextPrimary,
    onSurface = TribeLightTextPrimary,
    onSurfaceVariant = TribeLightTextSecondary,
    outline = TribeLightOutline,
    outlineVariant = TribeLightOutline.copy(alpha = 0.5f)
)

@Composable
fun TribeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
