package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Professional Stereo VU Audio Peak Meter with LED segments,
 * smooth ballistics, and clip warning indicator.
 */
@Composable
fun StereoVuMeter(
    leftPeak: Float,  // 0.0 to 1.0+
    rightPeak: Float, // 0.0 to 1.0+
    isClipping: Boolean,
    modifier: Modifier = Modifier,
    heightDp: Int = 120,
    widthDp: Int = 20
) {
    val animLeft by animateFloatAsState(
        targetValue = leftPeak.coerceIn(0f, 1.2f),
        animationSpec = tween(durationMillis = 60),
        label = "animLeftMeter"
    )
    val animRight by animateFloatAsState(
        targetValue = rightPeak.coerceIn(0f, 1.2f),
        animationSpec = tween(durationMillis = 60),
        label = "animRightMeter"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(StudioSurface)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clip LED indicator
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isClipping || leftPeak >= 0.98f || rightPeak >= 0.98f) StudioRed else StudioSurfaceBorder)
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Dual bar meters
        Row(
            modifier = Modifier
                .width(widthDp.dp)
                .height(heightDp.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SingleMeterBar(level = animLeft, modifier = Modifier.weight(1f).fillMaxHeight())
            Spacer(modifier = Modifier.width(2.dp))
            SingleMeterBar(level = animRight, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
fun SingleMeterBar(
    level: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(2.dp)).background(StudioSurfaceVariant)) {
        val totalHeight = size.height
        val totalWidth = size.width
        val fillHeight = (level * totalHeight).coerceIn(0f, totalHeight)

        // Draw segments from bottom to top
        val numSegments = 16
        val segmentGap = 1.5.dp.toPx()
        val segmentHeight = (totalHeight - (numSegments - 1) * segmentGap) / numSegments

        for (i in 0 until numSegments) {
            val segBottom = totalHeight - i * (segmentHeight + segmentGap)
            val segTop = segBottom - segmentHeight
            val segProgress = i.toFloat() / numSegments

            val color = when {
                segProgress >= 0.90f -> StudioRed
                segProgress >= 0.70f -> StudioYellow
                else -> StudioGreen
            }

            // Determine if segment is active
            val isActive = (totalHeight - segTop) <= fillHeight

            drawRoundRect(
                color = if (isActive) color else StudioSurfaceElevated.copy(alpha = 0.4f),
                topLeft = Offset(0f, segTop),
                size = Size(totalWidth, segmentHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}
