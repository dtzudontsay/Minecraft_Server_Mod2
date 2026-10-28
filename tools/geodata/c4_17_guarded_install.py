#!/usr/bin/env python3
"""C4.17/B4.2C guarded offline-source and derived-resource installation.

Refuses unknown source edits, snapshots all managed paths, restores on *any*
subprocess failure. Never opens/writes a Minecraft save or B4.0 identity shard.
"""
from __future__ import annotations
import argparse,datetime,hashlib,json,os,shutil,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('C4_17_STAGE')
ROOT_DATA=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
FILES=[
 'tools/geodata/c4_10_shared_layout.py',
 'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java',
]
NEW='src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42PhysicalRoutine.java'
RESOURCES=[
 ROOT_DATA/'universal_construction_c4_11_global_v1.json',
 ROOT_DATA/'c4_11_global_tiles',
 ROOT_DATA/'world_commutes_c4_15_v1',
 ROOT_DATA/'world_projection_b4_1a1_v1',
 ROOT_DATA/'world_daily_b4_2_v1',
 Path('tools/geodata/output/c4_10_shared_layout'),
]
SNAPROOT=Path('tools/geodata/output/c4_17_safe_snapshots')
MANAGED=[Path(p) for p in FILES]+[Path(NEW)]+RESOURCES
POP=ROOT_DATA/'world_population_b4_v1/manifest.json'
REQUIRED_GENERATORS=[
 'tools/geodata/c4_11_global_rollout.py',
 'tools/geodata/c4_15_commute_assignment.py',
 'tools/geodata/b4_1a1_household_index.py',
 'tools/geodata/b4_2_workplace_index.py',
]

def digest(file):return hashlib.sha256(file.read_bytes()).hexdigest()

def check(repo):
    for p in [POP,*map(Path,REQUIRED_GENERATORS)]:
        if not (repo/p).is_file():raise FileNotFoundError('Missing prerequisite: '+str(p))
    pop=json.loads((repo/POP).read_text(encoding='utf-8'))
    assert (pop.get('peopleTotal'),pop.get('householdsTotal'),pop.get('modeledWorkersTotal'),pop.get('siteCount'))==(3760684,799343,1880341,248),'B4.0 manifest census mismatch'
    status={}
    for rel in FILES:
        src=repo/rel;before=repo/STAGE/'baseline'/rel;after=repo/STAGE/'updated'/rel
        if not all(p.is_file() for p in [src,before,after]):raise FileNotFoundError('Missing live/staged file: '+rel)
        status[rel]='READY' if digest(src)==digest(before) else 'ALREADY_INSTALLED' if digest(src)==digest(after) else 'UNKNOWN_LOCAL_EDITS'
    new=repo/NEW;replacement=repo/STAGE/'updated'/NEW
    if not replacement.is_file():raise FileNotFoundError('Missing new runtime module: '+str(replacement))
    status[NEW]='NEW' if not new.exists() else 'ALREADY_INSTALLED' if digest(new)==digest(replacement) else 'UNKNOWN_LOCAL_EDITS'
    return status

def snapshot(repo):
    label=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    base=repo/SNAPROOT/label
    base.mkdir(parents=True,exist_ok=False)
    state=[]
    for rel in MANAGED:
        f=repo/rel
        exists=f.exists()
        state.append({'path':str(rel).replace('\\','/'),'existed':exists})
        if not exists:continue
        dest=base/rel;dest.parent.mkdir(parents=True,exist_ok=True)
        if f.is_dir():shutil.copytree(f,dest)
        else:shutil.copy2(f,dest)
    meta={'version':1,'batch':'C4_17_B4_2C','label':label,'items':state,
          'populationUnaffected':True,'saveFilesUnaffected':True,'completed':False}
    (base/'ROLLBACK_MANIFEST.json').write_text(json.dumps(meta,indent=2),encoding='utf-8')
    return base

