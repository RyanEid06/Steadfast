# Design references — HYBRID FREEZE

The approved V1 visual direction is a deliberate hybrid. **Do not average or freely mix the three boards.** Follow this priority map:

1. **Style 1 is the primary structure and identity** — `mockups/canonical/style_1_PRIMARY_structure_icon_dashboard_tutorial.png`
   - launcher icon concept
   - overall visual composition and hierarchy
   - circular Dashboard current-streak treatment
   - onboarding/tutorial presentation
   - default component feel unless another rule below overrides it
2. **Style 3 owns the color system** — `mockups/canonical/style_3_PALETTE_and_CHAPTER_reference.png`
   - dark mode: near-black/deep navy surfaces, restrained warm gold, cream/white text
   - light mode: warm cream/white with gold and dark ink text
   - use Style 3 contrast/surface discipline even when implementing Style 1 layouts
3. **Style 2 owns the Journal layout** — `mockups/canonical/style_2_JOURNAL_reference.png`
4. **Style 3 owns Book Detail / chapter-grid presentation**.

## Canonical crops

- `canonical/01_icon_style1.png` — icon reference.
- `canonical/02_dashboard_structure_style1.png` — Dashboard structure reference; keep the large circular streak presentation.
- `canonical/03_onboarding_tutorial_style1.png` — onboarding/tutorial visual reference.
- `canonical/04_journal_style2.png` — Journal visual reference.
- `canonical/05_chapter_grid_style3.png` — chapter-grid visual reference.

The three original full boards remain under `mockups/full/` for context. Their historical filenames are not priority indicators.

`branding/icon_concept.png` is a concept only. The production icon must recreate the Style 1 illuminated-cross/mountain composition inside Android adaptive-icon safe zones; do not simply ship a rectangular crop.

If any image contradicts `docs/PRODUCT_SPEC.md`, `docs/UI_SPEC.md`, or `docs/DESIGN_FREEZE.md`, the written specifications win.
