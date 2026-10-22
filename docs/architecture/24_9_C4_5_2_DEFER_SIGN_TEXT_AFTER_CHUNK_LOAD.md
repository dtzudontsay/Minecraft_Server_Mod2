# C4.5.2 – Sign text worldgen crash fix (2026-10-22)

## Observed failure

After teleport to Winterfell on a fresh 26.3 world, the client crashed during `minecraft:features` generation. The exact stack is `KnownWorldStructureSigns.attach:66 -> SignBlockEntity.setText -> markUpdated -> Level.sendBlockUpdated`, with `this.level == null`.

This is a guaranteed lifecycle bug, not proof that the 1,913 physical plots or 5,000 persistent residents exceed a performance budget. The V3 worldgen data was loaded: 8,278 indexed chunks, 1,913 building plots. The NPC roster imported all 5,000 identities before teleport.

## Patch

* The C4.5.1 sign helper's `attach(ChunkAccess...)` still places a sign block and block entity inside the current generating chunk, but **does not call `SignBlockEntity.setText` there**.
* Label content is queued by (x,z chunk) in a bounded concurrent map.
* `KnownWorld.onInitialize()` registers a `ServerChunkEvents.CHUNK_LOAD` callback; once a `ServerLevel` has the sign entity, the callback applies the text.
* Startup and server-stop lifecycle clear the transient queue, which is bounded to 50,000 entries; overflow leaves an unlabeled sign rather than crashing or consuming unlimited memory.
* The patch touches exactly `KnownWorldStructureSigns.java` and `KnownWorld.java`. All other C4.5.1 construction/job data is preserved.

### Important limitation

This is a **test-world stability hotfix**. The deferred in-memory text queue is not persisted separately. Chunks fully generated offline or with background generators but never loaded during the same server session may have blank signs after a restart. We should eventually replace this transient mechanism with persistent worldgen block-entity text data or deterministic replay from the saved building index. Do not mistake the current hotfix for the final persistent construction/sign system.

## Installation

Extract archive into mod repository root `D:\GameOfThronesMod2` next to `gradlew.bat`; run `RUN_C4_5_2_SIGN_CRASH_FIX_KEEP_OPEN.bat` from there. It will preflight source matches, back up both files, replace them, run `gradlew.bat build`, and keep console open. A failed build means **do not** test as though it succeeded.

After successful build, launch Minecraft (`.\gradlew.bat runClient`) and create a new test world. Do not reuse the partially generated `New World (71)` after the fatal chunkgen exception. For the first retry, disable Distant Horizons distant generation or set PRE_EXISTING_ONLY, and use render distance 8–10 to isolate remaining potential performance issues from the fixed crash. The DH setting is a test precaution, not the cause of this exception.

## Future regressions

Add a proper in-game chunkgen and loaded-sign test for Minecraft 26.3. Unit tests only checking architectural-template metadata will not detect this lifecycle failure. Also verify sign label persistence through save/reload, and verify no duplicate text jobs when chunks load repeatedly.
