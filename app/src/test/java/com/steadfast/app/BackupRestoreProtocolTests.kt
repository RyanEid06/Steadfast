package com.steadfast.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.steadfast.app.core.security.*
import com.steadfast.app.data.datastore.*
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreProtocolTests {
    private lateinit var context: Context
    private lateinit var db: SteadfastDatabase
    private lateinit var prefs: SteadfastPreferences
    private lateinit var store: InterceptingStore
    private lateinit var storeJob: CompletableJob
    private lateinit var manager: BackupManager
    private val recoveryKey = SecretKeySpec(ByteArray(32) { 23 }, "AES")
    private val passphrase = "my portable backup passphrase"
    private val settings = UserPreferencesSnapshot(
        "DARK", "alcohol", 5, ReadingLocation(19, 150), ReadingLocation(66, 22), true, setOf("d1", "d7"), false
    )
    private val originalSession = StreakSessionEntity(7, "alcohol", 1000, "UTC", 2000, 1000)
    private val originalEntry = JournalEntryEntity(11, 1500, 1800, "Private recovery reflection", "URGE", 7)
    private val payload = BackupPayload(
        settings = BackupSettingsPayload("LIGHT", "gaming", 4, ReadingLocationPayload(1, 50), ReadingLocationPayload(40, 28), false, "d3", true),
        streakSessions = listOf(StreakSessionPayload(30, "gaming", 3000, "UTC", null, 3000)),
        journalEntries = listOf(JournalEntryPayload(31, 3500, 3600, "Replacement reflection", "GENERAL", 30)),
        bibleCompletedChapters = listOf(BibleCompletionPayload(66, 22))
    )
    private val journalFile get() = File(context.noBackupFilesDir, "restore-recovery.enc")
    private fun newManager() = BackupManager(context, db, prefs) { recoveryKey }

    @Before fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java).allowMainThreadQueries().build()
        storeJob = SupervisorJob()
        val preferencesFile = File(context.filesDir, "backup-protocol.preferences_pb")
        preferencesFile.delete()
        val actual = PreferenceDataStoreFactory.create(scope = CoroutineScope(storeJob + Dispatchers.IO), produceFile = { preferencesFile })
        store = InterceptingStore(actual)
        prefs = SteadfastPreferences(context, store)
        manager = newManager()
        manager.recoverPendingRestore().getOrThrow()
        prefs.replaceUserSettings(settings)
        db.streakDao().insertSession(originalSession)
        db.journalDao().insertEntry(originalEntry)
        db.bibleDao().insertCompletion(BibleChapterCompletionEntity(1, 1))
    }

    @After fun tearDown() = runBlocking {
        storeJob.cancelAndJoin()
        db.close()
        journalFile.delete()
        File(journalFile.path + ".new").delete()
        File(journalFile.path + ".bak").delete()
        File(context.filesDir, "backup-protocol.preferences_pb").delete()
        Unit
    }

    @Test fun currentRoundtripRestoresAllFieldsAndPreviewIsReadOnly() = runBlocking {
        val output = ByteArrayOutputStream()
        manager.exportBackup(output, passphrase)
        val bytes = output.toByteArray()
        assertEquals("STDF_V2", String(bytes.copyOfRange(0, 7)))
        prefs.replaceUserSettings(UserPreferencesSnapshot())
        db.streakDao().clearAll()
        db.journalDao().clearAll()
        db.bibleDao().clearAll()
        val preview = manager.previewBackup(bytes.inputStream(), passphrase).getOrThrow()
        assertEquals(2, preview.schemaVersion)
        assertEquals(false, preview.settings.onboardingComplete)
        assertTrue(db.streakDao().getAllSessionsSync().isEmpty())
        assertEquals(UserPreferencesSnapshot(), prefs.snapshotUserSettings())
        manager.importBackup(bytes.inputStream(), passphrase).getOrThrow()
        assertEquals(listOf(originalSession), db.streakDao().getAllSessionsSync())
        assertEquals(listOf(originalEntry), db.journalDao().getAllEntriesSync())
        assertEquals(listOf(BibleChapterCompletionEntity(1, 1)), db.bibleDao().getAllCompletedSync())
        assertEquals(settings, prefs.snapshotUserSettings())
        assertEquals(RestoreState.READY, manager.restoreState.value)
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun legacyEnvelopeShortPasswordRestoresLegacyDefaults() = runBlocking {
        val legacy = payload.copy(schemaVersion = 1, settings = BackupSettingsPayload(recoveryFocusId = "gaming"))
        val bytes = encryptPortable(legacy, "x", "STDF_V1", 65536)
        assertTrue(manager.previewBackup(bytes.inputStream(), "x").isSuccess)
        manager.importBackup(bytes.inputStream(), "x").getOrThrow()
        assertEquals(UserPreferencesSnapshot(recoveryFocusId = "gaming", onboardingComplete = true), prefs.snapshotUserSettings())
        assertEquals(30, db.streakDao().getActiveSessionSync()?.id?.toInt())
    }

    @Test fun datastoreFailureRollsBackAllRoomTablesAndAllPreferences() = runBlocking {
        var failOnce = true
        store.beforeUpdate = { if (failOnce) { failOnce = false; throw IOException("injected preference write failure") } }
        assertTrue(manager.restorePayloadWithRollback(payload).isFailure)
        assertOriginalState()
        assertEquals(RestoreState.READY, manager.restoreState.value)
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun roomFailureRollsBackEarlierTableReplacement() = runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_backup_journal BEFORE INSERT ON journal_entries WHEN NEW.id=31 BEGIN SELECT RAISE(ABORT, 'injected journal failure'); END")
        assertTrue(manager.restorePayloadWithRollback(payload).isFailure)
        assertOriginalState()
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun rollbackFailureRetainsEncryptedJournalAndBlocksUntilRetry() = runBlocking {
        store.beforeUpdate = { throw IOException("injected persistent preference failure") }
        val error = manager.restorePayloadWithRollback(payload).exceptionOrNull()
        assertNotNull(error)
        assertEquals(RestoreState.RECOVERY_REQUIRED, manager.restoreState.value)
        assertTrue(BackupManager.hasPendingRestore(context))
        assertEquals(listOf(originalSession), db.streakDao().getAllSessionsSync())
        assertFalse(String(journalFile.readBytes()).contains("Private recovery reflection"))
        assertTrue(error!!.cause!!.suppressed.isNotEmpty())
        store.beforeUpdate = {}
        val restarted = newManager()
        restarted.recoverPendingRestore().getOrThrow()
        assertOriginalState()
        assertEquals(RestoreState.READY, restarted.restoreState.value)
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun startupRecoversPreferenceWriteInterruptedBeforeRoomCommit() = runBlocking {
        store.afterUpdate = { throw SimulatedProcessDeath() }
        try { manager.restorePayloadWithRollback(payload); fail("Crash should escape without cleanup") }
        catch (_: SimulatedProcessDeath) { }
        store.afterUpdate = {}
        assertTrue(BackupManager.hasPendingRestore(context))
        assertEquals("LIGHT", prefs.snapshotUserSettings().themeMode)
        assertEquals(listOf(originalSession), db.streakDao().getAllSessionsSync())
        val restarted = newManager()
        restarted.recoverPendingRestore().getOrThrow()
        assertOriginalState()
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun startupRollsForwardIfRoomCommittedBeforeJournalCleanup() = runBlocking {
        var stagedJournal: ByteArray? = null
        store.beforeUpdate = { stagedJournal = journalFile.readBytes() }
        manager.restorePayloadWithRollback(payload).getOrThrow()
        store.beforeUpdate = {}
        journalFile.writeBytes(requireNotNull(stagedJournal))
        prefs.replaceUserSettings(settings)
        val restarted = newManager()
        restarted.recoverPendingRestore().getOrThrow()
        assertEquals("LIGHT", prefs.snapshotUserSettings().themeMode)
        assertEquals(setOf("d3"), prefs.snapshotUserSettings().shownMilestones)
        assertEquals(30L, db.streakDao().getActiveSessionSync()?.id)
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun cancellationAtPreferenceBoundaryRestoresOldStateBeforeRethrow() = runBlocking {
        var cancelOnce = true
        store.afterUpdate = { if (cancelOnce) { cancelOnce = false; throw CancellationException("injected cancellation") } }
        try { manager.restorePayloadWithRollback(payload); fail("Cancellation must propagate") }
        catch (_: CancellationException) { }
        assertOriginalState()
        assertEquals(RestoreState.READY, manager.restoreState.value)
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun invalidDecryptedPayloadIsRejectedBeforeAnyMutation() = runBlocking {
        val bytes = encryptPortable(payload.copy(streakSessions = payload.streakSessions + payload.streakSessions), passphrase, "STDF_V2", 210000)
        assertTrue(manager.importBackup(bytes.inputStream(), passphrase).isFailure)
        assertOriginalState()
        assertFalse(BackupManager.hasPendingRestore(context))
    }

    @Test fun missingRequiredPayloadFieldsCannotBecomeDestructiveEmptyRestore() = runBlocking {
        val bytes = encryptJson("{}", passphrase, "STDF_V2", 210000)
        assertTrue(manager.importBackup(bytes.inputStream(), passphrase).isFailure)
        assertOriginalState()
    }

    @Test fun nullStreamsAndWeakNewExportFailTruthfully() = runBlocking {
        try { manager.exportBackup(null, passphrase); fail("Null output must fail") }
        catch (_: IllegalArgumentException) { }
        var closed = false
        val output = object : ByteArrayOutputStream() {
            override fun close() { closed = true; super.close() }
        }
        try { manager.exportBackup(output, "short"); fail("New export requires a strong passphrase") }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, output.size())
        assertTrue(closed)
        assertTrue(manager.importBackup(null, passphrase).isFailure)
        assertOriginalState()
    }

    @Test fun failedOutputWriteClosesTheStreamAndDoesNotReportSuccess() = runBlocking {
        var closed = false
        val output = object : java.io.OutputStream() {
            override fun write(value: Int) { throw IOException("injected SAF write failure") }
            override fun close() { closed = true }
        }
        try { manager.exportBackup(output, passphrase); fail("Failed write must propagate") }
        catch (_: IOException) { }
        assertTrue(closed)
        assertOriginalState()
    }

    @Test fun corruptRecoveryJournalFailsClosedAndIsRetained() = runBlocking {
        journalFile.writeBytes(ByteArray(48) { 9 })
        val restarted = newManager()
        assertTrue(restarted.recoverPendingRestore().isFailure)
        assertEquals(RestoreState.RECOVERY_REQUIRED, restarted.restoreState.value)
        assertTrue(BackupManager.hasPendingRestore(context))
        assertTrue(restarted.restorePayloadWithRollback(payload).isFailure)
        assertOriginalState()
    }

    @Test fun startupDeletesOnlyKnownLegacyPlaintextSnapshot() = runBlocking {
        val legacy = File(context.filesDir, "pre_restore_snapshot.json")
        val unrelated = File(context.filesDir, "keep-user-file.json")
        legacy.writeText("synthetic obsolete rollback snapshot")
        unrelated.writeText("preserve this file")
        try {
            newManager().recoverPendingRestore().getOrThrow()
            assertFalse(legacy.exists())
            assertEquals("preserve this file", unrelated.readText())
            assertOriginalState()
        } finally { legacy.delete(); unrelated.delete() }
    }

    @Test fun legacyPrivacyCleanupFailureBlocksWithoutDeletingUnrelatedContents() = runBlocking {
        val legacy = File(context.filesDir, "pre_restore_snapshot.json")
        legacy.mkdir()
        val child = File(legacy, "preserve")
        child.writeText("not a generated snapshot")
        try {
            val restarted = newManager()
            assertTrue(restarted.recoverPendingRestore().isFailure)
            assertEquals(RestoreState.RECOVERY_REQUIRED, restarted.restoreState.value)
            assertEquals("not a generated snapshot", child.readText())
            assertOriginalState()
        } finally { child.delete(); legacy.delete() }
    }

    private suspend fun assertOriginalState() {
        assertEquals(listOf(originalSession), db.streakDao().getAllSessionsSync())
        assertEquals(listOf(originalEntry), db.journalDao().getAllEntriesSync())
        assertEquals(listOf(BibleChapterCompletionEntity(1, 1)), db.bibleDao().getAllCompletedSync())
        assertEquals(settings, prefs.snapshotUserSettings())
    }

    private fun encryptPortable(value: BackupPayload, password: String, header: String, iterations: Int): ByteArray {
        val json = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(BackupPayload::class.java).toJson(value)
        return encryptJson(json, password, header, iterations)
    }

    private fun encryptJson(json: String, password: String, header: String, iterations: Int): ByteArray {
        val salt = ByteArray(16) { 1 }
        val iv = ByteArray(12) { 2 }
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, 256)).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return header.toByteArray() + salt + iv + cipher.doFinal(json.toByteArray())
    }

    private class SimulatedProcessDeath : Error("simulated process termination")
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
