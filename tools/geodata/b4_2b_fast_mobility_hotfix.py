#!/usr/bin/env python3
"""Guarded B4.2B.1 mobility generator performance hotfix.

Replaces ONLY tools/geodata/b4_2b_mobility_index.py, after exact
SHA-256 baseline matching, and safely relocates any interrupted .pending
mobility-index data to a separate immutable recovery folder. Does not
modify B4.0 data, Java, world saves or source resources.
"""
from __future__ import annotations
import argparse
import datetime
import hashlib
import json
import os
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REL = Path('tools/geodata/b4_2b_mobility_index.py')
PAYLOAD = Path('B4_2B_FAST_PAYLOAD/b4_2b_mobility_index.py')
SNAP = Path('tools/geodata/output/b4_2b_safe_snapshots/generator_hotfixes')
MOBILITY = Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_mobility_b4_2b_v1')
PROJECTION_MANIFEST = MOBILITY.parent / 'world_projection_b4_1a1_v1' / 'manifest.json'
PROJECTION_SCHEMA = 'knownworld.b4_1a1.household_projection.v1'
BASE_HASH = '28d45fe206df1cec492fd3e7f7617db480d25ebae96982066a761a0ab614815b'
PATCH_HASH = 'd9ccb33c365303a0ad218fa3e7cb6448f67c308c86991de286770d9254ea6f6f'


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as inp:
        for chunk in iter(lambda: inp.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def pending_path(root):
    p = root / MOBILITY
    return p.with_name(p.name + '.pending')


def is_link_or_junction(path):
    """Never traverse symlinks or Windows directory junctions in recovery data."""
    return path.is_symlink() or (getattr(path, 'is_junction', lambda: False)())


def authoritative_site_ids(root):
    """Use the actual B4.1A.1 projection catalog, not a made-up site-name regex.

    The projection generator reads each manifest siteId verbatim to create its
    output folders. IDs such as site_nw_missing_twins_01 are therefore valid.
    Legacy numeric-name fallback is only useful for isolated test fixtures
    that deliberately do not bundle a project projection manifest.
    """
    manifest = Path(root) / PROJECTION_MANIFEST
    if not manifest.exists():
        return None
    if is_link_or_junction(manifest) or not manifest.is_file():
        raise ValueError('Projection manifest is not a regular file: ' + str(manifest))
    try:
        source = json.loads(manifest.read_text(encoding='utf8'))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ValueError('Cannot validate pending output against projection manifest') from exc
    entries = source.get('sites')
    if source.get('schemaVersion') != PROJECTION_SCHEMA or not isinstance(entries, list) or len(entries) != 248:
        raise ValueError('Wrong B4.1A.1 projection manifest; refusing to relocate pending data')
    names = [site.get('siteId') if isinstance(site, dict) else None for site in entries]
    if not all(isinstance(name, str) and re.fullmatch(r'site_[a-z0-9_]+', name) for name in names):
        raise ValueError('Invalid projected site ID; refusing to relocate pending data')
    if len(set(names)) != len(names):
        raise ValueError('Duplicate projected site IDs; refusing to relocate pending data')
    return set(names)


def verify_pending(directory, root=None):
    """Relocate only recognized B4.2B output; never delete incomplete data.

    Site folders must belong to the projection manifest. The contents are
    generated p00..p15 compressed partition files, possibly truncated because
    the original job was interrupted; don't read/decompress or alter them.
    """
    if not directory.exists():
        return False
    if is_link_or_junction(directory) or not directory.is_dir():
        raise ValueError('Pending output is not a normal directory: ' + str(directory))
    trusted = authoritative_site_ids(root) if root is not None else None
    for site in directory.iterdir():
        if is_link_or_junction(site):
            raise ValueError('Unexpected link/junction in pending output: ' + str(site))
        if site.is_file() and site.name == 'manifest.json':
            continue
        # When there is no projection manifest (old synthetic fixtures), stay
        # as strict as the original hotfix; don't expand the name whitelist.
        if not site.is_dir() or (site.name not in trusted if trusted is not None else not re.fullmatch(r'site_[a-z]+_[0-9]{3}', site.name)):
            raise ValueError('Unknown data in pending output; refusing to move: ' + str(site))
        for entry in site.iterdir():
            if is_link_or_junction(entry) or not entry.is_file() or not re.fullmatch(r'p(?:0[0-9]|1[0-5])\.json\.gz', entry.name):
                raise ValueError('Unknown file in pending output; refusing to move: ' + str(entry))
    return True


def status(root):
    root = Path(root).resolve()
    live = root / REL
    proposed = root / PAYLOAD
    if not proposed.is_file() or sha(proposed) != PATCH_HASH:
        raise ValueError('Hotfix payload is missing or corrupted; re-extract the ZIP')
    if not live.is_file() or live.is_symlink():
        raise ValueError('Expected live B4.2B generator is missing or symlinked')
    actual = sha(live)
    name = 'READY_TO_PATCH' if actual == BASE_HASH else 'ALREADY_PATCHED' if actual == PATCH_HASH else 'UNKNOWN_LOCAL_EDITS'
    pending = pending_path(root)
    has_pending = verify_pending(pending, root)
    return {'generator': name, 'incomplete_mobility_index': 'PRESENT_SAFE_TO_ARCHIVE' if has_pending else 'NONE', 'live_mobility_index': 'PRESENT' if (root / MOBILITY).exists() else 'NOT_INSTALLED'}


def apply(root):
    root = Path(root).resolve()
    st = status(root)
    print(json.dumps(st, indent=2), flush=True)
    if st['generator'] == 'UNKNOWN_LOCAL_EDITS':
        raise ValueError('Refusing to overwrite unknown local modifications')
    if st['generator'] == 'ALREADY_PATCHED' and st['incomplete_mobility_index'] == 'NONE':
        print('Mobility performance hotfix already applied; no changes.')
        return
    if st['live_mobility_index'] == 'PRESENT':
        raise ValueError('Mobility output already exists. Do not patch during or after B4.2B installation; inspect first.')
    # Keep snapshots strictly outside the original batch managed paths.
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    dest = root / SNAP / stamp
    if dest.exists():
        raise FileExistsError('Snapshot ID collision')
    dest.mkdir(parents=True, exist_ok=False)
    original_copy = dest / 'original_b4_2b_mobility_index.py'
    shutil.copy2(root / REL, original_copy)
    archived_pending = dest / 'incomplete_previous_index'
    had_pending = st['incomplete_mobility_index'] == 'PRESENT_SAFE_TO_ARCHIVE'
    info = {
        'batch': 'B4_2B_FAST_MOBILITY_HOTFIX',
        'originalGeneratorSha256': sha(original_copy),
        'updatedGeneratorSha256': PATCH_HASH,
        'interruptedIndexArchived': had_pending,
    }
    (dest / 'HOTFIX_MANIFEST.json').write_text(json.dumps(info, indent=2), encoding='utf8')
    try:
        if had_pending:
            os.replace(pending_path(root), archived_pending)
            print('Preserved interrupted mobility index:', archived_pending, flush=True)
        if st['generator'] == 'READY_TO_PATCH':
            tmp = (root / REL).with_name('b4_2b_mobility_index.py.hotfix_tmp')
            if tmp.exists():
                raise FileExistsError('Unrecognized temporary patch file: ' + str(tmp))
            shutil.copy2(root / PAYLOAD, tmp)
            if sha(tmp) != PATCH_HASH:
                raise ValueError('Copied hotfix checksum mismatch')
            os.replace(tmp, root / REL)
        if sha(root / REL) != PATCH_HASH:
            raise ValueError('Post-install checksum mismatch')
        print('HOTFIX APPLIED. Generator backup:', dest, flush=True)
        print('Next: run RUN_B4_2B_CHECK_ONLY.bat and then the original installation BAT.', flush=True)
    except BaseException:
        print('HOTFIX ERROR: restoring generator and archived incomplete index', file=sys.stderr)
        if original_copy.exists() and sha(original_copy) == info['originalGeneratorSha256']:
            shutil.copy2(original_copy, root / REL)
        if had_pending and archived_pending.exists() and not pending_path(root).exists():
            os.replace(archived_pending, pending_path(root))
        raise


def rollback(root, label):
    root = Path(root).resolve()
    folder = root / SNAP
    if label == 'latest':
        options = sorted(p for p in folder.iterdir() if p.is_dir() and (p / 'HOTFIX_MANIFEST.json').is_file()) if folder.is_dir() else []
        if not options:
            raise FileNotFoundError('No mobility hotfix backup')
        previous = options[-1]
    else:
        if not re.fullmatch(r'\d{8}_\d{6}_\d{6}', label):
            raise ValueError('Invalid snapshot label')
        previous = folder / label
    info = json.loads((previous / 'HOTFIX_MANIFEST.json').read_text(encoding='utf8'))
    original_copy = previous / 'original_b4_2b_mobility_index.py'
    if info.get('batch') != 'B4_2B_FAST_MOBILITY_HOTFIX' or not original_copy.is_file() or sha(original_copy) != info['originalGeneratorSha256']:
        raise ValueError('Invalid or corrupt hotfix backup')
    if sha(root / REL) != PATCH_HASH:
        raise ValueError('Current generator does not match patched version, refusing rollback')
    if (root / MOBILITY).exists() or pending_path(root).exists():
        raise ValueError('Mobility output exists. Do not roll back hotfix until index has been handled.')
    shutil.copy2(original_copy, root / REL)
    archived_pending = previous / 'incomplete_previous_index'
    if info.get('interruptedIndexArchived') and archived_pending.exists():
        os.replace(archived_pending, pending_path(root))
    print('RESTORED original mobility generator:', previous.name)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root', type=Path, default=ROOT)
    mode = p.add_mutually_exclusive_group(required=True)
    mode.add_argument('--check', action='store_true')
    mode.add_argument('--apply', action='store_true')
    mode.add_argument('--rollback', metavar='LATEST_OR_ID')
    args = p.parse_args()
    try:
        if args.check:
            print(json.dumps(status(args.root), indent=2))
        elif args.apply:
            apply(args.root)
        else:
            rollback(args.root, args.rollback)
    except Exception as exc:
        print('B4.2B FAST HOTFIX SAFE STOP:', repr(exc), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
