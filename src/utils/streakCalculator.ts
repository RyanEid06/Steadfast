import { StreakDuration, TimeUnitDisplay } from '../types'

export function calculateStreakDuration(
  startEpochMillis: number,
  _startZoneId: string = 'UTC',
  nowEpochMillis: number = Date.now()
): StreakDuration {
  if (nowEpochMillis <= startEpochMillis) {
    return {
      years: 0,
      months: 0,
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0,
      totalDays: 0,
      totalHours: 0,
      totalMinutes: 0,
      totalSeconds: 0,
      tier: 'DAYS_HOURS_SECONDS',
    }
  }

  const totalSeconds = Math.floor((nowEpochMillis - startEpochMillis) / 1000)
  const totalMinutes = Math.floor(totalSeconds / 60)
  const totalHours = Math.floor(totalSeconds / 3600)
  const totalDays = Math.floor(totalSeconds / 86400)

  const startDate = new Date(startEpochMillis)
  const nowDate = new Date(nowEpochMillis)

  // Calendar years calculation
  let years = nowDate.getFullYear() - startDate.getFullYear()
  let tempDate = new Date(startDate)
  tempDate.setFullYear(startDate.getFullYear() + years)
  if (tempDate > nowDate) {
    years--
    tempDate = new Date(startDate)
    tempDate.setFullYear(startDate.getFullYear() + years)
  }

  // Calendar months calculation
  let months = (nowDate.getFullYear() - tempDate.getFullYear()) * 12 + (nowDate.getMonth() - tempDate.getMonth())
  let tempMonthDate = new Date(tempDate)
  tempMonthDate.setMonth(tempDate.getMonth() + months)
  if (tempMonthDate > nowDate) {
    months--
    tempMonthDate = new Date(tempDate)
    tempMonthDate.setMonth(tempDate.getMonth() + months)
  }

  // Calendar days
  const remainingMillis = nowDate.getTime() - tempMonthDate.getTime()
  const days = Math.floor(remainingMillis / (1000 * 60 * 60 * 24))
  const remainingAfterDays = remainingMillis % (1000 * 60 * 60 * 24)

  const hours = Math.floor(remainingAfterDays / (1000 * 60 * 60))
  const minutes = Math.floor((remainingAfterDays % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((remainingAfterDays % (1000 * 60)) / 1000)

  let tier: StreakDuration['tier'] = 'DAYS_HOURS_SECONDS'
  if (years >= 1) {
    tier = 'YEARS_MONTHS_DAYS_HOURS'
  } else if (totalDays >= 30) {
    tier = 'MONTHS_DAYS_HOURS_SECONDS'
  }

  return {
    years,
    months,
    days,
    hours,
    minutes,
    seconds,
    totalDays,
    totalHours,
    totalMinutes,
    totalSeconds,
    tier,
  }
}

export function getDisplayUnits(duration: StreakDuration): TimeUnitDisplay[] {
  switch (duration.tier) {
    case 'DAYS_HOURS_SECONDS':
      return [
        { value: duration.days, label: duration.days === 1 ? 'day' : 'days' },
        { value: duration.hours, label: duration.hours === 1 ? 'hour' : 'hours' },
        { value: duration.minutes, label: duration.minutes === 1 ? 'min' : 'mins' },
        { value: duration.seconds, label: duration.seconds === 1 ? 'sec' : 'secs' },
      ]
    case 'MONTHS_DAYS_HOURS_SECONDS':
      return [
        { value: duration.months, label: duration.months === 1 ? 'month' : 'months' },
        { value: duration.days, label: duration.days === 1 ? 'day' : 'days' },
        { value: duration.hours, label: duration.hours === 1 ? 'hour' : 'hours' },
        { value: duration.seconds, label: duration.seconds === 1 ? 'sec' : 'secs' },
      ]
    case 'YEARS_MONTHS_DAYS_HOURS':
      return [
        { value: duration.years, label: duration.years === 1 ? 'year' : 'years' },
        { value: duration.months, label: duration.months === 1 ? 'month' : 'months' },
        { value: duration.days, label: duration.days === 1 ? 'day' : 'days' },
        { value: duration.hours, label: duration.hours === 1 ? 'hour' : 'hours' },
        { value: duration.seconds, label: duration.seconds === 1 ? 'sec' : 'secs' },
      ]
  }
}
