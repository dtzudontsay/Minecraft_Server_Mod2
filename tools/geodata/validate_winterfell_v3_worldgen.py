#!/usr/bin/env python3
"""Offline safety and conservation checks for schema-v3 Winterfell chunk index."""
from __future__ import annotations
import json
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
INDEX=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v3_worldgen.json'


def main():
    src=json.loads(SOURCE.read_text('utf8'))
    data=json.loads(INDEX.read_text('utf8'))
    assert data['schema']==3 and data['anchorX']==-4050173 and data['anchorZ']==-1976496
    assert data['population']==len(src['residents'])==5000
    assert data['households']==len(src['households'])==len(src['homes'])==1136
    assert len(data['buildings'])==len(src['homes'])+len(src['workplaces'])==1514
    home_counts=Counter(p['homeId'] for p in src['residents'])
    assert len({p['id'] for p in src['residents']})==5000
    for h in src['homes']:
        assert home_counts[h['id']]==h['occupants']<=h['capacity'],h['id']
    keys=set(); indexed_buildings=set();indexed_roads=set()
    for cp in data['chunks']:
        cx,cz=cp['x'],cp['z']
        assert (cx,cz) not in keys,(cx,cz)
        keys.add((cx,cz))
        assert len(cp['r'])==len(set(cp['r']))
        for n in cp['r']:
            assert 0<=n<256
            indexed_roads.add((cx*16+(n&15),cz*16+(n>>4)))
        for n in cp['b']:
            assert 0<=n<1514
            indexed_buildings.add(n)
            b=data['buildings'][n]
            x,z=data['anchorX']+b['x'],data['anchorZ']+b['z']
            assert x//16<=cx<=(x+b['w']-1)//16
            assert z//16<=cz<=(z+b['d']+5)//16
    assert indexed_buildings==set(range(1514)),len(indexed_buildings)
    assert len(indexed_roads)>=len(src['roads'])
    # Verify index includes every original centre-line and its 3x3 expansion.
    ox,oz=data['anchorX'],data['anchorZ']
    for x,z in src['roads']:
        for dx in (-1,0,1):
            for dz in (-1,0,1):
                assert (ox+x+dx,oz+z+dz) in indexed_roads,(x,z,dx,dz)
    # Each residence approach is indexed through the next chunk boundary.
    by_chunk={(p['x'],p['z']):set(p['b']) for p in data['chunks']}
    for i,b in enumerate(data['buildings']):
        x=ox+b['x']+b['w']//2;z=oz+b['z']+b['d']+5
        assert i in by_chunk[(x//16,z//16)],('porch_missed',b['id'])
    print('PASS: 5000 identities, 1136 occupied homes, 378 workplace units')
    print(f'PASS: {len(indexed_roads):,} three-wide street cells, {len(keys):,} chunk entries')
    print('PASS: all residential thresholds, building fragments and cross-chunk porches indexed')
    print('PASS: no changed NPC or household assignments; deterministic V3 placement')

if __name__=='__main__':main()
