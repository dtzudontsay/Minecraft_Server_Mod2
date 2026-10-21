#!/usr/bin/env python3
"""Create strict, deterministic chunk-local C2 PILOT construction index.

C1 demands are estimates, NOT actual workers or residents. Only two explicitly
selected GENERAL_COMPACT_SETTLEMENT sites are included; no NPCs are created.
"""
from __future__ import annotations
import argparse, hashlib, json, math
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
SCENE = ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
SOURCE = SCENE/'settlement_presets_c1_v1.json'
OUTPUT = SCENE/'settlement_construction_c2_v1.json'
REPORT = ROOT/'tools/geodata/output/c2_physical_pilots'
POLICY = ROOT/'tools/geodata/c2_pilot_selection.json'
SCHEMA = 1
# A test subset is deliberate: never install a 253-site construction bomb.
MAX_PILOTS = 2
HOUSING_COUNTS = {'SMALL': 8, 'MEDIUM': 14}
COMPATIBLE = {'GENERAL_COMPACT_SETTLEMENT'}
BUILDINGS = {'HOUSE':(11,11),'BAKERY':(13,11),'SMITHY':(13,11),
 'INN':(15,13),'FARM':(13,13),'ARMORY':(15,13),'MARKET':(15,13),
 'BARRACKS':(17,13),'TEMPLE':(15,13),'ADMINISTRATION':(15,13),
 'GRANARY':(11,11),'WELL':(7,7)}


