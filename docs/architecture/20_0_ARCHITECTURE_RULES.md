
# Known World — Simulation Architecture Rules

Version: 1.0
Introduced: Batch 20.0
Status: Architecture baseline

## 1. Fundamental Principle

Known World is a persistent world simulation presented
through Minecraft.

Minecraft entities, chunks, and rendering are not the
authoritative source of simulation truth.

The simulation must remain capable of progressing without
physically loaded NPC entities.

## 2. Development Method

Development follows architecture-first vertical slices.

We do not:
- Finish isolated systems without testing integration.
- Build empty frameworks indefinitely.
- Introduce independent copies of shared state.
- Implement every potential future feature immediately.
- Use debug commands as substitutes for autonomous behavior.

We do:
- Define contracts before major dependent systems.
- Implement complete causal loops.
- Test systems together.
- Preserve and extend working code where reasonable.
- Maintain backwards-compatible persistence.
- Measure performance before scaling up.

## 3. Single Ownership Rule

Every piece of authoritative mutable state must have
exactly one defined owner.

Examples:

NPC identity:
NpcRegistry

Character profile:
CharacterProfileManager

Genealogy:
GenealogyManager

Marriage:
MarriageManager

Claims:
ClaimManager

Titles:
TitleManager

Campaign calendar:
CampaignCalendar

These are current ownership candidates derived from the
existing architecture.

Cross-system operations must not silently create
contradictory state.

A future domain contract must explicitly define who may
modify each state and how.

## 4. Mutation Rule

State changes affecting multiple domains must be executed
through validated operations.

Example: character death.

Potentially affected systems:
- NPC life state
- Marriage and family state
- Pregnancy and dependents
- Holding ownership
- Title succession
- Dynasty leadership
- Organization membership
- Relationships and memories
- Population records
- Historical chronicle

These changes must remain consistent.

Future implementation should introduce coordinated domain
operations with defined failure and recovery behavior.

The existing managers should not be bypassed casually.

## 5. Time Rule

Historical campaign time is the authoritative simulation
timeline for long-term world progression.

Minecraft physical time remains responsible for short-term
presentation and physical interactions.

Every time-based system must define:
- Its time unit
- Update frequency
- Scheduling phase
- Dependencies on earlier phases
- Fast-forward behavior
- Persistence requirements

No new system may silently invent a separate calendar.

Batch 20.1 will formalize conversion and scheduling rules.

## 6. Simulation Execution Rule

Normal progression and accelerated progression must operate
on the same authoritative world state.

They must apply equivalent domain rules.

Different computational strategies are permitted, but
they must preserve defined invariants and acceptable
outcome consistency.

Exact event-by-event equivalence may require a deterministic
mode and is not automatically guaranteed by aggregation.

## 7. Determinism Rule

Simulation randomness must be explicitly controlled.

Future systems should receive deterministic random streams
derived from documented seeds and simulation context.

Randomness must not depend on:
- Uncontrolled wall-clock time
- Rendering frame rate
- Iteration order of unordered collections
- Unstable runtime identifiers

The development test harness must support reproducible runs.

## 8. Simulation Fidelity Rule

NPC simulation fidelity is not defined solely by distance.

The system must support:

PHYSICAL:
A Minecraft entity represents an authoritative NPC.

DETAILED:
Individual character with detailed behavioral simulation.

ABSTRACT_INDIVIDUAL:
Persistent individual using reduced-frequency decisions.

COHORT:
Aggregated population with demographic and economic state.

REGIONAL:
Settlement or realm-level aggregate processes.

These are target architectural concepts.

They do not imply that all levels currently exist in code.

Materialization and abstraction must preserve:
- Population conservation
- Property ownership
- Important character identities
- Household relationships
- Historical continuity
- Inventory and resource balances

## 9. Economic Conservation Rule

Future resource systems must define conservation rules.

A resource transfer must not duplicate resources.

Production and consumption must be recorded as distinct
operations with explicit causes.

Currencies, debts and ownership claims require their own
accounting invariants.

