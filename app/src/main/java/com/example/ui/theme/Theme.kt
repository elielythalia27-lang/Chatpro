package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChatProDarkColorScheme = darkColorScheme(
    primary = ChatProTeal,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = ChatProCyanBright,
    secondary = ChatProCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00382E),
    onSecondaryContainer = Color(0xFF80CBC4),
    tertiary = ChatProCyanBright,
    background = ChatProDarkBg,
    onBackground = ChatProTextPrimary,
    surface = ChatProDarkSurface,
    onSurface = ChatProTextPrimary,
    surfaceVariant = ChatProCardDark,
    onSurfaceVariant = ChatProTextSecondary,
    outline = Color(0xFF37474F)
)

private val ChatProLightColorScheme = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    background = Color(0xFFF7FBFB),
    onBackground = Color(0xFF191C1D),
    surface = Color.White,
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFFB0BEC5)
)

@Composable
fun ChatProTheme(
    darkTheme: Boolean = true, // Default to premium dark as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ChatProDarkColorScheme else ChatProLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ChatProTheme(darkTheme = darkTheme, content = content)
}
