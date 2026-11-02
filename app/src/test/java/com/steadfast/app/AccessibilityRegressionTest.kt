package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.steadfast.app.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.pow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun functionalRolePairsAreReadableInBothThemes() {
        val theme = androidx.compose.runtime.mutableStateOf(ThemeSetting.LIGHT)
        var pairs = emptyList<Triple<String, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>>()
        compose.setContent { SteadfastTheme(theme.value) {
            val c = androidx.compose.material3.MaterialTheme.colorScheme
            androidx.compose.runtime.SideEffect {
                pairs = listOf(
                    Triple("primary/background", c.primary, c.background),
                    Triple("primary/surface", c.primary, c.surface),
                    Triple("primary/surfaceVariant", c.primary, c.surfaceVariant),
                    Triple("onPrimary/primary", c.onPrimary, c.primary),
                    Triple("onPrimaryContainer/primaryContainer", c.onPrimaryContainer, c.primaryContainer),
                    Triple("onSurface/surface", c.onSurface, c.surface),
                    Triple("onSurfaceVariant/surfaceVariant", c.onSurfaceVariant, c.surfaceVariant),
                    Triple("error/surface", c.error, c.surface),
                    Triple("onErrorContainer/errorContainer", c.onErrorContainer, c.errorContainer)
                )
            }
        } }
        for (mode in listOf(ThemeSetting.LIGHT, ThemeSetting.DARK)) {
            compose.runOnIdle { theme.value = mode }
            compose.waitForIdle()
            pairs.forEach { (label, foreground, background) ->
                val light = maxOf(foreground.luminance(), background.luminance())
                val dark = minOf(foreground.luminance(), background.luminance())
                val ratio = (light + .05f) / (dark + .05f)
                assertTrue("$mode $label contrast was $ratio", ratio >= 4.5f)
            }
        }
    }
    @Test fun lightFunctionalPrimaryHasReadableContrast() {
        fun luminance(c: androidx.compose.ui.graphics.Color): Double {
            fun linear(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return .2126 * linear(c.red) + .7152 * linear(c.green) + .0722 * linear(c.blue)
        }
        val ratio = (luminance(SteadfastCreamLight) + .05) / (luminance(SteadfastGoldLightMode) + .05)
        assertTrue("Actual light primary/cream contrast was $ratio", ratio >= 4.5)
    }
    @Test fun dashboardPrimaryActionTextIsReadableOnItsFixedNavySurfaceInBothThemes() {
        val theme = androidx.compose.runtime.mutableStateOf(ThemeSetting.LIGHT)
        compose.setContent { SteadfastTheme(theme.value) {
            com.steadfast.app.ui.dashboard.DashboardScreen(
                null,
                com.steadfast.app.data.repository.CuratedVerse("John 3:16", "John", 3, "16", "hope"),
                {}, {}
            )
        } }
        for (mode in listOf(ThemeSetting.LIGHT, ThemeSetting.DARK)) {
            compose.runOnIdle { theme.value = mode }
            compose.waitForIdle()
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText("I'm Struggling", useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val foreground = layouts.single().layoutInput.style.color
            val background = androidx.compose.ui.graphics.Color(0xFF1B2333)
            val ratio = (maxOf(foreground.luminance(), background.luminance()) + .05f) /
                (minOf(foreground.luminance(), background.luminance()) + .05f)
            assertTrue("$mode actual Dashboard action contrast was $ratio", ratio >= 4.5f)
        }
    }
    @Test fun selectedFocusHasExplicitRadioSelectionSemantics() {
        compose.setContent { SteadfastTheme {
            com.steadfast.app.ui.onboarding.OnboardingWizardScreen(
                listOf(com.steadfast.app.data.model.RecoveryFocus("discipline", "Discipline")), emptyList()
            ) { _, _, _, _, _, _, _, _, _, _ -> }
        } }
        compose.onNodeWithTag("onboarding_welcome_next").performClick()
        compose.onNodeWithTag("focus_item_discipline").performClick().assertIsSelected()
    }

    @Test fun strugglingShowsTruthfulLoadingWhileTheVerseQueryIsPending() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = androidx.room.Room.inMemoryDatabaseBuilder(context,
            com.steadfast.app.data.db.SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val gate = kotlinx.coroutines.CompletableDeferred<List<com.steadfast.app.data.model.BibleChapterCompletionEntity>>()
        val dao = object : com.steadfast.app.data.db.BibleDao by database.bibleDao() {
            override suspend fun getAllCompletedSync() = gate.await()
        }
        try {
            val repository = com.steadfast.app.data.repository.VerseRepository(com.steadfast.app.data.seed.AssetSeedLoader(context), dao)
            compose.setContent { SteadfastTheme {
                com.steadfast.app.ui.struggling.StrugglingInterventionScreen(repository, {}, {}, {}, {})
            } }
            compose.onNodeWithText("Loading Scripture…").assertExists()
        } finally { gate.complete(emptyList()); database.close() }
    }
}
