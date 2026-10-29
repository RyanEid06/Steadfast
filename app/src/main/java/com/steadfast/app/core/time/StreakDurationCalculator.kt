package com.steadfast.app.core.time

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class StreakDuration(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long
) {
    val tier: StreakTier
        get() = when {
            years >= 1 -> StreakTier.YEARS_MONTHS_DAYS_HOURS
            totalDays >= 30 -> StreakTier.MONTHS_DAYS_HOURS_SECONDS
            else -> StreakTier.DAYS_HOURS_SECONDS
        }
}

enum class StreakTier {
    DAYS_HOURS_SECONDS,
    MONTHS_DAYS_HOURS_SECONDS,
    YEARS_MONTHS_DAYS_HOURS
}

data class TimeUnitDisplay(
    val value: Long,
    val label: String
)

object StreakDurationCalculator {

    fun calculate(
        startEpochMillis: Long,
        startZoneId: String,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): StreakDuration {
        if (nowEpochMillis <= startEpochMillis) {
            return StreakDuration(
                years = 0,
                months = 0,
                days = 0,
                hours = 0,
                minutes = 0,
                seconds = 0,
                totalDays = 0,
                totalHours = 0,
                totalMinutes = 0,
                totalSeconds = 0
            )
        }

        val zone = try {
            ZoneId.of(startZoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val startZdt = Instant.ofEpochMilli(startEpochMillis).atZone(zone)
        val nowZdt = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)

        val totalSeconds = (nowEpochMillis - startEpochMillis) / 1000L
        val totalMinutes = totalSeconds / 60L
        val totalHours = totalSeconds / 3600L
        val totalDays = totalSeconds / 86400L

        var current = startZdt

        // 1. Calculate calendar years
        var years = 0
        while (true) {
            val nextYear = current.plusYears(1)
            if (!nextYear.isAfter(nowZdt)) {
                years++
                current = nextYear
            } else {
                break
            }
        }

        // 2. Calculate calendar months
        var months = 0
        while (true) {
            val nextMonth = current.plusMonths(1)
            if (!nextMonth.isAfter(nowZdt)) {
                months++
                current = nextMonth
            } else {
                break
            }
        }

        // 3. Calculate calendar days
        var days = 0
        while (true) {
            val nextDay = current.plusDays(1)
            if (!nextDay.isAfter(nowZdt)) {
                days++
                current = nextDay
            } else {
                break
            }
        }

        // 4. Calculate exact remaining hours, minutes, seconds
        val remDuration = Duration.between(current, nowZdt)
        val remSeconds = remDuration.seconds.coerceAtLeast(0)
        val hours = (remSeconds / 3600).toInt()
        val minutes = ((remSeconds % 3600) / 60).toInt()
        val seconds = (remSeconds % 60).toInt()

        return StreakDuration(
            years = years,
            months = months,
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalDays = totalDays,
            totalHours = totalHours,
            totalMinutes = totalMinutes,
            totalSeconds = totalSeconds
        )
    }

    /**
     * Formats units for display in the circular streak component.
     * Follows PRODUCT_SPEC.md:
     * - < 30 elapsed days: Days / Hours / Minutes / Seconds (or Days / Hours / Seconds)
     * - >= 30 days and < 1 year: Months / Days / Hours / Seconds
     * - >= 1 year: Years / Months / Days / Hours / Seconds
     */
    fun getDisplayUnits(duration: StreakDuration): List<TimeUnitDisplay> {
        return when (duration.tier) {
            StreakTier.DAYS_HOURS_SECONDS -> {
                listOf(
                    TimeUnitDisplay(duration.days.toLong(), if (duration.days == 1) "day" else "days"),
                    TimeUnitDisplay(duration.hours.toLong(), if (duration.hours == 1) "hour" else "hours"),
                    TimeUnitDisplay(duration.seconds.toLong(), if (duration.seconds == 1) "sec" else "secs")
                )
            }
            StreakTier.MONTHS_DAYS_HOURS_SECONDS -> {
                listOf(
                    TimeUnitDisplay(duration.months.toLong(), if (duration.months == 1) "month" else "months"),
                    TimeUnitDisplay(duration.days.toLong(), if (duration.days == 1) "day" else "days"),
                    TimeUnitDisplay(duration.hours.toLong(), if (duration.hours == 1) "hour" else "hours"),
                    TimeUnitDisplay(duration.seconds.toLong(), if (duration.seconds == 1) "sec" else "secs")
                )
            }
            StreakTier.YEARS_MONTHS_DAYS_HOURS -> {
                listOf(
                    TimeUnitDisplay(duration.years.toLong(), if (duration.years == 1) "year" else "years"),
                    TimeUnitDisplay(duration.months.toLong(), if (duration.months == 1) "month" else "months"),
                    TimeUnitDisplay(duration.days.toLong(), if (duration.days == 1) "day" else "days"),
                    TimeUnitDisplay(duration.hours.toLong(), if (duration.hours == 1) "hour" else "hours"),
                    TimeUnitDisplay(duration.seconds.toLong(), if (duration.seconds == 1) "sec" else "secs")
                )
            }
        }
    }
}
