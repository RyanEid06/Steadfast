# V1 acceptance checklist

Use this checklist for an acceptance run. A listed test is not considered passed merely because it is specified here. Local automated evidence and remaining manual/device checks are summarized below.

## Offline and privacy

- [ ] Complete onboarding, streak tracking, Bible tracking, journal, widget, backup export/import, and restart with airplane mode enabled.
- [x] Manifest inspection confirms no `INTERNET` permission and Android auto backup is disabled.
- [ ] Confirm no analytics, ad, or telemetry SDK is present in the packaged app.

## Onboarding

- [ ] Fresh install launches the wizard.
- [ ] Fresh setup requires an explicit recovery focus; user can choose a valid non-future start time, optional OT/NT bookmarks, reading goal **1–5**, theme, and app lock.
- [ ] Completed onboarding does not replay unless restarted from Settings.
- [ ] Interrupt after settings staging, Room insertion, or the final completion write; retry/relaunch preserves Room and preference rows, reconciles setup, and leaves exactly one active initial session. Repeated/concurrent Finish has the same result.
- [ ] Incomplete setup preserves existing journal/Bible/preferences and shown milestone IDs; invalid history enters repair/restore without row deletion or automatic replacement.

## Dashboard and time

- [ ] Portrait layout fits supported normal screens without scrolling; Settings is not a bottom destination.
- [ ] Timer advances each second while visible without per-second Room writes or queries.
- [ ] Display uses days/hours/seconds below 30 elapsed days, adds calendar months from 30 elapsed days, and uses years/months/days/hours from one calendar year.
- [ ] Leap-year, DST, month-end, and stored-zone behavior are correct.
- [ ] Editing a session's date/time after travel preserves its stored zone; invalid edits keep the dialog and draft available for correction.
- [ ] Wide/short window remains usable.

## Restart and journal

- [ ] Start Again requires confirmation and preserves the archived session.
- [ ] Restart chronology rejects future, inverted, and overlapping intervals; longest-streak calculation remains correct.
- [ ] Optional restart note persists and links to its prior streak session.
- [ ] Create, edit, delete, and newest-first journal ordering work offline; long lists remain usable.
- [ ] Struggling → Write Note opens the URGE composer directly.

## Bible and milestones

- [ ] Bundled catalog contains exactly 66 books and 1,189 chapters with correct chapter counts.
- [ ] Chapter completion immediately updates book, testament, and whole-Bible progress.
- [ ] OT and NT bookmarks are independent; each validates its testament and only one per testament is stored.
- [ ] Chapter completion does not move a reading bookmark.
- [ ] Reading goal displays as a motivational minimum and is not presented as measured daily completion.
- [ ] Day milestones use elapsed days; month/year milestones use calendar duration. Celebration history resets when restarting a session and does not repeat recorded IDs.
- [ ] A celebration is a true modal on an idle Dashboard only, stays session-scoped, defers through auth/restore and other workflows, and marks only the acknowledged milestone as shown.

## Struggling flow

- [ ] Reachable in one Dashboard action.
- [ ] Prefers references from completed chapters when available and falls back gracefully.
- [ ] Another Verse avoids immediate repetition.
- [ ] Breathing, Write Note, and View Progress actions work.

## Backup and restore

- [ ] Export creates a `.steadfast` file; wrong passphrase, corrupt, and unsupported files fail without changing user data.
- [ ] Valid round trip restores all user data, settings, bookmarks, and milestone IDs.
- [ ] Preview is read-only; replacement requires confirmation and is all-or-nothing across Room data.
- [ ] Rotation and file-picker return during export, inspection, preview, or restore do not duplicate work or save passphrases/previews into saved state; rejected imports leave existing data unchanged.
- [ ] Cross-device restore, SAF/provider failures, and interrupted-process recovery are accepted on supported devices.

## App lock

- [ ] API 29 enrolled credential and biometric behavior matches the weak-biometric/device-credential policy; API 30+ uses strong-biometric/device-credential policy.
- [ ] Unavailable authentication does not expose content; enabling lock requires available device authentication.
- [ ] Cold start, cancellation, failed credential, retry, background timeout, process restart, and activity recreation remain correctly locked/unlocked.

## Widget and UI quality

