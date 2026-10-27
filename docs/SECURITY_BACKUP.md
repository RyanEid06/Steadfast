# Security + Backup Specification

## App lock

Use AndroidX BiometricPrompt / modern equivalent with:

- biometric first when available
- device credential fallback
- no custom in-app PIN
- lock on cold start when enabled
- lock after returning from background beyond a short timeout (target 30 seconds unless UX testing proves annoying)
- lifecycle/race-safe behavior; never show duplicate prompts

Reuse the proven SpendWise/Memora behavior if source code is supplied to the implementation agent; otherwise implement cleanly from AndroidX primitives.

## Local privacy

V1 is offline. Do not request INTERNET permission. Do not add analytics, crash-reporting SDKs, ad SDKs, remote fonts, or telemetry.

Sensitive values should not be written to logs. Release builds must not log journal text, recovery focus, or backup content.

## Backup file

Extension: `.steadfast`

Requirements:

- versioned schema
- portable to another Android device
- contains all user-owned app data: streak sessions, journal, Bible completions, reading pointers/goal, recovery focus, theme preference, app-lock preference (but never biometric secrets/keys), tutorial flags where sensible
- import validates schema/version and data ranges before mutating live data
- import is transactional: all-or-nothing
- create a pre-import rollback snapshot in app-private storage when feasible
- provide a clear summary before final import

### Security

Because backups contain sensitive journal/recovery data, implement a portable encrypted backup using a user-entered export passphrase, following modern Android cryptographic best practices available at implementation time (authenticated encryption such as AES-GCM and a strong password-based KDF). Do not encrypt a portable backup solely with a device-bound Android Keystore key, because that would make cross-device restore impossible.

Never store the export passphrase.

### Logical payload

```json
{
  "schemaVersion": 1,
  "exportedAtEpochMillis": 0,
  "appVersion": "1.0.0",
  "settings": {},
  "streakSessions": [],
  "journalEntries": [],
  "bibleCompletedChapters": []
}
```

The encrypted envelope may contain salt/KDF parameters/nonce/ciphertext/tag as required. Exact cryptographic envelope can be finalized during implementation review.

## Import semantics

Default import mode: **replace user data from backup** after explicit confirmation. Do not attempt a silent merge of streak histories/journals in V1; merge conflict logic is unnecessary complexity.

Theme may be restored from backup because this app is single-user/private. Biometric lock preference may be restored, but the destination device must re-authenticate/re-enroll through Android APIs as needed.
