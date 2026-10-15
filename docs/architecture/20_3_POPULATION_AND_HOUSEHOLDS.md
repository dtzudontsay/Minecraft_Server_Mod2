
# Known World - Population and Household Architecture

Batch: 20.3
Status: Domain design
Scenario baseline: 298 AC

## 1. Objective

Define a scalable population simulation that supports
both persistent individual NPCs and abstract population.

The system must eventually represent the population
of Westeros, Essos and the wider Known World.

It must not require a Minecraft entity for every person.

This document defines contracts for future implementation.
It does not claim these components already exist.

## 2. Core Principle

Every simulated person must be accounted for exactly once
within the authoritative population model.

Important individuals may have permanent NpcId identities.

Ordinary population may remain aggregated in cohorts.

An individual represented by an NpcId must not also
be counted as a member of an aggregate cohort.

## 3. Existing Integration Points

Existing systems to preserve:

- NpcId
- NpcRegistry
- NpcState
- NpcLifeState
- SettlementId
- SettlementManager
- GenealogyManager
- MarriageManager
- LifeCycleService
- CharacterGenerationService
- DynastyManager
- LandedHoldingManager
- OrganizationManager
- NpcAffiliationManager

The existing IDs remain authoritative.

New systems must refer to these IDs rather than
introducing equivalent replacement identities.

Existing settlement and holding population-related
values must be assessed before migration.

Population weights are not automatically population counts.

## 4. Individual Population

An individual is a persistent person with an NpcId.

Individual state may include:

- Life state
- Biological age or birth date
- Culture and religion
- Sex and demographic attributes
- Household membership
- Current residence
- Economic role
- Employment
- Property interests
- Family connections
- Skills and personality
- Health and wellbeing

Existing managers should retain ownership of the
fields they already maintain.

A population manager must not become a duplicate
character profile or genealogy database.

## 5. Population Cohorts

A cohort represents an explicitly counted group
of non-individually instantiated people.

Proposed cohort dimensions:

- Settlement or territorial location
- Age band
- Sex or relevant demographic category
- Social and legal status
- Culture
- Religion
- Broad economic occupation
- Household status where needed

The final dimensions must be configurable and sparse.

Not every possible combination should create a cohort.

Core cohort fields:

- Cohort identity
- Location identity
- Population count
- Demographic composition
- Relevant economic participation
- Last processed campaign day
- Schema version

Population counts use integer quantities.

Negative or fractional people are forbidden.

## 6. Materialization

Materialization converts part of an aggregate population
into one or more persistent individual NPCs.

Required operation:

materialize(cohort, numberOfPeople, context)

Preconditions:

- Cohort exists.
- Population count is sufficient.
- Materialization context is valid.
- Stable NPC identities can be allocated.

Postconditions:

- Cohort count decreases by the exact number.
- Exactly that many individuals are created.
- New NPCs receive valid demographic attributes.
- Household and settlement relationships are consistent.
- Economic assets are transferred or allocated explicitly.
- Historical origin remains traceable.

If the operation fails, partial materialization must
not leave duplicated people.

## 7. Abstraction

Abstraction converts eligible individual NPCs into
aggregate population.

Required operation:

abstract(individuals, targetCohort, context)

Preconditions:

- The individuals are eligible for abstraction.
- No unresolved individual-only obligations exist.
- Ownership transfers can be resolved.
- Family and historical continuity rules are respected.

Postconditions:

- Aggregate count increases by the exact number.
- Individual representation is retired or archived.
- Assets and obligations are conserved.
- Active references remain valid.
- No person is counted twice.

Important named characters should normally remain
persistent individuals.

Abstraction must not silently destroy identity,
relationships, property or historical records.

## 8. Household Model

A household is a social and economic unit.

It is not synonymous with:

- Biological family
- Dynasty
- Political house
- Settlement
- Organization
- Building

Proposed state:

HouseholdId
ResidenceSettlementId
HouseholdType
Memberships
Economic decision-maker
Dependents
Assets
Obligations
Last processed campaign day

Potential household types:

- Peasant household
- Artisan household
- Merchant household
- Noble residence
- Military household
- Religious household
- Collective institution
- Other culture-specific forms

These are initial categories, not a closed list.

## 9. Household Membership

A person may belong to at most one primary
consumption household at a time.

Secondary affiliations may exist.

Memberships may specify:

- Head or decision-maker
- Adult member
- Dependent
- Servant
- Retainer
- Guest
- Other culturally defined status

Household membership does not modify biological
genealogy or dynastic membership.

Future co-residence and household membership must
be modeled separately when necessary.

## 10. Population Conservation

The total population in a region equals:

Persistent individual residents
+
Aggregated cohort residents

Population changes only through explicitly recorded:

- Births
- Deaths
- Inward migration
- Outward migration
- Materialization
- Abstraction
- Administrative boundary changes

Materialization and abstraction do not change the
underlying total population.

Migration changes location counts but not the
overall world population.

Births and deaths change the total population.

## 11. Demographic Progression

A campaign-day scheduler should control demographic
advancement.

Demographic simulation includes:

- Age progression
- Births
- Mortality
- Household formation
- Migration
- Labor participation
- Social mobility

Aggregate births and deaths may use statistical
models with deterministic random streams.

Long-running simulation must not require iteration
over millions of individual NPC objects.

## 12. Household Economy

Each household may:

- Own resources
- Consume resources
- Earn income
- Pay taxes and rents
- Owe debts
- Employ or supply labor
- Transfer property
- Support dependents

Household economic actions use the same authoritative
resource accounting operations as markets and rulers.

## 13. Scenario Initialization

Existing authored characters and NPC packs must
retain their identities.

Historical population estimates should be introduced
as explicitly sourced, uncertain scenario data.

No arbitrary numeric holding weight may be silently
reinterpreted as a population count.

When historical population values are uncertain,
scenario configuration must represent that uncertainty.

## 14. Save and Load

Required properties:

- Versioned cohort and household storage
- Stable IDs
- Referential validation
- Conservation checks
- Save/load round-trip tests
- Safe migration from previous world saves

A missing future population save must not cause the
system to invent people repeatedly on every restart.

## 15. Performance

The system must support:

- Individual simulation for important characters
- Cohort-based background progression
- On-demand materialization
- Sparse demographic categories
- Incremental updates
- Deterministic parallelizable workloads

Actual performance budgets require benchmark results.

## 16. Initial Vertical Slice

Target:

One small lordship with three settlements.

Each settlement has:

- Persistent important individuals
- Abstract commoner population
- Households
- Basic food requirements
- Basic occupations
- Resource inventories

The first benchmark must verify:

- Every person counted once
- Births and deaths update population
- Migration conserves global population
- Household consumption affects inventories
- Save/load preserves all counts
- Ten-year progression remains consistent

## 17. Acceptance Criteria for Implementation

Population domain core completion requires:

- Working cohort manager
- Working household manager
- Validated population transitions
- Materialization/abstraction tests
- Lifecycle integration
- Persistence
- Deterministic simulation
- Economic integration
- Scale benchmarks

No implementation is declared complete until
these criteria are verified.

## 18. Open Design Questions

- Exact cohort dimension schema
- Population estimate sources and uncertainty
- Which individuals may be abstracted
- Culture-specific household structures
- Biological versus administrative residence
- Frequency of cohort progression

These are implementation design gates for later batches.
