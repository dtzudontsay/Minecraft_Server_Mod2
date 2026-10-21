# C3 Minecraft 26.3 registry bootstrap test fix

Fixes the three C3 JUnit failures after the successful Java compile:
`Not bootstrapped (called from registry minecraft:game_event)`.

Root cause: KnownWorldC3Architecture initialized static BlockStates on class load,
including for metadata-only tests, before Minecraft registries were bootstrapped.

Changes: the two BlockState constants are moved to a lazy nested class used
only by physical build methods. All 43 JSON templates, worldgen locations,
C2 integration, wool API fix, and test cases remain unchanged.

INSTALL:
Extract this ZIP into D:\GameOfThronesMod2 with paths preserved. Agree
to replace `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC3Architecture.java`.
Do not rerun C1, C2, or C3 installers.

Run in PowerShell:
    cd D:\GameOfThronesMod2
    .\gradlew.bat clean build

Verification: Java source compiles against 26.3-shaped API stubs and class
initialization is independently tested with a deliberately uninitialized Blocks stub.
Only your actual Minecraft/Fabric Gradle build can confirm full compatibility.
