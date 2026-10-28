# C4.17/B4.2C design and authority boundaries

## Source of truth

B4.0 compressed person shards are identity authority. C4.9 occupation requirements and C4.11 lot IDs are source planning authority. C4.15 commute assignments are distance-proxy reservations, not path-verified commutes. B4.2 campaign day checkpoint is schedule authority. Winterfell V3 controls its own authored NPCs. B4.1B conversation histories remain private per NPC/player pair.

## Construction compatibility

C4.17 changes the deterministic absolute coordinates of C4.11 lots. Every home and work lot keeps its stable ID; recomputing the commute/projection/workplace derived indices is **mandatory** and performed by the installer after the worldgen recompile. This batch must be visually tested with *new chunks*: it does not rewrite explored saves.

## Visible actor ownership

`KnownWorldB41Activation` owns entity spawning/culling/identity lookup. `KnownWorldB42DailyLife` owns schedule-derived goals. `KnownWorldB42PhysicalRoutine` owns nav requests, cooldown and gate passage waypoints. The Winterfell conversation movement lock always wins. No B4.2C method invokes `setNoAi(true)`, forcibly teleports workers, registers millions of `NpcState` objects, writes fabricated social knowledge or calls Ollama on a world tick.

## Work and social actions

Actors request waypoint navigation, but path requests, arrival estimates, and visual meetings are not authoritative simulated events. The schedule overlay is recomputed lazily from the calendar. Offscreen progress cannot be equated with actual product output. Personal identities are stable across spawn/unload; *precise offscreen travel coordinates* are not yet persisted.

## Safety

The installation runner requires exact C4.16 preimages and known population counts, runs regression checks before mutation, snapshots all managed resources before regeneration, and runs the Gradle build. On failure it automatically restores sources and resources, leaving the B4.0 shards and the `run/saves` directory untouched. User explicitly chooses later manual rollback by snapshot ID.
