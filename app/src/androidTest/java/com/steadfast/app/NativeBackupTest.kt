package com.steadfast.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.datastore.UserPreferencesSnapshot
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NativeBackupTest {
    @Test fun deviceKeystoreRoundtripPreviewAndSqliteRollbackPreserveAllState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reconciliation-${UUID.randomUUID()}"
        val databaseName = "$name.db"
        val database = Room.databaseBuilder(context, SteadfastDatabase::class.java, databaseName).build()
        val job = SupervisorJob()
        val preferenceFile = File(context.cacheDir, "$name.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { preferenceFile }
        )
        val preferences = SteadfastPreferences(context, store)
        // Use the actual Android Keystore provider and on-disk Room/DataStore.
        val manager = BackupManager(context, database, preferences)
        val settings = UserPreferencesSnapshot(
            "DARK", "alcohol", 5, ReadingLocation(19, 150), ReadingLocation(66, 22), true, setOf("d1", "d7"), true
        )
        val session = StreakSessionEntity(7, "alcohol", 1000, "UTC", 2000, 1000)
        val entry = JournalEntryEntity(11, 1500, 1800, "Synthetic native recovery note", "URGE", 7)
        try {
            manager.recoverPendingRestore().getOrThrow()
            preferences.replaceUserSettings(settings)
            database.streakDao().insertSession(session)
            database.journalDao().insertEntry(entry)
            database.bibleDao().insertCompletion(BibleChapterCompletionEntity(1, 1))
            val output = ByteArrayOutputStream()
            val passphrase = "synthetic native backup passphrase"
            manager.exportBackup(output, passphrase)
            val bytes = output.toByteArray()
            assertEquals("STDF_V2", String(bytes.copyOfRange(0, 7), Charsets.UTF_8))
            database.withTransaction {
                database.journalDao().clearAll()
                database.bibleDao().clearAll()
                database.streakDao().clearAll()
            }
            preferences.replaceUserSettings(UserPreferencesSnapshot())
            val preview = manager.previewBackup(bytes.inputStream(), passphrase).getOrThrow()
            assertEquals(2, preview.schemaVersion)
            assertTrue(database.streakDao().getAllSessionsSync().isEmpty())
            assertEquals(UserPreferencesSnapshot(), preferences.snapshotUserSettings())
            manager.importBackup(bytes.inputStream(), passphrase).getOrThrow()
            assertEquals(listOf(session), database.streakDao().getAllSessionsSync())
            assertEquals(listOf(entry), database.journalDao().getAllEntriesSync())
            assertEquals(listOf(BibleChapterCompletionEntity(1, 1)), database.bibleDao().getAllCompletedSync())
            assertEquals(settings, preferences.snapshotUserSettings())
            assertFalse(BackupManager.hasPendingRestore(context))

            // Fail a later table insert after the session table was already replaced.
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_native_restore BEFORE INSERT ON journal_entries BEGIN SELECT RAISE(ABORT, 'synthetic rollback probe'); END"
            )
            assertTrue(manager.restorePayloadWithRollback(preview).isFailure)
            assertEquals(listOf(session), database.streakDao().getAllSessionsSync())
            assertEquals(listOf(entry), database.journalDao().getAllEntriesSync())
            assertEquals(listOf(BibleChapterCompletionEntity(1, 1)), database.bibleDao().getAllCompletedSync())
            assertEquals(settings, preferences.snapshotUserSettings())
            assertFalse(BackupManager.hasPendingRestore(context))
        } finally {
            job.cancelAndJoin()
            database.close()
            context.deleteDatabase(databaseName)
            preferenceFile.delete()
        }
    }
}
