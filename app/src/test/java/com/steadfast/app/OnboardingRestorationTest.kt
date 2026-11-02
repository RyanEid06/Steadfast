package com.steadfast.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.datastore.UserPreferencesSnapshot
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.SteadfastRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingRestorationTest {
    private lateinit var db: SteadfastDatabase
    private lateinit var prefs: SteadfastPreferences
    private lateinit var store: InterceptingStore
    private lateinit var storeJob: CompletableJob
    private lateinit var file: File
    private lateinit var context: Context
    private fun repository() = SteadfastRepository(db, JournalRepository(db.journalDao()), prefs)

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        storeJob = SupervisorJob()
        file = File(context.filesDir, "onboarding-restoration.preferences_pb")
        file.delete()
        store = InterceptingStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(storeJob + Dispatchers.IO), produceFile = { file }
        ))
        prefs = SteadfastPreferences(context, store)
    }

    @After fun close() = runBlocking {
        storeJob.cancelAndJoin()
        db.close()
        file.delete()
        Unit
    }

    @Test fun guardedInitialInsertionDoesNotResetPortablePreferences() = runBlocking {
        val before = UserPreferencesSnapshot(
            recoveryFocusId = "gaming", oldTestamentCurrent = ReadingLocation(1, 3),
            shownMilestones = setOf("d1", "d7")
        )
        prefs.replaceUserSettings(before)
        repository().startInitialSession("alcohol", 1_000, "UTC")
        assertEquals("Initial insertion owns Room only; completion owns atomic settings", before, prefs.snapshotUserSettings())
        assertEquals("alcohol", db.streakDao().getAllSessionsSync().single().recoveryFocusId)
    }

    private class InterceptingStore(private val actual: DataStore<Preferences>) : DataStore<Preferences> {
        var beforeUpdate: suspend () -> Unit = {}
        var afterUpdate: suspend () -> Unit = {}
        override val data: Flow<Preferences> get() = actual.data
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            beforeUpdate()
            val result = actual.updateData(transform)
            afterUpdate()
            return result
        }
    }
}
