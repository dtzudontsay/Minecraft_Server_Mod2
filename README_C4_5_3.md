# C4.5.3 — Fabric chunk-load callback compilation fix

This is a **minimal fix** to C4.5.2. In Minecraft 26.x Fabric's `ServerChunkEvents.Load` callback takes `(level, chunk, generated)`, not `(level, chunk)`.

## Installation

1. Extract all files to your `D:\GameOfThronesMod2` repository root (alongside `gradlew.bat`).
2. Run `RUN_C4_5_3_FIX_AND_BUILD_KEEP_OPEN.bat`.
3. Wait for `BUILD SUCCESSFUL` before invoking `gradlew.bat runClient`.
4. Create a fresh Minecraft test world; don't use the partially generated save from the previous crash.

## Changes

One source line in `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldStructureSigns.java`:

```java
// Previous, fails to compile in your Fabric version:
ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
// Correct:
ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
```

The patcher refuses unexpected versions and creates `KnownWorldStructureSigns.java.before_c4_5_3_callback.bak` if no such backup exists. It is safe to rerun.

No changes to the population, occupations, building counts, geometry, settlement configuration, world save, or main mod initializer.

The included batch writes the complete build output to `C4_5_3_CALLBACK_FIX_BUILD.log`, prints the last 60 lines on error, and keeps the window open. Successful compilation/tests **do not guarantee that worldgen is bug free**; the original C4.5.2 sign-null-world crash fix still requires a Minecraft test.
