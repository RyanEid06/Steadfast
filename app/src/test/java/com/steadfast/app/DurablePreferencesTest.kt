package com.steadfast.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.datastore.UserPreferencesSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DurablePreferencesTest {
    @Test fun newChoicesAndDeckSurviveRealDataStoreReopenWithoutChangingV1Settings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "v11-reopen.preferences_pb").also { it.delete() }
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        var actual = store()
        var prefs = SteadfastPreferences(context, actual)
        val legacy = UserPreferencesSnapshot(themeMode = "DARK", recoveryFocusId = "gaming", appLockEnabled = true, onboardingComplete = true)
        try {
            prefs.replaceUserSettings(legacy)
            actual.edit { it[stringPreferencesKey("future_unknown_setting")] = "preserve me" }
            assertEquals(0, prefs.bibleTestamentFilter.first())
            assertEquals(0, prefs.journeyTab.first())
            assertEquals("ALL", prefs.journalFilter.first())
            prefs.setBibleTestamentFilter(2)
            prefs.setJourneyTab(1)
            prefs.setJournalFilter("URGE")
            val ids = (1..500).map { "v$it" }
            val firstHalf = coroutineScope { (1..250).map { async { prefs.nextVerseId(ids, setOf("v1")) } }.awaitAll() }
            assertEquals(250, firstHalf.toSet().size)
            job.cancelAndJoin()
            job = SupervisorJob()
            actual = store()
            prefs = SteadfastPreferences(context, actual)
            assertEquals(2, prefs.bibleTestamentFilter.first())
            assertEquals(1, prefs.journeyTab.first())
            assertEquals("URGE", prefs.journalFilter.first())
            assertEquals(legacy.copy(bibleTestamentFilter = 2, journeyTab = 1, journalFilter = "URGE"), prefs.snapshotUserSettings())
            assertEquals("preserve me", actual.data.first()[stringPreferencesKey("future_unknown_setting")])
            val secondHalf = (1..250).map { prefs.nextVerseId(ids, setOf("v1")) }
            assertEquals(ids.toSet(), (firstHalf + secondHalf).toSet())
            assertEquals(500, (firstHalf + secondHalf).distinct().size)
            assertNotEquals(secondHalf.last(), prefs.nextVerseId(ids, emptySet()))
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
