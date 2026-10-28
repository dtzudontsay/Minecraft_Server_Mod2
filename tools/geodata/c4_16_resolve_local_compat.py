#!/usr/bin/env python3
"""C4.16 stage compatibility bridge for the exact local B4.1/B4.2 sources.

Changes only TWO baseline + TWO updated STAGED files. Never writes real Java
sources, generated resources, B4.0 identities, dialogue saves, or Minecraft saves.
Preserves the original C4.16 guarded installer and snapshot logic.
"""
from __future__ import annotations
import argparse
import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parents[2]
STAGE = Path('C4_16_STAGE')
PAYLOAD = Path('C4_16_COMPAT_PAYLOAD')
SNAP = Path('tools/geodata/output/c4_16_safe_snapshots/compatibility_stages')
RELS = (
    'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java',
    'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java',
)

def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def install(repo: Path, check: bool = False) -> bool:
    repo = repo.resolve()
    manifest_file = repo / PAYLOAD / 'compat_manifest.json'
    original_installer = repo / 'tools/geodata/c4_16_stage_apply.py'
    if not manifest_file.is_file() or not original_installer.is_file():
        raise FileNotFoundError('Original C4.16 installer and C4_16_COMPAT_PAYLOAD required at project root')
    manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    expected = manifest['expected_original_stage']
    replacements = manifest['replacement_stage']
    states = {}
    for rel in RELS:
        src = repo / rel
        if not src.is_file():
            raise FileNotFoundError(f'Missing current Java source: {rel}')
        local_hash = digest(src)
        o = expected[rel]
        n = replacements[rel]
        if local_hash == o['local']:
            local_state = 'MATCHES_UPLOADED_LOCAL'
        elif local_hash == n['updated']:
            local_state = 'C4_16_ALREADY_INSTALLED'
        else:
            raise ValueError(f'Local Java source has changed since upload; refusing: {rel}')
        for layer in ('baseline','updated'):
            target = repo / STAGE / layer / rel
            payload = repo / PAYLOAD / layer / rel
            if not target.is_file() or not payload.is_file():
                raise FileNotFoundError(f'Missing stage/payload file: {layer}/{rel}')
            if digest(payload) != n[layer]:
                raise ValueError('Compatibility payload is corrupt or incomplete: '+str(payload))
            found = digest(target)
            if found == n[layer]:
                status = 'ALREADY_COMPATIBLE'
            elif found == o[layer]:
                status = 'READY_TO_REPLACE_STAGE'
            else:
                raise ValueError('Unknown edits to C4.16 staged file; refusing: '+str(target))
            states[f'{layer}/{rel}'] = status
        states[f'live/{rel}'] = local_state
    print(json.dumps(states, indent=2))
    needs = [x for x, state in states.items() if state == 'READY_TO_REPLACE_STAGE']
    if not needs:
        print('C4.16 compatibility stage is already adjusted; no files changed.')
        return False
    if check:
        print('Read-only check passed. Run without --check to repair the C4.16 staged files.')
        return False
    timestamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup = repo / SNAP / timestamp
    backup.mkdir(parents=True, exist_ok=False)
    copied = []
    for layer in ('baseline','updated'):
        for rel in RELS:
            dest = repo / STAGE / layer / rel
            keep = backup / layer / rel
            keep.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(dest, keep)
            copied.append(f'{layer}/{rel}')
    (backup/'C4_16_COMPAT_STAGE_BACKUP.json').write_text(
        json.dumps({'kind':'c4_16_compat_stage_backup','files':copied},indent=2),encoding='utf-8')
    try:
        for layer in ('baseline','updated'):
            for rel in RELS:
                dest = repo / STAGE / layer / rel
                data = (repo / PAYLOAD / layer / rel).read_bytes()
                temp = dest.with_suffix(dest.suffix + '.c416compat_tmp')
                temp.write_bytes(data)
                os.replace(temp, dest)
        for layer in ('baseline','updated'):
            for rel in RELS:
                assert digest(repo/STAGE/layer/rel) == replacements[rel][layer]
    except Exception:
        for layer in ('baseline','updated'):
            for rel in RELS:
                src = backup / layer / rel
                dest = repo / STAGE / layer / rel
                shutil.copy2(src, dest)
        print('Restored staged files after failed compatibility installation.',file=sys.stderr)
        raise
    print('Adjusted 4 STAGED Java comparison files only. Original live Java remains untouched.')
    print('Original stage backups:', backup)
    print('Now run .\\RUN_C4_16_ORGANIC_TOWNS_AND_WORK_ATTENDANCE.bat')
    return True

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--root',type=Path,default=ROOT)
    parser.add_argument('--check',action='store_true')
    opts = parser.parse_args()
    try:
        install(opts.root,opts.check)
    except (OSError, ValueError, KeyError, AssertionError) as error:
        print('C4.16 compatibility SAFE STOP:',error,file=sys.stderr)
        raise SystemExit(1)

if __name__ == '__main__':
    main()
