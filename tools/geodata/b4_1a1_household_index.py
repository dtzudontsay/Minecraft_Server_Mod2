#!/usr/bin/env python3
"""B4.1A.1: full household projection resources, 512-block tiles, non-mutating B4.0 source.

Reads existing B4.0 and C4.11 resources. Never edits person IDs/saves. Every
person whose home is C4.11-renderable gets indexed, not just one/two per house.
"""
from __future__ import annotations
import argparse, collections, gzip, hashlib, json, shutil, tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SCENARIO=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
B4=SCENARIO/'world_population_b4_v1'
C4=SCENARIO/'universal_construction_c4_11_global_v1.json'
TILES=SCENARIO/'c4_11_global_tiles'
OUT=SCENARIO/'world_projection_b4_1a1_v1'
B2=SCENARIO/'world_sites_b2_b3_v1.json'
COMMUTE=SCENARIO/'world_commutes_c4_15_v1'
VERSION='knownworld.b4_1a1.household_projection.v1'
ELIGIBLE={'GLOBAL_TERRAIN_SURVEY_PENDING','PROVISIONAL_SPECIAL_GEOMETRY'}
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}
TILE_SPAN=512

def load(path):
    with Path(path).open(encoding='utf-8-sig') as f:return json.load(f)
def require(ok,msg):
    if not ok:raise ValueError(msg)
def safe_id(s):
    import re
    return isinstance(s,str) and re.fullmatch(r'site_[a-z][a-z0-9_]{1,90}',s) is not None

def available_houses(tile_dir,site):
    result={}
    for name in site['tiles']:
        f=tile_dir/site['siteId']/(name+'.json')
        require(f.is_file(),f'Missing C4.11 construction tile: {f}')
        tile=load(f)
        require(tile['siteId']==site['siteId'],'Cross-site C4.11 construction tile')
        for lot in tile['lots']:
            if lot['kind']!='HOUSE' or lot['permission']!='C3_MODULE_AVAILABLE':continue
            geo=(int(lot['x']),int(lot['z']),int(lot['w']),int(lot['d']))
            if lot['id'] in result: require(result[lot['id']]==geo,'Conflicting repeat house '+lot['id'])
            result[lot['id']]=geo
    return result

def family_name(p,household_name):
    """Noncanonical display convention only; core B4 name and identity unchanged.

    Avoid 'of Miller' for every person. Share household byname, with culturally
    appropriate exceptions. Derived household stem is stable across visits.
    """
    given=p.get('givenName') or (p.get('displayName') or 'Unknown').split()[0]
    culture=p.get('cultureId') or ''
    h=hashlib.blake2b(p['householdId'].encode(),digest_size=8).digest()
    n=int.from_bytes(h,'big')
    if culture in {'dothraki','jogos_nhai','wildling','thenn','lhazareen'}:
        # Many people will have just a personal name; a byname is occasional.
        return given if n%5 else given+' '+(household_name or 'of the grass')
    if culture in {'yi_tish','lengi'}:
        # Generated family marker first; this is a mod naming convention, not an asserted canon name.
        root=(household_name or 'Shan').split()[0]
        return root+' '+given
    if culture in {'summer_islander','naathi','ibbenese','sarnori','ghiscari','astapori',
                    'yunkish','meereenese','qarthene','qartheen','qaathi','valyrian','braavosi',
                    'lysene','myrish','tyroshi','volantene','norvoshi','qohorik','lorathi'}:
        return given if n%3 else given+' '+(household_name or '')
    stem=(household_name or 'River').replace(' ','').title()
    # First stage of non-noble byname expansion: labels > one-word trade names.
    endings=('ton','ford','well','mere','field','brook','wood','croft','wick','dale','by','ridge')
    # Keep roots like Stone/Frost, but don't force surnames on every Westerosi.
    family=stem if n%3==0 else stem+endings[(n>>9)%len(endings)]
    return given if n%7==0 else given+' '+family

