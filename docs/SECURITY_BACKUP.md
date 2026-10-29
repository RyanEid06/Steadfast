# Security and backup

## App lock

The app uses AndroidX `BiometricPrompt` with the system device credential. On API 29 it requests `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`, because `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` is unsupported there. On API 30 and later it requests `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`. The same policy is used for capability checks and the prompt.

Enabling the lock is rejected with Android enrollment guidance when authentication is unavailable. When enabled, personal content remains locked on cold start until authentication succeeds. Cancellation, failed/terminal authentication, capability errors, or prompt-launch errors do not unlock content; the user can retry. The retained activity-scoped session allows configuration recreation to keep the current prompt/session, but a fresh process is unauthenticated. Returning from background requires authentication after 30 seconds or if the clock moved backward. No custom PIN or biometric secret is stored by the app.

## Local privacy

The manifest disables Android auto backup and has no `INTERNET` permission. Journal entries are stored in the app's Room database; this documentation does not claim that the database itself is encrypted. Sensitive journal, recovery, and passphrase contents must not be logged.

## Portable backup format

The `.steadfast` file is an authenticated encrypted envelope. Its seven-byte header selects the KDF parameters; the header is followed by a random 16-byte salt, a random 12-byte GCM nonce, and AES-GCM ciphertext with a 128-bit tag. The encryption key is 256 bits and is derived from the user passphrase with PBKDF2-HMAC-SHA256.

| Envelope | Header | PBKDF2 iterations | Use |
|---|---|---:|---|
| V2 | `STDF_V2` | 210,000 | Current exports |
| V1 | `STDF_V1` | 65,536 | Legacy import compatibility |

Envelope version and JSON payload `schemaVersion` are separate. Exports use schema 2; imports support logical schemas 1 and 2 inside either recognized envelope. New exports require a matching passphrase confirmation of at least 12 characters. Legacy imports keep their historical passphrase behavior, including short or empty passphrases. The portable format is encrypted with the user's passphrase, never solely with a device-bound key. The passphrase is not embedded or saved by the app.

### Logical payload

Current schema 2 contains:

```json
{
  "schemaVersion": 2,
  "exportedAtEpochMillis": 1793116800000,
  "appVersion": "1.0",
  "settings": {
    "themeMode": "SYSTEM",
    "recoveryFocusId": "pornography",
    "readingGoalMinimum": 2,
    "oldTestamentCurrent": null,
    "newTestamentCurrent": null,
    "appLockEnabled": false,
    "shownMilestones": "",
    "onboardingComplete": true
  },
  "streakSessions": [],
  "journalEntries": [],
  "bibleCompletedChapters": []
}
```

Settings fields are optional for compatibility. If absent in a replacement import, defaults are SYSTEM theme, `pornography` focus, goal 2, no reading bookmarks, app lock off, and no shown milestones; missing `onboardingComplete` defaults to true. Schema 2 includes the shown-milestone IDs and onboarding state. There is no notification preference. The device-local background timestamp is never exported.

Before data changes, preview decrypts and validates the full payload, then the user confirms replacement. Validation checks the schema and required top-level fields, payload size (maximum 32 MiB), timestamps, unique IDs, recovery-focus/theme values, goal range 1..5, OT/NT bookmark bounds, milestone IDs, session chronology/non-overlap, journal references/context, and completed chapter bounds. A wrong passphrase, corrupt file, or unsupported schema cannot mutate live data.

## Replacement and interrupted-restore recovery

Restore replaces user data; it does not merge histories. Before mutation, an `AtomicFile` journal in app-private `noBackupFilesDir` stores before/after preference snapshots and an opaque random token. The journal is encrypted with an Android Keystore AES-GCM key. It contains no Room rows or journal body. The token is recorded in the `steadfast_restore_commit` metadata table within the same Room transaction that replaces streak, journal, and Bible-completion rows. DataStore applies all user settings in one edit before the Room transaction commits.

On startup, recovery reads the Room token: a matching token reapplies the after-preferences; otherwise it restores the before-preferences while Room retains its prior rows. It removes the encrypted journal only after reconciliation succeeds. Recovery failure retains the journal and blocks protected content until retry succeeds. Keystore loss or unreadable recovery data fails closed; there is no plaintext rollback fallback. Widget reads and publication are serialized with restore operations and use neutral content while restore is pending.

See [the reconciliation report](RECONCILIATION_REPORT.md) for verification scope and remaining platform/device checks.
