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
    val newTestamentCurrent: ReadingLocationPayload? = null
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

    private val magicHeader = "STDF_V1".toByteArray(StandardCharsets.UTF_8)
    private val saltLength = 16
    private val ivLength = 12
    private val tagBitLength = 128
    private val iterationCount = 65536
    private val keyBitLength = 256

    suspend fun exportBackup(outputStream: OutputStream, passphrase: String) {
        val theme = preferences.themeSetting.firstOrNull() ?: ThemeSetting.SYSTEM
        val focus = preferences.recoveryFocusId.firstOrNull() ?: "pornography"
        val goal = preferences.readingGoalMinimum.firstOrNull() ?: 2
        val otLoc = preferences.otCurrentReading.firstOrNull()
        val ntLoc = preferences.ntCurrentReading.firstOrNull()

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
                newTestamentCurrent = ntLoc?.let { ReadingLocationPayload(it.bookId, it.chapter) }
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
            val payload = adapter.fromJson(json) ?: return Result.failure(IllegalStateException("Failed to parse backup payload"))

            if (payload.schemaVersion > 1) {
                return Result.failure(IllegalStateException("Backup file version (${payload.schemaVersion}) is newer than this app supports."))
            }

            // Transactionally restore
            restorePayload(payload)

            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restorePayload(payload: BackupPayload) {
        // 1. Restore streak sessions
        val sessions = payload.streakSessions.map {
            StreakSessionEntity(
                id = it.id,
                recoveryFocusId = it.recoveryFocusId,
                startEpochMillis = it.startEpochMillis,
                startZoneId = it.startZoneId,
                endEpochMillis = it.endEpochMillis,
                createdAtEpochMillis = it.createdAtEpochMillis
            )
        }
        streakDao.replaceAll(sessions)

        // 2. Restore journal entries
        val entries = payload.journalEntries.map {
            JournalEntryEntity(
                id = it.id,
                createdAtEpochMillis = it.createdAtEpochMillis,
                updatedAtEpochMillis = it.updatedAtEpochMillis,
                body = it.body,
                context = it.context,
                streakSessionId = it.streakSessionId
            )
        }
        journalDao.replaceAll(entries)

        // 3. Restore Bible completed chapters
        val completions = payload.bibleCompletedChapters.map {
            BibleChapterCompletionEntity(it.bookId, it.chapter)
        }
        bibleDao.replaceAll(completions)

        // 4. Restore settings
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

        preferences.setOnboardingComplete(true)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterationCount, keyBitLength)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
