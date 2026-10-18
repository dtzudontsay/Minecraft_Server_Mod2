#!/usr/bin/env python3
"""Compile existing Winterfell V2 housing/road ledger into chunk-local V3 worldgen data.

Run AFTER build_winterfell_v2.py. Does not move/add/remove any person, household
or job. Output contains *structures only*. It is deterministic and independent
of which chunks a player visits or the moment NPCs become active.
"""
from __future__ import annotations
import collections
import hashlib
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INPUT = ROOT / 'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
OUTPUT = ROOT / 'src/main/resources/data/knownworld/case_study/winterfell_v3_worldgen.json'
REPORT = ROOT / 'docs/step3/WINTERFELL_V3_WORLDGEN_MANIFEST.json'
ANCHOR_X, ANCHOR_Z = -4050173, -1976496  # NW-029 verified projected atlas marker
INDEX_SCHEMA = 3


def compile_index(source: dict) -> tuple[dict, dict]:
    if len(source['residents']) != 5000 or len(source['households']) != 1136:
        raise ValueError('Expected 5,000 residents and 1,136 households. Rebuild V2 data first.')
    homes = source['homes']; workplaces = source['workplaces']
    buildings = homes + workplaces
    assert len(buildings) == 1514
    membership = collections.Counter(p['homeId'] for p in source['residents'])
    for h in homes:
        assert membership[h['id']] == h['occupants']
        assert h['capacity'] >= h['occupants']
    chunks: dict[tuple[int,int], dict[str,set[int]]] = collections.defaultdict(lambda: {'r':set(), 'b':set()})
    # Road points in source are centre-lines. V3 widens them to three blocks;
    # building footprints are placed later and override any cross-lane conflict.
    roads = set()
    for x,z in source['roads']:
        for dx in (-1,0,1):
            for dz in (-1,0,1):
                roads.add((x+dx,z+dz))
    for x,z in sorted(roads):
        wx,wz=ANCHOR_X+x,ANCHOR_Z+z
        cx,cz=wx//16,wz//16
        chunks[(cx,cz)]['r'].add((wz-cz*16)*16+wx-cx*16)
    # The castle ring/keep is generated analytically from anchor in the Java
    # decorator, rather than repeated as hundreds of thousands of JSON blocks.
    for i,b in enumerate(buildings):
        x0,z0=ANCHOR_X+b['x'],ANCHOR_Z+b['z']
        x1,z1=x0+b['w']-1,z0+b['d']-1
        for cx in range(x0//16,x1//16+1):
            for cz in range(z0//16,(z1+6)//16+1):
                chunks[(cx,cz)]['b'].add(i)
    # Ensure every chunk intersecting castle ring and keep can be evaluated,
    # even when it has no road or home entry of its own.
    for cx in range((ANCHOR_X-182)//16,(ANCHOR_X+182)//16+1):
        for cz in range((ANCHOR_Z-182)//16,(ANCHOR_Z+182)//16+1):
            chunks[(cx,cz)]
    packed = []
    for (cx,cz),entries in sorted(chunks.items()):
        packed.append({'x':cx,'z':cz, 'r':sorted(entries['r']), 'b':sorted(entries['b'])})
    # Trim objects down to what the terrain decorator actually uses. No NPC
    # data in worldgen, so generation doesn't touch mutable campaign state.
    reduced=[{'id':b['id'],'t':b['template'],'x':b['x'],'z':b['z'],
              'w':b['w'],'d':b['d'],'cap':b.get('capacity',0),
              'site':b['site']} for b in buildings]
    digest=hashlib.sha256(json.dumps({'buildings':reduced,'roads':source['roads']},separators=(',',':')).encode()).hexdigest()
    data={'schema':INDEX_SCHEMA,'anchorX':ANCHOR_X,'anchorZ':ANCHOR_Z,
          'worldgenId':'winterfell_nw029_v3','versionHash':digest,
          'households':1136,'population':5000,'buildings':reduced,'chunks':packed}
    report={'schema':INDEX_SCHEMA,'worldgenId':data['worldgenId'],'anchor':[ANCHOR_X,ANCHOR_Z],
            'residents':5000,'households':1136,'buildingCount':len(reduced),
            'roadCentreTiles':len(source['roads']),'roadSurfaceTiles':len(roads),
            'indexedChunks':len(chunks),'layoutSha256':digest,
            'worldgenPlacement':'CHUNK_DECORATION_BEFORE_PLAYER_RENDER',
            'legacyLiveBuilder':'MUST_BE_DISABLED_IN_NEW_WORLD'}
    return data,report


def main():
    if not INPUT.exists():
        raise SystemExit('Missing V2 population plan. Run: py tools\\geodata\\build_winterfell_v2.py')
    data,report=compile_index(json.loads(INPUT.read_text(encoding='utf8')))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf8')
    REPORT.write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()
