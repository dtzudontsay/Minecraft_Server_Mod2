#!/usr/bin/env python3
"""Static, no-Minecraft regression checks for Winterfell V3 detail patch.
Use to catch accidental change of anchor, layout, entrance and feature-gating.
These tests do not imply Java compilation or in-game validation.
"""
from pathlib import Path
import json
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[2]
J=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/WinterfellWorldgenV3.java'
G=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
A=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v3_worldgen.json'
B=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
s=J.read_text('utf8')
checks={
'anchor NW-029':'-4_050_173' in s and '-1_976_496' in s,
'chunk-local construction': 'chunk.setBlockState' in s,
'neighbor decoration reservation':'suppressNaturalDecoration(ChunkAccess chunk)' in s and 'dz<=2' in s,
'inner keep 3-deep arch':'rz>=58&&rz<=60&&rx>=15&&rx<=19' in s,
'keep double doors':'rz==58 && (rx==16||rx==17)' in s,
'ceil-mounted lantern':'LanternBlock.HANGING,true' in s and 'floor+deck+6' in s,
'floor post lantern':'Blocks.SPRUCE_FENCE.defaultBlockState()' in s,
'ladder west backing':'block(chunk,x-1,floor+h,z,wall);' in s,
'ladder upper landing':'!(xx==1 && (zz==2 || zz==3))' in s,
'cleansing lava':'FluidTags.LAVA' in s and 'surface-24' in s,
'cleansing tree blocks':'BlockTags.LEAVES' in s and 'BlockTags.LOGS' in s,
'no post-load building':'ServerTickEvents' not in s,
}
if G.is_file():
 g=G.read_text('utf8')
 checks['generator decoration integration']='!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)' in g
 checks['generator overlay retained']='WinterfellWorldgenV3.decorate(level, chunk)' in g
if A.is_file():
 d=json.loads(A.read_text('utf8'))
 checks['worldgen building count']=len(d['buildings'])==1514
 checks['population metadata preserved']=d['population']==5000 and d['households']==1136
if B.is_file():
 d=json.loads(B.read_text('utf8'))
 checks['person/household counts preserved']=len(d['residents'])==5000 and len(d['homes'])==1136
for name,result in checks.items():print(('PASS' if result else 'FAIL')+': '+name)
assert all(checks.values()), 'One or more regression checks failed'
print('Verified static checks:',len(checks))
