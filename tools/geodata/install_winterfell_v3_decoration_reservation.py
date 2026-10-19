#!/usr/bin/env python3
"""Guarded and idempotent change to the existing KnownWorldChunkGenerator.

Only disables vanilla biome decoration inside and immediately around V3
settlement plots/roads. Does not affect vanilla dimensions, construction data,
or other Known World regions. Apply on the source after V3 Stages 1-3.
"""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
OLD='''        super.applyBiomeDecoration(
                level,
                chunk,
                structureManager
        );'''
NEW='''        // Winterfell V3: reserved settlement chunks must not receive natural
        // trees/lava lakes from this or adjacent biome-decoration passes.
        // The V3 overlay still runs immediately after this conditional.
        if (!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)) {
            super.applyBiomeDecoration(
                    level,
                    chunk,
                    structureManager
            );
        }'''

def run():
    if not P.is_file():raise RuntimeError(f'Generator not found: {P}')
    text=P.read_text('utf-8')
    if NEW in text:
        print('OK: decoration reservation was already installed')
        return
    if text.count(OLD)!=1 or 'WinterfellWorldgenV3.decorate(level, chunk);' not in text:
        raise RuntimeError('V3 worldgen hook missing or unexpected source. Run V3 installer first; no changes made.')
    if 'import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;' not in text:
        raise RuntimeError('Missing V3 import. Check existing V3 setup; no changes made.')
    backup=P.with_suffix(P.suffix+'.before-wf3-decoration-reservation')
    if not backup.exists():backup.write_text(text,'utf-8')
    P.write_text(text.replace(OLD,NEW,1),'utf-8')
    print('OK: Winterfell settlement biome-decoration reservation installed')

if __name__=='__main__':
    try:run()
    except Exception as exc:print('ERROR:',exc,file=sys.stderr);sys.exit(1)
