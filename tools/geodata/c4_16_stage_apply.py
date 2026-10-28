#!/usr/bin/env python3
"""C4.16/B4.2A safe staged source installer and rollback.

Guarded source matching; snapshots exact resources before regeneration. No git, no
save changes, no population regeneration. Fails closed on unknown local edits.
"""
from __future__ import annotations
import argparse,datetime,hashlib,json,shutil,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=Path('C4_16_STAGE')
RSL=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
FILES=[
 'tools/geodata/c4_10_shared_layout.py',
 'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java',
 'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java']
RESOURCES=[RSL/'universal_construction_c4_11_global_v1.json', RSL/'c4_11_global_tiles',
 RSL/'world_commutes_c4_15_v1',RSL/'world_projection_b4_1a1_v1',RSL/'world_daily_b4_2_v1',
 Path('tools/geodata/output/c4_10_shared_layout')]
SNAPROOT=Path('tools/geodata/output/c4_16_safe_snapshots')
def hash_file(p:Path)->str:return hashlib.sha256(p.read_bytes()).hexdigest()
def must(repo):
 for rel in FILES:
  for path in [repo/rel,repo/STAGE/'baseline'/rel,repo/STAGE/'updated'/rel]:
   if not path.is_file():raise FileNotFoundError(str(path))
 for rel in [RSL/'world_population_b4_v1/manifest.json', RSL/'universal_construction_c4_11_global_v1.json',
             RSL/'world_projection_b4_1a1_v1/manifest.json',RSL/'world_daily_b4_2_v1/manifest.json']:
  if not (repo/rel).is_file():raise FileNotFoundError('Required prior batch missing: '+str(rel))
 pop=json.loads((repo/RSL/'world_population_b4_v1/manifest.json').read_text(encoding='utf-8'))
 if tuple(pop.get(k) for k in ('peopleTotal','householdsTotal','modeledWorkersTotal','siteCount')) != (3760684,799343,1880341,248):
  raise ValueError('B4.0 census mismatch; will not install')

def status(repo):
 must(repo)
 res={}
 for rel in FILES:
  source=repo/rel;before=repo/STAGE/'baseline'/rel;after=repo/STAGE/'updated'/rel
  original=hash_file(source)
  if original==hash_file(after):state='ALREADY_INSTALLED'
  elif original==hash_file(before):state='READY'
  else:state='UNKNOWN_LOCAL_EDITS'
  res[rel]=state
 return res

def snapshot(repo):
 stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
 root=repo/SNAPROOT/stamp
 root.mkdir(parents=True)
 copied=[]
 for rel in FILES+RESOURCES:
  f=repo/rel
  if not f.exists():continue
  to=root/rel;to.parent.mkdir(parents=True,exist_ok=True)
  if f.is_dir():shutil.copytree(f,to)
  else:shutil.copy2(f,to)
  copied.append(str(rel))
 meta={'kind':'KNOWNWORLD_C4_16_BACKUP','resourcesCapturedBeforeRegeneration':True,
       'files':copied,'created':stamp}
 (root/'C4_16_ROLLBACK_MANIFEST.json').write_text(json.dumps(meta,indent=2),encoding='utf-8')
 return root

def apply(repo):
 states=status(repo)
 print(json.dumps(states,indent=2))
 unsafe=[name for name,state in states.items() if state=='UNKNOWN_LOCAL_EDITS']
 if unsafe:raise ValueError('LOCAL FILES DO NOT MATCH KNOWN BATCHES, no files changed: '+', '.join(unsafe))
 if all(x=='ALREADY_INSTALLED' for x in states.values()):
  print('C4.16 already applied; no changes made. Existing rollback snapshot retained.')
  return
 backup=snapshot(repo)
 print('BACKUP SAVED:',backup,flush=True)
 for rel,st in states.items():
  if st!='READY':continue
  source=repo/rel;tmp=source.with_suffix(source.suffix+'.c416tmp')
  shutil.copyfile(repo/STAGE/'updated'/rel,tmp)
  tmp.replace(source)
 print('Staged source installed. Now run resource generators in BAT.',flush=True)

def rollback(repo,stamp):
 original=repo/SNAPROOT/stamp
 m=original/'C4_16_ROLLBACK_MANIFEST.json'
 if not m.is_file():raise FileNotFoundError('Unknown rollback snapshot '+str(original))
 meta=json.loads(m.read_text(encoding='utf-8'))
 if meta.get('kind')!='KNOWNWORLD_C4_16_BACKUP':raise ValueError('Not C4.16 snapshot')
 for relative in meta['files']:
  rel=Path(relative);src=original/rel;dst=repo/rel
  if src.is_dir():
   if dst.exists():shutil.rmtree(dst)
   shutil.copytree(src,dst)
  else:
   dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
 print('Restored snapshot:',stamp)

def main():
 a=argparse.ArgumentParser()
 a.add_argument('--root',type=Path,default=ROOT)
 a.add_argument('--check',action='store_true')
 a.add_argument('--rollback',metavar='SNAPSHOT_FOLDER')
 options=a.parse_args()
 repo=options.root.resolve()
 try:
  if options.rollback:rollback(repo,options.rollback)
  elif options.check:print(json.dumps(status(repo),indent=2))
  else:apply(repo)
 except (ValueError,FileNotFoundError,OSError) as exc:
  print('C4.16 SAFE STOP:',exc,file=sys.stderr);sys.exit(1)
if __name__=='__main__':main()
