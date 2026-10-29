# V1 acceptance checklist

Use this checklist for an acceptance run. A listed test is not considered passed merely because it is specified here. Local automated evidence and remaining manual/device checks are summarized below.

## Offline and privacy

- [ ] Complete onboarding, streak tracking, Bible tracking, journal, widget, backup export/import, and restart with airplane mode enabled.
- [x] Manifest inspection confirms no `INTERNET` permission and Android auto backup is disabled.
- [ ] Confirm no analytics, ad, or telemetry SDK is present in the packaged app.

## Onboarding

- [ ] Fresh install launches the wizard.
- [ ] User can choose a recovery focus, valid non-future start time, optional OT/NT bookmarks, reading goal **1–5**, theme, and app lock.
- [ ] Completed onboarding does not replay unless restarted from Settings.

## Dashboard and time

- [ ] Portrait layout fits supported normal screens without scrolling; Settings is not a bottom destination.
- [ ] Timer advances each second while visible without per-second Room writes or queries.
- [ ] Display uses days/hours/seconds below 30 elapsed days, adds calendar months from 30 elapsed days, and uses years/months/days/hours from one calendar year.
- [ ] Leap-year, DST, month-end, and stored-zone behavior are correct.
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
- [ ] Day milestones use elapsed days; month/year milestones use calendar duration. Celebration history resets for a new session and does not repeat recorded IDs.

## Struggling flow

- [ ] Reachable in one Dashboard action.
- [ ] Prefers references from completed chapters when available and falls back gracefully.
- [ ] Another Verse avoids immediate repetition.
- [ ] Breathing, Write Note, and View Progress actions work.

## Backup and restore

- [ ] Export creates a `.steadfast` file; wrong passphrase, corrupt, and unsupported files fail without changing user data.
- [ ] Valid round trip restores all user data, settings, bookmarks, and milestone IDs.
- [ ] Preview is read-only; replacement requires confirmation and is all-or-nothing across Room data.
- [ ] Cross-device restore, SAF/provider failures, and interrupted-process recovery are accepted on supported devices.

## App lock

- [ ] API 29 enrolled credential and biometric behavior matches the weak-biometric/device-credential policy; API 30+ uses strong-biometric/device-credential policy.
- [ ] Unavailable authentication does not expose content; enabling lock requires available device authentication.
- [ ] Cold start, cancellation, failed credential, retry, background timeout, process restart, and activity recreation remain correctly locked/unlocked.

## Widget and UI quality

- [ ] Widget shows current elapsed day count and opens Dashboard; it refreshes through platform widget updates rather than every second.
- [ ] Widget remains coherent across reboot/app update where Android permits.
- [ ] Both themes are coherent; text remains legible at common font scales and in landscape.
- [ ] Reduced-motion preference is respected; no dead buttons, placeholder pages, permanent fake data, or repeated loading flicker.

## Observed local evidence

These results describe the reconciliation work and do not mark the manual checklist above complete:

- Latest offline Gradle checkpoint: `testDebugUnitTest lintDebug assembleDebug assembleRelease` exited successfully. The cached JVM result reported **93 tests, 0 failures/errors/skips**. Lint reported **0 errors/fatal issues and 40 warnings**.
- Earlier full integrated gate included **4/4 API 37 instrumentation tests**. They covered app/package startup, unavailable-auth retry/recreation, navigation, and native backup/restore behavior; they are not physical-device acceptance.
- Universal and ARM64 optimized APK outputs were assembled and inspected: universal contains all four supported ABIs; ARM64 contains only `arm64-v8a`. Packaged non-native entries were byte-identical. APK verification/alignment checks passed for the private local-test outputs.
- Manual emulator coverage included credential cancel/wrong/valid PIN, cold start, one prompt rotation, theme/font/landscape, URGE flow, and breathing. A rapid rotate-back while the system prompt was dismissing once left a secure overlay requiring Back; normal and single-rotation paths subsequently passed.
- Physical-device authentication, API 29 hardware/credentials, complete TalkBack/OEM review, cross-device backup, SAF failures, Keystore invalidation, and true OS process-kill boundaries remain unverified.

See [the reconciliation report](RECONCILIATION_REPORT.md) for scope and evidence detail.
