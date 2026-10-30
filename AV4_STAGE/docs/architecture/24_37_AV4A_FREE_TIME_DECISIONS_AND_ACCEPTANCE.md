# AV4A — Verified free-time decisions and AV1–AV4 acceptance test

Baseline: GitHub `dtzudontsay/Minecraft_Server_Mod2`, `master` verified `f40215b5fe6e741687f8d07747d0f2ab292fbc0e` at packaging time. AV3 is a **local installation prerequisite** and had not yet been found pushed on this master commit. No source file is overwritten without an anchor check. This is a scoped first AV4 batch, not the whole future CK-style decision model.

## What is activated NOW

The existing B4.2 daily-life pathing routine makes one *bounded* decision for a currently materialized canonical B4 person in a genuine free-time, home/free-time or rest-day schedule phase:

- `HOUSEHOLD_ACTIVITY`: move to the existing deterministic home activity point.
- `PUBLIC_LEISURE`: use the existing, eligible `leisurePlace` from B4.2/C4 and attempt to move there, or fall back to home if there is none.
- `NEIGHBOR_VISIT`: look for an actually loaded nearby B4 actor and attempt to walk toward them; if no candidate is loaded, fall back to home.

The decision is computed deterministically from `personId + campaignDay` (minor tie-break noise) plus **nine** normalized inputs: sociability, empathy, friendship loyalty, urban norm, isolationist norm, and AV2B's `intervention`, `cultural_openness`, `peer_conformity`, `risk_taking` derived tendencies. The nine inputs are validated. `empathy`, friendship loyalty and norms use signed -> unsigned conversion exactly once; AV2B scores are already [0,1]. These are mod tuning weights, not canonical ASOIAF laws.

Profile authority is `B4 identity -> AV3 starting priors -> AV2A saved edits -> AV2D confirmed outcomes -> AV2B derived psychology -> AV4 decision`. The existing B4 work-shift, commute, appointment, adult/child routine windows are **not** overwritten. AV4 changes a target/role given to `KnownWorldB42PhysicalRoutine.steer`, so the choice can have a physical consequence, contingent on loaded chunks, real traversability, a currently projected actor, etc.

The current implementation does **not** store an AV4 decision in save files: cache is bounded to 768 visible people and cleared on server stop. The same person, same day, same effective profile evaluates identically after restart. A change made after the first decision of the same day is reflected on the next day or after restarting Minecraft; the cache intentionally stabilizes the day. Admin `kwprofile` writes remain authoritative and separate.

## Not yet implemented

- Individual travel/relocation, combat, political intrigue, employment selection, production or wages driven by psychology.
- An NPC **successfully meeting** another NPC, changing relationships or making friends. A visit is an intent and navigation attempt only, not a witnessed social event.
- NPC conversation that issues validated actions, skill awards, work receipts or quest consequences. LLM chat is expressive only.
- Current birth-time parent-profile inheritance and upbringing events (AV6), social reaction world consequences (AV5), economy outcome producers (AV8).
- AV4 free-time choices for legacy Winterfell V3's distinct NPC type; the integration is deliberately limited to B4 `kwb41_actor` projections.
- Perfect obedience to a choice: the B42 movement controller and safe worldgen pathing may defer or fail; no teleportation.

## Compiled files and source edits

New:
- `simulation/attributes/KnownWorldAv4DecisionRules.java` pure, JDK21-testable.
- `simulation/attributes/KnownWorldAv4Decisions.java` B4/AV2 adapter, cache, and commands.

Guarded edits:
- `KnownWorldB41Activation.java`: register commands, expose **validated visible** full effective profile to the B4 decision service, show cached AV4 decision in K-dialogue/right-click character sheet.
- `KnownWorldB42DailyLife.java`: select between eligible free-time destinations through AV4; retain old B4 fallback on invalid/unavailable profiles. Does not modify `observedAtWork`, the verified event journal, schedule calculations, or economics.
- `KnownWorldB41DialogueService.java`: include cached AV4 decision intent in K-chat context, with a mandatory warning that an intended visit is not a verified encounter.

All 761 Excel source rows / 706 canonical definitions remain in `AV4_ATTRIBUTE_COVERAGE.csv` and the `.xlsx` workbook (columns AA–AD and an AV4 Summary worksheet). No unrelated value is marked fully implemented.

## Installation and preflight

