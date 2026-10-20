
# Known World — Connected World Implementation Plan

Version: 23.1
Status: ACTIVE ROADMAP
Scenario baseline: AGOT 298 AC
First integration environment: Winterfell V3

Companion documents:
- docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md
- docs/architecture/20_0_SYSTEM_INVENTORY.md
- docs/architecture/20_0_ARCHITECTURE_RULES.md
- docs/architecture/23_0_CONNECTED_SIMULATION_ROADMAP.md
- docs/BATCH_19_0E_1_HOLDING_AUDIT.md
- docs/research/geography/
- docs/step3/

This plan incorporates the October 2026 user expansion requirements
and the earlier connected-simulation programme.

STATUS LEGEND

REUSE: Existing substantive code can remain the authoritative owner.
EXTEND: Existing foundation requires new capabilities or connections.
REFACTOR: Existing implementation requires structural generalization.
BUILD: No verified end-to-end implementation exists.
AUDIT: Existing data or functionality must be reconciled.
TEST: Code supplied, but acceptance tests not confirmed.

Important: Source-file existence is not proof of runtime integration.

## 0. Non-negotiable simulation architecture

### 0.1 Individual identity

Every living person represented as part of the authoritative world
population must have a stable, reproducible individual identity.

Named characters and ordinary residents follow the same identity
rules. A physical Minecraft entity is only a projection of that
simulation identity.

Every person does not need an actively rendered physical entity,
a continuously running AI model, or a full decision evaluation on
every Minecraft tick.

### 0.2 Cohort redesign

Existing PopulationCohortManager and cohort demography are valuable
but require reassessment against the individual-everywhere goal.

Target design:
- Individual identities remain authoritative.
- Regional demographic aggregates are derived indexes or caches.
- Aggregates never add people separate from actual residents.
- Bulk processing can operate on groups of individual identities.
- Important individuals receive higher-frequency simulation.
- Any compacted identity must remain deterministically recoverable.
- Identity, population, kinship and ownership are never duplicated.

Do not remove existing cohort saves before a tested migration exists.

### 0.3 Single authority

Existing owners remain authoritative:
- NPC identity: NpcRegistry
- Profile: CharacterProfileManager
- Genealogy: GenealogyManager
- Marriage: MarriageManager
- Beliefs: NpcKnowledgeManager
- Memories: NpcMemoryManager
- Events: WorldEventManager
- Relationships: NpcRelationshipManager
- Holdings: LandedHoldingManager
- Titles: TitleManager
- Claims and succession: existing dedicated managers
- Economy: existing inventory and accounting runtime
- Historical time: CampaignCalendar

All new behavior must use these owners or a documented domain
extension rather than introducing parallel mutable truth.

### 0.4 Simulation event contract

Required:
1. Intent, observation, or scheduled trigger.
2. Authoritative validation.
3. Actual state mutation.
4. Durable event with stable provenance.
5. Dependent domain consequences.
6. Witness and communication processing.
7. Memory, belief and relationship changes.
8. Persistence and reproducible catch-up.

LLM speech is not an authoritative world event.

## A. Historical knowledge, NPC dialogue and relationships

### A1. Scenario and character context

Status: TEST

Existing:
- ScenarioStartCatalog
- CampaignCalendar
- WinterfellDialogueContext
- WinterfellDialogueGuard

Required:
- Verify correct scenario-relative date.
- Verify active family relationships and titles.
- Prevent false historical claims and secret disclosures.
- Continue improving factual-response validation.
- Distinguish public identity from private identity.

Exit: 298 AC starting facts are coherent across named and ordinary NPCs.

### A2. Witnessed social events

Status: TEST

Existing:
- SocialWitnessRuntime
- WorldEventManager
- NpcMemoryManager
- NpcKnowledgeManager

Required:
- Validate actual public actions generate eyewitness records.
- Confirm persistence after save and reload.
- Verify abstract and physical location reconciliation.
- Add true hearing and visibility rules later.

