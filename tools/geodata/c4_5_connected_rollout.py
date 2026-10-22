#!/usr/bin/env python3
"""C4.5 generated JOBS + unbounded-demand SMALL/MEDIUM building plans + Winterfell roster.

Run from repository checkout; default READ-ONLY. --install applies resource changes
with backups. Never touches saves. Requires C4.3 architecture extension installed.
The only case-study runtime job importer currently implemented is Winterfell V3.
Other site rosters are intentionally NOT fabricated by this worldgen tool.
"""
from __future__ import annotations
import argparse, collections, copy, csv, hashlib, json, math, os, re, shutil, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
CASE=ROOT/'src/main/resources/data/knownworld/case_study'
OUT=ROOT/'tools/geodata/output/c4_5_connected_rollout'
C1=SC/'settlement_presets_c1_v1.json'
C3=SC/'architecture_templates_c3_v1.json'
C4=SC/'settlement_construction_c4_v1.json'
V2=CASE/'winterfell_v2.json'
V3=CASE/'winterfell_v3_worldgen.json'
DIST=SC/'settlement_job_distributions_c4_4_v2.json'
CAT=SC/'occupation_building_catalog_c4_4_v2.json'
RESTRICTED={'DOCK','FISHERY','MINE','SHIPYARD','FERRY_LANDING','CANAL_WORKS','FIGHTING_PIT','ARCANE_STUDY','BROTHEL'}
RURAL={'FARM','GARDEN','PASTURE','FOREST_CAMP'}
NO_GENERATION={'KEEP','MONUMENT','RUIN','CAMP','TEMPORARY_CAMP','RITUAL_GROUND'}


def read(p):
    if not p.is_file(): raise FileNotFoundError(str(p))
    return json.loads(p.read_text(encoding='utf-8'))

def stable(s): return int(hashlib.sha256(s.encode('utf-8')).hexdigest()[:16],16)

def slug(s):return re.sub(r'[^a-z0-9]+','_',s.lower()).strip('_')

def qceil(n,d):return (n+d-1)//d if n>0 else 0

