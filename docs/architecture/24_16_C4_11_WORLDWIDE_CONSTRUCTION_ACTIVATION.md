# C4.11 — Worldwide settlement construction activation

**Decision:** user explicitly requested automatic global rollout immediately. Do not keep the former arbitrary preview limits or require three manually selected pilot locations. Preserve all existing demand, even when physical generation is blocked.

## Authorities

- Physical site, marker, tier, source coordinates: B2/B3 + C1; C4.9 248-site ledger is the integrated construction-demand handoff.
- Housing, households and exact job-based workplace quota: C4.9, derived from 248 x 204 C4.8 matrices.
- Layout, mixed urban rings, rural outer belt, deterministic lot IDs: C4.10A.
- Actual C3 building templates/staff limits: original C3 catalogue.
- Central walls/castles: C4.10A `KnownWorldC410Fortifications`.
- Site anchor preflight: C4.9 dry connected land and relief, plus C4.11 complete wall/keep shoreline check.
- Chunk-local roads: C4.2.
- Existing Winterfell V3 and C2 sites: retain original worldgen and identities, never rewrite.

## Implementation

A manifest compiler reads all 248 C4.9 sites. It creates an index of site metadata and compact per-site/512-block build tiles under scenario resources. It keeps population, households, jobs, original map anchors, fallback flags and exact lot demand. It reports non-buildable site modes, restricted/unavailable physical templates, original bounding-box collisions, and protected existing construction separately.

Minecraft generator adds `KnownWorldC411GlobalWorldgen` to the existing decoration/feature/structure hooks. Index is loaded once; nearby terrain anchor checked once per site. Local building tiles are lazily loaded with LRU caching; the path for a shifted site is derived by reversing the site anchor delta from the generated chunk, without generating new chunks or loading neighbors. Each planned eligible lot still undergoes a dry-land/elevation/access survey and only approved blocks render into its own chunk. Roads use a local lattice rather than pre-indexing every road pixel in every city. Three-tier central cores remain workplaces for the existing preassigned KEEP worker slots; they do not create extra headcount.

The explicit physical fallback for SPECIAL and extraordinary MAJOR sites is provisional, not lore-perfect, and should be upgraded in future C4 construction batches without reassigning IDs. A site core may fail even with available lots; failed demand is reported, not silently reallocated. **No universal terrace/switchback solver has been implemented in this batch**; steep individual parcels still defer construction.

## State ownership and acceptance boundaries

- This batch does not implement B4/D worldwide persistent identity, employed person/NPC assignment, household ownership, physical family structures, population migration or C6 dynamic rebuilding.
- The quantitative worldwide reference remains planning targets, not a real-worldgen completion report. Generating a manifest is not the same as generating chunks. Terrain failures are not evidence that original C4.8 numbers were invalid.
- Running globally in fresh chunks can be expensive. Do not use a valued current save as the first run; terrain and architecture have not been verified visually at every site.
- Avoid flat world replacements, forced cliffs, arbitrary count reductions, or extra invented settlements.

## Follow-up

After user reports real generated-world results: fix rejected terrain/cross-site overlaps, missing special building modules, actual road connections and terrace/switchback roads; add a persisted construction census and a chunk-completion/repair mechanism. Then B4/D binds actual people, households, workplaces and owner IDs against these stable lot IDs.
