#!/usr/bin/env python3
"""Generate C4.3 proposed jobs/workplace demands for all 248 physical site presets.

Usage (repository root):
  py tools/geodata/build_c4_3_job_distribution.py
  py tools/geodata/build_c4_3_job_distribution.py --install-resource

By default writes only tools/geodata/output/c4_3_jobs/*. No NPC or world save
mutation, no new settlement/worldgen activation. Input C1 retains full original
headcounts and location provenance. The resulting data are MOD_ESTIMATE, not canon.
"""
from __future__ import annotations
import argparse, collections, csv, hashlib, json, math
from pathlib import Path
from c4_3_job_rules import build_index, EXECUTABLE_C3

ROOT=Path(__file__).resolve().parents[2]
SCENARIO=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
INPUT=SCENARIO/'settlement_presets_c1_v1.json'
CATALOG=SCENARIO/'occupation_building_catalog_c4_3_v1.json'
C3=SCENARIO/'architecture_templates_c3_v1.json'
OUT=ROOT/'tools/geodata/output/c4_3_jobs'

MACRO={
  'grain_farmer':'farming_herding','gardener':'farming_herding','herder':'farming_herding',
  'fisher':'fishing_forestry','forester':'fishing_forestry',
  'blacksmith':'crafts_mining','carpenter':'crafts_mining','mason':'crafts_mining',
  'weaver':'crafts_mining','miner':'crafts_mining',
  'merchant':'transport_trade','carter':'transport_trade','sailor':'transport_trade',
  'market_seller':'transport_trade','cook':'service_domestic','servant':'service_domestic',
  'innkeeper':'service_domestic','guard':'security_military','soldier':'security_military',
  'administrator':'governance_religion','religious_attendant':'governance_religion',
}
SECTOR_BY_SECTION={
 'Governance and Justice':'governance_religion',
 'Military':'security_military','Maritime':'transport_trade',
 'Scholary and Medical':'service_domestic','Religious':'governance_religion',
 'Trade and Finance':'transport_trade','Crafts and Manufacturing':'crafts_mining',
 'Agriculture and land Work and Forestry':'farming_herding','Food Production':'service_domestic',
 'Domestic Services':'service_domestic','Entertainment':'service_domestic',
 'Criminal and marginal':'service_domestic','Essosi occupations':'transport_trade',
 'Dothraki Occupations':'security_military',
 'Arcane and associated religious roles':'governance_religion','Intelligence':'governance_religion',
}
# Split occupational classes where geography makes the macro sector different.
SECTOR_OVERRIDE={
 'Forester':'fishing_forestry','Woodcutter':'fishing_forestry','Hunter':'fishing_forestry',
 'Gamekeeper':'fishing_forestry','Charcoal Burner':'fishing_forestry',
 'Fisherman':'fishing_forestry','Fishmonger':'transport_trade',
 'Shipwright':'crafts_mining','Salt Worker':'crafts_mining',
 'Maester':'governance_religion','Grandmaester':'governance_religion',
 'Bookbinder':'crafts_mining','Parchment Maker':'crafts_mining',
 'Camel Rider':'transport_trade','Elephant Handler':'farming_herding',
 'Dothraki Herbwoman':'service_domestic','Jaqqa Rhan':'governance_religion',
 'Slaver':'transport_trade','Trader':'transport_trade','Slave Trader':'transport_trade',
 'Quartermaster':'security_military','Armory Keeper':'security_military',
 'Stable Master':'service_domestic','Stablehand':'service_domestic',
 'Gravedigger':'service_domestic','Mercenary':'security_military',
 'Dosh Khaleen':'governance_religion',
 'Pit Fighter':'service_domestic','Unsullied Soldier':'security_military',
}
BUILTIN_STAFF={'SCHOLASTIC_HALL':12,'BOOKBINDERY':8,'JUSTICE_HALL':10,'BANK':9,
 'FERRY_LANDING':6,'SHIPYARD':24,'BRICKWORKS':20,'CHANDLERY':8,'LEATHERWORKS':12,
 'POTTERY':10,'JEWELER':7,'BREWERY':12,'BUTCHERY':8,'CANAL_WORKS':12,
 'CITY_SERVICES':18,'ENTERTAINMENT_HALL':16,'ARCANE_STUDY':6,'FIGHTING_PIT':40,
 'KEEP':40,'GRANARY':5,'WAREHOUSE':15,'PUBLIC_BATH':8,'SCHOOL':10,'GUARD_TOWER':6}
