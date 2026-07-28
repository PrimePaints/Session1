package app.repeatless

import android.os.Bundle
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.repeatless.audio.AudioPlayer
import app.repeatless.billing.BillingManager
import app.repeatless.audio.AudioRecorder
import app.repeatless.audio.AutoParentEngine
import app.repeatless.data.preferences.UserPreferences
import app.repeatless.data.preferences.UserPreferencesRepository
import app.repeatless.data.repository.SoundboardRepository
import app.repeatless.ui.screens.AutoParentSettingsScreen
import app.repeatless.ui.screens.BoardManagementDialog
import app.repeatless.ui.screens.BoardScreen
import app.repeatless.ui.screens.ProUpsellDialog
import app.repeatless.ui.screens.SettingsScreen
import app.repeatless.ui.theme.RepeatlessTheme
import app.repeatless.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: SoundboardRepository
    private lateinit var preferencesRepo: UserPreferencesRepository
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var autoParentEngine: AutoParentEngine
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SoundboardRepository(applicationContext)
        preferencesRepo = UserPreferencesRepository(applicationContext)
        audioPlayer = AudioPlayer(applicationContext)
        audioRecorder = AudioRecorder(applicationContext)
        autoParentEngine = AutoParentEngine(applicationContext, audioPlayer)
        billingManager = BillingManager(applicationContext) { owned ->
            lifecycleScope.launch { preferencesRepo.setProEntitlement(owned) }
        }
        billingManager.connect()

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
            val billingState by billingManager.state.collectAsState()

            var showBoardManagement by remember { mutableStateOf(false) }
            var showProUpsell by remember { mutableStateOf(false) }

            // Surface one-shot billing messages (purchase result, restore, pending).
            LaunchedEffect(billingState.message) {
                billingState.message?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    billingManager.consumeMessage()
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    autoParentEngine.stopListening()
                    audioPlayer.release()
                }
            }

            RepeatlessTheme(themeMode = userPrefs.themeMode) {
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
                            onShowProUpsell = { showProUpsell = true },
                            onNavigateToAutoParentSettings = {
                                navController.navigate("auto_parent_settings")
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

                // PRO purchase dialog (Google Play Billing)
                if (showProUpsell) {
                    ProUpsellDialog(
                        currentTier = userPrefs.tier,
                        proPrice = billingState.proPrice,
                        onDismiss = { showProUpsell = false },
                        onBuyPro = { billingManager.launchPurchase(this@MainActivity) },
                        onRestore = { billingManager.refreshPurchases(notifyUser = true) },
                        onDebugTogglePro = if (BuildConfig.DEBUG) {
                            { pro -> scope.launch { preferencesRepo.setProEntitlement(pro) } }
                        } else null
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        billingManager.destroy()
        super.onDestroy()
    }
}
