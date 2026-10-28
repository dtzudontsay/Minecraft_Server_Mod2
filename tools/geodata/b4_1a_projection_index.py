#!/usr/bin/env python3
"""B4.1A: index one or two actual B4.0 people for each C4.11 house lot.

Offline builder. Never touches saves. Uses C4.11 *approved template* lots, not
arbitrary guesses about building locations; runtime still checks site terrain approval.
"""
from __future__ import annotations
import argparse, gzip, hashlib, json, re, shutil
from pathlib import Path

VERSION='knownworld.b4_1a.physical_projection.v1'
C4='src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json'
B4='src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1'
TILE='src/main/resources/data/knownworld/scenarios/agot_298_ac/c4_11_global_tiles'
OUT='src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a_v1'
ELIGIBLE={'GLOBAL_TERRAIN_SURVEY_PENDING','PROVISIONAL_SPECIAL_GEOMETRY'}
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}
SITE_RE=re.compile(r'^site_[a-z][a-z0-9_]{1,90}$')

def read(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))
def require(condition,message):
    if not condition:raise ValueError(message)

def houses_for_site(tiles_root,site):
    sid=site['siteId'];result={}
    for tile in site['tiles']:
        path=tiles_root/sid/(tile+'.json')
        require(path.is_file(),f'{sid}: missing C4.11 tile {path}')
        data=read(path)
        require(data['siteId']==sid,f'{sid}: cross-site tile')
        for lot in data['lots']:
            if lot['kind']!='HOUSE':continue
            require(lot['permission']=='C3_MODULE_AVAILABLE',f'{sid}: invalid house permission')
            lotid=lot['id']
            coords=(int(lot['x']),int(lot['z']),int(lot['w']),int(lot['d']))
            if lotid in result:require(result[lotid]==coords,f'{sid}: inconsistent duplicate lot {lotid}')
            else:result[lotid]=coords
    return result

def person_projection(p,lot):
    x,z,w,d=lot
    return {'personId':p['personId'],'siteId':p['siteId'],
            'name':p['displayName'],'sex':p['sex'],
            'birthYearAC':int(p['birthYearAC']),
            'householdId':p['householdId'],'homeLotId':p['homeLotId'],
            'occupationId':p.get('occupationId'),'cultureId':p.get('cultureId'),
            'religionId':p.get('religionId'),'workplaceId':p.get('workplaceId'),
            'x':x,'z':z,'w':w,'d':d,
            'provenance':'B4_0_IDENTITY_C4_11_PLANNED_LOT_NOT_CONFIRMED_BUILT'}

def index_people(path,sid,lots,writer,max_per_home=2):
    counts={};individuals=0;homes=set();hashes=set()
    with gzip.open(path,'rt',encoding='utf8') as f:
        for line in f:
            p=json.loads(line)
            require(p['siteId']==sid,f'Cross-site person in {sid}')
            key=p.get('homeLotId')
            if key not in lots:continue
            number=counts.get(key,0)
            if number>=max_per_home:continue
            # A worker in a house might appear after a nonworking household head.
            # Keep an ordinary representative first, plus another person when available.
            ident=p['personId']
            require(ident not in hashes,f'Duplicate projected person {ident}')
            hashes.add(ident)
            writer(person_projection(p,lots[key]));individuals+=1
            counts[key]=number+1;homes.add(key)
    return {'personsIndexed':individuals,'homesRepresented':len(homes),'availableHouses':len(lots)}

def build(repo,out=None):
    repo=Path(repo).resolve();out=Path(out or repo/OUT).resolve()
    manifest=read(repo/B4/'manifest.json');world=read(repo/C4)
    require(manifest['schemaVersion']=='knownworld.b4_0.identity_baseline.v1','Wrong B4.0 schema')
    require(world['schemaVersion']==1 and world['globalEnabled'],'C4.11 not installed')
    require(manifest['siteCount']==world['siteCount']==248,'Site registry mismatch')
    require(manifest['peopleTotal']==3_760_684,'Source population mismatch')
    site_b4={s['siteId']:s for s in manifest['sites']}
    require(len(site_b4)==248,'Duplicate B4.0 sites')
    pending=out.with_name(out.name+'.building')
    if pending.exists():shutil.rmtree(pending)
    pending.mkdir(parents=True)
    summaries=[];total=0;house_total=0
    try:
        for i,site in enumerate(world['sites'],1):
            sid=site['siteId'];require(bool(SITE_RE.fullmatch(sid)),f'Unsafe site ID {sid}')
            require(sid in site_b4,f'No B4.0 site {sid}')
            b4=site_b4[sid];enabled=site['status'] in ELIGIBLE
            if sid in PROTECTED or int(b4['personCount'])==0 or not enabled:
                summaries.append({'siteId':sid,'name':site['name'],'status':'WINTERFELL_V3' if sid=='site_nw_029' else 'NOT_GLOBAL_C411_HOUSE_PROJECTION',
                                  'projectedCandidates':0,'indexedHomes':0,'file':None})
                continue
            lots=houses_for_site(repo/TILE,site)
            filename=sid+'.jsonl.gz';src=repo/B4/filename
            require(src.is_file(),f'Missing B4.0 shard {src}')
            with (pending/filename).open('wb') as raw:
                with gzip.GzipFile(fileobj=raw,mode='wb',compresslevel=6,mtime=0) as f:
                    def emit(rec):f.write((json.dumps(rec,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf8'))
                    result=index_people(src,sid,lots,emit)
            total+=result['personsIndexed'];house_total+=result['homesRepresented']
            summaries.append({'siteId':sid,'name':site['name'],'status':'PLANNED_HOUSE_CANDIDATES',
                        'projectedCandidates':result['personsIndexed'],'indexedHomes':result['homesRepresented'],
                        'availableC411Homes':result['availableHouses'],'file':filename,
                        'anchorX':int(site['originalX']),'anchorZ':int(site['originalZ']),
                        'bounds':site['bounds']})
            if i%40==0:print(f'B4.1A indexed {i}/248 site records; candidates={total}',flush=True)
        require(len(summaries)==248 and len({x['siteId'] for x in summaries})==248,'Missing site')
        publication={'schemaVersion':VERSION,'sourcePopulationCount':manifest['peopleTotal'],
           'sourceSiteCount':248,'projectionCandidateCount':total,'uniqueHousesRepresented':house_total,
           'physicalEntityBudget':128,'worldgenPlacementAuthority':'C4_11_RUNTIME_APPROVED_PLACEMENT_ONLY',
           'notAnNpcRegistryImport':True,'doesNotFabricateWorldEventsOrLlmMemories':True,
           'sites':summaries}
        (pending/'manifest.json').write_text(json.dumps(publication,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        previous=out.with_name(out.name+'.previous')
        if previous.exists():shutil.rmtree(previous)
        if out.exists():out.rename(previous)
        try:pending.rename(out)
        except Exception:
            if previous.exists():previous.rename(out)
            raise
        return publication
    except Exception:
        if pending.exists():shutil.rmtree(pending)
        raise

def main():
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--output',type=Path)
    args=p.parse_args();v=build(args.repo,args.output)
    print(json.dumps({'status':'B4.1A_PROJECTION_RESOURCES_COMPILED','sites':v['sourceSiteCount'],
                      'peopleRetained':v['sourcePopulationCount'],'indexedNpcCandidates':v['projectionCandidateCount'],
                      'homesRepresented':v['uniqueHousesRepresented']}))
if __name__=='__main__':main()
