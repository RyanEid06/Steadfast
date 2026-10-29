# Steadfast development rules

## Current project

Steadfast is an implemented native Android recovery and Bible-progress app. Read `CODEX_WORKLOG.md` and the current source before changes. The original files under `prompts/` describe completed implementation phases; do not restart them or replace the app with a scaffold.

## Product and design

- Preserve the Steadfast name, offline behavior, recovery focuses and Bible progress. No accounts, cloud, networking, analytics, ads, notifications, daily check-ins or Bible streak without explicit approval.
- Before UI work, read `docs/DESIGN_FREEZE.md` and `design/README.md`. Style 1 owns structure/identity, Style 3 palette, Style 2 Journal, and Style 3 chapter grid.
- Exactly three bottom destinations: Dashboard, Journey and Bible. Settings stays separate. Dashboard remains non-scrolling, adapting to compact windows.
- Never invent Scripture or associate text with the wrong reference. Use bundled verified WEB quotations or a neutral reference-only prompt; respect translation licensing.
- Never add shame language or unsupported medical/recovery claims. Preserve the safety copy in `docs/PRODUCT_SPEC.md`.

## Implementation and checks

- Use the existing Kotlin/Compose, Room, DataStore, AndroidX and JSON patterns. Keep namespace/applicationId `com.steadfast.app`.
- Keep universal and ARM64 packages on shared source. Preserve privacy/authentication validation and all-or-nothing restore/recovery behavior.
- Do not add placeholder actions, fake data/progress or unnecessary dependencies. Check current pinned versions before changing tools.
- Preserve design/data references, accessibility, reduced motion, efficient foreground clocks and user data. Keep matching reference/runtime JSON catalogs in sync.
- Run checks appropriate to changed behavior. The full local gate is `gradlew test lintDebug assembleDebug assembleRelease` (`gradlew.bat` on Windows). Report actual results and distinguish local, CI, emulator and physical-device evidence.
- Keep `CODEX_WORKLOG.md` compact and current. Do not commit secrets, local paths, APKs, personal backups, signing keys, build/cache output or machine workarounds.
