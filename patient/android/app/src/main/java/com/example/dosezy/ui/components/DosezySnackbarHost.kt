package com.example.dosezy.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun DosezySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isDark) Color(0xFF24272E) else Color(0xFFFFFFFF)
    val contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val actionColor = if (isDark) Color(0xFF4FC3F7) else Color(0xFF0277BD)

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(12.dp),
            containerColor = containerColor,
            contentColor = contentColor,
            actionColor = actionColor,
            actionContentColor = actionColor,
            dismissActionContentColor = contentColor.copy(alpha = 0.7f)
        )
    }
}
