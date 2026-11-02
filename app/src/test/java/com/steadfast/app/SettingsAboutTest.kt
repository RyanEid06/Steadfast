package com.steadfast.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.ui.settings.SettingsScreen
import com.steadfast.app.ui.theme.SteadfastTheme
import com.steadfast.app.ui.theme.ThemeSetting
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [NoopRobolectricPrefetch::class], instrumentedPackages = ["androidx.compose.foundation.lazy.layout"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsAboutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aboutDisplaysBuildVersionFromBuildConfig() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        try {
            compose.setContent {
                SteadfastTheme {
                    SettingsScreen(ThemeSetting.SYSTEM, false, "gaming", emptyList(), null, 2, null, null,
                        manager, {}, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("About Steadfast & Medical Safety"))
            compose.onNodeWithText("About Steadfast & Medical Safety").performClick()
            compose.onNodeWithText("Steadfast V${BuildConfig.VERSION_NAME}").assertExists()
        } finally {
            db.close()
        }
    }

    @Test fun fullAboutSafetyNoticeCanBeReachedByScrolling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        val manager = BackupManager(context, db, SteadfastPreferences(context)) { SecretKeySpec(ByteArray(32) { 5 }, "AES") }
        try {
            compose.setContent {
                SteadfastTheme {
                    SettingsScreen(ThemeSetting.SYSTEM, false, "gaming", emptyList(), null, 2, null, null,
                        manager, {}, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("About Steadfast & Medical Safety"))
            compose.onNodeWithText("About Steadfast & Medical Safety").performClick()
            compose.onNodeWithText(
                "Steadfast is a personal tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk."
            ).performScrollTo().assertIsDisplayed()
        } finally {
            db.close()
        }
    }
}
