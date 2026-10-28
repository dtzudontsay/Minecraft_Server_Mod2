#!/usr/bin/env python3
"""B4.2B.3 command permission compatibility repair, stage-only, fail-closed.
No modification to live Java, Minecraft saves, population or economy resources.
"""
from __future__ import annotations
import argparse, datetime, hashlib, json, shutil, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
REL=Path('B4_2B3_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42B3StartingStock.java')
LIVE=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42B3StartingStock.java')
OLD=b'.requires(s -> s.hasPermission(4))'
NEW=b'.requires(Commands.hasPermission(Commands.LEVEL_OWNERS))'
OLD_SHA='45516ff5ddd123c3873ef83e04383c9e377832241b327b790138838586705dfc'
NEW_SHA='9d1ded0ca4567b3dbcb00af1365d2ba2e46e49899a99e8862ad9fbf0db3d0cad'
BACK=Path('tools/geodata/output/b4_2b3_safe_snapshots/permission_hotfixes')

def digest(data):return hashlib.sha256(data).hexdigest()

def check(root):
    stage=root/REL;live=root/LIVE
    if not stage.is_file() or stage.is_symlink():raise ValueError('Staged Java file missing or unsafe')
    data=stage.read_bytes();h=digest(data)
    if h==OLD_SHA and data.count(OLD)==1 and data.count(NEW)==0:state='READY_TO_PATCH'
    elif h==NEW_SHA and data.count(NEW)==1 and data.count(OLD)==0:state='ALREADY_PATCHED'
    else:raise ValueError('Unknown staged Java changes: refusing overwrite')
    if live.exists() or live.is_symlink():
        raise ValueError('Live B4.2B.3 class already exists or partial install: refusing staged repair; inspect before proceeding')
    return {'staged_permission':'READY_TO_PATCH' if state=='READY_TO_PATCH' else 'ALREADY_PATCHED',
            'live_starting_stock':'NEW',
            'operator_security':'LEVEL_OWNERS',
            'old_staged_sha256':OLD_SHA,
            'new_staged_sha256':NEW_SHA}

def apply(root):
    state=check(root)
    if state['staged_permission']=='ALREADY_PATCHED':
        print('Stage permission repair ALREADY APPLIED; no changes');return
    # Save unmodified stage file and verify it before changing anything.
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    snapshot=root/BACK/stamp
    snapshot.mkdir(parents=True,exist_ok=False)
    original=(root/REL).read_bytes()
    if digest(original)!=OLD_SHA:raise ValueError('Staged file changed during backup')
    (snapshot/'KnownWorldB42B3StartingStock.java.before').write_bytes(original)
    manifest={'batch':'B4_2B3_PERMISSION_FIX','target':REL.as_posix(),'sha256':OLD_SHA}
    (snapshot/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
    updated=original.replace(OLD,NEW)
    if digest(updated)!=NEW_SHA:raise ValueError('Patched file checksum mismatch')
    target=root/REL;temp=target.with_name(target.name+'.b42b3_permission_tmp')
    if temp.exists() or temp.is_symlink():raise ValueError('Unsafe stale temporary file; refusing to overwrite')
    try:
        with temp.open('xb') as f:f.write(updated)
        if digest(temp.read_bytes())!=NEW_SHA:raise ValueError('Temporary patched file failed verification')
        temp.replace(target)
        if check(root)['staged_permission']!='ALREADY_PATCHED':raise ValueError('Post-install staged check did not pass')
        print('STAGE-ONLY PERMISSION FIX APPLIED; backup at '+str(snapshot))
    finally:
        if temp.exists():temp.unlink()

def rollback(root,s):
    base=root/BACK
    if s=='latest':
        if not base.is_dir():raise ValueError('No permission repair backup')
        options=sorted(p for p in base.iterdir() if p.is_dir() and (p/'manifest.json').is_file())
        if not options:raise ValueError('No permission repair backup')
        snap=options[-1]
    else:
        if not s.replace('_','').isdigit():raise ValueError('Unsafe snapshot name')
        snap=base/s
    manifest=json.loads((snap/'manifest.json').read_text(encoding='utf8'))
    original=(snap/'KnownWorldB42B3StartingStock.java.before').read_bytes()
    if (manifest.get('batch')!='B4_2B3_PERMISSION_FIX' or manifest.get('target')!=REL.as_posix()
        or manifest.get('sha256')!=OLD_SHA or digest(original)!=OLD_SHA):
        raise ValueError('Corrupt permission repair backup')
    if check(root)['staged_permission']!='ALREADY_PATCHED':
        raise ValueError('Stage is not the known patched version; refuse to overwrite')
    (root/REL).write_bytes(original)
    print('Original staged permission file restored from '+str(snap))

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--root',type=Path,default=ROOT)
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check',action='store_true')
    group.add_argument('--apply',action='store_true')
    group.add_argument('--rollback')
    a=parser.parse_args();root=a.root.resolve()
    try:
        if a.check:print(json.dumps(check(root),indent=2))
        elif a.apply:apply(root)
        else:rollback(root,a.rollback)
    except (OSError,ValueError,RuntimeError) as e:
        print('B4.2B.3 PERMISSION FIX SAFE STOP: '+str(e),file=sys.stderr)
        sys.exit(1)
if __name__=='__main__':main()
