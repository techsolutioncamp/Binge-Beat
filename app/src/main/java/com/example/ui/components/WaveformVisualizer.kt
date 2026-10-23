package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurfaceVariant
import kotlin.math.max

@Composable
fun WaveformVisualizer(
    peaks: List<Float>,
    modifier: Modifier = Modifier,
    waveColor: Color = StudioPrimary,
    backgroundColor: Color = StudioSurfaceVariant
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        if (peaks.isEmpty()) {
            // Draw center baseline
            drawLine(
                color = waveColor.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val centerY = size.height / 2
        val maxAmp = (size.height / 2) * 0.9f
        val stepX = size.width / max(1, peaks.size)

        for (i in peaks.indices) {
            val x = i * stepX + stepX / 2
            val amp = (peaks[i] * maxAmp).coerceIn(2.dp.toPx(), maxAmp)

            drawLine(
                color = waveColor,
                start = Offset(x, centerY - amp),
                end = Offset(x, centerY + amp),
                strokeWidth = max(2.dp.toPx(), stepX * 0.7f),
                cap = StrokeCap.Round
            )
        }
    }
}
