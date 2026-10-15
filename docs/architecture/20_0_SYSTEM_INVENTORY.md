
# Known World — Batch 20.0 System Inventory

Document version: 1.0
Baseline: Minecraft_Server_Mod2, master branch
Audit date: 2026-10-15

## 1. Purpose

This document establishes the architectural baseline for all
future simulation development.

The project aims to simulate the Known World of A Song of Ice
and Fire within Minecraft at a horizontal geographic scale
of one block per metre.

The target is a persistent, autonomous medieval world
simulation combining:

- Individual NPC simulation
- Family and generational continuity
- Social, cultural and religious simulation
- Households and population demographics
- Property ownership and economic production
- Political institutions and dynastic history
- Diplomacy, intrigue and warfare
- Geographic and environmental simulation
- Minecraft-based player interaction
- Scalable abstract and physical simulation

The scenario baseline is 298 AC.

This inventory must distinguish between:

1. Existing data models
2. Existing business logic
3. Autonomous simulation behavior
4. Persistence
5. Minecraft presentation
6. Verified tests

A class existing does not imply feature completeness.

## 2. Current Runtime Architecture

Primary entry point:

src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java

Primary simulation coordinator:

src/main/java/dev/dtzudontsay/knownworld/
simulation/NpcSimulation.java

The simulation coordinator currently constructs and connects
the principal NPC, settlement, organization, social, lifecycle,
title, and persistence services.

### 2.1 Server lifecycle

NpcSimulation registers lifecycle callbacks for:

- Server started
- End server tick
- Before save
- Server stopping
- Server stopped

At server start, simulation state is loaded and scenario
bootstrapping may occur if no existing simulation exists.

### 2.2 Runtime update loop

The regular simulation loop currently:

1. Advances the simulation clock.
2. Advances the campaign calendar.
3. Runs lifecycle processing on a new campaign day.
4. On the periodic update interval:
    - Updates NPC activation levels.
    - Updates NPC needs.
    - Updates NPC decisions.
    - Updates NPC routines.
    - Processes NPC actions.

This is a valid foundational runtime, not yet a complete
multi-domain world simulation scheduler.

### 2.3 Accelerated simulation

SimulationFastForwardService advances campaign days without
requiring ordinary Minecraft wall-clock progression.

It currently executes:

- Campaign clock advancement
- Campaign day advancement
- Lifecycle processing
- Abstract social simulation

Future economic, political, military and demographic systems
must be integrated consistently into accelerated progression.

## 3. Current System Inventory

STATUS DEFINITIONS:

ESTABLISHED:
Substantial implementation exists in the inspected code.

FOUNDATION:
Core models and working logic exist, but larger integration
or functionality remains incomplete.

PARTIAL:
Limited autonomous mechanics, models or services exist.

PLANNED:
No complete implementation verified in the current inventory.

Statuses are architectural assessments, not test-pass claims.

### SIM-001 — NPC Registry and Identity

Current status: ESTABLISHED

Primary implementation:
simulation/npc/

Principal components:
- NpcId
- NpcIdentity
- NpcState
- NpcRegistry
- NpcSpatialIndex
- NpcLifeState

Owned state:
- NPC identity
- NPC existence and life state
- Simulated character position
- Simulation level

Dependencies:
- SimulationPosition
- SimulationLevel

Consumers:
- Lifecycle
- Needs
- Decisions
- Relationships
- Social systems
- Character profiles
- Title and succession systems

Next requirement:
Define stable character identity and mutation contracts.

### SIM-002 — Simulation Time

Current status: FOUNDATION

Primary implementation:
simulation/time/

Principal components:
- SimulationClock
- CampaignCalendar

Owned state:
- Simulation tick
- Campaign day
- Campaign year
- Campaign time configuration

Consumers:
- Lifecycle
- Pregnancy and fertility
- Social simulation
- Fast-forward
- Events and memories

Next requirement:
Formalize authoritative time and update scheduling.

Important:
Minecraft routine time and historical campaign time
must have explicitly documented conversion rules.

### SIM-003 — Simulation Fidelity

Current status: PARTIAL

