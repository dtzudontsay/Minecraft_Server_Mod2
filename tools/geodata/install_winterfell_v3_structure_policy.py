#!/usr/bin/env python3
"""Idempotently prevent vanilla structure starts near the Winterfell estate.

This only affects our custom KnownWorldChunkGenerator, and only newly-created
chunks within an estate buffer. It leaves vanilla dimensions/worlds alone.
"""
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
MARK='KnownWorld V3: reserve Winterfell estate against vanilla structure starts.'
HOOK='''    @Override
    public void applyBiomeDecoration('''
METHOD='''    /** KnownWorld V3: reserve Winterfell estate against vanilla structure starts.
     * This world-specific custom generator retains vanilla features elsewhere,
     * but ensures pillager outposts and other vanilla structures cannot start
     * on top of the Winterfell castle, winter town or satellite villages.
     * A 4,200-block buffer encloses all existing V3 settlement centres.
     */
    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState state,
            StructureManager structureManager,
            ChunkAccess centerChunk,
            StructureTemplateManager structureTemplateManager,
            ResourceKey<Level> level
    ) {
        final boolean allowVanillaStructuresAtWinterfell = false;
        long x = (long) centerChunk.getPos().getMinBlockX() + 8L;
        long z = (long) centerChunk.getPos().getMinBlockZ() + 8L;
        boolean winterfellEstate = Math.abs(x - (-4_050_173L)) <= 4_200L
                && Math.abs(z - (-1_976_496L)) <= 4_200L;
        if (winterfellEstate && !allowVanillaStructuresAtWinterfell) return;
        super.createStructures(registryAccess,state,structureManager,
                centerChunk,structureTemplateManager,level);
    }

'''
IMPORTS='''import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
'''

def run():
    if not P.exists():raise RuntimeError(f'Missing expected repo file: {P}')
    src=P.read_text('utf-8')
    if MARK in src:
        print('Already patched: vanilla structures suppressed around Winterfell.')
        return
    if src.count(HOOK)!=1 or src.count('import net.minecraft.core.BlockPos;')!=1:
        raise RuntimeError('Unexpected generator source; refusing unsafe automatic edit.')
    # Keep imports in their ordinary position. Do not insert duplicates.
    additions=''.join(line for line in IMPORTS.splitlines(keepends=True)
                     if line.strip() not in src)
    modified=src.replace('import net.minecraft.core.BlockPos;\n',
            'import net.minecraft.core.BlockPos;\n'+additions,1)
    if modified.count(HOOK)!=1:raise RuntimeError('Worldgen hook disappeared unexpectedly')
    modified=modified.replace(HOOK,METHOD+HOOK,1)
    backup=P.with_suffix(P.suffix+'.before-wf3-structure-policy')
    if not backup.exists():backup.write_text(src,'utf-8')
    P.write_text(modified,'utf-8')
    print('Patched',P,'(original backed up)')

if __name__=='__main__':
    try:run()
    except Exception as e:print('ERROR:',e,file=sys.stderr);sys.exit(1)
