#!/usr/bin/env python3
"""C4.15: exact guarded local palette patch and installed-hook audit.
No GitHub activity; backup each changed source file in tools/geodata/output.
"""
from __future__ import annotations
import argparse,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
C3=Path('src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC3Architecture.java')
C411=Path('src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java')
B41=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
GEN=Path('src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java')
MAIN=Path('src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java')
LOOKUP='''        if (p.contains("GHISCAR") || p.contains("ESSOS_FREE")) return Blocks.BRICKS.defaultBlockState();'''
INSERT='''        // C4.15: ordinary residential stone/wood; fired bricks stay a special-building accent.
        if ("URBAN_HOME".equals(family))
            return p.contains("DORN") ? Blocks.SANDSTONE.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
        if ("HOME".equals(family) && (p.contains("GHISCAR") || p.contains("ESSOS_FREE")))
            return Blocks.OAK_PLANKS.defaultBlockState();
'''

def patch_palettes(source):
    if INSERT in source:return source
    if source.count(LOOKUP)!=1:raise ValueError('C3 wall mapping changed; refuse unsafe patch')
    return source.replace(LOOKUP,INSERT+LOOKUP,1)

def validate(root=ROOT):
    root=Path(root).resolve()
    need=[C3,C411,B41,GEN,MAIN,SC/'world_population_b4_v1/manifest.json',SC/'universal_construction_c4_11_global_v1.json',
          Path('tools/geodata/c4_10_shared_layout.py'),Path('tools/geodata/c4_11_global_rollout.py')]
    miss=[str(p) for p in need if not (root/p).is_file()]
    if miss:raise FileNotFoundError('Prerequisites missing: '+', '.join(miss))
    gen=(root/GEN).read_text(encoding='utf8')
    main=(root/MAIN).read_text(encoding='utf8')
    c4=(root/C411).read_text(encoding='utf8')
    b4=(root/B41).read_text(encoding='utf8')
    for needle in ('KnownWorldC411GlobalWorldgen.decorate(level, chunk);',
                   'KnownWorldC411GlobalWorldgen.suppressNaturalDecoration(chunk)'):
        if gen.count(needle)!=1:raise ValueError('C4.11 hook missing/duplicated: '+needle)
    if main.count('KnownWorldB41Activation.register();')!=1:
        raise ValueError('B4.1A worldwide NPC registration missing/duplicated')
    for needle in ('b41ApprovedPlacementForInspection(', 'private static void streetsForLot(', 'initialTerrainHeight('):
        if needle not in c4:raise ValueError('New C4.15 global renderer missing: '+needle)
    if 'initialTerrainHeight(xx,zz)' not in b4 or 'dz=(choice/10)' not in b4.replace(' ',''):
        raise ValueError('B4.1A.1 ground-frontage version was not extracted')
    man=json.loads((root/SC/'world_population_b4_v1/manifest.json').read_text(encoding='utf8'))
    if (man['peopleTotal'],man['householdsTotal'],man['modeledWorkersTotal'],man['siteCount']) != (3760684,799343,1880341,248):
        raise ValueError('Population baseline not the expected exact B4.0 census')
    return {'census':'PRESERVED','hooks':'ACTIVE','root':str(root),'C3PalettePatch':INSERT in (root/C3).read_text(encoding='utf8')}

def apply(root=ROOT,dry_run=False):
    root=Path(root).resolve()
    original=(root/C3).read_text(encoding='utf8');updated=patch_palettes(original)
    if updated!=original:
        if dry_run:print('DRY RUN: would patch C3 residential palette')
        else:
            backup=root/'tools/geodata/output/c4_15_safety_backups/KnownWorldC3Architecture.java.before_c4_15.bak'
            backup.parent.mkdir(parents=True,exist_ok=True)
            if not backup.exists():shutil.copy2(root/C3,backup)
            tmp=(root/C3).with_suffix('.java.tmp')
            tmp.write_text(updated,encoding='utf8');tmp.replace(root/C3)
            print('Patched ordinary home palette, backup:',backup)
    else:print('Residential palette already patched')
    print(json.dumps(validate(root),indent=2))

def main():
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=ROOT)
    p.add_argument('--apply',action='store_true');a=p.parse_args()
    validate(a.repo)
    apply(a.repo,not a.apply)
if __name__=='__main__':main()
