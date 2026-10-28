# C4.16 + B4.2A — Organic Settlement and Enforced Work Goals

## Compatibility

Requires **installed** C4.15, B4.1A.1, B4.1B, B4.2 **and the B4.2 clock compile hotfix**. The installer checks that four local source files match the known versions before touching anything. If your IntelliJ project has independent local edits, it prints `UNKNOWN_LOCAL_EDITS` and stops. This is intentional: **never blindly overwrite local work**.

This package does not include B4.0 NPC regeneration. It leaves all 3,760,684 starting identities, 799,343 households, 1,880,341 job holders, conversation histories, saved campaign time, and Winterfell V3 intact. It preserves lot reservation IDs and original map markers, but moves some *physical coordinates*. C4.11 and B4.1 indices must be rebuilt together.

## Install

1. Make a separate copy of your **existing saves and project** if they are important. Stop Minecraft and close any running Gradle build.
2. Extract this ZIP **at the repository root**, e.g. `D:\GameOfThronesMod2`. Leave `C4_16_STAGE` intact. **Do not manually copy** `C4_16_STAGE/updated` over your Java source files. Extraction will add the BAT, the staged directory, tests and installer; it will not overwrite live source files.
3. In PowerShell:

   ```powershell
   cd D:\GameOfThronesMod2
   .\RUN_C4_16_ORGANIC_TOWNS_AND_WORK_ATTENDANCE.bat
   ```

4. The BAT first runs a read-only compatibility check and regression tests. It then **copies all affected source files and existing generated resources** to a timestamped snapshot and updates source. Subsequently it rebuilds C4.11 worldgen tiles, C4.15 commute assignments, B4.1A.1 NPC tiles and B4.2 worksite index. The generation phase can be substantial for 248 sites and millions of people.
5. If BAT succeeds, run:

   ```powershell
   .\gradlew.bat clean build
   .\gradlew.bat runClient
   ```

6. **For geometry comparisons, use a fresh Known World test save or previously unexplored chunks.** Previously generated Minecraft chunks cannot be changed by changing worldgen resource JSON. Do not mix old and new geometry in the same settlement if evaluating roads/buildings.

## In-game diagnostics

```mcfunction
/kwdaily status
/kwdaily attendance
/kwactors nearby
```

`/kwdaily attendance` provides counts of physical projection route attempts, nearby work arrivals, unloaded waypoints, invalid landing candidates, missing assigned worksite requests and longer journeys. These counters are **attempt-based**, not a verified labor attendance census. The worldwide simulation remains an offscreen **schedule model**, not authoritative goods production.

### Implemented

- **Work goal priority:** loaded projected villagers repeatedly request work routes during their scheduled commute/work windows; the home-distance snap-back no longer cancels those work objectives.
- **Incremental pathfinding:** 3–18-block loaded-chunk waypoints toward the planned workplace; no teleports to far workplaces, no forced chunk loads, no assertion of successful arrival unless an NPC gets near the site.
- **Greater settlement variation:** per-house deterministic dimensions, larger/asymmetric front setbacks, 31-block base planning cell safety retained to prevent structural overlaps, street bend/width variation and selected service rather than every plot receiving four box edges.
- **Green pockets:** larger 19x19 reservations with up to three small trees, grasses, ferns and occasional flowers chosen by region. Garden variation comes from differing house setbacks and pocket placement.
- **Outer work access:** cardinal winding connector trunks extend from castle gate approaches toward the actual outer settlement bounds, including chunks with no building lots.
- **Rollback snapshots:** every overwritten source file and the current C4.11 index/tiles, commute index, B4.1 projection, B4.2 workplace index, and layout preview are backed up first.

### Not yet guaranteed

- This is not a full freeform medieval road-mesh generator. Buildings remain on cardinal grids with *irregular positions and frontage*, and not every field/wood lot has had a flood-fill walkability check. Road/field connectors are drawn conditionally on land/slope and may be broken by water, cliffs, or failed construction. **Do not interpret preliminary connectivity as proven navigability.**
- NPCs beyond their player's loaded chunks or a projected actor limit of 128 are virtual; their work shifts are modeled, not physically simulated. Short waypoint navigation can still fail if a road/door is blocked or inaccessible. Missing/invalid occupation assignments are logged rather than fabricated.
- Individual labor does not yet produce items or authoritative economic resources. B4.2B is still necessary.
- The installer does not upgrade existing generated chunk structures. Old NPC location indices work only with the matching new construction resources in new chunks.
- There is no arbitrary building rotation; the house entrances remain south-facing for compatibility with B4.1A.1.

## Recovery and rollback

Find your timestamped snapshot in:

`tools\geodata\output\c4_16_safe_snapshots\<TIMESTAMP>`

To roll back **both code and generated resources** (after stopping Minecraft), run from the repo root:

```powershell
py -3 tools\geodata\c4_16_stage_apply.py --rollback <TIMESTAMP>
.\gradlew.bat clean build
```

Use the exact folder name shown after `BACKUP SAVED:` in the installer. Snapshotting happens before the source edit and generators. Rollback restores *only* this batch's managed paths; it doesn't touch your Minecraft saves, B4.0 census, B4.1B dialogue files or other unrelated changes. **Do not rerun the regeneration BAT after rollback** unless you intend to reapply C4.16.

If the installer stops with `UNKNOWN_LOCAL_EDITS`, do **not** use `git reset`. Send the full `--check` output and the affected source file; this installer intentionally fails closed before changing anything.

## Testing evidence

- Nine Python regression/integrity tests passed in a synthetic repository, including apply/backup/rollback and exact modeled household/workplace slots.
- Java 21 interface-level stub compilation passed for the B4.2A daily-life class and C4.16 worldgen renderer. Those stubs are *not* a real Fabric build.
- Not verified: complete Fabric compilation in your checkout, terrain placement across all 248 sites, exact path continuity or successful NPC arrival. You will verify those with the local `gradlew clean build` and a new test save.
