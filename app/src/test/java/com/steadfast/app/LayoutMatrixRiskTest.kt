package com.steadfast.app

import androidx.compose.ui.test.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutMatrixRiskTest {
    @get:Rule val compose = createComposeRule()


    @Test fun bibleFilterAndReadingSummaryRemainReachableAtNarrowWidthAndLargeFont() {
        var settingsClicked = false
        val books = listOf(
            com.steadfast.app.data.repository.BookWithProgress(
                com.steadfast.app.data.model.BibleBook(1, "First Corinthians Extended Name", "OT", 50, 1),
                emptySet(), 0, 0f, false
            ),
            com.steadfast.app.data.repository.BookWithProgress(
                com.steadfast.app.data.model.BibleBook(40, "Revelation Extended Name", "NT", 28, 40),
                emptySet(), 0, 0f, false
            )
        )
        compactPortrait {
            com.steadfast.app.ui.bible.BibleScreen(
                progress = com.steadfast.app.data.repository.BibleProgress(1189, 1189, 1f, 929, 929, 1f, 260, 260, 1f, 66, 66),
                books = books,
                otReading = com.steadfast.app.data.datastore.ReadingLocation(1, 50),
                ntReading = null,
                readingGoal = 99,
                onBookClicked = {},
                onSettingsClicked = { settingsClicked = true }
            )
        }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Reading target: 99+ chapters"))
        compose.onNodeWithText("Reading target: 99+ chapters").assertIsDisplayed()
        assertTextInside("Reading target: 99+ chapters")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("New Testament (27)"))
        compose.onNodeWithText("New Testament (27)").assertIsDisplayed()
        assertTextInside("New Testament (27)")
        compose.onNodeWithText("New Testament (27)").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Revelation Extended Name"))
        compose.onNodeWithText("Revelation Extended Name").assertIsDisplayed()
        compose.onNodeWithTag("bible_book_item_1").assertDoesNotExist()
        compose.onNodeWithText("Set in Settings").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(settingsClicked) }
    }

    @Test fun bookDetailInstructionAndFirstChapterStayReachableAtNarrowWidthAndLargeFont() {
        var toggledChapter = 0
        val book = com.steadfast.app.data.model.BibleBook(1, "First Corinthians Extended Name", "OT", 50, 1)
        val progress = com.steadfast.app.data.repository.BookWithProgress(book, emptySet(), 0, 0f, false)
        compactPortrait {
            com.steadfast.app.ui.bible.BookDetailScreen(
                progress, null, onToggleChapter = { toggledChapter = it }, onSetCurrentReading = {}, onBackClicked = {}
            )
        }

        compose.onNodeWithText("Tap to toggle • Long press to bookmark").assertExists()
        assertTextInside("Tap to toggle • Long press to bookmark")
        compose.onNodeWithTag("chapter_cell_1").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("First chapter action did not run", toggledChapter == 1) }
    }

    @Test fun journalFiltersAndComposerContextWrapAtNarrowWidthAndLargeFont() {
        compactPortrait {
            com.steadfast.app.ui.journey.JourneyScreen(
                activeSession = null, allSessions = emptyList(), archivedSessions = emptyList(), journalEntries = emptyList(),
                nextMilestone = null, longestStreakDays = 0,
                onStartAgainConfirmed = { _, _ -> }, onAddJournalEntry = { _, _ -> },
                onUpdateJournalEntry = { _, _, _ -> }, onDeleteJournalEntry = {},
                onNavigateToMilestones = {}, onNavigateToMotivation = {}, onSettingsClicked = {}
            )
        }

        compose.onNodeWithTag("journey_tab_Journal").performClick()
        assertControlInside("journal_filter_RESTART")
        compose.onNodeWithTag("journal_filter_RESTART").performClick().assertIsSelected()
        compose.onNodeWithTag("journey_add_journal_fab").performClick()
        assertControlInside("journal_context_RESTART")
        compose.onNodeWithTag("journal_context_RESTART").performClick().assertIsSelected()
    }

    @Test fun largeMotivationTotalsRemainFullyInsideTheirMetricCards() {
        val session = com.steadfast.app.data.model.StreakSessionEntity(
            id = 1, recoveryFocusId = "gaming", startEpochMillis = 0, startZoneId = "UTC"
        )
        var wentBack = false
        compactPortrait {
            com.steadfast.app.ui.motivation.MotivationScreen(session, emptyList(), onBackClicked = { wentBack = true })
        }

        val hours = com.steadfast.app.core.time.StreakDurationCalculator.calculate(0, "UTC").totalHours
        val value = NumberFormat.getNumberInstance(Locale.US).format(hours)
        compose.onNodeWithText(value).assertIsDisplayed()
        assertTextInside(value)
        compose.onNodeWithTag("motivation_back_button").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }

    @Test fun longMilestoneTitleRemainsReadableInShortLargeFontWindow() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val title = "A long milestone title describing a meaningful season of sustained recovery and renewed daily purpose"
        val definitions = (1..12).joinToString(",") { index ->
            "{\"id\":\"milestone_$index\",\"kind\":\"elapsed_days\",\"value\":$index,\"title\":\"${if (index == 1) title else "Milestone $index"}\"}"
        }
        val loader = com.steadfast.app.data.seed.AssetSeedLoader(context) { name ->
            if (name == "milestones.json") "{\"milestones\":[$definitions]}" else error("Unexpected asset: $name")
        }
        val repository = com.steadfast.app.data.repository.MilestoneRepository(
            loader, com.steadfast.app.data.datastore.SteadfastPreferences(context)
        )
        var wentBack = false
        compactPortrait {
            com.steadfast.app.ui.milestones.MilestonesScreen(null, repository, onBackClicked = { wentBack = true })
        }

        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        assertTextInside(title)
        compose.onNodeWithTag("milestones_back_button").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }

    private fun assertControlInside(tag: String) {
        val root = compose.onNodeWithTag("layout_matrix_root").getUnclippedBoundsInRoot()
        val control = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertTrue("$tag bounds $control exceed usable bounds $root", control.left >= root.left && control.top >= root.top && control.right <= root.right && control.bottom <= root.bottom && control.right - control.left > 0.dp && control.bottom - control.top >= 48.dp)
    }

    private fun assertTextInside(text: String) {
        val root = compose.onNodeWithTag("layout_matrix_root").getUnclippedBoundsInRoot()
        val node = compose.onNodeWithText(text).getUnclippedBoundsInRoot()
        assertTrue("Text '$text' bounds $node exceed usable bounds $root", node.left >= root.left && node.top >= root.top && node.right <= root.right && node.bottom <= root.bottom)
    }

    private fun compactPortrait(content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("layout_matrix_root")) {
                    SteadfastTheme { content() }
                }
            }
        }
    }
}
