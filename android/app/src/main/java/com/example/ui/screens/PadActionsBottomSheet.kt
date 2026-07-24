package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.db.BoardEntity
import com.example.data.db.PadEntity
import com.example.ui.components.ColorPicker
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PadActionsBottomSheet(
    pad: PadEntity,
    boards: List<BoardEntity> = emptyList(),
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onReRecord: () -> Unit,
    onTrimEdit: (() -> Unit)? = null,
    onMoveToBoard: ((targetBoardId: String) -> Unit)? = null,
    onRenameColorSave: (newLabel: String, newColorHex: String, triggerTag: String?) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var labelInput by remember(pad.label) { mutableStateOf(pad.label) }
    var selectedColorHex by remember(pad.colorHex) { mutableStateOf(pad.colorHex) }
    var selectedTriggerTag by remember(pad.triggerTag) { mutableStateOf<String?>(pad.triggerTag) }
    var customTriggerInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveBoardPicker by remember { mutableStateOf(false) }

    val otherBoards = remember(boards, pad.boardId) {
        boards.filter { it.id != pad.boardId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Pad Options",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Play
            TextButton(
                onClick = {
                    onPlay()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RecRed)
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text("Play Sound", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
            }

            // Adjust Trim / Re-trim
            if (onTrimEdit != null) {
                TextButton(
                    onClick = {
                        onTrimEdit()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = AmberAccent)
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Adjust Trim / Edit Audio Range", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Move to Soundboard
            if (onMoveToBoard != null && otherBoards.isNotEmpty()) {
                TextButton(
                    onClick = { showMoveBoardPicker = !showMoveBoardPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = AmberAccent)
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Move to Soundboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (showMoveBoardPicker) {
                    Text(
                        text = "Select Destination Soundboard:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        otherBoards.forEach { board ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmberAccent.copy(alpha = 0.15f))
                                    .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onMoveToBoard(board.id)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = board.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Quick Re-record
            TextButton(
                onClick = {
                    onReRecord()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = RecRed)
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text("Re-record Audio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rename Label Input
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it },
                label = { Text("Pad Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RecRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selector
            Text(
                text = "Pad Color",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            ColorPicker(
                selectedHex = selectedColorHex,
                onColorSelected = { selectedColorHex = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TFLite Acoustic Trigger Tag Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Trigger Tag",
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Local TFLite Trigger Tag",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tag this clip as an acoustic trigger for instant local classification (e.g., yelling, whining, or requests):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset trigger tag chips
                    val triggerOptions = listOf(
                        "⚡ Yelling" to "yelling",
                        "🗣️ Whining/Nagging" to "nagging",
                        "💬 Spoken Request" to "talking",
                        "🥦 Refusing Food" to "refusing_food",
                        "🛌 Bedtime" to "bedtime",
                        "📵 Screen Time" to "screens",
                        "🧸 Sharing Toys" to "sharing_toys"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        triggerOptions.forEach { (displayLabel, tagValue) ->
                            val isSelected = selectedTriggerTag?.lowercase() == tagValue.lowercase()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AmberAccent else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AmberAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedTriggerTag = if (isSelected) null else tagValue
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = displayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Clear Tag Chip
                        if (selectedTriggerTag != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RecRed.copy(alpha = 0.15f))
                                    .border(1.dp, RecRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { selectedTriggerTag = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "❌ Clear Trigger",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RecRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom trigger tag input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customTriggerInput,
                            onValueChange = { customTriggerInput = it },
                            placeholder = { Text("Or enter custom trigger...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                        Button(
                            onClick = {
                                if (customTriggerInput.isNotBlank()) {
                                    selectedTriggerTag = customTriggerInput.trim()
                                    customTriggerInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (selectedTriggerTag != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Active Trigger Tag: \"$selectedTriggerTag\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = RecRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Delete Pad", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onRenameColorSave(labelInput, selectedColorHex, selectedTriggerTag)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Pad?") },
            text = { Text("Are you sure you want to delete '${pad.label}'? The recording will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = RecRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
