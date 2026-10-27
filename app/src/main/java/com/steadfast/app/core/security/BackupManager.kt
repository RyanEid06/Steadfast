package com.steadfast.app.core.security

import android.content.Context
import androidx.room.withTransaction
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.datastore.SteadfastPreferences
import com.steadfast.app.data.db.BibleDao
import com.steadfast.app.data.db.JournalDao
import com.steadfast.app.data.db.SteadfastDatabase
import com.steadfast.app.data.db.StreakDao
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.model.JournalEntryEntity
import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.ui.theme.ThemeSetting
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.ZoneId
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = true)
data class BackupSettingsPayload(
    val themeMode: String? = null,
    val recoveryFocusId: String? = null,
    val readingGoalMinimum: Int? = null,
    val oldTestamentCurrent: ReadingLocationPayload? = null,
    val newTestamentCurrent: ReadingLocationPayload? = null,
    val appLockEnabled: Boolean? = null
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
        val SUPPORTED_SCHEMA_VERSIONS = setOf(1, 2)
    }
}

class BackupManager(
    private val context: Context,
    private val database: SteadfastDatabase,
    private val streakDao: StreakDao,
    private val journalDao: JournalDao,
    private val bibleDao: BibleDao,
    private val preferences: SteadfastPreferences
) {
    // Overload constructor for backwards compatibility
    constructor(
        context: Context,
        streakDao: StreakDao,
        journalDao: JournalDao,
        bibleDao: BibleDao,
        preferences: SteadfastPreferences
    ) : this(
        context = context,
        database = SteadfastDatabase.getInstance(context),
        streakDao = streakDao,
        journalDao = journalDao,
        bibleDao = bibleDao,
        preferences = preferences
    )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val magicHeader = "STDF_V1".toByteArray(StandardCharsets.UTF_8)
    private val saltLength = 16
    private val ivLength = 12
    private val tagBitLength = 128
    private val iterationCount = 65536
    private val keyBitLength = 256

    companion object {
        // Protestant Canon: Chapter count by book ID (1..66)
        val BOOK_CHAPTER_COUNTS = intArrayOf(
            0, // index 0 unused
            50, 40, 27, 36, 34, 24, 21, 4, 31, 24, // 1..10 (Gen..2Sam)
            22, 25, 29, 36, 10, 13, 10, 42, 150, 31, // 11..20 (1Kgs..Prov)
            12, 8, 66, 52, 5, 48, 12, 14, 3, 9, // 21..30 (Eccl..Amos)
            1, 4, 7, 3, 3, 3, 2, 14, 4, // 31..39 (Obad..Mal)
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, // 40..49 (Matt..Eph)
            4, 4, 5, 3, 6, 4, 3, 1, 13, 5, // 50..59 (Phil..Jas)
            5, 3, 5, 1, 1, 1, 22 // 60..66 (1Pet..Rev)
        )
    }

    suspend fun exportBackup(outputStream: OutputStream, passphrase: String) {
        val theme = preferences.themeSetting.firstOrNull() ?: ThemeSetting.SYSTEM
        val focus = preferences.recoveryFocusId.firstOrNull() ?: "pornography"
        val goal = preferences.readingGoalMinimum.firstOrNull() ?: 2
        val otLoc = preferences.otCurrentReading.firstOrNull()
        val ntLoc = preferences.ntCurrentReading.firstOrNull()
        val appLock = preferences.isAppLockEnabled.firstOrNull() ?: false

        val sessions = streakDao.getAllSessionsSync().map {
            StreakSessionPayload(
                id = it.id,
                recoveryFocusId = it.recoveryFocusId,
                startEpochMillis = it.startEpochMillis,
                startZoneId = it.startZoneId,
                endEpochMillis = it.endEpochMillis,
                createdAtEpochMillis = it.createdAtEpochMillis
            )
        }

        val entries = journalDao.getAllEntriesSync().map {
            JournalEntryPayload(
                id = it.id,
                createdAtEpochMillis = it.createdAtEpochMillis,
                updatedAtEpochMillis = it.updatedAtEpochMillis,
                body = it.body,
                context = it.context,
                streakSessionId = it.streakSessionId
            )
        }

        val bibleCompletions = bibleDao.getAllCompletedSync().map {
            BibleCompletionPayload(it.bookId, it.chapter)
        }

        val payload = BackupPayload(
            schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = System.currentTimeMillis(),
            appVersion = "1.0.0",
            settings = BackupSettingsPayload(
                themeMode = theme.name,
                recoveryFocusId = focus,
                readingGoalMinimum = goal,
                oldTestamentCurrent = otLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                newTestamentCurrent = ntLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                appLockEnabled = appLock
            ),
            streakSessions = sessions,
            journalEntries = entries,
            bibleCompletedChapters = bibleCompletions
        )

        val json = moshi.adapter(BackupPayload::class.java).toJson(payload)
        val plaintext = json.toByteArray(StandardCharsets.UTF_8)

        // Generate salt & IV
        val random = SecureRandom()
        val salt = ByteArray(saltLength)
        random.nextBytes(salt)
        val iv = ByteArray(ivLength)
        random.nextBytes(iv)

        // Derive key with PBKDF2
        val key = deriveKey(passphrase, salt)

        // Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(tagBitLength, iv))
        val ciphertext = cipher.doFinal(plaintext)

        outputStream.use { out ->
            out.write(magicHeader)
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
            out.flush()
        }
    }

    suspend fun importBackup(inputStream: InputStream, passphrase: String): Result<BackupPayload> {
        return try {
            val allBytes = inputStream.use { it.readBytes() }
            if (allBytes.size < magicHeader.size + saltLength + ivLength + 16) {
                return Result.failure(IllegalArgumentException("Invalid .steadfast backup file structure"))
            }

            val buffer = ByteBuffer.wrap(allBytes)
            val headerBytes = ByteArray(magicHeader.size)
            buffer.get(headerBytes)
            if (!headerBytes.contentEquals(magicHeader)) {
                return Result.failure(IllegalArgumentException("Not a valid Steadfast backup file"))
            }

            val salt = ByteArray(saltLength)
            buffer.get(salt)
            val iv = ByteArray(ivLength)
            buffer.get(iv)

            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagBitLength, iv))
            val plaintext = cipher.doFinal(ciphertext)

            val json = String(plaintext, StandardCharsets.UTF_8)
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(json)
                ?: return Result.failure(IllegalStateException("Failed to parse backup payload"))

            // 1. Strict pre-mutation validation
            validatePayload(payload)

            // 2. All-or-nothing transactional restore with snapshot rollback
            restorePayloadTransactionally(payload)

            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validatePayload(payload: BackupPayload) {
        if (payload.schemaVersion !in BackupPayload.SUPPORTED_SCHEMA_VERSIONS) {
            throw IllegalArgumentException(
                "Unsupported backup schema version: ${payload.schemaVersion}. Supported: ${BackupPayload.SUPPORTED_SCHEMA_VERSIONS}"
            )
        }
        if (payload.exportedAtEpochMillis <= 0) {
            throw IllegalArgumentException("Invalid exportedAtEpochMillis: ${payload.exportedAtEpochMillis}")
        }

        // Validate settings
        payload.settings.themeMode?.let {
            if (it !in setOf("SYSTEM", "DARK", "LIGHT")) {
                throw IllegalArgumentException("Invalid themeMode in backup: $it")
            }
        }
        payload.settings.recoveryFocusId?.let {
            if (it.isBlank() || it.length > 100) {
                throw IllegalArgumentException("Invalid recoveryFocusId in backup: $it")
            }
        }
        payload.settings.readingGoalMinimum?.let {
            if (it !in 1..5) {
                throw IllegalArgumentException("readingGoalMinimum must be between 1 and 5, got $it")
            }
        }
        payload.settings.oldTestamentCurrent?.let { ot ->
            if (ot.bookId !in 1..39) {
                throw IllegalArgumentException("OT reading bookId must be 1..39, got ${ot.bookId}")
            }
            val maxCh = BOOK_CHAPTER_COUNTS[ot.bookId]
            if (ot.chapter !in 1..maxCh) {
                throw IllegalArgumentException("OT reading chapter ${ot.chapter} out of range for book ${ot.bookId} (max $maxCh)")
            }
        }
        payload.settings.newTestamentCurrent?.let { nt ->
            if (nt.bookId !in 40..66) {
                throw IllegalArgumentException("NT reading bookId must be 40..66, got ${nt.bookId}")
            }
            val maxCh = BOOK_CHAPTER_COUNTS[nt.bookId]
            if (nt.chapter !in 1..maxCh) {
                throw IllegalArgumentException("NT reading chapter ${nt.chapter} out of range for book ${nt.bookId} (max $maxCh)")
            }
        }

        // Validate streak sessions
        val sessionIds = mutableSetOf<Long>()
        var activeSessionCount = 0
        for (session in payload.streakSessions) {
            if (session.id <= 0) {
                throw IllegalArgumentException("Invalid streakSession id: ${session.id}")
            }
            if (!sessionIds.add(session.id)) {
                throw IllegalArgumentException("Duplicate streakSession id in backup: ${session.id}")
            }
            if (session.recoveryFocusId.isBlank()) {
                throw IllegalArgumentException("Streak session recoveryFocusId cannot be blank")
            }
            if (session.startEpochMillis <= 0) {
                throw IllegalArgumentException("Streak session startEpochMillis must be positive")
            }
            try {
                ZoneId.of(session.startZoneId)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid streak session timezone: ${session.startZoneId}")
            }
            if (session.endEpochMillis != null) {
                if (session.endEpochMillis < session.startEpochMillis) {
                    throw IllegalArgumentException("Streak session endEpochMillis (${session.endEpochMillis}) precedes start (${session.startEpochMillis})")
                }
            } else {
                activeSessionCount++
            }
        }
        if (activeSessionCount > 1) {
            throw IllegalArgumentException("Backup contains $activeSessionCount active streak sessions; maximum 1 permitted")
        }

        // Validate journal entries
        val entryIds = mutableSetOf<Long>()
        for (entry in payload.journalEntries) {
            if (entry.id <= 0) {
                throw IllegalArgumentException("Invalid journalEntry id: ${entry.id}")
            }
            if (!entryIds.add(entry.id)) {
                throw IllegalArgumentException("Duplicate journalEntry id in backup: ${entry.id}")
            }
            if (entry.body.isBlank()) {
                throw IllegalArgumentException("Journal entry body cannot be blank (id: ${entry.id})")
            }
            if (entry.createdAtEpochMillis <= 0) {
                throw IllegalArgumentException("Journal entry createdAtEpochMillis must be positive")
            }
            entry.context?.let { ctx ->
                if (ctx !in setOf("GENERAL", "URGE", "RESTART")) {
                    throw IllegalArgumentException("Invalid journal context: $ctx in entry ${entry.id}")
                }
            }
            entry.streakSessionId?.let { refSessionId ->
                if (!sessionIds.contains(refSessionId)) {
                    throw IllegalArgumentException("Journal entry ${entry.id} references non-existent streakSessionId: $refSessionId")
                }
            }
        }

        // Validate Bible completions
        val completionPairs = mutableSetOf<Pair<Int, Int>>()
        for (completion in payload.bibleCompletedChapters) {
            if (completion.bookId !in 1..66) {
                throw IllegalArgumentException("Bible bookId must be between 1 and 66, got ${completion.bookId}")
            }
            val maxChapters = BOOK_CHAPTER_COUNTS[completion.bookId]
            if (completion.chapter !in 1..maxChapters) {
                throw IllegalArgumentException("Chapter ${completion.chapter} out of range for bookId ${completion.bookId} (max $maxChapters)")
            }
            if (!completionPairs.add(completion.bookId to completion.chapter)) {
                throw IllegalArgumentException("Duplicate completed chapter in backup: book ${completion.bookId}, chapter ${completion.chapter}")
            }
        }
    }

    private suspend fun restorePayloadTransactionally(payload: BackupPayload) {
        // 1. Snapshot existing state for complete rollback if anything fails
        val previousSessions = streakDao.getAllSessionsSync()
        val previousEntries = journalDao.getAllEntriesSync()
        val previousCompletions = bibleDao.getAllCompletedSync()

        val previousTheme = preferences.themeSetting.firstOrNull() ?: ThemeSetting.SYSTEM
        val previousFocus = preferences.recoveryFocusId.firstOrNull() ?: "pornography"
        val previousGoal = preferences.readingGoalMinimum.firstOrNull() ?: 2
        val previousOtLoc = preferences.otCurrentReading.firstOrNull()
        val previousNtLoc = preferences.ntCurrentReading.firstOrNull()
        val previousAppLock = preferences.isAppLockEnabled.firstOrNull() ?: false
        val previousOnboarding = preferences.isOnboardingComplete.firstOrNull() ?: false

        // Write snapshot to app-private cache/files as a safety hedge
        try {
            val snapshotFile = File(context.filesDir, "pre_restore_snapshot.json")
            val snapshotPayload = BackupPayload(
                schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
                exportedAtEpochMillis = System.currentTimeMillis(),
                appVersion = "1.0.0",
                settings = BackupSettingsPayload(
                    themeMode = previousTheme.name,
                    recoveryFocusId = previousFocus,
                    readingGoalMinimum = previousGoal,
                    oldTestamentCurrent = previousOtLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                    newTestamentCurrent = previousNtLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                    appLockEnabled = previousAppLock
                ),
                streakSessions = previousSessions.map {
                    StreakSessionPayload(it.id, it.recoveryFocusId, it.startEpochMillis, it.startZoneId, it.endEpochMillis, it.createdAtEpochMillis)
                },
                journalEntries = previousEntries.map {
                    JournalEntryPayload(it.id, it.createdAtEpochMillis, it.updatedAtEpochMillis, it.body, it.context, it.streakSessionId)
                },
                bibleCompletedChapters = previousCompletions.map {
                    BibleCompletionPayload(it.bookId, it.chapter)
                }
            )
            val snapshotJson = moshi.adapter(BackupPayload::class.java).toJson(snapshotPayload)
            snapshotFile.writeText(snapshotJson, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Snapshot file write failure is non-fatal for in-memory rollback
        }

        val targetSessions = payload.streakSessions.map {
            StreakSessionEntity(
                id = it.id,
                recoveryFocusId = it.recoveryFocusId,
                startEpochMillis = it.startEpochMillis,
                startZoneId = it.startZoneId,
                endEpochMillis = it.endEpochMillis,
                createdAtEpochMillis = it.createdAtEpochMillis
            )
        }

        val targetEntries = payload.journalEntries.map {
            JournalEntryEntity(
                id = it.id,
                createdAtEpochMillis = it.createdAtEpochMillis,
                updatedAtEpochMillis = it.updatedAtEpochMillis,
                body = it.body,
                context = it.context,
                streakSessionId = it.streakSessionId
            )
        }

        val targetCompletions = payload.bibleCompletedChapters.map {
            BibleChapterCompletionEntity(it.bookId, it.chapter)
        }

        try {
            // 2. Atomic Room Database Transaction
            database.withTransaction {
                streakDao.replaceAll(targetSessions)
                journalDao.replaceAll(targetEntries)
                bibleDao.replaceAll(targetCompletions)
            }

            // 3. Apply settings
            payload.settings.themeMode?.let {
                val theme = when (it) {
                    "DARK" -> ThemeSetting.DARK
                    "LIGHT" -> ThemeSetting.LIGHT
                    else -> ThemeSetting.SYSTEM
                }
                preferences.setThemeSetting(theme)
            }
            payload.settings.recoveryFocusId?.let {
                preferences.setRecoveryFocusId(it)
            }
            payload.settings.readingGoalMinimum?.let {
                preferences.setReadingGoalMinimum(it)
            }
            val ot = payload.settings.oldTestamentCurrent
            preferences.setOtCurrentReading(ot?.bookId, ot?.chapter)
            val nt = payload.settings.newTestamentCurrent
            preferences.setNtCurrentReading(nt?.bookId, nt?.chapter)
            payload.settings.appLockEnabled?.let {
                preferences.setAppLockEnabled(it)
            }

            preferences.setOnboardingComplete(true)
        } catch (e: Exception) {
            // 4. Rollback both database and preferences completely on any error
            try {
                database.withTransaction {
                    streakDao.replaceAll(previousSessions)
                    journalDao.replaceAll(previousEntries)
                    bibleDao.replaceAll(previousCompletions)
                }
                preferences.setThemeSetting(previousTheme)
                preferences.setRecoveryFocusId(previousFocus)
                preferences.setReadingGoalMinimum(previousGoal)
                preferences.setOtCurrentReading(previousOtLoc?.bookId, previousOtLoc?.chapter)
                preferences.setNtCurrentReading(previousNtLoc?.bookId, previousNtLoc?.chapter)
                preferences.setAppLockEnabled(previousAppLock)
                preferences.setOnboardingComplete(previousOnboarding)
            } catch (_: Exception) {
                // Secondary error ignored; rethrow original root exception
            }
            throw e
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterationCount, keyBitLength)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
