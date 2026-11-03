package com.steadfast.app

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import com.steadfast.app.ui.journey.streakElapsedDurationLabel
import com.steadfast.app.ui.journey.JourneyScreen
import com.steadfast.app.ui.journey.JourneyTimelineChart
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
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Jan 1, 2024 — Jan 1, 2024"))
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
        val chartDescription = compose.onNodeWithTag("journey_timeline", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(chartDescription, chartDescription.contains("15 minutes"))
        assertTrue(chartDescription, chartDescription.contains("6 hours"))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Unknown recovery focus"))
        compose.onAllNodesWithText("Unknown recovery focus").assertCountEquals(1)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Gaming"))
        compose.onNodeWithText("Gaming").assertExists()
        compose.onNodeWithText("unknown-sensitive-id").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w400dp-h780dp-420dpi")
    fun thirtyYearTimelineMeasuresAndScrollsAtHighDensityAndLargeFont() {
        val session = StreakSessionEntity(
            id = 1, recoveryFocusId = "gaming", startZoneId = "UTC",
            startEpochMillis = Instant.parse("1990-01-01T00:00:00Z").toEpochMilli(),
            endEpochMillis = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli()
        )
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2.625f, 2f)) {
                SteadfastTheme { JourneyTimelineChart(listOf(session), Modifier.requiredSize(300.dp, 230.dp)) }
            }
        }
        compose.onNodeWithTag("journey_timeline").assertIsDisplayed()
        val chartBounds = compose.onNodeWithTag("journey_timeline").getUnclippedBoundsInRoot()
        listOf("Streak (days)", "Date / time · Swipe to explore history").forEach { label ->
            val node = compose.onNodeWithText(label).assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("Axis title clipped: $label $bounds / $chartBounds",
                bounds.left >= chartBounds.left && bounds.right <= chartBounds.right &&
                    bounds.top >= chartBounds.top && bounds.bottom <= chartBounds.bottom &&
                    bounds.right > bounds.left && bounds.bottom > bounds.top)
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Axis title has no text layout: $label", layouts.size == 1)
            val layout = remeasureTextAtMeasuredBounds(layouts.single())
            val glyphs = layout.layoutInput.text.indices.map(layout::getBoundingBox)
            assertTrue("Axis title overflows: $label size=${layout.size}, glyphs=$glyphs",
                !layout.hasVisualOverflow && glyphs.all {
                    it.left >= 0f && it.top >= 0f && it.right <= layout.size.width && it.bottom <= layout.size.height
                })
            assertEquals(label, layout.layoutInput.text.text)
        }
        val plot = compose.onNode(hasScrollAction(), useUnmergedTree = true)
        val range = plot.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue("Decades must retain a scrollable time axis", range.maxValue() > 0f)
        assertTrue("Canvas must stay below the Compose pixel constraint limit", range.maxValue() <= 60_000f)
        plot.performSemanticsAction(SemanticsActions.ScrollBy) { it(60_000f, 0f) }
        compose.waitForIdle()
        assertTrue("Long-history plot did not scroll", range.value() > 0f)
        compose.onNodeWithText("Streak (days)").assertIsDisplayed()
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
