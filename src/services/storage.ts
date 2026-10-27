import {
  StreakSession,
  JournalEntry,
  BibleChapterCompletion,
  ReadingLocation,
  ThemeSetting,
  MilestoneDefinition,
  StreakDuration,
  MilestoneStatus,
} from '../types'
import { MILESTONES, BIBLE_BOOKS, ALL_CURATED_VERSES } from '../data/seedData'
import { calculateStreakDuration } from '../utils/streakCalculator'

const STORAGE_KEYS = {
  SESSIONS: 'steadfast_sessions',
  JOURNAL: 'steadfast_journal',
  BIBLE_COMPLETIONS: 'steadfast_bible_completions',
  ONBOARDING_COMPLETE: 'steadfast_onboarding_complete',
  THEME_MODE: 'steadfast_theme_mode',
  APP_LOCK_ENABLED: 'steadfast_app_lock_enabled',
  APP_LOCK_PIN: 'steadfast_app_lock_pin',
  RECOVERY_FOCUS_ID: 'steadfast_recovery_focus_id',
  READING_GOAL: 'steadfast_reading_goal',
  OT_CURRENT: 'steadfast_ot_current',
  NT_CURRENT: 'steadfast_nt_current',
  SHOWN_MILESTONES: 'steadfast_shown_milestones',
}

class StorageService {
  private listeners: Set<() => void> = new Set()

  subscribe(listener: () => void) {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  private notify() {
    this.listeners.forEach((cb) => cb())
  }

  // --- Sessions ---
  getAllSessions(): StreakSession[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SESSIONS)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  getActiveSession(): StreakSession | null {
    const sessions = this.getAllSessions()
    return sessions.find((s) => !s.endEpochMillis) || null
  }

  getArchivedSessions(): StreakSession[] {
    const sessions = this.getAllSessions()
    return sessions
      .filter((s) => !!s.endEpochMillis)
      .sort((a, b) => (b.endEpochMillis || 0) - (a.endEpochMillis || 0))
  }

  startInitialSession(focusId: string, startMillis: number, zoneId: string = 'UTC'): StreakSession {
    const sessions = this.getAllSessions()
    const newSession: StreakSession = {
      id: Date.now(),
      recoveryFocusId: focusId,
      startEpochMillis: startMillis,
      startZoneId: zoneId,
      createdAtEpochMillis: Date.now(),
    }
    const updated = [newSession, ...sessions]
    localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(updated))
    this.notify()
    return newSession
  }

  restartStreak(newStartEpochMillis: number = Date.now(), optionalNote?: string) {
    const sessions = this.getAllSessions()
    const active = sessions.find((s) => !s.endEpochMillis)
    const now = Date.now()

    if (active) {
      active.endEpochMillis = newStartEpochMillis
    }

    const newSession: StreakSession = {
      id: now,
      recoveryFocusId: active ? active.recoveryFocusId : this.getRecoveryFocusId(),
      startEpochMillis: newStartEpochMillis,
      startZoneId: active ? active.startZoneId : 'UTC',
      createdAtEpochMillis: now,
    }

    const updated = [newSession, ...sessions]
    localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(updated))

    if (optionalNote && optionalNote.trim()) {
      this.addJournalEntry(optionalNote.trim(), 'RESTART', newSession.id)
    }

