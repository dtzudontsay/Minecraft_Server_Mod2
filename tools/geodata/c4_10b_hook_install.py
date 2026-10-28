#!/usr/bin/env python3
"""Idempotently wire C4.10B to the EXISTING generator with audited anchors.

Read-only by default. --install makes a dated backup of the ONE target Java
file and replaces three narrow, exact markers. Does not write world saves.
"""
from __future__ import annotations
import argparse
from datetime import datetime
import difflib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
IMPORT='import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC410BPreviewWorldgen;'


def patch(source):
    if IMPORT in source:
        if (source.count('KnownWorldC410BPreviewWorldgen.') != 3):
            raise ValueError('Unexpected partly patched C4.10B generator: inspect manually')
        return source
    edits=[(
        'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;',
        'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;\n'+IMPORT
    ),(
        '        if (KnownWorldUniversalWorldgenC2.suppressVanillaStructures(centerChunk)) return;',
        '        if (KnownWorldUniversalWorldgenC2.suppressVanillaStructures(centerChunk)\n'
        '                || KnownWorldC410BPreviewWorldgen.suppressVanillaStructures(centerChunk)) return;'
    ),(
        '                && !KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk)) {',
        '                && !KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk)\n'
        '                && !KnownWorldC410BPreviewWorldgen.suppressNaturalDecoration(chunk)) {'
    ),(
        '        KnownWorldUniversalWorldgenC2.decorate(level, chunk);',
        '        KnownWorldUniversalWorldgenC2.decorate(level, chunk);\n'
        '        KnownWorldC410BPreviewWorldgen.decorate(level, chunk);'
    )]
    out=source
    for old,new in edits:
        n=out.count(old)
        if n!=1:raise ValueError(f'Generator hook incompatible: expected ONE ({n} found): {old!r}')
        out=out.replace(old,new,1)
    if out.count('KnownWorldC410BPreviewWorldgen.')!=3:raise ValueError('C410B hook incomplete')
    return out


def run(path=TARGET,install=False):
    path=Path(path)
    current=path.read_text(encoding='utf-8')
    revised=patch(current)
    if revised==current:
        print('C4.10B hooks already installed, no changes required')
        return False
    diff=''.join(difflib.unified_diff(current.splitlines(keepends=True),
        revised.splitlines(keepends=True),fromfile=str(path)+' OLD',tofile=str(path)+' C4.10B'))
    print(diff)
    if install:
        backup=path.with_name(path.name+'.before_c4_10b_'+datetime.now().strftime('%Y%m%d_%H%M%S')+'.bak')
        backup.write_text(current,encoding='utf-8')
        tmp=path.with_name(path.name+'.c4_10b.tmp')
        tmp.write_text(revised,encoding='utf-8')
        tmp.replace(path)
        print('INSTALLED '+str(path)+' | backup '+str(backup))
    else:print('DRY RUN ONLY. Use --install to apply to your local checkout.')
    return True


def main():
    a=argparse.ArgumentParser(description=__doc__)
    a.add_argument('--install',action='store_true')
    a.add_argument('--path',type=Path,default=TARGET)
    args=a.parse_args()
    run(args.path,args.install)

if __name__=='__main__':main()
