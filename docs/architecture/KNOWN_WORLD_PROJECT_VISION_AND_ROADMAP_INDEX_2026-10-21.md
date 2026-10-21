# Known World — Project Vision & Roadmap Index

**Snapshot:** 2026-10-21  
**Repository:** `dtzudontsay/Minecraft_Server_Mod2` (GitHub is **read-only** to the assistant; user applies all files and commits)  
**Status of this file:** Context/handoff index **only**, not a replacement roadmap or claim that every old chat is retrievable.

## A. Final project vision — preserve unchanged

Build a persistent, autonomous simulation of the **A Song of Ice and Fire Known World** presented physically in Minecraft, initially set in **298 AC**, around Robert Baratheon's visit to Winterfell. Reconstruct known geography at roughly **one horizontal Minecraft block per metre** (current established map-scale intent), including Westeros, Essos, the Summer Isles and supported Sothoryos/Ulthos locations. The environment includes handcrafted exceptions for iconic sites and generic data-driven construction for ordinary settlements.

The essential product is **not** a world map, static villages, a dialogue chatbot or a Crusader Kings clone. It combines CK-like dynastic/political simulation with deeper person-level everyday life and a persistent Minecraft world. Named and ordinary NPCs are individuals with identities, households, traits, beliefs, memories, cultures, religions, jobs, resources and histories. People grow up, form relationships, marry, have children, migrate, work, trade, fight, fall ill and die. Simulated households, markets, farms, roads, governments, institutions, factions, diplomacy, armies and wars affect one another. The player can live in, converse with, influence and alter that history. 298 AC is the *starting state*, not a forced replay of the story. The world progresses without player attention or loaded chunks.

**Design invariants:** authoritative managers for each mutable domain; one person counted once; actual simulated NPC identity distinct from physical Minecraft body; deterministic/persisted local and offscreen progression; normal time and fast-forward obey same rules; LLMs generate speech/intent but never grant items, change titles or invent events without validated server actions; clear **CANON / SOURCE-INFERRED / MOD_ESTIMATE / USER_AUTHORED** provenance; safe old-world migration; scalable regional processing and bounded physical/LLM activity.

## B. The roadmaps already agreed upon (all remain valid)

### 1. Broad feature horizon — `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`

- **Phase 0:** existing frameworks (not proof of integration): identity, profiles, needs, families, titles, histories, geography, food/economic foundations.
- **Phase 1:** agriculture + population + labor; establish one autonomous multi-year settlement with conserved resources and no phantom residents.
- **Phase 2:** everyday life/demography: education, occupations, formation, migration, disease, childbirth, marriage and deaths.
- **Phase 3:** economic networks: production, property, prices, wages, trade, taxation, finance, logistics, climate impacts.
- **Phase 4:** dynasty/politics/law: inheritance, office, laws across cultures, plots, legitimacy, diplomacy, rebellion and justice.
- **Phase 5:** military/war: recruitment, training, logistics, battles, sieges, occupation, casualties and peace.
- **Phase 6:** geography/physical settlements/mobility: map correction, terrain, rivers/roads, specialized sites, navigation, rendered itineraries.
- **Phase 7:** player participation & conversations: LLM grounded in truth/memory/knowledge, reputations, work, ownership, player factions and consequences, news transmission.
- **Phase 8:** presentation/ASOIAF immersion: map/fast travel, chronicle/logbook, notification sounds, distinctive appearances, irregular seasons, magic, dragons, Others.
- **Phase 9:** world coverage, balancing, performance and robust long-running emergent history.
- Research, canon character authoring, holdings, cultures, institutions, origins, exact geo and uncertainty handling are *continuous*, not a one-off step.

### 2. Earlier integration plan — `docs/architecture/23_0_CONNECTED_SIMULATION_ROADMAP.md`

This is a **different letter namespace** from the current construction sequence:
- **A:** factual NPC context, witnessed events, persistent player-specific conversation memory, rumor/disclosure rules.
- **B:** canonical place/holding/settlement/resident links, migration, population accounting.
- **C:** **autonomous settlement simulation**, including time, work/consumption, demographic/institutional changes, and a C4 **simulation acceptance campaign** of Winterfell across many simulated years.
- **D:** settlement architecture and generalization: Winterfell extraction, smaller archetypes, later regional deployment.
- **E:** NPC decisions, social/institutional effects, LLM intents vetted by game rules.
- **F:** ongoing historical data, canon characters and reference integration.
- **G:** scaling, test gates and save compatibility.

