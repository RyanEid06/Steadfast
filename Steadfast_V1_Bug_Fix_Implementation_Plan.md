# Steadfast V1 Bug Fix Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans` to execute this plan task by task. Use `ponytail` for minimal complete fixes and `impeccable` for affected UI/accessibility surfaces. Use focused independent reviewers when useful; shared startup, state and persistence changes remain sequential. Checkboxes track execution, not assumed success.

**Goal:** Fix the validated Steadfast V1 bugs, investigate the remaining concrete risks, and leave a verified baseline for later UI improvements while preserving user data and supported behavior.

**Architecture:** Retain the existing Kotlin/Compose, Room, DataStore, Moshi KSP and AndroidX architecture. Repair seed parsing and startup reconciliation first, then state/time correctness, accessibility, reproduced layout defects and scoped clocks. Keep the existing backup recovery journal and fail-closed app-lock design authoritative.

**Tech stack:** Existing pinned Kotlin/Compose, AGP, Room, DataStore, Moshi/KSP, JUnit/Robolectric, Android instrumentation and Gradle wrapper. Android minSdk 29 / compileSdk and targetSdk 36; JDK21. No dependency upgrade is required by this plan.

**Canonical scope:** This is the implementation plan for the [independent S01–S82 review](docs/ISSUE_PLAN_REVIEW_2026_10_29.md). Read that review for per-ID evidence, plus [product requirements](docs/PRODUCT_SPEC.md), [design freeze](docs/DESIGN_FREEZE.md), [security/backup contract](docs/SECURITY_BACKUP.md), `GEMINI.md`, and `CODEX_WORKLOG.md`. This plan supersedes the external 82-issue document's execution instructions. The review remains evidence; it is not a second implementation checklist.

## Starting state and execution rules

- Reviewed baseline: `main` at `ac16dad9bcea27cd50afaf8587fd8b7a34712f94`. Recheck HEAD, branch, worktrees, status and applicable instructions before editing. Preserve the existing worklog/review/plan edits and all unrelated changes. Reuse the current checkout unless isolation is actually needed.
- Review baseline: 93 freshly executed JVM tests passed; universal/ARM64 optimized release APKs assembled. This did not prove optimized catalog loading or phone acceptance. Test/build outputs were exported and generated local build caches removed.
- Work sequentially through the checkpoints below. Do not restart the audit or redesign the app. Read the current source before each task; line numbers in the review can drift.
- For each behavioral repair: add a focused regression, observe the expected failure, implement the smallest complete fix, run the closest check, and update the tracker/worklog. Tests for already-correct behavior should pass before and after. Do not require a failing test for a documentation-only correction or cosmetic line edit.
- Independent known bugs do not block progress on unrelated repairs. Do not call a checkpoint green when its own acceptance is failing. A physical-device gap must remain explicit; it does not prevent independent local implementation/testing.
- Delivery authorization updated 2026-11-01: after local verification, update relevant documentation, commit and push to GitHub, and copy verified APKs to Desktop. No merge, public store release, production signing, repository visibility change, physical-device modification or user-data wipe is included. Never stage unrelated changes, secrets, raw evidence or APKs.

## Global constraints

- Preserve recovery history, active streak instant/zone, journal bodies/contexts, Bible completions/bookmarks, milestone state and preferences. Never remove the duplicate-initial-session guard to bypass S82.
- Preserve namespace/applicationId `com.steadfast.app`, shared universal/ARM64 source, three bottom destinations, separate Settings and the fixed **non-scrolling Dashboard**.
- Preserve all existing timer tiers, including deliberate omission of minutes. Do not add calendar-daily Scripture selection, live theme preview, new chapter-entry UX or artwork redesign during this bug pass.
- Remain offline/private: no INTERNET permission, accounts, cloud, analytics, notifications or added services. Keep approved safety copy and verified WEB quotation/reference pairing.
- Preserve V1/schema1 and V2/schema2 backup compatibility, including **short and empty legacy import passphrases**. New exports still require matching passphrases of at least 12 characters. Retain SAF/provider compatibility and content validation; a filename suffix is not a security boundary.
- Do not write passphrases, decrypted previews or sensitive draft/search text to SavedStateHandle, saveable Bundles, logs or new plaintext snapshots as an incidental lifecycle fix. Prefer an in-memory owner for configuration recreation; define deliberate process-death behavior.
- Preserve fail-closed authentication, pending-restore recovery, validation and rollback. UI lifecycle repairs must not replace the backup protocol or weaken validation.
- Verify exact foreground/background color pairs; decorative gold may remain. Accessibility and reproduced unreachable controls are bug scope; broader visual redesign is deferred.

## Review focus

These failure modes must have tests in the owning tasks:

1. Process interruption or repeated Finish between Room insertion and onboarding completion preserves every row and cannot create a second initial session — Task 2.
2. Shrinking removes reflective seed members while a debug suite remains green; optimized backup consumers also rely on the same catalogs — Tasks 1 and 13.
3. Travel followed by an unchanged date edit silently rewrites zone metadata; sub-day history and active day rollover also become misleading — Tasks 5 and 11.
4. Rotation/file-picker return cancels a write or exposes private state through saved-state persistence; restore gating removes the originating screen — Task 7.
5. A widget or celebration publishes stale/private state or interrupts a workflow during restart/import/authentication — Tasks 3 and 4.

## Verification conventions

Task file paths such as `MainActivity.kt` or `ui/...` are relative to `app/src/main/java/com/steadfast/app/`; test class filenames refer to the existing `app/src/test/java/com/steadfast/app/` package unless native coverage is specified. Resource paths are relative to `app/src/main/`.

Use Windows `gradlew.bat`, the installed SDK, and a real Python executable with `-X utf8` for XML helpers. List Android targets and verify an explicit emulator serial before any install/input/test. Use synthetic data in a disposable emulator; never uninstall an existing user installation to change signing identities. If no emulator exists, configure a disposable local target using an installed image when feasible and keep physical acceptance pending.

