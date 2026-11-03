package com.steadfast.app

import com.steadfast.app.data.model.StreakSessionEntity
import com.steadfast.app.ui.journey.JourneyTimelinePoint
import com.steadfast.app.ui.journey.journeyTimelineGeometry
import org.junit.Assert.*
import org.junit.Test

class JourneyTimelineGeometryTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private fun session(id: Long, start: Long, end: Long? = null) = StreakSessionEntity(
        id = id, recoveryFocusId = "gaming", startEpochMillis = start,
        startZoneId = "UTC", endEpochMillis = end
    )

    @Test fun sparseActiveHistoryGrowsFromItsActualStartToNow() {
        val geometry = journeyTimelineGeometry(listOf(session(1, day)), 3 * day, 280f)
        assertEquals(listOf(JourneyTimelinePoint(day, 0), JourneyTimelinePoint(3 * day, 2 * day)), geometry.strokes.single().points)
        assertEquals(280f, geometry.contentWidthDp, 0f)
        assertFalse(geometry.scrolls)
        assertEquals(0f, geometry.xFraction(day), 0f)
        assertEquals(1f, geometry.xFraction(3 * day), 0f)
    }

    @Test fun resetsDropVerticallyAtTheActualRestartTimestamp() {
        val geometry = journeyTimelineGeometry(listOf(session(2, 2 * day), session(1, 0, 2 * day)), 3 * day, 280f)
        assertEquals(listOf(1L, 2L), geometry.strokes.map { it.sessionId })
        assertEquals(listOf(JourneyTimelinePoint(0, 0), JourneyTimelinePoint(2 * day, 2 * day), JourneyTimelinePoint(2 * day, 0)), geometry.strokes.first().points)
        assertEquals(geometry.strokes.first().points.last(), geometry.strokes.last().points.first())
    }

    @Test fun gapsKeepTheirElapsedTimeInsteadOfBecomingEqualSessionSlots() {
        val geometry = journeyTimelineGeometry(listOf(session(1, 0, day), session(2, 9 * day)), 10 * day, 280f)
        assertEquals(0.1f, geometry.xFraction(day), 0.00001f)
        assertEquals(0.9f, geometry.xFraction(9 * day), 0.00001f)
        assertEquals(2, geometry.strokes.size)
    }

    @Test fun sameDayResetsAndShortDurationsRetainTimestampPrecision() {
        val start = 1_800_000_000_000L
        val geometry = journeyTimelineGeometry(listOf(session(1, start, start + hour), session(2, start + hour)), start + 4 * hour, 280f)
        assertEquals(0.25f, geometry.xFraction(start + hour), 0.00001f)
        assertEquals(3 * hour, geometry.strokes.last().points.last().elapsedMillis)
        assertTrue(geometry.timeTicks.zipWithNext().all { (a, b) -> b > a })
        assertFalse(geometry.scrolls)
    }

    @Test fun multipleYearsStayScrollableAndNoSessionsAreTruncated() {
        val sessions = (0L until 40).map { session(it, it * 30 * day, (it + 1) * 30 * day) }
        val geometry = journeyTimelineGeometry(sessions.reversed(), 1200 * day, 280f)
        assertEquals(40, geometry.strokes.size)
        assertEquals(14_400f, geometry.contentWidthDp, 0f)
        assertTrue(geometry.scrolls)
        assertEquals(0.5f, geometry.xFraction(600 * day), 0.00001f)
    }

    @Test fun scrollingBeginsOnlyWhenTwelveDpPerDayExceedsTheAvailableWidth() {
        assertFalse(journeyTimelineGeometry(listOf(session(1, 0)), 20 * day, 240f).scrolls)
        assertTrue(journeyTimelineGeometry(listOf(session(1, 0)), 20 * day + hour, 240f).scrolls)
    }

    @Test fun emptyAndZeroLengthHistoriesHaveFiniteAxes() {
        val empty = journeyTimelineGeometry(emptyList(), 0, 240f)
        val instant = journeyTimelineGeometry(listOf(session(1, day)), day, 240f)
        assertTrue(empty.strokes.isEmpty())
        assertTrue(instant.endEpochMillis > instant.startEpochMillis)
        assertTrue(instant.maximumElapsedMillis > 0)
        assertEquals(0f, instant.xFraction(day), 0f)
    }

    @Test fun epochStartHistoryCapsCanvasWidthWithoutChangingChronologicalFractions() {
        val now = java.time.Instant.parse("2026-11-02T00:00:00Z").toEpochMilli()
        val geometry = journeyTimelineGeometry(listOf(session(1, 0)), now, 280f)
        assertEquals(60_000f, geometry.contentWidthDp, 0f)
        assertTrue(geometry.scrolls)
        assertEquals(0.5f, geometry.xFraction(now / 2), 0.00001f)
        val dense = journeyTimelineGeometry(listOf(session(1, 0)), now, 280f, maxContentWidthDp = 60_000f / 2.625f)
        assertEquals(60_000f, dense.contentWidthDp * 2.625f, 0.01f)
        assertTrue(dense.timeTicks.size < geometry.timeTicks.size)
        assertTrue(dense.timeTicks.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun contentWidthCapNeverMakesThePlotNarrowerThanItsViewport() {
        val geometry = journeyTimelineGeometry(listOf(session(1, 0)), day, 280f, maxContentWidthDp = 100f)
        assertEquals(280f, geometry.contentWidthDp, 0f)
        assertFalse(geometry.scrolls)
    }
}
