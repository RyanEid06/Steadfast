# Steadfast S01–S82 independent review

Reviewed 2026-10-29 against `main` at `ac16dad9bcea27cd50afaf8587fd8b7a34712f94`.

The subsequent [canonical implementation plan](../Steadfast_V1_Bug_Fix_Implementation_Plan.md) now owns task order, acceptance and the issue ledger. This document retains the review evidence; its suggested execution outline below is historical guidance superseded by that plan.

## Decision

**Improve the plan before implementing it.** It identifies substantial real defects, including two credible entry blockers, but it is not a list of 82 independently demonstrated bugs. It mixes confirmed defects, partially accurate claims, untested layout risks, optimizations, and changes to deliberate product behavior. Several proposed fixes would regress existing requirements.

This review fulfills the request to validate the attachment before implementation. Application source, dependencies, product decisions, and the supplied attachment have not been changed. The original report's implementation commands, release rules, and handoff format are proposals under review, not authorization to execute them.

All 82 IDs are accounted for: **31 confirmed in source/artifacts, 22 partially accurate or overstated, 15 requiring runtime reproduction, 11 enhancements/product choices, and 3 incorrect or conflicting proposals**. Partial findings often contain a smaller worthwhile fix; these counts do not mean 53 independent reproduced bugs.

Treat S81/S82 as entry blockers; prioritize S01 privacy, S02 contrast, S06/S78 modal/workflow interference, S26 zone preservation, and S64 operation ownership next. Other confirmed functional/accessibility defects belong in the bug pass. Do not make low-impact artwork or speculative micro-optimization a prerequisite for repairing entry and data correctness.

## Baseline and evidence

- Local HEAD and GitHub `main` match. GitHub was verified with `git ls-remote origin refs/heads/main`; the connector was unavailable, so Git transport supplied this check.
- The supplied debugging ZIP has SHA-256 `599e2e7a6034989003d26900b4f4cde46213e716472e8d3f35ed302d1cc19391`. All 96 app/Gradle/config entries compared byte-for-byte with committed Git blobs match. Working-copy line endings differ from Git blobs in some files; that is not a source divergence.
- All five JSON assets parse as JSON and are present in the freshly assembled release APK: 18 recovery focuses, 66 Bible books / 1,189 chapters, 12 milestones, 8 verse themes, and 21 scale comparisons. This establishes asset contents, not successful Moshi parsing at runtime.
- Fresh command: `gradlew.bat testDebugUnitTest assembleRelease --offline --no-daemon`. Exit 0 in 3m41s; 79 tasks, 46 executed / 33 from cache. The test task executed: **93 tests, 0 failures, 0 errors, 0 skips across 13 suites**. Universal and ARM64 unsigned release APKs assembled; release vital lint also ran as a dependency. Full debug lint and instrumentation were not run in this review.
- Both installed JDKs initially failed in host AF_UNIX selector setup. Verification used a process-only JDK21 patch compiled from installed JDK source to use authenticated TCP loopback pipes. Installed JDK files and app/runtime code were unchanged. A KSP internal AWT disposal exception appeared, but the build completed with exit 0.
- R8 `usage.txt` from this build lists removal of seed-model fields and constructors, including `RecoveryFocus.id/label`, `RecoveryFocusesData.items/label`, `BibleBooksData.books`, and the milestone/comparison fields. Mapping also shows seed containers renamed/optimized. This is direct artifact evidence for the release reflection defect; assembly success does not establish that release catalogs work.
- No Android target was attached and no AVD was listed. No device was installed, cleared, or modified. The supplied plan calls S81/S82 device-confirmed, but this attachment does not include the original phone screenshots, APK hash, logs, or reproduction record. Phone-specific acceptance remains pending.
- Fresh test XML, build log, source/ZIP check, and R8 metadata are retained outside Git in this chat's `steadfast-report-review` evidence directory.

## Highest-priority conclusions

### S81: the release seed defect deserves first priority

