# GEMINI.md — implementation contract

This file is the top-level contract for any coding agent.

## Product truth

The frozen product is **Steadfast**, an offline Christian recovery + Bible-progress Android app. Do not rename it, reinterpret it as a porn-only app, add cloud features, or redesign the navigation without explicit approval.

## Visual implementation contract

Before UI work, read `docs/DESIGN_FREEZE.md` and `design/README.md`. Do **not** choose one board wholesale and do not blend them arbitrarily. Style 1 is structure/identity; Style 3 is palette; Style 2 is Journal; Style 3 is Book Detail/chapter grid.

## Implementation behavior

1. Work in small, reviewable phases from `prompts/`.
2. Before editing, inspect the existing repo and preserve working behavior.
3. After each phase run unit tests, lint, and a debug build. Fix failures before moving on.
4. Do not leave placeholder buttons, dead navigation, fake loading states, fake charts, or TODO-only production paths.
5. No network permission unless explicitly approved later. V1 must function in airplane mode.
6. Use current **stable** Android/Kotlin/Compose dependencies available at implementation time; avoid alpha/beta libraries unless unavoidable and documented.
7. Prefer standard AndroidX/Compose APIs over adding dependencies.
8. Never hardcode user-specific test data in production.
9. Never invent Bible verse text. Use only reference metadata unless licensed/user-provided verse text is present in `data/`.
10. Never add shame language such as “you failed.” Restart language must be encouraging and factual.
11. Do not make unsupported medical or habit-formation claims.
12. For recovery focuses involving substances, the app must never present itself as medical treatment; include the safety copy specified in `docs/PRODUCT_SPEC.md`.
13. Preserve accessibility: semantic labels, minimum touch targets, scalable text, contrast, reduced-motion behavior.

## Quality bar

The app should feel like a polished first-party mobile product: fast launch, no flicker, no unnecessary spinners, no layout jumps, no repeated database scans on each second tick, no janky chart recomputation, and no main-thread I/O.
