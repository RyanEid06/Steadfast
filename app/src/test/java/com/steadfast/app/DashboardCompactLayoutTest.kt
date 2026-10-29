package com.steadfast.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.steadfast.app.data.repository.CuratedVerse
import com.steadfast.app.ui.dashboard.DashboardScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w900dp-h360dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardCompactLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wideShortDashboardKeepsLongScriptureHeaderAndPrimaryActionVisibleAtLargeFont() {
        val verse = "No temptation has taken you except what is common to man. God is faithful, who will not allow you to be tempted above what you are able, but will with the temptation also make the way of escape, that you may be able to endure it."
        var strugglingClicked = false

        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                SteadfastTheme {
                    DashboardScreen(
                        activeSession = null,
                        dailyEncouragement = CuratedVerse(
                            "1 Corinthians 10:13", "1 Corinthians", 10, "13", "temptation",
                            text = verse, translation = "WEB"
                        ),
                        onStrugglingClicked = { strugglingClicked = true },
                        onSettingsClicked = {}
                    )
                }
            }
        }

        compose.onNodeWithText("STEADFAST").assertIsDisplayed()
        compose.onNodeWithTag("dashboard_streak_circle").assertIsDisplayed().assertWidthIsAtLeast(190.dp)
        compose.onNodeWithTag("dashboard_scripture_card").assertIsDisplayed()
        compose.onNodeWithText("“$verse”").assertIsDisplayed()
        compose.onNodeWithTag("dashboard_struggling_button").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(strugglingClicked) }
    }
}
