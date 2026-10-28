# 24.14 — C4.10A shared settlement layout and fortified-core prototype

**2026-10-22.** This is a construction sub-batch of the established cross-domain roadmap, not a new roadmap. GitHub read-only to assistant, user installs and pushes.

## Why

The old C4.5 Small/Medium planner interleaves C3 homes/workplaces but does not create a central fortified residence. Its default C2 loader accepts only SMALL/MEDIUM pilot manifests; it does not activate the 248 sites. Winterfell V3 has its own very different 171/149-block half-span double-curtain physical castle and a 1,136-home compact ring population. C4.9 computed unbounded baseline population/housing/workplace reservations and a separate terrain-placement policy, but did not produce universal 3-tier physical geometry.

## Construction demand and single authority

1. Resolve `markerId -> siteId` via `KnownWorldSiteRegistry`/C1. 253 marker observations identify 248 physical sites, not 253 populations. 259 political holdings are another independent entity type.
2. Use C4.9 generated site contract, sourced from C1/C4.4/C4.8: 3,760,684 planned people, 1,880,341 modeled employed workers, 204 profession columns/site.
3. Reuse C4.9 specific home units/household demand and type-specific workplace counts and staff caps. Place one physical house/building per requested lot, NOT one building per person or per job.
4. Reserve core staff slots from `KEEP` workers (661 worldwide modeled), but do not spawn additional staff or unrelated houses. Additional role-specific court rooms will be governed by actual use, avoiding double-counted dedicated buildings.
5. Winterfell existing V2/V3 NPC, housing, workplace and castle identities take precedence. It is not rebuilt by C4.10A.
6. A named NPC/ruler must generally consume an existing site's baseline headcount and appropriate role slot. Only explicit additional visitors/nonresidents may be modeled separately and must not silently increase residence count.

## Small/Medium/Major core and housing

| Tier | New visual core | Walls | Typical housing module | Nominal household units per building |
| --- | --- | --- | --- | ---: |
| SMALL | 37×29-block simple fortified manor hall | palisade, outer half-span 56 | HOUSE_SMALL | 1 |
| MEDIUM | 49×41-block minor stone keep | single stone curtain, outer half-span 106 | HOUSE_MEDIUM | 2 |
| MAJOR | 89×64-block main keep (offset within castle compound) | two stone curtains, half-spans 171/149 | HOUSE_URBAN | 4 |

The Major outer/inner wall **dimensions** are deliberately from Winterfell, but physical design is a placeholder, not a lore-copy of every castle. The C4.10 Java painter runs only when a future explicit authorized worldgen hook calls it; it must never touch the current Winterfell or player-edited/generated chunks.

## Unbounded placement

- Inner-ward slots for moderate houses/small service modules, constrained by walls and central keep footprint. Their fixed count bounds *only the ward*, not city housing demand.
- Ordinary housing and nonagricultural work buildings interleave using exact C4.9 quota weights. Dedicated businesses are sized according to C3 footprint and staffed from exact C4.8 workplace capacities. Missing/restricted templates are retained in the provisional manifest as visible deficits.
- Rectilinear square perimeter rings use a 56-block pitch and do not stop after 35 rings or after a pilot building quota. Real roads must later follow relief, attach to the four gates and remain continuous after invalid plots are rejected.
- Farms, pastures, gardens and forestry camps use a rural ring outside the last urban ring. The current planter intentionally does **not** manufacture six fields per farmhouse, because productive area belongs to agricultural land/yield authority, not the visual renderer.
- These plans presently reference C4.9 map X/Z **without declaring land approval**. C4.9 relocation may move the anchor before any site is chunk-indexed. Mountain terracing/retaining walls and coastal site policies must be connected and terrain-confirmed in C4.10B.
- Every lot is emitted as a line-delimited record with stable identity and household reservation or workplace reservation links. No giant monolithic per-chunk blob is required by this batch.

## Employment and households: next identity phase

C4.8's non-working total is not a measured unemployed total. A future B4 stage must identify working-age adults, unemployed/underemployed persons, apprentices and dependents from an actual demographic census. Its employed positions cannot exceed the same 204 occupation targets used for physical workplaces without recording changes as simulation events. Household reservations are not automatically biological families; actual `NpcId`, HouseholdId, property ownership, profession and physical Minecraft entity are distinct identities.

## Scope gates

C4.10A **implements an offline geometric layout planner plus an inactive Java chunk-clipped core painter**. Synthetic tests cover all three tiers, capacity conservation, Winterfell exception, special sites, rural outer belt, missing templates, deterministic layout and a 46,071-building King's Landing-scale workload. These are not screenshots or Minecraft runtime approvals. It does not alter existing C4.5/V3 manifests, worlds, saves, population data or model records.

**C4.10B must connect** C4.9 terrain policy + approval/failure state, shared C2/C3/V3 construction, terraced streets, safe actual chunk index paging and live worldgen gates. **C4.11 then activates eligible sites worldwide**, with exceptions/failures reported explicitly; B4/D individual population activation follows with stable identity/ownership/persistence reconciliation.
