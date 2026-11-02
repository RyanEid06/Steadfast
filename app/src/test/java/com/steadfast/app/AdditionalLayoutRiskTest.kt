package com.steadfast.app

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.ui.theme.SteadfastTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdditionalLayoutRiskTest {
    private var databaseToClose: com.steadfast.app.data.db.SteadfastDatabase? = null
    // Dispose the composition and its query coroutines before closing their Room fixture.
    @get:Rule(order = 0) val databaseCleanup = object : org.junit.rules.ExternalResource() {
        override fun after() { databaseToClose?.close() }
    }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun longStrugglingScriptureMetadataWrapsInsideTheCompactWindow() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val database = androidx.room.Room.inMemoryDatabaseBuilder(
            context, SteadfastDatabase::class.java
        ).allowMainThreadQueries().build().also { databaseToClose = it }
        com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = true
        try {
            val loader = AssetSeedLoader(context) { name ->
                if (name == "verse_reference_catalog_starter.json") {
                    """{"themes":{"temptation":["1 Corinthians 10:13"],"strength":["1 Corinthians 10:13"],"love":["1 Corinthians 10:13"],"forgiveness":["1 Corinthians 10:13"],"self_control":["1 Corinthians 10:13"],"peace":["1 Corinthians 10:13"],"perseverance":["1 Corinthians 10:13"],"renewal":["1 Corinthians 10:13"]}}"""
                } else {
                    context.assets.open(name).bufferedReader().use { it.readText() }
                }
            }
            val repository = VerseRepository(loader, database.bibleDao())
            val loadedVerse = runBlocking { repository.getNextVerse() }
            val callbacks = mutableSetOf<String>()
            compose.setContent {
                CompactLargeFontWindow {
                    SteadfastTheme {
                        com.steadfast.app.ui.struggling.StrugglingInterventionScreen(
                            verseRepository = repository,
                            onTakeBreathingExercise = { callbacks += "breathing" },
                            onWriteNote = { callbacks += "note" },
                            onViewProgress = { callbacks += "progress" },
                            onClose = { callbacks += "close" }
                        )
                    }
                }
            }

            val metadata = "1 Corinthians 10:13 · WEB"
            val scripture = "“No temptation has taken you except what is common to man. God is faithful, who will not allow you to be tempted above what you are able, but will with the temptation also make the way of escape, that you may be able to endure it.”"
            assertTrue("Deterministic verse fixture was not loaded: $loadedVerse",
                loadedVerse.reference == "1 Corinthians 10:13" && loadedVerse.translation == "WEB")
            val metadataLoaded = runCatching {
                compose.waitUntil(5_000) { compose.onAllNodesWithText(metadata).fetchSemanticsNodes().isNotEmpty() }
            }.isSuccess
            if (!metadataLoaded) {
                val metadataCount = compose.onAllNodesWithText(metadata).fetchSemanticsNodes().size
                val loadingCount = compose.onAllNodesWithText("Loading Scripture…").fetchSemanticsNodes().size
                compose.onRoot(useUnmergedTree = true).printToLog("struggling-scripture-metadata-failure")
                assertTrue("Expected scripture metadata after verse load; metadata nodes=$metadataCount, loading nodes=$loadingCount",
                    metadataLoaded)
            }
            compose.onNodeWithText(metadata).performScrollTo().assertIsDisplayed()
            assertFullyInside(metadata)
            assertTextHasNoVisualOverflow(metadata)
            compose.onNodeWithText(scripture).performScrollTo().assertIsDisplayed()
            assertTextHasPositiveBounds(scripture)
            assertTextHasNoVisualOverflow(scripture)

            listOf(
                "struggling_back_button", "struggling_close_button", "struggling_breathing_button",
                "struggling_another_verse_button", "struggling_write_note_button", "struggling_view_progress_button"
            ).forEach(::assertControlReachable)
            val affirmation = "I'm Grounded & Standing Firm Now"
            compose.onNodeWithText(affirmation).performScrollTo().assertIsDisplayed()
            assertFullyInside(affirmation)

            compose.onNodeWithTag("struggling_breathing_button").performScrollTo().performClick()
            compose.onNodeWithTag("struggling_another_verse_button").performScrollTo().performClick()
            compose.onNodeWithTag("struggling_write_note_button").performScrollTo().performClick()
            compose.onNodeWithTag("struggling_view_progress_button").performScrollTo().performClick()
            compose.onNodeWithTag("struggling_close_button").performScrollTo().performClick()
            compose.runOnIdle { assertTrue(setOf("breathing", "note", "progress", "close").all(callbacks::contains)) }
        } finally {
            com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = null
        }
    }

    @Test fun journeyCurrentAndLongestStatsFitAtCompactLargeFontAndLongValues() {
        val currentDays = 123_456_789L
        val longestDays = 987_654_321L
        compose.setContent {
            CompactLargeFontWindow {
                SteadfastTheme {
                    com.steadfast.app.ui.journey.JourneyScreen(
                        activeSession = StreakSessionEntity(
                            recoveryFocusId = "gaming", startEpochMillis = 0L, startZoneId = "UTC"
                        ),
                        allSessions = emptyList(), archivedSessions = emptyList(), journalEntries = emptyList(),
                        nextMilestone = null, longestStreakDays = longestDays,
                        currentStreakDays = currentDays,
                        onStartAgainConfirmed = { _, _ -> }, onAddJournalEntry = { _, _ -> },
                        onUpdateJournalEntry = { _, _, _ -> }, onDeleteJournalEntry = {},
                        onNavigateToMilestones = {}, onNavigateToMotivation = {}, onSettingsClicked = {}
                    )
                }
            }
        }

        listOf("CURRENT STREAK", "$currentDays days", "LONGEST STREAK", "$longestDays days").forEach { text ->
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
            assertFullyInside(text)
            assertTextHasNoVisualOverflow(text)
        }
    }

    private fun assertFullyInside(text: String) {
        val root = compose.onNodeWithTag("additional_layout_root").getUnclippedBoundsInRoot()
        val bounds = compose.onNodeWithText(text).getUnclippedBoundsInRoot()
        assertTrue("Text '$text' bounds $bounds exceed usable bounds $root",
            bounds.left >= root.left && bounds.top >= root.top && bounds.right <= root.right && bounds.bottom <= root.bottom)
    }

    private fun assertControlReachable(tag: String) {
        val node = compose.onNodeWithTag(tag).performScrollTo()
        val root = compose.onNodeWithTag("additional_layout_root").getUnclippedBoundsInRoot()
        val control = node.getUnclippedBoundsInRoot()
        assertTrue("Control $tag is outside the usable window: $control / $root",
            control.left >= root.left && control.top >= root.top && control.right <= root.right && control.bottom <= root.bottom)
        assertTrue("Control $tag is smaller than a 48dp touch target: $control",
            control.right - control.left >= 48.dp && control.bottom - control.top >= 48.dp)
        node.assertIsDisplayed()
    }

    private fun assertTextHasNoVisualOverflow(text: String): androidx.compose.ui.text.TextLayoutResult {
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Text '$text' has no layout result", layouts.isNotEmpty())
        val measured = remeasureTextAtMeasuredBounds(layouts.single())
        assertFalse("Text '$text' visually overflows at its rendered bounds: ${measured.size}, ${measured.layoutInput.constraints}",
            measured.hasVisualOverflow)
        return measured
    }

    private fun assertTextHasPositiveBounds(text: String) {
        val bounds = compose.onNodeWithText(text).getUnclippedBoundsInRoot()
        assertTrue("Text '$text' has no rendered bounds: $bounds", bounds.right > bounds.left && bounds.bottom > bounds.top)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Text '$text' has no text layout result", layouts.isNotEmpty())
        assertTrue("Text '$text' was truncated", layouts.single().layoutInput.text.text == text)
    }

    @Composable
    private fun CompactLargeFontWindow(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("additional_layout_root")) {
                content()
            }
        }
    }
}
