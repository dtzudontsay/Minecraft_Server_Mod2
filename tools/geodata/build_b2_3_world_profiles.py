#!/usr/bin/env python3
"""Generate provisional, source-labelled 298 AC profiles for every B2 active map marker.

NO Minecraft runtime or world data is changed. Run from the project checkout:
  py tools/geodata/build_b2_3_world_profiles.py

All percentages and unsourced headcounts are model estimates, not historical canon.
See accompanying docs/research/B2_3_WORLD_SITE_RESEARCH_PROTOCOL.md.
"""
from __future__ import annotations
import csv
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GEO = ROOT / 'tools/geodata'
REF = ROOT / 'src/main/resources/data/knownworld/reference'
SCN = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac'
OUTPUT = GEO / 'output/b2_3_world_profiles'
OVERRIDES = GEO / 'b2_3_world_research_overrides.json'
MARKERS = GEO / 'output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv'
LINKS = GEO / 'output/b2_holding_links/markers_253_audit.csv'
RESEARCH_DIR = ROOT / 'docs/research/geography'

WEB = 'https://awoiaf.westeros.org/index.php/'


def read_csv(path):
    with path.open(newline='', encoding='utf-8-sig') as f:
        return list(csv.DictReader(f))


def slug(s):
    s = s.lower().replace('’', '').replace("'", '')
    return re.sub(r'_+', '_', re.sub(r'[^a-z0-9]+', '_', s)).strip('_')


def record(value, grade, source_ids=None, reason=''):
    if grade not in ('CONFIRMED', 'CONTEXT', 'INVENTED'):
        raise ValueError(f'unknown evidence grade: {grade}')
    return {'value': value, 'evidence': grade, 'sources': list(source_ids or []), 'note': reason}


def enforce_percentages(jobs):
    if sum(jobs.values()) != 100 or any(v < 0 or v > 100 for v in jobs.values()):
        raise ValueError('occupation model must sum to exactly 100 percent')
    return jobs


# Occupation shares of ACTIVE WORKERS, never shares of the entire population.
JOBS = {
 'rural_north': {'farming_herding': 45, 'fishing_forestry': 15, 'crafts_mining': 12, 'transport_trade': 9, 'service_domestic': 7, 'security_military': 7, 'governance_religion': 5},
 'rural_south': {'farming_herding': 55, 'fishing_forestry': 6, 'crafts_mining': 14, 'transport_trade': 9, 'service_domestic': 7, 'security_military': 6, 'governance_religion': 3},
 'fortress': {'farming_herding': 7, 'fishing_forestry': 4, 'crafts_mining': 14, 'transport_trade': 12, 'service_domestic': 17, 'security_military': 38, 'governance_religion': 8},
 'wall_active': {'farming_herding': 3, 'fishing_forestry': 5, 'crafts_mining': 18, 'transport_trade': 14, 'service_domestic': 12, 'security_military': 42, 'governance_religion': 6},
 'major_port': {'farming_herding': 4, 'fishing_forestry': 12, 'crafts_mining': 22, 'transport_trade': 31, 'service_domestic': 19, 'security_military': 7, 'governance_religion': 5},
 'trade_city': {'farming_herding': 8, 'fishing_forestry': 4, 'crafts_mining': 26, 'transport_trade': 27, 'service_domestic': 22, 'security_military': 7, 'governance_religion': 6},
 'sacred_city': {'farming_herding': 2, 'fishing_forestry': 2, 'crafts_mining': 11, 'transport_trade': 22, 'service_domestic': 30, 'security_military': 8, 'governance_religion': 25},
 'island_port': {'farming_herding': 12, 'fishing_forestry': 24, 'crafts_mining': 17, 'transport_trade': 23, 'service_domestic': 12, 'security_military': 7, 'governance_religion': 5},
 'slaving_city': {'farming_herding': 12, 'fishing_forestry': 5, 'crafts_mining': 20, 'transport_trade': 24, 'service_domestic': 22, 'security_military': 11, 'governance_religion': 6},
 'steppe': {'farming_herding': 65, 'fishing_forestry': 4, 'crafts_mining': 8, 'transport_trade': 10, 'service_domestic': 6, 'security_military': 5, 'governance_religion': 2},
 'shadow_port': {'farming_herding': 0, 'fishing_forestry': 3, 'crafts_mining': 14, 'transport_trade': 24, 'service_domestic': 23, 'security_military': 8, 'governance_religion': 28},
 'frontier_fort': {'farming_herding': 2, 'fishing_forestry': 2, 'crafts_mining': 13, 'transport_trade': 14, 'service_domestic': 16, 'security_military': 48, 'governance_religion': 5},
 'ruin': {'farming_herding': 0, 'fishing_forestry': 0, 'crafts_mining': 0, 'transport_trade': 0, 'service_domestic': 0, 'security_military': 0, 'governance_religion': 0},
}
for k,v in JOBS.items():
    if k != 'ruin': enforce_percentages(v)

