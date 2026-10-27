export interface StreakSession {
  id: number
  recoveryFocusId: string
  startEpochMillis: number
  startZoneId: string
  endEpochMillis?: number | null
  createdAtEpochMillis: number
}

export interface JournalEntry {
  id: number
  createdAtEpochMillis: number
  updatedAtEpochMillis: number
  body: string
  context?: 'GENERAL' | 'RESTART' | 'URGE' | null
  streakSessionId?: number | null
}

export interface BibleChapterCompletion {
  bookId: number
  chapter: number
}

export interface BibleBook {
  id: number
  name: string
  testament: 'OT' | 'NT'
  chapters: number
  order: number
}

export interface RecoveryFocus {
  id: string
  label: string
}

export interface MilestoneDefinition {
  id: string
  kind: 'elapsed_days' | 'calendar_months' | 'calendar_years'
  value: number
  title: string
}

export interface MilestoneStatus {
  milestone: MilestoneDefinition
  isReached: boolean
  isCurrent: boolean
  progressPercent: number
  remainingDays: number
}

export interface ScaleComparison {
  id: string
  thresholdDays: number
  title: string
  description: string
  scriptureReference: string
}

export interface CuratedVerse {
  reference: string
  bookName: string
  chapter: number
  verseNumber: string
  theme: string
  text?: string
  isFromCompletedChapter?: boolean
}

export interface ReadingLocation {
  bookId: number
  chapter: number
}

export type ThemeSetting = 'SYSTEM' | 'DARK' | 'LIGHT'

export type ActiveScreen =
  | 'onboarding'
  | 'dashboard'
  | 'journey'
  | 'bible'
  | 'book_detail'
  | 'struggling'
  | 'breathing'
  | 'milestones'
  | 'motivation'
  | 'settings'

export interface StreakDuration {
  years: number
  months: number
  days: number
  hours: number
  minutes: number
  seconds: number
  totalDays: number
  totalHours: number
  totalMinutes: number
  totalSeconds: number
  tier: 'DAYS_HOURS_SECONDS' | 'MONTHS_DAYS_HOURS_SECONDS' | 'YEARS_MONTHS_DAYS_HOURS'
}

export interface TimeUnitDisplay {
  value: number
  label: string
}
