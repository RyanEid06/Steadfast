# Steadfast V1 QA status

Updated 2026-11-02. The active implementation follows the root [V1 bug-fix plan](Steadfast_V1_Bug_Fix_Implementation_Plan.md), with exact execution state in [CODEX_WORKLOG.md](CODEX_WORKLOG.md). The earlier [reconciliation report](docs/RECONCILIATION_REPORT.md) remains historical evidence.

## Implemented corrections

- Canonical `com.steadfast.app` package throughout runtime and tests; duplicate/generated source removed.
- Enabled app lock remains locked on unavailable/failed/cancelled authentication; supported API 29 policy, capability checks and retained prompt ownership.
- Schema 2/STDF_V2 backups with legacy compatibility, full validation, read-only preview, explicit confirmation, actual Room transaction and encrypted cross-store recovery protocol.
- Accurate local-storage privacy wording and exact-reference WEB Scripture with neutral fallback.
- JSON comparisons/source notes without unsupported recovery timelines; 4-4-4-4 reduced-motion breathing.
- Direct URGE composer, Dashboard return navigation, non-overlapping restart times, validated Bible pointers/completions and foreground clock updates.
- Newer artwork/onboarding/Journey/widget retained; short/landscape layouts, 48dp Journal controls and labelled switches repaired.
- Shared universal and ARM64 release variants; private optimized test APK verification is tracked separately from production signing.

## Current bug-fix implementation

The working diff adds generated seed adapters and retryable coherent startup, idempotent setup/reconciliation, neutral widgets, deferred modal milestones, stored-zone edits and accurate short history, explicit Bible loading/bookmark management, retained private in-memory workflows, accessible selections/contrast, reproduced compact-layout repairs, scoped threshold clocks and a themed monochrome icon. The canonical plan owns the per-issue dispositions; deferred product enhancements remain deferred.

These changes preserve the documented backup compatibility contract (including empty legacy import passphrases), restore/authentication gates, existing user rows, the three bottom destinations, the non-scrolling Dashboard and omitted timer minutes. Production signing, store release, physical-device operation and data wipe are outside this verification.

## Observed checks

The final ordinary gate passed 166/166 JVM/Robolectric tests across 34 suites, with 0 failures, errors, or skips. Lint reported 0 errors and 40 warnings. Debug, unsigned release, and Android test APKs assembled; optimized release-equivalent assembly passed. These local gates do not establish every device-specific acceptance condition.

Final optimized API 37 emulator backup acceptance passed V2 export/preview/restore, provider and malformed-input errors, rotation, V1 short and empty passphrase imports with actual replacement, and incomplete-flag history reconciliation. Final native regressions passed 4/4 tests, including Keystore-backed backup/preview/SQLite rollback, unavailable-lock retry/recreation, package context, and root navigation. A separate native breathing run completed all four phases/countdowns, confirmed pause after Home backgrounding, retained Hold progress after Activity recreation, returned to Dashboard, and preserved SQL rows and portable settings. Icon, widget-provider, and deliberate corrupt-catalog modes passed. Synthetic owner-data upgrades through universal and ARM64 installs preserved recorded Room and DataStore hashes. ARM64 runtime used `libndk_translation` on x86 emulator hardware; it is not native ARM64 acceptance. The APK audit found 384 identical non-native entries across ordinary unsigned, universal, and ARM64 targets, and confirmed the debug signing identity was unchanged. No CI was run.

A fresh optimized API 37 entry on a new test user verified catalog counts/sentinels, explicit focus, Activity recreation, one initial session, journal/Bible preservation, and relaunch. A separate force-stop/relaunch confirmed the app process exited, resumed to Dashboard without replaying setup, and preserved recorded Room/DataStore hashes. This ordinary restart check does not exercise process death during restore.

## Remaining acceptance

The scoped local checks are complete. Phone/API 29 authentication, biometric hardware, cross-device SAF/provider failures, Keystore invalidation, true OS process-kill recovery, native ARM64, and spoken TalkBack/OEM acceptance remain device checks. The SAF emulator run reached read-only preview and cancelled; it did not perform a real-provider restore. See the [acceptance checklist](docs/ACCEPTANCE_TESTS.md) and [canonical 13-task plan](Steadfast_V1_Bug_Fix_Implementation_Plan.md) for the remaining scope. Public store release and production signing remain separate work.