Primary components:
- NpcActivationManager
- SimulationLevel
- NpcSpatialIndex

Implemented:
- Detailed activation near players
- Abstract activation at intermediate distances
- Dormant state outside activation range

Current known limitation:
Physical NPC projection is not implemented.

Next requirement:
Separate individual simulation importance from physical
proximity and establish population aggregation rules.

### SIM-004 — Character Profiles

Current status: FOUNDATION

Primary implementation:
simulation/npc/profile/

Principal components:
- CharacterProfile
- CharacterProfileManager
- CharacterProfileGenerationService
- CharacterDisposition
- CharacterValue
- CharacterAptitude
- CharacterSkill
- CharacterSocialNorm
- CharacterHealthState
- DialoguePersona
- SkillPracticeService

Owned state:
- Character characteristics
- Skills and aptitudes
- Values and dispositions
- Profile-dependent personal information

Consumers:
- Psychology
- Social reactions
- Social decisions
- Culture
- Religion
- Character formation

Next requirement:
Standardize influence on autonomous decisions.

### SIM-005 — Needs and Routines

Current status: PARTIAL

Primary implementations:
simulation/npc/need/
simulation/npc/routine/

Principal components:
- NpcNeeds
- NpcNeedManager
- NpcRoutine
- NpcRoutineManager
- NpcRoutineService

Owned state:
- Character needs
- Routine configuration and progress

Consumers:
- NpcDecisionService
- NpcActionProcessor

Next requirement:
Integrate food, work, income, safety and household state.

### SIM-006 — Decisions, Goals and Actions

Current status: PARTIAL

Primary implementations:
simulation/npc/decision/
simulation/npc/goal/
simulation/npc/action/

Principal components:
- NpcDecisionService
- NpcGoalManager
- NpcActionProcessor

Current verified behavior:
- Fatigue can generate REST goals.
- REST goals recover fatigue.
- TRAVEL goals alter simulated position.
- WORK has no implemented productive side effect.

Next requirement:
Introduce general-purpose action selection and validated
domain-level state mutations.

### SIM-007 — Memory and Knowledge

Current status: FOUNDATION

Primary implementations:
simulation/npc/memory/
simulation/npc/knowledge/

Principal components:
- NpcMemoryManager
- NpcMemoryMeaningService
- NpcKnowledgeManager
- NpcBelief

Owned state:
- Individual memories
- Knowledge and beliefs

Consumers:
- Social interactions
- Communication
- Observation
- Social decisions

Next requirement:
Define consistent knowledge acquisition, propagation,
reliability and decision influence.

### SIM-008 — Relationships and Social Behavior

Current status: FOUNDATION

Primary implementations:
simulation/npc/relationship/
simulation/npc/social/
simulation/social/relationship/

Principal components:
- NpcRelationshipManager
- SocialInteractionService
- SocialReactionService
- SocialActionDecisionService
- SocialEncounterService
- GlobalRelationshipRuntime
- DynastyRelationshipManager

Owned state:
- Individual relationships
- Dynasty-level relationships
- Authored social relations

Next requirement:
Unify relationship consequences across individuals,
organizations, dynasties and political decisions.

### SIM-009 — Family and Lifecycle

Current status: FOUNDATION

Primary implementations:
simulation/npc/family/
simulation/npc/lifecycle/
simulation/npc/formation/

Principal components:
- GenealogyManager
- MarriageManager
- MarriageService
- PregnancyManager
- FertilityService
- LifeCycleService
- LifeHistoryManager
- CharacterGenerationService
- ChildFormationService
- UpbringingManager

Owned state:
- Genealogy
- Marriage
- Pregnancy
- Age and life history
- Childhood formation

Consumers:
- Succession
- Dynasty systems
- Social identity
- Character psychology

Next requirement:
Integrate households, resources and family decision-making.

### SIM-010 — Culture and Religion

Current status: FOUNDATION

Primary implementations:
simulation/npc/culture/
simulation/npc/religion/
world/reference/

Principal components:
- CultureService
- CultureDistanceService
- ReligionService
- ReligiousAuthorityService
- ReligiousInstitutionRuntimeManager
- ReligiousMembershipManager
- RegionalInfluenceResolver

