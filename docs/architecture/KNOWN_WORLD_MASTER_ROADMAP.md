
# Known World — Master Feature Roadmap

Scenario: ASOIAF, 298 AC (Royal Arrival at Winterfell). Long-term target: an autonomous, persistent Known World that remains consistent when encountered physically in Minecraft.

## Rules for every future development batch

1. Inspect the **whole repository tree** and read every existing implementation relevant to the feature, including other packages. Change existing owners instead of silently adding parallel state.
2. For each feature identify its authoritative data owner, mutation path, dependencies, save/restore contract, background and fast-forward behavior, and player-observable effect.
3. Do not mistake a data schema, a reference entry, a test fixture, or a debug command for an autonomous gameplay feature.
4. Keep a single campaign calendar and consistent deterministic random streams. Preserve saves and migrations.
5. Distinguish **CANON / SOURCE-INFERRED / MOD_ESTIMATE / USER_AUTHORED**. Do not turn unknowns into apparently factual measurements.
6. Exact physical placement is optional for abstract simulation. Use stable holding/settlement identities and known regions; add precise world coordinates when authored.
7. Persistent individuals and anonymous population cohorts must never represent the same person twice.
8. Tests should be focused on genuine high-risk logic. Keep this document updated rather than creating a new architecture file for each batch.

## Phase 0 — Established foundations (incomplete integration)

Existing: NPC registry, rich profiles, needs, memory/knowledge, interpersonal and dynastic relationships, genealogy, marriage, pregnancy, birth, mortality, childhood formation, culture/religion influences, societies, organizations/membership, landed holdings, titles, claims, succession, historical events/chronicle, calendar, settlement/terrain/raster systems, economic inventories and journal/snapshot persistence, controlled agricultural production/distribution.

## Phase 1 — Current critical path: agriculture + population + labor

- 22.16: regional and holding-level farmable/cultivated capacity (estimated inputs, known regional ancestry, exact placement not required), produce compatible existing AgriculturalDayEnrollment objects.
- Next: demographic census/cohorts and reconciled persistent NPC population (age, sex, class, occupation, religion, culture, residency; no duplication).
- Next: employment and actual labor assignment, including farms; make WORK goals productive or connect abstract labor once without double-counting.
- Next: agricultural yield with real labor, land, climate and season. Establish harvest calendars/seed reserves/stockpiles.
- Next: household food consumption affects NPC hunger, health and demographic mortality; household inventory is authoritative.
- Next: integrate operations into ordinary and accelerated daily simulation with safe cross-domain save/restart/recovery.
- Acceptance: one settlement runs autonomously for many years with coherent supplies, living population, work, needs and no duplication; then scale to multiple settlements and regions.

## Phase 2 — Ordinary life, livelihoods and demographics

- Complete generated character profiles; regional/family traits, education, occupations, friends/enemies, memories, legal status and realistic randomized values.
- People are born, grow up, apprentice, marry, form households, work, acquire property, move, fall ill, die; opportunities and social class matter.
- Large ordinary populations represented efficiently in cohorts, with safe conversion to fully persistent NPCs on demand.
- Migration, refugees, disease, epidemics, medicine and social mobility.

## Phase 3 — Economic networks and institutions

- Production chains: grain, other foods, livestock, fishing, forestry, ore/mining, stone, tools, textiles, equipment, construction, luxury goods.
- Stocks, spoilage, food reserves, hunger, prices, wages, household budgets, scarcity, commercial businesses.
- Market exchange, merchant trade, roads, river routes, ports, shipping and transport costs.
- Property/land tenure, feudal rents, taxes, exemptions, administrative costs and tax evasion.
- Money, financial accounts, lending, collateral, insolvency, the Iron Bank and other institutions.
- Impacts of war, winter, weather and political policies on economic outcomes.

## Phase 4 — Dynastic, political and legal autonomy

- Robust inheritance of names, dynasty roles, titles, individual wealth, properties, debts and holding control.
- Realm-specific laws in Westeros, the Free Cities, Slaver's Bay, Dothraki, Free Folk and other societies; clearly marked provisional fallbacks.
- Autonomous rulers, officials, councils, vassal obligations, factions, courtship/marriage politics, diplomacy and treaties.
- Loyalty, claim disputes, plots, spies, assassinations, rebellions, hostage-taking and information conflicts.
- Crimes, laws, trials, prisons, punishments, pardons, legal differences and social legitimacy.

