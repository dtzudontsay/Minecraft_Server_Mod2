KNOWN WORLD / C1-C3: UNIVERSAL SETTLEMENT PRESETS AND JOB CAPACITY PLANNING

Extract into D:\GameOfThronesMod2 preserving folder structure.
Do not delete your B2/B3 source files or previous Winterfell V3 worldgen.

1. Run:  .\INSTALL_AND_VALIDATE_C1_PRESETS.bat
2. Open: tools\geodata\output\c1_settlement_presets\KnownWorld_C1_Preset_Explorer.html
3. In game: /kwpresets status
            /kwpresets marker SW-046
            /kwpresets marker NEE-005
            /kwpresets marker SE-008
            /kwpresets marker SEE-009
            /kwpresets marker SI-003
            /kwpresets holding castle_cerwyn_domain
            /kwpresets workplace FARM
            /kwpresets workplace BAKERY

THIS BATCH IS PRESET BLUEPRINTS + INSPECTION, NOT WORLD BUILDING.
It intentionally does NOT generate buildings in newly explored chunks yet.
Do not read any 'planned' home or workplace as an actual constructed building.
No NPC simulation is initialized, edited or duplicated.

Presets are source-derived from all 253 map observations and 259 holdings.
Unique physical sites may number fewer than 253 due to shared positions.

EXACT FILES CHANGED:
NEW tools/geodata/build_c1_settlement_presets.py
NEW tools/geodata/test_c1_settlement_presets.py
NEW tools/geodata/c1_settlement_preset_rules.json
NEW tools/geodata/c1_building_grammars.json (43 building modules, 14 regional palettes)
NEW tools/geodata/verify_c1_java_syntax.py (optional API-shape smoke test; not Gradle)
NEW tools/geodata/c1_presets_dashboard_template.html
NEW src/main/java/dev/dtzudontsay/knownworld/simulation/integration/KnownWorldSettlementPresets.java
NEW src/test/java/dev/dtzudontsay/knownworld/simulation/integration/KnownWorldSettlementPresetsTest.java
NEW tools/geodata/install_c1_integration.py (idempotently adds a ModInitializer entrypoint and merges occupations)
NEW tools/geodata/c1_occupations_additions.json (52 new PLAUSIBLE occupations)
LOCAL MERGE via installer: src/main/resources/fabric.mod.json
LOCAL MERGE via installer: src/main/resources/data/knownworld/reference/occupations_extensions.json
NEW docs/architecture/24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md
NEW INSTALL_AND_VALIDATE_C1_PRESETS.bat

GENERATED (NOT PROVIDED IN ZIP; RUN BATCH):
src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_presets_c1_v1.json
tools/geodata/output/c1_settlement_presets/*

Installer merges your existing fabric.mod.json and occupations_extensions.json.
For rollback, it keeps backups under tools/geodata/output/c1_settlement_presets/backups/.
If an occupation ID conflicts, installation stops before changing either file.

Read-only GitHub access. No commits/pushes made.