PORTS='Port Harbor Harbour Eastwatch White Harbor Braavos Lorath Pentos Tyrosh Lys Volantis Myr Saltpans Gulltown Seagard Lannisport Kings Landing Maidenpool Oldtown Duskendale Shipyard Ibben Sisterton Sunspear Qarth Meereen Yunkai Astapor New Ghis Tall Trees Town Last Lament Lotus Point Ebonhead'.split('|') # supplemented by explicit words below
PORT_WORDS=('harbor','harbour','port ','ibben','ib sar','braavos','lorath','tyrosh','lys','volantis','myr',
            'saltpans','gulltown','seagard','lannisport','maidenpool','oldtown','duskendale',
            'sisterton','sunspear','qarth','meereen','yunkai','astapor','ghis','last lament',
            'lotus point','ebonhead','tall trees town','pyke','driftmark','dragonstone',
            'eastwatch','white harbor','morosh','weeping town','faros','zamettar',
            'va(h)ar','nefer','pentos','salt shore')
SLAVE_PORTS=('astapor','yunkai','meereen','new ghis','volantis','tyrosh','myr','lys','norvos')
DOTHRAKI_PLACES=('vaes dothrak',)
CRIMINAL={'Assasin','Bandit','Fence','Poacher','Smuggler','Thief','Spy'}
AUTHORED_ONLY={'Ruler','Courtier','Merchant Prince','Grandmaester','Dosh Khaleen','Dothraki Bloodrider',
               'Bloodmage','Necromancer','Shadowbinder','Warlock','Pyromancer','Godswife',
               'Alchemist','Woods Witch','Jaqqa Rhan','Fortune Teller','Torturer',
               'Corsair','Pirate','Reaver'}
SACRED={'Septon','Septa','Silent Sister'}


def eligible(job,site):
    if job['isDuplicate']: return False,'duplicate source row'
    k=job['jobId']; b=job['building'];n=site['name'].lower();z=site['zone']; pop=site['population']
    if pop<=0:return False,'uninhabited/ruin/unverified'
    if k in CRIMINAL:return False,'social/criminal status, not an ordinary paid job'
    if k in AUTHORED_ONLY:return False,'only authored scenario identity or event can assign'
    if k in ('Slaver','Slave Trader') and not any(x in n for x in SLAVE_PORTS):
        return False,'slavery excluded from normal Westeros, Braavos and unverified markets'
    if k in ('Unsullied Soldier',) and 'astapor' not in n:
        return False,'requires authored Unsullied organization; Astapor model only'
    if k in ('Pit Fighter',) and not any(x in n for x in ('meereen','yunkai','astapor','new ghis')):
        return False,'Ghiscari fighting pit regional rule'
    if k in ('Dothraki Herbwoman','Dosh Khaleen') and not any(x in n for x in DOTHRAKI_PLACES):
        return False,'Dothraki cultural role outside reviewed camp data'
    if k=='Reaver' and 'pyke' not in n:return False,'Ironborn polity/cultural constraint'
    if k=='Drowned Man' and 'pyke' not in n:return False,'Drowned God cult requires regional faith data'
    if k=='Moonsinger' and 'braavos' not in n:return False,'Braavosi cult requires local evidence'
    if k=='Maester' and (z not in ('NW','SW') or (pop<1200 and n not in ('castle black','eastwatch-by-the-sea','shadow tower'))):
        return False,'Citadel-trained maesters require a plausible staffed Westerosi seat'
    if k in ('Septon','Septa','Silent Sister') and (n in ('thenn',"craster's keep") or pop<150):
        return False,'no local Faith of the Seven institution established'
    if k=='Red Priest' and z not in ('NWE','SWE','SE','SEE'):
        return False,'R\'hllor institutional location not supported by scenario audit'
    if k in SACRED and z not in ('NW','SW'):
        return False,'Faith of the Seven distribution not locally established'
    if k=='Camel Rider' and z not in ('NE','SE','NEE','SEE'):
        return False,'camel region unsupported'
    if k=='Elephant Handler' and z not in ('SE','SEE','SI','SWE','NWE'):
        return False,'elephants not regionally modeled'
    if k=='Eunuch Healer' and z in ('NW','SW','SI'):
        return False,'no scenario evidence for local occupation'
    if b in ('DOCK','FISHERY','SHIPYARD','FERRY_LANDING') or k in ('Corsair','Pirate','Salt Worker'):
        if not (z=='SI' or any(w in n for w in PORT_WORDS)):
            return False,'physical coast/river placement not established'
    if b=='MINE' and not any(x in n for x in ('casterly rock','golden tooth','silverhill','saltpans','salt shore','stonehelm','goldengrove','pyke','the crag','high hermitage','rock','quarry','mine')):
        return False,'no documented/provisional ore, stone or salt resource anchor'
    if b=='CANAL_WORKS' and not any(x in n for x in ('braavos','volantis','selhorys','valysar','volon therys')):
        return False,'no canal network at geographic site'
    if k in ('Banker','Moneychanger') and pop<12000:
        return False,'financial center size threshold'
    if k=='Prostitute' or k=='Courtesan':
        # Employment projection only; brothel construction is separately policy-gated.
        if z in ('NW',) and pop<1000:return False,'no assumed licensed venue'
    return True,'mod-estimated eligible, not independently verified'


