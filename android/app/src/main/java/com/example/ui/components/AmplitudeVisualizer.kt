package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed

@Composable
fun AmplitudeVisualizer(
    amplitude: Float,
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedAmp by animateFloatAsState(
        targetValue = if (isRecording) amplitude.coerceIn(0.05f, 1f) else 0.05f,
        animationSpec = tween(durationMillis = 80)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 40
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(4.dp.toPx())

        // Show the latest 'barCount' amplitudes or the static waveform if reviewing
        val displayAmplitudes = if (isRecording) {
            val list = amplitudes.takeLast(barCount).toMutableList()
            while (list.size < barCount) {
                list.add(0, 0.05f) // Fill initial space with minimum height
            }
            // Overwrite the last one with the animated current amplitude
            if (list.isNotEmpty()) {
                list[list.lastIndex] = animatedAmp
            }
            list
        } else {
            // For trimming/reviewing, we scale the full amplitude history to fit 'barCount'
            val list = mutableListOf<Float>()
            if (amplitudes.isNotEmpty()) {
                val chunkSize = (amplitudes.size / barCount.toFloat()).coerceAtLeast(1f)
                for (i in 0 until barCount) {
                    val index = (i * chunkSize).toInt().coerceIn(0, amplitudes.lastIndex)
                    list.add(amplitudes[index].coerceAtLeast(0.05f))
                }
            } else {
                for (i in 0 until barCount) {
                    list.add(0.05f)
                }
            }
            list
        }

        for (i in 0 until barCount) {
            val x = i * (barWidth + barSpacing)
            val ampValue = displayAmplitudes.getOrNull(i) ?: 0.05f
            val barHeight = (height * ampValue).coerceIn(4.dp.toPx(), height * 0.9f)
            val top = centerY - barHeight / 2f

            val color = if (i % 2 == 0) RecRed else AmberAccent

            drawRoundRect(
                color = if (isRecording) color else Color.Gray.copy(alpha = 0.5f),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
