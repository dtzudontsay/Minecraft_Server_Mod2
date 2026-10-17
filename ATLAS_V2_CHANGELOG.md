# Known World Atlas V2 — continuous cursor coordinates

The only Java code modified relative to the previously supplied atlas is
`src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java`.

- Bottom info panel now displays the cursor's Minecraft X and Z **every frame**.
- Displays `/tp @s <x> ~ <z>` for the location below the mouse.
- Cursor master position is reconstructed from current pan, zoom, and viewport size:
  `mx = centerX + (mouseX - viewMiddleX())/scale`, analogous for Y.
- The same Wall-based provisional master calibration is used for marker pins, player
  location and cursor coordinates.
- `~` Y intentionally means "current altitude", **not a safe destination Y**.
- The map background remains coarse land/climate raster; no in-game chunks are read.
- Footer height increased so controls and coordinates do not overlap.

**Validation:** this is an implementation candidate; Fabric 26.3 compilation needs
an actual Gradle client run on the user's machine.