def weight(job,site):
    r=job['rarity']; k=job['jobId'];b=job['building']
    w={'COMMON':14.0,'SPECIALIST':3.0,'RESTRICTED':0.22,'UNIQUE':0.0}[r]
    if k in ('grain_farmer','Farmer','Herder','LIVESTOCK'):w*=2.0
    if k in ('Guard','Soldier'):w*=1.5
    if k in ('Administrator','Maester'):w*=0.5
    if k in ('Slaver','Slave Trader'):w*=0.25
    if b in ('MINE','SHIPYARD','FIGHTING_PIT'):w*=0.35
    if site['population']<300 and r!='COMMON':w*=0.5
    return w


def apportion(total,pairs):
    if not total or not pairs:return {}
    s=sum(w for _,w in pairs)
    if s<=0:return {}
    a=[(k,total*w/s) for k,w in pairs]
    out={k:int(math.floor(x)) for k,x in a}
    rem=total-sum(out.values())
    for k,_ in sorted(a,key=lambda kx:(-(kx[1]-math.floor(kx[1])),kx[0]))[:rem]:
        out[k]+=1
    assert sum(out.values())==total
    return {k:v for k,v in sorted(out.items()) if v>0}


def site_record(p):
    return {'siteId':p['siteId'],'name':p['name'],'zone':p['zone'],
            'tier':p['tier'],'mode':p['blueprintMode'],
            'population':int(p['buildingDemand']['peopleTarget']),
            'workforce':int(p['buildingDemand']['workforceTarget']),
            'c1':p['buildingDemand'],'anchor':p['workingAnchor']}


