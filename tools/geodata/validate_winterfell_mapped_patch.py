#!/usr/bin/env python3
"""Static audit of atlas anchoring and Winterfell V2 mapped command; no writes."""
import json, math, re
from pathlib import Path
root = Path(__file__).resolve().parents[2]
atlas = json.loads((root/'src/main/resources/assets/knownworld/maps/atlas_markers.json').read_text(encoding='utf-8'))
marker = next(m for m in atlas['markers'] if m['id'] == 'NW-029')
assert marker['name'] == 'Winterfell', marker
mpp = 482803.2/math.hypot(80,-7.5)
x = round((marker['masterX']-1024.0)*mpp)
z = round((marker['masterY']-678.5)*mpp)
assert (x,z)==(-4050173,-1976496),(x,z)
java = (root/'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellV2.java').read_text(encoding='utf-8')
for token in ('WINTERFELL_MAP_X = -4_050_173','WINTERFELL_MAP_Z = -1_976_496',
              'Commands.literal("mapped")','Commands.literal("survey")',
              'beginMapped(', 'surveyMapped(', 'mappedAnchorMatchesAtlas(',
              's.anchorX=mapped?WINTERFELL_MAP_X:',
              'state.roadsBlocked.add(i)','state.skipped.add(home.id)',
              'var winterfellSettlement = sim.authoredIds().findSettlement("winterfell")'):
    assert token in java, f'Missing expected implementation: {token}'
assert 'ifPresent(sid->sim.affiliations().setResidence(id,sid))' not in java
plan_path = root/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
plan = json.loads(plan_path.read_text(encoding='utf-8'))
assert len(plan['residents'])==5000
assert len(plan['households'])==1136
assert len(plan['homes'])==1136
assert len(plan['workplaces'])==378
print(f'PASS: atlas NW-029 X={x}, Z={z}, matching mapped V2 command and 5000-person case study.')
print('NOTE: static only; in-game terrain survey and Minecraft 26.3 compilation still required.')
