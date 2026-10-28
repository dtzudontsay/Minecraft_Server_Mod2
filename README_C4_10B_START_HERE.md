# Known World — C4.10B | Guarded Physical Worldgen Preview

**Install the C4.9 and corrected C4.10A files first.** Your C4.10A preflight should have passed.
This batch is additive. It does not upload, push, or edit your GitHub repository.

## What is actually implemented

- Adds `KnownWorldC410BPreviewWorldgen`, called through three guarded hooks in the **existing** `KnownWorldChunkGenerator` (structure exclusion, biome-decoration exclusion, final settlement decoration).
- Uses C4.9 **248-site population/workplace demand** and C4.10A **three-tier geometry**. Does not invent new profession quotas or families.
- Explicitly selected **ordinary SMALL / MEDIUM / MAJOR** sites use a central `MANOR_HALL / MINOR_KEEP / MAIN_CASTLE` fortification from C4.10A.
- Compiles the complete residential and workplace lot demand of selected sites; no silently discarded residences or workplaces. Occupation-to-staffing calculations are retained in C4.9/C4.10A.
- Uses existing C3 named architectural templates to render houses and allowed workplaces. HOUSE_SMALL / HOUSE_MEDIUM / HOUSE_URBAN variants follow tier. Region palette comes from original C1 preset.
- A candidate build anchor is found using C4.9's connected dry-land and relief policy (small, deliberately bounded preview search of 384 blocks). The whole layout is translated to that one approved anchor, so houses are not left behind at the original marker.
- Wall/keep additionally rechecked on a finer dry-land grid. Each lot is separately surveyed for land, grade and safe entrance. Steep/unsafe individual buildings are **deferred**, with once-per-lot warnings in the Minecraft log, not forcibly flattened.
- Street grid and gate spokes rendered by the existing terrain-following C4.2 road painter. Rural buildings remain in C4.10A's outer rings.
- *No NPC spawning, actual employment allocation, family generation, retrofit of old chunks or automatic worldwide activation.*

## What still is not implemented

- This is a **selected-site preview**, not the 248-site rollout. The preview limits selection to 3 sites and a total of 12,000 planned lots as a protection against accidental massive generation. These are **preview activation budgets, not village/city building caps**; over-budget previews abort with an explicit error, never silently truncate demand.
- Coastal/canal/pyramid/rock-specific `MAJOR` and `SPECIAL` sites are rejected rather than turned into ordinary inland towns.
- Site search may fail, and the core's approval does not imply every residence/workplace will pass its own survey. The output and logs show outstanding demand.
- The road lattice is a first general terrain-following solution, **not a fully connected mountainous switchback/retaining-wall network**. Severe slopes are deferred; they must not be mistaken for built lots.
- No automatic occupied-family, business-ownership or staffing records are written. Physical structures are visual counterparts of C4.9 reservations; actual identity assignment remains B4/D.
- This batch does not validate the full Minecraft Gradle build or in-world looks. **Use a separate fresh-world test save.**

## Installation and first test (Windows PowerShell)

