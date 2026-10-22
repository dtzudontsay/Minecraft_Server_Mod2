#!/usr/bin/env python3
"""C4.8: reconcile existing C1/C4.4 data; export a versioned, non-mutating worldwide reference.

Never creates NPCs, never changes population or occupations, never authorises construction.
The atlas-tier JSON is derived from the EXACT C1 marker groups.
"""
from __future__ import annotations
import csv, json, collections, hashlib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
C1=SC/'settlement_presets_c1_v1.json'
JOBS=SC/'settlement_job_distributions_c4_4_v2.json'
CAT=SC/'occupation_building_catalog_c4_4_v2.json'
OUT=ROOT/'docs/reference/c4_8_worldwide'
ASSET=ROOT/'src/main/resources/assets/knownworld/maps/atlas_site_tiers_c48.json'


def load(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def csv_write(path, headers, rows):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('w',encoding='utf-8-sig',newline='') as file:
        w=csv.writer(file,delimiter=';');w.writerow(headers);w.writerows(rows)

def marker_tag(site):
    if site['blueprintMode']=='RUIN_OR_LANDMARK_ONLY': return 'RUIN'
    return site['tier']

def make():
    c1=load(C1); job=load(JOBS); catalog=load(CAT)
    sites=c1['physicalSitePresets']; plans=job['plans']
    if len(sites)!=248 or len(plans)!=248: raise ValueError('Expected 248 unique physical sites in both versions')
    byid={p['siteId']:p for p in plans}
    if len(byid)!=248 or len({s['siteId'] for s in sites})!=248:raise ValueError('Duplicate site ID')
    source_jobs=[j['jobId'] for j in catalog['rows'] if not j['isDuplicate']]
    if len(source_jobs)!=204 or len(set(source_jobs))!=204:raise ValueError('C4.4 canonical job list is not 204 unique jobs')
    if set(s['siteId'] for s in sites)!=set(byid):raise ValueError('C1 and C4.4 site IDs mismatch')
    site_rows=[];job_rows=[];site_json=[];work_rows=[];flags=[];marker_map={}
    cats=collections.Counter();modes=collections.Counter();tags=collections.Counter()
    total_pop=total_work=0
    for s in sites:
        p=byid[s['siteId']];c=s['buildingDemand'];anchor=s['workingAnchor'];pop=int(c['peopleTarget']);work=int(c['workforceTarget'])
        if pop!=p['originalC1Population'] or work!=p['originalC1WorkforceEstimate']:
            raise ValueError('C1 / C4.4 headcount mismatch: '+s['siteId'])
        counts=p['jobWorkerCounts']
        if set(counts)-set(source_jobs):raise ValueError('Unrecognized occupation in '+s['siteId'])
        if sum(counts.values())!=work:raise ValueError('Workforce not conserved: '+s['siteId'])
        if sum(p['sectorWorkerCounts'].values())!=work:raise ValueError('Sector total not conserved: '+s['siteId'])
        if (pop==0)!=(work==0):raise ValueError('Zero population/workforce mismatch: '+s['siteId'])
        cats[s['tier']]+=1;modes[s['blueprintMode']]+=1;tags[marker_tag(s)]+=1
        total_pop+=pop;total_work+=work
        markers=s['markerIds'];
        for m in markers:
            if m in marker_map:raise ValueError('Duplicate map marker: '+m)
            marker_map[m]={'id':m,'siteId':s['siteId'],'tag':marker_tag(s),'tier':s['tier'],
                           'mode':s['blueprintMode'],'name':s['name']}
        houses=int(c['housingBuildingsDemand']); households=int(c['householdsEstimated']);units=int(c['housingUnitsPerBuilding']);
        if houses*units<households:raise ValueError('Insufficient theoretical housing units: '+s['siteId'])
        base=[s['siteId'],s['name'],s['primaryMarkerId'],'|'.join(markers),s['tier'],marker_tag(s),
              s['blueprintMode'],int(anchor['x']),int(anchor['z']),pop,work,pop-work,households,houses,units,
              len([n for n in counts.values() if n>0]),sum(d['workplacesRequested'] for d in p['buildingDemand'].values()),
              anchor.get('status',''),anchor.get('isBuildApproved',False)]
        site_rows.append(base)
        job_rows.append(base[:11]+[counts.get(j,0) for j in source_jobs])
        d={key:{'workers':int(v['workers']),'requested':int(v['workplacesRequested']),
                'capacityPerUnit':int(v['maxWorkersPerWorkplaceAssumption'])} for key,v in p['buildingDemand'].items()}
        for kind,v in sorted(d.items()):
            work_rows.append([s['siteId'],s['primaryMarkerId'],s['name'],s['tier'],kind,v['workers'],v['requested'],v['capacityPerUnit'],
                             'DEMAND_ONLY_NOT_CONFIRMED_CONSTRUCTED'])
        site_json.append({'siteId':s['siteId'],'name':s['name'],'markerIds':markers,'tier':s['tier'],'atlasTag':marker_tag(s),
                          'blueprintMode':s['blueprintMode'],'workingAnchor':anchor,'populationPlan':pop,
                          'workerPlan':work,'nonWorkerPopulationEstimate':pop-work,
                          'estimatedHouseholds':households,'housingBuildingsDemand':houses,
                          'housingUnitsPerBuilding':units,
                          'jobs':{j:counts.get(j,0) for j in source_jobs},'buildings':d,
                          'planningOnly':True,'constructedStructuresAudited':False,'npcRosterAssigned':False})
        if s['blueprintMode']!='GENERAL_COMPACT_SETTLEMENT' or s['tier'] in ('MAJOR','SPECIAL'):
            flags.append([s['siteId'],s['primaryMarkerId'],s['name'],s['tier'],s['blueprintMode'],int(anchor['x']),int(anchor['z']),
                          pop,'REQUIRES_SITE_POLICY_OR_MAJOR_STREAMING','NOT_A_FAILURE_LIST'])
    if len(marker_map)!=253:raise ValueError(f'Expected 253 map observations, found {len(marker_map)}')
    if total_pop!=3760684 or total_work!=1880341:raise ValueError('C4.4 scenario version changed: check expected totals')
    if sum(1 for s in site_json if s['populationPlan']>0)!=198:raise ValueError('Inhabited-site total changed')
    if cats!=collections.Counter({'SMALL':41,'MEDIUM':95,'MAJOR':47,'SPECIAL':65}):raise ValueError('Unexpected tier counts')
    return {'siteRows':site_rows,'jobRows':job_rows,'siteJson':site_json,'workRows':work_rows,
            'flagRows':flags,'markers':sorted(marker_map.values(),key=lambda x:x['id']),
            'jobs':source_jobs,'summary':{'sourceVersion':'C1 + C4.4 (2026-10-22)','sites':248,'mapMarkers':253,
                'inhabited':198,'uninhabited':50,'plannedPopulation':total_pop,'plannedWorkers':total_work,
                'originalJobRows':206,'canonicalJobIds':204,'tierCounts':dict(sorted(cats.items())),
                'blueprintModes':dict(sorted(modes.items())),'jobTotalMatchesEverySite':True,
                'housingDemandNotYetActualHouseholds':True,'workplacesAreDemandNotBuilt':True,
                'terrainApprovedWorldSites':0,'generatedIndividualNpcsFromThisExporter':0,
                'sourceC1SHA256':hashlib.sha256(C1.read_bytes()).hexdigest(),
                'sourceC44SHA256':hashlib.sha256(JOBS.read_bytes()).hexdigest()}}

def export(result):
    OUT.mkdir(parents=True,exist_ok=True)
    base=['site_id','name','primary_marker','all_markers','tier','atlas_tag','blueprint_mode',
          'working_x','working_z','planned_people','planned_workers','other_people','estimated_households',
          'houses_requested','units_per_house_building','nonzero_job_classes','workplace_buildings_requested',
          'anchor_status','build_approved']
    csv_write(OUT/'C4_8_SITE_POPULATION_HOUSING_248.csv',base,result['siteRows'])
    csv_write(OUT/'C4_8_ALL_248_SITES_204_JOBS.csv',base[:11]+result['jobs'],result['jobRows'])
    csv_write(OUT/'C4_8_WORKPLACE_DEMAND_248.csv',
              ['site_id','marker','name','tier','building_type','workers','requested_workplaces','capacity_per_unit','status'],result['workRows'])
    csv_write(OUT/'C4_8_MAJOR_SPECIAL_REVIEW.csv',
              ['site_id','marker','name','tier','mode','working_x','working_z','population','review_reason','is_failed_construction'],result['flagRows'])
    atlas={'schemaVersion':1,'source':'C1_MARKER_GROUPS_2026_10_22','classificationOnly':True,
           'markers':result['markers']}
    ASSET.parent.mkdir(parents=True,exist_ok=True)
    ASSET.write_text(json.dumps(atlas,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (OUT/'C4_8_WORLDWIDE_SITE_REGISTRY.json').write_text(json.dumps({
        'schemaVersion':1,'status':'PLANNING_ONLY_NO_NPC_INSTANCES_OR_BUILD_APPROVAL',
        'summary':result['summary'],'sites':result['siteJson']},ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
    (OUT/'C4_8_AUDIT_SUMMARY.json').write_text(json.dumps(result['summary'],indent=2)+'\n',encoding='utf-8')
    (OUT/'C4_8_README.md').write_text("""# C4.8 — Versioned worldwide settlement reference (298 AC)

These are **planning targets**, not existing simulated people, actually constructed homes, or surveyed lots.

- Source A: `settlement_presets_c1_v1.json` — accepted working X/Z, tiers, estimated households, housing requirements.
- Source B: `settlement_job_distributions_c4_4_v2.json` — 248 × 204 profession counts, labor and requested workplaces.
- Source C: `occupation_building_catalog_c4_4_v2.json` — job-to-workplace policy and job metadata.
- No new professions or people invented here. Every C4.4 job count and C1 population retained.
- 253 markers may reference 248 unique sites. **Do not duplicate population for co-located markers.**
- 50 sites have zero population; these generally include ruins/landmarks/unverified places.
- C4.4 adjusted seven regional economic mixes and inferred shares for most job titles; all remain MOD_ESTIMATE.
- 1,880,341 workers is a modeled ~50% share of 3,760,684 people, not 1,880,341 currently instantiated NPCs.
- Zero columns in the 248×204 matrix mean no default workers, not a deleted job.
- `working_x` / `working_z` come from accepted map pins; actual build position may require an approved nearby land point.
- `MAJOR_SPECIAL_REVIEW` is a **planning list**, NOT a record of failed construction.
- Winterfell V3 has 1,136 occupied residential houses; generic C1 estimates only 266 Major-tier buildings.
- Housing demand is a model; actual household graph, employer/owner links, and unbuilt lots require reconciliation.
- Exterior atlas labels are derived only from marker IDs, not from game-generated structure positions.
""",encoding='utf-8')
    print(json.dumps(result['summary'],indent=2))
    print('Created C4.8 reference under',OUT)
    print('Created atlas tier tags at',ASSET)

def main():
    result=make();export(result)
if __name__=='__main__':main()