Prerequisites AV1 v1.1, AV2A, AV2B v1.1, AV2C, AV2D, **AV3** all installed with successful `compileJava`. Work on a clean local Git branch with your authored changes committed. Extract ZIP over repo root, **preserving nested paths**.

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV4_CHECK_ONLY.bat
.\RUN_AV4_INSTALL_AND_TEST.bat
.\gradlew.bat compileJava --no-daemon
.\gradlew.bat test --no-daemon
```

The Java test uses Java 21; ensure the `java` and `javac` on PATH both report 21. The installer makes Git-internal backups and will restore both patched source files on a failed compilation. There is no save migration by AV4.

## AV1–AV4 checkpoint test — FIRST PASS WITHOUT OLLAMA

**Back up** the Minecraft save before testing (with the game/server stopped), and use a copy for administrator mutations. Run on the actual Known World generator/server, not a normal vanilla world. A projected `kwb41_actor` from the worldwide B4 population is needed; Winterfell V3-specific villagers are a *different* subsystem and are not proof AV4 works or fails.

1. Start server/client. Run `/kwattributes status`, `/kwformation status`, `/kwprofile status`, `/kwpsych status`, `/kwexperience status`, `/kwchanges status`, `/kwdecisions status`, `/kwactors nearby`, `/kwdaily status`, `/kwdaily attendance`. All available commands should complete without an exception or a corruption/blocker report. Some commands may say `WAITING` until an eligible world/site loads; note this rather than treating it as a pass.
2. Right-click one **B4 worldwide** projected NPC. Record its full exact `b4:site_...:<id>`, culture, religion, AV3 formation metadata, skills, 12 AV2B derived tendencies, and any AV2A or AV2D state. `/kwprofile inspect "<personId>"` must refer to that same identity. Look for `av3State=APPLIED_STARTING_PRIORS` and `av2bPsychology=SHARED_DERIVED_MODEL_READ_ONLY`.
3. `/kwdecisions preview "<personId>"` evaluates today's free-time preference *without requiring today's schedule to be free time*. This is a preview of what movement will be selected **when the schedule enters a free-time phase**, not proof that the NPC moved. `/kwdecisions person "<personId>"` shows the cached score/action. Right-click the same NPC again and compare `av4FreeTimeDecision` to the command; they must agree.
4. Repeat for several other B4 people, ideally different occupation, culture and household. Record the three scores. Different NPCs may choose the same action; that is not itself a defect. Inspect any distinctive scores.
5. Observe one person during genuine `FREE_TIME`, `HOME_AND_FREE_TIME` or `REST_DAY` per `/kwdaily nearby`. AV4 should influence movement toward a home, a public leisure site or another *currently loaded* nearby projected villager. `kwdaily attendance` path counts can help verify the movement controller was called; travel can still fail because of terrain or unloaded paths. No social relationship change is asserted merely by a visit attempt. Work and commute schedules must still function.
6. On a **throwaway save**, use `/kwprofile set "<personId>" CHARACTER_VALUE FRIENDSHIP_LOYALTY 1.0` or `/kwprofile set "<personId>" DISPOSITION EMPATHY 1.0`; inspect the new field. The AV2B tendency scores may change depending on the axis. A currently cached AV4 action is **stable for the current day**. On the next campaign day (or after restart), `/kwdecisions preview "<personId>"` should use the updated profile. Test `/kwprofile clear "<personId>" CHARACTER_VALUE FRIENDSHIP_LOYALTY` to restore the generated prior if desired. Do not edit a real production save.
7. Shut the server/client down normally, restart on the same backed-up test world, compare the profile and AV4 preview for the same character/day. Persistent AV2A edits should survive; the AV4 preference must recompute deterministically if the profile/day is unchanged. AV2C actual verified evidence persists. AV2D should have **zero or only genuine receipt-authorized** changes.
8. Check `/kwprogress status` and `/kwchanges status`. An AV4 social visit must not have invented production, wages, relationship scores, combat events, training rewards, memories or completed meeting events. Also inspect logs for `AV4`, `B4.2C`, `BLOCKED`, and save/parse exceptions.

**Troubleshooting order:** check-only failure -> correct source contract; Java compile failure -> send first `error:` plus 25 lines; no B4 NPC -> find a C4.11-built/approved population site; AV3 BLOCKED -> inspect source and person ID; AV2B BLOCKED -> inspect AV2A save; movement blocked -> inspect B4/C4 pathing and settlement geometry; Ollama failure -> speech-only and does not invalidate mechanical AV4.

## OPTIONAL LLM test (not necessary for AV1–AV4 mechanics)

AV4 does **not** require an LLM or external API. K-key dialogue already uses a server-side local Ollama HTTP chat endpoint from `KnownWorldB41DialogueService`:

- Default URL: `http://127.0.0.1:11434/api/chat` on the **server host**.
- Default model: `qwen3.5:9b` (as configured by current repository; check `ollama list` locally).
- Override via `KNOWNWORLD_OLLAMA_URL`, `KNOWNWORLD_OLLAMA_MODEL`; configured before the Minecraft/Java server process starts.

If Ollama is installed, use `ollama list`; if the configured model is not installed, obtain the matching model available for your Ollama version (e.g. `ollama pull qwen3.5:9b` if that tag exists). Start Ollama using your normal OS service or `ollama serve`, and check `http://127.0.0.1:11434/api/tags` from the server host. In game approach a B4 projected NPC, press **K**, open chat and ask about their profession, customs or recent conversations. Check that AV3/AV2B profile context is stable and that the LLM **does not claim** it actually did the movement decision or completed trade/social actions merely by speaking.

The LLM isn't required for status commands, AV4 choices, character inspection, persistence, or movement. It cannot create authoritative relationships or economics in this stage.

## Acceptance gates

PASS mechanical AV4: successful local Gradle compile, `kwdecisions preview` for a valid visible ID returns valid action and bounded scores, right-click sees the same cached action, two deliberate profiles differ in score, observed free-time routing changes without changing work periods, no new saves, no invented events.

PASS persistence: AV2A change survives restart, no AV2C/AV2D corruption, generated AV3 initial priors unchanged after restart.

Partial/blocked: if only compilation passed, label **COMPILED / NOT PLAYTESTED**; if only command previews passed, label **BEHAVIOR DECISION INSPECTED / NAVIGATION NOT PROVEN**. Do not imply general autonomous relationships, political decisions, or economic agency until the respective domain passes its own integration test.

## Next work after this checkpoint

AV4B: general goal/available-action scoring and guarded execution (work, rest, social interaction). AV5 relationship consequences and verified social event causality. AV6 family inheritance, mentorship and child development. AV7 culture, religion and language behavior. AV8 economy. All 706 canonical definitions and 761 source rows remain tracked and will not all be treated as activated by this one bounded AV4 batch.
