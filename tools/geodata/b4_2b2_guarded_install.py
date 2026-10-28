#!/usr/bin/env python3
"""B4.2B.2 guarded worldwide starting-standing / worksite roles install.

Never rewrites B4.0, B4.1, B4.2, B4.3A resource sources,
EconomyRuntime saves, player worlds, authored Winterfell or existing ledgers.
"""
from __future__ import annotations
import argparse,datetime,hashlib,json,os,shutil,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('B4_2B2_STAGE')
JAVA=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
EXISTING=[JAVA/'KnownWorldB41Activation.java']
NEW=[JAVA/'KnownWorldB42B2EconomicStanding.java']
RES=[Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_household_economy_b4_2b2_v1')]
FILES=EXISTING+NEW+RES
SNAP=Path('tools/geodata/output/b4_2b2_safe_snapshots')
REQUIRE=[
 'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1/manifest.json',
 'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a1_v1/manifest.json',
 'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_daily_b4_2_v1/manifest.json',
 'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_mobility_b4_2b_v1/manifest.json',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42BPersonalEvents.java',
]
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def src_bytes(path):return path.read_bytes().replace(b'\r\n',b'\n')
def check(root):
 root=Path(root);result={}
 for rel in EXISTING:
  live=root/rel;old=root/STAGE/'baseline'/rel;new=root/STAGE/'updated'/rel
  if not (live.is_file() and old.is_file() and new.is_file()):raise FileNotFoundError('Missing guarded source '+str(rel))
  data=src_bytes(live)
  result[str(rel)]='READY' if data==src_bytes(old) else 'ALREADY_INSTALLED' if data==src_bytes(new) else 'UNKNOWN_LOCAL_EDITS'
 for rel in NEW:
  live=root/rel;up=root/STAGE/'updated'/rel
  if not up.is_file():raise FileNotFoundError('Missing staged '+str(rel))
  result[str(rel)]='NEW' if not live.exists() else 'ALREADY_INSTALLED' if live.is_file() and src_bytes(live)==src_bytes(up) else 'UNKNOWN_LOCAL_EDITS'
 for rel in RES:
  live=root/rel
  if not live.exists():result[str(rel)]='NEW'
  elif live.is_dir() and (live/'manifest.json').is_file():
   try:
    j=json.loads((live/'manifest.json').read_text(encoding='utf8'))
    result[str(rel)]='ALREADY_INSTALLED' if j.get('schemaVersion')=='knownworld.b4_2b2.worldwide_household_standing.v1' else 'UNKNOWN_LOCAL_EDITS'
   except Exception:result[str(rel)]='UNKNOWN_LOCAL_EDITS'
  else:result[str(rel)]='UNKNOWN_LOCAL_EDITS'
 for rel in REQUIRE:
  if not (root/rel).is_file():raise FileNotFoundError('Required previous batch file absent: '+rel)
 return result

def backup(root):
 stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
 folder=root/SNAP/stamp;folder.mkdir(parents=True,exist_ok=False)
 entries=[]
 for rel in FILES:
  path=root/rel
  if path.is_symlink():raise ValueError('Refusing linked source '+str(rel))
  typ='file' if path.is_file() else 'dir' if path.is_dir() else 'missing' if not path.exists() else 'unsafe'
  if typ=='unsafe':raise ValueError('Unsafe managed path '+str(rel))
  desc={'path':rel.as_posix(),'type':typ}
  if typ=='file':
   target=folder/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
   desc['sha256']=sha(target)
  elif typ=='dir':
   target=folder/rel;shutil.copytree(path,target)
   desc['contents']={x.relative_to(target).as_posix():sha(x) for x in target.rglob('*') if x.is_file()}
  entries.append(desc)
 (folder/'ROLLBACK_MANIFEST.json').write_text(json.dumps({'batch':'B4_2B2','complete':False,'items':entries},indent=2),encoding='utf8')
 return folder

def rollback(root,label):
 root=Path(root);base=root/SNAP
 if label=='latest':
  candidates=sorted(f for f in base.iterdir() if f.is_dir() and (f/'ROLLBACK_MANIFEST.json').is_file()) if base.is_dir() else []
  if not candidates:raise FileNotFoundError('No snapshot to restore')
  folder=candidates[-1]
 else:
  if not label.replace('_','').isdigit():raise ValueError('Unsafe snapshot ID')
  folder=base/label
 data=json.loads((folder/'ROLLBACK_MANIFEST.json').read_text(encoding='utf8'))
 if data.get('batch')!='B4_2B2' or {v['path'] for v in data['items']}!={v.as_posix() for v in FILES}:
  raise ValueError('Snapshot manifest mismatch')
 for row in data['items']:
  copy=folder/row['path']
  if row['type']=='file' and (not copy.is_file() or sha(copy)!=row['sha256']):raise ValueError('Corrupt backup file')
  if row['type']=='dir':
   if not copy.is_dir() or {x.relative_to(copy).as_posix():sha(x) for x in copy.rglob('*') if x.is_file()}!=row['contents']:
    raise ValueError('Corrupt backed-up directory')
 for row in data['items']:
  dst=root/row['path'];saved=folder/row['path'];typ=row['type']
  if dst.is_symlink():raise ValueError('Refusing symlink during rollback')
  if dst.is_dir():shutil.rmtree(dst)
  elif dst.is_file():dst.unlink()
  elif dst.exists():raise ValueError('Unsafe rollback destination')
  if typ=='file':dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(saved,dst)
  elif typ=='dir':dst.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(saved,dst)
 print('RESTORED B4.2B.2 managed files from '+folder.name,flush=True)

def install(root,gradle=True,tests=True):
 state=check(root);print(json.dumps(state,indent=2),flush=True)
 if 'UNKNOWN_LOCAL_EDITS' in state.values():raise ValueError('SAFE STOP: Unknown local modifications, no source changes')
 if all(v=='ALREADY_INSTALLED' for v in state.values()):print('B4.2B.2 already installed');return
 if any(v=='ALREADY_INSTALLED' for v in state.values()):raise ValueError('Partial previous install; refusing mixed versions')
 if tests:
  command=['py','-3'] if os.name=='nt' else [sys.executable]
  subprocess.run(command+['-m','unittest','discover','-s','src/test/python','-p','test_b4_2b2_*.py','-v'],cwd=root,check=True)
 saved=backup(root);print('B4.2B.2 MANAGED BACKUP '+str(saved),flush=True)
 try:
  for rel in EXISTING+NEW:
   src=root/STAGE/'updated'/rel;dst=root/rel;dst.parent.mkdir(parents=True,exist_ok=True)
   tmp=dst.with_name(dst.name+'.b42b2_stage');shutil.copy2(src,tmp);tmp.replace(dst)
  cmd=['py','-3'] if os.name=='nt' else [sys.executable]
  subprocess.run(cmd+[str(root/'tools/geodata/b4_2b2_household_ownership.py'),'--repo',str(root)],cwd=root,check=True)
  if any(v!='ALREADY_INSTALLED' for v in check(root).values()):raise RuntimeError('Incomplete installation postcondition')
  if gradle:
   gw=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
   if not gw.is_file():raise FileNotFoundError('Missing gradle wrapper')
   subprocess.run(['cmd','/c',str(gw),'clean','build'] if os.name=='nt' else ['bash',str(gw),'clean','build'],cwd=root,check=True)
  mf=saved/'ROLLBACK_MANIFEST.json';data=json.loads(mf.read_text());data['complete']=True
  mf.write_text(json.dumps(data,indent=2),encoding='utf8')
  print('B4.2B.2 INSTALLED: generated global baseline and successful Gradle build. Snapshot: '+saved.name,flush=True)
 except BaseException:
  print('B4.2B.2 ERROR: Restoring source and generated resources...',file=sys.stderr,flush=True)
  rollback(root,saved.name)
  raise

def main():
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT)
 g=p.add_mutually_exclusive_group(required=True);g.add_argument('--check',action='store_true');g.add_argument('--install',action='store_true');g.add_argument('--rollback')
 a=p.parse_args();root=a.root.resolve()
 try:
  if a.check:print(json.dumps(check(root),indent=2))
  elif a.install:install(root)
  else:rollback(root,a.rollback)
 except Exception as e:
  print('B4.2B.2 SAFE STOP:',repr(e),file=sys.stderr,flush=True);sys.exit(1)
if __name__=='__main__':main()
