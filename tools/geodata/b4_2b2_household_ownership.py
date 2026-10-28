#!/usr/bin/env python3
"""B4.2B.2: build a worldwide immutable economic SOCIAL-BASELINE index.

The output IS NOT a coin or grain ledger. No EconomyRuntime transactions,
no unverified noble titles, no fabricated legal owners, and no save writes.
All B4.0 persons including protected/physically deferred sites are covered.
Projection source is preferred for placed sites to retain C4.15 work IDs.
"""
from __future__ import annotations
import argparse,collections,gzip,hashlib,json,re,shutil,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
B40=SC/'world_population_b4_v1'
B41=SC/'world_projection_b4_1a1_v1'
B42=SC/'world_daily_b4_2_v1'
OUT=SC/'world_household_economy_b4_2b2_v1'
SCHEMA='knownworld.b4_2b2.worldwide_household_standing.v1'
PARTS=16
VALID_ID=re.compile(r'site_[a-z][a-z0-9_]{1,90}')

def load(path):return json.loads(path.read_text(encoding='utf8'))
def jhash(s):
    h=0
    for c in s:h=(31*h+ord(c))&0xffffffff
    return h

def score_job(j):
    j=(j or '').lower().replace('_',' ').replace('-',' ')
    if any(k in j for k in ('merchant','banker','magister','shipowner','guildmaster','master trader')):return 8
    if any(k in j for k in ('steward','administrator','maester','physician','magistrate','master smith','master crafts','ship captain','tax collector')):return 6
    if any(k in j for k in ('smith','mason','carpenter','tailor','scribe','clerk','trader','teacher','priest','guard captain','innkeep')):return 4
    if any(k in j for k in ('farmer','fisher','herder','hunter','baker','brewer','sailor','artisan','guard')):return 3
    if any(k in j for k in ('labor','servant','porter','beggar','unskilled','day worker')):return 1
    return 2 if j else 0

def standing(hid,member_count,workers,occupation_score):
    # This is an INFERRED CLASS, NOT a balance or canonical social rank.
    headcount=member_count;dependents=headcount-workers
    stable=jhash(hid)
    raw=max(0, min(1000, 190 + occupation_score*37 + workers*32 - dependents*9 + (stable%147)))
    label=('PROSPEROUS' if raw>=640 else 'COMFORTABLE' if raw>=445 else
           'MODEST' if raw>=245 else 'PRECARIOUS')
    return label,raw

def read_people(repo,sid,proj):
    if proj['status']=='C4_11_HOME_CANDIDATES':
        for tile in proj['tiles']:
            with gzip.open(repo/B41/sid/(tile+'.jsonl.gz'),'rt',encoding='utf8') as f:
                for line in f: yield json.loads(line)
    else:
        with gzip.open(repo/B40/(sid+'.jsonl.gz'),'rt',encoding='utf8') as f:
            for line in f: yield json.loads(line)

def gzip_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('wb') as f:
        with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0,compresslevel=5) as g:
            g.write(json.dumps(obj,ensure_ascii=False,separators=(',',':'),sort_keys=True).encode('utf8'))

