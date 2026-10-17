#!/usr/bin/env python3
"""Create 50 *test-only* 24-person inspection pages from the existing Winterfell draft.

This does not add people to NpcRegistry, create housing, or alter a save except when
an operator manually runs one of the generated Minecraft functions.

For speed and safety there are at most 24 test villagers near the operator after
any ONE page. Inspect all 1200 people by loading pages sequentially. Real gameplay
will use distance-limited actor materialization, not 1200 always-ticking entities.

Run AFTER build_winterfell_case_study.py from repository root:
  py tools/geodata/build_winterfell_preview_pages.py
"""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ROSTER = ROOT / 'src/main/resources/data/knownworld/case_study/winterfell_roster.json'
OUT = ROOT / 'src/main/resources/data/knownworld/function/case_study'
PAGE_SIZE = 24


def main() -> None:
    if not ROSTER.is_file():
        raise SystemExit('Missing winterfell_roster.json: run build_winterfell_case_study.py first')
    j = json.loads(ROSTER.read_text(encoding='utf-8'))
    people = j['residents']
    assert len(people) == 1200 and len({p['id'] for p in people}) == 1200
    assert j['populationConservation']['cohorts'] == 0
    OUT.mkdir(parents=True, exist_ok=True)
    n_pages = (len(people) + PAGE_SIZE - 1) // PAGE_SIZE
    for page in range(n_pages):
        subset = people[page * PAGE_SIZE:(page + 1) * PAGE_SIZE]
        lines = [
            '# DISPOSABLE SUPERFLAT CREATIVE WORLD ONLY; no authoritative NpcIds allocated.',
            '# Page %03d / %03d of the 1200-record Winterfell roster.' % (page+1,n_pages),
            '# Only remove test villagers near the executing player, never untagged NPCs.',
            'kill @e[type=minecraft:villager,tag=kwf_case,distance=..80]',
        ]
        for i, p in enumerate(subset):
            ident = p['id']
            assert len('kwfid_'+ident) <= 64 and all(c.isalnum() or c in '_-' for c in ident)
            # Positions are only a temporary display grid, not their residences.
            dx = (i % 6) * 4
            dz = (i // 6) * 5
            name = json.dumps({'text': p['name']}, ensure_ascii=True, separators=(',', ':'))
            name = name.replace("'", "\\'")
            lines.append(
                f'summon minecraft:villager ~{dx} ~ ~{dz} '
                + '{Tags:["kwf_case","kwfid_' + ident + '"],'
                + 'NoAI:1b,PersistenceRequired:1b,CustomName:\'' + name + '\',CustomNameVisible:1b}'
            )
        lines.append('tellraw @s {"text":"Winterfell case study: showing page %03d / %03d (%d of 1200) -- right-click villagers; no authoritative simulation materialization.","color":"yellow"}' % (page+1,n_pages,len(subset)))
        (OUT / ('winterfell_page_%03d.mcfunction' % (page+1))).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (OUT/'winterfell_preview_clear.mcfunction').write_text(
        '# Removes ONLY tagged case-study villagers within 80 blocks.\n'
        'kill @e[type=minecraft:villager,tag=kwf_case,distance=..80]\n', encoding='utf-8')
    manifest = {
        'schemaVersion': 1,
        'mode': 'NON_AUTHORITATIVE_DISPOSABLE_WORLD_ONLY',
        'residentsRepresented': len(people),
        'pageSize': PAGE_SIZE,
        'numberOfPages': n_pages,
        'realNpcsRegistered': 0,
        'residencesPhysicallyBuilt': 0,
        'instructions': ['Run /function knownworld:case_study/winterfell_page_001',
                         'Continue through winterfell_page_050 to inspect every person.',
                         'Each page removes ONLY tagged villagers within 80 blocks.',
                         'Run /function knownworld:case_study/winterfell_preview_clear to remove current test proxies.']
    }
    manifest_path = ROOT/'docs/step3/winterfell_preview_pages_manifest.json'
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    assert len(list(OUT.glob('winterfell_page_*.mcfunction'))) == n_pages
    print('PASS: generated %d pages, 24 read-only test villagers each, covering %d distinct identities' % (n_pages,len(people)))


if __name__ == '__main__':
    main()
