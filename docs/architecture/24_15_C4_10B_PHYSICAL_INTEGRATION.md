# C4.10B — Opt-in universal physical settlement preview

**Stage:** Code delivered for local checkout; standalone Python / stub-Java checks passed. Not yet proved by Fabric Gradle + world inspection. No 248-site activation.

## Data flow

`B2/B3 site registry -> C1 authoritative physical anchor and palette -> C4.4 job distribution -> C4.8 248-site reference -> C4.9 household/workplace reservations -> C4.10A uncapped layout -> C4.10B explicit selected-site manifest -> C4.9 relocated dry anchor + per-lot terrain survey -> existing C2/C3/C4.2 chunk-local painters + C4.10A fortification painter`.

The C4.10B manifest is not an independent second population model. Every ordinary selected-site lot came directly from the C4.10A plan with C4.9 totals, stable reservation IDs and staffing quotas. Each central manor/keep/castle remains a distinct complex and reserves `coreWorkerSlots` for later B4/D. No extra population or duplicate KEEP building is created.

## Safety boundaries

1. Resource absent -> code dormant.
2. Explicit site IDs required for activation; at most 3, 12,000 lots, no partial-site truncation.
3. Ordinary `GENERAL_COMPACT_SETTLEMENT` tier SMALL/MEDIUM/MAJOR only. Coasts/canals/special/ruins not silently generic.
4. Winterfell/previous C2 pilots, as well as predicted footprint intersections, prohibited.
5. Original map marker may move at most 384 blocks in this preview. Connected dry expansion and core relief checked first, actual curtain/keep dry-grid checked again.
6. Individual physical lots reject water, unreachable door, excessive grade; every rejected lot retains its population/job demand in C4.9. Defer rather than terraform whole mountains.
7. All physical writes are on the currently generated `ChunkAccess`, after vanilla feature suppression and existing worldgen. No already-loaded neighbor chunk writes; test on fresh chunk/world.
8. Worldgen does not write household, genealogy, owner, employment, NPC or political save data.
9. Minecraft logs contain original markers/coords and attempted coordinates for failed central cores; rejected lots include IDs and reason. These are not the same as final worldwide persistent audit reports.

## Hook and ordering

Only `KnownWorldChunkGenerator.java` receives four narrow changes: one import, vanilla structure suppression, natural feature suppression and final `decorate`. The new `KnownWorldC410BPreviewWorldgen` builds its own chunk index on first use; no C2 manifest is replaced. `WinterfellWorldgenV3.decorate` and `KnownWorldUniversalWorldgenC2.decorate` execute unchanged and ahead of C4.10B. The installer refuses unrecognized source and saves a dated backup.

## Gaps carried forward

- Runtime terrain failures can leave the **rest** of the selected settlement built but one workplace/house deferred; the next pass needs a persistent per-site and per-lot audit, replacement-site planning or retry logic for these fragments.
- Current central castle fortification implements walls and body, not lore-correct layout, staffed interiors or rendered social occupancy.
- C4.10B uses generic ring-street tiles for ordinary land. Mountain switchbacks, adaptive retaining walls, regional harbor canals, shore edges and port streets need a differentiated construction module.
- Large city scheduling should stream partitioned districts to the worldgen index rather than instantiate a giant preview resource; temporary preview budgets are not settlement caps.
- Permanent building construction should be versioned by actual world construction state to avoid retroactive regeneration, migration and ownership duplication.
- Full build and real Minecraft verification required before C4.11 production activation.

## Acceptance run

1. Confirm all 11 C4.10B Python tests pass, exact known repository hook markers present, and the local Gradle project builds with Java 25.
2. Selected small, medium and major ordinary sites each show appropriate central fortification, walls and civic/house/workplace outer distribution in fresh chunks.
3. Test sea-side and slope edge cases. No full mountain flattening; no solid core in water; failure log includes site ID, marker IDs, original/attempted X/Z, deferrals and reason.
4. Check no Winterfell/C2 pilot geometry changes and no accidentally populated NPCs.
5. Record actual safe/deferred lots and compare to C4.9 demanded workplace/housing totals, then decide C4.11 readiness.
