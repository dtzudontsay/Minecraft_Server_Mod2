# C4.5.4 — Reversible safe worldgen isolation (Minecraft 26.3)

## Symptom and evidence

Screenshot after teleport shows a sky/void without loaded terrain; teleport commands stop responding. This is consistent with an integrated-server chunk generation backlog or deadlock; screenshot **cannot identify** the exact root cause. Previous crash from C4.5.1 was `SignBlockEntity.setText` invoked with `level == null`, but the subsequent C4.5.2 patch uses `ServerChunkEvents.CHUNK_LOAD` and calls `level.getBlockEntity(...)` and `setText(...)` while the chunk-load event fires. This could cause blocking or re-entrant chunk work; no fresh `latest.log` or thread dump has yet established that. Large initial structure generation and a 384-entity projection budget are additional load factors. Previous observed 1913 Winterfell plots / 8278 indexed chunks; **index size does not mean all loaded at once**.

## What this test changes

- Safely DISABLES experimental sign generation and deferred sign processing (default off) by adding an early-return in `KnownWorldStructureSigns.attach(...)`. Leaves class/event hook intact, with no new sign jobs queued by generation. **Building signs will be absent.** We will restore signs with a robust design after identifying the stall.
- Returns Winterfell visible NPC projection caps to the previous 128 / 128, rather than 384 / 256; this does **not** delete any simulated NPCs. Retains exactly 5000 winterfell residents / 1136 homes in scenario data.
- Does **not** change town layouts, block blueprints, C4.4 occupation allocations, persistent identities, farms, structure counts, world saves, or GitHub.
- Creates `.before_c4_5_4_safe_probe.bak` source backups and includes a guarded restoration script. Cannot be used as the finished sign architecture: this is a deliberately reversible diagnostic fallback.

## Instructions

1. Back up Minecraft saves. Extract all files next to `gradlew.bat`, likely `D:\GameOfThronesMod2`.
2. Run `RUN_C4_5_4_SAFE_PROBE_KEEP_OPEN.bat` and verify `BUILD SUCCESSFUL`. It logs failures to `C4_5_4_SAFE_PROBE_BUILD.log`.
3. In game, **disable Distant Horizons' distant generation** (or choose `PRE_EXISTING_ONLY`) and select Render Distance 6–8, Simulation Distance 4–6. This does not require deleting Distant Horizons.
4. Create a **brand-new world**. First, use the game's settings before the Winterfell teleport. Teleport to Winterfell at `-4050173 200 -1976496` with Creative or Spectator Mode.
5. If the player appears in empty sky, avoid repeated teleports. If the game still has a responsive F3 display, watch the Integrated Server MSPT for a short interval. Save `run/logs/latest.log`. If unresponsive, close safely if possible and use `CAPTURE_NEXT_STALL_LOG_KEEP_OPEN.bat` to extract the last 500 log lines. Also attach the newest crash report if it exists.
6. If stable, signs or the heavy projection budget contributed to the issue; to tell them apart we can selectively re-enable them and add bounded main-thread processing. It does not yet prove the sign event was the sole cause.
7. Never use `RESTORE_C4_5_4_SAFE_PROBE_KEEP_OPEN.bat` as a fix: it simply rolls back this *diagnostic* patch and might reinstate the unsafe event behavior. Only restore after a separate validated fix.

The 24 castle-home relocations, 12 replacements, remaining 1913 total plots, jobs, characters, and future NPC/settlement life cycles are unchanged.
