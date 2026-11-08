/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.notifications.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Guard: SlideToConfirmButton provides distinct slide-to-act tracks (Take drags left-to-right, Snooze drags right-to-left) with vector arrows to prevent corrupted unicode symbols
@Composable
fun SlideToConfirmButton(
    text: String,
    icon: ImageVector,
    trackColor: Color,
    thumbColor: Color,
    slideToLeft: Boolean,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var trackWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val thumbSizeDp = 58.dp
    val thumbPaddingDp = 5.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val paddingPx = with(density) { thumbPaddingDp.toPx() }
    val maxTravelPx = (trackWidthPx - thumbSizePx - (2 * paddingPx)).coerceAtLeast(1f)
    var isConfirmed by remember { mutableStateOf(false) }

    // Guard: Remember latest onConfirmed lambda reference so pointerInput gesture handlers never execute stale callbacks across recompositions (#113, #BUG-SLIDE-STALE-CALLBACK)
    val currentOnConfirmed by rememberUpdatedState(onConfirmed)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(trackColor.copy(alpha = 0.22f))
            .border(
                BorderStroke(1.5.dp, trackColor.copy(alpha = 0.6f)),
                RoundedCornerShape(22.dp)
            )
            .onSizeChanged {
                trackWidthPx = it.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val progress = (dragOffset.value / maxTravelPx).coerceIn(0f, 1f)

        // Progress fill inside track
        if (slideToLeft) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .align(Alignment.CenterEnd)
                    .background(trackColor.copy(alpha = 0.35f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .align(Alignment.CenterStart)
                    .background(trackColor.copy(alpha = 0.35f))
            )
        }

        // Guard: Center action prompt and animated chevrons horizontally within open lane between resting knob and opposite track edge
        val emptyLaneStart = if (slideToLeft) thumbPaddingDp else (thumbPaddingDp + thumbSizeDp)
        val emptyLaneEnd = if (slideToLeft) (thumbPaddingDp + thumbSizeDp) else thumbPaddingDp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = emptyLaneStart,
                    end = emptyLaneEnd
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .alpha((1f - progress * 1.5f).coerceAtLeast(0f))
            ) {
                if (slideToLeft) {
                    SleekAnimatedChevrons(
                        pointingLeft = true,
                        chevronColor = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SleekAnimatedChevrons(
                        pointingLeft = false,
                        chevronColor = Color.White
                    )
                }
            }
        }

        // Sliding knob
        val knobX = if (slideToLeft) {
            (trackWidthPx - paddingPx - thumbSizePx - dragOffset.value).coerceAtLeast(paddingPx)
        } else {
            (paddingPx + dragOffset.value).coerceAtMost(trackWidthPx - paddingPx - thumbSizePx)
        }

        val knobXOffsetDp = with(density) { knobX.toDp() }

        Box(
            modifier = Modifier
                .offset(x = knobXOffsetDp)
                .size(thumbSizeDp)
                .clip(RoundedCornerShape(17.dp))
                .background(thumbColor)
                .pointerInput(slideToLeft, maxTravelPx, isConfirmed) {
                    if (isConfirmed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (isConfirmed) return@detectHorizontalDragGestures
                            val currentProg = dragOffset.value / maxTravelPx
                            if (currentProg >= 0.70f) {
                                isConfirmed = true
                                coroutineScope.launch {
                                    dragOffset.animateTo(maxTravelPx)
                                    currentOnConfirmed()
                                }
                            } else {
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring())
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (isConfirmed) return@detectHorizontalDragGestures
                            val effectiveDelta = if (slideToLeft) -dragAmount else dragAmount
                            val nextVal = (dragOffset.value + effectiveDelta).coerceIn(0f, maxTravelPx)
                            coroutineScope.launch {
                                dragOffset.snapTo(nextVal)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

// Guard: Hardware-accelerated graphicsLayer animates directional chevrons (> > > or < < <) without recomposition overhead across API 24+
@Composable
fun SleekAnimatedChevrons(
    pointingLeft: Boolean,
    modifier: Modifier = Modifier,
    chevronColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chevron_flow")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    val density = LocalDensity.current
    val chevronIcon = if (pointingLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-7).dp)
    ) {
        for (i in 0..2) {
            // Guard: Staggered pulse wave travels in slide direction (left-to-right for take, right-to-left for snooze)
            val effectiveIndex = if (pointingLeft) (2 - i) else i
            val phase = (waveProgress - (effectiveIndex * 0.18f) + 1f) % 1f
            val pulse = if (phase in 0f..0.42f) {
                kotlin.math.sin((phase / 0.42f) * Math.PI.toFloat())
            } else {
                0f
            }

            val alpha = (0.28f + (0.72f * pulse)).coerceIn(0.28f, 1f)
            val nudgeX = if (pointingLeft) (-2.5f * pulse) else (2.5f * pulse)
            val nudgePx = with(density) { nudgeX.dp.toPx() }
            val scale = 0.94f + (0.14f * pulse)

            Icon(
                imageVector = chevronIcon,
                contentDescription = null,
                tint = chevronColor,
                modifier = Modifier
                    .size(19.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.translationX = nudgePx
                        this.scaleX = scale
                        this.scaleY = scale
                    }
            )
        }
    }
}
