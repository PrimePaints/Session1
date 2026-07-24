package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayer
import com.example.data.db.PadEntity
import com.example.ui.components.AudioTrimControl
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadTrimBottomSheet(
    pad: PadEntity,
    sourceFile: File,
    audioPlayer: AudioPlayer,
    onDismiss: () -> Unit,
    onSaveTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playbackState by audioPlayer.playbackState.collectAsState()

    var totalDurationMs by remember { mutableStateOf(pad.durationMs.coerceAtLeast(1000L)) }

    LaunchedEffect(sourceFile) {
        if (sourceFile.exists()) {
            try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(sourceFile.absolutePath)
                val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dur = durStr?.toLongOrNull() ?: pad.durationMs
                if (dur > 0L) {
                    totalDurationMs = dur
                }
                mmr.release()
            } catch (_: Exception) {}
        }
    }

    var trimStartMs by remember(totalDurationMs) { mutableStateOf(0f) }
    var trimEndMs by remember(totalDurationMs) { mutableStateOf(totalDurationMs.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = {
            audioPlayer.stop()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = RecRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Adjust Trim: ${pad.label}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AudioTrimControl(
                totalDurationMs = totalDurationMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                onTrimChange = { start, end ->
                    trimStartMs = start
                    trimEndMs = end
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Play Range Preview
            IconButton(
                onClick = {
                    if (sourceFile.exists()) {
                        audioPlayer.playRange("trim_edit", sourceFile, trimStartMs.toLong(), trimEndMs.toLong())
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AmberAccent)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Preview Trim Range",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    audioPlayer.stop()
                    onSaveTrim(trimStartMs.toLong(), trimEndMs.toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed)
            ) {
                Text("Save Trimmed Audio", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    audioPlayer.stop()
                    onDismiss()
                }
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
