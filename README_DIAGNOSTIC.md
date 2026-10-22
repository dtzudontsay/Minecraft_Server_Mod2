# C4.5 local install diagnosis (read-only inspection)

Extract this ZIP **into the root of `Minecraft_Server_Mod2`**, alongside `gradlew.bat`.

1. Double-click `RUN_C4_5_STATUS_KEEP_OPEN.bat`. The window remains open, and `C4_5_INSTALL_AUDIT.log` is written beside the script. This does **not** change Java, scenario files, or Minecraft saves.
2. Run `RUN_C4_5_BUILD_AND_CAPTURE.bat`. It runs `gradlew.bat build --console=plain --stacktrace`, keeps the window open, and writes `C4_5_BUILD_CHECK.log`. It compiles code but does **not** rerun the risky WORLD generator or edit saves.
3. Send both `.log` files if either shows a failure. Don't rerun the original `WORLD` installer until the status and error have been diagnosed.

## Meaning of the audit markers

- C4 resource has `C4_5_NEW_CHUNK_GENERATION_C4_4_DISTRIBUTIONS` and `site count > 2`: **WORLD** mode installed.
- C4 resource has that purpose and `site count == 2`: only **PILOTS** mode installed.
- Winterfell V2 residents have `occupationPlanVersion=45`: its saved *source roster* has diverse occupations. This is different from an existing world save, where old profiles remain.
- Winterfell V3 has `c45`, `total plots > 1514`, and `C3:` entries: additional specialty workplaces are actually indexed for **new-chunk** worldgen.
- Java `KnownWorldStructureSigns` markers are present: sign code has been installed, **not** that the build passed or signs rendered.
- `/kwinterfell3 status` **in Minecraft** tells you the building count the running game loaded. If it reports `1514`, you are running the old V3 structure index even if some source files on disk changed.

## Why the screenshot can look unchanged

The original Winterfell already plans **1,136 homes for exactly 5,000 residents**. C4.5 did NOT replace those homes, restructure the castle, enlarge the walls, or move existing streets. C4.5's Winterfell physical addition consists of **specialized workplaces outside or around the existing districts**, not housing expansion. `WORLD` mode still excludes `MAJOR` generic C1 sites; Winterfell has a separate V3 pipeline.

Most importantly, **already-generated Minecraft chunks never regenerate from a new generator**. Test in a newly created save, not an old save with a newly explored village. Also ensure you launched the just-built `gradlew.bat runClient` rather than a previously exported JAR.

**Known risk in original C4.5 `WORLD` installer:** it builds large detailed housing, road and field chunk indexes for every normal SMALL/MEDIUM site in one Python process, retains the plans until the end, and currently uses a quadratic lot-collision scan. On some PCs, this can consume significant RAM/time or fail before Winterfell's specialty overlay and signage are installed. A partial install is therefore possible. The audit detects this; it does not silently mark such an installation successful.

**Do not send world-save data or personal file paths unless needed**; both logs are typically sufficient. The logs should be inspected for sensitive local paths or other private details before sharing if preferred.
