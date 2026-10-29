package com.steadfast.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steadfast.app.core.security.BiometricLockManager
import com.steadfast.app.data.datastore.SteadfastPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockUnavailableTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun restoredLockCannotExposeContentWithoutAuthenticationAfterRetryOrRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeFalse("Requires an emulator without enrolled device authentication", BiometricLockManager.canAuthenticate(context))
        val preferences = SteadfastPreferences(context)
        val original = runBlocking { preferences.snapshotUserSettings() }
        try {
            // A portable restore may enable lock on a destination without credentials.
            runBlocking { preferences.replaceUserSettings(original.copy(appLockEnabled = true, onboardingComplete = true)) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                awaitLock()
                repeat(2) { compose.onNodeWithTag("app_locked_unlock_button").performClick() }
                compose.onNodeWithText(BiometricLockManager.ENROLLMENT_GUIDANCE).assertExists()
                compose.onNodeWithTag("dashboard_struggling_button").assertDoesNotExist()
                scenario.recreate()
                awaitLock()
                compose.onNodeWithTag("dashboard_struggling_button").assertDoesNotExist()
            }
        } finally {
            runBlocking { preferences.replaceUserSettings(original) }
        }
    }

    private fun awaitLock() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Steadfast is Locked").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
