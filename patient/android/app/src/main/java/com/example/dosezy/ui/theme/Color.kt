package com.example.dosezy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Primary Brand Blue Tokens complying with high-fidelity wireframes (#F16)
val BrandPrimaryLight = Color(0xFF2084E4)        // High-fidelity wireframe primary blue
val BrandPrimaryTintLight = Color(0xFFE3F2FD)    // Light mode primary tint / container
val BrandPrimaryDark = Color(0xFF4FC3F7)         // Dark mode primary blue (high-contrast 9:1 against #1E1E1E)
val BrandPrimaryTintDark = Color(0xFF0D253F)     // Dark mode primary tint / container

// Deprecated alias compatibility
val LightBlue80 = BrandPrimaryDark
val LightBlue40 = BrandPrimaryLight

// Guard: High-contrast clinical note colors complying with WCAG 2.2 AA (>= 4.5:1 text contrast) (#F10)
val NoteAmberLight = Color(0xFF9A4A00) // 5.6:1 - 6.2:1 against light surface/white
val NoteAmberDark = Color(0xFFFBBF24)  // 8.4:1 against dark surface

// Guard: High-contrast switch colors complying with WCAG 2.2 AA (>= 3:1 graphical component contrast) (#F10)
val SwitchThumbOffLight = Color(0xFF64748B)  // 5.6:1 against white
val SwitchBorderOffLight = Color(0xFF64748B) // 5.6:1 against white
val SwitchTrackOffLight = Color(0xFFE2E8F0)

val SwitchThumbOffDark = Color(0xFFCBD5E1)   // 11.1:1 against #1E1E1E
val SwitchBorderOffDark = Color(0xFF94A3B8)  // 6.5:1 against #1E1E1E
val SwitchTrackOffDark = Color(0xFF262A34)

@Composable
fun getMedicationNoteColor(): Color {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) NoteAmberDark else NoteAmberLight
}

val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryTintLight,
    onPrimaryContainer = Color(0xFF003258),
    secondary = BrandPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = BrandPrimaryTintLight,
    onSecondaryContainer = Color(0xFF003258),
    background = Color(0xFFF8F9FA),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = BrandPrimaryLight
)

val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = BrandPrimaryTintDark,
    onPrimaryContainer = Color(0xFFBEE8FF),
    secondary = BrandPrimaryDark,
    onSecondary = Color(0xFF003258),
    secondaryContainer = BrandPrimaryTintDark,
    onSecondaryContainer = Color(0xFFBEE8FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF444444),
    tertiary = Color(0xFF03DAC5),
    onTertiary = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF24272E),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = BrandPrimaryDark
)