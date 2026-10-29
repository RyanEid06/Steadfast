# Milestone + Comparison Logic

## Milestones

Definitions are in `data/milestones.json`.

Elapsed-day milestones are exact durations from streak start. Calendar-month/year milestones use calendar arithmetic in the stored start zone.

The 90-day and 3-month milestones are intentionally both present even when they occur close together; they represent different concepts.

## Celebration

- One celebration per milestone per active streak session.
- Dismissible immediately.
- Current catch-up selection shows the highest achieved milestone whose ID has not been recorded as shown. Dismissing it records that ID only; earlier unshown milestones can be selected on later evaluations. The implementation does not yet coalesce all missed milestones into one recorded batch.

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