def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def build(db, selection):
    if db.get('schemaVersion')!=1 or db.get('summary',{}).get('markerCount')!=253 or db['summary']['holdingCount']!=259:
        raise ValueError('C1 required: complete 253 marker / 259 holding registry')
    sites=db['physicalSitePresets']; by_marker={m:s for s in sites for m in s['markerIds']}
    if len(by_marker)!=253:raise ValueError('C1 missing or duplicate marker')
    chosen=[]
    for tier in ('SMALL','MEDIUM'):
        candidates=[s for s in sites if s['tier']==tier and s['blueprintMode'] in COMPATIBLE and
             'NW-029' not in s['markerIds'] and s['buildingDemand']['peopleTarget']>0]
        if not candidates:raise ValueError('No eligible '+tier+' settlement: explicit operator selection required')
        preferences=selection['preferredMarkers'][tier]
        rank={m:i for i,m in enumerate(preferences)}
        # Prefer accessible, reasonably familiar regions over a distant exotic site.
        candidates.sort(key=lambda s:(min([rank.get(m,9999) for m in s['markerIds']]),
             s.get('zone') not in ('NW','SW'),s['primaryMarkerId']))
        chosen.append(candidates[0])
    if len({s['siteId'] for s in chosen})!=MAX_PILOTS:raise ValueError('Duplicate pilot sites')
    plans=[]
    for site in chosen:
        anchor=site['workingAnchor']; x=int(anchor['x']);z=int(anchor['z'])
        demand=site['buildingDemand']; tier=site['tier']; lots=[]
        # Compact lane: houses face a central east-west street, no satellites.
        n=min(HOUSING_COUNTS[tier], demand['firstPhaseHousingPlots'])
        if n<3:raise ValueError('Pilot has too few houses for demonstration')
        # Distribute houses on both sides; 24 block wide parcels, keep 3-block
        # cleared lane at z=-1,0,1 and footpath connection z +/-2..7.
        columns=max(6,math.ceil((n+6)/2))
        for i in range(n):
            col=i//2; side=i%2; centerX=x+(col-(columns-1)//2)*24
            w,d=BUILDINGS['HOUSE']; zz=z+(-7-d if side==0 else 7)
            lots.append(dict(id=f'{site["siteId"]}_house_{i:03d}',kind='HOUSE',x=centerX-w//2,z=zz,
                width=w,depth=d,face='SOUTH' if side==0 else 'NORTH',capacity=5,staff=0,
                householdId=None,plannedOnlyHouseholdName=True))
        # Workers: put only workplaces indicated by C1 demand. Demonstration is
        # small; capacity remains max C1 staff and zero actual assigned staff.
        choices=['BAKERY','SMITHY','INN','MARKET','ARMORY','BARRACKS','GRANARY','TEMPLE','ADMINISTRATION']
        job_slots=[]
        for kind in choices:
            nwork=demand['workplaceCounts'].get(kind,0)
            if nwork>0:job_slots.append(kind)
        # At least one nonresidential building in a rural settlement, still
        # authored/declared, not an invented staffed bakery.
        if not job_slots:job_slots=['GRANARY']
        for i,kind in enumerate(job_slots[:4]):
            w,d=BUILDINGS[kind]; centerX=x+(columns//2+i)*24
            lots.append(dict(id=f'{site["siteId"]}_{kind.lower()}_{i:03d}',kind=kind,
               x=centerX-w//2,z=z-7-d,width=w,depth=d,face='SOUTH',capacity=0,
               staff=int(db['workplaceCatalog'].get(kind,{}).get('staff',0)),
               householdId=None,plannedOnlyHouseholdName=False))
        # Agricultural strip across the northbound spur, and explicit field.
        rural_count=demand['workplaceCounts'].get('FARM',0)
        if rural_count>0:
            w,d=BUILDINGS['FARM']; lots.append(dict(id=f'{site["siteId"]}_farm_000',kind='FARM',
                x=x-25,z=z+34,width=w,depth=d,face='NORTH',capacity=0,
                staff=int(db['workplaceCatalog']['FARM']['staff']),householdId=None,
                plannedOnlyHouseholdName=False))
            lots.append(dict(id=f'{site["siteId"]}_field_000',kind='FIELD',
                x=x-25,z=z+50,width=17,depth=26,face='NORTH',capacity=0,
                staff=0,householdId=None,plannedOnlyHouseholdName=False))
        # Pre-index all road pixels in chunk local coordinates. Roads are a lane
        # through the village, with a short spur to farms when applicable.
        minX=min(b['x'] for b in lots)-8;maxX=max(b['x']+b['width'] for b in lots)+8
        road=set((xx,zz) for xx in range(minX,maxX+1) for zz in range(z-2,z+3))
        if rural_count:
            # Spur lies BETWEEN housing parcels, never through a residential
            # building. Its cross-connection reaches the farm's entrance.
            street_x=x+12
            road.update((xx,zz) for zz in range(z+3,z+39)
                        for xx in range(street_x-1,street_x+2))
            road.update((xx,zz) for xx in range(x-25,street_x+2)
                        for zz in range(z+30,z+33))
        for b in lots:
            # 3 wide accessible path from center door to village lane (without
            # overwriting building footprints), or farm spur.
            doorx=b['x']+b['width']//2
            if b['kind']=='FIELD':continue
            frontage=b['z']-1 if b['face']=='NORTH' else b['z']+b['depth']
            if b['kind']=='FARM':
                for zz in range(z+30,frontage+1):
                    for xx in range(doorx-1,doorx+2):road.add((xx,zz))
            else:
                for zz in range(min(frontage,z),max(frontage,z)+1):
                    for xx in range(doorx-1,doorx+2):road.add((xx,zz))
        # Never allow a road to pass through a building footprint.
        for b in lots:
            for xx in range(b['x'],b['x']+b['width']):
                for zz in range(b['z'],b['z']+b['depth']):road.discard((xx,zz))
        chunks={}
        def c(cx,cz):return chunks.setdefault((cx,cz),{'r':set(),'b':set()})
        for xx,zz in road:
            cx,cz=xx//16,zz//16;c(cx,cz)['r'].add((zz-cz*16)*16+(xx-cx*16))
        for i,b in enumerate(lots):
            for cx in range(b['x']//16,(b['x']+b['width']-1)//16+1):
                for cz in range(b['z']//16,(b['z']+b['depth']-1)//16+1):c(cx,cz)['b'].add(i)
        bounds=[minX-42,maxX+42,min(b['z'] for b in lots)-42,max(b['z']+b['depth'] for b in lots)+42]
        plans.append({'siteId':site['siteId'],'name':site['name'],'tier':tier,
            'anchor':{'x':x,'z':z},'markerIds':site['markerIds'],
            'palette':site['paletteId'],'mode':site['blueprintMode'],
            'bounds':bounds,'lots':lots,'chunks':[{'cx':cx,'cz':cz,'r':sorted(data['r']),'b':sorted(data['b'])}
                 for (cx,cz),data in sorted(chunks.items())],
            'planningPeople':demand['peopleTarget'],'planningHomesNeeded':demand['housingBuildingsDemand'],
            'plannedHomesHere':n,'housingBacklogAtLeast':max(0,demand['housingBuildingsDemand']-n),
            'noNpcOrJobInstancesCreated':True,'terrainApproval':'RUNTIME_SURVEY_PER_PLOT'})
    return {'schemaVersion':SCHEMA,'purpose':'C2_CHUNK_DECORATION_PILOT_ONLY',
            'policy':{'pilotsOnly':True,'doNotModifyExistingChunks':True,'noNpcCreation':True,
            'noOther253SiteActivation':True,'winterfellExcluded':True},
            'summary':{'allMapMarkers':253,'allPoliticalHoldings':259,'pilotCount':MAX_PILOTS,
                'pilotMarkers':[p['markerIds'] for p in plans]}, 'pilots':plans}


def main():
    a=argparse.ArgumentParser();a.add_argument('--check',action='store_true');args=a.parse_args()
    out=build(read(SOURCE),read(POLICY))
    payload=json.dumps(out,ensure_ascii=False,indent=2)+'\n'
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8')!=payload:raise SystemExit('C2 resource absent/stale')
        print('C2 CHECK PASSED')
    else:
        OUTPUT.parent.mkdir(parents=True,exist_ok=True);OUTPUT.write_text(payload,encoding='utf-8')
        REPORT.mkdir(parents=True,exist_ok=True)
        (REPORT/'pilots.json').write_text(json.dumps(out['summary'],indent=2)+'\n',encoding='utf-8')
        print('Generated C2 pilot worldgen resource:',OUTPUT)
    for p in out['pilots']:
        print(p['tier'],p['name'],p['markerIds'],'anchor',p['anchor'],
            'actual test building plots',len(p['lots']),'chunk index',len(p['chunks']))

if __name__=='__main__':main()
