package com.steadfast.app

import androidx.compose.ui.test.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h780dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutRiskRegressionTest {
    private var databaseToClose: com.steadfast.app.data.db.SteadfastDatabase? = null
    // Dispose the composition and its query coroutines before closing their Room fixture.
    @get:Rule(order = 0) val databaseCleanup = object : org.junit.rules.ExternalResource() {
        override fun after() { databaseToClose?.close() }
    }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun strugglingActionsRemainReachableInShortPortraitAtLargeFont() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = androidx.room.Room.inMemoryDatabaseBuilder(
            context, com.steadfast.app.data.db.SteadfastDatabase::class.java
        ).allowMainThreadQueries().build().also { databaseToClose = it }
        var breathingClicked = false
        val firstFrame = kotlinx.coroutines.CompletableDeferred<Unit>()
        val dao = object : com.steadfast.app.data.db.BibleDao by database.bibleDao() {
            override suspend fun getAllCompletedSync(): List<com.steadfast.app.data.model.BibleChapterCompletionEntity> {
                firstFrame.await()
                return database.bibleDao().getAllCompletedSync()
            }
        }
        val repository = com.steadfast.app.data.repository.VerseRepository(
            com.steadfast.app.data.seed.AssetSeedLoader(context), dao
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.struggling.StrugglingInterventionScreen(
                            repository,
                            onTakeBreathingExercise = { breathingClicked = true },
                            onWriteNote = {}, onViewProgress = {}, onClose = {}
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Loading Scripture…").assertIsDisplayed()
        compose.runOnIdle { firstFrame.complete(Unit) }
        compose.onNodeWithTag("struggling_breathing_button").performScrollTo().assertIsDisplayed()
        assertFullyInside("struggling_breathing_button")
        compose.onNodeWithTag("struggling_breathing_button").performClick()
        compose.runOnIdle { assertTrue("Breathing action did not invoke its callback", breathingClicked) }

    }

    @Test fun dashboardPrimaryActionRemainsReachableInCompactPortraitAtLargeFont() {
        var strugglingClicked = false
        val verse = com.steadfast.app.data.repository.CuratedVerse(
            "1 Corinthians 10:13", "1 Corinthians", 10, "13", "temptation",
            text = "No temptation has taken you except what is common to man. God is faithful, who will not allow you to be tempted above what you are able, but will with the temptation also make the way of escape, that you may be able to endure it.",
            translation = "WEB"
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = null,
                            dailyEncouragement = verse,
                            onStrugglingClicked = { strugglingClicked = true },
                            onSettingsClicked = {}
                        )
                    }
                }
            }
        }

        assertFullyInside("dashboard_struggling_button")
        compose.onNodeWithTag("dashboard_struggling_button").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("Dashboard action did not invoke its callback", strugglingClicked) }
    }

    @Test
    @Config(sdk = [34], qualifiers = "w640dp-h360dp")
    fun intermediateShortDashboardKeepsControlsAndFiveUnitTimerReachableAtLargeFont() {
        val verse = com.steadfast.app.data.repository.CuratedVerse(
            "1 Corinthians 10:13", "1 Corinthians", 10, "13", "temptation",
            text = "God is faithful and provides a way through temptation.", translation = "WEB"
        )
        val expectedUnits = com.steadfast.app.core.time.StreakDurationCalculator.getDisplayUnits(
            com.steadfast.app.core.time.StreakDurationCalculator.calculate(0L, "UTC")
        )
        val fontScale = androidx.compose.runtime.mutableFloatStateOf(1.3f)
        var settingsClicked = false
        var strugglingClicked = false

        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                Box(Modifier.requiredSize(640.dp, 300.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = com.steadfast.app.data.model.StreakSessionEntity(
                                recoveryFocusId = "gaming", startEpochMillis = 0L, startZoneId = "UTC"
                            ),
                            dailyEncouragement = verse,
                            onStrugglingClicked = { strugglingClicked = true },
                            onSettingsClicked = { settingsClicked = true }
                        )
                    }
                }
            }
        }

        for (scale in listOf(1.3f, 1.5f)) {
            settingsClicked = false
            strugglingClicked = false
            compose.runOnIdle { fontScale.floatValue = scale }
            compose.waitForIdle()
            assertFullyInside("dashboard_settings_button")
            assertFullyInside("dashboard_streak_circle")
            assertFullyInside("dashboard_streak_units")
            assertFullyInside("dashboard_struggling_button")
            val usableBounds = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
            val valueOccurrences = mutableMapOf<String, Int>()
            expectedUnits.forEach { unit ->
                assertTextLayoutInsideWindow(unit.label, usableBounds)
                if (unit.label != "secs") {
                    val value = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0')
                    val index = valueOccurrences.getOrDefault(value, 0)
                    assertTextLayoutInsideWindow(value, usableBounds, index)
                    valueOccurrences[value] = index + 1
                }
            }
            compose.onNodeWithTag("dashboard_settings_button").assertIsDisplayed().performClick()
            compose.onNodeWithTag("dashboard_struggling_button").assertIsDisplayed().performClick()
            compose.runOnIdle { assertTrue("Settings action did not run at font scale $scale", settingsClicked) }
            compose.runOnIdle { assertTrue("Struggling action did not run at font scale $scale", strugglingClicked) }
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h400dp")
    fun veryWideShortDashboardKeepsFiveUnitTimerReadableAtDoubleFontScale() {
        val verse = com.steadfast.app.data.repository.CuratedVerse(
            "Psalm 46:1", "Psalms", 46, "1", "peace", text = "God is our refuge.", translation = "WEB"
        )
        val expectedUnits = com.steadfast.app.core.time.StreakDurationCalculator.getDisplayUnits(
            com.steadfast.app.core.time.StreakDurationCalculator.calculate(0L, "UTC")
        )
        var settingsClicked = false
        var strugglingClicked = false

        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(900.dp, 340.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = com.steadfast.app.data.model.StreakSessionEntity(
                                recoveryFocusId = "gaming", startEpochMillis = 0L, startZoneId = "UTC"
                            ),
                            dailyEncouragement = verse,
                            onStrugglingClicked = { strugglingClicked = true },
                            onSettingsClicked = { settingsClicked = true }
                        )
                    }
                }
            }
        }

        assertFullyInside("dashboard_settings_button")
        assertFullyInside("dashboard_streak_circle")
        assertFullyInside("dashboard_streak_units")
        assertFullyInside("dashboard_struggling_button")
        val usableBounds = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
        val valueOccurrences = mutableMapOf<String, Int>()
        expectedUnits.forEach { unit ->
            assertTextLayoutInsideWindow(unit.label, usableBounds)
            if (unit.label != "secs") {
                val value = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0')
                val index = valueOccurrences.getOrDefault(value, 0)
                assertTextLayoutInsideWindow(value, usableBounds, index)
                valueOccurrences[value] = index + 1
            }
        }
        compose.onNodeWithTag("dashboard_settings_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("dashboard_struggling_button").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("Settings action did not run", settingsClicked) }
        compose.runOnIdle { assertTrue("Struggling action did not run", strugglingClicked) }
    }

    @Test fun compactDashboardKeepsThreeUnitCounterTextReachableAtLargeFont() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = null,
                            dailyEncouragement = com.steadfast.app.data.repository.CuratedVerse(
                                "Psalm 46:1", "Psalms", 46, "1", "peace", text = "God is our refuge.", translation = "WEB"
                            ),
                            onStrugglingClicked = {}, onSettingsClicked = {}
                        )
                    }
                }
            }
        }

        assertCompactTimerInsideWindow(listOf("days", "hours", "secs"))
    }

    @Test
    @Config(sdk = [34], qualifiers = "w640dp-h208dp")
    fun dashboardAtPhysicalCompactLandscapeHeightKeepsActionVerseReferenceAndShortTimer() {
        val verse = com.steadfast.app.data.repository.CuratedVerse(
            "John 11:35", "John", 11, "35", "comfort", text = "Jesus wept.", translation = "WEB"
        )
        val expectedUnits = listOf(
            com.steadfast.app.core.time.TimeUnitDisplay(0, "days"),
            com.steadfast.app.core.time.TimeUnitDisplay(0, "hours"),
            com.steadfast.app.core.time.TimeUnitDisplay(0, "secs")
        )
        val activeSession = androidx.compose.runtime.mutableStateOf<com.steadfast.app.data.model.StreakSessionEntity?>(null)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(640.dp, 208.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = activeSession.value,
                            dailyEncouragement = verse,
                            onStrugglingClicked = {}, onSettingsClicked = {}
                        )
                    }
                }
            }
        }

        val usableBounds = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
        assertFullyInside("dashboard_struggling_button")
        assertFullyInside("dashboard_streak_circle")
        assertCompactTimerInsideWindow(expectedUnits.map { it.label })
        expectedUnits.forEach { assertSingleLineText(it.label) }
        val valueOccurrences = mutableMapOf<String, Int>()
        expectedUnits.forEach { unit ->
            val value = unit.value.toString().padStart(if (unit.label == "days" && unit.value < 10) 1 else 2, '0')
            val index = valueOccurrences.getOrDefault(value, 0)
            assertTextLayoutInsideWindow(value, usableBounds, index)
            valueOccurrences[value] = index + 1
        }
        compose.onNodeWithText(verse.reference).assertIsDisplayed().assertWidthIsAtLeast(90.dp)
        assertTextLayoutInsideWindow(verse.reference, usableBounds)
        compose.onNodeWithText("“${verse.text}”").performScrollTo().assertIsDisplayed()
        assertTextLayoutInsideWindow("“${verse.text}”", usableBounds)

        compose.runOnIdle {
            activeSession.value = com.steadfast.app.data.model.StreakSessionEntity(
                recoveryFocusId = "gaming", startEpochMillis = 0L, startZoneId = "UTC"
            )
        }
        compose.waitForIdle()
        val longTierBounds = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
        assertFullyInside("dashboard_struggling_button")
        // A live counter legitimately uses the singular label at value 1.
        val longLabels = listOf("years", "months", "days", "hours", "secs").map { plural ->
            if (compose.onAllNodesWithText(plural).fetchSemanticsNodes().isNotEmpty()) plural else plural.removeSuffix("s")
        }
        assertCompactTimerInsideWindow(longLabels)
        longLabels.forEach {
            assertSingleLineText(it)
            assertTextLayoutInsideWindow(it, longTierBounds)
        }
    }

    @Test fun compactDashboardKeepsFiveUnitCounterTextReachableAtLargeFont() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.dashboard.DashboardScreen(
                            activeSession = com.steadfast.app.data.model.StreakSessionEntity(
                                recoveryFocusId = "gaming", startEpochMillis = 0L, startZoneId = "UTC"
                            ),
                            dailyEncouragement = com.steadfast.app.data.repository.CuratedVerse(
                                "Psalm 46:1", "Psalms", 46, "1", "peace", text = "God is our refuge.", translation = "WEB"
                            ),
                            onStrugglingClicked = {}, onSettingsClicked = {}
                        )
                    }
                }
            }
        }

        val labels = listOf("years", "months", "days", "hours", "secs").map { plural ->
            if (compose.onAllNodesWithText(plural).fetchSemanticsNodes().isNotEmpty()) plural else plural.removeSuffix("s")
        }
        assertCompactTimerInsideWindow(labels)
    }

    @Test fun breathingFinishActionRemainsReachableInShortLandscape() {
        var finished = false
        com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = true
        try {
            compose.setContent {
                Box(Modifier.requiredSize(568.dp, 320.dp).clipToBounds().testTag("layout_usable_window")) {
                    SteadfastTheme {
                        com.steadfast.app.ui.struggling.BreathingExerciseScreen(onDoneClicked = { finished = true })
                    }
                }
            }

            assertFullyInside("breathing_finish_button")
            compose.onNodeWithTag("breathing_finish_button").assertIsDisplayed().performClick()
            compose.runOnIdle { assertTrue("Finish action did not invoke its callback", finished) }
        } finally {
            com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = null
        }
    }

    private fun assertFullyInside(tag: String) {
        val root = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
        val control = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        println("Layout bounds $tag: usable=$root control=$control")
        assertTrue(
            "$tag bounds $control exceed usable bounds $root",
            control.left >= root.left && control.top >= root.top && control.right <= root.right && control.bottom <= root.bottom && control.right - control.left > 0.dp && control.bottom - control.top >= 48.dp
        )
    }

    private fun assertCompactTimerInsideWindow(labels: List<String>) {
        val root = compose.onNodeWithTag("layout_usable_window").getUnclippedBoundsInRoot()
        val circle = compose.onNodeWithTag("dashboard_streak_circle").getUnclippedBoundsInRoot()
        val units = compose.onNodeWithTag("dashboard_streak_units").getUnclippedBoundsInRoot()
        assertTrue("Timer ring exceeds usable bounds: $circle / $root", circle.left >= root.left && circle.top >= root.top && circle.right <= root.right && circle.bottom <= root.bottom)
        assertTrue("Timer units exceed usable bounds: $units / $root", units.left >= root.left && units.top >= root.top && units.right <= root.right && units.bottom <= root.bottom)
        assertTrue("Timer ring and labels overlap: $circle / $units", circle.right <= units.left || units.right <= circle.left)
        labels.forEach { label ->
            assertTextLayoutInsideWindow(label, root)
        }
        if (labels.size == 3) compose.onAllNodesWithText("0").fetchSemanticsNodes().indices.forEach { index ->
            assertTextLayoutInsideWindow("0", root, index)
        }
    }

    private fun assertTextLayoutInsideWindow(text: String, root: androidx.compose.ui.unit.DpRect, index: Int = 0) {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        assertTrue("Expected timer text '$text' to render", nodes.size > index)
        val node = compose.onAllNodesWithText(text).get(index)
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue("Timer text '$text' was clipped to zero height: $bounds", bounds.bottom > bounds.top)
        assertTrue("Timer text '$text' extends outside the usable window: $bounds / $root", bounds.left >= root.left && bounds.top >= root.top && bounds.right <= root.right && bounds.bottom <= root.bottom)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Timer text '$text' has no layout result", layouts.isNotEmpty())
        val layout = layouts.single()
        val drawnLayout = remeasureTextAtMeasuredBounds(layout)
        val glyphBounds = layout.layoutInput.text.indices.map(drawnLayout::getBoundingBox)
        assertFalse(
            "Timer text '$text' overflows its measured node; nodeSize=${layout.size}, " +
                "remeasuredSize=${drawnLayout.size}, glyphBounds=$glyphBounds, " +
                "semanticsConstraints=${layout.layoutInput.constraints}, " +
                "semanticsParagraphWidth=${layout.multiParagraph.width}, " +
                "semanticsLineLeft=${layout.multiParagraph.getLineLeft(0)}, " +
                "semanticsLineRight=${layout.multiParagraph.getLineRight(0)}",
            drawnLayout.hasVisualOverflow || glyphBounds.any {
                it.left < 0f || it.top < 0f || it.right > drawnLayout.size.width || it.bottom > drawnLayout.size.height
            }
        )
        assertTrue("Timer text did not render in full: ${layout.layoutInput.text}", layout.layoutInput.text.text == text)
    }

    private fun assertSingleLineText(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Expected one text layout for '$text'", layouts.size == 1)
        assertTrue("Timer label '$text' wrapped across ${layouts.single().lineCount} lines", layouts.single().lineCount == 1)
    }
}

/**
 * Compose 1.7 GetTextLayoutResult remeasures at parent max width while retaining Text's intrinsic size.
 * See ParagraphLayoutCache.slowCreateTextLayoutResultOrNull in the pinned official source archive:
 * https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.7.0/foundation-android-1.7.0-sources.jar
 */
internal fun remeasureTextAtMeasuredBounds(layout: TextLayoutResult): TextLayoutResult {
    val input = layout.layoutInput
    return TextMeasurer(
        defaultFontFamilyResolver = input.fontFamilyResolver,
        defaultDensity = input.density,
        defaultLayoutDirection = input.layoutDirection,
        cacheSize = 0
    ).measure(
        text = input.text,
        style = input.style,
        overflow = input.overflow,
        softWrap = input.softWrap,
        maxLines = input.maxLines,
        placeholders = input.placeholders,
        constraints = Constraints(maxWidth = layout.size.width, maxHeight = layout.size.height),
        layoutDirection = input.layoutDirection
    )
}
