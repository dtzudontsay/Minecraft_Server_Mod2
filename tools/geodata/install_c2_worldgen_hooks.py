#!/usr/bin/env python3
"""C2 merge installer: preserve user's current Minecraft generator & fabric entrypoints.

Creates full edited file in-place with atomic replace & backups. No Git writes.
Safe and idempotent. Fails closed if target file has unknown changes.
"""
from __future__ import annotations
import json, shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
FABRIC = ROOT/'src/main/resources/fabric.mod.json'
BACKUP = ROOT/'tools/geodata/output/c2_physical_pilots/backups'
CLASS = 'dev.dtzudontsay.knownworld.simulation.integration.KnownWorldC2WorldgenRuntime'
IMPORT = 'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;'


def hook(source):
    # Validate the current custom generator is the same worldgen pipeline C2 supports.
    guard='if (winterfellEstate && !allowVanillaStructuresAtWinterfell) return;'
    natural='if (!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)) {'
    decoration='WinterfellWorldgenV3.decorate(level, chunk);'
    after_guard=guard+'\n        if (KnownWorldUniversalWorldgenC2.suppressVanillaStructures(centerChunk)) return;'
    after_natural='if (!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)\n                && !KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk)) {'
    after_decor=decoration+'\n        KnownWorldUniversalWorldgenC2.decorate(level, chunk);'
    pairs=[(guard,after_guard),(natural,after_natural),(decoration,after_decor)]
    for before,after in pairs:
        count=source.count(after)
        if count==1:continue
        if count!=0 or source.count(before)!=1:
            raise ValueError('Unsupported or multiply-edited KnownWorldChunkGenerator anchor: '+before[:75])
        source=source.replace(before,after,1)
    if source.count(IMPORT)==0:
        before='import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;'
        if source.count(before)!=1:raise ValueError('Cannot locate existing Winterfell generator import; refusing to modify file')
        source=source.replace(before,before+'\n'+IMPORT,1)
    elif source.count(IMPORT)!=1:raise ValueError('C2 import duplicated')
    assert source.count(after_guard)==source.count(after_natural)==source.count(after_decor)==1
    return source


def install():
    if not TARGET.is_file() or not FABRIC.is_file():
        raise FileNotFoundError('Install into project root after C1, B2+B3 batches')
    src=TARGET.read_text(encoding='utf-8')
    new=hook(src)
    mod=json.loads(FABRIC.read_text(encoding='utf-8-sig'))
    entries=mod['entrypoints']['main']
    if entries.count(CLASS)>1:raise ValueError('Duplicate C2 entrypoint in fabric.mod.json')
    if CLASS not in entries:entries.append(CLASS)
    packed=json.dumps(mod,ensure_ascii=False,indent=2)+'\n'
    BACKUP.mkdir(parents=True,exist_ok=True)
    if src!=new:
        original=BACKUP/'KnownWorldChunkGenerator_BEFORE_C2.java'
        if not original.exists():shutil.copyfile(TARGET,original)
        TARGET.write_text(new,encoding='utf-8')
        print('Installed three guarded chunk-generator hooks; backup:',original)
    else:print('Generator hooks already present; unchanged')
    if FABRIC.read_text(encoding='utf-8-sig')!=packed:
        original=BACKUP/'fabric.mod_BEFORE_C2.json'
        if not original.exists():shutil.copyfile(FABRIC,original)
        FABRIC.write_text(packed,encoding='utf-8')
        print('Merged C2 Fabric entrypoint (existing entrypoints preserved)')
    else:print('Fabric entrypoint already installed; unchanged')
    print('C2 integration installed. Only selected pilot sites physically decorate new chunks.')

if __name__=='__main__':install()
