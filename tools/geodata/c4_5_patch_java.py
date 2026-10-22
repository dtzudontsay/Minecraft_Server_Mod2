#!/usr/bin/env python3
"""Guarded, reversible source-only Java bridge for C4.5.

Requires target exact known C4.3 code; refuses unmatched versions. Only calls
new-chunk decorators, never edits old saves or spawns villagers. Preview mode
prints matches. --install backs up originals before installing patches.
"""
from __future__ import annotations
import argparse, os, re
from pathlib import Path
from c4_5_connected_rollout import ROOT,backup_and_install
PKG=ROOT/'src/main/java/dev/dtzudontsay/knownworld'
C2=PKG/'world/generation/settlement/KnownWorldUniversalWorldgenC2.java'
C3=PKG/'world/generation/settlement/KnownWorldC3Architecture.java'
V3=PKG/'world/generation/settlement/WinterfellWorldgenV3.java'
SIGNS=PKG/'world/generation/settlement/KnownWorldStructureSigns.java'
V3NPC=PKG/'simulation/casestudy/WinterfellV3Runtime.java'

def once(s,old,new,path):
    if new in s:return s
    n=s.count(old)
    if n!=1:raise ValueError(f'{path}: expected exactly one anchor ({n} found): '+old[:95])
    return s.replace(old,new,1)

SIGN_CLASS=r'''package dev.dtzudontsay.knownworld.world.generation.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import java.util.Locale;

/** New-chunk ONLY: visible public workplace type names, never invented household labels. */
public final class KnownWorldStructureSigns {
    private KnownWorldStructureSigns() { }

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
        // Walls get an exterior plaque; open-air plots get a standing signpost.
        int sx = outdoors ? x + 1 : x + Math.min(width - 2, width / 2 + 2);
        int sz = outdoors ? z + 1 : north ? z - 1 : z + depth;
        BlockPos p = new BlockPos(sx, floor + (outdoors ? 1 : 3), sz);
        if ((sx >> 4) != chunk.getPos().x() || (sz >> 4) != chunk.getPos().z()) return;
        Direction facing = north ? Direction.NORTH : Direction.SOUTH;
        BlockState state = outdoors
                ? Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8)
                : Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        chunk.setBlockState(p, state, 0);
        SignBlockEntity entity = new SignBlockEntity(p, state);
        String name = buildingType.replace("C3:", "").replace('_', ' ').trim();
        // Four short lines avoid rendering unreadable overflowing sign text.
        List<String> words = List.of(name.split(" +"));
        List<String> rows = new ArrayList<>();
        String line = "";
        for (String word : words) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > 14) {
                rows.add(line); line = "";
            }
            if (!line.isEmpty()) line += " ";
            line += word;
        }
        if (!line.isEmpty()) rows.add(line);
        // Minecraft 26.3 sign API uses SignTextSlot and mutable sign text.
        var text = SignText.EMPTY.asMutable();
        for (int i = 0; i < Math.min(4, rows.size()); i++) {
            String shown = rows.get(i);
            if (shown.length() > 15) shown = shown.substring(0, 15);
            text.setLine(i, Component.literal(shown));
        }
        entity.setText(text.asImmutable(), SignTextSlot.FRONT);
        chunk.setBlockEntity(entity);
    }
}
'''


def patch_c2(src):
    src=once(src,'root.getAsJsonObject("summary").get("pilotCount").getAsInt() != 2)',
        'root.getAsJsonObject("summary").get("pilotCount").getAsInt() != root.getAsJsonArray("pilots").size())',C2)
    src=once(src,'if (all.size() != 2) throw new IllegalStateException("C2 must have exactly two pilots");',
        'if (all.size() != root.getAsJsonObject("summary").get("pilotCount").getAsInt())\n                throw new IllegalStateException("C4.5 site index count disagrees with manifest");',C2)
    # No disabling the Winterfell and special-worldgen exclusions.
    assert 'Cannot override Winterfell V3' in src
    return src


def patch_c3(src):
    anchor='''        if ("FIELD".equals(lot.kind()))
            throw new IllegalArgumentException("C2 farmland remains its own safe field renderer");
        if (template.openAir()) {
            buildOutdoor(c, lot, floor, template);
        } else if ("SHELTER".equals(template.family())) {
            buildShelter(c, lot, floor, template);
        } else {
            buildHouse(c, lot, floor, palette, template);
        }
'''
    replacement=anchor+'''        // C4.5 - instance-local public building signs for new chunks only.
        KnownWorldStructureSigns.attach(c, lot.x(), lot.z(), lot.width(),
                lot.depth(), floor, lot.face(), requested);
'''
    return once(src,anchor,replacement,C3)


