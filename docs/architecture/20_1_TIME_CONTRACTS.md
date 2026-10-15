
# Known World — Batch 20.1 Time Contracts

Version: 1.0
Introduced: Batch 20.1
Status: Contracts introduced; scheduler integration pending

## 1. Purpose

Define consistent time terminology, scheduling contracts
and execution rules for Known World's future simulation.

This batch deliberately preserves existing behavior.

## 2. Current Authoritative Objects

SimulationClock:
- Owns the monotonically increasing simulation tick.
- Defines 24,000 ticks per routine day.
- Supports advancing by multiple ticks.
- Is persisted by existing simulation persistence.

CampaignCalendar:
- Owns the historical campaign date.
- Defaults to 365 days per year.
- Defaults to year 298.
- Defaults to 1,200 simulation ticks per campaign day.
- Is persisted separately.

Neither object is replaced in Batch 20.1.

## 3. Current Time Conversion

At default settings:

1 routine day = 24,000 simulation ticks.

1 campaign day = 1,200 simulation ticks.

Therefore:

1 routine day = 20 campaign days.

This relationship is configurable because the campaign
calendar permits its tick duration to change.

The conversion must never be hardcoded in future
simulation engines.

## 4. Current Live Update Order

The existing NpcSimulation.tick() performs:

1. Simulation clock advancement
2. Campaign calendar advancement
3. Lifecycle update if a campaign day advanced
4. Periodic NPC activation
5. Needs update
6. Decision update
7. Routine update
8. Action processing

Periodic NPC work currently runs every 20 ticks.

Batch 20.1 does not change that order.

## 5. New Contracts

SimulationTimeContract:
Immutable read-only snapshot of current time.

SimulationPhase:
Defines the intended domain execution categories.

SimulationSchedule:
Defines when a domain operation is eligible to run.

These do not themselves execute simulation mechanics.

## 6. Campaign Day Boundary Semantics

An update scheduled for a campaign-day boundary must
receive an explicit indication that a day advanced.

A calendar snapshot with remainder zero is insufficient.

For example, a newly initialized calendar may have
remainder zero without having advanced any time.

## 7. Fast-Forward Semantics

Existing accelerated simulation advances:

- The simulation clock
- The historical campaign calendar
- Lifecycle mechanics
- Abstract social simulation

It does not execute every subsystem in the ordinary
NPC tick loop.

Batch 20.2 must investigate and test equivalence between
live and accelerated progression.

Future systems should expose common domain operations
that can be called by either execution mode.

## 8. Deterministic Scheduling

Future scheduling must use explicit:

- Time units
- Frequencies
- Phase ordering
- Execution boundaries
- Randomness policies
- Mutation ownership

Simulation frequency is not the same as simulation
fidelity.

A distant important character may need daily political
decisions without physical entity updates.

## 9. Known Scheduling Risks

- Different systems may interpret a day differently.
- Fast-forward skips some live simulation operations.
- Changing campaign-day duration can alter ongoing
  simulation behavior.
- Reordering lifecycle events may alter succession.
- Future economic production and consumption need
  carefully ordered execution.
- Domain events may require same-day immediate handling.
- Aggregated population progression requires distinct
  fidelity and consistency rules.

## 10. Migration Policy

Do not change existing save formats in this batch.

Do not change the existing CampaignCalendar defaults.

Do not replace SimulationClock.

Do not replace the NpcSimulation.tick() implementation
until equivalent behavior is covered by regression tests.

Future scheduler adoption must include:

1. Existing-behavior baseline tests
2. New scheduler unit tests
3. Save/load compatibility tests
4. Live versus accelerated simulation tests
5. Explicit phase integration
6. A migration or rollback plan

## 11. Remaining Design Decisions

Decision A:
Keep the two clocks or consolidate historical time
into a single canonical timeline with derived views.

Decision B:
How fast should campaign years advance during
normal gameplay?

Decision C:
How should time pass when no player is online?

Decision D:
Should historical and routine progression be
independently pausable or configurable?

Decision E:
What consistency guarantees should fast-forward
provide compared with live simulation?

These decisions must be resolved before major changes
to the central scheduler.

## 12. Acceptance Criteria

Batch 20.1 is accepted when:

- All new Java sources compile.
- Standalone time contract tests pass.
- Existing Gradle build still succeeds.
- No current clock values change unexpectedly.
- Existing saves remain readable.
- No obsolete code is introduced by replacement.

## 13. Next Steps

Batch 20.2:
Determinism, persistence and testing architecture.

Before extensive scheduler replacement:
Resolve open time-design decisions and establish
a baseline for live/fast-forward consistency.
