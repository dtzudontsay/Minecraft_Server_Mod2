#!/usr/bin/env python3
"""C4.1: assemble complete *connected first districts* for the TWO C2 pilot sites.

Reads B2/B3->C1 demand, C2 explicit pilot selection, and C3 templates.
Writes a separate C4 resource consumed by the existing C2 chunk-local painter.
No server/world writes, new sites, people, jobs, or authorizations.
"""
from __future__ import annotations
import argparse
import collections
import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac'
C1 = BASE / 'settlement_presets_c1_v1.json'
C2 = BASE / 'settlement_construction_c2_v1.json'
C3 = BASE / 'architecture_templates_c3_v1.json'
OUTPUT = BASE / 'settlement_construction_c4_v1.json'
REPORT = ROOT / 'tools/geodata/output/c4_settlements/C4_LAYOUT_SUMMARY.json'

# C4.1 stays deliberately below the worldwide/full-city construction stage.
# Higher C1 housing demands become explicit backlog, NOT fictional homes.
LIMITS = {
    'SMALL': {'houses': 48, 'workplaces': 18, 'farms': 4, 'lanes': 3, 'columns': 11},
    'MEDIUM': {'houses': 180, 'workplaces': 45, 'farms': 8, 'lanes': 5, 'columns': 23},
}
SPACING_X, SPACING_Z = 34, 80
SAFE_OUTDOOR = {'WELL', 'COURTYARD', 'GARDEN', 'PASTURE', 'GUARD_TOWER', 'GRANARY'}
RURAL = {'FARM', 'GARDEN', 'PASTURE'}


def read(p: Path) -> dict:
    return json.loads(p.read_text(encoding='utf-8-sig'))


def around(a: int, b: int):
    return range(min(a, b), max(a, b) + 1)


def rectangular_road(road: set, x0: int, x1: int, z0: int, z1: int):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            road.add((x, z))


def place(lots: list, site: str, kind: str, x: int, z: int, w: int, d: int,
          face: str, capacity: int, staff: int):
    if not 5 <= w <= 33 or not 5 <= d <= 33:
        raise ValueError(f'{kind}: unsupported C2 footprint {w}x{d}')
    if face not in ('NORTH', 'SOUTH'):
        raise ValueError(f'{site}: unsupported doorway orientation {face}')
    ordinal = sum(l['kind'] == kind for l in lots)
    lot = {'id': f'{site}_c4_{kind.lower()}_{ordinal:04d}', 'kind': kind,
           'x': x, 'z': z, 'width': w, 'depth': d, 'face': face,
           'capacity': capacity, 'staff': staff,
           'householdId': None, 'plannedOnlyHouseholdName': kind == 'HOUSE'}
    for other in lots:
        if (x < other['x'] + other['width'] and other['x'] < x + w
                and z < other['z'] + other['depth'] and other['z'] < z + d):
            raise ValueError(f'Overlapping C4 plots: {lot["id"]} / {other["id"]}')
    lots.append(lot)
    return lot


