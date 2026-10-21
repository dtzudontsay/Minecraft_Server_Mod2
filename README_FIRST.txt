KNOWN WORLD — B2+B3 WORLDWIDE SITE REGISTRY

Purpose: accept all 253 existing X/Z map anchors for geographic PLANNING,
connect every original marker to an independently addressable site identity,
preserve all canonical geographic place/region identities,
classify every site and all 259 political holdings as major/medium/small/
special/non-settlement, and expose the researched culture, religion,
regional influences, population target and 21 occupation categories.

IMPORTANT:
- NO EXISTING NPC OR BUILDING IS MOVED OR CREATED.
- ALL ORIGINAL MARKER X/Z VALUES ARE RETAINED.
- NO SETTLEMENT SITE IS AUTO BUILD APPROVED OR WORLDGEN ENABLED.
- POPULATION NUMBERS ARE AUTHORING ESTIMATES, NOT EXTRA NPCS.
- NO MISSING NAMED LORDS OR HOUSES ARE INVENTED.

INSTALL:
1. Back up any work-in-progress files and important Minecraft worlds.
2. Extract this ZIP into D:\GameOfThronesMod2, preserving folders.
3. Run from the repo root:
     .\INSTALL_AND_VALIDATE_B2_B3.bat
   or run these commands manually:
     py tools\geodata\test_b2_b3_world_registry.py
     py tools\geodata\build_b2_b3_world_registry.py
     py tools\geodata\build_b2_b3_world_registry.py --check
     .\gradlew.bat clean build
4. Open:
     tools\geodata\output\b2_b3_world_registry\KnownWorld_B2_B3_Overview.html

REQUIRES PREVIOUSLY COMMITTED B2.2 AND B2.3 DATA:
  tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv
  tools/geodata/output/b2_holding_links/markers_253_audit.csv
  src/main/resources/data/knownworld/scenarios/agot_298_ac/holding_geo_candidates.json
  tools/geodata/output/b2_3_world_profiles/site_profiles_253.json
  tools/geodata/output/b2_3_world_profiles/holding_profiles_259.json
If missing, rerun their existing B2.2 / B2.3 Python generators first.

IN GAME COMMANDS (OPTIONAL):
  /kwsites status
  /kwsites marker NW-029
  /kwsites marker NEE-005
  /kwsites holding winterfell_domain
  /kwsites holding red_keep_domain
  /kwsites region the_north
  /kwsites place winterfell

See docs/architecture/23_5_B2_B3_WORLDWIDE_SITE_REGISTRY.md
for exact source policy, provenance and what's left for B4/B5 and C.

All source read-only from the GitHub repo; no GitHub writes performed.