# All ID lists are deliberately inspectable and editable in one place.
RUINS = {
 'NW-004','NW-005','NW-016','NW-023','SW-004','SW-063','SW-078',
 'NWE-004','NWE-007','NWE-008','NWE-009','NWE-010','NWE-013','NWE-014','NWE-015','NWE-016',
 'NE-008','NE-010','SE-001','SE-002','SE-003','SE-004','SE-005','SE-014','SE-016','SE-017','SE-018',
 'SE-024','SEE-008','SEE-009','SWE-005','SWE-020','SO-004','SO-005',
}
# Not all ruin status assumptions above are verified; override grade remains explicit.
UNVERIFIED_RUIN = {'SE-001','SE-002','SE-003','SE-004','SE-005','SE-014','SE-016','SE-017','SE-018','SWE-005','SO-004','SO-005','SEE-008'}
LEGENDARY = {'NEE-002','NEE-004','SEE-001'}
FIVE_FORTS = {'NEE-005','NEE-006','NEE-007','NEE-008','NEE-009'}
WALL = {'NW-008','NW-009','NW-010','NW-011','NW-012','NW-013','NW-014','NW-015','NW-016','NW-017','NW-018','NW-019','NW-020','NW-021','NW-MISSING-WALL-01','NW-MISSING-WALL-02','NW-MISSING-WALL-03','NW-MISSING-WALL-04'}
WALL_MANNED = {'NW-008','NW-017','NW-020'}
PORT_NAMES = {'white harbor','gulltown','kings landing','oldtown','lannisport','maidenpool','duskendale','weeping town','saltpans','port of ibben','braavos','pentos','lorath','myr','tyrosh','lys','volantis','nefer','qarth','new ghis','tall trees town','ebonhead','last lament','lotus point','lotus port','port yhos','port moraq','astapor','yunkai','meereen'}
LARGE_CITY = {'kings landing','oldtown','lannisport','gulltown','white harbor','braavos','pentos','myr','tyrosh','lys','volantis','qarth','yin','meereen','yunkai','astapor','norvos','qohor','nefer','asshai','leng yi','jinqi','new ghis'}
# Explicit population *numbers* supported by primary text only for King's Landing.
# Other population numbers/ranges below are intentionally INVENTED scenario envelopes.

ZONE_DEFAULTS = {
 'NW': ('Westerosi northern/Wall/Vale context; refine by parent region', 'Old gods / Seven / Watch traditions depending on actual site', 'northern border and feudal influences'),
 'SW': ('Westerosi region-specific culture; do not assume all south Westeros identical', 'Faith of the Seven / drowned god / old gods according to local site', 'feudal lords, cities and coastal commerce'),
 'NWE': ('western Essosi Free City, Rhoynar or former Sarnori context', 'mixed Essosi faiths; local identity needed', 'Free City trade or ruined Sarnori successor landscape'),
 'NE': ('Ibbenese / Dothraki / Sarnori / eastern caravan context', 'local Ibbenese / Dothraki / mixed rites', 'nomadic trade routes and northern maritime influence'),
 'NEE': ('N\'ghai / Yi Ti / frontier cultures', 'YiTish / local frontier traditions; exact rites unknown', 'eastern imperial frontier and steppe relations'),
 'SWE': ('Valyrian-derived / Free City / Rhoynar riverside context', 'local mixed urban faiths; no universal uniform religion', 'Valyrian legacy and river trading networks'),
 'SE': ('Ghiscari / Dothraki / Qartheen / Lhazareen context', 'local Ghiscari / Lhazareen / Dothraki / Qartheen faiths', 'trade routes, slaver polities and steppe expansion'),
 'SEE': ('YiTish / Leng / Shadow Lands cultures', 'local cults, imperial tradition or syncretism', 'Jade Sea trade, empire, legends'),
 'SO': ('Sothoryi frontier / abandoned colonial site', 'unknown; never assign contemporary worship to a ruin', 'hazardous frontier, ruins and past colonial trade'),
 'SI': ('Summer Islander', 'Summer Islander deities and priestess traditions', 'maritime trade, family/temple institutions'),
}
RESEARCH_FILENAMES = {'NW':'NW','SW':'SW','NWE':'NWE','NE':'NE','NEE':'NEE','SWE':'SWE','SE':'SE','SEE':'SEE','SO':'SO','SI':'SI'}


def detailed_jobs(broad):
    # 21 concrete role targets. Group sums reproduce the parent categories exactly.
    # Never adds workers; represents the SAME percentage split at finer fidelity.
    mapping={
      'farming_herding': ['grain_farmer','gardener','herder'],
      'fishing_forestry': ['fisher','forester'],
      'crafts_mining': ['blacksmith','carpenter','mason','weaver','miner'],
      'transport_trade': ['merchant','carter','sailor','market_seller'],
      'service_domestic': ['cook','servant','innkeeper'],
      'security_military': ['guard','soldier'],
      'governance_religion': ['administrator','religious_attendant']}
    out={}
    for category,roles in mapping.items():
        p=broad[category]; base,remainder=divmod(p,len(roles))
        for index,role in enumerate(roles): out[role]=base+(1 if index<remainder else 0)
    if sum(out.values()) != sum(broad.values()): raise AssertionError('job split lost workers')
    return out


