# C4.17 B41Activation.java precheck: line-ending-only repair

The user's C4.16 compatibility hotfix used Windows CRLF line endings for `KnownWorldB41Activation.java`, while the original C4.17 staged baseline uses LF. The Java content is identical; exact SHA-256 comparison marks it `UNKNOWN_LOCAL_EDITS`.

This repair modifies **only** `C4_17_STAGE/baseline/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java`. It does NOT change the live Java file, the updated C4.17 code, runtime or resource files, population shards, or Minecraft saves. It verifies the expected unchanged C4.17 staged files and allows **only** a line-ending difference. If your current B41 Java contains any substantive edit, the repair stops and asks for your file.

## Steps

1. Extract this ZIP at `D:\GameOfThronesMod2`, preserving the directory structure. Keep `C4_17_STAGE` from the original C4.17 ZIP intact.
2. Run `./RUN_C4_17_REPAIR_B41_LINE_ENDINGS.bat`. It executes five isolated Python tests, verifies the files, updates the one staged baseline, and runs the original check-only BAT.
3. Confirm all four managed sources show `READY` and the new `KnownWorldB42PhysicalRoutine.java` shows `NEW`.
4. Run `./RUN_C4_17_DENSE_TOWNS_AND_PHYSICAL_DAILY_LIFE.bat`. This guarded installer creates the full rollback snapshot, regenerates derived resources, and performs Gradle `clean build`.

The staged-baseline backup from this fix is saved in `tools/geodata/output/c4_17_safe_snapshots/compatibility_stages/<timestamp>/` with a manifest. The C4.17 **full source/resource snapshot** is created separately when the original installer runs.

If verification fails, **do not** force installation. Upload the actual local `KnownWorldB41Activation.java` so the substantive changes can be merged explicitly.

No older BAT scripts or population regeneration are needed.