Focused JVM command pattern: `.\gradlew.bat testDebugUnitTest --tests "com.steadfast.app.<TestClass>" --offline --no-daemon`. Run the appropriate existing/native task for UI or Android-only behavior, not a JVM surrogate that cannot cover it. The final local gate is `.\gradlew.bat test lintDebug assembleDebug assembleRelease --offline --no-daemon`; rerun online only when missing cached dependencies actually require it. Never describe a release unit test or assembly as an optimized runtime smoke test.

Host-specific note: unmodified JDK21/JDK25 attempts failed with AF_UNIX `Invalid argument: connect`. Verification used a process-only JDK21 TCP-pipe workaround, without editing the installed JDK or application. Exact local paths and invocation are retained only in ignored `.build-outputs/bug-fix/continuation-environment.txt`; do not commit the workaround. Recheck the host failure before reusing it. Determine success from the actual task exit and results, not incidental non-fatal KSP disposal messages.

## Checkpoint A — reliable entry and private widgets

### Task 1 — release-safe catalogs and truthful bootstrap (S81, narrowed S04)

**Files:** `app/src/main/java/com/steadfast/app/data/model/SeedModels.kt`, `data/seed/AssetSeedLoader.kt`, `MainActivity.kt`; inspect every loader caller, especially `data/repository/{BibleRepository,MilestoneRepository,VerseRepository}.kt` and `core/security/BackupManager.kt`. Existing KSP dependency and ProGuard configuration are already present. Proposed tests: `app/src/test/java/com/steadfast/app/SeedCatalogTest.kt` and optimized native coverage in Task 13.

**Interfaces:** Preserve existing loader names and return types (`loadRecoveryFocuses`, `loadBibleBooks`, `loadMilestones`, `loadVerseCatalog`, `getScaleComparisons`) where possible. Successful calls return validated data; failures must be distinguishable from legitimately empty user data. The app bootstrap owns loading/error/retry, and downstream repositories consume the same successful catalog result rather than inventing fallback content.

- [x] Add tests asserting 18 focuses, 66 books / 1,189 chapters, 12 milestones, 8 verse themes and 21 comparisons, with known IDs and valid references. Add a malformed/missing catalog case that expects an identifiable failure rather than an empty success. Keep fixtures synthetic.
- [ ] Run the focused test and observe the failure for swallowed catalog errors; record that debug parsing alone cannot reproduce the R8 defect.
- [x] Add `@JsonClass(generateAdapter = true)` to all eight seed wrappers/item models using existing KSP. Remove silent-success error handling, validate required fields/IDs and expose retryable bootstrap errors. Move required initial state loading off the main thread where it does I/O. Gate protected content on coherent catalog/preferences/session readiness while preserving the existing restore/auth gates. Verified by final JVM gate and minified catalog/error modes.
- [x] Keep startup loading free of fake sensitive focus, fabricated progress or empty-history claims. Real empty user history is a loaded state; malformed catalogs are errors. Ensure widget/backup callers safely surface catalog failure rather than crashing or mutating user data. Optimized corrupt-catalog mode hid protected content and preserved rows.
- [x] Run seed and existing backup/Bible/Scripture tests, then `assembleRelease`. Inspect generated adapters and R8 output; require the actual optimized smoke in Task 13 before declaring S81 verified. Optimized catalog and deliberate corrupt-catalog modes passed in the final release-equivalent APK.

### Task 2 — idempotent onboarding and installation reconciliation (S82, S03)

**Files:** `MainActivity.kt`, `data/repository/SteadfastRepository.kt`, `data/datastore/SteadfastPreferences.kt`, `ui/onboarding/OnboardingWizardScreen.kt`; preserve DAO/history contracts. Proposed tests: `StartupReconciliationTest.kt`, `OnboardingRestorationTest.kt` under the existing JVM test package; native recreation/kill-boundary coverage where JVM cannot model it.

**Interfaces:** Retain the guarded `startInitialSession` repository operation and existing onboarding callback fields. Introduce one owned startup/completion path, with explicit Loading / Ready / NeedsSetup / RepairRequired outcomes as needed; callers must not independently infer readiness from a Boolean and empty initial Flow values. A Room transaction does not make DataStore writes transactional.

| Loaded Room state | Loaded onboarding flag | Required result |
|---|---|---|
| No history | false | Normal setup with no preselected recovery focus |
| Valid history with one active session | false | Reconcile existing installation/setup without inserting another initial session |
| Valid history with one active session | true | Enter after catalog/restore/auth readiness |
| No history | true | Repair inconsistent flag and resume setup, preserving journal/Bible/preferences |
| Archived history only, invalid focus, or multiple active sessions | either | Explicit repair/setup path; do not reactivate old history, choose a sensitive fallback or delete rows |

- [x] Add tests for this matrix, missing preferences, invalid/missing focus, repeated launches/Finish, failure after initial insertion, failure during each remaining setup write, and lock enable. Assert exact session IDs/counts, active instant/zone, journal/Bible rows and retry outcomes. Final startup/reconciliation suites passed; optimized fresh entry asserted explicit focus, exactly one session and journal/Bible preservation.
- [ ] Observe the existing-history rejection/current reset failures before fixing them.
- [x] Require an explicit valid focus for fresh setup and disable Continue until it exists. Reconcile an existing valid focus from stored data without silently reclassifying it. Inspect preference/restart/legacy-conversion defaults; preserve documented legacy interpretation while preventing a new sensitive fallback in fresh setup and error states. Verified in optimized fresh entry on a new test user.
- [x] Serialize completion and disable repeat submission while it runs. Make retries idempotent across the Room/DataStore boundary and mark onboarding complete only after required setup succeeds. Keep an existing session untouched on repeat completion; preserve the repository guard. Never auto-insert a replacement to repair corrupt history. Fresh entry/relaunch retained exactly one initial session.
- [x] Retain wizard step, chosen focus/date/zone, OT/NT selection, reading goal, theme and lock draft through normal recreation using appropriately scoped state. Process death may require reauthentication; never use restored UI state to bypass lock. Activity recreation and locked-state regressions passed locally.
- [ ] Run reconciliation, chronology, app-lock and onboarding tests plus rotation/native entry checks. Add milestone/show-set and Bible/bookmark preservation assertions to interruption cases. Automated suites and fresh entry passed; milestone/show-set preservation through every interrupted completion boundary remains unverified.

