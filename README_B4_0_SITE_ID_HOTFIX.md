# B4.0 — Valid recovered settlement ID hotfix (2026-10-23)

## Root cause

The previous B4.0 runtime validator AND the Minecraft Java population inspector
incorrectly require `site_` + alphabetic region + exactly 3 decimal digits,
e.g. `site_nw_029`. But the project's canonical B2/B3 site registry also
contains descriptive IDs such as `site_nw_missing_twins_01`.

- Python failure: `ValueError: Duplicate/malformed site id 'site_nw_missing_twins_01'`.
- Java failure: `IllegalStateException: Unsafe population resource ID: site_nw_missing_twins_01`.

This means the **validator code is wrong**, not necessarily the generated data.

## Install

1. Extract this ZIP into `D:\GameOfThronesMod2`, preserving folders.
2. Execute `RUN_B4_0_FIX_SITE_IDS.bat` from the repo root.
3. The installer patches *only* the two regex expressions already installed in:
   - `tools/geodata/b4_0_validate_runtime.py`
   - `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB40PopulationIndex.java`
4. Run `./gradlew.bat clean build` and start `runClient`.
5. Test `/kwpopulation diagnose`, `/kwpopulation status`, and `/kwpopulation site site_nw_missing_twins_01`.

The BAT also runs tests and scans the existing resident shard headers.
If it reports a new error, report its FIRST traceback/line. The old B4.0
regeneration BAT is **not** needed.

## ID rules

The new regular expressions accept sequences of lower-case letters/digits
separated by single underscores after `site_`, including descriptive tokens.
Paths, dots, uppercase letters, spaces and adjacent underscores remain denied.
A shard's file name must still be exactly `{siteId}.jsonl.gz`.

## Safety

- Existing 3,760,684 resident baseline is **not regenerated**.
- No individual identity, household, job count, map marker or construction is modified.
- No changes to world saves or GitHub; user commits locally.
- Backups are saved alongside each edited Python/Java file.
- The patch is idempotent: running the BAT again leaves corrected code untouched.
- The command diagnostics from the previous B4.0 hotfix are preserved.
- Full Fabric/Gradle game compilation must be run on the user's own setup.
