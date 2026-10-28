# C4.16 / B4.2A — Compatibility repair for your uploaded local Java files

This package is tailored to the TWO source files you uploaded after C4.16 reported `UNKNOWN_LOCAL_EDITS`.

## Exact cause (verified against the old C4.16 ZIP)

- `KnownWorldB41Activation.java`: *identical Java text* to the expected baseline, except Windows CRLF line endings instead of LF. It retains B4.1B integration and B4.2 hooks.
- `KnownWorldB42DailyLife.java`: identical code to expected baseline, except the existing B4.2 clock fix is expanded across several lines. It uses `NpcSimulation.getNullable().campaignCalendar().tickRemainder()`, which is correct for your Minecraft version. There are no missing gameplay changes in your local files.

The C4.16 installer checks byte-level SHA-256 equality, so it correctly refused to proceed, even though the differences are harmless formatting changes.

## Installation (do NOT copy source files by hand)

1. Keep your **original C4.16 ZIP contents** extracted at `D:\GameOfThronesMod2`, including `C4_16_STAGE`, and keep Minecraft closed.
2. Extract this compatibility ZIP into the **same project root**. It creates `C4_16_COMPAT_PAYLOAD`, a Python validator, and a BAT. **It does not overwrite any live Java source files or the C4.16 stage on extraction.**
3. Run in PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_C4_16_FIX_LOCAL_COMPATIBILITY.bat
```

4. The BAT verifies **exact SHA-256 checksums** for your two actual Java files and both original C4.16 staged baseline/updated versions. It saves copies of the four staged files, replaces only those four staged files with compatibility-adjusted versions, and runs the original C4.16 installer precheck. You should see `READY` for all four managed sources.
5. Then run the actual original C4.16 installer:

```powershell
.\RUN_C4_16_ORGANIC_TOWNS_AND_WORK_ATTENDANCE.bat
.\gradlew.bat clean build
```

C4.16 itself will still create its normal timestamped **source + generated-resource backup** BEFORE installing live Java and regenerating tiles. The original safety guard remains active. All B4.0 identifiers and counts, B4.1B dialogue state, Minecraft saves and Winterfell V3 are untouched by this compatibility repair.

## Failure and rollback

If your live Java files have changed since you uploaded them, or the staged C4.16 files differ from the original ZIP, the compatibility repair **stops rather than forcing a merge**. Send the output; do not overwrite Java files manually.

The compatibility stage's backups are kept under:

`tools\geodata\output\c4_16_safe_snapshots\compatibility_stages\<timestamp>`

Original C4.16's **normal** source/resource rollback snapshots remain under `tools\geodata\output\c4_16_safe_snapshots\<timestamp>`. Follow the original C4.16 README for rollback of its actual code/resources.

Changes to roads/buildings require a **new test world / ungenerated chunks**. NPC changes and clock phase do not.

## Verification scope

The patch is checked against the two uploaded files and original C4.16 ZIP; Python regression tests exercise exact matching, snapshot creation, installer compatibility and refusal of unknown edits. Full Fabric build and NPC pathing are still verified on your computer.