### Task 3 — neutral widgets and coherent refresh (S01, S72)

**Files:** `widget/SteadfastWidgetProvider.kt`, `res/layout/widget_steadfast.xml`, `res/xml/widget_info.xml`, successful mutation call sites in `MainActivity.kt` / repository and backup completion integration. Extend `ReconciliationUiDataTest.kt` and add native provider coverage if needed.

**Interfaces:** Keep `loadWidgetContent(context)` private-content-safe and the existing stable-user-data restore guard. One shared `requestWidgetRefresh(context: Context)` helper may notify the provider for current IDs; invoke it only after a coherent successful mutation. Do not build a per-second widget service.

- [x] Add regression cases for every focus and lock state asserting no focus/journal/Bible content in rendered widget data; assert pending-restore neutral content and refresh after restart, initial/reconciled creation and completed restore. Widget privacy/reconciliation suites and the optimized registered-widget check passed.
- [x] Remove focus labels from all widget branches and use neutral copy. Immediately republish existing widgets after app upgrade/first safe launch so old sensitive text is replaced. Keep receiver work bounded and background-safe. Source review, privacy tests, and optimized neutral `RemoteViews` passed.
- [x] Request refresh after relevant successful mutations and safe resume/threshold/day boundaries. Do not publish partial restore data or depend on the screen coroutine surviving import. Keep the existing battery-friendly periodic fallback. Widget/restore regression coverage passed; OS process-kill recovery remains separate.
- [x] Verify no redundant updates on unchanged second ticks, neutral error recovery, and actual RemoteViews publication to registered emulator widgets when available. Optimized registered widget received neutral `RemoteViews`; automated widget tests passed.

**Checkpoint A gate:** Focused tests pass, old data is preserved, startup failure is recoverable, widget privacy is proven, and Task 13's early optimized catalog/onboarding smoke passes. Keep affected-phone verification explicitly pending when unavailable.

## Checkpoint B — time, navigation, and state correctness

### Task 4 — modal, deferred celebrations (S06, S78)

**Files:** `MainActivity.kt`, `data/repository/MilestoneRepository.kt`; tests `MilestonePresentationTest.kt` and existing navigation/app-lock regressions.

**Interfaces:** Retain milestone IDs and shown-set persistence. Evaluate pending milestones independently from their presentation; the presentation owner exposes one dismiss action and cannot overlay authentication/recovery or an unsafe workflow.

- [ ] Reproduce bottom-navigation/pointer/Back access under celebration and threshold arrival during journaling/import/onboarding. Assert no underlying click/focus can fire and no workflow is dismissed or draft lost. Emulator pointer/Back checks passed; workflow/draft combinations remain unverified.
- [x] Show queued celebrations only on an idle Dashboard with no other workflow/modal active; defer during restore/auth and cancel stale queued session state after restart/import. Use a true modal layer with explicit Back dismissal semantics. Milestone/app-lock regression suites passed; broader OEM conditions remain open.
- [x] Persist shown state only for the actual acknowledged milestone/session; preserve the existing highest-unshown selection policy. Do not silently mark unseen milestones celebrated. Milestone presentation tests passed.
- [ ] Run focused modal/semantics tests, native navigation and app-lock regressions, including recreation and consecutive pending milestones. JVM suites and pointer/Back emulator checks passed; native recreation with consecutive pending milestones remains unverified.

### Task 5 — stored-zone editing and truthful history (S23–S28, S41, narrowed S42)

**Files:** `data/repository/SteadfastRepository.kt`, `ui/settings/SettingsScreen.kt`, `ui/journey/JourneyScreen.kt`, callbacks in `MainActivity.kt`. Extend `StreakChronologyTest.kt`; add `StreakPresentationTest.kt` for edit/date/chart behavior.

**Interfaces:** Change `updateSessionStart(sessionId: Long, newStartEpochMillis: Long, newZoneId: String? = null)` so null means preserve the found session's zone. New-session restart still intentionally captures its creation zone. Use existing recovery-focus labels rather than ID prettification. Keep elapsed duration separate from local calendar formatting.

- [x] Add failing tests for travel + unchanged save preserving zone, persisted-zone display, picker reopening from its current draft, invalid edit retaining dialog/draft, and accurate 15-minute versus 6-hour bars/labels. Cover future/overlap chronology, midnight, DST transitions, leap dates and month ends without assuming an unverified date-construction bug. Chronology/presentation suites passed; future-date rejection and valid retry passed on emulator.
- [x] Initialize native pickers from the draft in the displayed stored zone; keep selection and formatting consistent. Return validation results before dismissal and retain repository checks as the final boundary. Do not add timezone editing/schema fields unless a real requirement needs them. Emulator future-date rejection retained the draft; valid retry succeeded.
- [x] Resolve historical and backup-preview focus labels through loaded catalogs with a neutral unknown-ID fallback. Never alter stored IDs to repair presentation. Verified by catalog-backed UI and backup tests.
- [x] Use continuous elapsed duration for bar proportions and honest short-duration labels; include session/date/value identification in accessible summary and minimal visual labels. Remove the arbitrary 10% minimum that misrepresents relative duration. Preserve the bounded recent-session chart. Presentation and chronology suites passed locally.
- [x] Run chronology/presentation tests and native picker rejection/retry checks. A01 longest-streak invalidation belongs to Task 11. The emulator rejected a future date without dismissing the draft, then accepted the valid retry.

### Task 6 — Bible loading and required reading management (S05, S11, narrowed S12/S13, S14/S15)

**Files:** `ui/bible/BibleScreen.kt`, `ui/bible/BookDetailScreen.kt`, `ui/settings/SettingsScreen.kt`, `data/repository/BibleRepository.kt`, `MainActivity.kt`; extend `BibleStateValidationTest.kt` and UI/navigation tests.

**Interfaces:** Keep existing validated `setOtCurrentReading`, `setNtCurrentReading`, `setReadingGoal` and chapter-completion operations. Distinguish loading, found, unknown book and failed catalog states rather than overloading null. Reuse one bounded book/chapter selection flow across visible bookmark actions and Settings.

