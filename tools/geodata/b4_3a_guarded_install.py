#!/usr/bin/env python3
"""B4.3A guarded, reversible increase of worldwide NPC capacity + offscreen commute continuity.

Only the listed source files and the projection *manifest* are writable.
No B4.0 population shards, Minecraft saves, C4.17 town tiles, or conversation files are touched.
"""
from __future__ import annotations
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('B4_3A_STAGE')
JAVA_ROOT='src/main/java/dev/dtzudontsay/knownworld/simulation/population/'
EXISTING=[
 JAVA_ROOT+'KnownWorldB41Activation.java',
 JAVA_ROOT+'KnownWorldB42DailyLife.java',
 'tools/geodata/b4_1a1_household_index.py',
]
NEW=[JAVA_ROOT+'KnownWorldB43TravelModel.java',JAVA_ROOT+'KnownWorldB43OffscreenTravel.java']
MANIFEST=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a1_v1/manifest.json')
SNAPROOT=Path('tools/geodata/output/b4_3a_safe_snapshots')
FILES=[Path(x) for x in EXISTING+NEW]+[MANIFEST]


def normalize(path:Path):
    data=path.read_bytes()
    return data.replace(b'\r\n',b'\n') if path.suffix=='.java' else data


def check(repo:Path):
    if not (repo/MANIFEST).is_file():raise FileNotFoundError('Missing generated B4.1A.1 projection manifest')
    manifest=json.loads((repo/MANIFEST).read_text(encoding='utf-8'))
    if (manifest.get('schemaVersion')!='knownworld.b4_1a1.household_projection.v1'
       or manifest.get('sourcePopulationCount')!=3760684
       or manifest.get('sourceHouseholdCount')!=799343
       or len(manifest.get('sites',[]))!=248):
        raise ValueError('Population projection provenance mismatch; refused to update')
    status={}
    for s in EXISTING:
        live=repo/s;old=repo/STAGE/'baseline'/s;new=repo/STAGE/'updated'/s
        if not all(p.is_file() for p in [live,old,new]):raise FileNotFoundError('Missing guarded source: '+s)
        content=normalize(live)
        status[s]='READY' if content==normalize(old) else 'ALREADY_INSTALLED' if content==normalize(new) else 'UNKNOWN_LOCAL_EDITS'
    for s in NEW:
        live=repo/s;new=repo/STAGE/'updated'/s
        if not new.is_file():raise FileNotFoundError('Missing B4.3A new source: '+s)
        status[s]='NEW' if not live.exists() else 'ALREADY_INSTALLED' if normalize(live)==normalize(new) else 'UNKNOWN_LOCAL_EDITS'
    limit=manifest.get('physicalProjectionLimit')
    status[str(MANIFEST)]='READY' if limit==128 else 'ALREADY_INSTALLED' if limit==256 else 'UNKNOWN_LOCAL_EDITS'
    return status


def snapshot(repo:Path):
    label=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    target=repo/SNAPROOT/label
    target.mkdir(parents=True,exist_ok=False)
    items=[]
    for path in FILES:
        original=repo/path; exists=original.is_file()
        if original.exists() and not exists:raise ValueError('Unsafe path, not a file: '+str(path))
        items.append({'path':path.as_posix(),'existed':exists,'sha256':hashlib.sha256(original.read_bytes()).hexdigest() if exists else None})
        if exists:
            dst=target/path;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(original,dst)
    info={'batch':'B4_3A','version':1,'completed':False,'items':items,
          'savesUntouched':True,'b4PopulationUntouched':True}
    (target/'ROLLBACK_MANIFEST.json').write_text(json.dumps(info,indent=2),encoding='utf8')
    return target


