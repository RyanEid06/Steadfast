package com.steadfast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.steadfast.app.ui.bible.BookDetailScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BiblePresentationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unknownBookAlwaysOffersAWorkingBackAction() {
        var wentBack = false
        compose.setContent { SteadfastTheme {
            BookDetailScreen(null, null, {}, {}, { wentBack = true })
        } }
        compose.onNodeWithTag("book_detail_back_button").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }

    @Test fun readingTargetIsDisplayedAsAnIntention() {
        compose.setContent { SteadfastTheme {
            com.steadfast.app.ui.bible.BibleScreen(
                com.steadfast.app.data.repository.BibleProgress(0, 1189, 0f, 0, 929, 0f, 0, 260, 0f, 0, 66),
                emptyList(), null, null, 3, {}, {}
            )
        } }
        compose.onNodeWithText("3+ chapters", substring = true).assertExists()
    }

    @Test fun bookHasAVisibleReadingBookmarkAction() {
        val book = com.steadfast.app.data.model.BibleBook(1, "Genesis", "OT", 50, 1)
        val progress = com.steadfast.app.data.repository.BookWithProgress(book, emptySet(), 0, 0f, false)
        compose.setContent { SteadfastTheme { BookDetailScreen(progress, null, {}, {}, {}) } }
        compose.onNodeWithTag("book_set_reading").assertIsDisplayed().performClick()
        compose.onNode(hasText("Set current reading") and hasAnyAncestor(isDialog())).assertExists()
    }
}
