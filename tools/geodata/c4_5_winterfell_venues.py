#!/usr/bin/env python3
"""Append C3 specialty workplaces to V3 chunk-local WINTERFELL blueprint (fresh worlds).

Must run AFTER c4_5_connected_rollout.py --winterfell --install: source roster
then retains original 5000 people, 1136 homes, 378 V2 workplaces, and original
castle construction. Additional C3 facilities follow actual generated worker
job/building counts by neighborhood; a deferred list records blocked concepts.
Requires companion Java C4.5 patch (C3: templates and signs).
"""
from __future__ import annotations
import collections, hashlib, json, math, os, shutil
from pathlib import Path
from c4_5_connected_rollout import ROOT, SC, CASE, read, backup_and_install, stable, qceil, RESTRICTED, NO_GENERATION
V2=CASE/'winterfell_v2.json'; V3=CASE/'winterfell_v3_worldgen.json';C3=SC/'architecture_templates_c3_v1.json'
OUT=ROOT/'tools/geodata/output/c4_5_connected_rollout'
AX,AZ=-4050173,-1976496

def overlaps(a,b,margin=4):
 return a[0]-margin<b[2] and b[0]-margin<a[2] and a[1]-margin<b[3] and b[1]-margin<a[3]

def pixels(x1,z1,x2,z2):
 if x1!=x2 and z1!=z2: raise ValueError('Not orthogonal')
 out=set()
 if x1==x2:
  for z in range(min(z1,z2),max(z1,z2)+1):
   for dx in (-1,0,1):out.add((x1+dx,z))
 else:
  for x in range(min(x1,x2),max(x1,x2)+1):
   for dz in (-1,0,1):out.add((x,z1+dz))
 return out

