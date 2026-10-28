#!/usr/bin/env python3
"""B4.2B guarded NPC discovery / verified events / economic readiness install.

Creates immutable managed-file snapshot. Full population shards, existing
EconomyRuntime inventory saves, Minecraft worlds, Winterfell and dialogue
histories are NEVER modified by this installer.
"""
from __future__ import annotations
import argparse,datetime,hashlib,json,os,shutil,subprocess,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('B4_2B_STAGE')
JAVA='src/main/java/dev/dtzudontsay/knownworld/simulation/population/'
EXISTING=[JAVA+x for x in ('KnownWorldB41Activation.java','KnownWorldB42DailyLife.java','KnownWorldB43OffscreenTravel.java')]
NEW=[JAVA+x for x in ('KnownWorldB42BSpatialDiscovery.java','KnownWorldB42BPersonalEvents.java','KnownWorldB42BEconomyReadiness.java')]
MOB=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_mobility_b4_2b_v1')
ECO=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_economy_readiness_b4_2b_v1')
RESOURCE=[MOB,ECO]
SOURCES=[Path(p) for p in EXISTING+NEW]
FILES=SOURCES+RESOURCE
SNAP=Path('tools/geodata/output/b4_2b_safe_snapshots')

def normalize(p):
    content=p.read_bytes()
    return content.replace(b'\r\n',b'\n') if p.suffix=='.java' else content

def check(root):
    root=Path(root);status={}
    for rel in EXISTING:
        orig=root/STAGE/'baseline'/rel;upd=root/STAGE/'updated'/rel;live=root/rel
        if not all(p.is_file() for p in (orig,upd,live)):raise FileNotFoundError('Missing guarded Java file: '+rel)
        actual=normalize(live)
        status[rel]='READY' if actual==normalize(orig) else 'ALREADY_INSTALLED' if actual==normalize(upd) else 'UNKNOWN_LOCAL_EDITS'
    for rel in NEW:
        upd=root/STAGE/'updated'/rel;live=root/rel
        if not upd.is_file():raise FileNotFoundError('Missing staged new Java file: '+rel)
        status[rel]='NEW' if not live.exists() else 'ALREADY_INSTALLED' if live.is_file() and normalize(live)==normalize(upd) else 'UNKNOWN_LOCAL_EDITS'
    for resource in RESOURCE:
        path=root/resource
        if not path.exists():status[str(resource)]='NEW'
        elif path.is_dir() and (path/'manifest.json').is_file():status[str(resource)]='ALREADY_INSTALLED'
        else:status[str(resource)]='UNKNOWN_LOCAL_EDITS'
    for req in [
      'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a1_v1/manifest.json',
      'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_daily_b4_2_v1/manifest.json',
      'src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json']:
        if not (root/req).is_file():raise FileNotFoundError('Required B4.1/B4.2/C4.11 resource missing: '+req)
    return status

def hash_file(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda:f.read(4*1024*1024),b''):h.update(b)
    return h.hexdigest()

def snapshot(root):
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    dest=root/SNAP/stamp;dest.mkdir(parents=True)
    items=[]
    for rel in FILES:
        source=root/rel
        if source.is_symlink():raise ValueError('Unsafe symlink: '+str(rel))
        typ='dir' if source.is_dir() else 'file' if source.is_file() else 'missing' if not source.exists() else 'unsafe'
        if typ=='unsafe':raise ValueError('Unsafe snapshot path: '+str(rel))
        meta={'path':rel.as_posix(),'type':typ}
        if typ=='file':
            target=dest/rel;target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(source,target);meta['sha256']=hash_file(target)
        elif typ=='dir':
            target=dest/rel;shutil.copytree(source,target)
            meta['contents']={p.relative_to(source).as_posix():hash_file(p) for p in source.rglob('*') if p.is_file()}
        items.append(meta)
    (dest/'ROLLBACK_MANIFEST.json').write_text(json.dumps({'batch':'B4_2B','complete':False,'items':items},indent=2),encoding='utf8')
    return dest

