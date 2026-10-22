# Worldwide settlement construction and population binding — C4.8 design decision (298 AC)

**Status:** Decision and versioned reference. No worldwide worldgen enabled by this document.

## Authoritative inputs

- Unique site ID + marker group + map working anchor: C1 settlement presets.
- Settlement total people, estimated workers, household/housing demand and tier: C1.
- Each of 204 unique job counts and corresponding building demand: C4.4.
- C3/C4.7 templates provide *physical forms*, not worker identities.
- Winterfell V3 (5,000 persons, 1,136 occupied residences) is a separately authored physical/NPC prototype.

## Design invariants

```
site.plan.population = sum(site.population_categories)
site.plan.workers = sum(site.jobs[204])
site.plan.estimated_households = site.household_planning_target
site.generated_home_capacity >= site.occupied_households   [before moving families in]
site.generated_workplace_usable_slots >= site.workers_assigned_to_building_jobs
site.actual_people = site.plan.population   [when full site activation is commissioned]
```

Do not confuse worker counts with total residents or households with residential *buildings*. Building capacity counts are only **provisional** until a terrain-approved structure is placed and accessible. Use the same ledger to give names, owners, jobs, household memberships and workplace IDs to future NPCs. Children, communal housing, visitors, military units, dependents, unemployed people and politically authored characters remain explicit exception categories.

## Three-tier placement feasibility

- **Dry land:** Search a bounded area around the accepted map pin for a contiguous viable build footprint. Prefer shortest offset that fits the requested central district and expansion corridors; grow inland rather than into the sea. Keep the map pin unchanged and record actual construction anchor separately.
- **Mountains:** Sample local relief, place terraces, retain walls/foundations, switchback streets and limited grade; split streets into neighborhoods if necessary. Never erase authored mountains or accept an unchecked cliff-spanning building foundation.
- **Failure register:** On every proposed physical site, report `siteId`, name, all marker IDs, original X/Z, attempted construction X/Z, tier, planned people, reason, deferred homes/jobs and mitigation. Include water/steep mountain/overlap/permission/exceptional-template/limit conditions. No silent skipping.

## Major-city scaling

Do not transplant Winterfell walls/castle to every Major-tier site. Make its **demand mathematics** universal; select an authored or archetype-specific central complex by site type. A generic King's Landing-scale demand is approximately 500,000 people and 26,596 residential buildings in C1; that needs district paging and bounded chunk work. Small/Medium-only C4.5 generation currently cannot execute that safely. Each settlement's map anchor is accurate *as a map target*, not already-approved building elevation or terrain.

## Recommended order

**Freeze the per-site plan → generate household and job slots → terrain-scan and reserve building footprints → construct chunk-streamed roads/homes/workplaces → verify physical capacity → instantiate NPC identities and bind preallocated IDs → ongoing life cycle, trade, migration and construction.**

Household *plans* first; expensive full NPC simulation records afterward. It is safe to populate in a later batch as long as generated household IDs and job slots are persistent, and construction deficits are reported instead of silently assumed away.
