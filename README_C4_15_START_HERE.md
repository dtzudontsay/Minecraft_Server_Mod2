# C4.15 — Worldwide settlement architecture, organic streets, commute planning

**Install after:** C4.11 global rollout, B4.0 full population, B4.1A and B4.1A.1. Keep C4.13 diagnostics and the teleport repair. **Do not reinstall the old C4.11 or B4.1A.1 ZIP after C4.15**, since those packages would overwrite the new source/planner.

## Safety first

- The ZIP contains `C4_15_PAYLOAD/` and **does not overwrite Java/Python source when extracted**. Place the ZIP at `D:\GameOfThronesMod2`, extract there, run `RUN_C4_15_REBUILD_TOWNS_AND_COMMUTES.bat` from that root.
- The bootstrap `tools/geodata/c4_15_stage_apply.py` backs up every replaced source in `tools/geodata/output/c4_15_safety_backups/source_before_<date>/` **before** changing it. C3 home palette patch is also backed up there.
- No save/world is modified by BAT. No NPC ID is changed, no B4.0 compressed baseline record is rewritten, no additional residents are created, no Winterfell V3 or C2 pilot site is reconstructed. Those three are protected.
- **You must create a new test world** for consistent physical settlement appearance. The generator cannot retrofit already-generated chunks. Never test by mixing old and new chunks in an important save.
- Full Fabric Gradle compilation and in-game rendering **must be validated locally**. Python regression and Java stub-interface tests do NOT prove roofs/streets look good in Minecraft.

## What changed

1. `KnownWorldC410Fortifications.java`: three distinct modular cores: timber fortified hall and turrets; stone minor keep with wings and round towers; much larger multi-building great keep with an arched gatehouse, roofed interior, side wings, floors and prominent towers. Stone gates gain rounded openings and towers/battlements. Non-custom cities get a generic tier archetype, not a true authored model of every canonical castle.
2. `KnownWorldC3Architecture.java`: exact guarded installer patch replaces ordinary urban-home fired brick walls with stone brick (Dorne sandstone), uses timber/wood for some regular homes, reserves brick for designated specialist construction.
3. `c4_10_shared_layout.py`: fixed large 56-block lots replaced by a **31-block frontage cell**, small house footprint variations, 2×2 or larger reserved cells around sizeable special workplaces, sparse green pockets, preserved sequential home/employee lot IDs and household reservation IDs. Geometry remains axis-aligned per building; the street network and setbacks gain small nonuniform warps.
4. `c4_11_global_rollout.py` and `KnownWorldC411GlobalWorldgen.java`: regenerated global 512×512 resource tiles; nearby building street perimeters are joined by shared warped nodes and short road spurs from the south entrance. A few reserved green cells support small real trees. Actual terrain/slope/coast errors can still interrupt painted routes, so no guaranteed fully walkable routes at every failed site.
5. `KnownWorldB41Activation.java`: NPCs look for clear ground **in front of, not on, their homes**; the accepted feet height is restricted to a band around the sampled *pristine terrain* to rule out house roofs. If there is no reliable ground, do not spawn an avatar there. This does not delete that person.
6. `c4_15_commute_assignment.py`: new offline assignment overlay, resolving each recorded employed resident's job/building kind to a **nearby compatible planned workplace** with free reserved staff slots. It conserves all existing occupations and person IDs. By default maximum *Manhattan distance proxy* is **600 blocks** (a planning approximation for ten minutes; this is **NOT** in-game navigation time). Workers without capacity or a nearby suitable building retain their job and get an explicit unverified flag rather than imaginary employment. The per-site `audit.json` reports the unmatched counts and reasons. Castle interior workers remain a separate exceptional core type.
7. `b4_1a1_household_index.py`: reads commute overlay during reindexing, preserving original B4.0 baseline and names and using matched workplace IDs for the nearby actor view.

## Local steps

```powershell
cd D:\GameOfThronesMod2
.\RUN_C4_15_REBUILD_TOWNS_AND_COMMUTES.bat
.\gradlew.bat clean build
```

Then run your normal `runClient` and **create a fresh Known World test world**. Teleport to new Small, Medium and Major settlements by exact markers, inspect their central buildings, use `/kwactors status`, and test whether arrivals are on roads/land rather than roofs. Use `/kwpopulation status` to confirm exactly 3,760,684 people, 799,343 households and 1,880,341 jobs.

Commute exceptions and totals:

`src/main/resources/data/knownworld/scenarios/agot_298_ac/world_commutes_c4_15_v1/audit.json`

C4.11 terrain/lot failures:

`run/logs/latest.log` search `C4.15 LOT FAILURE` and `C4.11 SITE FAILURE`.

## Explicit remaining limits

- A programmatically built great keep is not a hand-authored canonical individual model. The castle is larger and richer in form, but new Minecraft visuals need inspection and iterative architectural refinements.
- All generic home doors still point south. Roads wind mildly and plots differ in size; full medieval irregular street routing, rotated architecture and custom district road graphs are later enhancements.
- **Ten-minute commute is NOT guaranteed.** The sidecar measures straight-axis plan distance, not actual route walk time; no pathfinding on final block walkability is performed. Unmatched workers stay employed in the B4.0 model but are tagged as unverified assignments rather than silently moved or discarded. Farms/forestry far from existing houses can exceed the 600-block target. Their placement needs a follow-up optimization of households/field neighborhoods.
- Building render approvals remain terrain-dependent, and C4.11 cannot guarantee all 347k+ planned buildings appear.
- Daily work, productive jobs, offscreen schedules, travel, LLM chat outside Winterfell are not added by this construction batch.
- Some roofs/decorations might still have elevation/clearance issues on mountains; inspect locally and share screenshot+log.