- [x] Add tests for valid book's delayed first emission, genuine unknown ID with usable Back, displayed `N+ chapters` intention, visible set/change/clear of both bookmarks and canonical names. Assert bookmark changes do not toggle completion or auto-advance reading. Bible presentation/state suites passed in the final gate.
- [x] Add truthful loading/error/empty copy; show the existing reading target. Provide a visible accessible bookmark action while retaining the documented long-press shortcut. Clarify empty-card instructions or make its action open the real picker. Verified in Bible presentation and navigation tests.
- [x] Expose set/change/clear in Settings using canonical names. Validate book/testament/chapter bounds through existing repository operations; no new Bible data source or schema migration. Settings/Bible workflow tests passed.
- [x] Run Bible repository/UI tests and navigation regression, including recreation and empty bookmarks.

### Task 7 — lifecycle-safe drafts and backup workflow ownership (S50–S54, S64; narrowed S80)

**Files:** `ui/settings/SettingsScreen.kt`, `ui/journey/JourneyScreen.kt`, `ui/bible/BibleScreen.kt`, `ui/onboarding/TutorialReplayScreen.kt`, `ui/struggling/BreathingExerciseScreen.kt`; small screen-scoped ViewModels may be added beside their owning screen. Extend `BackupRestoreProtocolTests.kt`, existing UI tests and new targeted lifecycle tests.

**Interfaces:** Harmless scalar filters/steps can be saveable. Sensitive text, passphrases, previews and pending operation state use an in-memory lifecycle owner; configuration recreation must not duplicate work. Existing `BackupManager` recovery/rollback APIs remain unchanged unless a proven defect requires a bounded change.

- [x] Test tab exit/reentry as well as rotation: Journal query/filter, Bible filter, tutorial step, Start Again date/note, new/edit journal draft, selected backup URI and password/preview stages. Include a real process restart and distinguish intentional sensitive-draft loss from configuration retention. JVM lifecycle tests plus optimized ordinary process relaunch passed; true restore-time OS-kill boundaries remain separate.
- [x] Hoist Journal search/filter outside conditional tab content. Retain required drafts through rotation without serializing secrets. Preserve a nullable existing journal context during edit; Save must continue to require only a body. If needed, represent the existing null context as a neutral “None” option; retain direct URGE composer behavior and legacy GENERAL values without data migration. Journal lifecycle/UI-state tests passed.
- [x] Own export/inspection/import/result work above disposable dialog composition. Prevent duplicate starts and give cancellation/retry a coherent file outcome. Restore remains protected by its existing recovery journal and stable-data gate; do not claim cancelling UI proves data corruption. Optimized backup/recreation and rollback acceptance passed.
- [x] Make breathing pause while backgrounded and retain phase/remaining count across rotation without resetting to a full phase. On genuine process restart, restart visibly from the first inhale. Keep system/app reduced motion and timing cues working; do not fast-forward unseen breathing phases. Native acceptance completed all four phases/countdowns, verified Home pause, Activity recreation retaining Hold progress, Dashboard return, and unchanged SQL rows/portable settings.
- [x] Test rotation at file-picker return, KDF/write, preview and restore boundaries; assert one operation, no passphrases/previews in saved-state containers, unchanged data on rejected imports, correct rollback/recovery, and cleared sensitive workflow state after completion/cancel. JVM lifecycle coverage, optimized backup acceptance, actual SAF-to-preview, and native Keystore regressions passed.
- [ ] Run focused lifecycle plus backup/lock tests and the relevant native SAF/Keystore coverage. Keep actual OS process-kill recovery gaps distinct from coroutine-cancellation tests. Lifecycle/backup/Keystore checks passed; process death during an interrupted restore remains untested.

### Task 8 — bounded Settings feedback and metadata fixes (S57, S59, S62, narrowed S22/S66)

**Files:** `ui/settings/SettingsScreen.kt`, `core/security/BiometricLockManager.kt` only if needed for exposing current capability, `MainActivity.kt`; tests in existing UI/lock suites.

- [x] Add explicit passphrase-mismatch supporting text, stable safe messages for export/decrypt/validation/provider/recovery failures, and `BuildConfig.VERSION_NAME` in both version displays. Preserve export validation and empty legacy import support. Settings and backup tests passed; optimized V1 short/empty import acceptance passed.
- [x] Show the approved substance-safety notice near the applicable Settings selection. Surface current app-lock capability/enrollment guidance inline, refresh capability on return from Android Settings, and allow disabling an enabled lock. Do not confuse temporary hardware unavailability with unsupported authentication or weaken locked-state behavior. Settings and lock-policy tests passed; physical enrollment remains separate.
- [x] Verify error text does not interpolate raw exceptions/private values, invalid submission remains gated, correct version renders, and unavailable-auth retry/fail-closed tests still pass. Do not add broad exception logging.

**Checkpoint B gate:** Targeted behavior tests pass; timezone/history truthfulness, bookmark actions, modal safety and configuration retention are observed. Preserve all existing chronology, backup rollback and authentication checks.

## Checkpoint C — accessibility and proven layout defects

### Task 9 — contrast, semantics, loading and directional correctness

**IDs:** S02, narrowed S29/S31/S32/S44/S45/S46, S33, S43, S67/S68.

**Files:** `ui/theme/{Color,Theme,Type}.kt`, `MainActivity.kt`, affected onboarding/Settings/Journey/Struggling/timer controls. Use existing `core/design/ReducedMotion.kt` policy. Add focused Compose semantics/color tests and extend existing reduced-motion tests.

- [x] Verify each changed text/interactive color pair in light and dark themes. Use semantic foreground/container roles; target at least 4.5:1 for normal text, 3:1 for large text and meaningful non-text indicators. Fix the actual gold/white and light-primary/cream failures while retaining decorative brand accents. Automated contrast checks and compact light/dark screenshots passed.
- [x] Add explicit mutually exclusive selected/radio semantics to focus/theme cards, preserve their existing visible checks/borders and native minimum touch handling. Restore a non-color navigation selection cue; decorative navigation icon descriptions should not duplicate visible labels. Accessibility regression tests passed.
- [x] Add one concise step-progress announcement per wizard/tutorial rather than individual focusable dots. Replace `View Timeline >` with an auto-mirrored icon. Improve undersized functional text selectively and verify text scaling rather than globally changing typography. Automated semantics/layout checks passed.
- [x] Provide stable truthful Struggling verse-loading content. Honor the existing reduced-motion policy for verse transitions; test both system animator scale zero and app test policy while keeping breathing timing functional. Automated loading/accessibility tests and native breathing timing passed.
- [ ] Run semantic, color, touch-target and reduced-motion tests; verify actual TalkBack announcements/focus on an emulator or device. Automated semantics, contrast and reduced-motion checks passed; spoken TalkBack acceptance remains pending.