**Do not confuse this older C4 (multi-year simulation acceptance) with physical-construction C4.3.**

### 3. Expanded implementation plan — `docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`

The current most explicit cross-domain breakdown, with dependent gates:
- **A1–A5:** reliable NPC knowledge, observer memories/rumors, persistent conversation, player-specific relationships and validated social consequences.
- **B1–B5:** unified time, geography/holding ownership, construction size classification, globally coherent **individual population authority**, named 298 AC people. Cohort data is an efficiency view, never additional phantom residents.
- **C1–C5:** reusable physical construction, size archetypes, terrain/materials, functional buildings/property, persistence and **live construction**. Winterfell is the first integration environment, with authored landmark exceptions.
- **D1–D4:** genuine daily life, jobs, agriculture/food, household economy, family lifecycle, social agency and NPC responses to events.
- **E1–E3:** real travel, trade and information spread with non-instant travel/communication times and materialization near players.
- **F1–F3:** autonomous institutions, offices, government finances, laws, title succession, regencies, claims, intrigue, factions and criminal justice.
- **G1–G3:** recruitment, armies, war/sieges/occupation, faction construction of camps, forts and holdings using resources, labor and political authority.
- **H1–H2:** world-generated contracts/quests and server-validated LLM-to-action bridges.
- **I1–I3:** Northern prototype, regional expansion, then functioning global 298 AC rollout; simulation never depends on loaded chunks.
- **J:** research, named characters, historic lore, place positions, traditions, provenance, ongoing data validation.
- **K:** compile, tests, performance, save migration, deterministic replay and full file delivery after each change.

**Major dependency gates:** A knowledge/identity -> B time/geography/population -> C physical construction -> D local autonomous life -> E regional travel/trade/news -> F political agency -> G warfare and building -> H player agency -> I global integrated simulation. J/K are continuous. These are integration gates, not a prohibition on independent parallel research or physical-world prototypes.

### 4. Construction implementation mini-roadmap — `docs/architecture/24_*.md`, current chat

The recent C1–C4.3 numbering refers to *specific physical-construction batches*, not exactly to any A/B/C letter in older plans:
- **C1** planning presets across 248 deduplicated physical sites (253 observations; 259 political holdings; 41 SMALL / 95 MEDIUM / 47 MAJOR / 65 SPECIAL). Planning data, not a finished settlement or actual census.
- **C2** safe chunk-local physical generator, limited to 2 pilots (Ramsgate, Acorn Hall).
- **C3** 43 architectural renderers for authorized lots, not a populated/staffed society.
- **C4.1** compact roads/lots/districts/farmland for both pilots.
- **C4.2** bounded slope/door grading; successful local build log in repository, still limited on extreme terrain.
- **C4.3** current pending work in chat; **do not mark complete**. Full details must be checked against actual files / user's latest local test, rather than guessed from the number.
- **Winterfell V3** remains separately implemented with a 5,000-person planning census, castle, town/villages, and runtime projection. Universal construction rules must eventually be shared without destroying saved Winterfell structures, households or identities.
- **C5/C6 numbering collision:** older physical roadmap calls C5 live construction while the later C1/C3 pilot document calls C5 broad physical rollout and C6 persistent phased construction. Reconcile names before implementing new stages; preserve *both* features. No fake claim that dynamic growth is already implemented.

**User's stated proposed near-term direction:** Finish C4.3 and inspect; integrate general-purpose buildings and future growth with Winterfell; then generate ordinary eligible small-village presets en masse using already-researched size/population/household data, rather than manually choosing two new sample sites every time. Automated coordinate/land/slope/footprint validation, controlled exclusions and backlog reports are necessary. Expand generic medium/major settlement types without replacing distinctive authored castles/cities. Full person-level simulation activation requires B4 identity reconciliation, staffing/households, live construction ledger and save-safe integration; initial physical block construction may proceed separately if accurately labeled.

