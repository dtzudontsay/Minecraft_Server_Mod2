#!/usr/bin/env python3
"""Full source/resource snapshot, sha1 git-blob guard, fail-closed install/rollback.
This installer NEVER writes run/saves or the persistent EconomyRuntime ledger.
"""
from __future__ import annotations
import argparse, datetime, hashlib, json, os, shutil, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('B4_2B3_STAGE')
JAVA=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
ECO=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/economy/EconomyRuntime.java')
STANDING=JAVA/'KnownWorldB42B2EconomicStanding.java'
NEW=JAVA/'KnownWorldB42B3StartingStock.java'
RESOURCE=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_opening_reserves_b4_2b3_v1')
FILES=[STANDING,ECO,NEW,RESOURCE]
SNAP=Path('tools/geodata/output/b4_2b3_safe_snapshots')
KNOWN_GIT_SHA='759c8ea2ddaa77382712fd63342d48535f237f3d'
MARKER=b'    private void saveEconomy() throws IOException {'
NEEDS=['src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1/manifest.json',
       'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_household_economy_b4_2b2_v1/manifest.json',
       'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42B2EconomicStanding.java']

def raw(path):return path.read_bytes().replace(b'\r\n',b'\n')
def sha(data):return hashlib.sha256(data).hexdigest()
def gitsha(data):return hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest()
def method(root):return raw(root/STAGE/'patches/EconomyRuntime_method.txt')
def updated_economy(root,source):
    m=method(root)
    if source.count(MARKER)!=1:raise ValueError('EconomyRuntime expected save anchor missing/duplicated')
    return source.replace(MARKER,m+MARKER)

def check(root):
    root=Path(root);s={};base=root/STAGE/'baseline'/STANDING;up=root/STAGE/'updated'/STANDING
    if not (base.is_file() and up.is_file() and (root/STANDING).is_file()):raise FileNotFoundError('Missing guarded B4.2B.2 Java class')
    live=raw(root/STANDING)
    s[str(STANDING)]='READY' if live==raw(base) else 'ALREADY_INSTALLED' if live==raw(up) else 'UNKNOWN_LOCAL_EDITS'
    econ=root/ECO
    if not econ.is_file():raise FileNotFoundError('Missing economy authority runtime')
    e=raw(econ);m=method(root)
    if gitsha(e)==KNOWN_GIT_SHA:s[str(ECO)]='READY'
    elif e.count(m)==1 and gitsha(e.replace(m,b''))==KNOWN_GIT_SHA:s[str(ECO)]='ALREADY_INSTALLED'
    else:s[str(ECO)]='UNKNOWN_LOCAL_EDITS'
    new=root/NEW;staged=root/STAGE/'updated'/NEW
    if not staged.is_file():raise FileNotFoundError('Missing staged starting-stock service')
    s[str(NEW)]='NEW' if not new.exists() else 'ALREADY_INSTALLED' if new.is_file() and raw(new)==raw(staged) else 'UNKNOWN_LOCAL_EDITS'
    res=root/RESOURCE
    if not res.exists():s[str(RESOURCE)]='NEW'
    elif res.is_dir() and (res/'manifest.json').is_file():
        try:s[str(RESOURCE)]='ALREADY_INSTALLED' if json.loads((res/'manifest.json').read_text())['schemaVersion']=='knownworld.b4_2b3.opening_public_reserves.v1' else 'UNKNOWN_LOCAL_EDITS'
        except Exception:s[str(RESOURCE)]='UNKNOWN_LOCAL_EDITS'
    else:s[str(RESOURCE)]='UNKNOWN_LOCAL_EDITS'
    for rel in NEEDS:
        if not (root/rel).is_file():raise FileNotFoundError('Missing previous economic batch prerequisite: '+rel)
    pend=res.with_name(res.name+'.pending')
    if pend.exists():raise ValueError('Existing unfinished opening-stock plan; inspect, do not overwrite')
    return s

def backup(root):
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f'); folder=root/SNAP/stamp
    folder.mkdir(parents=True,exist_ok=False);items=[]
    for rel in FILES:
        live=root/rel
        if live.is_symlink():raise ValueError('Linked managed file forbidden: '+str(rel))
        typ='file' if live.is_file() else 'dir' if live.is_dir() else 'missing' if not live.exists() else 'unsafe'
        if typ=='unsafe':raise ValueError('Unsafe managed path')
        dst=folder/rel
        x={'path':rel.as_posix(),'type':typ}
        if typ=='file':
            dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(live,dst);x['sha256']=sha(dst.read_bytes())
        elif typ=='dir':
            shutil.copytree(live,dst);x['contents']={p.relative_to(dst).as_posix():sha(p.read_bytes()) for p in dst.rglob('*') if p.is_file()}
        items.append(x)
    (folder/'ROLLBACK_MANIFEST.json').write_text(json.dumps({'batch':'B4_2B3','items':items,'complete':False},indent=2))
    return folder