[`AssetSeedLoader.kt`](../app/src/main/java/com/steadfast/app/data/seed/AssetSeedLoader.kt) uses `KotlinJsonAdapterFactory` (17–19), then catches parsing/adapter exceptions and returns empty catalogs (21–68). All eight classes in [`SeedModels.kt`](../app/src/main/java/com/steadfast/app/data/model/SeedModels.kt) lack `@JsonClass`; the existing rules protect annotated classes/fields, not these models. The actual R8 output removes reflection-required members. The valid JSON and packaged assets rule out a missing-source-data explanation for this baseline.

Use the already configured Moshi KSP codegen for every seed wrapper and nested item type; no dependency upgrade is needed. Make catalog loading/validation explicit and propagate an actionable error. Use one coherent bootstrap result for essential catalogs, with retry; avoid crashes, empty lists masquerading as success, and five independent ad hoc fallback paths. Generate adapters first, then verify both APK outputs and backup validation against them. Moshi's [official R8 guidance](https://github.com/square/moshi#r8--proguard) explicitly requires protection for reflectively serialized classes.

The automatic sensitive selection is independently real: onboarding line 116 chooses the first focus or `pornography`, and Continue is not gated on a valid explicit choice. Also inspect fallback use in preferences, restart, and legacy backup conversion rather than deleting every default without considering old-backup compatibility.

### S82: the startup lockout follows directly from the source

[`MainActivity.kt`](../app/src/main/java/com/steadfast/app/MainActivity.kt):507 routes only from the onboarding flag. Finishing calls `startInitialSession` at 519 before persisting setup and the completion flag at 520–525. [`SteadfastRepository.kt`](../app/src/main/java/com/steadfast/app/data/repository/SteadfastRepository.kt):30 rejects any existing history. Therefore a successfully inserted session followed by interrupted/failed setup leaves a retry path that always rejects the existing session. The error instructs the user to use an action inside the inaccessible app.

Preserve the duplicate-initial-session guard. Repair startup/completion idempotently after pending-backup recovery and before choosing a route. Do not delete history or silently make an archived session active. Room and DataStore do not share a single transaction: define ordering, durable completion conditions, and retry behavior explicitly. Cover all four history/flag combinations plus interrupted setup, repeated finish/launch, no-active-but-archived history, multiple active sessions, and invalid/missing focus data. Inconsistent history should receive a repair/setup path, not fabricated chronology.

### Privacy, modal behavior, and time accuracy are real bugs

- S01: [`SteadfastWidgetProvider.kt`](../app/src/main/java/com/steadfast/app/widget/SteadfastWidgetProvider.kt):34 publishes the active recovery focus without an app-lock privacy gate. Use neutral content and explicitly republish existing widgets so old sensitive text disappears.
- S06/S78: the celebration is a plain Box inside Scaffold content (`MainActivity`:736–761), below the bottom navigation and without modal input/focus/Back handling. Its producer at 432–438 has no safe-route restriction. Fix presentation and deferral together; do not interrupt onboarding, import, journaling, authentication, or recovery work.
- S26: `updateSessionStart` defaults to the current device zone (`SteadfastRepository`:87), and its Settings caller omits a zone (`MainActivity`:705). Editing can overwrite the stored zone even when the user never requested that change. S25/S27 concern formatting; duration calculation already uses the stored zone and should be preserved.

## Full finding disposition

`C` = confirmed concrete defect or missing specified behavior; fix in the bug pass. `P` = partially accurate/overstated; fix only the supported portion. `R` = plausible source risk; reproduce before choosing a fix. `E` = enhancement or product/design decision; defer to UI work. `X` = incorrect or conflicting proposed change; close/correct it. These are review verdicts, not completion marks. Line numbers refer to this review's baseline.