def build_plan(site,jobs,templates):
    demand=site['c1']; groups=collections.Counter()
    for role,count in demand['roleWorkerTargets'].items():
        groups[MACRO[role]]+=int(count)
    if sum(groups.values())!=site['workforce']:raise ValueError('C1 workforce mismatch at '+site['siteId'])
    eligible_jobs={}; counts={}; unresolved=[]
    for group,total in sorted(groups.items()):
        pairs=[]
        for job in jobs:
            sector=SECTOR_OVERRIDE.get(job['jobId'],SECTOR_BY_SECTION.get(job['section']))
            if sector!=group or job['isDuplicate']:continue
            allow,why=eligible(job,site)
            if allow and weight(job,site)>0:pairs.append((job['jobId'],weight(job,site)))
        if not pairs and total:
            raise ValueError(f'No eligible workers for {site["siteId"]} sector {group} / {total}')
        dist=apportion(total,pairs)
        counts.update(dist)
    if sum(counts.values())!=site['workforce']:
        raise ValueError('Worker conservation failed '+site['siteId'])
    bybuilding=collections.Counter()
    for job in jobs:
        n=counts.get(job['jobId'],0)
        if n:bybuilding[job['building']]+=n
    workplace={}
    for kind,workers in sorted(bybuilding.items()):
        if kind in ('KEEP','NONE_PRIVATE'):
            workplace[kind]={'workers':workers,'buildings':0,'constructionType':'INTERIOR_OR_NONBUILDING',
                             'capacityPerUnit':0,'reason':'must attach to real authoritative castle/organization'}
            continue
        base_staff=int(templates.get(kind,{}).get('maxStaff') or BUILTIN_STAFF.get(kind,8))
        capacity=max(1,base_staff)
        workplace[kind]={'workers':workers,'buildings':math.ceil(workers/capacity),
                         'capacityPerUnit':capacity,
                         'constructionType':'C3_EXECUTABLE' if kind in EXECUTABLE_C3 else 'NEW_TEMPLATE_REQUIRED',
                         'reason':'planned demand; NOT actual occupied workplaces'}
    households=int(site['c1']['householdsEstimated'])
    if site['name']=='Winterfell':
        households=1136 # preserve existing, real authored 5k-person roster and V3 layout
    required_households=households
    required_buildings=1136 if site['name']=='Winterfell' else int(site['c1']['housingBuildingsDemand'])
    if households and required_buildings<=0:raise ValueError('Housing demand disappeared')
    units_per_building=max(1,math.ceil(households/max(1,required_buildings)))
    return {'siteId':site['siteId'],'name':site['name'],'zone':site['zone'],
            'tier':site['tier'],'mode':site['mode'],'anchor':site['anchor'],
            'peopleTarget':site['population'],'workforceTarget':site['workforce'],
            'housingUnitsRequired':required_households,'housingBuildingsRequired':required_buildings,
            'housingUnitsPerBuildingTarget':units_per_building,
            'householdsEstimated':households,
            'occupationCounts':dict(sorted(counts.items())),
            'workplaces':workplace,
            'fieldPolicy':{'minimumVisualFieldTilesPerFarm':24,'fieldTileSizeBlocks':[31,31],
                           'totalVisibleAreaM2PerFarm':24*31*31,
                           'status':'VISUAL_LOWER_BOUND_NOT_ECONOMICALLY_SUFFICIENT',
                           'actualCultivatedHectares':'MUST_COME_FROM_AGRICULTURE_ECONOMY'},
            'exceptionalBuildingsNotGeneric':site['mode']!='GENERAL_COMPACT_SETTLEMENT' or site['tier']=='SPECIAL',
            'status':'PROPOSED_NOT_APPROVED_FOR_WORLDGEN',
            'noNewNpcIdentitiesCreated':True,'estimateProvenance':'MOD_ESTIMATE'}


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--install-resource',action='store_true');args=parser.parse_args()
    data=json.loads(INPUT.read_text(encoding='utf8'))
    catalog=json.loads(CATALOG.read_text(encoding='utf8'))
    templates={x['id']:x for x in json.loads(C3.read_text(encoding='utf8'))['templates']}
    jobs=catalog['rows'];sites=[site_record(p) for p in data['physicalSitePresets']]
    if len(sites)!=248 or len(jobs)!=206:raise ValueError('Versioned user data mismatch')
    plans=[build_plan(s,jobs,templates) for s in sites]
    OUT.mkdir(parents=True,exist_ok=True)
    payload={'schemaVersion':1,'scope':'SCENARIO_PLANNING_ONLY','scenario':'agot_298_ac',
             'jobSource':'Jobs.xlsx, 206 rows preserved','canonPolicy':'all non-authored quantities are MOD_ESTIMATE',
             'sourceHash':hashlib.sha256(INPUT.read_bytes()).hexdigest(),
             'summary':{'sites':len(plans),'positivePopulationSites':sum(p['peopleTarget']>0 for p in plans),
                        'actualWorkersAssigned':0,'newlyBuiltStructures':0},'plans':plans}
    (OUT/'settlement_job_demand_c4_3_v1.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    with (OUT/'ALL_JOBS_AT_ALL_LOCATIONS.csv').open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.writer(f,delimiter=';');w.writerow(['site_id','site','zone','tier','population_estimate',
                       'job_id','workers_planned','building','building_units_demand','building_status','provenance'])
        jid={j['jobId']:j['building'] for j in jobs if not j['isDuplicate']}
        for p in plans:
            for job in jobs:
                if job['isDuplicate']:continue
                n=p['occupationCounts'].get(job['jobId'],0);k=jid[job['jobId']]
                b=p['workplaces'].get(k,{})
                w.writerow([p['siteId'],p['name'],p['zone'],p['tier'],p['peopleTarget'],
                            job['jobId'],n,k,b.get('buildings',0),b.get('constructionType','NOT_PLANNED'),
                            'MOD_ESTIMATE_NOT_NPC_ROSTER'])
    with (OUT/'BUILDING_CONSTRUCTION_DEMAND.csv').open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.writer(f,delimiter=';');w.writerow(['site','tier','name','building_type','workers','buildings','status','per_building_capacity'])
        for p in plans:
            for b,d in sorted(p['workplaces'].items()):
                w.writerow([p['siteId'],p['tier'],p['name'],b,d['workers'],d['buildings'],d['constructionType'],d['capacityPerUnit']])
    (OUT/'SUMMARY.md').write_text('\n'.join(['# C4.3 — Proposed full occupation model',
     f"- {len(jobs)} original Excel rows, including repeated occupations kept in original rows.",
     f"- {len(plans)} physical site presets; {sum(p['peopleTarget']>0 for p in plans)} with nonzero planned population.",
     '- Every physical site conserves its existing C1 workforce total and seven macro-sector sums.',
     '- Counts and locations are proposed; no NPC jobs or work buildings have been instantiated.',
     '- Exceptional sites, Winterfell V3 and existing saves remain unchanged.',
     '- Missing physical templates, coastal/mining surveys, fortification-specific slots, and castle interior navigation require further implementation.',
     '- Public working conditions/monuments/ruins must never become generic settlements.',
     '']),encoding='utf8')
    if args.install_resource:
        dest=SCENARIO/'settlement_job_demand_c4_3_v1.json'
        dest.write_text(json.dumps(payload,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf8')
        print('INSTALLED separate proposed demand resource',dest)
    print(json.dumps(payload['summary'],indent=2))
    print('C4.3 preflight output:',OUT)

if __name__=='__main__':main()
