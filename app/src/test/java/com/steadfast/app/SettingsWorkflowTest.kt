package com.steadfast.app

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.model.RecoveryFocus
import com.steadfast.app.ui.settings.SettingsScreen
import com.steadfast.app.ui.settings.backupPreviewFocusLabel
import com.steadfast.app.ui.settings.SettingsWorkflowModel
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [NoopRobolectricPrefetch::class],
    instrumentedPackages = ["androidx.compose.foundation.lazy.layout"]
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsWorkflowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsCanSetAndClearNamedBookmarksWithoutChangingGoal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        val books = listOf(BibleBook(1, "Genesis", "OT", 50, 1), BibleBook(40, "Matthew", "NT", 28, 40))
        val saved = mutableListOf<Pair<Int, Int>>()
        var cleared = 0

        try {
            compose.setContent {
                SteadfastTheme {
                    SettingsScreen(
                        currentTheme = ThemeSetting.SYSTEM,
                        isAppLockEnabled = false,
                        currentFocusId = "gaming",
                        focuses = listOf(RecoveryFocus("gaming", "Gaming")),
                        activeSession = null,
                        readingGoal = 3,
                        otReading = null,
                        ntReading = ReadingLocation(40, 3),
                        backupManager = manager,
                        onThemeChanged = {}, onAppLockChanged = {}, onFocusChanged = {}, onStreakStartChanged = {},
                        onReadingGoalChanged = {}, onClearOtReading = {}, onClearNtReading = { cleared++ },
                        onReplayTutorial = {}, onBackClicked = {},
                        bibleBooks = books,
                        onSetOtReading = { book, chapter -> saved += book to chapter },
                        onSetNtReading = { book, chapter -> saved += book to chapter }
                    )
                }
            }

            compose.onNode(hasScrollAction()).performScrollToNode(hasText("3+ chapters"))
            compose.onNodeWithText("3+ chapters").assertExists()
            compose.onNodeWithTag("settings_set_ot_bookmark").performScrollTo().performClick()
            compose.onNodeWithText("Genesis").assertExists()
            compose.onNodeWithText("Chapter 1 of 50").assertExists()
            compose.onNodeWithText("+", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Chapter 2 of 50").assertExists()
            compose.onNodeWithText("Save bookmark").performClick()
            compose.runOnIdle { assertEquals(listOf(1 to 2), saved) }
            compose.onNodeWithText("Choose a book and chapter").assertExists()
            assertEquals("Not specified; legacy default: Pornography", backupPreviewFocusLabel(null, listOf(RecoveryFocus("pornography", "Pornography"))))
            assertEquals("Unknown recovery focus", backupPreviewFocusLabel("unknown", emptyList()))

            compose.onNodeWithTag("settings_set_nt_bookmark").performScrollTo().performClick()
            compose.onNodeWithText("Clear bookmark").performClick()
            compose.runOnIdle { assertEquals(1, cleared) }
        } finally {
            db.close()
        }
    }

    @Test fun backupWorkflowRetainsPrivateDraftInViewModelStoreAndClearsItOnCancel() {
        val owner = TestViewModelOwner()
        val model = ViewModelProvider(owner)[SettingsWorkflowModel::class.java]
        val uri = Uri.parse("content://test/private-backup")

        assertTrue(model.beginExportPicker())
        model.onExportPickerResult(uri)
        model.updateExportPassphrase("synthetic-private-passphrase")
        model.updateExportConfirmation("synthetic-private-passphrase")

        val retained = ViewModelProvider(owner)[SettingsWorkflowModel::class.java]
        assertSame(model, retained)
        assertEquals(uri, retained.backupState.value.uri)
        assertEquals("synthetic-private-passphrase", retained.backupState.value.passphrase)
        assertFalse(retained.beginExportPicker())

        retained.cancelBackupWorkflow()
        assertEquals(null, retained.backupState.value.uri)
        assertEquals("", retained.backupState.value.passphrase)
        owner.viewModelStore.clear()
    }

    @Test fun passphraseMismatchHasVisibleSafeSupportingText() {
        val owner = TestViewModelOwner()
        val model = ViewModelProvider(owner)[SettingsWorkflowModel::class.java]
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        model.beginExportPicker()
        model.onExportPickerResult(Uri.parse("content://test/private-backup"))
        model.updateExportPassphrase("synthetic-passphrase-1")
        model.updateExportConfirmation("synthetic-passphrase-2")

        try {
            compose.setContent {
                SteadfastTheme {
                    androidx.compose.runtime.CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                        SettingsScreen(
                            ThemeSetting.SYSTEM, false, "gaming", emptyList(), null, 2, null, null,
                            manager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                        )
                    }
                }
            }
            compose.onNodeWithTag("settings_export_confirm").assertIsNotEnabled()
            compose.onNodeWithText("Passphrases do not match.").assertExists()
            compose.onNodeWithText("synthetic-passphrase-1").assertDoesNotExist()
        } finally {
            owner.viewModelStore.clear()
            db.close()
        }
    }

    @Test fun aboutSafetyNoticeCanBeScrolledFullyIntoViewAndShowsBuildVersion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        try {
            compose.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    SteadfastTheme {
                        SettingsScreen(
                            ThemeSetting.SYSTEM, false, "gaming", emptyList(), null, 2, null, null,
                            manager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                        )
                    }
                }
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("About Steadfast & Medical Safety"))
            compose.onNodeWithTag("settings_about_row").performClick()
            compose.onNodeWithText("Steadfast V${com.steadfast.app.BuildConfig.VERSION_NAME}").assertExists()
            compose.onNodeWithText("Steadfast is a personal tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk.")
                .performScrollTo().assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h480dp")
    fun exportFormKeepsBothPassphraseFieldsAndSubmitReachableAtTwoTimesFontScale() {
        RuntimeEnvironment.setFontScale(2f)
        val owner = TestViewModelOwner()
        val workflow = ViewModelProvider(owner)[SettingsWorkflowModel::class.java]
        workflow.beginExportPicker()
        workflow.onExportPickerResult(Uri.parse("content://test/private-backup"))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }

        try {
            compose.setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner
                ) {
                    SteadfastTheme {
                        SettingsScreen(
                            ThemeSetting.SYSTEM, false, "gaming", emptyList(), null, 2, null, null,
                            manager, {}, {}, {}, {}, {}, {}, {}, {}, {}
                        )
                    }
                }
            }

            val root = compose.onNode(isDialog()).getUnclippedBoundsInRoot()
            val titleLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("settings_export_title").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
            assertTrue("Export dialog title has no text layout", titleLayouts.isNotEmpty())
            assertEquals(2f, titleLayouts.single().layoutInput.density.fontScale, 0.01f)
            val passphraseField = compose.onNodeWithTag("settings_export_passphrase")
            val confirmationField = compose.onNodeWithTag("settings_export_confirmation")
            listOf(passphraseField, confirmationField).forEachIndexed { index, field ->
                field.performScrollTo()
                val bounds = field.getUnclippedBoundsInRoot()
                assertTrue("Export password field $index is outside the dialog window: $bounds / $root",
                    bounds.left >= root.left && bounds.top >= root.top && bounds.right <= root.right && bounds.bottom <= root.bottom && bounds.right > bounds.left && bounds.bottom > bounds.top)
                field.assertIsDisplayed()
            }

            val secret = "synthetic-layout-passphrase"
            passphraseField.performTextInput(secret)
            confirmationField.performTextInput(secret)
            compose.onNodeWithTag("settings_export_confirm").assertIsEnabled()
            val submitBounds = compose.onNodeWithTag("settings_export_confirm").getUnclippedBoundsInRoot()
            assertTrue("Export action is not fully reachable: $submitBounds / $root",
                submitBounds.left >= root.left && submitBounds.top >= root.top && submitBounds.right <= root.right && submitBounds.bottom <= root.bottom && submitBounds.right > submitBounds.left && submitBounds.bottom > submitBounds.top)
            compose.onNodeWithTag("settings_export_confirm").performClick()
            assertTrue("Export click did not start the workflow",
                workflow.backupState.value.stage != com.steadfast.app.ui.settings.SettingsBackupStage.EXPORT_FORM)
        } finally {
            owner.viewModelStore.clear()
            db.close()
        }
    }

    private class TestViewModelOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
}
