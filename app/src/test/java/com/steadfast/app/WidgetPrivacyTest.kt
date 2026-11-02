package com.steadfast.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.seed.AssetSeedLoader
import com.steadfast.app.widget.loadWidgetContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPrivacyTest {
    @Test fun everyFocusIsNeutralWithOrWithoutAppLock() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SteadfastDatabase.getInstance(context)
        val prefs = SteadfastPreferences(context)
        val original = prefs.snapshotUserSettings()
        val history = db.streakDao().getAllSessionsSync()
        try {
            for (locked in listOf(false, true)) {
                prefs.setAppLockEnabled(locked)
                for (focus in AssetSeedLoader(context).loadRecoveryFocuses()) {
                    db.streakDao().clearAll()
                    db.streakDao().insertSession(StreakSessionEntity(
                        recoveryFocusId = focus.id, startEpochMillis = System.currentTimeMillis() - 60_000,
                        startZoneId = "UTC"
                    ))
                    val content = loadWidgetContent(context)
                    assertEquals("Widget must never disclose recovery focus ${focus.id}", "A stronger you. A closer walk.", content.focus)
                    assertEquals("0 days", content.streak)
                }
            }
        } finally {
            db.streakDao().clearAll()
            history.forEach { db.streakDao().insertSession(it) }
            prefs.replaceUserSettings(original)
        }
    }
}
