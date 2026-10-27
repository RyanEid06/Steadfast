import React, { useState, useEffect, useMemo } from 'react'
import {
  Shield,
  BookOpen,
  Compass,
  Flame,
  Settings as SettingsIcon,
  Heart,
  CheckCircle2,
  Circle,
  Calendar,
  RotateCcw,
  Plus,
  Search,
  X,
  ChevronRight,
  ChevronLeft,
  Share2,
  Lock,
  Unlock,
  Download,
  Upload,
  Bookmark,
  Award,
  Sparkles,
  Wind,
  Edit2,
  Trash2,
  Check,
  RefreshCw,
} from 'lucide-react'
import { storage } from './services/storage'
import {
  ActiveScreen,
  BibleBook,
  JournalEntry,
  MilestoneDefinition,
  RecoveryFocus,
  StreakDuration,
} from './types'
import {
  BIBLE_BOOKS,
  MILESTONES,
  RECOVERY_FOCUSES,
  SCALE_COMPARISONS,
} from './data/seedData'
import { calculateStreakDuration, getDisplayUnits } from './utils/streakCalculator'

export default function App() {
  // Navigation & Screen State
  const [activeTab, setActiveTab] = useState<'dashboard' | 'journey' | 'bible'>('dashboard')
  const [journeySubTab, setJourneySubTab] = useState<'journal' | 'milestones' | 'comparisons'>('journal')
  const [selectedBook, setSelectedBook] = useState<BibleBook | null>(null)
  const [isStrugglingOpen, setIsStrugglingOpen] = useState(false)
  const [isSettingsOpen, setIsSettingsOpen] = useState(false)
  const [isRestartModalOpen, setIsRestartModalOpen] = useState(false)
  const [isAddJournalOpen, setIsAddJournalOpen] = useState(false)
  const [isEditingJournal, setIsEditingJournal] = useState<JournalEntry | null>(null)
  const [showTutorial, setShowTutorial] = useState(false)

  // Live Timer State (ticks every second)
  const [nowMillis, setNowMillis] = useState<number>(Date.now())

  // Storage synced state
  const [, setStorageTick] = useState(0)

  // Initialize and subscribe
  useEffect(() => {
    storage.seedInitialDataIfEmpty()
    const unsubscribe = storage.subscribe(() => {
      setStorageTick((t) => t + 1)
    })

    const timer = setInterval(() => {
      setNowMillis(Date.now())
    }, 1000)

    return () => {
      unsubscribe()
      clearInterval(timer)
    }
  }, [])

  const activeSession = storage.getActiveSession()
  const currentFocus = RECOVERY_FOCUSES.find(
    (f) => f.id === (activeSession?.recoveryFocusId || storage.getRecoveryFocusId())
  ) || RECOVERY_FOCUSES[0]

  // Streak calculations
  const streakDuration: StreakDuration = useMemo(() => {
    if (!activeSession) {
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
    return calculateStreakDuration(
      activeSession.startEpochMillis,
      activeSession.startZoneId,
      nowMillis
    )
  }, [activeSession, nowMillis])

  const displayUnits = useMemo(() => getDisplayUnits(streakDuration), [streakDuration])
  const nextMilestone = useMemo(
    () => storage.getNextMilestone(streakDuration),
    [streakDuration]
  )
  const milestoneStatuses = useMemo(
    () => storage.evaluateMilestones(streakDuration),
    [streakDuration]
  )
  const bibleProgress = storage.getBibleProgress()
  const completions = storage.getBibleCompletions()

  // Daily encouragement verse
  const [currentVerse, setCurrentVerse] = useState(() => storage.getNextVerse())

  const handleNextVerse = () => {
    setCurrentVerse(storage.getNextVerse())
  }

  // Journal Search & Filter
  const [journalSearch, setJournalSearch] = useState('')
  const [journalFilter, setJournalFilter] = useState<'ALL' | 'GENERAL' | 'RESTART' | 'URGE'>('ALL')
  const journalEntries = storage.getJournalEntries()
  const filteredJournalEntries = useMemo(() => {
    return journalEntries.filter((entry) => {
      const matchesSearch = entry.body.toLowerCase().includes(journalSearch.toLowerCase())
      const matchesFilter =
        journalFilter === 'ALL' ||
        (journalFilter === 'GENERAL' && (!entry.context || entry.context === 'GENERAL')) ||
        entry.context === journalFilter
      return matchesSearch && matchesFilter
    })
  }, [journalEntries, journalSearch, journalFilter])

  // Active Reading bookmarks
  const otBookmark = storage.getOtReadingLocation()
  const ntBookmark = storage.getNtReadingLocation()

  return (
    <div className="min-h-screen bg-[#0A0E17] text-[#F6F4ED] flex flex-col font-sans selection:bg-[#D4AF37]/30 selection:text-white">
      {/* Top Header Bar */}
      <header className="sticky top-0 z-30 bg-[#0A0E17]/95 backdrop-blur border-b border-[#28344D]/50 px-4 py-3 flex items-center justify-between max-w-2xl mx-auto w-full">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#A68522] flex items-center justify-center shadow-lg shadow-[#D4AF37]/15">
            <Flame className="w-5 h-5 text-[#0A0E17]" />
          </div>
          <div>
            <h1 className="font-cinzel text-lg font-bold tracking-wider text-[#F6F4ED] leading-none">
              STEADFAST
            </h1>
            <span className="text-[10px] font-semibold tracking-wider uppercase text-[#D4AF37]">
              {currentFocus.label}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsSettingsOpen(true)}
            aria-label="Settings"
            className="w-9 h-9 rounded-lg bg-[#121824] border border-[#28344D] text-[#94A0B4] hover:text-[#D4AF37] flex items-center justify-center transition"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-2xl mx-auto w-full px-4 py-5 pb-24 overflow-y-auto">
        {activeTab === 'dashboard' && (
          <DashboardScreen
            activeSession={activeSession}
            streakDuration={streakDuration}
            displayUnits={displayUnits}
            nextMilestone={nextMilestone}
            currentVerse={currentVerse}
            onNextVerse={handleNextVerse}
            onOpenStruggling={() => setIsStrugglingOpen(true)}
            onOpenRestart={() => setIsRestartModalOpen(true)}
          />
        )}

        {activeTab === 'journey' && (
          <JourneyScreen
            journeySubTab={journeySubTab}
            setJourneySubTab={setJourneySubTab}
            journalEntries={filteredJournalEntries}
            allJournalCount={journalEntries.length}
            journalSearch={journalSearch}
            setJournalSearch={setJournalSearch}
            journalFilter={journalFilter}
            setJournalFilter={setJournalFilter}
            onOpenAddJournal={() => setIsAddJournalOpen(true)}
            onEditJournal={(entry) => setIsEditingJournal(entry)}
            onDeleteJournal={(id) => storage.deleteJournalEntry(id)}
            milestoneStatuses={milestoneStatuses}
            longestDays={storage.calculateLongestStreakDays()}
            allSessions={storage.getAllSessions()}
          />
        )}

        {activeTab === 'bible' && (
          <BibleScreen
            progress={bibleProgress}
            completions={completions}
            selectedBook={selectedBook}
            onSelectBook={setSelectedBook}
            otBookmark={otBookmark}
            ntBookmark={ntBookmark}
          />
        )}
      </main>

      {/* Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 bg-[#0A0E17]/95 backdrop-blur border-t border-[#28344D]/60 max-w-2xl mx-auto">
        <div className="grid grid-cols-3 h-16">
          <button
            onClick={() => {
              setActiveTab('dashboard')
              setSelectedBook(null)
            }}
            className={`flex flex-col items-center justify-center gap-1 transition ${
              activeTab === 'dashboard'
                ? 'text-[#D4AF37]'
                : 'text-[#94A0B4] hover:text-[#F6F4ED]'
            }`}
          >
            <Shield className="w-5 h-5" />
            <span className="text-xs font-medium">Dashboard</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('journey')
              setSelectedBook(null)
            }}
            className={`flex flex-col items-center justify-center gap-1 transition ${
              activeTab === 'journey'
                ? 'text-[#D4AF37]'
                : 'text-[#94A0B4] hover:text-[#F6F4ED]'
            }`}
          >
            <Compass className="w-5 h-5" />
            <span className="text-xs font-medium">Journey</span>
          </button>

          <button
            onClick={() => setActiveTab('bible')}
            className={`flex flex-col items-center justify-center gap-1 transition ${
              activeTab === 'bible'
                ? 'text-[#D4AF37]'
                : 'text-[#94A0B4] hover:text-[#F6F4ED]'
            }`}
          >
            <BookOpen className="w-5 h-5" />
            <span className="text-xs font-medium">Bible</span>
          </button>
        </div>
      </nav>

      {/* "I'm Struggling" Modal */}
      {isStrugglingOpen && (
        <StrugglingModal
          onClose={() => setIsStrugglingOpen(false)}
          onAddJournalNote={() => {
            setIsStrugglingOpen(false)
            setIsAddJournalOpen(true)
          }}
        />
      )}

      {/* Settings Modal */}
      {isSettingsOpen && (
        <SettingsModal
          onClose={() => setIsSettingsOpen(false)}
          currentFocusId={currentFocus.id}
          activeSession={activeSession}
          onReplayTutorial={() => {
            setIsSettingsOpen(false)
            setShowTutorial(true)
          }}
        />
      )}

      {/* Restart Streak Modal */}
      {isRestartModalOpen && (
        <RestartStreakModal onClose={() => setIsRestartModalOpen(false)} />
      )}

      {/* Add / Edit Journal Modal */}
      {(isAddJournalOpen || isEditingJournal) && (
        <JournalEditModal
          entry={isEditingJournal}
          onClose={() => {
            setIsAddJournalOpen(false)
            setIsEditingJournal(null)
          }}
        />
      )}

      {/* Tutorial Replay Modal */}
      {showTutorial && <TutorialModal onClose={() => setShowTutorial(false)} />}
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Dashboard Screen
// ----------------------------------------------------------------------------------------
function DashboardScreen({
  activeSession,
  streakDuration,
  displayUnits,
  nextMilestone,
  currentVerse,
  onNextVerse,
  onOpenStruggling,
  onOpenRestart,
}: {
  activeSession: any
  streakDuration: StreakDuration
  displayUnits: { value: number; label: string }[]
  nextMilestone: MilestoneDefinition | null
  currentVerse: any
  onNextVerse: () => void
  onOpenStruggling: () => void
  onOpenRestart: () => void
}) {
  return (
    <div className="space-y-6">
      {/* Circular Streak Counter */}
      <div className="relative flex flex-col items-center justify-center py-6">
        <div className="relative w-64 h-64 sm:w-72 sm:h-72 rounded-full border-4 border-[#28344D] flex flex-col items-center justify-center bg-gradient-to-b from-[#121824] to-[#0A0E17] shadow-2xl gold-glow">
          {/* Subtle decorative inner ring */}
          <div className="absolute inset-3 rounded-full border border-[#D4AF37]/20 pointer-events-none" />

          <span className="text-xs uppercase font-cinzel tracking-widest text-[#94A0B4] mb-1">
            Current Freedom
          </span>

          {/* Primary Big Number */}
          <div className="font-serif text-5xl sm:text-6xl font-bold text-[#F6F4ED] tracking-tight">
            {streakDuration.tier === 'YEARS_MONTHS_DAYS_HOURS'
              ? streakDuration.years
              : streakDuration.tier === 'MONTHS_DAYS_HOURS_SECONDS'
              ? streakDuration.months
              : streakDuration.days}
          </div>

          <span className="font-cinzel text-xs tracking-wider text-[#D4AF37] font-semibold uppercase mt-0.5 mb-3">
            {streakDuration.tier === 'YEARS_MONTHS_DAYS_HOURS'
              ? streakDuration.years === 1 ? 'Year Free' : 'Years Free'
              : streakDuration.tier === 'MONTHS_DAYS_HOURS_SECONDS'
              ? streakDuration.months === 1 ? 'Month Free' : 'Months Free'
              : streakDuration.days === 1 ? 'Day Free' : 'Days Free'}
          </span>

          {/* Sub Units Grid */}
          <div className="flex items-center gap-2 text-center border-t border-[#28344D]/70 pt-2 px-3">
            {displayUnits.map((unit, idx) => (
              <div key={idx} className="flex flex-col items-center px-1">
                <span className="font-mono text-xs sm:text-sm font-semibold text-[#F6F4ED]">
                  {unit.value.toString().padStart(2, '0')}
                </span>
                <span className="text-[9px] uppercase tracking-wider text-[#94A0B4]">
                  {unit.label}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Action button row under circle */}
        <div className="mt-5 flex items-center gap-3 w-full max-w-xs">
          <button
            onClick={onOpenStruggling}
            className="flex-1 py-2.5 px-4 rounded-xl bg-gradient-to-r from-[#C85A48] to-[#9B3929] text-white text-xs font-semibold tracking-wider uppercase flex items-center justify-center gap-1.5 shadow-lg shadow-[#C85A48]/20 hover:brightness-110 active:scale-95 transition"
          >
            <Shield className="w-3.5 h-3.5" />
            I'm Struggling
          </button>
          <button
            onClick={onOpenRestart}
            title="Reset or adjust start time"
            className="py-2.5 px-3 rounded-xl bg-[#121824] border border-[#28344D] text-[#94A0B4] hover:text-[#D4AF37] hover:border-[#D4AF37]/40 active:scale-95 transition text-xs flex items-center gap-1"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Restart
          </button>
        </div>
      </div>

      {/* Next Milestone Card */}
      {nextMilestone && (
        <div className="p-4 rounded-2xl bg-[#121824] border border-[#28344D]/80 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Award className="w-4 h-4 text-[#D4AF37]" />
              <span className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4]">
                Next Milestone
              </span>
            </div>
            <span className="text-xs font-mono font-semibold text-[#D4AF37]">
              {nextMilestone.title}
            </span>
          </div>

          {/* Progress bar */}
          {(() => {
            const targetDays = storage.getMilestoneTargetDays(nextMilestone)
            const remaining = Math.max(0, targetDays - streakDuration.totalDays)
            const pct = Math.min(100, Math.round((streakDuration.totalDays / targetDays) * 100))
            return (
              <div className="space-y-1.5">
                <div className="w-full bg-[#1A2234] rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-gradient-to-r from-[#D4AF37] to-[#E7CA6D] h-2 rounded-full transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                </div>
                <div className="flex justify-between text-[11px] text-[#94A0B4]">
                  <span>{pct}% Completed</span>
                  <span>{remaining} days remaining</span>
                </div>
              </div>
            )
          })()}
        </div>
      )}

      {/* Daily Scripture Card */}
      <div className="p-4 rounded-2xl bg-[#121824] border border-[#28344D]/80 flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-[#D4AF37]" />
            <span className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4]">
              Daily Encouragement
            </span>
          </div>
          <button
            onClick={onNextVerse}
            title="Next verse"
            className="text-xs text-[#94A0B4] hover:text-[#D4AF37] flex items-center gap-1 transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Refresh
          </button>
        </div>

        <p className="font-serif italic text-sm text-[#F6F4ED]/90 leading-relaxed">
          {currentVerse.text}
        </p>

        <div className="flex items-center justify-between pt-1 border-t border-[#28344D]/50 text-xs text-[#D4AF37] font-semibold">
          <span>{currentVerse.reference}</span>
          <span className="text-[10px] uppercase font-sans text-[#94A0B4] px-2 py-0.5 rounded-full bg-[#1A2234]">
            {currentVerse.theme}
          </span>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Journey Screen (Journal, Milestones, Fact Comparisons)
// ----------------------------------------------------------------------------------------
function JourneyScreen({
  journeySubTab,
  setJourneySubTab,
  journalEntries,
  allJournalCount,
  journalSearch,
  setJournalSearch,
  journalFilter,
  setJournalFilter,
  onOpenAddJournal,
  onEditJournal,
  onDeleteJournal,
  milestoneStatuses,
  longestDays,
  allSessions,
}: {
  journeySubTab: 'journal' | 'milestones' | 'comparisons'
  setJourneySubTab: (t: 'journal' | 'milestones' | 'comparisons') => void
  journalEntries: JournalEntry[]
  allJournalCount: number
  journalSearch: string
  setJournalSearch: (s: string) => void
  journalFilter: 'ALL' | 'GENERAL' | 'RESTART' | 'URGE'
  setJournalFilter: (f: 'ALL' | 'GENERAL' | 'RESTART' | 'URGE') => void
  onOpenAddJournal: () => void
  onEditJournal: (entry: JournalEntry) => void
  onDeleteJournal: (id: number) => void
  milestoneStatuses: any[]
  longestDays: number
  allSessions: any[]
}) {
  return (
    <div className="space-y-4">
      {/* Sub tabs */}
      <div className="flex rounded-xl bg-[#121824] p-1 border border-[#28344D]">
        <button
          onClick={() => setJourneySubTab('journal')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition ${
            journeySubTab === 'journal'
              ? 'bg-[#222C42] text-[#F6F4ED] shadow-sm'
              : 'text-[#94A0B4] hover:text-[#F6F4ED]'
          }`}
        >
          Journal ({allJournalCount})
        </button>
        <button
          onClick={() => setJourneySubTab('milestones')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition ${
            journeySubTab === 'milestones'
              ? 'bg-[#222C42] text-[#F6F4ED] shadow-sm'
              : 'text-[#94A0B4] hover:text-[#F6F4ED]'
          }`}
        >
          Milestones & History
        </button>
        <button
          onClick={() => setJourneySubTab('comparisons')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition ${
            journeySubTab === 'comparisons'
              ? 'bg-[#222C42] text-[#F6F4ED] shadow-sm'
              : 'text-[#94A0B4] hover:text-[#F6F4ED]'
          }`}
        >
          Biblical Scale
        </button>
      </div>

      {/* Subtab 1: Journal */}
      {journeySubTab === 'journal' && (
        <div className="space-y-4">
          {/* Search & Filter Header */}
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-[#94A0B4] absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Search journal entries..."
                value={journalSearch}
                onChange={(e) => setJournalSearch(e.target.value)}
                className="w-full bg-[#121824] border border-[#28344D] rounded-xl pl-9 pr-3 py-2 text-xs text-[#F6F4ED] placeholder-[#5D687E] focus:outline-none focus:border-[#D4AF37]"
              />
            </div>
            <button
              onClick={onOpenAddJournal}
              className="py-2 px-3.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-semibold text-xs flex items-center justify-center gap-1.5 shadow-md shadow-[#D4AF37]/10 hover:brightness-110 active:scale-95 transition"
            >
              <Plus className="w-4 h-4" />
              Add Entry
            </button>
          </div>

          {/* Context Filter Chips */}
          <div className="flex gap-1.5 overflow-x-auto pb-1 text-xs">
            {(['ALL', 'GENERAL', 'URGE', 'RESTART'] as const).map((filter) => (
              <button
                key={filter}
                onClick={() => setJournalFilter(filter)}
                className={`px-3 py-1 rounded-full border text-[11px] font-medium transition ${
                  journalFilter === filter
                    ? 'bg-[#D4AF37]/15 border-[#D4AF37] text-[#D4AF37]'
                    : 'bg-[#121824] border-[#28344D] text-[#94A0B4] hover:border-[#5D687E]'
                }`}
              >
                {filter === 'ALL'
                  ? 'All Notes'
                  : filter === 'GENERAL'
                  ? 'General'
                  : filter === 'URGE'
                  ? 'Overcame Urge'
                  : 'Relapse Reflection'}
              </button>
            ))}
          </div>

          {/* Entries List */}
          {journalEntries.length === 0 ? (
            <div className="p-8 text-center rounded-2xl bg-[#121824] border border-[#28344D]/60 space-y-2">
              <p className="text-sm text-[#94A0B4]">No journal entries found.</p>
              <p className="text-xs text-[#5D687E]">
                Record prayers, reflections, and victories as you walk in steadfast purity.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {journalEntries.map((entry) => (
                <div
                  key={entry.id}
                  className="p-4 rounded-2xl bg-[#121824] border border-[#28344D]/70 space-y-2 hover:border-[#5D687E] transition"
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-[#94A0B4] font-mono">
                      {new Date(entry.createdAtEpochMillis).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                        hour: 'numeric',
                        minute: '2-digit',
                      })}
                    </span>
                    <div className="flex items-center gap-2">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase ${
                          entry.context === 'RESTART'
                            ? 'bg-[#C85A48]/20 text-[#C85A48]'
                            : entry.context === 'URGE'
                            ? 'bg-[#D4AF37]/20 text-[#D4AF37]'
                            : 'bg-[#222C42] text-[#94A0B4]'
                        }`}
                      >
                        {entry.context || 'GENERAL'}
                      </span>
                      <button
                        onClick={() => onEditJournal(entry)}
                        className="text-[#94A0B4] hover:text-[#D4AF37] p-1"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => onDeleteJournal(entry.id)}
                        className="text-[#94A0B4] hover:text-[#C85A48] p-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                  <p className="text-sm text-[#F6F4ED]/90 whitespace-pre-wrap leading-relaxed">
                    {entry.body}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Subtab 2: Milestones & History */}
      {journeySubTab === 'milestones' && (
        <div className="space-y-5">
          {/* Stat summary cards */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3.5 rounded-xl bg-[#121824] border border-[#28344D]">
              <span className="text-[11px] uppercase tracking-wider text-[#94A0B4] font-medium">
                Longest Clean Run
              </span>
              <p className="text-2xl font-serif font-bold text-[#D4AF37] mt-0.5">
                {longestDays} <span className="text-xs font-sans text-[#94A0B4]">days</span>
              </p>
            </div>
            <div className="p-3.5 rounded-xl bg-[#121824] border border-[#28344D]">
              <span className="text-[11px] uppercase tracking-wider text-[#94A0B4] font-medium">
                Total Sessions
              </span>
              <p className="text-2xl font-serif font-bold text-[#F6F4ED] mt-0.5">
                {allSessions.length}
              </p>
            </div>
          </div>

          {/* Milestone Ladder */}
          <div className="space-y-2.5">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4]">
              Milestone Pathway
            </h3>
            <div className="space-y-2">
              {milestoneStatuses.map(({ milestone, isReached, isCurrent, remainingDays }) => (
                <div
                  key={milestone.id}
                  className={`p-3 rounded-xl border flex items-center justify-between transition ${
                    isReached
                      ? 'bg-[#121824] border-[#D4AF37]/50 text-[#F6F4ED]'
                      : isCurrent
                      ? 'bg-[#1A2234] border-[#D4AF37] shadow-sm'
                      : 'bg-[#0A0E17] border-[#28344D]/60 text-[#5D687E]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-7 h-7 rounded-full flex items-center justify-center text-xs ${
                        isReached
                          ? 'bg-[#D4AF37] text-[#0A0E17]'
                          : isCurrent
                          ? 'bg-[#D4AF37]/20 text-[#D4AF37] border border-[#D4AF37]'
                          : 'bg-[#121824] text-[#5D687E] border border-[#28344D]'
                      }`}
                    >
                      {isReached ? <Check className="w-4 h-4" /> : <Award className="w-3.5 h-3.5" />}
                    </div>
                    <div>
                      <span className="font-semibold text-xs block text-[#F6F4ED]">
                        {milestone.title}
                      </span>
                      <span className="text-[10px] text-[#94A0B4]">
                        {milestone.value}{' '}
                        {milestone.kind === 'elapsed_days'
                          ? 'days'
                          : milestone.kind === 'calendar_months'
                          ? 'months'
                          : 'years'}
                      </span>
                    </div>
                  </div>

                  <span className="text-xs font-mono font-medium">
                    {isReached ? (
                      <span className="text-[#4E9E67]">Unlocked</span>
                    ) : isCurrent ? (
                      <span className="text-[#D4AF37]">{remainingDays}d to go</span>
                    ) : (
                      <span className="text-[#5D687E]">Locked</span>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Subtab 3: Scale Comparisons */}
      {journeySubTab === 'comparisons' && (
        <div className="space-y-3">
          <p className="text-xs text-[#94A0B4]">
            Ground your progress in spiritual history. See what Scripture accomplishes across these
            same spans of time.
          </p>
          <div className="space-y-3">
            {SCALE_COMPARISONS.map((item) => (
              <div
                key={item.id}
                className="p-4 rounded-2xl bg-[#121824] border border-[#28344D]/80 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <span className="font-cinzel text-xs font-bold text-[#D4AF37] uppercase tracking-wider">
                    {item.thresholdDays} Days — {item.title}
                  </span>
                </div>
                <p className="text-xs text-[#F6F4ED]/90 leading-relaxed">{item.description}</p>
                <p className="text-[11px] font-serif italic text-[#D4AF37]/90 pt-1 border-t border-[#28344D]/40">
                  {item.scriptureReference}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Bible Screen (Progress & Chapters)
// ----------------------------------------------------------------------------------------
function BibleScreen({
  progress,
  completions,
  selectedBook,
  onSelectBook,
  otBookmark,
  ntBookmark,
}: {
  progress: any
  completions: Set<string>
  selectedBook: BibleBook | null
  onSelectBook: (b: BibleBook | null) => void
  otBookmark: any
  ntBookmark: any
}) {
  const [testamentFilter, setTestamentFilter] = useState<'ALL' | 'OT' | 'NT'>('ALL')

  const filteredBooks = useMemo(() => {
    if (testamentFilter === 'ALL') return BIBLE_BOOKS
    return BIBLE_BOOKS.filter((b) => b.testament === testamentFilter)
  }, [testamentFilter])

  // If viewing a single book detail
  if (selectedBook) {
    const bookDoneChapters = storage.getBookCompletedChapters(selectedBook.id)
    const isWholeBookComplete = bookDoneChapters.size === selectedBook.chapters

    return (
      <div className="space-y-4">
        {/* Top return breadcrumb */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => onSelectBook(null)}
            className="flex items-center gap-1.5 text-xs text-[#94A0B4] hover:text-[#D4AF37] transition"
          >
            <ChevronLeft className="w-4 h-4" />
            Back to Books
          </button>
          <button
            onClick={() => storage.toggleWholeBook(selectedBook.id, selectedBook.chapters)}
            className="text-xs text-[#D4AF37] hover:underline font-semibold"
          >
            {isWholeBookComplete ? 'Uncheck All' : 'Mark Whole Book Complete'}
          </button>
        </div>

        {/* Book Header Card */}
        <div className="p-4 rounded-2xl bg-[#121824] border border-[#28344D] flex items-center justify-between">
          <div>
            <h2 className="font-cinzel text-lg font-bold text-[#F6F4ED]">
              {selectedBook.name}
            </h2>
            <span className="text-xs text-[#94A0B4]">
              {selectedBook.testament === 'OT' ? 'Old Testament' : 'New Testament'} •{' '}
              {selectedBook.chapters} Chapters
            </span>
          </div>
          <div className="text-right">
            <span className="font-serif text-xl font-bold text-[#D4AF37]">
              {bookDoneChapters.size}/{selectedBook.chapters}
            </span>
            <span className="block text-[10px] uppercase text-[#94A0B4]">Done</span>
          </div>
        </div>

        {/* Chapter Grid */}
        <div className="grid grid-cols-5 sm:grid-cols-8 gap-2">
          {Array.from({ length: selectedBook.chapters }, (_, i) => i + 1).map((ch) => {
            const isDone = bookDoneChapters.has(ch)
            return (
              <button
                key={ch}
                onClick={() => storage.toggleChapter(selectedBook.id, ch)}
                className={`aspect-square rounded-xl border flex flex-col items-center justify-center font-mono text-xs font-semibold transition active:scale-90 ${
                  isDone
                    ? 'bg-[#D4AF37] text-[#0A0E17] border-[#D4AF37] shadow-sm shadow-[#D4AF37]/20'
                    : 'bg-[#121824] text-[#94A0B4] border-[#28344D] hover:border-[#D4AF37]/50'
                }`}
              >
                {ch}
              </button>
            )
          })}
        </div>
      </div>
    )
  }

  // Overview with Books Grid
  return (
    <div className="space-y-5">
      {/* Overall Progress Banner */}
      <div className="p-4 rounded-2xl bg-[#121824] border border-[#28344D] space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-[10px] uppercase font-semibold tracking-wider text-[#94A0B4]">
              Entire Bible Completed
            </span>
            <p className="font-serif text-2xl font-bold text-[#D4AF37]">
              {progress.completedChapters}{' '}
              <span className="text-sm font-sans font-normal text-[#94A0B4]">/ 1,189 chapters</span>
            </p>
          </div>
          <div className="w-14 h-14 rounded-full border-4 border-[#28344D] flex items-center justify-center bg-[#0A0E17]">
            <span className="font-mono text-xs font-bold text-[#D4AF37]">
              {progress.overallPercentage}%
            </span>
          </div>
        </div>

        {/* OT / NT Bar */}
        <div className="grid grid-cols-2 gap-3 pt-2 border-t border-[#28344D]/60 text-xs">
          <div>
            <span className="text-[#94A0B4]">Old Testament</span>
            <div className="flex items-baseline justify-between mt-0.5">
              <span className="font-semibold text-[#F6F4ED]">
                {progress.otCompletedChapters}/{progress.otTotalChapters}
              </span>
              <span className="text-[10px] text-[#D4AF37] font-mono">{progress.otPercentage}%</span>
            </div>
          </div>
          <div>
            <span className="text-[#94A0B4]">New Testament</span>
            <div className="flex items-baseline justify-between mt-0.5">
              <span className="font-semibold text-[#F6F4ED]">
                {progress.ntCompletedChapters}/{progress.ntTotalChapters}
              </span>
              <span className="text-[10px] text-[#D4AF37] font-mono">{progress.ntPercentage}%</span>
            </div>
          </div>
        </div>
      </div>

      {/* Active Bookmarks */}
      {(otBookmark || ntBookmark) && (
        <div className="flex gap-2">
          {otBookmark && (
            <div className="flex-1 p-3 rounded-xl bg-[#1A2234] border border-[#28344D] flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <Bookmark className="w-3.5 h-3.5 text-[#D4AF37]" />
                <span>
                  OT: {BIBLE_BOOKS.find((b) => b.id === otBookmark.bookId)?.name}{' '}
                  {otBookmark.chapter}
                </span>
              </div>
            </div>
          )}
          {ntBookmark && (
            <div className="flex-1 p-3 rounded-xl bg-[#1A2234] border border-[#28344D] flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <Bookmark className="w-3.5 h-3.5 text-[#D4AF37]" />
                <span>
                  NT: {BIBLE_BOOKS.find((b) => b.id === ntBookmark.bookId)?.name}{' '}
                  {ntBookmark.chapter}
                </span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex gap-2 text-xs">
        {(['ALL', 'OT', 'NT'] as const).map((filter) => (
          <button
            key={filter}
            onClick={() => setTestamentFilter(filter)}
            className={`px-3 py-1.5 rounded-lg border font-medium transition ${
              testamentFilter === filter
                ? 'bg-[#D4AF37]/15 border-[#D4AF37] text-[#D4AF37]'
                : 'bg-[#121824] border-[#28344D] text-[#94A0B4] hover:border-[#5D687E]'
            }`}
          >
            {filter === 'ALL'
              ? 'All 66 Books'
              : filter === 'OT'
              ? 'Old Testament (39)'
              : 'New Testament (27)'}
          </button>
        ))}
      </div>

      {/* Books List Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
        {filteredBooks.map((book) => {
          const done = storage.getBookCompletedChapters(book.id).size
          const isComplete = done === book.chapters
          return (
            <button
              key={book.id}
              onClick={() => onSelectBook(book)}
              className={`p-3 rounded-xl border text-left transition hover:border-[#D4AF37]/50 active:scale-98 ${
                isComplete
                  ? 'bg-[#121824] border-[#D4AF37]/40 text-[#F6F4ED]'
                  : 'bg-[#121824] border-[#28344D] text-[#94A0B4]'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-semibold text-xs text-[#F6F4ED] truncate">{book.name}</span>
                {isComplete && <CheckCircle2 className="w-3.5 h-3.5 text-[#4E9E67]" />}
              </div>
              <div className="flex items-center justify-between text-[11px] text-[#94A0B4]">
                <span>{book.chapters} chs</span>
                <span className="font-mono text-[10px] text-[#D4AF37]">
                  {done}/{book.chapters}
                </span>
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// "I'm Struggling" Modal
// ----------------------------------------------------------------------------------------
function StrugglingModal({
  onClose,
  onAddJournalNote,
}: {
  onClose: () => void
  onAddJournalNote: () => void
}) {
  const [phase, setPhase] = useState<'Inhale' | 'Hold' | 'Exhale'>('Inhale')
  const [secondsLeft, setSecondsLeft] = useState(4)
  const [cycle, setCycle] = useState(1)
  const [verse, setVerse] = useState(() => storage.getNextVerse())

  useEffect(() => {
    const interval = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev > 1) return prev - 1

        // Advance breath phase: 4s Inhale -> 7s Hold -> 8s Exhale
        if (phase === 'Inhale') {
          setPhase('Hold')
          return 7
        } else if (phase === 'Hold') {
          setPhase('Exhale')
          return 8
        } else {
          setPhase('Inhale')
          setCycle((c) => c + 1)
          return 4
        }
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [phase])

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0E17]/90 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-[#121824] border border-[#28344D] w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-6 relative">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 text-[#94A0B4] hover:text-[#F6F4ED]"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="text-center space-y-1">
          <span className="text-xs uppercase font-cinzel tracking-widest text-[#C85A48] font-bold">
            Urge Intervention
          </span>
          <h2 className="font-cinzel text-xl font-bold text-[#F6F4ED]">
            Pause. Breathe. Surrender.
          </h2>
          <p className="text-xs text-[#94A0B4]">
            Urges peak within 15–20 minutes and subside. Christ is with you right now.
          </p>
        </div>

        {/* Breathing Circle Indicator */}
        <div className="flex flex-col items-center justify-center py-2">
          <div
            className={`w-36 h-36 rounded-full border-4 flex flex-col items-center justify-center transition-all duration-1000 ${
              phase === 'Inhale'
                ? 'border-[#D4AF37] scale-110 shadow-lg shadow-[#D4AF37]/20'
                : phase === 'Hold'
                ? 'border-[#E7CA6D] scale-105'
                : 'border-[#94A0B4] scale-95'
            }`}
          >
            <span className="font-cinzel text-sm font-bold uppercase tracking-wider text-[#D4AF37]">
              {phase}
            </span>
            <span className="font-mono text-3xl font-bold text-[#F6F4ED]">{secondsLeft}</span>
            <span className="text-[10px] text-[#94A0B4] uppercase">Cycle {cycle}</span>
          </div>
        </div>

        {/* Emergency Scripture Verse */}
        <div className="p-4 rounded-xl bg-[#0A0E17] border border-[#28344D] space-y-2">
          <p className="font-serif italic text-xs sm:text-sm text-[#F6F4ED] leading-relaxed">
            {verse.text}
          </p>
          <div className="flex items-center justify-between pt-1 border-t border-[#28344D]/40 text-xs text-[#D4AF37] font-semibold">
            <span>{verse.reference}</span>
            <button
              onClick={() => setVerse(storage.getNextVerse())}
              className="text-[#94A0B4] hover:text-[#D4AF37] flex items-center gap-1 text-[11px]"
            >
              <RefreshCw className="w-3 h-3" /> Another
            </button>
          </div>
        </div>

        {/* Actions */}
        <div className="space-y-2">
          <button
            onClick={onAddJournalNote}
            className="w-full py-2.5 rounded-xl bg-[#222C42] border border-[#28344D] text-[#F6F4ED] hover:text-[#D4AF37] font-semibold text-xs transition"
          >
            Write Down What You're Feeling
          </button>
          <button
            onClick={onClose}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-bold text-xs uppercase tracking-wider transition hover:brightness-110"
          >
            I Got Through It
          </button>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Settings Modal
// ----------------------------------------------------------------------------------------
function SettingsModal({
  onClose,
  currentFocusId,
  activeSession,
  onReplayTutorial,
}: {
  onClose: () => void
  currentFocusId: string
  activeSession: any
  onReplayTutorial: () => void
}) {
  const [focusId, setFocusId] = useState(currentFocusId)
  const [readingGoal, setReadingGoal] = useState(() => storage.getReadingGoal())
  const [lockEnabled, setLockEnabled] = useState(() => storage.isAppLockEnabled())

  const handleSave = () => {
    storage.updateActiveSessionFocus(focusId)
    storage.setReadingGoal(readingGoal)
    storage.setAppLockEnabled(lockEnabled)
    onClose()
  }

  const handleExport = () => {
    const data = storage.exportBackupData()
    const blob = new Blob([data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `steadfast_backup_${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (event) => {
      const content = event.target?.result as string
      if (content) {
        const success = storage.importBackupData(content)
        if (success) {
          alert('Backup restored successfully.')
          onClose()
        } else {
          alert('Failed to parse backup file.')
        }
      }
    }
    reader.readAsText(file)
  }

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0E17]/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#121824] border border-[#28344D] w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-[#28344D]">
          <h2 className="font-cinzel text-base font-bold text-[#F6F4ED]">Settings</h2>
          <button onClick={onClose} className="text-[#94A0B4] hover:text-[#F6F4ED]">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Recovery Focus */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4]">
            Recovery Focus
          </label>
          <select
            value={focusId}
            onChange={(e) => setFocusId(e.target.value)}
            className="w-full bg-[#1A2234] border border-[#28344D] rounded-xl px-3 py-2 text-xs text-[#F6F4ED] focus:outline-none focus:border-[#D4AF37]"
          >
            {RECOVERY_FOCUSES.map((f) => (
              <option key={f.id} value={f.id}>
                {f.label}
              </option>
            ))}
          </select>
        </div>

        {/* Daily Reading Goal */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4]">
            Daily Bible Chapters Goal ({readingGoal} / day)
          </label>
          <div className="flex gap-2">
            {[1, 2, 3, 4, 5].map((g) => (
              <button
                key={g}
                onClick={() => setReadingGoal(g)}
                className={`flex-1 py-1.5 rounded-lg border text-xs font-mono font-semibold transition ${
                  readingGoal === g
                    ? 'bg-[#D4AF37] text-[#0A0E17] border-[#D4AF37]'
                    : 'bg-[#1A2234] border-[#28344D] text-[#94A0B4]'
                }`}
              >
                {g}
              </button>
            ))}
          </div>
        </div>

        {/* Privacy & Lock */}
        <div className="p-3.5 rounded-xl bg-[#1A2234] border border-[#28344D] space-y-2">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-xs font-semibold text-[#F6F4ED] block">Privacy Screen Lock</span>
              <span className="text-[11px] text-[#94A0B4]">Require biometric or PIN on open</span>
            </div>
            <button
              onClick={() => setLockEnabled(!lockEnabled)}
              className={`p-1.5 rounded-lg border transition ${
                lockEnabled
                  ? 'bg-[#D4AF37]/20 border-[#D4AF37] text-[#D4AF37]'
                  : 'bg-[#121824] border-[#28344D] text-[#94A0B4]'
              }`}
            >
              {lockEnabled ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Backup & Export */}
        <div className="space-y-2 pt-2 border-t border-[#28344D]">
          <span className="text-xs font-semibold uppercase tracking-wider text-[#94A0B4] block">
            Data Backup (Offline)
          </span>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={handleExport}
              className="py-2 px-3 rounded-xl bg-[#222C42] border border-[#28344D] text-xs font-semibold text-[#F6F4ED] hover:text-[#D4AF37] flex items-center justify-center gap-1.5"
            >
              <Download className="w-3.5 h-3.5" /> Export Data
            </button>
            <label className="py-2 px-3 rounded-xl bg-[#222C42] border border-[#28344D] text-xs font-semibold text-[#F6F4ED] hover:text-[#D4AF37] flex items-center justify-center gap-1.5 cursor-pointer text-center">
              <Upload className="w-3.5 h-3.5" /> Restore Data
              <input type="file" accept=".json" onChange={handleImport} className="hidden" />
            </label>
          </div>
        </div>

        {/* Replay Tutorial */}
        <div className="pt-2 border-t border-[#28344D]">
          <button
            onClick={onReplayTutorial}
            className="w-full py-2 text-xs text-[#94A0B4] hover:text-[#D4AF37] text-center"
          >
            Replay Introduction Tutorial
          </button>
        </div>

        {/* Save button */}
        <div className="pt-2">
          <button
            onClick={handleSave}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-bold text-xs uppercase tracking-wider"
          >
            Save Settings
          </button>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Restart Streak Modal
// ----------------------------------------------------------------------------------------
function RestartStreakModal({ onClose }: { onClose: () => void }) {
  const [reflection, setReflection] = useState('')

  const handleConfirm = () => {
    storage.restartStreak(Date.now(), reflection)
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0E17]/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#121824] border border-[#28344D] w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between pb-2 border-b border-[#28344D]">
          <h2 className="font-cinzel text-base font-bold text-[#F6F4ED]">Reset Freedom Clock</h2>
          <button onClick={onClose} className="text-[#94A0B4] hover:text-[#F6F4ED]">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-[#94A0B4] leading-relaxed">
          Stumbles do not erase God’s grace or the progress you have made. "The righteous falls seven
          times and rises again" (Proverbs 24:16).
        </p>

        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-[#F6F4ED]">
            Optional Reflection (saved to Journal)
          </label>
          <textarea
            rows={3}
            value={reflection}
            onChange={(e) => setReflection(e.target.value)}
            placeholder="What triggered this? What will you do differently next time?..."
            className="w-full bg-[#1A2234] border border-[#28344D] rounded-xl p-3 text-xs text-[#F6F4ED] placeholder-[#5D687E] focus:outline-none focus:border-[#D4AF37]"
          />
        </div>

        <div className="flex gap-2 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl bg-[#1A2234] border border-[#28344D] text-xs font-semibold text-[#94A0B4]"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            className="flex-1 py-2.5 rounded-xl bg-[#C85A48] text-white font-bold text-xs uppercase tracking-wider hover:brightness-110"
          >
            Reset Clock Now
          </button>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Journal Add / Edit Modal
// ----------------------------------------------------------------------------------------
function JournalEditModal({
  entry,
  onClose,
}: {
  entry: JournalEntry | null
  onClose: () => void
}) {
  const [body, setBody] = useState(entry ? entry.body : '')
  const [context, setContext] = useState<'GENERAL' | 'URGE' | 'RESTART'>(
    entry?.context || 'GENERAL'
  )

  const handleSave = () => {
    if (!body.trim()) return
    if (entry) {
      storage.updateJournalEntry(entry.id, body.trim(), context)
    } else {
      storage.addJournalEntry(body.trim(), context)
    }
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0E17]/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#121824] border border-[#28344D] w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between pb-2 border-b border-[#28344D]">
          <h2 className="font-cinzel text-base font-bold text-[#F6F4ED]">
            {entry ? 'Edit Note' : 'New Journal Note'}
          </h2>
          <button onClick={onClose} className="text-[#94A0B4] hover:text-[#F6F4ED]">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Category Pills */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-[#94A0B4] uppercase tracking-wider">
            Context
          </label>
          <div className="flex gap-2">
            {(['GENERAL', 'URGE', 'RESTART'] as const).map((ctx) => (
              <button
                key={ctx}
                onClick={() => setContext(ctx)}
                className={`flex-1 py-1.5 rounded-lg border text-xs font-medium transition ${
                  context === ctx
                    ? 'bg-[#D4AF37]/20 border-[#D4AF37] text-[#D4AF37]'
                    : 'bg-[#1A2234] border-[#28344D] text-[#94A0B4]'
                }`}
              >
                {ctx === 'GENERAL' ? 'General' : ctx === 'URGE' ? 'Urge Victory' : 'Relapse'}
              </button>
            ))}
          </div>
        </div>

        {/* Text Area */}
        <textarea
          rows={6}
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Pour out your heart to God, write your victories, or process triggers..."
          className="w-full bg-[#1A2234] border border-[#28344D] rounded-xl p-3 text-xs text-[#F6F4ED] placeholder-[#5D687E] focus:outline-none focus:border-[#D4AF37] leading-relaxed"
        />

        <div className="flex gap-2 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl bg-[#1A2234] border border-[#28344D] text-xs font-semibold text-[#94A0B4]"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={!body.trim()}
            className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-bold text-xs uppercase tracking-wider disabled:opacity-50"
          >
            Save Note
          </button>
        </div>
      </div>
    </div>
  )
}

// ----------------------------------------------------------------------------------------
// Tutorial Modal (Non-destructive)
// ----------------------------------------------------------------------------------------
function TutorialModal({ onClose }: { onClose: () => void }) {
  const [step, setStep] = useState(0)

  const steps = [
    {
      title: 'Welcome to Steadfast',
      desc: 'Steadfast is your private, 100% offline Christian companion for freedom from compulsive habits and consistent Bible reading.',
    },
    {
      title: 'Grace Over Guilt',
      desc: 'Freedom is built on daily surrender, not perfectionism. Your streak measures consecutive time in the light, supported by daily Scripture.',
    },
    {
      title: 'Active Bible Journey',
      desc: 'Read through God’s Word at your own pace. Track chapters, mark bookmarks in the Old and New Testaments, and deepen your spiritual foundation.',
    },
    {
      title: 'Urge Intervention & Journal',
      desc: 'When tempted, hit "I’m Struggling" for guided breathing and immediate promises from God. Record your prayers and reflections safely on device.',
    },
  ]

  const current = steps[step]

  return (
    <div className="fixed inset-0 z-50 bg-[#0A0E17]/90 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-[#121824] border border-[#28344D] w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <span className="text-xs uppercase font-cinzel tracking-widest text-[#D4AF37] font-bold">
            Step {step + 1} of {steps.length}
          </span>
          <h2 className="font-cinzel text-xl font-bold text-[#F6F4ED]">{current.title}</h2>
          <p className="text-xs text-[#94A0B4] leading-relaxed">{current.desc}</p>
        </div>

        <div className="flex justify-center gap-1.5">
          {steps.map((_, i) => (
            <div
              key={i}
              className={`h-1.5 rounded-full transition-all duration-300 ${
                i === step ? 'w-6 bg-[#D4AF37]' : 'w-1.5 bg-[#28344D]'
              }`}
            />
          ))}
        </div>

        <div className="flex gap-2">
          {step > 0 && (
            <button
              onClick={() => setStep(step - 1)}
              className="py-2.5 px-4 rounded-xl bg-[#1A2234] border border-[#28344D] text-xs font-semibold text-[#94A0B4]"
            >
              Previous
            </button>
          )}
          {step < steps.length - 1 ? (
            <button
              onClick={() => setStep(step + 1)}
              className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-bold text-xs uppercase tracking-wider"
            >
              Continue
            </button>
          ) : (
            <button
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-[#D4AF37] to-[#A68522] text-[#0A0E17] font-bold text-xs uppercase tracking-wider"
            >
              Finish & Return
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
