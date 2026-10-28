# B4.2 — Worldwide Daily Life & Employment: foundation

Scenario: AGOT 298 AC. This is the implementation batch following B4.1B.

## Scope and non-claims

This batch **does** install a worldwide, campaign-clock-derived day model for all **3,760,684 existing people** (248 sites), without ever constructing 3.76m Java villager objects. It tracks which campaign days have occurred in a world and evaluates an individual's scheduled activity on demand. It derives age-appropriate home, work, rest and commute windows and a **bounded, estimated** primary skill-practice adjustment for individuals with a C4.15-matched *planned* workplace. Where the distance is short enough and destination chunks are loaded, up to 128 B4.1A.1 physical NPCs can attempt vanilla navigation toward the job or home. The existing K-key conversation UI receives schedule context as **model data only**, separate from memories and observed facts.

This batch **does not** yet implement reliable full-city pathfinding; actual verified workplace construction; real goods production, wages, food consumption, ownership or stock transactions; new persistent personal encounters, pregnancy/birth/death events or truthful witness/rumor propagation in the offscreen population. The existing NpcSimulation components and Winterfell's own active features remain intact. People without a matching workplace are recorded as **unverified**, not silently assigned to a fake building. All planned worker numbers remain unchanged.

An offscreen resident exists as a stable B4.0 person with a lazily evaluated schedule. It is **not** a continuously moving Java entity; future B4.3 travel can give actual long-distance itineraries, and a B4.2 economy integration can commit work to the authoritative inventory ledgers. No inferred work shift is written as an eyewitness memory or economic transfer.

## Install — one ZIP, no separate payload directory

1. Before installing, keep your existing local source and Minecraft world backups. Do not `git reset`; newer local changes are not on GitHub.
2. Extract **the entire ZIP contents** into `D:\GameOfThronesMod2`, keeping the included `src/`, `tools/`, and `docs/` paths. There is **no `PAYLOAD` folder** for this batch.
3. Run from PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_B4_2_WORLDWIDE_DAILY_LIFE.bat
```

The BAT runs 9 Python/JDK checks, builds a read-only index of the existing C4.15 planned workplaces, verifies B4.1B integration anchors and backs up three Java files before patching. It neither reruns B4.0 identity genesis nor updates any Minecraft save while installing. A Java/JDK not on the shell PATH makes the optional pure-Java test skip; the subsequent Gradle build still checks actual Java compilation.

Java originals go under `tools/geodata/output/b4_2_source_backups/<timestamp>/`. The resource compiler's previous index is backed up outside the resources directory at `tools/geodata/output/b4_2_previous_worksite_index`. On rerun, the integration patch is idempotent.

4. Then run:

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

**No fresh world is required for B4.2's simulation code**. Cities generated in C4.15 remain the same; this batch does not rewrite structures or households.

## In-game proof

In a Known World test save:

```
/kwdaily status
/kwdaily nearby
/kwactors nearby
```

`/kwdaily status` shows exactly 3,760,684 residents, 248 site metadata entries, the first/last committed campaign day, elapsed days and the bounded workplace cache. `/kwdaily nearby` reads the nearest worldwide villager within six blocks, giving provisional daily routine/occupation information. It does not inspect Winterfell V3 because Winterfell uses its separate authored NPC runtime.

The existing right-click **Life** tab now also displays scheduled activity, work window, estimated commute, modeled workdays and estimated practice gain. For an eligible employed person, their main occupation skill can receive a **deterministic visual estimate** based on modeled workdays; this is not yet a persisted learned-skill change in `CharacterProfileManager`.

K-key conversations use the same B4.1B ledger and guard as before; the new schedule context is explicitly not accepted as verified actions or witnessed history. Chat conversation locks still override movement goals.

Campaign day checkpoints for each Minecraft world are stored at:

```
run/saves/<your world>/data/knownworld/daily_life_b4_2_v1.json
```

This small checkpoint is written at initialization and campaign-day advances, not for every person. It refuses to overwrite mismatched/corrupt save state or replay a reversed calendar. It contains `firstDay`, `lastDay`, `daysAdvanced`, and authority markers—not millions of character JSON updates. Offline **real-world** time does not advance a Minecraft campaign day.

## Explicit architecture limits / next integration

- A C4.15 worksite is only a **planned** lot; C4.15's 600-block Manhattan commute target is **not verified pathfinding**.
- Physical NPCs are still selected near their B4.1A.1 homes. Faraway workplaces and true player-interceptable journeys are not yet projected along travel corridors; local path requests are attempted only inside 48 blocks without teleporting.
- Individual skill progression in the inspector is a read-only deterministic estimate. For persistent gameplay progression, we need event-based differential saves and `SkillPracticeService` integration for the active person, not 3.76m full profiles in memory.
- Ordinary simulated shifts do not authorize invented world events, memories, friendships, income, produce or new family relations. These require explicit committed simulation rules and evidence.

The next substantial extension to B4.2 should integrate authoritative economic job execution and sparse durable personal life events before B4.3's route-aware travel, news and political reactions.

## Files

- `src/main/java/.../population/KnownWorldB42Schedule.java` — zero-entity deterministic schedule evaluator.
- `src/main/java/.../population/KnownWorldB42DailyLife.java` — world clock checkpoint, cached workplace lookups, nearby movement and UI/LLM adapters.
- `tools/geodata/b4_2_workplace_index.py` — streaming C4.11 worksite coordinate resource compiler.
- `tools/geodata/b4_2_install.py` — checked, backed-up, idempotent B4.1B/C4.15 patcher.
- `src/test/python/test_b4_2_*.py` — resource/index/integration, JDK schedule tests.
- `docs/architecture/24_22_B4_2_LAZY_DAILY_LIFE_FOUNDATION.md` — rationale, authorities and next-stage interfaces.
