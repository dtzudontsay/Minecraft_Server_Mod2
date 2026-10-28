#!/usr/bin/env python3
"""C4.15: spatially reconcile existing B4.0 workers with C4.11 workplace lots.

No occupation/identity/census changes. Exact capacity accounting per workspace kind.
A 600-block Manhattan metric is only a planned-road distance proxy, not a
verified navigation path or ten-minute promise. Explicit failures are audited.
"""
from __future__ import annotations
import argparse, collections, gzip, json, math, pathlib, shutil, tempfile

ROOT=pathlib.Path(__file__).resolve().parents[2]
SC='src/main/resources/data/knownworld/scenarios/agot_298_ac'
C4=pathlib.Path(SC)/'universal_construction_c4_11_global_v1.json'
TILES=pathlib.Path(SC)/'c4_11_global_tiles'
B4=pathlib.Path(SC)/'world_population_b4_v1'
JOBS=pathlib.Path(SC)/'occupation_building_catalog_c4_4_v2.json'
OUT=pathlib.Path(SC)/'world_commutes_c4_15_v1'
LIMIT=600
BUCKET=120
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}


def read(path):return json.loads(pathlib.Path(path).read_text(encoding='utf-8-sig'))


def lots_for_site(repo,meta):
    seen={}
    for key in meta.get('tiles',[]):
        path=repo/TILES/meta['siteId']/(key+'.json')
        for lot in read(path)['lots']:
            lid=lot['id']
            old=seen.setdefault(lid,lot)
            if old!=lot:raise ValueError('Conflicting lot '+lid)
    return seen


def eligible(meta):
    return meta['siteId'] not in PROTECTED and meta['status'] in {
        'PROVISIONAL_SPECIAL_GEOMETRY','GLOBAL_TERRAIN_SURVEY_PENDING'}


def worker_kind(person,mapper):
    job=person.get('occupationId')
    return mapper.get(job) if job else None


def cell(x,z):return x//BUCKET,z//BUCKET


def allocate_worker(home,kind,by_type,cap,limit=LIMIT):
    if kind=='KEEP':return None,None,'CORE_SPECIAL_WORKPLACE'
    grid=by_type.get(kind)
    if not grid:return None,None,'NO_CONSTRUCTIBLE_TEMPLATE'
    x,z=home;cx,cz=cell(x,z)
    best=None
    for ring in range(0,math.ceil(limit/BUCKET)+2):
        for tx in range(cx-ring,cx+ring+1):
            for tz in range(cz-ring,cz+ring+1):
                if ring and max(abs(tx-cx),abs(tz-cz))!=ring:continue
                for obj in grid.get((tx,tz),()):
                    wx,wz,lid=obj
                    if cap[lid]<=0:continue
                    distance=abs(wx-x)+abs(wz-z)
                    if distance<=limit and (best is None or distance<best[0] or
                            (distance==best[0] and lid<best[1])):
                        best=(distance,lid)
        if best is not None and ring>=1:break
    if best is None:return None,None,'NO_CAPACITY_WITHIN_PROXY_LIMIT'
    cap[best[1]]-=1
    return best[1],best[0],'PROVISIONAL_NEARBY_NOT_PATH_VERIFIED'


