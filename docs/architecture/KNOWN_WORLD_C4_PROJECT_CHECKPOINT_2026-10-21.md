# Known World — C4 Project Checkpoint

**Updated:** 2026-10-21  
**Scenario:** ASOIAF / AGOT, 298 AC  
**Repository:** `dtzudontsay/Minecraft_Server_Mod2`, branch `master`  
**Status:** C4.2 committed and locally built; C4.3 not yet established as complete in the repository  
**Repository ownership rule:** Assistant reads the repository and supplies changes as files or patches; user alone installs, commits, and pushes them.

## 1. Non-negotiable vision

A persistent, autonomous simulation of the Known World, represented physically in Minecraft at approximately 1 block = 1 metre. Geographic locations and political holdings must resolve to stable settlement identities. Reusable procedural generation must produce settlements consistent with local geography, culture, religion, population, labour and economic roles. Households, real NPCs, workplaces, and population counts must reconcile; later simulation drives actual building demand, growth, construction and demolition. Iconic structures and special locales remain custom-authored or exceptional. Winterfell must **use and benefit from the universal systems**, rather than being a permanently isolated prototype.

## 2. Three current physical test cases

- **Winterfell (`NW-029`, X=-4,050,173, Z=-1,976,496):** separate V3 new-chunk generator and persistent 5,000-person case-study data. Locked V3 planning: 1,136 residential and 378 workplace footprints (1,514 total), one castle, a winter town, four villages and 15 hamlets. Existing footprint spans much farther than new compact preset defaults. Preserve existing building positions, assigned NPC identities, household identities, and worlds unless an explicit migration is approved.
- **Ramsgate (`NW-031`, X=-3,454,784, Z=-1,589,008):** SMALL generic pilot. C4.1 planning summary: 74 buildings, 48 homes, 62 homes of housing-demand backlog; terrain approval is evaluated during actual decoration.
- **Acorn Hall (`SW-023`, X=-4,115,723, Z=-284,673):** MEDIUM generic pilot. C4.1 summary: 241 buildings, 180 homes, 39 homes of housing-demand backlog; terrain approval is evaluated during actual decoration.

## 3. C-batch implementation state

- **B2/B3:** 253 active map observations, not equivalent to 253 settlements or 259 political holdings. Geographic anchors are candidates with varying confidence; direct build-footprint approval is not globally established.
- **C1:** `settlement_presets_c1_v1.json` and offline `KnownWorld_C1_Preset_Explorer.html` exist. The generated summary enumerates 248 deduplicated physical sites linked to 259 holdings. Physical tier counts: 41 SMALL, 95 MEDIUM, 47 MAJOR, 65 SPECIAL. These are **planning presets**, including estimated people, household/building and job demand, not populations automatically instantiated in-game. The summary's `safelyConstructedNow: 0` refers to C1 data generation, not the later C2/C4 pilot worldgen.
- **C2:** chunk-local new-world physical generation, constrained to two pilots. It checks land/slope and refuses unsafe plots; it never edits already generated chunks.
- **C3:** 43 executable architectural template identifiers and a physical renderer for approved lots. The templates do not, by themselves, activate sites, link real households, staff workstations or implement dynamic construction.
- **C4.1:** designed compact connected cores with roads, farm annexes and housing/workplace lots for Ramsgate and Acorn Hall. Plan connectivity does not guarantee actual 3D road connectivity after dangerous lots are skipped.
- **C4.2:** terrain grading and entrance approach adjustments for the same two pilots. Local `C4_2_BUILD_LOG.txt` reports five passing source/patch tests and `BUILD SUCCESSFUL` on 2026-10-21; actual in-game quality remains a visual test.
- **C4.3:** **NOT COMPLETE / NO CURRENT C4.3 MANIFEST OR IMPLEMENTATION DOCUMENT FOUND** in the inspected `master` tree. Do not claim it is installed just because C4.2 builds.
- **Winterfell integration:** **NOT COMPLETE**. `WinterfellWorldgenV3` and `KnownWorldUniversalWorldgenC2` are separate classes. The C2 loader explicitly requires exactly two SMALL/MEDIUM pilots, and rejects `NW-029`.
- **Dynamic growth:** **NOT COMPLETE**. C1 defines proposed expansion actions and stable lot/phase concepts. Persistent live building construction, ownership decisions, resource/labour consumption and safe already-generated-chunk modifications require a dedicated authoritative subsystem (described as C6 in newer C1/C3 docs; older planning roadmap labels persistence and live construction C5).
- **Actual population ownership:** B4/D household/jobs/economy integration remains pending. Do not claim planning estimates are fully persisted residents.

## 4. Key compatibility risks

