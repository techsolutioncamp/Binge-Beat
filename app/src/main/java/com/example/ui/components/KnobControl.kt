package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.*

/**
 * Tactile Rotary Audio Knob control with vertical drag sensitivity,
 * glowing arc indicator, and centered readout.
 */
@Composable
fun KnobControl(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    displayValue: String? = null,
    activeColor: Color = StudioPrimary,
    sizeDp: Int = 64
) {
    var dragAccumulator by remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        dragAccumulator = value
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Upwards drag increases value
                        val delta = -dragAmount / 200f
                        val newValue = (dragAccumulator + delta).coerceIn(0f, 1f)
                        dragAccumulator = newValue
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                val startAngle = 135f
                val sweepAngle = 270f
                val valueAngle = sweepAngle * value.coerceIn(0f, 1f)

                // Background track
                drawArc(
                    color = StudioSurfaceBorder,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active glowing arc
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = valueAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                )

                // Indicator needle dot
                val currentRad = Math.toRadians((startAngle + valueAngle).toDouble())
                val needleX = center.x + radius * cos(currentRad).toFloat()
                val needleY = center.y + radius * sin(currentRad).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(needleX, needleY)
                )
            }

            // Center value text
            Text(
                text = displayValue ?: "${(value * 100).toInt()}%",
                fontSize = (sizeDp * 0.17).sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            maxLines = 1
        )
    }
}
