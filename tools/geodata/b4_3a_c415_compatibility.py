#!/usr/bin/env python3
"""Repair B4.3A *staged* household-index sources after C4.15.

Read-only inspection by default; --apply changes ONLY two staged files. Never
modifies the live generator, Minecraft saves or B4.0 population. Refuses
unrecognised contents. Compatible with running an already-repaired stage again.
"""
from __future__ import annotations
import argparse
import datetime
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import sys

REPO = Path(__file__).resolve().parents[2]
REL = Path('tools/geodata/b4_1a1_household_index.py')
STAGE = Path('B4_3A_STAGE')
REFERENCE = Path('B4_3A_C415_COMPAT/c415_household_index_reference.py')
SNAPSHOTS = Path('tools/geodata/output/b4_3a_compat_stage_snapshots')
OLD_BASE_SHA = 'bf99bb9654b0f2c6c56e17887520891f0b6f6e9465627b03baacc4f5fd2e60e4'
OLD_NEXT_SHA = '68d62dd772e4986a508fc1a1814bab9b153140fccae0a12fda11a6581f9647de'
C415_SHA = '73b20d42d4d8288bd535bd938740d9a9ba32bb961d35ba1000aa0e87de9d3fa7'
OLD = b"'physicalProjectionLimit':128"
NEW = b"'physicalProjectionLimit':256"


def canonical(data: bytes) -> bytes:
    return data.replace(b'\r\n', b'\n')


def digest(data: bytes) -> str:
    return hashlib.sha256(canonical(data)).hexdigest()


def replacement(data: bytes) -> bytes:
    data = canonical(data)
    if data.count(OLD) != 1 or NEW in data:
        raise ValueError('Expected exactly one B4.1A.1 manifest limit 128; refusing unsafe change')
    new = data.replace(OLD, NEW, 1)
    if new.replace(NEW, OLD, 1) != data:
        raise AssertionError('Only the projection capacity may change')
    return new


def inspect(repo: Path):
    live = repo / REL
    base = repo / STAGE / 'baseline' / REL
    nextfile = repo / STAGE / 'updated' / REL
    reference = repo / REFERENCE
    for p in (live, base, nextfile, reference):
        if not p.is_file():
            raise FileNotFoundError('Missing required source: ' + str(p))
    source = canonical(reference.read_bytes())
    if digest(source) != C415_SHA:
        raise ValueError('Hotfix C4.15 reference corrupted')
    target = replacement(source)
    actual = canonical(live.read_bytes())
    oldbase = canonical(base.read_bytes())
    oldnext = canonical(nextfile.read_bytes())

    # Do not guess whether a locally modified generator can safely be rebased.
    if actual == source:
        live_state = 'C415_LIVE_VERIFIED'
    elif actual == target:
        live_state = 'B43_ALREADY_INSTALLED'
    else:
        live_state = 'UNKNOWN_LOCAL_EDITS'

    if oldbase == source and oldnext == target:
        stage_state = 'STAGE_ALREADY_REPAIRED'
    elif digest(oldbase) == OLD_BASE_SHA and digest(oldnext) == OLD_NEXT_SHA:
        if replacement(oldbase) != oldnext:
            raise ValueError('Original B4.3A stage delta unexpectedly differs from 128-to-256')
        stage_state = 'READY_TO_REPAIR_STAGE'
    else:
        stage_state = 'UNKNOWN_STAGE_EDITS'

    return {'liveGenerator': live_state,
            'b43Stage': stage_state,
            'c415CommuteOverlayPreserved': 'world_commutes_c4_15_v1' in source.decode('utf8'),
            'writeTargets': [str((STAGE / x / REL).as_posix()) for x in ('baseline', 'updated')]}, source, target


