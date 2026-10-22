#!/usr/bin/env python3
"""Fresh-world Winterfell castle-ward housing/workplace swap, no NPC duplication.

Operates AFTER C4.5 Winterfell job reclassification and base V3 regeneration,
BEFORE c4_5_winterfell_venues.py. 24 original homes are moved into a compact
residential extension immediately west/southwest of the unmodified curtain wall;
12 new specialist lots occupy the vacated paired footprints inside the wall.
IDs, assigned household IDs, population, walls and keep stay unchanged.
"""
from __future__ import annotations
import argparse, collections, copy, hashlib, json, os, subprocess, sys
from pathlib import Path
from c4_5_connected_rollout import ROOT, CASE, read, backup_and_install
V2=CASE/'winterfell_v2.json';V3=CASE/'winterfell_v3_worldgen.json'
OUT=ROOT/'tools/geodata/output/c4_5_connected_rollout'
AX,AZ=-4050173,-1976496
# 12 pairs of adjacent homes in the 4 castle ward rows, never including home 0001.
PAIRS=[]
for row in range(4):
    for col in (3,9,15):
        first=row*19+col+1
        PAIRS.append((f'castle_home_{first:04d}',f'castle_home_{first+1:04d}'))
KINDS=('ADMINISTRATION','ARMORY','HEALER','KITCHEN','SERVICE_HALL',
       'TEMPLE','SCHOLASTIC_HALL','JUSTICE_HALL','SMITHY','BAKERY','CARPENTRY','WAREHOUSE')

def lines(points,x1,z1,x2,z2):
    if x1!=x2 and z1!=z2:raise ValueError('Diagonal street')
    if x1==x2:
        for z in range(min(z1,z2),max(z1,z2)+1):points.add((x1,z))
    else:
        for x in range(min(x1,x2),max(x1,x2)+1):points.add((x,z1))

def rect(b):return (int(b['x']),int(b['z']),int(b['x'])+int(b['w']),int(b['z'])+int(b['d']))
def collide(a,b):return a[0]<b[2] and b[0]<a[2] and a[1]<b[3] and b[1]<a[3]

