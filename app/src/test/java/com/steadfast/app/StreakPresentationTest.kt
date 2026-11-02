package com.steadfast.app

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.ui.journey.streakBarHeightRatio
import com.steadfast.app.ui.journey.streakElapsedDurationLabel
import com.steadfast.app.ui.journey.JourneyScreen
import com.steadfast.app.ui.settings.SettingsScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StreakPresentationTest {
    @get:Rule val compose = createComposeRule()
    private var defaultTimeZone: TimeZone? = null

    @After fun restoreTimeZone() {
        defaultTimeZone?.let(TimeZone::setDefault)
    }

    @Test fun settingsUsesSessionStoredZoneWhenShowingStartDate() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val session = StreakSessionEntity(
            id = 10, recoveryFocusId = "gaming", startEpochMillis = Instant.parse("2024-01-01T00:30:00Z").toEpochMilli(),
            startZoneId = "UTC", endEpochMillis = Instant.parse("2024-01-01T01:30:00Z").toEpochMilli()
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val preferences = SteadfastPreferences(context)
        val manager = BackupManager(context, database, preferences) {
            javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 }, "AES")
        }

        try {
            compose.setContent {
                SteadfastTheme {
                    SettingsScreen(
                        ThemeSetting.SYSTEM, false, "gaming", listOf(RecoveryFocus("gaming", "Gaming")), session,
                        2, null, null, manager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                    )
                }
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Edit Streak Start Date & Time"))
            val rendered = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().allText()
            assertTrue(rendered.joinToString(" | "), rendered.any { it.contains("Edit Streak Start Date") })
            assertTrue(rendered.joinToString(" | "), rendered.any { it.contains("Jan 1, 2024 12:30 AM") })

        } finally {
            database.close()
        }
    }

    @Test fun historyUsesSessionStoredZoneAndNeutralUnknownFocusLabel() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val session = StreakSessionEntity(
            id = 10, recoveryFocusId = "sensitive-unknown-id", startEpochMillis = Instant.parse("2024-01-01T00:30:00Z").toEpochMilli(),
            startZoneId = "UTC", endEpochMillis = Instant.parse("2024-01-01T01:30:00Z").toEpochMilli()
        )
        compose.setContent {
            SteadfastTheme {
                JourneyScreen(
                    null, listOf(session), listOf(session), emptyList(), null, 0,
                    { _, _ -> }, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithTag("journey_tab_History").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("ARCHIVED STREAKS"))
        val rendered = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().allText()
        assertTrue(rendered.joinToString(" | "), rendered.any { it.contains("Jan 1, 2024 — Jan 1, 2024") })
        compose.onNodeWithText("Unknown recovery focus").assertExists()
        compose.onNodeWithText("sensitive-unknown-id").assertDoesNotExist()
    }

    @Test fun historyChartReportsContinuousShortDurationsAndNeutralUnknownFocus() {
        val now = Instant.parse("2024-05-01T12:00:00Z").toEpochMilli()
        val fifteenMinutes = StreakSessionEntity(
            id = 2, recoveryFocusId = "unknown-sensitive-id", startEpochMillis = now - 15 * 60_000,
            startZoneId = "UTC", endEpochMillis = now
        )
        val sixHours = StreakSessionEntity(
            id = 1, recoveryFocusId = "gaming", startEpochMillis = now - 6 * 60 * 60_000,
            startZoneId = "UTC", endEpochMillis = now
        )
        assertEquals(1f / 24f, streakBarHeightRatio(15 * 60_000L, 6 * 60 * 60_000L), 0.0001f)
        assertEquals("15 minutes", streakElapsedDurationLabel(15 * 60_000L))
        assertEquals("6 hours", streakElapsedDurationLabel(6 * 60 * 60_000L))
        compose.setContent {
            SteadfastTheme {
                JourneyScreen(
                    null, listOf(sixHours, fifteenMinutes), listOf(sixHours, fifteenMinutes), emptyList(), null, 0,
                    { _, _ -> }, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {},
                    focuses = listOf(RecoveryFocus("gaming", "Gaming"))
                )
            }
        }

        compose.onNodeWithTag("journey_tab_History").performClick()
        val chartDescription = compose.onNode(hasContentDescription("Recent streaks", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(chartDescription, chartDescription.contains("15 minutes"))
        assertTrue(chartDescription, chartDescription.contains("6 hours"))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("ARCHIVED STREAKS"))
        compose.onAllNodesWithText("Unknown recovery focus").assertCountEquals(1)
        compose.onNodeWithText("Gaming").assertExists()
        compose.onNodeWithText("unknown-sensitive-id").assertDoesNotExist()
    }

    @Test fun failedSettingsDateSaveKeepsTheDialogAndDraftForRetry() {
        val session = StreakSessionEntity(
            id = 10, recoveryFocusId = "gaming", startEpochMillis = Instant.parse("2024-01-01T00:30:00Z").toEpochMilli(),
            startZoneId = "UTC"
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, database, SteadfastPreferences(context)) {
            javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 }, "AES")
        }
        var attempts = 0
        val savedMillis = mutableListOf<Long>()
        compose.setContent {
            SteadfastTheme {
                SettingsScreen(
                    ThemeSetting.SYSTEM, false, "gaming", listOf(RecoveryFocus("gaming", "Gaming")), session,
                    2, null, null, manager, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    onSaveStreakStartValidated = { millis ->
                        attempts++
                        savedMillis += millis
                        if (attempts == 1) Result.failure(IllegalArgumentException("internal overlap details"))
                        else Result.success(Unit)
                    }
                )
            }
        }

        try {
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Edit Streak Start Date & Time"))
            compose.onNodeWithTag("settings_streak_start_edit").performClick()
            compose.onNodeWithTag("settings_streak_start_save").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("settings_streak_start_error").assertExists()
            compose.onNodeWithText("Edit Streak Start Time").assertExists()

            compose.onNodeWithTag("settings_streak_start_save").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Edit Streak Start Time").assertDoesNotExist()
            assertEquals(2, attempts)
            assertEquals(listOf(session.startEpochMillis, session.startEpochMillis), savedMillis)
        } finally {
            database.close()
        }
    }

    @Test fun failedRestartKeepsReflectionNoteAndDialogOpenForRetry() {
        val entryAttempts = mutableListOf<String?>()
        var attempts = 0
        compose.setContent {
            SteadfastTheme {
                JourneyScreen(
                    null, emptyList(), emptyList(), emptyList(), null, 0,
                    { _, _ -> }, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {},
                    onStartAgainValidated = { _, note ->
                        attempts++
                        entryAttempts += note
                        if (attempts == 1) Result.failure(IllegalArgumentException("internal chronology details"))
                        else Result.success(Unit)
                    }
                )
            }
        }

        compose.onNodeWithTag("journey_start_again_button").performClick()
        compose.onNodeWithText("Reflection Note (Optional)").performTextInput("keep this note")
        compose.onNodeWithTag("journey_restart_confirm").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("journey_restart_error").assertExists()
        compose.onNodeWithText("keep this note").assertExists()
        compose.onNodeWithTag("journey_restart_confirm").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Begin Anew in Grace").assertDoesNotExist()
        assertEquals(listOf("keep this note", "keep this note"), entryAttempts)
    }

    private fun SemanticsNode.allText(): List<String> = buildList {
        if (SemanticsProperties.Text in config) config[SemanticsProperties.Text].mapTo(this) { it.text }
        children.forEach { addAll(it.allText()) }
    }
}
