# Product Specification — Steadfast V1

## 1. Product statement

Steadfast is a private, offline Android app built around two equal pillars:

- **Recovery / self-discipline:** track one active recovery focus, maintain a streak, preserve prior streaks, journal, view milestones and motivational scale comparisons, and use an “I’m struggling” intervention screen.
- **Scripture progress:** track completed chapters across the 66-book Protestant Bible, maintain one current OT reading and one current NT reading, see per-book/testament/whole-Bible progress, and set a simple reading target.

The app should feel Christian, peaceful, disciplined, mature, cinematic, and hopeful. It must not feel childish, bubbly, AI-generic, shame-driven, or like a clinical addiction-treatment app.

## 2. Branding

- **Name:** Steadfast
- **Tagline:** A stronger you. A closer walk with God.
- Visual motif: illuminated cross, mountains/horizon, warm gold light, near-black/navy darkness, calm cream light theme.
- Cross is prominent on icon/splash/dashboard hero and restrained elsewhere.
- Hybrid design freeze: `docs/DESIGN_FREEZE.md`. Style 1 is the structural base; Style 3 supplies the color system; Style 2 supplies Journal; Style 3 supplies Book Detail/chapter grid.

## 3. Recovery setup

First-run wizard asks the user to:

1. Choose a hard-coded recovery focus.
2. Set the exact streak start date + time. Future times are invalid.
3. Optionally set current OT and NT reading locations.
4. Choose a reading goal: integer 1–5, displayed as `N+ chapters`.
5. Choose dark/light/system theme.
6. Enable/skip biometric/device-credential lock.
7. Complete a brief product tour.

Recovery focus is editable later. Previous streak sessions retain the focus associated with that session.

## 4. Main streak

Store an exact start instant and the zone ID used at creation.

Dashboard display rules:

- `< 30 elapsed days`: **Days / Hours / Seconds**
- `>= 30 days and < 1 calendar year`: **Months / Days / Hours / Seconds**
- `>= 1 calendar year`: **Years / Months / Days / Hours / Seconds**

The timer updates once per second only while a relevant screen is actively visible. The database must not be queried every second; derive the display from an in-memory start timestamp/state.

Calendar years/months must be real calendar units; never approximate a year as `12 × 30 days`.

## 5. Restart flow

A clear action in Journey allows the user to start again.

Flow:

1. Tap **Start Again**.
2. Confirmation explains that history is preserved.
3. Optional journal note may be added, but no trigger form is required.
4. Current streak is archived with exact end timestamp.
5. New streak starts now by default; user may edit date/time before confirmation.
6. Show encouraging copy. Never use “failure,” “lost everything,” or guilt-heavy copy.

Historical streaks remain available forever unless the app itself is uninstalled or a backup is replaced.

## 6. Journey

Journey contains three internal sections/tabs: **Overview / History / Journal** plus access to Milestones/Motivation.

Required statistics:

- Current streak
- Longest streak

Do not add extra KPI clutter such as reset count or average streak unless approved later.

History shows archived streaks and a clear visual chart. Chart computation must be based on persisted streak sessions and cached/derived efficiently.

## 7. Journal

- Add free-form text notes at any time.
- Notes have creation timestamp.
- Notes may optionally be categorized internally as `GENERAL`, `RESTART`, or `URGE`, but UI must not force a category.
- Sort newest first.
- Edit/delete is allowed with confirmation for delete.
- Journal is included in backup/export.

## 8. “I’m struggling” mode

Must be reachable from Dashboard in one tap.

Screen behavior:

1. Enter a calmer full-screen state with minimal UI.
2. Show a relevant Scripture verse/reference.
3. Prefer verses from Bible chapters the user has already marked completed.
4. If that pool is too small/empty, fall back to the curated global reference catalog.
5. Actions:
   - Another verse
   - Take a deep breath (simple guided timer/animation, not medical treatment)
   - Write a note
   - View my progress
