package com.example.ui.journey

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.time.StreakDurationCalculator
import com.example.data.model.JournalEntryEntity
import com.example.data.model.MilestoneDefinition
import com.example.data.model.StreakSessionEntity
import com.example.ui.theme.SteadfastGold
import com.example.ui.theme.SteadfastGoldDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showStartAgainDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntryEntity?>(null) }

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isScreenVisible by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isScreenVisible = true
        currentTimeMillis = System.currentTimeMillis()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isScreenVisible = false
    }

    androidx.compose.runtime.LaunchedEffect(isScreenVisible, activeSession) {
        while (isActive && isScreenVisible && activeSession != null) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val currentDuration = remember(activeSession, currentTimeMillis) {
        if (activeSession != null) {
            StreakDurationCalculator.calculate(
                startEpochMillis = activeSession.startEpochMillis,
                startZoneId = activeSession.startZoneId,
                nowEpochMillis = currentTimeMillis
            )
        } else {
            StreakDurationCalculator.calculate(currentTimeMillis, "UTC", currentTimeMillis)
        }
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
                            .height(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) SteadfastGold else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .testTag("journey_tab_$title"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> JourneyOverviewTab(
                    currentStreakDays = currentDuration.totalDays,
                    longestStreakDays = longestStreakDays,
                    nextMilestone = nextMilestone,
                    archivedCount = archivedSessions.size,
                    allSessions = allSessions,
                    onStartAgainClick = { showStartAgainDialog = true },
                    onNavigateToMilestones = onNavigateToMilestones,
                    onNavigateToMotivation = onNavigateToMotivation
                )
                1 -> JourneyHistoryTab(
                    allSessions = allSessions,
                    archivedSessions = archivedSessions
                )
                2 -> JourneyJournalTab(
                    entries = journalEntries,
                    onAddClick = { showAddNoteDialog = true },
                    onEditClick = { entryToEdit = it },
                    onDeleteClick = { entryToDelete = it }
                )
            }
        }

        // Floating Action Button for Journal Tab
        if (selectedTab == 2) {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("journey_add_journal_fab"),
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Journal Entry")
            }
        }
    }

    // Start Again Confirmation Dialog
    if (showStartAgainDialog) {
        StartAgainDialog(
            onDismiss = { showStartAgainDialog = false },
            onConfirm = { startMillis, note ->
                showStartAgainDialog = false
                onStartAgainConfirmed(startMillis, note)
            }
        )
    }

    // Add Journal Entry Dialog
    if (showAddNoteDialog) {
        JournalEntryEditDialog(
            initialBody = "",
            initialContext = "GENERAL",
            title = "New Journal Entry",
            onDismiss = { showAddNoteDialog = false },
            onSave = { body, ctx ->
                showAddNoteDialog = false
                onAddJournalEntry(body, ctx)
            }
        )
    }

    // Edit Journal Entry Dialog
    entryToEdit?.let { entry ->
        JournalEntryEditDialog(
            initialBody = entry.body,
            initialContext = entry.context ?: "GENERAL",
            title = "Edit Entry",
            onDismiss = { entryToEdit = null },
            onSave = { body, ctx ->
                entryToEdit = null
                onUpdateJournalEntry(entry.id, body, ctx)
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
                                color = SteadfastGold,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${allSessions.size} Session${if (allSessions.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        StreakHistoryBarChart(
                            sessions = allSessions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
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
                            color = SteadfastGold,
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
                        tint = SteadfastGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT MILESTONE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SteadfastGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = nextMilestone?.title ?: "All Milestones Reached!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "View Timeline >",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        tint = SteadfastGold,
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
    archivedSessions: List<StreakSessionEntity>
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
                        color = SteadfastGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (allSessions.isNotEmpty()) {
                        StreakHistoryBarChart(
                            sessions = allSessions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
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
                val duration = StreakDurationCalculator.calculate(
                    session.startEpochMillis,
                    session.startZoneId,
                    session.endEpochMillis ?: session.startEpochMillis
                )

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
                                text = "${duration.totalDays} Days Completed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${startInstant.atZone(ZoneId.systemDefault()).format(dateFormatter)} — ${endInstant.atZone(ZoneId.systemDefault()).format(dateFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = session.recoveryFocusId.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = SteadfastGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakHistoryBarChart(
    sessions: List<StreakSessionEntity>,
    modifier: Modifier = Modifier
) {
    val barColor = SteadfastGold
    val activeColor = SteadfastGoldDark

    val now = System.currentTimeMillis()
    val sessionDays = remember(sessions) {
        sessions.take(10).reversed().map { session ->
            val end = session.endEpochMillis ?: now
            val d = StreakDurationCalculator.calculate(session.startEpochMillis, session.startZoneId, end)
            d.totalDays.coerceAtLeast(1) to (session.endEpochMillis == null)
        }
    }

    val maxDays = remember(sessionDays) {
        sessionDays.maxOfOrNull { it.first } ?: 1L
    }

    Canvas(modifier = modifier) {
        val count = sessionDays.size
        if (count == 0) return@Canvas
        val totalWidth = size.width
        val barWidth = (totalWidth / (count * 1.6f)).coerceIn(12f, 36f)
        val space = (totalWidth - (barWidth * count)) / (count + 1)
        val maxHeight = size.height - 24f

        sessionDays.forEachIndexed { idx, (days, isActive) ->
            val heightRatio = (days.toFloat() / maxDays.toFloat()).coerceIn(0.1f, 1f)
            val barHeight = maxHeight * heightRatio
            val left = space + idx * (barWidth + space)
            val top = size.height - barHeight

            drawRoundRect(
                color = if (isActive) barColor else activeColor.copy(alpha = 0.7f),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun JourneyJournalTab(
    entries: List<JournalEntryEntity>,
    onAddClick: () -> Unit,
    onEditClick: (JournalEntryEntity) -> Unit,
    onDeleteClick: (JournalEntryEntity) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a") }

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
                    text = "Capture prayers, victories, and moments of urge. All journal entries remain encrypted on your device.",
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                entry.context?.let { ctx ->
                                    val badgeColor = when (ctx) {
                                        "RESTART" -> SteadfastGold
                                        "URGE" -> Color(0xFFC75D4D)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(
                                        text = ctx,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = { onEditClick(entry) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("journal_edit_button_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Note",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteClick(entry) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("journal_delete_button_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Note",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
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

@Composable
private fun StartAgainDialog(
    onDismiss: () -> Unit,
    onConfirm: (newStartEpochMillis: Long, optionalNote: String?) -> Unit
) {
    val context = LocalContext.current
    var startEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reflectionNote by remember { mutableStateOf("") }

    val currentZdt = remember(startEpochMillis) {
        Instant.ofEpochMilli(startEpochMillis).atZone(ZoneId.systemDefault())
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        val c = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, min ->
                                        val zdt = currentZdt.withYear(y).withMonth(m + 1).withDayOfMonth(d).withHour(hour).withMinute(min)
                                        startEpochMillis = zdt.toInstant().toEpochMilli().coerceAtMost(System.currentTimeMillis())
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
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart time: ${currentZdt.format(dateFormatter)}")
                }

                OutlinedTextField(
                    value = reflectionNote,
                    onValueChange = { reflectionNote = it },
                    label = { Text("Reflection Note (Optional)") },
                    placeholder = { Text("What did you learn? Where will you lean on God today?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startEpochMillis, reflectionNote.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Confirm & Start Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun JournalEntryEditDialog(
    initialBody: String,
    initialContext: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (body: String, context: String) -> Unit
) {
    var body by remember { mutableStateOf(initialBody) }
    var selectedContext by remember { mutableStateOf(initialContext) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Context chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GENERAL", "URGE", "RESTART").forEach { ctx ->
                        val isSelected = selectedContext == ctx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SteadfastGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) SteadfastGold else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ctx,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Write your honest thoughts, prayers, or commitments...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (body.isNotBlank()) onSave(body, selectedContext) },
                enabled = body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SteadfastGold, contentColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