Additional source locations for the UI/state rows: [Bible](../app/src/main/java/com/steadfast/app/ui/bible/BibleScreen.kt) 64–69, 141–264, 332–368; [Onboarding](../app/src/main/java/com/steadfast/app/ui/onboarding/OnboardingWizardScreen.kt) 112–131, 166–181, 388–462, 820–840, 1019–1055, 1147–1189; [Settings](../app/src/main/java/com/steadfast/app/ui/settings/SettingsScreen.kt) 122–135, 205–230, 273–345, 397, 441–488, 544–680, 707–830; [Journey](../app/src/main/java/com/steadfast/app/ui/journey/JourneyScreen.kt) 108–114, 362–450, 634–707, 769–799, 953–998, 1043–1089; [Struggling](../app/src/main/java/com/steadfast/app/ui/struggling/StrugglingInterventionScreen.kt) 80–84, 114–119, 186–253, 256–342; [Breathing](../app/src/main/java/com/steadfast/app/ui/struggling/BreathingExerciseScreen.kt) 66–89, 103–205; [Tutorial](../app/src/main/java/com/steadfast/app/ui/onboarding/TutorialReplayScreen.kt) 98, 134–185, 237–299; [Theme](../app/src/main/java/com/steadfast/app/ui/theme/Theme.kt) 40–43 and [Typography](../app/src/main/java/com/steadfast/app/ui/theme/Type.kt) 68–108; [Milestones](../app/src/main/java/com/steadfast/app/ui/milestones/MilestonesScreen.kt) 61–80, 185–218; [Motivation](../app/src/main/java/com/steadfast/app/ui/motivation/MotivationScreen.kt) 47–61, 110–127, 211–215. The [Dashboard compact regression](../app/src/test/java/com/steadfast/app/DashboardCompactLayoutTest.kt) covers 900×360dp at 1.3× font, not the whole proposed matrix.

