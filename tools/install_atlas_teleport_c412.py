#!/usr/bin/env python3
"""Install strict, repeatable atlas teleport repair without touching saves."""
from pathlib import Path
import argparse
import shutil

# Installer lives in <repository>/tools/, so its parent is the repo root.
# parents[2] incorrectly pointed to D:\ instead of D:\GameOfThronesMod2.
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/KnownWorldUiPackets.java'
START = '    private static void teleport(\n'
END = '    private static boolean withinBorder(\n'
MARKER = 'C4_12_ATLAS_DIRECT_TERRAIN_ARRIVAL'

NEW_METHOD = '''    /** C4_12_ATLAS_DIRECT_TERRAIN_ARRIVAL
     * The atlas coordinate is authoritative: never reject a destination simply
     * because nearby blocks cannot fit a player or a rescue platform.
     * Only actual world limits and permissions can reject the request.
     */
    private static void teleport(
            ServerPlayer player,
            MinecraftServer server,
            int requestedX,
            int requestedZ
    ) {
        if (!player.isCreative()
                || !Commands.LEVEL_GAMEMASTERS.check(
                        player.createCommandSourceStack().permissions())) {
            player.sendSystemMessage(Component.literal(
                    "[Atlas] Teleport requires Creative mode and gamemaster permissions."));
            return;
        }

        long now = System.nanoTime();
        Long previous = lastTeleportNanos.get(player.getUUID());
        if (previous != null && now - previous < TELEPORT_GAP_NANOS) {
            player.sendSystemMessage(Component.literal(
                    "[Atlas] Please wait three seconds between teleports."));
            return;
        }

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            player.sendSystemMessage(Component.literal("[Atlas] Overworld is not available."));
            return;
        }
        if (Math.abs((long) requestedX) > 29_999_980L
                || Math.abs((long) requestedZ) > 29_999_980L
                || !withinBorder(level, requestedX, requestedZ)) {
            player.sendSystemMessage(Component.literal(
                    "[Atlas] Destination is outside the world border."));
            return;
        }

        // Force the destination chunk to be fully generated before sampling.
        // A heightmap queried from an ungenerated chunk is not a reliable
        // source for a cross-continent atlas teleport.
        try {
            level.getChunk(requestedX >> 4, requestedZ >> 4);
            // MOTION_BLOCKING_NO_LEAVES yields a Y above terrain/water/buildings
            // without requiring any free-space or collision survey. A modest
            // vertical clearance lets Creative players inspect the structure.
            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    requestedX, requestedZ);
            int arrivalY = Math.max(level.getMinY() + 4,
                    Math.min(level.getMaxY() - 4, surfaceY + 5));

            boolean success = player.teleportTo(
                    level,
                    requestedX + 0.5,
                    arrivalY,
                    requestedZ + 0.5,
                    Set.<Relative>of(),
                    player.getYRot(),
                    player.getXRot(),
                    false);
            if (success) {
                // Creative flyers arrive ready to inspect even above water or
                // an irregular fortification. No placement blocks are changed.
                player.setDeltaMovement(0.0, 0.0, 0.0);
                lastTeleportNanos.put(player.getUUID(), now);
                player.sendSystemMessage(Component.literal(
                        "[Atlas] Teleported to X=" + requestedX
                        + " Y=" + arrivalY + " Z=" + requestedZ
                        + " (5 blocks above terrain surface)."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "[Atlas] Minecraft rejected the teleport after chunk generation."));
            }
        } catch (RuntimeException failure) {
            player.sendSystemMessage(Component.literal(
                    "[Atlas] Destination chunk or terrain lookup failed: "
                    + failure.getClass().getSimpleName()));
        }
    }

'''


def patched(source: str) -> str:
    if MARKER in source:
        return source
    if source.count(START) != 1 or source.count(END) != 1:
        raise ValueError('Unexpected teleport server source layout; no files changed')
    start = source.index(START)
    end = source.index(END, start)
    existing = source[start:end]
    for token in ('findNaturalLanding(', 'makeRescuePlatform(', 'player.teleportTo(',
                  'lastTeleportNanos', 'LEVEL_GAMEMASTERS'):
        if token not in existing:
            raise ValueError('Missing expected original teleport logic: ' + token)
    return source[:start] + NEW_METHOD + source[end:]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    args = ap.parse_args()
    if not TARGET.is_file():
        raise SystemExit('ERROR: source not found: ' + str(TARGET))
    source = TARGET.read_text(encoding='utf-8')
    changed = patched(source)
    if args.check:
        print('C4.12 atlas teleport:', 'already installed' if source == changed else 'ready to install')
        return
    if source == changed:
        print('C4.12 atlas teleport already installed; no changes made.')
        return
    backup = TARGET.with_suffix('.java.before_c4_12_atlas.bak')
    if not backup.exists():
        shutil.copy2(TARGET, backup)
    TARGET.write_text(changed, encoding='utf-8')
    print('Installed terrain-height teleport repair:', TARGET.relative_to(ROOT))
    print('Backup:', backup.relative_to(ROOT))

if __name__ == '__main__':
    main()
