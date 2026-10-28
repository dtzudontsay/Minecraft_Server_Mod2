# B4.3A versus C4.15 generator — guarded staging repair

## Root cause

The original B4.3A stage bundled the **B4.1A.1** `b4_1a1_household_index.py` as its expected baseline. Your current project should have the **C4.15** revision, which adds commute-aware workplace assignment overlays. This is a meaningful upgrade, **not** a newline-only mismatch. C4.16/C4.17 did not revert that generator.

B4.3A needs to change only `'physicalProjectionLimit':128` to `256` in the generated manifest (and keep the generator in sync for future rebuilds). The repair preserves all C4.15 commute features.

## Procedure

1. Extract this ZIP **into the repository root**, leaving `B4_3A_C415_COMPAT` intact. It is separate from `B4_3A_STAGE`.
2. Run `RUN_B4_3A_REPAIR_C415_GENERATOR.bat`.
3. Confirm the *original* `RUN_B4_3A_CHECK_ONLY.bat` now reports three existing sources `READY`, two new Java classes `NEW`, and the projection manifest `READY`.
4. Run `RUN_B4_3A_DOUBLE_NPCS_OFFSCREEN_TRAVEL.bat`. This original installer makes its own rollback snapshot, applies all new files and runs the Gradle build.

**Do not rerun** B4.0, B4.1, B4.2, C4.15, C4.16 or C4.17 installation scripts. **Do not** copy any stage files by hand.

## Safety

This hotfix alters exactly two paths, both under `B4_3A_STAGE`:

- `B4_3A_STAGE/baseline/tools/geodata/b4_1a1_household_index.py`
- `B4_3A_STAGE/updated/tools/geodata/b4_1a1_household_index.py`

It makes a separate backup of their former contents under `tools/geodata/output/b4_3a_compat_stage_snapshots/`. It never touches live `tools/geodata/b4_1a1_household_index.py`, Minecraft saves, original B4.0 population, C4.15 commute assignments or physical town resources. The precheck compares the **entire** live source against the known C4.15 generator (normalizing CRLF only). If it differs for any substantive reason, it stops and asks for the local source for a proper merge.

Once the original B4.3A installer completes, its native rollback (`RUN_B4_3A_ROLLBACK_LATEST.bat`) restores the version of your **live** source and projection manifest from before B4.3A. No need to regenerate the population or town resources.
