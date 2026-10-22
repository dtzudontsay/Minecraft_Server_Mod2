# C4.6.1 — Fixed Minecraft 26.3 gallery permission check + clean retry

**Install only this newest bat. Do not re-run C4.3, C4.5.1, C4.5.2, C4.5.3 or C4.5.4.**

## Install

1. Copy/back up the repository and test worlds. Extract the ZIP into the repository root `D:\GameOfThronesMod2` with `gradlew.bat`, retaining the relative folders.
2. Run `RUN_C4_6_1_INSTALL_KEEP_OPEN.bat` (not older `RUN_` or `RESTORE_` scripts).
3. Enter `NEW_WORLD` to authorize **fresh-world source/data** changes. The script verifies the C4.5.1 V2/V3 manifests, runs geometry/Java preflight, snapshots five original files, applies all changes and runs `gradlew.bat build`. **If the build fails, it restores those five source/data files and removes the newly installed gallery Java classes.** Full output: `C4_6_1_INSTALL_AND_BUILD.log`.
4. Do not launch Minecraft if Gradle reports failure. If success: `./gradlew.bat runClient` in PowerShell (`.\gradlew.bat runClient`).
5. For **Winterfell**, make a fresh Known World save, and teleport to `/tp @s -4050173 200 -1976496`. Check `/kwinterfell3 status` for plot count >1,913. Keep render distance 6–8 and Distant Horizons background distant generation disabled for initial review.
6. For **the gallery**, make a DIFFERENT fresh **Superflat Creative** world with cheats/commands. Face empty land, run `/kwgallery build`. Construction proceeds gradually (61 alphabetical C3 templates in a 7-column grid). `/kwgallery status` reports active progress, `/kwgallery cancel` halts remaining construction. Walk/teleport around to review each labeled module. Download/export of the entire original C3 catalogue appears under `tools\geodata\output\c4_6_winterfell\C46_ALL_BUILDINGS_REVIEW.csv`.

## What you'll see

- Winterfell's outer homes and workplaces redistributed in a ring surrounding the unmodified castle. Existing 1,136 household IDs, 5,000 unique individuals, specialty worksite definitions, 128 visible NPC cap and previous 12 castle plot replacements are retained.
- An outer belt of 24 substantial 64x64 cultivated field plots — **review-only farmland**, not a complete food/production economy.
- Signs on newly generated special buildings, written gradually on live server ticks. If a label isn't filled immediately, it may take a few ticks after coming close. Some far-away blank signs can persist across restarts until a future durable sign reconciliation system is added.
- All **61 universal C3 template types** in a separate superflat inspection area, including templates too geographically restricted to spawn in Winterfell. The six special Winterfell V3 original variants use a separate procedural renderer and aren't faked as C3 templates in this gallery.

## Important limits

This changes newly generated chunks ONLY. Do not test on already-visited Winterfell chunks. Signs, especially in large rural belts, still need full restart-proof persistence. Superflat gallery intentionally modifies terrain in the *gallery* world, so use a disposable test save. Castle room-specific navigation and actual employee-to-workplace binding are beyond this batch.

## Why this is a new installer

The original C4.6 failed at `KnownWorldBlueprintGallery.java:52` because `CommandSourceStack.hasPermission(2)` is unavailable in Minecraft 26.3. This package checks `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` instead, preserving the original command permission requirement. Fabric's current command documentation describes this pattern.

The prior unsuccessful C4.6 attempt correctly restored the old mod source and scenario files but left an audit/rollback snapshot directory. C4.6.1 uses **a new timestamped snapshot folder for each attempt**, so no manual deletion of old backups is necessary. Each retry still backs up and restores the five original files if the build fails. Run only the C4.6.1 batch; it includes all the original C4.6 files and needs no earlier installer.

The command gallery and the Winterfell compact terrain have NOT yet been validated in-game as of packaging. If the build fails again, share `C4_6_1_INSTALL_AND_BUILD.log` rather than making manual source changes.
