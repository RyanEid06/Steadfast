package com.steadfast.app

import androidx.compose.ui.test.*

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.steadfast.app.ui.journey.JourneyScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JournalMutationLifecycleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun submittedJournalDraftSurvivesScreenRecreationAndWritesOnce() {
        val owner = TestOwner()
        val started = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val canceledWrites = AtomicInteger()
        val persistedWrites = AtomicInteger()
        var mounted by mutableStateOf(true)

        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                if (mounted) {
                    SteadfastTheme {
                        JourneyScreen(
                            activeSession = null,
                            allSessions = emptyList(),
                            archivedSessions = emptyList(),
                            journalEntries = emptyList(),
                            nextMilestone = null,
                            longestStreakDays = 0L,
                            onStartAgainConfirmed = { _, _ -> },
                            onAddJournalEntry = { _, _ -> },
                            onUpdateJournalEntry = { _, _, _ -> },
                            onDeleteJournalEntry = {},
                            onNavigateToMilestones = {},
                            onNavigateToMotivation = {},
                            onSettingsClicked = {},
                            onSaveJournalEntryValidated = { _, _, _ ->
                                started.complete(Unit)
                                try {
                                    finishWrite.await()
                                    persistedWrites.incrementAndGet()
                                    Result.success(Unit)
                                } catch (cancelled: CancellationException) {
                                    canceledWrites.incrementAndGet()
                                    throw cancelled
                                }
                            }
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("journey_tab_Journal").performClick()
        compose.onNodeWithTag("journey_add_journal_fab").performClick()
        compose.onNodeWithTag("journal_entry_body").performTextInput("A note submitted before rotation")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(timeoutMillis = 3_000) { started.isCompleted }

        // A submitted draft stays visible and owned by its ViewModel while its write is pending.
        compose.onNodeWithText("New Journal Entry").assertExists()
        compose.onNodeWithTag("journal_entry_body").assertIsNotEnabled()

        compose.runOnIdle { mounted = false }
        compose.waitForIdle()
        compose.runOnIdle { mounted = true }
        compose.waitForIdle()
        compose.onNodeWithText("New Journal Entry").assertExists()
        compose.onNodeWithTag("journal_entry_body").assertTextContains("A note submitted before rotation")

        finishWrite.complete(Unit)
        compose.waitUntil(timeoutMillis = 3_000) { persistedWrites.get() == 1 }
        compose.onNodeWithText("New Journal Entry").assertDoesNotExist()
        assertEquals(0, canceledWrites.get())
        assertEquals(1, persistedWrites.get())
        owner.store.clear()
    }

    private class TestOwner : ViewModelStoreOwner {
        val store = ViewModelStore()
        override val viewModelStore: ViewModelStore get() = store
    }
}
