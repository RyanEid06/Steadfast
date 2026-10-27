# Gemini Master Implementation Prompt

You are implementing **Steadfast**, a frozen V1 Android product. This repository is the source of truth.

Before writing code, read:

- `GEMINI.md`
- `docs/PRODUCT_SPEC.md`
- `docs/UI_SPEC.md`
- `docs/ARCHITECTURE.md`
- `docs/DATA_MODEL.md`
- `docs/SECURITY_BACKUP.md`
- `docs/MILESTONES.md`
- `docs/ACCEPTANCE_TESTS.md`
- `docs/DECISIONS.md`
- `design/README.md`
- `data/*.json`

Then read `docs/DESIGN_FREEZE.md`, `design/README.md`, and inspect `design/mockups/canonical/` before the original full boards. The hybrid priority is mandatory; never average all three concepts.

## Rules

- Do not redesign or add scope without permission.
- Build native Kotlin + Compose.
- V1 must be fully offline and require no account/cloud.
- No network/analytics/ad dependencies.
- Use current stable Android tooling.
- Work phase-by-phase using the prompt files in numeric order.
- After each phase: run tests, lint, and `assembleDebug`; report exact failures and fix them before proceeding.
- Preserve data across schema changes with proper Room migrations once a persisted build exists.
- Do not bulk-copy NLT text or invent Scripture. Use verse references only until licensed/user-supplied text is added.
- Treat the mockups as visual references; written specs override any mockup inconsistency.

Start with `prompts/01_FOUNDATION.md`. Do not jump ahead.
