
# Known World — Connected Simulation Roadmap

Status: ACTIVE PLANNING AND INCREMENTAL IMPLEMENTATION
Scope: Dialogue, NPC integration, settlements, population, economy, and agency
Scenario baseline: AGOT 298 AC
First integration environment: Winterfell V3

This roadmap complements KNOWN_WORLD_MASTER_ROADMAP.md.
It is not a replacement for the project's wider feature roadmap.

## 0. Completion criteria

The programme is complete only when:

1. Each game fact has one authoritative owner.
2. Historical events cannot be silently invented by AI dialogue.
3. NPC families, current roles, beliefs, and relationships remain coherent.
4. Named NPCs possess distinct, persistent histories and memories.
5. Player conversations can influence NPC memories and relationships.
6. Settlement economic and demographic changes are simulated and persisted.
7. Simulation results do not depend on whether chunks are loaded.
8. A settlement can be generated from a reusable archetype and validated data.
9. Regional and global population totals reconcile with individual NPCs.
10. The world can advance and save/reload without duplicate events, people,
    resources, or replayed consequences.
11. Costly LLM inference remains on demand and is not required for routine
    simulation.
12. Every release batch has build, regression, persistence, and performance tests.

## 1. Architecture rules

- Never use generated language as an authoritative gameplay event.
- Do not introduce duplicate systems for genealogy, time, economy, or geography.
- Use stable IDs for characters, settlements, holdings, and organizations.
- Distinguish objective facts, subjective beliefs, rumors, and secrets.
- Distinguish biological kinship, marriage, household, dynasty, and faction.
- Separate simulated residents from physically projected Minecraft entities.
- Keep background simulation deterministic and independent of the LLM.
- Prefer versioned data migrations over rewriting save files opportunistically.
- Protect existing authored data and user-created world structures.
- Validate both integrated and standalone/dedicated server environments.
- Keep GitHub read-only for assistant work; provide complete files and paths.

## A. Historical consistency and NPC knowledge

### A1. Authoritative dialogue context

Status: CODE PROVIDED PREVIOUSLY; USER BUILD/TEST NOT YET CONFIRMED

Deliverables:
- Scenario-aware context instead of merely "year 298".
- Current title holders and original scenario anchor.
- Recorded genealogy, marriage, affiliation, and social relationships.
- Correct distinction between public history and private information.
- Basic guard against known historical contradictions.

Acceptance:
- Ned knows his actual public relationships.
- Ordinary Winterfell residents know who currently holds the kingship.
- Neither invents King Aemon Snow, Ramon Tully, or House Snow.
- Known confidential lineage is not disclosed to an unfamiliar player.
- Later political history is not treated as preordained.

### A2. Event-driven personal knowledge

Status: FIRST INTEGRATION SLICE PROVIDED WITH THIS ROADMAP

Deliverables:
- Observe newly generated, approved public social events.
- Generate witness-specific memory and belief records.
- Do not replay historical events when reloading a save.
- Preserve event origin, subject identity, and perspective.
- Keep observation work bounded.

Remaining work:
- Line of sight and audibility.
- Controlled rumor propagation via existing NpcCommunicationService.
- Knowledge decay, belief revisions, misinformation, and deception.
- Event publication contracts for deaths, marriages, trials, battles,
  political changes, and significant economic events.
- Explicit disclosure classification for sensitive information.

Acceptance:
- An interaction creates different knowledge for participants,
  witnesses, and people who were absent.
- The knowledge can persist without replaying an event on restart.
- No LLM reply is treated as an observed objective event.

### A3. Player-specific persistent conversation memory

Status: PLANNED

Deliverables:
- Stable NPC-ID plus player-UUID memory association.
- Persistent conversation summaries, not infinite raw transcripts.
- Speaker attribution and separation of claims from verified facts.
- Relevance-based memory retrieval and bounded memory retention.
- NPC recognition and continuity across world reloads.
- Player trust, familiarity, and reputation with individual NPCs.
- Privacy policy for confidential conversations.

Acceptance:
- Speak to one guard, leave the world, return, and continue later.
- The same guard remembers relevant previous interactions.
- Another guard does not automatically know the conversation.
- Knowledge spreads only through explicit simulated communication.
- Memory retrieval remains efficient at global population scale.

### A4. Historical and knowledge completeness

Status: PLANNED

Deliverables:
- Canon-safe opening historical state.
- Event-driven updates to current historical knowledge.
- Explicit public/private/rumor/unknown classifications.
- Person- and settlement-specific knowledge limits.
- Lore reference retrieval for culture, religion, institutions,
  regional conditions, political offices, and local events.
