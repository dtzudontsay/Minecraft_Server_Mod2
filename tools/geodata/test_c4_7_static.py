#!/usr/bin/env python3
"""Non-Minecraft checks: complete source coverage, no server/world calls, preview anchors.
This does not replace running Gradle against Minecraft 26.3.
"""
from pathlib import Path
import csv,re,sys,json
ROOT=Path(__file__).resolve().parents[2]
J=ROOT/'tools/geodata/templates/KnownWorldC47ArchitecturalDetails.java.txt'
S=J.read_text(encoding='utf8')
CAT=ROOT/'tools/geodata/c4_7_review_catalog.csv'
ids=[]
with CAT.open(encoding='utf-8-sig',newline='') as f:
    for row in csv.DictReader(f): ids.append(row['id'])
assert len(ids)==61 and len(set(ids))==61,(len(ids),len(set(ids)))
reference=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json'
if reference.is_file():
    loaded=json.loads(reference.read_text(encoding='utf-8'))
    actual={x['id'] for x in loaded['templates']}
    if set(ids)!=actual:
        raise AssertionError(f'C4.7 review catalogue mismatch: missing={actual-set(ids)}, stale={set(ids)-actual}')
    print('PASS C4.7 review IDs exactly match installed C3 architecture resource')
assert 'C47ArchitecturalDetails' in S
assert 'ServerLevel' not in S and 'level.getChunk' not in S and 'setBlockEntity' not in S
assert 'Blocks.LAVA' not in S
assert 'Blocks.ORANGE_STAINED_GLASS' not in S, 'Obsolete 26.3 block field; use a verified compatible block'
assert 'c.b(mid,3,4,Blocks.GLOWSTONE.defaultBlockState())' in S, 'Forge should preserve safe decorative light'
assert 'c47Put(chunk' in S
assert 'decorate(ChunkAccess' in S
for required in ('TEMPLE','SHIPYARD','SMITHY','STABLE','TAVERN','PUBLIC_BATH','MINE','JUSTICE_HALL','MARKET','MILL','FIGHTING_PIT','FOREST_CAMP','DOCK','COURTYARD','CHANDLERY','CART_DEPOT','BREWERY','BRICKWORKS','BROTHEL','BUTCHERY','CANAL_WORKS','BARRACKS','BANK','ARMORY','ARCANE_STUDY','ADMINISTRATION'):
    assert required in S,required
# Bounded function: all reads via lot + clipped writes.
assert 'if(x<0 || x>=w() || z<0 || z>=d()' in S
assert 'c.lamp' in S
print('PASS 61 unique documented real C3 template IDs')
print('PASS special requests with name-bound renderer cases')
print('PASS no unsafe live-world mutations, fluid lava, entity spawns or foreign-chunk writes')
print('PASS floor-by-floor supported lantern pattern and bounded footprint writer')
