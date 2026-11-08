# Steadfast

Steadfast is a private, offline Android companion for recovery, reflection, and Bible reading. **Version 1.1.0** runs on **Android 10 and newer** and is built with Kotlin, Jetpack Compose, Room, and DataStore.

**[Download universal APK](https://github.com/RyanEid06/Steadfast/releases/download/v1.1.0/Steadfast-v1.1.0-universal.apk)** · [Download ARM64 APK](https://github.com/RyanEid06/Steadfast/releases/download/v1.1.0/Steadfast-v1.1.0-arm64-v8a.apk) · [Latest release and checksums](https://github.com/RyanEid06/Steadfast/releases/latest)

Open the APK on your Android device and follow the install prompt. The universal APK works across the supported device architectures. Use the smaller ARM64 APK on 64-bit ARM devices. If you already have the signed V1 app, install 1.1.0 over it to keep your history and settings.

## Features

- Track recovery with a streak timer, milestones, a chronological Journey chart, and a home-screen widget.
- Keep a searchable journal and use guided breathing and Scripture for moments when you need support.
- Track reading across 66 Bible books and 1,189 chapters, with separate Old and New Testament bookmarks.
- Protect app access with optional Android biometric or device-credential lock, and export encrypted `.steadfast` backups.

Steadfast has no accounts, ads, analytics, or network access. Android automatic backup is disabled. The local Room database is stored in the app sandbox without database encryption. Portable backups use AES-256-GCM with a passphrase. See [backup and restore behavior](docs/SECURITY_BACKUP.md) for compatibility details.

The app includes 500 complete passages from the public-domain **World English Bible Classic**. The catalog credits eBible.org and records the exact edition and publisher source. [Scripture attribution and provenance](data/scripture_source/README.md)

## Build from source

Install JDK 21, Android SDK Platform 36, and Build Tools 36.0.0 or newer. Set `ANDROID_HOME` or configure the Android SDK in `local.properties`. On Windows, run `.\gradlew.bat :app:assembleDebug`. On macOS or Linux, run `./gradlew :app:assembleDebug`. APKs are written to `app/build/outputs/apk/debug/`.

Use the signed APKs from the GitHub release for a compatible update to an existing installation.
