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