def append_venues(install=False):
 data=read(V2);root=read(V3); tpls={t['id']:t for t in read(C3)['templates']}
 if len(data['residents'])!=5000 or len(data['households'])!=1136:
  raise ValueError('Unexpected Winterfell V2 identity/household totals')
 if root.get('c45'):
  print('C4.5 Winterfell specialty venues are already present; no duplication.')
  return {'additionalWorkplaces':root['c45']['addedWorkplaces'],'unplaced':sum(root['c45']['unplacedByGeometry'].values()),
          'indexedChunks':len(root['chunks']),'alreadyInstalled':True}
 if len(root['buildings'])<1514 or len(root['buildings'])>1514+100:
  raise ValueError('Unexpected Winterfell V3 baseline; refuse unknown generated blueprint')
 if root.get('c451') and root['c451'].get('newInnerWorkplaces')!=12:
  raise ValueError('Unknown castle-ward swap version')
 labels={'SMITH':'Smithy','CARPENTER':'Carpentry','MASON':'Masonry','FARMER':'Farm',
   'LIVESTOCK':'Pasture','FORESTER':'Forest Camp','MERCHANT':'Market',
   'TRANSPORT':'Cart Depot','GUARD':'Barracks','SERVANT':'Service Hall',
   'COOK':'Kitchen','HEALER':'Healer','ADMIN':'Administration'}
 old_roles={w['id']:w.get('role','') for w in data['workplaces']}
 for b in root['buildings']:
  if b['id'] in old_roles:b['label']=labels.get(old_roles[b['id']], old_roles[b['id']].replace('_',' ').title())
 centers={x['id']:(int(x['cx']),int(x['cz'])) for x in data['sites']}
 baseline=len(root['buildings'])
 original=collections.Counter()
 for p in data['residents']:
  if p.get('occupationPlanVersion')==45 and p.get('occupationBuildingC45'):
   original[(p['site'],p['occupationBuildingC45'])]+=1
 ext=[];deferred={}
 interior={'ADMINISTRATION','GREAT_HALL','COURTYARD','KEEP','JUSTICE_HALL','HEALER','SCHOLASTIC_HALL'}
 for (site,kind),workers in sorted(original.items()):
  t=tpls.get(kind)
  if site=='castle' and kind in interior:
   deferred[f'{site}/{kind}']={'workers':workers,'reason':'reserved for instance-relative castle rooms; no exterior duplicate'}
   continue
  if kind in RESTRICTED or kind in NO_GENERATION or not t or t.get('requiresPolicy') or t.get('requiresShore') or t.get('requiresMountainResource'):
   deferred[f'{site}/{kind}']={'workers':workers,'reason':'site/policy/terrain authorization required'}
   continue
  cap=int(t.get('maxStaff',0))
  if cap<=0:
   deferred[f'{site}/{kind}']={'workers':workers,'reason':'template has no worker capacity'}
   continue
  ext.extend((site,kind) for _ in range(qceil(workers,cap)))
 # Build compact lookup to connect each new facility to actual V3 pre-existing streets.
 roads=set();chunkmap={}
 for j in root['chunks']:
  chunkmap[(j['x'],j['z'])]={'r':set(j['r']),'b':set(j['b'])}
  for pixel in j['r']:
   roads.add((j['x']*16+(pixel&15)-AX,j['z']*16+(pixel>>4)-AZ))
 bycell=collections.defaultdict(list)
 for x,z in roads:bycell[(x//64,z//64)].append((x,z))
 rects=[(b['x'],b['z'],b['x']+b['w'],b['z']+b['d']) for b in root['buildings']]
 blocked=set()
 def mark_blocked(rect):
  for bx in range(rect[0]-2,rect[2]+2):
   for bz in range(rect[1]-2,rect[3]+2):blocked.add((bx,bz))
 for rect in rects:mark_blocked(rect)
 # Town centers are dispersed, so occupied farms/hamlets get local work capacity.
 placed=collections.Counter();fail=collections.Counter();newpaths=set()
 for site,kind in ext:
  t=tpls[kind];w=int(t['c1Width']);d=int(t['c1Depth'])
  cx,cz=centers[site]
  # Castle jobs that don't have dedicated rooms use the adjacent winter-town.
  if site=='castle':cx,cz=centers['wintertown']
  found=False
  salt=stable(f'{site}:{kind}:{placed[(site,kind)]}')
  for attempt in range(480):
   angle=((attempt+salt%31)*2.399963229728653)
   radius=120+(attempt//26)*30
   x=cx+int(math.cos(angle)*radius)-w//2
   z=cz+int(math.sin(angle)*radius)-d//2
   if abs(x)>4400 or abs(z)>4400:continue
   rect=(x,z,x+w,z+d)
   if any(overlaps(rect,b) for b in rects):continue
   # Place a three-wide street spur, seeking an existing nearby road pixel.
   door=(x+w//2,z+d+2)
   nearby=[]
   bx,bz=door[0]//64,door[1]//64
   for ox in range(-5,6):
    for oz in range(-5,6):
     for rx,rz in bycell.get((bx+ox,bz+oz),()):
      dist=abs(rx-door[0])+abs(rz-door[1])
      if dist<400:nearby.append((dist,rx,rz))
   if not nearby:continue
   path=None
   for _,rx,rz in sorted(nearby)[:12]:
    for first in ('x','z'):
     mid=(rx,door[1]) if first=='x' else (door[0],rz)
     candidate=pixels(*door,*mid)|pixels(*mid,rx,rz)
     # Ignore the existing roadway cells; all others must respect homes/shops.
     if not any(pt in blocked for pt in candidate if pt not in roads):
      path=candidate;break
    if path is not None:break
   if path is None:continue
   n=placed[(site,kind)];newid=f'wf3_c45_{site}_{kind.lower()}_{n:04d}'
   root['buildings'].append({'id':newid,'t':'C3:'+kind,'label':kind.replace('_',' ').title(),
       'x':x,'z':z,'w':w,'d':d,'cap':0,'site':site,'jobCapacity':t['maxStaff']})
   rects.append(rect);mark_blocked(rect);placed[(site,kind)]+=1
   newpaths.update(path)
   for xx,zz in path:bycell[(xx//64,zz//64)].append((xx,zz))
   roads.update(path)
   found=True;break
  if not found:fail[(site,kind)]+=1
 # Re-index ALL structures: original V3 indices are stable, append new C3 structures.
 for i in range(baseline,len(root['buildings'])):
  b=root['buildings'][i];x0,z0=AX+b['x'],AZ+b['z']
  for ccx in range((x0-1)//16,(x0+b['w'])//16+1):
   for ccz in range((z0-1)//16,(z0+b['d'])//16+1):
    chunkmap.setdefault((ccx,ccz),{'r':set(),'b':set()})['b'].add(i)
 for xx,zz in newpaths:
  wx,wz=AX+xx,AZ+zz;ccx,ccz=wx//16,wz//16
  chunkmap.setdefault((ccx,ccz),{'r':set(),'b':set()})['r'].add((wz-ccz*16)*16+(wx-ccx*16))
 root['chunks']=[{'x':cx,'z':cz,'r':sorted(v['r']),'b':sorted(v['b'])} for (cx,cz),v in sorted(chunkmap.items())]
 root['versionHash']=hashlib.sha256(json.dumps(root['buildings'],sort_keys=True).encode()).hexdigest()
 root['c45']={'source':'C4_4_EMPLOYMENT_OF_ACTUAL_GENERATED_WINTERFELL_RESIDENTS',
    'addedWorkplaces':len(root['buildings'])-baseline,'skippedByPolicy':deferred,
    'unplacedByGeometry':{f'{s}/{k}':v for (s,k),v in fail.items()},
    'authoritativeResidentCount':5000,'newPeopleCreated':0,'newWorldOnly':True,
    'original1514Preserved':not bool(root.get('c451')),'preservesHousingIds':True,
    'castleRoomJobsAreLogicalOnly':True}
 OUT.mkdir(parents=True,exist_ok=True)
 preview=OUT/'winterfell_v3_c45_preview.json'
 preview.write_text(json.dumps(root,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf-8')
 report={'additionalWorkplaces':len(root['buildings'])-baseline,'templatesUsed':dict(sorted(collections.Counter(b['t'][3:] for b in root['buildings'][baseline:]).items())),
         'unplaced':sum(fail.values()),'reservedInteriorJobs':deferred,'placementFailures':dict((f'{a}/{b}',n) for (a,b),n in fail.items()),
         'indexedChunks':len(root['chunks']),'existingV3Buildings':baseline,'census':5000}
 (OUT/'winterfell_c45_venues_report.json').write_text(json.dumps(report,indent=2)+'\n')
 if install:backup_and_install(V3,preview.read_text(encoding='utf-8'))
 print(json.dumps({'additionalWorkplaces':report['additionalWorkplaces'],'unplaced':report['unplaced'],
   'indexedChunks':report['indexedChunks']},indent=2))
 return report

if __name__=='__main__':
 import argparse
 ap=argparse.ArgumentParser();ap.add_argument('--install',action='store_true');a=ap.parse_args()
 if a.install and os.environ.get('KNOWNWORLD_C45_CONFIRM')!='NEW_WORLD':
  raise SystemExit('Set KNOWNWORLD_C45_CONFIRM=NEW_WORLD for install')
 append_venues(a.install)
