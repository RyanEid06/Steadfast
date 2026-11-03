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
  core/time/                      streak arithmetic and foreground boundary clocks
  data/datastore/                 user settings and bookmarks
  data/db/                        Room database and DAOs
  data/model/                     persisted and bundled-data models
  data/repository/                streak, journal, Bible, verse, milestone data
  data/seed/                      bundled JSON loading
  ui/journey/                     Journey-scoped drafts and restart workflow state
  ui/onboarding/                  onboarding completion coordination
  ui/settings/                    Activity-scoped backup workflow state
  ui/                             Compose screens and theme
  widget/                         standard Android app widget
```

This is the implemented structure; there are no separate `domain`, `feature`, `backup`, or `dao` package scaffolds.

## Data flow and startup

Repositories expose Room and DataStore values as Flows. Compose screens collect those values; UI-only values such as timer ticks remain in memory. Room is authoritative for streak history, journal entries, and Bible completion. DataStore is authoritative for theme, recovery focus, app-lock preference, onboarding completion, reading goal, OT/NT bookmarks, shown milestone IDs, Bible testament/Journey/journal view choices, the local Scripture deck and last-background time.

At process start, `BackupManager` reconciles any interrupted restore before `MainActivity` composes protected content. The app then reads the coherent restored lock preference. If restore recovery is not ready, or if enabled authentication has not succeeded, personal content stays hidden. Authenticated session state is retained for activity configuration changes only and never across process death.

After restore and authentication gates, startup loads and validates the bundled catalogs off the main thread, then reconciles Room history and the onboarding preference. Startup distinguishes setup-needed, ready, and repair-required states; malformed catalogs show a retry path, while invalid history preserves stored rows and offers restore-only recovery. Fresh onboarding stages its selected preferences while incomplete, inserts the guarded initial Room session, and only then marks setup complete. Existing valid history reconciles an interrupted completion without creating another session.

Sensitive journal drafts and backup passphrases/previews live in configuration-retained ViewModels, not `rememberSaveable` or `SavedStateHandle`; they are intentionally not restored after process death. Harmless filters and tutorial steps may use saved state. Backup export/inspection/restore work continues across activity recreation, with the existing restore journal and content gate protecting replacement.

## Streak time and milestone behavior

`StreakSessionEntity` persists the start instant and its IANA zone ID. `StreakDurationCalculator` derives elapsed totals from instants and calendar years/months/days in the stored zone. Editing a session start preserves that stored zone unless a caller explicitly supplies another. The Dashboard/Journey shared clock wakes at elapsed-day or stored-zone calendar boundaries; components that display live seconds use a lifecycle-aware foreground clock. These are in-memory clocks, not per-second Room queries.

Milestone definitions are loaded from `app/src/main/assets/milestones.json`. Day milestones use whole elapsed days; month and year milestones use calendar units from the streak duration. The ordered first unachieved milestone is the next target. Celebrated milestone IDs are stored in DataStore and cleared when a restarted session begins; onboarding staging preserves an existing set. Celebration selection is the highest achieved milestone whose ID is not yet recorded as shown, and presentation is a modal scoped to the active session on Dashboard. Longest-streak history is recalculated across active day boundaries without waiting for a Room write.

## Bundled data

`AssetSeedLoader` reads Bible-book structure, recovery-focus options, milestone definitions, verse-reference catalogs, and scale comparisons from app assets using generated Moshi adapters. Successful catalogs are validated and cached; a failed load is retryable and does not silently become an empty catalog. The optimized native smoke harness is opt-in test wiring and must not retain seed models in the optimized application. The sanctuary loads 500 complete passages from `scripture_catalog.json`, with canonical metadata, verified WEB text and passage-specific tags. `VerseDeck` samples without replacement; a serialized DataStore edit commits the used IDs and last ID before returning each draw. Completed chapters prioritize unused cards only. Publisher XML, exact source hashes and the development-only importer validate all text/reference mappings. No runtime network or reference-only fallback is used.

The widget keeps its existing provider identity and uses a static navy/gold cross scene behind a large elapsed-day count and neutral encouragement. Brightness grows logarithmically with the current streak, caps at 365 days and returns to dim after a restart; unavailable or pending-restore state is dim. The scene is a bounded native bitmap, with no animation or new service. A shared refresh request runs after coherent startup and relevant successful mutations; restore replacement refreshes from its owning operation, and the stable-data guard prevents publication during pending restore. Widget output never includes the selected recovery focus, journal or Bible data.

## Checks

The project has JVM/Robolectric and Android instrumentation suites, plus Gradle lint and debug/release assembly tasks. Current V1.1 results are in [V1.1 verification](V1_1_VERIFICATION.md); remaining device/manual conditions and historical baseline evidence are in [Acceptance tests](ACCEPTANCE_TESTS.md). Pending checks are not implied by this architecture description.
