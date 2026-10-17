# Known World atlas: 26.3 rendering fix

## Diagnosed problem
The 10-argument `GuiGraphicsExtractor.blit(...)` used by `KnownWorldAtlasScreen` implicitly equates the sampled source rectangle size to the zoomed destination rectangle size. For an atlas of 2048x1357 with zoom and viewport clipping, this draws the WRONG geographic texture pixels. Marker positions and hover coordinates are transformed with the intended scale, creating misleading “red markers in the ocean”.

The 12-argument overload accepts separate `width,height` (destination), `srcWidth,srcHeight` (sample), and `textureWidth,textureHeight` (underlying texture). This fix holds `srcWidth,srcHeight` at 2048x1357. Nothing else changes.

## Installation
Copy `src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java` into the same path inside your repository, overwriting only that Java file.

From the repository root:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Open the Known World preset, press M, press F to view the full map. Zoom and pan; the red markers should now remain anchored to land coastlines as you move the view. Then test `/kwgeo` at a known on-land location (the command is already registered in the repository) to confirm server geography independent from the atlas.

## Caveats
- This file corrects the atlas renderer only. It does not change runtime landmask, existing generated chunks, or coordinate catalogue.
- The canonical black/white mask has text and map-frame artifacts that can become tiny false islands. Clean separately with a verified masking pass, never invert land/water and never shift the entire map.
- A correct atlas and the coordinate math do NOT establish that a saved world was created with the `knownworld:known_world` world preset or that existing chunks were regenerated. Check actual terrain on a backup/test world.
- Minecraft client build/test not possible in this environment. API overload verified from 26.3 javadocs.
