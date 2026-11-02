package com.steadfast.app.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.core.security.BiometricLockManager
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.ThemeSetting
import com.steadfast.app.ui.bible.ReadingLocationDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    currentTheme: ThemeSetting,
    isAppLockEnabled: Boolean,
    currentFocusId: String,
    focuses: List<RecoveryFocus>,
    activeSession: StreakSessionEntity?,
    readingGoal: Int,
    otReading: ReadingLocation?,
    ntReading: ReadingLocation?,
    backupManager: BackupManager,
    onThemeChanged: (ThemeSetting) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onFocusChanged: (String) -> Unit,
    onStreakStartChanged: (Long) -> Unit,
    onReadingGoalChanged: (Int) -> Unit,
    onClearOtReading: () -> Unit,
    onClearNtReading: () -> Unit,
    onReplayTutorial: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveStreakStartValidated: (suspend (Long) -> Result<Unit>)? = null,
    bibleBooks: List<BibleBook> = emptyList(),
    onSetOtReading: (Int, Int) -> Unit = { _, _ -> },
    onSetNtReading: (Int, Int) -> Unit = { _, _ -> },
    workflow: SettingsWorkflowModel = viewModel(),
    recoveryOnly: Boolean = false
) {
    val context = LocalContext.current
    val backupState by workflow.backupState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var authCapability by remember(context) { mutableStateOf(BiometricLockManager.checkAuthCapability(context)) }
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val scope = rememberCoroutineScope()

    var showFocusDialog by rememberSaveable { mutableStateOf(false) }
    var showStreakEditDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showOtReadingDialog by rememberSaveable { mutableStateOf(false) }
    var showNtReadingDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    var streakStartDraft by rememberSaveable(activeSession?.id, activeSession?.startEpochMillis) {
        mutableLongStateOf(activeSession?.startEpochMillis ?: 0L)
    }
    var streakSaveError by rememberSaveable { mutableStateOf<String?>(null) }
    var streakSaving by remember { mutableStateOf(false) }


    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authCapability = BiometricLockManager.checkAuthCapability(context)
                isResumed = true
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isResumed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(backupState.stage, backupState.resultMessage, isResumed) {
        if (backupState.stage == SettingsBackupStage.RESULT && isResumed) {
            workflow.takeResultMessage()?.let { message ->
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF Launchers
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        workflow.onExportPickerResult(uri)
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        workflow.onImportPickerResult(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (recoveryOnly) "Restore backup" else "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!recoveryOnly) {
            // Theme section
            item {
                SectionHeader("APPEARANCE")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ThemeSetting.SYSTEM to "System",
                                ThemeSetting.DARK to "Dark",
                                ThemeSetting.LIGHT to "Light"
                            ).forEach { (setting, label) ->
                                val isSelected = currentTheme == setting
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .selectable(selected = isSelected, role = Role.RadioButton) {
                                            onThemeChanged(setting)
                                        }
                                        .testTag("settings_theme_${setting.name}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security Section
            item {
                SectionHeader("SECURITY & APP LOCK")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Biometric / App Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Require authentication when opening app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = authCapabilityMessage(authCapability),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("settings_app_lock_guidance")
                                )
                            }
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = onAppLockChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = SteadfastGold.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("settings_app_lock_switch").semantics { contentDescription = "Biometric / App Lock" }
                        )
                    }
                }
            }

            // Recovery Configuration
            item {
                SectionHeader("RECOVERY CONFIGURATION")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        val focusName = focuses.find { it.id == currentFocusId }?.label ?: "Unknown recovery focus"
                        SettingsClickableRow(
                            icon = Icons.Default.SelfImprovement,
                            title = "Recovery Focus",
                            subtitle = focusName,
                            onClick = { showFocusDialog = true }
                        )
                        Text(
                            text = "Steadfast is a personal tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("settings_substance_safety_notice")
                        )

                        activeSession?.let { session ->
                            val zone = runCatching { ZoneId.of(session.startZoneId) }.getOrElse { ZoneId.systemDefault() }
                            val zdt = Instant.ofEpochMilli(streakStartDraft).atZone(zone)
                            val dFmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                            SettingsClickableRow(
                                icon = Icons.Default.DateRange,
                                title = "Edit Streak Start Date & Time",
                                subtitle = zdt.format(dFmt),
                                onClick = { streakSaveError = null; showStreakEditDialog = true },
                                modifier = Modifier.testTag("settings_streak_start_edit")
                            )
                        }
                    }
                }
            }

            // Scripture Preferences
            item {
                SectionHeader("SCRIPTURE PREFERENCES")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "Daily Chapter Intention",
                            subtitle = "$readingGoal+ chapters",
                            onClick = { showGoalDialog = true }
                        )

                        val otName = bibleBooks.firstOrNull { it.id == otReading?.bookId }?.name
                        val ntName = bibleBooks.firstOrNull { it.id == ntReading?.bookId }?.name
                        SettingsClickableRow(
                            icon = Icons.Default.Check,
                            title = if (otReading == null) "Set Old Testament Bookmark" else "Change Old Testament Bookmark",
                            subtitle = otReading?.let { "${otName ?: "Unknown book"}, chapter ${it.chapter}" } ?: "Choose a book and chapter",
                            onClick = { showOtReadingDialog = true },
                            enabled = bibleBooks.any { it.testament.equals("OT", true) },
                            modifier = Modifier.testTag("settings_set_ot_bookmark")
                        )
                        if (otReading != null) SettingsClickableRow(
                            icon = Icons.Default.Check,
                            title = "Clear Old Testament Bookmark",
                            subtitle = "${otName ?: "Unknown book"}, chapter ${otReading.chapter}",
                            onClick = onClearOtReading,
                            modifier = Modifier.testTag("settings_ot_bookmark")
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Check,
                            title = if (ntReading == null) "Set New Testament Bookmark" else "Change New Testament Bookmark",
                            subtitle = ntReading?.let { "${ntName ?: "Unknown book"}, chapter ${it.chapter}" } ?: "Choose a book and chapter",
                            onClick = { showNtReadingDialog = true },
                            enabled = bibleBooks.any { it.testament.equals("NT", true) },
                            modifier = Modifier.testTag("settings_set_nt_bookmark")
                        )
                        if (ntReading != null) SettingsClickableRow(
                            icon = Icons.Default.Check,
                            title = "Clear New Testament Bookmark",
                            subtitle = "${ntName ?: "Unknown book"}, chapter ${ntReading.chapter}",
                            onClick = onClearNtReading,
                            modifier = Modifier.testTag("settings_nt_bookmark")
                        )
                    }
                }
            }

            }
            // Backup & Restore Section
            item {
                SectionHeader("BACKUP & RESTORE (.STEADFAST)")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        if (!recoveryOnly) SettingsClickableRow(
                            icon = Icons.Default.Upload,
                            title = "Export Encrypted Backup",
                            subtitle = "Save portable .steadfast file with personal passphrase",
                            onClick = {
                                if (workflow.beginExportPicker()) exportFileLauncher.launch("steadfast_backup_${System.currentTimeMillis()}.steadfast")
                            }
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Download,
                            title = "Import & Restore Backup",
                            subtitle = "Replace data from encrypted .steadfast file",
                            onClick = {
                                if (workflow.beginImportPicker()) importFileLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }
            }

            // General & About
            if (!recoveryOnly) item {
                SectionHeader("ABOUT & PRIVACY")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Default.Refresh,
                            title = "Replay Product Tour",
                            subtitle = "Review the core pillars of Steadfast",
                            onClick = onReplayTutorial
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            title = "About Steadfast & Medical Safety",
                            subtitle = "Version ${com.steadfast.app.BuildConfig.VERSION_NAME} • Offline Privacy Policy",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("settings_about_row")
                        )
                    }
                }
            }
        }
    }

    // Focus Change Dialog
    if (showFocusDialog) {
        AlertDialog(
            onDismissRequest = { showFocusDialog = false },
            title = { Text("Change Recovery Focus", fontFamily = FontFamily.Serif) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp).selectableGroup()) {
                    items(focuses) { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = f.id == currentFocusId, role = Role.RadioButton) {
                                    onFocusChanged(f.id)
                                    showFocusDialog = false
                                }
                                .padding(vertical = 10.dp)
                                .testTag("settings_focus_${f.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = f.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (f.id == currentFocusId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFocusDialog = false }) { Text("Close") }
            }
        )
    }

    // Streak Start Edit Dialog with Strong Warning
    if (showStreakEditDialog && activeSession != null) {
        val zone = runCatching { ZoneId.of(activeSession.startZoneId) }.getOrElse { ZoneId.systemDefault() }
        val zdt = Instant.ofEpochMilli(streakStartDraft).atZone(zone)
        val dFmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

        AlertDialog(
            onDismissRequest = { if (!streakSaving) showStreakEditDialog = false },
            title = { Text("Edit Streak Start Time", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "WARNING: Modifying your streak start date alters your active streak counter and milestone progress. Only use this if you originally entered the wrong time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            val local = LocalDateTime.of(LocalDate.of(y, m + 1, d), LocalTime.of(hour, min))
                                            streakStartDraft = local.atZone(zone).toInstant().toEpochMilli()
                                            streakSaveError = null
                                        },
                                        zdt.hour,
                                        zdt.minute,
                                        false
                                    ).show()
                                },
                                zdt.year,
                                zdt.monthValue - 1,
                                zdt.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_streak_start_picker")
                    ) {
                        Text("Pick: ${zdt.format(dFmt)}")
                    }
                    streakSaveError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("settings_streak_start_error"))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validate = onSaveStreakStartValidated
                        if (validate == null) {
                            showStreakEditDialog = false
                            onStreakStartChanged(streakStartDraft)
                        } else if (!streakSaving) {
                            scope.launch {
                                streakSaving = true
                                val result = runCatching { validate(streakStartDraft) }
                                    .getOrElse { Result.failure(it) }
                                streakSaving = false
                                if (result.isSuccess) {
                                    showStreakEditDialog = false
                                } else {
                                    streakSaveError = if (result.exceptionOrNull() is IllegalArgumentException) {
                                        "Choose a valid start time that does not overlap your recovery history."
                                    } else {
                                        "Could not save the start time. Please try again."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !streakSaving,
                    modifier = Modifier.testTag("settings_streak_start_save"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(if (streakSaving) "Saving…" else "Save Start Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStreakEditDialog = false }, enabled = !streakSaving) { Text("Cancel") }
            }
        )
    }

    // Reading Goal Dialog
    if (showGoalDialog) {
        var tempGoal by remember { mutableIntStateOf(readingGoal) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Daily Reading Goal", fontFamily = FontFamily.Serif) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$tempGoal+ chapters per day",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempGoal.toFloat(),
                        onValueChange = { tempGoal = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReadingGoalChanged(tempGoal)
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    when (backupState.stage) {
        SettingsBackupStage.EXPORT_FORM, SettingsBackupStage.EXPORTING -> {
            val busy = backupState.stage == SettingsBackupStage.EXPORTING
            AlertDialog(onDismissRequest = { if (!busy) workflow.cancelBackupWorkflow() },
                title = { Text("Encrypt Export File", fontFamily = FontFamily.Serif, modifier = Modifier.testTag("settings_export_title")) },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Use at least 12 characters. Keep this passphrase safe: it cannot be recovered and is required to restore your backup on another device.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(backupState.passphrase, workflow::updateExportPassphrase, label = { Text("Export Passphrase") },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            enabled = !busy,
                            supportingText = { if (backupState.passphrase.isNotEmpty() && backupState.passphrase.length < BackupManager.MIN_PASSPHRASE_LENGTH) Text("Use at least 12 characters", color = MaterialTheme.colorScheme.error) },
                            isError = backupState.passphrase.isNotEmpty() && backupState.passphrase.length < BackupManager.MIN_PASSPHRASE_LENGTH,
                            modifier = Modifier.fillMaxWidth().testTag("settings_export_passphrase"))
                        OutlinedTextField(backupState.confirmation, workflow::updateExportConfirmation, label = { Text("Confirm Passphrase") },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("settings_export_confirmation"))
                        if (backupState.confirmation.isNotEmpty() && backupState.confirmation != backupState.passphrase) Text("Passphrases do not match.", color = MaterialTheme.colorScheme.error)
                        backupState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = { Button(onClick = { workflow.exportBackup(backupManager, context.applicationContext.contentResolver) },
                    enabled = backupState.passphrase.length >= BackupManager.MIN_PASSPHRASE_LENGTH && backupState.passphrase == backupState.confirmation && !busy,
                    modifier = Modifier.testTag("settings_export_confirm"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)) { Text(if (busy) "Exporting…" else "Export (.steadfast)") } },
                dismissButton = { TextButton(onClick = workflow::cancelBackupWorkflow, enabled = !busy) { Text("Cancel") } })
        }
        SettingsBackupStage.IMPORT_FORM, SettingsBackupStage.INSPECTING -> {
            val busy = backupState.stage == SettingsBackupStage.INSPECTING
            AlertDialog(onDismissRequest = { if (!busy) workflow.cancelBackupWorkflow() }, title = { Text("Unlock Backup File") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Enter the original passphrase to inspect this backup. Older backups may use shorter passphrases.")
                        OutlinedTextField(backupState.passphrase, workflow::updateImportPassphrase, label = { Text("Backup Passphrase") },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("settings_import_passphrase"))
                        backupState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = { Button(onClick = { workflow.inspectBackup(backupManager, context.applicationContext.contentResolver) }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)) { Text("Inspect & Preview") } },
                dismissButton = { TextButton(onClick = workflow::cancelBackupWorkflow, enabled = !busy) { Text("Cancel") } })
        }
        SettingsBackupStage.PREVIEW, SettingsBackupStage.RESTORING -> backupState.payload?.let { payload ->
            val busy = backupState.stage == SettingsBackupStage.RESTORING
            val focusName = backupPreviewFocusLabel(payload.settings.recoveryFocusId, focuses)
            AlertDialog(onDismissRequest = { if (!busy) workflow.cancelBackupWorkflow() }, title = { Text("Confirm Backup Restore", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("WARNING: Restoring will completely replace all current streaks, journal entries, and Bible progress with the contents below:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• Export Date: " + Instant.ofEpochMilli(payload.exportedAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")), style = MaterialTheme.typography.bodySmall)
                                Text("• Recovery Focus: " + focusName, style = MaterialTheme.typography.bodySmall)
                                Text("• Streak Sessions: " + payload.streakSessions.size + " records", style = MaterialTheme.typography.bodySmall)
                                Text("• Journal Entries: " + payload.journalEntries.size + " notes", style = MaterialTheme.typography.bodySmall)
                                Text("• Bible Progress: " + payload.bibleCompletedChapters.size + " chapters read", style = MaterialTheme.typography.bodySmall)
                                Text("• Daily Reading Goal: " + (payload.settings.readingGoalMinimum ?: 2) + " chapters", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        backupState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = { Button(onClick = { workflow.restoreBackup(backupManager) }, enabled = !busy, modifier = Modifier.testTag("backup_confirm_restore_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.surface)) {
                    Text(if (busy) "Restoring Data…" else "Replace All Data & Restore")
                } },
                dismissButton = { TextButton(onClick = workflow::cancelBackupWorkflow, enabled = !busy) { Text("Cancel") } })
        }
        SettingsBackupStage.RESULT -> Unit
        else -> Unit
    }

    if (showOtReadingDialog && bibleBooks.any { it.testament.equals("OT", true) }) ReadingLocationDialog(
        title = if (otReading == null) "Set Old Testament Bookmark" else "Change Old Testament Bookmark",
        books = bibleBooks.filter { it.testament.equals("OT", true) }, current = otReading,
        onSave = { book, chapter -> onSetOtReading(book, chapter); showOtReadingDialog = false },
        onDismiss = { showOtReadingDialog = false },
        onClear = if (otReading == null) null else { { onClearOtReading(); showOtReadingDialog = false } }
    )
    if (showNtReadingDialog && bibleBooks.any { it.testament.equals("NT", true) }) ReadingLocationDialog(
        title = if (ntReading == null) "Set New Testament Bookmark" else "Change New Testament Bookmark",
        books = bibleBooks.filter { it.testament.equals("NT", true) }, current = ntReading,
        onSave = { book, chapter -> onSetNtReading(book, chapter); showNtReadingDialog = false },
        onDismiss = { showNtReadingDialog = false },
        onClear = if (ntReading == null) null else { { onClearNtReading(); showNtReadingDialog = false } }
    )

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Steadfast V${com.steadfast.app.BuildConfig.VERSION_NAME}", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).testTag("settings_about_content")
                ) {
                    Text(
                        text = "A stronger you. A closer walk with God.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Steadfast is an offline, private recovery and Scripture progress companion. It contains no accounts, no cloud sync, no tracking, and no shame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Medical Safety Notice:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Steadfast is a personal tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Dismiss") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun authCapabilityMessage(capability: BiometricLockManager.AuthCapability): String = when (capability) {
    BiometricLockManager.AuthCapability.READY -> "Device authentication is ready."
    BiometricLockManager.AuthCapability.NONE_ENROLLED -> "Set up a screen lock or biometrics in Android Settings to enable app lock."
    BiometricLockManager.AuthCapability.HW_UNAVAILABLE -> "Device authentication is temporarily unavailable. Try again later."
    BiometricLockManager.AuthCapability.NO_HARDWARE -> "This device has no supported authentication hardware."
    BiometricLockManager.AuthCapability.UNSUPPORTED -> "Device authentication is not supported on this device."
}

internal fun backupPreviewFocusLabel(focusId: String?, focuses: List<RecoveryFocus>): String {
    val effectiveId = focusId ?: "pornography"
    val label = focuses.firstOrNull { it.id == effectiveId }?.label ?: "Unknown recovery focus"
    return if (focusId == null) "Not specified; legacy default: $label" else label
}