def prepare(lots):
    homes={};workplaces={};by_type=collections.defaultdict(lambda:collections.defaultdict(list));cap={}
    for lot in lots.values():
        if lot['permission']!='C3_MODULE_AVAILABLE':continue
        x,z,w,d=(int(lot[k]) for k in ('x','z','w','d'))
        if lot['kind']=='HOUSE':homes[lot['id']]=(x+w//2,z+d+2)
        else:
            key=lot['id'];n=int(lot['s'])
            if n>0:
                workplaces[key]=lot
                cap[key]=n
                by_type[lot['kind']][cell(x+w//2,z+d//2)].append((x+w//2,z+d//2,key))
    for grid in by_type.values():
        for values in grid.values():values.sort(key=lambda x:x[2])
    return homes,cap,by_type


def match_site(repo,site,mapper,out,limit=LIMIT):
    sid=site['siteId'];lots=lots_for_site(repo,site)
    homes,cap,by_type=prepare(lots)
    counts=collections.Counter();examples=[]
    source=repo/B4/(sid+'.jsonl.gz')
    if not source.is_file():raise FileNotFoundError(str(source))
    with gzip.open(source,'rt',encoding='utf8') as people, gzip.open(out,'wt',encoding='utf8',compresslevel=5) as assigned:
        for line in people:
            p=json.loads(line)
            kind=worker_kind(p,mapper)
            if kind is None:continue
            counts['workersSeen']+=1
            base=homes.get(p.get('homeLotId'))
            if base is None:
                wid=None;distance=None;state='HOME_TEMPLATE_UNAVAILABLE'
            else:
                wid,distance,state=allocate_worker(base,kind,by_type,cap,limit)
            if state=='PROVISIONAL_NEARBY_NOT_PATH_VERIFIED':counts['provisionalMatches']+=1
            else:
                counts['unverifiedCommuters']+=1;counts[state]+=1
                if len(examples)<15:examples.append({'personId':p['personId'],'job':p['occupationId'],'reason':state})
            assigned.write(json.dumps({'personId':p['personId'],'workplaceId':wid,
                'commuteDistanceProxyBlocks':distance,'commuteStatus':state},separators=(',',':'))+'\n')
    # Some slots are for internally unallocated KEEP work; they are not occupations lost.
    counts['unfilledProvisionalBuildingSlots']=sum(cap.values())
    return {'siteId':sid,'workersSeen':counts['workersSeen'],'matched':counts['provisionalMatches'],
            'unverified':counts['unverifiedCommuters'],'unfilledSlots':counts['unfilledProvisionalBuildingSlots'],
            'reasons':{k:v for k,v in counts.items() if k not in {'workersSeen','provisionalMatches','unverifiedCommuters','unfilledProvisionalBuildingSlots'}},
            'examples':examples}


def run(repo=ROOT,site_filter=None,limit=LIMIT):
    repo=pathlib.Path(repo).resolve()
    index=read(repo/C4);mapper={x['jobId']:x.get('building') for x in read(repo/JOBS)['rows'] if not x.get('isDuplicate')}
    if len(mapper)!=204:raise ValueError('204-job catalog altered')
    if index['siteCount']!=248:raise ValueError('Expected 248 sites')
    out=repo/OUT;stage=out.with_name(out.name+'.incomplete')
    if stage.exists():shutil.rmtree(stage)
    stage.mkdir(parents=True)
    report=[];totals=collections.Counter()
    try:
        for i,site in enumerate(index['sites'],1):
            if not eligible(site) or (site_filter is not None and site['siteId'] not in site_filter):continue
            dest=stage/(site['siteId']+'.jsonl.gz')
            result=match_site(repo,site,mapper,dest,limit)
            report.append(result)
            totals['workersSeen']+=result['workersSeen'];totals['matched']+=result['matched'];totals['unverified']+=result['unverified']
            if i%20==0:print(f'commute audit {i}/248: {totals["workersSeen"]} workers',flush=True)
        summary={'version':1,'maximumProxyBlocks':limit,'kind':'NEARBY_JOB_RECONCILIATION_NOT_PHYSICAL_PATHFINDING',
                 'baseB40Unmodified':True,'employmentTotalsPreserved':True,
                 'total':dict(totals),'sites':report,
                 'warning':'UNVERIFIED entries retain initial occupation but have no confirmed ten-minute workplace. Provisional matches are not verified walkable routes.'}
        (stage/'audit.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        backup=repo/'tools/geodata/output/c4_15_previous_commutes'
        if backup.exists():shutil.rmtree(backup)
        if out.exists():shutil.move(str(out),str(backup))
        try:stage.rename(out)
        except Exception:
            if backup.exists():shutil.move(str(backup),str(out))
            raise
    except Exception:
        shutil.rmtree(stage,ignore_errors=True)
        raise
    return summary


def main():
    p=argparse.ArgumentParser();p.add_argument('--site',action='append');p.add_argument('--max-proxy-blocks',type=int,default=LIMIT)
    args=p.parse_args();result=run(ROOT,set(args.site) if args.site else None,args.max_proxy_blocks)
    print(json.dumps(result['total'],indent=2))
    print('NO CLAIM OF 10-MINUTE NAVIGABLE COMMUTES; see world_commutes_c4_15_v1/audit.json')
if __name__=='__main__':main()
