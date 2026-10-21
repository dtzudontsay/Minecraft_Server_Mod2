#!/usr/bin/env python3
"""Idempotent, fail-closed C4.2 patch to the ALREADY INSTALLED C4 C2 renderer.

Modifies one existing Java source with a timestamped backup. It does not
rebuild any settlement manifest, touch Winterfell, or edit saved chunks.
"""
from __future__ import annotations

import argparse
import datetime
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / ('src/main/java/dev/dtzudontsay/knownworld/world/generation/'
               'settlement/KnownWorldUniversalWorldgenC2.java')
MANIFEST = ROOT / ('src/main/resources/data/knownworld/scenarios/agot_298_ac/'
                   'settlement_construction_c4_v1.json')
BACKUPS = ROOT / 'tools/geodata/output/c4_terrain_roads/backups'
BEGIN = '    private static void road(ChunkAccess c, int x, int z, Pilot p) {'
END = '    private static void clearHazards(ChunkAccess c, int x, int z, int terrainY) {'
OLD_LOOP = '            for (int index = cp.roads.nextSetBit(0);'
OLD_CALL = '                road(chunk, x, z, p);'
OLD_SURVEY = '                Survey terrain = survey(lot);'
NEW_SURVEY = '                Survey terrain = approvedSurvey(lot);'
MARKER = '    // C4.2 APPROVED STREET ENTRANCE CONNECTIONS'
NEW_METHOD = '''    // C4.2 APPROVED STREET ENTRANCE CONNECTIONS
    private static final java.util.concurrent.ConcurrentMap<String, Survey> SURVEY_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static Survey approvedSurvey(Lot lot) {
        return SURVEY_CACHE.computeIfAbsent(lot.id(), ignored -> survey(lot));
    }

    /** Only entrances near the current chunk; no O(roads * all buildings) surveys. */
    private static List<KnownWorldC4RoadPainter.Entrance> entrancesNear(
            Pilot pilot, int worldX, int worldZ) {
        List<KnownWorldC4RoadPainter.Entrance> nearby = new ArrayList<>();
        for (Lot lot : pilot.lots) {
            if ("FIELD".equals(lot.kind())) continue;
            int doorX = lot.x() + lot.width() / 2;
            int doorZ = "NORTH".equals(lot.face())
                    ? lot.z() - 1 : lot.z() + lot.depth();
            if (doorX < worldX - 5 || doorX > worldX + 20
                    || doorZ < worldZ - 5 || doorZ > worldZ + 20) continue;
            Survey survey = approvedSurvey(lot);
            if (survey.safe())
                nearby.add(new KnownWorldC4RoadPainter.Entrance(doorX, doorZ, survey.floor()));
        }
        return nearby;
    }

    private static void road(ChunkAccess chunk, int x, int z, Pilot pilot,
                             List<KnownWorldC4RoadPainter.Entrance> entrances) {
        KnownWorldC4RoadPainter.paint(chunk, x, z, pilot.palette,
                KnownWorldUniversalWorldgenC2::top,
                KnownWorldUniversalWorldgenC2::isLand, entrances);
    }

'''


def validate_manifest():
    if not MANIFEST.is_file():
        raise ValueError('C4 manifest missing. Install successful C4.1 first.')
    data = json.loads(MANIFEST.read_text(encoding='utf-8-sig'))
    if data.get('schemaVersion') != 1 or data.get('summary', {}).get('pilotCount') != 2:
        raise ValueError('Unexpected C4 construction manifest')
    expected = {
        'site_nw_031': (-3454784, -1589008),
        'site_sw_023': (-4115723, -284673),
    }
    found = {p['siteId']: (p['anchor']['x'], p['anchor']['z']) for p in data['pilots']}
    if found != expected or not data.get('policy', {}).get('pilotsOnly'):
        raise ValueError('Site scope or coordinates changed. C4.2 refuses broad deployment.')
    return data


def build_patch(source: str) -> str:
    if MARKER in source:
        if source.count(MARKER) != 1 or 'KnownWorldC4RoadPainter.paint(' not in source:
            raise ValueError('Incomplete C4.2 integration found; inspect Java manually')
        return source
    if 'settlement_construction_c4_v1.json' not in source:
        raise ValueError('C4.1 Java manifest integration is missing')
    if 'if ("FIELD".equals(lot.kind())) {' not in source:
        raise ValueError('C4.1 FIELD painter integration is missing')
    if source.count(BEGIN) != 1 or source.count(END) != 1:
        raise ValueError('Unexpected C2 road method; refusing blind replacement')
    if source.count(OLD_CALL) != 1 or source.count(OLD_SURVEY) != 1:
        raise ValueError('Unexpected C2 decoration loop; refusing blind replacement')
    if source.count(OLD_LOOP) != 1:
        raise ValueError('Unexpected C2 street-loop anchor')
    before, rest = source.split(BEGIN, 1)
    _, after = rest.split(END, 1)
    source = before + NEW_METHOD + END + after
    source = source.replace(OLD_LOOP,
            '            List<KnownWorldC4RoadPainter.Entrance> entrances = entrancesNear(p, wx, wz);\n'
            + OLD_LOOP, 1)
    source = source.replace(OLD_CALL, '                road(chunk, x, z, p, entrances);', 1)
    source = source.replace(OLD_SURVEY, NEW_SURVEY, 1)
    # Force all displayed survey counts to use the same cache as construction.
    source = source.replace('if (survey(l).safe)', 'if (approvedSurvey(l).safe)')
    return source


def install(check: bool = False):
    validate_manifest()
    for name in ('KnownWorldC4StreetGrade.java', 'KnownWorldC4RoadPainter.java'):
        if not (JAVA.parent / name).is_file():
            raise ValueError(f'Missing C4.2 source {name}; extract all ZIP files first')
    if not JAVA.is_file():
        raise ValueError('Missing KnownWorldUniversalWorldgenC2.java')
    existing = JAVA.read_text(encoding='utf-8')
    updated = build_patch(existing)
    if check:
        if MARKER not in existing or existing != updated:
            raise ValueError('C4.2 integration is not installed')
        print('C4.2 CHECK PASSED: two sites and chunk-local street connection')
        return
    if updated == existing:
        print('C4.2 street patch already installed; no changes')
        return
    BACKUPS.mkdir(parents=True, exist_ok=True)
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    (BACKUPS / f'KnownWorldUniversalWorldgenC2_BEFORE_C4_2_{stamp}.java').write_text(
        existing, encoding='utf-8')
    JAVA.write_text(updated, encoding='utf-8')
    print('C4.2 connected road/door painting; backed up existing C2 Java source')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    install(args.check)
