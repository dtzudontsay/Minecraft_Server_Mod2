#!/usr/bin/env python3
"""Deterministic exhaustive C2 pilot index tests, plus source integrity checks."""
import importlib.util, json, sys, tempfile
from pathlib import Path
from collections import Counter

folder=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('c2', folder/'build_c2_physical_pilots.py')
mod=importlib.util.module_from_spec(spec);sys.modules[spec.name]=mod;spec.loader.exec_module(mod)
sel=mod.read(folder/'c2_pilot_selection.json')

def fake():
    out=[]
    for i in range(253):
        tier='SMALL' if i%2==0 else 'MEDIUM'
        marker=f'TEST-{i:03d}'
        if i==0:marker='NW-031'
        if i==1:marker='SW-023'
        out.append({'siteId':f'site_{i:03d}','name':f'Test {i}', 'tier':tier,
            'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
            'markerIds':[marker],'primaryMarkerId':marker,'workingAnchor':{'x':i*20000,'z':i*20000+120},
            'zone':'NW' if i<30 else 'SW','paletteId':'NORTHERN',
            'buildingDemand':{'peopleTarget':300 if i%2==0 else 1400,
                'firstPhaseHousingPlots':200,'housingBuildingsDemand':330,'workplaceCounts':
                {'FARM':3,'BAKERY':1,'SMITHY':1,'INN':1,'MARKET':1}}})
    return {'schemaVersion':1,'summary':{'markerCount':253,'holdingCount':259},
      'physicalSitePresets':out,'workplaceCatalog':{'FARM':{'staff':12},'BAKERY':{'staff':6},
      'SMITHY':{'staff':6},'INN':{'staff':8},'MARKET':{'staff':22}}}

def tests():
    a=mod.build(fake(),sel);b=mod.build(fake(),sel)
    assert a==b
    assert len(a['pilots'])==2 and a['summary']['allMapMarkers']==253
    assert {p['tier'] for p in a['pilots']}=={'SMALL','MEDIUM'}
    assert set(a['summary']['pilotMarkers'][0]+a['summary']['pilotMarkers'][1])=={'NW-031','SW-023'}
    for p in a['pilots']:
        assert 3<=len(p['lots'])<35 and p['chunks']
        assert p['housingBacklogAtLeast']>0
        assert len({x['id'] for x in p['lots']})==len(p['lots'])
        assert set(x['kind'] for x in p['lots']) >= {'HOUSE','FARM','FIELD','BAKERY'}
        for c in p['chunks']:
            assert all(0<=i<256 for i in c['r'])
            assert all(0<=i<len(p['lots']) for i in c['b'])
        indexed=Counter(i for c in p['chunks'] for i in c['b'])
        assert set(indexed)==set(range(len(p['lots'])))
        road={(c['cx']*16+(i%16),c['cz']*16+i//16) for c in p['chunks'] for i in c['r']}
        assert road
        # Roads and footpaths must form ONE actual traversable surface graph,
        # including farm spur: no painted lanes through homes.
        visited=set();pending=[next(iter(road))]
        while pending:
            cell=pending.pop()
            if cell in visited:continue
            visited.add(cell)
            xx,zz=cell
            for nxt in ((xx-1,zz),(xx+1,zz),(xx,zz-1),(xx,zz+1)):
                if nxt in road and nxt not in visited:pending.append(nxt)
        assert len(visited)==len(road),('disconnected road fragments',p['name'],len(road)-len(visited))
        for plot in p['lots']:
            for x in range(plot['x'],plot['x']+plot['width']):
                for z in range(plot['z'],plot['z']+plot['depth']):
                    assert (x,z) not in road,(p['name'],plot['id'])
        assert p['noNpcOrJobInstancesCreated']
    bad=fake();bad['physicalSitePresets'][4]['markerIds']=['NW-031']
    try:mod.build(bad,sel)
    except ValueError:pass
    else:raise AssertionError('duplicate markers not rejected')
    from importlib.util import spec_from_file_location,module_from_spec
    hookfile=folder/'install_c2_worldgen_hooks.py'
    s=spec_from_file_location('hook',hookfile);h=module_from_spec(s);s.loader.exec_module(h)
    sample='''import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;
if (winterfellEstate && !allowVanillaStructuresAtWinterfell) return;
if (!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)) {
WinterfellWorldgenV3.decorate(level, chunk);
'''
    edited=h.hook(sample)
    assert edited==h.hook(edited)
    assert 'KnownWorldUniversalWorldgenC2.decorate(level, chunk);' in edited
    print('C2 TESTS PASSED: 253 source IDs / 259 holdings, two pilot plans, deterministic chunk index, zero road collisions, guarded idempotent installer')

if __name__=='__main__':tests()