def record(p,house):
    x,z,w,d=house
    return {'personId':p['personId'],'siteId':p['siteId'],'name':family_name(p,p.get('householdName')),
            'birthYearAC':p['birthYearAC'],'sex':p['sex'],'householdId':p['householdId'],
            'homeLotId':p['homeLotId'],'householdRole':p.get('householdRole'),
            'motherId':p.get('motherId'),'fatherId':p.get('fatherId'),'spouseId':p.get('spouseId'),
            'occupationId':p.get('occupationId'),'cultureId':p.get('cultureId'),
            'religionId':p.get('religionId'),'languageId':p.get('primaryLanguageId'),
            'workplaceId':p.get('workplaceId'),'commuteStatus':p.get('commuteStatus','NOT_YET_ASSIGNED'),
            'profileSeed':p.get('profileSeed'),
            'regionalProfileId':p.get('regionalProfileId'),
            'x':x,'z':z,'w':w,'d':d,'identityStatus':'B4_0_PROVISIONAL_UNCHANGED_ID',
            'buildingStatus':'C4_11_PLANNED_NOT_CONFIRMED_PER_LOT'}

def tile_key(house):
    x,z,w,d=house
    return f'{(x+w//2)//TILE_SPAN}_{(z+d+2)//TILE_SPAN}'

def write_people(src,sid,houses,temporary,profile=None,max_open=20,commutes=None):
    """Stream one population shard into tile buckets with bounded file descriptors."""
    count=0;households=set();tile_counts=collections.Counter();seen=set()
    buffers=collections.OrderedDict()
    temporary.mkdir(parents=True,exist_ok=True)
    try:
        with gzip.open(src,'rt',encoding='utf8') as f:
            for line in f:
                p=json.loads(line)
                require(p['siteId']==sid,'Cross-site person in '+sid)
                if commutes and p['personId'] in commutes:
                    override=commutes[p['personId']]
                    if override.get('workplaceId'):
                        p['workplaceId']=override['workplaceId']
                    p['commuteStatus']=override['commuteStatus']
                house=houses.get(p.get('homeLotId'))
                if house is None:continue
                pid=p['personId'];require(pid not in seen,'Duplicate person in '+sid+': '+pid)
                seen.add(pid)
                tid=tile_key(house)
                if tid in buffers:fh=buffers.pop(tid)
                else:
                    if len(buffers)>=max_open:
                        _,old=buffers.popitem(last=False);old.close()
                    fh=(temporary/(tid+'.jsonl')).open('a',encoding='utf8')
                buffers[tid]=fh
                fh.write(json.dumps(record(p,house),separators=(',',':'),ensure_ascii=False)+'\n')
                count+=1;households.add(p['householdId']);tile_counts[tid]+=1
    finally:
        for fh in buffers.values():fh.close()
    return count,len(households),dict(sorted(tile_counts.items()))

