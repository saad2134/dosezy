package com.example.dosezy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.data.model.PillShape
import com.example.dosezy.data.model.getLocalizedName

val PRESET_PILL_COLORS = listOf(
    "#1193D4", // Dosezy Blue
    "#10B981", // Emerald Green
    "#EF4444", // Red
    "#F59E0B", // Amber / Orange
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#06B6D4", // Cyan
    "#64748B", // Slate Gray
    "#F97316", // Bright Orange
    "#14B8A6"  // Teal
)

fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF1193D4)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

@Composable
fun PillShapeVisual(
    shape: PillShape,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val pillColor = parseHexColor(colorHex)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (shape) {
                PillShape.ROUND -> {
                    drawCircle(
                        color = pillColor,
                        radius = w * 0.42f,
                        center = center
                    )
                    // Score line
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(w * 0.5f, h * 0.22f),
                        end = Offset(w * 0.5f, h * 0.78f),
                        strokeWidth = w * 0.06f
                    )
                }
                PillShape.CAPSULE -> {
                    val capsuleRect = Size(w * 0.76f, h * 0.42f)
                    val topLeft = Offset(w * 0.12f, h * 0.29f)
                    val cornerRadius = CornerRadius(h * 0.21f, h * 0.21f)

                    val capsulePath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    offset = topLeft,
                                    size = capsuleRect
                                ),
                                cornerRadius = cornerRadius
                            )
                        )
                    }

                    // Draw base capsule
                    drawPath(path = capsulePath, color = pillColor)

                    // Right half overlay highlight clipped to capsule bounds so it never leaks
                    clipPath(capsulePath) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(w * 0.5f, h * 0.29f),
                            size = Size(w * 0.38f, h * 0.42f)
                        )
                    }

                    // Center separator
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(w * 0.5f, h * 0.29f),
                        end = Offset(w * 0.5f, h * 0.71f),
                        strokeWidth = w * 0.05f
                    )
                }
                PillShape.OVAL -> {
                    drawOval(
                        color = pillColor,
                        topLeft = Offset(w * 0.12f, h * 0.25f),
                        size = Size(w * 0.76f, h * 0.5f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(w * 0.5f, h * 0.28f),
                        end = Offset(w * 0.5f, h * 0.72f),
                        strokeWidth = w * 0.05f
                    )
                }
                PillShape.LIQUID -> {
                    // Bottle shape
                    val path = Path().apply {
                        moveTo(w * 0.35f, h * 0.15f)
                        lineTo(w * 0.65f, h * 0.15f)
                        lineTo(w * 0.65f, h * 0.3f)
                        lineTo(w * 0.8f, h * 0.42f)
                        lineTo(w * 0.8f, h * 0.85f)
                        lineTo(w * 0.2f, h * 0.85f)
                        lineTo(w * 0.2f, h * 0.42f)
                        lineTo(w * 0.35f, h * 0.3f)
                        close()
                    }
                    drawPath(path = path, color = pillColor)
                    // Cross indicator
                    drawRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.45f, h * 0.5f),
                        size = Size(w * 0.1f, h * 0.2f)
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.4f, h * 0.55f),
                        size = Size(w * 0.2f, h * 0.1f)
                    )
                }
                PillShape.INHALER -> {
                    // L-shape inhaler
                    val path = Path().apply {
                        moveTo(w * 0.25f, h * 0.2f)
                        lineTo(w * 0.55f, h * 0.2f)
                        lineTo(w * 0.55f, h * 0.55f)
                        lineTo(w * 0.85f, h * 0.55f)
                        lineTo(w * 0.85f, h * 0.8f)
                        lineTo(w * 0.25f, h * 0.8f)
                        close()
                    }
                    drawPath(path = path, color = pillColor)
                }
                PillShape.INJECTION -> {
                    // Syringe
                    drawRoundRect(
                        color = pillColor,
                        topLeft = Offset(w * 0.35f, h * 0.3f),
                        size = Size(w * 0.3f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                    // Needle
                    drawLine(
                        color = Color.Gray,
                        start = Offset(w * 0.5f, h * 0.75f),
                        end = Offset(w * 0.5f, h * 0.92f),
                        strokeWidth = w * 0.05f
                    )
                    // Plunger
                    drawLine(
                        color = pillColor,
                        start = Offset(w * 0.5f, h * 0.15f),
                        end = Offset(w * 0.5f, h * 0.3f),
                        strokeWidth = w * 0.08f
                    )
                }
                PillShape.DROPS -> {
                    // Tear drop
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.15f)
                        cubicTo(w * 0.15f, h * 0.55f, w * 0.2f, h * 0.85f, w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.8f, h * 0.85f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.15f)
                        close()
                    }
                    drawPath(path = path, color = pillColor)
                }
                PillShape.PATCH -> {
                    // Bandage patch
                    drawRoundRect(
                        color = pillColor,
                        topLeft = Offset(w * 0.18f, h * 0.22f),
                        size = Size(w * 0.64f, h * 0.56f),
                        cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(w * 0.32f, h * 0.34f),
                        size = Size(w * 0.36f, h * 0.32f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillShapeSelector(
    selectedShape: PillShape,
    selectedColorHex: String,
    onShapeSelected: (PillShape) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(com.example.dosezy.R.string.med_shape_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillShape.values().forEach { shape ->
                val isSelected = (shape == selectedShape)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .clickable { onShapeSelected(shape) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        PillShapeVisual(
                            shape = shape,
                            colorHex = selectedColorHex,
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shape.getLocalizedName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun BorderStroke(width: Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillColorSelector(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(com.example.dosezy.R.string.med_color_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            PRESET_PILL_COLORS.take(5).forEach { hex ->
                val color = parseHexColor(hex)
                val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            PRESET_PILL_COLORS.drop(5).forEach { hex ->
                val color = parseHexColor(hex)
                val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun PillVisualDialog(
    selectedShape: PillShape,
    selectedColorHex: String,
    onShapeSelected: (PillShape) -> Unit,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(com.example.dosezy.R.string.med_shape_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Live Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PillShapeVisual(
                        shape = selectedShape,
                        colorHex = selectedColorHex,
                        size = 64.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shapes Grid
                PillShapeSelector(
                    selectedShape = selectedShape,
                    selectedColorHex = selectedColorHex,
                    onShapeSelected = onShapeSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Palette
                PillColorSelector(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = onColorSelected
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1193D4))
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