6. Avoid repeating the same verse until the eligible pool has been substantially cycled.

## 9. Milestones

Frozen milestones:

- 1, 3, 7, 14, 21, 30, 60, 90 elapsed days
- 3, 6, 9 calendar months
- 1 calendar year

Milestone pages may show total seconds/minutes/hours plus true, sourced scale comparisons. A celebration may be shown once per milestone per streak session.

Do not make claims such as “21 days creates a habit.” Facts/comparisons must be verifiable and stored locally with source notes before shipping.

## 10. Motivation / impact section

This is separate from the main Dashboard timer and can show:

- Total elapsed seconds
- Total elapsed minutes
- Total elapsed hours
- Carefully verified scale comparisons
- Relevant Scripture references

Use a large offline pool and anti-repeat logic. No network lookup at runtime.

## 11. Bible tracking

- Canon: **66-book Protestant canon**.
- Total chapters: **1,189**.
- Track chapter completion as completed/not completed only.
- No completion date/time persisted.
- Book completion is derived: all chapters complete.
- Show book `completed / total` and percentage.
- Show OT progress, NT progress, and whole-Bible progress.
- A chapter grid uses completed styling vs unread gray styling.
- Tapping a chapter toggles its completion state with clear visual feedback.

### Currently Reading

Maximum two active positions:

- 0–1 Old Testament location
- 0–1 New Testament location

The user can jump to any chapter/book. Completing a chapter **does not automatically advance current reading**. A convenience action such as “Set next chapter as current” may be offered, but only after an explicit user tap.

### Reading goal

Selectable target: 1–5 chapters, displayed as e.g. `2+ chapters`.

V1 does **not** record daily goal completion because chapter timestamps are intentionally not stored. It is an intention/target, not a streak or compliance tracker.

## 12. Verse library

Long-term target: 1,000+ curated verse records spanning OT and NT, including encouragement, warning, repentance, perseverance, love, self-control, peace, temptation, forgiveness, and hope.

Each verse record should include:

- canonical reference
- testament/book/chapter
- thematic tags
- optional importance/popularity rank
- optional text only when legally licensed or supplied by project owner

NLT is the preferred translation for user-facing text, but Gemini must not scrape or bulk-copy NLT text.

## 13. Home-screen widget

V1 includes a discreet Duolingo-style widget:

- Shows `Day N` / current streak and a small Steadfast cross mark.
- Optional next milestone label.
- Tap opens Dashboard.
- No live seconds on the widget; battery-friendly refresh only.
- Must work in both light/dark launcher environments.

## 14. Settings

Settings is opened from a small top-right control, not bottom navigation.

Include:

- Theme: system/dark/light
- Biometric/device credential lock toggle
- Recovery focus
- Streak start date/time edit with strong warning
- Bible reading goal
- Current OT/NT reading management
- Export backup
- Import backup
- Replay tutorial
- About / privacy statement

Do **not** add an “Erase all data” button in V1.

## 15. Privacy and safety

- Offline by design.
- No account, cloud, analytics, ad SDKs, trackers, telemetry, or remote AI.
- Screenshots allowed.
- Biometric/device credential lock optional.
- Journal and recovery data are sensitive; backup security follows `SECURITY_BACKUP.md`.
- For substance-related recovery focuses, show concise safety copy in onboarding/settings: **Steadfast is a tracking/support tool, not medical treatment. Stopping some substances suddenly can be dangerous; seek qualified medical guidance when withdrawal may be a risk.**
- No shame, salvation guarantees, or invented theology. Use Scripture accurately and in context.

## 16. Explicitly out of scope

- Accounts/login
- Cloud sync
- Social features
- Leaderboards
- Ads/monetization
- Push/local notifications
- Bible-reading streak
- Mandatory daily check-ins
- Embedded full Bible text unless rights are resolved
- AI/chatbot/network features
