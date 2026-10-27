# Steadfast V1 — Hybrid Design Freeze

This document resolves the three exploratory design boards into one implementation direction.

## Priority hierarchy

### Primary: Style 1 — structure and personality
Use Style 1 as the default answer to layout and composition questions. Its strengths are the calm cinematic Christian identity, generous spacing, circular streak focal point, and onboarding/tutorial flow.

### Override A: Style 3 — colors and theme system
Style 3 replaces Style 1's color treatment. Dark mode must use near-black/deep navy surfaces with restrained warm-gold accents and clean cream/white typography. Light mode should use warm white/cream, gold accents, and dark ink text. Avoid orange overload, muddy gray surfaces, and excessive glow.

### Override B: Style 2 — Journal
The Journal should follow Style 2's denser, practical card/list composition. It should feel like a real personal journal, not a decorative quote feed. New-note access must be obvious, entries readable, newest-first, and edit/delete available.

### Override C: Style 3 — Book Detail / chapter grid
Book detail follows Style 3: clear title/progress summary, compact progress bar, clean numbered chapter grid, gold completed state, neutral unread state, and an independent current-reading marker.

## Dashboard

Use Style 1's Dashboard composition and **large circular current-streak focus**. The circle is the emotional center of the screen.

The Dashboard remains a fixed, no-scroll page and contains only:

1. compact Steadfast identity/settings area,
2. restrained cross/landscape hero treatment,
3. circular current-streak display,
4. one real Scripture encouragement card,
5. one prominent `I'm struggling` action,
6. 3-item bottom navigation.

Time-unit behavior remains governed by the product spec. The circular design must adapt to 3/4/5 displayed time units without turning into a cramped dashboard.

## Icon and splash

Use Style 1's icon concept: illuminated cross, dramatic but peaceful sky/horizon, strong central silhouette, warm light. Production art must remain legible at small launcher sizes and inside adaptive-icon masks.

Splash may use the same world/visual language, but should not be a long blocking animation.

## Onboarding / tutorial

Use Style 1's onboarding/tutorial style: visually led, short copy, cinematic illustration, clear single next action, page-position feedback. Setup fields may be more functional than the concept art, but must retain this visual language.

## Journal

Use Style 2's journal as the visual basis. Cards should prioritize date/time, note title/first line, readable body preview, and optional context badge. Avoid huge decorative imagery that reduces information density.

## Bible chapter view

Use Style 3's chapter screen. Completion is shown through gold/filled chapter cells; unread is neutral/dark or light neutral depending on theme. Current-reading state must be visually distinct from completion and cannot rely on color alone.

## Global motion

Smooth, restrained, professional motion only: counter transitions, ring/progress animation, page transitions, cross glow, and milestone emphasis. No bouncing cartoon UI, confetti spam, floating bubbles, or constant ambient motion.