def rollback(root,stamp):
    root=Path(root);base=root/SNAP
    if stamp=='latest':
        choices=sorted(p for p in base.iterdir() if p.is_dir() and (p/'ROLLBACK_MANIFEST.json').is_file()) if base.exists() else []
        if not choices:raise FileNotFoundError('No saved managed snapshot')
        snap=choices[-1]
    else:
        if not stamp.replace('_','').isdigit():raise ValueError('Invalid snapshot ID')
        snap=base/stamp
    obj=json.loads((snap/'ROLLBACK_MANIFEST.json').read_text());items=obj['items']
    if obj['batch']!='B4_2B3' or {x['path'] for x in items}!={x.as_posix() for x in FILES}:raise ValueError('Wrong or corrupted backup manifest')
    # Verify ALL backup files before modifying a single live path.
    for x in items:
        p=snap/x['path'];typ=x['type']
        if typ=='file' and (not p.is_file() or sha(p.read_bytes())!=x['sha256']):raise ValueError('Bad backup checksum')
        if typ=='dir' and (not p.is_dir() or {q.relative_to(p).as_posix():sha(q.read_bytes()) for q in p.rglob('*') if q.is_file()}!=x['contents']):raise ValueError('Bad backup directory checksum')
    for x in items:
        live=root/x['path'];saved=snap/x['path']
        if live.is_symlink():raise ValueError('Unsafe symlink at rollback target '+str(live))
        if live.is_file():live.unlink()
        elif live.is_dir():shutil.rmtree(live)
        elif live.exists():raise ValueError('Unknown rollback destination')
        if x['type']=='file':live.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(saved,live)
        if x['type']=='dir':live.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(saved,live)
    print('B4.2B.3 source/resource rollback restored snapshot '+snap.name,flush=True)

def install(root,run_tests=True,run_gradle=True):
    state=check(root);print(json.dumps(state,indent=2),flush=True)
    if 'UNKNOWN_LOCAL_EDITS' in state.values():raise ValueError('SAFE STOP: unknown local source edits; nothing overwritten')
    if all(v=='ALREADY_INSTALLED' for v in state.values()):print('Already installed');return
    if any(v=='ALREADY_INSTALLED' for v in state.values()):raise ValueError('Partial install detected; no mixed source versions')
    if run_tests:
        command=['py','-3'] if os.name=='nt' else [sys.executable]
        subprocess.run(command+['-m','unittest','discover','-s','src/test/python','-p','test_b4_2b3_*.py','-v'],cwd=root,check=True)
    saved=backup(root);print('B4.2B.3 FULL MANAGED SOURCE BACKUP '+str(saved),flush=True)
    try:
        for rel in [STANDING,NEW]:
            src=root/STAGE/'updated'/rel;dst=root/rel;dst.parent.mkdir(parents=True,exist_ok=True)
            tmp=dst.with_name(dst.name+'.b42b3_stage');shutil.copy2(src,tmp);tmp.replace(dst)
        original=raw(root/ECO);replacement=updated_economy(root,original)
        tmp=(root/ECO).with_name('EconomyRuntime.java.b42b3_stage');tmp.write_bytes(replacement);tmp.replace(root/ECO)
        cmd=['py','-3'] if os.name=='nt' else [sys.executable]
        subprocess.run(cmd+[str(root/'tools/geodata/b4_2b3_opening_reserves.py'),'--repo',str(root)],check=True,cwd=root)
        after=check(root)
        if any(v!='ALREADY_INSTALLED' for v in after.values()):raise RuntimeError('Post-install version mismatch: '+str(after))
        if run_gradle:
            wrapper=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
            if not wrapper.is_file():raise FileNotFoundError('Gradle wrapper missing')
            subprocess.run(['cmd','/c',str(wrapper),'clean','build'] if os.name=='nt' else ['bash',str(wrapper),'clean','build'],cwd=root,check=True)
        mf=saved/'ROLLBACK_MANIFEST.json';d=json.loads(mf.read_text());d['complete']=True;mf.write_text(json.dumps(d,indent=2))
        print('B4.2B.3 INSTALLED (resource plan, verified transaction command); snapshot '+saved.name,flush=True)
        print('No Minecraft saves modified by the installer. Native grants require explicit /kwstocks grant.',flush=True)
    except BaseException:
        print('B4.2B.3 ERROR: Attempting automatic source/resource restoration',flush=True,file=sys.stderr)
        rollback(root,saved.name)
        raise

def main():
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT)
    g=p.add_mutually_exclusive_group(required=True)
    g.add_argument('--check',action='store_true');g.add_argument('--install',action='store_true');g.add_argument('--rollback')
    a=p.parse_args()
    try:
        if a.check:print(json.dumps(check(a.root.resolve()),indent=2))
        elif a.install:install(a.root.resolve())
        else:rollback(a.root.resolve(),a.rollback)
    except Exception as e:print('B4.2B.3 SAFE STOP:',repr(e),file=sys.stderr,flush=True);sys.exit(1)
if __name__=='__main__':main()
