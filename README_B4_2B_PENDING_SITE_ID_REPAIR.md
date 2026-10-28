# B4.2B.1 — Safe pending-index recovery for nonnumeric site IDs

## Why this repair is necessary

Your `RUN_B4_2B_FAST_MOBILITY_FIX.bat` stopped at:

    Unknown data in pending output; refusing to move: ...\world_mobility_b4_2b_v1.pending\site_nw_missing_twins_01

The previous recovery validator only recognized directory names like `site_nw_001`. But the *actual* B4.1A.1 projection manifest contains additional kinds of settlement site IDs. The original interrupted mobility generator created its output folder directly from those source site IDs.

This repair replaces that incorrect naming assumption with **authoritative site-ID validation** against `world_projection_b4_1a1_v1/manifest.json` (248 records). It still refuses arbitrary/unrecognized data, unexpected files, symlinks and Windows junctions. It does not weaken the generator's SHA-256 safety checks.

## How to use it

1. Exit any previous batch shell prompt.
2. Extract this ZIP to `D:\GameOfThronesMod2`, preserving directories and **replacing** `tools\geodata\b4_2b_fast_mobility_hotfix.py` and `RUN_B4_2B_FAST_MOBILITY_FIX.bat`. It also restores the identical, hash-checked mobility payload and original tests and adds one new test file.
3. Optional read-only check: `py -3 tools\geodata\b4_2b_fast_mobility_hotfix.py --check`.
4. Run: ` .\RUN_B4_2B_FAST_MOBILITY_FIX.bat`.
5. If successful, run: ` .\RUN_B4_2B_CHECK_ONLY.bat`.
6. Only when the source precheck is clear, run: ` .\RUN_B4_2B_POSITION_DISCOVERY_ECONOMY_FOUNDATION.bat`.

**Do not manually remove or rename** `.pending`, the unusual site directory, or the original generator. Do not rerun old B4.0/B4.3A installers. Do not force installation through another `UNKNOWN_LOCAL_EDITS` result.

## What this hotfix changes

- Modifies only the previous *hotfix's* recovery validator and test runner (plus test/payload copies). On its successful run, that validator backs up and replaces only the B4.2B Python mobility generator after exact SHA checks.
- Validates every directory in the interrupted `.pending` index against the existing 248-site projection manifest. Handles names such as `site_nw_missing_twins_01` if they actually appear in that manifest.
- Archives the entire incomplete index directory by moving it intact to:
  `tools\geodata\output\b4_2b_safe_snapshots\generator_hotfixes\<timestamp>\incomplete_previous_index`.
- Keeps the original generator byte-for-byte in the same snapshot. The original B4.2B installer remains responsible for Java/source/resource snapshots, Gradle build and rollback.
- Does **not** edit B4.0 population shards, B4.1 projection resources, household identities, economic balances or Minecraft saves.

## If a safety check still stops

Keep the files unchanged and send the new error. An unexpected file or a site ID absent from the projection manifest is a genuine mismatch and requires inspection. We are intentionally not deleting the pending directory or bypassing checks.

## Verification on packaged sources

- 6 new regression tests covering nonnumeric site IDs, byte-preserving archive/rollback, corrupted manifests and unknown data: passed.
- 8 previous mobility-hotfix tests: passed.
- 10 original B4.2B.1 tests, including simulated generator failure and source/resource rollback: passed.
- Actual generation over 3.76 million residents and full Fabric build still require your local run.
