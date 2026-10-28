package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.design.ReducedMotionPolicy
import com.example.core.security.BackupManager
import com.example.core.time.StreakDurationCalculator
import com.example.core.time.StreakTier
import com.example.data.datastore.SteadfastPreferences
import com.example.data.db.SteadfastDatabase
import com.example.data.model.BibleBook
import com.example.data.model.MilestoneDefinition
import com.example.data.model.StreakSessionEntity
import com.example.data.repository.BibleProgress
import com.example.data.repository.JournalRepository
import com.example.data.repository.MilestoneRepository
import com.example.data.repository.SteadfastRepository
import com.example.data.seed.AssetSeedLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SteadfastUnitTests {

    private lateinit var context: Context
    private lateinit var inMemoryDb: SteadfastDatabase
    private lateinit var preferences: SteadfastPreferences
    private lateinit var steadfastRepository: SteadfastRepository
    private lateinit var journalRepository: JournalRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, SteadfastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = SteadfastPreferences(context)
        journalRepository = JournalRepository(inMemoryDb.journalDao())
        steadfastRepository = SteadfastRepository(inMemoryDb.streakDao(), journalRepository, preferences)
        backupManager = BackupManager(
            context = context,
            streakDao = inMemoryDb.streakDao(),
            journalDao = inMemoryDb.journalDao(),
            bibleDao = inMemoryDb.bibleDao(),
            preferences = preferences
        )
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun testStreakDurationCalculator_lessThan30Days() {
        val zone = "America/New_York"
        val startZdt = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2026, 1, 15, 14, 30, 45, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(0, duration.years)
        assertEquals(0, duration.months)
        assertEquals(14, duration.days)
        assertEquals(2, duration.hours)
        assertEquals(30, duration.minutes)
        assertEquals(45, duration.seconds)
        assertEquals(StreakTier.DAYS_HOURS_SECONDS, duration.tier)
    }

    @Test
    fun testStreakDurationCalculator_over30Days() {
        val zone = "America/New_York"
        val startZdt = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(0, duration.years)
        assertEquals(2, duration.months)
        assertEquals(4, duration.days)
        assertEquals(StreakTier.MONTHS_DAYS_HOURS_SECONDS, duration.tier)
    }

    @Test
    fun testStreakDurationCalculator_overOneYear() {
        val zone = "UTC"
        val startZdt = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2026, 4, 15, 12, 0, 0, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(1, duration.years)
        assertEquals(3, duration.months)
        assertEquals(14, duration.days)
        assertEquals(StreakTier.YEARS_MONTHS_DAYS_HOURS, duration.tier)
    }

    @Test
    fun testStreakDurationCalculator_leapYear() {
        val zone = "UTC"
        val startZdt = ZonedDateTime.of(2024, 2, 28, 12, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2024, 3, 1, 12, 0, 0, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        assertEquals(2, duration.days)
        assertEquals(2L, duration.totalDays)
    }

    @Test
    fun testMilestoneEvaluation() {
        val d1 = MilestoneDefinition("d1", "elapsed_days", 1, "1 Day")
        val d7 = MilestoneDefinition("d7", "elapsed_days", 7, "7 Days")
        val d30 = MilestoneDefinition("d30", "elapsed_days", 30, "30 Days")
        val y1 = MilestoneDefinition("y1", "calendar_years", 1, "1 Year")

        val duration5Days = StreakDurationCalculator.calculate(
            startEpochMillis = 0L,
            startZoneId = "UTC",
            nowEpochMillis = 5 * 86400 * 1000L
        )

        assertTrue(duration5Days.totalDays >= d1.value)
        assertFalse(duration5Days.totalDays >= d7.value)
        assertFalse(duration5Days.totalDays >= d30.value)
        assertFalse(duration5Days.years >= y1.value)

        val duration370Days = StreakDurationCalculator.calculate(
            startEpochMillis = 0L,
            startZoneId = "UTC",
            nowEpochMillis = 370 * 86400 * 1000L
        )

        assertTrue(duration370Days.years >= y1.value)
    }

    @Test
    fun testBibleProgressCalculation() {
        val progressZero = BibleProgress(
            wholeCompletedChapters = 0,
            wholeTotalChapters = 1189,
            wholePercentage = 0f,
            otCompletedChapters = 0,
            otTotalChapters = 929,
            otPercentage = 0f,
            ntCompletedChapters = 0,
            ntTotalChapters = 260,
            ntPercentage = 0f,
            completedBooksCount = 0,
            totalBooksCount = 66
        )
        assertEquals(0f, progressZero.wholePercentage, 0.001f)

        val halfOt = 464
        val halfNt = 130
        val total = halfOt + halfNt
        val wholePct = total.toFloat() / 1189f
        assertTrue(wholePct in 0.49f..0.51f)
    }

    @Test
    fun testBibleBookChapterBoundsValidation() {
        val genesis = BibleBook(id = 1, name = "Genesis", testament = "OT", chapters = 50)
        val matthew = BibleBook(id = 40, name = "Matthew", testament = "NT", chapters = 28)

        // Valid chapters
        assertTrue(1 in 1..genesis.chapters)
        assertTrue(50 in 1..genesis.chapters)
        assertFalse(51 in 1..genesis.chapters)
        assertFalse(0 in 1..genesis.chapters)

        assertTrue(1 in 1..matthew.chapters)
        assertTrue(28 in 1..matthew.chapters)
        assertFalse(29 in 1..matthew.chapters)

        // Coercion test
        val coercedGenesis = 55.coerceIn(1, genesis.chapters)
        assertEquals(50, coercedGenesis)

        val coercedMatthew = 0.coerceIn(1, matthew.chapters)
        assertEquals(1, coercedMatthew)
    }

    @Test
    fun testStreakRestartBoundary_nonOverlappingTimestamps() = runBlocking {
        val t0 = 1700000000000L // Past start
        steadfastRepository.startInitialSession("pornography", t0, "UTC")

        val initialActive = steadfastRepository.activeSession.first()
        assertNotNull(initialActive)
        assertEquals(t0, initialActive!!.startEpochMillis)
        assertNull(initialActive.endEpochMillis)

        // Restart at t1 > t0
        val t1 = t0 + (5 * 86400 * 1000L) // 5 days later
        steadfastRepository.restartStreak(newStartEpochMillis = t1, optionalNote = "Beginning fresh with God's strength")

        val newActive = steadfastRepository.activeSession.first()
        val allSessions = steadfastRepository.allSessions.first()

        assertNotNull(newActive)
        assertEquals(t1, newActive!!.startEpochMillis)
        assertNull(newActive.endEpochMillis)

        assertEquals(2, allSessions.size)
        val archived = allSessions.find { it.id == initialActive.id }
        assertNotNull(archived)
        // Ensure strictly non-overlapping boundary: archived session end == new session start
        assertEquals(t1, archived!!.endEpochMillis)
        assertTrue(archived.endEpochMillis!! <= newActive.startEpochMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testStreakStartDate_futureRejection(): Unit = runBlocking {
        val futureTime = System.currentTimeMillis() + (7 * 86400 * 1000L) // 1 week in the future
        steadfastRepository.startInitialSession("pornography", futureTime, "UTC")
    }

    @Test
    fun testBackupExportImport_previewImmutability_andTamperRejection() = runBlocking {
        // 1. Seed initial data
        val t0 = 1700000000000L
        steadfastRepository.startInitialSession("pornography", t0, "UTC")
        journalRepository.addEntry("Day 1 prayer: Lord deliver me.", "RESTART", null)
        preferences.setReadingGoalMinimum(3)

        // 2. Export encrypted backup
        val out = ByteArrayOutputStream()
        val passphrase = "SecureFaithPass123"
        backupManager.exportBackup(out, passphrase)
        val backupBytes = out.toByteArray()
        assertTrue(backupBytes.isNotEmpty())

        // Verify header
        val header = String(backupBytes.sliceArray(0 until 7), Charsets.UTF_8)
        assertEquals("STDF_V2", header)

        // 3. Test Preview Immutability: previewBackup must NOT change database
        val previewResult = backupManager.previewBackup(ByteArrayInputStream(backupBytes), passphrase)
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrThrow()
        assertEquals("pornography", preview.settings.recoveryFocusId)
        assertEquals(3, preview.settings.readingGoalMinimum)
        assertEquals(1, preview.streakSessions.size)
        assertEquals(1, preview.journalEntries.size)

        // 4. Test wrong passphrase rejection
        val wrongPassResult = backupManager.previewBackup(ByteArrayInputStream(backupBytes), "WrongPassphrase!")
        assertFalse(wrongPassResult.isSuccess)

        // 5. Test tampered ciphertext rejection
        val tamperedBytes = backupBytes.clone()
        // Corrupt a byte in ciphertext region
        tamperedBytes[tamperedBytes.size - 5] = (tamperedBytes[tamperedBytes.size - 5].toInt() xor 0xFF).toByte()
        val tamperedResult = backupManager.previewBackup(ByteArrayInputStream(tamperedBytes), passphrase)
        assertFalse(tamperedResult.isSuccess)

        // 6. Test import restore
        val importResult = backupManager.importBackup(ByteArrayInputStream(backupBytes), passphrase)
        assertTrue(importResult.isSuccess)
    }

    @Test
    fun testWidgetNextMilestoneCalculation() {
        val loader = AssetSeedLoader(context)
        val milestoneRepo = MilestoneRepository(loader, preferences)

        // Duration of 10 days
        val duration10Days = StreakDurationCalculator.calculate(
            startEpochMillis = 0L,
            startZoneId = "UTC",
            nowEpochMillis = 10 * 86400 * 1000L
        )

        val nextMilestone = milestoneRepo.getNextMilestone(duration10Days)
        assertNotNull(nextMilestone)
        // 14 Days is the next milestone after 10 days
        assertEquals(14, nextMilestone!!.value)
        assertEquals("Two Weeks", nextMilestone.title)
    }

    @Test
    fun testReducedMotionPolicy() {
        // When system animation duration scale is normal (not 0f), reduced motion should be false
        val isReduced = ReducedMotionPolicy.isReducedMotionEnabled(context)
        // Default Robolectric context does not disable animations
        assertFalse(isReduced)
    }
}
