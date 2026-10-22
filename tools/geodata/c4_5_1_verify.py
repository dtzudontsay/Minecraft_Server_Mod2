#!/usr/bin/env python3
"""Read-only C4.5.1 installed-resource assertions, never mutates any file."""
from __future__ import annotations
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
CASE=ROOT/'src/main/resources/data/knownworld/case_study'
SRC=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement'

def require(cond,msg):
 if not cond:raise AssertionError(msg)

def main():
 manifest=json.loads((SC/'settlement_construction_c4_v1.json').read_text('utf8'))
 require('C4_5' in manifest.get('purpose',''),'Missing C4.5 physical data')
 n=len(manifest['pilots']);require(n>=2,'No normal settlements activated')
 require(n==manifest['summary']['pilotCount'],'Site count mismatch')
 v2=json.loads((CASE/'winterfell_v2.json').read_text('utf8'))
 require(len(v2['residents'])==5000 and len(v2['homes'])==1136,'Winterfell population/homes changed')
 marked=[p for p in v2['residents'] if p.get('occupationPlanVersion')==45]
 require(len(marked)>0,'Winterfell new occupations missing')
 require(v2.get('c451',{}).get('relocatedCastleHomes')==24,'Castle-ward housing relocation missing')
 v3=json.loads((CASE/'winterfell_v3_worldgen.json').read_text('utf8'))
 nbuild=len(v3['buildings']);require(nbuild>1526,'Winterfell new work buildings not appended')
 require(v3.get('c451',{}).get('newInnerWorkplaces')==12,'No 12 castle-ward replacement shops')
 require(v3.get('c45',{}).get('addedWorkplaces',0)>0,'No distributed specialties')
 require((SRC/'KnownWorldStructureSigns.java').exists(),'Missing exterior signs helper')
 java=(SRC/'KnownWorldUniversalWorldgenC2.java').read_text('utf8')
 require('site index count disagrees' in java,'Java loader still hardcoded to two pilots')
 tests=(ROOT/'src/test/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC3ArchitectureTest.java').read_text('utf8')
 require('assertEquals(43, KnownWorldC3Architecture.allTemplates().size())' not in tests,'Obsolete architecture test remains')
 print(f'VERIFIED: {n} SMALL/MEDIUM planned sites; 5000 Winterfell residents; 1136 homes; '
       f'{len(marked)} reassigned generated occupations; 24 relocated castle homes; '
       f'{nbuild} total Winterfell lots; {v3["c45"]["addedWorkplaces"]} distributed workplaces; signs source installed.')
if __name__=='__main__':main()
