#!/usr/bin/env python3
"""Run before starting the game: checks marker joins, source provenance and building plot collisions."""
from pathlib import Path
import csv, json, itertools
ROOT=Path(__file__).resolve().parents[2]
atlas=json.loads((ROOT/'src/main/resources/assets/knownworld/maps/atlas_markers.json').read_text('utf8'))
rows=list(csv.DictReader((ROOT/'tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv').open(newline='',encoding='utf8')))
markers={x['id']:x for x in atlas['markers']}
assert len(markers)==248 and atlas['sourceObservations']==253
assert sum(x['named'] for x in markers.values())==247
for id,name in [('NW-021','Westwatch-by-the-Bridge'),('NW-020','Shadow Tower'),('NW-019','Shieldwatch'),('NW-018','Greywatch'),('NW-012','Stonegate'),('NW-015','Hoarfrost Hill'),('NW-013','Icemark'),('NW-016','The Nightfort'),('NW-014','Deep Lake'),('SO-004','Gogossos'),('SO-005','Yeen')]:
    assert markers[id]['name']==name,(id,markers[id]['name'])
assert markers['NW-049']['name']=='Unlinked NW-049'
assert markers['NW-008']['sourceMasterX']==401.3997
assert markers['NW-008']['masterX']==409.7
assert len(rows)==253
w=next(x for x in rows if x['id']=='NW-008')
assert float(w['master_x'])==401.3997 and float(w['atlas_master_x'])==409.7
assert all(not x.get('final_approved_x') and not x.get('final_approved_z') for x in rows)
plots=json.loads((ROOT/'docs/step3/WINTERFELL_TEST_BUILDING_PLOTS.json').read_text('utf8'))['plots']
assert len(plots)==55 and len({x['id'] for x in plots})==55
ignored={'DEFENSIVE_ENVELOPE','ROAD','SETTLEMENT_ROAD','ANCIENT_CRYPT','GEOTHERMAL_POOL'}
for a,b in itertools.combinations(plots,2):
    if a['kind'] in ignored or b['kind'] in ignored:continue
    A=a['bounds'];B=b['bounds']; dx=min(A['xmax'],B['xmax'])-max(A['xmin'],B['xmin'])+1;dz=min(A['zmax'],B['zmax'])-max(A['zmin'],B['zmin'])+1
    assert dx<=0 or dz<=0 or dx*dz<=4,(a['id'],b['id'],dx*dz)
folder=ROOT/'src/main/resources/data/knownworld/function/step3/winterfell'
commands=[s for path in sorted(folder.glob('*.mcfunction')) for s in path.read_text('utf8').splitlines() if s and not s.startswith('#')]
assert len(commands)>1800
for s in commands:
    assert s.startswith(('fill ','setblock ')),s
    tokens=s.split()
    if tokens[0]=='fill':
        coords=[int(x.replace('~','') or 0) for x in [tokens[1],tokens[2],tokens[3],tokens[4],tokens[5],tokens[6]]]
        volume=1
        for a,b in [(0,3),(1,4),(2,5)]:volume*=abs(coords[a]-coords[b])+1
        assert volume<=8000,(s,volume)
print(f'PASS: 253 observations -> 248 pins; 247 named; Castle Black source kept and overlay moved; 55 role plots; {len(commands)} bounded voxel commands; no unauthorized final XYZ.')
