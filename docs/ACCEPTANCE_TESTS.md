# V1 Acceptance Tests

A build is not “done” until these pass.

## Offline/privacy

- App completes onboarding, streak tracking, Bible tracking, journal, widget, backup export/import and restart with airplane mode enabled.
- Release manifest has no unnecessary INTERNET permission.
- No analytics/ad/telemetry SDK detected.

## Onboarding

- Fresh install launches wizard.
- User can choose recovery focus, exact non-future start time, optional OT/NT current reading, 1–5+ reading goal, theme, and lock.
- Reopening after completion does not replay onboarding unless requested from Settings.

## Dashboard

- No scrolling on normal supported portrait screens.
- Counter updates each second without DB writes/queries each second.
- `<30d` shows D/H/S; `>=30d` adds calendar month; `>=1y` adds calendar year.
- Leap-year and DST tests pass.
- Settings is not a bottom-nav item.

## Recovery restart/history

- Start Again requires confirmation.
- Old session remains in history.
- Longest streak remains correct.
- New streak starts at requested timestamp.
- Optional journal note survives restart.

## Journal

- Create/edit/delete works offline.
- Newest-first ordering is stable.
- Large journal histories scroll smoothly without repeated loading flicker.

## Bible

- Exactly 66 books / 1,189 chapters available.
- Chapter counts match `data/bible_books.json`.
- Toggling a chapter immediately updates book/testament/whole progress.
- Current OT and NT pointers are independent and capped at one each.
- Completing a chapter never silently moves current reading.
- Reading goal displays but does not pretend to know daily completion.

## “I’m struggling”

- Reachable in one tap from Dashboard.
- Prioritizes verse references from completed chapters when available.
- Falls back gracefully when none exist.
- Another Verse avoids immediate repetition.
- Breathe, Write Note, View Progress actions work.

## Milestones

- Every frozen milestone triggers correctly.
- Celebration does not repeat every launch.
- 90 days and 3 calendar months are treated independently.

## Backup

- Export creates a `.steadfast` file.
- Wrong passphrase fails safely without data mutation.
- Valid round trip restores all user data.
- Corrupt/unsupported backup is rejected.
- Import is transactional.

## Lock

- Biometric and device credential paths work.
- No duplicate prompts or stuck overlay on lifecycle changes.
- Back/foreground timeout behavior is consistent.

## Widget

- Shows current day count and opens Dashboard.
- Does not try to update every second.
- Survives reboot/app update where platform permits.

## UI quality

- Both themes are coherent.
- No purple AI-gradient/bubble aesthetic.
- No clipped text at common font scales.
- Reduced-motion preference is respected.
- No dead buttons, placeholder pages, permanent fake data, or repeated spinners.
