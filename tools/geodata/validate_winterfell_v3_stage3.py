#!/usr/bin/env python3
"""Offline integrity tests for combined 5000-population + V3 worldgen + Stage3 UI patch."""
from pathlib import Path
import json,collections,hashlib
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/main/resources/data/knownworld/case_study'
a=json.loads((BASE/'winterfell_v2.json').read_text(encoding='utf8'))
b=json.loads((BASE/'winterfell_v3_worldgen.json').read_text(encoding='utf8'))
people=a['residents'];homes=a['homes'];houses=a['households'];works=a['workplaces'];visitors=a['visitors']
assert len(people)==len({p['id'] for p in people})==5000
assert len(homes)==len(houses)==1136
assert len(works)==378
assert len(visitors)==5
assert {v['id'] for v in visitors}.isdisjoint({p['id'] for p in people})
H={h['id']:h for h in homes};hh={h['id']:h for h in houses}
assert len(H)==1136 and len(hh)==1136
assert all(p['homeId'] in H and p['householdId'] in hh for p in people)
assert all(H[hh[p['householdId']]['residence']]['id']==p['homeId'] for p in people)
assert sum(h['occupants'] for h in homes)==5000
assert b['schema']==3 and b['anchorX']==-4050173 and b['anchorZ']==-1976496
assert b['population']==5000 and b['households']==1136
assert b['versionHash']
assert len(b['chunks'])>6000
assert any(p['id']=='eddard_stark' for p in people)
assert len({p['id'] for p in people if p['status']=='AUTHORED_EXISTING'})==9
# Ensure the code actually presents mutable campaign age, not 298-guest birth year.
runtime=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellV3Runtime.java').read_text()
assert 'sim.campaignCalendar().year()' in runtime
assert 'age < 16' in runtime
assert 'FULL_BUDGET = 128' in runtime and 'REDUCED_BUDGET = 128' in runtime
assert 'buildNearby(' not in runtime and 'setBlock(' not in runtime
assert 'ServerPlayNetworking.send(player,new KnownWorldUiPackets.CharacterSheet(' in runtime
pkt=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/KnownWorldUiPackets.java').read_text()
assert 'Commands.LEVEL_GAMEMASTERS.check(' in pkt and 'isCreative()' in pkt
assert 'getHeight(Heightmap.Types.WORLD_SURFACE' in pkt
print(json.dumps({'status':'PASS (offline, NOT Gradle compiled)','residents':len(people),
 'households':len(homes),'workplaces':len(works),'visitors':len(visitors),
 'v3WorldgenChunks':len(b['chunks']), 'npcFullBudget':128,'npcReducedBudget':128},indent=2))
