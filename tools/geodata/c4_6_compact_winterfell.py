#!/usr/bin/env python3
"""C4.6: compact existing Winterfell V2/V3 structures without new people.

Input: the already-installed C4.5.1 V2 roster and V3 physical index.
Keeps every resident and household ID, the castle keep, gates, walls, existing
castle interior plots, and all original worksite identities. Moves existing
outlying plots into a tight square-ring street grid near the keep. Farms go
at the outer edge; a first visual band of field plots is added.
No direct world edits. --install requires explicit C46_NEW_WORLD environment.
"""
from __future__ import annotations
import argparse, collections, copy, csv, hashlib, json, math, os, shutil, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
CASE=ROOT/'src/main/resources/data/knownworld/case_study'
V2=CASE/'winterfell_v2.json';V3=CASE/'winterfell_v3_worldgen.json'
OUT=ROOT/'tools/geodata/output/c4_6_winterfell'
AX,AZ=-4050173,-1976496
CELL=36
FIELD_SIZE=64
FIELD_COUNT=24  # 24 x 4096m2 physical review strip, not economic productive land

def load(path):return json.loads(path.read_text(encoding='utf-8'))
def rect(b):return (b['x'],b['z'],b['x']+b['w'],b['z']+b['d'])
def intersect(a,b,pad=0):return a[0]<b[2]+pad and b[0]<a[2]+pad and a[1]<b[3]+pad and b[1]<a[3]+pad

def cells():
    for r in range(7,36):
        ring=[(i,j) for i in range(-r,r+1) for j in range(-r,r+1) if max(abs(i),abs(j))==r]
        # Walk the ring in changing compass order, rather than one solid residential quadrant.
        ring.sort(key=lambda p: ((math.atan2(p[1],p[0])+math.pi*2)%(math.pi*2),p[0],p[1]))
        shift=(r*17)%len(ring)
        yield from ring[shift:]+ring[:shift]

def is_fixed(b):
    # Keep every physical object *inside* the existing castle, including 12
    # home-for-workplace swaps. Move the 24 already-relocated houses too.
    return b.get('site')=='castle' and -150<=b['x'] and b['x']+b['w']<=150 and -150<=b['z'] and b['z']+b['d']<=161

def agriculture(b):
    return (b.get('t') in ('FARMHOUSE','C3:FARM','C3:PASTURE','C3:FOREST_CAMP','C3:GARDEN')
            or b.get('template')=='FARMHOUSE' or b.get('role') in ('FARMER','LIVESTOCK','FORESTER'))

def deterministic_mix(homes,other):
    # Interleave normal homes and specialist workplaces, not segregated rows.
    homes=sorted(homes,key=lambda b:hashlib.sha256(b['id'].encode()).hexdigest())
    other=sorted(other,key=lambda b:hashlib.sha256(b['id'].encode()).hexdigest())
    if not homes:return other
    if not other:return homes
    total=len(homes)+len(other);out=[];ih=io=0
    for slot in range(total):
        desired_h=round((slot+1)*len(homes)/total)
        if ih<desired_h and ih<len(homes):out.append(homes[ih]);ih+=1
        else:
            if io<len(other):out.append(other[io]);io+=1
            else:out.append(homes[ih]);ih+=1
    return out

