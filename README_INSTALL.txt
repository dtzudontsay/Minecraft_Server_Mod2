KNOWN WORLD ATLAS V2 — MOUSE COORDINATE READOUT (Fabric / Minecraft 26.3)

WHAT THE MAP DOES
- Displays all of the Known World as a fixed client-side raster from the beginning.
- Draws 248 provisional place pins in the same master-map projection as worldgen.
- M toggles screen; wheel zooms; left-drag pans; F fits entire map; U toggles unlinked pins.
- Hover/click on a pin reveals its name or observation ID, provisional Minecraft X/Z, and research link.
- NEW: Hover ANYWHERE over the map to see the Minecraft X and Z under the mouse.
- NEW: Bottom panel continuously displays /tp @s X ~ Z with those X/Z coordinates.
- Shows a cyan crosshair at the sampled mouse position and a square for player position.
- Completely optional: set KnownWorldAtlasConfig.ENABLED=false and rebuild.

GEOGRAPHICAL ACCURACY — IMPORTANT
- The background is generated from your actual runtime land_mask.png and optional climate.png,
  NOT from the source map with your red handwritten Xs.
- These rasters are the same broad geography inputs used by the mod, so coastlines and
  land/water classifications are intended to align with fresh world generation.
- It is NOT a live terrain or chunk map: micro terrain, buildings, custom modifications and
  chunks already generated under older terrain code are NOT scanned or displayed.
- Named red pins are map-calibrated provisional X/Z positions, NOT physical settlements.
  They can point to empty terrain until Step 3 places the structures.
- Every pixel of the 2048x1357 master map represents ~6 km: mouse readout is a projected
  Minecraft block coordinate, but its geographical precision is limited by map raster,
  mouse sampling and the provisional Wall-based calibration.
- Minecraft Y is not obtainable from the 2D map: the /tp command deliberately uses '~'
  to retain your CURRENT Y. You should choose a safe Y based on current in-game terrain.
- Beware: /tp at a faraway X/Z at your current Y may put you inside terrain, in the air,
  or underwater. Use creative/spectator mode and verify destination before teleporting.

INSTALL
1. Extract this ZIP into D:\GameOfThronesMod2 (merge src/ and tools/).
2. Run: py tools\geodata\build_known_world_atlas.py
3. Run: .\gradlew.bat clean build
4. Run: .\gradlew.bat runClient
5. Enter a Known World save. Press M.
6. Place mouse over map. Read bottom-left CURSOR X, Z and the teleport command.

If you previously installed atlas V1, OVERWRITE its Java files with those in V2.
There is no need to overwrite terrain or simulation source.

DATA STATUS
- 253 source observations, five confirmed crossmap duplicated pairs -> 248 displayed pins.
- 236 have name links. 12 remain pending review; U toggles their visibility.
- None of the pin positions are certified as final construction coordinates; this map
  is the tool intended to let us confirm them before physical construction.

TEST LIMITATION
Python data generation has been checked. Java 26.3 client compilation and rendering
have not been executed here. Send the first compiler error if Gradle rejects an API.
