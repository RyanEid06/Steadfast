# Product and design decisions

This log records the frozen product direction and focused V1.1 additions. Implementation details are in [Architecture](ARCHITECTURE.md).

## Product

- Product name: **Steadfast**.
- Equal recovery and Bible focus, with multiple predefined recovery focuses.
- Local, offline-first use; no accounts or cloud service.
- No notifications and no mandatory daily check-in.
- Preserve restart history; show current and longest streak as headline statistics.
- Exact streak date and time, with elapsed duration plus calendar-accurate months/years based on the stored start zone.
- Milestones: 1, 3, 7, 14, 21, 30, 60, and 90 elapsed days; 3, 6, and 9 calendar months; and 1 calendar year.
- Full personal journal. The sanctuary uses a persistent exhaustive 500-passage WEB deck; completed chapters prioritize only unused entries, and a cycle boundary cannot immediately repeat the last passage.
- Bible tracking uses the 66-book Protestant canon, chapter completion, book/testament/whole-Bible progress, and at most one current-reading bookmark per testament. No Bible streak.
- Bible text is not embedded wholesale until rights are resolved.
- Reading goal is a motivational minimum of **1–5 chapters**, not a measured daily completion metric.
- System, dark, and light themes.
- App lock uses Android biometrics/device credentials; no custom in-app PIN.
- The home-screen widget uses static cross/streak artwork that brightens with the current streak and resets to dim. Existing focus privacy and restore gates remain authoritative.
- The chronological Journey chart, durable Bible/Journey/journal view choices and full offline WEB passages are focused V1.1 changes; package, signing identity, Room schema 1 and existing user storage remain compatible with the delivered private V1.
- English for V1; modern Android phones with API 29 minimum.
- Public GitHub V1.1 publication is authorized after private upgrade validation and owner physical-test acceptance. Play Store publication is outside this update.

## Navigation and visual direction

- Bottom destinations: Dashboard, Journey, and Bible. Settings is not a bottom destination.
- Mature cinematic cross/mountain visual direction; smooth, restrained motion and no cartoon styling.
- Hybrid visual freeze (2026-10-27): Style 1 is the primary layout and identity; Style 1 launcher icon, Dashboard, onboarding, and tutorial; Style 2 Journal; Style 3 Book Detail/chapter grid and app-wide dark/light palette. Elsewhere, use Style 1 structure with Style 3 colors.

## Explicit exclusions

- No cloud/account feature, notifications, Bible streak, erase-all-data button, or telemetry dependency.
- No separate OT and NT daily goals; the single reading target is a motivational setting.
