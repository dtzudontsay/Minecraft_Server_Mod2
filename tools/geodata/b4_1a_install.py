#!/usr/bin/env python3
"""Idempotent and reversible (backups) B4.1A Java hook installer."""
from __future__ import annotations
import argparse, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MAIN='src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
GLOBAL='src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java'
UI='src/main/java/dev/dtzudontsay/knownworld/client/casestudy/WinterfellCharacterScreen.java'
IMPORT='import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41Activation;'
HOOK='        KnownWorldB41Activation.register();'
PLACEMENT='''    /** B4.1A: recompute deterministic approval when revisiting a saved,
     * previously generated settlement. Never writes blocks/loads chunks. */
    public static int[] b41ApprovedPlacementForInspection(String siteId) {
        for (Site site : sites()) {
            if (!site.id.equals(siteId)) continue;
            Placement p = placements.computeIfAbsent(site.id, ignored -> resolve(site));
            return p.approved ? new int[]{p.x,p.z} : null;
        }
        return null;
    }
'''
ANCHOR='    public static void decorate(WorldGenLevel level,ChunkAccess chunk) {'

def main_patch(source):
    if source.count(IMPORT)==source.count(HOOK)==1:return source
    if IMPORT in source or HOOK in source:raise ValueError('Partial B4.1A KnownWorld hook')
    a='import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB40PopulationIndex;'
    b='        KnownWorldB40PopulationIndex.register();'
    if source.count(a)!=1 or source.count(b)!=1:raise ValueError('B4.0 registration is missing; install B4.0 first')
    return source.replace(a,a+'\n'+IMPORT,1).replace(b,b+'\n'+HOOK,1)

def global_patch(source):
    if 'b41ApprovedPlacementForInspection(' in source:
        if source.count('b41ApprovedPlacementForInspection(')!=1:raise ValueError('Multiple B4.1 placement hooks')
        return source
    if source.count(ANCHOR)!=1:raise ValueError('Original C4.11 global runtime not found')
    return source.replace(ANCHOR,PLACEMENT+'\n'+ANCHOR,1)

def ui_patch(source):
    new_title='(value("id").startsWith("b4:") ? "KNOWN WORLD" : "WINTERFELL") + "  •  CHARACTER RECORD"'
    if new_title in source:return source
    original='"WINTERFELL  •  CHARACTER RECORD"'
    if source.count(original)!=1:raise ValueError('Winterfell character UI title has changed; refuse unsafe patch')
    return source.replace(original,new_title,1)

def write(path,new,original,check):
    if new==original:print('B4.1A already patched '+str(path));return
    if check:print('B4.1A check: would patch '+str(path));return
    bak=path.with_name(path.name+'.before_b4_1a.bak')
    if not bak.exists():shutil.copy2(path,bak)
    tmp=path.with_name(path.name+'.b4_1a.tmp')
    tmp.write_text(new,encoding='utf8');tmp.replace(path)
    print('B4.1A PATCHED '+str(path)+' backup '+str(bak))

def install(root,check=False):
    root=Path(root).resolve()
    for relative,patch in [(MAIN,main_patch),(GLOBAL,global_patch),(UI,ui_patch)]:
        path=root/relative
        if not path.is_file():raise FileNotFoundError(f'B4.1A requires {path}')
        old=path.read_text(encoding='utf-8-sig')
        write(path,patch(old),old,check)

def main():
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=ROOT);a.add_argument('--check',action='store_true');args=a.parse_args()
    install(args.repo,args.check)
if __name__=='__main__':main()
