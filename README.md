# Steadfast

**A stronger you. A closer walk with God.**

A private, offline Android companion for recovery, reflection and Bible reading. Track your progress, preserve your history and find a moment of peace—without accounts, ads or cloud services.

Built with Kotlin and Jetpack Compose for **Android 10+**. Current version: **1.1.0 · version code 2**. [Download the latest release](https://github.com/RyanEid06/Steadfast/releases/latest).

## What you can do

- **Track your recovery.** A live, calendar-aware streak timer, preserved restart history and milestones keep progress visible.
- **See your journey.** A chronological chart shows streak growth and resets, with horizontal scrolling for longer histories. A searchable journal keeps private reflections close.
- **Build a reading habit.** Track all 66 Bible books and 1,189 chapters, keep independent Old and New Testament bookmarks, and choose a reading target.
- **Pause when things feel difficult.** The sanctuary offers 500 complete, verified WEB Scripture passages, box breathing, direct urge journaling and a path back to your progress.
- **Keep encouragement nearby.** A static cross-and-streak home-screen widget gradually brightens as your current streak grows.
- **Make it yours.** Light, dark and system themes, reduced motion, optional system app lock, and remembered Bible and Journey view choices.
- **Keep a portable backup.** Encrypted exports, read-only import previews and confirmed replacement preserve control over your data.

## Updating from V1

Install the signed V1.1 APK **over the existing app**. The package remains `com.steadfast.app`; the November 2 private V1 and V1.1 use the same signing certificate. Room stays at schema 1, and existing database rows, storage names and preference keys are preserved. Do not uninstall to resolve a signing mismatch.

An actual V1 → V1.1 emulator update passed with synthetic recovery history, journal, Bible progress, bookmarks, preferences and enabled App Lock. On November 3, the full host gate passed: 191 debug JVM tests across 40 classes, with zero failures, errors or skips; lint reported 0 errors and 41 warnings; debug, private release and debug instrumentation APKs assembled. All seven native API 37 tests passed. The optimized release candidate was reinstalled, then synthetic upgrade data and owner-0 storage hashes were verified unchanged. Owner-reported physical-device acceptance remains separate. See the [V1.1 verification record](docs/V1_1_VERIFICATION.md) for current results, artifact identity and limits, and the [upgrade harness](docs/V1_1_UPGRADE_REGRESSION.md) for reproducible checks. The GitHub APKs retain the compatible private V1 signing identity; no GitHub Actions CI or Play Store signing result is claimed.

## Private and offline

There is no `INTERNET` permission, account, cloud sync, analytics, advertising or notification service. Data stays in the app sandbox, and Android automatic backup is disabled. The Room database itself is **not encrypted**; optional biometric/device-credential lock protects entry to the app UI.

Portable `.steadfast` backups use AES-256-GCM and PBKDF2-HMAC-SHA256 with 210,000 iterations. Current exports use logical schema 2 and `STDF_V2`; legacy imports remain supported, including their historical passphrase behavior. See [Security and backup](docs/SECURITY_BACKUP.md).

All 500 Scripture passages are bundled, complete public-domain **World English Bible (WEB)** text, checked against pinned publisher sources. The sanctuary's persistent deck exhausts every passage before repeating; completed reading chapters prioritize unused passages only. The fixed Dashboard uses short, complete encouragements from the same verified catalog. [Scripture selection and provenance](data/scripture_source/README.md).

## Build

Use **JDK 21**, Android SDK Platform **36**, and Build Tools **36.0.0 or later**. Configure `ANDROID_HOME` or an ignored `local.properties`; dependencies are pinned in the [version catalog](gradle/libs.versions.toml).

```powershell
# Windows
.\gradlew.bat test lintDebug assembleDebug assembleRelease
```

```bash
# macOS / Linux
./gradlew test lintDebug assembleDebug assembleRelease
```

Each build produces shared-source **universal** and **ARM64-only** APKs:

| Build | Output directory | Signing |
| --- | --- | --- |
| Debug | `app/build/outputs/apk/debug/` | Local debug key |
| Release | `app/build/outputs/apk/release/` | Unsigned unless configured |
| Private V1 update | Release command with `-PprivateUpdate=true` | Existing private/debug key |

Universal includes ARM64, ARM32, x86 and x86_64 libraries. Both APKs share the same features and data format. Release builds enable R8 minification and resource shrinking. For an update, preserve the existing signing key; never generate a replacement. Production signing uses external `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` values with alias `upload`. Keep keys, passwords, personal backups, SDK paths and APKs out of Git.

Instrumentation uses an explicitly selected disposable emulator and `connectedDebugAndroidTest`. Emulator results do not establish physical-device, biometric-hardware, OEM-launcher or native ARM64 acceptance.

## Project guide

- [Acceptance checklist](docs/ACCEPTANCE_TESTS.md) · [V1.1 verification](docs/V1_1_VERIFICATION.md) · [current work checkpoint](CODEX_WORKLOG.md)
- [Architecture](docs/ARCHITECTURE.md) · [data model](docs/DATA_MODEL.md) · [security and backup](docs/SECURITY_BACKUP.md)
- [Product requirements](docs/PRODUCT_SPEC.md) · [UI specification](docs/UI_SPEC.md) · [design freeze](docs/DESIGN_FREEZE.md) · [visual references](design/README.md)
- [Development rules](GEMINI.md) · [milestones](docs/MILESTONES.md) · [synthetic backup examples](samples/README.md)
- [Historical V1 review and dispositions](docs/ISSUE_PLAN_REVIEW_2026_10_29.md) · [earlier reconciliation evidence](docs/RECONCILIATION_REPORT.md)

Application code lives under `app/src/main/java/com/steadfast/app/`, with JVM and native checks in the matching test source trees. Runtime catalogs are in `app/src/main/assets/`; reference data and reproducible Scripture evidence are in `data/`. Completed implementation roadmaps have been retired; current contracts, evidence and remaining acceptance checks are linked above.
