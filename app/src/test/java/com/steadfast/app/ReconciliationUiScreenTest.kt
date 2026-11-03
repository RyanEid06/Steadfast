package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.steadfast.app.data.repository.CuratedVerse
import com.steadfast.app.ui.dashboard.DashboardScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.unit.dp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h780dp", shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReconciliationUiScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dashboardDisplaysTheSelectedPassageAndItsTranslation() {
        compose.setContent {
            SteadfastTheme {
                DashboardScreen(null, CuratedVerse("James 4:7", "James", 4, "7", "temptation", text = "Be subject therefore to God. Resist the devil, and he will flee from you.", translation = "WEB"), {}, {})
            }
        }
        compose.onNodeWithText("WEB").assertExists()
        compose.onNodeWithText("“Be subject therefore to God. Resist the devil, and he will flee from you.”").assertExists()
        compose.onNodeWithText("NLT").assertDoesNotExist()
    }

    @Test fun dashboardDisplaysTheFullVerifiedMatthewPassage() {
        val text = "Watch and pray, that you don’t enter into temptation. The spirit indeed is willing, but the flesh is weak.”"
        compose.setContent {
            SteadfastTheme {
                DashboardScreen(null, CuratedVerse("Matthew 26:41", "Matthew", 26, "41", "temptation", text = text, translation = "WEB"), {}, {})
            }
        }
        compose.onNodeWithText("“$text”").assertExists()
        compose.onNodeWithText("Matthew 26:41").assertExists()
        compose.onNodeWithText("WEB").assertExists()
        compose.onNodeWithText("Open your Bible to Matthew 26:41.").assertDoesNotExist()
        compose.onNodeWithText("NLT").assertDoesNotExist()
    }
    @Test fun onboardingCanFinishWithBothReadingLocationsUnset() {
        var locations: List<Int?>? = null
        compose.setContent {
            SteadfastTheme {
                com.steadfast.app.ui.onboarding.OnboardingWizardScreen(
                    listOf(com.steadfast.app.data.model.RecoveryFocus("discipline", "Discipline")),
                    listOf(com.steadfast.app.data.model.BibleBook(1, "Genesis", "OT", 50, 1), com.steadfast.app.data.model.BibleBook(40, "Matthew", "NT", 28, 40))
                ) { _, _, _, otBook, otChapter, ntBook, ntChapter, _, _, _ ->
                    locations = listOf(otBook, otChapter, ntBook, ntChapter)
                }
            }
        }
        compose.onNodeWithTag("onboarding_welcome_next").performClick()
        compose.onNodeWithTag("onboarding_focus_next").assertIsNotEnabled()
        compose.onNodeWithTag("focus_item_discipline").performClick()
        compose.onNodeWithTag("onboarding_focus_next").performClick()
        compose.onNodeWithTag("onboarding_datetime_next").assertIsDisplayed().performClick()
        compose.onNodeWithTag("onboarding_bible_next").performClick()
        compose.onNodeWithTag("onboarding_security_next").performClick()
        compose.onNodeWithTag("onboarding_finish_button").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf(null, null, null, null), locations) }
    }

    @Test fun journalSearchAndContextFilterBothApply() {
        compose.setContent {
            SteadfastTheme {
                com.steadfast.app.ui.journey.JourneyScreen(
                    null, emptyList(), emptyList(),
                    listOf(
                        com.steadfast.app.data.model.JournalEntryEntity(id = 1, body = "Prayer for strength", context = "URGE"),
                        com.steadfast.app.data.model.JournalEntryEntity(id = 2, body = "Prayer of thanks", context = "GENERAL"),
                        com.steadfast.app.data.model.JournalEntryEntity(id = 3, body = "Quiet reflection", context = "URGE")
                    ), null, 0, { _, _ -> }, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithTag("journey_tab_Journal").performClick()
        compose.onNodeWithTag("journal_search_input").performTextInput("prayer")
        compose.onNodeWithTag("journal_filter_URGE").performClick()
        compose.onNodeWithTag("journal_entry_1").assertExists()
        compose.onNodeWithTag("journal_entry_2").assertDoesNotExist()
        compose.onNodeWithTag("journal_entry_3").assertDoesNotExist()
        compose.onNodeWithTag("journey_tab_Overview").performClick()
        compose.onNodeWithTag("journey_tab_Journal").performClick()
        compose.onNodeWithTag("journal_search_input").assertTextContains("prayer")
        compose.onNodeWithTag("journal_entry_1").assertExists()
        compose.onNodeWithTag("journal_entry_2").assertDoesNotExist()
        compose.onNodeWithTag("journal_entry_3").assertDoesNotExist()
    }

    @Test fun journalEditAndDeleteTargetsStay48dpWithLargeFonts() {
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.3f)
            ) {
                SteadfastTheme {
                    com.steadfast.app.ui.journey.JourneyScreen(
                        null, emptyList(), emptyList(),
                        listOf(com.steadfast.app.data.model.JournalEntryEntity(id = 1, body = "Synthetic note", context = "GENERAL")),
                        null, 0, { _, _ -> }, { _, _ -> }, { _, _, _ -> }, {}, {}, {}, {}
                    )
                }
            }
        }
        compose.onNodeWithTag("journey_tab_Journal").performClick()
        listOf("Edit", "Delete").forEach {
            compose.onNodeWithContentDescription(it).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test fun reducedMotionKeepsBreathingRingStillAndPhaseChangesContinue() {
        com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = true
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { SteadfastTheme { com.steadfast.app.ui.struggling.BreathingExerciseScreen({}) } }
            compose.mainClock.advanceTimeByFrame()
            val before = compose.onNodeWithTag("breathing_ring").fetchSemanticsNode().boundsInRoot
            compose.mainClock.advanceTimeBy(4_100)
            compose.onNodeWithText("Hold Gently").assertExists()
            val after = compose.onNodeWithTag("breathing_ring").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertEquals(before, after)
        } finally { com.steadfast.app.core.design.ReducedMotionPolicy.testReducedMotionOverride = null }
    }

}
