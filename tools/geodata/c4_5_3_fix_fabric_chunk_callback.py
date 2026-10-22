#!/usr/bin/env python3
"""C4.5.3: correct Fabric ServerChunkEvents.Load's 3-argument callback.

Only changes one exact line in KnownWorldStructureSigns.java; safe to rerun.
--check verifies without writing; --install backs up and edits the source.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parents[2]
SIGN = ROOT / 'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldStructureSigns.java'
OLD = 'ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {'
NEW = 'ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {'
VERSION_MARKER = 'C4_5_2_DEFERRED_SIGNS'


def ensure_valid(source: str) -> bool:
    if VERSION_MARKER not in source:
        raise ValueError('Expected C4.5.2 deferred-sign helper not found; refusing to edit unrelated code.')
    if source.count(NEW) == 1 and source.count(OLD) == 0:
        print('PASS: Fabric CHUNK_LOAD callback already uses 3 arguments (C4.5.3 present).')
        return False
    if source.count(OLD) != 1 or source.count(NEW) != 0:
        raise ValueError(f'Expected exactly one old 2-argument callback, found {source.count(OLD)}; '
                         f'corrected callbacks: {source.count(NEW)}. No changes made.')
    print('PASS: found exactly one faulty 2-argument Fabric CHUNK_LOAD callback.')
    return True


def main() -> None:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--install', action='store_true')
    args = parser.parse_args()
    if not SIGN.is_file():
        raise FileNotFoundError(f'Missing sign source: {SIGN}. Check that this zip was extracted into the repository root.')
    source = SIGN.read_text(encoding='utf-8')
    needs_change = ensure_valid(source)
    if not needs_change:
        return
    if args.check:
        print('PREFLIGHT ONLY: correction can be applied; no files changed.')
        return
    backup = SIGN.with_name(SIGN.name + '.before_c4_5_3_callback.bak')
    if not backup.exists():
        shutil.copy2(SIGN, backup)
        print('BACKUP:', backup)
    corrected = source.replace(OLD, NEW, 1)
    SIGN.write_text(corrected, encoding='utf-8')
    if SIGN.read_text(encoding='utf-8').count(NEW) != 1:
        raise RuntimeError('Post-write verification failed.')
    print('FIXED:', SIGN)
    print('No other project files, scenario data, or Minecraft saves changed.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, RuntimeError) as exc:
        print('ERROR:', exc, file=sys.stderr)
        sys.exit(1)
