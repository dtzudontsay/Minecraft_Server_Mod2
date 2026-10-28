#!/usr/bin/env python3
"""Install worldwide C4.11 hooks into existing generator, preserving C4.10B.

Read-only validation by default; --install writes ONE backed-up Java file.
Also backs up/disables any stale C4.10B preview resource when installed.
"""
from pathlib import Path
from datetime import datetime
import argparse,difflib,shutil
ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
PREVIEW=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_10b_preview_v1.json'
IMPORT='import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC411GlobalWorldgen;'

def patch(text):
    if IMPORT in text:
        if text.count('KnownWorldC411GlobalWorldgen.')!=3:
            raise ValueError('Partly patched global generator; inspect before installing')
        return text
    edits=[(
        'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC410BPreviewWorldgen;',
        'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC410BPreviewWorldgen;\n'+IMPORT),(
        '                || KnownWorldC410BPreviewWorldgen.suppressVanillaStructures(centerChunk)) return;',
        '                || KnownWorldC410BPreviewWorldgen.suppressVanillaStructures(centerChunk)\n'
        '                || KnownWorldC411GlobalWorldgen.suppressVanillaStructures(centerChunk)) return;'),(
        '                && !KnownWorldC410BPreviewWorldgen.suppressNaturalDecoration(chunk)) {',
        '                && !KnownWorldC410BPreviewWorldgen.suppressNaturalDecoration(chunk)\n'
        '                && !KnownWorldC411GlobalWorldgen.suppressNaturalDecoration(chunk)) {'),(
        '        KnownWorldC410BPreviewWorldgen.decorate(level, chunk);',
        '        KnownWorldC410BPreviewWorldgen.decorate(level, chunk);\n'
        '        KnownWorldC411GlobalWorldgen.decorate(level, chunk);')]
    revised=text
    for before,after in edits:
        count=revised.count(before)
        if count!=1:raise ValueError(f'Expected 1 C4.10B anchor; saw {count}: {before!r}')
        revised=revised.replace(before,after,1)
    if revised.count('KnownWorldC411GlobalWorldgen.')!=3:raise AssertionError('Incomplete C4.11 patch')
    return revised

def run(path=TARGET,install=False,preview=PREVIEW):
    path=Path(path);preview=Path(preview)
    source=path.read_text(encoding='utf-8')
    revised=patch(source)
    if source!=revised:
        diff=''.join(difflib.unified_diff(source.splitlines(keepends=True),
            revised.splitlines(keepends=True),fromfile='BEFORE',tofile='C4.11'))
        print(diff)
    if not install:
        print('DRY RUN: no changes installed')
        return
    if preview.exists():
        backup=preview.with_name(preview.name+'.disabled_before_c4_11_'+datetime.now().strftime('%Y%m%d_%H%M%S'))
        preview.rename(backup)
        print('Deactivated old site-preview manifest:',backup)
    if revised!=source:
        backup=path.with_name(path.name+'.before_c4_11_'+datetime.now().strftime('%Y%m%d_%H%M%S')+'.bak')
        shutil.copy2(path,backup)
        tmp=path.with_name(path.name+'.c411.tmp')
        tmp.write_text(revised,encoding='utf-8')
        tmp.replace(path)
        print('INSTALLED '+str(path)+' (backup '+str(backup)+')')
    else:print('C4.11 hooks already installed; no source change')

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--install',action='store_true');p.add_argument('--path',type=Path,default=TARGET)
    p.add_argument('--preview',type=Path,default=PREVIEW)
    a=p.parse_args();run(a.path,a.install,a.preview)
if __name__=='__main__':main()
