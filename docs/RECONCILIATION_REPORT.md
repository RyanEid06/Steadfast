# Steadfast reconciliation and packaging report

Updated 2026-10-29. Reconciled implementation published to GitHub `main` as [`e7cb984`](https://github.com/RyanEid06/Steadfast/commit/e7cb98451b6332f6eba8096cf23d9ad15e4b2cad). This report consolidates the former subsystem/review checkpoints; the current implementation and contracts are described in [Architecture](ARCHITECTURE.md), [Data model](DATA_MODEL.md), [Security and backup](SECURITY_BACKUP.md), and [Acceptance tests](ACCEPTANCE_TESTS.md).

## Base and result

CurrentSteadfast supplied Git history, `main`, origin `RyanEid06/Steadfast` and base commit `ca88b9bb672b1615cd3876fe6e44580ce4541e72`. Trusted OldSteadfast behavior was semantically reconciled into that base. The final app uses `com.steadfast.app` in Gradle, source and tests, with no old runtime/test namespace or duplicate instrumentation tree.

Before donor removal, SHA-256 inventories confirmed all 146 OldSteadfast and 150 CurrentSteadfast non-Git files were unchanged, as were all 30 design references from each donor and the four Current JPGs. After verification, the owner explicitly requested removal of the redundant donor folders; only those copies were moved to the Recycle Bin. Git history and the final app were preserved.

## Reconciled behavior

- Retained clean hero/splash artwork, optional OT/NT onboarding, Journey tabs/charts/journal search, real asynchronous widget state, import preview/confirmation and safe release shrinking.
- Restored exact-reference bundled WEB Scripture and neutral fallback, JSON scale comparisons with source notes, direct URGE journaling, and reduced-motion 4-4-4-4 breathing. Room privacy wording no longer claims database encryption.
- App lock now validates capability before enable and remains locked on unavailable/cancelled/failed authentication. API 29 uses weak biometric or device credential; newer APIs use strong biometric or device credential. Retained prompt/session ownership suppresses duplicates and stale callbacks.
- Backups export schema 2 inside STDF_V2, import legacy formats, validate before mutation, replace Room in one transaction and reconcile DataStore through an encrypted device-key journal plus a Room commit token. Preview does not mutate state; error/rollback paths remain gated.
- Restart rejects future/inverted/overlapping chronology and retains history; Bible repository checks book/testament/chapter bounds. Foreground clocks update UI memory without per-second database reads.
- Native navigation regression repaired Dashboard saved-stack replay after the URGE composer. Compact onboarding and landscape Dashboard layouts, 48dp Journal targets/wrapping timestamps, and switch labels were also corrected.

Independent static reviews covered backup/restore, lock gating and integration. A later native test exposed the navigation issue, which was fixed and observed passing. Review results do not replace runtime acceptance.

## Observed verification

| Gate | Observed outcome |
|---|---|
| Integrated `clean test lintDebug assembleDebug assembleRelease connectedDebugAndroidTest` | Exit 0; 93 JVM tests and 4 API 37 instrumentation tests, no failures/errors/skips |
| Final publication `test lintDebug assembleDebug assembleRelease`, offline | Exit 0 in 24s; 93 passing JVM results reused from cache; debug/release outputs built, lint 0 errors/40 warnings |
| Shared universal/ARM64 `testDebugUnitTest lintDebug assembleDebug assembleRelease`, offline | Exit 0 in 2m49s; 93 passing JVM results reused from cache; debug and release outputs built |
| Lint | Latest offline: 0 errors/fatal, 40 warnings; earlier online: 0 errors/fatal, 56 warnings including dependency freshness |
| Packaging | Universal includes ARM64/ARM32/x86/x86_64; ARM64 includes only arm64-v8a; 467 non-native entries byte-identical |
| Private test signatures | Both optimized APKs verify with the same local debug certificate; 16KiB-aware ZIP alignment passes |

The native backup test uses real Android Keystore and on-disk Room/DataStore for read-only preview, roundtrip and a later-table SQLite rollback. Other native tests cover package startup, unavailable-auth retry/recreation and navigation. Manual API 37 emulator checks covered credential cancel/wrong/valid PIN, cold start, single prompt rotation, themes, 130% font, landscape, URGE and breathing.

No GitHub CI result is claimed. Raw logs, XML/HTML reports, donor hashes, native screenshots, mapping and private APKs are retained in the operator's local evidence archive rather than source control.

## APK outputs

One source branch generates universal and ARM64 APKs; release uses R8 and resource shrinking. Default release outputs require external signing before installation. The delivered private test copies use a local debug certificate, not a production release identity.

| Private test APK | Bytes | SHA-256 |
|---|---:|---|
| Steadfast-universal.apk | 8,535,276 | `c3b43748a059398b33b1d098bfd3c8ed8dce78c46713e12ad10e3c61adc85c0a` |
| Steadfast-arm64.apk | 8,440,548 | `71807e7ecced0e6f1740d9d5394fbdb1dbcb6da97a33d715a7577f9a7a116953` |

The original 70,976,958-byte debug APK contained 66,041,040 bytes of unoptimized DEX and only 60,292 bytes of native libraries. Shrinking explains the large size reduction; ABI filtering plus packaging alignment accounts for the much smaller universal/ARM64 difference.

## Cleanup

Removed generated AI Studio ZIP/paste/environment/metadata debris, the old namespace/duplicate tests, obsolete account handoff, and redundant subsystem reports. Current README, manifest, QA, development rules, examples and technical docs replace scaffold assumptions. Original design references and historical implementation prompts remain as reference material. APKs, private backups, keys, logs, local paths and build/cache output are excluded from Git.

## Limits

Physical/API 29 enrolled authentication, real biometric sensors, cross-device backup/SAF failures, Keystore invalidation, true OS process-kill restore boundaries, full TalkBack/OEM coverage and optimized phone-runtime acceptance remain unverified. One rapid rotate-back while the system credential prompt dismissed left a secure transient overlay requiring Back; normal/single-rotation checks passed afterward.

A host-specific Java loopback issue required a temporary process-local Java21 IPC workaround for local validation. Installed JDK/app/Android runtime were unchanged; workaround source/classes remain outside Git. KSP emitted an internal AWT disposal exception during an earlier build, which continued to successful exit; compiler/lint warnings remain. These local results do not establish public-store readiness or production signing.
