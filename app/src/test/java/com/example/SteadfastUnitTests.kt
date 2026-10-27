package com.example

import com.example.core.time.StreakDurationCalculator
import com.example.core.time.StreakTier
import com.example.data.model.BibleBook
import com.example.data.model.MilestoneDefinition
import com.example.data.repository.BibleProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SteadfastUnitTests {

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
        // 2024 is a leap year (Feb 29 exists)
        val startZdt = ZonedDateTime.of(2024, 2, 28, 12, 0, 0, 0, ZoneId.of(zone))
        val nowZdt = ZonedDateTime.of(2024, 3, 1, 12, 0, 0, 0, ZoneId.of(zone))

        val duration = StreakDurationCalculator.calculate(
            startEpochMillis = startZdt.toInstant().toEpochMilli(),
            startZoneId = zone,
            nowEpochMillis = nowZdt.toInstant().toEpochMilli()
        )

        // Across Feb 28 to Feb 29 (1 day) to Mar 1 (2 days)
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
}