### Task 10 — reproduce and fix reachability/layout risks

**IDs:** narrowed S07; S08–S10, S16–S18, S35–S40, S55, S65, S71, S79.

**Files:** Only the owning screen/layout for each reproduced failure. Start with fixed non-scroll About/safety content in `SettingsScreen.kt`; retain the existing `DashboardCompactLayoutTest.kt` and add a regression per demonstrated inaccessible control/data layout.

- [ ] Make long About/safety content bounded and scrollable; verify the complete notice is reachable. This is a confirmed structural problem, not permission for an About redesign.
- [ ] Exercise 320×568dp and 360×640dp portrait, standard portrait, 568×320dp and 640×360dp short landscape, split-screen constraints, font scales 1.0/1.3/1.5/2.0, light/dark, RTL, IME and long labels/values. Measure usable content bounds after bars/navigation/IME. Distribute combinations sensibly, including the worst cases, instead of blindly multiplying every dimension. Observed compact 320×568/font-scale 2 export/IME and 640×360/font-scale 2 light/dark/RTL; broader combinations remain unverified.
- [ ] For each risk, record exact scenario, bounds/screenshot and actionable failure. If reproduced, add a regression then make the smallest reflow/scroll/wrap change. If not reproduced, record tested conditions and close as not reproduced there; do not claim universal correctness or invent a fix.
- [ ] Keep Dashboard non-scrolling with constraint-based compact sizing/reflow; do not replace its existing wide/short branch wholesale. Keep required Struggling actions. Use scrolling/wrapping elsewhere only where it makes content/actions reachable.
- [ ] Verify actual widget minimum launcher sizes and ellipsize/hide secondary neutral lines when necessary. Treat fixed milestone connector length as cosmetic unless demonstrated harm; adjust to row height only for a reproduced defect.
- [ ] Assert full critical text/control bounds and successful interaction, not just node existence or partial visibility. Check focus order and touch separation after reflow. Keep untested OEM/launcher combinations pending.

### Task 11 — scoped clocks, threshold work and longest-streak rollover

**IDs:** S75, narrowed S73/S74/S76/S77, A01.

**Files:** `core/time/ForegroundClock.kt`, `MainActivity.kt`, `ui/{dashboard,journey,milestones,motivation}/*Screen.kt`, `data/repository/MilestoneRepository.kt`; add deterministic clock/boundary tests.

**Interfaces:** Reuse the foreground clock helper; do not introduce globally emitted per-second state that invalidates unrelated screens. Duration remains derived from loaded session memory, not a database poll. Milestone state depends on session, meaningful thresholds and shown IDs; live counters depend on visible routes/components.

- [x] Add deterministic tests showing Bible/Settings have no active visual 1Hz ticker, hidden/background screens stop ticking, live seconds remain correct, and root/screen/History chart do not duplicate unnecessary subscriptions. Foreground-clock tests passed locally.
- [x] Test threshold crossing, resume after day rollover, shown-set edits, restart/import invalidation and highest-unshown selection. Preserve calendar-month/year semantics and the once-per-milestone/session policy. Clock and milestone suites passed.
- [x] Reproduce A01: an active streak crossing a day boundary must update longest streak without requiring a Room emission. Include a previously longer archived streak and resume case. Focused boundary lifecycle and final `ForegroundClockTest` passed.
- [ ] Scope shared timing and derive static milestone/comparison work on relevant boundary changes. Isolate Motivation's real live counter from unchanged comparison content. DataStore collection each tick is cached subscription work, not proof of disk I/O; measure actual subscriptions/recompositions/cost before expanding optimization.
- [x] Run time/milestone/widget tests; retain corrected modal behavior and timely neutral widget refresh from Tasks 3/4. Final foreground-clock/milestone/widget tests passed; native widget provider returned neutral `RemoteViews`.

### Task 12 — bounded themed-icon construction fix (narrowed S69)

**Files:** `res/mipmap-anydpi-v26/ic_launcher*.xml`, `res/drawable/ic_launcher_foreground.xml`, a simple monochrome vector if needed. Preserve existing branding/art files and the current foreground inset.

- [x] Replace detailed opaque raster reuse in the monochrome layer with a simple brand/cross silhouette, preferably using existing vector geometry. Do not commission new artwork or redesign the foreground for this bug fix. Optimized adaptive/monochrome icon mode passed.
- [x] Inspect optimized adaptive/themed icons under common masks at launcher size. Verify the existing inset rather than assuming safe-zone failure. Change foreground construction only for a demonstrated masking defect; retain the frozen visual identity. Optimized icon verification passed; OEM launcher masks remain device-specific.

**Checkpoint C gate:** Accessibility defects are corrected with semantic/rendered evidence, reproduced layout issues are repaired, remaining risks have honest dispositions, and timing tests pass. Broad UI enhancements remain deferred.

## Checkpoint D — optimized runtime and final regressions

### Task 13 — release-equivalent smoke and upgrade/failure acceptance

**Files:** Add a focused native seed/startup smoke under `app/src/androidTest/java/com/steadfast/app/`; minimally adjust Gradle test wiring if necessary. Update `docs/ACCEPTANCE_TESTS.md`, `docs/SECURITY_BACKUP.md`, relevant product/UI docs, `STEADFAST_V1_CORRECTION_QA.md` and `CODEX_WORKLOG.md` to match final behavior/evidence.

