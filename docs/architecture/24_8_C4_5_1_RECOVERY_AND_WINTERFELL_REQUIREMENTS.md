# C4.5.1 Recovery and Winterfell configuration — 2026-10-22

## Authoritative design intent

**Do not redesign Winterfell's existing central keep, both curtain-wall rings, towers or gate geometry at this stage.** Existing historic/canon household identities, culture/religion and relationships are immutable initial inputs. The 298 AC roster comprises 5,000 unique residents in 1,136 identified households; an ephemeral Minecraft villager is only a projection of one of these characters, not an additional person.

Housing must be sufficient for all households, near the settlement; the source V2 prototype already contains 1,136 house slots, but its 15 hamlets and 4 villages are *physically too far away* for the desired compact Winterfell urban/near-hinterland appearance. C4.5.1 only moves 24 currently allocated castle-ward homes to adjacent streets outside the western wall and replaces their former positions with 12 smaller C3 specialty workplaces. The rest of the prototype 1,136-house geographic layout **still needs a locality-conserving compaction pass**; do not claim it is finished. That pass must preserve homeId, householdId, all person IDs, named characters, workforce and accessible roads, without building over the terrain coast or existing castle.

## Physical rules for all ordinary settlements

- Allocate housing by **authoritative households** when individuals have been materialized, else label the projected housing requirement as **scenario planning**. Do not fabricate NPCs to make a requested home/field count true.
- Workplace demand derives from employed resident counts, occupation catalog, local settlement supply chains, space/capacity, terrain, institutions and economic infrastructure, **not** from randomly adding a fixed number of shops per place.
- A proposed workplace has a durable lot ID, staff capacity, expected job type(s), actual employee assignments, current construction status and empty/vacancy count. Physical plans with staff capacities are **not** equivalent to actual staffing.
- In-castle workplaces: assign a named logical room or wing linked to a durable keep ID. The present C3 castle-ward buildings are external plots INSIDE the walls; they are not yet a room system. Add room-relative local coordinates, doors/pathing, jobs and signs only after the castle interior navigation authoring exists.
- Distribute city trades among homes, with inside-wall priority for administration, security, noble service, healing and palace functions; distribute productive dangerous/noisy trades appropriately; farms and cultivated parcels remain at the outer edge and must be connected to paths. Land acreage/yields are managed by the agricultural land authority, not by counting arbitrary decorative fields.
- Signs identify physical building type at the public approach, never automatically claim a residential family belongs to the ruling dynasty. The actual sign entity must be created at chunk generation and persist/reload in Minecraft 26.3.
- Do not replace existing chunks or player buildings silently. New construction after world creation requires a persistent authoritative append-only construction/demolition process and resource checks.

## NPC careers — status and future integration

C4.5.1 reclassifies eligible *generated* adult residents of the existing Winterfell roster with the C4.4 occupation plan. The V3 import creates fresh profile occupation tags from the diversified roster on a NEW save. It leaves canon authored NPCs, children, dependents and existing saves unchanged. It does not assign actual physical workplace or calculate wages/resources.

The generic NpcRegistry / birth and adult development APIs DO NOT currently choose jobs from local distributions. Planned next connected employment system: on adulthood, migration, loss of work or a new vacancy, read the household's CURRENT residence, local demand, existing staffing, skills/education, age, cultural/legal restrictions, wages and available jobs; choose deterministically; reserve a real job position; save the resulting occupation and workplace; never silently overwrite an existing profession or recreate a person. Regional occupation ratios must change with observed historical development and cannot be reapplied as static percentages every tick.

## Verification gates

1. Existing 43 templates preserved; C3 catalogue extension to 61 loads; local Gradle tests pass.
2. A C4.5 manifest is installed with ≥2 sites; missing sign helper and hard-coded two-site Java guard are replaced.
3. Exactly 5,000 Winterfell residents and 1,136 existing household/home IDs; 24 selected castle ward homes relocated, 12 C3 specialty lots replace their original footprints; keep, curtain walls and historic coordinates unchanged.
4. C4.5 adds locally requested specialist venues with an explicit report for rejected terrain/policy/geometry demand; no silent fake hiring.
5. Runtime/new world test: V3 planned count >1,526, signs render and persist on reload, near-west-housing road is walkable, original walls have intact gates, households load with diversified jobs.
6. SMALL and MEDIUM generation pilot test first; global WORLD only after larger site performance and shoreline validation.

Original 23.0/23.1 and KNOWN_WORLD_MASTER_ROADMAP remain authoritative for daily life, economy, persistent construction, demographic lifecycles, politics, law, travel, factions and wars. This correction is a physical/worldgen + Winterfell occupation step, not a replacement roadmap.
