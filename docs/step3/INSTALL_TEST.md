# INSTALL: WINTERFELL POPULATION CASE STUDY (26.3)

1. Back up your project and **use a disposable Creative Superflat world**. Do NOT test in your real campaign save.
2. Unzip files in `D:\GameOfThronesMod2` matching `src/`, `docs/`, and `tools/`. This does NOT overwrite any existing classes or authored character records.
3. In `KnownWorld.java`, add this import:

   `import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellCaseStudyInteraction;`

   In `onInitialize()` after `NpcSimulation.registerLifecycle();`, add:

   `WinterfellCaseStudyInteraction.register();`

4. Run, in PowerShell at repo root (the Python builder is required; without it the resource/function will not exist):

   ```powershell
   py tools\geodata\build_winterfell_case_study.py
   py tools\geodata\validate_winterfell_case_study.py
   .\gradlew.bat build
   .\gradlew.bat runClient
   ```

5. In the disposable test world, move to empty flat ground and type:

   `/reload`
   `/function knownworld:case_study/winterfell_npcs`

6. **24 demo villagers** should appear in a 6×4 grid near you. Right click each; their preview card is shown in chat. Check different names, faith, age, occupation, lord, home ID, skills. The villagers have `NoAI` so they remain standing for inspection.
7. To remove ONLY the test proxies (in your disposable world):

   `/kill @e[tag=kwf_case]`

## Caution and limitations

- The generator is deliberately non-destructive to old scenario files and does NOT mutate/save actual simulation NPC records. New `wf298_*` IDs are **candidates**, not `NpcId` allocations.
- If you execute this function more than once near the same location, it checks for the ID tags within 80 blocks. It is a test utility, NOT robust world-scale entity deduplication.
- The 24 proxy villagers only visualize data for a 1,200-person draft roster, and have vanilla villager models, not final custom human models. Do not interpret an extra Minecraft entity as an extra counted simulated person.
- The showcase opens a *chat-based character card*, not the final graphical character-sheet screen. Full GUI and live NpcRegistry data are a separate integration.
- Minecraft 26.3 interaction compilation has **not** been tested locally. If Gradle rejects an API, send the first compiler error and path; don't modify other systems.
- `docs/step3/winterfell_case_study/profile_proposals/*.json` contains proposed full-shape data for 14 authored people. Do not copy these over `characters/*.json` automatically without reviewing fiction/supplemental-estimate provenance.
- No new physical housing or castle is built by this kit; all 631 building roles/units are **planned**. The earlier `/function knownworld:step3/winterfell_test` is still a separate low-detail castle test.

Confirm the compiled resource exists after build:

```powershell
Test-Path "build\resources\main\data\knownworld\function\case_study\winterfell_npcs.mcfunction"
Test-Path "build\resources\main\data\knownworld\case_study\winterfell_roster.json"
```

Both should return `True`.
