# Known World — AV2B: Shared Psychology Rules for Legacy NPCs and Worldwide B4

**Prepared against the publicly visible repository master `964baba0e02bcd9cd9f688e701a7682c2a5ecab0` (October 29, 2026) and the supplied, locally installed AV2A package.** The local AV2A installation need not be pushed yet, but **must have compiled successfully**. Assistant never pushes to GitHub or edits saves.

## Install

Extract this archive into `D:\GameOfThronesMod2` (the folder containing `gradlew.bat`). Keep the `AV2B_STAGE` and `tools\av2b` directories in place.

From PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV2B_CHECK_ONLY.bat
.\RUN_AV2B_INSTALL_AND_TEST.bat
```

The check-only command never alters source or saves; it verifies the AV1/AV2A prerequisites, current B41 + legacy hooks, 761 original Excel rows, all 706 definitions, the 12 enum constants and a Java 21 formula + adapter compilation smoke test.

The guarded installer makes **three narrow changes** to existing Java sources:

1. `src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyService.java`: `derivedTendencies(NpcId)` delegates to the shared stateless evaluator instead of maintaining a second copy of its formulas.
2. `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java`: registers AV2B and computes derived tendencies after the existing AV2A overlay, for both right-click and K-key views.
3. `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java`: includes the bounded derived-tendencies snapshot in the existing LLM context, without promoting any speech to world truth.

It **adds** the following source, diagnostics and documentation:

- `src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyRules.java`
- `src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2BPsychologyBridge.java`
- `docs/architecture/24_33_AV2B_SHARED_PSYCHOLOGY_BRIDGE.md`
- `docs/architecture/AV2B_ATTRIBUTE_COVERAGE.csv` (every original AV1 spreadsheet row, canonical identifiers unchanged)
- `tools/av2b/test_av2b_shared_psychology.py`
- `tools/av2b/test_av2b_java_smoke.py`

The installer saves the **unmodified original source bytes** in `.git\av2b_backups\<timestamp>` before installing and restores source on any failure. Source modifications already made by AV1 or AV2A are preserved. It runs the real project's `compileJava --no-daemon` and **does not claim success until that Gradle command returns zero**. A disposable world is still needed for in-game acceptance.

## What actually changes in gameplay

- Visible worldwide B4 residents now have a derived psychological snapshot using the same 12 rules as authored/legacy `NpcRegistry` characters: deceitful advancement, intervention, impulsive confrontation, peer conformity, risk taking, mercy, vengeance, loyal duty, political assertiveness, religious receptiveness, cultural openness and manipulation susceptibility.
- The same calculated values are visible in character inspection JSON and provided as estimated *character tone/context* to K-key Ollama conversations, not as personal memories or actions.
- AV2A persistent changes to a character's typed values/dispositions can change these derived scores without duplicating or overwriting the original 298 AC identity baseline.
- The B4 personality 0..1 score format is adapted to the signed legacy `NpcPersonality` axes before calculation; no extra stat is stored.

**Explicitly not yet implemented:** autonomous decision selection, global personality growth from world events, NPC friendships/rivalries from estimated tendencies, newborn population integration, real-world economic transactions or event propagation. Those require later AV2C–AV12 batches.

## Optional grouped Minecraft test (later)

```text
/kwpsych status
/kwactors nearby
/kwprofile status
```

Right-click an ordinary worldwide NPC to view `derivedTendencies`, `socialSusceptibility`, `persuasionResistance`, and `av2bPsychology`. Use the verified B4 person ID from their inspection or command output in a disposable save:

```text
/kwprofile set "b4:<real-site-id>:<real-person-suffix>" DISPOSITION FORGIVENESS 1.0
```

Close/reopen the inspector and verify that the `mercy` readout changes; verify it remains changed after restarting the world. K-chat should read the same profile values while avoiding invented memories. Reset with `/kwprofile clear` in the test world.

## Next

**AV2C:** objective events and consistent change provenance for worldwide profiles. **AV3:** generation, distributions and upbringing. **AV4:** validated choices that use psychological scores. The AV2B coverage file retains all 706 definitions; not all are activated by the 12 shared formulas.
