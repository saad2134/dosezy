package com.example.dosezy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.dosezy.ui.theme.SwitchBorderOffDark
import com.example.dosezy.ui.theme.SwitchBorderOffLight
import com.example.dosezy.ui.theme.SwitchThumbOffDark
import com.example.dosezy.ui.theme.SwitchThumbOffLight
import com.example.dosezy.ui.theme.SwitchTrackOffDark
import com.example.dosezy.ui.theme.SwitchTrackOffLight

/**
 * Standard Dosezy switch complying with WCAG 2.2 contrast standards (>= 3:1 for interface components).
 * In dark mode, provides a high-contrast track outline (Color(0xFF94A3B8) giving 6.5:1 against #1E1E1E)
 * and distinct thumb (Color(0xFFCBD5E1) giving 11.1:1) in the off state (#F10).
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = appSwitchColors(checkedTrackColor = checkedTrackColor)
    )
}

@Composable
fun appSwitchColors(
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary
): SwitchColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = checkedTrackColor,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = if (isDark) SwitchThumbOffDark else SwitchThumbOffLight,
        uncheckedTrackColor = if (isDark) SwitchTrackOffDark else SwitchTrackOffLight,
        uncheckedBorderColor = if (isDark) SwitchBorderOffDark else SwitchBorderOffLight
    )
}
