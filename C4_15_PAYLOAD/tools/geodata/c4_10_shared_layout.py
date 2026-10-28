#!/usr/bin/env python3
"""C4.10A: universal uncapped, site-by-site *physical layout* preview.

C4.9 is the population/workplace authority. This module does not modify any
Minecraft world or source manifest. It emits exactly the required ordinary
residential/workplace lots; placement and terrain remain provisional.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import heapq
import json
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'tools/geodata/output/c4_9_settlement_ledgers/C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json'
ARCH = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json'
OUT = ROOT / 'tools/geodata/output/c4_10_shared_layout'
SCHEMA = 'knownworld.c4_10.layout.v1'
PITCH = 31  # compact frontage grid; no maximum city radius
WINTERFELL = 'site_nw_029'
RURAL = {'FARM', 'PASTURE', 'GARDEN', 'FOREST_CAMP'}
CIVIC = ('ADMINISTRATION', 'JUSTICE_HALL', 'TEMPLE', 'GREAT_HALL', 'BARRACKS', 'MARKET')
REQUIRES_SPECIAL_POLICY = {'CANAL_CITY_REQUIRE_WATER_LAYOUT',
   'COASTAL_REQUIRE_SHORE_LAYOUT', 'PYRAMID_CITY_REQUIRE_AUTHORED_BLUEPRINT',
   'ROCK_INTEGRATED_REQUIRE_AUTHORED_BLUEPRINT',
   'SHADOW_CITY_REQUIRE_AUTHORED_BLUEPRINT',
   'MONUMENTAL_FORTRESS_REQUIRE_AUTHORED_BLUEPRINT'}
BLOCKED = {'RUIN_OR_LANDMARK_ONLY', 'UNVERIFIED_NO_CONSTRUCTION', 'MOBILE_NO_FIXED_FOOTPRINT'}
CORE = {
    'SMALL':  {'building': 'MANOR_HALL',  'outerWallHalfSpan': 56,  'innerWallHalfSpan': None,
               'walls': 'PALISADE', 'outsideFirstRing': 3},
    'MEDIUM': {'building': 'MINOR_KEEP',  'outerWallHalfSpan': 106, 'innerWallHalfSpan': None,
               'walls': 'STONE_CURTAIN', 'outsideFirstRing': 5},
    'MAJOR':  {'building': 'MAIN_CASTLE', 'outerWallHalfSpan': 171, 'innerWallHalfSpan': 149,
               'walls': 'TWO_STONE_CURTAINS', 'outsideFirstRing': 7},
}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def ward_centres(tier):
    """Small safe, nonoverlapping interior ward cells; no central building overlap.

    The castle occupies the center. Roads, gates and actual heights remain to
    be surveyed. These coordinates are relative to the construction anchor.
    """
    if tier == 'SMALL':
        return [(x,z) for z in (-34,34) for x in (-34,34)]
    if tier == 'MEDIUM':
        return [(x,z) for z in (-72,72) for x in (-72,-40,40,72)] + [
            (x,z) for z in (-40,40) for x in (-72,72)]
    if tier == 'MAJOR':
        return [(x,z) for z in (-120,-80,80,120) for x in (-120,-90,-60,60,90,120)]
    raise ValueError('Unknown tier '+tier)


def ring_cells(start=1):
    """Expand without an upper radius or housing quota.

    Deterministic clockwise perimeter traversal, 8*r slots per square ring.
    """
    r = start
    while True:
        for x in range(-r,r+1): yield x,-r,r
        for z in range(-r+1,r+1): yield r,z,r
        for x in range(r-1,-r-1,-1): yield x,r,r
        for z in range(r-1,-r,-1): yield -r,z,r
        r+=1


def grid_offset(gx, gz):
    """Small, deterministic block-level position jitter; adjacent streets stay connected."""
    raw = (gx * 11939 + gz * 32771 + gx * gz * 23) & 0x7fffffff
    return (raw % 5) - 2, ((raw // 11) % 5) - 2


def required_cells(w, d):
    # Reserve extra street cells around broad civic/monumental modules.
    return max(1, (w + 11 + PITCH - 1) // PITCH), max(1, (d + 11 + PITCH - 1) // PITCH)


def site_cells(tier):
    return ring_cells(CORE[tier]['outsideFirstRing'])


def cell_footprint(ax, az, gx, gz, sx, sz, w, d):
    jx, jz = grid_offset(gx, gz)
    cx = ax + (gx * PITCH) + ((sx - 1) * PITCH)//2 + jx
    cz = az + (gz * PITCH) + ((sz - 1) * PITCH)//2 + jz
    return cx - w//2, cz - d//2


def green_cell(gx, gz):
    # Sparse reserved pockets rather than a rectangle around EVERY house.
    return (gx * 17 + gz * 29 + gx * gz * 3) % 41 == 0


def ordered_work(demand):
    """Spread workplace *types* evenly without a huge expanded list."""
    counts = {t['kind']:int(t['requestedBuildings']) for t in demand
              if int(t['requestedBuildings'])>0 and t['kind'] not in RURAL}
    for kind in CIVIC:
        if counts.get(kind,0):
            counts[kind]-=1
            yield kind
    heap = []
    for kind,qty in counts.items():
        if qty: heapq.heappush(heap,(.5/qty,kind,1,qty))
    while heap:
        _,kind,served,qty=heapq.heappop(heap)
        yield kind
        if served<qty:
            heapq.heappush(heap,((served+.5)/qty,kind,served+1,qty))


def blended_urban(homes, demand):
    workplaces=sum(int(t['requestedBuildings']) for t in demand
                   if t['kind'] not in RURAL)
    total=homes+workplaces
    done_h=0
    work=iter(ordered_work(demand))
    for i in range(total):
        # Largest-remainder-style interleaving: no segregated trade district.
        due=((i+1)*homes+total//2)//total
        if done_h<due:
            done_h+=1
            yield 'HOUSE'
        else:
            yield next(work)
    if done_h!=homes or next(work,None) is not None:
        raise AssertionError('Urban demand was not conserved')


def geometry(site,tier,kind,templates):
    if kind=='HOUSE':
        name={'SMALL':'HOUSE_SMALL','MEDIUM':'HOUSE_MEDIUM','MAJOR':'HOUSE_URBAN'}[tier]
    else: name=kind
    template=templates.get(name)
    if template is None:
        return name,21,21,'TEMPLATE_MISSING'
    width,depth=int(template['c1Width']),int(template['c1Depth'])
    if min(width,depth)<5 or max(width,depth)>PITCH-6:
        return name,width,depth,'OVERSIZED_MODULE_REQUIRES_DISTRICT'
    if template.get('requiresShore') or template.get('requiresMountainResource') or template.get('requiresPolicy'):
        return name,width,depth,'TEMPLATE_REQUIRES_SPECIAL_SITE_POLICY'
    # The source C3 dimensions remain a safe upper bound. Urban homes are
    # varied 0-3 blocks, never below a functional 11x12 shell.
    if kind=='HOUSE':
        variation=int.from_bytes(hashlib.blake2b((site['siteId']+name).encode(),digest_size=2).digest(),'big')
        width=max(11,width-(variation%3))
        depth=max(12,depth-((variation//3)%3))
    return name,width,depth,'C3_MODULE_AVAILABLE'


def _safe_number(record,key):
    v=record[key]
    if isinstance(v,bool) or not isinstance(v,int) or v<0:
        raise ValueError(f'Invalid {key}: {v!r}')
    return v


def plan(site, templates, emit, emit_feature=None):
    """Plan one site, streaming lots through emit(record). Return summary.

    No reference to C4.5 pilot limits, C4.6 radius limits, or NpcId mutation.
    """
    sid=site['siteId'];name=site['name'];tier=site['tier'];mode=site['blueprintMode']
    people=_safe_number(site,'populationTarget'); workers=_safe_number(site,'workingPopulationTarget')
    homes=_safe_number(site,'residentialBuildingsTarget')
    households=_safe_number(site,'householdsTarget')
    units=_safe_number(site,'residentialUnitsPerBuilding')
    job_counts=site['occupationTargetsNonzero']
    if sum(int(v) for v in job_counts.values())!=workers or workers>people:
        raise ValueError(sid+': workforce not conserved')
    if homes*units<households: raise ValueError(sid+': insufficient household units')
    types=site['buildingDemandByKind']
    if len(set(t['kind'] for t in types))!=len(types):
        raise ValueError(sid+': duplicate building kind')
    work_count=sum(int(t['requestedBuildings']) for t in types)
    if work_count!=site['workplaceBuildingsTarget']:
        raise ValueError(sid+': workplace counts not conserved')
    for t in types:
        n=int(t['requestedBuildings']);cap=int(t['slotsPerBuilding']);staff=int(t['workersTarget'])
        if min(n,cap,staff)<0:raise ValueError(sid+': negative workplace demand')
        if staff>n*cap and not (t['kind']=='KEEP' and n==0 and
                              staff==site['mainComplex'].get('reservedKeepWorkerSlots',0)):
            raise ValueError(sid+': uncapped worker demand cannot be satisfied for '+t['kind'])
    result={'siteId':sid,'name':name,'tier':tier,'blueprintMode':mode,
            'mapAnchor':site['mapAnchor'],'constructionAnchor':None,
            'populationTarget':people,'workingPopulationTarget':workers,
            'householdsTarget':households,'residentialBuildingsTarget':homes,
            'workplaceBuildingsTarget':work_count,
            'source':'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json',
            'newNpcIdsCreated':0,'terrainApproved':False,'physicalBuilt':False,
            'status':None,'core':None,'lotCounts':{},'exceptionCounts':{},
            'outerRadiusCells':0,'roadPlan':'TOPOLOGY_ONLY_AWAITING_TERRAIN_SURVEY'}
    if people==0 or mode in BLOCKED:
        result['status']='NO_CONSTRUCTION_RUIN_UNVERIFIED_OR_MOBILE'
        return result
    if sid==WINTERFELL:
        result['status']='EXISTING_WINTERFELL_V3_PROTECTED_NO_REBUILD'
        result['core']={'id':sid+':core:1','template':'EXISTING_WINTERFELL_V3',
                        'workplaceStaffSlots':site['mainComplex'].get('reservedKeepWorkerSlots',0)}
        return result
    if tier not in CORE:
        result['status']='SPECIAL_SITE_REQUIRES_DISTINCT_CORE_POLICY'
        return result
    anchor=site['mapAnchor'];ax,az=int(anchor['x']),int(anchor['z'])
    cfg=CORE[tier]
    result['status']=('GEOMETRY_PREVIEW_EXCEPTIONAL_SITE_NO_CONSTRUCTION'
                      if mode!='GENERAL_COMPACT_SETTLEMENT' else
                      'GEOMETRY_PREVIEW_REQUIRES_TERRAIN_APPROVAL')
    result['core']={'id':sid+':core:1', 'kind':'CENTRAL_COMPLEX',
             'template':cfg['building'], 'x':ax,'z':az,
             'walls':cfg['walls'], 'outerWallHalfSpan':cfg['outerWallHalfSpan'],
             'innerWallHalfSpan':cfg['innerWallHalfSpan'],
             'gatePolicy':'GATES_NORTH_SOUTH_EAST_WEST',
             'workplaceStaffSlots':site['mainComplex'].get('reservedKeepWorkerSlots',0),
             'physicalPainter':'KnownWorldC410Fortifications',
             'terrainStatus':'UNSURVEYED_NOT_RENDER_AUTHORISED'}
    # The core is one physical complex, not an extra population or an invented KEEP demand.
    ward=iter(ward_centres(tier)); outer=site_cells(tier)
    occupied=set(); pockets=0

    def allocate(width,depth):
        nonlocal furthest,pockets
        sx,sz=required_cells(width,depth)
        while True:
            gx,gz,ring=next(outer)
            cells={(gx+dx,gz+dz) for dx in range(sx) for dz in range(sz)}
            if any(c in occupied for c in cells):continue
            # Explicit tree/green-space reserves in the compact street network.
            if sx==1 and sz==1 and green_cell(gx,gz):
                occupied.add((gx,gz));pockets+=1
                tx,tz=cell_footprint(ax,az,gx,gz,1,1,5,5)
                if emit_feature is not None:emit_feature({'schemaVersion':SCHEMA,'siteId':sid,
                      'lotId':f'{sid}:green:{pockets:07d}','kind':'GREEN_POCKET',
                      'template':'GREEN_POCKET','region':'GREEN_POCKET',
                      'x':tx,'z':tz,'width':5,'depth':5,'face':'SOUTH',
                      'grid':[gx,gz,1,1], 'physicalPermission':'GREEN_POCKET',
                      'reservedWorkerSlots':0,'householdReservationIds':[]})
                continue
            occupied.update(cells)
            x,z=cell_footprint(ax,az,gx,gz,sx,sz,width,depth)
            furthest=max(furthest,max(abs(gx),abs(gz),abs(gx+sx-1),abs(gz+sz-1)))
            return x,z,ring,gx,gz,sx,sz

    ward_candidate=next(ward,None)
    kinds=collections.Counter();exceptions=collections.Counter();total_units_allocated=0
    employment={t['kind']:[int(t['workersTarget']),int(t['slotsPerBuilding']),0]
                for t in types}
    house_index=collections.Counter();type_index=collections.Counter();household_num=0
    furthest=0

    def put(kind, region, x, z, ring, w, d, template,availability, gx=None, gz=None, sx=1, sz=1):
        nonlocal household_num,total_units_allocated
        if kind=='HOUSE':
            house_index['HOUSE']+=1
            lotid=f'{sid}:home:{house_index["HOUSE"]:07d}'
            remain=households-household_num
            own_households=min(units,remain)
            links=[f'{sid}:hh:{i:06d}' for i in range(household_num+1,
                                                    household_num+own_households+1)]
            household_num+=own_households
            total_units_allocated+=units
            staff=0;link=None
        else:
            type_index[kind]+=1
            ordinal=type_index[kind]
            lotid=f'{sid}:work:{kind}:{ordinal:06d}'
            links=[]
            rem,cap,assigned=employment[kind]
            staff=min(rem,cap)
            employment[kind][0]-=staff
            employment[kind][2]+=staff
            link=lotid
        emit({'schemaVersion':SCHEMA,'siteId':sid,'lotId':lotid,
              'kind':kind,'template':template,'constructionAnchorStatus':'MAP_ONLY',
              'region':region,'ring':ring,'x':x,'z':z,'width':w,'depth':d,
              'grid':None if gx is None else [gx,gz,sx,sz],
              'face':'SOUTH','householdReservationIds':links,
              'workplaceReservationId':link,'reservedWorkerSlots':staff,
              'physicalPermission':availability,
              'constructionState':'PLAN_ONLY_UNSURVEYED'})
        kinds[kind]+=1
        if availability!='C3_MODULE_AVAILABLE':exceptions[availability]+=1

    for kind in blended_urban(homes,types):
        template,w,d,availability=geometry(site,tier,kind,templates)
        # Wards accept only modest footprints and do not force a large site into the keep.
        if ward_candidate is not None and max(w,d)<=({'SMALL':18,'MEDIUM':24,'MAJOR':24}[tier]):
            dx,dz=ward_candidate
            put(kind,'INNER_WARD',ax+dx-w//2,az+dz-d//2,0,w,d,template,availability)
            ward_candidate=next(ward,None)
        else:
            x,z,ring,gx,gz,sx,sz=allocate(w,d)
            put(kind,'MIXED_URBAN_RING',x,z,ring,w,d,template,availability,gx,gz,sx,sz)
    # Rural workplaces begin beyond the last urban block (regardless of size).
    field_start=max(furthest+2,cfg['outsideFirstRing']+2)
    rural_cells=ring_cells(field_start)
    def allocate_rural(width,depth):
        nonlocal furthest
        sx,sz=required_cells(width,depth)
        for gx,gz,ring in rural_cells:
            cells={(gx+dx,gz+dz) for dx in range(sx) for dz in range(sz)}
            if any(c in occupied for c in cells):continue
            occupied.update(cells)
            x,z=cell_footprint(ax,az,gx,gz,sx,sz,width,depth)
            furthest=max(furthest,max(abs(gx+sx-1),abs(gz+sz-1)))
            return x,z,ring,gx,gz,sx,sz
    for t in sorted(types,key=lambda x:x['kind']):
        kind=t['kind']
        if kind not in RURAL:continue
        n=int(t['requestedBuildings'])
        template,w,d,availability=geometry(site,tier,kind,templates)
        for _ in range(n):
            x,z,ring,gx,gz,sx,sz=allocate_rural(w,d)
            put(kind,'OUTER_RURAL_BELT',x,z,ring,w,d,template,availability,gx,gz,sx,sz)
    # Exact conservation includes all restricted jobs as *deferred physical* lots.
    if kinds['HOUSE']!=homes or sum(n for k,n in kinds.items() if k!='HOUSE')!=work_count:
        raise ValueError(sid+': planned building count was reduced')
    if household_num!=households:
        raise ValueError(sid+': household reservation links not conserved')
    for kind, (remaining,cap,assigned) in employment.items():
        if remaining and not (kind=='KEEP' and type_index[kind]==0
                              and assigned==0
                              and remaining==site['mainComplex'].get('reservedKeepWorkerSlots',0)):
            raise ValueError(sid+': staffing slots insufficient for '+kind)
    result['greenPocketsReserved']=pockets
    result['layoutStyle']='COMPACT_WARPED_CONNECTED_STREETS'
    result['outerRadiusCells']=furthest
    result['outerApproximateHalfSpanBlocks']=(furthest+1)*PITCH
    result['lotCounts']=dict(sorted(kinds.items()))
    result['exceptionCounts']=dict(sorted(exceptions.items()))
    result['allocatedHouseholdUnits']=total_units_allocated
    result['linkedHouseholdReservations']=household_num
    result['plannedExternalWorkplaceLots']=work_count
    result['deferredTerrainLotCount']=homes+work_count
    result['needsSitePolicy']=mode!='GENERAL_COMPACT_SETTLEMENT'
    return result


def run(source=SOURCE,arch=ARCH,out=OUT,site_filter=None,include_lots=True):
    source=Path(source);arch=Path(arch);out=Path(out)
    sites=read(source)['sites']
    templates={t['id']:t for t in read(arch)['templates']}
    if not site_filter and len(sites)!=248:
        raise ValueError('Expected C4.9 baseline of 248 sites')
    out.mkdir(parents=True,exist_ok=True)
    summaries=[];totals=collections.Counter();exceptions=[]
    with tempfile.TemporaryDirectory(dir=out) as td:
        scratch=Path(td)
        temp_lots=scratch/'lots.jsonl'
        with temp_lots.open('w',encoding='utf-8') as f:
            def emit(rec):
                if include_lots:
                    f.write(json.dumps(rec,separators=(',',':'),ensure_ascii=False)+'\n')
            for site in sites:
                if site_filter and site['siteId'] not in site_filter:continue
                info=plan(site,templates,emit)
                summaries.append(info)
                totals['siteCount']+=1
                totals['population']+=info['populationTarget']
                totals['workers']+=info['workingPopulationTarget']
                totals['households']+=info['householdsTarget']
                totals['residentialDemand']+=info['residentialBuildingsTarget']
                totals['workplaceDemand']+=info['workplaceBuildingsTarget']
                totals['newPhysicalLotsProposed']+=sum(info['lotCounts'].values())
                if info['status']!='GEOMETRY_PREVIEW_REQUIRES_TERRAIN_APPROVAL':
                    exceptions.append({'siteId':site['siteId'],'name':site['name'],
                     'status':info['status'],'homesAwaitingPolicy':site['residentialBuildingsTarget'],
                     'workplacesAwaitingPolicy':site['workplaceBuildingsTarget']})
        (scratch/'sites.json').write_text(json.dumps({'schemaVersion':SCHEMA,
          'status':'OFFLINE_PREVIEW_NOT_WORLDGEN','sites':summaries},
          ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        (scratch/'exceptions.json').write_text(json.dumps(exceptions,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        (scratch/'summary.json').write_text(json.dumps({'schemaVersion':SCHEMA,
          'totals':dict(totals),'newNpcsCreated':0,'terrainApproved':0,
          'worldChunksModified':0,'status':'OFFLINE_PREVIEW_NOT_WORLDGEN'},indent=2)+'\n',encoding='utf-8')
        for src,dest in [('sites.json','C4_10_SITE_GEOMETRY.json'),
            ('exceptions.json','C4_10_EXCEPTIONS.json'),('summary.json','C4_10_SUMMARY.json')]:
            (scratch/src).replace(out/dest)
        if include_lots:temp_lots.replace(out/'C4_10_ALL_PLANNED_LOTS.jsonl')
    return dict(totals)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source',type=Path,default=SOURCE)
    p.add_argument('--architecture',type=Path,default=ARCH)
    p.add_argument('--output',type=Path,default=OUT)
    p.add_argument('--site',action='append',help='Optional site ID, repeatable')
    p.add_argument('--summary-only',action='store_true',help='No huge lot file')
    args=p.parse_args()
    print(json.dumps(run(args.source,args.architecture,args.output,set(args.site) if args.site else None,
                         not args.summary_only),indent=2))
    print('PLAN ONLY; no Minecraft chunks or NPCs were changed.')

if __name__=='__main__':main()