def build(repo=ROOT,out=None,selection=None):
    repo=Path(repo).resolve();out=Path(out or repo/OUT).resolve()
    b4=load(repo/B4/'manifest.json');c4=load(repo/C4);b2=load(repo/B2)
    require(b4['schemaVersion']=='knownworld.b4_0.identity_baseline.v1','B4.0 missing/stale')
    require(int(b4['peopleTotal'])==3_760_684 and int(b4['householdsTotal'])==799343,'B4.0 census mismatch')
    require(c4['siteCount']==b4['siteCount']==248 and c4['globalEnabled'],'C4.11 global resources missing')
    b4sites={s['siteId']:s for s in b4['sites']}
    b2sites={s['siteId']:s for s in b2['physicalSites']}
    require(len(b4sites)==len(b2sites)==248,'B2/B4 registry mismatch')
    if selection is not None:require(set(selection)<=set(b4sites),'Unknown site selection')
    pending=out.with_name(out.name+'.incomplete')
    if pending.exists():shutil.rmtree(pending)
    pending.mkdir(parents=True)
    sites=[];total=0;homeTotal=0;familyTotal=0
    try:
        for i,site in enumerate(c4['sites'],1):
            sid=site['siteId'];require(safe_id(sid),'Unsafe ID: '+str(sid))
            require(sid in b4sites,'C4 site absent from B4')
            enabled=sid not in PROTECTED and site['status'] in ELIGIBLE and b4sites[sid]['personCount']>0
            if selection is not None and sid not in selection:enabled=False
            meta={'siteId':sid,'name':site['name'],'status':'C4_11_HOME_CANDIDATES' if enabled else 'DEFERRED_EXISTING_OR_NONBUILDABLE',
                  'tiles':{},'residentsIndexed':0,'householdsIndexed':0}
            if enabled:
                require('bounds' in site,'Missing bounds '+sid)
                houses=available_houses(repo/TILES,site)
                src=repo/B4/(sid+'.jsonl.gz');require(src.is_file(),'Missing B4.0 people: '+sid)
                tmp=Path(tempfile.mkdtemp(prefix='b411_'+sid+'_'))
                try:
                    overlay=repo/COMMUTE/(sid+'.jsonl.gz')
                    commutes={}
                    if overlay.is_file():
                        with gzip.open(overlay,'rt',encoding='utf8') as r:
                            for line in r:
                                data=json.loads(line);commutes[data['personId']]=data
                    n,family_count,counts=write_people(src,sid,houses,tmp,commutes=commutes)
                    dest=pending/sid;dest.mkdir(parents=True,exist_ok=True)
                    for tid,npeople in counts.items():
                        with (tmp/(tid+'.jsonl')).open('rb') as inp, gzip.GzipFile(filename=str(dest/(tid+'.jsonl.gz')),mode='wb',compresslevel=5,mtime=0) as output:
                            shutil.copyfileobj(inp,output)
                    meta.update({'tiles':counts,'residentsIndexed':n,'householdsIndexed':family_count,
                                 'anchorX':site['originalX'],'anchorZ':site['originalZ'],'bounds':site['bounds'],
                                 'regionId':((b2sites[sid].get('regionalInfluences') or {}).get('effectiveSourceLocationId') or 'unknown'),
                                 'regionalValues':{k:v.get('value',0.0) for k,v in ((b2sites[sid].get('regionalInfluences') or {}).get('values') or {}).items()},
                                 'regionalNorms':{k:v.get('value',0.0) for k,v in ((b2sites[sid].get('regionalInfluences') or {}).get('socialNorms') or {}).items()}})
                    total+=n;familyTotal+=family_count;homeTotal+=len(houses)
                finally:shutil.rmtree(tmp,ignore_errors=True)
            sites.append(meta)
            if i%40==0:print(f'B4.1A.1 planned {i}/248 site records, indexed {total} residents',flush=True)
        require(len(sites)==248,'Missing physical site rows')
        manifest={'schemaVersion':VERSION,'physicalProjectionLimit':256,'sourcePopulationCount':3760684,
            'sourceHouseholdCount':799343,'indexedResidents':total,'indexedHouseholds':familyTotal,
            'projectedExistingWinterfellIds':False,'changesBasePopulation':False,
            'sites':sites,'schemaNote':'One B4 person per row; household grouping preserved; tiles lazily decoded'}
        (pending/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        backup=repo/'tools/geodata/output/b4_1a1_projection_backup'
        backup.parent.mkdir(parents=True,exist_ok=True)
        if backup.exists():shutil.rmtree(backup)
        if out.exists():shutil.move(str(out),str(backup))
        try:pending.rename(out)
        except Exception:
            if backup.exists():shutil.move(str(backup),str(out))
            raise
        return manifest
    except Exception:
        shutil.rmtree(pending,ignore_errors=True)
        raise

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo',type=Path,default=ROOT)
    p.add_argument('--output',type=Path)
    p.add_argument('--site',action='append',help='Development option: compile selected sites, audit retains 248 registry')
    a=p.parse_args();v=build(a.repo,a.output,set(a.site) if a.site else None)
    print(json.dumps({'status':'B4.1A.1_INDEX_INSTALLED','worldwideBasePeople':v['sourcePopulationCount'],
                      'projectablePeople':v['indexedResidents'],'indexedHouseholds':v['indexedHouseholds']}))
if __name__=='__main__':main()
