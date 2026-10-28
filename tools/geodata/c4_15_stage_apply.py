#!/usr/bin/env python3
"""Safe local project source replacement for C4.15 staged payload.

The ZIP contains its own Java/Python sources under C4_15_PAYLOAD; extracting
it alone never replaces working source. This installer backs up every replaced
file before copying it to the checkout, then verifies installed checksums.
"""
from __future__ import annotations
import argparse,datetime,hashlib,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGED='C4_15_PAYLOAD'
SOURCES=[
 'tools/geodata/c4_10_shared_layout.py',
 'tools/geodata/c4_11_global_rollout.py',
 'tools/geodata/c4_15_commute_assignment.py',
 'tools/geodata/c4_15_local_install.py',
 'tools/geodata/b4_1a1_household_index.py',
 'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC410Fortifications.java',
 'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java',
 'src/test/python/test_c4_15_revision.py',
 'docs/architecture/24_20_C4_15_ORGANIC_SETTLEMENT_AND_COMMUTES.md',
]

def apply(root=ROOT,check=False):
    root=Path(root).resolve();payload=root/STAGED
    if not payload.is_dir():raise FileNotFoundError('Missing C4_15_PAYLOAD directory: unpack ZIP at repository root.')
    required=['src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java',
              'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java',
              'tools/geodata/c4_11_install_hooks.py',
              'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1/manifest.json',
              'src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json']
    for path in required:
        if not (root/path).is_file():raise FileNotFoundError('Missing prior installed batch: '+path)
    current=root/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java'
    if 'b41ApprovedPlacementForInspection(' not in current.read_text(encoding='utf8'):
        raise ValueError('Current runtime lacks B4.1A hook; install B4.1A before C4.15.')
    for path in SOURCES:
        if not (payload/path).is_file():raise FileNotFoundError('ZIP incomplete: '+path)
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    base=root/'tools/geodata/output/c4_15_safety_backups'
    backup=base/('source_before_'+stamp)
    plan=[]
    for path in SOURCES:
        src=payload/path;dst=root/path
        expected=hashlib.sha256(src.read_bytes()).hexdigest()
        differs=not dst.exists() or hashlib.sha256(dst.read_bytes()).hexdigest()!=expected
        plan.append({'path':path,'changed':differs,'sha256':expected})
    if check:
        print(json.dumps({'dryRun':True,'targets':plan},indent=2))
        return
    backup.mkdir(parents=True,exist_ok=False)
    # First create all backups before writing any of the new source files.
    for item in plan:
        dst=root/item['path']
        if not item['changed'] or not dst.is_file():continue
        safe=backup/item['path'];safe.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(dst,safe)
    (backup/'manifest.json').write_text(json.dumps(plan,indent=2)+'\n',encoding='utf8')
    for item in plan:
        if not item['changed']:continue
        src=payload/item['path'];dest=root/item['path']
        dest.parent.mkdir(parents=True,exist_ok=True)
        tmp=dest.with_name(dest.name+'.c4_15_pending')
        tmp.write_bytes(src.read_bytes());tmp.replace(dest)
        if hashlib.sha256(dest.read_bytes()).hexdigest()!=item['sha256']:
            raise RuntimeError('Copy verification failed '+item['path'])
    print('C4.15 staged replacement applied; source backups:',backup)
    print('Updated',sum(x['changed'] for x in plan),'of',len(plan),'source/test/docs files')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);p.add_argument('--check',action='store_true')
    a=p.parse_args();apply(a.root,a.check)