- [x] **Run the seed/onboarding portion early, before Checkpoint A can be green**, then rerun relevant changed portions at final integration. Test the actual minified/resource-shrunk code and assets; debug, release-unit tests or an optimization-disabled test variant cannot replace it. Test wiring must not keep seed models alive merely because a test directly references them. Fresh optimized entry passed on a new test user with catalog sentinels, explicit focus, Activity recreation, one initial session, journal/Bible preservation, and relaunch.
- [x] Record APK SHA-256, ABI, package, version, API/target and signing identity. Private debug-identity signing of a disposable test copy is acceptable; it is not production signing. Cover universal runtime on a compatible emulator and ARM64 on an available compatible target; record unavailable target coverage. Neither an ABI file listing nor a universal x86 emulator proves ARM64 runtime behavior. Artifact audit recorded package/version/signature/hashes; ARM64 ran through `libndk_translation` on x86, so native ARM64 coverage remains unavailable.
- [x] Assert all catalog counts and sentinel records through actual app loading, complete fresh onboarding with explicit focus, exercise existing-history/incomplete-flag startup, recreate/relaunch and assert no duplicate session or lost journal/Bible/history. Deliberately broken test assets must produce a recoverable explicit error without weakening the production APK smoke. The fresh entry test verified counts/sentinels, explicit focus, recreation, exactly one session, data preservation, and relaunch; separate optimized modes covered incomplete history and broken-catalog recovery.
- [ ] Verify optimized backup export/import/preview with current format and legacy V1/schema1 short/empty passwords, correct/wrong passwords, malformed/oversized files and provider errors. Preserve read-only preview, atomic replacement/recovery and rollback checks. Exercise lock enable/unavailable/cancel/recreation around entry and import.
- [x] Run the final local gate from Verification conventions and relevant native regression suite. Inspect actual XML/result totals and APKs; report cached versus executed results accurately. Use exact failing-job/task evidence and rerun only relevant failed checks before broadening. Observed: 166/166 JVM tests, lint 0 errors/40 warnings, optimized assembly, and 4/4 native regressions.
- [x] Perform synthetic upgrade acceptance without destructive uninstall/wipe: preserve session IDs/counts, active instant/zone, notes, contexts, Bible completions/bookmarks and preferences. Capture required known behavior before upgrade and compare after. Real phone/biometrics/SAF/OEM/TalkBack acceptance remains a separate explicit checklist. Universal and ARM64 installs preserved recorded Room and DataStore hashes; ARM64 used x86 `libndk_translation`, not native ARM64.
- [x] Obtain a focused final review of startup/persistence/privacy and integrated changes, address actionable findings, then inspect diff/dirty state. Preserve valuable evidence outside Git and remove only verified task-generated disposable clutter. Final review completed; the widget restore Important finding was fixed and its 18/18 regression suite passed. Disposable users/artifacts were cleaned, original owner data/settings restored, final universal APK reinstalled, and emulator 5584 shut down; logs/evidence were preserved.

## Issue ledger and completion policy

The table below assigns each original ID exactly once. Tasks may interact, but this is the owning task for completion. `C/P/R/E/X` retain the review meanings; `P` requires only the narrowed supported repair and `R` requires reproduction/disposition. Deferred enhancements do not become hidden requirements for this bug pass. Incorrect claims are closed with their reasoning, not implemented.

For every owned item, record **Not started / Investigating / Implemented, awaiting verification / Verified / Not reproduced in recorded conditions / Deferred / Closed, intentional** plus evidence, checkpoint/commit if any and remaining target limitations. Mark an item Verified only after its acceptance actually passes. The project can be locally bug-verified while phone acceptance remains pending; never label it release-ready until required device/signing/release gates are satisfied.

The ledger retains all 11 E enhancements as deferred and all 3 X items as intentionally closed.

**Current integration checkpoint (2026-11-02):** The final ordinary gate passed 166/166 JVM/Robolectric tests across 34 suites; lint reported 0 errors and 40 warnings. The final optimized release-equivalent assembly, optimized backup acceptance, final four-test native regression suite, dedicated breathing acceptance, fresh optimized entry, and ordinary process restart passed. Optimized catalog-error, widget-provider, and icon modes passed; actual compact light/dark/RTL emulator layouts and a real SAF encrypted export/import-to-read-only-preview were observed. The force-stop/relaunch returned to Dashboard with SQL and DataStore hashes unchanged; this was not OS-kill recovery during restore. The breathing harness excludes Android's changing `LAST_BACKGROUND_EPOCH` from its portable-settings digest while retaining lifecycle and SQL guards. C/P rows below describe only the acceptance actually observed; R dispositions stay scoped to tested conditions. Physical phone, API 29 enrolled-auth, native ARM64, OEM/launcher/TalkBack, and true OS process-kill acceptance remain separate gates.

