package app.repeatless.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.repeatless.ui.theme.RecRed
import java.util.Locale

@Composable
fun AudioTrimControl(
    totalDurationMs: Long,
    trimStartMs: Float,
    trimEndMs: Float,
    onTrimChange: (startMs: Float, endMs: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxMs = totalDurationMs.toFloat().coerceAtLeast(200f)
    val start = trimStartMs.coerceIn(0f, maxMs - 100f)
    val end = trimEndMs.coerceIn(start + 100f, maxMs)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Trim Clip",
                    tint = RecRed,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Trim Start / End",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                val durationSec = (end - start) / 1000f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(RecRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fs clip", durationSec),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = RecRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Range Slider for visual trimming
            RangeSlider(
                value = start..end,
                onValueChange = { range ->
                    val minGap = 100f
                    var newStart = range.start
                    var newEnd = range.endInclusive
                    if (newEnd - newStart < minGap) {
                        newEnd = (newStart + minGap).coerceAtMost(maxMs)
                    }
                    onTrimChange(newStart, newEnd)
                },
                valueRange = 0f..maxMs,
                colors = SliderDefaults.colors(
                    activeTrackColor = RecRed,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thumbColor = RecRed
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fine tuning controls for Start & End
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Time Adjuster
                TrimAdjuster(
                    label = "Start",
                    timeSec = start / 1000f,
                    onMinus = { onTrimChange((start - 100f).coerceAtLeast(0f), end) },
                    onPlus = { onTrimChange((start + 100f).coerceAtMost(end - 100f), end) }
                )

                // End Time Adjuster
                TrimAdjuster(
                    label = "End",
                    timeSec = end / 1000f,
                    onMinus = { onTrimChange(start, (end - 100f).coerceAtLeast(start + 100f)) },
                    onPlus = { onTrimChange(start, (end + 100f).coerceAtMost(maxMs)) }
                )
            }
        }
    }
}

@Composable
private fun TrimAdjuster(
    label: String,
    timeSec: Float,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
            onClick = onMinus,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease $label",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = String.format(Locale.US, "%.1fs", timeSec),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onPlus,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase $label",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
