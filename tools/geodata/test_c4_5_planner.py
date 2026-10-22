#!/usr/bin/env python3
"""No-Minecraft deterministic invariants and collision tests of C4.5 planners."""
import copy,json,tempfile
from pathlib import Path
import c4_5_connected_rollout as mod


def fixture():
 d=mod.read(Path(__file__).parents[2]/'src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_job_distributions_c4_4_v2.json')
 plans={p['siteId']:p for p in d['plans']}
 specs={}
 for p in plans.values():
  for k,v in p['buildingDemand'].items():
   specs.setdefault(k,{'id':k,'c1Width':17,'c1Depth':14,'maxStaff':max(1,v['maxWorkersPerWorkplaceAssumption']),
                  'requiresShore':k in ('DOCK','FISHERY','SHIPYARD','FERRY_LANDING','CANAL_WORKS'),
                  'requiresMountainResource':k=='MINE',
                  'requiresPolicy':k in ('ARCANE_STUDY','FIGHTING_PIT','BROTHEL')})
 for id in ('WELL','GRANARY','HOUSE_SMALL','HOUSE_MEDIUM','GREAT_HALL','COURTYARD','GUARD_TOWER','FARM'):
  specs[id]={'id':id,'c1Width':5 if id=='WELL' else 13,
       'c1Depth':5 if id=='WELL' else 14,'maxStaff':8,'requiresShore':False,
       'requiresMountainResource':False,'requiresPolicy':False}
 sites=[]
 for id,marker,name,x,z,tier,pop,houses in [('site_nw_031','NW-031','Ramsgate',-3454784,-1589008,'SMALL',550,110),
     ('site_sw_023','SW-023','Acorn Hall',-4115723,-284673,'MEDIUM',2100,219)]:
  site={'siteId':id,'markerIds':[marker],'name':name,'tier':tier,'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
      'workingAnchor':{'x':x,'z':z},'zone':marker.split('-')[0], 'paletteId':'NORTHERN',
      'buildingDemand':{'peopleTarget':pop,'workforceTarget':pop//2,'housingBuildingsDemand':houses,
                         'workplaceCounts':{'WELL':1,'GRANARY':1,'GREAT_HALL':int(tier=='MEDIUM')}}}
  sites.append((site,plans[id]))
 return sites,specs


def run():
 sites,templates=fixture()
 for s,p in sites:
  a=mod.build_site(s,p,templates,{})
  b=mod.build_site(s,p,templates,{})
  assert json.dumps(a,sort_keys=True)==json.dumps(b,sort_keys=True)
  assert sum(l['kind']=='HOUSE' for l in a['lots'])==s['buildingDemand']['housingBuildingsDemand']
  assert len(a['chunks'])>0
  ids=[l['id'] for l in a['lots']];assert len(ids)==len(set(ids))
  assert a['c4']['plannedWorkers']==p['originalC1WorkforceEstimate']
  print('PASS',s['name'],len(a['lots']),'lots',len(a['chunks']),'chunk indexes')
 cat=mod.read(Path(__file__).parents[2]/'src/main/resources/data/knownworld/scenarios/agot_298_ac/occupation_building_catalog_c4_4_v2.json')
 assert len(cat['rows'])==206
 dist=mod.read(Path(__file__).parents[2]/'src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_job_distributions_c4_4_v2.json')
 assert len(dist['plans'])==248
 for p in dist['plans']:
  assert sum(p['jobWorkerCounts'].values())==p['originalC1WorkforceEstimate'],p['siteId']
 print('PASS job conservation for all',len(dist['plans']),'sites')

if __name__=='__main__':run()
