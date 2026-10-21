KNOWN WORLD C3 — Minecraft 26.3 wool compilation correction

The Java source now uses Blocks.WOOL.brown().defaultBlockState()
instead of the removed standalone Blocks.BROWN_WOOL field.

INSTALL
1. Extract this archive into D:\GameOfThronesMod2, preserving folders.
2. Replace the existing file when prompted:
   src\main\java\dev\dtzudontsay\knownworld\world\generation\settlement\KnownWorldC3Architecture.java
3. Run: .\gradlew.bat clean build

DO NOT rerun the C3 installation script for this one-line source correction.
The prior C3 template generation, integration, and seven Python tests
already succeeded. This correction does not modify save data.
