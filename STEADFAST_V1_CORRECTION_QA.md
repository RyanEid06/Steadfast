# Steadfast V1 QA status

Updated 2026-10-29. The initial correction brief has been superseded by the reconciled implementation, [current acceptance checklist](docs/ACCEPTANCE_TESTS.md), and [reconciliation report](docs/RECONCILIATION_REPORT.md).

## Implemented corrections

- Canonical `com.steadfast.app` package throughout runtime and tests; duplicate/generated source removed.
- Enabled app lock remains locked on unavailable/failed/cancelled authentication; supported API 29 policy, capability checks and retained prompt ownership.
- Schema 2/STDF_V2 backups with legacy compatibility, full validation, read-only preview, explicit confirmation, actual Room transaction and encrypted cross-store recovery protocol.
- Accurate local-storage privacy wording and exact-reference WEB Scripture with neutral fallback.
- JSON comparisons/source notes without unsupported recovery timelines; 4-4-4-4 reduced-motion breathing.
- Direct URGE composer, Dashboard return navigation, non-overlapping restart times, validated Bible pointers/completions and foreground clock updates.
- Newer artwork/onboarding/Journey/widget retained; short/landscape layouts, 48dp Journal controls and labelled switches repaired.
- Shared universal and ARM64 outputs; optimized local test APKs delivered.

## Observed checks

93 JVM/Robolectric tests and 4 API 37 instrumentation tests passed in the integrated gate. The later packaging-only gate reused the 93 passing JVM results from cache and built both debug/release APK outputs. Latest offline lint: 0 errors, 40 warnings. Artifact ABI/signature/alignment checks passed. These are local/emulator results, not physical-device acceptance or CI.

## Remaining acceptance

Use [the acceptance checklist](docs/ACCEPTANCE_TESTS.md) for phone/API 29 authentication, real biometrics, cross-device backup/SAF failures, Keystore invalidation, actual process-kill recovery, TalkBack/OEM coverage and optimized-runtime acceptance. Public store release and production signing remain separate work.
