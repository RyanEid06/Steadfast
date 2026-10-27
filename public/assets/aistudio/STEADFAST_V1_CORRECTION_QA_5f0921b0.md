# Steadfast V1 — Correction & QA Handoff

This document is the authoritative correction pass after reviewing the first generated Android implementation.

## Goal

Repair the current implementation without redesigning the app. Preserve the approved hybrid design freeze:
- Style 1: overall structure, app identity, onboarding/tutorial, circular Dashboard streak
- Style 3: dark/light palette and Bible chapter grid
- Style 2: Journal visual treatment
- Bottom navigation: Dashboard / Journey / Bible only
- Settings: top-right access, not a fourth bottom-nav destination

Do not restart the project. Fix the existing implementation.

---

## P0 — Must Fix Before Device Testing

### 1. Local privacy claims must become true
The UI currently claims journal content is encrypted on-device, while the Room database is plain SQLite.

Choose ONE correct implementation:
- Preferred: encrypt sensitive local persistence properly using an established Android-compatible encrypted database/storage approach; OR
- If that is not safely achievable in this pass, remove every claim that local journal/database contents are encrypted.

The exported `.steadfast` backup encryption must remain PBKDF2 + AES-GCM.

Also:
- disable Android Auto Backup for sensitive local app data, or explicitly exclude Room/DataStore/private recovery data from system/cloud backup.
- verify `android:allowBackup` and backup rules are consistent with the privacy claim.
- no INTERNET permission.

### 2. Scripture correctness
Current implementation can display a verse reference that does not match the displayed text.

Requirements:
- Every visible Scripture quote must be bound to the exact matching reference.
- Never invent, merge, paraphrase, or substitute Scripture while presenting it as a quotation.
- Remove the generic fallback quote that combines unrelated verses.
- If verse text is unavailable, show the reference only or use a safe locally curated entry whose text/reference pair is verified.
- Do not bulk-copy copyrighted NLT text. Keep the verse system structured so licensed/user-provided verse text can be added later.
- Existing user-visible verse text should only remain when its exact source wording and reference are verified.

### 3. “I’m Struggling” verse engine
- completed-chapter prioritization must work.
- anti-repeat cycling must work.
- do not fall back to fake/generic combined Scripture.
- if no verified local text is available for a selected reference, show the reference and a neutral action such as “Open your Bible to this passage” rather than fabricated text.

### 4. Tutorial replay must never mutate recovery state
Settings → replay tutorial/onboarding currently risks starting another streak/session.

Split:
- FIRST-RUN SETUP: writes initial configuration and creates the initial active streak once.
- TUTORIAL REPLAY: presentation only. It must not change streak history, recovery start time, Bible progress, theme, app-lock choice, or create sessions unless the user explicitly edits those settings elsewhere.

Add tests for this.

### 5. Bible active reading/bookmark behavior
The Book Detail UI says long-press bookmarks a chapter but the action is not wired.

Fix it:
- completed state and active-reading bookmark are separate concepts.
- tap toggles completed/unread.
- long-press (or another clearly exposed interaction matching the approved design) sets that chapter as the active reading location for its testament.
- maximum one OT bookmark and one NT bookmark.
- completing a chapter must NOT automatically force the bookmark to the next chapter.
- chapter order remains non-mandatory.

### 6. Journal behavior
The first implementation claimed capabilities that are missing.

Implement:
- search
- context filters: GENERAL / URGE / RESTART
- full create/edit/delete
- from “I’m Struggling” → “Write a note”, navigate directly to Journal and immediately open the note composer with URGE context preselected.
- restart flow may create a RESTART-context entry only when the user chooses to write one; do not force journaling.

---

## P1 — Functional Alignment

### 7. Dashboard timer formatting
Before 30 days, the primary Dashboard counter must display:
- Days
- Hours
- Seconds

Do NOT show Minutes in the main focal counter before 30 days.

After 30 days:
- add Months using accurate calendar arithmetic.

After 1 year:
- add Years.

Total minutes / total seconds belong in the separate motivation/impact section.

### 8. Accurate time engine
Keep calendar-accurate streak duration:
- leap years
- variable month lengths
- DST-safe behavior
- exact start date + time

Add unit tests for representative boundary cases.

### 9. Motivation/comparison library
Replace the tiny pool with a larger offline structured dataset.

Rules:
- facts must be defensible.
- remove pseudoscientific recovery claims such as fixed neural “clearing” timelines.
- no unsupported “X days rewires your brain” claims.
- scale comparisons should use simple verifiable arithmetic or clearly sourced stable facts.
- avoid repeating the same comparison frequently.
- structure the dataset so more facts can be added without code changes.

### 10. Bible data
Keep canonical Protestant structure:
- 66 books
- 1,189 chapters
- OT / NT / whole Bible progress
- maximum two current reading locations: one OT + one NT
- daily chapter target remains motivational only.

Do NOT create a Bible streak.

---

## P1 — Project Hygiene

### 11. Clean package/namespace
Remove template namespace/package remnants such as `com.example`.

Before permanent signing, make the package structure consistent.

Canonical target for this pre-release project:
`com.steadfast.app`

Use the same namespace/applicationId consistently unless a technical Android reason requires otherwise. If migration impacts persisted test data, that is acceptable because this is pre-release.

### 12. Remove unused AI Studio/cloud scaffolding
This is an offline app.

Remove unused:
- Google Services plugin/config
- secrets plugin/config
- cloud/API placeholders
- `.env.example` if it exists solely for unused online services
- unused dependencies from AI Studio scaffolding

Do not remove anything actually required for local Android functionality.

### 13. Gradle wrapper
Repository must be clone-and-build capable.

Ensure committed:
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

A clean clone should be buildable without relying on AI Studio magic.

### 14. README
Create a real README containing:
- what Steadfast is
- offline/privacy model
- feature overview
- tech stack
- build requirements
- debug build command
- project architecture
- backup format summary
- note that Scripture text licensing must be respected
- current status: pre-release / private testing

Do not advertise unimplemented features.

### 15. Repository privacy
The repository should be PRIVATE during development.
If this environment can change GitHub visibility, set it to private.
If not, report this as the single manual GitHub action required.

---

## P2 — Verification

Before claiming completion, run and report:

1. `./gradlew clean assembleDebug`
2. `./gradlew test`
3. `./gradlew lint`
4. any Room schema / compile checks required by the project

Fix failures instead of hiding them.

Also verify:
- no INTERNET permission
- no analytics/tracking SDK
- one and only one active streak after tutorial replay
- bookmark long-press works
- Journal search/filter works
- “I’m Struggling” note opens Journal composer directly
- verse text always matches verse reference
- no fabricated Scripture fallback
- Dashboard main timer follows the frozen format
- backup export/import still round-trips correctly
- biometric + device-credential fallback still works
- widget still renders active day count
- dark and light themes remain visually aligned with the frozen hybrid references

---

## Constraints

- DO NOT redesign the app.
- DO NOT replace the approved visual system.
- DO NOT add cloud accounts.
- DO NOT add networking.
- DO NOT add notifications.
- DO NOT add a daily check-in.
- DO NOT add a Bible streak.
- DO NOT silently change user data semantics.
- DO NOT claim tests/builds passed unless you actually ran them.

When finished:
1. commit all fixes to the current branch,
2. provide the commit hash,
3. summarize each repaired issue,
4. list any remaining known limitations,
5. provide exact build/test/lint results,
6. identify any manual action I still need to do.
