#!/usr/bin/env python3
"""Fix a verified CRLF-vs-LF-only mismatch in C4.17's B41 stage baseline.

Fail closed for any actual source edit. Never touch the live Java file.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from datetime import datetime

ROOT = Path(__file__).resolve().parents[2]
REL = Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
BASE = Path('C4_17_STAGE/baseline') / REL
UPDATED = Path('C4_17_STAGE/updated') / REL
SNAPSHOTS = Path('tools/geodata/output/c4_17_safe_snapshots/compatibility_stages')

# Expected files from the unmodified C4.17 release. These hashes are
# provenance checks, not permissive "ignore local modifications" flags.
EXPECTED_BASE_SHA256 = '6fe1cb9dcf64abc3dbe145a8fd780d4859ae73555155b2f05f46a58cd4a97d98'
EXPECTED_UPDATED_SHA256 = '2747eb8622fa97db65333819b88a871f479855db8c611cc100ed1667274d07a6'


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def lf(data: bytes) -> bytes:
    return data.replace(b'\r\n', b'\n')


def verify(repo: Path) -> tuple[bytes, bytes, bool]:
    """Return live data, original baseline, and whether stage repair is needed."""
    live_file, base_file, updated_file = repo / REL, repo / BASE, repo / UPDATED
    for p in (live_file, base_file, updated_file):
        if not p.is_file():
            raise FileNotFoundError(f'Missing required C4.17 path: {p}')
    live = live_file.read_bytes()
    base = base_file.read_bytes()
    updated = updated_file.read_bytes()
    if digest(updated) != EXPECTED_UPDATED_SHA256:
        raise ValueError('C4.17 UPDATED stage is not the original released version; refusing to patch.')
    # The baseline may already have been repaired in a prior invocation.
    if digest(base) != EXPECTED_BASE_SHA256 and base != live:
        raise ValueError('C4.17 BASELINE stage has unexpected modifications; refusing to patch.')
    if lf(live) != lf(base):
        raise ValueError('Live B4.1A/LLM Java differs in actual code, not just CRLF line endings. STOP: upload the current file for an explicit merge.')
    if digest(lf(live)) != EXPECTED_BASE_SHA256:
        raise ValueError('Unexpected live B41 contents even after line-ending normalization; STOP.')
    return live, base, live != base


def run(repo: Path, check_only: bool = False) -> str:
    live, base, needs_repair = verify(repo)
    if not needs_repair:
        msg = 'ALREADY_COMPATIBLE: C4.17 staged baseline matches your live Java byte-for-byte.'
        print(msg, flush=True)
        return 'ALREADY_COMPATIBLE'
    print('CONFIRMED: live B41 source matches the expected C4.16 version, apart from CRLF line endings.', flush=True)
    if check_only:
        print('READ-ONLY PASS: staged baseline can be repaired safely; no files changed.', flush=True)
        return 'READ_ONLY_PASS'
    stamp = datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    save = repo / SNAPSHOTS / stamp
    save.mkdir(parents=True, exist_ok=False)
    stage_backup = save / 'baseline_B41Activation.java'
    shutil.copy2(repo / BASE, stage_backup)
    (save / 'manifest.json').write_text(json.dumps({
        'purpose': 'C4.17 stage-only CRLF compatibility correction',
        'liveJavaUnmodified': True,
        'originalStageSha256': digest(base),
        'liveSha256': digest(live),
        'stageBaseline': str(BASE).replace('\\', '/')
    }, indent=2), encoding='utf-8')
    parent = (repo / BASE).parent
    tmp = None
    try:
        with tempfile.NamedTemporaryFile(dir=parent, prefix='.c417-lineendings-', suffix='.tmp', delete=False) as f:
            tmp = Path(f.name)
            f.write(live)
        os.replace(tmp, repo / BASE)
        # Re-read, and preserve all original live bytes.
        assert (repo / BASE).read_bytes() == live
        verify(repo)
    except BaseException:
        shutil.copy2(stage_backup, repo / BASE)
        raise
    finally:
        if tmp is not None:
            tmp.unlink(missing_ok=True)
    print('REPAIRED: one STAGED baseline only. LIVE Java and UPDATED stage unchanged.', flush=True)
    print('STAGE BACKUP:', save, flush=True)
    return 'REPAIRED'


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--root', type=Path, default=ROOT)
    ap.add_argument('--check', action='store_true')
    args = ap.parse_args()
    try:
        run(args.root.resolve(), args.check)
    except Exception as exc:
        print('C4.17 COMPATIBILITY SAFE STOP:', exc, file=sys.stderr, flush=True)
        sys.exit(1)


if __name__ == '__main__':
    main()