1. Copy the ZIP contents into `D:\GameOfThronesMod2` (or your own checkout root). Preserve `src/...`, `tools/...`, `docs/...`, and `.bat` relative paths.
2. Run `./RUN_C4_10B_INSTALL_HOOKS.bat`. It tests the specific current `KnownWorldChunkGenerator.java` text, prints its diff, installs **only those 4 exact changes**, creates a dated local backup, and runs the Python regression suite. It aborts on unexpected source, without partial writing.
3. Test Fabric compilation locally (Java 25 is required by the repository): `./gradlew.bat test` or your usual existing build/test workflow. **Do not activate a site if the build fails.**
4. Run `./RUN_C4_10B_SELECT_SITE_PREVIEW.bat`. It prints small/medium/major candidate site IDs from your **own** C4.9 output, asks you to choose one, then runs the **read-only** layout preflight.
5. Read `tools/geodata/output/c4_10b_preview/C4_10B_PREVIEW_AUDIT.json`. It includes selected marker coordinates, every lot count, and up to 20 examples of template/policy exceptions. The report does **not** claim terrain was tested offline.
6. Only if you choose to try this site in a **new or backed-up Minecraft world**, enter `ACTIVATE` at the batch prompt. That writes `src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_10b_preview_v1.json` (no other source resource rewritten).
7. Rebuild/relaunch the mod; enter a **fresh Known World save/new chunks** at that site's marker vicinity. Watch `logs/latest.log` for `C4.10B preview APPROVED`, translated construction coordinates, `C4.10B UNSAFE LOT` reasons, or a rejected-core reason. A nearby coast/mountain candidate may be relocated; teleport to the logged **shifted** construction X/Z, not the original marker.
8. Inspect core proportions, gates, homes, workplaces, road access and farm placement. **Returning to an already-generated chunk does not add structures.**

To try a different site, run selection again, rebuild and use an untouched test save. Multiple sites may be selected via direct CLI (up to 3):

```powershell
py -3 tools/geodata/c4_10b_preview_compiler.py --list
py -3 tools/geodata/c4_10b_preview_compiler.py --site site_ID --site another_ID
py -3 tools/geodata/c4_10b_preview_compiler.py --site site_ID --activate
```

To turn off future C4.10B previews, run `./RUN_C4_10B_DISABLE_PREVIEW.bat` and rebuild. This deletes the opt-in manifest; installed code remains dormant if no manifest exists. **It does not undo blocks already present in a save**. Keep test worlds disposable.

## Protection and workflow notes

- Winterfell `site_nw_029`, Ramsgate `site_nw_031`, Acorn Hall `site_sw_023` and any sites in the currently installed C2 pilot manifest are rejected. Geometric overlap with existing pilot ranges and the protected Winterfell estate is also checked, with a generous margin for preview anchor relocation.
- Do not run old C2 or C4.5 installers again or overwrite authored Winterfell files. This batch leaves the old systems untouched apart from the narrow generator hook.
- If your local `KnownWorldChunkGenerator.java` differs, installer stops with the exact missing anchor. **Do not brute-force apply or overwrite it with an old version.** Provide the full exception and the affected file.
- `C4.10B_PREVIEW_AUDIT.json` is a planning/installation report. Actual runtime-approved sites and rejected lots are reported in the Minecraft log; neither equals a complete worldwide build census.
- The source Python tests and Java source are included; Java structural compilation was smoke-checked with lightweight Minecraft/Gson interface stubs. Full Fabric Gradle and in-game rendering **not yet run**.

## File list

- `tools/geodata/c4_10b_preview_compiler.py`: strict per-site complete manifest selection, C1 palettes and demand audit; writes resource only with `--activate`.
- `tools/geodata/c4_10b_hook_install.py`: exact, idempotent generator hook insertion with backup.
- `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC410BPreviewWorldgen.java`: runtime geo policy, chunk-indexed streets/C3 lots/fortifications, site/lot failure diagnostics.
- `src/test/python/test_c4_10b_preview.py`: 11 hook/manifest/identity/scale/activation tests.
- `RUN_C4_10B_INSTALL_HOOKS.bat`, `RUN_C4_10B_SELECT_SITE_PREVIEW.bat`, `RUN_C4_10B_DISABLE_PREVIEW.bat`.
- `docs/architecture/24_15_C4_10B_PHYSICAL_INTEGRATION.md` and the checkpoint.

## The next batch

C4.10C / C4.11: improve actual terrace and switchback construction; systematic runtime terrain/failure audit across settlement classes; safe streaming generation of cities above the temporary preview budget; special coast/port modes; then controlled worldwide activation. NPC people and jobs remain a separate B4/D integration step, using the same target ledgers without inventing headcount.
