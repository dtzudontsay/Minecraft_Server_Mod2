#!/usr/bin/env python3
"""B2/B3: deterministic world-wide map-site and political holding registry.

Must be run from a checkout with the existing B2.2/B2.3 source datasets.
No world-save modification, NPC creation, structure generation or invented atlas XY.
Run: py tools/geodata/build_b2_b3_world_registry.py
     py tools/geodata/build_b2_b3_world_registry.py --check
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GEO = ROOT / 'tools' / 'geodata'
REF = ROOT / 'src/main/resources/data/knownworld/reference'
SCENARIO = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac'
PROFILES = GEO / 'output/b2_3_world_profiles'
OUTPUT = SCENARIO / 'world_sites_b2_b3_v1.json'
REPORT = GEO / 'output/b2_b3_world_registry'
OVERRIDES = GEO / 'b2_b3_tier_overrides.json'
SOURCE = {
    'marker_catalog': GEO/'output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv',
    'marker_audit': GEO/'output/b2_holding_links/markers_253_audit.csv',
    'holding_links': SCENARIO/'holding_geo_candidates.json',
    'holdings': SCENARIO/'landed_holdings.json',
    'site_research': PROFILES/'site_profiles_253.json',
    'holding_research': PROFILES/'holding_profiles_259.json',
    'location_references': REF/'locations.json',
    'holding_references': REF/'locations_holdings.json',
    'regional_influences': REF/'regional_influences.json',
    'regional_influence_extensions': REF/'regional_influence_extensions.json',
    'territory_zone_catalog': REF/'territory_zones.json',
    'subregion_mask_catalog': REF/'subregion_masks.json',
    'tier_overrides': OVERRIDES,
}

REGIONAL_KINDS = {'WORLD','CONTINENT','REGION','REALM','SUBREGION'}
TIERS = {'MAJOR','MEDIUM','SMALL','SPECIAL','NO_SETTLEMENT'}
ALLOWED_GRADES = {'CONFIRMED','CONTEXT','INVENTED'}
DOMAIN_TYPES = {'REGION','TERRITORY'}
MAJOR_HOLDINGS = {'winterfell_domain','storms_end_domain','highgarden_domain','casterly_rock_domain',
  'harrenhal_domain','sunspear_domain','dragonstone_domain','dreadfort_domain','eyrie_domain',
  'riverrun_domain','pyke_domain'}
# This is GENERATOR authoring, not a claim about canon sizes.
PRESET_DEFS = {
 'NORTHERN': {'primary':['stone_bricks','cobblestone','spruce_planks'],'roof':'spruce','climate':'cool_temperate','source':'INVENTED'},
 'WESTEROSI_COAST': {'primary':['stone_bricks','oak_planks','terracotta'],'roof':'oak','climate':'temperate','source':'INVENTED'},
 'WESTEROSI_INTERIOR': {'primary':['stone_bricks','oak_planks','bricks'],'roof':'oak','climate':'temperate','source':'INVENTED'},
 'DORNISH': {'primary':['sandstone','smooth_sandstone','terracotta'],'roof':'terracotta','climate':'arid','source':'INVENTED'},
 'IRONBORN': {'primary':['stone_bricks','cobblestone','dark_oak_planks'],'roof':'dark_oak','climate':'maritime','source':'INVENTED'},
 'SUMMER_ISLES': {'primary':['jungle_planks','mud_bricks','terracotta'],'roof':'jungle','climate':'tropical','source':'INVENTED'},
 'ESSOS_FREE_CITY': {'primary':['bricks','terracotta','stone_bricks'],'roof':'terracotta','climate':'temperate_to_warm','source':'INVENTED'},
 'DOTHRAKI_STEPPE': {'primary':['mud_bricks','oak_planks','packed_mud'],'roof':'oak','climate':'steppe','source':'INVENTED'},
 'GHISCARI': {'primary':['mud_bricks','terracotta','bricks'],'roof':'terracotta','climate':'warm_arid','source':'INVENTED'},
 'YITI': {'primary':['stone_bricks','deepslate_bricks','dark_oak_planks'],'roof':'dark_oak','climate':'continental','source':'INVENTED'},
 'SHADOWLANDS': {'primary':['deepslate_bricks','blackstone','polished_blackstone'],'roof':'deepslate','climate':'unknown','source':'INVENTED'},
 'SOTHORYOS': {'primary':['mud_bricks','jungle_planks','mossy_cobblestone'],'roof':'jungle','climate':'humid_tropical','source':'INVENTED'},
 'IBBEN': {'primary':['stone_bricks','spruce_planks','cobblestone'],'roof':'spruce','climate':'cold','source':'INVENTED'},
 'THE_WALL': {'primary':['stone_bricks','spruce_planks','packed_ice'],'roof':'spruce','climate':'subarctic','source':'INVENTED'},
}

def csvrows(path):
    with path.open(newline='',encoding='utf-8-sig') as f:
        return list(csv.DictReader(f))

def jsonread(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def slug(s):
    return re.sub(r'_+', '_',re.sub(r'[^a-z0-9]+','_',s.lower())).strip('_')

def val(record, default=None):
    return record.get('value',default) if isinstance(record,dict) else default

def grade(record, default='CONTEXT'):
    v=record.get('evidence',default) if isinstance(record,dict) else default
    if v not in ALLOWED_GRADES: raise ValueError(f'Invalid research grade {v}')
    return v

def check_qty(pop, identity):
    if not isinstance(pop,dict): raise ValueError(f'bad population object for {identity}')
    amounts=[pop.get(x) for x in ('min','baseline','max')]
    if any(not isinstance(n,(int,float)) or isinstance(n,bool) or n<0 for n in amounts):
        raise ValueError(f'bad population values for {identity}: {amounts}')
    if amounts[0]>amounts[1] or amounts[1]>amounts[2]:
        raise ValueError(f'population range not ordered for {identity}: {amounts}')
    return {k:int(pop[k]) for k in ('min','baseline','max')}

def job_distribution(site, ident):
    workers = val(site['society']['occupationsOfWorkers'],{})
    roles = val(site['society']['occupationRolesOfWorkers'],{})
    if not isinstance(workers,dict) or not isinstance(roles,dict):
        raise ValueError(f'Bad occupations for {ident}')
    total = sum(workers.values()); fine = sum(roles.values())
    if total not in (0,100) or fine!=total:
        raise ValueError(f'Occupation percentages for {ident} do not add up: {total}/{fine}')
    return {'workerSectorsPercent':workers,'workerRolesPercent':roles,'evidence':'INVENTED',
       'shareDenominator':'EMPLOYED_WORKERS_ONLY_NOT_TOTAL_RESIDENTS'}

def architecture(zone, geography, name, site_kind):
    n=name.casefold();g=geography or ''
    if site_kind=='MONUMENTAL_FRONTIER_FORT': return 'YITI'
    if zone=='SI':return 'SUMMER_ISLES'
    if zone=='SO':return 'SOTHORYOS'
    if zone=='SEE':return 'SHADOWLANDS' if 'asshai' in n or 'stygai' in n else 'YITI'
    if zone=='NEE':return 'YITI'
    if zone=='NE':return 'IBBEN' if 'ib' in n else 'DOTHRAKI_STEPPE'
    if zone=='NWE' or zone=='SWE':return 'ESSOS_FREE_CITY'
    if zone=='SE':return 'DOTHRAKI_STEPPE' if 'vaes' in n else 'GHISCARI'
    if zone=='NW':return 'THE_WALL' if 'wall_' in g or 'watch' in n or 'nightfort' in n or 'castle black' in n else 'NORTHERN'
    if zone=='SW':
        if any(k in g for k in ('dorne','sunspear','sandstone','yronwood','skyreach','hellholt')) or any(k in n for k in ('sunspear','sandstone','yronwood','hellholt','starfall','salt shore')):return 'DORNISH'
        if any(k in g for k in ('iron_islands','pyke','harlaw','goodbrother','blacktyde')) or 'pyke' in n:return 'IRONBORN'
        if any(k in g for k in ('saltpans','gulltown','oldtown','kings_landing','lannisport')):return 'WESTEROSI_COAST'
        return 'WESTEROSI_INTERIOR'
    return 'WESTEROSI_INTERIOR'

def tier_archetype(kind, habitation, name, pop, marker_id):
    n=name.casefold()
    if kind=='LEGENDARY_UNVERIFIED' or habitation=='UNVERIFIED_EXISTENCE':
        return 'SPECIAL','LEGENDARY_UNVERIFIED','DO_NOT_CONSTRUCT', 'CONTEXT'
    if kind=='MONUMENTAL_FRONTIER_FORT':
        return 'SPECIAL','MONUMENTAL_FORTRESS','CUSTOM_MONUMENTAL_BLUEPRINT','CONTEXT'
    if kind=='ABANDONED_RUIN' or 'RUIN' in habitation:
        return 'SPECIAL','ABANDONED_RUIN','RUIN_BLUEPRINT_ONLY','CONTEXT'
    if kind=='LANDMARK':
        return 'SPECIAL','LANDMARK','FEATURE_BLUEPRINT_ONLY','CONTEXT'
    if kind=='ACTIVE_FORTRESS':
        return 'SPECIAL','GARRISON_FORTRESS','CUSTOM_FORTRESS_BLUEPRINT','CONTEXT'
    if kind=='SACRED_CENTER':
        return 'SPECIAL','SACRED_CITY','CULTURAL_BLUEPRINT','CONTEXT'
    if kind=='MAJOR_CITY' or pop>=25000:
        return 'MAJOR','CITY','DENSE_URBAN_BLUEPRINT','CONTEXT'
    if kind=='TRADE_CITY':
        return ('MAJOR','PORT_CITY','DENSE_URBAN_BLUEPRINT','CONTEXT') if pop>=24000 else ('MEDIUM','MARKET_TOWN','MEDIUM_TOWN_BLUEPRINT','CONTEXT')
    if kind=='CASTLE_AND_TOWN' or 'castle' in n or 'keep' in n:
        if n in {'winterfell','harrenhal','storms end','casterly rock','the eyrie','highgarden','sunspear'}:
            return 'MAJOR','CASTLE_TOWN','MAJOR_CASTLE_BLUEPRINT','CONTEXT'
        return 'MEDIUM','CASTLE_TOWN','MEDIUM_CASTLE_BLUEPRINT','CONTEXT'
    if kind=='VILLAGE':return 'SMALL','VILLAGE','SMALL_RURAL_BLUEPRINT','CONTEXT'
    return 'SMALL','HAMLET_OR_OUTPOST','SMALL_RURAL_BLUEPRINT','INVENTED'

def holding_tier(holding, research, candidate_sites, site_lookup):
    typ=holding['type']; hid=holding['id']; geo=holding['worldLocationId']
    if typ in DOMAIN_TYPES:
        return 'NO_SETTLEMENT','ADMINISTRATIVE_DOMAIN','DOMAIN_NO_BUILD','CONFIRMED',False
    if research.get('insideKnownPlace'):
        return 'SPECIAL','CITY_SUBSITE','SUBSITE_INSIDE_PARENT','CONTEXT',False
    if candidate_sites:
        active=[site_lookup[s] for s in candidate_sites]
        valid=[s for s in active if s['tier'] not in ('NO_SETTLEMENT','SPECIAL')]
        primary=(valid or active)[0]
        return primary['tier'],primary['archetype'],'LINKED_TO_MAP_SITE','CONTEXT',False
    if typ=='ESTATE':return 'SMALL','MANOR_ESTATE','UNPLACED_SEAT','INVENTED',True
    if typ=='FORTRESS':return 'SPECIAL','GARRISON_FORTRESS','UNPLACED_SEAT','CONTEXT',True
    if typ=='CITY':return 'MAJOR','CITY','UNPLACED_SEAT','CONTEXT',True
    if typ in ('TOWN','PORT'):return 'MEDIUM','MARKET_TOWN','UNPLACED_SEAT','CONTEXT',True
    if hid in MAJOR_HOLDINGS:return 'MAJOR','CASTLE_TOWN','UNPLACED_SEAT','CONTEXT',True
    return 'MEDIUM','CASTLE_TOWN','UNPLACED_SEAT','INVENTED',True

def nearest_influences(geo, refs_by_id, influences):
    seen=set(); cursor=geo
    while cursor and cursor not in seen:
        seen.add(cursor)
        if cursor in influences:
            value=influences[cursor]
            return {'effectiveSourceLocationId':cursor,
                    'cultureWeights':value.get('cultures',{}),
                    'religionWeights':value.get('religions',{}),
                    'languageWeights':value.get('languages',{}),
                    'values':value.get('values',{}),
                    'socialNorms':value.get('socialNorms',{}),
                    'source':'project:regional_influences'}
        ref=refs_by_id.get(cursor)
        cursor=ref.get('parentId') if ref else None
    return {'effectiveSourceLocationId':None,'cultureWeights':{},'religionWeights':{},
            'languageWeights':{},'values':{},'socialNorms':{},'source':'UNRESOLVED'}

def combine_regional_influences(path_a, path_b):
    out={}
    for path in (path_a,path_b):
        for row in jsonread(path):
            loc=row['locationId']; current=out.setdefault(loc,{})
            for category in ('cultures','religions','languages','values','socialNorms'):
                current.setdefault(category,{}).update(row.get(category,{}))
    return out

def build(root=ROOT):
    paths={k:root/p.relative_to(ROOT) for k,p in SOURCE.items()}
    for k,p in paths.items():
        if not p.is_file():raise FileNotFoundError(f'Missing source {k}: {p}')
    markers=csvrows(paths['marker_catalog'])
    b2_audit={x['marker_id']:x for x in csvrows(paths['marker_audit'])}
    hraw=jsonread(paths['holdings'])['holdings']
    candidates=jsonread(paths['holding_links'])['holdings']
    rsites=jsonread(paths['site_research'])['sites']
    rholdings=jsonread(paths['holding_research'])['holdings']
    overrides=jsonread(paths['tier_overrides'])
    refs_by_id={}
    for key in ('location_references','holding_references'):
        for x in jsonread(paths[key]):refs_by_id.setdefault(x['id'],x)
    influences=combine_regional_influences(paths['regional_influences'],paths['regional_influence_extensions'])
    territorial_zones=jsonread(paths['territory_zone_catalog'])
    subregion_masks=jsonread(paths['subregion_mask_catalog'])
    if len(markers)!=253 or len(hraw)!=259 or len(rsites)!=253 or len(rholdings)!=259 or len(candidates)!=259:
        raise ValueError('B2+B3 corpus changed; expected 253 markers + 259 holdings. Review migration rather than silently losing entries.')
    def unique(items,key,label):
        out={}
        for x in items:
            id=x[key]
            if id in out:raise ValueError(f'Duplicate {label}: {id}')
            out[id]=x
        return out
    by_profile=unique(rsites,'id','B2.3 site profile')
    by_hresearch=unique(rholdings,'id','B2.3 holding profile')
    by_holding=unique(hraw,'id','political holding')
    by_candidates=unique(candidates,'holdingId','holding candidate')
    by_marker=unique(markers,'id','map marker')
    if set(by_marker)!=set(by_profile) or set(by_holding)!=set(by_hresearch) or set(by_holding)!=set(by_candidates):
        raise ValueError('Research does not cover every source marker/holding')
    if set(b2_audit)!=set(by_marker):raise ValueError('B2.2 marker audit is stale/missing observations')
    if overrides.get('schemaVersion')!=1 or set(overrides)-{'schemaVersion','sites','holdings','notes'}:
        raise ValueError('Unexpected override schema')
    if set(overrides.get('sites',{}))-set(by_marker) or set(overrides.get('holdings',{}))-set(by_holding):
        raise ValueError('Override references unknown marker or holding')
    for section in ('sites','holdings'):
        for key,data in overrides.get(section,{}).items():
            if set(data)-{'tier','archetype','note','evidence'}:raise ValueError('Unsupported tier override field')
            if data.get('tier') and data['tier'] not in TIERS:raise ValueError('Unknown tier override')
            if data.get('evidence','CONTEXT') not in ALLOWED_GRADES:raise ValueError('Invalid override grade')
            if data.get('evidence')=='CONFIRMED':raise ValueError('Generation tiers are planning decisions, not confirmed canon')

    marker_records=[]
    for marker in sorted(markers,key=lambda x:x['id']):
        mid=marker['id']; profile=by_profile[mid]; audit=b2_audit[mid]
        if profile['map']['observationId']!=mid:raise ValueError('Misindexed research observation')
        x=int(round(float(marker['minecraft_x'])));z=int(round(float(marker['minecraft_z'])))
        if abs(x)>30_000_000 or abs(z)>30_000_000:raise ValueError(f'{mid} outside Minecraft world border')
        if (x,z)!=(profile['map']['candidateX'],profile['map']['candidateZ']):raise ValueError(f'{mid} research coordinate mismatch')
        primary=profile['map']['primaryPopulationMarkerId']
        if primary not in by_marker:raise ValueError(f'{mid} has missing primary population marker {primary}')
        geo=val(profile['geography']['geographicId'])
        record={
           'markerId':mid,'siteId':'site_'+slug(primary),'primaryMarkerId':primary,
           'name':profile['displayName'],'zone':marker['zone'],
           'workingAnchor':{'x':x,'z':z,'y':None,'status':'ACCEPTED_FOR_PLANNING',
                   'source':'project:marker_catalog','userReview':'USER_ACCEPTED_WORKING_MAP_ANCHORS_2026_10_21',
                   'isBuildApproved':False},
           'geographicId':geo,'sourceCanonicalGeographicId':marker.get('canonical_place_id') or None,
           'markerIdentityStatus':audit['link_status'],
           'atlasCoordinateReview':marker.get('review_status',''),
           'landMaskStatus':marker.get('land_mask_status',''),
           'sourceProvenance':profile['sourceIds'],
           'sharedSiteKey':marker.get('shared_site_key') or None,
           'groupId':marker.get('group_id') or None,
           'sourceImagePixel':{'x':marker.get('pixel_x') or None,'y':marker.get('pixel_y') or None},
           'primaryPopulationOwner':mid==primary,
        }
        marker_records.append(record)
    # One physical site per independently counted map identity. Preserve every raw observation.
    markers_by_site=defaultdict(list)
    for m in marker_records:markers_by_site[m['siteId']].append(m)
    physical_sites=[]
    for sid,members in sorted(markers_by_site.items()):
        owner=next((m for m in members if m['markerId']==m['primaryMarkerId']),None)
        if owner is None:raise ValueError(f'No primary observation in group {sid}')
        source=by_profile[owner['markerId']]
        site_kind=val(source['site']['kind'],'UNKNOWN')
        habitation=val(source['site']['habitationAt298AC'],'UNVERIFIED_EXISTENCE')
        population=check_qty(val(source['society']['population']),sid)
        if not source['map']['countsIndependentResidents']:
            raise ValueError(f'Duplicate population owner {sid}')
        tier,archetype,strategy,tier_evidence=tier_archetype(site_kind,habitation,owner['name'],population['baseline'],owner['markerId'])
        override=overrides.get('sites',{}).get(owner['markerId'],{})
        if override:
            tier=override.get('tier',tier);archetype=override.get('archetype',archetype)
            strategy='MANUAL_SCENARIO_OVERRIDE';tier_evidence=override.get('evidence','CONTEXT')
        geo=owner['geographicId']
        palette=architecture(owner['zone'],geo,owner['name'],site_kind)
        influence=nearest_influences(geo,refs_by_id,influences)
        site={
            'siteId':sid,'primaryMarkerId':owner['markerId'],'markerIds':sorted(m['markerId'] for m in members),
            'geographicId':geo,'name':owner['name'],'zone':owner['zone'],
            'workingAnchor':owner['workingAnchor'],
            'physicalType':{'value':site_kind,'evidence':grade(source['site']['kind']),
                            'sources':source['site']['kind'].get('sources',[])},
            'habitation':{'value':habitation,'evidence':grade(source['site']['habitationAt298AC']),
                            'sources':source['site']['habitationAt298AC'].get('sources',[])},
            'tier':tier,'tierEvidence':tier_evidence,'tierNote':override.get('note','Source-derived mod generation classification, not a canonical building size.'),
            'archetype':archetype,'generatorStrategy':strategy,
            'materialPalette':palette,'materialPaletteEvidence':'INVENTED',
            'culturalContext':source['society']['culture'],
            'religiousContext':source['society']['religion'],
            'regionalInfluences':influence,
            'government':source['politics']['leadingStructure'],
            'regionalPoliticalInfluences':source['politics']['regionalInfluences'],
            'populationPlanning':{'min':population['min'],'baseline':population['baseline'],'max':population['max'],
                     'baselineEvidence':val(source['society']['population']).get('baselineEvidence','INVENTED'),
                     'rangeEvidence':val(source['society']['population']).get('planningRangeEvidence','INVENTED'),
                     'spawnedIndividualCount':None,'isAuthoringEstimateOnly':True},
            'demography':source['society']['demographyShares'],
            'laborParticipation':source['society']['laborParticipation'],
            'occupationDistribution':job_distribution(source,sid),
            'occupationArchetype':source['society']['occupationalArchetype'],
            'historicalNotes':source['site']['historicalOrPhysicalNotes'],
            'sourceIds':source['sourceIds'],
            'linkedPoliticalHoldingIds':[],
            'canAutoGenerate':False,'needsTerrainFootprintValidation':True,
            'keepExistingWorldStructures':True,'createResidentsNow':False,'createHoldingsNow':False,
        }
        physical_sites.append(site)
    site_by_id=unique(physical_sites,'siteId','physical site')
    marker_by_id=unique(marker_records,'markerId','marker observation')
    physical_by_geo=defaultdict(list)
    for s in physical_sites:
        if s['geographicId']:physical_by_geo[s['geographicId']].append(s['siteId'])

    holding_records=[]
    for h in sorted(hraw,key=lambda x:x['id']):
        hid=h['id'];research=by_hresearch[hid];candidate=by_candidates[hid]
        if candidate['worldLocationId']!=h['worldLocationId'] or research['worldLocationId']!=h['worldLocationId']:
            raise ValueError(f'Stale link/research for holding {hid}')
        candidate_ids=[x['markerId'] for x in candidate['candidates']]
        if set(candidate_ids)-set(by_marker):raise ValueError(f'Unknown marker referenced from holding {hid}')
        physical_ids=sorted({marker_by_id[x]['siteId'] for x in candidate_ids})
        # Some holdings intentionally share the same geographic point and no new residents.
        tier,archetype,placement,tier_grade,needs_unique=holding_tier(h,research,physical_ids,site_by_id)
        ov=overrides.get('holdings',{}).get(hid,{})
        if ov:
            tier=ov.get('tier',tier);archetype=ov.get('archetype',archetype)
            tier_grade=ov.get('evidence','CONTEXT')
        parent_place=research.get('insideKnownPlace')
        parent_sites=physical_by_geo.get(parent_place,[]) if parent_place else []
        # Known nested keeps/estates share the parent geographic anchor, never a separate city population.
        linked_anchors=sorted(set(physical_ids+parent_sites))
        record={
          'holdingId':hid,'name':h['name'],'holdingType':h['type'],
          'worldLocationId':h['worldLocationId'],
          'parentHoldingId':h.get('parentHoldingId'),
          'deJureDynastyId':h.get('deJureDynastyId'),
          'ownerDynastyId':h.get('ownerDynastyId'),
          'holderNpcId':h.get('holderNpcId'),
          'governmentOrganizationId':h.get('governmentOrganizationId'),
          'linkedTitleId':h.get('linkedTitleId'),
          'politicsEvidence':'CONFIRMED_PROJECT_SCENARIO',
          'politicalStatus':h.get('status','ACTIVE'),
          'isPoliticalDomain':h['type'] in DOMAIN_TYPES,
          'tier':tier,'tierEvidence':tier_grade,'archetype':archetype,
          'placementStatus':placement,'hasIndependentSeat':needs_unique,
          'candidateMarkerIds':candidate_ids,'linkedSiteIds':physical_ids,
          'parentCityGeographicId':parent_place,
          'parentCitySiteIds':sorted(parent_sites),
          'referenceAnchorSiteIds':linked_anchors,
          'hasIndependentPopulation':bool(needs_unique),
          'populationPlanning': research['populationPlanning'],
          'regionalCulture':research['regionalCulture'],
          'regionalReligion':research['regionalReligion'],
          'geographicParentId':research.get('geographicParentId'),
          'region':nearest_influences(research['worldLocationId'],refs_by_id,influences),
          'potentialBuildingSiteApproved':False,'constructionApproved':False,
          'notYetGenerated':True,'needsGroundSiteResolution':True,
          'notes':ov.get('note',''),
        }
        if h['type'] in DOMAIN_TYPES and record['hasIndependentPopulation']:
            raise ValueError('Political domain should never have independent population')
        holding_records.append(record)
    for h in holding_records:
        for sid in h['linkedSiteIds']:site_by_id[sid]['linkedPoliticalHoldingIds'].append(h['holdingId'])
    for s in physical_sites:s['linkedPoliticalHoldingIds'].sort()
    # Double-count check: primary sites must be one population budget; nested holdings cannot generate extra people.
    if any(len(site['markerIds'])!=len(set(site['markerIds'])) for site in physical_sites):raise ValueError('Duplicate physical marker within group')
    if len({m['markerId'] for m in marker_records})!=253:raise ValueError('Lost marker')
    if len({h['holdingId'] for h in holding_records})!=259:raise ValueError('Lost holding')
    if sum(len(s['markerIds']) for s in physical_sites)!=253:raise ValueError('Duplicate marker site identity')
    if any(x['workingAnchor']['isBuildApproved'] or x['canAutoGenerate'] for x in physical_sites):raise ValueError('Unsafe build approval')
    if any(s['tier'] not in TIERS for s in physical_sites) or any(h['tier'] not in TIERS for h in holding_records):raise ValueError('Invalid tier')
    # Every existing authored REGION/REALM/SUBREGION reference gets an inspectable
    # regional profile. Territorial polygons and subregion-mask entries are JOINED
    # by geographic ID, not copied into a second conflicting spatial authority.
    region_ids={key for key,ref in refs_by_id.items() if ref.get('kind') in REGIONAL_KINDS}
    zone_by_id=defaultdict(list)
    for zone in territorial_zones:
        zone_by_id[zone['locationId']].append({'source':'territory_zones.json',
             'priority':zone.get('priority'), 'provenance':zone.get('provenance'),
             'polygonCount':len(zone.get('polygons',[]))})
        region_ids.add(zone['locationId'])
    for mask in subregion_masks:
        for row in mask.get('entries',[]):
            zone_by_id[row['locationId']].append({'source':'subregion_masks.json',
                 'zoneId':mask['zoneId'],'hexColor':row.get('hexColor'),
                 'imageResource':mask.get('imageResource')})
            region_ids.add(row['locationId'])
    region_profiles=[]
    for rid in sorted(region_ids):
        ref=refs_by_id.get(rid,{})
        descendants=[]
        for site in physical_sites:
            cursor=site['geographicId'];visited=set()
            while cursor and cursor not in visited:
                visited.add(cursor)
                if cursor==rid:
                    descendants.append(site['siteId']);break
                cursor=refs_by_id.get(cursor,{}).get('parentId')
        domains=[h['holdingId'] for h in holding_records if h['worldLocationId']==rid]
        region_profiles.append({
          'regionId':rid,'displayName':ref.get('displayName',rid),
          'geographicKind':ref.get('kind','UNCLASSIFIED_MAP_ZONE'),
          'parentLocationId':ref.get('parentId'),
          'sourceProvenance':ref.get('provenance') or 'PROJECT_ZONE_DATA',
          'sourceNote':ref.get('sourceNote',''),
          'spatialDefinitions':zone_by_id.get(rid,[]),
          'regionalInfluences':nearest_influences(rid,refs_by_id,influences),
          'directDomainHoldingIds':domains,
          'descendantPhysicalSiteIds':sorted(set(descendants)),
          'populationRule':'DERIVE_ONLY_FROM_UNIQUE_INDIVIDUAL_NPC_RESIDENCES_NEVER_EXTRA_COHORT',
          'regionPopulationEstimate':None,
          'noAutoConstruction':True,
        })
    # Completeness of the existing CANONICAL WORLD REFERENCE catalog is separate
    # from the 253 red-X observations and the 259 political holdings. Keep any
    # named reference with no atlas marker or holding as an explicit unresolved
    # place, not as an invented settlement coordinate.
    marker_ids_by_geo=defaultdict(list)
    for m in marker_records:
        if m['geographicId']:marker_ids_by_geo[m['geographicId']].append(m['markerId'])
    holding_ids_by_geo=defaultdict(list)
    for h in holding_records: holding_ids_by_geo[h['worldLocationId']].append(h['holdingId'])
    references=[]
    for rid,ref in sorted(refs_by_id.items()):
        references.append({
          'referenceId':rid,'displayName':ref.get('displayName',rid),
          'geographicKind':ref.get('kind','UNKNOWN'),
          'parentId':ref.get('parentId'),
          'sourceProvenance':ref.get('provenance') or 'PROJECT_AUTHORING',
          'sourceNote':ref.get('sourceNote',''),
          'mappedMarkerIds':sorted(marker_ids_by_geo.get(rid,[])),
          'linkedPoliticalHoldingIds':sorted(holding_ids_by_geo.get(rid,[])),
          'physicalSiteIds':sorted(physical_by_geo.get(rid,[])),
          'regionalInfluenceSourceId':nearest_influences(rid,refs_by_id,influences)['effectiveSourceLocationId'],
          'notAnApprovedBuildingPosition':True,
        })
    summary={
      'referencePlaceCount':len(references),
      'referencePlacesWithoutMarker':sum(not r['mappedMarkerIds'] for r in references),
      'regionProfileCount':len(region_profiles),
      'mappedRegionProfiles':sum(bool(r['spatialDefinitions']) for r in region_profiles),
      'markerCount':len(marker_records),'physicalSiteCount':len(physical_sites),'holdingCount':len(holding_records),
      'siteTiers':dict(sorted(Counter(s['tier'] for s in physical_sites).items())),
      'holdingTiers':dict(sorted(Counter(h['tier'] for h in holding_records).items())),
      'unplacedHoldingSeats':sum(h['placementStatus']=='UNPLACED_SEAT' for h in holding_records),
      'mappedHoldingRecords':sum(bool(h['linkedSiteIds']) for h in holding_records),
      'politicalDomains':sum(h['isPoliticalDomain'] for h in holding_records),
      'mapMarkersWithNoPoliticalHolding':sum(not any(m['markerId'] in h['candidateMarkerIds'] for h in holding_records) for m in marker_records),
      'generationApprovedCount':0,'populationInstantiatedCount':0,
      'anchorPolicy':'USER_ACCEPTED_FOR_PLANNING_NO_FOOTPRINT_APPROVAL',
    }
    digest={k:hashlib.sha256(v.read_bytes()).hexdigest() for k,v in paths.items()}
    result={'schemaVersion':1,'scenario':'agot_298_ac','buildAuthority':'PLANNING_ONLY',
      'policy':{'workingMapCoordinatesAccepted':True,'terrainFootprintsApproved':False,
       'doNotGenerateChunks':True,'doNotCreateOrModifyPopulation':True,'noDuplicatePhysicalSites':True,
       'sourceEvidenceGrades':['CONFIRMED','CONTEXT','INVENTED']},
      'summary':summary,'sourceFileSha256':digest,'markers':marker_records,
      'physicalSites':physical_sites,'holdings':holding_records,
      'regions':region_profiles,'referencePlaces':references}
    return result

def export_rows(result):
    sites={s['siteId']:s for s in result['physicalSites']}
    marker_rows=[]
    for m in result['markers']:
        s=sites[m['siteId']]
        marker_rows.append({'marker_id':m['markerId'],'name':m['name'],'geographic_id':m['geographicId'] or '',
        'site_id':m['siteId'],'primary_marker':m['primaryMarkerId'],'zone':m['zone'],
        'x':m['workingAnchor']['x'],'z':m['workingAnchor']['z'],
        'tier':s['tier'],'archetype':s['archetype'],'habitation':s['habitation']['value'],
        'population_baseline_plan':s['populationPlanning']['baseline'] if m['primaryPopulationOwner'] else 'SHARED_SITE',
        'linked_holdings':';'.join(s['linkedPoliticalHoldingIds']),
        'identity_evidence':m['markerIdentityStatus'],'culture':str(val(s['culturalContext'])),
        'religion':str(val(s['religiousContext'])),'palettes':s['materialPalette'],
        'planning_anchor_accepted':'YES','construction_approved':'NO','manual_review':''})
    holding_rows=[]
    for h in result['holdings']:
        holding_rows.append({'holding_id':h['holdingId'],'name':h['name'],'type':h['holdingType'],
          'tier':h['tier'],'archetype':h['archetype'],'placement_status':h['placementStatus'],
          'geographic_id':h['worldLocationId'],'candidate_markers':';'.join(h['candidateMarkerIds']),
          'linked_site_ids':';'.join(h['linkedSiteIds']),'parent_city':h['parentCityGeographicId'] or '',
          'owner_house':h['ownerDynastyId'] or '', 'individual_holder':h['holderNpcId'] or '',
          'parent_holding':h['parentHoldingId'] or '',
          'regional_culture':str(val(h['regionalCulture'])),'regional_religion':str(val(h['regionalReligion'])),
          'needs_unique_site':'YES' if h['hasIndependentSeat'] else 'NO',
          'construction_approved':'NO','review_notes':h['notes']})
    return marker_rows,holding_rows

def render_html(result):
    # static, file://-compatible, no APIs/network.
    blob=json.dumps(result,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c').replace('>','\\u003e').replace('&','\\u0026')
    template=(Path(__file__).parent/'b2_b3_registry_dashboard.html').read_text(encoding='utf-8')
    return template.replace('__REGISTRY_JSON__',blob)

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--check',action='store_true',help='Verify generated registry matches committed sources')
    ap.add_argument('--dry-run',action='store_true',help='Report coverage without updating files')
    args=ap.parse_args()
    result=build(); payload=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    print(json.dumps(result['summary'],indent=2))
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8')!=payload:
            raise SystemExit('B2/B3 derived registry is missing or stale. Re-run without --check.')
        print('CHECK PASSED: B2/B3 site and holding registry is current')
    elif not args.dry_run:
        OUTPUT.parent.mkdir(parents=True,exist_ok=True);OUTPUT.write_text(payload,encoding='utf-8')
        REPORT.mkdir(parents=True,exist_ok=True)
        m,h=export_rows(result)
        for filename,records in [('ALL_253_MARKERS.csv',m),('ALL_259_HOLDINGS.csv',h)]:
            with (REPORT/filename).open('w',encoding='utf-8-sig',newline='') as f:
                w=csv.DictWriter(f,fieldnames=list(records[0]),delimiter=';');w.writeheader();w.writerows(records)
        regions=[{'region_id':r['regionId'],'name':r['displayName'],
           'kind':r['geographicKind'],'parent_location_id':r['parentLocationId'] or '',
           'mapped_boundary_or_mask':'YES' if r['spatialDefinitions'] else 'NO',
           'regional_influence_source':r['regionalInfluences']['effectiveSourceLocationId'] or '',
           'connected_domain_holdings':';'.join(r['directDomainHoldingIds']),
           'descendant_physical_sites':len(r['descendantPhysicalSiteIds'])} for r in result['regions']]
        with (REPORT/'ALL_REGIONS_AND_ZONES.csv').open('w',encoding='utf-8-sig',newline='') as f:
            w=csv.DictWriter(f,fieldnames=list(regions[0]),delimiter=';');w.writeheader();w.writerows(regions)
        reference_rows=[{'reference_id':p['referenceId'],'name':p['displayName'],
         'kind':p['geographicKind'],'parent':p['parentId'] or '',
         'provenance':p['sourceProvenance'],'mapped_marker_ids':';'.join(p['mappedMarkerIds']),
         'linked_holding_ids':';'.join(p['linkedPoliticalHoldingIds']),
         'physical_site_ids':';'.join(p['physicalSiteIds'])} for p in result['referencePlaces']]
        with (REPORT/'ALL_CANONICAL_GEOGRAPHIC_REFERENCES.csv').open('w',encoding='utf-8-sig',newline='') as f:
            w=csv.DictWriter(f,fieldnames=list(reference_rows[0]),delimiter=';');w.writeheader();w.writerows(reference_rows)
        (REPORT/'KnownWorld_B2_B3_Overview.html').write_text(render_html(result),encoding='utf-8')
        print('Wrote:',OUTPUT.relative_to(ROOT));print('Wrote:',REPORT.relative_to(ROOT))

if __name__=='__main__':main()
