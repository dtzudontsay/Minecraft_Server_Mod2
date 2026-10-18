# Winterfell V2 technical & research status

Scope: experimental, 298 AC; source repo `Minecraft_Server_Mod2`, Fabric 26.3. The underlying source research reports a provisional Winterfell castle population 300–1,100, a mostly vacant winter town in the long summer, and expressly **no authoritative 298 AC Winterfell/estate census**. The 5,000 resident target is **USER-REQUESTED MOD CALIBRATION**, not discovered book canon.

**Planning results / source-of-truth contracts:**

- 5,000 primary resident individuals, of which the nine already-authored Stark-household residents must reuse `AuthoredIdRegistry` IDs. 13 additional book-named staff proposals, remaining random generated residents.
- Separately tagged visiting royals and Benjen; no visitor counted as new Winterfell resident.
- 1,136 occupied households and 1,136 distinct housing plots. Housing capacity >= actual occupants, exact 5,000 people counted once.
- 378 workplace plots supporting 2,771 workers, all with role-based workplace assignments. Non-workers retain household and home identities.
- Castle, 10× central town, 4 of 5× villages, 15 of 1× hamlets. 77,213 connected planned road tiles; footprint locations are **relative to the explicitly approved test-world origin, not provisional NW-029 world X/Z**.
- Source named-character profiles are not replaced or filled with pretend private biography: their authoritative scenario profile stays in the native mod resource. Candidate-generated non-named characters get seeded skills/personality/aptitudes/values at scenario time. Book-named staff have separate provenance.

**No hidden duplicate population:** `NpcRegistry` and `AuthoredIdRegistry` own people. Villagers in the actual Minecraft `ServerLevel` have a `kwf2_<authoredId>` scoreboard tag; bodies are discarded when outside activation, do not create/clone a person. Existing 9 authored IDs are looked up before any creation; generated people's IDs are created only when absent. Households are registered under existing native `OrganizationType.HOUSEHOLD`.

**Persistence:** native `NpcSimulation.save()` for people, affiliations, genealogy and authored IDs, plus separate test placement progress under world `data/knownworld_winterfell_v2_test.json`. A server-restart smoke test must verify ID uniqueness and population conservation. Entity load event discards stale lingering test bodies from earlier save without affecting native registry residents.

**Real physical limitations:** baseline homes are compact 12×11 rural/city footprints or 11×12 castle quarters, 7 or 9 beds per housing unit; workplaces are similarly simplified. Castle has double wall rings and a central keep plus dedicated quarters; it is deliberately not a finished Winterfell. Road graph connectivity checked at source level, not pathfinding navigation. Terrain fitting is four-corner slope<=7 threshold with fill foundations; too steep => site stays unbuilt until further work. Builder is opt-in and proximity-driven, not automatic worldgen.

**Performance:** 128 regular AI + 128 no-AI is a target ceiling per loaded eligible region (multiple players share this pool), not a measured TPS promise. Minecraft Fabric 26.3 Java APIs not compiled against a local game jar in this environment. Run Gradle build in user environment; send full error report if needed.

**Tests performed in this environment:** 5,000 unique person keys + unique names; distinct 1,136 households/homes; residential and job conservation, job-specific workplaces; age-appropriate modeled parent linkage; total connected graph and collision-free building footprints; no duplication of visitors, no unauthorized final geography placement. The physical runtime tests cannot be claimed until an actual game session.
