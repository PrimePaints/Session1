package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.AutoParentEngine
import com.example.audio.PlaybackState
import com.example.data.db.BoardEntity
import com.example.data.db.PadEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.SoundboardRepository
import com.example.ui.components.SoundPadItem
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    board: BoardEntity?,
    boards: List<BoardEntity>,
    pads: List<PadEntity>,
    userPreferences: UserPreferences,
    repository: SoundboardRepository,
    audioPlayer: AudioPlayer,
    audioRecorder: AudioRecorder,
    autoParentEngine: AutoParentEngine,
    playbackState: PlaybackState,
    onNavigateToSettings: () -> Unit,
    onNavigateToAutoParentSettings: () -> Unit = {},
    onSelectBoard: (BoardEntity) -> Unit,
    onShowBoardManagement: () -> Unit,
    onShowProUpsell: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    val autoParentState by autoParentEngine.state.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showRecordSheet by remember { mutableStateOf(false) }

    var selectedPadForActions by remember { mutableStateOf<PadEntity?>(null) }
    var padToReRecord by remember { mutableStateOf<PadEntity?>(null) }
    var padToTrimEdit by remember { mutableStateOf<PadEntity?>(null) }

    // Permission launcher for Mic
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showRecordSheet = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required to record voice clips.")
            }
        }
    }

    fun startRecordFlow() {
        if (!userPreferences.isPro && pads.size >= 12) {
            onShowProUpsell()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showRecordSheet = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShowBoardManagement() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Power Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RecRed)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = board?.name ?: "Soundboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Board",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Auto-Parent AI Rapid Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (autoParentState.isListening) AmberAccent else AmberAccent.copy(alpha = 0.15f))
                            .clickable {
                                if (userPreferences.isPro) {
                                    if (autoParentState.isListening) {
                                        autoParentEngine.stopListening()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Auto-Parent AI Listening Disabled")
                                        }
                                    } else {
                                        autoParentEngine.startListening(pads)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Auto-Parent AI Listening Active")
                                        }
                                    }
                                } else {
                                    onShowProUpsell()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (autoParentState.isListening) Color.White else AmberAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Auto-Parent AI",
                                tint = if (autoParentState.isListening) Color.White else AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (autoParentState.isListening) "Auto-Parent ON" else "Auto-Parent OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoParentState.isListening) Color.White else AmberAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onShowBoardManagement) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Board List",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto-Parent Settings") },
                            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberAccent) },
                            onClick = {
                                showMenu = false
                                onNavigateToAutoParentSettings()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Settings & Backup") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            }
                        )

                        if (!userPreferences.isPro) {
                            DropdownMenuItem(
                                text = { Text("Upgrade to Pro", fontWeight = FontWeight.Bold, color = AmberAccent) },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = AmberAccent) },
                                onClick = {
                                    showMenu = false
                                    onShowProUpsell()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { startRecordFlow() },
                containerColor = RecRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Record Sound", modifier = Modifier.size(28.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (autoParentState.isListening) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToAutoParentSettings() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ Auto-Parent Listening: ${autoParentState.statusMessage}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }
                }
            }

            if (pads.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = RecRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Sounds on this Board",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Say it once. Tap it forever. Tap the red record button below to create your first sound pad!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Pad Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pads, key = { _, item -> item.id }) { index, pad ->
                        val isThisPadPlaying = playbackState.activePadId == pad.id && playbackState.isPlaying

                        SoundPadItem(
                            pad = pad,
                            index = index,
                            isPlaying = isThisPadPlaying,
                            playProgress = if (isThisPadPlaying) playbackState.progress else 0f,
                            hapticsEnabled = userPreferences.hapticsEnabled,
                            onPlayClick = {
                                val audioFile = repository.getAudioFileForPad(pad.audioFileName)
                                audioPlayer.playPad(pad.id, audioFile)
                            },
                            onOptionsClick = {
                                selectedPadForActions = pad
                            }
                        )
                    }

                    // Dashed "+ Record" Tile
                    item {
                        DashedRecordPadTile(
                            onClick = { startRecordFlow() }
                        )
                    }
                }
            }
        }
    }

    // Record Flow Sheet
    if (showRecordSheet && board != null) {
        RecordBottomSheet(
            recorder = audioRecorder,
            audioPlayer = audioPlayer,
            hapticsEnabled = userPreferences.hapticsEnabled,
            onDismiss = {
                showRecordSheet = false
                padToReRecord = null
            },
            onSaveClip = { label, colorHex, tempFile, durationMs ->
                showRecordSheet = false
                scope.launch {
                    val targetPad = padToReRecord
                    if (targetPad != null) {
                        repository.reRecordPad(targetPad.id, tempFile, durationMs)
                        padToReRecord = null
                    } else {
                        val result = repository.addPad(
                            boardId = board.id,
                            label = label,
                            colorHex = colorHex,
                            tempAudioFile = tempFile,
                            durationMs = durationMs,
                            isPro = userPreferences.isPro
                        )
                        result.fold(
                            onSuccess = {
                                snackbarHostState.showSnackbar("Sound pad saved!")
                            },
                            onFailure = { err ->
                                snackbarHostState.showSnackbar(err.message ?: "Failed to save pad")
                            }
                        )
                    }
                }
            }
        )
    }

    // Pad Options Sheet
    selectedPadForActions?.let { pad ->
        PadActionsBottomSheet(
            pad = pad,
            boards = boards,
            onDismiss = { selectedPadForActions = null },
            onPlay = {
                val file = repository.getAudioFileForPad(pad.audioFileName)
                audioPlayer.playPad(pad.id, file)
            },
            onReRecord = {
                padToReRecord = pad
                startRecordFlow()
            },
            onTrimEdit = {
                padToTrimEdit = pad
            },
            onMoveToBoard = { targetBoardId ->
                scope.launch {
                    val result = repository.movePadToBoard(pad.id, targetBoardId)
                    result.fold(
                        onSuccess = {
                            val targetBoard = boards.find { b -> b.id == targetBoardId }
                            snackbarHostState.showSnackbar("Moved pad to '${targetBoard?.name ?: "soundboard"}'")
                        },
                        onFailure = { err ->
                            snackbarHostState.showSnackbar(err.message ?: "Failed to move pad")
                        }
                    )
                }
            },
            onRenameColorSave = { newLabel, newColorHex, triggerTag ->
                scope.launch {
                    repository.updatePad(pad.id, newLabel, newColorHex, triggerTag)
                    snackbarHostState.showSnackbar("Pad updated")
                }
            },
            onDelete = {
                scope.launch {
                    repository.deletePad(pad.id)
                    snackbarHostState.showSnackbar("Pad deleted")
                }
            }
        )
    }

    // Pad Trim Edit Sheet
    padToTrimEdit?.let { pad ->
        val rawFile = repository.getRawAudioFileForPad(pad)
        PadTrimBottomSheet(
            pad = pad,
            sourceFile = rawFile,
            audioPlayer = audioPlayer,
            onDismiss = { padToTrimEdit = null },
            onSaveTrim = { startMs, endMs ->
                padToTrimEdit = null
                scope.launch {
                    val result = repository.trimAndSavePad(pad.id, startMs, endMs)
                    result.fold(
                        onSuccess = {
                            snackbarHostState.showSnackbar("Trim updated successfully!")
                        },
                        onFailure = { err ->
                            snackbarHostState.showSnackbar(err.message ?: "Failed to save trim")
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun DashedRecordPadTile(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                width = 2.dp,
                color = RecRed.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RecRed)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Sound",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Record Sound",
                color = RecRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
