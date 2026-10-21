# C4.3 — Occupation / settlement construction integration preflight

**Status: READ-ONLY AUDIT; C4.3 NOT COMPLETE.**

This is a deliberately non-invasive checkpoint before changing the settlement generator, staffing models, Winterfell, or global activation. It does not install or claim to implement the future growth system.

## Rationale

Four distinct inputs exist in the repository:

1. `reference/occupations.json` (136) plus `reference/occupations_extensions.json` (59) define **195 potential occupations**. These are vocabulary, not all live NPC employment assignments.
2. B2.3 / C1 use 21 normalized workforce demand role keys and a deterministic C1 mapping to physical workplace types. These planning percentages were inferred or invented; they are not a census.
3. Winterfell V2 has a separate authored + randomly generated roster, including its own 13 weighted job labels, as well as dependent/visitor/noble statuses. Its 1,136 household plots and 378 workplace plots are not interchangeable with C1's density model.
4. C3 contains 43 physical block-building templates. Buildings and nominal worker capacities do not automatically link to persisted residents, economic jobs, or productive land.

## Execution

Extract package to the repository root and run `RUN_C4_3_READONLY_AUDIT.bat` after committing a backup. Or run:

```powershell
py tools/geodata/audit_c4_3_jobs_and_settlements.py --check
py tools/geodata/audit_c4_3_jobs_and_settlements.py
```

The script reads source data and writes *only* CSV/Markdown review outputs in `tools/geodata/output/c4_3_readonly_audit/`. It never modifies installed mod source, world chunks, saves, NPCs, the geography raster, or original population distributions. The `--check` option creates nothing.

## Next decisions required before physical changes

- Confirm which of the 195 occupation IDs are real work, social roles, offices, livelihoods, criminal activities or status descriptions.
- Approve occupation-to-economic-role-to-building mappings with local cultural/religious/terrain constraints.
- Determine facility sizes and productive land independently; **one 17 x 26 field is NOT sufficient agricultural acreage for 12 laborers**.
- Reconcile actual Winterfell household/workplace assignments against C1 before extending the universal construction pipeline.
- Specify construction ledgers, persistent phased growth, safe chunk modification and player protection before C5/C6 activation.
- Keep the active physical-world pilot authorization restricted to existing two sites until a reviewed implementation and in-game tests pass.

## Source documents

`docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`, `23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`, `24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md`, `24_3_C4_CONNECTED_SETTLEMENT_LAYOUTS.md`, and `24_4_C4_2_TERRAIN_STREETS.md`.
