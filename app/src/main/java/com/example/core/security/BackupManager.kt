package com.example.core.security

import android.content.Context
import com.example.data.datastore.ReadingLocation
import com.example.data.datastore.SteadfastPreferences
import com.example.data.db.BibleDao
import com.example.data.db.JournalDao
import com.example.data.db.StreakDao
import com.example.data.model.BibleChapterCompletionEntity
import com.example.data.model.JournalEntryEntity
import com.example.data.model.StreakSessionEntity
import com.example.ui.theme.ThemeSetting
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.firstOrNull
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
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
    val appLockEnabled: Boolean? = null,
    val shownMilestones: String? = null
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
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val settings: BackupSettingsPayload = BackupSettingsPayload(),
    val streakSessions: List<StreakSessionPayload> = emptyList(),
    val journalEntries: List<JournalEntryPayload> = emptyList(),
    val bibleCompletedChapters: List<BibleCompletionPayload> = emptyList()
)

class BackupManager(
    private val context: Context,
    private val streakDao: StreakDao,
    private val journalDao: JournalDao,
    private val bibleDao: BibleDao,
    private val preferences: SteadfastPreferences
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    companion object {
        val HEADER_V1 = "STDF_V1".toByteArray(StandardCharsets.UTF_8)
        val HEADER_V2 = "STDF_V2".toByteArray(StandardCharsets.UTF_8)
        const val ITERATIONS_V1 = 65536
        const val ITERATIONS_V2 = 210000
        const val SALT_LENGTH = 16
        const val IV_LENGTH = 12
        const val TAG_BIT_LENGTH = 128
        const val KEY_BIT_LENGTH = 256
        const val MIN_PASSPHRASE_LENGTH = 6
    }

    suspend fun exportBackup(outputStream: OutputStream?, passphrase: String) {
        if (outputStream == null) {
            throw IllegalArgumentException("Target file output stream could not be opened.")
        }
        if (passphrase.length < MIN_PASSPHRASE_LENGTH) {
            throw IllegalArgumentException("Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters long.")
        }

        val theme = preferences.themeSetting.firstOrNull() ?: ThemeSetting.SYSTEM
        val focus = preferences.recoveryFocusId.firstOrNull() ?: "pornography"
        val goal = preferences.readingGoalMinimum.firstOrNull() ?: 2
        val otLoc = preferences.otCurrentReading.firstOrNull()
        val ntLoc = preferences.ntCurrentReading.firstOrNull()
        val lockEnabled = preferences.isAppLockEnabled.firstOrNull() ?: false
        val shownMilestones = preferences.shownMilestones.firstOrNull()?.joinToString(",")

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
            schemaVersion = 1,
            exportedAtEpochMillis = System.currentTimeMillis(),
            appVersion = "1.0.0",
            settings = BackupSettingsPayload(
                themeMode = theme.name,
                recoveryFocusId = focus,
                readingGoalMinimum = goal,
                oldTestamentCurrent = otLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                newTestamentCurrent = ntLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) },
                appLockEnabled = lockEnabled,
                shownMilestones = shownMilestones
            ),
            streakSessions = sessions,
            journalEntries = entries,
            bibleCompletedChapters = bibleCompletions
        )

        val json = moshi.adapter(BackupPayload::class.java).toJson(payload)
        val plaintext = json.toByteArray(StandardCharsets.UTF_8)

        // Generate salt & IV
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        // Use modern STDF_V2 envelope (210,000 iterations)
        val key = deriveKey(passphrase, salt, ITERATIONS_V2)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BIT_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext)

        outputStream.use { out ->
            out.write(HEADER_V2)
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
            out.flush()
        }
    }

    /**
     * Preview decrypted backup contents WITHOUT mutating any database or preferences state.
     */
    fun previewBackup(inputStream: InputStream?, passphrase: String): Result<BackupPayload> {
        if (inputStream == null) {
            return Result.failure(IllegalArgumentException("Backup input stream is not accessible"))
        }

        return try {
            val allBytes = inputStream.use { it.readBytes() }
            val minSize = HEADER_V1.size + SALT_LENGTH + IV_LENGTH + 16
            if (allBytes.size < minSize) {
                return Result.failure(IllegalArgumentException("File is too small to be a valid .steadfast backup"))
            }

            val buffer = ByteBuffer.wrap(allBytes)
            val headerBytes = ByteArray(HEADER_V1.size)
            buffer.get(headerBytes)

            val iterations = when {
                headerBytes.contentEquals(HEADER_V2) -> ITERATIONS_V2
                headerBytes.contentEquals(HEADER_V1) -> ITERATIONS_V1
                else -> return Result.failure(IllegalArgumentException("Unrecognized Steadfast backup header format"))
            }

            val salt = ByteArray(SALT_LENGTH)
            buffer.get(salt)
            val iv = ByteArray(IV_LENGTH)
            buffer.get(iv)

            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val key = deriveKey(passphrase, salt, iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BIT_LENGTH, iv))
            val plaintext = cipher.doFinal(ciphertext)

            val json = String(plaintext, StandardCharsets.UTF_8)
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(json) ?: return Result.failure(IllegalStateException("Malformed backup payload JSON"))

            // Validate payload
            validatePayload(payload).onFailure { return Result.failure(it) }

            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Decryption failed. Please check your passphrase."))
        }
    }

    suspend fun importBackup(inputStream: InputStream?, passphrase: String): Result<Unit> {
        val preview = previewBackup(inputStream, passphrase)
        if (preview.isFailure) {
            return Result.failure(preview.exceptionOrNull() ?: IllegalArgumentException("Import failed"))
        }
        return restorePayloadWithRollback(preview.getOrThrow())
    }

    /**
     * Transactional replace-restore with in-memory rollback. Never writes plaintext files to disk.
     */
    suspend fun restorePayloadWithRollback(payload: BackupPayload): Result<Unit> {
        // Take in-memory snapshots of existing live data
        val oldSessions = streakDao.getAllSessionsSync()
        val oldEntries = journalDao.getAllEntriesSync()
        val oldCompletions = bibleDao.getAllCompletedSync()
        val oldTheme = preferences.themeSetting.firstOrNull() ?: ThemeSetting.SYSTEM
        val oldFocus = preferences.recoveryFocusId.firstOrNull() ?: "pornography"
        val oldGoal = preferences.readingGoalMinimum.firstOrNull() ?: 2
        val oldOt = preferences.otCurrentReading.firstOrNull()
        val oldNt = preferences.ntCurrentReading.firstOrNull()
        val oldLock = preferences.isAppLockEnabled.firstOrNull() ?: false
        val oldMilestones = preferences.shownMilestones.firstOrNull()?.joinToString(",")

        return try {
            // 1. Restore streak sessions
            val newSessions = payload.streakSessions.map {
                StreakSessionEntity(
                    id = it.id,
                    recoveryFocusId = it.recoveryFocusId,
                    startEpochMillis = it.startEpochMillis,
                    startZoneId = it.startZoneId,
                    endEpochMillis = it.endEpochMillis,
                    createdAtEpochMillis = it.createdAtEpochMillis
                )
            }
            streakDao.replaceAll(newSessions)

            // 2. Restore journal entries
            val newEntries = payload.journalEntries.map {
                JournalEntryEntity(
                    id = it.id,
                    createdAtEpochMillis = it.createdAtEpochMillis,
                    updatedAtEpochMillis = it.updatedAtEpochMillis,
                    body = it.body,
                    context = it.context,
                    streakSessionId = it.streakSessionId
                )
            }
            journalDao.replaceAll(newEntries)

            // 3. Restore Bible completions
            val newCompletions = payload.bibleCompletedChapters.map {
                BibleChapterCompletionEntity(it.bookId, it.chapter)
            }
            bibleDao.replaceAll(newCompletions)

            // 4. Restore preferences
            payload.settings.themeMode?.let {
                val theme = when (it) {
                    "DARK" -> ThemeSetting.DARK
                    "LIGHT" -> ThemeSetting.LIGHT
                    else -> ThemeSetting.SYSTEM
                }
                preferences.setThemeSetting(theme)
            }
            payload.settings.recoveryFocusId?.let { preferences.setRecoveryFocusId(it) }
            payload.settings.readingGoalMinimum?.let { preferences.setReadingGoalMinimum(it) }
            preferences.setOtCurrentReading(payload.settings.oldTestamentCurrent?.bookId, payload.settings.oldTestamentCurrent?.chapter)
            preferences.setNtCurrentReading(payload.settings.newTestamentCurrent?.bookId, payload.settings.newTestamentCurrent?.chapter)
            payload.settings.appLockEnabled?.let { preferences.setAppLockEnabled(it) }

            preferences.setOnboardingComplete(true)
            Result.success(Unit)
        } catch (e: Exception) {
            // Roll back in-memory snapshot
            try {
                streakDao.replaceAll(oldSessions)
                journalDao.replaceAll(oldEntries)
                bibleDao.replaceAll(oldCompletions)
                preferences.setThemeSetting(oldTheme)
                preferences.setRecoveryFocusId(oldFocus)
                preferences.setReadingGoalMinimum(oldGoal)
                preferences.setOtCurrentReading(oldOt?.bookId, oldOt?.chapter)
                preferences.setNtCurrentReading(oldNt?.bookId, oldNt?.chapter)
                preferences.setAppLockEnabled(oldLock)
            } catch (_: Exception) {}

            Result.failure(e)
        }
    }

    private fun validatePayload(payload: BackupPayload): Result<Unit> {
        if (payload.schemaVersion > 1) {
            return Result.failure(IllegalStateException("Backup schema version ${payload.schemaVersion} is newer than this app supports."))
        }

        // Validate active sessions count (at most one active)
        val activeCount = payload.streakSessions.count { it.endEpochMillis == null }
        if (activeCount > 1) {
            return Result.failure(IllegalStateException("Invalid backup: Multiple active streak sessions found."))
        }

        // Validate Bible chapters bounds (book 1..66, chapter >= 1)
        for (comp in payload.bibleCompletedChapters) {
            if (comp.bookId !in 1..66 || comp.chapter < 1) {
                return Result.failure(IllegalStateException("Invalid Bible completion in backup: Book ${comp.bookId}, Ch. ${comp.chapter}"))
            }
        }

        // Validate journal entries
        for (entry in payload.journalEntries) {
            if (entry.updatedAtEpochMillis < entry.createdAtEpochMillis) {
                return Result.failure(IllegalStateException("Invalid journal entry: updated time earlier than created time."))
            }
        }

        return Result.success(Unit)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BIT_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