def rollback(repo,label):
    if label=='latest':
        root=repo/SNAPROOT
        labels=sorted(p.name for p in root.iterdir() if p.is_dir() and (p/'ROLLBACK_MANIFEST.json').is_file()) if root.exists() else []
        if not labels:raise FileNotFoundError('No rollback snapshots')
        label=labels[-1]
    base=repo/SNAPROOT/label
    meta=json.loads((base/'ROLLBACK_MANIFEST.json').read_text(encoding='utf-8'))
    if meta.get('batch')!='C4_17_B4_2C':raise ValueError('Not a C4.17 backup')
    for entry in meta['items']:
        rel=Path(entry['path']);dst=repo/rel;src=base/rel
        if rel not in MANAGED:raise ValueError('Unsafe rollback entry '+str(rel))
        if dst.is_dir():shutil.rmtree(dst)
        elif dst.exists():dst.unlink()
        if not entry['existed']:continue
        if not src.exists():raise FileNotFoundError('Incomplete backup: '+str(src))
        dst.parent.mkdir(parents=True,exist_ok=True)
        if src.is_dir():shutil.copytree(src,dst)
        else:shutil.copy2(src,dst)
    print('RESTORED C4.17 SOURCE + RESOURCES:',label,flush=True)

def run(repo,argv):
    cmd=[str(a) for a in argv]
    print('RUNNING:', ' '.join(cmd),flush=True)
    subprocess.run(cmd,cwd=repo,check=True)

def install(repo):
    status=check(repo);print(json.dumps(status,indent=2),flush=True)
    unknown=[f for f,state in status.items() if state=='UNKNOWN_LOCAL_EDITS']
    if unknown:raise ValueError('UNKNOWN LOCAL EDITS; REFUSING TO OVERWRITE: '+', '.join(unknown))
    if all(s=='ALREADY_INSTALLED' for s in status.values()):
        print('Sources already installed; no automatic resource rebuild. Use a fresh test save to inspect changes.')
        return
    # Test the staged implementation before touching the working project.
    run(repo,['py','-3','-m','unittest','discover','-s','src/test/python','-p','test_c4_17_*.py','-v'])
    backup=snapshot(repo)
    label=backup.name
    print('ROLLBACK SNAPSHOT:',label,flush=True)
    try:
        for rel in FILES+[NEW]:
            source=repo/STAGE/'updated'/rel
            destination=repo/rel
            destination.parent.mkdir(parents=True,exist_ok=True)
            staged=destination.with_name(destination.name+'.c417partial')
            shutil.copy2(source,staged);staged.replace(destination)
        run(repo,['py','-3','tools/geodata/c4_10_shared_layout.py','--summary-only'])
        run(repo,['py','-3','tools/geodata/c4_11_global_rollout.py','--install'])
        run(repo,['py','-3','tools/geodata/c4_15_commute_assignment.py'])
        run(repo,['py','-3','tools/geodata/b4_1a1_household_index.py'])
        run(repo,['py','-3','tools/geodata/b4_2_workplace_index.py'])
        gradlew=repo/'gradlew.bat'
        if not gradlew.is_file():raise FileNotFoundError('Missing gradlew.bat')
        run(repo,['cmd','/c',str(gradlew),'clean','build']) if os.name=='nt' else run(repo,['bash',str(repo/'gradlew'),'clean','build'])
        meta_file=backup/'ROLLBACK_MANIFEST.json'
        meta=json.loads(meta_file.read_text(encoding='utf-8'))
        meta['completed']=True
        meta_file.write_text(json.dumps(meta,indent=2),encoding='utf-8')
        print('C4.17 INSTALLED + BUILD PASSED. ROLLBACK ID:',label,flush=True)
        print('Use NEW TEST SAVE for architecture. Existing chunks are unchanged.',flush=True)
    except BaseException as error:
        print('C4.17 INSTALL FAILED:',repr(error),'-- restoring snapshot now.',file=sys.stderr,flush=True)
        rollback(repo,label)
        raise

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT)
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check',action='store_true')
    group.add_argument('--install',action='store_true')
    group.add_argument('--rollback',metavar='ID_OR_LATEST')
    args=parser.parse_args();repo=args.root.resolve()
    try:
        if args.check:print(json.dumps(check(repo),indent=2))
        elif args.install:install(repo)
        else:rollback(repo,args.rollback)
    except Exception as error:
        print('C4.17 SAFE STOP:',repr(error),file=sys.stderr)
        sys.exit(1)
if __name__=='__main__':main()
