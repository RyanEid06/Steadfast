# Technical Architecture

## Stack

- Kotlin
- Jetpack Compose + Material 3 primitives with a custom Steadfast theme
- Single-activity architecture
- Navigation Compose
- Room for relational/persistent user data
- DataStore Preferences for small settings/onboarding state
- AndroidX Biometric for biometric/device-credential lock
- Kotlin coroutines + Flow
- ViewModel + immutable UI state
- AppWidget: Jetpack Glance if stable/compatible at implementation time; otherwise standard AppWidget APIs
- Local JSON assets for Bible structure, recovery focuses, milestone definitions, and verse metadata

Use current stable versions available when implementation begins. Avoid speculative dependencies.

## Recommended package structure

```
com.steadfast.app
  core/
    design/
    time/
    security/
    backup/
    util/
  data/
    db/
    dao/
    entity/
    repository/
    seed/
  domain/
    model/
    usecase/
  feature/
    onboarding/
    dashboard/
    journey/
    journal/
    milestones/
    bible/
    struggling/
    settings/
  widget/
```

## State rules

- Room is the source of truth for streak sessions, journal, and Bible completion.
- DataStore is source of truth for theme, lock toggle, onboarding completion, reading target, currently-reading pointers, and last-seen celebration IDs where practical.
- UI collects flows lifecycle-aware.
- Dashboard's second ticker is in-memory and scoped to the visible lifecycle. It must not write to DB and must not trigger a DB query each tick.

## Time model

Persist:

- `startEpochMillis`
- `startZoneId`
- `endEpochMillis` for archived streaks

For elapsed seconds/minutes/hours use exact duration between instants.

For calendar years/months/days in the primary display, compute using the stored start zone so changing device timezone does not make the streak visually jump by a calendar day. Then derive remaining hours/seconds from the exact instant difference consistently.

Test leap years, DST transitions, month-end starts, and timezone changes.

## Performance

- Never scan all Bible chapters to compute every UI label on every recomposition. Use query/aggregate flows or efficient in-memory derived state.
- Journal uses indexed timestamp ordering.
- Streak history chart derives when the session list changes, not every frame.
- Seed assets load once and are cached/repository-backed.
- No artificial loading spinner for local synchronous/fast operations.
- Avoid storing second-by-second streak values; store timestamps and derive.

## Testing

At minimum:

- Unit tests for streak/calendar calculations
- Unit tests for milestone detection
- Room DAO tests
- Backup round-trip tests
- Bible progress percentage tests
- ViewModel tests for restart and chapter toggle
- UI smoke tests for onboarding/navigation
- Widget state test where practical
