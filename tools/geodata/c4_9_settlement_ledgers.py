#!/usr/bin/env python3
"""C4.9: read-only C4.8 -> versioned site/household/workplace reservation records.

This generator never edits the mod resources, GitHub, world saves or existing
NPC identities. It does not create Minecraft entities or authorize a build.
"""
from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import json
import math
from pathlib import Path
import random
import tempfile

SOURCE = Path('docs/reference/c4_8_worldwide')
OUTPUT = Path('tools/geodata/output/c4_9_settlement_ledgers')
SCHEMA = 'knownworld.c4_9.planning.v1'
# Keep exact source building requirements. These choices select ONLY the core.
CORE = {
    'SMALL': ('MANOR_HALL', 'PALISADE_WITH_GATE', 'CIVIC_CORE_SMALL'),
    'MEDIUM': ('KEEP', 'STONE_CURTAIN_WITH_GATE', 'CIVIC_CORE_MEDIUM'),
    'MAJOR': ('MAIN_CASTLE', 'INNER_AND_OUTER_CURTAINS_WITH_GATES', 'CIVIC_CORE_MAJOR'),
    'SPECIAL': ('SITE_SPECIFIC_CORE', 'SITE_SPECIFIC', 'EXCEPTION_REVIEW'),
}
BLOCKED_MODES = {'RUIN_OR_LANDMARK_ONLY', 'UNVERIFIED_NO_CONSTRUCTION',
                 'MOBILE_NO_FIXED_FOOTPRINT'}
WINTERFELL = 'site_nw_029'
WINTERFELL_HOUSEHOLDS = 1136
WINTERFELL_HOUSES = 1136


def rows(path: Path):
    with path.open('r', encoding='utf-8-sig', newline='') as stream:
        yield from csv.DictReader(stream, delimiter=';')


def number(v, key):
    try:
        k = int(v[key])
    except (KeyError, TypeError, ValueError) as err:
        raise ValueError(f'Invalid integer {key} in {v!r}') from err
    if k < 0:
        raise ValueError(f'Negative {key}: {k}')
    return k


def stable_seed(site: str, purpose: str) -> int:
    return int.from_bytes(hashlib.sha256(f'{SCHEMA}:{site}:{purpose}'.encode()).digest()[:8], 'big')


def household_sizes(population: int, households: int, seed: int):
    """Exact conservation, deterministic variable sizes, bounded to 1..10 when feasible.

    This is a synthetic household *capacity* forecast, NOT a married-family graph
    and never an NpcId. Generation is O(households), with no arbitrary city cap.
    """
    if population == households == 0:
        return
    if not (1 <= households <= population):
        raise ValueError('Nonzero population requires 1..population households')
    if population > households * 10:
        raise ValueError('Household average >10; requires special collective-housing policy')
    rng = random.Random(seed)
    # Baseline 1 member, then distribute remaining people using variable weights.
    # A few common large-family households receive more weight; no demographic
    # parents/children or political identities are asserted by these slots.
    weights = [rng.triangular(0.3, 2.5, 1.15) for _ in range(households)]
    total_w = sum(weights)
    capacity = [1] * households
    extras = population - households
    # Largest remainder allocation, then redistribute any >10 overages.
    raw = [extras * w / total_w for w in weights]
    floors = [min(9, int(x)) for x in raw]
    remaining = extras - sum(floors)
    for i, n in enumerate(floors):
        capacity[i] += n
    # Deterministic rounds. Typically one round; avoids O(extra people).
    while remaining:
        candidates = [i for i, n in enumerate(capacity) if n < 10]
        if not candidates:
            raise ValueError('Household slots have no remaining capacity')
        candidates.sort(key=lambda i: (raw[i] - math.floor(raw[i]), weights[i], -i), reverse=True)
        n = min(remaining, len(candidates))
        for i in candidates[:n]:
            capacity[i] += 1
        remaining -= n
    rng.shuffle(capacity)
    assert sum(capacity) == population
    return capacity