def restore(repo:Path,label:str):
    folder=repo/SNAPROOT
    if label=='latest':
        valid=sorted(p.name for p in folder.iterdir() if p.is_dir() and (p/'ROLLBACK_MANIFEST.json').is_file()) if folder.is_dir() else []
        if not valid:raise FileNotFoundError('No B4.3A rollback snapshot')
        label=valid[-1]
    target=folder/label
    info=json.loads((target/'ROLLBACK_MANIFEST.json').read_text(encoding='utf8'))
    if info.get('batch')!='B4_3A':raise ValueError('Not a B4.3A snapshot')
    # Preflight before touching any managed file; avoid half-rollback.
    items=info['items']
    if {j['path'] for j in items}!={p.as_posix() for p in FILES}:
        raise ValueError('Rollback path mismatch')
    for item in items:
        source=target/item['path']
        if item['existed'] and (not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest()!=item['sha256']):
            raise ValueError('Corrupt or missing backup: '+item['path'])
    for item in items:
        targetPath=repo/item['path'];source=target/item['path']
        if targetPath.is_file():targetPath.unlink()
        elif targetPath.exists():raise ValueError('Unsafe rollback destination: '+str(targetPath))
        if item['existed']:
            targetPath.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(source,targetPath)
    print('RESTORED B4.3A sources and projection manifest:',label,flush=True)


def install(repo:Path,run_gradle=True):
    states=check(repo)
    print(json.dumps(states,indent=2),flush=True)
    bad=[p for p,s in states.items() if s=='UNKNOWN_LOCAL_EDITS']
    if bad:raise ValueError('Refusing unknown edits; upload the exact files: '+', '.join(bad))
    if all(s=='ALREADY_INSTALLED' for s in states.values()):
        print('B4.3A already installed. No changes made.')
        return
    if any(s=='ALREADY_INSTALLED' for s in states.values()):
        raise ValueError('Partial pre-existing B4.3A install; refuse mixed versions')
    subprocess.run(['py','-3','-m','unittest','discover','-s','src/test/python','-p','test_b4_3a_*.py','-v'],cwd=repo,check=True) if run_gradle and os.name=='nt' else None
    backup=snapshot(repo)
    print('FULL MANAGED-FILE BACKUP:',backup,flush=True)
    try:
        for path in EXISTING+NEW:
            src=repo/STAGE/'updated'/path
            dest=repo/path
            dest.parent.mkdir(parents=True,exist_ok=True)
            tmp=dest.with_name(dest.name+'.b43partial')
            shutil.copy2(src,tmp);tmp.replace(dest)
        manifest=json.loads((repo/MANIFEST).read_text(encoding='utf-8'))
        if manifest['physicalProjectionLimit']!=128:raise ValueError('Unexpected old projection limit')
        manifest['physicalProjectionLimit']=256
        # No rebuild of 3.76m tiles is needed: their contents are identical.
        (repo/MANIFEST).write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        state=check(repo)
        if any(v!='ALREADY_INSTALLED' for v in state.values()):raise ValueError('Patch postcondition failed: '+str(state))
        if run_gradle:
            gradlew=repo/'gradlew.bat'
            if not gradlew.is_file():raise FileNotFoundError('Missing gradlew.bat')
            command=['cmd','/c',str(gradlew),'clean','build'] if os.name=='nt' else ['bash',str(repo/'gradlew'),'clean','build']
            subprocess.run(command,cwd=repo,check=True)
        infoPath=backup/'ROLLBACK_MANIFEST.json'
        info=json.loads(infoPath.read_text(encoding='utf8'));info['completed']=True
        infoPath.write_text(json.dumps(info,indent=2),encoding='utf8')
        print('B4.3A INSTALL SUCCESS. Rollback ID:',backup.name,flush=True)
    except BaseException:
        print('B4.3A INSTALL FAILED: restoring managed files now',file=sys.stderr,flush=True)
        restore(repo,backup.name)
        raise


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT)
    flags=parser.add_mutually_exclusive_group(required=True)
    flags.add_argument('--check',action='store_true')
    flags.add_argument('--install',action='store_true')
    flags.add_argument('--rollback',metavar='ID_OR_LATEST')
    opt=parser.parse_args();repo=opt.root.resolve()
    try:
        if opt.check:print(json.dumps(check(repo),indent=2))
        elif opt.install:install(repo)
        else:restore(repo,opt.rollback)
    except Exception as exc:
        print('B4.3A SAFE STOP:',repr(exc),file=sys.stderr,flush=True)
        sys.exit(1)

if __name__=='__main__':main()