    this.notify()
  }

  updateSessionStart(id: number, newStartMillis: number) {
    const sessions = this.getAllSessions()
    const session = sessions.find((s) => s.id === id)
    if (session) {
      session.startEpochMillis = newStartMillis
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions))
      this.notify()
    }
  }

  updateActiveSessionFocus(newFocusId: string) {
    const sessions = this.getAllSessions()
    const active = sessions.find((s) => !s.endEpochMillis)
    if (active) {
      active.recoveryFocusId = newFocusId
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions))
    }
    this.setRecoveryFocusId(newFocusId)
    this.notify()
  }

  calculateLongestStreakDays(sessions?: StreakSession[]): number {
    const list = sessions || this.getAllSessions()
    if (list.length === 0) return 0

    let maxSeconds = 0
    const now = Date.now()

    for (const session of list) {
      const end = session.endEpochMillis ?? now
      const duration = (end - session.startEpochMillis) / 1000
      if (duration > maxSeconds) {
        maxSeconds = duration
      }
    }

    return Math.floor(maxSeconds / 86400)
  }

  // --- Journal Entries ---
  getJournalEntries(): JournalEntry[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.JOURNAL)
      const list: JournalEntry[] = data ? JSON.parse(data) : []
      return list.sort((a, b) => b.createdAtEpochMillis - a.createdAtEpochMillis)
    } catch {
      return []
    }
  }

  addJournalEntry(body: string, context?: 'GENERAL' | 'RESTART' | 'URGE' | null, streakSessionId?: number | null): JournalEntry {
    const entries = this.getJournalEntries()
    const now = Date.now()
    const newEntry: JournalEntry = {
      id: now,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      body,
      context: context ?? 'GENERAL',
      streakSessionId: streakSessionId ?? this.getActiveSession()?.id ?? null,
    }
    entries.unshift(newEntry)
    localStorage.setItem(STORAGE_KEYS.JOURNAL, JSON.stringify(entries))
    this.notify()
    return newEntry
  }

  updateJournalEntry(id: number, body: string, context?: 'GENERAL' | 'RESTART' | 'URGE' | null) {
    const entries = this.getJournalEntries()
    const index = entries.findIndex((e) => e.id === id)
    if (index !== -1) {
      entries[index].body = body
      if (context !== undefined) entries[index].context = context
      entries[index].updatedAtEpochMillis = Date.now()
      localStorage.setItem(STORAGE_KEYS.JOURNAL, JSON.stringify(entries))
      this.notify()
    }
  }

  deleteJournalEntry(id: number) {
    const entries = this.getJournalEntries().filter((e) => e.id !== id)
    localStorage.setItem(STORAGE_KEYS.JOURNAL, JSON.stringify(entries))
    this.notify()
  }

  // --- Bible Completions ---
  getBibleCompletions(): Set<string> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.BIBLE_COMPLETIONS)
      return data ? new Set(JSON.parse(data)) : new Set()
    } catch {
      return new Set()
    }
  }

  toggleChapter(bookId: number, chapter: number): boolean {
    const completions = this.getBibleCompletions()
    const key = `${bookId}-${chapter}`
    let isCompleted = false
    if (completions.has(key)) {
      completions.delete(key)
      isCompleted = false
    } else {
      completions.add(key)
      isCompleted = true
    }
    localStorage.setItem(STORAGE_KEYS.BIBLE_COMPLETIONS, JSON.stringify(Array.from(completions)))
    this.notify()
    return isCompleted
  }

  toggleWholeBook(bookId: number, totalChapters: number) {
    const completions = this.getBibleCompletions()
    let allDone = true
    for (let ch = 1; ch <= totalChapters; ch++) {
      if (!completions.has(`${bookId}-${ch}`)) {
        allDone = false
        break
      }
    }
    if (allDone) {
      for (let ch = 1; ch <= totalChapters; ch++) {
        completions.delete(`${bookId}-${ch}`)
      }
    } else {
      for (let ch = 1; ch <= totalChapters; ch++) {
        completions.add(`${bookId}-${ch}`)
      }
    }
    localStorage.setItem(STORAGE_KEYS.BIBLE_COMPLETIONS, JSON.stringify(Array.from(completions)))
    this.notify()
  }

  getBibleProgress() {
    const completions = this.getBibleCompletions()
    const totalChapters = 1189
    const otTotal = 929
    const ntTotal = 260
    let completedTotal = 0
    let completedOt = 0
    let completedNt = 0
    let completedBooksCount = 0

    BIBLE_BOOKS.forEach((book) => {
      let bookDone = 0
      for (let ch = 1; ch <= book.chapters; ch++) {
        if (completions.has(`${book.id}-${ch}`)) {
          bookDone++
          completedTotal++
          if (book.testament === 'OT') completedOt++
          else completedNt++
        }
      }
      if (bookDone === book.chapters) {
        completedBooksCount++
      }
    })

    return {
      completedChapters: completedTotal,
      totalChapters,
      overallPercentage: Math.round((completedTotal / totalChapters) * 1000) / 10,
      otCompletedChapters: completedOt,
      otTotalChapters: otTotal,
      otPercentage: Math.round((completedOt / otTotal) * 1000) / 10,
      ntCompletedChapters: completedNt,
      ntTotalChapters: ntTotal,
      ntPercentage: Math.round((completedNt / ntTotal) * 1000) / 10,
      completedBooks: completedBooksCount,
      totalBooks: 66,
    }
  }

  getBookCompletedChapters(bookId: number): Set<number> {
    const completions = this.getBibleCompletions()
    const set = new Set<number>()
    const book = BIBLE_BOOKS.find((b) => b.id === bookId)
    if (!book) return set
    for (let ch = 1; ch <= book.chapters; ch++) {
      if (completions.has(`${bookId}-${ch}`)) {
        set.add(ch)
      }
    }
    return set
  }

  // --- Preferences ---
  isOnboardingComplete(): boolean {
    return localStorage.getItem(STORAGE_KEYS.ONBOARDING_COMPLETE) === 'true'
  }

  setOnboardingComplete(complete: boolean) {
    localStorage.setItem(STORAGE_KEYS.ONBOARDING_COMPLETE, complete ? 'true' : 'false')
    this.notify()
  }

  getThemeSetting(): ThemeSetting {
    return (localStorage.getItem(STORAGE_KEYS.THEME_MODE) as ThemeSetting) || 'SYSTEM'
  }

  setThemeSetting(theme: ThemeSetting) {
    localStorage.setItem(STORAGE_KEYS.THEME_MODE, theme)
    this.notify()
  }

  isAppLockEnabled(): boolean {
    return localStorage.getItem(STORAGE_KEYS.APP_LOCK_ENABLED) === 'true'
  }

  setAppLockEnabled(enabled: boolean) {
    localStorage.setItem(STORAGE_KEYS.APP_LOCK_ENABLED, enabled ? 'true' : 'false')
    this.notify()
  }

  getAppLockPin(): string {
    return localStorage.getItem(STORAGE_KEYS.APP_LOCK_PIN) || ''
  }

  setAppLockPin(pin: string) {
    localStorage.setItem(STORAGE_KEYS.APP_LOCK_PIN, pin)
    this.notify()
  }

  getRecoveryFocusId(): string {
    return localStorage.getItem(STORAGE_KEYS.RECOVERY_FOCUS_ID) || 'pornography'
  }

  setRecoveryFocusId(id: string) {
    localStorage.setItem(STORAGE_KEYS.RECOVERY_FOCUS_ID, id)
    this.notify()
  }

  getReadingGoal(): number {
    const g = localStorage.getItem(STORAGE_KEYS.READING_GOAL)
    return g ? parseInt(g, 10) : 2
  }

  setReadingGoal(goal: number) {
    localStorage.setItem(STORAGE_KEYS.READING_GOAL, Math.min(5, Math.max(1, goal)).toString())
    this.notify()
  }

  getOtReadingLocation(): ReadingLocation | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.OT_CURRENT)
      return data ? JSON.parse(data) : null
    } catch {
      return null
    }
  }

  setOtReadingLocation(bookId: number | null, chapter: number | null) {
    if (bookId && chapter) {
      localStorage.setItem(STORAGE_KEYS.OT_CURRENT, JSON.stringify({ bookId, chapter }))
    } else {
      localStorage.removeItem(STORAGE_KEYS.OT_CURRENT)
    }
    this.notify()
  }

  getNtReadingLocation(): ReadingLocation | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NT_CURRENT)
      return data ? JSON.parse(data) : null
    } catch {
      return null
    }
  }

  setNtReadingLocation(bookId: number | null, chapter: number | null) {
    if (bookId && chapter) {
      localStorage.setItem(STORAGE_KEYS.NT_CURRENT, JSON.stringify({ bookId, chapter }))
    } else {
      localStorage.removeItem(STORAGE_KEYS.NT_CURRENT)
    }
    this.notify()
  }

  getShownMilestones(): Set<string> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SHOWN_MILESTONES)
      return data ? new Set(JSON.parse(data)) : new Set()
    } catch {
      return new Set()
    }
  }

  markMilestoneShown(id: string) {
    const set = this.getShownMilestones()
    set.add(id)
    localStorage.setItem(STORAGE_KEYS.SHOWN_MILESTONES, JSON.stringify(Array.from(set)))
    this.notify()
  }

  // --- Milestones Evaluation ---
  getMilestoneTargetDays(milestone: MilestoneDefinition): number {
    switch (milestone.kind) {
      case 'elapsed_days':
        return milestone.value
      case 'calendar_months':
        return milestone.value * 30
      case 'calendar_years':
        return milestone.value * 365
    }
  }

  evaluateMilestones(duration: StreakDuration): MilestoneStatus[] {
    const list: MilestoneStatus[] = []
    let foundCurrent = false

    for (const m of MILESTONES) {
      let isReached = false
      if (m.kind === 'elapsed_days') {
        isReached = duration.totalDays >= m.value
      } else if (m.kind === 'calendar_months') {
        isReached = duration.years * 12 + duration.months >= m.value
      } else if (m.kind === 'calendar_years') {
        isReached = duration.years >= m.value
      }

      const targetDays = this.getMilestoneTargetDays(m)
      const remainingDays = Math.max(0, targetDays - duration.totalDays)
      const progressPercent = Math.min(100, Math.round((duration.totalDays / targetDays) * 100))

      const isCurrent = !isReached && !foundCurrent
      if (isCurrent) foundCurrent = true

      list.push({
        milestone: m,
        isReached,
        isCurrent,
        progressPercent,
        remainingDays,
      })
    }

    return list
  }

  getNextMilestone(duration: StreakDuration): MilestoneDefinition | null {
    const statuses = this.evaluateMilestones(duration)
    return statuses.find((s) => s.isCurrent)?.milestone || null
  }

  getPendingCelebration(duration: StreakDuration): MilestoneDefinition | null {
    const shown = this.getShownMilestones()
    const statuses = this.evaluateMilestones(duration)
    for (const s of statuses) {
      if (s.isReached && !shown.has(s.milestone.id)) {
        return s.milestone
      }
    }
    return null
  }

  // --- Curated Verses with Anti-Repeat ---
  private recentVerseRefs: string[] = []

  getNextVerse(): (typeof ALL_CURATED_VERSES)[0] {
    const completions = this.getBibleCompletions()

    // Find verses belonging to completed chapters
    const completedVerses = ALL_CURATED_VERSES.filter((v) => {
      const book = BIBLE_BOOKS.find((b) => b.name.toLowerCase() === v.bookName.toLowerCase())
      return book && completions.has(`${book.id}-${v.chapter}`)
    }).map((v) => ({ ...v, isFromCompletedChapter: true }))

    const eligibleCompleted = completedVerses.filter(
      (v) => !this.recentVerseRefs.includes(v.reference)
    )

    let selected =
      eligibleCompleted.length > 0
        ? eligibleCompleted[Math.floor(Math.random() * eligibleCompleted.length)]
        : null

    if (!selected) {
      const eligibleGlobal = ALL_CURATED_VERSES.filter(
        (v) => !this.recentVerseRefs.includes(v.reference)
      )
      selected =
        eligibleGlobal.length > 0
          ? eligibleGlobal[Math.floor(Math.random() * eligibleGlobal.length)]
          : ALL_CURATED_VERSES[Math.floor(Math.random() * ALL_CURATED_VERSES.length)]
    }

    this.recentVerseRefs.push(selected.reference)
    if (this.recentVerseRefs.length > 15) {
      this.recentVerseRefs.shift()
    }

    return selected
  }

  getRandomDailyEncouragement() {
    return ALL_CURATED_VERSES[Math.floor(Math.random() * ALL_CURATED_VERSES.length)]
  }

  // --- Backup & Restore ---
  exportBackupData() {
    return JSON.stringify(
      {
        version: 1,
        exportedAt: new Date().toISOString(),
        sessions: this.getAllSessions(),
        journal: this.getJournalEntries(),
        bibleCompletions: Array.from(this.getBibleCompletions()),
        otReading: this.getOtReadingLocation(),
        ntReading: this.getNtReadingLocation(),
        readingGoal: this.getReadingGoal(),
        recoveryFocusId: this.getRecoveryFocusId(),
      },
      null,
      2
    )
  }

  importBackupData(jsonString: string): boolean {
    try {
      const data = JSON.parse(jsonString)
      if (data.sessions) localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(data.sessions))
      if (data.journal) localStorage.setItem(STORAGE_KEYS.JOURNAL, JSON.stringify(data.journal))
      if (data.bibleCompletions)
        localStorage.setItem(STORAGE_KEYS.BIBLE_COMPLETIONS, JSON.stringify(data.bibleCompletions))
      if (data.otReading)
        localStorage.setItem(STORAGE_KEYS.OT_CURRENT, JSON.stringify(data.otReading))
      if (data.ntReading)
        localStorage.setItem(STORAGE_KEYS.NT_CURRENT, JSON.stringify(data.ntReading))
      if (data.readingGoal) this.setReadingGoal(data.readingGoal)
      if (data.recoveryFocusId) this.setRecoveryFocusId(data.recoveryFocusId)
      this.notify()
      return true
    } catch {
      return false
    }
  }

  // Demo seed data if app starts fresh
  seedInitialDataIfEmpty() {
    if (!this.isOnboardingComplete() && this.getAllSessions().length === 0) {
      // 3 days ago for initial sample
      const threeDaysAgo = Date.now() - 3 * 24 * 3600 * 1000 - 4 * 3600 * 1000 - 15 * 60 * 1000
      this.startInitialSession('pornography', threeDaysAgo)
      this.setReadingGoal(2)
      this.setOtReadingLocation(1, 1) // Genesis 1
      this.setNtReadingLocation(40, 1) // Matthew 1
      this.toggleChapter(1, 1)
      this.toggleChapter(1, 2)
      this.toggleChapter(40, 1)
      this.addJournalEntry(
        'Began my journey of steadfast renewal. By God’s grace, taking it one day at a time.',
        'GENERAL'
      )
      this.setOnboardingComplete(true)
    }
  }
}

export const storage = new StorageService()
