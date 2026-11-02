package com.steadfast.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiStateRetentionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bibleFilterSurvivesSavedInstanceRecreation() {
        val restoration = StateRestorationTester(compose)
        val books = listOf(
            com.steadfast.app.data.model.BibleBook(1, "Genesis", "OT", 50, 1),
            com.steadfast.app.data.model.BibleBook(40, "Matthew", "NT", 28, 40)
        ).map { com.steadfast.app.data.repository.BookWithProgress(it, emptySet(), 0, 0f, false) }
        restoration.setContent { SteadfastTheme {
            com.steadfast.app.ui.bible.BibleScreen(
                com.steadfast.app.data.repository.BibleProgress(0, 1189, 0f, 0, 929, 0f, 0, 260, 0f, 0, 66),
                books, null, null, 2, {}, {}
            )
        } }
        compose.onNodeWithText("New Testament (27)").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("New Testament (27)").assertIsSelected()
    }

    @Test fun tutorialStepSurvivesSavedInstanceRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { SteadfastTheme { com.steadfast.app.ui.onboarding.TutorialReplayScreen({}) } }
        compose.onNodeWithTag("tutorial_next_button").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("tutorial_prev_button").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("tutorial_prev_button").assertExists()
    }

    @Test fun breathingResumesRemainingSecondsWithoutRestartingPhase() {
        lateinit var owner: TestOwner
        compose.runOnIdle { owner = TestOwner() }
        compose.mainClock.autoAdvance = false
        compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            SteadfastTheme { com.steadfast.app.ui.struggling.BreathingExerciseScreen({}) }
        } }
        compose.mainClock.advanceTimeBy(2100)
        compose.onNodeWithText("2").assertExists()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithText("2").assertExists()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(1100)
        compose.onNodeWithText("1").assertExists()
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
