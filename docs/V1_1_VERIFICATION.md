# Steadfast V1.1 release — 2026-11-03 verification update

## Upgrade identity and storage

| Item | Delivered November 2 V1 | V1.1 |
| --- | --- | --- |
| applicationId / namespace | `com.steadfast.app` | unchanged |
| versionCode | 1 | 2 |
| versionName | 1.0 | 1.1.0 |
| Room database | `steadfast.db`, schema 1 | unchanged; no migration needed |
| Preferences store | `steadfast_settings` | same store and existing keys |
| APK signing certificate SHA-256 | `f362708af345c607f91700a453893cf0c50731a60b38fcc13be85d0e579e3c58` | exact match |

The owner confirmed their installed V1 is the November 2 private APK. Its preserved universal APK SHA-256 is `0876b06211c66d9617a51c855b6bb28eac9ead61e4d05a2c3ad37ed55373a6f3`. V1.1 uses the existing local private/debug signing identity with `-PprivateUpdate=true`; no key was generated or substituted. Production store signing remains separate.

No entity, schema, database filename, destructive fallback, splash theme, authentication logic or main manifest changed. New view preferences have backward-compatible defaults. Portable schema-2 backup fields are optional, so existing backups remain readable. New local verse-deck keys initialize without replacing existing settings.

## Actual install-over result: PASS

The actual preserved V1 APK ran on API 37 emulator `emulator-5584`, isolated synthetic user 12. The fixture contained three sessions with two restarts and distinct focus IDs/timezones, four Unicode/multiline/contextual/legacy journal entries, eight completed Bible chapters, both reading bookmarks, goal 4, dark theme, onboarding and milestone preferences, enabled App Lock, and committed backup-token state. The harness captured all original typed database cells, schema SQL and preferences before installing V1.1 with `adb install --user 12 -r`.

Android accepted the replacement as an update: the package, signing certificate and UID `1210234` remained identical; code 1 became code 2. The original database rows, start instants/zones, history/restarts, addiction/focus selections, journal, Bible progress/completions, reading/bookmark state, goals, settings/theme, App Lock and backup state all matched after launch. Only the device-local last-background timestamp was allowed to advance monotonically as normal lifecycle bookkeeping. The locked screen/retry action remained visible with protected content hidden. No uninstall, package clear, destructive migration, root access or physical-device modification was used.

Only after proving preserved App Lock did the test disable that flag in the isolated synthetic fixture to exercise new UI. New Testament selection, Journey Journal/URGE choices and the deck persisted across force-stop/relaunch. Four distinct native draws displayed the exact complete text/reference belonging to their persisted catalog IDs. Later native regressions and optimized-APK reinstallation were followed by another successful original-data audit. A separate owner's emulator-user-0 database/WAL/SHM/preferences retained all four original byte hashes through all updates.

The immutable fixture SHA-256 remained `5b4b8f84ad15935a89b2f4b6ae1b9897f166ba4fc57c1bb61418ebcf0668a2d4`. Synthetic JSON baseline/proofs and mode logs are retained locally under ignored `.build-outputs/v1.1/upgrade/`. See [reproducible harness instructions](V1_1_UPGRADE_REGRESSION.md).

## Feature and regression evidence

