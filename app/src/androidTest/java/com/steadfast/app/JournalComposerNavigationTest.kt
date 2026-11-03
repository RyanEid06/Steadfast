package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steadfast.app.ui.journey.JourneyScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Initial deep links create a separate Android dialog window; exercise its real window lifecycle.
@RunWith(AndroidJUnit4::class)
class JournalComposerNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun urgeRouteOpensComposerWithUrgeContextAndSavesIt() {
        var savedContext: String? = null
        compose.setContent {
            SteadfastTheme {
                JourneyScreen(
                    null, emptyList(), emptyList(), emptyList(), null, 0,
                    { _, _ -> }, { _, context -> savedContext = context }, { _, _, _ -> }, {}, {}, {}, {},
                    initialJournalContext = "URGE"
                )
            }
        }
        compose.onNodeWithText("New Journal Entry").assertExists()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("Help me stand firm")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals("URGE", savedContext) }
        compose.onNodeWithTag("journey_add_journal_fab").performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("A fresh reflection")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals("GENERAL", savedContext) }
    }

    @Test fun journalComposerContextControlsChangeSavedContext() {
        var savedContext: String? = null
        compose.setContent {
            SteadfastTheme {
                JourneyScreen(
                    null, emptyList(), emptyList(), emptyList(), null, 0,
                    { _, _ -> }, { _, context -> savedContext = context }, { _, _, _ -> }, {}, {}, {}, {},
                    initialJournalContext = "URGE"
                )
            }
        }
        compose.onNodeWithTag("journal_context_RESTART").performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("Choosing a fresh start")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals("RESTART", savedContext) }
    }

    @Test fun editingANotePreservesItsOptionalNullContext() {
        var saved = false
        var savedContext: String? = "not saved"
        compose.setContent { SteadfastTheme {
            JourneyScreen(
                null, emptyList(), emptyList(),
                listOf(com.steadfast.app.data.model.JournalEntryEntity(id = 1, body = "Synthetic uncategorized note", context = null)),
                null, 0, { _, _ -> }, { _, _ -> }, { _, _, context -> saved = true; savedContext = context }, {}, {}, {}, {}
            )
        } }
        compose.onNodeWithTag("journey_tab_Journal").performClick()
        compose.onNodeWithContentDescription("Edit").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(saved); org.junit.Assert.assertNull(savedContext) }
    }
}