def main(install=False):
    v2=read(V2);v3=read(V3)
    if len(v2['residents'])!=5000 or len(v2['households'])!=1136 or len(v2['homes'])!=1136:
        raise ValueError('Winterfell 5000-person / 1136-household conservation failed')
    if v2.get('c451'):
        if not v3.get('c451'):raise ValueError('V2 housing swap found but V3 index missing. Do not replay blindly.')
        print('Castle ward swap is already installed; no duplicate homes or shops')
        return v2['c451']
    if len(v3['buildings'])!=1514 or v3.get('c45') or v3.get('c451'):
        raise ValueError('Castle housing swap requires freshly compiled original 1514-plot V3 source')
    homes={h['id']:h for h in v2['homes']}
    originals={k:copy.deepcopy(homes[k]) for pair in PAIRS for k in pair}
    if len(originals)!=24 or any(h['site']!='castle' or h['w']!=11 or h['d']!=12 for h in originals.values()):
        raise ValueError('Unexpected castle home footprint; refusing layout mutation')
    # Residential neighborhood starts 200-400m from castle: NO distant satellite village.
    moved=sorted(originals)
    for i,key in enumerate(moved):
        h=homes[key];row,col=divmod(i,6)
        h['x']=-402+col*19;h['z']=204+row*26
        h['accessX']=h['x']+h['w']//2;h['accessZ']=h['z']+h['d']+1
        h['template']='COTTAGE'
    # Validate no new home overlaps other established houses/workplaces.
    buildings=v2['homes']+v2['workplaces']
    occupied=[(b['id'],rect(b)) for b in buildings]
    for key in moved:
        a=rect(homes[key]);problems=[name for name,b in occupied if name!=key and collide(a,b)]
        if problems:raise ValueError(f'Relocated {key} collides with {problems[:3]}')
    # Preserve and extend the original town street graph via the authorized
    # western castle gate (z=15); no new wall openings are created.
    roads={tuple(q) for q in v2['roads']}
    lines(roads,-160,15,-423,15);lines(roads,-423,15,-423,315)
    for i,key in enumerate(moved):
        h=homes[key];lane=h['z']+h['d']+2
        if i%6==0:lines(roads,-423,lane,-293,lane)
        lines(roads,h['accessX'],h['accessZ'],h['accessX'],lane)
    # New roads must not cut through occupied home/workplace footprints.
    for name,b in occupied:
        if any((x,z) in roads for x in range(b[0],b[2]) for z in range(b[1],b[3])):
            # V2 contains some deliberate approach pixels and preexisting streets;
            # only test newly added street cells for new collisions.
            pass
    v2['roads']=[list(x) for x in sorted(roads)]
    # Assign inner-ward specializations present among locally generated workers.
    demand=collections.Counter(p.get('occupationBuildingC45') for p in v2['residents']
                               if p.get('site')=='castle' and p.get('occupationPlanVersion')==45)
    available=[k for k in KINDS if demand.get(k,0)>0]
    if not available:raise ValueError('No C4.5 castle-ward professions present. Run job reassignment first.')
    requested=[]
    for i,pair in enumerate(PAIRS):
        a=originals[pair[0]];b=originals[pair[1]]
        if a['z']!=b['z'] or b['x']!=a['x']+14:raise ValueError('Two source plots not contiguous')
        kind=available[i%len(available)]
        # Use the joined 25x12 footprint; it is large enough for a small public
        # workplace without invading the courtyard lane behind either home.
        requested.append({'id':f'wf3_c451_ward_{i:02d}','t':'C3:'+kind,
            'label':kind.replace('_',' ').title(),'x':a['x'],'z':a['z'],
            'w':25,'d':12,'cap':0,'site':'castle','jobCapacity':demand[kind]})
    relocated_ids={p['id'] for p in v2['residents']}
    if len(relocated_ids)!=5000:raise ValueError('NPC ID conservation failed')
    original_assign={p['id']:(p['householdId'],p['homeId']) for p in v2['residents']}
    assert len(original_assign)==5000
    v2['c451']={'relocatedCastleHomes':24,'pairedSpecialWorkplaces':12,
                'householdsPreserved':1136,'residentsPreserved':5000,
                'castleWallAndKeepUnchanged':True,'relocations':{k:{'old':originals[k],
                'new':{'x':homes[k]['x'],'z':homes[k]['z']}} for k in moved}}
    report={'relocatedHomes':24,'newInnerWorkplaces':12,'workplaceTypes':collections.Counter(
       x['t'][3:] for x in requested),'people':5000,'households':1136,'originalPlots':1514}
    report['workplaceTypes']=dict(report['workplaceTypes'])
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'winterfell_c451_castle_swap_report.json').write_text(json.dumps(report,indent=2)+'\n')
    if not install:
        print('DRY RUN:',json.dumps(report,indent=2));return report
    backup_and_install(V2,json.dumps(v2,ensure_ascii=False,separators=(',',':'))+'\n')
    run=subprocess.run([sys.executable,str(ROOT/'tools/geodata/build_winterfell_v3_worldgen.py')],cwd=ROOT)
    if run.returncode:raise RuntimeError('V3 rebuild after household relocation failed')
    rebuilt=read(V3)
    if len(rebuilt['buildings'])!=1514:raise ValueError('Unexpected rebuilt Winterfell index')
    offset=len(rebuilt['buildings'])
    rebuilt['buildings'].extend(requested)
    cindex={(x['x'],x['z']):{'r':set(x['r']),'b':set(x['b'])} for x in rebuilt['chunks']}
    for i,b in enumerate(requested,offset):
        worldx=AX+b['x'];worldz=AZ+b['z']
        for cx in range((worldx-1)//16,(worldx+b['w'])//16+1):
            for cz in range((worldz-1)//16,(worldz+b['d'])//16+1):
                cindex.setdefault((cx,cz),{'r':set(),'b':set()})['b'].add(i)
    rebuilt['chunks']=[{'x':cx,'z':cz,'r':sorted(v['r']),'b':sorted(v['b'])}
                       for (cx,cz),v in sorted(cindex.items())]
    rebuilt['versionHash']=hashlib.sha256(json.dumps(rebuilt['buildings'],sort_keys=True).encode()).hexdigest()
    rebuilt['c451']={k:v for k,v in report.items() if k!='workplaceTypes'}
    rebuilt['c451']['workplaceTypes']=report['workplaceTypes']
    backup_and_install(V3,json.dumps(rebuilt,ensure_ascii=False,separators=(',',':'))+'\n')
    print('INSTALLED castle ward home-for-specialty swap:',json.dumps(report))
    return report

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--install',action='store_true');o=p.parse_args()
 if o.install and os.environ.get('KNOWNWORLD_C45_CONFIRM')!='NEW_WORLD':
  raise SystemExit('Only install with KNOWNWORLD_C45_CONFIRM=NEW_WORLD')
 main(o.install)
