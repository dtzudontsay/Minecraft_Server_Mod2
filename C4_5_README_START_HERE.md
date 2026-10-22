# C4.5.1 — installation recovery, Winterfell ward reallocation, signed workplaces

This ZIP **supersedes** the previously issued C4.5 ZIP. Use it as one complete package.
Unzip into the root of `Minecraft_Server_Mod2` **next to `gradlew.bat`** and double-click **`RUN_C4_5_1_KEEP_OPEN.bat`**. It retains its terminal window and records every stage in `C4_5_1_INSTALL.log`. A failure will identify which numbered stage stopped and will not silently continue. 

Choose `PILOTS` first. It includes Winterfell's five-thousand-person existing case study, Ramsgate and Acorn Hall. When that complete build succeeds and the new Minecraft world works, repeat with `WORLD` for standard SMALL and MEDIUM sites. WORLD generation, disk size and Minecraft chunk-generation RAM usage have **not** been performance-tested at global scale; do not begin with WORLD unless you accept that stress-test risk.

## What this installs

- Fixes the old `KnownWorldC3ArchitectureTest` that incorrectly asserts exactly 43 architecture modules even though C4.3 legitimately supplies 61. All original C1 template IDs must still exist, and the generator separately requires at least 61 templates.
- Changes `WORLD` planning to write site JSON progressively, instead of keeping every settlement's geometry in memory. Removes the old all-building-pixel collision bitmap to reduce farmland planner memory. It still requires substantial RAM in Minecraft at large sites.
- Populates the original **5,000-person Winterfell roster** with C4.4 planned primary occupations for eligible newly generated adults. Named/authored characters and their existing authored occupations are preserved. 2,500 worldwide planning workers is NOT identical to the eligible resident count in the source roster; the import reports any mismatch.
- Preserves all original 1,136 Winterfell household IDs. Moves **24 castle-ward residences** immediately outside the western wall (roughly 300–420 blocks from castle center); puts **12 appropriately sized C3 specialty workplaces** in the vacated paired footprints inside the castle. It preserves existing curtain-wall, gates, towers and central keep geometry, and extends accessible road plans via the existing west gate.
- Adds other C4.4-demanded specialized Winterfell workplaces among the existing winter town, villages and hamlets where geometric and terrain policies permit. The unplaced or policy-blocked demands remain visible in the generation report; they are not silently counted as built.
- Generates SMALL/MEDIUM standard settlement physical building programs from C4.4; preserves Winterfell as a separate major-seat V3 blueprint. New storefronts/workplaces display exterior name signs. No made-up family surnames are placed on residential signs.
- Preserves the existing Winterfell physical-NPC projection budget safety model (C4.5 optionally raises the visible budget from 128 to 384/256); **does not spawn 5,000 simultaneous villagers** or pretend that all 198 inhabited sites already have individual people.

## Not yet implemented / constraints

- This does **not** reauthor every Winterfell outer hamlet/village into a 500-block radius. It relocates a specific castle-ward group close to the castle while retaining the preexisting 1,136-home layout elsewhere. The original V2 source already places some satellite villages and hamlets thousands of blocks away. A wider *outer districts compaction* is a separate geography / road / household migration and terrain-safety pass, not part of this repair.
- This does **not** implement actual inside-keep room navigation, job reservations or household-based moving of beds. The new workplaces occupy real vacated **building footprints**. Court offices and maester's quarters that must be inside the main keep remain logical workplace requirements for later room authorities.
- New people created by generic `NpcRegistry.create` do not yet read settlement distributions. Future employment must draw from CURRENT labor demands, age, skills and vacancies, update authoritative simulation/persistence, and avoid resetting existing occupations. The distributed-data component alone cannot do that.
- Existing chunks are never rewritten. A **new Minecraft world** is required. Keep the prior saves as backups. The `WORLD` path excludes ruins, monuments, coastline-specific sites, large cities and the MAJOR generic archetype. It does NOT make every marker in the 253-observation atlas into a town.
- The script cannot compile Minecraft 26.3 here. The full `gradlew.bat build` runs locally and must report success before gameplay testing. Sign code uses 26.3 `SignTextSlot` and mutable sign-text API; if a mapping-dependent Java error occurs, the line-level Gradle log will show it, and the installer will stop.

## Verify success

After a successful run, the log must include a `VERIFIED:` line with `5000 Winterfell residents`, `1136 homes`, `24 relocated castle homes`, more than 1,526 Winterfell plots, and a nonzero count of C4.5 job assignments. The last stage must report `BUILD SUCCESSFUL`.

You may rerun the read-only verification at any time:

```
py -3 tools\geodata\c4_5_1_verify.py
```

In Minecraft, verify `/kwinterfell3 status` lists more than 1,526 planned buildings. Look at the castle-ward rows near the west gate and the nearby new residential streets, as well as the existing town's specialized job buildings. The physical worldgen is chunk-local; visually inspect new terrain in a **new** save.

Do **not** run `tools\geodata\build_winterfell_v2.py` or `build_winterfell_v3_worldgen.py` manually after installation: rerunning these baseline compilers independently can erase the intended C4.5.1 layout overlay until the installer sequence is repeated.