The economy must not rely on nearby Minecraft chunks
remaining loaded.

## 10. Decision-Making Rule

NPC intelligence must be decomposed into:

1. Observation and available information
2. Personal state, values and needs
3. Goals and possible actions
4. Action evaluation
5. Action selection
6. Action execution
7. Consequence observation
8. Memory and belief updates

Important:
The decision system must not directly modify unrelated
domain state without a validated domain operation.

Optional language-model-generated intentions must obey
the same rules as deterministic decisions.

## 11. Political Simulation Rule

Political decisions must operate on authoritative data.

Relevant inputs include:
- Characters
- Dynasties
- Titles
- Claims
- Holdings
- Population
- Resources
- Military capacity
- Relationships
- Culture
- Religion
- Laws and legitimacy

Political power should arise from meaningful world
conditions rather than unrelated arbitrary scores.

## 12. Minecraft Integration Rule

Minecraft is the presentation and interaction layer.

A physical NPC represents a simulated individual.

Physical entity unloading must not delete the individual.

Player actions must pass through validated simulation
operations whenever they affect persistent world state.

Physical behavior and background simulation should not
apply the same economic or demographic effect twice.

## 13. Persistence Rule

All newly introduced authoritative state requires:

- A documented save format
- Explicit schema versioning
- Validation during loading
- Defined behavior for missing data
- Migration or compatibility handling
- Round-trip tests

Related domain state must not be partially committed
without defined recovery behavior.

Never silently discard unknown existing data.

## 14. Event Rule

Domain events should identify:
- Event type
- Campaign time
- Responsible actor or system
- Relevant target entities
- Context and cause
- Resulting consequences
- Stable event identity where needed

An event journal does not replace authoritative state.

State-changing operations and historical records must
remain causally consistent.

## 15. Testing Rule

Every major mechanic should have:

UNIT TEST:
Tests isolated rules.

CONTRACT TEST:
Tests public domain operations and invariants.

INTEGRATION TEST:
Tests interactions with dependent systems.

PERSISTENCE TEST:
Tests saving, loading and migration.

SIMULATION TEST:
Tests behavior over multiple simulated days or years.

PERFORMANCE TEST:
Tests cost under increasing population size.

No system is declared complete merely because its source
files compile.

## 16. Benchmark Strategy

Benchmark A:
Single household and basic lifecycle.

Benchmark B:
One settlement with production and consumption.

Benchmark C:
Three settlements and an independent lordship.

Benchmark D:
Several houses with competing interests.

Benchmark E:
A regional political and economic simulation.

Benchmark F:
Multiple regions and realms.

Benchmark G:
Global Known World simulation.

Each benchmark must preserve earlier regression tests.

## 17. Refactoring Rule

Before a refactor:

1. Identify all affected source files.
2. Identify persistent data dependencies.
3. Establish expected behavior.
4. Add regression tests.
5. Implement the smallest safe change.
6. Verify the results.
7. Delete obsolete code only after replacement is proven.

Obsolete files must be explicitly listed in each batch.

Do not retain dead code solely to preserve historical
development work.

Git history is the archive.

## 18. External Simulation Reuse

External open-source simulation algorithms may be reused
only after evaluating:

- License compatibility
- Code quality
- Data model compatibility
- Runtime performance
- Determinism
- Persistence behavior
- Long-term maintainability

We preserve existing Known World domain data unless a
replacement offers clearly demonstrated advantages.

## 19. Future Batch Acceptance Criteria

Each implementation batch must state:

- Files to create
- Files to replace
- Files to delete
- Source ownership changes
- Persistence changes
- Gameplay changes
- Tests required
- Expected behavior
- Known limitations
- Next architectural dependencies

Changed source files should be delivered in complete form
when requested.

## 20. Next Batch

Batch 20.1:
Simulation contracts and time semantics.

Initial focus:
- Authoritative campaign time
- Update frequency definitions
- Simulation phase ordering
- Domain state ownership
- Action execution contracts
- Event contracts

Preserve existing mechanics until migration is tested.