def assert_compatible_state(status):
    if status['liveGenerator'] not in ('C415_LIVE_VERIFIED', 'B43_ALREADY_INSTALLED'):
        raise ValueError('UNKNOWN_LOCAL_EDITS: live household generator is not byte-equivalent to C4.15; upload it for a proper merge')
    if status['b43Stage'] not in ('READY_TO_REPAIR_STAGE', 'STAGE_ALREADY_REPAIRED'):
        raise ValueError('UNKNOWN_STAGE_EDITS: staged B4.3A generator differs from expected; do not overwrite it')
    if not status['c415CommuteOverlayPreserved']:
        raise ValueError('Missing C4.15 commute-aware generator')
    if status['liveGenerator'] == 'B43_ALREADY_INSTALLED' and status['b43Stage'] != 'STAGE_ALREADY_REPAIRED':
        raise ValueError('Partial previous installation requires individual inspection')


def check_other_sources(repo: Path):
    # Validate the original B4.3A guard without letting it write. It uses the
    # repaired stage after --apply, or the original stage before the repair.
    path = repo/'tools/geodata/b4_3a_guarded_install.py'
    if not path.is_file():
        raise FileNotFoundError('Missing original B4.3A guarded installer')
    spec = importlib.util.spec_from_file_location('b43a_original_guard', path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module.check(repo), module.EXISTING, module.NEW, module.MANIFEST


def run(repo: Path, apply: bool):
    status, source, target = inspect(repo)
    print(json.dumps(status, indent=2), flush=True)
    assert_compatible_state(status)
    if status['b43Stage'] == 'READY_TO_REPAIR_STAGE':
        if not apply:
            print('C4.15 version confirmed; run without --check to adjust B4.3A staging only.')
            return
        if status['liveGenerator'] != 'C415_LIVE_VERIFIED':
            raise ValueError('Cannot repair stage when live B4.3A has already been installed')
        label = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
        out = repo/SNAPSHOTS/label
        out.mkdir(parents=True, exist_ok=False)
        base = repo/STAGE/'baseline'/REL
        new = repo/STAGE/'updated'/REL
        try:
            for branch,p in [('baseline',base),('updated',new)]:
                dest = out/branch/REL
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(p,dest)
            manifest = {'batch':'B4_3A_C415_STAGE_COMPAT','files':[
                {'stage':branch,'sha256':hashlib.sha256((repo/STAGE/branch/REL).read_bytes()).hexdigest()}
                for branch in ('baseline','updated')], 'liveSourceUnchanged':True}
            (out/'STAGE_BACKUP.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
            base.write_bytes(source)
            new.write_bytes(target)
            after, _, _ = inspect(repo)
            if after['b43Stage'] != 'STAGE_ALREADY_REPAIRED' or after['liveGenerator']!='C415_LIVE_VERIFIED':
                raise ValueError('Staged postcondition failed')
            print('B4.3A staged baseline and staged replacement updated. Stage backup:', out, flush=True)
        except BaseException:
            # If stage patch fails, restore *only* those two staged files.
            for branch,p in [('baseline',base),('updated',new)]:
                backup = out/branch/REL
                if backup.is_file():shutil.copy2(backup,p)
            raise
    else:
        print('B4.3A staged generator already compatible. No changes.', flush=True)

    orig, existing, new_files, manifest = check_other_sources(repo)
    print('ORIGINAL B4.3A GUARD PRECHECK:')
    print(json.dumps(orig,indent=2),flush=True)
    if status['liveGenerator']=='C415_LIVE_VERIFIED':
        expected={**{x:'READY' for x in existing},**{x:'NEW' for x in new_files},str(manifest):'READY'}
        if orig!=expected:
            raise ValueError('Original B4.3A precheck has remaining differences: '+str(orig))
    print('Compatibility repair complete; original B4.3A installer safety checks remain enabled.',flush=True)


def main():
    a=argparse.ArgumentParser(description=__doc__)
    a.add_argument('--root',type=Path,default=REPO)
    a.add_argument('--check',action='store_true')
    options=a.parse_args()
    try:run(options.root.resolve(), not options.check)
    except Exception as e:
        print('B4.3A C4.15 COMPATIBILITY SAFE STOP:',str(e),file=sys.stderr,flush=True)
        raise SystemExit(1)

if __name__=='__main__':main()
