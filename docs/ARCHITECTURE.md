# Technical architecture

## Application

- Android app, Kotlin, one `MainActivity`, Jetpack Compose and Material 3.
- Namespace and application ID: `com.steadfast.app`; minimum Android API 29.
- Navigation Compose provides Dashboard, Journey, and Bible destinations. Settings is opened from the app UI, not a bottom destination.
- Room stores streak sessions, journal entries, and completed Bible chapters. Preferences DataStore stores user settings and reading bookmarks.
- AndroidX BiometricPrompt handles system biometrics/device credentials. Moshi reads bundled JSON catalogs and backup payloads.
- The manifest disables Android auto backup and declares no `INTERNET` permission.
- The home-screen widget is an `AppWidgetProvider` using platform `RemoteViews` and an XML layout; it is not implemented with Glance.

## Current source layout

```text
com.steadfast.app/
  MainActivity.kt                 startup gates, dependencies, app navigation
  core/accessibility/             reduced-motion helpers
  core/design/                    shared design behavior
  core/security/                  app lock and encrypted backup/restore
  core/time/                      streak arithmetic and foreground UI clock
  data/datastore/                 user settings and bookmarks
  data/db/                        Room database and DAOs
  data/model/                     persisted and bundled-data models
  data/repository/                streak, journal, Bible, verse, milestone data
  data/seed/                      bundled JSON loading
  ui/                             Compose screens and theme
  widget/                         standard Android app widget
```

This is the implemented structure; there are no separate `domain`, `feature`, `backup`, or `dao` package scaffolds.

## Data flow and startup

Repositories expose Room and DataStore values as Flows. Compose screens collect those values; UI-only values such as timer ticks remain in memory. Room is authoritative for streak history, journal entries, and Bible completion. DataStore is authoritative for theme, recovery focus, app-lock preference, onboarding completion, reading goal, OT/NT bookmarks, shown milestone IDs, and the device-local last-background time.

At process start, `BackupManager` reconciles any interrupted restore before `MainActivity` composes protected content. The app then reads the coherent restored lock preference. If restore recovery is not ready, or if enabled authentication has not succeeded, personal content stays hidden. Authenticated session state is retained for activity configuration changes only and never across process death.

## Streak time and milestone behavior

`StreakSessionEntity` persists the start instant and its IANA zone ID. `StreakDurationCalculator` derives elapsed totals from instants and calendar years/months/days in the stored zone. `rememberForegroundTimeMillis` updates while the Compose lifecycle is `RESUMED`; Dashboard recalculates from that in-memory clock, without a Room write or per-second database query.

Milestone definitions are loaded from `app/src/main/assets/milestones.json`. Day milestones use whole elapsed days; month and year milestones use calendar units from the streak duration. The ordered first unachieved milestone is the next target. Celebrated milestone IDs are stored in DataStore and cleared when an initial or restarted session begins. Celebration selection is the highest achieved milestone whose ID is not yet recorded as shown.

## Bundled data

`AssetSeedLoader` reads Bible-book structure, recovery-focus options, milestone definitions, verse-reference catalogs, and scale comparisons from app assets. Bible text itself is looked up by exact reference from the bundled WEB data; the UI can fall back to showing the reference when text is unavailable.

## Checks

The project has JVM/Robolectric and Android instrumentation suites, plus Gradle lint and debug/release assembly tasks. See [Acceptance tests](ACCEPTANCE_TESTS.md) for the checklist and the evidence observed for this reconciliation.
