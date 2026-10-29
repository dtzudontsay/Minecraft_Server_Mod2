#!/usr/bin/env python3
"""B4.2B.5 guarded read-only economic authority bridge installer.

Run from D:\\GameOfThronesMod2, without admin rights. Does NOT touch saves,
resources, economy snapshots, generated outputs, or any Git remote.
"""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

HERE = Path(__file__).resolve().parent
STAGE = HERE.parent
REPO = Path.cwd().resolve()
PACKAGE_JAVA = Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
B42B2 = PACKAGE_JAVA/'KnownWorldB42B2EconomicStanding.java'
B42B3 = PACKAGE_JAVA/'KnownWorldB42B3StartingStock.java'
NEW = PACKAGE_JAVA/'KnownWorldB42B5HouseholdAuthority.java'
DOCS = [Path('docs/architecture/24_30_B4_2B5_CANONICAL_HOUSEHOLD_ECONOMIC_AUTHORITY_PREFLIGHT.md'),
        Path('docs/architecture/KNOWN_WORLD_VALUE_INTEGRATION_CONTRACT.md'),
        Path('docs/architecture/VALUE_COVERAGE_ALL_SOURCE_ROWS.csv')]
BACKUPS = REPO/'.git'/'knownworld-b4-2b5-backups'
TARGETS = [B42B2, B42B3, NEW, *DOCS]
OLD_REGISTER = '        KnownWorldB42B4HouseholdDaily.register();'
NEW_REGISTER = OLD_REGISTER + '\n        KnownWorldB42B5HouseholdAuthority.register();'
OLD_CANONICAL = '    private static SettlementId canonicalSettlement(String siteId) {'
NEW_CANONICAL = '    static SettlementId canonicalSettlement(String siteId) {'


def check():
    if not (REPO/'.git').is_dir():
        raise ValueError(f'Not a Git repository root: {REPO}')
    if not (REPO/'gradlew.bat').exists() and not (REPO/'gradlew').exists():
        raise ValueError('Missing Gradle wrapper')
    a = (REPO/B42B2).read_text(encoding='utf-8')
    b = (REPO/B42B3).read_text(encoding='utf-8')
    exists = (REPO/NEW).exists()
    installed = NEW_REGISTER in a and NEW_CANONICAL in b and exists
    if installed:
        if a.count(NEW_REGISTER) != 1 or b.count(NEW_CANONICAL) != 1:
            raise ValueError('Unexpected duplicate installed B4.2B.5 hooks')
        print('B4.2B.5 already installed: hooks and new Java source present.')
        return True
    if exists:
        raise ValueError('New Java target already exists without recognized hooks: refusing overwrite')
    if a.count(OLD_REGISTER) != 1 or b.count(OLD_CANONICAL) != 1:
        raise ValueError('Unrecognized B4.2B.2/B4.2B.3 source: abort without changes')
    if any((REPO/f).exists() for f in DOCS):
        raise ValueError('A B4.2B.5 documentation target already exists: refusing overwrite')
    for f in [NEW, *DOCS]:
        if not (STAGE/f).is_file():
            raise ValueError(f'Stage file missing: {STAGE/f}')
    for token in ['class KnownWorldB42B5HouseholdAuthority', 'public static Audit audit(String siteId)', 'static void register()']:
        if token not in (STAGE/NEW).read_text(encoding='utf-8'):
            raise ValueError(f'Unrecognized staged Java content: {token}')
    print('B4.2B.5 preflight PASS: correct source hooks and stage present.')
    return False


def write_atomic(path: Path, content: bytes):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + '.b425tmp')
    if temp.exists():
        raise ValueError(f'Interrupted prior write found: {temp}')
    try:
        temp.write_bytes(content)
        os.replace(temp, path)
    finally:
        if temp.exists():
            temp.unlink()


