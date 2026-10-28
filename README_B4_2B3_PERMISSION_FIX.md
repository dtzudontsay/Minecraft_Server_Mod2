# B4.2B.3 — Command permission API hotfix

The previous B4.2B.3 installer correctly rolled back after Minecraft rejected `CommandSourceStack.hasPermission(int)`.
This repair changes exactly one **staged** source line:

- Before: `.requires(s -> s.hasPermission(4))`
- After: `.requires(Commands.hasPermission(Commands.LEVEL_OWNERS))`

This preserves Minecraft owner-level permission for `/kwstocks grant`.

## Installation

1. Extract this ZIP into `D:\GameOfThronesMod2` (retain its directory structure).
2. Run `RUN_B4_2B3_FIX_COMMAND_PERMISSION.bat`.
3. Run `RUN_B4_2B3_AUTHORITATIVE_OPENING_STOCK.bat` (the original guarded batch).
4. The original installer runs the full Gradle build and snapshots sources/resources.

The hotfix modifies only `B4_2B3_STAGE/updated/.../KnownWorldB42B3StartingStock.java`.
It verifies the old/new byte hashes and that the live Java class is absent, and archives the original stage under `tools/geodata/output/b4_2b3_safe_snapshots/permission_hotfixes/`.

No B4.0 identities, saved worlds, actual inventories, or public-granary resources are modified by the hotfix. The new grant command is not run automatically.

If you see `UNKNOWN_LOCAL_EDITS` or another safe stop, do not manually overwrite the file. Share the output.
