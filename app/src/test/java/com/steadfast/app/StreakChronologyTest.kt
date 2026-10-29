package com.steadfast.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.repository.JournalRepository
import com.steadfast.app.data.repository.SteadfastRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreakChronologyTest {
    private lateinit var db: SteadfastDatabase
    private lateinit var repository: SteadfastRepository
    private val now get() = System.currentTimeMillis()

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        repository = SteadfastRepository(db, JournalRepository(db.journalDao()), SteadfastPreferences(context))
    }

    @After fun close() = db.close()

    private suspend fun active(start: Long) = db.streakDao().insertSession(
        StreakSessionEntity(recoveryFocusId = "pornography", startEpochMillis = start, startZoneId = "UTC")
    )

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Impossible chronology must be rejected") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun restartBeforeActiveStartRejectsWithoutMutatingHistory() = runBlocking {
        val id = active(now - 10_000)
        val before = db.streakDao().getAllSessionsSync()
        rejected { repository.restartStreak(newStartEpochMillis = before.single().startEpochMillis - 1, optionalNote = "Must not save") }
        assertEquals(before, db.streakDao().getAllSessionsSync())
        assertEquals(id, db.streakDao().getActiveSessionSync()!!.id)
        assertTrue(db.journalDao().getAllEntriesSync().isEmpty())
    }

    @Test fun restartAtSameStartRejectsZeroLengthHistory() = runBlocking {
        val start = now - 10_000
        active(start)
        rejected { repository.restartStreak(newStartEpochMillis = start) }
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
    }

    @Test fun customPastRestartKeepsHistoryWithoutOverlap() = runBlocking {
        val start = now - 100_000
        val id = active(start)
        val restart = start + 20_000
        repository.restartStreak(newStartEpochMillis = restart, optionalNote = "Starting again")
        val sessions = db.streakDao().getAllSessionsSync()
        assertEquals(2, sessions.size)
        assertEquals(restart, sessions.single { it.id == id }.endEpochMillis)
        assertEquals(restart, sessions.single { it.endEpochMillis == null }.startEpochMillis)
        assertEquals(id, db.journalDao().getAllEntriesSync().single().streakSessionId)
    }

    @Test fun normalRestartCreatesOneNewActiveSession() = runBlocking {
        val id = active(now - 10_000)
        repository.restartStreak()
        val sessions = db.streakDao().getAllSessionsSync()
        assertEquals(1, sessions.count { it.endEpochMillis == null })
        assertEquals(sessions.single { it.id != id }.startEpochMillis, sessions.single { it.id == id }.endEpochMillis)
    }

    @Test fun futureRestartIsRejected() = runBlocking {
        active(now - 10_000)
        rejected { repository.restartStreak(newStartEpochMillis = now + 60_000) }
        assertEquals(1, db.streakDao().getAllSessionsSync().size)
    }

    @Test fun editingStartCannotOverlapArchivedHistoryOrBeFuture() = runBlocking {
        val boundary = now - 20_000
        db.streakDao().insertSession(StreakSessionEntity(recoveryFocusId = "pornography", startEpochMillis = boundary - 20_000, endEpochMillis = boundary, startZoneId = "UTC"))
        val id = active(boundary + 1_000)
        val before = db.streakDao().getAllSessionsSync()
        rejected { repository.updateSessionStart(id, boundary - 1) }
        rejected { repository.updateSessionStart(id, now + 60_000) }
        assertEquals(before, db.streakDao().getAllSessionsSync())
    }
}
