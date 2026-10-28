#!/usr/bin/env python3
"""B4.2: build a compact READ-ONLY workplace coordinate index for daily-life schedules.

Retains C4.15 employment overlay and all B4.0 people. No save mutation.
A worksite is a planned lot, NOT proof that it rendered/has a navigable path.
"""
from __future__ import annotations
import argparse,gzip,hashlib,json,pathlib,re,shutil
ROOT=pathlib.Path(__file__).resolve().parents[2]
SC=pathlib.Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
POP=SC/'world_population_b4_v1/manifest.json'
C4=SC/'universal_construction_c4_11_global_v1.json'
TILES=SC/'c4_11_global_tiles'
COMMUTES=SC/'world_commutes_c4_15_v1/audit.json'
PROJECTION=SC/'world_projection_b4_1a1_v1/manifest.json'
OUT=SC/'world_daily_b4_2_v1'
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}
SCHEMA='knownworld.b4_2.daily_worksites.v1'
def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def valid(s):return isinstance(s,str) and re.fullmatch(r'site_[a-z][a-z0-9_]{1,90}',s) is not None
def index_site(repo,site):
    sid=site['siteId'];lots={};instances=0
    for tile_id in site.get('tiles',[]):
        path=repo/TILES/sid/(tile_id+'.json')
        obj=read(path)
        if obj['siteId']!=sid:raise ValueError('Foreign construction tile '+str(path))
        for lot in obj.get('lots',[]):
            if lot['kind']=='HOUSE' or lot.get('permission')!='C3_MODULE_AVAILABLE':continue
            n=int(lot.get('s',0))
            if n<=0:continue
            lid=lot['id']
            if not lid.startswith(sid+':'):raise ValueError('Unsafe work lot ID '+lid)
            x,z,w,d=(int(lot[k]) for k in ('x','z','w','d'))
            if min(w,d)<1 or n>50000:raise ValueError('Invalid lot bounds/staff '+lid)
            row={'id':lid,'kind':lot['kind'],'x':x,'z':z,'w':w,'d':d,'maxStaff':n,
                 'status':'PLANNED_MODULE_NOT_RUNTIME_VERIFIED'}
            old=lots.setdefault(lid,row)
            if old!=row:raise ValueError('Conflicting lot '+lid)
            instances+=1
    return {'siteId':sid,'workplaces':sorted(lots.values(),key=lambda x:x['id'])},instances

def build(root=ROOT,subset=None):
    root=pathlib.Path(root).resolve();p=read(root/POP);c=read(root/C4);proj=read(root/PROJECTION)
    if (p.get('peopleTotal'),p.get('modeledWorkersTotal'),p.get('householdsTotal'),p.get('siteCount')) != (3760684,1880341,799343,248):
        raise ValueError('B4.0 population baseline mismatch')
    if c.get('siteCount')!=248 or not c.get('globalEnabled') or len(proj.get('sites',[]))!=248:
        raise ValueError('C4.15/B4.1A.1 prerequisites missing')
    if not (root/COMMUTES).is_file():raise FileNotFoundError('Missing C4.15 commute audit. Run C4.15 once.')
    commute=read(root/COMMUTES)
    if commute.get('version')!=1 or not commute.get('employmentTotalsPreserved'):
        raise ValueError('Invalid C4.15 commute summary')
    projection_by={j['siteId']:j for j in proj['sites']}
    pop_by={j['siteId']:j for j in p['sites']}
    if set(pop_by)!=set(projection_by):raise ValueError('B4.1A.1 and B4.0 site mismatch')
    if subset is not None and not subset<=set(pop_by):raise ValueError('Unknown --site')
    out=root/OUT;temp=out.with_name(out.name+'.incomplete')
    if temp.exists():shutil.rmtree(temp)
    temp.mkdir(parents=True)
    metadata=[];count=0
    try:
        for i,site in enumerate(c['sites'],1):
            sid=site['siteId']
            if not valid(sid):raise ValueError('Unsafe site ID: '+repr(sid))
            enabled=sid not in PROTECTED and projection_by[sid]['status']=='C4_11_HOME_CANDIDATES'
            if subset is not None:enabled=enabled and sid in subset
            record={'siteId':sid,'people':pop_by[sid]['personCount'],'workers':pop_by[sid]['modeledWorkerCount'],
                    'status':'READ_ONLY_PLANNED_WORKSITES' if enabled else 'NO_GENERIC_PROJECTION',
                    'workplaceCount':0,'file':None}
            if enabled:
                data,_=index_site(root,site)
                filename=sid+'.json.gz'; raw=json.dumps(data,separators=(',',':'),ensure_ascii=False).encode()
                with (temp/filename).open('wb') as f:
                    with gzip.GzipFile(filename='',mode='wb',fileobj=f,compresslevel=6,mtime=0) as gz:gz.write(raw)
                record['file']=filename;record['workplaceCount']=len(data['workplaces']);count+=len(data['workplaces'])
            metadata.append(record)
            if i%40==0:print(f'[B4.2] Indexed {i}/248 sites, {count} candidate work lots',flush=True)
        if len(metadata)!=248 or sum(x['people'] for x in metadata)!=3760684 or sum(x['workers'] for x in metadata)!=1880341:
            raise ValueError('Census conservation failure')
        manifest={'schemaVersion':SCHEMA,'sites':metadata,'siteCount':248,'peopleTotal':3760684,
                  'workersTotal':1880341,'workplacesIndexed':count,'winterfellV3Independent':True,
                  'fixedSourceIdentity':True,'commutesArePathVerified':False,
                  'jobProductionImplemented':False,
                  'description':'Read-only work lot coordinates for lazy world-time schedule evaluation; no NPC work events or actual building verification.'}
        (temp/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
        old=root/'tools/geodata/output/b4_2_previous_worksite_index'
        old.parent.mkdir(parents=True,exist_ok=True)
        if old.exists():shutil.rmtree(old)
        if out.exists():out.rename(old)
        try:temp.rename(out)
        except Exception:
            if old.exists():old.rename(out)
            raise
        print('[B4.2] Complete:',len(metadata),'sites,',count,'planned workplaces')
        return manifest
    except Exception:
        shutil.rmtree(temp,ignore_errors=True)
        raise

def main():
    a=argparse.ArgumentParser();a.add_argument('--root',type=pathlib.Path,default=ROOT);a.add_argument('--site',action='append');args=a.parse_args()
    build(args.root,set(args.site) if args.site else None)
if __name__=='__main__':main()