| ID | Review | Owner / disposition | Current status and evidence |
|---|---|---|---|
| S01 | C | Task 3 | Verified locally in the final JVM gate; `WidgetPrivacyTest`, `ReconciliationUiDataTest`, optimized `OptimizedSmokeRunner` widget mode. |
| S02 | C | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`, `SettingsAccessibilityTest`. |
| S03 | C | Task 2 | Verified locally: `OnboardingRestorationTest`, `StartupReconciliationTest`, and optimized fresh entry on a new test user passed. Explicit focus, Activity recreation, exactly one initial session, journal/Bible preservation, and relaunch were observed. |
| S04 | P | Task 1 | Verified locally in the final JVM gate; `SeedCatalogTest`, `StartupReconciliationTest`, `StartupRepairUiTest`, optimized catalog smoke. |
| S05 | C | Task 6 | Verified locally in the final JVM gate; `BiblePresentationTest`, `BibleStateValidationTest`, navigation coverage. |
| S06 | C | Task 4 | Verified locally in the final JVM gate; `MilestonePresentationTest`, `RootNavigationTest`. |
| S07 | P | Task 10 | Verified locally in the final JVM gate; `DashboardCompactLayoutTest`, `LayoutRiskRegressionTest`; wide/short and font matrix results remain scoped to recorded windows. |
| S08 | R | Task 10 | Repaired after compact Struggling actions/metadata bounds failed; `struggling-green-2026-11-01.log` 2/2 passed, including compact actions and close callbacks. Tests: `AdditionalLayoutRiskTest`, `LayoutRiskRegressionTest`; phone/IME conditions remain pending. |
| S09 | R | Task 10 | Not reproduced as a breathing layout failure in the recorded compact/short matrix; `layout-matrix4-2026-11-01.log` passed breathing/action cases. Test: `LayoutRiskRegressionTest`; other device sizes remain pending. |
| S10 | R | Task 10 | Not reproduced in recorded JVM bounds: `TutorialReachabilityTest.primaryActionIsFullyWithinTheUsableWindowAtTwoTimesFont` passed (1/1). Short-height/large-font device coverage remains pending. |
| S11 | C | Task 6 | Verified locally in the final JVM gate; `BiblePresentationTest`, `BibleStateValidationTest`. |
| S12 | P | Task 6 | Verified locally in the final JVM gate; `BiblePresentationTest`, `SettingsWorkflowTest`. |
| S13 | P | Task 6 | Verified locally in the final JVM gate; `BiblePresentationTest`, `SettingsWorkflowTest`. |
| S14 | C | Task 6 | Verified locally in the final JVM gate; `SettingsWorkflowTest`. |
| S15 | C | Task 6 | Verified locally in the final JVM gate; `SettingsWorkflowTest`, `BiblePresentationTest`. |
| S16 | R | Task 10 | Repaired with wrapping Bible filters; recorded 320dp/fontScale 2 matrix passed. Test: `LayoutMatrixRiskTest`; phone coverage remains pending. |
| S17 | R | Task 10 | Repaired with wrapping Journal filters; recorded 320dp/fontScale 2 matrix passed. Test: `LayoutMatrixRiskTest`; phone coverage remains pending. |
| S18 | R | Task 10 | Repaired with wrapping composer context choices; recorded 320dp/fontScale 2 matrix passed. Test: `LayoutMatrixRiskTest`; IME/phone coverage remains pending. |
| S19 | E | Deferred to later UI/product work | Deferred: chapter direct-entry control; repeated increment/decrement remains valid and no tracking defect was established. |
| S20 | E | Deferred to later UI/product work | Deferred: onboarding live theme preview; selected theme persists at completion, so this is a preview enhancement. |
| S21 | E | Deferred to later UI/product work | Deferred: exact OT/NT locations in the Finish summary; selected locations are persisted. |
| S22 | P | Task 8 | Verified locally in the final JVM gate; `SettingsAccessibilityTest`, `SettingsWorkflowTest`. |
| S23 | C | Task 5 | Verified locally in the final JVM gate; `StreakChronologyTest`, `StreakPresentationTest`. |
| S24 | P | Task 5 | Verified locally: `StreakPresentationTest` covers validation-before-dismissal; emulator date-picker future-date rejection retained the draft, and valid retry succeeded. Physical-device behavior remains pending. |
| S25 | C | Task 5 | Verified locally in the final JVM gate; `StreakPresentationTest`. |
| S26 | C | Task 5 | Verified locally in the final JVM gate; `StreakChronologyTest`. |
| S27 | C | Task 5 | Verified locally in the final JVM gate; `StreakPresentationTest`. |
| S28 | C | Task 5 | Verified locally in the final JVM gate; `StreakPresentationTest`. |
| S29 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`. |
| S30 | X | Close original proposal; retain correct behavior | Closed, intentional: existing title line height plus 28dp vertical padding meets the 48dp target; no undersized-control defect was established. |
| S31 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`. |
| S32 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`. |
| S33 | C | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`, `ReconciliationUiScreenTest`. |
| S34 | E | Deferred to later UI/product work | Deferred: restructuring multiple Struggling exits/actions; review classified it as design preference, not broken navigation. |
| S35 | R | Task 10 | Repaired after RED scripture metadata measured 0×0; `struggling-green-2026-11-01.log` 2/2 passed after scroll/wrap/header changes. Test: `AdditionalLayoutRiskTest`; phone coverage pending. |
| S36 | R | Task 10 | Not reproduced in recorded 320dp/fontScale 2 layout bounds; `LayoutMatrixRiskTest` passed. Other widths/fonts/OEM behavior remain pending. |
| S37 | R | Task 10 | Not reproduced in recorded 320dp/fontScale 2 layout bounds; `LayoutMatrixRiskTest` passed. Other widths/fonts remain pending. |
| S38 | R | Task 10 | Not reproduced in recorded 320dp/fontScale 2 layout bounds; `LayoutMatrixRiskTest` passed. Other widths/fonts remain pending. |
| S39 | R | Task 10 | Not reproduced in the recorded compact/large-value scenario; Journey statistic bounds passed in `struggling-green-2026-11-01.log`. Test: `AdditionalLayoutRiskTest`; other device coverage pending. |
| S40 | R | Task 10 | Not reproduced in recorded compact/large-value metric-card bounds; `LayoutMatrixRiskTest` passed. Other widths/fonts remain pending. |
| S41 | C | Task 5 | Verified locally in the final JVM gate; `StreakPresentationTest`. |
| S42 | P | Task 5 | Verified locally in the final JVM gate; `StreakPresentationTest`. |
| S43 | C | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`. |
| S44 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`; actual spoken TalkBack behavior remains a device gate. |
| S45 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`. |
| S46 | P | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`, `LayoutMatrixRiskTest`. |
| S47 | E | Deferred to later UI/product work | Deferred: defining the timer arc as milestone progress; review found no existing milestone contract for the seconds-cycle arc. |
| S48 | X | Close original proposal; retain correct behavior | Closed, intentional: PRODUCT_SPEC deliberately omits minutes in all timer tiers; preserve the approved behavior. |
| S49 | E | Deferred to later UI/product work | Deferred: calendar-deterministic verse selection; current specification does not promise a date-fixed daily verse. |
| S50 | P | Task 7 | Verified locally: native run completed all four phases/countdowns, paused while Home was backgrounded, retained Hold progress across Activity recreation, returned to Dashboard, and preserved SQL rows and portable settings. Android's OS-managed `LAST_BACKGROUND_EPOCH` is excluded from the portable-settings byte digest. |
| S51 | C | Task 7 | Verified locally in the final JVM gate; `UiStateRetentionTest`. |
| S52 | C | Task 7 | Verified locally in the final JVM gate; `UiStateRetentionTest`, `TutorialReachabilityTest`. |
| S53 | C | Task 7 | Verified locally in the final JVM gate; `JournalMutationLifecycleTest`, `LayoutMatrixRiskTest`. |
| S54 | C | Task 7 | Verified locally in the final JVM gate; `JournalMutationLifecycleTest`, `UiStateRetentionTest`; process-death draft loss remains intentional. |
| S55 | R | Task 10 | Actual platform fontScale 2 RED reproduced (first export field unreachable), then bounded scrolling passed Settings 5/5 in `resume-layout5-2026-11-01.log`; final actual keyboard input and enabled export action were observed at 320×568/font-scale 2. Phone coverage remains pending. |
| S56 | E | Deferred to later UI/product work | Deferred: password visibility toggle; masking by default is retained and the toggle is a later usability enhancement. |
| S57 | P | Task 8 | Verified locally in the final JVM gate; `SettingsWorkflowTest`. |
| S58 | X | Close original proposal; retain correct behavior | Closed, intentional: preserve short and empty legacy import passphrases for V1 compatibility; `BackupRestoreProtocolTests` covers both. |
| S59 | C | Task 8 | Verified locally in the final JVM gate; `SettingsWorkflowTest`, `BackupRestoreProtocolTests`. |
| S60 | E | Deferred to later UI/product work | Deferred: persistent actionable result feedback; current operation outcomes are reported, while persistent feedback is UI polish. |
| S61 | E | Deferred to later UI/product work | Deferred: filename/MIME filtering; preserve SAF/provider and legacy compatibility and validate backup content instead. |
| S62 | C | Task 8 | Verified locally in the final JVM gate; `SettingsAboutTest`, `SettingsWorkflowTest`. |
| S63 | E | Deferred to later UI/product work | Deferred: undo/destructive styling for clearing a bookmark; it changes only a recoverable reading pointer, not chapter history. |
| S64 | C | Task 7 | Verified locally: `SettingsWorkflowTest`, `BackupRestoreProtocolTests`, optimized backup/Keystore acceptance, and actual SAF export plus import to read-only preview passed. Preview was cancelled; cross-device/provider failure coverage remains pending. |
| S65 | C | Task 10 | Verified locally in the final JVM gate; `SettingsAboutTest`. |
| S66 | P | Task 8 | Verified locally in the final JVM gate; `SettingsAccessibilityTest`, `BiometricPromptPolicyTest`. |
| S67 | C | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`, `TutorialReachabilityTest`. |
| S68 | C | Task 9 | Verified locally in the final JVM gate; `AccessibilityRegressionTest`, `TutorialReachabilityTest`. |
| S69 | P | Task 12 | Verified locally: optimized adaptive and monochrome icon mode passed; launcher masks and OEM appearance remain device/launcher-specific. |
| S70 | E | Deferred to later UI/product work | Deferred: replacing detailed launcher art with a simpler widget glyph; this is a branding/design choice. |
| S71 | R | Task 10 | Minimum-widget bounds RED reproduced then repaired; `WidgetLayoutTest` RED→GREEN and optimized registered-provider check passed. Real launcher cell/font coverage remains pending. |
| S72 | C | Task 3 | Verified locally: `WidgetPrivacyTest`, `ReconciliationUiDataTest`, and optimized registered-widget neutral `RemoteViews` check passed. Other launchers remain untested. |
| S73 | P | Task 11 | Verified locally in the final JVM gate; `ForegroundClockTest`. |
| S74 | P | Task 11 | Verified locally in the final JVM gate; `ForegroundClockTest`. |
| S75 | C | Task 11 | Verified locally in the final JVM gate; `ForegroundClockTest`. |
| S76 | P | Task 11 | Verified locally in the final JVM gate; `ForegroundClockTest`. |
| S77 | P | Task 11 | Verified locally in the final JVM gate; `ForegroundClockTest`, `MilestonePresentationTest`. |
| S78 | C | Task 4 | Verified locally: JVM `MilestonePresentationTest` and `RootNavigationTest` passed; emulator pointer and Back checks are recorded. Broader workflow/OEM conditions remain pending. |
| S79 | R | Task 10 | Fixed 48dp connector gap is cosmetic and deferred under the design freeze; existing long-title bounds passed in `LayoutMatrixRiskTest`. No connector-redraw change warranted. |
| S80 | P | Task 7 | Verified locally in the final JVM gate; `JournalMutationLifecycleTest`, `BackupRestoreProtocolTests`. |
| S81 | C | Task 1 | Verified locally: `SeedCatalogTest` and optimized minified catalog smoke passed; deliberate corrupt-catalog mode showed retryable error, hid protected content, and preserved rows. |
| S82 | C | Task 2 | Verified locally: final JVM startup/reconciliation tests, optimized fresh entry, and force-stop/relaunch passed. Relaunch returned to Dashboard with exactly one session and unchanged Room/DataStore hashes; this is not OS-kill recovery during an interrupted restore. |
| A01 | Source-supported; reproduce regression | Task 11: longest-streak rollover | Focused boundary lifecycle test and final `ForegroundClockTest` passed; active longest-streak refresh on resume does not require a Room emission. Broader device timing remains pending. |

## Handoff and next action

- This plan is being implemented in the existing `main` checkout; source and documentation changes remain in the inherited working tree. Do not restart from Task 1 or treat historical reconciliation results as current verification.
- Continue from the current worklog checkpoint. The final ordinary/optimized build gates, optimized backup acceptance, four native regressions, breathing acceptance, fresh entry/restart, owner-data upgrade hashes, optimized icon/widget/catalog modes, compact light/dark/RTL checks, and real SAF export-to-preview are recorded complete. Parent owns cleanup and authorized commit/push/Desktop delivery. Device-specific acceptance remains separate.
- C/P statuses above are local, evidence-scoped dispositions; they do not establish phone acceptance. Preserve each R disposition's tested conditions. E rows stay deferred and X rows retain their reason.
- At meaningful checkpoints update the root worklog with objective/stage, IDs and source changes, test commands/results, artifact/commit IDs, known limits, dirty state and exact next action. Keep it compact by linking this plan and the review rather than copying their contents.
- Final implementation handoff: list actual verified/remaining IDs, important files, fresh test/build/native evidence, unresolved phone/manual gates and Git state. No claim that all 82 were bugs or all phone checks passed.
