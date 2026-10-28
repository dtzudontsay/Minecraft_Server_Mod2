# B4.1A.1 — Projection and profile parity bridge

## Status

Implemented: spatially partitioned C4.11-home-based B4.0 physical candidates, more than two candidates per household, unique nearby spawn attempts, vanilla local actor movement, enriched deterministic right-click character sheets, improved nonnoble display naming. Does not claim Winterfell V3 full authoritative NpcRegistry, OLLAMA dialogue or world events.

## Authorities

- Initial identity/census: existing B4.0 `world_population_b4_v1/*.jsonl.gz`; NEVER regenerate IDs or change job totals.
- Home reservations, position: C4.11 available `HOUSE` lots. Terrain translation: C4.11 placement accessor.
- Culture, faith, language: B4.0 person fields; B2/B3 region-specific numeric influences from `world_sites_b2_b3_v1.json`.
- Existing canon/author identities: Winterfell V3 and scenario packs; not imported again.

## Movement and projection

At most 128 worldwide physical avatars are selected from nearby tiles around the player. Projection is a view of existing identities; physical entities are discarded on departure and on reload rather than causing population inflation. No chunk generation is forced from NPC logic. Vanilla AI uses local movement, with distant actors returned to their own front-door neighborhood; do not interpret movement as executed offscreen journey.

## Profiles and reproducibility

The person `profileSeed` drives deterministic baseline approximation. Occupation and age weight learned skills. B2 regional `values` and `socialNorms` bias individual convictions/norms with variation. Until B4.2, no skill growth/experience overrides are stored in this generated profile layer.

## Follow-ups

- B4.1B: generalize K-key dialogue with per-pair private histories, verified knowledge, social credibility, safe identity lookup for B4 persons.
- B4.2: persistent profile overlays, household routine registry, per-lot actual-construction ledger, offline productivity and scheduled work/home movement.
- B4.3: movement itineraries and encounter visibility, offscreen event evaluation and regionally scoped information propagation.
