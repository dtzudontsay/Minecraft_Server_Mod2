#!/usr/bin/env python3
"""Guarded source/resource installation, full snapshots, automatic rollback.
Does not touch run/saves, B4.0 identities, previous economy inventories.
"""
from __future__ import annotations
import argparse, datetime, hashlib, json, os, shutil, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('B4_2B4_STAGE')
JAVA=Path('src/main/java/dev/dtzudontsay/knownworld/simulation')
STANDING=JAVA/'population/KnownWorldB42B2EconomicStanding.java'
DAILY=JAVA/'population/KnownWorldB42B4HouseholdDaily.java'
NATIVE=JAVA/'economy/KnownWorldB42B4NativeDailyTransactions.java'
B43=JAVA/'population/KnownWorldB42B3StartingStock.java'
ECON=JAVA/'economy/EconomyRuntime.java'
RESOURCE=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_household_daily_b4_2b4_v1')
MANAGED=[STANDING,DAILY,NATIVE,RESOURCE]
SNAPS=Path('tools/geodata/output/b4_2b4_safe_snapshots')
PREREQ=[Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_household_economy_b4_2b2_v1/manifest.json'),
        Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_opening_reserves_b4_2b3_v1/manifest.json')]
B43_PERMISSION=b'.requires(Commands.hasPermission(Commands.LEVEL_OWNERS))'
COMMIT_ANCHOR=b'public void commitB42B3SnapshotNow() throws IOException'

def norm(path):return path.read_bytes().replace(b'\r\n',b'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def check(root):
 root=Path(root);result={}
 for rel in PREREQ:
  if not (root/rel).is_file():raise FileNotFoundError('Missing prerequisite '+str(rel))
 for rel,anchor in [(B43,B43_PERMISSION),(ECON,COMMIT_ANCHOR)]:
  if not (root/rel).is_file() or anchor not in norm(root/rel):
   raise ValueError('B4.2B.3 source/hotfix not present: '+str(rel))
 for rel in (STANDING,DAILY,NATIVE):
  baseline=root/STAGE/'baseline'/rel
  updated=root/STAGE/'updated'/rel
  if not updated.is_file():raise FileNotFoundError('Staged file missing: '+str(rel))
  live=root/rel
  if live.is_symlink():raise ValueError('Unsafe linked live source')
  if rel==STANDING:
   if not baseline.is_file() or not live.is_file():raise FileNotFoundError('Missing B4.2B.3 economic standing source')
   result[str(rel)]='READY' if norm(live)==norm(baseline) else 'ALREADY_INSTALLED' if norm(live)==norm(updated) else 'UNKNOWN_LOCAL_EDITS'
  else:
   result[str(rel)]='NEW' if not live.exists() else 'ALREADY_INSTALLED' if live.is_file() and norm(live)==norm(updated) else 'UNKNOWN_LOCAL_EDITS'
 p=root/RESOURCE;pend=p.with_name(p.name+'.pending')
 if p.is_symlink() or pend.is_symlink() or pend.exists():raise ValueError('Unsafe or unfinished derived resource directory; inspect before install')
 if not p.exists():result[str(RESOURCE)]='NEW'
 elif p.is_dir() and (p/'manifest.json').is_file():
  try:
   m=json.loads((p/'manifest.json').read_text(encoding='utf-8'))
   result[str(RESOURCE)]='ALREADY_INSTALLED' if m.get('schemaVersion')=='knownworld.b4_2b4.household_daily_plan.v1' and m.get('dailyTransactionsActivated') is False else 'UNKNOWN_LOCAL_EDITS'
  except Exception:result[str(RESOURCE)]='UNKNOWN_LOCAL_EDITS'
 else:result[str(RESOURCE)]='UNKNOWN_LOCAL_EDITS'
 return result

def content_hash(path):
 return {p.relative_to(path).as_posix():sha(p.read_bytes()) for p in path.rglob('*') if p.is_file() and not p.is_symlink()}
def snapshot(root):
 stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f');base=root/SNAPS/stamp
 if base.exists():raise ValueError('Backup collision')
 base.mkdir(parents=True);items=[]
 for rel in MANAGED:
  src=root/rel;dst=base/rel
  if src.is_symlink():raise ValueError('Linked managed path forbidden '+str(rel))
  typ='file' if src.is_file() else 'dir' if src.is_dir() else 'missing' if not src.exists() else 'unsafe'
  if typ=='unsafe':raise ValueError('Unsafe managed path '+str(rel))
  entry={'path':rel.as_posix(),'type':typ}
  if typ=='file':dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);entry['sha256']=sha(dst.read_bytes())
  if typ=='dir':
   if any(p.is_symlink() for p in src.rglob('*')):raise ValueError('Linked resource path forbidden')
   shutil.copytree(src,dst);entry['contents']=content_hash(dst)
  items.append(entry)
 (base/'ROLLBACK_MANIFEST.json').write_text(json.dumps({'batch':'B4_2B4','items':items,'complete':False},indent=2)+'\n',encoding='utf-8')
 return base

def rollback(root,snapshot_id):
 base=root/SNAPS
 if snapshot_id=='latest':
  options=sorted(p for p in base.iterdir() if p.is_dir() and (p/'ROLLBACK_MANIFEST.json').is_file()) if base.exists() else []
  if not options:raise FileNotFoundError('No backup')
  saved=options[-1]
 else:
  if not snapshot_id.replace('_','').isdigit():raise ValueError('Unsafe snapshot ID')
  saved=base/snapshot_id
 m=json.loads((saved/'ROLLBACK_MANIFEST.json').read_text(encoding='utf8'))
 if m.get('batch')!='B4_2B4' or {i['path'] for i in m['items']}!={x.as_posix() for x in MANAGED}:
  raise ValueError('Corrupt or mismatched snapshot manifest')
 for it in m['items']:
  orig=saved/it['path'];typ=it['type']
  if typ=='file' and (not orig.is_file() or sha(orig.read_bytes())!=it['sha256']):raise ValueError('Backup failed verification')
  if typ=='dir' and (not orig.is_dir() or content_hash(orig)!=it['contents']):raise ValueError('Backup directory failed verification')
  if typ not in ('dir','file','missing'):raise ValueError('Bad backup type')
 for it in m['items']:
  live=root/it['path'];orig=saved/it['path']
  if live.is_symlink():raise ValueError('Linked rollback target forbidden')
  if live.is_file():live.unlink()
  elif live.is_dir():shutil.rmtree(live)
  elif live.exists():raise ValueError('Unsafe rollback target')
  if it['type']=='file':live.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(orig,live)
  elif it['type']=='dir':live.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(orig,live)
 print('B4.2B.4 source/resource state restored: '+saved.name,flush=True)

def install(root,run_tests=True,run_build=True):
 state=check(root);print(json.dumps(state,indent=2),flush=True)
 if 'UNKNOWN_LOCAL_EDITS' in state.values():raise ValueError('Unknown local edits; refusing to overwrite')
 if all(v=='ALREADY_INSTALLED' for v in state.values()):print('Already installed.');return
 if any(v=='ALREADY_INSTALLED' for v in state.values()):raise ValueError('Partial installation; refusing mixed states')
 if run_tests:
  subprocess.run((['py','-3'] if os.name=='nt' else [sys.executable])+['-m','unittest','discover','-s','src/test/python','-p','test_b4_2b4_*.py','-v'],cwd=root,check=True)
 saved=snapshot(root);print('FULL MANAGED BACKUP: '+str(saved),flush=True)
 try:
  for rel in (STANDING,DAILY,NATIVE):
   src=root/STAGE/'updated'/rel;dest=root/rel;dest.parent.mkdir(parents=True,exist_ok=True)
   tmp=dest.with_name(dest.name+'.b42b4_new')
   if tmp.exists():raise ValueError('Stale staged temp: '+str(tmp))
   shutil.copy2(src,tmp);tmp.replace(dest)
  gen=root/'tools/geodata/b4_2b4_household_daily_plan.py'
  cmd=(['py','-3'] if os.name=='nt' else [sys.executable])+[str(gen),'--repo',str(root)]
  subprocess.run(cmd,cwd=root,check=True)
  after=check(root)
  if any(v!='ALREADY_INSTALLED' for v in after.values()):raise RuntimeError('Post-install mismatch: '+str(after))
  if run_build:
   wrapper=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
   if not wrapper.is_file():raise FileNotFoundError('Missing Gradle wrapper')
   subprocess.run(['cmd','/c',str(wrapper),'clean','build'] if os.name=='nt' else ['bash',str(wrapper),'clean','build'],cwd=root,check=True)
  m=saved/'ROLLBACK_MANIFEST.json';obj=json.loads(m.read_text());obj['complete']=True;m.write_text(json.dumps(obj,indent=2)+'\n')
  print('B4.2B.4 INSTALLED + BUILD COMPLETE; backup '+saved.name,flush=True)
  print('No native economy balances or Minecraft saves were touched by installer.',flush=True)
 except BaseException:
  print('B4.2B.4 FAILED: attempting automatic managed-file rollback',file=sys.stderr,flush=True)
  rollback(root,saved.name)
  raise

def main():
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT)
 q=p.add_mutually_exclusive_group(required=True);q.add_argument('--check',action='store_true');q.add_argument('--install',action='store_true');q.add_argument('--rollback')
 a=p.parse_args()
 try:
  if a.check:print(json.dumps(check(a.root.resolve()),indent=2))
  elif a.install:install(a.root.resolve())
  else:rollback(a.root.resolve(),a.rollback)
 except (Exception) as e:print('B4.2B.4 SAFE STOP: '+repr(e),file=sys.stderr,flush=True);sys.exit(1)
if __name__=='__main__':main()
