# Known World — AV2D Validated Attribute Changes & Reconciliation

**Baseline:** GitHub master `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6` + installed AV2B v1.1 and AV2C. GitHub only contains AV2A as of this audit; do NOT try AV2D before AV2C has compiled.

## Install

Extract all ZIP contents into your repository root (`D:\GameOfThronesMod2`), keeping folder names. The scripts must be beside `gradlew.bat`.

In PowerShell:

```
cd D:\GameOfThronesMod2
.\RUN_AV2D_CHECK_ONLY.bat
.\RUN_AV2D_INSTALL_AND_TEST.bat
```

Check-only is read-only. Install backs up B41 Java under `.git/av2d_backups/`, patches one existing file, copies two Java sources and docs/tests into their permanent paths, runs `compileJava`, and restores on failure. It does **not** modify game saves, money, jobs or GitHub.

## What it actually implements

- One canonical event-sourced change receipt per verified completed outcome/axis, with deterministic replay guard, a strict 512-receipt per-person ceiling and atomic/checksummed persistence.
- An internal future-producer entry point that demands a real completed-outcome verifier; **no gameplay event producer is connected yet**, so AV2D adds no automatic XP or personality changes in the current game.
- Right-click / K-chat profile reads can include any later verified AV2D event changes, before AV2B's derived psychology calculations.
- Read-only `/kwchanges reconcile` compares AV2C presence evidence with existing B4 work-attendance counts; discrepancies are not auto-fixed.
- All 117 currently writable numeric attribute dimensions and all 706 canonical AV1 definitions remain in their respective systems. Every original Excel row is retained in `AV2D_ATTRIBUTE_COVERAGE.csv`.

## Optional deferred in-game checks

```
/kwchanges status
/kwchanges person "b4:site_ne_001:ACTUAL_PERSON_ID"
/kwchanges reconcile "b4:site_ne_001:ACTUAL_PERSON_ID"
```

Use a **real** B4 person ID, not the literal sample. **Do not infer that workplace attendance earned skill or wages.**

## Caution

AV2D's own per-person writes are atomic, but the B4/AV2C/AV2D systems do **not** share one cross-domain transaction. Existing `.pending` files should be inspected, not blindly deleted. You must still run full `compileJava` on your actual Windows/Fabric tree. Java API stubs cannot substitute for an in-game test.

Full design, limits, migration and future roadmap: `docs/architecture/24_35_AV2D_CHANGE_EVENTS_AND_RECONCILIATION.md`.
