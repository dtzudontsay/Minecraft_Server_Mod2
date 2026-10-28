# C4.12 Atlas Teleport Fix — Corrected Installer (path hotfix)

This is the corrected complete C4.12 package. The earlier Python installer
accidentally used `Path(__file__).resolve().parents[2]`, which looked for
`D:\src\...` rather than `D:\GameOfThronesMod2\src\...`.

Extract ALL files over the repository root **`D:\GameOfThronesMod2`**,
allowing existing C4.12 script files to be overwritten.

The following paths should now exist:
- `D:\GameOfThronesMod2\RUN_C4_12_ATLAS_TELEPORT_FIX.bat`
- `D:\GameOfThronesMod2\tools\install_atlas_teleport_c412.py`
- `D:\GameOfThronesMod2\tools\test_atlas_teleport_c412.py`
- `D:\GameOfThronesMod2\src\main\java\dev\dtzudontsay\knownworld\simulation\casestudy\KnownWorldUiPackets.java` (existing file; installer patches it)

Then in PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_C4_12_ATLAS_TELEPORT_FIX.bat
```

After success, rebuild the mod with your usual Gradle workflow and restart Minecraft.
The patch is idempotent and backs up the Java file once; Minecraft saves are not modified.

The teleport changes only its server landing policy; Creative/gamemaster
permission and world border restrictions remain. It loads the exact clicked X/Z,
uses its heightmap, and teleports approximately five blocks above the sampled surface.

If a different error occurs, send the complete log output.
