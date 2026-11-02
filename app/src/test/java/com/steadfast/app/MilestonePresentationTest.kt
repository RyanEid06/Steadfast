package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.steadfast.app.data.model.MilestoneDefinition
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
class MilestonePresentationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun celebrationUsesAnActualModalDialogAndOneAcknowledgement() {
        var acknowledgements = 0
        compose.setContent { SteadfastTheme {
            MilestoneCelebrationOverlay(MilestoneDefinition("d1", "elapsed_days", 1, "1 Day")) { acknowledgements++ }
        } }
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithTag("celebration_continue_button").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, acknowledgements) }
    }
}
