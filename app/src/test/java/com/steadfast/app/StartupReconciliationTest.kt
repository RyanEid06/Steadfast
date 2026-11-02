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
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.StartupState
import com.steadfast.app.data.repository.SteadfastRepository
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartupReconciliationTest {
    private val validFocusIds = setOf("gaming", "alcohol")
    private val books = listOf(BibleBook(1, "Genesis", "OT", 50, 1))
    private lateinit var db: SteadfastDatabase
    private lateinit var prefs: SteadfastPreferences
    private lateinit var store: InterceptingStore
    private lateinit var storeJob: CompletableJob
    private lateinit var file: File
    private lateinit var context: Context

    private fun repository() = SteadfastRepository(db, JournalRepository(db.journalDao()), prefs)

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        storeJob = SupervisorJob()
        file = File(context.filesDir, "startup-reconciliation.preferences_pb")
        file.delete()
        store = InterceptingStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(storeJob + Dispatchers.IO), produceFile = { file }
        ))
        prefs = SteadfastPreferences(context, store)
    }

    @After fun tearDown() = runBlocking {
        storeJob.cancelAndJoin()
        db.close()
        file.delete()
        Unit
    }

    @Test fun emptyInstallWithoutCompletionNeedsSetup() = runBlocking {
        assertEquals(StartupState.NeedsSetup, repository().reconcileStartup(validFocusIds))
    }

    @Test fun completionFlagWithoutAnyHistoryIsClearedWithoutLosingUserData() = runBlocking {
        val data = UserPreferencesSnapshot(
            onboardingComplete = true,
            oldTestamentCurrent = ReadingLocation(1, 4),
            shownMilestones = setOf("day-1", "day-7")
        )
        prefs.replaceUserSettings(data)

        assertEquals(StartupState.NeedsSetup, repository().reconcileStartup(validFocusIds))
        assertEquals(data.copy(onboardingComplete = false), prefs.snapshotUserSettings())
    }

    @Test fun incompleteSetupWithOneValidActiveSessionAlignsPreferencesAndResumes() = runBlocking {
        db.streakDao().insertSession(session(focus = "gaming"))
        prefs.replaceUserSettings(UserPreferencesSnapshot(
            recoveryFocusId = "alcohol", oldTestamentCurrent = ReadingLocation(1, 2),
            shownMilestones = setOf("day-7")
        ))

        assertEquals(StartupState.Ready(db.streakDao().getActiveSessionSync()!!), repository().reconcileStartup(validFocusIds))
        assertEquals("gaming", prefs.snapshotUserSettings().recoveryFocusId)
        assertTrue(prefs.snapshotUserSettings().onboardingComplete)
        assertEquals(ReadingLocation(1, 2), prefs.snapshotUserSettings().oldTestamentCurrent)
        assertEquals(setOf("day-7"), prefs.snapshotUserSettings().shownMilestones)
    }

    @Test fun incompleteFlagWithExistingHistoryIgnoresNewDraftAndPreservesAllRows() = runBlocking {
        val active = session(focus = "gaming")
        val id = db.streakDao().insertSession(active)
        val entry = JournalEntryEntity(body = "private note", streakSessionId = id, createdAtEpochMillis = 900)
        db.journalDao().insertEntry(entry)
        db.bibleDao().insertCompletion(BibleChapterCompletionEntity(1, 3))
        val installed = UserPreferencesSnapshot(
            themeMode = "DARK", recoveryFocusId = "alcohol", readingGoalMinimum = 4,
            newTestamentCurrent = ReadingLocation(1, 5), shownMilestones = setOf("day-30"), onboardingComplete = false
        )
        prefs.replaceUserSettings(installed)
        val incomingDraft = installed.copy(themeMode = "LIGHT", readingGoalMinimum = 1, recoveryFocusId = "gaming")

        val ready = repository().completeOnboarding(incomingDraft, 2_000, "UTC", validFocusIds, books)

        assertEquals(StartupState.Ready(active.copy(id = id)), ready)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertEquals(listOf("private note"), db.journalDao().getAllEntriesSync().map { it.body })
        assertEquals(listOf(BibleChapterCompletionEntity(1, 3)), db.bibleDao().getAllCompletedSync())
        assertEquals(installed.copy(onboardingComplete = true, recoveryFocusId = "gaming"), prefs.snapshotUserSettings())
    }

    @Test fun archivedOnlyHistoryRequiresRepairAndIsNotReactivated() = runBlocking {
        val archived = session(focus = "gaming", end = 1_500)
        val archivedId = db.streakDao().insertSession(archived)
        val storedArchived = archived.copy(id = archivedId)
        val installed = UserPreferencesSnapshot(recoveryFocusId = "alcohol", shownMilestones = setOf("day-7"))
        prefs.replaceUserSettings(installed)

        assertTrue(repository().reconcileStartup(validFocusIds) is StartupState.RepairRequired)
        assertEquals(listOf(storedArchived), db.streakDao().getAllSessionsSync())
        assertEquals(installed, prefs.snapshotUserSettings())
    }

    @Test fun zeroLengthArchivedBoundaryBeforeActiveSessionIsValid() = runBlocking {
        db.streakDao().insertSession(session(focus = "gaming", start = 1_500, end = 1_500))
        val active = session(focus = "gaming", start = 1_500)
        val activeId = db.streakDao().insertSession(active)

        assertEquals(StartupState.Ready(active.copy(id = activeId)),
            repository().reconcileStartup(validFocusIds))
    }

    @Test fun anyInvalidHistoryRequiresRepairWithoutChangingRowsOrPreferences() = runBlocking {
        val invalid = listOf(
            session(focus = "unknown"),
            session(zone = "Not/AZone"),
            session(start = 2_000, end = 1_500),
            session(start = 2_000, end = null).copy(id = 2) // Multiple active rows.
        )
        for (bad in invalid) {
            db.streakDao().clearAll()
            db.streakDao().insertSession(bad)
            if (bad.id == 2L) db.streakDao().insertSession(session(focus = "gaming"))
            val beforeRows = db.streakDao().getAllSessionsSync()
            val beforePrefs = prefs.snapshotUserSettings()

            assertTrue(repository().reconcileStartup(validFocusIds) is StartupState.RepairRequired)
            assertEquals(beforeRows, db.streakDao().getAllSessionsSync())
            assertEquals(beforePrefs, prefs.snapshotUserSettings())
        }
    }

    @Test fun repeatedAndConcurrentFreshCompletionCreatesExactlyOneSession() = runBlocking {
        prefs.replaceUserSettings(UserPreferencesSnapshot(
            oldTestamentCurrent = ReadingLocation(1, 2), shownMilestones = setOf("existing-day-7")
        ))
        val draft = UserPreferencesSnapshot(recoveryFocusId = "gaming")

        val results = (1..8).map {
            async(Dispatchers.Default) {
                repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books)
            }
        }.awaitAll()

        assertTrue(results.all { it is StartupState.Ready })
        val sessions = db.streakDao().getAllSessionsSync()
        assertEquals(1, sessions.size)
        assertEquals("gaming", sessions.single().recoveryFocusId)
        assertEquals(StartupState.Ready(sessions.single()), repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books))
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertEquals(ReadingLocation(1, 2), prefs.snapshotUserSettings().oldTestamentCurrent)
        assertEquals(setOf("existing-day-7"), prefs.snapshotUserSettings().shownMilestones)
    }

    @Test fun invalidFreshFocusDoesNotWriteSettingsOrInsertSession() = runBlocking {
        val before = UserPreferencesSnapshot(
            themeMode = "DARK", recoveryFocusId = "alcohol", oldTestamentCurrent = ReadingLocation(1, 6),
            shownMilestones = setOf("day-1")
        )
        prefs.replaceUserSettings(before)

        assertTrue(runCatching {
            repository().completeOnboarding(before.copy(recoveryFocusId = "missing"), 1_000, "UTC", validFocusIds, books)
        }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(db.streakDao().getAllSessionsSync().isEmpty())
        assertEquals(before, prefs.snapshotUserSettings())
    }

    @Test fun atomicPreferenceWriteFailureCanBeRetriedWithoutDuplicateRows() = runBlocking {
        val draft = UserPreferencesSnapshot(recoveryFocusId = "gaming", shownMilestones = setOf("day-7"))
        var failAfterCommit = true
        store.afterUpdate = {
            if (failAfterCommit) {
                failAfterCommit = false
                error("simulated process interruption after DataStore commit")
            }
        }

        assertTrue(runCatching {
            repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books)
        }.isFailure)
        assertTrue(db.streakDao().getAllSessionsSync().isEmpty())
        assertFalse(prefs.snapshotUserSettings().onboardingComplete)
        store.afterUpdate = {}

        assertTrue(repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books) is StartupState.Ready)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertTrue(prefs.snapshotUserSettings().onboardingComplete)
        assertEquals(setOf("day-7"), prefs.snapshotUserSettings().shownMilestones)
    }

    @Test fun failureBeforeStagingPreferencesLeavesSetupRetryable() = runBlocking {
        val before = prefs.snapshotUserSettings()
        store.beforeUpdate = { error("simulated failure before DataStore commit") }

        assertTrue(runCatching {
            repository().completeOnboarding(UserPreferencesSnapshot(recoveryFocusId = "gaming"), 1_000, "UTC", validFocusIds, books)
        }.isFailure)
        assertTrue(db.streakDao().getAllSessionsSync().isEmpty())
        assertEquals(before, prefs.snapshotUserSettings())
        store.beforeUpdate = {}

        assertTrue(repository().completeOnboarding(UserPreferencesSnapshot(recoveryFocusId = "gaming"), 1_000, "UTC", validFocusIds, books)
            is StartupState.Ready)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
    }

    @Test fun failureBeforeFinalCompletionWriteReconcilesExistingSessionOnRetry() = runBlocking {
        var update = 0
        store.beforeUpdate = {
            update++
            if (update == 2) error("simulated failure before final completion commit")
        }
        val draft = UserPreferencesSnapshot(recoveryFocusId = "gaming")

        assertTrue(runCatching { repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books) }.isFailure)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertFalse(prefs.snapshotUserSettings().onboardingComplete)
        store.beforeUpdate = {}

        assertTrue(repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books) is StartupState.Ready)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertTrue(prefs.snapshotUserSettings().onboardingComplete)
    }

    @Test fun failureAfterFinalCompletionCommitReturnsReadyOnRetryWithoutDuplicateSession() = runBlocking {
        var update = 0
        store.afterUpdate = {
            update++
            if (update == 2) error("simulated failure after final completion commit")
        }
        val draft = UserPreferencesSnapshot(recoveryFocusId = "gaming")

        assertTrue(runCatching { repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books) }.isFailure)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
        assertTrue(prefs.snapshotUserSettings().onboardingComplete)
        store.afterUpdate = {}

        assertTrue(repository().completeOnboarding(draft, 1_000, "UTC", validFocusIds, books) is StartupState.Ready)
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
    }

    private fun session(
        focus: String = "gaming",
        start: Long = 1_000,
        zone: String = "UTC",
        end: Long? = null
    ) = StreakSessionEntity(recoveryFocusId = focus, startEpochMillis = start, startZoneId = zone, endEpochMillis = end)

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
