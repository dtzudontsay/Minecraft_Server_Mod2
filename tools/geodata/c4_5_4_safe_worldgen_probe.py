#!/usr/bin/env python3
"""C4.5.4 reversible, guarded Minecraft 26.3 freeze isolation.

Default: audit only. --install: disable C4.5 deferred sign creation by default,
return Winterfell visible projection budgets to 128. Does not change NPC
identities, occupations, scenario data, worldgen plots, or world saves.
--restore: remove exactly the patch produced by this script.
"""
from __future__ import annotations
import argparse
import difflib
from pathlib import Path
import shutil
import sys

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/'src/main/java/dev/dtzudontsay/knownworld'
SIGN=PKG/'world/generation/settlement/KnownWorldStructureSigns.java'
NPC=PKG/'simulation/casestudy/WinterfellV3Runtime.java'
MARK='C4_5_4_SAFE_PROBE'
METHOD=('    public static void attach(ChunkAccess chunk, int x, int z, int width, int depth,\n'
        '                              int floor, String face, String buildingType) {\n')
ADD=('        // '+MARK+': experimental generated signs OFF during freeze diagnostics.\n'
     '        // Re-enable ONLY for isolated developer tests; not a supported production mode.\n'
     '        if (!Boolean.getBoolean("knownworld.experimentalC45Signs")) return;\n')
ORIG_BUDGETS=[('public static final int FULL_BUDGET = 384;',
               'public static final int FULL_BUDGET = 128;'),
              ('public static final int REDUCED_BUDGET = 256;',
               'public static final int REDUCED_BUDGET = 128;')]

def transform(sign,npc,restore=False):
    if 'C4_5_2_DEFERRED_SIGNS' not in sign or 'public final class KnownWorldStructureSigns' not in sign:
        raise ValueError('C4.5.2/3 deferred sign helper not detected. Refusing to touch unrelated file.')
    if sign.count(METHOD)!=1:
        raise ValueError(f'Sign attach method signature mismatch ({sign.count(METHOD)} found)')
    if not restore:
        if sign.count(MARK)!=0 or sign.count(ADD)!=0:
            if sign.count(ADD)!=1 or sign.count(MARK)!=1:
                raise ValueError('Partial/unrecognized C4.5.4 sign patch')
            new_sign=sign
        else:
            new_sign=sign.replace(METHOD,METHOD+ADD,1)
        new_npc=npc
        for old,new in ORIG_BUDGETS:
            if new_npc.count(old)==1 and new_npc.count(new)==0:
                new_npc=new_npc.replace(old,new,1)
            elif new_npc.count(old)==0 and new_npc.count(new)==1:
                pass  # already at baseline budget
            else:
                raise ValueError('Unrecognized NPC projection budget at '+old)
    else:
        if sign.count(ADD)!=1 or sign.count(MARK)!=1:
            raise ValueError('C4.5.4 sign marker absent or modified; refusing rollback')
        new_sign=sign.replace(ADD,'',1)
        new_npc=npc
        for old,new in ORIG_BUDGETS:
            if new_npc.count(new)!=1 or new_npc.count(old)!=0:
                raise ValueError('Unexpected NPC projection budget during restore: '+new)
            new_npc=new_npc.replace(new,old,1)
    return new_sign,new_npc

def main():
    parser=argparse.ArgumentParser()
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check',action='store_true')
    group.add_argument('--install',action='store_true')
    group.add_argument('--restore',action='store_true')
    args=parser.parse_args()
    for p in (SIGN,NPC):
        if not p.is_file():raise FileNotFoundError(f'Missing {p}. Extract zip to repo root alongside gradlew.bat')
    sign=SIGN.read_text(encoding='utf-8'); npc=NPC.read_text(encoding='utf-8')
    target_sign,target_npc=transform(sign,npc,args.restore)
    operations=[(SIGN,sign,target_sign),(NPC,npc,target_npc)]
    for p,before,after in operations:
        print(('UNCHANGED' if before==after else 'READY'),' : ',p.relative_to(ROOT))
        if before!=after:
            diff=list(difflib.unified_diff(before.splitlines(),after.splitlines(),lineterm='',n=2))
            print('\n'.join(diff[:35]))
    if args.check:
        print('PASS -- READ ONLY. No files or saved worlds changed.')
        return
    for p,before,after in operations:
        if before==after:continue
        backup=p.with_name(p.name+'.before_c4_5_4_safe_probe.bak')
        if not backup.exists():
            shutil.copy2(p,backup)
            print('BACKUP:',backup.relative_to(ROOT))
        p.write_text(after,encoding='utf-8')
        if p.read_text(encoding='utf-8')!=after:
            raise RuntimeError('Write/read verification failed: '+str(p))
    print('DONE. C4.5.4 safe probe', 'RESTORED' if args.restore else 'INSTALLED')
    print('No changes to generated maps, population, job lists, save files or structure counts.')

if __name__=='__main__':
    try: main()
    except Exception as exc:
        print('STOPPED:',type(exc).__name__,exc,file=sys.stderr)
        sys.exit(1)
