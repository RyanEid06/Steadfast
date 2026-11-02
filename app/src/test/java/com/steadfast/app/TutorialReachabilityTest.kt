package com.steadfast.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h568dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialReachabilityTest {
    @get:Rule val compose = createComposeRule()
    @Test fun primaryActionIsFullyWithinTheUsableWindowAtTwoTimesFont() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(320.dp, 480.dp).clipToBounds().testTag("usable_window")) {
                    SteadfastTheme { com.steadfast.app.ui.onboarding.TutorialReplayScreen({}) }
                }
            }
        }
        val root = compose.onNodeWithTag("usable_window").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithTag("tutorial_next_button").getUnclippedBoundsInRoot()
        println("Tutorial usable=$root; next=$button")
        assertTrue("Tutorial primary action $button outside usable $root", button.left >= root.left && button.right <= root.right && button.top >= root.top && button.bottom <= root.bottom)
        assertTrue("Tutorial primary action lost its touch height", button.bottom - button.top >= 48.dp)
        compose.onNodeWithTag("tutorial_next_button").performClick()
        compose.onNodeWithText("Scripture Anchors").assertExists()
    }
}
