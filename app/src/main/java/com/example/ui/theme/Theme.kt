package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = KurdPrimary,
    onPrimary = Color.White,
    primaryContainer = KurdSecondary,
    onPrimaryContainer = KurdText,
    secondary = KurdDarkBlue,
    onSecondary = Color.White,
    background = KurdBackground,
    onBackground = KurdText,
    surface = KurdCardBg,
    onSurface = KurdText,
    outline = KurdBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = KurdPrimary,
    onPrimary = Color.White,
    primaryContainer = KurdDarkBlue,
    onPrimaryContainer = Color.White,
    secondary = KurdSecondary,
    onSecondary = KurdText,
    background = KurdDarkBlue,
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // We strictly use our customized theme to represent Shivan Haji's curated professional design.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