Owned state:
- Individual faith membership
- Religious institutional membership
- Cultural and religious influence where persisted

Reference catalogs:
- Cultures
- Religions
- Languages
- Religious institutions
- Religious orders
- Regional influences

Next requirement:
Connect culture and faith to education, governance,
law, social conflict and autonomous decisions.

### SIM-011 — Dynasties and Houses

Current status: FOUNDATION

Primary implementation:
simulation/social/dynasty/
simulation/dynasty/

Principal components:
- DynastyManager
- DynastyContinuity
- DynastyAllegianceManager
- DynastyHierarchyBootstrapService
- DynastyLeadershipReconciliationService

Owned state:
- Dynasty identity
- Membership and status
- Leadership
- Allegiance
- Hierarchical relationships

Next requirement:
Define autonomous house interests and political strategy.

### SIM-012 — Titles, Claims and Succession

Current status: FOUNDATION

Primary implementations:
simulation/social/title/
simulation/social/succession/

Principal components:
- TitleManager
- ClaimManager
- SuccessionRuleManager
- SuccessionService
- AuthorityService

Owned state:
- Political titles and assignments
- Claims
- Succession rules
- Authority relationships

Current verified behavior:
SuccessionService can evaluate heirs and process the death
of a title holder.

Next requirement:
Integrate legitimacy, factions, governance, usurpation,
territorial ownership and diplomatic consequences.

### SIM-013 — Holdings and Settlements

Current status: FOUNDATION

Primary implementations:
simulation/social/holding/
simulation/world/settlement/
world/settlement/
world/reference/

Principal components:
- LandedHoldingManager
- LandedHoldingBootstrapService
- DeadHoldingHolderReconciliationService
- SettlementManager
- WorldReferenceCatalog

Owned state:
- Holding identities and assignments
- Settlement identities
- Related geographic references

Next requirement:
Define physical property, economic ownership, land use,
settlement production and resource stocks.

Important:
World/reference definitions, runtime settlements, and
holdings must remain conceptually separate.

### SIM-014 — Organizations and Societies

Current status: FOUNDATION

Primary implementations:
simulation/social/
simulation/social/membership/
simulation/social/society/

Principal components:
- OrganizationManager
- OrganizationMembershipManager
- NonDynasticSocietyManager
- NonDynasticSocietyService
- NpcAffiliationManager

Owned state:
- Organization identity
- Membership
- Society structures
- NPC affiliations

Next requirement:
Integrate alternative government systems and their
economic, social and political consequences.

### SIM-015 — World Events and History

Current status: FOUNDATION

Primary implementation:
simulation/event/

Principal components:
- WorldEventManager
- HistoricalChronicleManager

Owned state:
- Simulation events
- Historical chronicle records

Next requirement:
Define event ordering, provenance, stable IDs,
domain-level event contracts and event retention.

### SIM-016 — Scenario Bootstrap

Current status: ESTABLISHED

Primary implementation:
simulation/bootstrap/

Principal components:
- ScenarioBootstrapper
- ScenarioStartCatalog
- PopulationPackCatalog
- AuthoredIdRegistry
- NamedCharacterAuthoringRuntime

Owned responsibility:
- Initial scenario data loading
- Authored-to-runtime identity resolution
- Starting population initialization

Next requirement:
Explicit versioning and validation of scenario content.

### SIM-017 — Persistence

Current status: FOUNDATION

Primary implementation:
simulation/persistence/

The repository contains dedicated persistence for NPCs,
genealogy, marriage, pregnancy, profile data, legal status,
claims, succession, titles, and other state domains.

Next requirement:
Define save consistency, schema evolution, migrations,
recovery and deterministic save/load testing.

### SIM-018 — Fast-Forward and Testing

Current status: FOUNDATION

Primary implementation:
simulation/testing/

Principal components:
- SimulationFastForwardService
- AbstractSocialSimulationService
- SyntheticPopulationService
- SimulationReportService

Next requirement:
Establish a headless, deterministic integration test suite
and baseline long-duration performance metrics.

### SIM-019 — Geography and Terrain

Current status: FOUNDATION

