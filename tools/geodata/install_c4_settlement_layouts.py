#!/usr/bin/env python3
"""Minimal, guarded C4 integration into the EXISTING C2 chunk-local renderer.

C4 owns its own generated manifest. Original C2 resource/generator remain intact.
No need to touch KnownWorldChunkGenerator.java, fabric.mod.json or Winterfell.
"""
from __future__ import annotations
import argparse
import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / ('src/main/java/dev/dtzudontsay/knownworld/world/generation/'
               'settlement/KnownWorldUniversalWorldgenC2.java')
BACKUP = ROOT / 'tools/geodata/output/c4_settlements/backups'
OLD_RESOURCE = 'settlement_construction_c2_v1.json'
NEW_RESOURCE = 'settlement_construction_c4_v1.json'
OLD_CALL = 'KnownWorldC3Architecture.buildLot(chunk, lot, terrain.floor, p.palette, p.tier);'
NEW_CALL = ('if ("FIELD".equals(lot.kind())) {\n'
            '                    // FIELD uses the C2 irrigated-field painter; C3 intentionally\n'
            '                    // rejects it as a building template.\n'
            '                    buildLot(chunk, lot, terrain.floor, p.palette, wx, wz);\n'
            '                } else {\n'
            '                    KnownWorldC3Architecture.buildLot(chunk, lot, terrain.floor, p.palette, p.tier);\n'
            '                }')


def integrate(check: bool = False):
    if not JAVA.exists():
        raise FileNotFoundError('C2 source absent; install C2 and C3 before C4')
    s = JAVA.read_text(encoding='utf-8')
    if 'class KnownWorldUniversalWorldgenC2' not in s or 'C2 pilot manifest invalid' not in s:
        raise ValueError('Unexpected C2 Java revision. Refusing blind source edits.')
    if OLD_RESOURCE not in s and NEW_RESOURCE not in s:
        raise ValueError('C2 resource declaration changed. Manual review required.')
    if OLD_CALL not in s and NEW_CALL not in s:
        raise ValueError('C3 build hook changed. Manual review required.')
    out = s.replace(OLD_RESOURCE, NEW_RESOURCE, 1)
    if NEW_CALL not in out:
        if out.count(OLD_CALL) != 1:
            raise ValueError('C3 hook ambiguous: cannot safely install C4 FIELD dispatch')
        out = out.replace(OLD_CALL, NEW_CALL, 1)
    if check:
        if out != s or NEW_RESOURCE not in s or NEW_CALL not in s:
            raise ValueError('C4 integration missing; run installer without --check')
        print('C4 INTEGRATION CHECK PASSED')
        return
    if out == s:
        print('C4 hooks already installed (no changes)')
        return
    BACKUP.mkdir(parents=True,exist_ok=True)
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    (BACKUP / f'KnownWorldUniversalWorldgenC2_BEFORE_C4_{stamp}.java').write_text(s,encoding='utf-8')
    JAVA.write_text(out,encoding='utf-8')
    print('C4 resource selected; FIELD painting repaired; previous C2 saved to backups')


if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('--check',action='store_true')
    integrate(p.parse_args().check)
