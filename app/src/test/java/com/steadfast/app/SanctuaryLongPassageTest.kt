package com.steadfast.app

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.steadfast.app.core.design.ReducedMotionPolicy
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.ui.struggling.StrugglingInterventionScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h780dp", shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SanctuaryLongPassageTest {
    private var database: SteadfastDatabase? = null
    private var preferencesJob: Job? = null
    private var preferencesFile: File? = null

    // Compose/query coroutines must be disposed before their isolated storage fixture.
    @get:Rule(order = 0) val cleanup = object : ExternalResource() {
        override fun after() {
            database?.close()
            runBlocking { preferencesJob?.cancelAndJoin() }
            preferencesFile?.delete()
            ReducedMotionPolicy.testReducedMotionOverride = null
        }
    }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun longestCatalogPassageIsFullyReadableInOrdinaryPortrait() =
        assertLongestPassageReadable(400.dp, 780.dp, 1f)

    @Test fun longestCatalogPassageAndActionsRemainReachableAtNarrowLargeFont() =
        assertLongestPassageReadable(320.dp, 480.dp, 2f)

    private fun assertLongestPassageReadable(width: Dp, height: Dp, fontScale: Float) {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val loader = AssetSeedLoader(context)
        val catalog = loader.loadScriptureCatalog()
        assertEquals(500, catalog.size)
        val longest = catalog.maxBy { it.text.length }
        assertEquals("2 Peter 1:3-8", longest.reference)
        assertEquals(810, longest.text.length)
        val quoted = "“${longest.text}”"
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java)
            .allowMainThreadQueries().build().also { database = it }
        val job = SupervisorJob().also { preferencesJob = it }
        val file = File(context.filesDir, "sanctuary-long-${UUID.randomUUID()}.preferences_pb")
            .also { preferencesFile = it }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file }
        )
        runBlocking {
            store.edit {
                it[stringPreferencesKey("verse_deck_used")] = catalog.filter { entry -> entry.id != longest.id }.joinToString(",") { entry -> entry.id }
                it[stringPreferencesKey("verse_deck_last")] = catalog.first { entry -> entry.id != longest.id }.id
            }
        }
        val firstFrame = kotlinx.coroutines.CompletableDeferred<Unit>()
        val dao = object : com.steadfast.app.data.db.BibleDao by db.bibleDao() {
            override suspend fun getAllCompletedSync(): List<com.steadfast.app.data.model.BibleChapterCompletionEntity> {
                firstFrame.await()
                return db.bibleDao().getAllCompletedSync()
            }
        }
        val repository = VerseRepository(loader, dao, SteadfastPreferences(context, store))
        // Match the established layout fixture: prepare lazy catalog parsing before Compose starts.
        val loadedVerse = repository.getAllCatalogVerses().single { it.reference == longest.reference }
        assertEquals("“${loadedVerse.text}”", quoted)
        val callbacks = mutableSetOf<String>()
        var actualDensity: Density? = null
        ReducedMotionPolicy.testReducedMotionOverride = true
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                val configuredDensity = LocalDensity.current
                SideEffect { actualDensity = configuredDensity }
                Box(Modifier.requiredSize(width, height).clipToBounds().testTag(ROOT)) {
                    SteadfastTheme(themeSetting = ThemeSetting.DARK) {
                        StrugglingInterventionScreen(
                            verseRepository = repository,
                            onTakeBreathingExercise = { callbacks += "breathing" },
                            onWriteNote = { callbacks += "note" },
                            onViewProgress = { callbacks += "progress" },
                            onClose = { callbacks += "close" }
                        )
                    }
                }
            }
        }
        // Commit the loading composition before real Room/DataStore work can finish on an IO thread.
        compose.onNodeWithText("Loading Scripture…").assertIsDisplayed()
        compose.runOnIdle { firstFrame.complete(Unit) }
        try {
            compose.waitUntil(5_000) { compose.onAllNodesWithText(quoted).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            // Failure-only evidence distinguishes an async/deck error from semantics or viewport issues.
            println("SANCTUARY long-passage timeout: requested=$width×$height/font$fontScale actualDensity=$actualDensity " +
                "configuration=${context.resources.configuration} autoAdvance=${compose.mainClock.autoAdvance} selectedHistory=${repository.getRecentHistory()} " +
                "expectedReference=${longest.reference} expectedQuotedCharacters=${quoted.length}")
            println("SANCTUARY root geometry: " + runCatching {
                val node = compose.onNodeWithTag(ROOT).fetchSemanticsNode()
                "clipped=${node.boundsInRoot}, position=${node.positionInRoot}, size=${node.size}, " +
                    "unclipped=${compose.onNodeWithTag(ROOT).getUnclippedBoundsInRoot()}"
            }.getOrElse { "unavailable: $it" })
            println("SANCTUARY persisted deck: " + runCatching {
                runBlocking {
                    withTimeout(2_000) {
                        val prefs = store.data.first()
                        "usedCount=${prefs[stringPreferencesKey("verse_deck_used")]?.split(',')?.size}, " +
                            "last=${prefs[stringPreferencesKey("verse_deck_last")]}"
                    }
                }
            }.getOrElse { "unavailable: $it" })
            for (unmerged in listOf(false, true)) {
                for ((label, text) in listOf("quote" to quoted, "metadata" to "${longest.reference} · WEB",
                    "loading" to "Loading Scripture…", "error" to "Could not load Scripture. Please try another verse.")) {
                    println("SANCTUARY $label unmerged=$unmerged count=" + runCatching {
                        compose.onAllNodesWithText(text, useUnmergedTree = unmerged).fetchSemanticsNodes().size
                    }.getOrElse { -1 })
                }
                println("SANCTUARY semantics unmerged=$unmerged:\n" + runCatching {
                    compose.onRoot(useUnmergedTree = unmerged).printToString(maxDepth = 100)
                }.getOrElse { "unavailable: $it" })
            }
            throw failure
        }
        compose.runOnIdle { assertEquals("Configured font scale", fontScale, actualDensity!!.fontScale, 0f) }
        val rootBounds = compose.onNodeWithTag(ROOT).getUnclippedBoundsInRoot()
        assertEquals("Requested viewport width", width, rootBounds.right - rootBounds.left)
        assertEquals("Requested viewport height", height, rootBounds.bottom - rootBounds.top)
        compose.onNodeWithText("${longest.reference} · WEB").assertExists()

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(quoted).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("The full quotation must be passed to Text", quoted, layouts.single().layoutInput.text.text)
        val measured = remeasureTextAtMeasuredBounds(layouts.single())
        assertTrue("Quotation must have positive rendered dimensions", measured.size.width > 0 && measured.size.height > 0)
        // The former ordinary-portrait weighted card constrained Text height and failed this assertion.
        assertFalse("Longest passage is clipped at font$fontScale in ${width}×$height: ${measured.size}", measured.hasVisualOverflow)
        assertEquals("The final quotation character must be laid out", quoted.length, measured.getLineEnd(measured.lineCount - 1, visibleEnd = true))

        // Text semantics alone can retain clipped characters. Verify first and final rendered lines can
        // each be moved fully into the real clipped viewport using its vertical scrolling action.
        compose.onNode(hasScrollAction()).assertExists()
        compose.onNodeWithText("${longest.reference} · WEB").performScrollTo().assertIsDisplayed()
        assertLineReachable(quoted, measured, 0)
        assertLineReachable(quoted, measured, measured.lineCount - 1)

        listOf("struggling_back_button", "struggling_close_button", "struggling_breathing_button",
            "struggling_another_verse_button", "struggling_write_note_button", "struggling_view_progress_button")
            .forEach(::assertControlReachable)
        compose.onNodeWithText("I'm Grounded & Standing Firm Now").performScrollTo().assertIsDisplayed()
        assertInsideRoot(compose.onNodeWithText("I'm Grounded & Standing Firm Now").getUnclippedBoundsInRoot(), "Grounded affirmation")

        compose.onNodeWithTag("struggling_breathing_button").performScrollTo().performClick()
        compose.onNodeWithTag("struggling_write_note_button").performScrollTo().performClick()
        compose.onNodeWithTag("struggling_view_progress_button").performScrollTo().performClick()
        compose.onNodeWithTag("struggling_close_button").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(setOf("breathing", "note", "progress", "close"), callbacks) }
    }

    private fun assertLineReachable(quoted: String, layout: TextLayoutResult, line: Int) {
        repeat(12) {
            compose.waitForIdle()
            val viewport = compose.onNodeWithTag(ROOT).fetchSemanticsNode().boundsInRoot
            val position = compose.onNodeWithText(quoted).fetchSemanticsNode().positionInRoot
            val top = position.y + layout.getLineTop(line)
            val bottom = position.y + layout.getLineBottom(line)
            val left = position.x + layout.getLineLeft(line)
            val right = position.x + layout.getLineRight(line)
            assertTrue("Passage line $line extends outside viewport horizontally: $left..$right / $viewport",
                left >= viewport.left - 1f && right <= viewport.right + 1f)
            if (top >= viewport.top - 1f && bottom <= viewport.bottom + 1f) return
            val delta = if (top < viewport.top) top - viewport.top - 1f else bottom - viewport.bottom + 1f
            compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, delta) }
        }
        throw AssertionError("Could not scroll rendered passage line $line fully into the viewport")
    }

    private fun assertControlReachable(tag: String) {
        val node = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        assertInsideRoot(bounds, tag)
        assertTrue("$tag must retain a 48dp target: $bounds", bounds.right - bounds.left >= 48.dp && bounds.bottom - bounds.top >= 48.dp)
    }

    private fun assertInsideRoot(bounds: androidx.compose.ui.unit.DpRect, label: String) {
        val root = compose.onNodeWithTag(ROOT).getUnclippedBoundsInRoot()
        assertTrue("$label bounds $bounds exceed usable window $root", bounds.left >= root.left && bounds.top >= root.top && bounds.right <= root.right && bounds.bottom <= root.bottom)
    }

    private companion object { const val ROOT = "sanctuary_long_passage_root" }
}
