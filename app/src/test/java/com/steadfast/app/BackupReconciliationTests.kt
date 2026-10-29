package com.steadfast.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.*
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.SteadfastDatabase
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupReconciliationTests {
    private lateinit var db: SteadfastDatabase
    private lateinit var manager: BackupManager
    private lateinit var preferences: SteadfastPreferences
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        preferences = SteadfastPreferences(context)
        manager = BackupManager(context, db, preferences) { javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 }, "AES") }
    }
    @Test fun exportsCurrentLogicalSchemaIndependentlyOfEnvelope() = runBlocking {
        val output = java.io.ByteArrayOutputStream()
        manager.exportBackup(output, "a sufficiently long passphrase")
        val result = manager.previewBackup(output.toByteArray().inputStream(), "a sufficiently long passphrase").getOrThrow()
        assertEquals(2, result.schemaVersion)
    }
    @After fun tearDown() { db.close() }
    private fun session(id: Long = 1) = StreakSessionPayload(id, "pornography", 1000, "UTC", 2000, 1000)
    private fun rejected(payload: BackupPayload) {
        try { manager.validatePayload(payload); fail("Invalid backup was accepted") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun rejectsDuplicateSessionIds() = rejected(BackupPayload(streakSessions = listOf(session(), session())))
    @Test fun rejectsSessionEndBeforeStart() = rejected(BackupPayload(streakSessions = listOf(session().copy(endEpochMillis = 999))))
    @Test fun rejectsOverlappingSessions() = rejected(BackupPayload(streakSessions = listOf(session(), session(2).copy(startEpochMillis = 1500))))
    @Test fun rejectsUnknownFocusAndInvalidSettings() {
        rejected(BackupPayload(settings = BackupSettingsPayload(recoveryFocusId = "missing")))
        rejected(BackupPayload(settings = BackupSettingsPayload(themeMode = "purple")))
        rejected(BackupPayload(settings = BackupSettingsPayload(readingGoalMinimum = 6)))
        rejected(BackupPayload(settings = BackupSettingsPayload(oldTestamentCurrent = ReadingLocationPayload(40, 1))))
        rejected(BackupPayload(settings = BackupSettingsPayload(newTestamentCurrent = ReadingLocationPayload(66, 23))))
    }
    @Test fun rejectsInvalidJournalFields() {
        val entry = JournalEntryPayload(1, 1000, 2000, "note", "GENERAL", null)
        rejected(BackupPayload(journalEntries = listOf(entry.copy(body = "  "))))
        rejected(BackupPayload(journalEntries = listOf(entry.copy(context = "UNKNOWN"))))
        rejected(BackupPayload(journalEntries = listOf(entry.copy(updatedAtEpochMillis = 999))))
        rejected(BackupPayload(journalEntries = listOf(entry, entry)))
    }
    @Test fun rejectsNonpositiveTimestampsAndIds() {
        rejected(BackupPayload(exportedAtEpochMillis = 0))
        rejected(BackupPayload(streakSessions = listOf(session(0))))
        rejected(BackupPayload(streakSessions = listOf(session().copy(createdAtEpochMillis = 0))))
    }
    @Test fun rejectsUnknownOrDuplicateShownMilestones() {
        rejected(BackupPayload(settings = BackupSettingsPayload(shownMilestones = "unknown")))
        rejected(BackupPayload(settings = BackupSettingsPayload(shownMilestones = "d1,d1")))
    }
    @Test fun rejectsTimestampsAfterExport() {
        rejected(BackupPayload(exportedAtEpochMillis = 2000, streakSessions = listOf(session().copy(createdAtEpochMillis = 2001))))
        rejected(BackupPayload(exportedAtEpochMillis = 2000, journalEntries = listOf(JournalEntryPayload(1, 1000, 2001, "note", null, null))))
    }
    @Test fun acceptsZeroLengthArchivedSessionAtActiveStartRegardlessOfListOrder() {
        val active = session(2).copy(startEpochMillis = 2000, endEpochMillis = null, createdAtEpochMillis = 2000)
        val archived = session(1).copy(startEpochMillis = 2000, endEpochMillis = 2000, createdAtEpochMillis = 2000)
        manager.validatePayload(BackupPayload(streakSessions = listOf(active, archived)))
    }
}