def pack(v2,v3,with_fields=True):
    if v3.get('c46') or v2.get('c46'):
        raise ValueError('C4.6 already installed. Do not run it twice or duplicate farms.')
    if len(v2['residents'])!=5000 or len(v2['homes'])!=1136 or len(v2['households'])!=1136:
        raise ValueError('Winterfell identity/household invariant failed')
    if len(v3['buildings'])<1514 or not v3.get('c45') or not v3.get('c451'):
        raise ValueError('Expected C4.5.1 installed Winterfell source; do not rerun earlier installers')
    original_ids=[p['id'] for p in v2['residents']]
    assert len(original_ids)==len(set(original_ids))==5000
    original_home={p['id']:(p['homeId'],p['householdId'],p.get('job')) for p in v2['residents']}
    v2,v3=copy.deepcopy(v2),copy.deepcopy(v3)
    existing=v3['buildings'];fixed=[b for b in existing if is_fixed(b)]
    moving=[b for b in existing if not is_fixed(b)]
    homes=[b for b in moving if b['id'] in {h['id'] for h in v2['homes']}]
    fields_preexisting=[b for b in moving if b['id'].startswith('wf3_c46_field_')]
    if fields_preexisting:raise ValueError('Already has C4.6 field lots')
    nonfarm=[b for b in moving if not agriculture(b)]
    farm=[b for b in moving if agriculture(b)]
    residential=[b for b in nonfarm if b in homes]
    specials=[b for b in nonfarm if b not in homes]
    ordering=deterministic_mix(residential,specials)+sorted(farm,key=lambda b:hashlib.sha256(b['id'].encode()).hexdigest())
    if len(ordering)!=len(moving):raise ValueError('Lost or repeated moving lot')
    it=cells();used=set();moved={};max_ring=0
    for b in ordering:
        if b['w']>32 or b['d']>32:
            raise ValueError(f'Building {b["id"]} ({b["w"]}x{b["d"]}) needs an oversized district cell')
        col,row=next(it);max_ring=max(max_ring,abs(col),abs(row))
        if (col,row) in used:raise AssertionError('duplicate grid cell')
        used.add((col,row));old=(b['x'],b['z'])
        b['x']=col*CELL+2;b['z']=row*CELL+2
        moved[b['id']]={'from':list(old),'to':[b['x'],b['z']]}
    # Reconnect existing NPC household-worksite IDs to the relocated physical objects.
    lookup={b['id']:b for b in existing}
    for category in ('homes','workplaces'):
        for b in v2[category]:
            if b['id'] not in lookup:raise ValueError('V2 / V3 plot missing: '+b['id'])
            new=lookup[b['id']]
            b['x'],b['z']=new['x'],new['z']
            if 'accessX' in b:b['accessX']=b['x']+b['w']//2
            if 'accessZ' in b:b['accessZ']=b['z']+b['d']+1
    # Keep the historical social district ids but recompute their representative
    # coordinates from actual home plots. No change to personal residence IDs.
    sitehomes=collections.defaultdict(list)
    for home in v2['homes']:sitehomes[home['site']].append(home)
    for s in v2.get('sites',[]):
        group=sitehomes.get(s['id'],[])
        if group and s['id']!='castle':
            s['cx']=round(sum(x['x']+x['w']/2 for x in group)/len(group))
            s['cz']=round(sum(x['z']+x['d']/2 for x in group)/len(group))
    # First visible field ring outside residential + farm structures. Productive
    # land is NOT inferred from rendered fields. No fields inside castle walls.
    fields=[]
    if with_fields:
        field_radius=(max_ring+5)*CELL+80
        for i in range(FIELD_COUNT):
            ring=i//12;slot=i%12
            angle=2*math.pi*(slot+.43*ring)/12
            radius=field_radius+ring*105
            x=round(math.cos(angle)*radius)-FIELD_SIZE//2
            z=round(math.sin(angle)*radius)-FIELD_SIZE//2
            fields.append({'id':f'wf3_c46_field_{i+1:03}','t':'C3:GARDEN',
                           'label':'Cultivated Fields','x':x,'z':z,'w':FIELD_SIZE,'d':FIELD_SIZE,
                           'cap':0,'site':'winterfell_farm_belt','visualFieldOnly':True})
        existing.extend(fields)
    rects=[(b['id'],rect(b)) for b in existing]
    # Spatial hash: exhaustive pairwise comparisons among ~2k rectangles are quick.
    for i,(ident,a) in enumerate(rects):
        for other,b in rects[i+1:]:
            if intersect(a,b):raise ValueError(f'Overlapping physical lots {ident} and {other}')
    # Source castle road snippets and a square-ring road mesh. Road surfaces
    # are clipped away from any building footprint; existing gate openings stay.
    roads=set((x,z) for x,z in v2['roads'] if -186<=x<=186 and -186<=z<=186)
    lo=-max_ring-2;hi=max_ring+2
    roadline=set()
    for col in range(lo,hi+1):
        rx=col*CELL+34
        for z in range(lo*CELL,hi*CELL+36):
            if max(abs(rx),abs(z))>195:roadline.add((rx,z))
    for row in range(lo,hi+1):
        rz=row*CELL+34
        for x in range(lo*CELL,hi*CELL+36):
            if max(abs(x),abs(rz))>195:roadline.add((x,rz))
    # From existing wall gates to the outer street grid; castle geometry remains fixed.
    for x0,x1,z in ((-172,-(7*CELL+36),15),(172,7*CELL+36,15)):
        for x in range(min(x0,x1),max(x0,x1)+1):roadline.add((x,z))
    for z0,z1,x in ((-172,-(7*CELL+36),158),(172,7*CELL+36,158)):
        for z in range(min(z0,z1),max(z0,z1)+1):roadline.add((x,z))
    # Short front walks to the nearest horizontal service road.
    for b in ordering:
        x=b['x']+b['w']//2
        start=b['z']+b['d'];end=(b['z']//CELL)*CELL+34
        for z in range(start,end+1):roadline.add((x,z))
    # An outer access road rings the compact settlement. Field gates face
    # inward and have direct approaches to this ring; garden rectangles must
    # remain separate from public streets. Supply/harvest transport can use it.
    if fields:
        outer=(max_ring+4)*CELL+34
        for t in range(-outer,outer+1):
            roadline.update(((t,-outer),(t,outer),(-outer,t),(outer,t)))
        # Connect the perimeter road with each of the four city quadrants.
        for t in range((max_ring+2)*CELL+34,outer+1):
            roadline.update(((t,0),(-t,0),(0,t),(0,-t)))
        for b in fields:
            cx=b['x']+b['w']//2;cz=b['z']+b['d']//2
            if abs(cx)>=abs(cz):
                entrance_x=b['x']-3 if cx>0 else b['x']+b['w']+2
                for xx in range(min(entrance_x,outer if cx>0 else -outer),
                                max(entrance_x,outer if cx>0 else -outer)+1):
                    roadline.add((xx,cz))
            else:
                entrance_z=b['z']-3 if cz>0 else b['z']+b['d']+2
                for zz in range(min(entrance_z,outer if cz>0 else -outer),
                                max(entrance_z,outer if cz>0 else -outer)+1):
                    roadline.add((cx,zz))
    # Remove ground-level lanes inside footprints, incl. all original castle rooms.
    occupied=set()
    for b in existing:
        for x in range(b['x'],b['x']+b['w']):
            for z in range(b['z'],b['z']+b['d']):occupied.add((x,z))
    roads.update(roadline-occupied)
    roads.difference_update(occupied)
    v2['roads']=[list(p) for p in sorted(roads)]
    # Chunk index rebuilt from the SAME building objects and current road surface.
    chunks=collections.defaultdict(lambda:{'r':set(),'b':set()})
    for x,z in roads:
        wx,wz=AX+x,AZ+z;cx,cz=wx//16,wz//16
        chunks[(cx,cz)]['r'].add((wz-cz*16)*16+(wx-cx*16))
    for i,b in enumerate(existing):
        x0,z0=AX+b['x'],AZ+b['z']
        for cx in range((x0-2)//16,(x0+b['w']+2)//16+1):
            for cz in range((z0-2)//16,(z0+b['d']+7)//16+1):
                chunks[(cx,cz)]['b'].add(i)
    for cx in range((AX-182)//16,(AX+182)//16+1):
        for cz in range((AZ-182)//16,(AZ+182)//16+1):chunks[(cx,cz)]
    v3['chunks']=[{'x':cx,'z':cz,'r':sorted(q['r']),'b':sorted(q['b'])}
                  for (cx,cz),q in sorted(chunks.items())]
    v3['versionHash']=hashlib.sha256(json.dumps(existing,sort_keys=True,separators=(',',':')).encode()).hexdigest()
    assert original_home=={p['id']:(p['homeId'],p['householdId'],p.get('job')) for p in v2['residents']}
    if len({b['id'] for b in existing})!=len(existing):raise ValueError('Duplicate structure ids')
    if len(v2['homes'])!=1136 or len(v2['residents'])!=5000:raise ValueError('Population changed')
    end_radius=max(max(abs(b['x']),abs(b['z'])) for b in existing)
    result={'version':'C4.6','population':5000,'households':1136,'existingPlotsBeforeFields':len(existing)-len(fields),
            'plotsAfterFields':len(existing),'movedExistingPlots':len(moved),'fixedCastlePlots':len(fixed),
            'visibleFarmFields':len(fields),'maxStructureCoordinateMetresFromCastle':end_radius,
            'streetPoints':len(roads),'chunkIndexes':len(chunks),
            'wallsAndKeep':'UNCHANGED_JAVA_GENERATOR','allNpcIdsAndHouseholdIds':'PRESERVED',
            'productiveFarmAcreage':'NOT_YET_DERIVED_FROM_AGRICULTURAL_CAPACITY'}
    v2['c46']={k:v for k,v in result.items() if k!='chunkIndexes'}
    v3['c46']=result
    return v2,v3,result,moved

def save_atomic(path,data):
    content=json.dumps(data,ensure_ascii=False,separators=(',',':'))+'\n'
    backup=Path(str(path)+'.before_c46.bak')
    if not backup.exists():shutil.copy2(path,backup)
    temp=path.with_name(path.name+'.c46.tmp')
    temp.write_text(content,encoding='utf-8')
    os.replace(temp,path)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--install',action='store_true');a=ap.parse_args()
    if not V2.exists() or not V3.exists():raise SystemExit('Missing V2/V3 local manifests. Extract into Git project root.')
    if a.install and os.environ.get('KNOWNWORLD_C46_CONFIRM')!='NEW_WORLD':raise SystemExit('Set C4.6 confirmation NEW_WORLD')
    v2,v3,report,moved=pack(load(V2),load(V3))
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'C46_WINTERFELL_LAYOUT_REPORT.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    with (OUT/'C46_MOVED_PLOTS.csv').open('w',encoding='utf-8',newline='') as f:
        w=csv.writer(f,delimiter=';');w.writerow(['building_id','old_x','old_z','new_x','new_z'])
        for ident,info in sorted(moved.items()):w.writerow([ident,*info['from'],*info['to']])
    if a.install:
        save_atomic(V2,v2);save_atomic(V3,v3)
        print('INSTALLED fresh-world Winterfell compact rings; V2+V3 backed up')
    else:print('PREFLIGHT ONLY: no scenario manifests changed')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