1. Do not double-generate Winterfell or duplicate its 5,000 persistent individuals and 1,136 households.
2. Do not confuse marker, physical site, settlement, landed holding, structure or dynasty.
3. Do not spawn housing simply by dividing speculative population by an arbitrary household size; respect authored rosters, capacity, density tiers and explicit backlog.
4. Shore, mountains, cliffs, canals, ports, ruins, special monuments and nested castle sites require terrain/structure policies beyond generic SMALL/MEDIUM templates.
5. Existing generated chunks must not be overwritten by a new worldgen build; live changes require a persisted executor and conflict protection.
6. Generator indexes, bounds and per-chunk workload will expand enormously with global activation; budget and index by spatial region to avoid scanning every site per chunk.
7. New C4 code must connect to existing C2, C3, B2/B3 and Winterfell owners; do not create parallel registries.
8. Distinguish source-supported facts, inference, mod-estimated data and user-authored decisions.

## 5. Proposed continuation of physical road map (confirm against new work before renumbering)

**C4.3 — Finish universal construction prerequisites:** fix unsafe 3D routing, lot/entrance connectivity, stairs and hill paths; stable site/phase/lot identity; performance and deterministic validation; preserve two pilot tests. The exact C4.3 scope was not committed as an authoritative roadmap in the inspected tree, so this is a recommended scope, not a claim about an earlier unseen chat message.

**Winterfell bridge:** create a shared construction interface or adapter so Winterfell's existing V3 plan is retained while universal C3/C4 templates and future growth can be used for appropriate buildings. Preserve bespoke Stark castle pieces and all existing residency/placement IDs. Provide an opt-in fresh-test-world mode and a separately designed existing-world migration path.

**Bulk SMALL rollout:** Build a *read-only preflight report* for all SMALL tier presets, flagging which are eligible for ordinary generic generation, with coordinates, occupancy/capacity, architectural palette, cultural/religious building policies, terrain status, confidence and reason for exclusion. Automatically enable all passing ordinary villages in a fresh test world. Publish an exception list; do not demand a manual approval for every uncomplicated site.

**MEDIUM rollout:** Apply the same process to ordinary MEDIUM sites after visual review of a small representative cross-section and performance benchmarking.

**MAJOR and SPECIAL:** Major urban districts can share ordinary residential/workplace modules, but special architecture (Winterfell, White Harbor, King's Landing, Braavos, Five Forts, ruins, ports, etc.) requires authored anchors/overrides. Do not blindly scale a generic village.

**C5/C6 and B4/D:** The newer C1/C3 documents use C5 for global physical activation and C6 for persistent growth, while the older connected-world plan uses C5 for persistence/live construction. Reconcile numbering explicitly in the next authoritative roadmap edit. Real building growth is driven by authoritative household and economic state, not solely a population-estimate field.

## 6. Validation gates before mass worldgen

- Source tree and references audited; version-pinned deterministic manifests and exact site IDs.
- Existing Winterfell placement and NPC persistence untouched unless migration explicitly opted in.
- Whole-lot and adjacent-chunk water, slope, hazard, road-connectivity and doorway safety checks.
- Minimum in-game samples: flat, mountainous, forest, river/coast, dense, and low-capacity settlements.
- Safe reloading: no duplication or unintentional overwrite of player blocks.
- Verified build, tests and server/client load; check chunk-generation performance and manifest sizes.
- Unresolved coordinates and prohibited sites appear in a visible exclusion report instead of silently spawning incorrectly.

## 7. Repository reference pointers

- [`docs/architecture/24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md)
- [`docs/architecture/24_3_C4_CONNECTED_SETTLEMENT_LAYOUTS.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/24_3_C4_CONNECTED_SETTLEMENT_LAYOUTS.md)
- [`docs/architecture/24_4_C4_2_TERRAIN_STREETS.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/24_4_C4_2_TERRAIN_STREETS.md)
- [`docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md)
- [`docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md)
- [`tools/geodata/output/c1_settlement_presets/C1_SUMMARY.json`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/tools/geodata/output/c1_settlement_presets/C1_SUMMARY.json)
- [`tools/geodata/output/c4_settlements/C4_LAYOUT_SUMMARY.json`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/tools/geodata/output/c4_settlements/C4_LAYOUT_SUMMARY.json)
- [`src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldUniversalWorldgenC2.java`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldUniversalWorldgenC2.java)
- [`src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/WinterfellWorldgenV3.java`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/WinterfellWorldgenV3.java)

## 8. Chat continuity procedure

At the start of each new conversation, read **this checkpoint** and the latest commit tree and inspect the relevant implementations; do not assume the entire older conversation transcript is accessible. At the end of each development batch, provide a complete project-relative file manifest, installation/validation commands, test evidence, new limitations, and an updated checkpoint for the user to push. Keep GitHub read-only from the assistant's perspective; never directly commit or mutate repository data.