def core_for(site):
    core, wall, shape = CORE[site['tier']]
    if number(site, 'planned_people') == 0 or site['blueprint_mode'] in BLOCKED_MODES:
        return {'required': False, 'template': None, 'wallPolicy': None,
                'layout': 'NO_AUTOMATIC_POPULATED_CORE'}
    return {'required': True, 'template': core, 'wallPolicy': wall,
            'layout': shape, 'count': 1,
            'implementation': 'PLAN_ONLY_REQUIRES_PHYSICAL_BLUEPRINT_AND_TERRAIN_APPROVAL'}


def build(sites, work, jobs, out: Path, *, include_slot_files=True, strict_total=False):
    site_ids = [x['site_id'] for x in sites]
    if len(site_ids) != len(set(site_ids)):
        raise ValueError('Duplicate physical site ID')
    by_work = collections.defaultdict(list)
    for w in work:
        if w['site_id'] not in site_ids:
            raise ValueError('Workplace without site: '+w['site_id'])
        by_work[w['site_id']].append(w)
    by_jobs = {}
    for j in jobs:
        sid = j['site_id']
        if sid in by_jobs: raise ValueError('Duplicate 204-job site '+sid)
        by_jobs[sid] = j
    if set(site_ids) != set(by_jobs):
        raise ValueError('C4.8 204-job matrix and site keys differ')
    if strict_total and len(sites) != 248:
        raise ValueError('Expected all 248 C4.8 physical sites')
    out.mkdir(parents=True, exist_ok=True)
    summary = []
    deficits = []
    totals = collections.Counter()
    hh_path = out / 'C4_9_HOUSEHOLD_RESERVATIONS.jsonl'
    wp_path = out / 'C4_9_WORKPLACE_RESERVATIONS.jsonl'
    # Atomic per-output files: avoid partial, published ledgers upon exception.
    with tempfile.TemporaryDirectory(dir=out) as temporary:
        temp = Path(temporary)
        hh = (temp/'household.jsonl').open('w', encoding='utf-8') if include_slot_files else None
        wp = (temp/'workplace.jsonl').open('w', encoding='utf-8') if include_slot_files else None
        try:
            for site in sites:
                sid = site['site_id']
                people = number(site, 'planned_people')
                workers = number(site, 'planned_workers')
                baseline_hh = number(site, 'estimated_households')
                baseline_houses = number(site, 'houses_requested')
                units_per_building = number(site, 'units_per_house_building')
                if workers > people:
                    raise ValueError('Workers exceed population '+sid)
                occupation_names = [key for key in by_jobs[sid] if key not in
                    ('site_id','name','primary_marker','all_markers','tier','atlas_tag',
                     'blueprint_mode','working_x','working_z','planned_people','planned_workers')]
                occupation_counts = {key: number({'value': by_jobs[sid][key]},'value')
                                     for key in occupation_names}
                if sum(occupation_counts.values()) != workers:
                    raise ValueError('204 occupation counts do not conserve workers '+sid)
                # Winterfell already has stable authoritative IDs; do not fabricate
                # conflicting replacement household IDs.
                authored = sid == WINTERFELL
                households = WINTERFELL_HOUSEHOLDS if authored else baseline_hh
                homes = WINTERFELL_HOUSES if authored else baseline_houses
                units = 1 if authored else units_per_building
                if (people == 0 and (households or homes or workers)) or (people > 0 and households == 0):
                    raise ValueError('Population/household mismatch '+sid)
                if homes * units < households:
                    raise ValueError('Housing units insufficient by planning arithmetic '+sid)
                site_work = sorted(by_work[sid], key=lambda row: row['building_type'])
                expected_sites_worker_sum = sum(number(w,'workers') for w in site_work)
                if expected_sites_worker_sum != workers:
                    raise ValueError('Worker categories do not reconcile '+sid)
                requested = sum(number(w,'requested_workplaces') for w in site_work)
                if requested != number(site,'workplace_buildings_requested'):
                    raise ValueError('Workplace totals do not reconcile '+sid)
                types = []
                core = core_for(site)
                core_keep_workers = 0
                for w in site_work:
                    kind = w['building_type']
                    count = number(w, 'requested_workplaces')
                    slots = number(w, 'capacity_per_unit')
                    assigned = number(w, 'workers')
                    if count and slots <= 0: raise ValueError(f'No capacity {sid}/{kind}')
                    if assigned > count * slots:
                        if kind == 'KEEP' and count == 0 and core['required']:
                            core_keep_workers += assigned  # staffed IN central core
                        else:
                            raise ValueError(f'Unresolved workplace demand {sid}/{kind}')
                    types.append({'kind':kind,'workersTarget':assigned,
                                  'requestedBuildings':count,'slotsPerBuilding':slots})
                    if wp is not None and not authored:
                        # Fill only the last workplace partially. No staff is yet a named NPC.
                        remaining = assigned
                        for ix in range(count):
                            positions = min(slots, remaining)
                            remaining -= positions
                            record = {'siteId':sid,'workplaceId':f'{sid}:work:{kind}:{ix+1:06d}',
                                      'kind':kind,'reservedWorkerSlots':positions,
                                      'physicalStaffCapacity':slots,'ownerNpcId':None,
                                      'buildingLotId':None,'constructionState':'PLANNED',
                                      'bindingState':'NO_NPCS_ASSIGNED'}
                            wp.write(json.dumps(record,ensure_ascii=False,separators=(',',':'))+'\n')
                        if remaining and kind != 'KEEP':
                            raise ValueError(f'Unassigned worker quota {sid}/{kind}')
                if core_keep_workers:
                    core['reservedKeepWorkerSlots'] = core_keep_workers
                sizes = (household_sizes(people,households,stable_seed(sid,'households'))
                         if not authored else None)
                size_histogram = (dict(sorted(collections.Counter(sizes or ()).items()))
                                  if not authored else None)
                if hh is not None and not authored:
                    for ix, count in enumerate(sizes or (),1):
                        hh.write(json.dumps({'siteId':sid,
                                 'householdReservationId':f'{sid}:hh:{ix:06d}',
                                 'membersTarget':count,'membersAssigned':0,
                                 'familyGraphState':'NOT_AUTHORED',
                                 'houseLotId':None,'ownerNpcId':None,
                                 'physicalAddress':None,'status':'PLANNED_NOT_SIMULATED'},
                                 ensure_ascii=False,separators=(',',':'))+'\n')
                housing_units = homes*units
                entries = {
                    'schemaVersion':SCHEMA,'siteId':sid,'name':site['name'],
                    'markerIds':site['all_markers'].split('|'), 'tier':site['tier'],
                    'blueprintMode':site['blueprint_mode'],
                    'mapAnchor':{'x':int(site['working_x']),'z':int(site['working_z'])},
                    'constructionAnchor':None,'inlandExpansionVector':None,
                    'populationTarget':people,'workingPopulationTarget':workers,
                    'nonworkingPopulationTarget':people-workers,
                    'householdsTarget':households,
                    'householdSizeHistogramForecast':size_histogram,
                    'multiPersonHouseholdCandidates':(sum(n for size,n in (size_histogram or {}).items() if size>=2)
                                                      if not authored else None),
                    'residentialBuildingsTarget':homes,
                    'residentialUnitsPerBuilding':units,
                    'residentialUnitsTarget':housing_units,
                    'unoccupiedResidentialUnitReserve':housing_units-households,
                    'workplaceBuildingsTarget':requested,'buildingDemandByKind':types,
                    'occupationTargetsNonzero':{name:n for name,n in occupation_counts.items() if n>0},
                    'originalOccupationCount':len(occupation_names),
                    'mainComplex':core,
                    'deferredPhysicalPlacement':True,'terrainStatus':'NOT_SURVEYED',
                    'constructionState':'PLANNED_NOT_BUILT',
                    'populationStatus':'TARGET_ONLY_NOT_INSTANTIATED',
                    'authoredWinterfellPreserved':authored,
                    'workplaceSlotSource':('WINTERFELL_V3_AUTHORED_LOT_IDS_RECONCILE_BEFORE_ANY_CREATION'
                                           if authored else 'C4_9_WORKPLACE_RESERVATIONS'),
                    'existingWinterfellPlannedWorkplots':777 if authored else None,
                    'householdSlotSource':('WINTERFELL_V2_AUTHORED_IDS_DO_NOT_RECREATE'
                                           if authored else 'C4_9_HOUSEHOLD_RESERVATIONS'),
                    'jobReference':'C4_8_ALL_248_SITES_204_JOBS.csv',
                    'sourcePlanningProvenance':'C1_C4_4_C4_8_MOD_ESTIMATE',
                }
                summary.append(entries)
                totals.update(people=people,workers=workers,households=households,
                              homes=homes,workplaces=requested,
                              keepWorkersAssignedToCore=core_keep_workers)
                if site['blueprint_mode'] not in ('GENERAL_COMPACT_SETTLEMENT',):
                    deficits.append({'siteId':sid,'name':site['name'],
                                    'markerIds':entries['markerIds'],
                                    'reason':'REQUIRES_SITE_BLUEPRINT_POLICY',
                                    'mode':site['blueprint_mode'],
                                    'plannedPeople':people,
                                    'homesDeferred':homes,'workplacesDeferred':requested,
                                    'notActualTerrainFailure':True})
                if not core['required'] and people > 0:
                    deficits.append({'siteId':sid,'name':site['name'],
                                     'markerIds':entries['markerIds'],
                                     'reason':'NO_FIXED_CORE_MOBILE_OR_UNVERIFIED',
                                     'homesDeferred':homes,'workplacesDeferred':requested,
                                     'notActualTerrainFailure':True})
            if strict_total and (totals['people'],totals['workers']) != (3760684,1880341):
                raise ValueError('C4.8 aggregate headcount drift')
        finally:
            if hh: hh.close()
            if wp: wp.close()
        (temp/'sites.json').write_text(json.dumps({'schemaVersion':SCHEMA,'status':'PLANNING_ONLY',
             'sites':summary},ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        (temp/'summary.json').write_text(json.dumps({'schemaVersion':SCHEMA,'siteCount':len(sites),
             'totals':dict(totals),'existingWinterfellHouseholdsReused':WINTERFELL_HOUSEHOLDS,
             'terrainApprovedSites':0,'constructedWorldSitesFromThisBatch':0,
             'newNpcsCreated':0,'staffAssignedToNpcIds':0,
             'workplaceSlotsAreNotJobsOrIndividuals':True,'status':'PLANNING_ONLY'},
             ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        (temp/'exceptions.json').write_text(json.dumps(deficits,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        files = [('sites.json','C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json'),
                 ('summary.json','C4_9_SUMMARY.json'),
                 ('exceptions.json','C4_9_TEMPLATE_POLICY_EXCEPTIONS.json')]
        if include_slot_files:
            files += [('household.jsonl',hh_path.name),('workplace.jsonl',wp_path.name)]
        for src,dst in files:
            (temp/src).replace(out/dst)
    return dict(totals)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output',type=Path,default=None)
    parser.add_argument('--summary-only',action='store_true',help='Omit detailed reservation ledgers')
    args=parser.parse_args()
    root=args.repo.resolve()
    source=root/SOURCE
    output=(args.output or root/OUTPUT).resolve()
    sites=list(rows(source/'C4_8_SITE_POPULATION_HOUSING_248.csv'))
    work=list(rows(source/'C4_8_WORKPLACE_DEMAND_248.csv'))
    jobs=list(rows(source/'C4_8_ALL_248_SITES_204_JOBS.csv'))
    print(json.dumps(build(sites,work,jobs,output,
         include_slot_files=not args.summary_only,strict_total=True),sort_keys=True))
    print('Generated PLANNING ledgers only in',output)

if __name__=='__main__':
    main()
