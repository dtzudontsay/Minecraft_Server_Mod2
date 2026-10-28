# B4.0 census command diagnostic hotfix

This replaces **one** B4.0 command class and adds an **offline read-only verifier**. It does not regenerate or modify the 3,760,684 generated people, the C4.11 construction resources, Winterfell V3, your NPCs or a savegame.

## Why

Minecraft displays `An unexpected error occurred while trying to execute that command` whenever a registered command throws. The B4.0 `/kwpopulation status` and `/kwpopulation site ...` handlers previously called `index()` and `preview()` without guarding exceptions. The precise error is therefore invisible in chat. This is a verified error-reporting defect, **not proof** of the underlying data error; the crash stack trace is needed to identify that error.

## Install

1. Extract this zip **into `D:\GameOfThronesMod2`**, preserving paths. Overwrite `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB40PopulationIndex.java`.
2. Run `RUN_B4_0_COMMAND_DIAGNOSTICS.bat` from the repo root (PowerShell: `.\RUN_B4_0_COMMAND_DIAGNOSTICS.bat`). It checks the complete census manifest, checks existence and the first gzip record of every inhabited site, and runs 5 offline tests. It does **not** rewrite the generated roster.
3. Run `.\gradlew.bat clean build`, then IntelliJ `runClient`, **restart** the game.
4. Run `/kwpopulation diagnose`, then `/kwpopulation status`, then `/kwpopulation site site_ne_001`.

If `diagnose` itself fails, the updated code now catches the exception and prints its root cause. Copy the chat error and the `[B4.0]` stack trace from `D:\GameOfThronesMod2\run\logs\latest.log` (or whichever directory your runClient uses).

## Additional validation

Full record check (reads all ~3.76m records, considerably slower):

```powershell
py -3 tools\geodata\b4_0_validate_runtime.py --deep
```

The fast validator checks the original 248-site totals, filenames and shard headers. It cannot certify the full file contents or gameplay integration. Successful census inspection **does not** mean the 3.76m generated people are active villagers or AI agents; B4.0 remains a baseline, with activation as follow-up work.

## Scope

- Pure defensive handling and logs on B4.0 commands; existing commands keep their names and outputs when successful.
- New `/kwpopulation diagnose` command prints classpath manifest presence and calls the same population validation.
- No modifications to `KnownWorld.java` registration hook necessary. **Do not rerun B4.0 population generation** just to install this hotfix.

Python validator tests: 5 passed. Java source stub-compilation: passed with Java 21. Actual Fabric/Gradle compilation and world runtime are unverified here.
