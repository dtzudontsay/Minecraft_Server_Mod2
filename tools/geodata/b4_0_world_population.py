#!/usr/bin/env python3
"""B4.0: deterministic, sharded WORLDWIDE identity baseline, not active AI.

Inputs are the existing C4.9 household reservations, C4.8 204-job matrix,
C4.4 occupation -> workplace catalog, Winterfell V2 and B2/B3 region profiles.
Never edits saves or GitHub. --install writes a distributable initial resource
under src/main/resources so Minecraft can inspect everyone on demand.

This is a PROVISIONAL birth census: no fabricated true noble identities,
independently verified genealogies or witnessed events. Generated person records
are versioned simulation candidates pending integration with NpcRegistry.
"""
from __future__ import annotations
import argparse, array, collections, csv, gzip, hashlib, json, os, random, shutil, tempfile
from pathlib import Path

SCHEMA='knownworld.b4_0.identity_baseline.v1'
TOTAL_PEOPLE=3_760_684
TOTAL_WORKERS=1_880_341
TOTAL_HOUSEHOLDS=799_343
WINTERFELL='site_nw_029'
SOURCE='tools/geodata/output/c4_9_settlement_ledgers'
JOBS='docs/reference/c4_8_worldwide/C4_8_ALL_248_SITES_204_JOBS.csv'
B2='src/main/resources/data/knownworld/scenarios/agot_298_ac/world_sites_b2_b3_v1.json'
C44='src/main/resources/data/knownworld/scenarios/agot_298_ac/occupation_building_catalog_c4_4_v2.json'
WINTERFELL_SOURCE='src/main/resources/data/knownworld/case_study/winterfell_v2.json'
PALETTES='tools/data/b4_0_name_pools.json'
RESOURCE='src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1'
DEFAULT_OUT='tools/geodata/output/b4_0_world_population'


def jread(path):
    with path.open(encoding='utf-8-sig') as f:return json.load(f)


def seed(*parts):
    return int.from_bytes(hashlib.sha256(':'.join(map(str,parts)).encode()).digest()[:8],'big')


def rows(path):
    with path.open(encoding='utf-8-sig',newline='') as f:
        yield from csv.DictReader(f,delimiter=';')


def pid(sid,n):return f'b4:{sid}:{n:08d}'


def hh_index(identifier):
    try:return int(identifier.rsplit(':',1)[1])
    except (ValueError,IndexError):raise ValueError('Invalid household reservation ID: '+identifier)


def resolve_profiles(registry, regional_base, regional_ext):
    """Link accepted site-region metadata; never infer culture from place spelling."""
    source={str(x['locationId']):x for x in regional_base}
    extension={str(x['locationId']):x for x in regional_ext}
    lookup={s['siteId']:s for s in registry['physicalSites']}
    region_map=collections.defaultdict(list)
    for reg in registry['regions']:
        for sid in reg.get('descendantPhysicalSiteIds',[]):
            region_map[sid].append(reg)
    profiles={}
    for sid,site in lookup.items():
        r=site.get('regionalInfluences') or {}
        location=r.get('effectiveSourceLocationId')
        candidates=region_map.get(sid,[])
        reg=next((x for x in candidates if (x.get('regionalInfluences') or {}).get('effectiveSourceLocationId')==location),None)
        if not reg and candidates:
            reg=max(candidates,key=lambda x:sum(bool((x.get('regionalInfluences') or {}).get(k)) for k in ('cultureWeights','religionWeights')))
        influence=reg.get('regionalInfluences',{}) if reg else {}
        base=source.get(location,{})
        ext=extension.get(location,{})
        def weights(key):
            # Regional profiles are source of truth; B2's actual reconciled
            # geography-specific weights, where present, override the base.
            acc={}
            for origin in (base,ext):
                for culture,meta in (origin.get(key) or {}).items():
                    w=meta.get('weight',0) if isinstance(meta,dict) else 0
                    if w>0:acc[culture]=float(w)
            for identity,raw_weight in (influence.get({'cultures':'cultureWeights','religions':'religionWeights','languages':'languageWeights'}[key]) or {}).items():
                # B2/B3 stores weighted provenance objects, not bare floats.
                w=raw_weight.get('weight',0) if isinstance(raw_weight,dict) else raw_weight
                if float(w)>0:acc[identity]=float(w)
            # Unknown remaining mass is NOT a fictitious minority ethnicity.
            if acc and sum(acc.values())<0.999:
                acc['unknown_religion' if key=='religions' else 'unknown_'+key[:-1]]=max(.0,1-sum(acc.values()))
            if not acc:acc={'unknown_religion' if key=='religions' else 'unknown_'+key[:-1]:1.0}
            return acc
        profiles[sid]={'regionProfileId':location or 'UNRESOLVED',
                       'cultures':weights('cultures'),'religions':weights('religions'),
                       'languages':weights('languages'),
                       'values':{k:float(v['value']) for origin in (base,ext) for k,v in (origin.get('values') or {}).items() if isinstance(v,dict) and 'value' in v},
                       'provenance':'B2_B3_REGIONAL_INFLUENCES_WITH_EXPLICIT_GAPS'}
    return profiles