- [ ] Widget shows current elapsed day count and opens Dashboard; it refreshes through platform widget updates rather than every second.
- [ ] Widget output contains no recovery-focus label or journal/Bible content for any focus or app-lock setting; a provider refresh during pending restore is neutral, and successful setup/restart/import refreshes coherent content.
- [ ] Widget remains coherent across reboot/app update where Android permits.
- [ ] Minified/resource-shrunk runtime loads every catalog and sentinel without test wiring retaining seed models; optimized onboarding and backup failure/restore flows preserve their rows and settings.
- [ ] Both themes are coherent; text remains legible at common font scales and in landscape.
- [ ] Reduced-motion preference is respected; no dead buttons, placeholder pages, permanent fake data, or repeated loading flicker.

## Current bug-fix acceptance status

Final local bug-fix evidence, 2026-11-02:

- The final ordinary gate passed 166 JVM/Robolectric tests across 34 suites, 0 failures/errors/skips; lint reported 0 errors and 40 warnings. Debug, unsigned release, and Android test APKs assembled. The optimized release-equivalent assembly passed.
- Optimized API 37 backup acceptance passed V2 export/preview/restore, provider and malformed-input errors, rotation, V1 short and empty passphrase imports with actual replacement, and incomplete-flag history reconciliation. The final native regression suite passed 4/4 tests for Keystore backup/preview/SQLite rollback, unavailable-lock retry/recreation, package context, and root navigation.
- Optimized API 37 icon, registered widget-provider, and corrupt-catalog modes passed. Synthetic in-place owner-data upgrades through universal and ARM64 installs preserved the recorded Room and DataStore hashes. ARM64 ran through `libndk_translation` on x86 emulator hardware, not native ARM64 hardware. The APK audit found 384 identical non-native entries across ordinary unsigned, universal, and ARM64 targets; the debug signing identity was unchanged.
- The native breathing run completed all four phases/countdowns, paused while Home was backgrounded, retained Hold progress across Activity recreation, returned to Dashboard, and preserved SQL rows and portable settings. The portable-settings comparison excludes the OS-managed `LAST_BACKGROUND_EPOCH` timestamp that changes on stop.
- Fresh optimized API 37 entry on a new test user verified catalog counts/sentinels, explicit focus, Activity recreation, exactly one initial session, journal/Bible preservation, and relaunch. A separate force-stop confirmed PID exit; relaunch returned to Dashboard without replaying setup and preserved recorded Room/DataStore hashes. This does not cover process death during restore.
- Compact 640×360/font-scale 2 emulator layouts passed in light, dark, and RTL, with live Dashboard units and the action control present. At 320×568/font-scale 2, actual SAF export produced an encrypted backup; selecting it through the document picker reached the expected read-only preview. The preview was cancelled, so a real-provider restore was not performed.
- No GitHub CI result is claimed.

Keep the checklist above unchecked until its individual acceptance condition is observed. Phone/API 29, biometric hardware, real cross-device SAF/provider and OEM behavior, true OS process-kill recovery, spoken TalkBack, and native ARM64 acceptance remain separate manual/device gates. The current checkpoint and known failures are in [the worklog](../CODEX_WORKLOG.md); the canonical scope and per-issue ledger are in the [13-task implementation plan](../Steadfast_V1_Bug_Fix_Implementation_Plan.md). Earlier results below are historical and do not substitute for current or manual acceptance.

## Historical local evidence — reconciliation baseline, 2026-10-29

These results describe the earlier reconciliation snapshot only. They do not verify the current bug-fix diff or mark the checklist above complete:

- Latest checkpoint recorded for that baseline: `testDebugUnitTest lintDebug assembleDebug assembleRelease` exited successfully. The cached JVM result reported **93 tests, 0 failures/errors/skips**. Lint reported **0 errors/fatal issues and 40 warnings**.
- Earlier full integrated gate included **4/4 API 37 instrumentation tests**. They covered app/package startup, unavailable-auth retry/recreation, navigation, and native backup/restore behavior; they are not physical-device acceptance.
- Universal and ARM64 optimized APK outputs were assembled and inspected: universal contains all four supported ABIs; ARM64 contains only `arm64-v8a`. Packaged non-native entries were byte-identical. APK verification/alignment checks passed for the private local-test outputs.
- Manual emulator coverage included credential cancel/wrong/valid PIN, cold start, one prompt rotation, theme/font/landscape, URGE flow, and breathing. A rapid rotate-back while the system prompt was dismissing once left a secure overlay requiring Back; normal and single-rotation paths subsequently passed.
- Physical-device authentication, API 29 hardware/credentials, complete TalkBack/OEM review, cross-device backup, SAF failures, Keystore invalidation, and true OS process-kill boundaries remain unverified.

See [the reconciliation report](RECONCILIATION_REPORT.md) for scope and evidence detail.