Exit: Real witnesses remember eligible actual events;
bystanders outside observation range do not.

### A3. Persistent individual conversations

Status: TEST

Existing:
- WinterfellConversationLedger
- WinterfellConversationService
- WinterfellConversationPackets

Required:
- Complete cross-reload tests.
- Separate histories by NPC and player UUID.
- Protect unfinished and cancelled conversations.
- Add cold conversation archive or compressed historical log.
- Develop relevance summaries for long-running histories.
- Keep all conversation quotes unverified.

Exit: NPCs recall actual prior player exchanges,
with identity boundaries and safe persistence.

### A4. Public information and rumors

Status: CODE SUPPLIED IN THIS BATCH — TEST REQUIRED

New:
- KnownWorldPublicKnowledge
- KnownWorldPublicRumorRuntime

Existing:
- NpcCommunicationService
- SocialWitnessRuntime
- NpcKnowledgeManager

Required:
- Validate public social events before dialogue disclosure.
- Distinguish eyewitness knowledge from attributed hearsay.
- Transfer a bounded selection of reports through real NPC contact.
- Retain uncertainty and trust-adjusted confidence.
- Do not expose unclassified private facts or memories.
- Audit provenance and memory persistence.

Later:
- Death, birth, trial, battle and political event disclosures.
- Lies, conflicting rumors and belief revision.
- Public/private/confidential event classification.
- Event-specific visibility and institutional knowledge.

Exit: One event can produce different, persistent beliefs in
witnesses, listeners and uninformed people.

### A5. Player relationships and dialogue consequences

Status: EXTEND

Required:
- Stable player-to-NPC relationship identifiers.
- Player affection, trust, respect, fear and familiarity.
- Conversation-driven social intentions.
- Validated promise, gift, insult, threat and assistance operations.
- Distinguish claims from deeds.
- Persistent consequences across reloads.
- Reputation propagation through actual witnesses and institutions.
- Consent and authorization for economic or political actions.

Exit: What the player does changes behavior.
What the player merely says does not automatically alter reality.

## B. Time, geography, settlement ownership and population identity

### B1. Unified time and deterministic scheduling

Status: EXTEND

Existing:
- CampaignCalendar
- CampaignDayObserverHub
- SimulationFastForwardService
- Existing persistence/checkpoint services

Required:
- Decide time scale and adjustable speed.
- Align normal ticking, fast-forward and offline catch-up.
- Define daily, seasonal and yearly execution phases.
- Define ordered event delivery and idempotence.
- Provide save-safe rollback/migration for changes.

Exit: One simulated day produces a reproducible ordered
set of economic, demographic and social consequences.

### B2. Canonical geography and holding identity

Status: AUDIT + EXTEND

Existing:
- WorldReferenceCatalog
- Landmass, territory and regional subregion masks
- LandedHoldingManager
- SettlementManager
- Research dossiers and holding audit

Current resource:
- 259 holding entries in landed_holdings.json
- They include 171 CASTLE, 17 CITY, 6 TOWN, 2 FORTRESS,
  17 ESTATE, 34 TERRITORY and 12 REGION entries.

Important:
A holding is not automatically one physical settlement.
A territory may contain multiple settlements.
A city can be associated with a holding.
A named political region may not require urban generation.

Required:
- Establish canonical IDs and exact physical coordinates.
- Keep placement provenance and confidence.
- Link holding, seat, settlement, ruler, jurisdiction and region.
- Reconcile Winterfell abstract and physical coordinates.
- Separate occupied settlement sites from non-buildable domains.

Exit: All connected systems resolve the same place.

### B3. Holding-to-generation classification

Status: BUILD + MANUAL AUDIT

Required:
- Assign each eligible settlement a physical archetype.
- Use MAJOR, MEDIUM, SMALL as initial visual size tiers.
- Preserve castle/city/town/fortress/estate/region as
  separate political/functional classifications.