Primary implementations:
world/geography/
world/reference/spatial/
world/terrain/
world/generation/
world/biome/

Current capabilities:
- Geographic projection and calibration
- Raster geographic sampling
- Land and ocean generation
- Elevation and terrain generation
- Biome and climate references
- Geographic masks and zone resolution

Next requirement:
Geographic accuracy testing, physical settlement
integration, roads and navigation cost data.

## 4. Missing Domain Families

The following are required by the final vision but do not
yet have verified complete autonomous implementations.

SIM-020: Household and residence simulation
SIM-021: Population cohorts and demography
SIM-022: Property and asset ownership
SIM-023: Resource production and inventories
SIM-024: Consumption, scarcity and prices
SIM-025: Labor, wages and professions
SIM-026: Trade and transport logistics
SIM-027: Government finance and taxation
SIM-028: Autonomous governance
SIM-029: Diplomacy and treaties
SIM-030: Political factions and intrigue
SIM-031: Military recruitment and forces
SIM-032: Army logistics and combat
SIM-033: Sieges, occupation and peace
SIM-034: Crime, law and justice
SIM-035: Illness, disease and healthcare
SIM-036: Education and apprenticeship
SIM-037: Migration and social mobility
SIM-038: Roads, routing and travel hazards
SIM-039: Physical NPC projection and navigation
SIM-040: Player character and dialogue integration
SIM-041: Buildings and settlement infrastructure
SIM-042: Climate, seasonal shocks and ecology
SIM-043: Maritime economy and naval systems
SIM-044: Extraordinary ASOIAF phenomena
SIM-045: Population-scale fidelity conversion

This list is a planning inventory, not proof that no
related classes exist.

## 5. Known Cross-System Risks

RISK-001:
Different simulation systems may use different
interpretations of time.

RISK-002:
Live progression and accelerated progression execute
different sets of systems.

RISK-003:
NPC activation currently depends on player proximity;
persistent world-scale progression needs an independent
background scheduler.

RISK-004:
WORK goals have no productive economic side effect.

RISK-005:
Political data models exist without a complete autonomous
governance, diplomacy and warfare engine.

RISK-006:
There is not yet a unified, authoritative economic state.

RISK-007:
Multiple managers may need coordinated changes following
death, title transfer, affiliation changes or marriage.

RISK-008:
Physical Minecraft entity state and abstract NPC state
need explicit synchronization contracts.

RISK-009:
Saving multiple related state domains separately may
require consistency and recovery guarantees.

RISK-010:
Population aggregation and individual materialization
need invariant-preserving conversion rules.

## 6. Intended Development Sequence

20.0 — Inventory and architectural baseline
20.1 — Contracts, simulation time and domain boundaries
20.2 — Determinism, persistence and testing
20.3 — Households, population and economy design
20.4 — First integrated society benchmark

Following batches:
- Economic foundation
- General-purpose individual AI
- Autonomous political simulation
- Warfare and logistics
- Minecraft physical integration
- Deep secondary systems
- Global-scale validation

## 7. Completion Requirements

A system can be declared CONTRACT COMPLETE when:

- Its authoritative state is identified.
- Public contracts are documented.
- Mutation ownership is clear.
- Persistence responsibilities are established.
- Invariants are specified.
- Contract tests pass.

A system can be declared CORE COMPLETE when:

- Its essential behavior executes autonomously.
- Its dependencies work through stable contracts.
- Its outcomes produce persistent consequences.
- It works with normal and accelerated progression,
  where applicable.
- Integration and behavioral tests pass.

A system can be declared FEATURE COMPLETE when:

- Intended mechanics are implemented.
- Edge cases are addressed.
- Tuning and balance are validated.
- Required Minecraft-facing behavior exists.
- Performance targets are satisfied.
- Long-duration regression tests pass.

## 8. Audit Limitations

This inventory is based on source inspection and repository
structure. It is not a claim that every file has undergone
line-by-line inspection or that every feature has passed
runtime verification.

The dependency inventory generated by the companion Python
script is a static Java import map.

It cannot prove runtime data flow, dynamically registered
callbacks, reflection, or behavior across resource files.

Those require targeted inspection and tests.
