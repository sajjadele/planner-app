package com.example.plugins.planner.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Gentle, low-contrast shimmer used by all skeleton placeholders. Calm sweep (no flicker) tuned to
 * the app palette: a soft highlight glides across surface-variant placeholders. Amplitude is kept
 * small so it reads as "breathing" rather than "loading spinner".
 */
@Composable
fun skeletonShimmerBrush(): Brush {
    val isDark = LocalIsDarkTheme.current
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, delayMillis = 200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    // Sweep a soft band across the placeholder. translate in [-1, 1] so the band enters/exits cleanly.
    val shift = (translate - 0.5f) * 2f
    return Brush.linearGradient(
        0.0f to base,
        0.5f + shift * 0.5f to highlight,
        1.0f to base,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
    )
}

/** A single shimmering placeholder bar. */
@Composable
private fun ShimmerBar(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 12.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(skeletonShimmerBrush())
    )
}

/**
 * Skeleton placeholder mirroring [TaskCard]'s layout (checkbox + title line + action icons) so the
 * list height/shape stays stable while real tasks load. Shown 2-3x during cold launch.
 */
@Composable
fun TaskCardSkeleton(modifier: Modifier = Modifier) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 6
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox placeholder
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .shadow(3.dp, RoundedCornerShape(7.dp))
                    .background(skeletonShimmerBrush(), RoundedCornerShape(7.dp))
                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Title + meta lines placeholder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
            }

            // Action icons placeholder
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(skeletonShimmerBrush(), RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(skeletonShimmerBrush(), RoundedCornerShape(6.dp))
            )
        }
    }
}

/**
 * Skeleton placeholder for ActivityMessageCard.
 * Mirrors the Telegram-style bubble layout: right-aligned bubble with
 * shimmer bars for text lines, image thumbnail, and metadata.
 */
@Composable
fun ActivityMessageSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        // Duration box placeholder (left side)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(skeletonShimmerBrush())
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Message bubble placeholder
        Surface(
            modifier = Modifier.fillMaxWidth(0.75f),
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Text lines
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.8f), height = 12.dp)
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.5f), height = 10.dp)

                // Image thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(skeletonShimmerBrush())
                )

                // Timestamp
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.2f), height = 8.dp)
            }
        }
    }
}