- Permit multiple settlements under one holding.
- Record unknown sites explicitly.
- Provide auditable mappings and overrides.

Exit: All 259 holding records are reviewed without falsely
turning every territory or region into one generated town.

### B4. Individual population authority

Status: REFACTOR

Existing:
- NpcRegistry and authored IDs
- Winterfell persistent census
- PopulationCohortManager
- AnnualCohortDemography
- HoldingPopulationProjection
- SettlementPopulationCensus

Required:
- Adopt persistent individual population as authoritative.
- Reconcile existing cohort saves and named NPCs.
- Avoid any hidden extra headcount.
- Allow batched low-frequency processing of individual identities.
- Assign residence, household, workplace and travel state.
- Preserve age, culture, religion and social class.
- Build regional sharding, lazy loading and recovery.
- Version all migrations and run conservation audits.

Exit: Every counted resident resolves to an individual identity,
even when not physically rendered or frequently updated.

### B5. Scenario-authored named characters

Status: EXTEND + DATA AUDIT

Existing:
- 21 core population-pack authored character records
- Additional Winterfell census and profile proposals
- NamedCharacterAuthoringRuntime
- Genealogy, dynasty, titles and scenario state

Required:
- Reconcile the original full user-supplied character list.
- Assign canonical or explicitly inferred starting locations.
- Preserve affiliations, parentage, age, culture, religion,
  traits, offices, secrets and scenario-limited knowledge.
- Distinguish present, absent, traveling, unborn and dead.
- Assign one persistent identity across all future locations.
- Avoid creating duplicate versions of famous characters.

Exit: Named characters appear or travel from validated
starting states and follow the same rules as ordinary residents.

## C. Reusable physical settlements

### C1. Settlement generator extraction

Status: REFACTOR

Existing:
- WinterfellWorldgenV3
- KnownWorldChunkGenerator
- Terrain and elevation providers

Required:
- Extract reusable road, district, plot, wall and building logic.
- Keep authored landmarks separately configurable.
- Make footprint, density and street network data-driven.
- Preserve Winterfell as a migration-safe case study.

Exit: Multiple places share one configurable generation framework.

### C2. MAJOR / MEDIUM / SMALL settlement archetypes

Status: BUILD

Required:
- Major castle/seat rules generalized from Winterfell.
- Medium seat and fortified/local town templates.
- Small hold, village and manor templates.
- Configurable settlement density.
- Buildings scaled to resident population and local jobs.
- Residential capacity checked against household counts.
- Winterfell layout rebalanced for closer building clusters.
- Settlement districts and open-space ratio configurable.
- Regional architecture variations.

Exit: The same generator creates differently sized,
believable and internally valid communities.

### C3. Terrain and building-material adaptation

Status: EXTEND

Existing:
- RasterTerrainProvider
- ElevationProvider
- MountainTerrainGenerator
- LowlandTerrainGenerator
- Regional influence and geographic masks

Required:
- Slope-aware foundations, stepped roads and retaining walls.
- Height limits and cut/fill safeguards.
- Terrain preservation on authored custom mountains.
- Hard rejection of submerged and hazardous building plots.
- No building foundations, residents or doorways in water.
- Allow coast-specific harbors only by explicit templates.
- Biome, geological, culture and region material palettes.
- Collision and terrain regeneration audits.

Exit: Settlements work on appropriate hillsides, steep
terrain and custom mountains without accidental flooding
or unwanted flattening.

### C4. Functional buildings and property

Status: BUILD + CONNECT

Required:
- Homes with household ownership/occupancy.
- Family or business-name signs derived from current ownership.
- Bakeries, mills, breweries, smithies, armories, workshops.
- Markets, stores, inns and warehouses.
- Religious buildings tied to appropriate faiths.
- Stables, barracks, training grounds, guard posts.
- Farms, farmhouses, fields, barns and pasture.
- Castles, administrative courts and noble housing.
- Public spaces, roads, wells, bridges and gates.
- Building capacity, entrances, beds, workstations and access.
- Update signs and usage when occupancy or ownership changes.

