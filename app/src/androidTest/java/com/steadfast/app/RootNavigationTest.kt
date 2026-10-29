package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steadfast.app.data.datastore.SteadfastPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RootNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun dashboardReturnsFromUrgeComposerAndOtherRootDestinations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = SteadfastPreferences(context)
        val original = runBlocking { preferences.snapshotUserSettings() }
        try {
            runBlocking { preferences.replaceUserSettings(original.copy(appLockEnabled = false, onboardingComplete = true)) }
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitDashboard()
                compose.onNodeWithTag("dashboard_struggling_button").performClick()
                compose.onNodeWithTag("struggling_write_note_button").performClick()
                compose.onNodeWithText("New Journal Entry").assertExists()
                compose.onNodeWithTag("journal_context_URGE").assertIsSelected()
                compose.onNodeWithText("Cancel").performClick()
                compose.onNodeWithTag("nav_item_dashboard").performClick()
                awaitDashboard()
                compose.onNodeWithTag("nav_item_bible").performClick()
                compose.onNodeWithTag("nav_item_dashboard").performClick()
                awaitDashboard()
            }
        } finally {
            runBlocking { preferences.replaceUserSettings(original) }
        }
    }

    private fun awaitDashboard() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("dashboard_struggling_button").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