def weighted(rng,values):
    pairs=[(k,max(0.0,float(v))) for k,v in sorted(values.items()) if float(v)>0]
    if not pairs:return 'unknown'
    point=rng.random()*sum(w for _,w in pairs)
    for key,w in pairs:
        point-=w
        if point<0:return key
    return pairs[-1][0]


def names_for(culture,palettes):
    pool=palettes['cultureAliases'].get(culture,culture)
    return palettes['pools'].get(pool,palettes['pools']['unknown']),pool


def age_plan(sizes,workers,seed_value):
    """Adult slots per household; guarantees capacity for exact workforce.

    Age ≠ employed: all employees occupy an adult slot; remaining adults may
    include elder, homemaker, disabled, unemployed or other non-employees.
    """
    total=sum(sizes)
    minimum=[min(n,2) for n in sizes]
    needed=max(workers,min(total,int(total*.68)))
    remaining=max(0,needed-sum(minimum))
    adults=minimum[:]
    # Allocate in deterministic rotating waves; avoid putting every extra
    # adult into an early household, without O(population * household_count).
    rng=random.Random(seed_value)
    start=rng.randrange(max(1,len(sizes)))
    for stride in range(2,max(sizes,default=0)):
        if remaining==0:break
        for off in range(len(sizes)):
            i=(off+start)%len(sizes)
            if remaining and sizes[i]>adults[i]:
                adults[i]+=1;remaining-=1
        start=(start+7919)%max(1,len(sizes))
    if remaining:raise ValueError('Cannot allocate adult slots')
    return adults


