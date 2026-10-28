# 24.20 — C4.15: globally revised settlement morphology and starting-place commute policy

## Contracts

**Input remains authoritative:** 248 site records, 3,760,684 people, 799,343 households, 1,880,341 workers, 204 occupation IDs, all C4.9 residential/workplace quotas. No fallback is permitted to drop a household, job, site or existing authored NPC. Existing Winterfell, Ramsgate and Acorn Hall remain protected.

**Structural IDs are invariant:** the `site:*:home:%07d`, `site:*:hh:%06d` and `site:*:work:KIND:%06d` sequences are independent of actual world coordinates. This permits an already-authored population to be rebound to revised footprints in new worlds without changing person IDs. However world saves with old chunks are *not* retconned; using an old save yields mixed incompatible positions.

**Layout:** ring-ordered, variable module size. Effective frontage PITCH=31 replaces 56. A lot requesting more than `31-11` blocks in either direction reserves multiple cells to preserve clearance. Sparse omitted cells become green pockets (not counted as additional residential/workplace buildings). Existing mixed homes/workplaces and remote rural belt remain. Building orientation remains south pending a full rotated architecture/road grammar.

**Roads:** shared deterministic warped block nodes generated from global cell coordinates, yielding joint road segments across plot boundaries. Every ordinary eligible lot has a south doorway spur into the network. The runtime uses chunk-local bitsets with 512-block on-demand tile resource paging; no global road-pixel index. Painted path segments can be absent due to terrain grade/dry-land veto; final walkability must be verified independently in a future pass.

**Castles:** tier-separated physical castle renderer. Small hall is timber and palisade-protected. Medium minor keep with corner towers and wings. Major great keep is ~89×87 in its main footprint, with front wings and towers, surrounded by two large stone curtains (half spans 171 and 149). These are not substitutes for hand-authored Casterly Rock, Eyrie, Braavos, etc. C4.11 extraordinary cities remain provisional generic fallbacks.

**Population projections:** B4.1A.1 `safeSpawn()` uses front-of-home block search and limits roof-elevation discrepancy relative to the pristine terrain. No mutation of person identity, house membership or current household data. It is an explicit conservative placement filter, not NPC navigation.

**Worker assignment:** job class quotas remain fixed from the C4.8/C4.9 matrix; a new *sidecar* keyed by existing immutable person IDs uses C4.4 job->workplace-kind mapping, C4.11 lot IDs and reserved staff capacities, with a 600-block Manhattan proximity proxy. Rebind only when a compatible slot exists within the proxy cap. A failed match is persistently audited, not dropped or relabeled unemployed. Generic KEEP/core assignments are exempt and kept as core jobs. Do not treat a pass as proof of ten-minute route travel; validated route graph and settlement house/field redistribution will be required for that.

**Future work:** per-door terrain-verified path connectivity and shortest path; inner-ward gate entrances and rotated footprints; advanced topographic terraces; work/farm commutes; actual workplace location/schedule persistence and skill-based productivity; external NPC dialogue A1-A5 safely generalized worldwide.
