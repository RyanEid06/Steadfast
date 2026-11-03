# V1 → V1.1 emulator regression

This is an actual signed optimized APK install-over check. `UpgradeRegression` runs through the existing `OptimizedSmokeRunner`; it uses Android SQLite and a test-only Preferences protobuf codec, without relying on production class names or new V1.1 APIs. No product manifest, production code or Room schema changes belong to this harness.

Build the optimized instrumentation APK with the existing `-PoptimizedSmoke=true` configuration and the same private debug certificate as the delivered V1 artifact. Preserve the V1 universal baseline with SHA256 `0876b06211c66d9617a51c855b6bb28eac9ead61e4d05a2c3ad37ed55373a6f3`. The harness requires package `com.steadfast.app`, certificate SHA256 `f362708af345c607f91700a453893cf0c50731a60b38fcc13be85d0e579e3c58`, V1 code1/name1.0 and V1.1 code2/name1.1.0.

Use a newly created, unlocked secondary **emulator** user with no Steadfast records, and explicitly verify its serial. Do not use owner0, a phone, `adb root`, uninstall or `pm clear`. Android package code is shared between users: preserve/check owner0 storage independently around APK replacements. The script does not create, switch or remove users and will refuse an emulator whose foreground user differs from `UserId`.

From the repository root, supply absolute paths appropriate to the verified environment:

```powershell
& ./tools/qa/Invoke-V11Upgrade.ps1 -Adb $adb -Serial emulator-5584 -UserId 12 `
  -V1Apk .build-outputs/v1.1/Steadfast-V1-baseline.apk `
  -V11Apk $signedV11UniversalApk -InstrumentationApk $optimizedTestApk
```

The script uses sequential `adb -s <serial> install --user <user> -r <apk>` for V1 and V1.1 and never uninstalls either package. It launches V1 once to let Room create its genuine schema, then force-stops the target user before every instrumentation mode. The same modes can be run individually:

```powershell
& $adb -s emulator-5584 shell am force-stop --user 12 com.steadfast.app
& $adb -s emulator-5584 shell am instrument --user 12 -w -r `
  -e mode upgradeSeed -e fixtureUser 12 `
  com.steadfast.app.test/com.steadfast.app.smoke.OptimizedSmokeRunner
```

Run these modes in order:

1. `upgradeSeed` on V1 only: refuses existing application rows or a prior fixture; seeds three historical/current sessions, two restarts, multiple focus IDs/timezones, four Unicode/multiline/legacy journal notes with contexts/session links, eight OT/NT Bible completions, current reading, goal4, dark theme, onboarding, AppLock **enabled**, all shown milestone IDs and completed-restore token state. It retains any other already-present typed preferences.
2. `upgradeCheckV1` on V1: proves the original app can consume the seeded rows/preferences and displays its locked gate. No protected Dashboard, Bible or journal UI may be exposed.
3. Force-stop this user and `install -r` the signed V1.1 APK.
4. `upgradeVerify`: before and after launching V1.1, asserts stable package/UID/signing certificate, Room userVersion1, all SQLite schema SQL, column names and every typed cell of every table (including Room identity, sequences and backup token), all original typed preferences, and pre-existing backup recovery file presence/hashes. It proves the preserved AppLock gate/retry action is still visible with protected content hidden. Only the legitimate monotonic `last_background_epoch` bookkeeping may differ; the type and upper time bound remain checked.
5. `upgradeUnlockFixture`: after a force-stop and successful V1.1 locked proof, changes only the **isolated synthetic fixture's** AppLock flag to false for UI checks. No authentication bypass is added to the app or used on owner data.
6. `upgradeFeatures`: checks exactly500 unique offline passages spanning OT/NT, persists the NT Bible filter and Journey Journal/Urge choices through the real UI, then draws two distinct passages in Struggling. Each displayed reference/WEB text must exactly match its persisted deck ID and bundled catalog text; used IDs must grow without loss/repetition. All original rows/settings are rechecked (allowing only the explicit synthetic AppLock change).
7. `upgradeFeaturesRelaunch` after a force-stop: verifies the whole persisted preference/deck snapshot before launch, then restored NT UI/Journey choices and two further unused, exact-text draws. This is a process-stop/relaunch check, not an OS-kill during restore.

The immutable baseline JSON and separate per-phase proofs stay in the target application's private `no_backup/v11-upgrade-*.json`. They include full synthetic rows/typed preferences and package/UID/version/certificate. Success output includes the unchanged baseline file SHA256. Instrumentation success requires `OK <mode>:` plus `INSTRUMENTATION_CODE: -1`; `am instrument`'s host exit code alone is insufficient. Mode output/logs are saved under ignored `.build-outputs/v1.1/upgrade/` by default. Preserve logs and record observed results in the root worklog; these instructions alone are not evidence of an executed pass.

`PreferenceWire.main` is an assert-based host/device self-check for all eight preference types, negative integers, Unicode, set canonicalization and malformed protobuf rejection. It needs only `javac`/`java`; the Android harness can also be compiled against the installed Android SDK jar before the real Gradle/instrumentation gate.

Limits: this fixture checks committed backup token state and preserves any existing recovery-file state; it does not fabricate a pending encrypted recovery journal or establish interrupted-restore acceptance. Two draws per process establish native deck persistence; exhaustive500-cycle/concurrency behavior belongs to the repository's focused JVM checks. It does not establish physical-device/real-biometric, provider, cross-device, OEM or production-signature acceptance. APK package replacement affects shared code for all emulator users, so keep independent owner0 hashes and remove only task-created users during parent-owned cleanup.
