package app.repeatless.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.repeatless.audio.AudioPlayer
import app.repeatless.audio.AudioRecorder
import app.repeatless.audio.AudioTrimmer
import app.repeatless.audio.RecordingState
import app.repeatless.ui.components.AmplitudeVisualizer
import app.repeatless.ui.components.AudioTrimControl
import app.repeatless.ui.components.ColorPicker
import app.repeatless.ui.theme.AmberAccent
import app.repeatless.ui.theme.PadHexStrings
import app.repeatless.ui.theme.RecRed
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordBottomSheet(
    recorder: AudioRecorder,
    audioPlayer: AudioPlayer,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSaveClip: (label: String, colorHex: String, tempFile: File, durationMs: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recordingStatus by recorder.status.collectAsState()
    val view = LocalView.current

    var labelInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(PadHexStrings.random()) }
    var showSaveForm by remember { mutableStateOf(false) }

    var trimStartMs by remember(recordingStatus.tempFile) { mutableStateOf(0f) }
    var trimEndMs by remember(recordingStatus.tempFile, recordingStatus.durationMs) { mutableStateOf(recordingStatus.durationMs.toFloat()) }
    var finalClipFile by remember(recordingStatus.tempFile) { mutableStateOf<File?>(null) }
    var finalClipDurationMs by remember(recordingStatus.durationMs) { mutableStateOf(recordingStatus.durationMs) }

    val playbackState by audioPlayer.playbackState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            audioPlayer.stop()
            recorder.cancelRecording()
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
            Text(
                text = when {
                    showSaveForm -> "Name Your Sound"
                    recordingStatus.state == RecordingState.RECORDING -> "Recording Sound..."
                    recordingStatus.state == RecordingState.REVIEWING -> "Review Clip"
                    else -> "Record New Clip"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!showSaveForm) {
                // Waveform Amplitude Visualizer
                AmplitudeVisualizer(
                    amplitude = recordingStatus.amplitudeNormalized,
                    amplitudes = recordingStatus.amplitudes,
                    isRecording = recordingStatus.state == RecordingState.RECORDING,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Timer Display
                Text(
                    text = when (recordingStatus.state) {
                        RecordingState.RECORDING -> "${recordingStatus.remainingSeconds}s remaining"
                        RecordingState.REVIEWING -> "${recordingStatus.durationMs / 1000f}s captured"
                        else -> "Max 30 seconds"
                    },
                    color = if (recordingStatus.state == RecordingState.RECORDING) RecRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Controls based on Recording State
                when (recordingStatus.state) {
                    RecordingState.IDLE -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(RecRed)
                                .clickable {
                                    if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    recorder.startRecording()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start Record",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Tap to Record",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    RecordingState.RECORDING -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(RecRed)
                                .clickable {
                                    if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    recorder.stopRecordingForReview()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Record",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    RecordingState.REVIEWING -> {
                        // Trimming Tool UI
                        AudioTrimControl(
                            totalDurationMs = recordingStatus.durationMs,
                            trimStartMs = trimStartMs,
                            trimEndMs = trimEndMs,
                            onTrimChange = { start, end ->
                                trimStartMs = start
                                trimEndMs = end
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play / Stop Review with Trim Range
                            IconButton(
                                onClick = {
                                    val tempFile = recordingStatus.tempFile
                                    if (tempFile != null) {
                                        audioPlayer.playRange("preview", tempFile, trimStartMs.toLong(), trimEndMs.toLong())
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(AmberAccent)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Trimmed Audio",
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Re-do
                            IconButton(
                                onClick = {
                                    audioPlayer.stop()
                                    recorder.startRecording()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-do Recording",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                audioPlayer.stop()
                                val rawFile = recordingStatus.tempFile
                                if (rawFile != null && rawFile.exists()) {
                                    val calculatedDuration = (trimEndMs - trimStartMs).toLong().coerceAtLeast(100L)
                                    if (trimStartMs > 0f || trimEndMs < recordingStatus.durationMs.toFloat() - 50f) {
                                        val trimmedFile = File(rawFile.parentFile, "trimmed_${System.currentTimeMillis()}.m4a")
                                        val success = AudioTrimmer.trim(rawFile, trimmedFile, trimStartMs.toLong(), trimEndMs.toLong())
                                        if (success && trimmedFile.exists() && trimmedFile.length() > 0) {
                                            finalClipFile = trimmedFile
                                            finalClipDurationMs = calculatedDuration
                                        } else {
                                            finalClipFile = rawFile
                                            finalClipDurationMs = recordingStatus.durationMs
                                        }
                                    } else {
                                        finalClipFile = rawFile
                                        finalClipDurationMs = recordingStatus.durationMs
                                    }
                                }
                                showSaveForm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                        ) {
                            Text("Use This Recording", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Save Form: Label & Color Selection
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Pad Label") },
                    placeholder = { Text("e.g. Brush Teeth, Well Done!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RecRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick Pad Color",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                ColorPicker(
                    selectedHex = selectedColorHex,
                    onColorSelected = { selectedColorHex = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val targetFile = finalClipFile ?: recordingStatus.tempFile
                        if (targetFile != null && targetFile.exists()) {
                            onSaveClip(
                                labelInput.ifBlank { "Clip" },
                                selectedColorHex,
                                targetFile,
                                finalClipDurationMs
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Save Pad", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    audioPlayer.stop()
                    recorder.cancelRecording()
                    onDismiss()
                }
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
