package com.steadfast.app.ui.journey

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.MilestoneDefinition
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.ui.theme.SteadfastGold
import com.steadfast.app.ui.theme.SteadfastGoldDark
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun JourneyScreen(
    activeSession: StreakSessionEntity?,
    allSessions: List<StreakSessionEntity>,
    archivedSessions: List<StreakSessionEntity>,
    journalEntries: List<JournalEntryEntity>,
    nextMilestone: MilestoneDefinition?,
    longestStreakDays: Long,
    onStartAgainConfirmed: (newStartEpochMillis: Long, optionalNote: String?) -> Unit,
    onAddJournalEntry: (body: String, context: String?) -> Unit,
    onUpdateJournalEntry: (id: Long, body: String, context: String?) -> Unit,
    onDeleteJournalEntry: (id: Long) -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToMotivation: () -> Unit,
    onSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    initialJournalContext: String? = null,
    initialSelectedTab: Int = 0,
    onSelectedTabChanged: (Int) -> Unit = {},
    initialJournalFilter: String = "ALL",
    onJournalFilterChanged: (String) -> Unit = {},
    onStartAgainValidated: (suspend (Long, String?) -> Result<Unit>)? = null,
    focuses: List<RecoveryFocus> = emptyList(),
    currentStreakDays: Long? = null,
    onSaveJournalEntryValidated: (suspend (Long?, String, String?) -> Result<Unit>)? = null
) {
    var selectedTab by rememberSaveable(initialJournalContext) { mutableIntStateOf(if (initialJournalContext != null) 2 else initialSelectedTab) }
    val draft = androidx.lifecycle.viewmodel.compose.viewModel<JourneyDraftModel>()
    androidx.compose.runtime.LaunchedEffect(initialJournalContext) {
        if (!draft.initialized) {
            draft.initialized = true
            draft.filter = initialJournalFilter
            if (initialJournalContext != null) {
                draft.openNew(initialJournalContext)
                onSelectedTabChanged(2)
            }
        }
    }
    var showStartAgainDialog by draft::showRestart
    var showAddNoteDialog by draft::showNew
    var newEntryContext by draft::context
    var entryToEdit by draft::editing
    var entryToDelete by draft::deleting
    var startAgainStartMillis by draft::restartMillis
    var restartError by draft::restartError
    var restartSubmitting by draft::restarting
    val scope = draft.operationScope
    val displayedStreakDays = currentStreakDays ?: remember(activeSession) {
        activeSession?.let {
            StreakDurationCalculator.calculate(it.startEpochMillis, it.startZoneId).totalDays
        } ?: 0L
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Journey",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("journey_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Custom Segmented Pill Tab Selector (Style 1 structure, Style 3 palette)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Overview", "History", "Journal").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { selectedTab = index; onSelectedTabChanged(index) })
                            .testTag("journey_tab_$title"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> JourneyOverviewTab(
                    currentStreakDays = displayedStreakDays,
                    longestStreakDays = longestStreakDays,
                    nextMilestone = nextMilestone,
                    archivedCount = archivedSessions.size,
                    allSessions = allSessions,
                    onStartAgainClick = { restartError = null; showStartAgainDialog = true },
                    onNavigateToMilestones = onNavigateToMilestones,
                    onNavigateToMotivation = onNavigateToMotivation
                )
                1 -> JourneyHistoryTab(
                    allSessions = allSessions,
                    archivedSessions = archivedSessions,
                    focuses = focuses
                )
                2 -> JourneyJournalTab(
                    entries = journalEntries,
                    searchQuery = draft.query, selectedFilter = draft.filter,
                    onQueryChanged = { draft.query = it }, onFilterChanged = { draft.filter = it; onJournalFilterChanged(it) },
                    onAddClick = { draft.openNew() },
                    onEditClick = { draft.openEdit(it) },
                    onDeleteClick = { entryToDelete = it }
                )
            }
        }

        // Floating Action Button for Journal Tab
        if (selectedTab == 2) {
            FloatingActionButton(
                onClick = { draft.openNew() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("journey_add_journal_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Journal Entry")
            }
        }
    }

    // Start Again Confirmation Dialog
    if (showStartAgainDialog) {
        StartAgainDialog(
            reflectionNote = draft.restartNote, onReflectionNoteChanged = { draft.restartNote = it },
            startEpochMillis = startAgainStartMillis,
            onStartEpochMillisChanged = { startAgainStartMillis = it; restartError = null },
            errorMessage = restartError,
            isSubmitting = restartSubmitting,
            onDismiss = { if (!restartSubmitting) draft.clearRestart() },
            onConfirm = { startMillis, note ->
                if (onStartAgainValidated == null) {
                    draft.clearRestart()
                    onStartAgainConfirmed(startMillis, note)
                } else if (!restartSubmitting) {
                    scope.launch {
                        restartSubmitting = true
                        val result = runCatching { onStartAgainValidated(startMillis, note) }
                            .getOrElse { Result.failure(it) }
                        restartSubmitting = false
                        if (result.isSuccess) {
                            restartError = null
                            draft.clearRestart()
                        } else {
                            restartError = if (result.exceptionOrNull() is IllegalArgumentException) {
                                "Choose a restart time after your current streak and outside archived streaks."
                            } else {
                                "Could not start again. Please try once more."
                            }
                        }
                    }
                }
            }
        )
    }

    // Add Journal Entry Dialog
    if (showAddNoteDialog) {
        JournalEntryEditDialog(
            body = draft.body, selectedContext = draft.context,
            onBodyChanged = { draft.body = it }, onContextChanged = { draft.context = it },
            title = "New Journal Entry",
            isSaving = draft.saving, errorMessage = draft.saveError,
            onDismiss = { if (!draft.saving) draft.clearEditor() },
            onSave = { body, ctx ->
                val save = onSaveJournalEntryValidated
                if (save == null) {
                    draft.clearEditor()
                    onAddJournalEntry(body, ctx)
                } else draft.saveJournalEntry(null, save)
            }
        )
    }

    // Edit Journal Entry Dialog
    entryToEdit?.let { entry ->
        JournalEntryEditDialog(
            body = draft.body, selectedContext = draft.context,
            onBodyChanged = { draft.body = it }, onContextChanged = { draft.context = it },
            title = "Edit Entry",
            isSaving = draft.saving, errorMessage = draft.saveError,
            onDismiss = { if (!draft.saving) draft.clearEditor() },
            onSave = { body, ctx ->
                val save = onSaveJournalEntryValidated
                if (save == null) {
                    draft.clearEditor()
                    onUpdateJournalEntry(entry.id, body, ctx)
                } else draft.saveJournalEntry(entry.id, save)
            }
        )
    }

    // Delete Confirmation Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Journal Entry?") },
            text = { Text("This entry will be permanently removed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = entry.id
                        entryToDelete = null
                        onDeleteJournalEntry(id)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun JourneyOverviewTab(
    currentStreakDays: Long,
    longestStreakDays: Long,
    nextMilestone: MilestoneDefinition?,
    archivedCount: Int,
    allSessions: List<StreakSessionEntity>,
    onStartAgainClick: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToMotivation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compact Streak Activity Preview Canvas (Screen 13 style)
        if (allSessions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STREAK ACTIVITY PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${allSessions.size} Session${if (allSessions.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        JourneyTimelineChart(
                            sessions = allSessions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )
                    }
                }
            }
        }
        // Headline Stats: Current & Longest Streak
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CURRENT STREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currentStreakDays days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LONGEST STREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$longestStreakDays days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Next Milestone Card
        item {
            Card(
                onClick = onNavigateToMilestones,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT MILESTONE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = nextMilestone?.title ?: "All Milestones Reached!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View Timeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Time & Motivation Action Card
        item {
            Card(
                onClick = onNavigateToMotivation,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time & Motivation Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total hours, minutes, seconds & biblical scale comparisons.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Start Again Flow Trigger
        item {
            OutlinedCard(
                onClick = onStartAgainClick,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_start_again_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Again",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preserve history and begin a new streak in grace ($archivedCount archived).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyHistoryTab(
    allSessions: List<StreakSessionEntity>,
    archivedSessions: List<StreakSessionEntity>,
    focuses: List<RecoveryFocus>
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Visual Chart Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STREAK TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (allSessions.isNotEmpty()) {
                        JourneyTimelineChart(
                            sessions = allSessions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        )
                    } else {
                        Text(
                            text = "No streak history yet recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "ARCHIVED STREAKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (archivedSessions.isEmpty()) {
            item {
                Text(
                    text = "No archived streaks. Keep standing firm!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(archivedSessions) { session ->
                val startInstant = Instant.ofEpochMilli(session.startEpochMillis)
                val endInstant = Instant.ofEpochMilli(session.endEpochMillis ?: session.startEpochMillis)
                val zone = runCatching { ZoneId.of(session.startZoneId) }.getOrElse { ZoneId.systemDefault() }
                val elapsedMillis = streakElapsedMillis(session.startEpochMillis, session.endEpochMillis ?: session.startEpochMillis)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${streakElapsedDurationLabel(elapsedMillis)} Completed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${startInstant.atZone(zone).format(dateFormatter)} — ${endInstant.atZone(zone).format(dateFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = focuses.find { it.id == session.recoveryFocusId }?.label ?: "Unknown recovery focus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

internal fun streakElapsedMillis(startEpochMillis: Long, endEpochMillis: Long): Long =
    (endEpochMillis - startEpochMillis).coerceAtLeast(0L)

internal fun streakElapsedDurationLabel(durationMillis: Long): String {
    val millis = durationMillis.coerceAtLeast(0L)
    val minutes = millis / 60_000
    val hours = millis / 3_600_000
    val days = millis / 86_400_000
    return when {
        millis < 60_000 -> "${millis / 1_000} seconds"
        millis < 3_600_000 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"}"
        millis < 86_400_000 -> "$hours ${if (hours == 1L) "hour" else "hours"}"
        else -> "$days ${if (days == 1L) "day" else "days"}"
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun JourneyJournalTab(
    entries: List<JournalEntryEntity>,
    searchQuery: String, selectedFilter: String,
    onQueryChanged: (String) -> Unit, onFilterChanged: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (JournalEntryEntity) -> Unit,
    onDeleteClick: (JournalEntryEntity) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a") }



    val filteredEntries = remember(entries, searchQuery, selectedFilter) {
        entries.filter { entry ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                else -> entry.context.equals(selectedFilter, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.body.contains(searchQuery, ignoreCase = true) ||
                        (entry.context?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Context Filter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search your entries...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Context filter chips
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "GENERAL", "URGE", "RESTART").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) SteadfastGold.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .heightIn(min = 48.dp)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onFilterChanged(filter) })
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("journal_filter_$filter")
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your Private Sanctuary",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Capture prayers, victories, and moments of urge. Journal entries are stored locally on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Entries Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No journal entries match your search criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    val formattedDate = remember(entry.createdAtEpochMillis) {
                        Instant.ofEpochMilli(entry.createdAtEpochMillis)
                            .atZone(ZoneId.systemDefault())
                            .format(dateFormatter)
                    }

                    // Journal Card following Style 2 density
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_entry_${entry.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    entry.context?.let { ctx ->
                                        val (badgeColor, badgeContainerColor) = when (ctx) {
                                            "RESTART" -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primaryContainer
                                            "URGE" -> MaterialTheme.colorScheme.onErrorContainer to MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        Text(
                                            text = ctx ?: "NONE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeContainerColor)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = { onEditClick(entry) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteClick(entry) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = entry.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartAgainDialog(
    reflectionNote: String, onReflectionNoteChanged: (String) -> Unit,
    startEpochMillis: Long,
    onStartEpochMillisChanged: (Long) -> Unit,
    errorMessage: String?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newStartEpochMillis: Long, optionalNote: String?) -> Unit
) {
    val context = LocalContext.current

    val creationZone = remember { ZoneId.systemDefault() }

    val currentZdt = remember(startEpochMillis, creationZone) {
        Instant.ofEpochMilli(startEpochMillis).atZone(creationZone)
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Begin Anew in Grace",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your completed days remain safely archived in your history. Freedom is built step by step; stand back up with your head held high.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        onStartEpochMillisChanged(local.atZone(creationZone).toInstant().toEpochMilli())
                                    },
                                    currentZdt.hour,
                                    currentZdt.minute,
                                    false
                                ).show()
                            },
                            currentZdt.year,
                            currentZdt.monthValue - 1,
                            currentZdt.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("journey_restart_time_picker")
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart time: ${currentZdt.format(dateFormatter)}")
                }

                OutlinedTextField(
                    value = reflectionNote,
                    onValueChange = onReflectionNoteChanged,
                    label = { Text("Reflection Note (Optional)") },
                    placeholder = { Text("What did you learn? Where will you lean on God today?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
                errorMessage?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("journey_restart_error"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startEpochMillis, reflectionNote.ifBlank { null }) },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("journey_restart_confirm"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (isSubmitting) "Starting…" else "Confirm & Start Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun JournalEntryEditDialog(
    body: String,
    selectedContext: String?,
    onBodyChanged: (String) -> Unit,
    onContextChanged: (String?) -> Unit,
    title: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (body: String, context: String?) -> Unit
) {



    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Context chips
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, "GENERAL", "URGE", "RESTART").forEach { ctx ->
                        val isSelected = selectedContext == ctx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SteadfastGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .heightIn(min = 48.dp)
                                .selectable(selected = isSelected, enabled = !isSaving, role = Role.RadioButton, onClick = { onContextChanged(ctx) })
                                .testTag("journal_context_$ctx")
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ctx ?: "NONE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChanged,
                    enabled = !isSaving,
                    placeholder = { Text("Write your honest thoughts, prayers, or commitments...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("journal_entry_body")
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("journal_entry_save_error")) }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (body.isNotBlank()) onSave(body, selectedContext) },
                enabled = body.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}
