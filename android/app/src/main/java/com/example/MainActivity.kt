package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.AutoParentEngine
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserTier
import com.example.data.repository.SoundboardRepository
import com.example.smarthome.SmartHomeManager
import com.example.ui.screens.AutoParentSettingsScreen
import com.example.ui.screens.BoardManagementDialog
import com.example.ui.screens.BoardScreen
import com.example.ui.screens.ProUpsellDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartHomeSettingsScreen
import com.example.ui.theme.SoundboardTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: SoundboardRepository
    private lateinit var preferencesRepo: UserPreferencesRepository
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var autoParentEngine: AutoParentEngine
    private lateinit var smartHomeManager: SmartHomeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SoundboardRepository(applicationContext)
        preferencesRepo = UserPreferencesRepository(applicationContext)
        audioPlayer = AudioPlayer(applicationContext)
        audioRecorder = AudioRecorder(applicationContext)
        autoParentEngine = AutoParentEngine(applicationContext, audioPlayer)
        smartHomeManager = SmartHomeManager(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                repository.ensureInitialData()
            }

            val userPrefs by preferencesRepo.userPreferencesFlow.collectAsState(
                initial = UserPreferences(
                    activeBoardId = null,
                    widgetBoardId = null,
                    hapticsEnabled = true,
                    themeMode = ThemeMode.CONSOLE_DARK,
                    isPro = false
                )
            )

            val boards by repository.boards.collectAsState(initial = emptyList())

            val activeBoard = remember(boards, userPrefs.activeBoardId) {
                boards.find { it.id == userPrefs.activeBoardId } ?: boards.firstOrNull()
            }

            val padsFlow = remember(activeBoard?.id) {
                activeBoard?.id?.let { repository.getPadsForBoard(it) }
            }
            val pads by (padsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

            val playbackState by audioPlayer.playbackState.collectAsState()

            var showBoardManagement by remember { mutableStateOf(false) }
            var showProUpsell by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                onDispose {
                    autoParentEngine.stopListening()
                    audioPlayer.release()
                }
            }

            SoundboardTheme(themeMode = userPrefs.themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "board",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("board") {
                        BoardScreen(
                            board = activeBoard,
                            boards = boards,
                            pads = pads,
                            userPreferences = userPrefs,
                            repository = repository,
                            audioPlayer = audioPlayer,
                            audioRecorder = audioRecorder,
                            autoParentEngine = autoParentEngine,
                            playbackState = playbackState,
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToAutoParentSettings = {
                                navController.navigate("auto_parent_settings")
                            },
                            onSelectBoard = { selectedBoard ->
                                scope.launch {
                                    preferencesRepo.setActiveBoardId(selectedBoard.id)
                                }
                            },
                            onShowBoardManagement = {
                                showBoardManagement = true
                            },
                            onShowProUpsell = {
                                showProUpsell = true
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            userPreferences = userPrefs,
                            repository = repository,
                            onBackClick = { navController.popBackStack() },
                            onSetHaptics = { enabled ->
                                scope.launch { preferencesRepo.setHapticsEnabled(enabled) }
                            },
                            onSetThemeMode = { themeMode ->
                                scope.launch { preferencesRepo.setThemeMode(themeMode) }
                            },
                            onToggleProEntitlement = { isPro ->
                                scope.launch { preferencesRepo.setProEntitlement(isPro) }
                            },
                            onToggleUltraEntitlement = { isUltra ->
                                scope.launch { preferencesRepo.setUltraEntitlement(isUltra) }
                            },
                            onNavigateToAutoParentSettings = {
                                navController.navigate("auto_parent_settings")
                            },
                            onNavigateToSmartHomeSettings = {
                                navController.navigate("smarthome_settings")
                            }
                        )
                    }

                    composable("auto_parent_settings") {
                        AutoParentSettingsScreen(
                            pads = pads,
                            autoParentEngine = autoParentEngine,
                            getAudioFile = { targetPad ->
                                repository.getAudioFileForPad(targetPad.audioFileName)
                            },
                            onBackClick = { navController.popBackStack() },
                            onShowProUpsell = { showProUpsell = true },
                            isPro = userPrefs.isPro
                        )
                    }

                    composable("smarthome_settings") {
                        SmartHomeSettingsScreen(
                            smartHomeManager = smartHomeManager,
                            onBackClick = { navController.popBackStack() },
                            onShowUltraUpsell = { showProUpsell = true },
                            isUltra = userPrefs.isUltra
                        )
                    }
                }

                // Board Management Sheet
                if (showBoardManagement) {
                    BoardManagementDialog(
                        boards = boards,
                        activeBoardId = activeBoard?.id ?: "",
                        isPro = userPrefs.isPro,
                        onDismiss = { showBoardManagement = false },
                        onSelectBoard = { board ->
                            scope.launch {
                                preferencesRepo.setActiveBoardId(board.id)
                            }
                        },
                        onCreateBoard = { name ->
                            scope.launch {
                                val result = repository.createBoard(name, userPrefs.isPro)
                                result.onSuccess { newBoard ->
                                    preferencesRepo.setActiveBoardId(newBoard.id)
                                }
                            }
                        },
                        onRenameBoard = { boardId, newName ->
                            scope.launch { repository.renameBoard(boardId, newName) }
                        },
                        onDeleteBoard = { boardId ->
                            scope.launch { repository.deleteBoard(boardId) }
                        },
                        onShowProUpsell = {
                            showBoardManagement = false
                            showProUpsell = true
                        }
                    )
                }

                // Tier Selection Dialog
                if (showProUpsell) {
                    ProUpsellDialog(
                        currentTier = userPrefs.tier,
                        onDismiss = { showProUpsell = false },
                        onSelectTier = { selectedTier ->
                            scope.launch {
                                when (selectedTier) {
                                    UserTier.FREE -> {
                                        preferencesRepo.setProEntitlement(false)
                                        preferencesRepo.setUltraEntitlement(false)
                                    }
                                    UserTier.PRO -> {
                                        preferencesRepo.setProEntitlement(true)
                                        preferencesRepo.setUltraEntitlement(false)
                                    }
                                    UserTier.ULTRA -> {
                                        preferencesRepo.setUltraEntitlement(true)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
