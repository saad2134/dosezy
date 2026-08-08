package com.example.dosezy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF26292E),
            Color(0xFF383C45),
            Color(0xFF26292E)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

@Composable
fun MedicineCardSkeleton(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmer()
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title Line
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle Line
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Badge Placeholder
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmer()
            )
        }
    }
}

@Composable
fun ScheduleCardSkeleton(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Badge
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shimmer()
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Medicine Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Button Placeholder
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer()
            )
        }
    }
}

@Composable
fun MedicineListSkeleton(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) {
            MedicineCardSkeleton()
        }
    }
}

@Composable
fun ScheduleSkeletonList(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) {
            ScheduleCardSkeleton()
        }
    }
}

@Composable
fun HomeSkeletonView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        // Header Greeting Skeleton
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .fillMaxWidth(0.5f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Schedule Section Title Skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .height(18.dp)
                .width(140.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // List Skeletons
        repeat(3) {
            ScheduleCardSkeleton()
        }
    }
}
