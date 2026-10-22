# 24.12 — C4.7 Full Catalogue Architectural Review

**Phase:** C4.7 architecture, visual test and iterated review. **Prerequisite:** C4.6.1 installed and building.

## Scope

All **61 named C3 programs**, renderer compatibility with C4.6 gallery, small/medium physical sites and Winterfell's *C3 specialist buildings*. No changes to population, household memberships, NPC projection counts, trading/economic jobs, political holdings, world borders, signs, global maps or road/parcel layout.

## Technical rules

- Immutable per-building inputs: `Lot` id/kind/x/z/width/depth, template ID, palette, terrain floor and chunk.
- All writes use `KnownWorldC3Architecture.c47Put` delegating to the already trusted C3 sink, which clips to the **current ChunkAccess** in worldgen and to the **live ServerLevel** only inside a manually requested gallery lot.
- No block-entity text updates, no entity spawns, no live server world access from the enhancement module, no neighbor-chunk loads.
- No exposed flowing lava. Brick kiln/forge simulated with static furnace and colored glass; the bath uses a bounded, shallow below-floor water basin.
- Foundations and basic C3 stairs/ladders/doors retained. Enhancements must not resize any lot beyond C4.6's spatial survey. Bigger institutions require future reallocation of neighboring plots.
- Every enclosed template is lit with a supported fence-and-lantern configuration per physical floor. Most trade buildings gain role-specific furniture or display blocks.
- For future versions: imported blueprints ideally move to generated architectural rules with explicit walls, roofs, rooms, doors, navigable path validation and role-specific props. Persist blueprint version per generated chunk if live retrofits ever become supported.

## Regression gates

1. Actual C4.6 marker and anchor preflight; absent or edited source fails closed.
2. CSV contains 61 unique template IDs.
3. Java class syntax compile with structural stubs.
4. All 61 actual template footprint sizes execute in an instrumentation harness without writes outside their footprint.
5. Gradle `build` against user's local Minecraft 26.3 and whole project's JUnit suite.
6. Fresh superflat `/kwgallery build` and inspection of type labels, rooms, water containment, stair/ladder support and multi-floor lanterns.
7. Fresh Known World at Winterfell and one small/medium pilot. No direct changes to already-generated chunks.

## Open tasks

- Distinctive *structural* facade grammar for largest landmarks and ancient/canon monuments (rather than block-level procedural furnishing).
- Proper animal display only via safe server-side spawning policies tied to household jobs, not during worldgen.
- Optional dynamic mine depth, functioning docks, moving mill mechanisms and dynamic cultural/religious interiors.
- C3 integration for original Winterfell V3 workplaces and future architectural updates across all approved settlements.
- Future server-side shop signs with durable requests; C4.7 leaves C4.6 sign pipeline untouched.