- NPC reactions to disputed and contradictory claims.
- Dialogue consistency tests for all scenario-authored characters.

Acceptance:
- No known future knowledge is leaked as current history.
- AI answers can be traced to authorized world-context inputs.
- Unknown facts remain unknown rather than being invented.

## B. Unified locations, holdings, and population identities

### B1. Canonical location graph

Status: PLANNED

Connect:
World map -> geographic site -> settlement -> holding ->
ruler/administration -> building -> workplace -> household -> NPC.

Deliverables:
- Stable IDs and a single coordinate authority.
- Provenance and confidence of geographic placement.
- Settlement and holding distinctions.
- Current ruler and liege resolved through actual titles.
- Data-driven spatial links and validation tools.

Acceptance:
- Winterfell's abstract settlement location and physical castle agree.
- Title, household, economy, and dialogue services resolve the same place.

### B2. Save-safe location migration

Status: PLANNED

Deliverables:
- Preflight reports before changes.
- Save backup and version identification.
- Migration dry-run and rollback instructions.
- No arbitrary teleportation of existing NPCs.
- Handling of already generated Minecraft chunks.

Acceptance:
- Old and fresh worlds produce coherent geographic identities.
- No saved NPC or holding is silently duplicated or displaced.

### B3. Population ownership and reconciliation

Status: PLANNED

Deliverables:
- Named individuals counted exactly once.
- Synthetic individuals and anonymous cohorts reconciled.
- Households, dependent children, visitors, travelers, and garrisons.
- Residence versus current physical location.
- Migration and population changes from actual events.

Acceptance:
- Settlement and regional population totals reconcile.
- Physical projection counts never alter demographic totals.

## C. Autonomous settlement simulation

### C1. Time and scheduling contract

Status: DESIGN DECISION REQUIRED

Deliverables:
- Explicit campaign-time speed or pause setting.
- Deterministic order of daily and yearly processes.
- Time catch-up and unloaded-region execution.
- Scenario-event scheduling policies.
- Integration with existing persistence/checkpoint systems.

Acceptance:
- A test world advances predictably and reproducibly.
- Multiple subsystems do not count the same elapsed day twice.

### C2. Work, production, and consumption

Status: EXISTING FOUNDATIONS; INTEGRATION PLANNED

Deliverables:
- Connect WORK goals to actual employment and output.
- Activate tested agriculture beyond MONITOR_ONLY only after audit.
- Household food demand, distribution, and inventory ownership.
- Wages, workshop production, storage, taxation, and basic markets.
- Regional logistics and resource movement.

Acceptance:
- Winterfell consumes actual supplies.
- Farms and workers produce measurable resources.
- Starvation or shortages cause real, persisted consequences.
- No negative or duplicated resources.

### C3. Population and institutional change

Status: PLANNED

Deliverables:
- Births, deaths, marriages, household creation, migration.
- Demographic cohorts reconciled with individual named characters.
- Local leadership, laws, religion, security, and military obligations.
- Institutional response to economic and demographic events.

Acceptance:
- Multi-year simulations preserve conservation and identity invariants.
- Results remain coherent when players are absent.

### C4. Simulation acceptance campaign

Status: PLANNED

Test:
- Fresh Winterfell world.
- Existing Winterfell save.
- One simulated season.
- One simulated year.
- Multiple simulated years.
- Save/reload at random checkpoints.
- Determinism and performance profiling.

Gate:
Do not activate global settlement simulations until this test suite passes.

## D. Settlement archetypes and global expansion

### D1. Generalize Winterfell generator

Status: PLANNED

Deliverables:
- Extract reusable terrain, road, district, plot, and building systems.
- Keep Winterfell-authored exceptions where necessary.
- Solve accessibility, interior clearance, doorway, ladder,
  floating lantern, vegetation, and lava-placement constraints.
- Structure-generation versioning and chunk consistency.

### D2. Two smaller urban archetypes

Status: PLANNED

Deliverables:
- Confirm target settlement size definitions.
- Define population density, plot count, labor mix, fortification,
  infrastructure, markets, and residential distribution.
- Use region-specific architecture and religion/culture parameters.
- Prototype and audit before regional rollout.

### D3. Regional settlement rollout

Status: PLANNED

Deliverables:
- Reconcile location catalogs, holdings, and missing placement audits.
- Audit canon-named residents and families at scenario opening.
- Create plausible anonymous residents without inventing canon facts.
- Assign locally consistent households, government, and employment.
- Populate the existing atlas with authoritative settlement identities.

