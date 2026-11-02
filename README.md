# Steadfast

Offline Android app for recovery tracking, personal journaling, and Bible reading progress. Built with Kotlin and Jetpack Compose for Android 10+.

**Status: pre-release; local JVM, optimized backup/native regression, and selected API 37 emulator checks have passed, including fresh-user entry and relaunch. Device/manual acceptance remains open.** Public store distribution and production signing are not configured.

## Features

- Live recovery timer, calendar-aware months/years, history, and validated restart times.
- Journey with Overview, History, searchable Journal, and optional restart reflections.
- Bible completion across 66 books / 1,189 chapters, independent OT/NT bookmarks, and a reading target of 1–5 chapters.
- “I'm Struggling” support with exact-reference Scripture, 4-4-4-4 box breathing, progress, and direct URGE journaling.
- Milestones, conservative JSON-driven elapsed-time comparisons with source notes, and a home-screen widget.
- System/light/dark themes, reduced motion, scalable layouts, and optional system biometric/device-credential lock.
- Encrypted portable backups, read-only import preview, explicit replacement confirmation, validation, and interrupted-restore recovery.

## Privacy and Scripture

No accounts, cloud sync, analytics, ads, notifications, or INTERNET permission. Room and DataStore keep user data in the app sandbox; Android automatic backup is disabled. The Room database itself is **not encrypted**. App lock controls access to the app UI.

Portable `.steadfast` backups use AES-256-GCM with PBKDF2-HMAC-SHA256 (210,000 iterations), logical schema 2 and the `STDF_V2` envelope. Legacy schemas/envelopes remain importable. Export requires matching passphrases of at least 12 characters. See [Security and backup](docs/SECURITY_BACKUP.md).

Displayed bundled quotations use verified public-domain WEB text paired with the exact reference. If text is unavailable, the app shows a neutral Bible-reading prompt. Licensed translations such as NLT require permission before adding their text; the reference catalogs do not grant those rights.

## Build

Use JDK 21 and an existing Android SDK with Platform 36 and Build Tools 36.0.0 or later. Dependencies and plugin versions are pinned in [the version catalog](gradle/libs.versions.toml). Set `ANDROID_HOME` or use an ignored local `local.properties` file for your SDK.

Windows:

```powershell
.\gradlew.bat test lintDebug assembleDebug assembleRelease
```

macOS/Linux:

```bash
./gradlew test lintDebug assembleDebug assembleRelease
```

One codebase produces two APKs in each build:

| Variant | Universal | ARM64 only |
|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-universal-debug.apk` | `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` |
| Release, unsigned by default | `app/build/outputs/apk/release/app-universal-release-unsigned.apk` | `app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk` |

The universal APK includes the bundled ARM64, ARM32, x86 and x86_64 libraries. ARM64 contains only `arm64-v8a`; both share package `com.steadfast.app`, version 1.0/code 1, features and data format. Install the APK compatible with your device.

Release builds use R8 minification and resource shrinking. The current optimized local test APKs are approximately **8.29 MiB universal / 8.19 MiB ARM64**; the unoptimized universal debug build is about 67.69 MiB. Most of that difference is unshrunk bytecode/development tooling, rather than CPU libraries.

Debug builds use the standard local debug signing identity. Release builds are unsigned unless `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` are supplied externally; the configured release alias is `upload`. Configured signing removes `-unsigned` from release output filenames. Private test copies may be signed with a local debug identity; these are not production release signatures. Do not commit keys, passwords, backup files, local SDK paths or APKs.

For instrumentation, use a disposable emulator and an explicit `ANDROID_SERIAL`, then run `connectedDebugAndroidTest`. The unavailable-authentication test requires a device without enrolled authentication. Do not treat emulator tests as physical-device acceptance.

## Verification

Current local bug-fix evidence, 2026-11-02:

- The final ordinary gate passed: 166 JVM/Robolectric tests across 34 suites, 0 failures/errors/skips; lint reported 0 errors and 40 warnings. Debug, unsigned release, and Android test APKs assembled. The optimized release-equivalent assembly also passed.
- A fresh optimized API 37 entry used a new test user and verified catalog counts/sentinels, explicit focus, Activity recreation, one initial session, journal/Bible preservation, and relaunch. A separate force-stop/relaunch check confirmed the process exited, returned to Dashboard without replaying setup, and preserved the recorded Room and DataStore hashes. This was an ordinary app restart, not a process kill during restore.
- Optimized API 37 backup acceptance passed for V2 export/preview/restore, provider and malformed-input errors, rotation, V1 short and empty passphrases with actual replacement, and incomplete-flag history reconciliation. The final native regression suite passed 4/4 tests covering device Keystore backup/preview/SQLite rollback, unavailable-lock retry/recreation, package context, and root navigation. A separate native breathing run completed all four phases/countdowns, preserved Hold progress across Activity recreation, paused while Home was backgrounded, returned to Dashboard, and preserved SQL rows and portable settings.
- Optimized API 37 icon, registered widget-provider, and corrupt-catalog modes passed. Compact 640×360/font-scale 2 light/dark/RTL layouts retained the live Dashboard units and actionable control. On the 320×568/font-scale 2 emulator, actual SAF export created an encrypted backup and importing it reached the correct read-only preview; that preview was cancelled. Final universal and ARM64 installs preserved the owner’s recorded Room and DataStore hashes. ARM64 ran through `libndk_translation` on x86 emulator hardware; this is not native ARM64 hardware coverage. The APK audit found 384 identical non-native entries across ordinary unsigned, universal, and ARM64 outputs; the debug signing identity was unchanged.
- These optimized artifacts are private test builds, not production-signed releases.

The local entry and ordinary process-restart checks passed; OS process-kill recovery boundaries during restore remain open. The breathing harness initially compared raw DataStore bytes, including Android's changing `LAST_BACKGROUND_EPOCH`; the corrected portable-settings comparison passed while retaining lifecycle and SQL guards. Physical/API 29 enrolled authentication, real biometric sensors, cross-device backup/SAF-provider failures, Keystore invalidation, native ARM64, and full spoken TalkBack/OEM acceptance remain open. See [Acceptance tests](docs/ACCEPTANCE_TESTS.md) and [Reconciliation report](docs/RECONCILIATION_REPORT.md). No GitHub CI result is claimed; the repository currently has no Actions workflow.

## Repository guide

- [Architecture](docs/ARCHITECTURE.md) and [data model](docs/DATA_MODEL.md)
- [Product requirements](docs/PRODUCT_SPEC.md), [UI specification](docs/UI_SPEC.md), [frozen design](docs/DESIGN_FREEZE.md) and [design references](design/README.md)
- [Security and backup](docs/SECURITY_BACKUP.md), [synthetic payload examples](samples/README.md), [milestones](docs/MILESTONES.md)
- [Development rules](GEMINI.md), [current work checkpoint](CODEX_WORKLOG.md), [QA status](STEADFAST_V1_CORRECTION_QA.md), and [canonical V1 correction plan](Steadfast_V1_Bug_Fix_Implementation_Plan.md)
- `app/src/main/java/com/steadfast/app/`: application and shared code; matching JVM/instrumentation source trees under `app/src/test` and `app/src/androidTest`
- `data/`: reference catalogs; `app/src/main/assets/`: runtime catalogs. Keep corresponding copies in sync.
- [Original implementation prompts](prompts/README.md): historical reference, not a request to rebuild completed phases.