Exit: Buildings correspond to jobs and real local economic needs.

### C5. Generation persistence and live construction

Status: BUILD

Required:
- Versioned structure definitions and generation manifests.
- Avoid destruction of player-built structures.
- Distinguish fresh-world generation from existing-world migration.
- Building construction, demolition and repair operations.
- Ensure structures cannot spawn through people or roads.
- Idempotent chunk generation and save/reload behavior.

Exit: World changes survive, and regeneration does not
duplicate or destroy structures without an explicit migration.

## D. Individual daily life, production and families

### D1. Jobs, needs and routines

Status: EXTEND

Existing:
- NpcRoutineService
- NpcGoalManager
- NpcDecisionService
- NpcActionProcessor
- CharacterProfile, psychology and needs systems

Required:
- Real workplaces and valid working hours.
- Sleeping, eating, working, worship, leisure and social visits.
- Travel between assigned individual destinations.
- Dynamic needs, health, fatigue, hunger and safety.
- Personality, skills, culture and religion affecting choices.
- Distinct schedules for guards, farmers, merchants,
  nobles, servants, children, travelers and religious orders.

Exit: Every sampled individual has coherent routine
destinations even when their Minecraft entity is absent.

### D2. Employment and regional economy

Status: EXTEND

Existing:
- EconomyRuntime
- InventoryManager
- AgriculturalFoodCycle
- ResourceCatalog
- HouseholdFoodDemandModel
- Economic accounting and checkpoint systems

Required:
- Connect WORK actions to actual production.
- Validate and activate autonomous agriculture.
- Allocate workers to facilities and productive land.
- Implement production inputs, output, wages and consumption.
- Household budgets and shared property.
- Local markets and inventory trading.
- Taxes, rents, debts, prices and shortages.
- No duplicated food or money across systems.

Exit: Winterfell can feed and employ its residents
through a multi-season simulation.

### D3. Family lifecycle and interpersonal conflict

Status: EXTEND

Existing:
- MarriageManager and MarriageService
- FertilityService and PregnancyManager
- LifeCycleService
- GenealogyManager
- ChildFormationService
- UpbringingManager

Required:
- Marriage and courtship decisions.
- Pregnancy, fertility and birth outcomes.
- Child development and inheritance of characteristics.
- Family disputes, obligations and reconciliation.
- Household formation, separation and widowhood.
- Death reactions, mourning and changing responsibilities.
- Distinguish public family information from private parentage.

Exit: Family events create actual household, emotional,
legal and demographic consequences.

### D4. Local social agency

Status: EXTEND

Required:
- Personal goals influence daily action selection.
- Actions affect needs and relationships.
- Friendships, feuds, patronage and reputation develop.
- Witnessed events and rumors influence local decisions.
- NPCs respond to deaths, leadership changes and shortages.
- Social life continues without player contact.

Exit: An event can alter subsequent autonomous individual behavior.

## E. Travel, trade and regional interactions

### E1. Authoritative world travel

Status: BUILD ON EXISTING TRAVEL GOALS

Required:
- Route graph for roads, rivers, passes and sea transport.
- Origin, destination, speed, party and travel schedule.
- Simulated position along journeys while chunks are unloaded.
- Materialize travelers near player routes without duplication.
- Player interception of caravans, armies and named travelers.
- Terrain-aware movement, delays and impassable routes.

Exit: Travel is continuous in simulation and visible on encounter.

### E2. Regional economic exchange

Status: BUILD

Required:
- Merchants, caravans, cargo ships and trade fleets.
- Supply, demand, prices, tolls and transport costs.
- Route danger, raids and cargo ownership.
- Market changes caused by wars, weather and production.
- Cross-region resource conservation.

Exit: Goods actually move between settlements through
tracked agents and economic transactions.

### E3. Event propagation across distance

Status: EXTEND

