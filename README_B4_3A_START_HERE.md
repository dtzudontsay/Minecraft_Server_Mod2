# B4.3A — 256 nearby worldwide NPCs + offscreen commuting continuity

**Target:** your already-working C4.17/B4.2C local project (B4.0 through B4.1B and B4.2 are prerequisites).

**What the implementation does**

* Doubles the global B4.1A.1 **worldwide** projected-villager ceiling from 128 to **256**; Winterfell V3 is separate. This is a cap, not a guarantee that 256 NPCs are always present. Configurable JVM property `-Dknownworld.worldwideNpcBudget=128` (32–256 allowed) lets you reduce it without reinstalling. Default 256.
* Updates the existing B4.1A.1 projection **manifest only** (`physicalProjectionLimit=256`) and the B4.1A.1 compiler for future regeneration. It does **not** rewrite any of the 3.76m B4.0 identities or the projection tile contents.
* Computes **daily offscreen home↔work commuting** lazily using the already-authored B4.2 work schedule, time phase, home lot and workplace location. Intermediate positions are interpolated along a short route estimate; when one endpoint is inside a fortification and one outside, a known wall gate becomes an estimated waypoint.
* Keeps up to **4,096 recently observed departures** as world-scoped, restart-safe handoff samples. These samples record the actual position of a *projected NPC at unload*, not fictional lived history. Later re-projection can reconcile its last observed position with the current scheduled destination. The server saves to its own world folder `data/knownworld/offscreen_travel_b4_3a_v1.json` using atomic replacement. If the file is corrupt, travel handoffs are disabled and **the corrupt file is never overwritten**.
* Extends the nearby candidate search to admit recently seen commuters whose home tile is no longer in the player's immediate scan range. Still only scans **bounded home tiles**: all 3.76 million inhabitants are **not** actively materialized, and intercity travelers hundreds of kilometers from their home are not discoverable yet.
* All existing K-key dialogues, private histories, C4.17 movement controller, people/households, B4.2 schedules, worldgen and Winterfell V3 stay intact.

**What it is not**

This is **not** a full physical path-history simulation or intercity population migration system. Schedule-based positions and castle gate waypoints are *estimates*, not verified walks through actual road blocks, water, mountain passes or every building. This phase has **no** NPC-initiated multi-settlement departures, trade convoys, consequences of travel, resource creation or new personal memories. These require event/route systems in B4.3 proper. NPCs cannot be guaranteed to physically reach an unreachable workplace. Stable identity and memory survive projection unloading; the Minecraft entity itself is temporary.

## Installation — staging and rollback

1. Keep a separate backup of `D:\GameOfThronesMod2\run\saves` (this installer does not edit saves, but Minecraft runtime obviously can).
2. Unpack the ZIP at `D:\GameOfThronesMod2`, **keep `B4_3A_STAGE` separate**. Do not copy Java files from it manually.
3. Run `RUN_B4_3A_CHECK_ONLY.bat`. Three managed existing files and projection manifest must show `READY`, and both new Java files must show `NEW`. `UNKNOWN_LOCAL_EDITS` means **stop** and provide the current files; never force.
4. Run `RUN_B4_3A_DOUBLE_NPCS_OFFSCREEN_TRAVEL.bat`. It snapshots all five managed source files + projection manifest, installs the staged Java and generator revision, changes just the projection-manifest numeric limit, then invokes `gradlew.bat clean build`. Any failure automatically restores the snapshot.
5. Launch `gradlew.bat runClient`. You do **not** need to rebuild cities, regenerate B4.0 identities or create a fresh world.
6. For manual source/manifest rollback: `py -3 tools/geodata/b4_3a_guarded_install.py --rollback latest` or use the printed snapshot ID under `tools/geodata/output/b4_3a_safe_snapshots/`. The per-world saved handoff JSON may remain after rollback; it has no effect without B4.3A.

## In-game check

* `/kwactors status` should report up to **256** projected worldwide avatars, and the number of retained offscreen handoffs.
* `/kwdaily status` continues to show the authoritative B4.2 day checkpoint and now displays a handoff count.
* Observe a villager walking to work, leave the loaded area for a while, then return at a different phase of its workday. Its arrival point should be consistent with the estimated schedule and last observed handoff rather than being forced to the original front door.
* Pay attention to server FPS/tick rate, especially dense urban sites. If 256 causes lag, restart with `-Dknownworld.worldwideNpcBudget=128` in the Minecraft JVM arguments.

## Tested vs. untested

The tests exercise census/projection-schema compatibility, deterministic route interpolation, Java 21 pure-model compilation, Java 21 interface compilation of the offscreen-save module, source guarding, and rollback with synthetic repository resources. **The full Fabric build and in-game movement have not been run here.** They are intentionally mandatory inside the installer on your machine.