## Phase 5 — Military and war

- Manpower/recruitment by demographics and landowners; unit types, equipment, training, commanders, morale, tactics and discipline.
- Army movement and supply, raids, battles, sieges, occupation, surrender, casualties, prisoners and peace settlements.
- War changes labor, wealth, population, loyalties, territorial control, physical settlements and memories.
- Local physical armies/animations when near the player; identical ongoing battle state off-screen.

## Phase 6 — Physical geography, settlements and mobility

- Preserve master map, projection, raster coastlines, terrain and mountain data; check and improve accuracy.
- Decide map-scale policy **before mass structure generation**, and handle scale consistently across terrain, travel, rivers, roads and architecture.
- Author major city/castle/village placement gradually, not as a gate to political/economic simulation.
- Generate rivers/lakes, roads/paths, ports, bridges and specialized landmarks based on geographic layers.
- Generate ordinary culturally distinctive structures; hand-author iconic settlements and special landscapes.
- Persistent NPC/army itineraries and player-interceptable travel, with physical projections of the same simulation state.

## Phase 7 — Player-facing participation and conversation

- LLM NPC dialogue grounded in authoritative personality, beliefs, relationships, location and persistent memories.
- Dialogue proposes intentions; the world rules validate changes; the LLM never arbitrarily mutates economy, titles or history.
- Player reputation, property, jobs, guilds/orders, allegiances, faction politics, thievery, witnesses/crime and consequences.
- Player can witness, join or influence ongoing conflict; actions persist when the player leaves.
- Ravens, letters, messengers, rumors and public information; realistic travel/delivery times and intercepted messages.

## Phase 8 — Presentation and ASOIAF immersion

- Map/minimap with authored world layers and discovered destinations, bells/unlockable fast travel and persistence.
- Logbook of known events, filtered important chronicles, warning jingles, war news, death/succession news.
- Distinct regional/species NPC appearances, occupations, battle animations and other physical representations.
- Irregular ASOIAF seasons, extraordinary historical events, magic, dragons, Others/White Walkers and associated threats.

## Phase 9 — World coverage, balance and simulation performance

- Expand validated names, seats, dynasties, characters and relations across Westeros, Essos, Summer Isles and other known regions.
- Keep unknown Sothoryos/Ulthos content marked unknown rather than fabricate canon.
- Benchmark long-duration simulated history, economy inventories, migration, war, physical projection and cross-domain saves.
- Balance cascading effects, political agency, local travel and player impact; preserve emergent history rather than replaying a forced TV plot.

## Manual authoring and external-source checklist

- Curate canon characters (age, relationships, occupation, motives, starting position) and dynasties in 298 AC.
- Verify holding-to-region/parent hierarchy, political seat, historical ownership and any known actual settlement placement.
- Research or plausibly estimate population by region/settlement, agricultural scale, labor composition, fertility, seasonal yields, crops and consumed food. **Never treat taxBase, militaryValue or populationWeight as real measurements.**
- Maintain regional culture/religion distributions and precise exceptions; author realm and culture-specific laws.
- Trace major roads, rivers, lakes and special structures when there are trustworthy sources.
- Provide cultural architecture, NPC assets and rare unique landmarks through manual art pipelines.
- All uncertain data must contain a provenance marker and a note explaining its basis.

## Known critical gaps in the repository (audit 2026-10-16)

- Source contains 259 authored landed holdings, but main scenario settlements.json has only one settlement.
- Many dynasty/catalog definitions exist but named characters and operational institutions are sparse.
- Generic NPC WORK action is currently a no-op.
- EconomyRuntime observes campaign days; autonomously executing agriculture is not enabled.
- Coordinated full-domain calendar/NPC/economy/political checkpointing and restart safety remain incomplete.
- Population cohorts, real prices/trade, autonomous diplomacy, comprehensive military and Minecraft physical NPC projection are not complete.
- Existing geographic data is valuable, but most settlement coordinates cannot be assumed accurate.

Update this single document when features move from foundation to operational gameplay. Do not delete deferred tasks just because another subsystem becomes urgent.
