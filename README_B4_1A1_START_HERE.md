# B4.1A.1 — Worldwide Winterfell-quality physical NPC foundation (2026-10-24)

## Install exactly here

Extract **all files in this ZIP**, retaining directories, into `D:\GameOfThronesMod2`. Choose **overwrite** for the one existing Java source:

`src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java`

This does not overwrite Winterfell's V3 runtime, its LLM system or any previously generated settlement buildings.

**Prerequisites:** C4.11 global rollout installed, B4.0 population generated (including the B4.0 ID hotfix), and B4.1A installed and run. The installer checks these explicitly; it does NOT rerun their BATs.

Open PowerShell at `D:\GameOfThronesMod2`:

```powershell
.\RUN_B4_1A1_WINTERFELL_PARITY.bat
.\gradlew.bat clean build
```

Launch IntelliJ runClient normally and test in a freshly generated or previously built **C4.11 Known World** settlement. **No new world is needed** for the NPC projection resources alone; the underlying buildings must already exist in loaded chunks.

In-game:

- `/kwactors status`: nearby projection and resource-cache counts
- `/kwactors nearby`: named currently visible residents
- Right-click an actor for the existing multi-tab character UI
- Winterfell remains under `/kwinterfell3npc status` and its native V3 right-click/LLM code

## What changed (compared to B4.1A)

1. **All members of a household can project.** The old `max_per_home=2` reduction is removed. Every B4.0 person with a C4.11 home lot that has a C3 construction template gets a corresponding nearby-projection candidate. Total live global actor budget remains **128**. The nearest candidates win; this does not guarantee all members are visible when the global budget is occupied.
2. **Non-overlapping individual spawn attempts.** Multiple inhabitants of a house get independently selected landing cells around their home's entrance (bounded check; no block edits or forced chunks).
3. **Villager motion.** Nearby ordinary B4.1A.1 actors now use vanilla villager AI, with a 48-block home-area return limit. This is *wandering*, not a physically/pathfinding-correct home-to-work schedule.
4. **Substantially richer right-click sheets.** Deterministic seed-derived personality, skills, aptitudes, dispositions, traits, character values, regional social norms, primary language, education and dialogue style. Occupations bias skills; regional values and norms come from the B2/B3 profile. These are **provisional model estimates** until the B4.2 full persisted profile bridge, not independently experienced history.
5. **Improved *display* naming.** No universal `Given of HouseholdLabel` construction; household-consistent nonnoble surnames/bynames where appropriate, optional mononyms, and order variations. The names are provisional and regional pools remain limited, but all person IDs, original B4.0 baseline fields and household IDs are unchanged.
6. **Scalable disk-backed spatial resources.** The candidate file is no longer one enormous per-settlement JSONL loaded wholesale on visit. Instead each eligible settlement has gzipped 512-block tiles; Minecraft keeps at most 24 decoded tiles in cache. This is important for King's Landing.
7. **Winterfell protected.** No duplicate IDs/entities; Winterfell V3 retains its authored 5,000 residents. Existing C2 Ramsgate/Acorn Hall are not covered by the C4.11 projection and remain a future adapter task.

## Data truth / important limitations

- **B4.0** remains the authority for initial population and 204 occupation counts. *This batch reads only.* It does not reinterpret workers or change a single job quota.
- **C4.11** supplies *planned* home lots and an approved settlement offset. Individual placement success is not recorded persistently per lot, so a person can still be projected near a planned home that failed to render. The next construction ledger should fix that. This batch does not falsely report all buildings verified.
- A location that C4.11 excluded or rejected, or whose house template was deferred, receives no new B4.1A.1 physical projection. Its B4.0 people continue to exist as data.
- `setNoAi(false)` permits natural local motion but does not implement job effects, skill progression, offscreen routines, observed events or cross-region travel. **B4.2/B4.3 are required for that.**
- **Worldwide K-key LLM chat is NOT in this batch.** B4.1B must integrate B4 IDs with the established Winterfell individual conversation guard/ledger without fabricating conversation knowledge.
- Generated profile fields are deterministic on reload and have not been persisted as evolving CharacterProfile instances in `NpcRegistry`; later B4.2 must preserve acquired changes separately.
- The lifetime 128-actor cap applies to this bridge, while Winterfell V3 retains its own cap.
- If all 128 projected people are already nearest to the player, there is no house-by-house minimum; this is deliberate to prevent unbounded entities.

## Test status

15 Python tests (household preservation, site-ID safety, duplicate rejects, 248-site manifest, naming, installer prerequisites) run from the BAT. Full multi-million-person creation and Minecraft/Fabric compilation must run on your local project with its installed resources and Minecraft mappings. Python tests cannot prove villagers visually navigate roads or buildings.

## Resource backup

The earlier generated B4.1A.1 resources, if any, are backed up to `tools/geodata/output/b4_1a1_projection_backup` rather than left inside `src/main/resources`. Existing B4.0, C4.11 and B4.1A version-one data remain untouched.

## Reverting

To return to B4.1A, extract only `KnownWorldB41Activation.java` from the earlier B4.1A package and rebuild. The B4.1A.1 tile folder can be left on disk (unused). Original B4.0 person resources have not been altered.
