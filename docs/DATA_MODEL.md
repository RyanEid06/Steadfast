# Data Model

Names are conceptual; Gemini may adjust Kotlin naming while preserving semantics.

## Room entities

### `StreakSessionEntity`

- `id: Long` primary key
- `recoveryFocusId: String`
- `startEpochMillis: Long`
- `startZoneId: String`
- `endEpochMillis: Long?` — null means active
- `createdAtEpochMillis: Long`

Invariant: exactly one active session after onboarding.

### `JournalEntryEntity`

- `id: Long` primary key
- `createdAtEpochMillis: Long` indexed
- `updatedAtEpochMillis: Long`
- `body: String`
- `context: String?` (`GENERAL`, `RESTART`, `URGE`) optional
- `streakSessionId: Long?` optional link

### `BibleChapterCompletionEntity`

Store completed rows only:

- `bookId: Int`
- `chapter: Int`
- composite primary key `(bookId, chapter)`

Absence means unread. Do not store completion timestamp in V1.

## DataStore settings

- `onboardingComplete: Boolean`
- `themeMode: SYSTEM | DARK | LIGHT`
- `appLockEnabled: Boolean`
- `recoveryFocusId: String`
- `readingGoalMinimum: Int` in `1..5`
- `oldTestamentCurrentBookId: Int?`
- `oldTestamentCurrentChapter: Int?`
- `newTestamentCurrentBookId: Int?`
- `newTestamentCurrentChapter: Int?`
- `shownMilestoneCelebrations: Set<String>` scoped to active session, or equivalent persisted mapping
- tutorial/version flags as required

## Seed/static models

- `BibleBook`: see `data/bible_books.json`
- `RecoveryFocus`: see `data/recovery_focuses.json`
- `MilestoneDefinition`: see `data/milestones.json`
- `VerseReference`: see `data/verse_reference_catalog_starter.json`

## Derived values

Never persist values that can be derived safely:

- current streak duration
- longest streak duration
- book completion percentage
- testament completion percentage
- whole-Bible completion percentage
- total completed books
- milestone current/next state

This prevents drift and stale values.
