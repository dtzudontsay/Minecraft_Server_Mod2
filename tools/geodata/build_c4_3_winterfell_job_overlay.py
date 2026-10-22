#!/usr/bin/env python3
"""Prepare job reclassification of the EXISTING 5,000 Winterfell V2 residents.

No people created or deleted. Existing IDs, homes, household IDs, role tags,
named canon characters and physical workIDs are untouched. A separate overlay
is emitted for future *explicit* NPC runtime integration; writing an overlay
is not equivalent to staffing actual C3 blocks or mutating a saved world.
"""
from __future__ import annotations
import collections, hashlib, json
from pathlib import Path
from build_c4_3_job_distribution import ROOT, SCENARIO, CATALOG, eligible, weight, apportion, SECTOR_BY_SECTION, SECTOR_OVERRIDE
SOURCE=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
OUT=ROOT/'tools/geodata/output/c4_3_jobs'
OLD_SECTORS={
 'FARMER':'farming_herding','LIVESTOCK':'farming_herding','FORESTER':'fishing_forestry',
 'SMITH':'crafts_mining','CARPENTER':'crafts_mining','MASON':'crafts_mining',
 'MERCHANT':'transport_trade','TRANSPORT':'transport_trade',
 'GUARD':'security_military','SERVANT':'service_domestic','COOK':'service_domestic',
 'HEALER':'service_domestic','ADMIN':'governance_religion',
}

def main():
 data=json.loads(SOURCE.read_text(encoding='utf8'))
 people=data['residents']
 if len(people)!=5000 or len({p['id'] for p in people})!=5000:
  raise ValueError('Winterfell 5000-person identity conservation failed')
 jobs=json.loads(CATALOG.read_text(encoding='utf8'))['rows']
 site={'name':'Winterfell','zone':'NW','population':5000}
 groups=collections.defaultdict(list)
 for p in people:
  if p['status']=='GENERATED_MOD_CALIBRATION' and p.get('job') in OLD_SECTORS:
   groups[OLD_SECTORS[p['job']]].append(p)
 remap={};totals=collections.Counter()
 for macro,members in groups.items():
  pool=[]
  for j in jobs:
   if j['isDuplicate']:continue
   if SECTOR_OVERRIDE.get(j['jobId'],SECTOR_BY_SECTION.get(j['section']))!=macro:continue
   allowed,_=eligible(j,site)
   if allowed and weight(j,site)>0:pool.append((j['jobId'],weight(j,site)))
  counts=apportion(len(members),pool)
  ordered_jobs=sorted([k for k,n in counts.items() for _ in range(n)])
  ordered_people=sorted(members,key=lambda p:hashlib.sha256(('C43:Winterfell:'+p['id']).encode()).hexdigest())
  if len(ordered_jobs)!=len(ordered_people):raise ValueError('Job conservation failed')
  for p,occupation in zip(ordered_people,ordered_jobs):
   source=next(j for j in jobs if j['jobId']==occupation and not j['isDuplicate'])
   remap[p['id']]={'npcId':p['id'],'fromLegacyJob':p['job'],'proposedPrimaryOccupation':occupation,
                    'proposedBuildingType':source['building'],'existingWorkId':p.get('workId'),
                    'householdId':p.get('householdId'),'homeId':p.get('homeId'),
                    'status':'PROPOSED_NOT_IMPORTED'}
   totals[occupation]+=1
 if any(p['id'] in remap for p in people if p.get('status')!='GENERATED_MOD_CALIBRATION'):
  raise AssertionError('Named or previously authored character modified')
 OUT.mkdir(parents=True,exist_ok=True)
 dest=OUT/'winterfell_c4_3_primary_occupation_overlay.json'
 dest.write_text(json.dumps({'schemaVersion':1,'residentCountVerified':5000,
   'generatedAdultsProposed':len(remap),'authoredProfilesPreserved':True,
   'source':'winterfell_v2.json','runtimeApplied':False,'noNpcCreated':True,
   'proposedRolesById':remap,'occupationCounts':dict(totals)},ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 print('Winterfell proposed adult job overlays:',len(remap),'full census unchanged: 5000',dest)
if __name__=='__main__':main()
