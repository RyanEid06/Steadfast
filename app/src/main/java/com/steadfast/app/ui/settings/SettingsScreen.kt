package com.steadfast.app.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.ThemeSetting
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFocusDialog by remember { mutableStateOf(false) }
    var showStreakEditDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportPassphraseDialog by remember { mutableStateOf(false) }
    var importUriHolder by remember { mutableStateOf<android.net.Uri?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var exportPassphrase by remember { mutableStateOf("") }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // SAF Launchers
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            showExportDialog = true
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importUriHolder = uri
            showImportPassphraseDialog = true
        }
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
                text = "Settings",
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
            // Theme section
            item {
                SectionHeader("APPEARANCE")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ThemeSetting.SYSTEM to "System",
                                ThemeSetting.DARK to "Dark",
                                ThemeSetting.LIGHT to "Light"
                            ).forEach { (setting, label) ->
                                val isSelected = currentTheme == setting
                                OutlinedCard(
                                    onClick = { onThemeChanged(setting) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_theme_${setting.name}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.outline
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
                                            color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.onSurface
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
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SteadfastGold)
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
                            }
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = onAppLockChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SteadfastGold,
                                checkedTrackColor = SteadfastGold.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("settings_app_lock_switch")
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
                        val focusName = focuses.find { it.id == currentFocusId }?.label ?: currentFocusId
                        SettingsClickableRow(
                            icon = Icons.Default.SelfImprovement,
                            title = "Recovery Focus",
                            subtitle = focusName,
                            onClick = { showFocusDialog = true }
                        )

                        activeSession?.let { session ->
                            val zdt = Instant.ofEpochMilli(session.startEpochMillis).atZone(ZoneId.systemDefault())
                            val dFmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                            SettingsClickableRow(
                                icon = Icons.Default.DateRange,
                                title = "Edit Streak Start Date & Time",
                                subtitle = zdt.format(dFmt),
                                onClick = { showStreakEditDialog = true }
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

                        if (otReading != null) {
                            SettingsClickableRow(
                                icon = Icons.Default.Check,
                                title = "Clear Old Testament Bookmark",
                                subtitle = "Book ID: ${otReading.bookId}, Ch. ${otReading.chapter}",
                                onClick = onClearOtReading
                            )
                        }

                        if (ntReading != null) {
                            SettingsClickableRow(
                                icon = Icons.Default.Check,
                                title = "Clear New Testament Bookmark",
                                subtitle = "Book ID: ${ntReading.bookId}, Ch. ${ntReading.chapter}",
                                onClick = onClearNtReading
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
                        SettingsClickableRow(
                            icon = Icons.Default.Upload,
                            title = "Export Encrypted Backup",
                            subtitle = "Save portable .steadfast file with personal passphrase",
                            onClick = {
                                exportFileLauncher.launch("steadfast_backup_${System.currentTimeMillis()}.steadfast")
                            }
                        )
                        SettingsClickableRow(
                            icon = Icons.Default.Download,
                            title = "Import & Restore Backup",
                            subtitle = "Replace data from encrypted .steadfast file",
                            onClick = {
                                importFileLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }
            }

            // General & About
            item {
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
                            subtitle = "Version 1.0.0 • Offline Privacy Policy",
                            onClick = { showAboutDialog = true }
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
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(focuses) { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFocusChanged(f.id)
                                    showFocusDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = f.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (f.id == currentFocusId) SteadfastGold else MaterialTheme.colorScheme.onSurface
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
        var newMillis by remember { mutableLongStateOf(activeSession.startEpochMillis) }
        val zdt = Instant.ofEpochMilli(newMillis).atZone(ZoneId.systemDefault())
        val dFmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

        AlertDialog(
            onDismissRequest = { showStreakEditDialog = false },
            title = { Text("Edit Streak Start Time", fontFamily = FontFamily.Serif, color = SteadfastGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "WARNING: Modifying your streak start date alters your active streak counter and milestone progress. Only use this if you originally entered the wrong time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            val updatedZdt = zdt.withYear(y).withMonth(m + 1).withDayOfMonth(d).withHour(hour).withMinute(min)
                                            newMillis = updatedZdt.toInstant().toEpochMilli().coerceAtMost(System.currentTimeMillis())
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick: ${zdt.format(dFmt)}")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStreakEditDialog = false
                        onStreakStartChanged(newMillis)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Save Start Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStreakEditDialog = false }) { Text("Cancel") }
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
                        color = SteadfastGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempGoal.toFloat(),
                        onValueChange = { tempGoal = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = SteadfastGold, activeTrackColor = SteadfastGold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReadingGoalChanged(tempGoal)
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Export Passphrase Dialog
    if (showExportDialog && pendingExportUri != null) {
        var pass by remember { mutableStateOf("") }
        var isExporting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Encrypt Export File", fontFamily = FontFamily.Serif) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a strong passphrase to encrypt your backup. You will need this exact passphrase to restore your data on another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Export Passphrase") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingExportUri ?: return@Button
                        isExporting = true
                        scope.launch {
                            try {
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    backupManager.exportBackup(out, pass)
                                }
                                Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExporting = false
                                showExportDialog = false
                                pendingExportUri = null
                            }
                        }
                    },
                    enabled = pass.length >= 4 && !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false; pendingExportUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Passphrase Dialog
    if (showImportPassphraseDialog && importUriHolder != null) {
        var pass by remember { mutableStateOf("") }
        var isImporting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportPassphraseDialog = false },
            title = { Text("Restore From Backup", fontFamily = FontFamily.Serif) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "WARNING: Importing will replace all live user data (streaks, journal entries, Bible progress) with the data from this backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Backup Passphrase") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = importUriHolder ?: return@Button
                        isImporting = true
                        scope.launch {
                            try {
                                val result = context.contentResolver.openInputStream(uri)?.use { ins ->
                                    backupManager.importBackup(ins, pass)
                                }
                                if (result != null && result.isSuccess) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Import failed: ${result?.exceptionOrNull()?.message ?: "Invalid passphrase or file"}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isImporting = false
                                showImportPassphraseDialog = false
                                importUriHolder = null
                            }
                        }
                    },
                    enabled = pass.isNotBlank() && !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Restore Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPassphraseDialog = false; importUriHolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Steadfast V1.0.0", fontFamily = FontFamily.Serif, color = SteadfastGold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
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
        color = SteadfastGold,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SteadfastGold,
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
