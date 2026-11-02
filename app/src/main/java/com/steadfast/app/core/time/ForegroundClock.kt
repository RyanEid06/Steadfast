package com.steadfast.app.core.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Foreground UI clock; callers keep Room data as independent Flow state. */
@Composable
fun rememberForegroundTimeMillis(enabled: Boolean = true, intervalMillis: Long = 1_000L): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(System.currentTimeMillis(), lifecycle, enabled) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            do {
                value = System.currentTimeMillis()
                if (!enabled) break
                delay(intervalMillis.coerceAtLeast(1L))
            } while (isActive)
        }
    }
    return now
}

/** Returns the next exact elapsed-day or stored-zone calendar boundary affecting a session duration. */
fun nextStreakChangeEpochMillis(startEpochMillis: Long, startZoneId: String, nowEpochMillis: Long): Long {
    val zone = runCatching { ZoneId.of(startZoneId) }.getOrElse { ZoneId.systemDefault() }
    val start = Instant.ofEpochMilli(startEpochMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)

    val elapsedBoundary = if (nowEpochMillis < startEpochMillis) {
        startEpochMillis
    } else {
        startEpochMillis + ((nowEpochMillis - startEpochMillis) / 86_400_000L + 1L) * 86_400_000L
    }

    var current = start
    while (!current.plusYears(1).isAfter(now)) current = current.plusYears(1)
    val nextYear = current.plusYears(1)
    while (!current.plusMonths(1).isAfter(now)) current = current.plusMonths(1)
    val nextMonth = current.plusMonths(1)
    while (!current.plusDays(1).isAfter(now)) current = current.plusDays(1)
    val nextDay = current.plusDays(1)

    return minOf(
        elapsedBoundary,
        nextYear.toInstant().toEpochMilli(),
        nextMonth.toInstant().toEpochMilli(),
        nextDay.toInstant().toEpochMilli()
    )
}

/** Foreground clock for threshold-driven UI; it wakes only at duration boundaries. */
@Composable
fun rememberForegroundBoundaryTimeMillis(
    startEpochMillis: Long?,
    startZoneId: String?,
    enabled: Boolean = startEpochMillis != null && startZoneId != null,
    clock: Clock = Clock.systemUTC()
): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(clock.millis(), lifecycle, startEpochMillis, startZoneId, enabled, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val current = clock.millis()
                value = current
                val start = startEpochMillis
                val zone = startZoneId
                if (!enabled || start == null || zone == null) break
                val boundary = nextStreakChangeEpochMillis(start, zone, current)
                delay((boundary - current).coerceAtLeast(1L))
            }
        }
    }
    return now
}
