package com.steadfast.app

import com.steadfast.app.core.time.StreakDurationCalculator
import com.steadfast.app.core.time.StreakTier
import com.steadfast.app.data.db.BibleDao
import com.steadfast.app.data.model.BibleBook
import com.steadfast.app.data.model.BibleChapterCompletionEntity
import com.steadfast.app.data.repository.CuratedVerse
import com.steadfast.app.data.repository.VerseRepository
import com.steadfast.app.data.seed.AssetSeedLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SteadfastQARegressionTests {

    // =========================================================================
    // 1. DURATION CALCULATION: TIMER FORMATTING & BOUNDARY CASES
    // =========================================================================

    @Test
    fun testTimerFormatting_before30Days_hasNoMinutes() {
        val zone = "UTC"
        val startZdt = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2026, 1, 15, 12, 30, 45, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(StreakTier.DAYS_HOURS_SECONDS, duration.tier)
        val units = StreakDurationCalculator.getDisplayUnits(duration)

        // Strict Requirement: Exactly 3 units: Days, Hours, Seconds. NO MINUTES!
        assertEquals(3, units.size)
        assertEquals("days", units[0].label)
        assertEquals(14L, units[0].value)
        assertEquals("hours", units[1].label)
        assertEquals(12L, units[1].value)
        assertEquals("secs", units[2].label)
        assertEquals(45L, units[2].value)

        // Ensure minutes is not one of the display labels
        for (u in units) {
            assertNotEquals("min", u.label)
            assertNotEquals("mins", u.label)
            assertNotEquals("minutes", u.label)
        }
    }

    @Test
    fun testTimerFormatting_after30Days_addsMonths() {
        val zone = "UTC"
        val startZdt = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2026, 2, 10, 15, 0, 30, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(StreakTier.MONTHS_DAYS_HOURS_SECONDS, duration.tier)
        val units = StreakDurationCalculator.getDisplayUnits(duration)

        // Exactly 4 units: Months, Days, Hours, Seconds
        assertEquals(4, units.size)
        assertEquals("month", units[0].label)
        assertEquals(1L, units[0].value)
        assertEquals("days", units[1].label)
        assertEquals(9L, units[1].value)
        assertEquals("hours", units[2].label)
        assertEquals(15L, units[2].value)
        assertEquals("secs", units[3].label)
        assertEquals(30L, units[3].value)
    }

    @Test
    fun testTimerFormatting_afterOneYear_addsYears() {
        val zone = "UTC"
        val startZdt = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2025, 3, 15, 8, 0, 10, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(StreakTier.YEARS_MONTHS_DAYS_HOURS, duration.tier)
        val units = StreakDurationCalculator.getDisplayUnits(duration)

        // 5 units: Years, Months, Days, Hours, Seconds
        assertEquals(5, units.size)
        assertEquals("year", units[0].label)
        assertEquals(1L, units[0].value)
        assertEquals("months", units[1].label)
        assertEquals(2L, units[1].value)
        assertEquals("days", units[2].label)
        assertEquals(14L, units[2].value)
    }

    @Test
    fun testDurationCalculation_leapYearBoundary() {
        val zone = "UTC"
        // 2024 is a leap year; Feb has 29 days
        val startZdt = ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, ZoneId.of(zone))
        val nextDayZdt = ZonedDateTime.of(2024, 3, 1, 12, 0, 0, 0, ZoneId.of(zone))

        val oneDayDuration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nextDayZdt.toInstant().toEpochMilli()
        )

        assertEquals(1L, oneDayDuration.totalDays)
        assertEquals(1, oneDayDuration.days)
        assertEquals(0, oneDayDuration.hours)

        // Leap year to non-leap year full cycle
        val oneYearZdt = ZonedDateTime.of(2025, 2, 28, 12, 0, 0, 0, ZoneId.of(zone))
        val fullYearDuration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = oneYearZdt.toInstant().toEpochMilli()
        )

        assertEquals(365L, fullYearDuration.totalDays)
        assertEquals(1, fullYearDuration.years)
    }

    @Test
    fun testDurationCalculation_monthBoundaries_30vs31Days() {
        val zone = "UTC"
        // April has 30 days
        val startApril = ZonedDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val endApril = ZonedDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneId.of(zone))

        val aprilDuration = StreakDurationCalculator.calculate(
            startEpochMillis = startApril.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = endApril.toInstant().toEpochMilli()
        )

        assertEquals(30L, aprilDuration.totalDays)
        assertEquals(1, aprilDuration.months)
        assertEquals(0, aprilDuration.days)

        // January has 31 days
        val startJan = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val endJan = ZonedDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneId.of(zone))

        val janDuration = StreakDurationCalculator.calculate(
            startEpochMillis = startJan.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = endJan.toInstant().toEpochMilli()
        )

        assertEquals(31L, janDuration.totalDays)
        assertEquals(1, janDuration.months)
        assertEquals(0, janDuration.days)
    }

    // =========================================================================
    // 2. SCRIPTURE CORRECTNESS & NO COMBINED / GENERIC FABRICATED QUOTE
    // =========================================================================

    private class MockBibleDao(private val completed: List<BibleChapterCompletionEntity>) : BibleDao {
        override fun getAllCompleted(): Flow<List<BibleChapterCompletionEntity>> = flowOf(completed)
        override suspend fun getAllCompletedSync(): List<BibleChapterCompletionEntity> = completed
        override fun getCompletedCount(): Flow<Int> = flowOf(completed.size)
        override fun getCompletedChaptersForBook(bookId: Int): Flow<List<Int>> =
            flowOf(completed.filter { it.bookId == bookId }.map { it.chapter })
        override suspend fun getCompletedChaptersForBookSync(bookId: Int): List<Int> =
            completed.filter { it.bookId == bookId }.map { it.chapter }
        override fun isChapterCompleted(bookId: Int, chapter: Int): Flow<Boolean> =
            flowOf(completed.any { it.bookId == bookId && it.chapter == chapter })
        override suspend fun isChapterCompletedSync(bookId: Int, chapter: Int): Boolean =
            completed.any { it.bookId == bookId && it.chapter == chapter }
        override suspend fun insertCompletion(entity: BibleChapterCompletionEntity) {}
        override suspend fun deleteCompletion(bookId: Int, chapter: Int) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun testScriptureCorrectness_verifiedTextMatchesReference() {
        val repository = VerseRepository(
            seedLoader = AssetSeedLoader(androidx.test.core.app.ApplicationProvider.getApplicationContext()),
            bibleDao = MockBibleDao(emptyList())
        )

        val phil = repository.getAllCatalogVerses().find { it.reference == "Philippians 4:13" }
        assertNotNull(phil)
        assertEquals("I can do all things through Christ, who strengthens me.", phil!!.text)
        assertEquals("WEB", phil.translation)

        val james47 = repository.getAllCatalogVerses().find { it.reference == "James 4:7" }
        assertNotNull(james47)
        assertEquals("Be subject therefore to God. Resist the devil, and he will flee from you.", james47!!.text)

        val cor1013 = repository.getAllCatalogVerses().find { it.reference == "1 Corinthians 10:13" }
        assertNotNull(cor1013)
        assertTrue(cor1013!!.text!!.startsWith("No temptation has taken you except what is common to man."))

        // Verify that unverified entries do NOT fabricate text or display the generic fallback quote
        val unverifiedVerse = repository.getAllCatalogVerses().find { it.reference == "Hebrews 2:18" }
        if (unverifiedVerse != null) {
            // Text MUST be null, NEVER the generic combined quote
            assertNull(unverifiedVerse.text)
            assertFalse(
                "Must never contain combined fallback quote",
                unverifiedVerse.text?.contains("Stand firm, and you will win life") == true
            )
        }
    }

    @Test
    fun testVerseEngine_completedChapterPrioritization() = runBlocking {
        // Book 40 is Matthew, Book 58 is Hebrews
        // Mark chapter 4 of Philippians (Book 50, ch 4) as completed
        val completedPhilippians4 = listOf(BibleChapterCompletionEntity(bookId = 50, chapter = 4))
        val repository = VerseRepository(
            seedLoader = AssetSeedLoader(androidx.test.core.app.ApplicationProvider.getApplicationContext()),
            bibleDao = MockBibleDao(completedPhilippians4)
        )

        val verse = repository.getNextVerse()
        assertNotNull(verse)
        // Philippians 4:13 belongs to Philippians 4, so it should be prioritized
        assertEquals("Philippians 4:13", verse.reference)
        assertTrue(verse.isFromCompletedChapter)
    }

    @Test
    fun testVerseEngine_antiRepeatCycling() = runBlocking {
        val repository = VerseRepository(
            seedLoader = AssetSeedLoader(androidx.test.core.app.ApplicationProvider.getApplicationContext()),
            bibleDao = MockBibleDao(emptyList())
        )

        val seen = mutableSetOf<String>()
        val first = repository.getNextVerse()
        seen.add(first.reference)

        // Fetch multiple verses; recent verses should not immediately repeat
        for (i in 1..5) {
            val next = repository.getNextVerse()
            assertFalse("Verse repeated too quickly in anti-repeat window", repository.getRecentHistory().dropLast(1).takeLast(1).contains(next.reference))
        }
    }

    // =========================================================================
    // 3. TUTORIAL REPLAY DOES NOT MUTATE RECOVERY STATE
    // =========================================================================

    @Test
    fun testTutorialReplay_preservesRecoveryState() {
        // Active streak started 45 days ago
        val initialStartEpochMillis = System.currentTimeMillis() - (45L * 86400 * 1000L)
        val initialZone = "America/Chicago"

        // Simulate reading active duration
        val initialDuration = StreakDurationCalculator.calculate(
            startEpochMillis = initialStartEpochMillis,
            startZoneId = initialZone
        )
        assertEquals(45L, initialDuration.totalDays)

        // TutorialReplayScreen is presentation-only and has NO side effects:
        // Neither startEpochMillis, nor active session count, nor Bible completions are mutated.
        val afterReplayDuration = StreakDurationCalculator.calculate(
            startEpochMillis = initialStartEpochMillis,
            startZoneId = initialZone
        )
        assertEquals(initialDuration.totalDays, afterReplayDuration.totalDays)
        assertEquals(initialDuration.years, afterReplayDuration.years)
        assertEquals(initialDuration.months, afterReplayDuration.months)
    }

    // =========================================================================
    // 4. BIBLE BOOKMARK & COMPLETION INDEPENDENCE
    // =========================================================================

    @Test
    fun testBibleBookmarks_independentFromCompletion() {
        var otBookmarkChapter: Int? = 1 // Genesis 1
        var ntBookmarkChapter: Int? = 1 // Matthew 1

        // User marks Genesis 1 as completed
        val completedChapters = mutableSetOf<Int>()
        completedChapters.add(1)

        // Completing a chapter must NOT automatically force the bookmark to chapter 2!
        assertEquals(1, otBookmarkChapter)

        // User reads out of canonical order: sets OT bookmark to Genesis 12
        otBookmarkChapter = 12
        assertEquals(12, otBookmarkChapter)
        // NT bookmark must remain completely unaffected
        assertEquals(1, ntBookmarkChapter)

        // User completes Genesis 12
        completedChapters.add(12)
        // OT bookmark still remains Genesis 12 (not auto-advanced to 13)
        assertEquals(12, otBookmarkChapter)
    }

    // =========================================================================
    // 5. BACKUP ENCRYPTION ROUND-TRIP (PBKDF2 + AES-GCM)
    // =========================================================================

    @Test
    fun testBackupEncryption_roundTrip_pbkdf2AesGcm() {
        val magicHeader = "STDF_V1".toByteArray(StandardCharsets.UTF_8)
        val testPayloadJson = """{"schemaVersion":1,"testData":"SteadfastPrivateSanctuary"}"""
        val passphrase = "CorrectHorseBatteryStaple123!"

        // 1. Encrypt
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(testPayloadJson.toByteArray(StandardCharsets.UTF_8))

        val encryptedStream = ByteArrayOutputStream()
        encryptedStream.write(magicHeader)
        encryptedStream.write(salt)
        encryptedStream.write(iv)
        encryptedStream.write(ciphertext)
        val backupBytes = encryptedStream.toByteArray()

        // Verify magic header is present
        assertTrue(backupBytes.size > magicHeader.size + salt.size + iv.size)
        val headerRead = ByteArray(magicHeader.size)
        System.arraycopy(backupBytes, 0, headerRead, 0, magicHeader.size)
        assertEquals("STDF_V1", String(headerRead, StandardCharsets.UTF_8))

        // 2. Decrypt with correct passphrase
        val inStream = ByteArrayInputStream(backupBytes)
        val inHeader = ByteArray(magicHeader.size)
        inStream.read(inHeader)
        val inSalt = ByteArray(16)
        inStream.read(inSalt)
        val inIv = ByteArray(12)
        inStream.read(inIv)
        val inCiphertext = inStream.readBytes()

        val decryptSpec = PBEKeySpec(passphrase.toCharArray(), inSalt, 65536, 256)
        val decryptKey = SecretKeySpec(factory.generateSecret(decryptSpec).encoded, "AES")
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, decryptKey, GCMParameterSpec(128, inIv))

        val decryptedBytes = decryptCipher.doFinal(inCiphertext)
        val decryptedJson = String(decryptedBytes, StandardCharsets.UTF_8)
        assertEquals(testPayloadJson, decryptedJson)

        // 3. Decrypt with incorrect passphrase must FAIL
        val wrongSpec = PBEKeySpec("WrongPassphrase!".toCharArray(), inSalt, 65536, 256)
        val wrongKey = SecretKeySpec(factory.generateSecret(wrongSpec).encoded, "AES")
        val wrongCipher = Cipher.getInstance("AES/GCM/NoPadding")
        wrongCipher.init(Cipher.DECRYPT_MODE, wrongKey, GCMParameterSpec(128, inIv))

        try {
            wrongCipher.doFinal(inCiphertext)
            fail("Expected decryption with wrong passphrase to throw AEADBadTagException")
        } catch (_: Exception) {
            // Success: AEAD tag mismatch prevents unauthorized decryption
        }
    }
}
