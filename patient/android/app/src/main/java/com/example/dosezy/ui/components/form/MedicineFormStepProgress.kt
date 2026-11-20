/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.ui.components.form

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dosezy.R

/**
 * Animated 3-Step Wizard Progress Header for Add and Edit Medicine (#145, F13).
 *
 * Provides animated progress bar with smooth transitions between steps,
 * step counter header, and interactive squircle step tabs.
 */
@Composable
fun MedicineFormStepProgress(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStepUnlocked: Int = currentStep
) {
    val targetProgress = when (currentStep) {
        1 -> 0.333f
        2 -> 0.666f
        3 -> 1.0f
        else -> 0.333f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "medicine_form_progress_bar"
    )

    val stepLabels = listOf(
        1 to stringResource(R.string.form_step_what),
        2 to stringResource(R.string.form_step_when),
        3 to stringResource(R.string.form_step_extras)
    )

    val activeLabel = stepLabels.find { it.first == currentStep }?.second.orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Step Counter and Active Step Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.form_step_counter, currentStep, 3),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = activeLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                ),
                color = Color(0xFF1193D4)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated Smooth Progress Bar
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF1193D4),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Squircle Step Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stepLabels.forEach { (step, label) ->
                val isActive = step == currentStep
                val isCompleted = step < currentStep
                val isClickable = step <= maxStepUnlocked || isCompleted

                val backgroundColor = when {
                    isActive -> Color(0xFF1193D4).copy(alpha = 0.16f)
                    isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                }

                val borderColor = when {
                    isActive -> Color(0xFF1193D4)
                    isCompleted -> Color(0xFF1193D4).copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = backgroundColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isClickable) {
                            onStepClick(step)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Badge: checkmark for completed, number for current/future (MANDATORY squircle per Rule 12)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isActive -> Color(0xFF1193D4)
                                        isCompleted -> Color(0xFF1193D4).copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF1193D4),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Text(
                                    text = step.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(6.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = when {
                                isActive -> Color(0xFF1193D4)
                                isCompleted -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