def generate_generic(site,hh_rows,jobrow,mapping,palettes,profile,writer):
    sid=site['siteId'];pop=int(site['populationTarget']);workers=int(site['workingPopulationTarget'])
    household_total=int(site['householdsTarget']);housing_units=int(site['residentialUnitsPerBuilding'])
    modes={'MOBILE_NO_FIXED_FOOTPRINT','RUIN_OR_LANDMARK_ONLY','UNVERIFIED_NO_CONSTRUCTION'}
    if int(site['residentialBuildingsTarget'])*housing_units<household_total:
        raise ValueError(sid+': housing reservations over capacity')
    if len(hh_rows)!=household_total or sum(int(h['membersTarget']) for h in hh_rows)!=pop:
        raise ValueError(sid+': missing/incorrect C4.9 household reservations')
    if sorted(hh_index(h['householdReservationId']) for h in hh_rows)!=list(range(1,household_total+1)):
        raise ValueError(sid+': household IDs are not contiguous')
    sizes=[int(h['membersTarget']) for h in hh_rows]
    adults=age_plan(sizes,workers,seed(SCHEMA,sid,'adults'))
    if sum(adults)<workers:raise ValueError(sid+': too few adult job candidates')
    columns=[x for x in jobrow if x not in {'site_id','name','primary_marker','all_markers','tier','atlas_tag','blueprint_mode','working_x','working_z','planned_people','planned_workers'}]
    jobs={k:int(jobrow[k]) for k in columns if int(jobrow[k])>0}
    if len(columns)!=204 or sum(jobs.values())!=workers:
        raise ValueError(sid+': 204 occupation matrix does not conserve workers')
    for job in jobs:
        if job not in mapping:raise ValueError(sid+': occupation missing C4.4 mapping: '+job)
    rng=random.Random(seed(SCHEMA,sid,'people'))
    job_pool=[key for k,v in sorted(jobs.items()) for key in [k]*v]
    rng.shuffle(job_pool)
    # Employees selected evenly across the adult population rather than all
    # the first adults, preserving an exact per-occupation job assignment.
    eligible=[];start=0
    for a,n in zip(adults,sizes):
        eligible.extend(range(start,start+a));start+=n
    rng.shuffle(eligible)
    jobs_by_ordinal={pos:job_pool[j] for j,pos in enumerate(eligible[:workers])}
    counters=collections.Counter();unlinked=collections.Counter();cultures=collections.Counter();religions=collections.Counter()
    age_counts=collections.Counter();workplaces=collections.Counter();n=0
    for slot,adult in zip(hh_rows,adults):
        hid=slot['householdReservationId'];size=int(slot['membersTarget'])
        if slot['siteId']!=sid:raise ValueError('Mixed-site household ledger')
        home=(f'{sid}:home:{(hh_index(hid)-1)//housing_units+1:07d}'
              if site['blueprintMode'] not in modes else None)
        primary_culture=weighted(rng,profile['cultures'])
        pool,_=names_for(primary_culture,palettes)
        household_label=rng.choice(pool['nonNobleHouseholdLabels'])
        # First two adults are potential carers. Never invent noble-house membership.
        sexes=['MALE' if rng.randrange(2) else 'FEMALE' for _ in range(size)]
        if adult>=2 and rng.random()<.68:sexes[1]='FEMALE' if sexes[0]=='MALE' else 'MALE'
        age0=rng.randint(28,62)
        ages=[age0]
        if adult>=2:ages.append(max(20,min(68,age0+rng.randint(-9,9))))
        for ix in range(2,adult):ages.append(rng.randint(17,77))
        spouse=(adult>=2 and sexes[0]!=sexes[1] and abs(ages[0]-ages[1])<=20)
        for ix in range(adult,size):
            ages.append(rng.randint(0,max(0,min(17, min(ages[:2]) - 16 if spouse else age0 - 16))))
        household_ids=[pid(sid,n+i+1) for i in range(size)]
        parents=household_ids[:2] if spouse else household_ids[:1]
        for ix in range(size):
            unique=household_ids[ix];culture=(weighted(rng,profile['cultures']) if rng.random()<.11 else primary_culture)
            religion=weighted(rng,profile['religions']);language=weighted(rng,profile['languages'])
            npool,used_pool=names_for(culture,palettes)
            sex=sexes[ix]
            first=rng.choice(npool['male'] if sex=='MALE' else npool['female'])
            job=jobs_by_ordinal.get(n+ix)
            workplace=None
            if job:
                kind=mapping[job]
                spec=next((x for x in site['buildingDemandByKind'] if x['kind']==kind),None)
                order=counters[kind];counters[kind]+=1
                if kind=='KEEP' and site['mainComplex'].get('required'):
                    workplace=f'{sid}:core:1'
                elif spec is not None and int(spec['slotsPerBuilding'])>0:
                    unit=order//int(spec['slotsPerBuilding'])+1
                    if unit<=int(spec['requestedBuildings']):
                        workplace=f'{sid}:work:{kind}:{unit:06d}'
                if not workplace:unlinked[kind]+=1
                else:workplaces[workplace]+=1
            mother=next((i for i in parents if sexes[household_ids.index(i)]=='FEMALE'),None) if ix>=adult else None
            father=next((i for i in parents if sexes[household_ids.index(i)]=='MALE'),None) if ix>=adult else None
            role=('HOUSEHOLD_HEAD' if ix==0 else 'PARTNER_OR_CARER' if ix==1 and adult>=2
                  else 'ADULT_RELATIVE' if ix<adult else 'CHILD_DEPENDENT')
            data={'personId':unique,'siteId':sid,'householdId':hid,
                  'homeLotId':home,'givenName':first,'householdName':household_label,
                  'displayName':first+' of '+household_label if household_label else first,
                  'birthYearAC':298-ages[ix],'sex':sex,'householdRole':role,
                  'motherId':mother,'fatherId':father,
                  'spouseId':(household_ids[1-ix] if spouse and ix<2 else None),
                  'cultureId':culture,'religionId':religion,'primaryLanguageId':language,
                  'occupationId':job,'workplaceId':workplace,
                  'employment':('MODELED_WORKER' if job else 'NO_ASSIGNED_JOB'),
                  'regionalProfileId':profile['regionProfileId'],
                  'profileSeed':seed(SCHEMA,unique,'traits'),
                  'identityProvenance':'MOD_GENERATED_298AC_PROVISIONAL',
                  'physicalNpcPresent':False,'actualWorldExperiences':0}
            writer(data);cultures[culture]+=1;religions[religion]+=1
            age_counts['ADULT' if ix<adult else 'CHILD']+=1
        n+=size
    if n!=pop or sum(counters.values())!=workers or sum(age_counts.values())!=pop:
        raise ValueError(sid+': conservation failed during person generation')
    return {'people':n,'households':len(sizes),'workers':sum(counters.values()),
            'cultureCounts':dict(cultures),'religionCounts':dict(religions),
            'ageBandCounts':dict(age_counts),'unboundWorkplaceWorkers':dict(unlinked),
            'workplaceReservationCountUsed':len(workplaces)}