- Exactly **500** locally bundled WEB passages: **1,263** complete source verses, **249 OT / 251 NT**, all **66 books**, 46 passage-specific themes. Offline importer and independent JVM source checks verify canonical IDs, bounds, unique/non-overlapping references, exact publisher text, manifest and checksums. [Source/provenance](../data/scripture_source/README.md).
- Persistent deck tests cover exhaustive cycles, boundary repeats, completed-chapter priority, real DataStore reopen and concurrent 500 draws. Priority cannot reset the wider deck.
- Journey geometry tests cover chronology, growth, resets, same-day records, gaps, viewport fitting and bounded long history. Compose tests include 30 years at high density/font scale 2. Native screenshots were inspected before/after horizontal dragging with all three original sessions and both reset drops visible; statistics stayed intact.
- Sanctuary retains the dark design and full WEB text. Layout tests use the longest actual passage (810 characters) at 400×780/font 1 and 320×480/font 2, checking every rendered character, final-line scrolling and all actions. Real-I/O fixtures wait until the initial loading composition is committed before releasing their Room query, avoiding Robolectric's unconfined effect/initial-subcomposition race without changing the app or bypassing storage.
- Widget tests verify monotonic brightness, dim/reset/unavailable states, capped art size, text contrast, privacy/restore gates, scaled layout and tap behavior. Rendered previews were inspected. The registered optimized provider received real RemoteViews on API 37; the temporary test-host bind grant was revoked. Provider identity is unchanged.
- Final native AndroidX suite: **7/7 PASS** (`final-native7.log`, instrumentation code 1, 63.687 seconds). It includes Android Keystore backup/preview/SQLite rollback, unavailable-authentication retry/recreation, package context, root navigation, and all three journal-dialog cases (URGE save/default context, context change to RESTART, legacy null-context edit).
- Robolectric fixtures provide a host window large enough for their explicitly bounded compact surfaces and use the repository's existing lazy-prefetch shadow where needed. Compact surface sizes, full-text/geometry/action assertions and real persistence checks remain in force. Three existing journal-dialog interaction checks retain their assertions in Android instrumentation, where real dialog-window lifecycle runs; Robolectric stalled on pending measure/layout for those windows. Temporary dispatcher/reflection/rendering experiments were removed.
- November 3 host gate: **BUILD SUCCESSFUL in 8m6s** (`final-full-gate.log`). Debug JVM: **191 tests across 40 classes, 0 failures, 0 errors, 0 skipped**; release JVM tests are not configured. Lint: **0 errors / 41 warnings**. Debug, private release and debug instrumentation APKs assembled. After the seven-case native run, the identical optimized release candidate was reinstalled; `upgrade/final-release-data-audit.log` passed with the original synthetic baseline hash and all rows/settings unchanged. `owner0-release-final.log` confirms all four owner-0 storage hashes match their original values. A final Git-staging check normalized the development curation TSV to LF and updated its provenance hash; the offline importer and all **6 catalog tests** then passed. Runtime assets and APKs were unchanged. No GitHub CI result is claimed.
- APK checks observed: signature verification for V1/universal/ARM64 with the same certificate; universal ZIP/native 16 KiB alignment passed; package/code/name correct; 500 catalog entries present; no `INTERNET` permission. Universal APK SHA-256: `31122b328703b6d173d89072b55214169aafc11d89833cc97fd8016b277aee0e`; ARM64 APK SHA-256: `f09a102a1dfcfc2041f33d6a1d5ed3c64af0ad0e9d26b39e3ce4c9ba080f7e6b`.

Local ignored evidence includes `final-full-gate.log`, `final-build-lint.log`, `final-native7.log`, `final-alignment.log`, `final-apk-badging.log`, `owner0-before.log`, `owner0-final.log`, `owner0-release-final.log`, and the upgrade/widget logs. The isolated synthetic user was removed after switching to owner 0 and waiting for it to stop; emulator5584 is closed. Initial host-pressure/system-UI and stale accessibility-node failures were followed by observed successful reruns. Temporary tracing and unsuccessful fixture experiments were removed.

## Artifact and limits

Universal APK SHA-256: `31122b328703b6d173d89072b55214169aafc11d89833cc97fd8016b277aee0e` (8,793,460 bytes). Install it **over the existing V1 app; do not uninstall**.

The owner reported their physical test passed on 2026-11-02 and authorized public publication. This is owner acceptance, separate from the agent's local/API-37 emulator observations. Specific enrolled API-29 credentials, biometric hardware, spoken TalkBack and real-provider/cross-device backup failure cases have no separate agent-observed acceptance record. Existing owner data was never copied into the synthetic fixture. The release keeps the November 2 signing identity and is intended for GitHub distribution; Play Store distribution is outside this update.

## Public release

[Steadfast v1.1.0](https://github.com/RyanEid06/Steadfast/releases/tag/v1.1.0) was published on November 3 and independently returned by GitHub's `/releases/latest` endpoint, with draft and prerelease both false. Annotated tag `v1.1.0` points to source commit `6b3f5a853161c449991bfa27f31623e58d49bc95`. The server-reported SHA-256 digests for universal and ARM64 APKs exactly match the verified bytes above; `SHA256SUMS.txt` also matched. Release ID: `432352498`.

Publication metadata is retained locally in ignored `.build-outputs/v1.1/release/release-verification.json`. GitHub Actions reports zero runs; all automated results here are local or emulator evidence. A subsequent documentation-only checkpoint records publication without changing the tagged runtime source or released APKs.
