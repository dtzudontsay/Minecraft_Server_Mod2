# B4.1A — Worldwide Physical NPC Projection / Character Inspection

## Why B4.1A, not a fake full B4.1 promise?

Your B4.0 screenshots verify the **complete packaged 3,760,684-person identity baseline** (799,343 households; 1,880,341 modeled jobs). Existing Winterfell IDs are reused. Winterfell already has active villagers + its separate Ollama / A1-A5 dialogue system. The new population records are not imported as 3.76 million active Java `NpcState`s. The initial goal is **to see real baseline persons at the homes we already planned** without corrupting the authoritative existing Winterfell simulation or breaking performance.

B4.1A is the first working runtime connection, not yet worldwide Winterfell AI parity. This distinction is visible in `/kwactors status`.

## Installation

Extract the ZIP at `D:\GameOfThronesMod2` (not inside tools or src).

Run `RUN_B4_1A_WORLDWIDE_PHYSICAL_NPCS.bat`. Prerequisites: B4.0 worldwide resources already successfully installed, C4.11 worldwide global resources already installed, and the B4.0 site-ID hotfix. This batch **does not rerun any earlier global generator** or modify world saves. The script:

1. Runs isolated Python tests and dry-run installer checks.
2. Reads only existing B4.0 compressed identity shards and the 248-site C4.11 tile index to associate actual resident IDs with their *planned house lots*. It publishes new compressed, site-specific projection candidate resources under `src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a_v1/`.
3. Registers `KnownWorldB41Activation` in `KnownWorld.java` and adds a read-only accessor to the **existing C4.11** terrain-approval cache. Both changes are idempotent and backed up as `.before_b4_1a.bak`.
4. Requires normal Gradle `clean build`, then `runClient` and a *Known World* world preset.

In-game: `/kwactors status`; `/kwactors nearby`. Fly to a populated area in a C4.11 constructed settlement, wait for loaded chunks, look for newly visible villagers, and **right-click** to inspect a person with name, age, job, culture/religion, household and logical home/workplace reservation. Winterfell is still handled exclusively by `/kwinterfell3npc status` and its existing interactions.

## Exact data/authority limitations

- **No population duplication:** one B4.0 `personId` corresponds to one projected actor; B4.1A does *not* insert the person in `NpcRegistry`. Winterfell stays the only source of Winterfell actors. Only one or two B4.0 people per *available planned house* are emitted into the efficient nearby-visibility index, but **all 3.76m remain in B4.0 resources**.
- C4.11 physically accepts up to 194 site attempts; Winterfell and two existing C2 pilots are protected. Two C2 pilot settlements (Ramsgate/Acorn Hall) need a separate live-lot adapter; they are **not** covered by this initial B4.1A global-lot adapter. Any genuinely unbuildable inhabited site likewise has no physical NPCs until a construction plan exists. This does not remove their logical population.
- C4.11 can relocate approved plans by up to 512 blocks. The projection uses `KnownWorldC411GlobalWorldgen`'s deterministically recomputed/cached terrain-approval position, including on revisits to saved chunks; it does not write blocks or force-load chunks. If the physical site is not approved, it does not produce misleading villagers on empty ground.
- A *planned* house template can still fail at lot level. The index records planned house origins and does not yet receive an authoritative per-lot **actually built** journal. An NPC may stand at the nearest safe surface near a planned-but-unbuilt lot. B4.1B/C should link to a persisted placement success ledger. This is the known limitation before exact residence traversal.
- Only ~128 additional B4.1A actors **globally** are visible at once, their Villager AI is paused so they don't wander off and lie about a location. This is not a daily routine or actual travel. Physical projections are disposable; only compressed B4.0 identities are durable at this stage. No Minecraft chunks are forced by the projection.
- The right-click UI is reused, with real fields only; **no invented skills, witness experiences, secrets, world events or dialogue memory**. K-key Ollama for these globally generated villagers remains **B4.1B**. Winterfell's K-key continues to work as before. This deliberately avoids bypassing existing A1-A5 safeguards with a weak substitute prompt.
- The 194 C4.11 sites include exceptionally difficult terrain; user screenshots of terrain approval and generated walls are encouraging but do not verify each site's constructed homes.

## Persistence and scalability

The B4.0 immutable population identity shards stay unchanged. The new B4.1A resources are a site/lot *index*, not a second population. When players move, the server loads at most 2 nearby settlement projection shards into a 64-block spatial hash and limits physical actors to 128. They can be recreated deterministically without permanent villager-entity state. The eventual event/delta store owns mutable personal experience.

## Next

B4.1B: generalized K-key conversation bridge that reuses Winterfell's personal NPC/player ledger semantics, verified public knowledge and conservative guard; avoid exposing unsupported facts; add persistent relationship/memory keyed by **B4.0 stable person ID**. B4.2: real/offscreen daily routines, activities, skill growth and employment; B4.3: itineraries, travel, witnesses, event propagation. None of these are present in this ZIP.

**Validation:** Installer/tests can be verified offline but actual Fabric build and runtime behavior require the user's local Minecraft 26.3/JDK 25 environment. No claims of in-game verification are made.
