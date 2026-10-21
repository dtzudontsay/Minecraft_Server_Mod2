#!/usr/bin/env python3
"""C1-C3: derive conservative worldwide settlement BLUEPRINTS from B2/B3.

NO WORLD MUTATION. Does not place blocks, assign NPC jobs, or change populations.
All 253 markers are preserved indirectly by one plan per unique physicalSite.
Reports workforce/building shortfalls rather than silently creating extra residents.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
SOURCE=BASE/'world_sites_b2_b3_v1.json'
OUTPUT=BASE/'settlement_presets_c1_v1.json'
REPORT=ROOT/'tools/geodata/output/c1_settlement_presets'
OVERRIDE=ROOT/'tools/geodata/c1_settlement_preset_rules.json'
GRAMMAR=ROOT/'tools/geodata/c1_building_grammars.json'

# A worker is *assigned* only once. Percentages describe employed adults,
# not total people. Exact numbers remain provisional until B4 builds individuals.
ROLE_WORKPLACE={
 'grain_farmer':'FARM','gardener':'GARDEN','herder':'PASTURE',
 'fisher':'FISHERY','forester':'FOREST_CAMP',
 'blacksmith':'SMITHY','carpenter':'CARPENTRY','mason':'MASONRY',
 'weaver':'WEAVERY','miner':'MINE',
 'merchant':'MARKET','carter':'CART_DEPOT','sailor':'DOCK',
 'market_seller':'MARKET','cook':'KITCHEN','servant':'SERVICE_HALL',
 'innkeeper':'INN','guard':'BARRACKS','soldier':'BARRACKS',
 'administrator':'ADMINISTRATION','religious_attendant':'TEMPLE'
}
# Capacity is staff PER workplace, not its housing capacity.
WORKPLACES={
 'FARM':{'staff':12,'w':17,'d':15,'field':True,'sector':'FOOD'},
 'GARDEN':{'staff':8,'w':11,'d':11,'field':True,'sector':'FOOD'},
 'PASTURE':{'staff':10,'w':15,'d':12,'field':True,'sector':'FOOD'},
 'FISHERY':{'staff':9,'w':14,'d':11,'shore':True,'sector':'FOOD'},
 'FOREST_CAMP':{'staff':14,'w':15,'d':11,'sector':'RAW_MATERIAL'},
 'SMITHY':{'staff':6,'w':13,'d':11,'sector':'CRAFT'},
 'CARPENTRY':{'staff':9,'w':13,'d':11,'sector':'CRAFT'},
 'MASONRY':{'staff':12,'w':17,'d':12,'sector':'CRAFT'},
 'WEAVERY':{'staff':12,'w':16,'d':12,'sector':'CRAFT'},
 'MINE':{'staff':20,'w':15,'d':12,'mountain':True,'sector':'RAW_MATERIAL'},
 'MARKET':{'staff':22,'w':21,'d':17,'sector':'TRADE'},
 'CART_DEPOT':{'staff':12,'w':17,'d':14,'sector':'TRANSPORT'},
 'DOCK':{'staff':35,'w':21,'d':14,'shore':True,'sector':'TRADE'},
 'KITCHEN':{'staff':10,'w':14,'d':12,'sector':'FOOD'},
 'SERVICE_HALL':{'staff':26,'w':20,'d':13,'sector':'SERVICE'},
 'INN':{'staff':8,'w':18,'d':14,'sector':'SERVICE'},
 'BARRACKS':{'staff':55,'w':26,'d':19,'sector':'MILITARY'},
 'ADMINISTRATION':{'staff':18,'w':17,'d':14,'sector':'GOVERNANCE'},
 'TEMPLE':{'staff':16,'w':21,'d':19,'sector':'RELIGION'},
 'BAKERY':{'staff':6,'w':13,'d':12,'sector':'FOOD'},
 'ARMORY':{'staff':8,'w':16,'d':13,'sector':'MILITARY'},
 'MILL':{'staff':8,'w':16,'d':13,'sector':'FOOD'},
 'TAVERN':{'staff':8,'w':17,'d':14,'sector':'SERVICE'},
 'STABLE':{'staff':10,'w':17,'d':14,'sector':'TRANSPORT'},
 'BROTHEL':{'staff':8,'w':14,'d':13,'sector':'SERVICE','requiresPolicy':True},
 'HEALER':{'staff':7,'w':13,'d':11,'sector':'HEALTH'},
 'WELL':{'staff':0,'w':5,'d':5,'sector':'INFRASTRUCTURE'},
 'GRANARY':{'staff':0,'w':15,'d':13,'sector':'STORAGE'},
 'WAREHOUSE':{'staff':0,'w':19,'d':16,'sector':'STORAGE'},
 'GUARD_TOWER':{'staff':0,'w':10,'d':10,'sector':'DEFENSE'},
 'GREAT_HALL':{'staff':0,'w':37,'d':29,'sector':'GOVERNANCE'},
 'KEEP':{'staff':0,'w':49,'d':41,'sector':'GOVERNANCE'},
 'PUBLIC_BATH':{'staff':0,'w':19,'d':17,'sector':'SERVICE'},
 'SCHOOL':{'staff':0,'w':16,'d':14,'sector':'EDUCATION'},
 'COURTYARD':{'staff':0,'w':29,'d':27,'sector':'INFRASTRUCTURE'},
 'RITUAL_GROUND':{'staff':0,'w':31,'d':31,'sector':'CULTURE'},
 'MONUMENT':{'staff':0,'w':43,'d':43,'sector':'HISTORY'},
 'RUIN':{'staff':0,'w':35,'d':31,'sector':'HISTORY'},
 'CAMP':{'staff':0,'w':15,'d':15,'sector':'MILITARY'},
}

# Compact *real* settlements; farmland is an outer belt, not scattered housing.
# For major capitals, later district streaming is required before all people
# can be housed. The blueprint does not claim all housing has been built.
TIERS={
 'MAJOR':{'household':4.7,'unitsPerHouse':4,'radius':860,'core':330,'plotLimit':2500,'slopeLimit':4,'districts':8,'road':5},
 'MEDIUM':{'household':4.8,'unitsPerHouse':2,'radius':440,'core':190,'plotLimit':950,'slopeLimit':5,'districts':5,'road':4},
 'SMALL':{'household':5.0,'unitsPerHouse':1,'radius':210,'core':100,'plotLimit':380,'slopeLimit':6,'districts':3,'road':3},
 'SPECIAL':{'household':4.0,'unitsPerHouse':1,'radius':320,'core':170,'plotLimit':500,'slopeLimit':4,'districts':3,'road':4},
 'NO_SETTLEMENT':{'household':5.0,'unitsPerHouse':1,'radius':0,'core':0,'plotLimit':0,'slopeLimit':0,'districts':0,'road':0},
}
SPECIAL_RUIN_WORDS=('RUIN','ABANDON','DOOMED','LEGENDARY','UNVERIFIED','TOWER_OF_JOY')
NO_FIXED_SITE=('MOBILE_CRANNOG','GREAT_GRASS_SEA','NOMADIC')


def rd(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def qceil(num,div):return math.ceil(num/div) if num else 0

def apportion(total, shares):
    """Largest-remainder allocator; workers never vanish or get counted twice."""
    if not shares or total<=0:return {k:0 for k in shares}
    if sum(shares.values())!=100:raise ValueError(f'bad job shares: {sum(shares.values())}')
    raw={k:total*v/100 for k,v in shares.items()}
    ret={k:int(math.floor(n)) for k,n in raw.items()}
    remain=total-sum(ret.values())
    for k in sorted(raw,key=lambda k:(-(raw[k]-ret[k]),k))[:remain]:ret[k]+=1
    assert sum(ret.values())==total
    return ret


def scenario_modes(site):
    a=site['archetype'].upper(); kind=site['physicalType']['value'].upper()
    habitation=site['habitation']['value'].upper()
    if any(t in a for t in NO_FIXED_SITE):return 'MOBILE_NO_FIXED_FOOTPRINT'
    if kind=='LEGENDARY_UNVERIFIED' or any(t in a for t in ('LEGENDARY','UNVERIFIED')) or 'UNVERIFIED_EXISTENCE' in habitation:
        return 'UNVERIFIED_NO_CONSTRUCTION'
    # Harrenhal remains an inhabited political seat in 298 AC, despite damage.
    if 'HARRRENHAL' in a or 'HARRENHAL' in a:return 'GENERAL_COMPACT_SETTLEMENT'
    if kind in ('ABANDONED_RUIN','LANDMARK') or any(t in a for t in SPECIAL_RUIN_WORDS) or 'RUIN' in habitation:
        return 'RUIN_OR_LANDMARK_ONLY'
    if 'MONUMENTAL' in a or 'FIVE_FORT' in a:return 'MONUMENTAL_FORTRESS_REQUIRE_AUTHORED_BLUEPRINT'
    if 'CANAL' in a:return 'CANAL_CITY_REQUIRE_WATER_LAYOUT'
    if 'PYRAMID' in a:return 'PYRAMID_CITY_REQUIRE_AUTHORED_BLUEPRINT'
    if 'ROCK_FORTRESS' in a:return 'ROCK_INTEGRATED_REQUIRE_AUTHORED_BLUEPRINT'
    if 'ISLAND' in a or 'PORT' in a:return 'COASTAL_REQUIRE_SHORE_LAYOUT'
    if 'SHADOW' in a or 'ASSHAI' in a:return 'SHADOW_CITY_REQUIRE_AUTHORED_BLUEPRINT'
    return 'GENERAL_COMPACT_SETTLEMENT'


def building_demands(site, policy):
    tier=site['tier']; pop=int(site['populationPlanning']['baseline'])
    raw_rate=site.get('laborParticipation')
    rate=raw_rate.get('value',0.5) if isinstance(raw_rate,dict) else 0.5
    rate=max(0.,min(.80,float(rate)))
    # workforce percentage applies to entire population as provisional *planning*
    # target only, not a fabricated current roster.
    workers=int(round(pop*rate))
    shares=site['occupationDistribution']['workerRolesPercent']
    assert not set(shares)-set(ROLE_WORKPLACE),f"Unmapped occupation role(s) in {site['siteId']}: {set(shares)-set(ROLE_WORKPLACE)}"
    assigned=apportion(workers,shares) if sum(shares.values())==100 else {k:0 for k in shares}
    if sum(shares.values()) not in (0,100):raise ValueError(f"bad occupation model for {site['siteId']}")
    counts=collections.Counter(); capacity=collections.Counter(); staffByWorkplace=collections.Counter()
    for role,n in assigned.items():
        kind=ROLE_WORKPLACE[role]
        staffByWorkplace[kind]+=n
    # Split existing workers rather than creating extra jobs for specialist sites.
    for parent,child,fraction in [('KITCHEN','BAKERY',.25),('KITCHEN','MILL',.10),('KITCHEN','TAVERN',.12),
                                   ('BARRACKS','ARMORY',.08),('CART_DEPOT','STABLE',.30),('SERVICE_HALL','HEALER',.04)]:
        move=min(staffByWorkplace[parent],int(staffByWorkplace[parent]*fraction))
        staffByWorkplace[parent]-=move;staffByWorkplace[child]+=move
    if policy.get('permitBrothel'):
        n=int(staffByWorkplace['SERVICE_HALL']*.03)
        staffByWorkplace['SERVICE_HALL']-=n;staffByWorkplace['BROTHEL']+=n
    for kind,n in sorted(staffByWorkplace.items()):
        spec=WORKPLACES[kind]
        count=qceil(n,spec['staff'])
        # Shore-dependent businesses stay marked for spatial survey: none
        # are silently placed into an inland pond or cliff.
        counts[kind]=count; capacity[kind]=count*spec['staff']
    if workers and sum(staffByWorkplace.values())!=workers:raise ValueError('double-counted workforce')
    if pop>0 and site['tier'] not in ('NO_SETTLEMENT',):
        counts['WELL']=max(1,qceil(pop,700)); counts['GRANARY']=max(1,qceil(pop,1900))
        counts['WAREHOUSE']=qceil(pop,6000);counts['SCHOOL']=qceil(pop,8500)
        counts['PUBLIC_BATH']=qceil(pop,18000)
        counts['GUARD_TOWER']=2 if tier in ('MAJOR','SPECIAL') else (1 if tier=='MEDIUM' else 0)
        if tier=='MAJOR': counts['GREAT_HALL']=1;counts['KEEP']=1;counts['COURTYARD']=1
        elif tier=='MEDIUM':counts['GREAT_HALL']=1;counts['COURTYARD']=1
    # Residents are NOT workers. Household estimates are NOT individual NPCs.
    settings=TIERS[tier];households=qceil(pop,settings['household'])
    houses=qceil(households,settings['unitsPerHouse'])
    # Never let projected workplaces consume every housing plot in a large
    # settlement. Rural farms/foresters/mines live in their own outer belt.
    rural={'FARM','GARDEN','PASTURE','FISHERY','FOREST_CAMP','MINE'}
    totalUrban=sum(n for k,n in counts.items() if k not in rural)
    housingPlotBudget=math.ceil(settings['plotLimit']*.68)
    workplacePlotBudget=max(0,settings['plotLimit']-housingPlotBudget)
    plannedHouses=min(houses,housingPlotBudget)
    firstPhaseWorkplaces={}
    remain=workplacePlotBudget
    for k,n in sorted(counts.items(),key=lambda x: (x[0] not in ('WELL','GRANARY','MARKET','BAKERY','KITCHEN','BARRACKS'),x[0])):
        if k in rural: continue
        firstPhaseWorkplaces[k]=min(n,remain)
        remain-=firstPhaseWorkplaces[k]
    workplaceBacklog={k:n-firstPhaseWorkplaces.get(k,0) for k,n in counts.items()
                       if k not in rural and n>firstPhaseWorkplaces.get(k,0)}
    unmet=max(0,houses-plannedHouses)
    # Farm fields are annexes to a farm workplace; one farm can employ 12
    # farmers, field area must be landscape-validated rather than inferred as
    # one field per worker.
    annex={k:counts[k] for k in ('FARM','GARDEN','PASTURE') if counts[k]}
    return {'peopleTarget':pop,'workforceTarget':workers,'laborShare':rate,
      'roleWorkerTargets':assigned,'staffByWorkplace':dict(sorted(staffByWorkplace.items())),
      'workplaceCounts':dict(sorted((k,int(v)) for k,v in counts.items() if v>0)),
      'workplaceMaxStaff':{k:WORKPLACES[k]['staff'] for k,v in counts.items() if v>0},
      'workplaceCapacity':dict(sorted((k,int(v)) for k,v in capacity.items() if v>0)),
      'householdsEstimated':households,'housingUnitsPerBuilding':settings['unitsPerHouse'],
      'housingBuildingsDemand':houses,'firstPhaseHousingPlots':plannedHouses,
      'housingBuildingBacklog':unmet,'firstPhasePlotLimit':settings['plotLimit'],
      'firstPhaseWorkplaceCounts':firstPhaseWorkplaces,
      'urbanWorkplaceBuildingDemand':totalUrban,'urbanWorkplaceBacklog':workplaceBacklog,
      'ruralBeltWorkplaceCounts':{k:int(counts[k]) for k in sorted(rural) if counts[k]},
      'fieldAnnexDemand':annex,'doesNotCreateResidentsOrWorkers':True,
      'isDemandNotExistingBuildings':True}


def preview_lots(site, demand, settings, mode):
    """A deterministic schematic, NOT a terrain-approved build footprint.

    A connected axial street-and-block preview clustered around one center.
    These are conceptual local offsets only; they are not worldgen commands.
    """
    if mode not in ('GENERAL_COMPACT_SETTLEMENT',):return []
    n=min(90,demand['firstPhaseHousingPlots']+
             sum(demand['firstPhaseWorkplaceCounts'].values()))
    if n<=0:return []
    composition=['HOUSE']*min(70,demand['firstPhaseHousingPlots'])
    for k,count in sorted(demand['firstPhaseWorkplaceCounts'].items()):
        composition.extend([k]*min(count,3))
    if not composition:return []
    composition=(composition+['HOUSE']*n)[:n]
    # Fixed square grid with center-reserved square; order guarantees all
    # offsets live close to the same central square, not 4,500m satellites.
    spacing=25 if site['tier']=='MAJOR' else (23 if site['tier']=='MEDIUM' else 21)
    positions=[]
    for gx in range(-11,12):
        for gz in range(-11,12):
            if abs(gx)<=1 and abs(gz)<=1:continue  # civic/keep center
            positions.append((gx,gz))
    positions.sort(key=lambda p:(max(abs(p[0]),abs(p[1])),abs(p[0])+abs(p[1]),p[0],p[1]))
    results=[]
    for i,(gx,gz) in enumerate(positions[:len(composition)]):
        k=composition[i]
        spec=WORKPLACES.get(k,{'w':13,'d':11})
        results.append({'plotId':f'{site["siteId"]}_lot_{i:04d}',
            'localX':gx*spacing,'localZ':gz*spacing,
            'type':k,'width':spec['w'],'depth':spec['d'],
            'streetGridSpacing':spacing,'simulationPreviewOnly':True,
            'hasTerrainApproval':False,'reserveForFutureGrowth':i>=len(composition)//2})
    return results


def make_site(site,policy):
    tier=site['tier']; sid=site['siteId'];marker_ids=site['markerIds']
    if tier not in TIERS:raise ValueError('unknown tier')
    if not marker_ids or len(set(marker_ids))!=len(marker_ids):raise ValueError(f'bad marker group {sid}')
    settings=TIERS[tier]; mode=scenario_modes(site)
    demands=building_demands(site,policy)
    pop=demands['peopleTarget']
    # Domain ownership and the house named on a home are separate concepts.
    # House signs need actual resident household labels from B4, never all
    # occupants silently inheriting the ruling dynasty.
    output={
      'siteId':sid,'name':site['name'],'zone':site['zone'],
      'primaryMarkerId':site['primaryMarkerId'],'markerIds':marker_ids,
      'geographicId':site['geographicId'],'workingAnchor':site['workingAnchor'],
      'tier':tier,'archetype':site['archetype'],'paletteId':site['materialPalette'],
      'historicalStatus':site['habitation'],
      'linkedPoliticalHoldingIds':site['linkedPoliticalHoldingIds'],
      'blueprintMode':mode,'footprintRequiresLocalSurvey':True,
      'cityCore':{'shape':'COMPACT_CONNECTED_DISTRICTS','radiusMaximumMeters':settings['radius'],
        'centralCoreRadiusMeters':settings['core'],'districtCountTarget':settings['districts'],
        'roadWidthBlocks':settings['road'],'neverScatterResidentialDistrictsBeyondCoreRadius':True,
        'farmAndFieldPlacement':'DEDICATED_OUTER_BELT_WITH_TRANSPORT_CONNECTIONS',
        'mountainPlacement':'LOCAL_SLOPE_SURVEY_TERRACE_STILT_STAIR_SWITCHBACK',
        'maxSingleBuildingSlopeBlocks':settings['slopeLimit'],
        'neverBridgeValleysWithUnboundedFill':True,
        'landAndSeaMustBeSurveyed':True},
      'constructionSafety':{'doNotSuppressAllLandmassDecoration':True,
        'noLavaLakesWithinFootprintOrFeatureMargin':True,
        'noTreesWithinFootprintOrFeatureMargin':True,
        'reserveNeighborSourceChunkMargin':2,'doNotEditExistingGeneratedChunks':True,
        'requireApprovedYAndFootprint':True,'skipOnWaterUnlessExplicitShoreTemplate':True,
        'worldgenOrder':'TERRAIN_THEN_RESERVE_THEN_ROADS_THEN_BUILDINGS_THEN_DETAILS'},
      'buildingDemand':demands,
      'compactLayoutPreview':preview_lots(site,demands,settings,mode),
      'householdSigns':{'textSource':'AUTHORED_RESIDENT_HOUSEHOLD_DISPLAY_NAME',
        'fallback':'UNASSIGNED_HOME','mustNotUseRulingHouseForEveryDwelling':True,
        'renderWhenHouseholdsExist':True},
      'expansion':{'reserveLots':True,'requiresPopulationOrEconomicDecision':True,
        'appendOnlyConstructionPhases':True,'stableSiteId':sid,
        'constructionActions':['ADD_HOUSING','ADD_FARM','ADD_WORKPLACE','ADD_WALL','ADD_TEMPORARY_CAMP'],
        'constructionAndDemolitionRequireSeparateWorldAuthority':True,
        'temporaryCampsHaveLifetimeAndOwningFaction':True},
      'generation':{'physicalConstructionEnabled':False,'terrainApproved':False,
        'approach':'PRESET_BLUEPRINT_ONLY_C1','overwriteWinterfell':False,
        'residentsInstantiated':0,'workforceAssigned':0},
      'evidence':{'siteType':site['physicalType']['evidence'],
          'populationRange':site['populationPlanning']['rangeEvidence'],
          'presets':'INVENTED','jobCapacities':'INVENTED',
          'economicDemand':'INVENTED','householdNames':'MUST_BE_AUTHORED_OR_DERIVED_FROM_REAL_NPCS'},
    }
    # Independent construction is categorically blocked for non-sites.
    if mode!='GENERAL_COMPACT_SETTLEMENT':output['generation']['specialBlueprintRequired']=True
    if site['primaryMarkerId']=='NW-029':output['generation']['preserveWinterfellV3']=True
    return output


def make_holding(holding,sites):
    siteids=holding.get('linkedSiteIds',[])
    for sid in siteids:
        if sid not in sites:raise ValueError(f'unknown physical site {sid}')
    return {'holdingId':holding['holdingId'],'name':holding['name'],
      'worldLocationId':holding['worldLocationId'],
      'tier':holding['tier'],'archetype':holding['archetype'],
      'physicalSiteIds':siteids,
      'parentHoldingId':holding.get('parentHoldingId'),
      'parentCitySiteIds':holding.get('parentCitySiteIds',[]),
      'ownerDynastyId':holding.get('ownerDynastyId'),
      'individualHolderNpcId':holding.get('holderNpcId'),
      'requiresSeparateSeatPlacement':holding['placementStatus']=='UNPLACED_SEAT',
      'isAdministrativeDomain':holding['isPoliticalDomain'],
      'noAdditionalPopulationOnTopOfSites':True,
      'noConstructionWithoutCoordinates':not bool(siteids),
      'ownershipDataIsScenarioNotVerifiedCurrentWorld':True}


def build(registry,policy,grammars=None):
    if registry.get('schemaVersion')!=1 or registry.get('summary',{}).get('markerCount')!=253 or registry['summary']['holdingCount']!=259:
        raise ValueError('B2/B3 registry must be current, 253/259')
    sites={s['siteId']:make_site(s,policy) for s in registry['physicalSites']}
    if len(sites)!=len(registry['physicalSites']):raise ValueError('duplicate site id')
    holdings={h['holdingId']:make_holding(h,sites) for h in registry['holdings']}
    if len(holdings)!=259:raise ValueError('duplicate holding id')
    markers=registry['markers'];observed={x['markerId'] for x in markers}
    if len(observed)!=253:raise ValueError('lost/duplicated marker ID')
    flattened=[m for s in sites.values() for m in s['markerIds']]
    if set(flattened)!=observed or len(flattened)!=253:raise ValueError('marker coverage lost/duplicate')
    for m in markers:
        s=sites[m['siteId']]
        if m['markerId'] not in s['markerIds']:raise ValueError('broken marker-site crosswalk')
        if m['markerId']==s['primaryMarkerId']:
            if (m['workingAnchor']['x'],m['workingAnchor']['z'])!=(s['workingAnchor']['x'],s['workingAnchor']['z']):
                raise ValueError('B2 atlas anchor changed')
    # Family ownership of a physical building remains to be resolved by B4.
    for s in sites.values():
        if s['generation']['physicalConstructionEnabled'] or s['generation']['terrainApproved']:
            raise ValueError('C1 must not authorize chunk mutation')
    special=sum(s['tier']=='SPECIAL' for s in sites.values())
    return {'schemaVersion':1,'scenario':'agot_298_ac','purpose':'C1_C3_BLUEPRINT_AND_WORKPLACE_DEMAND_NOT_BUILT_WORLD',
      'policy':{'noWorldgenActivation':True,'noPhysicalNPCsCreated':True,
        'preserveExistingWorld':True,'noInvisiblePopulationCohorts':True,
        'allMapObservationsRetained':True,'doNotMisrepresentModEstimatesAsLore':True},
      'summary':{'markerCount':len(flattened),'physicalSiteCount':len(sites),
        'holdingCount':len(holdings),'specialSiteCount':special,
        'safelyConstructedNow':0,'workerAssignmentsNow':0,
        'tierCounts':dict(sorted(collections.Counter(x['tier'] for x in sites.values()).items())),
        'unplacedHoldingSeats':sum(x['requiresSeparateSeatPlacement'] for x in holdings.values())},
      'workplaceCatalog':WORKPLACES,'occupationToWorkplace':ROLE_WORKPLACE,
      'buildingGeometryGrammars':grammars or {'planningOnly':True},
      'tierPresets':TIERS,'physicalSitePresets':list(sites.values()),
      'politicalHoldingPresets':list(holdings.values()),
      'sourceSHA256':hashlib.sha256(json.dumps(registry,sort_keys=True).encode()).hexdigest()}


def main():
    a=argparse.ArgumentParser();a.add_argument('--check',action='store_true');a.add_argument('--dry-run',action='store_true');args=a.parse_args()
    registry=rd(SOURCE);policy=rd(OVERRIDE);grammars=rd(GRAMMAR)
    if set(WORKPLACES)-set(grammars['buildingModules']):
        raise ValueError('Missing building grammar for workplace type')
    result=build(registry,policy,grammars);payload=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    print(json.dumps(result['summary'],indent=2))
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8')!=payload:
            raise SystemExit('C1 generated file missing/stale; rerun without --check')
        print('C1 CHECK PASSED')
    elif not args.dry_run:
        OUTPUT.parent.mkdir(parents=True,exist_ok=True);OUTPUT.write_text(payload,encoding='utf-8')
        REPORT.mkdir(parents=True,exist_ok=True)
        (REPORT/'C1_SUMMARY.json').write_text(json.dumps(result['summary'],indent=2)+'\n',encoding='utf-8')
        # Human review format avoids Excel separator issue.
        import csv
        with (REPORT/'ALL_SITE_PRESETS_EXCEL_DE.csv').open('w',encoding='utf-8-sig',newline='') as f:
            cols=['site_id','name','marker_ids','tier','archetype','mode','x','z','population_plan',
                  'workforce_plan','housing_demand','housing_first_phase','housing_backlog','workplaces','evidence','user_notes']
            writer=csv.DictWriter(f,fieldnames=cols,delimiter=';');writer.writeheader()
            for s in result['physicalSitePresets']:
                d=s['buildingDemand'];writer.writerow({'site_id':s['siteId'],'name':s['name'],
                  'marker_ids':';'.join(s['markerIds']),'tier':s['tier'],'archetype':s['archetype'],
                  'mode':s['blueprintMode'],'x':s['workingAnchor']['x'],'z':s['workingAnchor']['z'],
                  'population_plan':d['peopleTarget'],'workforce_plan':d['workforceTarget'],
                  'housing_demand':d['housingBuildingsDemand'],'housing_first_phase':d['firstPhaseHousingPlots'],
                  'housing_backlog':d['housingBuildingBacklog'],'workplaces':json.dumps(d['workplaceCounts']),
                  'evidence':s['evidence']['presets'],'user_notes':''})
        html=Path(__file__).with_name('c1_presets_dashboard_template.html').read_text(encoding='utf-8')
        # Local-only HTML; preview plots remain explicitly NOT build approved.
        subset={'summary':result['summary'],'sites':result['physicalSitePresets'],
                'holdings':result['politicalHoldingPresets'],'workplaces':result['workplaceCatalog']}
        blob=json.dumps(subset,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c').replace('>','\\u003e')
        (REPORT/'KnownWorld_C1_Preset_Explorer.html').write_text(html.replace('__PRESET_JSON__',blob),encoding='utf-8')
        print('Wrote',OUTPUT.relative_to(ROOT));print('Wrote',REPORT.relative_to(ROOT))

if __name__=='__main__':main()
