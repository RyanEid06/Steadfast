# Steadfast — New Gemini Account Handoff

## Source of truth order

1. CURRENT GITHUB REPOSITORY = latest implementation/code.
2. This handoff package = frozen product/design reference.
3. STEADFAST_V1_CORRECTION_QA.md = required correction rules and QA expectations.

IMPORTANT:
- Do NOT replace current GitHub code with any older project scaffold from this package.
- The package is primarily reference material: design, docs, data, prompts, and product rules.
- Preserve fixes already present in the current GitHub repository.
- Restore reference-only folders/files into the repo where missing:
  - /docs
  - /design
  - /data
  - /prompts
  - GEMINI.md
- These reference files must remain outside Android packaged resources unless explicitly needed at runtime.
- Do not add web/Vite/React companion code. This project is a native Android app.
- Do not start a web dev server.
- Validate Android using Gradle only.

## Latest known correction commit

5c998bcdf44f869ac00cc0a70ac17d90219c094a
Before making changes, confirm the current repository includes this commit or a descendant of it.

## Approved design hierarchy

- Style 1 = overall layout/identity, onboarding/tutorial, circular Dashboard streak.
- Style 3 = dark/light palette and Bible chapter grid.
- Style 2 = Journal treatment.
- Bottom nav = Dashboard / Journey / Bible only.
- Settings = top-right control.
- Dashboard = fixed/no scrolling.

## Current phase

Pre-release physical-device QA preparation.
Do NOT call the app release-ready yet.