| ID | Verdict | Evidence and action |
|---|---|---|
| S01 | C | Widget provider:31–35 publishes the recovery focus. Neutralize and refresh existing widgets; privacy priority. |
| S02 | C | Theme/Color and widespread direct gold uses produce low-contrast light text/buttons. Correct semantic color pairs, not a blanket replacement of decorative gold. |
| S03 | C | OnboardingWizard:112–131 stores wizard step/choices in plain `remember`; recreation resets them. Retain appropriate state. |
| S04 | P | MainActivity:173–175 already gates restore/auth/onboarding loading, but content flows at 387–400 still start with null/empty/fake defaults. Extend readiness to actual required state. |
| S05 | C | MainActivity:615 initially emits null; BookDetail:82–87 treats it as “Book not found,” without its normal Back control. Distinguish loading from unknown ID/catalog failure. |
| S06 | C | MainActivity:736–761 uses an ordinary content Box, not a modal window; bottom bar is outside it. Add modal focus/input/Back handling with S78. |
| S07 | P | Dashboard:88–168 already has a wide/short branch and a matching regression test. Remaining fixed-size portrait/narrow landscape/font risks need targeted reproduction; preserve the no-scroll freeze. |
| S08 | R | Struggling uses a fixed non-scrolling composition with substantial content. Reproduce actual unreachable controls in the size/font matrix before reflow. |
| S09 | R | Breathing uses fixed ring/spacing inside a weighted center. Reduced motion already works; test narrow/short layouts separately. |
| S10 | R | Tutorial replay has a fixed composition; onboarding already has scroll fallbacks on several steps. Test replay at short height/large fonts. |
| S11 | C | BibleScreen accepts `readingGoal` without displaying it; UI_SPEC explicitly requires it. Display the intention without inventing daily completion tracking. |
| S12 | P | Empty Currently Reading card says “Tap book to set,” while its own click is gated on a non-null location. This may refer to the book list below; clarify the instruction or provide a direct selection action. |
| S13 | P | BookDetail explains the long-press shortcut, so it is not completely undocumented; the actual bookmark action remains behind long press. Add a visible accessible action in the functional pass. |
| S14 | C | Settings exposes clear actions for OT/NT bookmarks but no set/change editor; required management flow is incomplete. Reuse a shared validated picker. |
| S15 | C | Settings formats bookmark IDs as `Book N` rather than canonical names. Resolve through the loaded catalog. |
| S16 | R | Bible filters are a fixed Row. Text expansion/font scaling can overflow; verify actual bounds and then wrap/scroll as needed. |
| S17 | R | Journal filters are a fixed Row. Journey tabs themselves already have selectable roles. Verify overflow before adapting. |
| S18 | R | Composer context options are a fixed Row inside a dialog. Test narrow/large-font/IME conditions. |
| S19 | E | Onboarding chapter selection uses repeated increment/decrement; inefficient for Psalms, but valid selection works. Add direct entry during later UI improvement. |
| S20 | E | Onboarding does not apply its pending theme choice until completion. Live preview is a product improvement, not failed theme persistence. |
| S21 | E | Finish summary omits exact selected OT/NT locations. Add confirmation during UI work; persistence receives the choices correctly. |
| S22 | P | Safety copy exists in onboarding and About; changing to a substance focus in Settings lacks contextual presentation. Reuse approved copy; do not claim all safety guidance is absent. |
| S23 | C | Settings native picker and Start Again picker initialize Calendar from now rather than their draft/stored timestamp. Seed both date and time correctly. |
| S24 | P | Repository validation already prevents invalid/future/overlapping writes. Settings dismisses before validation; Start Again also needs chronology-aware inline feedback. Keep drafts open on rejection. |
| S25 | C | Settings renders session start in `ZoneId.systemDefault()` rather than `startZoneId`. Correct formatting and show the zone. |
| S26 | C | Repository edit defaults to the system zone; MainActivity caller omits it. Preserve original zone unless explicitly changed. |
| S27 | C | Journey archived dates use systemDefault while duration uses saved zone. Display consistent historical zone context. |
| S28 | C | Journey historical focus uses underscore replacement, not catalog labels. Resolve through the same catalog used elsewhere. |
| S29 | P | Focus cards already show a check icon with “Selected”; the missing piece is explicit selectable/radio group state and role semantics. |
| S30 | X | Focus row title line height plus 28dp vertical padding already meets 48dp; Material interactive sizing also applies. Verify bounds, do not label it a demonstrated undersized control. |
| S31 | P | Theme cards have a visible selection cue but lack explicit selected/radio semantics. Repair semantics without redesigning the cards. |
| S32 | P | Struggling does not consult the shared reduced-motion policy for verse fades. Breathing/onboarding already do; confirm system-zero-duration behavior and honor the shared override. |
| S33 | C | Struggling currentVerse starts null and its verse content is conditional on asynchronous selection. Provide truthful stable loading/fallback content. |
| S34 | E | Multiple exits/actions are a design critique, not broken navigation. Defer restructuring to UI improvement. |
| S35 | R | Struggling scripture metadata shares a Row without robust width allocation. Test long references/large fonts before changing layout. |
| S36 | R | Book-detail instruction row mixes long text and an affordance. Reproduce collision at narrow widths/font scaling. |
| S37 | R | Bible progress summary places texts at opposite edges. Verify bounds at maximum counts/fonts before restructuring. |
| S38 | R | Bible OT/NT cards use equal-weight columns. Test narrow/large-font readability; two columns alone do not establish a bug. |
| S39 | R | Journey statistics use fixed columns. Verify clipping/reachability before adapting. |
| S40 | R | Motivation totals use three weighted columns and can grow substantially. Test long values and font scaling. |
| S41 | C | Journey:661–684 uses integer totalDays and a 10% minimum bar height. Different sub-day durations become identical zero-day bars. Use truthful continuous duration and short-duration labels. |
| S42 | P | Chart already has accessibility duration summary (673–674), but lacks dates/visual labels. Correct time/data identification with S41; axis styling belongs to UI work. |
| S43 | C | ASCII directional chevrons do not mirror with layout direction. Replace directional symbols with the existing auto-mirrored icon pattern. |
| S44 | P | MainActivity:482 repeats the destination text in icon contentDescription and visible label. Remove redundant decorative descriptions; actual TalkBack duplication needs device validation. |
| S45 | P | Navigation has semantic selected state, but transparent indicator and equal-weight labels make visible selection largely color-dependent. Restore a non-color cue; do not claim selection semantics are absent. |
| S46 | P | Type scale includes small text and timer labels reach 8–10sp. Readability warrants targeted corrections; sp still scales, and not every small decorative caption is a functional blocker. |
| S47 | E | Circular timer:79 draws a seconds-cycle arc; it is not bound to a milestone. Define desired ring meaning during UI work; do not silently add a new progress contract. |
| S48 | X | PRODUCT_SPEC section 4 deliberately omits minutes in all three timer tiers. Preserve the existing behavior unless the owner changes that product decision. |
| S49 | E | VerseRepository:190 chooses randomly; MainActivity:406 retains per composition. Current UI says “Scripture of Peace,” and specs do not promise a calendar-daily selection. Date-deterministic behavior is optional. |
| S50 | P | Breathing phase/count use `remember`; recreation resets them. Decide/document safe restart versus retention. Do not advance breathing phases unseen while backgrounded. |
| S51 | C | Bible filter uses plain remember. Retain the filter through recreation. |
| S52 | C | Tutorial step uses plain remember. Retain it through recreation. |
| S53 | C | Journey:706–707 search/filter live inside the conditional Journal tab and reset even when switching tabs. Hoist/retain them; selected Journey tab is already saveable. Search text can be sensitive; prefer in-memory rotation retention unless persistence is deliberately approved. |
| S54 | C | Start Again dialog visibility/date/note use transient composition state. Retain rotation drafts in memory; do not persist sensitive notes as an incidental fix. |
| S55 | R | Backup dialog bodies lack explicit scrolling. Material AlertDialog/IME handling alone does not establish failure; test visible fields/actions under IME and large fonts. |
| S56 | E | Password masking lacks a visibility toggle. Useful typo-prevention/accessibility improvement; defer with UI controls, preserving masking by default. |
| S57 | P | Export already marks mismatch as an error and disables invalid submission; it lacks supporting mismatch explanation. Add clear text without weakening validation. |
| S58 | X | SECURITY_BACKUP:22 explicitly supports short/empty legacy import passphrases. Disabling Inspect for blank input would break compatibility. Keep import behavior and test empty legacy backups. |
| S59 | C | Settings includes raw exception messages in export/inspection/import toasts. Map to stable user errors; never log sensitive payloads/passphrases. |
| S60 | E | Outcomes use Toasts and can be missed. Persistent actionable feedback is worthwhile UI polish; operation success/failure is already reported. |
| S61 | E | `OpenDocument` accepts `*/*`, while export uses application/octet-stream and UI already says .steadfast. Preserve SAF/provider and legacy compatibility; validate content, not a filename suffix. |
| S62 | C | Settings:397/805 hardcodes 1.0.0 while Gradle versionName is 1.0. Replace both displays with BuildConfig.VERSION_NAME; small, concrete metadata fix. |
| S63 | E | Clear bookmark is immediate, but does not delete chapter progress/history. Undo/destructive styling is a UI policy choice; avoid blanket confirmation for a recoverable pointer. |
| S64 | C | Settings workflow visibility, selected URI, preview and operations use composition-scoped state/scope. Rotation loses/cancels work. Retain an operation owner and in-memory workflow state; never save passphrases/previews to SavedStateHandle. |
| S65 | C | Settings:807–830 constrains long About/safety text to a 280dp non-scroll Column. Remove the fixed non-scroll constraint; verify complete safety notice reachability at 2× font. |
| S66 | P | Lock capability is already checked when enabling and rejection explains unavailability; Settings lacks persistent inline status. Improve capability feedback without bypassing authentication or permanently disabling a stale capability result. |
| S67 | C | Onboarding position dots lack step/progress semantics. Add “Step X of Y” and meaningful traversal. |
| S68 | C | Tutorial position dots have the same omission. Share the semantic approach. |
| S69 | P | Adaptive wrappers and a 66dp foreground inset already exist, but foreground/monochrome reuse detailed opaque raster art. Correct monochrome construction; inspect masked foreground quality before claiming a safe-zone failure. |
| S70 | E | Widget uses detailed launcher artwork at 32dp. A simpler glyph is a design improvement; preserve branding until the UI pass. |
| S71 | R | Widget minHeight is 110dp with padding and several text rows. Test real launcher cell sizing/font scaling before claiming clipped content. |
| S72 | C | No app-triggered widget updates exist outside provider onUpdate; XML requests hourly refresh. Restart/import/focus changes can leave stale or sensitive content. Refresh at coherent mutations and safe boundaries, never every second. |
| S73 | P | Motivation reads a 1Hz clock and duration in the screen/static items. Isolate the live seconds where useful; measure actual recompositions/cost before declaring a performance regression. |
| S74 | P | Milestones evaluates its small catalog each second. Work is unnecessary, but unmeasured severity is overstated; fix threshold keys with the shared clock work. |
| S75 | C | MainActivity:409 enables the root 1Hz ticker for any active session, including Bible/Settings. Scope visual time updates to relevant routes/boundaries. |
| S76 | P | Root plus screen clocks can coexist; Journey also has a chart ticker. Shared helper already exists and stops in background. Remove redundant active subscriptions without building a global ticking state that invalidates every screen. |
| S77 | P | MainActivity:432 calls getPendingCelebration each duration tick; repository:63 recollects DataStore flow. This is cached preference access, not proof of disk I/O every second. React to thresholds/shown-set changes. |
| S78 | C | Celebration selection has no route/workflow safety check. Defer until Dashboard or another explicit safe boundary and pair with S06. |
| S79 | R | Milestone connector is fixed at 48dp while cards can wrap. A timeline alignment risk, usually cosmetic; reproduce with long title/large font before changing drawing. |
| S80 | P | Model/repository permit nullable context and Save requires only a body, but composer always assigns a category/default GENERAL. Resolve the optional-context meaning consistently; a new required category step is unnecessary. |
| S81 | C | Confirmed source and R8 artifact defect: unprotected reflective seed members are removed and failures return empty data. Exact affected-phone reproduction remains pending. Fix startup catalogs and explicit selection first. |
| S82 | C | Onboarding flag selects wizard, finish always calls guarded initial creation, and Room insertion precedes final setup flag. Repair startup/completion idempotently; preserve the guard/history. |

