# C4.11 — Worldwide construction activation (all eligible physical sites)

**This is the unrestricted global rollout requested, not a three-site demonstration.**
The batch adds no MAX_PREVIEW_SITES, MAX_PREVIEW_LOTS, city-radius limit, residential cap or workplace cap. It reads your **existing C4.9 248-site** source and calculates residential/workplace lots through **C4.10A**. It does NOT create new demand, jobs or family identities.

## Install (Windows PowerShell)

1. Keep the C4.9, **corrected** C4.10A and C4.10B files installed. You have already run the C4.10B hook installer and standard build. Extract this C4.11 ZIP into your `D:\GameOfThronesMod2` repository root, preserving its directories.
2. Run `./RUN_C4_11_GLOBAL_ROLLOUT.bat` in PowerShell. **No site-selection prompt**: the script validates tests, audits all 248 sites, installs the additional three generator hooks, writes all per-site construction tiles and activates the global index. This is not a world-edit script; no Minecraft world files are touched by it.
3. **Rebuild the Minecraft mod** with the normal Gradle command you already used. The generated JSON tiles and global index must be bundled as resources in the new mod JAR.
4. Start a **new world** or go to previously ungenerated chunks. The mod automatically attempts settlements when Minecraft naturally generates chunks covering their extents. Existing generated chunks stay unchanged. The terrain policy may relocate an approved core by at most 512 blocks from its original map anchor; original map X/Z and actual construction X/Z are both logged.
5. Inspect `tools/geodata/output/c4_11_global/C4_11_WORLDWIDE_CONSTRUCTION_AUDIT.json` for all 248 site counts, exceptions and potential bounding-box collisions. Search `logs/latest.log` for `C4.11 SITE APPROVED`, `C4.11 SITE FAILURE` and `C4.11 LOT FAILURE`.

## Exact scope and limits

- All **248 physical sites** remain in the index and audit, including the 50 uninhabited/ruin/marker sites. Non-buildable, zero-population or mobile-only sites do not get invented towns. Every inhabited, nonblocked and previously unbuilt site receives a **global construction attempt**.
- **Winterfell, Ramsgate, Acorn Hall** and all sites already in the C2 manifest retain their existing generators/IDs and are not duplicated. Their real structures remain as before; they are still included in the worldwide population totals.
- **Special MAJOR** settlements (e.g. coastal/canal/rock cities) and **inhabited SPECIAL** sites are included using an explicit **provisional generic castle/settlement fallback**. Original tier, blueprint mode, exact worker and housing counts remain in the audit. This generates an attempt without claiming accurate Braavos canals or Meereen pyramids.
- Every ordinary house/workplace with an available C3 renderer is packaged as a tile-local build request. Nonexistent, oversized, shore-restricted and otherwise unapproved C3 templates remain **reported as deferred** rather than fabricated and reducing economic quotas. An actual terrain failure is logged with site ID, lot ID, template and position.
- The original marker coordinates **are preserved**. Only the construction anchor is shifted if the C4.9 dry-land/relief search finds a better location nearby. Terrain surveys still reject unsafe lots rather than flattening mountains or building in water. This does not magically guarantee all demand fits geography.
- All blocks are placed in **fresh chunks only**, using the current C3 template painter, C4.10A castle walls and the C4 road painter. Structures already in the save are not regenerated.
- **No new physical NPCs, families, owners or workers** are spawned. Planning numbers for 3,760,684 worldwide residents and their 204 occupation categories are preserved and available for the NPC phase; they are not instantiated by this batch.
- Runtime uses a bounded **96-tile cache**, and only resolves site terrain and geometry near generated chunks; it is **not** a single 400,000-lot pre-index in memory. Even so, full-map generation has not been benchmarked and may expose performance issues.

## Disable if necessary

Run `./RUN_C4_11_DISABLE_GLOBAL.bat`, type `DISABLE`, and **rebuild** the JAR. This stops new global construction, not structures already written into world chunks. Keep a separate backup world if you may need rollback.

## What was tested here

Python global compiler/activation regression suite; representative 248-site inventory, 46,071-lot major-city demand fixture, staffed workplace conservation, existing-site protection, special city fallback, missing-template deferral and exact-hook idempotency. Java C4.11 sources compiled against minimal Minecraft/Gson interfaces under Java 21. **Full Fabric Java 25 build, real geography sampling and in-game worldgen have not been executed here.**

## Supplied files

- `tools/geodata/c4_11_global_rollout.py`
- `tools/geodata/c4_11_install_hooks.py`
- `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java`
- `src/test/python/test_c4_11_global.py`
- `RUN_C4_11_GLOBAL_ROLLOUT.bat`
- `RUN_C4_11_DISABLE_GLOBAL.bat`
- `docs/architecture/24_16_C4_11_WORLDWIDE_CONSTRUCTION_ACTIVATION.md`
- `README_C4_11_START_HERE.md`

GitHub remains READ-ONLY. No GitHub mutation or Minecraft save edits are performed by the batch installer; source files are installed in your own local checkout.
