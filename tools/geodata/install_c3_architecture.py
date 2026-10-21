#!/usr/bin/env python3
"""Install the C3 renderer in-place without replacing local project files.

Atomic-ish fail-closed text anchors and backups. Never modifies Git remotely.
Run after C2: its two-pilot whitelist and chunk-safe survey are retained.
"""
from __future__ import annotations
import json
import shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
C2=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldUniversalWorldgenC2.java'
FABRIC=ROOT/'src/main/resources/fabric.mod.json'
RES=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json'
OUTPUT=ROOT/'tools/geodata/output/c3_architecture/backups'
ENTRY='dev.dtzudontsay.knownworld.simulation.integration.KnownWorldC3ArchitectureRuntime'
ORIGINAL='buildLot(chunk, lot, terrain.floor, p.palette, wx, wz);'
UPGRADED='KnownWorldC3Architecture.buildLot(chunk, lot, terrain.floor, p.palette, p.tier);'

def rewrite(src):
    if src.count(UPGRADED)==1:
        if src.count(ORIGINAL):raise ValueError('Mixed C2+C3 renderer calls; refusing ambiguous install')
        return src
    if src.count(ORIGINAL)!=1:
        raise ValueError('Expected one original C2 rendering call, found '+str(src.count(ORIGINAL))
                         +'. Please provide your full C2 source for review.')
    return src.replace(ORIGINAL,UPGRADED,1)

def main():
    for p in (C2,FABRIC,RES):
        if not p.is_file():raise FileNotFoundError('C3 prerequisite absent: '+str(p))
    source=C2.read_text(encoding='utf-8')
    # Previous C2 build used a Minecraft mapping-only symbol not available to
    # the user's MC 26.3 mappings. Repair that exact known issue if still present.
    if 'FarmBlock.MOISTURE' in source:
        source=source.replace('import net.minecraft.world.level.block.FarmBlock;\n','')
        source=source.replace('FarmBlock.MOISTURE','BlockStateProperties.MOISTURE')
        if 'import net.minecraft.world.level.block.state.properties.BlockStateProperties;' not in source:
            anchor='import net.minecraft.world.level.block.state.properties.BedPart;'
            if source.count(anchor)!=1:raise ValueError('Cannot repair original C2 farmland import')
            source=source.replace(anchor,anchor+'\nimport net.minecraft.world.level.block.state.properties.BlockStateProperties;',1)
    modified=rewrite(source)
    mod=json.loads(FABRIC.read_text(encoding='utf-8-sig'))
    entry=mod['entrypoints']['main']
    if entry.count(ENTRY)>1:raise ValueError('C3 Fabric entrypoint already duplicated')
    if ENTRY not in entry:entry.append(ENTRY)
    new_mod=json.dumps(mod,ensure_ascii=False,indent=2)+'\n'
    OUTPUT.mkdir(parents=True,exist_ok=True)
    if C2.read_text(encoding='utf-8')!=modified:
        backup=OUTPUT/'KnownWorldUniversalWorldgenC2_BEFORE_C3.java'
        if not backup.exists():shutil.copy2(C2,backup)
        C2.write_text(modified,encoding='utf-8')
        print('Connected C3 physical renderer to existing C2 pilot construction; backed up C2')
    else:print('C3 renderer already installed; no source changes')
    if FABRIC.read_text(encoding='utf-8-sig')!=new_mod:
        backup=OUTPUT/'fabric.mod_BEFORE_C3.json'
        if not backup.exists():shutil.copy2(FABRIC,backup)
        FABRIC.write_text(new_mod,encoding='utf-8')
        print('Registered /kwc3 read-only inspection command; backed up fabric.mod.json')
    else:print('C3 Fabric entrypoint already installed')
    print('C3 installer complete: no additional sites authorized for construction')

if __name__=='__main__':main()
