# C4.5 — Occupation-connected NPC and new-world construction stress rollout

**Repository:** `dtzudontsay/Minecraft_Server_Mod2`, Minecraft **26.3**.  Read-only GitHub from assistant; apply these files locally and push them yourself.  This ZIP is NOT a replacement for `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md` or `23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`.

## Run (Windows)

1. Make a backup of your existing Minecraft saves and your working Git branch.
2. Extract this ZIP directly into the repository root so `RUN_C4_5_CONNECTED_NEW_WORLD.bat` sits beside `gradlew.bat`.
3. Run `RUN_C4_5_CONNECTED_NEW_WORLD.bat`. Select **PILOTS** (Ramsgate + Acorn Hall + Winterfell) or **WORLD** (all C1 `SMALL`/`MEDIUM` **normal compact settlement** sites plus Winterfell). Confirm `NEW_WORLD`.
4. The installer runs Python tests, guarded Java source-preflight, generates job-connected building plans, revises **only generated adult** occupations in Winterfell's V2 roster, recompiles the V3 worldgen index, adds new job-specific physical workplaces to V3, applies exterior building signs, and runs `gradlew.bat build`.
5. **Only when `BUILD SUCCESSFUL`** run `gradlew.bat runClient` and create a brand-new Known World save. No additional `clean` required.
6. Inspect `/kwinterfell3 status`, `/kwinterfell3npc status` and C2 inspection commands (if available) along with the installed reports under `tools/geodata/output/c4_5_connected_rollout/`.

**Ramsgate:** `/tp @s -3454784 180 -1589008`   **Acorn Hall:** `/tp @s -4115723 180 -284673`   **Winterfell:** `/tp @s -4050173 180 -1976496`. Use `/gamemode spectator` if ground height is different.

## Exactly what this batch implements

- Installs the prior C4.4 occupation catalogue and all **248** site-level occupation distributions. Unique worker target counts are conserved in C4.4; the 50 uninhabited/ruined sites still have **no synthetic permanent workforce**.
- C4's **normal SMALL and MEDIUM** settlements derive **all planned housing buildings**, eligible workplaces and rural farm buildings from C1 housing demand + C4.4 `buildingDemand` (not from earlier 48-home/180-home test limits). **WORLD** activates all eligible normal SMALL/MEDIUM sites without requiring site-by-site editing. Regions, exact anchors and physical-footprint reports remain associated with each site.
- The new generic layouts distribute workshops, civic buildings, farms and houses across multiple connected streets rather than segregating all specialized buildings to one end. Major special, shore-bound, policy-gated, unverified and mountain-only structures remain explicit `deferredDemand` records rather than spawning inland or on ruins.
- Every eligible C3 workplace module gets a physical exterior sign or outdoor signpost with its **type name** (e.g. `SMITHY`, `BAKERY`, `BOOKBINDERY`) using the Minecraft 26.3 sign API. Outdoor facilities such as wells, gardens and pastures receive signposts; field annexes do not receive separate occupation signs. These are NOT household surname signs, which still require actual occupant binding.
- **Winterfell** preserves its 5000 person IDs and 1136 households. Only adult, mod-generated residents are reclassified among C4.4 occupations, weighted according to C4.4 and prioritized towards rural versus central work. Existing named and canon-authored person jobs are unchanged. Profiles of **newly imported** generated NPCs contain their new occupation. The separate V3 blueprint retains its original **1514** homes/workplaces and appends **additional C3 specialized work buildings** distributed around existing villages and winter town. Castle interior posts are deliberately reserved as instance-relative future workrooms and not duplicated outside the walls.
- **Fresh world only**: direct chunk-local decoration; no existing chunks or player construction is edited. Homes and NPCs are *not* silently invented at unpopulated C1 settlements.
- `--npc-visual-stress` raises Winterfell's **nearby projection budget** from 128 to 384, without creating extra persistent people or requiring 5000 simultaneously loaded Minecraft villagers. If performance degrades, restore this source file from its backup and build again.

## Important unfinished systems

**Do new residents automatically acquire jobs at all 248 locations? Not yet.** The current `NpcRegistry.create` and `NpcAffiliationManager.setResidence` do not consult site labor demand. The generic SMALL/MEDIUM sites have **planned populations**, but not bootstrapped unique persistent per-person rosters. The V3 Winterfell path is the *only* full NPC import connected in this installer. Automatic future migration/birth-to-adulthood employment assignment, per-building staffing, wage/productivity, persistent job changes, and actual individual simulation of the other 197 inhabited sites require a shared B4/D settlement employment authority and live saves; that authority is **not simulated by static JSON**.

Also pending: `MAJOR` and `SPECIAL` archetype universality; castle-room relative coordinates, full wall gates and occupation-aware zoning; safe shoreline/dock/mine templates; rural land ledger to visual field-acreage reconciliation; true long-term live construction. Terrain checks may reject certain lots, meaning planned totals can exceed successfully built structures. The `WORLD` preview can be very large—its performance was not stress-tested against real Minecraft 26.3; prefer `PILOTS` first before opting into worldwide normal-site generation.

## No arbitrary housing quota, but simulation and projection have different scales

All `SMALL`/`MEDIUM` C1 housing **demand** is now emitted rather than silently capped at the old first-phase numbers. This is only a geometric blueprint; it does **not** create resident NPCs, and does not guarantee all blocks survive terrain surveys. Fields are **64×64 blocks per plot**, six per farmhouse in this worldwide prototype. The visible annex is **24,576 m² per farmhouse**, slightly larger than C4.3’s 24 × 31×31 belt (23,064 m²); it is *not* a canonical productive acreage measurement. This significantly increases fresh-chunk generation costs. This is an intentionally larger physical field annex, not an authoritative farm-size model; economic acreage, yields, regional climate and infrastructure must still be reconciled.

Physical villagers remain bounded projections because 3.76 million simultaneously loaded Minecraft entities is not a viable representation of 3.76 million simulation identities. The long-term solution is off-screen identity/household simulation + nearby projection, not lifting Minecraft entity budgets blindly.

## Rollback

Modified files are backed up alongside the source under names ending `*.before_c4_5.bak`, retaining the original bytes from before the *first* C4.5 install. You can restore them manually, or use `git status` and `git restore` for files you are certain you want reverted. **Do not restore over unrelated edits made after installing**. Your world saves are untouched. If Java compilation fails, do not launch from the modified source tree until corrected/reverted. Do not rerun `build_winterfell_v2.py` after installing without rerunning C4.5: doing so would replace the generated diverse occupations with its legacy 13-job prior.

## Testing done by assistant

- Python `py_compile` for the batch scripts.
- Data conservation for the 248 site-level C4.4 job plans.
- Determinism, zero overlaps and complete C1 housing demand in **synthetic** SMALL (Ramsgate 550) and MEDIUM (Acorn Hall 2100) layout fixtures.
- Synthetic Winterfell 5000-person generated adult reclassification, preserving named profiles.
- **Not** compiled against Minecraft 26.3 Gradle, nor run inside a real game world, nor independently verified for all SMALL/MEDIUM terrains. Installer must run Gradle locally before game testing.
