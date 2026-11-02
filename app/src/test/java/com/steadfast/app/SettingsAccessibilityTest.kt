package com.steadfast.app

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.ui.settings.SettingsScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun currentThemeIsExposedAsTheSelectedRadioOption() = withSettings { backupManager ->
        var cardSurface = Color.Unspecified
        compose.setContent {
            SteadfastTheme(ThemeSetting.LIGHT) {
                val surface = MaterialTheme.colorScheme.surface
                SideEffect { cardSurface = surface }
                SettingsScreen(
                    ThemeSetting.LIGHT, false, "gaming", focuses(), null, 2, null, null,
                    backupManager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                )
            }
        }

        val selectedTheme = compose.onNodeWithTag("settings_theme_LIGHT").assertIsSelected()
        assertEquals(Role.RadioButton, selectedTheme.fetchSemanticsNode().config[SemanticsProperties.Role])
        compose.onNodeWithTag("settings_theme_DARK").assertIsNotSelected()

        val textLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Light").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
        assertReadable("selected Light theme text", textLayouts.single().layoutInput.style.color, cardSurface)
    }

    @Test fun selectedRecoveryFocusIsA48DpRadioOption() = withSettings { backupManager ->
        var dialogSurface = Color.Unspecified
        compose.setContent {
            SteadfastTheme(ThemeSetting.LIGHT) {
                val surface = MaterialTheme.colorScheme.surfaceContainerHigh
                SideEffect { dialogSurface = surface }
                SettingsScreen(
                    ThemeSetting.LIGHT, false, "gaming", focuses(), null, 2, null, null,
                    backupManager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                )
            }
        }

        compose.onNodeWithText("Recovery Focus").performScrollTo().performClick()
        val focus = compose.onNodeWithTag("settings_focus_gaming").assertIsSelected().assertHeightIsAtLeast(48.dp)
        assertEquals(Role.RadioButton, focus.fetchSemanticsNode().config[SemanticsProperties.Role])
        compose.onNodeWithTag("settings_focus_discipline").assertIsNotSelected()
        val textLayouts = mutableListOf<TextLayoutResult>()
        compose.onNode(
            hasText("Gaming") and hasAnyAncestor(hasTestTag("settings_focus_gaming")),
            useUnmergedTree = true
        ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
        assertReadable("selected Gaming focus text", textLayouts.single().layoutInput.style.color, dialogSurface)
    }

    private fun assertReadable(label: String, foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + .05f) / (darker + .05f)
        assertEquals("$label foreground must be specified", true, foreground.isSpecified)
        assertEquals("$label background must be specified", true, background.isSpecified)
        org.junit.Assert.assertTrue("$label contrast was $ratio for foreground=$foreground background=$background", ratio >= 4.5f)
    }

    private fun focuses() = listOf(RecoveryFocus("gaming", "Gaming"), RecoveryFocus("discipline", "Discipline"))

    private inline fun withSettings(block: (BackupManager) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, database, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        try {
            block(manager)
        } finally {
            database.close()
        }
    }
}
