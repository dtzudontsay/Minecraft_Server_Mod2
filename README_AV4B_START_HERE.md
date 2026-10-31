# AV4B v2 — Java cache type compilation fix

**Fix for the 2026-10-30 compiler error:** The private cache record in `KnownWorldAv4BGoals.java` is now named `CachedGoal`, not `Entry`, so it no longer conflicts with `java.util.Map.Entry` in the `LinkedHashMap.removeEldestEntry` override. All AV4B source hooks, existing QOL patches, NPC identities and saves remain unchanged.

**After the failed first AV4B install:** Your log confirms rollback of all seven installed files. Extract this ZIP over the same checkout and allow replacement of the previous AV4B BAT files, installer, and `tools/av4b/payload` contents. Re-run `RUN_AV4B_CHECK_ONLY.bat`, then `RUN_AV4B_INSTALL_AND_TEST.bat`. No manual recovery is necessary if the rollback output matches the log you supplied.

**Verification:** The AV4B cache adapter and pure rules compiled under Java 21 against isolated API stubs, and the installer fixture passed (check-only, clean install, QOL preservation, repeat install, refusal on drift, rollback). The full Minecraft 26.3/Fabric `compileJava` and `test` must run in your checkout and are automatically invoked by the installer.

---

# AV4B — General Autonomous Goals & Action Selection (first installation batch)

**This is the next planned stage after AV4A, before AV5 social relationships.**

## Install into your local project

1. Stop Minecraft / your local server. Back up your save and preferably commit your current local source changes (including the optional AV4-QOL v3 changes).
2. Extract `AV4B_GENERAL_AUTONOMOUS_GOALS_BATCH.zip` directly into `D:\GameOfThronesMod2`, preserving subfolders. This ZIP provides only new launchers, an installer, source-code **payloads** (not immediate Java overwrites), tests and the docs payload.
3. Run in PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV4B_CHECK_ONLY.bat
.\RUN_AV4B_INSTALL_AND_TEST.bat
```

The `CHECK_ONLY` script validates narrow anchors and planned changes without modifying Java files. `INSTALL_AND_TEST` creates `.av4b_backups/<UTC timestamp>`, applies changes, then runs `gradlew.bat compileJava --no-daemon` followed by `gradlew.bat test --no-daemon`. If either check fails, Java files and newly written tests/docs are restored or removed. The installer does not execute `git` commands, publish to GitHub, change saved Minecraft worlds or regenerate NPCs.

Because we cannot run your Fabric 26.3 environment on this machine, **passing the source fixture tests is not equivalent to a successful Minecraft compile**. Please send the first compiler error if your local Gradle check fails.

## What's genuinely functional in this delivery

- Existing B4.2 sleep, commute, work and home-care phases remain authoritative.
- Visible B4 adults in free time choose among a household activity, recovery at home, leisure at an available public location, a visit to a loaded nearby B4 actor, or an extra voluntary visit to a registered assigned workplace.
- Scoring connects AV3 regional/family prior -> saved AV2 edits/verified AV2D -> AV2B psychology to 11 normalized profile attributes: ambition, sociability, courage, discipline, empathy, duty, wealth seeking, family loyalty, friendship loyalty, personal freedom and risk-taking. Scores are deterministic with tiny per-person/day variability.
- Available destinations, age and the B4.2 schedule are eligibility gates. Unavailable choices are reported as excluded with reasons. The chosen goal is physically attempted using the existing B4.2 navigation controller (not a teleport).
- Right-click sheets and K-key LLM contexts now include the selected **server intent**, with explicit warnings not to fabricate completed actions.
- B4.2's original AV4A free-time logic remains the fallback when AV4B's profile is unavailable or blocked.
- No new save file, NpcId, 3.76m NPC tick loop, relationship change, wage payment, work receipt or invented memory is created.

## In-game smoke test

Stand near a projected worldwide B4 actor for a few seconds. Use its exact person ID:

```mcfunction
/kwgoals status
/kwgoals person "b4:site_ne_007:00000296"
/kwgoals explain "b4:site_ne_007:00000296"
/kwdaily attendance
```

If Aren is not currently projected, substitute the `b4:site_...` ID of someone visible. Right-click to inspect the `av4bGoal` field; expect either a fixed schedule goal or one of the discretionary goals with a score table and exclusions. A social goal or work goal is a **navigation intention**, not a completed interaction or paid work. No need to repeat the EMPATHY save/restart check you already passed.

## Next stage

AV5: event-verified social interactions and relationship changes. Thereafter AV6 development/upbringing, AV7 cultural and religious behavior, AV8 occupation/economy, AV9 institutions/law, AV10 dialogue/knowledge/memory, AV11 world events, AV12 worldwide performance and coverage. The project-wide Vaes Dothrak/research-to-B4 population data audit remains a separate reconciliation task.

Technical specification: `docs/architecture/24_38_AV4B_GENERAL_AUTONOMOUS_GOALS.md` (installed by the script).