def apportion(total, counts):
    weights={k:int(v) for k,v in counts.items() if v>0}
    if total<0 or not weights and total:raise ValueError('Cannot apportion without workers')
    if total==0:return {}
    denom=sum(weights.values()); q={k:total*n//denom for k,n in weights.items()}
    missing=total-sum(q.values())
    order=sorted(weights,key=lambda k:(-(total*weights[k]%denom),k))
    for k in order[:missing]:q[k]+=1
    assert sum(q.values())==total
    return {k:n for k,n in q.items() if n}

def backup_and_install(dest,content):
    dest.parent.mkdir(parents=True,exist_ok=True)
    saved=dest.with_name(dest.name+'.before_c4_5.bak')
    if dest.exists() and not saved.exists():shutil.copy2(dest,saved)
    tmp=dest.with_name(dest.name+'.c4_5.tmp')
    tmp.write_text(content,encoding='utf-8')
    tmp.replace(dest)
    print('INSTALLED',dest.relative_to(ROOT),'backup',saved.relative_to(ROOT) if saved.exists() else '(new)')


def append_lot(lots,sid,kind,x,z,w,d,face,capacity,staff,used_rects):
    w,d=int(w),int(d)
    if min(w,d)<5 or max(w,d)>64:raise ValueError(f'{kind}: footprint {w}x{d} exceeds guarded 5..64')
    rect=(x,z,x+w,z+d)
    for old in used_rects:
        if rect[0]<old[2] and old[0]<rect[2] and rect[1]<old[3] and old[1]<rect[3]:
            raise ValueError(f'{sid}: collision {kind} at {rect} with {old}')
    used_rects.append(rect)
    lot={'id':f'{sid}_c45_{kind.lower()}_{len(lots):06d}', 'kind':kind,
         'x':int(x),'z':int(z),'width':w,'depth':d,'face':face,
         'capacity':int(capacity),'staff':int(staff),'householdId':None,
         'plannedOnlyHouseholdName':kind=='HOUSE'}
    lots.append(lot);return lot


def line(roads,x1,z1,x2,z2,width=3):
    if x1!=x2 and z1!=z2:raise ValueError('Road must be orthogonal')
    half=width//2
    if x1==x2:
        for z in range(min(z1,z2),max(z1,z2)+1):
            for x in range(x1-half,x1+half+1):roads.add((x,z))
    else:
        for x in range(min(x1,x2),max(x1,x2)+1):
            for z in range(z1-half,z1+half+1):roads.add((x,z))


def indexed(site, lots, roads):
    """Chunk index without materializing every field and building footprint pixel.

    The original set-of-every-occupied-square algorithm could require hundreds
    of MB for a single agricultural settlement.  Test each road pixel against
    rectangle rows via a coarse grid and index buildings by intersected chunks.
    """
    buckets=collections.defaultdict(list)
    for l in lots:
        x0,x1=l['x'],l['x']+l['width']
        z0,z1=l['z'],l['z']+l['depth']
        for cx in range(x0//64,(x1-1)//64+1):
            for cz in range(z0//64,(z1-1)//64+1):
                buckets[(cx,cz)].append((x0,x1,z0,z1,l['id']))
    for x,z in roads:
        for x0,x1,z0,z1,n in buckets.get((x//64,z//64),()):
            if x0<=x<x1 and z0<=z<z1:
                raise ValueError(f'{site}: road overlaps lot {n} at {(x,z)}')
    chunks=collections.defaultdict(lambda:{'r':set(),'b':set()})
    for x,z in roads:
        cx,cz=x//16,z//16
        chunks[(cx,cz)]['r'].add((z-cz*16)*16+(x-cx*16))
    for i,l in enumerate(lots):
        for cx in range((l['x']-1)//16,(l['x']+l['width'])//16+1):
            for cz in range((l['z']-1)//16,(l['z']+l['depth'])//16+1):
                chunks[(cx,cz)]['b'].add(i)
    entries=[{'cx':cx,'cz':cz,'r':sorted(v['r']),'b':sorted(v['b'])}
             for (cx,cz),v in sorted(chunks.items())]
    minx=min(min(l['x'] for l in lots),min(x for x,_ in roads))-40
    maxx=max(max(l['x']+l['width'] for l in lots),max(x for x,_ in roads))+40
    minz=min(min(l['z'] for l in lots),min(z for _,z in roads))-40
    maxz=max(max(l['z']+l['depth'] for l in lots),max(z for _,z in roads))+40
    return entries,[minx,maxx,minz,maxz]


def palette(site):
    return site.get('paletteId') or {'NW':'NORTHERN','SW':'WESTEROSI_INTERIOR','NE':'DOTHRAKI_STEPPE',
           'NEE':'YITI','NWE':'ESSOS_FREE_CITY','SWE':'ESSOS_FREE_CITY',
           'SI':'SUMMER_ISLES','SO':'SOTHORYOS','SE':'GHISCARI','SEE':'YITI'}.get(site.get('zone'),'NORTHERN')


def build_site(site, occupation, templates, source):
    sid=site['siteId']; tier=site['tier']; a=site['workingAnchor'];ax,az=int(a['x']),int(a['z'])
    if tier not in ('SMALL','MEDIUM') or site['blueprintMode']!='GENERAL_COMPACT_SETTLEMENT':
        raise ValueError('Unsupported generic archetype '+sid)
    if sid=='site_nw_029' or 'NW-029' in site['markerIds']:raise ValueError('Winterfell V3 protected')
    c1=site['buildingDemand'];pop=int(c1['peopleTarget']);workers=int(c1['workforceTarget'])
    if pop<=0 or occupation['originalC1Population']!=pop or occupation['originalC1WorkforceEstimate']!=workers:
        raise ValueError('Population/worker disagreement '+sid)
    houses=int(c1['housingBuildingsDemand'])
    # Never manufacture NPCs: planned houses are unoccupied until B4 identity creation.
    counts=collections.Counter();skipped={}
    for kind,spec in occupation['buildingDemand'].items():
        n=int(spec['workplacesRequested'])
        if n<=0:continue
        t=templates.get(kind)
        if not t or kind in RESTRICTED or kind in NO_GENERATION or t.get('requiresShore') or t.get('requiresMountainResource') or t.get('requiresPolicy'):
            skipped[kind]={'requested':n,'reason':'special terrain/policy or template pending'};continue
        counts[kind]=n
    # Civic structures from original C1 planning demand (not fake employees).
    for k in ('WELL','GRANARY','WAREHOUSE','GUARD_TOWER','GREAT_HALL','COURTYARD'):
        n=int(c1['workplaceCounts'].get(k,0))
        if n>0 and k in templates: counts[k]=max(counts[k],n)
    rural=[]
    for k in sorted(RURAL):rural.extend([k]*counts.pop(k,0))
    urban=[]
    for kind,n in sorted(counts.items()):urban.extend([kind]*n)
    nslots=houses+len(urban)
    lanes=max(3,int(math.ceil(math.sqrt(nslots/8))))
    columns=max(3,qceil(nslots,2*lanes))
    pitchx,pitchz=68,160
    left=ax-(columns-1)*pitchx//2
    zs=[az+(j-(lanes-1)//2)*pitchz for j in range(lanes)]
    slots=[]
    for j,rz in enumerate(zs):
        for i in range(columns):
            cx=left+i*pitchx
            for face in ('NORTH','SOUTH'):slots.append((j,i,cx,rz,face))
    # Interleaved housing and specialized works; civic buildings take inner slots.
    slots.sort(key=lambda s:(abs(s[0]-(lanes-1)/2)+abs(s[1]-(columns-1)/2)*.5,stable(sid+str(s))%1024))
    civic={'ADMINISTRATION','GREAT_HALL','COURTYARD','TEMPLE','JUSTICE_HALL','SCHOLASTIC_HALL','BARRACKS'}
    urban.sort(key=lambda k:(k not in civic,stable(sid+k)%100000,k))
    types=[];left_h=houses;left_urban=len(urban);idx=0
    while left_h or left_urban:
        # Place civic buildings early; otherwise distribute evenly among homes.
        if left_urban and (idx<min(6,left_urban) or left_h==0 or idx*len(urban)//max(1,nslots) > len(urban)-left_urban):
            types.append(urban[len(urban)-left_urban]);left_urban-=1
        elif left_h:types.append('HOUSE');left_h-=1
        else:types.append(urban[len(urban)-left_urban]);left_urban-=1
        idx+=1
    lots=[];rects=[];roads=set();mincol=left-36;maxcol=left+(columns-1)*pitchx+36
    for rz in zs:line(roads,mincol,rz,maxcol,rz,5)
    for x in (mincol,maxcol):line(roads,x,min(zs),x,max(zs),3)
    for (_,_,cx,rz,face),kind in zip(slots,types):
        t=templates['HOUSE_MEDIUM' if tier=='MEDIUM' else 'HOUSE_SMALL'] if kind=='HOUSE' else templates[kind]
        w,d=int(t['c1Width']),int(t['c1Depth'])
        z=rz+12 if face=='NORTH' else rz-12-d
        l=append_lot(lots,sid,kind,cx-w//2,z,w,d,face,10 if kind=='HOUSE' and tier=='MEDIUM' else 5 if kind=='HOUSE' else 0,0 if kind=='HOUSE' else t['maxStaff'],rects)
        doorz=z-1 if face=='NORTH' else z+d
        line(roads,cx,doorz,cx,rz,3)
    # Agricultural district. No farmhouse/field cap; row-based blocks avoid 50km linear belts.
    rcols=10;fx_pitch=170;fz_pitch=260
    rleft=ax-(rcols-1)*fx_pitch//2;first_road=min(zs)-100
    rural_spine=rleft+(rcols-1)*fx_pitch+120
    if rural:
        line(roads,maxcol,min(zs),maxcol,first_road,3)
        line(roads,rleft-90,first_road,rural_spine,first_road,5)
    for index,kind in enumerate(rural):
        row,col=divmod(index,rcols); cx=rleft+col*fx_pitch;rz=first_road-row*fz_pitch
        if col==0:
            line(roads,rleft-90,rz,rural_spine,rz,5)
            if row:line(roads,rural_spine,rz,rural_spine,rz+fz_pitch,3)
        t=templates[kind];w,d=int(t['c1Width']),int(t['c1Depth'])
        if kind=='GARDEN':w,d=max(21,w),max(21,d)
        if kind=='PASTURE':w,d=max(31,w),max(31,d)
        z=rz-13-d
        append_lot(lots,sid,kind,cx-w//2,z,w,d,'SOUTH',0,t['maxStaff'],rects)
        line(roads,cx,z+d,cx,rz,3)
        if kind=='FARM':
            # Six 64x64 blocks: 24,576 m² visually cultivated per farmhouse.
            # More than the previous C4.3 24 x 31x31 (~23,064m²) pilot,
            # but a visual annex, NOT authoritative soil/yield/acreage.
            # Fits within each 170x260 rural grid slot without overlapping
            # other farms, planted plots, streets or farm tracks.
            for fr in range(3):
                for fc in range(2):
                    append_lot(lots,sid,'FIELD',cx-67+fc*70,
                               rz-96-fr*75,64,64,'NORTH',0,0,rects)
    chunks,bounds=indexed(sid,lots,roads)
    return {'siteId':sid,'name':site['name'],'tier':tier,'anchor':{'x':ax,'z':az},
      'markerIds':list(site['markerIds']),'palette':palette(site),'mode':'GENERAL_COMPACT_SETTLEMENT',
      'bounds':bounds,'lots':lots,'chunks':chunks,'housingBacklogAtLeast':0,
      'c4':{'phase':'C4_5_C4_4_JOB_DEMAND_FIRST_FRESH_CHUNKS','homes':houses,
        'plannedHousingCapacity':sum(l['capacity'] for l in lots if l['kind']=='HOUSE'),
        'plannedPopulation':pop,'plannedWorkers':workers,
        'plannedStructures':len(lots),'fieldAnnexes':sum(l['kind']=='FIELD' for l in lots),
        'buildings':dict(collections.Counter(l['kind'] for l in lots)),'deferredDemand':skipped,
        'actualNpcsCreated':0,'actualWorkersAssigned':0,'requiresTerrainSurvey':True,
        'newWorldOnly':True}}


def winterfell_roster(v2,plan,cat):
    people=v2['residents']; assert len(people)==5000 and len(v2['households'])==1136
    names={x['jobId']:x for x in cat['rows'] if not x['isDuplicate']}
    candidates=[p for p in people if p.get('status')=='GENERATED_MOD_CALIBRATION'
                and 17<=int(p.get('age',298-int(p['birthYear'])))<=64
                and p['job'] not in ('CHILD','DEPENDENT','WARD','LORD','LADY')]
    already=[p for p in candidates if p.get('occupationPlanVersion')==45]
    if already:
        if len(already)!=len(candidates):
            raise ValueError('Partial Winterfell C4.5 job install found; refusing mixed replay')
        return {'residents':5000,'generatedWorkersReassigned':len(candidates),
            'C4_4WorkersEstimate':plan['originalC1WorkforceEstimate'],
            'populationPlanningWorkerMismatch':plan['originalC1WorkforceEstimate']-len(candidates),
            'authoredAndNamedChanged':0,'jobCount':len({p['occupationDisplayC45'] for p in already}),
            'alreadyInstalled':True,'note':'C4.5 roster occupations already installed; replay preserved'}
    counts=apportion(len(candidates),plan['jobWorkerCounts'])
    # Build site affinity: farming roles preferentially placed at rural hamlets,
    # governance/security at the castle; every person keeps the SAME home/site/ID.
    rural={'FARM','PASTURE','GARDEN','FOREST_CAMP','MINE'}
    def priority(p,job):
        building=names[job]['building'];site=p['site']
        if site.startswith('hamlet_'):return 0 if building in rural else 2
        if site=='castle':return 0 if building in ('ADMINISTRATION','BARRACKS','ARMORY','TEMPLE','HEALER','JUSTICE_HALL') else 2
        if site.startswith('village5_'):return 0 if building in rural else 1
        return 0 if building not in rural else 1
    # Largest employment classes assigned first; sorted candidate scoring with stable tiebreak.
    free={p['id']:p for p in candidates}
    result=collections.Counter()
    for job in sorted(counts,key=lambda j:(-counts[j],j)):
        ranked=sorted(free.values(),key=lambda p:(priority(p,job),stable('c45:'+job+':'+p['id'])))
        for p in ranked[:counts[job]]:
            p['legacyJobC45']=p['job']
            p['job']=slug(job).upper()
            p['occupationDisplayC45']=job
            p['occupationBuildingC45']=names[job]['building']
            p['occupationPlanVersion']=45
            free.pop(p['id']);result[job]+=1
    assert not free and sum(result.values())==len(candidates)
    return {'residents':5000,'generatedWorkersReassigned':len(candidates),
       'C4_4WorkersEstimate':plan['originalC1WorkforceEstimate'],
       'populationPlanningWorkerMismatch':plan['originalC1WorkforceEstimate']-len(candidates),
       'authoredAndNamedChanged':0,'jobCount':len(result),'roleTargets':dict(sorted(result.items())),
       'note':'Fresh-world initial generated profiles only; existing save jobs are not overwritten.'}


def build_all(scope,install):
    catalog=read(CAT);dist=read(DIST)
    plan_by={p['siteId']:p for p in dist['plans']}
    if len(plan_by)!=248:raise ValueError('C4.4 source must contain exactly 248 distinct physical sites')
    c1=read(C1)
    site_by={p['siteId']:p for p in c1['physicalSitePresets']}
    if len(site_by)!=248:raise ValueError('C1 must contain 248 unique sites')
    source=read(C4)
    templates={t['id']:t for t in read(C3)['templates']}
    required=set(x['building'] for x in catalog['rows'] if x['building'] not in ('NONE_PRIVATE',))
    missing=sorted(required-set(templates))
    if missing:print('Notice: catalogue has unimplemented physical templates:',missing)
    if len(templates)<61:raise ValueError('C4.3 61-template architecture has not been installed yet')
    selected=['site_nw_031','site_sw_023'] if scope=='pilots' else [sid for sid,s in sorted(site_by.items())
                if s['tier'] in ('SMALL','MEDIUM') and s['blueprintMode']=='GENERAL_COMPACT_SETTLEMENT']
    OUT.mkdir(parents=True,exist_ok=True)
    summary=[]
    preview=OUT/('C4_5_'+scope+'_preview.json')
    with preview.open('w',encoding='utf-8') as stream:
        # Stream the worldgen index directly; never keep 100+ regional
        # settlement geometries in the same Python heap.
        header={'schemaVersion':1,'purpose':'C4_5_NEW_CHUNK_GENERATION_C4_4_DISTRIBUTIONS',
          'policy':{'pilotsOnly':True,'doNotModifyExistingChunks':True,
                    'noNpcCreation':True,'noOther253SiteActivation':False,
                    'winterfellExcluded':True},
          'summary':{'allMapMarkers':253,'allPoliticalHoldings':259,
                     'pilotCount':len(selected),
                     'pilotMarkers':[site_by[s]['markerIds'] for s in selected]}}
        prefix=json.dumps(header,separators=(',',':'),ensure_ascii=False)
        stream.write(prefix[:-1]+',"pilots":[')
        for idx,sid in enumerate(selected,1):
            obj=build_site(site_by[sid],plan_by[sid],templates,source)
            if idx>1:stream.write(',')
            json.dump(obj,stream,ensure_ascii=False,separators=(',',':'))
            summary.append({'siteId':sid,'name':obj['name'],
                'population':obj['c4']['plannedPopulation'],
                'workers':obj['c4']['plannedWorkers'],'houses':obj['c4']['homes'],
                'buildings':obj['c4']['plannedStructures'],
                'indexedChunks':len(obj['chunks']),
                'deferredDemands':obj['c4']['deferredDemand']})
            print(f'[{idx}/{len(selected)}] {obj["name"]}: {obj["c4"]["homes"]} houses, '
                  f'{obj["c4"]["plannedStructures"]} lots, {len(obj["chunks"])} chunks',flush=True)
            del obj
        stream.write('],"c4Policy":'+json.dumps({
           'siteSelection':scope,'newWorldOnly':True,
           'requiresPerPlotTerrainSurvey':True,'readC4_4JobDemands':True,
           'unreviewedExceptionalSitesExcluded':True,'noNPCsMaterializedHere':True},
           separators=(',',':'))+'}\n')
    report=OUT/('C4_5_'+scope+'_report.json')
    report.write_text(json.dumps({'sites':summary,
        'totalBuildings':sum(x['buildings'] for x in summary),
        'totalHomes':sum(x['houses'] for x in summary),
        'totalIndexedChunks':sum(x['indexedChunks'] for x in summary),
        'unapprovedTerrainWarning':True},ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('Generated',preview,'size(MB)=',round(preview.stat().st_size/1048576,2))
    if install:
        saved=C4.with_name(C4.name+'.before_c4_5.bak')
        if C4.exists() and not saved.exists():shutil.copy2(C4,saved)
        staging=C4.with_name(C4.name+'.c4_5.tmp')
        shutil.copyfile(preview,staging)
        staging.replace(C4)
        print('INSTALLED C4.5 worldgen manifest with backup',saved)
    return summary


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--scope',choices=['pilots','all-small-medium'],default='pilots')
    parser.add_argument('--install',action='store_true',help='back up and overwrite NEW WORLD resources')
    parser.add_argument('--winterfell',action='store_true',help='reassign only generated adults in 5000-person source roster; recompile V3 using existing generator')
    parser.add_argument('--only-winterfell',action='store_true',help='Do not regenerate the SMALL/MEDIUM worldgen manifest')
    args=parser.parse_args()
    if args.install and os.environ.get('KNOWNWORLD_C45_CONFIRM')!='NEW_WORLD':
        raise SystemExit('For installation set KNOWNWORLD_C45_CONFIRM=NEW_WORLD. Back up your saves and use a FRESH world.')
    dist=read(DIST)
    if args.winterfell:
        v2=read(V2)
        jobs=next(p for p in dist['plans'] if p['siteId']=='site_nw_029')
        report=winterfell_roster(v2,jobs,read(CAT))
        OUT.mkdir(parents=True,exist_ok=True)
        (OUT/'winterfell_job_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
        (OUT/'winterfell_v2_c45_preview.json').write_text(json.dumps(v2,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf8')
        print(json.dumps(report,indent=2))
        if args.install:
            backup_and_install(V2,json.dumps(v2,separators=(',',':'),ensure_ascii=False)+'\n')
            # C4.5.1 ward relocations are an installed architectural overlay.
            # NEVER erase them on a second installer run after a failed Gradle
            # check. Only rebuild the original index BEFORE the ward step.
            if v2.get('c451'):
                if not read(V3).get('c451'):
                    raise RuntimeError('Winterfell housing relocation ledger exists but V3 index missing; recover from backups')
                print('Preserving existing castle-ward relocation and V3 specialty structures on retry')
            else:
                import subprocess
                run=subprocess.run([sys.executable,str(ROOT/'tools/geodata/build_winterfell_v3_worldgen.py')],cwd=ROOT)
                if run.returncode:raise RuntimeError('Winterfell V3 structure index recompile failed')
                print('Winterfell V3 base structures compiled; relocation and specialty stages follow.')
    if not args.only_winterfell:
        build_all(args.scope,args.install)

if __name__=='__main__':main()
