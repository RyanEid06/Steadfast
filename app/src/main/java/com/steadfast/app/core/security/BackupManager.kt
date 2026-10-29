package com.steadfast.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.room.withTransaction
import com.steadfast.app.BuildConfig
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.datastore.UserPreferencesSnapshot
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.data.seed.AssetSeedLoader
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.time.ZoneId
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.coroutineContext

@JsonClass(generateAdapter = true)
data class BackupSettingsPayload(
    val themeMode: String? = null,
    val recoveryFocusId: String? = null,
    val readingGoalMinimum: Int? = null,
    val oldTestamentCurrent: ReadingLocationPayload? = null,
    val newTestamentCurrent: ReadingLocationPayload? = null,
    val appLockEnabled: Boolean? = null,
    val shownMilestones: String? = null,
    val onboardingComplete: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ReadingLocationPayload(
    val bookId: Int,
    val chapter: Int
)

@JsonClass(generateAdapter = true)
data class StreakSessionPayload(
    val id: Long,
    val recoveryFocusId: String,
    val startEpochMillis: Long,
    val startZoneId: String,
    val endEpochMillis: Long?,
    val createdAtEpochMillis: Long
)

@JsonClass(generateAdapter = true)
data class JournalEntryPayload(
    val id: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val body: String,
    val context: String?,
    val streakSessionId: Long?
)

@JsonClass(generateAdapter = true)
data class BibleCompletionPayload(
    val bookId: Int,
    val chapter: Int
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val settings: BackupSettingsPayload = BackupSettingsPayload(),
    val streakSessions: List<StreakSessionPayload> = emptyList(),
    val journalEntries: List<JournalEntryPayload> = emptyList(),
    val bibleCompletedChapters: List<BibleCompletionPayload> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

enum class RestoreState { CHECKING, READY, RESTORING, RECOVERY_REQUIRED }

@JsonClass(generateAdapter = true)
internal data class RestoreJournal(
    val token: String,
    val before: UserPreferencesSnapshot,
    val after: UserPreferencesSnapshot
)

class BackupManager(
    private val context: Context,
    private val database: SteadfastDatabase,
    private val preferences: SteadfastPreferences,
    private val recoveryKeyProvider: () -> SecretKey = { deviceRecoveryKey() }
) {
    private val streakDao = database.streakDao()
    private val journalDao = database.journalDao()
    private val bibleDao = database.bibleDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java)
    private val journalAdapter = moshi.adapter(RestoreJournal::class.java)
    private val journal = AtomicFile(journalFile(context))
    private val mutableRestoreState = MutableStateFlow(RestoreState.CHECKING)
    val restoreState: StateFlow<RestoreState> = mutableRestoreState.asStateFlow()
    private val focusIds by lazy { AssetSeedLoader(context).loadRecoveryFocuses().map { it.id }.toSet() }
    private val milestoneIds by lazy { AssetSeedLoader(context).loadMilestones().map { it.id }.toSet() }

    companion object {
        val HEADER_V1 = "STDF_V1".toByteArray(StandardCharsets.UTF_8)
        val HEADER_V2 = "STDF_V2".toByteArray(StandardCharsets.UTF_8)
        const val ITERATIONS_V1 = 65536
        const val ITERATIONS_V2 = 210000
        const val SALT_LENGTH = 16
        const val IV_LENGTH = 12
        const val TAG_BIT_LENGTH = 128
        const val KEY_BIT_LENGTH = 256
        const val MIN_PASSPHRASE_LENGTH = 12
        private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
        private const val MAX_EPOCH_MILLIS = 253402300799999L
        private const val COMMIT_TABLE = "steadfast_restore_commit"
        private const val RECOVERY_KEY_ALIAS = "steadfast.restore.recovery.v1"
        private val operationMutex = Mutex()

        val BOOK_CHAPTER_COUNTS = intArrayOf(
            0, 50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31,
            12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5,
            5, 3, 5, 1, 1, 1, 22
        )

        private fun journalFile(context: Context) = File(context.noBackupFilesDir, "restore-recovery.enc")

        fun hasPendingRestore(context: Context): Boolean {
            val file = journalFile(context)
            return file.exists() || File(file.path + ".bak").exists() || File(file.path + ".new").exists()
        }

        /** Keeps widget reads/publication on one side of a restore. */
        suspend fun <T> withStableUserData(context: Context, block: suspend () -> T): T? = operationMutex.withLock {
            if (hasPendingRestore(context)) null else block()
        }

        private fun deviceRecoveryKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keyStore.getKey(RECOVERY_KEY_ALIAS, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(
                    RECOVERY_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BIT_LENGTH).build())
                generateKey()
            }
        }
    }

    /** Must complete before presenting user data, including on an ordinary cold start. */
    suspend fun recoverPendingRestore(): Result<Unit> = withContext(NonCancellable + Dispatchers.IO) {
        operationMutex.withLock {
            mutableRestoreState.value = RestoreState.CHECKING
            try {
                removeLegacyPlaintextSnapshot()
                resolvePendingJournal()
                mutableRestoreState.value = RestoreState.READY
                Result.success(Unit)
            } catch (error: Exception) {
                mutableRestoreState.value = RestoreState.RECOVERY_REQUIRED
                Result.failure(error)
            }
        }
    }

    suspend fun exportBackup(outputStream: OutputStream?, passphrase: String) = withContext(Dispatchers.IO) {
        requireNotNull(outputStream) { "Target file could not be opened." }
        outputStream.use { out ->
            require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Use at least $MIN_PASSPHRASE_LENGTH characters." }
            operationMutex.withLock {
                ensureReady()
                val payload = database.withTransaction {
                    val settings = preferences.snapshotUserSettings()
                    BackupPayload(
                        appVersion = BuildConfig.VERSION_NAME,
                        settings = settings.toPayload(),
                        streakSessions = streakDao.getAllSessionsSync().map {
                            StreakSessionPayload(it.id, it.recoveryFocusId, it.startEpochMillis, it.startZoneId, it.endEpochMillis, it.createdAtEpochMillis)
                        },
                        journalEntries = journalDao.getAllEntriesSync().map {
                            JournalEntryPayload(it.id, it.createdAtEpochMillis, it.updatedAtEpochMillis, it.body, it.context, it.streakSessionId)
                        },
                        bibleCompletedChapters = bibleDao.getAllCompletedSync().map { BibleCompletionPayload(it.bookId, it.chapter) }
                    ).copy(exportedAtEpochMillis = System.currentTimeMillis())
                }
                validatePayload(payload)
                val plaintext = payloadAdapter.toJson(payload).toByteArray(StandardCharsets.UTF_8)
                try {
                    require(plaintext.size <= MAX_BACKUP_BYTES - 64) { "Backup exceeds the supported 32 MiB size." }
                    val salt = randomBytes(SALT_LENGTH)
                    val iv = randomBytes(IV_LENGTH)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, ITERATIONS_V2), GCMParameterSpec(TAG_BIT_LENGTH, iv))
                    val ciphertext = cipher.doFinal(plaintext)
                    out.write(HEADER_V2)
                    out.write(salt)
                    out.write(iv)
                    out.write(ciphertext)
                    out.flush()
                } finally { plaintext.fill(0) }
            }
        }
    }

    /** Decryption and complete validation only; never recovers or writes either live store. */
    fun previewBackup(inputStream: InputStream?, passphrase: String): Result<BackupPayload> = try {
        requireNotNull(inputStream) { "Backup file could not be opened." }
        val allBytes = inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BACKUP_BYTES) { "Backup exceeds the supported 32 MiB size." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        require(allBytes.size >= HEADER_V1.size + SALT_LENGTH + IV_LENGTH + 16) { "Backup file is incomplete." }
        val buffer = ByteBuffer.wrap(allBytes)
        val header = ByteArray(HEADER_V1.size).also(buffer::get)
        val iterations = when {
            header.contentEquals(HEADER_V1) -> ITERATIONS_V1
            header.contentEquals(HEADER_V2) -> ITERATIONS_V2
            else -> throw IllegalArgumentException("Unrecognized Steadfast backup format.")
        }
        val salt = ByteArray(SALT_LENGTH).also(buffer::get)
        val iv = ByteArray(IV_LENGTH).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val plaintext = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BIT_LENGTH, iv))
                doFinal(ciphertext)
            }
        } catch (error: Exception) {
            throw IllegalArgumentException("Could not unlock backup. Check the passphrase and file.", error)
        }
        val payload = try {
            val json = String(plaintext, StandardCharsets.UTF_8)
            val fields = moshi.adapter(Map::class.java).fromJson(json)
            require(fields != null && fields.keys.containsAll(setOf(
                "schemaVersion", "exportedAtEpochMillis", "settings", "streakSessions", "journalEntries", "bibleCompletedChapters"
            ))) { "Backup is missing required data fields." }
            requireNotNull(payloadAdapter.fromJson(json)) { "Malformed backup data." }
        } finally { plaintext.fill(0) }
        validatePayload(payload)
        Result.success(payload)
    } catch (error: Exception) { Result.failure(error) }

    suspend fun importBackup(inputStream: InputStream?, passphrase: String): Result<Unit> = withContext(Dispatchers.IO) {
        val payload = previewBackup(inputStream, passphrase).getOrElse { return@withContext Result.failure(it) }
        restorePayloadWithRollback(payload)
    }

    /** Cancellation after staging finishes/reconciles the replace before content can become visible. */
    suspend fun restorePayloadWithRollback(payload: BackupPayload): Result<Unit> {
        coroutineContext.ensureActive()
        return withContext(NonCancellable + Dispatchers.IO) {
            operationMutex.withLock {
                try {
                    validatePayload(payload)
                    ensureReady()
                } catch (error: Exception) { return@withLock Result.failure(error) }
                mutableRestoreState.value = RestoreState.RESTORING
                var roomCommitted = false
                try {
                    val pending = RestoreJournal(UUID.randomUUID().toString(), preferences.snapshotUserSettings(), payload.settings.toSnapshot())
                    writeJournal(pending)
                    // Atomic Room writes, including the decision used after a process interruption.
                    database.withTransaction {
                        streakDao.replaceAll(payload.streakSessions.map {
                            StreakSessionEntity(it.id, it.recoveryFocusId, it.startEpochMillis, it.startZoneId, it.endEpochMillis, it.createdAtEpochMillis)
                        })
                        journalDao.replaceAll(payload.journalEntries.map {
                            JournalEntryEntity(it.id, it.createdAtEpochMillis, it.updatedAtEpochMillis, it.body, it.context, it.streakSessionId)
                        })
                        bibleDao.replaceAll(payload.bibleCompletedChapters.map { BibleChapterCompletionEntity(it.bookId, it.chapter) })
                        val sql = database.openHelper.writableDatabase
                        sql.execSQL("CREATE TABLE IF NOT EXISTS $COMMIT_TABLE (id INTEGER NOT NULL PRIMARY KEY, token TEXT NOT NULL)")
                        sql.execSQL("INSERT OR REPLACE INTO $COMMIT_TABLE (id, token) VALUES (1, ?)", arrayOf(pending.token))
                        preferences.replaceUserSettings(pending.after)
                    }
                    roomCommitted = true
                    clearJournal()
                    mutableRestoreState.value = RestoreState.READY
                    Result.success(Unit)
                } catch (error: Exception) {
                    try {
                        val committed = resolvePendingJournal()
                        mutableRestoreState.value = RestoreState.READY
                        if (error is CancellationException) throw error
                        if (committed || roomCommitted) Result.success(Unit) else Result.failure(error)
                    } catch (recoveryError: Exception) {
                        if (recoveryError === error && error is CancellationException) throw error
                        error.addSuppressed(recoveryError)
                        mutableRestoreState.value = RestoreState.RECOVERY_REQUIRED
                        if (error is CancellationException) throw error
                        Result.failure(IllegalStateException("Restore recovery is incomplete. Retry recovery before using the app.", error))
                    }
                }
            }
        }
    }

    private suspend fun ensureReady() {
        if (mutableRestoreState.value == RestoreState.CHECKING) {
            try { removeLegacyPlaintextSnapshot(); resolvePendingJournal(); mutableRestoreState.value = RestoreState.READY }
            catch (error: Exception) { mutableRestoreState.value = RestoreState.RECOVERY_REQUIRED; throw error }
        }
        check(mutableRestoreState.value == RestoreState.READY) { "Finish backup recovery before continuing." }
    }

    /** Returns the Room commit decision; safe to repeat after any recovery failure. */
    private suspend fun resolvePendingJournal(): Boolean {
        if (!journal.baseFile.exists() && !File(journal.baseFile.path + ".bak").exists()) {
            // AtomicFile's .new alone precedes publication, hence all live mutations.
            clearJournal()
            return false
        }
        val pending = readJournal()
        val sql = database.openHelper.readableDatabase
        val tableExists = sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(COMMIT_TABLE)).use { it.moveToFirst() }
        val committed = tableExists && sql.query("SELECT token FROM $COMMIT_TABLE WHERE id=1").use {
            it.moveToFirst() && it.getString(0) == pending.token
        }
        preferences.replaceUserSettings(if (committed) pending.after else pending.before)
        clearJournal()
        return committed
    }

    private fun writeJournal(pending: RestoreJournal) {
        val plaintext = journalAdapter.toJson(pending).toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, recoveryKeyProvider())
        val encrypted = try { cipher.doFinal(plaintext) } finally { plaintext.fill(0) }
        val stream = journal.startWrite()
        try {
            stream.write(cipher.iv)
            stream.write(encrypted)
            journal.finishWrite(stream)
        } catch (error: Exception) {
            journal.failWrite(stream)
            throw error
        }
        // AtomicFile reports some rename failures only to logcat: verify before touching live data.
        check(readJournal().token == pending.token) { "Could not persist restore recovery journal." }
    }

    private fun readJournal(): RestoreJournal {
        val bytes = journal.openRead().use { it.readBytes() }
        require(bytes.size >= IV_LENGTH + 16) { "Restore recovery journal is incomplete." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, recoveryKeyProvider(), GCMParameterSpec(TAG_BIT_LENGTH, bytes.copyOfRange(0, IV_LENGTH)))
        val plaintext = cipher.doFinal(bytes.copyOfRange(IV_LENGTH, bytes.size))
        return try { requireNotNull(journalAdapter.fromJson(String(plaintext, StandardCharsets.UTF_8))) }
        finally { plaintext.fill(0) }
    }

    private fun clearJournal() {
        journal.delete()
        check(!hasPendingRestore(context)) { "Could not remove restore recovery journal." }
    }

    private fun removeLegacyPlaintextSnapshot() {
        val legacy = File(context.filesDir, "pre_restore_snapshot.json")
        check(!legacy.exists() || (legacy.isFile && legacy.delete())) {
            "Could not remove the legacy private plaintext restore snapshot. Retry privacy cleanup before using the app."
        }
    }

    fun validatePayload(payload: BackupPayload) {
        require(payload.schemaVersion in 1..BackupPayload.CURRENT_SCHEMA_VERSION) { "Unsupported backup schema version." }
        require(payload.exportedAtEpochMillis in 1..MAX_EPOCH_MILLIS) { "Invalid export timestamp." }
        require(payload.appVersion.isNotBlank() && payload.appVersion.length <= 100) { "Invalid application version." }
        fun timestamp(value: Long) = require(value in 1..payload.exportedAtEpochMillis) { "Invalid backup timestamp." }
        fun focus(value: String) = require(value in focusIds) { "Unrecognized recovery focus." }
        fun chapter(book: Int, chapter: Int, books: IntRange = 1..66) {
            require(book in books && chapter in 1..BOOK_CHAPTER_COUNTS[book]) { "Invalid Bible reading location." }
        }
        payload.settings.apply {
            themeMode?.let { require(it in setOf("SYSTEM", "LIGHT", "DARK")) { "Invalid theme." } }
            recoveryFocusId?.let(::focus)
            readingGoalMinimum?.let { require(it in 1..5) { "Reading goal must be between 1 and 5." } }
            oldTestamentCurrent?.let { chapter(it.bookId, it.chapter, 1..39) }
            newTestamentCurrent?.let { chapter(it.bookId, it.chapter, 40..66) }
            shownMilestones?.takeIf { it.isNotEmpty() }?.split(",")?.let { ids ->
                require(ids.size == ids.toSet().size && ids.all { it in milestoneIds }) { "Invalid shown milestones." }
            }
        }
        val sessionIds = mutableSetOf<Long>()
        require(payload.streakSessions.count { it.endEpochMillis == null } <= 1) { "Multiple active streak sessions." }
        payload.streakSessions.forEach { session ->
            require(session.id > 0 && sessionIds.add(session.id)) { "Invalid or duplicate streak session ID." }
            focus(session.recoveryFocusId)
            timestamp(session.startEpochMillis)
            timestamp(session.createdAtEpochMillis)
            try { ZoneId.of(session.startZoneId) }
            catch (error: java.time.DateTimeException) { throw IllegalArgumentException("Invalid session timezone.", error) }
            session.endEpochMillis?.let { timestamp(it); require(it >= session.startEpochMillis) { "Session ends before it starts." } }
        }
        payload.streakSessions.sortedWith(
            compareBy<StreakSessionPayload> { it.startEpochMillis }.thenBy { it.endEpochMillis ?: Long.MAX_VALUE }
        ).zipWithNext().forEach { (previous, next) ->
            require(previous.endEpochMillis != null && previous.endEpochMillis <= next.startEpochMillis) { "Overlapping streak sessions." }
        }
        val entryIds = mutableSetOf<Long>()
        payload.journalEntries.forEach { entry ->
            require(entry.id > 0 && entryIds.add(entry.id)) { "Invalid or duplicate journal ID." }
            require(entry.body.isNotBlank()) { "Journal entry must contain text." }
            timestamp(entry.createdAtEpochMillis)
            timestamp(entry.updatedAtEpochMillis)
            require(entry.updatedAtEpochMillis >= entry.createdAtEpochMillis) { "Journal update precedes creation." }
            require(entry.context == null || entry.context in setOf("GENERAL", "URGE", "RESTART")) { "Invalid journal context." }
            require(entry.streakSessionId == null || entry.streakSessionId in sessionIds) { "Journal references a missing streak session." }
        }
        val completions = mutableSetOf<Pair<Int, Int>>()
        payload.bibleCompletedChapters.forEach {
            chapter(it.bookId, it.chapter)
            require(completions.add(it.bookId to it.chapter)) { "Duplicate completed Bible chapter." }
        }
    }

    private fun UserPreferencesSnapshot.toPayload() = BackupSettingsPayload(
        themeMode, recoveryFocusId, readingGoalMinimum,
        oldTestamentCurrent?.let { ReadingLocationPayload(it.bookId, it.chapter) },
        newTestamentCurrent?.let { ReadingLocationPayload(it.bookId, it.chapter) },
        appLockEnabled, shownMilestones.joinToString(","), onboardingComplete
    )

    private fun BackupSettingsPayload.toSnapshot() = UserPreferencesSnapshot(
        themeMode = themeMode ?: "SYSTEM",
        recoveryFocusId = recoveryFocusId ?: "pornography",
        readingGoalMinimum = readingGoalMinimum ?: 2,
        oldTestamentCurrent = oldTestamentCurrent?.let { ReadingLocation(it.bookId, it.chapter) },
        newTestamentCurrent = newTestamentCurrent?.let { ReadingLocation(it.bookId, it.chapter) },
        appLockEnabled = appLockEnabled ?: false,
        shownMilestones = shownMilestones.orEmpty().split(",").filter { it.isNotEmpty() }.toSet(),
        onboardingComplete = onboardingComplete ?: true
    )

    private fun randomBytes(size: Int) = ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BIT_LENGTH)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally { spec.clearPassword() }
    }
}
