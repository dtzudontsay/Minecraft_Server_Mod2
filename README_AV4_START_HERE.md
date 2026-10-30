# Known World — AV4A playable decisions and AV1–AV4 test checkpoint

This ZIP was prepared from verified GitHub `master` f40215b5 (2026-10-29), with **AV3 installed locally as a required prerequisite**. Do not apply AV4 before successfully compiling AV3. None of the prior AV batches should be uncompiled or blocked.

## Install

Extract into `D:\GameOfThronesMod2` (the folder containing `gradlew.bat`).

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV4_CHECK_ONLY.bat
.\RUN_AV4_INSTALL_AND_TEST.bat
.\RUN_AV4_CHECKPOINT_BUILD.bat
```

The check-only step makes no changes. The installer backs up all three modified Java source files under `.git/av4_backups/`, refuses unexpected existing staged files or changed anchors, and rolls back its changes if the full Fabric `compileJava` fails.

## What is now real

During genuine free time, physically projected worldwide B4 characters evaluate three options based on their AV3/AV2A/AV2D/AV2B effective profiles: household, public leisure or nearby resident visit. This **changes which existing pathing target they attempt**. K-key chat receives the latest evaluated AV4 intent but must not claim it was completed. It does not invent jobs, meetings, memories, friendships, wages, production or combat.

New commands: `/kwdecisions status`, `/kwdecisions preview "<b4-person-id>"`, `/kwdecisions person "<b4-person-id>"`.

## Test checkpoint after installation

See `docs/architecture/24_37_AV4A_FREE_TIME_DECISIONS_AND_ACCEPTANCE.md` for complete command sequence, backup instructions, expected outputs, red flags, optional local Ollama setup, and explicit pass/fail gates.

Do **not** enable an LLM/API to test mechanical decisions. K-key speech via existing local Ollama is optional; it cannot enact changes.

## Files

- `AV4_STAGE/src/main/java/.../KnownWorldAv4DecisionRules.java`: pure testable decision model
- `AV4_STAGE/src/main/java/.../KnownWorldAv4Decisions.java`: guarded B4 adapter
- `AV4_STAGE/docs/architecture/`: source audit, 761-row CSV and updated XLSX workbook
- `AV4_STAGE/tools/av4/`: source and JDK21 smoke tests
- `tools/av4/av4_guarded_install.py`: guarded source installation

No game save or Git remote is modified by this ZIP itself.

The final checkpoint BAT repeats AV4 source checks, compiles all Java and runs Gradle unit tests. If your repository has pre-existing unrelated unit-test failures, report them separately rather than treating them as an AV4 pass.