def patch_v3(src):
    src=once(src,'''private record Building(String id, String template, int x, int z,
                            int width, int depth, int capacity) { }''',
    '''private record Building(String id, String template, int x, int z,
                            int width, int depth, int capacity, String label) { }''',V3)
    src=once(src,'''j.get("z").getAsInt(), j.get("w").getAsInt(),
                            j.get("d").getAsInt(), j.get("cap").getAsInt()));''',
    '''j.get("z").getAsInt(), j.get("w").getAsInt(),
                             j.get("d").getAsInt(), j.get("cap").getAsInt(),
                             j.has("label") ? j.get("label").getAsString() : j.get("t").getAsString()));''',V3)
    anchor='''    private static void buildingInChunk(ChunkAccess chunk, Building b, int chunkX0, int chunkZ0) {
        int x0=X+b.x, z0=Z+b.z;'''
    replacement='''    private static void buildingInChunk(ChunkAccess chunk, Building b, int chunkX0, int chunkZ0) {
        // C4.5 specialized new-chunk structures; no change to original 1514 lots.
        if (b.template.startsWith("C3:")) {
            int bx = X + b.x, bz = Z + b.z;
            int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
            for (int dx : new int[] {0, b.width / 2, b.width - 1}) {
                for (int dz : new int[] {0, b.depth / 2, b.depth - 1}) {
                    if (!naturalLand(bx + dx, bz + dz)) return;
                    int y = naturalTopY(bx + dx, bz + dz);
                    low = Math.min(low, y); high = Math.max(high, y);
                }
            }
            if (high - low > 6) return;  // no cliff foundations
            var plot = new KnownWorldUniversalWorldgenC2.Lot(b.id,
                    b.template.substring(3), bx, bz, b.width, b.depth,
                    "SOUTH", 0, 0);
            KnownWorldC3Architecture.buildLot(chunk, plot, buildingFloor(b),
                    "NORTHERN", "MEDIUM");
            return;
        }
        int x0=X+b.x, z0=Z+b.z;'''
    src=once(src,anchor,replacement,V3)
    # Add labels for the original generic workplaces as well; homes remain unsigned.
    original='''            buildingInChunk(chunk, b, wx, wz);'''
    changed='''            buildingInChunk(chunk, b, wx, wz);
            if (!b.template.startsWith("C3:") && !b.id.contains("_home_")) {
                KnownWorldStructureSigns.attach(chunk,
                        X + b.x, Z + b.z, b.width, b.depth,
                        buildingFloor(b), "SOUTH", b.label);
            }'''
    src=once(src,original,changed,V3)
    return src


def patch_npc(src):
    # The population remains exactly 5000; projection budget is deliberately
    # bounded to avoid loading 5000 villagers simultaneously.
    src=once(src,'public static final int FULL_BUDGET = 128;',
                  'public static final int FULL_BUDGET = 384;',V3NPC)
    src=once(src,'public static final int REDUCED_BUDGET = 128;',
                  'public static final int REDUCED_BUDGET = 256;',V3NPC)
    return src


def main():
    ap=argparse.ArgumentParser();ap.add_argument('--install',action='store_true');
    ap.add_argument('--npc-visual-stress',action='store_true');a=ap.parse_args()
    if a.install and os.environ.get('KNOWNWORLD_C45_CONFIRM')!='NEW_WORLD':
        raise SystemExit('Set KNOWNWORLD_C45_CONFIRM=NEW_WORLD')
    files=[(C2,patch_c2),(C3,patch_c3),(V3,patch_v3)]
    if a.npc_visual_stress:files.append((V3NPC,patch_npc))
    patched=[]
    for path,fn in files:
        old=path.read_text(encoding='utf-8')
        new=fn(old)
        patched.append((path,new))
        print(('UNCHANGED' if old==new else 'PATCH READY'),path.relative_to(ROOT))
    if SIGNS.exists() and SIGNS.read_text(encoding='utf-8')!=SIGN_CLASS:
        raise ValueError('Refusing to replace existing custom signage implementation')
    if a.install:
        for path,new in patched:backup_and_install(path,new)
        if not SIGNS.exists():backup_and_install(SIGNS,SIGN_CLASS)
    else: print('READ-ONLY preflight; pass --install to write (with backups)')

if __name__=='__main__':main()
