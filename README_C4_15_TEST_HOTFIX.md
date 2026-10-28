# C4.15 — Fortification test expectation correction

The C4.15 `MAJOR` core is now 89 x 87 blocks (x=-44..44, z=35..121).
The actual principal great hall is 89 x 64 blocks (z=58..121). The old
C4.10 JUnit test expected that 64-block hall depth to be the entire core depth.

This patch **only updates the stale test**. It does not change the generator,
world data, NPC identities, population, or saved worlds.

## Apply
Extract this ZIP to `D:\GameOfThronesMod2` preserving directories, allowing
it to overwrite the one test file:

`src/test/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC410FortificationsTest.java`

No BAT is required. In PowerShell:

```powershell
.\gradlew.bat clean build
```

If you prefer a manual one-line correction, change the old test assertion
`assertEquals(64,c.keepSouth()-c.keepNorth()+1);` to
`assertEquals(87,c.keepSouth()-c.keepNorth()+1);`. The ZIP additionally
checks the two endpoint coordinates and relative major/medium footprints.

## Validation
Tests compiled and executed against the packaged C4.15 fortification class
using Java 21 and minimal local JUnit API stand-ins. A full Fabric/Gradle
build still requires the user's local Minecraft workspace.
