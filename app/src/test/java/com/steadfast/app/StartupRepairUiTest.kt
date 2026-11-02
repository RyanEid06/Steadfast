package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StartupRepairUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun preservedInvalidHistoryOffersAnExplicitRecoveryAction() {
        var opened = false
        compose.setContent { SteadfastTheme {
            StartupRepairScreen("Recovery history has no single active session.", {}, onRestore = { opened = true })
        } }
        compose.onNodeWithText("Your records have been preserved.").assertExists()
        compose.onNodeWithText("Restore a backup").assertIsDisplayed().performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(opened) }
    }
}
