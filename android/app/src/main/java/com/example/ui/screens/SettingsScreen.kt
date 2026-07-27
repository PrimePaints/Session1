package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.UserTier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserPreferences
import com.example.data.repository.SoundboardRepository
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    repository: SoundboardRepository,
    onBackClick: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onShowProUpsell: () -> Unit,
    onNavigateToAutoParentSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val allPads by repository.allPads.collectAsState(initial = emptyList())
    val tagsMap = remember(allPads) {
        allPads.mapNotNull { pad ->
            pad.triggerTag?.takeIf { it.isNotBlank() }?.let { it to pad }
        }.groupBy({ it.first }, { it.second })
    }

    var renameTagTarget by remember { mutableStateOf<String?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    var mergeSourceTag by remember { mutableStateOf<String?>(null) }
    var mergeTargetTag by remember { mutableStateOf<String?>(null) }

    var deleteTagTarget by remember { mutableStateOf<String?>(null) }

    // SAF Document Creator launcher for Backup
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonText = repository.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonText.toByteArray())
                    }
                    snackbarHostState.showSnackbar("Backup saved successfully!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Backup failed: ${e.message}")
                }
            }
        }
    }

    // SAF Document Picker launcher for Restore
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            restoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Backup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // MEMBERSHIP TIER SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (userPreferences.tier) {
                        UserTier.FREE -> MaterialTheme.colorScheme.surfaceVariant
                        UserTier.PRO -> AmberAccent.copy(alpha = 0.12f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (userPreferences.tier) {
                        UserTier.FREE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        else -> AmberAccent
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Tier",
                        tint = AmberAccent,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (userPreferences.tier) {
                                UserTier.FREE -> "Standard Tier (Free)"
                                UserTier.PRO -> "Repeatless PRO Active"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (userPreferences.tier) {
                                UserTier.FREE -> "Basic soundboard & local pads"
                                UserTier.PRO -> "Auto-Parent AI + Unlimited Boards & Trimming"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onShowProUpsell,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Manage Plan", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }

            // PREFERENCES
            Text(
                text = "PREFERENCES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Haptics Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = RecRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Haptic Vibration",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = userPreferences.hapticsEnabled,
                            onCheckedChange = onSetHaptics,
                            colors = SwitchDefaults.colors(checkedThumbColor = RecRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = AmberAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme Mode",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeModeOption(
                        title = "Console Dark (Default)",
                        selected = userPreferences.themeMode == ThemeMode.CONSOLE_DARK,
                        onSelect = { onSetThemeMode(ThemeMode.CONSOLE_DARK) }
                    )
                    ThemeModeOption(
                        title = "Console Light",
                        selected = userPreferences.themeMode == ThemeMode.CONSOLE_LIGHT,
                        onSelect = { onSetThemeMode(ThemeMode.CONSOLE_LIGHT) }
                    )
                    ThemeModeOption(
                        title = "System Default",
                        selected = userPreferences.themeMode == ThemeMode.SYSTEM,
                        onSelect = { onSetThemeMode(ThemeMode.SYSTEM) }
                    )
                }
            }

            // AUTO-PARENT AI FEATURE SETTINGS
            Text(
                text = "AUTO-PARENT AI FEATURE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable { onNavigateToAutoParentSettings() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Auto-Parent AI",
                            tint = AmberAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Parent AI Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sensitivity, auto-play response, test simulator & trigger history",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate to Auto-Parent Settings",
                        tint = AmberAccent
                    )
                }
            }

            // SOUNDBOARD TAGS & TFLITE TRIGGERS
            Text(
                text = "SOUNDBOARD TAGS & TFLITE TRIGGERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "Triggers", tint = AmberAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acoustic Trigger Tag Management", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Manage audio tags used for instant TFLite local trigger classification", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (tagsMap.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No trigger tags created yet.\nAssign tags to sound clips in clip options (e.g. ⚡ yelling, 🥦 refusing food) to enable local TFLite classification.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        tagsMap.forEach { (tagName, padsWithTag) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AmberAccent.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "⚡ $tagName",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AmberAccent
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "(${padsWithTag.size} clip${if (padsWithTag.size > 1) "s" else ""})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                renameTagTarget = tagName
                                                renameInputText = tagName
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename Tag", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                mergeSourceTag = tagName
                                                val otherTags = tagsMap.keys.filter { !it.equals(tagName, ignoreCase = true) }
                                                mergeTargetTag = otherTags.firstOrNull() ?: ""
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.CallMerge, contentDescription = "Merge Tag", tint = AmberAccent, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { deleteTagTarget = tagName },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Tag", tint = RecRed, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Clips: " + padsWithTag.joinToString(", ") { "\"${it.label}\"" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DATA & BACKUP
            Text(
                text = "DATA & PORTABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Export Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportLauncher.launch("soundboard_backup.json") }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = RecRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export Backup JSON", fontWeight = FontWeight.SemiBold)
                            Text("Save recordings and soundboards to a local file", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Import Restore
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importLauncher.launch(arrayOf("application/json", "*/*")) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = AmberAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Restore Backup / Web Import", fontWeight = FontWeight.SemiBold)
                            Text("Import Soundboard JSON (v1 Web prototype or v2 file)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ABOUT & PRIVACY
            Text(
                text = "ABOUT & PRIVACY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyPolicy = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Privacy Policy", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("About Repeatless v1.0", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Restore Confirm Dialog
    if (showRestoreConfirmDialog && restoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Soundboard Data") },
            text = { Text("Do you want to MERGE with existing soundboards or REPLACE all existing data?") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = restoreUri ?: return@Button
                        showRestoreConfirmDialog = false
                        scope.launch {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val result = repository.importBackupJson(stream, replaceAll = false)
                                    result.fold(
                                        onSuccess = { count ->
                                            snackbarHostState.showSnackbar("Successfully restored $count sound pads!")
                                        },
                                        onFailure = { err ->
                                            snackbarHostState.showSnackbar("Import failed: ${err.message}")
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error opening backup file: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val uri = restoreUri ?: return@TextButton
                        showRestoreConfirmDialog = false
                        scope.launch {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val result = repository.importBackupJson(stream, replaceAll = true)
                                    result.fold(
                                        onSuccess = { count ->
                                            snackbarHostState.showSnackbar("Replaced data with $count sound pads!")
                                        },
                                        onFailure = { err ->
                                            snackbarHostState.showSnackbar("Import failed: ${err.message}")
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error opening backup file: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Replace All", color = RecRed)
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Repeatless is designed with privacy as a foundational principle.\n\n" +
                    "• Your recordings stay on your device: all audio clips, labels, and soundboards are stored only on this device and are never uploaded.\n" +
                    "• Microphone: used to record your clips, and — only while you turn on Auto-Parent AI listening — to transcribe nearby speech to text on your device.\n" +
                    "• AI matching sends text only: when Auto-Parent AI is enabled, the on-device transcript (never the audio itself) is sent to Google's Gemini API to choose which of your clips to play.\n" +
                    "• You stay in control: Auto-Parent AI is off unless you turn it on, and you can export or delete your data at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text("Close")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Repeatless") },
            text = {
                Text(
                    "Repeatless v1.0\n" +
                    "Say it once. Tap it forever.\n\n" +
                    "Designed for parents, teachers, coaches, and daily repeating voice clips.\n\n" +
                    "Built with Kotlin, Jetpack Compose, Material 3, and Room Database."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Rename Tag Dialog
    if (renameTagTarget != null) {
        val target = renameTagTarget!!
        AlertDialog(
            onDismissRequest = { renameTagTarget = null },
            title = { Text("Rename Tag \"$target\"") },
            text = {
                Column {
                    Text("Enter new name for this trigger tag. All associated audio clips will be updated:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        label = { Text("Tag Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInputText
                        renameTagTarget = null
                        if (newName.isNotBlank()) {
                            scope.launch {
                                repository.renameTriggerTag(target, newName)
                                snackbarHostState.showSnackbar("Tag renamed to \"$newName\"")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTagTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Merge Tag Dialog
    if (mergeSourceTag != null) {
        val source = mergeSourceTag!!
        val otherTags = tagsMap.keys.filter { !it.equals(source, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { mergeSourceTag = null },
            title = { Text("Merge Tag \"$source\"") },
            text = {
                Column {
                    Text("Select or type a target tag to merge \"$source\" into. All associated clips will be re-tagged:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (otherTags.isNotEmpty()) {
                        Text("Existing Tags:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        otherTags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mergeTargetTag = tag }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mergeTargetTag?.equals(tag, ignoreCase = true) == true,
                                    onClick = { mergeTargetTag = tag },
                                    colors = RadioButtonDefaults.colors(selectedColor = AmberAccent)
                                )
                                Text("⚡ $tag", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = mergeTargetTag ?: "",
                        onValueChange = { mergeTargetTag = it },
                        singleLine = true,
                        label = { Text("Target Tag Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = mergeTargetTag
                        mergeSourceTag = null
                        if (!target.isNullOrBlank()) {
                            scope.launch {
                                repository.mergeTriggerTags(source, target)
                                snackbarHostState.showSnackbar("Merged \"$source\" into \"$target\"")
                            }
                        }
                    },
                    enabled = !mergeTargetTag.isNullOrBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("Merge Tags")
                }
            },
            dismissButton = {
                TextButton(onClick = { mergeSourceTag = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Tag Dialog
    if (deleteTagTarget != null) {
        val target = deleteTagTarget!!
        val clipsCount = tagsMap[target]?.size ?: 0
        AlertDialog(
            onDismissRequest = { deleteTagTarget = null },
            title = { Text("Delete Tag \"$target\"?") },
            text = {
                Text("This will remove the trigger tag from $clipsCount clip(s). The recorded audio clips themselves will NOT be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTagTarget = null
                        scope.launch {
                            repository.deleteTriggerTag(target)
                            snackbarHostState.showSnackbar("Deleted tag \"$target\"")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)
                ) {
                    Text("Delete Tag")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTagTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun ThemeModeOption(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = RecRed)
        )
        Text(text = title, fontSize = 14.sp)
    }
}