def default_population(kind, zone):
    if kind == 'ABANDONED_RUIN': return (0,0,0)
    if kind == 'LEGENDARY_UNVERIFIED': return (0,0,0)  # not evidence of actual absence; no auto-spawn
    if kind == 'ACTIVE_FORTRESS': return (300, 120, 1500)
    if kind == 'MONUMENTAL_FRONTIER_FORT': return (1600, 700, 3500)
    if kind == 'VILLAGE': return (550, 120, 1700)
    if kind == 'CASTLE_AND_TOWN': return (2100, 350, 8500)
    if kind == 'TRADE_CITY': return (33000, 7000, 110000)
    if kind == 'MAJOR_CITY': return (95000, 25000, 350000)
    if kind == 'SACRED_CENTER': return (800, 120, 3500)
    if kind == 'LANDMARK': return (0, 0, 0)
    return (400, 80, 2000)


def guess_kind(mid, label, zone, linked_holding):
    n = slug(label)
    if mid in LEGENDARY: return 'LEGENDARY_UNVERIFIED'
    if mid in RUINS or (mid in WALL and mid not in WALL_MANNED): return 'ABANDONED_RUIN'
    if mid in FIVE_FORTS: return 'MONUMENTAL_FRONTIER_FORT'
    if mid in WALL_MANNED or n in ('moat_cailin','bloody_gate'): return 'ACTIVE_FORTRESS'
    if n in ('vaes_dothrak',): return 'SACRED_CENTER'
    if n in ('fist_of_the_first_men','the_sorrows','water_gardens','quiet_isle','north_arbor','south_arbor','central_arbor','the_nightfort'):
        return 'LANDMARK'
    if n in LARGE_CITY: return 'MAJOR_CITY'
    if n in PORT_NAMES or n in ('seagard','lordsport','fairmarket','stoney_sept','barrowton','moles_town','sisterton','tumbleton','stonehelm','old_oak'): return 'TRADE_CITY'
    if mid.startswith('NW-') and n in ('thenn','ramsgate','greywater_watch','hardhome','crasters_keep'): return 'VILLAGE'
    if mid.startswith('SE-') and (n.startswith('vaes_') or n.startswith('krazaaj')): return 'ABANDONED_RUIN'
    if linked_holding and linked_holding.get('type') == 'CITY': return 'MAJOR_CITY'
    if linked_holding and linked_holding.get('type') in ('TOWN','PORT'): return 'TRADE_CITY'
    if zone in ('SI',): return 'TRADE_CITY'
    return 'CASTLE_AND_TOWN' if zone in ('NW','SW') else 'VILLAGE'


def job_kind(kind, zone, name):
    n=slug(name)
    if kind in ('ABANDONED_RUIN','LEGENDARY_UNVERIFIED','LANDMARK'): return 'ruin'
    if kind == 'MONUMENTAL_FRONTIER_FORT': return 'frontier_fort'
    if kind == 'ACTIVE_FORTRESS': return 'wall_active' if zone=='NW' else 'fortress'
    if kind == 'SACRED_CENTER': return 'sacred_city'
    if n in ('asshai',): return 'shadow_port'
    if n in ('astapor','yunkai','meereen','volantis','new_ghis'): return 'slaving_city'
    if zone == 'SI': return 'island_port'
    if kind in ('TRADE_CITY','MAJOR_CITY'): return 'major_port' if n in PORT_NAMES else 'trade_city'
    if zone in ('NE','NWE','SE'): return 'steppe' if ('vaes' in n or 'dothr' in n) else 'rural_south'
    if zone in ('NW','NEE'): return 'rural_north'
    return 'rural_south'


def classify_population(name, kind, estimate, overrides):
    if overrides.get('population'):
        d = overrides['population']
        return record({'baseline': d['baseline'], 'min': d['min'], 'max': d['max'],
                       'baselineEvidence':d.get('grade','INVENTED'), 'planningRangeEvidence':'INVENTED',
                       'unit': 'unique individual permanent residents, NOT simultaneously spawned entities'},
                      'CONTEXT' if d.get('grade')=='CONFIRMED' else 'INVENTED', d.get('sources',[]),
                      (d.get('note','')+' The min/max bounds are invented planning tolerances, even when baseline is sourced.').strip())
    v,low,high = estimate
    return record({'baseline':v,'min':low,'max':high,'baselineEvidence':'INVENTED','planningRangeEvidence':'INVENTED',
                   'unit':'unique individual permanent residents, NOT simultaneously spawned entities'},'INVENTED',[],
                  'Provisional scenario planning estimate; no census supported. Not yet enrolled or spawned.')


def read_refs():
    out=defaultdict(list)
    for p in ('locations.json','locations_holdings.json'):
        for row in json.loads((REF/p).read_text(encoding='utf-8')):
            out[row['id']].append(row)
    return out


