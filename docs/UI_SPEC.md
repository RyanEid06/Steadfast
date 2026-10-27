# UI / UX Specification

## Visual language

- The authoritative visual merge is defined in `docs/DESIGN_FREEZE.md`.
- **Style 1 controls layout/composition**, **Style 3 controls the dark/light palette**, **Style 2 controls Journal**, and **Style 3 controls Book Detail/chapter grid**.
- Mature Christian cinematic aesthetic.
- Dark theme: near-black / deep navy surfaces, warm gold accents, restrained cream text.
- Light theme: warm cream/white, muted gold, dark ink text.
- Avoid purple AI gradients, glassmorphism overload, bubbles, neon gaming visuals, cartoon badges, and noisy particle effects.
- Use the cross and horizon imagery as identity, not as decoration on every card.
- Use an elegant serif for major branded/display headings and a highly readable sans-serif for body/UI text.
- Motion should be smooth and deliberate, comparable to polished iOS apps: fades, subtle shared movement, counter transitions, progress-ring motion, and milestone glow. Respect system reduced-motion settings.

## Navigation

Bottom navigation has exactly 3 items:

1. Dashboard
2. Journey
3. Bible

Settings is a small top-right gear/menu control on root screens.

## Dashboard — fixed, no scroll

Purpose: a calm glanceable home, not a data dump.

Hierarchy:

1. Compact top bar: Steadfast identity + Settings.
2. Cross/landscape hero area.
3. Large **circular** live streak counter, following Style 1 as the primary focal point.
4. One Scripture encouragement card.
5. Primary **I’m struggling** action.
6. Bottom navigation.

No long stats, journal list, history chart, milestone list, or Bible book list on Dashboard.

The layout must adapt to smaller screens by reducing hero height/spacing and scaling type within accessible bounds. Do not introduce user scrolling simply to rescue a poor fixed layout.

## Journey

Root sections:

- Overview: current + longest streak, compact history preview, next milestone, actions to motivation/milestones/start again.
- History: streak chart and archived streak cards.
- Journal: list + add/edit note. **Journal visual composition follows Style 2**, while retaining the global Style 3 palette.

Do not flash loading states when local data is already available. Use Room Flow/state and stable keys.

## Motivation

Use segmented tabs/sections such as Time / Comparisons / Scripture Facts. Large numbers should animate smoothly only when entering or changing meaningfully—not every second if that causes unnecessary recomposition.

## Milestones

Vertical milestone timeline. Completed milestones are warm gold; future milestones muted. Current/next milestone is visually emphasized. Celebration overlay is short, dismissible, and not childish.

## Bible

Bible root:

- Overall ring/progress
- OT / NT progress
- Currently Reading cards (max one each)
- Reading goal
- Testament filters/book list

Book detail (visual layout follows Style 3):

- Book name, chapter count, completed count, percentage
- Progress bar
- Chapter number grid
- Completed chapter = gold/warm state
- Unread = gray/neutral
- Current-reading marker is distinct from completion state

## I’m Struggling

Full-screen, calm, low-clutter intervention surface. Background may use a subdued cross/mountain image. Verse/reference occupies the focus. Actions are secondary and stacked.

## Theme behavior

- Theme selection: System / Dark / Light.
- No Material You dynamic-color override of brand identity.
- Theme choice is local app preference and must survive backup import behavior as defined in backup spec.

## Accessibility

- 48dp minimum touch targets.
- Content descriptions for icons.
- Text must support system font scaling without clipping.
- Color is not the only indicator of completion/current state.
- Charts have textual summaries.
- Animations obey reduced-motion preference when available.