Recommended rollout:
1. The North around Winterfell.
2. Other Westerosi regions.
3. Free Cities and settled Essos.
4. Remaining Essos and island regions.
5. Summer Isles, Sothoryos, Ulthos, and uncertain geography
   according to available evidence and authored decisions.

Acceptance:
- New settlements do not require their own bespoke simulation managers.
- All geographic placements retain evidence and confidence metadata.

## E. NPC agency and interconnected world consequences

### E1. Person-level decision loop

Status: FOUNDATIONS EXIST; WIDER INTEGRATION PLANNED

Loop:
Observe -> update beliefs/needs -> choose goal ->
validate and perform action -> emit objective event ->
update memory/relationships -> persist.

Deliverables:
- Behavior conditioned by personality, values, culture, and situation.
- Opportunity and resource constraints.
- Social, economic, travel, family, and political intentions.
- Actions with actual world effects.

### E2. Social and institutional dynamics

Status: PLANNED

Deliverables:
- Rumors, trust, friendships, rivalries, and marriages.
- Crimes, witnesses, investigation, prosecution, punishment.
- Household obligations, patronage, offices, courts, and factions.
- Loyalty, legitimacy, claims, succession, intrigue and diplomacy.
- Armed forces, recruitment, logistics, war, sieges and occupation.

### E3. LLM-to-simulation intent integration

Status: PLANNED

Deliverables:
- Structured NPC dialogue intentions as proposals, not effects.
- Server-authoritative legality and feasibility checks.
- Actual action execution through existing domain systems.
- Failure explanations returned naturally in dialogue.
- Safeguards against invented gifts, titles, quests, or outcomes.

Acceptance:
- A spoken agreement has no gameplay effect until validated.
- Once validated, its consequences are real and persist.
- The same consequences are visible outside the conversation UI.

## F. Authored characters and reference-data completion

Status: PARALLEL RESEARCH TRACK

Deliverables:
- Recover/reconcile the original supplied character master list.
- Distinguish living, dead, not yet born, and uncertain characters.
- Canonical dynasty, title, spouse, parentage, and residence data.
- Separate confirmed book canon, adaptation, inference, and invention.
- Reconcile all landed houses and seat audits.
- Link character records to local population and event history.

Acceptance:
- No duplicate identity for a named character.
- No contradictory scenario-start parentage or office.
- Every uncertainty remains flagged rather than silently invented.

## G. Scaling and release gates

### G1. Simulation scaling

- Deterministic abstract simulation outside loaded chunks.
- Spatial indexing and bounded event/memory histories.
- Cohort-based activity for populations not needing individual detail.
- Strict budgets for physical entity projections.
- No permanent AI-model instance per NPC.
- LLM inference only for actual conversations and rare narrative tasks.

### G2. Save and world compatibility

- Save version and migration reports.
- Idempotent initialization and replay protection.
- Atomic or validated persistence checkpoints.
- Dedicated-server and singleplayer parity.
- Cross-system invariant tests.

### G3. Required gates for every integration batch

[ ] Full Java compile
[ ] Unit tests
[ ] Fresh world smoke test
[ ] Existing world smoke test
[ ] Save/reload and idempotence test
[ ] Canon/privacy regression test if NPC-facing
[ ] Performance and memory check
[ ] Full changed-file paths and replacement instructions

## Recommended execution order

A1 -> A2 -> A3 -> A4
|
v
B1 -> B2 -> B3
|
v
C1 -> C2 -> C3 -> C4
|
v
D1 -> D2 -> D3
|
v
E1 -> E2 -> E3

Track F continuously.
Apply G to every stage, not only at the end.

Do not expand the autonomous simulation worldwide before C4 passes.
Do not let dialogue bypass server-side simulation rules.

## Open design decisions

1. Campaign time: real-time ratio, adjustable, paused, or hybrid.
2. Book canon versus adaptation in disputed character records.
3. Authority and handling of player-created alternative history.
4. Appropriate target population and density for each settlement tier.
5. What qualifies as public knowledge, private information, and rumor.
6. Regional rollout order after the Northern settlements.

## Current progress statement

- Existing foundation: NPC identities, personality, genealogy, titles,
  relationships, memory, belief, observation, communication, social
  interactions, settlement data, economy and population frameworks.
- Winterfell V3: bespoke city and persistent census/projection prototype.
- A1: replacement files supplied, awaiting local build confirmation.
- A2: first public-social-event witness integration supplied.
- A3 onward: not yet implemented in this integration programme.

Do not mark a batch COMPLETE merely because its files were supplied.
Completion requires the relevant acceptance tests to pass.