def build(c1: dict, c2: dict, c3: dict) -> dict:
    if c1['summary']['markerCount'] != 253 or c1['summary']['holdingCount'] != 259:
        raise ValueError('C1 worldwide authority must retain 253 markers and 259 holdings')
    if (c2['schemaVersion'] != 1 or c2['summary']['pilotCount'] != 2
            or c2['summary']['allMapMarkers'] != 253
            or c2['summary']['allPoliticalHoldings'] != 259
            or not c2['policy']['pilotsOnly']):
        raise ValueError('C2 must be strict two-site resource; no worldwide authorization')
    if c3['templateCount'] != 43 or not c3['doesNotAuthoriseNewSites']:
        raise ValueError('C3 architecture catalog incomplete or unsafe')
    templates = {t['id']: t for t in c3['templates']}
    sites = {s['siteId']: s for s in c1['physicalSitePresets']}
    if len(sites) != c1['summary']['physicalSiteCount']:
        raise ValueError('Duplicate or lost C1 physical site')
    plans = []
    seen_markers = set()
    for old in c2['pilots']:
        sid, tier = old['siteId'], old['tier']
        if (tier not in LIMITS or sid not in sites
                or old['mode'] != 'GENERAL_COMPACT_SETTLEMENT'
                or sites[sid]['blueprintMode'] != old['mode']
                or 'NW-029' in old['markerIds']):
            raise ValueError(f'Unauthorized/special/Winterfell pilot: {sid}')
        original = sites[sid]
        if (any(int(old['anchor'][a]) != int(original['workingAnchor'][a]) for a in ('x','z'))
                or sorted(old['markerIds']) != sorted(original['markerIds'])):
            raise ValueError(f'C4 refuses to move or rename C2 map anchor: {sid}')
        if any(m in seen_markers for m in old['markerIds']):
            raise ValueError('Pilot map markers overlap')
        seen_markers.update(old['markerIds'])
        lim = LIMITS[tier]
        demand = original['buildingDemand']
        x0, z0 = old['anchor']['x'], old['anchor']['z']
        houses = min(int(demand['housingBuildingsDemand']), lim['houses'])
        if houses < 3:
            raise ValueError(f'C4 pilot has no housing demand: {sid}')
        # Do not add non-existent jobs: zero workers means zero job workplaces.
        workplaces = collections.Counter()
        for kind, value in sorted(demand['firstPhaseWorkplaceCounts'].items()):
            t = templates.get(kind)
            if (not t or t['requiresShore'] or t['requiresMountainResource']
                    or t['requiresPolicy'] or kind in RURAL):
                continue
            if kind in ('KEEP', 'GREAT_HALL', 'MONUMENT', 'RUIN', 'CAMP'):
                continue  # C4 special authoring / C6 faction authority
            workplaces[kind] = min(int(value), 10 if tier == 'MEDIUM' else 5)
        # Civic infrastructure is already demanded by C1. Do not invent a court.
        if tier == 'MEDIUM' and int(demand['firstPhaseWorkplaceCounts'].get('GUARD_TOWER', 0)):
            workplaces['GUARD_TOWER'] = 1
        # These extras never introduce simulated staff.
        wishlist = []
        priority = ['WELL', 'GRANARY', 'BAKERY', 'MARKET', 'SMITHY', 'INN',
                    'TEMPLE', 'ADMINISTRATION', 'BARRACKS', 'ARMORY', 'GUARD_TOWER']
        for kind in priority + sorted(set(workplaces) - set(priority)):
            wishlist.extend([kind] * workplaces[kind])
        wishlist = wishlist[:lim['workplaces']]
        farms = min(lim['farms'], int(demand['workplaceCounts'].get('FARM', 0)))
        # Connected street blocks, not detached Winterfell-style satellite villages.
        capacity = lim['lanes'] * lim['columns'] * 2
        if houses + len(wishlist) > capacity:
            houses = max(3, capacity - len(wishlist))
        ordered = (['WELL', 'GRANARY'] + [w for w in wishlist if w not in ('WELL', 'GRANARY')])
        # Put businesses near the main street by allocating its first columns.
        slots = []
        for lane in range(lim['lanes']):
            gz = z0 + (lane - lim['lanes'] // 2) * SPACING_Z
            for col in range(lim['columns']):
                cx = x0 + (col - (lim['columns'] - 1) // 2) * SPACING_X
                for side in ('SOUTH', 'NORTH'):
                    slots.append((lane, col, side, cx, gz))
        slots.sort(key=lambda v: (abs(v[0] - lim['lanes']//2),
                                  abs(v[1] - (lim['columns']-1)//2),v[1],v[0],v[2]))
        central = sorted(slots[:max(len(wishlist)*4, len(wishlist))],
                         key=lambda v:(abs(v[0]-lim['lanes']//2), abs(v[1]-(lim['columns']-1)//2),v[2]))
        used = set()
        arrangement = []
        for slot in central:
            if len(arrangement) >= len(wishlist):
                break
            arrangement.append((slot, wishlist[len(arrangement)]))
            used.add(slot)
        remaining = [v for v in slots if v not in used]
        arrangement.extend((v, 'HOUSE') for v in remaining[:houses])
        lots = []
        for (_, _, side, cx, road_z), kind in arrangement:
            if kind == 'HOUSE':
                t = templates['HOUSE_MEDIUM' if tier == 'MEDIUM' else 'HOUSE_SMALL']
                w, d = min(17, int(t['c1Width'])), min(17, int(t['c1Depth']))
                cap, staff = (10 if tier == 'MEDIUM' else 5), 0
            else:
                t = templates[kind]
                w, d = int(t['c1Width']), int(t['c1Depth'])
                cap, staff = 0, int(t['maxStaff'])
            # The street itself is always clear; doors face its near edge.
            z = road_z + 9 if side == 'NORTH' else road_z - 9 - d
            place(lots, sid, kind, cx - w // 2, z, w, d, side, cap, staff)
        # Rural workplaces and field annexes lie beyond the SOUTHERNMOST block.
        south = z0 - (lim['lanes']//2) * SPACING_Z
        farm_road = south - 100
        for j in range(farms):
            t = templates['FARM']
            w, d = int(t['c1Width']), int(t['c1Depth'])
            fx = x0 + (j - (farms - 1)//2) * 37
            place(lots, sid, 'FARM', fx-w//2, farm_road - 8 - d, w, d,
                  'SOUTH', 0, int(t['maxStaff']))
            # FIELD uses the legacy C2 physical farmland painter (not C3).
            place(lots, sid, 'FIELD', fx - 8, farm_road - 18 - d - 32,
                  17, 26, 'NORTH', 0, 0)
        # Every street is connected at both ends; doors connect by short paths.
        roads: set[tuple[int, int]] = set()
        cx_values = [x0 + (col - (lim['columns'] - 1)//2) * SPACING_X for col in range(lim['columns'])]
        left, right = min(cx_values)-19, max(cx_values)+19
        street_zs = [z0+(lane-lim['lanes']//2)*SPACING_Z for lane in range(lim['lanes'])]
        for rz in street_zs:
            rectangular_road(roads, left, right, rz-2, rz+2)
        # Perimeter connectors are outside residential parcels.
        for rx in (left, right):
            rectangular_road(roads, rx-1, rx+1, min(street_zs), max(street_zs))
        if farms:
            rectangular_road(roads, left, right, farm_road-2, farm_road+2)
            rectangular_road(roads, right-1, right+1, farm_road, min(street_zs))
        for j in range(farms):
            fx = x0 + (j - (farms - 1)//2) * 37
            fld = next(l for l in lots if l['id'].endswith(f'_field_{j:04d}'))
            # Follow the eastern edge from the farm avenue to the field, with
            # no road blocks running through the irrigated soil.
            rectangular_road(roads, fx+12, fx+14, fld['z']+fld['depth']//2, farm_road)
        for l in lots:
            if l['kind'] == 'FIELD':
                continue
            door_x = l['x'] + l['width']//2
            door_z = l['z'] - 1 if l['face'] == 'NORTH' else l['z'] + l['depth']
            nearest = farm_road if l['kind']=='FARM' else min(street_zs,key=lambda z:abs(z-door_z))
            rectangular_road(roads,door_x-1,door_x+1,door_z,nearest)
        # Explicitly reject accidental road/lot intersections: do not silently
        # trim them, which would conceal an inaccessible plot.
        occupied = {(xx, zz) for l in lots for xx in range(l['x'],l['x']+l['width'])
                    for zz in range(l['z'],l['z']+l['depth'])}
        collision = occupied.intersection(roads)
        if collision:
            raise ValueError(f'{sid}: {len(collision)} road blocks pass through buildings')
        # Connectivity check in road-pixel space, not just a schematic assertion.
        start = min(roads)
        visit, queue = {start}, collections.deque([start])
        while queue:
            xx, zz = queue.popleft()
            for v in ((xx-1,zz),(xx+1,zz),(xx,zz-1),(xx,zz+1)):
                if v in roads and v not in visit:
                    visit.add(v);queue.append(v)
        if len(visit) != len(roads):
            raise ValueError(f'{sid}: disconnected road pixels {len(roads)-len(visit)}')
        chunk_index = collections.defaultdict(lambda:{'r':set(),'b':set()})
        for xx,zz in roads:
            cx,cz = xx//16,zz//16
            chunk_index[(cx,cz)]['r'].add((zz-cz*16)*16+(xx-cx*16))
        for idx,l in enumerate(lots):
            for cx in range(l['x']//16,(l['x']+l['width']-1)//16+1):
                for cz in range(l['z']//16,(l['z']+l['depth']-1)//16+1):
                    chunk_index[(cx,cz)]['b'].add(idx)
        min_x = min(xx for xx,zz in roads); max_x=max(xx for xx,zz in roads)
        min_z = min(zz for xx,zz in roads); max_z=max(zz for xx,zz in roads)
        min_x = min(min_x, *(l['x'] for l in lots))
        max_x = max(max_x, *(l['x']+l['width']-1 for l in lots))
        min_z = min(min_z, *(l['z'] for l in lots))
        max_z = max(max_z, *(l['z']+l['depth']-1 for l in lots))
        new = copy.deepcopy(old)
        new['lots'] = lots
        new['chunks'] = [{'cx':cx,'cz':cz,'r':sorted(v['r']),'b':sorted(v['b'])}
                         for (cx,cz),v in sorted(chunk_index.items())]
        new['bounds'] = [min_x-44,max_x+44,min_z-44,max_z+44]
        new['planningPeople'] = int(demand['peopleTarget'])
        new['planningHomesNeeded'] = int(demand['housingBuildingsDemand'])
        new['plannedHomesHere'] = houses
        new['housingBacklogAtLeast'] = max(0,int(demand['housingBuildingsDemand'])-houses)
        new['c4'] = {'phase':'C4_1_COMPACT_FIRST_DISTRICTS', 'realBuildingsPlanned':len(lots),
                     'homes':houses,'housingCapacityEstimate':houses*(10 if tier=='MEDIUM' else 5),
                     'workplaces':dict(sorted(collections.Counter(l['kind'] for l in lots
                                      if l['kind'] not in ('HOUSE','FIELD')).items())),
                     'farmFields':farms,'roadsConnectedInPlan':True,
                     'roadsTerrainApproved':False,'plotYApproved':False,
                     'actualNpcsCreated':0,'actualWorkersAssigned':0,
                     'firstPhaseNotFullSettlementCensus':True,
                     'householdNamesNotInvented':True}
        plans.append(new)
    if len(plans)!=2 or {p['siteId'] for p in plans}!={p['siteId'] for p in c2['pilots']}:
        raise ValueError('C4 cannot change pilot identities')
    result = copy.deepcopy(c2)
    result['purpose']='C4_1_COMPACT_FIRST_DISTRICTS_C2_PAINTER'
    result['pilots']=plans
    result['c4Policy']={'explicitTwoC2PilotsOnly':True,'winterfellUntouched':True,
        'noNewResidentsOrWorkforce':True,'noExistingChunkModification':True,
        'rejectedSitesRequireNoGeneration':True,'worldwideDeploymentNotAuthorized':True}
    return result


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args()
    result=build(read(C1),read(C2),read(C3))
    payload=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8')!=payload:
            raise SystemExit('C4 resource missing/stale; rerun C4 builder')
        print('C4 CHECK PASSED: deterministic pilot districts and 253/259 authority preserved')
        return
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    OUTPUT.write_text(payload,encoding='utf-8')
    REPORT.parent.mkdir(parents=True,exist_ok=True)
    summary={'purpose':result['purpose'],'pilots':[{**p['c4'],
        'siteId':p['siteId'],'name':p['name'],'tier':p['tier'],
        'anchor':p['anchor'],'markers':p['markerIds'],
        'plannedChunks':len(p['chunks']),'housingBacklog':p['housingBacklogAtLeast']}
        for p in result['pilots']]}
    REPORT.write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
    print('Generated',OUTPUT)
    for p in result['pilots']:
        print(f"{p['tier']}: {p['name']} {p['markerIds']} X/Z={p['anchor']} "
              f"homes={p['c4']['homes']} lots={len(p['lots'])} "
              f"housingBacklog={p['housingBacklogAtLeast']} chunks={len(p['chunks'])}")


if __name__=='__main__':
    main()
