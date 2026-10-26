package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF3B2506),
    onPrimaryContainer = StudioAmberBright,
    secondary = StudioCyan,
    onSecondary = Color(0xFF090D16),
    secondaryContainer = Color(0xFF0C2D48),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = StudioIndigo,
    onTertiary = Color.White,
    background = StudioObsidian,
    onBackground = TextPrimaryDark,
    surface = StudioSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = StudioBorder,
    error = StudioCrimson,
    onError = Color.White
)

@Composable
fun ThumbnailAIStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