Required:
- Communication via travelers, messengers and institutions.
- Travel-time limits for information.
- Public news, secret intelligence and unreliable hearsay.
- Cross-region political and economic responses.
- No instantaneous world-wide knowledge synchronization.

Exit: Distant people learn about events through plausible channels.

## F. Government, institutions and succession

### F1. Political office and authority

Status: EXTEND

Existing:
- TitleManager
- LandedHoldingManager
- DynastyManager
- OrganizationManager
- AuthorityService
- Religious authorities and social/legal state models

Required:
- Governance roles and institutional jurisdictions.
- Tax collection and public spending.
- Religious and cultural legitimacy.
- Local councils and courts.
- Authority delegation and administrative capacity.
- Leadership response to real population and economic needs.

### F2. Succession, regencies and obligations

Status: EXTEND + BUILD

Existing:
- SuccessionService
- SuccessionRuleManager
- ClaimManager
- Dynasty continuity and holding reconciliation

Required:
- Distinct succession rules by society and title.
- Regency for minors or incapacitated rulers.
- Designation, election, appointment and hereditary offices.
- Feudal obligations, vows, oaths and their violations.
- Recruiting retainers, officers and local administrators.
- Claims, succession crises and legitimacy disputes.

### F3. Intrigue, law and factions

Status: BUILD ON EXISTING FOUNDATIONS

Required:
- Secrets, investigations, plots and information asymmetry.
- Loyalty, fear, respect, leverage and institutional trust.
- Crime, prosecution, punishments and law enforcement.
- Factions and their recruitment, funding and membership.
- Religious and cultural institutional conflicts.
- Rebellions and revolts from actual causes.

Exit F: Political decisions depend on actual people,
resources, titles, information and legal authority.

## G. Warfare, armies and dynamic holdings

### G1. Armed organizations

Status: BUILD

Required:
- Levies, guards, professional soldiers and mercenaries.
- Command hierarchy and recruiting authority.
- Individual soldiers accounted for in population totals.
- Training, equipment, supply, pay and morale.
- Movement, scouting, ambush, desertion and casualties.

### G2. Conflicts and sieges

Status: BUILD

Required:
- Battles, raids, sieges and occupation.
- Deterministic strategic outcomes and local physical combat.
- Fortifications, provisions, terrain and supply lines.
- Prisoners, wounded, killed and captured equipment.
- Legitimate and illegitimate war causes.
- Political, economic, personal and demographic consequences.

### G3. Temporary and permanent military construction

Status: BUILD USING C GENERATION FRAMEWORK

Required:
- Traveling armies place temporary camps.
- Factions establish outposts or fortified positions.
- Construction consumes real labor and resources.
- Player/NPC factions may establish new forts or settlements.
- Ownership and territorial authority are registered.
- Camps may relocate, decay or be dismantled.
- Forts may be upgraded, besieged, captured or abandoned.

Exit G: Military structures and wars interact with
the same economy, population, ownership and map systems.

## H. Quests and deep player interaction

### H1. Simulation-derived contracts and requests

Status: BUILD

Required:
- Dynamic work, delivery, escort, investigation, diplomacy,
  rescue, training and military service opportunities.
- Quests generated from actual needs and world situations.
- Validated objectives and NPC authority to offer rewards.
- Real resource, currency, title, trust and reputation rewards.
- Consequences for success, betrayal, failure and refusal.

### H2. LLM action bridge

Status: BUILD

Required:
- Structured proposed NPC intentions from conversation.
- No direct state mutation by arbitrary LLM text.
- Server validation for feasibility and permissions.
- Dialogue reflects accepted or rejected actions.
- Persistent contracts, obligations and consequences.

Exit H: A conversation can initiate actual gameplay
through validated simulation operations.

## I. Global rollout

### I1. Northern prototype cluster

Status: PLANNED

Deploy:
- Winterfell.
- Several medium seats.
- Multiple small holdings and villages.
- Associated farmland, roads and institutions.
- Residents and named scenario characters.