def build(root=ROOT,output=None,selection=None):
    repo=Path(root).resolve();dest=Path(output or repo/OUT).resolve()
    pop=load(repo/B40/'manifest.json');proj=load(repo/B41/'manifest.json');work=load(repo/B42/'manifest.json')
    if (pop.get('schemaVersion'),pop.get('peopleTotal'),pop.get('householdsTotal'),pop.get('modeledWorkersTotal'),pop.get('siteCount')) != (
        'knownworld.b4_0.identity_baseline.v1',3760684,799343,1880341,248):raise ValueError('B4.0 input census mismatch')
    if (proj.get('schemaVersion'),proj.get('sourcePopulationCount'),len(proj.get('sites',[])))!=(
        'knownworld.b4_1a1.household_projection.v1',3760684,248):raise ValueError('B4.1 input mismatch')
    if (work.get('schemaVersion'),work.get('peopleTotal'),work.get('workersTotal'),len(work.get('sites',[])))!=(
        'knownworld.b4_2.daily_worksites.v1',3760684,1880341,248):raise ValueError('B4.2 input mismatch')
    ps={s['siteId']:s for s in proj['sites']};ws={s['siteId']:s for s in work['sites']}
    if set(ps)!={s['siteId'] for s in pop['sites']} or set(ws)!=set(ps):raise ValueError('Site set mismatch')
    if selection and not set(selection)<=set(ps):raise ValueError('Unknown selection')
    if dest.exists() or dest.with_name(dest.name+'.pending').exists():raise FileExistsError('Refusing to overwrite published or pending economic index')
    temp=dest.with_name(dest.name+'.pending');temp.mkdir(parents=True)
    site_out=[];grand=collections.Counter()
    try:
        for n,site in enumerate(pop['sites'],1):
            sid=site['siteId'];expected_p=site['personCount'];expected_h=site['householdCount'] if 'householdCount' in site else site.get('households',0)
            if not VALID_ID.fullmatch(sid):raise ValueError('Unsafe site '+sid)
            if selection and sid not in selection:
                site_out.append({'siteId':sid,'status':'SKIPPED_TEST_ONLY','people':0,'households':0,'workers':0,'workplaces':0,'parts':[]});continue
            # Winterfell's authored V3 residents are NOT B4:* provisional identities.
            # Include them in the global conservation census but leave their
            # existing authoritative households, offices and runtime untouched.
            if expected_p==0:
                if expected_h!=0 or site['modeledWorkerCount']!=0:raise ValueError('Empty site census mismatch '+sid)
                site_out.append({'siteId':sid,'status':'EMPTY_NO_RESIDENTS',
                    'people':0,'households':0,'workers':0,'workplaces':0,'parts':[]})
                continue
            if sid=='site_nw_029':
                site_out.append({'siteId':sid,'status':'WINTERFELL_V3_SEPARATELY_AUTHORED_NO_BOOTSTRAP',
                    'people':expected_p,'households':expected_h,'workers':site['modeledWorkerCount'],
                    'workplaces':0,'parts':[]})
                grand.update({'people':expected_p,'households':expected_h,'workers':site['modeledWorkerCount']})
                continue
            homes={};jobs={};npeople=0;nworkers=0
            for p in read_people(repo,sid,ps[sid]):
                if p.get('siteId')!=sid or not str(p.get('personId','')).startswith('b4:'+sid+':'):raise ValueError('Invalid person or cross-site identity in '+sid)
                h=p.get('householdId');pid=p['personId'];job=p.get('occupationId')
                if not h or not isinstance(h,str):raise ValueError('Unassigned household in '+sid)
                v=homes.setdefault(h,{'members':0,'workers':0,'jobScore':0,'head':None,'homeLotId':p.get('homeLotId')})
                if v['homeLotId']!=p.get('homeLotId'):raise ValueError('Household split over home lots: '+h)
                v['members']+=1;npeople+=1
                if job:
                    v['workers']+=1;v['jobScore']+=score_job(job);nworkers+=1
                if p.get('householdRole')=='HOUSEHOLD_HEAD':
                    if v['head'] is not None:raise ValueError('Multiple household heads: '+h)
                    v['head']=pid
                wid=p.get('workplaceId')
                if wid and job:
                    entry=jobs.setdefault(wid,{'count':0,'coordinator':None})
                    entry['count']+=1
                    prev=entry['coordinator']
                    if prev is None or (jhash(pid),pid)<(jhash(prev),prev):entry['coordinator']=pid
            if npeople!=expected_p or nworkers!=site['modeledWorkerCount'] or (expected_h and len(homes)!=expected_h):
                raise ValueError(f'Population mismatch {sid}: actual {npeople}/{len(homes)}/{nworkers}, expected {expected_p}/{expected_h}/{site["modeledWorkerCount"]}')
            partition=[{'households':{},'workplaces':{}} for _ in range(PARTS)]
            strata=collections.Counter();estimated_coins=0
            for h,v in homes.items():
                if v['head'] is None:raise ValueError('Household without head: '+h)
                label,score=standing(h,v['members'],v['workers'],v['jobScore'])
                # score is a relative estimate, never an actual money credit
                partition[jhash(h)&15]['households'][h]={'standing':label,'prosperityIndex':score,
                    'members':v['members'],'workers':v['workers'],'headPersonId':v['head'],
                    'residenceId':v['homeLotId'],'wealthAuthority':'MODELED_UNFUNDED_STARTING_ESTIMATE'}
                strata[label]+=1
            for wid,staff in jobs.items():
                if not wid.startswith(sid+':'):raise ValueError('Cross-site worksite: '+wid)
                # Deterministic acting coordinator, NOT assumed legal owner or employer.
                coordinator=staff['coordinator']
                partition[jhash(wid)&15]['workplaces'][wid]={
                    'assignedWorkers':staff['count'],'coordinatorPersonId':coordinator,
                    'coordinatorStatus':'PROVISIONAL_SHIFT_COORDINATOR',
                    'legalOwnerStatus':'UNRESOLVED_NO_INVENTED_HOLDER',
                    'legalOwnerPersonId':None,'employerStatus':'UNRESOLVED',
                    'physicalAccessVerified':False}
            part_ids=[]
            for idx,data in enumerate(partition):
                if not data['households'] and not data['workplaces']:continue
                gzip_json(temp/sid/(f'p{idx:02}.json.gz'),data)
                part_ids.append(idx)
            site_out.append({'siteId':sid,'status':'DERIVED_PROVISIONAL_NOT_ECONOMY_AUTHORITY',
                'people':npeople,'households':len(homes),'workers':nworkers,'workplaces':len(jobs),
                'parts':part_ids,'standingCounts':dict(strata)})
            grand.update({'people':npeople,'households':len(homes),'workers':nworkers,'workplaces':len(jobs)})
            if n%32==0:print(f'B4.2B.2 indexed {n}/248 sites; {grand["people"]:,} people, {grand["households"]:,} households',flush=True)
        if not selection and (grand['people'],grand['households'],grand['workers'])!=(3760684,799343,1880341):raise ValueError('WORLD CENSUS DOES NOT CONSERVE')
        manifest={'schemaVersion':SCHEMA,'siteCount':248,'populationTotal':grand['people'],
            'householdsTotal':grand['households'],'workersTotal':grand['workers'],
            'workplaceGroupsTotal':grand['workplaces'],
            'source':'B4.0 all people + C4.15-enhanced B4.1A.1 projection + B4.2 workforce validation',
            'historicalStartAC':298,'partitionCount':PARTS,
            'authority':'READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK',
            'actualCoinCredited':0,'actualGrainCreditedKg':0,'actualWagesPaid':0,
            'derivedStandingNotLegalTitle':True,'worksiteCoordinatorsNotProvenOwners':True,
            'sites':site_out}
        (temp/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
        temp.rename(dest)
        return {'siteCount':248,'people':grand['people'],'households':grand['households'],'workers':grand['workers'], 'workplaceGroups':grand['workplaces'],'actualCoinCredited':0,'readOnly':True}
    except BaseException:
        # Keep failed input for auditing? All data is derived. Purge partial output to avoid ambiguous installed status.
        shutil.rmtree(temp,ignore_errors=True)
        raise

def main():
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=ROOT);a.add_argument('--output',type=Path);a.add_argument('--site',action='append');o=a.parse_args()
    print(json.dumps(build(o.repo,o.output,set(o.site) if o.site else None),indent=2))
if __name__=='__main__':main()
