# Milestone + Comparison Logic

## Milestones

Definitions are in `data/milestones.json`.

Elapsed-day milestones are exact durations from streak start. Calendar-month/year milestones use calendar arithmetic in the stored start zone.

The 90-day and 3-month milestones are intentionally both present even when they occur close together; they represent different concepts.

## Celebration

- One celebration per milestone per active streak session.
- Dismissible immediately.
- A missed celebration should be shown on next app open, but do not stack multiple full-screen celebrations. If several were crossed while away, show the highest newly reached milestone and mark the earlier ones reached.

## Impact counters

Derive exact integer totals:

- totalSeconds = floor(elapsed milliseconds / 1000)
- totalMinutes = floor(totalSeconds / 60)
- totalHours = floor(totalSeconds / 3600)

## Comparison facts

Production comparison facts must be:

- locally stored
- mathematically reproducible where possible
- accompanied by a source note for any external constant
- phrased conservatively
- never medical claims

Do not ship the illustrative comparison text embedded in generated concept art as factual data unless independently verified.
