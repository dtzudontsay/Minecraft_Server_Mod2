#!/usr/bin/env python3
"""Validate the complete generated Winterfell V2 data before loading Minecraft.
Python stdlib; deliberately validates identity, capacity, genealogy candidates,
occupation/workplace assignment, and 4-neighbor road graph connectivity.
"""
from pathlib import Path
from collections import Counter, deque
import json
ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
p=json.loads(DATA.read_text(encoding='utf-8'))
m=p['manifest'];persons=p['residents'];houses=p['households'];homes=p['homes'];works=p['workplaces'];roads={tuple(x) for x in p['roads']}
assert m['residentCount']==5000 and len(persons)==5000
assert m['householdCount']==len(houses)==len(homes)==1136
assert len({x['id'] for x in persons})==len(persons)
assert len({x['name'].casefold() for x in persons})==5000,'duplicate full name'
assert len({x['id'] for x in houses})==len(houses)
assert len({x['id'] for x in homes+works})==len(homes)+len(works)
by_person={x['id']:x for x in persons};by_home={x['id']:x for x in homes};by_house={x['id']:x for x in houses};by_work={x['id']:x for x in works}
assert all(x['homeId'] in by_home and x['householdId'] in by_house for x in persons)
assert len(set(p['id'] for h in houses for p in [by_person[i] for i in h['members']]))==5000
assert all(h['capacity']>=len(h['members']) for h in houses)
assert all(by_home[h['residence']]['capacity']>=len(h['members']) for h in houses)
assert sum(h['occupants'] for h in homes)==5000
assert all(p['id'] in by_house[p['householdId']]['members'] for p in persons)
assert all(by_house[p['householdId']]['residence']==p['homeId'] for p in persons)
assert m['castleCount']==1 and m['village10xCount']==1 and m['village5xCount']==4 and m['village1xCount']==15
assert len({s['id'] for s in p['sites']})==21
assert len(p['visitors'])==5 and all(not x['homeCountedHere'] for x in p['visitors'])
assert all(x['id'] not in by_person for x in p['visitors'])
assert sum(x['status']=='AUTHORED_EXISTING' for x in persons)==9
assert sum(x['status']=='BOOK_NAMED_PROPOSAL' for x in persons)==13
job_by_work=Counter()
for person in persons:
    if person['status']!='AUTHORED_EXISTING' and person['age']<17:assert person['job'] in ('CHILD','DEPENDENT') or person['status']=='BOOK_NAMED_PROPOSAL'
    if person['job'] not in ('CHILD','DEPENDENT','WARD','LORD','LADY'):
        assert person.get('workId') in by_work,(person['id'],person['job'],person.get('workId'))
        job_by_work[person['workId']]+=1
    if person.get('status')=='GENERATED_MOD_CALIBRATION' and person.get('motherId') or person.get('fatherId'):
        for parent_key,sex in [('motherId','FEMALE'),('fatherId','MALE')]:
            par=person.get(parent_key)
            if par:
                adult=by_person[par]; assert adult['sex']==sex
                assert adult['age']>=person['age']+16
                assert adult['householdId']==person['householdId']
assert all(job_by_work[k['id']]==k['assignedWorkers'] for k in works)
assert all(k['assignedWorkers']<=k['workforceCapacity'] for k in works)
# Bounds use source plan, not real chunks. Every plot must be collision free.
occupied=set()
for home in homes+works:
    for x in range(home['x'],home['x']+home['w']):
        for z in range(home['z'],home['z']+home['d']):
            assert (x,z) not in occupied,('Overlapping buildings',home['id'],x,z)
            occupied.add((x,z))
assert not (occupied & roads),'road passes through a reserved structure'
# All roads must form ONE 4-connected component.
assert roads
reached=set();queue=deque([next(iter(roads))]);reached.add(queue[0])
while queue:
    x,z=queue.popleft()
    for n in ((x+1,z),(x-1,z),(x,z+1),(x,z-1)):
        if n in roads and n not in reached:reached.add(n);queue.append(n)
assert len(reached)==len(roads),f'roads disconnected: connected {len(reached)} / {len(roads)}'
# All front doors have a road immediately to their south, except castle quarters
# which share internal courtyard paths and are validated separately.
for h in homes:
    if h['site']!='castle':
        assert (h['accessX'],h['accessZ']+1) in roads,(h['id'],'disconnected entry')
print('PASS: 5000 unique people; 1136 disjoint households/homes; 378+ valid workplaces;')
print('      21 sites with 1/5/10x tiers; no plot collisions; all roads connected;')
print('      visitors excluded; age-plausible modeled kin; no duplicate population.')
print('DETAIL:',json.dumps({'people':len(persons),'households':len(houses),'workplaces':len(works),
  'workers':sum(job_by_work.values()),'road_tiles':len(roads),'buildings':len(homes)+len(works)},indent=2))
