# Steadfast (Android)

> **Current Status**: Pre-release / Private Testing. Not for public store release yet.

Steadfast is a private, offline Christian recovery and daily Scripture progress tracker built around personal freedom, grace, and sustained victory. It contains no accounts, no cloud sync, no tracking, and no shame mechanics.

---

## Architectural Philosophy & Privacy Model

- **100% Offline & Local**: No `INTERNET` permission in `AndroidManifest.xml`. Zero network requests, analytics, crash reporters, or external API endpoints.
- **Zero Cloud Accounts**: No logins, email registrations, or authentication servers.
- **No Mandatory Daily Check-Ins**: Designed around freedom from anxiety. The timer counts forward continuously without requiring daily check-in button taps.
- **Local Persistence**: Recovery sessions, journal entries, and Bible chapter completions are persisted locally on-device in Room (SQLite) and DataStore.
- **Auto Backup Excluded**: Android cloud auto-backup is disabled (`android:allowBackup="false"`), preventing Google Drive / system cloud backup of sensitive recovery history.
- **Encrypted Portable Backup**: Manual backup exports (`.steadfast`) are encrypted using PBKDF2WithHmacSHA256 (65,536 iterations, 256-bit key) and AES-256-GCM authenticated encryption.
- **Local Device Lock**: Biometric Prompt with fallback to device credentials (PIN/Pattern/Password) protects app access when enabled.

---

## Core Feature Overview

1. **Dashboard & Precision Streak Counter**
   - Live second-by-second circular counter updating gracefully in the foreground with zero background battery impact.
   - **Display Tiers**:
     - *Under 30 days*: Days : Hours : Seconds (Minutes omitted from focal counter for clean visual hierarchy).
     - *30 days to 1 year*: Months : Days : Hours : Seconds (accurate calendar arithmetic).
     - *Over 1 year*: Years : Months : Days : Hours : Seconds.
   - Cumulative minutes and seconds won back remain available in the Time & Motivation section.
   - Daily verified Scripture of Peace paired strictly with its exact matching biblical reference.
   - Prominent, instant access to the "I'm Struggling" emergency sanctuary.

2. **Emergency Grace Sanctuary ("I'm Struggling")**
   - Rapid-relief modal offering calm grounding without shame.
   - Prioritizes Scripture passages from chapters the user has already marked completed, using verified anti-repeat cycling.
   - Calming 4-4-4-4 Box Breathing exercise with real-time visual pacing and haptic guidance.
   - "Write Note" shortcut that immediately opens the Journal composer with the `URGE` context tag preselected.

3. **Canonical Protestant Bible Progress**
   - All 66 canonical books (39 Old Testament, 27 New Testament) across 1,189 chapters.
   - Interactive chapter grid:
     - **Tap**: Toggles completed / unread state.
     - **Long-Press**: Sets active reading location bookmark for that testament.
   - Supports up to one Old Testament bookmark and one New Testament bookmark simultaneously.
   - Non-mandatory chapter order; completing a chapter never forces the bookmark forward.
   - No "Bible streak" or guilt mechanics. Daily goals remain motivational only.

4. **Journey, History & Sanctuary Journal**
   - Lifetime analytics: current streak, longest streak, archived streaks, and milestone timeline.
   - "Start Again" grace flow that safely archives completed streaks and provides an optional reflection note (`RESTART` tag) without forcing journaling.
   - Sanctuary Journal with real-time search and context filter chips (`ALL`, `GENERAL`, `URGE`, `RESTART`).
   - Full create, edit, and delete operations with confirmation dialogs.

5. **Scriptural & Spiritual Scale Comparisons**
   - Offline library of verifiable, grounded scale comparisons based on biblical milestones, solar/lunar cycles, and physical history.
   - Free of pseudoscientific neural rewiring claims.

---

## Tech Stack

- **Platform**: Android (minSdk 29, targetSdk 36, compileSdk 36)
- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material 3 & Material Icons Extended
- **Architecture**: Single-Activity, Navigation Compose, Clean Architecture (Repositories, DAOs, DataStore)
- **Local Persistence**: AndroidX Room 2.7.0 with Kotlin Symbol Processing (KSP)
- **Preferences**: Jetpack DataStore Preferences
- **Security & Biometrics**: AndroidX Biometric 1.1.0, JCE PBKDF2WithHmacSHA256 + AES-GCM
- **Serialization**: Moshi 1.15.2 (Kotlin JSON adapter)
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin (AGP) 9.1.1
- **Testing**: JUnit 4, Robolectric 4.16.1, AndroidX Test Core, Kotlinx Coroutines Test

---

## Scripture Licensing & Text Rules

- Scripture quotes must always match their designated reference exactly.
- Never invent, merge, or paraphrase verses while presenting them as quotations.
- Default verified text in this build utilizes verified public-domain translations (such as the World English Bible / WEB).
- If exact verified text is not packaged for a reference, the UI displays the reference with a neutral reading prompt ("Open your Bible to this passage") rather than generic or fabricated text.
- Do not bulk-copy copyrighted translations (e.g. NLT) without proper publication licenses.

---

## Build Requirements

- JDK 21 (Eclipse Temurin 21 recommended)
- Android SDK Platform 36 and build-tools 36.0.0+
- Linux, macOS, or Windows

---

## Build & Test Commands

Steadfast is completely self-contained and clone-and-build capable:

```bash
# 1. Clean and assemble debug APK
./gradlew clean assembleDebug

# 2. Run unit tests
./gradlew test

# 3. Run Android lint checks
./gradlew lint
```

The compiled debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Repository Privacy

During pre-release development, this repository must remain **Private**. If access permissions in your environment do not allow automatic visibility toggling, ensure the repository visibility is set to **Private** in GitHub repository settings.
