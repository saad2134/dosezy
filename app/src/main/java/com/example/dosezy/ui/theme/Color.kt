package com.example.dosezy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light blue color scheme
val LightBlue80 = Color(0xFF4FC3F7)
val LightBlue40 = Color(0xFF0277BD)

val LightColorScheme = lightColorScheme(
    primary = LightBlue40,
    onPrimary = Color.White,
    primaryContainer = LightBlue80,
    onPrimaryContainer = Color.Black,
    secondary = LightBlue40,
    onSecondary = Color.White,
    secondaryContainer = LightBlue80,
    onSecondaryContainer = Color.Black,
    background = Color(0xFFF8F9FA),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = LightBlue80,
    onPrimary = Color.White,
    primaryContainer = LightBlue40,
    onPrimaryContainer = Color.White,
    secondary = LightBlue80,
    onSecondary = Color.White,
    secondaryContainer = LightBlue40,
    onSecondaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF444444),
    tertiary = Color(0xFF03DAC5),
    onTertiary = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black
)