## Improvements required in the correction plan

1. **Keep the IDs; change what their status means.** Track accepted bug, narrowed claim, needs reproduction, intentional behavior, deferred enhancement, implemented, and verified separately. Multiple IDs can share a root cause and one checkpoint. Do not call an ID fixed simply because code changed or delete an ID to inflate progress.
2. **Correct unsafe/conflicting requirements before work starts.** Keep empty legacy import support (S58), frozen minute omission (S48), the non-scrolling Dashboard, neutral-only widgets, offline behavior, and sensitive-state handling. Avoid mandatory filename/MIME filters that hide valid backups. Default focus removal must account for legacy payload interpretation; never silently reclassify a user's history.
3. **Strengthen the startup repair contract.** Cover all four history/flag combinations, valid versus absent versus corrupt catalogs, interrupted completion at every persistence boundary, retry/double finish, app-lock enable, no active session with archived history, and corrupt multiple-active state. Do not equate “some history exists” with a valid active session. Keep restore recovery and lock gates ahead of protected content.
4. **Test the actual optimized app.** Current JVM tests plus release assembly missed S81. Require a smoke test on the minified APK that asserts all catalog counts/known sentinel records and completes onboarding. Use the same code, R8 rules and shrinking as shipped; changing a test build to disable optimization would invalidate this evidence. Record APK hash, ABI, package, version, API/device, and signing identity. Compare fresh install and upgrade with synthetic data, without uninstalling/wiping user data.
5. **Include backup consumers of catalogs.** BackupManager validates focus/milestone IDs from these seed loaders. Test optimized export/import, current schema plus V1/schema1 short/empty passphrases, malformed/wrong-password files, preview non-mutation, rollback, and pending-restore recovery after the seed/startup change. Preserve existing security tests rather than rebuilding backup architecture.
6. **Separate lifecycle ownership from persistence.** Retain non-sensitive selections with saveable state. Keep passphrases, decrypted previews, sensitive search text, and sensitive draft bodies in a deliberately scoped in-memory owner for configuration recreation; clear them on workflow completion/cancel as appropriate. New-journal body already uses saveable state while the edit target uses plain remember; audit this inconsistency rather than converting everything to saveable state. Test native file picker return, edit-versus-new draft retention, background/resume breathing behavior, and import/export interruption, not just dialog visibility. Existing restore recovery remains authoritative; lost UI state does not prove database corruption.
7. **Reproduce layout risks before prescribing changes.** Use available content area (after system bars/navigation/IME), not screen pixels alone. Exercise small portrait, narrower short landscape, split screen, long catalog labels/large counts, 1.0/1.3/1.5/2.0 fonts, both themes, and RTL. Assert full primary-action/text bounds and actual tapping; merely asserting a node exists or is partly displayed can miss clipping. Accessibility reachability, contrast and touch targets are bug fixes even though visual redesign is deferred.
8. **Merge related work and reduce speculative optimization.** S06/S78 share presentation ownership; S03/S82 share onboarding completion; S02/S45/S46 share accessible tokens; S25–S27 share stored-zone policy; S73–S77 share clock/threshold subscriptions. Preserve live seconds on relevant surfaces, foreground-only behavior, in-memory duration calculation, and already-working chapter/auth/restore validation. Measure performance instead of treating every 12-item recomputation as a release blocker.
9. **Make release gates proportional.** Require accepted blockers/correctness/accessibility/privacy defects and their regressions to pass. Unreproduced risks need disposition; deliberate product behavior and deferred visual enhancements need decisions, not arbitrary “bug fixed” marks. Keep real-phone, emulator, JVM, build, and CI evidence separate; there is no GitHub Actions result for this review.

