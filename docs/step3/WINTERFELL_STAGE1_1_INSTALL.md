# Winterfell 298 AC case study -- Stage 1.1

## What changed

- Full `KnownWorld.java` with one `WinterfellCaseStudyInteraction.register()` call.
- Includes the right-click character-card handler and original case-study roster generator.
- Adds 50 separately callable preview pages covering ALL 1,200 proposed residents, in batches of 24. All are NO-AI **visual test proxies**, not duplicate NpcRegistry individuals.
- Does NOT alter authoring profile files, production terrain, the winterfell_test castle, or the NPC save.
- Does NOT place homes; the 631 records are housing/workplace **units** pending actual physical planning, not necessarily 631 separate buildings.

## Install and generate (project root: D:\\GameOfThronesMod2)

1. Unzip the archive to the project root, preserving `src/`, `docs/`, and `tools/` paths.
2. Back up your world; use a fresh disposable **Superflat Creative** world only.
3. Run:

```powershell
cd D:\GameOfThronesMod2
py tools\geodata\build_winterfell_case_study.py
py tools\geodata\validate_winterfell_case_study.py
py tools\geodata\build_winterfell_preview_pages.py
.\gradlew.bat clean build
.\gradlew.bat runClient
```

4. In the new disposable test world:

```mcfunction
/reload
/function knownworld:case_study/winterfell_page_001
```

Right-click one of the 24 villagers to inspect their proposed name, job, house, personality/traits/skills. Use page 002 etc through 050. Each page removes just `kwf_case`-tagged **villagers within 80 blocks** of the executor. It does not delete vanilla villagers or knownworld real NPCs.

```mcfunction
/function knownworld:case_study/winterfell_preview_clear
```

To test the old short function, `/function knownworld:case_study/winterfell_npcs` still exists after running the original generator.

## Verification

```powershell
Test-Path "src\main\resources\data\knownworld\function\case_study\winterfell_page_050.mcfunction"
Test-Path "build\resources\main\data\knownworld\function\case_study\winterfell_page_050.mcfunction"
Test-Path "build\resources\main\data\knownworld\case_study\winterfell_roster.json"
```

All should be True. After build, autocomplete should include `knownworld:case_study/winterfell_page_050`.

## Mandatory next stage (not secretly included)

- Real persistent NPCs: one-time staged import of proposed IDs to **existing** NpcRegistry, AuthoredIdRegistry, CharacterProfileManager, NpcAffiliationManager and existing save. Test two reloads and zero new IDs.
- Physical housing: plan connected roads, terrain surveys, farm plots, castle rooms, seasonal town and rural hamlets before putting down blocks. Distinguish a residential **unit** from a separate building: 55 castle room/household units can be in few larger buildings, and some of 111 workplaces are in the same building as residences.
- Spawn optimization: only physically materialize nearby individuals from persistent simulation state. Remove their physical representation when unloaded; do not delete their NpcId.
- Starting population 1,200 is a MOD pilot, not canon Winterfell census and not an assumed 5,000 inhabitants.

## Cautions

The preview is still NOT authoritative population integration. The Python builder already stores all 1,200 distinct person records; the new pages show all entries for inspection in controlled groups. No server test or Java build was performed by this package's author; report any compile errors verbatim.
