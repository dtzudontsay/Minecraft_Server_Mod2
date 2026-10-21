# C4.3 read-only job / settlement reconciliation

**Status: REVIEW ONLY. No construction activation, NPC mutation, or job assignment.**

- Occupation references: 195 (136 core, 59 extension).
- Atlas detailed job-role keys: 21.
- Winterfell V2 roster: 5000 planned residents across 19 job/status labels.
- Named character files: 21.
- Physical architectural templates: 43.
- C1 physical sites: 248; tiers: {'MAJOR': 47, 'MEDIUM': 95, 'SMALL': 41, 'SPECIAL': 65}.
- C1 generation modes: {'CANAL_CITY_REQUIRE_WATER_LAYOUT': 1, 'COASTAL_REQUIRE_SHORE_LAYOUT': 6, 'GENERAL_COMPACT_SETTLEMENT': 182, 'MOBILE_NO_FIXED_FOOTPRINT': 1, 'MONUMENTAL_FORTRESS_REQUIRE_AUTHORED_BLUEPRINT': 5, 'PYRAMID_CITY_REQUIRE_AUTHORED_BLUEPRINT': 1, 'ROCK_INTEGRATED_REQUIRE_AUTHORED_BLUEPRINT': 1, 'RUIN_OR_LANDMARK_ONLY': 47, 'SHADOW_CITY_REQUIRE_AUTHORED_BLUEPRINT': 1, 'UNVERIFIED_NO_CONSTRUCTION': 3}.
- C4 currently enabled pilot site IDs: ['site_nw_031', 'site_sw_023'].

## Blocking mismatches

- Catalog occupations, atlas role keys, Winterfell assigned job labels, and building types are separate vocabularies.
- Reference occupation existence does not imply NPC assignment, and a rendered workstation does not imply productive labor.
- C1 computes estimates; C4 caps these estimates to two physical pilots.
- Neither active worker reassignment nor global construction is authorized by this audit.
- Land/rural area capacity and income/production still need independent integration.

## Review exports

- `01_occupation_catalog.csv`: all reference professions, editable review columns.
- `02_assigned_npc_occupations.csv`: authored-profile fields and Winterfell roster job counts.
- `03_physical_building_catalog.csv`: 43 dimensions/staff values and planning role links.
- `04_role_building_crosswalk.csv`: atlas roles, Winterfell labels, direct matches and editable decisions.
- `05_site_readiness.csv`: all C1 sites, demand and existing pilot-lot counts.

Do not treat this as C4.3 completion or authorize worldwide construction from these files.