## Suggested execution order after this review

| Checkpoint | Scope | Required proof |
|---|---|---|
| 1. Entry and privacy | S81/S82, explicit focus, essential bootstrap/readiness, S01 + stale widget republish | Optimized catalog/onboarding smoke; all history/flag states; failure/relaunch/double-finish; unchanged user data; widget privacy |
| 2. Correctness and durable interactions | S03–S06/S78, S23–S28, S41, S51–S54/S64, confirmed missing Bible actions | Loading states; modal input/Back; stored-zone edits/travel; sub-day chart; recreation/file-picker/operation regressions; current-reading set/change/clear |
| 3. Accessibility and proven layout defects | S02, narrowed semantics/reduced-motion/typography findings, risks reproduced by the matrix | Contrast calculations and rendered checks; semantic tree + TalkBack; reachable 48dp actions; font/IME/RTL/window matrix |
| 4. Clock/widget consistency and final bug regression | S72/S75 and supported parts of S73–S77, actual remaining accepted bugs | No ticker on unrelated routes; threshold/day rollover and shown-set behavior; prompt widget updates; existing JVM/lint/build/native regressions |
| Later UI work | Direct chapter entry, theme preview, finish summary, action simplification, ring meaning, optional calendar-daily Scripture, password visibility/feedback, artwork polish | Owner's UI requirements and visual acceptance; no silent change to frozen behavior |

Checkpoints are coherent bug-fix groups, not permission to postpone an independently confirmed blocker. Implementation can start with checkpoint 1 once this corrected scope is adopted; the original nine-phase plan should not be executed verbatim.

## Additional issue found during this review

**A01 — Longest streak can remain stale across a day boundary (source-confirmed; regression reproduction pending).** `MainActivity`:426–428 calculates it with `remember(allSessions)`, while the active session keeps growing without a Room emission. Journey then receives a stale value even though the current streak updates. Include an active-streak day-boundary/resume test in the clock work and key the derived maximum to a meaningful boundary. This is absent from S01–S82.

## Limits

This is a full per-ID source/plan review with fresh JVM/build and R8 artifact evidence, not 82 physical-device reproductions or a claim that the app is ready for release. The layout/font/TalkBack/launcher matrix and the affected phone's optimized startup scenario remain unrun. No bugs have been marked fixed, no application change committed, and no push/release performed by this review.