def generate_winterfell(site,roster,writer):
    residents=roster['residents'];hh=roster['households']
    if len(residents)!=5000 or len(hh)!=1136 or int(site['populationTarget'])!=5000:
        raise ValueError('Cannot replace Winterfell without exact existing authored roster')
    ids=set();culture=collections.Counter();religion=collections.Counter()
    for p in residents:
        original=p['id']
        if original in ids:raise ValueError('Duplicate existing Winterfell ID')
        ids.add(original);culture[p.get('culture') or 'unknown_culture']+=1
        religion[p.get('religion') or 'unknown_religion']+=1
        writer({'personId':original,'siteId':WINTERFELL,'householdId':p.get('householdId'),
                'homeLotId':p.get('homeId'),'displayName':p.get('name'),
                'birthYearAC':p.get('birthYear'),'sex':p.get('sex'),
                'cultureId':p.get('culture'),'religionId':p.get('religion'),
                'occupationId':p.get('job'),'workplaceId':p.get('workplaceId'),
                'identityProvenance':'EXISTING_WINTERFELL_V2_REUSED_NOT_IMPORTED_AGAIN',
                'physicalNpcPresent':False,'actualWorldExperiences':None})
    return {'people':5000,'households':1136,'workers':int(site['workingPopulationTarget']),
            'cultureCounts':dict(culture),'religionCounts':dict(religion),
            'ageBandCounts':{},'unboundWorkplaceWorkers':{},'workplaceReservationCountUsed':0,
            'notes':['C4.8 worker target not treated as reassignment of existing Winterfell people',
                     'Use existing Winterfell V3 runtime and identity records']}


