
# Known World - Batch 20.2

## Determinism, Persistence and Testing

Version: 1.0
Date: 2026-10-15

## 1. Purpose

Establish contracts and development tools for
reproducible world simulation and safer persistence.

This batch does not change existing runtime behavior.

## 2. Confirmed Time Decisions

Campaign speed:
Configurable.

Offline progression:
Paused. No wall-clock catch-up on server restart.

Pause controls:
Historical and physical progression must be
independently controllable.

These are target behavior requirements.
The controls are not implemented in this batch.

## 3. Deterministic Randomness

SimulationRandom defines algorithm version 1.

Random streams are derived from:

- World simulation seed
- Simulation domain
- Stable entity ID
- Campaign day
- Operation key

The same complete context must derive the same stream.

Different independent operations should use different
operation keys.

Iteration over unordered collections must not determine
random stream ownership.

All future authoritative randomness must follow an
explicit reproducibility policy.

## 4. Randomness Limitations

Existing services have not yet been migrated to
SimulationRandom.

Exact reproducibility is not yet guaranteed for the
complete live simulation.

A deterministic random stream does not by itself
guarantee deterministic results if:

- Entity iteration is unstable
- Update order varies
- Floating-point reduction order varies
- Operations use wall-clock time
- External input changes
- Existing random generators remain uncontrolled

The random seed and algorithm version must eventually
be included in authoritative persistence.

## 5. State Fingerprints

SimulationStateFingerprint hashes ordered canonical
state records.

It is intended for replay comparisons.

A domain canonicalizer must specify:

- Included authoritative fields
- Stable entity ordering
- Numeric formatting
- Null representation
- String encoding
- State format version

Fingerprint matches are useful evidence of replay
consistency but not a proof of behavioral correctness.

## 6. Existing Persistence Architecture

NpcPersistence currently uses versioned TSV storage,
including an NPC save header.

NpcPersistence uses a temporary file followed by
a replacement move for its main save file.

CampaignCalendarPersistence currently writes a separate
campaign_calendar.tsv file.

Other simulation domains have additional persistence
classes and files.

The complete multi-file save is not yet guaranteed to
commit atomically as one consistent snapshot.

## 7. Save Safety Requirements

Future persistence work should implement:

- Stable snapshot identity
- Consistent cross-domain save boundaries
- Explicit schema versions
- Referential integrity verification
- Checksums or equivalent corruption detection
- Migration for existing saves
- Recovery behavior after interrupted saves
- Save/load regression testing
- Backups before migration

Existing save formats must remain readable during
the transition.

Do not delete or overwrite existing saves as part
of a migration without explicit backup and testing.

## 8. Save Auditor

tools/architecture/audit_save_files.py

The auditor:

- Lists saved files
- Computes SHA-256 checksums
- Checks selected known file headers
- Reports temporary files
- Outputs a JSON manifest

It does not modify saved data.

It does not verify relational or historical correctness.

Run it against a stopped server to avoid mixed snapshots.

## 9. Testing

The standalone Java test class verifies:

- Repeated random contexts produce repeated results
- Different entity contexts derive different seeds
- Different operation keys derive different seeds
- Different campaign days derive different seeds
- Context objects derive the expected stream
- Canonical fingerprints are reproducible
- Changed records affect fingerprints
- Invalid contexts are rejected

These are contract tests, not full simulation tests.

## 10. Migration Plan

Stage A:
Introduce deterministic utilities and baseline tests.

Stage B:
Identify every authoritative use of randomness.

Stage C:
Choose stable world seed and persistence representation.

Stage D:
Migrate one simulation service at a time.

Stage E:
Add canonical state snapshots and replay tests.

Stage F:
Add consistent multi-domain save transactions.

Stage G:
Compare live and accelerated simulation outcomes.

No existing simulation behavior should be changed
without a documented migration and regression test.

## 11. Outstanding Questions

- Exact normal-gameplay campaign speed.
- Whether the selected speed can be changed mid-campaign.
- Whether pausing historical time also pauses economic work.
- How physical actions interact with paused campaign time.
- The acceptable replay equivalence for population cohorts.

These must be decided before activating the new scheduler.

## 12. Acceptance Criteria

Batch 20.2 is complete when:

1. New Java classes compile.
2. Standalone determinism tests pass.
3. Existing Gradle build succeeds.
4. Save auditor can inspect a stopped test-world save.
5. No existing save files are modified.
6. Existing NPC gameplay remains unchanged.

## 13. Next Batch

Batch 20.3:
Households, Population and Economy Domain Design.

Focus:

- Population representation
- Households
- Resource ownership
- Production and consumption
- Economic accounting
- Entity and settlement relationships
- Contract definitions
- Testable invariants

The initial economy implementation must use the
existing Known World character and settlement models
rather than creating incompatible parallel identities.