def read_holding_profiles():
    j=json.loads((SCN/'landed_holdings.json').read_text(encoding='utf-8'))
    holdings_by_loc=defaultdict(list)
    for h in j['holdings']:
        holdings_by_loc[h['worldLocationId']].append(h)
    return holdings_by_loc


def indexes():
    out=defaultdict(list)
    for src in ('research_marker_name_index.csv','sw_marker_name_crosswalk.csv'):
        for r in read_csv(GEO/src):
            if r.get('name'): out[r['id']].append((r['name'].strip(),src))
    return out


def main():
    if not MARKERS.exists():
        raise SystemExit('Missing marker CSV; run from repository with B2.2 data present.')
    marker_rows=read_csv(MARKERS)
    if len(marker_rows)!=253: raise SystemExit(f'Expected 253 active markers, got {len(marker_rows)}; review corpus/version before updating')
    marker_ids=[r['id'] for r in marker_rows]
    if len(set(marker_ids))!=253: raise SystemExit('Duplicate marker ID in source data')
    link_rows={r['marker_id']:r for r in read_csv(LINKS)}
    ref=read_refs()
    houses=read_holding_profiles()
    idx=indexes()
    research=json.loads(OVERRIDES.read_text(encoding='utf-8'))
    if research.get('schemaVersion')!=1: raise SystemExit('Override schema invalid')
    overrides=research['sites']
    unknown=set(overrides)-set(marker_ids)
    if unknown: raise SystemExit('Overrides reference unknown marker IDs: '+str(sorted(unknown)))
    # A named city can have two image observations in overlapping map zones.
    # Choose a deterministic PRIMARY per canonical geographic identity;
    # secondary observations remain in research but NEVER count new residents.
    location_markers=defaultdict(list)
    for row in marker_rows:
        marker_id=row['id']
        linked=(link_rows.get(marker_id,{}).get('candidate_location_id') or '').strip()
        loc=(row.get('canonical_place_id') or '').strip() or linked
        if loc: location_markers[loc].append(marker_id)
    primary_for_location={k:sorted(v)[0] for k,v in location_markers.items() if len(v)>1}

    output=[]
    for m in marker_rows:
        mid=m['id']; zone=m['zone']; o=overrides.get(mid,{})
        existing=link_rows.get(mid,{})
        choices=[x[0] for x in idx.get(mid,[]) if x[0]]
        name=o.get('displayName') or (m.get('name') or '').strip() or (choices[0] if choices else f'Unidentified {mid}')
        source_loc=(m.get('canonical_place_id') or '').strip()
        linked_loc=(existing.get('candidate_location_id') or '').strip()
        geo_id=source_loc or linked_loc or None
        local_refs=ref.get(geo_id,[]) if geo_id else []
        matched_holdings=houses.get(linked_loc,[]) if linked_loc else []
        marker_links=record([x['id'] for x in matched_holdings], 'CONTEXT',
            ['project:landed_holdings','project:b2_marker_links'],
            'A holding link is a recorded scenario-data relation or matching candidate, not field verification of political governance.')
        authoritative_owners=[{'holdingId':h['id'], 'ownerDynastyId':h.get('ownerDynastyId'),
                               'deJureDynastyId':h.get('deJureDynastyId'),'holderNpcId':h.get('holderNpcId'),
                               'parentHoldingId':h.get('parentHoldingId'),'type':h['type'],
                               'governmentOrganizationId':h.get('governmentOrganizationId')} for h in matched_holdings]
        raw_kind=guess_kind(mid,name,zone,matched_holdings[0] if matched_holdings else None)
        kind=o.get('siteKind',raw_kind)
        status=o.get('habitation', ('UNVERIFIED_EXISTENCE' if kind=='LEGENDARY_UNVERIFIED' else
            'RUIN_NO_ESTABLISHED_PERMANENT_POPULATION' if kind in ('ABANDONED_RUIN','LANDMARK') else 'INHABITED'))
        status_grade=o.get('habitationGrade', 'CONTEXT')
        # No hallucinated permanent residents at ruins, legends, sacred landmarks.
        if status in ('RUIN_NO_ESTABLISHED_PERMANENT_POPULATION','UNVERIFIED_EXISTENCE'):
            v=(0,0,0)
        else: v=default_population(kind,zone)
        pop=classify_population(name,kind,v,o)
        duplicate_primary=primary_for_location.get(geo_id) if geo_id else None
        if duplicate_primary and duplicate_primary!=mid:
            pop=record({'baseline':0,'min':0,'max':0,
                        'baselineEvidence':'CONTEXT','planningRangeEvidence':'CONTEXT',
                        'unit':'no independent residents: shared location already modeled by primary observation'},
                        'CONTEXT',['project:marker_catalog'],
                        f'Overlapping observation of geographic ID {geo_id}. Population planning at {duplicate_primary}; re-evaluate if distinct sites are proven.')
        if status in ('RUIN_NO_ESTABLISHED_PERMANENT_POPULATION','UNVERIFIED_EXISTENCE') and pop['value']['baseline']>0:
            raise ValueError(f'Inhabitant count at ruin/legend: {mid}')
        context=ZONE_DEFAULTS[zone]
        religion=o.get('religion',context[1]); culture=o.get('culture',context[0]); influences=o.get('influences',context[2])
        authority_hint=('Scenario political authorities: '+ '; '.join(
            'holding='+h['id']+' owner='+str(h.get('ownerDynastyId') or '?')+' holder='+str(h.get('holderNpcId') or '?')+
            ' gov='+str(h.get('governmentOrganizationId') or '?') for h in matched_holdings)) if matched_holdings else 'Unresolved local government; do not invent a named ruler'
        gov=o.get('government',authority_hint)
        occupation=o.get('occupationModel',job_kind(kind,zone,name))
        if occupation not in JOBS: raise ValueError(f'Unknown occupation template: {occupation} ({mid})')
        occupation_pct=dict(JOBS[occupation]);
        if occupation != 'ruin': enforce_percentages(occupation_pct)
        occupation_grade='INVENTED';
        deps=o.get('dependencyShares', {'workingAgePercent':60,'childPercent':25,'elderPercent':15})
        if sum(deps.values())!=100: raise ValueError('dependent shares need 100%')
        sources=['project:marker_catalog', 'project:marker_name_crosswalk']
        if linked_loc: sources += ['project:holding_geo_candidates','project:landed_holdings']
        if geo_id and local_refs: sources+= ['project:locations_reference']
        sources += o.get('sources',[])
        source_ids=list(dict.fromkeys(sources))
        feature={'id':mid,'scenario':'agot_298_ac','worldAgeYear':298,'displayName':name,
          'map':{'zone':zone,'observationId':mid,'candidateX':int(round(float(m['minecraft_x']))),
                 'candidateZ':int(round(float(m['minecraft_z']))), 'finalY':None,
                 'constructionApproved':False,'status':m.get('placement_status','NOT_APPROVED'),
                 'landMaskStatus':m.get('land_mask_status',''),'sourceNameStatus':m.get('source_name_status',''),
                 'reviewStatus':m.get('review_status',''),'sourcePixelX':m.get('pixel_x'), 'sourcePixelY':m.get('pixel_y'),
                 'duplicateGroup':m.get('group_id',''),'primaryPopulationMarkerId':duplicate_primary or mid,
                 'countsIndependentResidents': not bool(duplicate_primary and duplicate_primary!=mid)},
          'geography':{'geographicId':record(geo_id,'CONFIRMED' if source_loc else 'CONTEXT',
                         ['project:marker_catalog'] if source_loc else ['project:b2_marker_links'],
                         'Source marker geographic ID, NOT proof of a land holding or final construction site.'),
                       'parentLocation': record(local_refs[0].get('parentId') if local_refs else None,
                              'CONFIRMED' if local_refs else 'CONTEXT',['project:locations_reference'] if local_refs else [],
                              'Exact parent known only when present in authored reference.'),
                       'knownAliases':list(dict.fromkeys(choices)),
                       'externalMarkerHoldingLinkStatus':existing.get('link_status','NOT_LINKED_TO_HOLDING')},
          'site':{'kind':record(kind,o.get('siteKindGrade','CONTEXT'),o.get('kindSources',[]),
                               o.get('siteKindNote','Explicit marker category where known; otherwise geographic context.')),
                  'habitationAt298AC':record(status,status_grade,o.get('habitationSources',[]),
                               o.get('habitationNote','Do not turn legends or ruins into functioning towns.')),
                  'historicalOrPhysicalNotes':record(o.get('notes',f'Regional 298 AC scenario context: {context[2]}. No separate site-specific census established.'),
                               o.get('notesGrade','CONTEXT'),o.get('noteSources',[]),'Research/drafting note, not runtime lore truth.')},
          'politics':{'scenarioHoldingIds':marker_links,
                      'holdingAuthorities':record(authoritative_owners,'CONTEXT',
                             ['project:landed_holdings'], 'House/holder IDs are authored scenario facts; association of the map observation to a holding is still an unverified B2 candidate.'),
                      'leadingStructure':record(gov,o.get('governmentGrade','CONTEXT'),o.get('governmentSources',[]),
                           'Contextual governance where not explicitly supported by scenario sources.'),
                      'regionalInfluences':record(influences,o.get('influencesGrade','CONTEXT'),o.get('influenceSources',[]))},
          'society':{'culture':record(culture,o.get('cultureGrade','CONTEXT'),o.get('cultureSources',[])),
                     'religion':record(religion,o.get('religionGrade','CONTEXT'),o.get('religionSources',[])),
                     'population':pop,
                     'demographyShares':record(deps,'INVENTED',[],
                                    'Share of resident individuals by broad age group; baseline tuning, not canon.'),
                     'laborParticipation':record(o.get('laborParticipation',0.50),'INVENTED',[],
                                    'Active workers / total unique individuals; not all residents have occupations.'),
                     'occupationsOfWorkers':record(occupation_pct,occupation_grade,[],
                                    f'Percent of working individuals, exactly 100%. Template {occupation}; no site census.'),
                     'occupationRolesOfWorkers':record(detailed_jobs(occupation_pct),'INVENTED',[],
                           '21 concrete occupation proportions for simulation job targets; percentages of workers, never population.'),
                     'householdsPlanning':record({'averageResidentsPerHousehold':4.5,'housingUnitsAtBaseline':round(pop['value']['baseline']/4.5) if kind not in ('SACRED_CENTER','ACTIVE_FORTRESS','MONUMENTAL_FRONTIER_FORT','ABANDONED_RUIN','LEGENDARY_UNVERIFIED','LANDMARK') else None},
                          'INVENTED',[], 'Estimate only, no new household entities allocated.'),
                     'occupationalArchetype':occupation},
          'integration':{'instantiatePopulation':False, 'createHolding':False,'generateStructure':False,
                         'pendingReview':True, 'safeForExistingWorld':True,
                         'futureSystems':['identity','households','job_market','economy','politics','travel','structure_generator']},
          'sourceIds':source_ids}
        # Include the zone dossier as a source for rechecking site-specific assertions.
        dossier='docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_'+zone+'_298AC.md'
        if (RESEARCH_DIR/('KNOWN_WORLD_RESEARCH_2_1_'+zone+'_298AC.md')).exists():
            feature['sourceIds'].append('project:dossier_'+zone.lower())
        if mid in FIVE_FORTS:
            feature['site']['historicalCapacityPeople']=record(10000,'CONFIRMED',['wiki:five_forts'],
                'Max accommodation, not current soldiers or census; do not add to resident count.')
        # Field-level citation discipline: a CONFIRMED field requires at least one source.
        for field in (feature['site']['kind'],feature['site']['habitationAt298AC'],
                      feature['politics']['leadingStructure'],feature['society']['culture'],feature['society']['religion']):
            if field['evidence']=='CONFIRMED' and not field['sources']:
                raise ValueError('CONFIRMED field without source in '+mid+': '+str(field['value']))
        if o.get('special'): feature['special']={k:record(v['value'],v['grade'],v.get('sources',[]),v.get('note','')) for k,v in o['special'].items()}
        output.append(feature)

    if len(output)!=253 or len({x['id'] for x in output})!=253: raise AssertionError('Marker loss/duplication')
    summary={'schemaVersion':1,'scenario':'agot_298_ac','markerCount':253,
             'byZone':dict(Counter(x['map']['zone'] for x in output)),
             'siteKindCounts':dict(Counter(x['site']['kind']['value'] for x in output)),
             'habitationCounts':dict(Counter(x['site']['habitationAt298AC']['value'] for x in output)),
             'sourcePolicy':'CONFIRMED fact only with cited source; CONTEXT for inference; INVENTED for all population/job defaults',
             'important':'DO NOT sum individual population baselines across parent city/subsites as independent people. NO world state changes.',
             'sources':dict(research['sources'], **{'project:dossier_'+z.lower():'docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_'+z+'_298AC.md' for z in ZONE_DEFAULTS})}
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT/'site_profiles_253.json').write_text(json.dumps({'summary':summary,'sites':output},indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    sheet=[]
    for x in output:
        p=x['society']['population']; gov=x['politics']['leadingStructure']; kind=x['site']['kind']; hab=x['site']['habitationAt298AC']; s=x['society']
        sheet.append({'Marker':x['id'],'Place':x['displayName'],'Zone':x['map']['zone'],'X candidate':x['map']['candidateX'],
            'Z candidate':x['map']['candidateZ'],'Site kind':kind['value'],'Kind source tier':kind['evidence'],
            '298 AC status':hab['value'],'Habitation source tier':hab['evidence'],
            'Population baseline':p['value']['baseline'],'Population min':p['value']['min'],'Population max':p['value']['max'],
            'Population source tier':p['value']['baselineEvidence'], 'Population range tier':p['value']['planningRangeEvidence'],'Worker occupation model':s['occupationalArchetype'],
            'Farming/herding %':s['occupationsOfWorkers']['value']['farming_herding'],
            'Fishing/forestry %':s['occupationsOfWorkers']['value']['fishing_forestry'],
            'Crafts/mining %':s['occupationsOfWorkers']['value']['crafts_mining'],
            'Transport/trade %':s['occupationsOfWorkers']['value']['transport_trade'],
            'Domestic/services %':s['occupationsOfWorkers']['value']['service_domestic'],
            'Security/military %':s['occupationsOfWorkers']['value']['security_military'],
            'Government/religion %':s['occupationsOfWorkers']['value']['governance_religion'],
            'Culture':s['culture']['value'],'Religion':s['religion']['value'],
            'Political structure':gov['value'],
            'Authored holding IDs':'; '.join(x['politics']['scenarioHoldingIds']['value']),
            'Leading houses (authored)':'; '.join(str(h['ownerDynastyId']) for h in x['politics']['holdingAuthorities']['value'] if h.get('ownerDynastyId')),
            'Individual holder IDs (authored)':'; '.join(str(h['holderNpcId']) for h in x['politics']['holdingAuthorities']['value'] if h.get('holderNpcId')),
            'Geographic ID':x['geography']['geographicId']['value'] or '',
            'Marker holding link':x['geography']['externalMarkerHoldingLinkStatus'],
            'Construction approved':'NO', 'Notes':x['site']['historicalOrPhysicalNotes']['value'],
            'Sources':'; '.join(x['sourceIds']),'Your review': 'NOT_REVIEWED','Your notes':''})
    csvfile=OUTPUT/'site_profiles_253_excel_de.csv'
    with csvfile.open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(sheet[0].keys()),delimiter=';')
        writer.writeheader();writer.writerows(sheet)
    # Every political holding also receives a separate planning/profile row.
    # Site headcounts are NOT duplicated per owning title, including shared-city holdings.
    all_holding_records=json.loads((SCN/'landed_holdings.json').read_text(encoding='utf-8'))['holdings']
    mapped_to_site=defaultdict(list)
    for site in output:
        for hid in site['politics']['scenarioHoldingIds']['value']:
            mapped_to_site[hid].append(site)
    subsite_parents={
        'red_keep':'kings_landing', 'new_castle':'white_harbor', 'wolf_den':'white_harbor',
        'hightower':'oldtown', 'illyrios_manse':'pentos', 'xaros_home':'qarth',
        'high_tide':'driftmark',
    }
    holding_records=[]
    regional_context={
     'the_north':('Northern First Men / noble-vassal culture','old gods / local minority Seven'),
     'the_vale':('Vale Andal knightly and mountain-clan influences','Faith of the Seven / mountain traditional'),
     'riverlands':('Riverlands feudal and river-trade culture','Faith of the Seven; old gods minority'),
     'westerlands':('Westerlands feudal and mining/trade culture','Faith of the Seven'),
     'the_reach':('Reach agricultural, tourney and guild culture','Faith of the Seven'),
     'crownlands':('Crownlands mixed royal court and port populace','Faith of the Seven / imported minority rites'),
     'stormlands':('Stormlands marcher and coastal feudal culture','Faith of the Seven'),
     'dorne':('Dornish Rhoynar/Andal/First Men influence','Faith of the Seven, regional customs'),
     'iron_islands':('Ironborn maritime raiding and farming communities','Drowned God; Seven minority'),
     'yi_ti':('YiTish imperial administrative and market cultures','Local imperial traditions; exact cult unresolved'),
     'summer_isles':('Summer Islander maritime and temple cultures','Summer Isles deities, priestess traditions'),
    }
    for h in sorted(all_holding_records,key=lambda x:x['id']):
        place=h['worldLocationId']; m=mapped_to_site.get(h['id'],[])
        is_domain=h['type'] in ('REGION','TERRITORY')
        parent_place=subsite_parents.get(place) or ('meereen' if place.startswith('pyramid_of_') else None)
        if is_domain:
            count={'baseline':None,'min':None,'max':None}
            count_note='Regional or territorial population is a sum of distinct individual residents in constituent sites; not a new independent population.'
        elif m:
            one=next((x for x in m if x['map']['countsIndependentResidents']),m[0]);pv=one['society']['population']['value']
            count={'baseline':None,'min':None,'max':None}
            count_note=f'Population is accounted for at map observation {one["id"]}: baseline {pv["baseline"]}; do not double count this holding.'
        elif parent_place:
            count={'baseline':None,'min':None,'max':None}
            count_note=f'Treat as physical building/site INSIDE {parent_place}; no independent city-scale census.'
        else:
            bytype={'CASTLE':(1800,250,7000),'FORTRESS':(600,80,1700),'TOWN':(2600,600,9500),
                    'CITY':(60000,12000,220000),'ESTATE':(190,30,800),'VILLAGE':(500,80,1700),
                    'PORT':(7500,1200,33000),'OTHER':(600,100,2000)}
            n,low,high=bytype.get(h['type'],(500,80,1900))
            count={'baseline':n,'min':low,'max':high}
            count_note='INVENTED planning envelope for UNPLACED holding, not an active or additional cohort.'
        refs=ref.get(place,[])
        region_parent=refs[0].get('parentId') if refs else None
        region_culture,region_religion=regional_context.get(region_parent,('Regional culture unresolved from holding ID; do not infer a specific ethnicity','Regional faith unresolved; see geographic/cultural references'))
        jobmodel='ruin' if is_domain else ('trade_city' if h['type'] in ('TOWN','CITY','PORT') else 'fortress' if h['type']=='FORTRESS' else 'rural_south')
        if m: jobmodel=m[0]['society']['occupationalArchetype']
        holding_records.append({
          'id':h['id'],'name':h['name'],'holdingType':h['type'],'worldLocationId':place,
          'matchedMarkerIds':[x['id'] for x in m],
          'geographicParentId':refs[0].get('parentId') if refs else None,
          'geographicLocationKind':refs[0].get('kind') if refs else None,
          'authoritativeScenario':{'ownerDynastyId':h.get('ownerDynastyId'),'deJureDynastyId':h.get('deJureDynastyId'),
                                  'holderNpcId':h.get('holderNpcId'),'governmentOrganizationId':h.get('governmentOrganizationId'),
                                  'parentHoldingId':h.get('parentHoldingId'),'linkedTitleId':h.get('linkedTitleId'),
                                  'status':h.get('status','ACTIVE')},
          'authorityEvidence':'CONFIRMED_IN_PROJECT_SCENARIO_NOT_INDEPENDENT_CANON_VERIFICATION',
          'leadingStructure':record(h.get('governmentOrganizationId') or (h.get('ownerDynastyId') or 'UNRESOLVED_AUTHORITY'),
                                    'CONFIRMED' if h.get('governmentOrganizationId') or h.get('ownerDynastyId') else 'CONTEXT',
                                    ['project:landed_holdings'] if h.get('governmentOrganizationId') or h.get('ownerDynastyId') else [],
                                    'Authored 298 AC scenario government/owner ID, not an independent assertion about contemporary ruler.'),
          'regionalCulture':record(region_culture,'CONTEXT',['project:locations_reference'],'Generic parent-region cultural pattern only, not verified site demographics.'),
          'regionalReligion':record(region_religion,'CONTEXT',['project:locations_reference'],'Generic regional faith context only, not verified site census.'),
          'siteIdentityStatus':'POLITICAL_DOMAIN_NO_INDEPENDENT_SEAT' if is_domain else
                     'KNOWN_SUBSITE_WITHIN_LARGER_PLACE' if parent_place else
                     'CANDIDATE_MARKER_LINK' if m else 'UNPLACED_POLITICAL_HOLDING',
          'insideKnownPlace':parent_place,
          'populationPlanning':record(count,'INVENTED' if count['baseline'] is not None else 'CONTEXT',[],count_note),
          'occupationsOfWorkers':record(JOBS[jobmodel],'INVENTED',[],f'Scenario template {jobmodel}; only applies if future population is attached to this site.'),
          'notIndependentPopulation':is_domain or bool(m) or bool(parent_place),
          'noAutoCreation':{'population':True,'holding':True,'worldStructure':True},
          'populationWeightNotHeadcount':h.get('populationWeight'),
          'sourceIds':['project:landed_holdings','project:locations_reference','project:holding_geo_candidates']})
    if len(holding_records)!=259:raise AssertionError('Lost political holdings')
    (OUTPUT/'holding_profiles_259.json').write_text(json.dumps({'summary':{'count':259,'joinPolicy':'Political holdings do not multiply individual resident counts'},'holdings':holding_records},indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    holding_csv=OUTPUT/'holding_profiles_259_excel_de.csv'
    with holding_csv.open('w',newline='',encoding='utf-8-sig') as f:
        fields=['Holding ID','Name','Type','Geo ID','Geo parent','Marker IDs','Placement category','Inside city','Owner dynasty','Current holder NPC','Parent political holding','Government org','Population planned low','Population planned baseline','Population planned high','Population note','Occupations grade','Review']
        w=csv.writer(f,delimiter=';');w.writerow(fields)
        for h in holding_records:
            v=h['populationPlanning']['value'];a=h['authoritativeScenario']
            w.writerow([h['id'],h['name'],h['holdingType'],h['worldLocationId'],h['geographicParentId'],'; '.join(h['matchedMarkerIds']),h['siteIdentityStatus'],h['insideKnownPlace'],a['ownerDynastyId'],a['holderNpcId'],a['parentHoldingId'],a['governmentOrganizationId'],v['min'],v['baseline'],v['max'],h['populationPlanning']['note'],'INVENTED','NOT_REVIEWED'])

    (OUTPUT/'README_RESEARCH_STATUS.md').write_text('''# B2.3 World Site Research – Generated Scenario Planning\n\n'''+
        '\n'.join(f'- {k}: {v}' for k,v in summary.items() if k!='sources')+'''

All 253 active observations have a profile. **Population/jobs are INVENTED scenario
planning defaults unless a record explicitly says otherwise.** A source-supported
site status does not make its generated population canonical. All footprint approvals
remain false. Geographic links never automatically create new political holdings.

This file is a derived report. Edit `tools/geodata/b2_3_world_research_overrides.json`
for corrections, then rerun the generator. All manual checks should be kept as
separate review records; do not overwrite original marker observations.
''',encoding='utf-8')
    # Offline, browsable research reader with source links and exportable local notes.
    template=(GEO/'b2_3_research_html_template.html').read_text(encoding='utf-8')
    embedded=json.dumps({'summary':summary,'sites':output},ensure_ascii=False,separators=(',',':'))
    embedded=embedded.replace('<','\\u003c').replace('>','\\u003e').replace('&','\\u0026')
    html=template.replace('__PROFILE_DATA__',embedded).replace('__OVERRIDE_COUNT__',str(len(overrides)))
    (OUTPUT/'WorldResearchAtlas_253.html').write_text(html,encoding='utf-8')
    print('B2.3 generated:',csvfile)
    print(json.dumps({k:v for k,v in summary.items() if k!='sources'},indent=2))

if __name__=='__main__': main()
