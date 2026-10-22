# C4.8 — Versioned worldwide settlement reference (298 AC)

These are **planning targets**, not existing simulated people, actually constructed homes, or surveyed lots.

- Source A: `settlement_presets_c1_v1.json` — accepted working X/Z, tiers, estimated households, housing requirements.
- Source B: `settlement_job_distributions_c4_4_v2.json` — 248 × 204 profession counts, labor and requested workplaces.
- Source C: `occupation_building_catalog_c4_4_v2.json` — job-to-workplace policy and job metadata.
- No new professions or people invented here. Every C4.4 job count and C1 population retained.
- 253 markers may reference 248 unique sites. **Do not duplicate population for co-located markers.**
- 50 sites have zero population; these generally include ruins/landmarks/unverified places.
- C4.4 adjusted seven regional economic mixes and inferred shares for most job titles; all remain MOD_ESTIMATE.
- 1,880,341 workers is a modeled ~50% share of 3,760,684 people, not 1,880,341 currently instantiated NPCs.
- Zero columns in the 248×204 matrix mean no default workers, not a deleted job.
- `working_x` / `working_z` come from accepted map pins; actual build position may require an approved nearby land point.
- `MAJOR_SPECIAL_REVIEW` is a **planning list**, NOT a record of failed construction.
- Winterfell V3 has 1,136 occupied residential houses; generic C1 estimates only 266 Major-tier buildings.
- Housing demand is a model; actual household graph, employer/owner links, and unbuilt lots require reconciliation.
- Exterior atlas labels are derived only from marker IDs, not from game-generated structure positions.