def restore(backup: Path):
    meta = json.loads((backup/'manifest.json').read_text(encoding='utf-8'))
    if meta.get('batch') != 'B4_2B5' or len(meta['paths']) != len(TARGETS):
        raise ValueError('Invalid backup manifest')
    for e in meta['paths']:
        rel = Path(e['path'])
        if rel.is_absolute() or '..' in rel.parts or rel not in TARGETS:
            raise ValueError('Unsafe backup path')
    for e in meta['paths']:
        rel = Path(e['path'])
        if e['existed']:
            write_atomic(REPO/rel, (backup/rel).read_bytes())
        else:
            if (REPO/rel).exists():
                (REPO/rel).unlink()
    print('Restored all B4.2B.5-managed files from', backup)


def install():
    if check():
        print('Nothing to install. Run Gradle/build checks if desired.')
        return
    stamp = time.strftime('%Y%m%d_%H%M%S', time.localtime())
    backup = BACKUPS/stamp
    if backup.exists():
        raise ValueError('Backup collision; no edits made')
    backup.mkdir(parents=True)
    entries = []
    for rel in TARGETS:
        current = REPO/rel
        exists = current.is_file()
        entries.append({'path': rel.as_posix(), 'existed': exists})
        if exists:
            out = backup/rel
            out.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(current, out)
    (backup/'manifest.json').write_text(json.dumps({'batch':'B4_2B5','paths':entries},indent=2),encoding='utf-8')
    print('SAFETY BACKUP:', backup)
    try:
        # Patch raw bytes to preserve the repository's actual CRLF/LF policy.
        a = (REPO/B42B2).read_bytes()
        b = (REPO/B42B3).read_bytes()
        newline = b'\r\n' if b'\r\n' in a else b'\n'
        register = OLD_REGISTER.encode('utf-8')
        replacement = register + newline + b'        KnownWorldB42B5HouseholdAuthority.register();'
        write_atomic(REPO/B42B2, a.replace(register, replacement, 1))
        write_atomic(REPO/B42B3, b.replace(OLD_CANONICAL.encode('utf-8'), NEW_CANONICAL.encode('utf-8'), 1))
        for rel in [NEW, *DOCS]:
            write_atomic(REPO/rel, (STAGE/rel).read_bytes())
        print('Source installed. Running focused checks...')
        subprocess.run([sys.executable,str(HERE/'test_b4_2b5_authority.py')],cwd=REPO,check=True)
        gradle = REPO/('gradlew.bat' if os.name == 'nt' else 'gradlew')
        if not gradle.is_file():
            gradle = REPO/'gradlew.bat'
        print('Running Gradle compileJava; no world is launched or modified.')
        subprocess.run([str(gradle),'compileJava','--no-daemon'],cwd=REPO,check=True,shell=(os.name=='nt'))
    except Exception:
        print('ERROR detected; restoring managed source/docs (not touching world saves).')
        restore(backup)
        raise
    print('B4.2B.5 installed and compiled. No economic transactions activated.')
    print('Run in-game: /kwbridge status ; /kwbridge site site_ne_001')
    print('To undo later: py -3 B4_2B5_STAGE\\scripts\\b4_2b5_guarded_install.py --restore "' + str(backup) + '"')


def main():
    p=argparse.ArgumentParser()
    a=p.add_mutually_exclusive_group(required=True)
    a.add_argument('--check-only',action='store_true')
    a.add_argument('--install',action='store_true')
    a.add_argument('--restore',type=Path)
    args=p.parse_args()
    if args.check_only:
        check()
        subprocess.run([sys.executable,str(HERE/'test_b4_2b5_authority.py')],cwd=REPO,check=True)
    elif args.install:install()
    else:
        back=args.restore.resolve()
        if BACKUPS.resolve() not in back.parents:
            raise ValueError('Restore must reference this repository\'s managed backup directory')
        restore(back)

if __name__ == '__main__':
    try:main()
    except Exception as ex:
        print('B4.2B.5 STOP:',ex,file=sys.stderr)
        sys.exit(1)
