package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BoardEntity
import com.example.ui.theme.RecRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardManagementDialog(
    boards: List<BoardEntity>,
    activeBoardId: String,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onSelectBoard: (BoardEntity) -> Unit,
    onCreateBoard: (name: String) -> Unit,
    onRenameBoard: (boardId: String, newName: String) -> Unit,
    onDeleteBoard: (boardId: String) -> Unit,
    onShowProUpsell: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showCreateDialog by remember { mutableStateOf(false) }
    var newBoardName by remember { mutableStateOf("") }

    var renamingBoard by remember { mutableStateOf<BoardEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var deletingBoard by remember { mutableStateOf<BoardEntity?>(null) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soundboards",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (!isPro && boards.size >= 2) {
                            onShowProUpsell()
                        } else {
                            showCreateDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Board")
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("New Board", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(boards) { board ->
                    val isSelected = board.id == activeBoardId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                onSelectBoard(board)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = board.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = RecRed
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Rename Button
                        IconButton(
                            onClick = {
                                renamingBoard = board
                                renameInput = board.name
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename Board", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Delete Button
                        if (boards.size > 1) {
                            IconButton(
                                onClick = { deletingBoard = board }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Board", tint = RecRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Board Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Soundboard") },
            text = {
                OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { newBoardName = it },
                    label = { Text("Board Name") },
                    placeholder = { Text("e.g. Kids, Work, Memes") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RecRed)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBoardName.isNotBlank()) {
                            onCreateBoard(newBoardName.trim())
                            showCreateDialog = false
                            newBoardName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Board Dialog
    renamingBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { renamingBoard = null },
            title = { Text("Rename Soundboard") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RecRed)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameBoard(board.id, renameInput.trim())
                            renamingBoard = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingBoard = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Board Dialog
    deletingBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { deletingBoard = null },
            title = { Text("Delete Soundboard?") },
            text = { Text("Are you sure you want to delete '${board.name}' and all of its recorded pads?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBoard(board.id)
                        deletingBoard = null
                    }
                ) {
                    Text("Delete", color = RecRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBoard = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
