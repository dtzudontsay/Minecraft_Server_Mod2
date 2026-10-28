# C4.10A — Universal settlement layout + fortified-core painter

**Repo:** `dtzudontsay/Minecraft_Server_Mod2`  
**GitHub ownership:** assistant reads only; user applies/pushes files  
**Status:** OFFLINE LAYOUT PREFLIGHT + INACTIVE CHUNK-CLIPPED RENDERER. No mass worldgen activated.

## Installation (after C4.9)

1. Back up local repository and your Minecraft test saves. Extract this ZIP into the repository root **preserving paths**. The package adds new files only.
2. Ensure C4.9's `RUN_C4_9_PLAN_READONLY.bat` has already generated:
   `tools/geodata/output/c4_9_settlement_ledgers/C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json`.
3. Run `RUN_C4_10A_PREFLIGHT.bat` from the repository root, or manually:

   ```bat
   py -3 -m unittest discover -s src/test/python -p test_c4_10_shared_layout.py -v
   py -3 tools/geodata/c4_10_shared_layout.py --summary-only
   py -3 tools/geodata/c4_10_shared_layout.py --site site_nw_031
   ```

   The last command generates only Ramsgate's actual provisional lot list; without `--site`, all 248 sites are considered. To materialize all ordinary lots, run without `--summary-only` and without `--site`.
4. For the Java class, run `gradlew.bat test` on your machine. Only an offline synthetic Java stub compilation + smoke test was possible in this environment. The supplied JUnit class still needs real Minecraft dependency compilation.
5. **Do not replace the existing C4.5 settlement manifest or Winterfell V3 files** with C4.10A output. These are layout plans for the next gated runtime bridge.

## Generated files

`tools/geodata/output/c4_10_shared_layout/`:

- `C4_10_SUMMARY.json`: expected residential/workplace counts and preview status
- `C4_10_SITE_GEOMETRY.json`: one core/wall/ring plan per selected site
- `C4_10_EXCEPTIONS.json`: special/ruined/mobile/Winterfell sites kept out of unattended physical rollout
- `C4_10_ALL_PLANNED_LOTS.jsonl`: emitted when not using `--summary-only`; can be large in a 248-site run

## What is materially implemented

- An uncapped square-ring lot planner for Small, Medium and Major, with deterministic site/building IDs, small inner wards, interleaved homes/urban workplaces, and rural buildings on the outer rim.
- Explicit assignment of C4.9 household-reservation ID ranges to residential buildings and exact planned worker positions to each requested workplace. It does not create NPCs, owners, or families.
- All required workplace types are retained in the plan, even when the Minecraft renderer or terrain approval is missing. Missing/restricted modules are labeled and counted, **not silently skipped**.
- One core per ordinary populated settlement. Work positions for `KEEP` occupations are housed in that core, without generating phantom employees or a separate repeated keep.
- A new **Minecraft chunk-clipped** Java painter with simple Small palisade/manor, Medium stone-wall/minor keep, and Major Winterfell-scale two-curtain castle geometry. It is an actual rendering method, but deliberately has **no registration or live worldgen hook** until all overlaps, terrain and save safety can be approved.

## Deliberate limitations

- C4.9 Java terrain policy is not connected to worldgen yet; the unmodified map anchor in the preview is **not** approved placement. Coast/mountain retries and persistent failure records are still integration work.
- Roads are *logical geometry* only in this batch; no guaranteed navigable 3D road placement or stair/switchback renderer. Fields are not fabricated by a fixed number per farm; economics must determine acreage later.
- Physical district paging, chunk-index publishing, streaming new-world activation, and compatibility with existing generated chunks are **not done**.
- No complete source-backed separation exists yet of eligible adult workforce, unemployment and dependent/nonworking population. C4.8 `workingPopulationTarget` is an employed-occupation planning quota, not a census of all working-age people.
- Housing templates currently verified in C3: `HOUSE_SMALL`, `HOUSE_MEDIUM`, `HOUSE_URBAN`; no separate `HOUSE_LARGE` template has been verified.
- Exactly 204 occupational IDs are currently available per site in C4.8. Job allocations and special authored people must eventually be reconciled through B4/NpcRegistry. Winterfell's 5,000 people / 1,136 existing homes stay untouched.
- `SPECIAL` is a physical tier, not necessarily “unknown.” Inhabited SPECIAL sites need site-specific policies, and zero-pop ruins/unverified sites must not turn into towns.

## Next runtime integration gate (C4.10B)

1. Load preflighted, versioned per-chunk lot/road/core geometry into a **single** generic worldgen owner; do not scan 248 cities every chunk.
2. Resolve and store construction anchor from the C4.9 land/relief sampler; resurvey every lot and street. Add stable road topology, terrace/retaining-wall and switchback painting.
3. Render the C4.10A core safely on ungenerated chunks; connect to C3 residential/workplace painter and C4.6 Winterfell adapter (no duplicate NW-029 core).
4. Save structured failed-site/failed-lot reports and remaining demand exactly, with no reduction of requested households/jobs.
5. Benchmark and visually inspect at least Ramsgate, Acorn Hall, a mountainside, a shoreline, Winterfell and a large Major test city before worldwide activation.
