# AV4B — General Autonomous Goals & Action Selection

## Authority and scope

This is the next AV roadmap stage after AV4A and before AV5. AV4A character-state persistence was playtested in a local save/restart (EMPATHY); physical AV4A navigation remains unverified. AV4B is scoped to currently projected B4.0 worldwide persons. It does not replace Winterfell V3's independent legacy NPCs, does not regenerate any B4 IDs, does not touch world saves, and is not an offscreen 3.76-million-person per-tick AI.

**Read-only repository baseline:** `dtzudontsay/Minecraft_Server_Mod2` master at packaging time; the installer intentionally uses narrow source anchors instead of overwriting locally patched B41Activation (including the AV4-QOL v3 NPC lookup code). GitHub remains entirely read-only.

## Real decision flow

`B4.0 identity -> AV3 initial regional/family priors -> AV2A durable attribute edits -> AV2D verified change overlay -> AV2B 12 derived psych tendencies -> AV4B goal score and applicability -> B4.2 schedule + guarded local movement`.

1. A **fixed authoritative B4.2 schedule** wins over discretionary goals during sleep, modeled work, commuting and household care; AV4B records why alternatives were excluded.
2. During `FREE_TIME`, `HOME_AND_FREE_TIME` or `REST_DAY`, eligible visible B4 people score **HOUSEHOLD_ACTIVITY, RECOVER, PUBLIC_LEISURE, SOCIAL_VISIT, OPTIONAL_WORK**. Inputs: ambition, sociability, courage, discipline, empathy, duty, wealth seeking, family loyalty, friendship loyalty, personal freedom, AV2B risk-taking. Signed disposition/value fields are normalized once to [0,1]. Day/ID supplies a tiny deterministic tie-breaking influence.
3. Availability gates exclude public leisure when there is no nearby eligible registered public worksite, social visiting when no loaded nearby B4 person is visible, and optional work when under 16 or lacking an assigned registered workplace reservation. A reserved worksite is **not** a confirmed constructed workplace. Optional work is merely a movement/goal intent; it does not imply wage, production, time worked, or receipt.
4. A chosen feasible target goes into existing `KnownWorldB42PhysicalRoutine.steer`, including existing worldgen / fortification routing. Pathfinding may fail or be deferred; the AI has not thereby completed anything. The existing AV4A movement choice is preserved as a fallback if AV4B cannot read a safe profile.
5. Per-person/day/phase/availability caches are bounded to 768 evaluated visible identities and cleared on server stop. They do not create new per-world save files. Mid-day AV2A changes may not change a cached decision until phase/availability changes or next day/restart.
6. `av4bGoal` and `av4bGoalStatus` are added to B41 right-click sheet and K-key LLM context. LLMs only learn the server-selected current **intent**, never a false outcome.

## Deliberate non-goals and limitations

- No wages, purchases, actual job switching, negotiated contracts, quantified fatigue/health mechanics, economic state mutations, relationship increments, memories, births, war, law execution, travel relocation, or event journal writes.
- Recovery is a **routine-phase proxy**, not simulated measurable nutrition, stamina or sleep debt. Future AV4/AV8 integration should replace it when durable needs exist.
- B4.2 schedule priorities remain authoritative. AV4B currently changes discretionary goal selection, not compulsory work shifts or sleep windows. Social contact means attempted navigation to another loaded B4 actor. AV5 establishes event verification and relationship outcomes.
- These are scenario tuning weights, not universal ASOIAF facts. The existing `AV4_ATTRIBUTE_COVERAGE` ledger retains 706 canonical definitions / 761 original spreadsheet lines. Do not certify every column as integrated on the basis of this small AV4B subset.
- Existing B4.3 offscreen travel handoff and B4.2 `virtualArrival` remain independent. Offscreen autonomous goals, real fatigue, long-range migration, navigation arrival proof, and general all-action / all-system eligibility are future expansion, not falsely marked done here.

## User commands and acceptance

Before installation, preferably make a local source commit / save backup. Extract the ZIP into the local repository root, preserving paths. Execute:

```
.\RUN_AV4B_CHECK_ONLY.bat
.\RUN_AV4B_INSTALL_AND_TEST.bat
```

The installer checks source anchors and retains unrelated code, writes a timestamped `.av4b_backups` copy, runs `compileJava` then `test` using the local Fabric Gradle wrapper, and rolls back files on failure.

In the Known World with a B4 projected actor (not a Winterfell V3-only actor), after 2–4 seconds of being nearby:

```
/kwgoals status
/kwgoals person "b4:site_ne_007:00000296"
/kwgoals explain "b4:site_ne_007:00000296"
/kwdecisions person "b4:site_ne_007:00000296"
/kwdaily attendance
```

Use another real visible B4 ID if that person is not projected; `kwgoals person` reports the most recent evaluated plan. The right-click character sheet must show `av4bGoal` with selected action, phase, eligible scores and exclusions. Work shifts should remain uninterrupted and schedule actions should show fixed goal authority. Observe some varied B4 NPCs in actual free-time to see different feasible actions (household/recover/public/social/optional work when corresponding resources are present). If `kwgoals` shows no plan or AV4B remains blocked, look for AV3/AV2 profile errors; AV4A fallback should continue. Social visiting is not an observed meeting and optional work is not productive labor.

Unit tests cover fixed-schedule authority, ambitious risk-sensitive voluntary work, feasible social behavior, children and resource constraints, determinism, identity/day/attribute validation.

**Next roadmap batch:** AV5 social interaction validation and relationship consequences; then AV6 birth/education/upbringing, AV7 culture/religion/language, AV8 occupations/economy, AV9 institutions/law, AV10 dialogue/memory, AV11 validated world-event causality, AV12 worldwide performance and coverage audits.
