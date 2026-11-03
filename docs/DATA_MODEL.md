# Data model

Persisted entity and preference definitions live in `app/src/main/java/com/steadfast/app/data/`.

## Room (`steadfast.db`, schema version 1)

### `streak_sessions`

`StreakSessionEntity` fields:

- `id: Long` — auto-generated primary key.
- `recoveryFocusId: String`.
- `startEpochMillis: Long` and `startZoneId: String` — the starting instant and its zone.
- `endEpochMillis: Long?` — null denotes an active session.
- `createdAtEpochMillis: Long`.

Restart archives the prior active session and creates another. Repository validation allows at most one active session and rejects future, inverted, or overlapping chronology. A restart note, when supplied, is a journal entry linked to the archived session.

### `journal_entries`

`JournalEntryEntity` fields: auto-generated `id: Long`, `createdAtEpochMillis: Long`, `updatedAtEpochMillis: Long`, `body: String`, optional `context: String?` (`GENERAL`, `RESTART`, or `URGE`), and optional `streakSessionId: Long?`. Creation time is indexed; lists are ordered newest first.

### `bible_chapter_completions`

`BibleChapterCompletionEntity` stores `bookId: Int` and `chapter: Int` as a composite primary key. Only completed chapters have rows. Repository checks book, chapter, and testament bounds. Book and testament progress are derived from the bundled 66-book catalog.

## Preferences DataStore (`steadfast_settings`)

User settings are stored as preference keys and read together by `snapshotUserSettings` for backup:

- `onboardingComplete: Boolean` (defaults to false in normal startup settings).
- `themeMode: SYSTEM | DARK | LIGHT`.
- `appLockEnabled: Boolean`.
- `recoveryFocusId: String` (default `pornography`).
- `readingGoalMinimum: Int` in **1..5** (default 2). It is a motivational target; completion is not tracked daily.
- `oldTestamentCurrent` and `newTestamentCurrent`: optional `{ bookId, chapter }` bookmarks, each validated against its testament.
- `shownMilestones: Set<String>`: milestone IDs already celebrated for the current streak session. The set is cleared when a session begins or restarts.
- `lastBackgroundEpoch: Long`: device-local app-lock timing state; excluded from portable backup.
- `bibleTestamentFilter: Int`: All / OT / NT (`0..2`, default `0`).
- `journeyTab: Int`: Overview / History / Journal (`0..2`, default `0`).
- `journalFilter: String`: `ALL`, `GENERAL`, `URGE`, or `RESTART` (default `ALL`).

The three V1.1 view choices are optional in portable schema-2 backups; absent fields use these defaults. Existing preference keys and Room schema remain unchanged. Device-local `verse_deck_used` and `verse_deck_last` store canonical passage IDs: unused completed-chapter passages have priority, then all other unused passages. A new cycle begins only after all 500 are used and excludes the previous final passage from its first draw. These new keys initialize lazily without resetting other preferences and are excluded from portable backups.

## Bundled catalog models

`AssetSeedLoader` reads these JSON assets into models defined in `data/model/SeedModels.kt`:

- Bible books and chapter counts (`bible_books.json`).
- Recovery focus IDs and labels (`recovery_focuses.json`).
- Milestone IDs, kinds, values, and titles (`milestones.json`).
- `ScriptureEntry`: canonical ID, book ID/name, chapter, verse start/end, reference, exact WEB text, testament and themes (`scripture_catalog.json`, exactly 500 passages). `scripture_verse_counts.json` supplies canonical chapter/verse bounds. Publisher source, curated references and import verification are retained under `data/scripture_source/`, `data/scripture_curation.tsv` and `tools/import_scripture.py`.
- Legacy verse-reference catalog and scale comparisons.

## Derived values

Streak duration, longest streak, milestone achievement/next target, chapter/book/testament/Bible completion percentages, and completed-book count are derived from persisted rows, preferences, and bundled definitions; they are not separate persisted fields.