## C. Current status / crucial limitations

- The active repository is `dtzudontsay/Minecraft_Server_Mod2`, **not** the early `GameOfThronesMod` starter repository.
- `KnownWorldUniversalWorldgenC2.java` explicitly checks `pilotCount==2`, only SMALL/MEDIUM mode, rejects Winterfell `NW-029`; mass rollout is NOT enabled.
- `WinterfellWorldgenV3.java` remains a separate physical builder and must be unified/migrated carefully; existing generated chunks are not changed merely by altering code.
- 248 C1 unique physical sites are planning profiles; 65 SPECIAL sites cannot be flattened into generic villages. The C1 summary reports **81 unplaced holding seats** and **0 safely constructed from global presets** at the time of generation. B2 geographic observations are provisional anchors; precise terrain approval remains distinct.
- `settlements.json` contains only the one bootstrapped Winterfell settlement in the 2026-10-16 audit; full population authority and productive WORK/operational agriculture, widespread trade/politics/war are not complete.
- The separate Winterfell **K-key** LLM prototype is documented; **right click** still inspects character sheet/GM details and **M** Atlas remains independent. Richer lore retrieval, private disclosure gates, persistent conversation and validated simulation actions remain future work. Status and tests must be verified before claiming release complete.
- `README_C4_2.txt`, `C4_2_BUILD_LOG.txt`, `tools/geodata/output/c4_settlements/C4_LAYOUT_SUMMARY.json` provide specific C4 physical progress and Ramsgate/Acorn Hall counts.

## D. Standing work rules / future-chat handoff

1. Read this index *and all three authoritative roadmap docs* at the beginning of substantial new work; do not shrink project scope to the latest implementation batch.
2. Inspect the complete GitHub **tree** for relevant packages, then read full relevant source, resources, installers and recent docs. Never falsely claim to have read every code file unless actually done.
3. GitHub **read only**. The assistant supplies full, exact relative paths and replacement/new files locally; the user alone installs, tests, commits and pushes.
4. After each completed batch, update the **existing** authoritative roadmap(s) and one current-status/handoff checkpoint; avoid competing improvised roadmaps. Do not rename an older roadmap stage silently.
5. Report: intended change, authoritative data owners, what's coded, tests actually executed, missing validations, invariants, install steps, regression/migration risks, and next agreed step.
6. Preserve the user-specified but **not-yet-discussed additional roadmap** as an open item. Never invent its contents.
7. Differentiate historic plans, agreed decisions, personal recommendations and untested proposals explicitly.

## E. Primary original sources (repository-relative)

- `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`
- `docs/architecture/23_0_CONNECTED_SIMULATION_ROADMAP.md`
- `docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`
- `docs/architecture/20_0_ARCHITECTURE_RULES.md`
- `docs/architecture/20_0_SYSTEM_INVENTORY.md`
- `docs/architecture/24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md`
- `docs/architecture/24_1_C2_PHYSICAL_WORLDGEN_PILOT.md`
- `docs/architecture/24_2_C3_ARCHITECTURAL_TEMPLATES.md`
- `docs/architecture/24_3_C4_CONNECTED_SETTLEMENT_LAYOUTS.md`
- `docs/architecture/24_4_C4_2_TERRAIN_STREETS.md`
- `docs/step3/WINTERFELL_LLM_DIALOGUE.md`
- `docs/WINTERFELL_V3_PRESERVE_LAYOUT_FINISHING_2026_10_19.md`
- `tools/geodata/output/c1_settlement_presets/C1_SUMMARY.json`
- `tools/geodata/output/c4_settlements/C4_LAYOUT_SUMMARY.json`

## F. Known uncertainties

- Full verbatim transcripts of all previous chat sessions are not reliably accessible from a new chat. Some user intentions may have been discussed but not persisted in repository documents. Do not claim perfect recollection.
- C4.3 is currently unfinished per user statement. Current repo artifacts reviewed through 2026-10-21, but this checkpoint does not certify unpushed files or screenshot-only changes.
- There is a further roadmap the user said they wanted to discuss later, but had not yet specified. Await user direction rather than pretend it is already known.