def rollback(root,label):
    base=root/SNAP
    if label=='latest':
        possible=sorted(x for x in base.iterdir() if x.is_dir() and (x/'ROLLBACK_MANIFEST.json').is_file()) if base.is_dir() else []
        if not possible:raise FileNotFoundError('No B4.2B snapshots')
        folder=possible[-1]
    else:
        if not label.isdigit() and not (len(label)==22 and label.replace('_','').isdigit()):
            raise ValueError('Use an exact B4.2B timestamp or latest')
        folder=base/label
    manifest=json.loads((folder/'ROLLBACK_MANIFEST.json').read_text(encoding='utf8'))
    expected={p.as_posix() for p in FILES}
    if manifest.get('batch')!='B4_2B' or {i['path'] for i in manifest['items']}!=expected:raise ValueError('Wrong rollback manifest')
    for item in manifest['items']:
        source=folder/item['path'];typ=item['type']
        if typ=='file' and (not source.is_file() or hash_file(source)!=item['sha256']):raise ValueError('Snapshot checksum mismatch')
        if typ=='dir':
            if not source.is_dir():raise ValueError('Missing backup directory')
            if {p.relative_to(source).as_posix():hash_file(p) for p in source.rglob('*') if p.is_file()}!=item['contents']:
                raise ValueError('Corrupt directory backup')
    for item in manifest['items']:
        relative=Path(item['path']);target=root/relative;prior=folder/relative
        if target.is_symlink():raise ValueError('Refusing to delete symlink')
        if target.is_dir():shutil.rmtree(target)
        elif target.is_file():target.unlink()
        elif target.exists():raise ValueError('Unsafe rollback target')
        if item['type']=='file':target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(prior,target)
        elif item['type']=='dir':target.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(prior,target)
    print('RESTORED B4.2B managed source and resource files: '+folder.name,flush=True)

def install(root,run_gradle=True,run_tests=True):
    statuses=check(root);print(json.dumps(statuses,indent=2),flush=True)
    errors=[k for k,s in statuses.items() if s=='UNKNOWN_LOCAL_EDITS']
    if errors:raise ValueError('SAFE STOP unknown local file edits: '+', '.join(errors))
    if all(s=='ALREADY_INSTALLED' for s in statuses.values()):
        print('B4.2B already installed; nothing changed');return
    if any(s=='ALREADY_INSTALLED' for s in statuses.values()):raise ValueError('Partial B4.2B state; refuse mixed versions')
    if run_tests:
        subprocess.run(['py','-3','-m','unittest','discover','-s','src/test/python','-p','test_b4_2b_*.py','-v'] if os.name=='nt' else [sys.executable,'-m','unittest','discover','-s','src/test/python','-p','test_b4_2b_*.py','-v'],cwd=root,check=True)
    saved=snapshot(root);print('MANAGED SNAPSHOT:',saved,flush=True)
    try:
        for rel in EXISTING+NEW:
            src=root/STAGE/'updated'/rel;to=root/rel;to.parent.mkdir(parents=True,exist_ok=True)
            tmp=to.with_name(to.name+'.b42bpartial');shutil.copy2(src,tmp);tmp.replace(to)
        command=[sys.executable] if os.name!='nt' else ['py','-3']
        for script in ('b4_2b_mobility_index.py','b4_2b_economy_readiness.py'):
            subprocess.run(command+[str(root/'tools/geodata'/script),'--repo',str(root)],check=True,cwd=root)
        after=check(root)
        if any(v!='ALREADY_INSTALLED' for v in after.values()):raise RuntimeError('Installation postcondition mismatch: '+str(after))
        if run_gradle:
            gradlew=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
            if not gradlew.is_file():raise FileNotFoundError('No gradle wrapper')
            subprocess.run(['cmd','/c',str(gradlew),'clean','build'] if os.name=='nt' else ['bash',str(gradlew),'clean','build'],check=True,cwd=root)
        infofile=saved/'ROLLBACK_MANIFEST.json';data=json.loads(infofile.read_text());data['complete']=True
        infofile.write_text(json.dumps(data,indent=2),encoding='utf8')
        print('B4.2B INSTALLED, build complete; snapshot:',saved.name,flush=True)
    except BaseException:
        print('B4.2B FAILURE. Attempting automatic restore...',file=sys.stderr,flush=True)
        rollback(root,saved.name)
        raise

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',type=Path,default=ROOT)
    flags=p.add_mutually_exclusive_group(required=True)
    flags.add_argument('--check',action='store_true');flags.add_argument('--install',action='store_true');flags.add_argument('--rollback',metavar='ID_OR_LATEST');args=p.parse_args()
    root=args.root.resolve()
    try:
        if args.check:print(json.dumps(check(root),indent=2))
        elif args.install:install(root)
        else:rollback(root,args.rollback)
    except Exception as e:print('B4.2B SAFE STOP:',repr(e),file=sys.stderr,flush=True);sys.exit(1)
if __name__=='__main__':main()
