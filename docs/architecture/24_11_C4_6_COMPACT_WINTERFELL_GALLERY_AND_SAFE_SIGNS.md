# Known World — C4.6 connected physical review batch

## Why this batch exists

C4.5.1 did succeed after the safety probes, but inherited the Winterfell V2 geometry of 4 villages around 1.2–2.2 km and 15 hamlets around 2.7–3.4 km away. Only the fixed castle core lies by NW-029. C4.5.1 appended additional C3 workplaces around each of those distant centers. Its 1,913 building plots include 1,136 homes, 378 original work plots, 12 inner replacement venues and 387 further specialized venues. All of these are **plans** independent from the maximum 128 physically projected NPCs; no additional people are created in C4.6.

The sign code went through two failures: (1) calling SignBlockEntity.setText during chunk decoration while `level` was null (immediate crash), and (2) moving updates to chunk-load callbacks, which may re-enter expensive chunk processing. C4.5.4 disabled experimental building-sign processing and returned visible NPC budgets to 128; that established a working baseline.

## New geometry: keep, wards, surrounding compact rings

- Preserve all 5,000 resident IDs, 1,136 household IDs, 1,136 original homes, existing workplace IDs, and approved new C4.5 venues. A plot moved spatially retains its identity. The original 12 castle-ward home-pair replacements remain intact.
- **Do not change the scripted V3 keep, towers, gates or curtain walls.** Existing in-wall residences and in-wall workplaces are kept fixed.
- Move every *other* physical lot into streets in square rings around the keep, initially 7 grid cells (36 m per cell) beyond the NW-029 origin. Interleave residential and specialized worksite lots so that industry is not relegated to a single remote development.
- Apply special treatment for existing farmer, herder and forester workplaces by assigning those plots to the outer-most urban rings.
- Put **24 real 64 × 64 block cultivated-field annexes** outside the settlement's outer ring, retaining a visual field belt but **not** falsely claiming it supplies the town's complete food demand. Production area/soil yields, actual assignment of workers to physical workplaces and dynamic expansion await our phase D employment/agriculture authority.
- Reconstruct roads and the V3 chunk plot index from new lot coordinates. Avoid block footprints. Preserve the castle's original internal road skeleton and join the outer mesh at existing gate locations.
- The ring geometry uses a fixed *plot pitch*, not a density cap: all planned homes and workplaces are retained and the occupied radius expands as required. C4.6 works with current 1,913 lots; it is **not** safe to enable at King's Landing without its own city planning.
- The old V2 `sites` identities remain social labels and are assigned representative new centroids; older village/hamlet geography is not treated as 19 still-separated physical settlements.

## Blueprint gallery `/kwgallery build`

- Use a **new, separate superflat creative world**. The command refuses the custom Known World terrain generator.
- Gallery loads **all 61 C3 module templates** (alphabetical IDs, same renderer) and places one every 20 server ticks in a **seven-column review grid**, spacing 78 blocks. Every template gets a numbered exterior sign, including normally restricted sites such as SHIPYARD, DOCK or MINE. This is a review environment; their environmental/policy restrictions remain in the normal world.
- `C46_ALL_BUILDINGS_REVIEW.csv` is an editable audit of IDs, actual C3 footprint, floor count, staffing, stations, restriction flags and blank user-review columns.
- The gallery reuses `KnownWorldC3Architecture.buildLot` and changes its block sink only while serving one explicit server-side gallery command; no separate "fake" demo buildings.
- Building creation is bounded to one building every 20 ticks; all 61 at once in a single tick could cause a server freeze.
- **Scope:** C3 accounts for all 61 universal architecture programs. The legacy Winterfell V3 grammar (e.g., COTTAGE, CASTLE_QUARTER, FARMHOUSE and the analytical keep/walls) remains separate and is best inspected in Winterfell itself. These six legacy template variants are *not* silently substituted with lookalikes in the C3 gallery.
- Real examples of the 61 C3 programs share the generic C3 facade and a handful of floor/interior grammars. Many job-specific buildings still look similar; the gallery is specifically meant to guide the next bespoke model/interior revisions.

## C4.6 signs: safety rules

1. `KnownWorldStructureSigns.attach` during worldgen sets only a blank sign BlockEntity and adds a label intent to an in-process, bounded queue. **Never** call `SignBlockEntity.setText` on an unattached protochunk BlockEntity.
2. A `ServerTickEvents.END_SERVER_TICK` service applies at most **16 signs per tick**, only in already-loaded overworld chunks within 7 chunks of an active player. It does not perform block mutation or inventory access inside a CHUNK_LOAD callback.
3. The world and gallery sign systems remain separate. Gallery signs are placed on an existing `ServerLevel` and labeled synchronously at the end of each deliberately bounded construction step.
4. Worldgen pending intents are session-local. After text is applied, Minecraft persists it in a sign BlockEntity; however, remote/unvisited generated chunks may still carry blank signs if the process terminates before they are loaded. Persisting or reconstructing remote pending intents from stable plot IDs is **not** implemented by C4.6; this is the remaining gap for full-world signage durability.
5. Bounded queue max 20,000. Overflow deliberately drops labels rather than hanging generation. Logs and batch report should be used to measure the effect before making worldwide rollout default.

## Intentional non-goals

- No new people, marriages, jobs, occupational redistribution, household rewrites or NPC projection increases. C4.5.1's job diversity and **128** nearby projected villagers stay active.
- No activation of MAJOR or special sites globally.
- No changes to existing generated blocks or old saves.
- Castle rooms for maester, castellan and court positions remain semantic interior locations, not yet a physically authored floorplan with pathfinding.
- No guarantee of every exterior sign being filled across shutdowns until immutable worldgen intent -> permanent block entity labeling is completed.
- No claims that 24 visual field modules feed all 5,000 Winterfell residents.

## Validation

A source-free synthetic 5,000-person fixture verifies 1,136 homes, 1,913 original plans, unique IDs, collision-free new plots, and 24 fields. A local `gradlew.bat build` and new Minecraft superflat/knownworld tests are **required**: this environment does not have the user's Minecraft Gradle dependencies and cannot run those compiled checks.