def generate(repo:Path,out:Path,selection=None,strict=True):
    sources=repo/SOURCE
    site_data=jread(sources/'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json')['sites']
    jobs={x['site_id']:x for x in rows(repo/JOBS)}
    work_catalog=jread(repo/C44)
    mapping={x['jobId']:x.get('building') for x in work_catalog['rows'] if not x.get('isDuplicate')}
    if len(mapping)!=204:raise ValueError(f'Expected 204 unique job-to-building records, got {len(mapping)}')
    b2=jread(repo/B2)
    profiles=resolve_profiles(b2,jread(repo/'src/main/resources/data/knownworld/reference/regional_influences.json'),
                              jread(repo/'src/main/resources/data/knownworld/reference/regional_influence_extensions.json'))
    palettes=jread(repo/PALETTES)
    if len(site_data)!=248 or len(jobs)!=248:raise ValueError('Missing 248-site baseline')
    if strict and (sum(int(x['populationTarget']) for x in site_data),
                   sum(int(x['workingPopulationTarget']) for x in site_data),
                   sum(int(x['householdsTarget']) for x in site_data))!=(TOTAL_PEOPLE,TOTAL_WORKERS,TOTAL_HOUSEHOLDS):
        raise ValueError('C4.9 conservation baseline drift')
    filtered=[x for x in site_data if selection is None or x['siteId'] in selection]
    if selection and set(x['siteId'] for x in filtered)!=set(selection):raise ValueError('Unknown selected site')
    temp=out.parent/(out.name+'.build_incomplete')
    if temp.exists():shutil.rmtree(temp)
    temp.mkdir(parents=True)
    source_households=sources/'C4_9_HOUSEHOLD_RESERVATIONS.jsonl'
    if not source_households.exists():raise FileNotFoundError('C4.9 household reservations missing. Re-run C4.9 without --summary-only.')
    wanted={x['siteId'] for x in filtered if x['siteId']!=WINTERFELL}
    # Input is C4.9 site-ordered, so only hold ONE settlement in memory.
    sites=iter(filtered);hh_input=source_households.open(encoding='utf8')
    pending=None
    def site_households(sid):
        nonlocal pending
        group=[]
        if pending is not None:
            if pending['siteId']==sid:group.append(pending);pending=None
            else:return group
        for line in hh_input:
            item=json.loads(line)
            if item['siteId']==sid:group.append(item)
            else:pending=item;break
        return group
    # Allow filtered test mode with out-of-order skips by alternatively loading all
    # requested source site reservations into bounded site buffers (test only).
    if selection is not None:
        hh_input.close()
        grouped=collections.defaultdict(list)
        with source_households.open(encoding='utf8') as f:
            for line in f:
                item=json.loads(line)
                if item['siteId'] in wanted:grouped[item['siteId']].append(item)
    summaries=[];alltot=collections.Counter()
    try:
        for ix,site in enumerate(filtered,1):
            sid=site['siteId'];people=int(site['populationTarget']);hhs=int(site['householdsTarget'])
            prof=profiles.get(sid,{'regionProfileId':'UNRESOLVED','cultures':{'unknown_culture':1},'religions':{'unknown_religion':1},'languages':{'unknown_language':1},'values':{},'provenance':'UNRESOLVED'})
            if people==0:
                summ={'people':0,'households':0,'workers':0,'unboundWorkplaceWorkers':{}}
                file_name=None;sha=None
            else:
                file_name=f'{sid}.jsonl.gz'
                file_path=temp/file_name
                with file_path.open('wb') as raw, gzip.GzipFile(fileobj=raw,mode='wb',compresslevel=6,mtime=0) as f:
                    digest=hashlib.sha256();lines=0
                    def emit(person):
                        nonlocal lines
                        data=(json.dumps(person,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf8')
                        f.write(data);digest.update(data);lines+=1
                    if sid==WINTERFELL:summ=generate_winterfell(site,jread(repo/WINTERFELL_SOURCE),emit)
                    else:
                        reservations=grouped[sid] if selection is not None else site_households(sid)
                        summ=generate_generic(site,reservations,jobs[sid],mapping,palettes,prof,emit)
                    if lines!=people:raise ValueError(sid+': emission count mismatch')
                    sha=digest.hexdigest()
            alltot.update(people=summ['people'],households=summ['households'],workers=summ['workers'])
            summaries.append({'siteId':sid,'name':site['name'],'tier':site['tier'],
                'mapAnchor':site['mapAnchor'],'markerIds':site['markerIds'],'file':file_name,
                'uncompressedSha256':sha,'personCount':summ['people'],
                'householdCount':summ['households'],'modeledWorkerCount':summ['workers'],
                'regionalProfile':prof,'audit':summ,
                'status':'BASELINE_AUTHORED_REUSED' if sid==WINTERFELL else ('EMPTY_LOCATION' if people==0 else 'GENERATED_PROVISIONAL_NOT_IN_NPC_REGISTRY')})
            if ix%20==0:print(f'  B4.0 generated {ix}/{len(filtered)} sites | people={alltot["people"]:,}',flush=True)
        if selection is None and strict and (alltot['people'],alltot['households'],alltot['workers'])!=(TOTAL_PEOPLE,TOTAL_HOUSEHOLDS,TOTAL_WORKERS):
            raise ValueError('Worldwide output headcount mismatch: '+str(alltot))
        manifest={'schemaVersion':SCHEMA,'worldYearAC':298,'siteCount':len(summaries),
                  'peopleTotal':alltot['people'],'householdsTotal':alltot['households'],
                  'modeledWorkersTotal':alltot['workers'],
                  'populationAuthority':'INITIAL_BASELINE_FOR_FUTURE_RECONCILIATION',
                  'npcRegistryIntegrated':False,'worldSimulationDailyLifeActive':False,
                  'physicalMinecraftEntitiesSpawned':0,'conversationSystemIntegratedOutsideWinterfell':False,
                  'authoredCharactersOutsideWinterfellReconciliation':'PENDING_BEFORE_LIVE_NPC_IMPORT',
                  'winterfellIdPolicy':'EXACT_EXISTING_IDS_REUSED_NO_SECOND_IMPORT',
                  'sites':summaries}
        (temp/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
        if out.exists():
            # Atomic-ish replacement on Windows: existing output remains as backup
            # until complete success; on failure do not silently leave no data.
            back=out.with_name(out.name+'.previous')
            if back.exists():shutil.rmtree(back)
            out.replace(back)
            try:temp.replace(out)
            except Exception:
                back.replace(out)
                raise
        else:temp.replace(out)
        return manifest
    except Exception:
        if temp.exists():shutil.rmtree(temp)
        raise
    finally:
        if not hh_input.closed:hh_input.close()


def install(out:Path,resource:Path):
    manifest=jread(out/'manifest.json')
    if manifest['siteCount']!=248 or manifest['peopleTotal']!=TOTAL_PEOPLE:
        raise ValueError('Refusing to install partial or wrong-size census into game resources')
    staging=resource.with_name(resource.name+'.incoming')
    if staging.exists():shutil.rmtree(staging)
    shutil.copytree(out,staging)
    previous=resource.with_name(resource.name+'.previous')
    if previous.exists():shutil.rmtree(previous)
    if resource.exists():resource.rename(previous)
    try:staging.rename(resource)
    except Exception:
        if previous.exists():previous.rename(resource)
        raise
    print('INSTALLED full baseline resource: '+str(resource))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output',type=Path)
    parser.add_argument('--site',action='append',help='For limited offline validation only (cannot --install)')
    parser.add_argument('--install',action='store_true',help='Publish full validated population to game resources')
    args=parser.parse_args()
    repo=args.repo.resolve();output=(args.output or repo/DEFAULT_OUT).resolve()
    if args.install and args.site:parser.error('--install is only supported for complete worldwide generation')
    result=generate(repo,output,set(args.site) if args.site else None,strict=not bool(args.site))
    print(json.dumps({'sites':result['siteCount'],'people':result['peopleTotal'],
                      'households':result['householdsTotal'],'workers':result['modeledWorkersTotal'],
                      'personResources':str(output),'simulatedToday':False},indent=2))
    if args.install:install(output,repo/RESOURCE)


if __name__=='__main__':main()
