#!/usr/bin/env python3
"""Surgical recovery: defer C4.5.1 sign text updates until a server chunk is live.

Only modifies KnownWorldStructureSigns.java and KnownWorld.java, after validating
that C4.5.1's exact vulnerable helper is installed. Creates backups. Does not
change any generated scenario JSON, characters, holdings, saves or layouts.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'src/main/java/dev/dtzudontsay/knownworld'
SIGN = SRC / 'world/generation/settlement/KnownWorldStructureSigns.java'
MAIN = SRC / 'KnownWorld.java'
MARK = 'C4_5_2_DEFERRED_SIGNS'

SIGN_REPLACEMENT = r'''package dev.dtzudontsay.knownworld.world.generation.settlement;

import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C4_5_2_DEFERRED_SIGNS: sign blocks are placed during chunk worldgen; text
 * updates are deferred to Fabric's SERVER chunk-load callback (live Level).
 * Do not call SignBlockEntity.setText during applyBiomeDecoration: the sign's
 * `level` reference is NULL there and Minecraft 26.3 will crash.
 *
 * Note: queue entries exist only during this game session. A chunk generated
 * offline by another tool and saved without ever being loaded may retain an
 * empty sign until a future persistent-sign migration. This batch is a
 * correctness/stability hotfix for the visual-generation test.
 */
public final class KnownWorldStructureSigns {
    private static final int MAX_PENDING_SIGNS = 50000;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final AtomicInteger PENDING_COUNT = new AtomicInteger();
    private static final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Pending>> PENDING
            = new ConcurrentHashMap<>();

    private record Pending(BlockPos pos, SignText text) { }

    private KnownWorldStructureSigns() { }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    /** Must be called once in KnownWorld.onInitialize(), before any chunks load. */
    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (!Level.OVERWORLD.equals(level.dimension())) return;
            long k = key(chunk.getPos().x(), chunk.getPos().z());
            var jobs = PENDING.remove(k);
            if (jobs == null) return;
            int assigned = 0;
            int skipped = 0;
            Pending next;
            while ((next = jobs.poll()) != null) {
                PENDING_COUNT.decrementAndGet();
                // Only access BlockEntity once the chunk has a real ServerLevel.
                if (level.getBlockEntity(next.pos()) instanceof SignBlockEntity sign
                        && sign.getLevel() != null) {
                    sign.setText(next.text(), SignTextSlot.FRONT);
                    assigned++;
                } else {
                    skipped++;
                }
            }
            if (skipped > 0) {
                KnownWorld.LOGGER.warn("C4.5.2 sign text deferred: {} applied, {} missing at chunk load {} {}",
                        assigned, skipped, chunk.getPos().x(), chunk.getPos().z());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.clear();
            PENDING_COUNT.set(0);
        });
        KnownWorld.LOGGER.info("C4.5.2 safe sign-text handler registered (after chunk load)");
    }

    public static void attach(ChunkAccess chunk, int x, int z, int width, int depth,
                              int floor, String face, String buildingType) {
        if (buildingType == null || buildingType.isBlank()
                || "HOUSE".equals(buildingType) || "FIELD".equals(buildingType)
                || buildingType.startsWith("HOUSE_") || "COTTAGE".equals(buildingType)
                || "CASTLE_QUARTER".equals(buildingType)) return;
        boolean north = "NORTH".equals(face);
        boolean outdoors = switch (buildingType) {
            case "GARDEN", "PASTURE", "WELL", "COURTYARD", "RITUAL_GROUND",
                 "RUIN", "MONUMENT" -> true;
            default -> false;
        };
        int sx = outdoors ? x + 1 : x + Math.min(width - 2, width / 2 + 2);
        int sz = outdoors ? z + 1 : north ? z - 1 : z + depth;
        BlockPos p = new BlockPos(sx, floor + (outdoors ? 1 : 3), sz);
        if ((sx >> 4) != chunk.getPos().x() || (sz >> 4) != chunk.getPos().z()) return;
        Direction facing = north ? Direction.NORTH : Direction.SOUTH;
        BlockState state = outdoors
                ? Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8)
                : Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        chunk.setBlockState(p, state, 0);

        // Safe in a detached ChunkAccess: DO NOT call entity.setText here.
        chunk.setBlockEntity(new SignBlockEntity(p, state));

        String name = buildingType.replace("C3:", "").replace('_', ' ').trim();
        List<String> words = List.of(name.split(" +"));
        List<String> rows = new ArrayList<>();
        String line = "";
        for (String word : words) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > 14) {
                rows.add(line);
                line = "";
            }
            if (!line.isEmpty()) line += " ";
            line += word;
        }
        if (!line.isEmpty()) rows.add(line);
        var text = SignText.EMPTY.asMutable();
        for (int i = 0; i < Math.min(4, rows.size()); i++) {
            String shown = rows.get(i);
            if (shown.length() > 15) shown = shown.substring(0, 15);
            text.setLine(i, Component.literal(shown));
        }
        if (PENDING_COUNT.incrementAndGet() > MAX_PENDING_SIGNS) {
            PENDING_COUNT.decrementAndGet();
            // Fail safe: an unlabeled sign is better than a crash or unbounded queue.
            return;
        }
        PENDING.computeIfAbsent(key(p.getX() >> 4, p.getZ() >> 4),
                ignored -> new ConcurrentLinkedQueue<>())
                .add(new Pending(p, text.asImmutable()));
    }
}
'''

IMPORT_LINE = 'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldStructureSigns;\n'
CALL_ANCHOR = '        KnownWorldChunkGenerators.register();\n'
CALL_LINE = '        KnownWorldStructureSigns.register();\n'


def prepare() -> list[tuple[Path, str]]:
    if not SIGN.is_file() or not MAIN.is_file():
        raise RuntimeError('Missing generated C4.5.1 sign helper or mod entrypoint. Run C4.5.1 installation first.')
    sign_source = SIGN.read_text(encoding='utf-8')
    main_source = MAIN.read_text(encoding='utf-8')
    if MARK in sign_source:
        if IMPORT_LINE not in main_source or CALL_LINE not in main_source:
            raise RuntimeError('C4.5.2 helper is present but startup hook is missing. Refusing mixed install.')
        print('ALREADY INSTALLED: C4.5.2 safe signage and startup hook')
        return []
    # Refuse to overwrite unrelated/custom sign implementations.
    vulnerable = 'entity.setText(text.asImmutable(), SignTextSlot.FRONT);'
    if 'public final class KnownWorldStructureSigns' not in sign_source or sign_source.count(vulnerable) != 1:
        raise RuntimeError('Unexpected sign helper version; stop and inspect it instead of overwriting.')
    if 'chunk.setBlockEntity(entity);' not in sign_source:
        raise RuntimeError('Sign helper is not the vulnerable C4.5.1 version; refusing overwrite.')
    if CALL_LINE in main_source or IMPORT_LINE in main_source:
        raise RuntimeError('Partial/unexpected KnownWorld registration already exists; refusing to duplicate.')
    if main_source.count(CALL_ANCHOR) != 1:
        raise RuntimeError('Unexpected KnownWorld.java initialization; refusing to guess where to register.')
    anchor_import = 'import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;\n'
    if main_source.count(anchor_import) != 1:
        raise RuntimeError('Unexpected KnownWorld.java import layout; refusing to guess where to inject.')
    updated_main = main_source.replace(anchor_import, IMPORT_LINE + anchor_import, 1)
    updated_main = updated_main.replace(CALL_ANCHOR, CALL_ANCHOR + '\n' + CALL_LINE, 1)
    return [(SIGN, SIGN_REPLACEMENT), (MAIN, updated_main)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--install', action='store_true')
    args = ap.parse_args()
    updates = prepare()
    if not updates:
        return
    for file, content in updates:
        print('READY:', file.relative_to(ROOT), 'bytes=', len(content))
    if not args.install:
        print('PREFLIGHT ONLY: No files changed.')
        return
    backup_paths = []
    for file, _ in updates:
        backup = Path(str(file) + '.before_c4_5_2_sign_fix.bak')
        if not backup.exists():
            shutil.copy2(file, backup)
        backup_paths.append(backup)
        print('BACKUP:', backup.relative_to(ROOT))
    for file, content in updates:
        file.write_text(content, encoding='utf-8')
        print('UPDATED:', file.relative_to(ROOT))
    print('SIGN HOTFIX INSTALLED. Now run Gradle build and test in a FRESH WORLD.')


if __name__ == '__main__':
    main()
