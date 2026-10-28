#!/usr/bin/env python3
"""Idempotently register the read-only worldwide census inspector."""
from pathlib import Path
import argparse, shutil

REL='src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
IMPORT='import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB40PopulationIndex;'
LINE='        KnownWorldB40PopulationIndex.register();'


def patch(source):
    if source.count(LINE)==1 and source.count(IMPORT)==1:return source
    if LINE in source or IMPORT in source:raise ValueError('Partially installed B4.0 hook, inspect manually')
    target='import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellV3Runtime;'
    anchor='        WinterfellV3Runtime.register();'
    if source.count(target)!=1 or source.count(anchor)!=1:
        raise ValueError('Expected Winterfell V3 hook missing/changed. Refuse unsafe patch.')
    return source.replace(target,target+'\n'+IMPORT,1).replace(anchor,anchor+'\n'+LINE,1)


def main():
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);a.add_argument('--check',action='store_true');args=a.parse_args()
    path=args.repo.resolve()/REL
    original=path.read_text(encoding='utf-8-sig')
    newer=patch(original)
    if args.check:print('B4.0 HOOK OK (check only)');return
    if newer!=original:
        backup=path.with_name(path.name+'.before_b4_0.bak')
        if not backup.exists():shutil.copy2(path,backup)
        temp=path.with_name(path.name+'.b4_0_install_tmp')
        temp.write_text(newer,encoding='utf8');temp.replace(path)
        print('INSTALLED '+str(path)+'; backup='+str(backup))
    else:print('B4.0 HOOK ALREADY INSTALLED')

if __name__=='__main__':main()
