#!/usr/bin/env python3
"""Validate generated Winterfell case-study ledger; requires running builder first."""
from pathlib import Path
from collections import Counter
import json
ROOT=Path(__file__).resolve().parents[2]
p=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_roster.json'
j=json.loads(p.read_text('utf-8'))
people=j['residents'];homes=j['households'];buildings=j['buildings']
assert len(people)==1200 and len(set(x['id'] for x in people))==1200
assert len(homes)==257 and len({x['id'] for x in homes})==257
assert len(buildings)==631 and len(set(x['id'] for x in buildings))==631
assert sum(h['population'] for h in homes)==1200
assert sum(len(h['primaryMembers']) for h in homes)==1200
assert sum(1 for p in people if p['ageGroup']=='child')==330
assert sum(1 for p in people if p['ageGroup']=='worker')==744
assert sum(1 for p in people if p['ageGroup']=='elder')==126
assert sum(1 for p in people if p['status']=='AUTHORED_SCENARIO')==9
assert len(j['visitors'])==5
assert sum(1 for x in buildings if x['purpose']=='seasonal_residence_vacant')==263
assert sum(1 for x in buildings if x.get('assignedWorkers'))==111
assert sum(x.get('assignedWorkers',0) for x in buildings)==744
assert len({p['name'] for p in people})==1200
assert all(p['homeHouseholdId'] and p['homeBuildingId'] for p in people)
assert all(p.get('workBuildingId') for p in people if p['ageGroup']=='worker')
assert not any(b.get('placed') for b in buildings)
assert len(people)==j['populationConservation']['residentIndividuals']+j['populationConservation']['cohorts']
print('PASS: Winterfell 1,200 resident persons / 257 households / 631 plots / 111 job sites / 744 adult jobs / 5 nonresident visitors; zero actual placements')
