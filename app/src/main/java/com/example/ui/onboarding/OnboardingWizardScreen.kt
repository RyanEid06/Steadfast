package com.example.ui.onboarding

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.design.rememberReducedMotion
import com.example.core.security.BiometricLockManager
import com.example.data.model.BibleBook
import com.example.data.model.RecoveryFocus
import com.example.ui.theme.SteadfastGold
import com.example.ui.theme.ThemeSetting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun OnboardingWizardScreen(
    focuses: List<RecoveryFocus>,
    bibleBooks: List<BibleBook>,
    onComplete: (
        focusId: String,
        startEpochMillis: Long,
        startZoneId: String,
        otBookId: Int?,
        otChapter: Int?,
        ntBookId: Int?,
        ntChapter: Int?,
        readingGoal: Int,
        themeSetting: ThemeSetting,
        appLockEnabled: Boolean
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6
    val reducedMotion = rememberReducedMotion()

    var selectedFocusId by remember { mutableStateOf(focuses.firstOrNull()?.id ?: "pornography") }
    var startEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val zoneId = remember { ZoneId.systemDefault().id }

    // OT and NT current reading options (optional selection!)
    var isOtReadingActive by remember { mutableStateOf(false) }
    var selectedOtBookId by remember { mutableStateOf<Int?>(1) } // Genesis
    var selectedOtChapter by remember { mutableIntStateOf(1) }

    var isNtReadingActive by remember { mutableStateOf(false) }
    var selectedNtBookId by remember { mutableStateOf<Int?>(40) } // Matthew
    var selectedNtChapter by remember { mutableIntStateOf(1) }

    var readingGoal by remember { mutableIntStateOf(2) }
    var selectedTheme by remember { mutableStateOf(ThemeSetting.SYSTEM) }
    var appLockEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Top Navigation & Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    IconButton(
                        onClick = { step-- },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Page Position Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = if (idx == step) 20.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= step) SteadfastGold
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Animated step transition (instant fade when reduced motion enabled)
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_step"
            ) { currentStep ->
                when (currentStep) {
                    0 -> StepWelcome(onNext = { step = 1 })
                    1 -> StepFocus(
                        focuses = focuses,
                        selectedFocusId = selectedFocusId,
                        onSelectFocus = { selectedFocusId = it },
                        onNext = { step = 2 }
                    )
                    2 -> StepStartDateTime(
                        startEpochMillis = startEpochMillis,
                        onDateTimeChanged = { startEpochMillis = it },
                        onNext = { step = 3 }
                    )
                    3 -> StepBibleSetup(
                        bibleBooks = bibleBooks,
                        isOtActive = isOtReadingActive,
                        otBookId = selectedOtBookId,
                        otChapter = selectedOtChapter,
                        isNtActive = isNtReadingActive,
                        ntBookId = selectedNtBookId,
                        ntChapter = selectedNtChapter,
                        readingGoal = readingGoal,
                        onOtActiveChanged = { isOtReadingActive = it },
                        onOtBookChanged = { selectedOtBookId = it },
                        onOtChapterChanged = { selectedOtChapter = it },
                        onNtActiveChanged = { isNtReadingActive = it },
                        onNtBookChanged = { selectedNtBookId = it },
                        onNtChapterChanged = { selectedNtChapter = it },
                        onGoalChanged = { readingGoal = it },
                        onNext = { step = 4 }
                    )
                    4 -> StepThemeAndLock(
                        selectedTheme = selectedTheme,
                        appLockEnabled = appLockEnabled,
                        onThemeChanged = { selectedTheme = it },
                        onAppLockChanged = { appLockEnabled = it },
                        onNext = { step = 5 }
                    )
                    5 -> StepTourAndFinish(
                        selectedFocusName = focuses.find { it.id == selectedFocusId }?.label ?: selectedFocusId,
                        readingGoal = readingGoal,
                        hasOtReading = isOtReadingActive && selectedOtBookId != null,
                        hasNtReading = isNtReadingActive && selectedNtBookId != null,
                        onFinish = {
                            onComplete(
                                selectedFocusId,
                                startEpochMillis,
                                zoneId,
                                if (isOtReadingActive) selectedOtBookId else null,
                                if (isOtReadingActive) selectedOtChapter else null,
                                if (isNtReadingActive) selectedNtBookId else null,
                                if (isNtReadingActive) selectedNtChapter else null,
                                readingGoal,
                                selectedTheme,
                                appLockEnabled
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Clean hero image without phone bezels or status bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SteadfastGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_splash),
                    contentDescription = "Cross upon mountain at sunset",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC0A0E17))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "STEADFAST",
                style = MaterialTheme.typography.displayMedium,
                color = SteadfastGold,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A stronger you. A closer walk with God.",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Steadfast unites recovery discipline and complete Scripture tracking in a completely private, offline sanctuary on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_welcome_next"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Begin Your Walk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepFocus(
    focuses: List<RecoveryFocus>,
    selectedFocusId: String,
    onSelectFocus: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Select Recovery Focus",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Choose the habit or compulsive pattern you are committing to overcome. You can adjust this later without losing historical records.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        val isSubstance = remember(selectedFocusId) {
            selectedFocusId in listOf("alcohol", "smoking_nicotine", "cannabis", "recreational_drugs")
        }
        if (isSubstance) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Medical Safety Notice: Steadfast is a personal tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(focuses) { focus ->
                val isSelected = focus.id == selectedFocusId
                OutlinedCard(
                    onClick = { onSelectFocus(focus.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_item_${focus.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = focus.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = SteadfastGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_focus_next"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Confirm Focus & Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepStartDateTime(
    startEpochMillis: Long,
    onDateTimeChanged: (Long) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val currentZdt = remember(startEpochMillis) {
        Instant.ofEpochMilli(startEpochMillis).atZone(ZoneId.systemDefault())
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun showDatePicker() {
        val c = Calendar.getInstance()
        c.timeInMillis = startEpochMillis
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newZdt = currentZdt.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth)
                val newMillis = newZdt.toInstant().toEpochMilli()
                if (newMillis > System.currentTimeMillis()) {
                    errorMessage = "Start time cannot be in the future."
                } else {
                    errorMessage = null
                    onDateTimeChanged(newMillis)
                }
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    fun showTimePicker() {
        val c = Calendar.getInstance()
        c.timeInMillis = startEpochMillis
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newZdt = currentZdt.withHour(hourOfDay).withMinute(minute).withSecond(0)
                val newMillis = newZdt.toInstant().toEpochMilli()
                if (newMillis > System.currentTimeMillis()) {
                    errorMessage = "Start time cannot be in the future."
                } else {
                    errorMessage = null
                    onDateTimeChanged(newMillis)
                }
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            false
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "When Did Your Streak Begin?",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Set the exact date and time you started walking in freedom. The timer counts forward by the second from this instant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Date Selection
            OutlinedCard(
                onClick = { showDatePicker() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("streak_date_picker_trigger"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date", tint = SteadfastGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "START DATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = currentZdt.format(dateFormatter), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Selection
            OutlinedCard(
                onClick = { showTimePicker() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("streak_time_picker_trigger"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SteadfastGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Time", tint = SteadfastGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "EXACT TIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = currentZdt.format(timeFormatter), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    errorMessage = null
                    onDateTimeChanged(System.currentTimeMillis())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set to Right Now")
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Button(
            onClick = {
                if (startEpochMillis > System.currentTimeMillis()) {
                    errorMessage = "Start time cannot be in the future."
                } else {
                    onNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_datetime_next"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Lock In Start Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepBibleSetup(
    bibleBooks: List<BibleBook>,
    isOtActive: Boolean,
    otBookId: Int?,
    otChapter: Int,
    isNtActive: Boolean,
    ntBookId: Int?,
    ntChapter: Int,
    readingGoal: Int,
    onOtActiveChanged: (Boolean) -> Unit,
    onOtBookChanged: (Int?) -> Unit,
    onOtChapterChanged: (Int) -> Unit,
    onNtActiveChanged: (Boolean) -> Unit,
    onNtBookChanged: (Int?) -> Unit,
    onNtChapterChanged: (Int) -> Unit,
    onGoalChanged: (Int) -> Unit,
    onNext: () -> Unit
) {
    val otBooks = remember(bibleBooks) { bibleBooks.filter { it.testament == "OT" } }
    val ntBooks = remember(bibleBooks) { bibleBooks.filter { it.testament == "NT" } }

    val selectedOtBook = otBooks.find { it.id == otBookId } ?: otBooks.firstOrNull()
    val selectedNtBook = ntBooks.find { it.id == ntBookId } ?: ntBooks.firstOrNull()

    var otMenuExpanded by remember { mutableStateOf(false) }
    var ntMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Scripture Progress Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Optionally set where you are currently reading in the Old and New Testaments, and select your daily chapter target.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Reading Goal Target Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Target Goal",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$readingGoal+ chapters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SteadfastGold
                            )
                        }
                        Text(
                            text = "A motivational intention across your active walk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Slider(
                            value = readingGoal.toFloat(),
                            onValueChange = { onGoalChanged(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = SteadfastGold, activeTrackColor = SteadfastGold),
                            modifier = Modifier.testTag("onboarding_goal_slider")
                        )
                    }
                }
            }

            // Old Testament Bookmark Section (Fully Interactive & Optional)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Old Testament Reading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isOtActive,
                                onCheckedChange = { active ->
                                    onOtActiveChanged(active)
                                    if (active && otBookId == null) {
                                        onOtBookChanged(1) // Genesis
                                        onOtChapterChanged(1)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = SteadfastGold, checkedTrackColor = SteadfastGold.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("onboarding_ot_switch")
                            )
                        }

                        if (isOtActive && selectedOtBook != null) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Book selector dropdown
                            ExposedDropdownMenuBox(
                                expanded = otMenuExpanded,
                                onExpandedChange = { otMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedOtBook.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Book") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = otMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("onboarding_ot_book_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = otMenuExpanded,
                                    onDismissRequest = { otMenuExpanded = false }
                                ) {
                                    otBooks.forEach { book ->
                                        DropdownMenuItem(
                                            text = { Text(book.name) },
                                            onClick = {
                                                onOtBookChanged(book.id)
                                                // Validate chapter range
                                                if (otChapter > book.chapters) {
                                                    onOtChapterChanged(1)
                                                }
                                                otMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Chapter slider / stepper validated against book.chapters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Chapter $otChapter of ${selectedOtBook.chapters}", style = MaterialTheme.typography.bodyMedium, color = SteadfastGold)
                                Row {
                                    OutlinedButton(
                                        onClick = { if (otChapter > 1) onOtChapterChanged(otChapter - 1) },
                                        enabled = otChapter > 1,
                                        contentPadding = PaddingValues(4.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("-") }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { if (otChapter < selectedOtBook.chapters) onOtChapterChanged(otChapter + 1) },
                                        enabled = otChapter < selectedOtBook.chapters,
                                        contentPadding = PaddingValues(4.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("+") }
                                }
                            }
                        } else {
                            Text(
                                text = "Not currently reading the Old Testament (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // New Testament Bookmark Section (Fully Interactive & Optional)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New Testament Reading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isNtActive,
                                onCheckedChange = { active ->
                                    onNtActiveChanged(active)
                                    if (active && ntBookId == null) {
                                        onNtBookChanged(40) // Matthew
                                        onNtChapterChanged(1)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = SteadfastGold, checkedTrackColor = SteadfastGold.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("onboarding_nt_switch")
                            )
                        }

                        if (isNtActive && selectedNtBook != null) {
                            Spacer(modifier = Modifier.height(10.dp))

                            ExposedDropdownMenuBox(
                                expanded = ntMenuExpanded,
                                onExpandedChange = { ntMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedNtBook.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Book") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ntMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("onboarding_nt_book_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = ntMenuExpanded,
                                    onDismissRequest = { ntMenuExpanded = false }
                                ) {
                                    ntBooks.forEach { book ->
                                        DropdownMenuItem(
                                            text = { Text(book.name) },
                                            onClick = {
                                                onNtBookChanged(book.id)
                                                if (ntChapter > book.chapters) {
                                                    onNtChapterChanged(1)
                                                }
                                                ntMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Chapter $ntChapter of ${selectedNtBook.chapters}", style = MaterialTheme.typography.bodyMedium, color = SteadfastGold)
                                Row {
                                    OutlinedButton(
                                        onClick = { if (ntChapter > 1) onNtChapterChanged(ntChapter - 1) },
                                        enabled = ntChapter > 1,
                                        contentPadding = PaddingValues(4.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("-") }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { if (ntChapter < selectedNtBook.chapters) onNtChapterChanged(ntChapter + 1) },
                                        enabled = ntChapter < selectedNtBook.chapters,
                                        contentPadding = PaddingValues(4.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("+") }
                                }
                            }
                        } else {
                            Text(
                                text = "Not currently reading the New Testament (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_bible_next"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Scripture Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepThemeAndLock(
    selectedTheme: ThemeSetting,
    appLockEnabled: Boolean,
    onThemeChanged: (ThemeSetting) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Personalize & Protect",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Choose your theme and optionally enable Biometric or Device Lock to secure your private notes and recovery walk.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ThemeSetting.SYSTEM to "System",
                    ThemeSetting.DARK to "Dark",
                    ThemeSetting.LIGHT to "Light"
                ).forEach { (setting, label) ->
                    val isSelected = selectedTheme == setting
                    OutlinedCard(
                        onClick = { onThemeChanged(setting) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_${setting.name}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.outline
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) SteadfastGold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Lock Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = SteadfastGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Biometric / App Lock",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Requires fingerprint or device PIN when opening app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { wanted ->
                            if (wanted) {
                                val capability = BiometricLockManager.checkAuthCapability(context)
                                if (capability == BiometricLockManager.AuthCapability.READY) {
                                    onAppLockChanged(true)
                                } else {
                                    // Device lock not available
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please configure a device PIN, pattern or biometric in Android Settings before enabling App Lock.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                onAppLockChanged(false)
                            }
                        },
                        modifier = Modifier.testTag("onboarding_app_lock_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SteadfastGold,
                            checkedTrackColor = SteadfastGold.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_security_next"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Next: Product Tour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepTourAndFinish(
    selectedFocusName: String,
    readingGoal: Int,
    hasOtReading: Boolean,
    hasNtReading: Boolean,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = SteadfastGold,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your Walk Begins",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Focus: $selectedFocusName • $readingGoal+ chapters target",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TourItem(
                        title = "Circular Streak Timer",
                        desc = "Your dashboard timer updates live by the second, advancing gracefully across days, months, and years."
                    )
                    TourItem(
                        title = "One-Tap 'I'm Struggling'",
                        desc = "When temptation strikes, open the intervention sanctuary for Scripture, calming breathwork, and urgent journaling."
                    )
                    TourItem(
                        title = "Bible Journey & Chapters",
                        desc = "Track through all 66 books and 1,189 chapters without artificial pressure."
                    )
                }
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_finish_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SteadfastGold,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Enter Steadfast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TourItem(title: String, desc: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SteadfastGold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
