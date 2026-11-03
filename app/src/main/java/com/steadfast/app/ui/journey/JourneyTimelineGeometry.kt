package com.steadfast.app.ui.journey

import com.steadfast.app.data.model.StreakSessionEntity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class JourneyTimelinePoint(val epochMillis: Long, val elapsedMillis: Long)

internal data class JourneyTimelineStroke(
    val sessionId: Long,
    val active: Boolean,
    val points: List<JourneyTimelinePoint>
)

internal data class JourneyTimelineGeometry(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val maximumElapsedMillis: Long,
    val elapsedUnitMillis: Long,
    val elapsedTicks: List<Long>,
    val timeTicks: List<Long>,
    val contentWidthDp: Float,
    val scrolls: Boolean,
    val strokes: List<JourneyTimelineStroke>
) {
    fun xFraction(epochMillis: Long): Float =
        ((epochMillis - startEpochMillis).toDouble() / (endEpochMillis - startEpochMillis)).toFloat()

    fun yFraction(elapsedMillis: Long): Float =
        (elapsedMillis.toDouble() / maximumElapsedMillis).toFloat()
}

internal fun journeyTimelineGeometry(
    sessions: List<StreakSessionEntity>,
    nowEpochMillis: Long,
    viewportWidthDp: Float,
    maxContentWidthDp: Float = 60_000f
): JourneyTimelineGeometry {
    val hour = 3_600_000L
    val day = 24 * hour
    val ordered = sessions.sortedWith(compareBy<StreakSessionEntity> { it.startEpochMillis }.thenBy { it.id })
    val strokes = ordered.map { session ->
        val end = (session.endEpochMillis ?: nowEpochMillis).coerceAtLeast(session.startEpochMillis)
        JourneyTimelineStroke(session.id, session.endEpochMillis == null, buildList {
            add(JourneyTimelinePoint(session.startEpochMillis, 0))
            add(JourneyTimelinePoint(end, end - session.startEpochMillis))
            if (session.endEpochMillis != null) add(JourneyTimelinePoint(end, 0))
        })
    }
    val start = ordered.firstOrNull()?.startEpochMillis ?: nowEpochMillis
    val last = strokes.maxOfOrNull { it.points.last().epochMillis } ?: start
    val end = if (last > start) last else start + hour
    val span = end - start
    // Bound the Canvas for Compose measurement; chronology stays proportional even across decades.
    val width = (span.toDouble() / day * 12).toFloat()
        .coerceIn(viewportWidthDp, maxOf(viewportWidthDp, maxContentWidthDp))
    val maxElapsed = strokes.maxOfOrNull { stroke -> stroke.points.maxOf { it.elapsedMillis } } ?: 0L
    val unitMillis = when {
        maxElapsed >= day -> day
        maxElapsed >= hour -> hour
        maxElapsed >= 60_000 -> 60_000L
        else -> 1_000L
    }
    val roughStep = maxOf(maxElapsed, 1_000L).toDouble() / unitMillis / 3
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val step = (listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= roughStep } * magnitude * unitMillis).toLong().coerceAtLeast(1)
    val divisions = ceil(maxOf(maxElapsed, 1_000L).toDouble() / step).toInt()
    val maximum = step * divisions
    val desiredTickMillis = span.toDouble() / maxOf(1f, width / 100f)
    val intervals = listOf(60_000L, 5 * 60_000L, 15 * 60_000L, hour, 3 * hour, 6 * hour, 12 * hour,
        day, 2 * day, 7 * day, 14 * day, 30 * day, 90 * day, 180 * day, 365 * day)
    val interval = intervals.firstOrNull { it >= desiredTickMillis } ?: (ceil(desiredTickMillis / day).toLong() * day)
    val ticks = buildList {
        add(start)
        var next = start + interval
        while (next < end) { add(next); next += interval }
        if ((end - last()).toDouble() / span * width >= 80) add(end)
    }
    return JourneyTimelineGeometry(start, end, maximum, unitMillis, (0..divisions).map { it * step }, ticks,
        width, width > viewportWidthDp, strokes)
}