Verify:
- Population and employment.
- Trade and travel.
- Family continuity.
- Social rumor spread.
- Economy and governance.
- Minecraft generation performance.

### I2. Regional rollout

Status: PLANNED

Use audited datasets and authoritative location IDs.
Recommended progression:
1. The North.
2. Remaining Westerosi regions.
3. Free Cities and heavily documented Essosi regions.
4. Wider Essos.
5. Summer Isles and remaining supported locations.
6. Sothoryos, Ulthos and uncertain or legendary sites
   using explicit confidence and optional-generation rules.

Do not silently convert legendary locations into canon towns.

### I3. Full 298 AC activation

Status: PLANNED

Required:
- Each generated settlement connected to population.
- Named people assigned to their scenario locations.
- All ordinary residents generated from local rules.
- Complete region-wide accounting.
- Regional simulation active without loaded chunks.
- Durable political, trade and military interactions.

## J. Continuous research and authored data

Status: ACTIVE

Existing:
- Global holdings audits.
- Geographic research dossiers.
- Winterfell census and household data.
- Dynasty and relationship resources.
- Named character profile proposals.
- Culture/religion and territory reference catalogs.

Required:
- Reconcile older documents with newer scenario resources.
- Preserve source provenance and uncertainty.
- Complete the original named-character master list.
- Match named characters to actual 298 AC locations.
- Resolve ambiguous and misidentified map markers.
- Document inferred population densities and occupations.
- Research regional construction materials and institutions.
- Never label invented population estimates as canon.

## K. Continuous engineering and scaling

Status: ACTIVE

Required for every release batch:
- Compilation.
- Unit/integration tests.
- Fresh-world validation.
- Existing-save migration verification.
- Save/reload and replay protection.
- Determinism checks.
- CPU, RAM, VRAM and storage profiling.
- Chunk-load independence.
- Dedicated-server and local-client compatibility.
- Full changed files and exact paths in the implementation response.

## Dependency sequence and release gates

Gate 1 — Reliable knowledge and identity:
A1, A2, A3, A4, then A5 prototype.

Gate 2 — Authoritative time, place and population:
B1 through B5 with persistence migrations.

Gate 3 — Reusable physical world:
C1 through C5 and a Northern prototype cluster.

Gate 4 — Autonomous local society:
D1 through D4, with household and economic tests.

Gate 5 — Regional world:
E1 through E3, including transport and information flow.

Gate 6 — Politics:
F1 through F3.

Gate 7 — War and construction:
G1 through G3.

Gate 8 — Player agency:
H1 through H2.

Gate 9 — Global coverage:
I1 through I3, progressively rather than in one generation pass.

J and K run continuously.

## Practical limitations

1. Every person can have a persistent identity, but rendering and
   high-frequency simulation of all world residents simultaneously
   is not practical on one consumer computer.

2. Individual identities at many millions of residents require
   regional sharding and incremental processing; a global in-memory
   Java object for every person is not a viable final architecture.

3. Full conversation transcripts can be archived on disk, but
   cannot all be sent to an LLM prompt. Use indexed retrieval,
   summaries and optional compressed cold storage.

4. No model can perfectly guarantee canonical dialogue facts.
   Authoritative retrieval, disclosure rules and validation
   are still needed.

5. Coordinates and populations not established by published
   sources require an explicit mod-authored or inferred label.

6. Structures in previously generated chunks do not automatically
   change when generator code is updated. Existing worlds need a
   safe migration or intentional regeneration strategy.

7. A consumer GPU cannot give millions of NPCs continuous
   individual LLM inference. Deterministic simulation, selective
   inference and persistent identity are the scalable solution.

## Immediate next release after A4

A5:
- Authoritative player-NPC relationship records.
- Validated player social actions.
- Player and NPC memory consistency.
- Public versus private interaction outcomes.
- Dialogue-to-simulation intent proposals.

Then begin B1 and B2 before expanding settlement generation.
