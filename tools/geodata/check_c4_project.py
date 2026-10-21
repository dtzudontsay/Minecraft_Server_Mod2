#!/usr/bin/env python3
"""C4 integration preflight/postflight. Standard library only; read-only.

Checks the ACTUAL repo's naming, pilots, asset schemas, C2 and C3 wiring,
and rejects accidental worldwide construction.  No save/world mutations.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac'
GEN = ROOT / 'src/main/java/dev/dtzudontsay/knownworld/world/generation'
C2JAVA = GEN / 'settlement/KnownWorldUniversalWorldgenC2.java'
C3JAVA = GEN / 'settlement/KnownWorldC3Architecture.java'
CHUNK = GEN / 'KnownWorldChunkGenerator.java'
OLD = 'settlement_construction_c2_v1.json'
NEW = 'settlement_construction_c4_v1.json'


def read(name):
    path = BASE / name
    if not path.is_file():
        raise ValueError(f'Missing resource: {path.relative_to(ROOT)}')
    return json.loads(path.read_text(encoding='utf-8-sig'))


def require(cond, message):
    if not cond:
        raise ValueError(message)


def check(after):
    for path in (ROOT / 'tools/geodata/build_c4_settlement_layouts.py',
                 ROOT / 'tools/geodata/install_c4_settlement_layouts.py',
                 ROOT / 'tools/geodata/test_c4_settlement_layouts.py',
                 C2JAVA, C3JAVA, CHUNK):
        require(path.is_file(), f'Missing source: {path.relative_to(ROOT)}')
    c1 = read('settlement_presets_c1_v1.json')
    c2 = read(OLD)
    c3 = read('architecture_templates_c3_v1.json')
    require(c1.get('summary', {}).get('markerCount') == 253 and
            c1.get('summary', {}).get('holdingCount') == 259,
            'C1 must retain 253 markers and 259 holdings')
    require(c3.get('templateCount') == 43 and
            c3.get('doesNotAuthoriseNewSites') is True,
            'C3 must retain 43 approved catalog templates')
    require(c2.get('schemaVersion') == 1 and
            c2.get('summary', {}).get('pilotCount') == 2 and
            c2.get('policy', {}).get('pilotsOnly') is True,
            'C2 manifest must authorize only the two original pilots')
    require(len(c2['pilots']) == 2 and
            {p['siteId'] for p in c2['pilots']} == {'site_nw_031', 'site_sw_023'},
            'Original C2 pilot identities differ from audited Ramsgate/Acorn Hall')
    require(all(p['tier'] in {'SMALL', 'MEDIUM'} and
                'NW-029' not in p['markerIds'] for p in c2['pilots']),
            'Special site or Winterfell accidentally selected')
    chunk = CHUNK.read_text(encoding='utf-8')
    require('KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk)' in chunk and
            'KnownWorldUniversalWorldgenC2.suppressVanillaStructures(centerChunk)' in chunk and
            'KnownWorldUniversalWorldgenC2.decorate(level, chunk)' in chunk,
            'KnownWorldChunkGenerator C2 hooks absent; STOP rather than corrupt worldgen')
    c2src = C2JAVA.read_text(encoding='utf-8')
    require('KnownWorldC3Architecture.buildLot(chunk, lot, terrain.floor, p.palette, p.tier);' in c2src,
            'C3 physical renderer hook missing from C2')
    if after:
        c4 = read(NEW)
        require(c4.get('schemaVersion') == 1 and
                c4.get('policy', {}).get('pilotsOnly') is True and
                c4.get('c4Policy', {}).get('worldwideDeploymentNotAuthorized') is True,
                'C4 construction scope may have expanded outside two pilots')
        require(len(c4.get('pilots', [])) == 2, 'C4 does not have exactly two pilots')
        for prior, now in zip(c2['pilots'], c4['pilots']):
            for field in ('siteId', 'tier', 'anchor', 'markerIds', 'palette'):
                require(now[field] == prior[field],
                        f'{prior["siteId"]} changed an approved original {field}')
            require(now['c4']['actualNpcsCreated'] == 0 and
                    now['c4']['actualWorkersAssigned'] == 0,
                    'C4 must not create synthetic NPCs/jobs')
            require(len(now['lots']) > len(prior['lots']) and len(now['chunks']) > 0,
                    f'{now["siteId"]}: C4 not substantially expanded')
            print(f'  {now["name"]}: marker {now["markerIds"]}, '
                  f'X={now["anchor"]["x"]} Z={now["anchor"]["z"]}, '
                  f'plots={len(now["lots"])}, chunks={len(now["chunks"])}, '
                  f'housing_backlog={now["housingBacklogAtLeast"]}')
        require(f'"{NEW}"' in c2src or NEW in c2src,
                'C2 Java has not switched to the C4 generated resource')
        require('if ("FIELD".equals(lot.kind()))' in c2src and
                'buildLot(chunk, lot, terrain.floor, p.palette, wx, wz);' in c2src,
                'FIELD still routed into C3 renderer, which rejects it')
    print('C4 ' + ('POSTFLIGHT' if after else 'PREFLIGHT') + ' PASSED')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--after', action='store_true')
    args = parser.parse_args()
    try:
        check(args.after)
    except (ValueError, KeyError, json.JSONDecodeError) as exc:
        parser.exit(1, f'C4 PROJECT CHECK FAILED: {exc}\n')
