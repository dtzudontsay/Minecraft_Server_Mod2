#!/usr/bin/env python3
"""B4.0 surgical hotfix: accept existing canonical descriptive site IDs.

Edits only the site-ID regular expressions in the previously installed Python
validator and Java census loader. Does not touch generated population files,
other Java classes, Minecraft saves, worldgen, or any network/GitHub endpoint.
"""
from __future__ import annotations

import argparse
import datetime as dt
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PY_REL = Path('tools/geodata/b4_0_validate_runtime.py')
JAVA_REL = Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB40PopulationIndex.java')

# Require lower-case, path-safe, underscore-separated ID components. Supports
# both site_nw_029 and canonical descriptive IDs such as site_nw_missing_twins_01.
OLD_PY = r"SITE_PATTERN = re.compile(r'site_[a-z]+_[0-9]{3}\Z')"
NEW_PY = r"SITE_PATTERN = re.compile(r'site_[a-z0-9]+(?:_[a-z0-9]+)+\Z')"
OLD_JAVA = 'id.matches("site_[a-z]+_[0-9]{3}")'
NEW_JAVA = 'id.matches("site_[a-z0-9]+(?:_[a-z0-9]+)+")'


def patched(content: str, old: str, new: str, label: str) -> tuple[str, bool]:
    """Return new bytes as text, idempotently; reject unexpected or mixed revisions."""
    matches_old = content.count(old)
    matches_new = content.count(new)
    if matches_old == 1 and matches_new == 0:
        return content.replace(old, new, 1), True
    if matches_old == 0 and matches_new == 1:
        return content, False
    raise RuntimeError(
        f'{label}: unexpected validator version: found {matches_old} old and '
        f'{matches_new} new rules. File NOT modified; inspect it manually.'
    )


def install(root: Path) -> None:
    changes = []
    for rel, old, new in ((PY_REL, OLD_PY, NEW_PY), (JAVA_REL, OLD_JAVA, NEW_JAVA)):
        path = root / rel
        if not path.is_file():
            raise FileNotFoundError(f'Missing previously installed B4.0 file: {path}')
        raw = path.read_bytes()
        content = raw.decode('utf-8-sig')
        result, changed = patched(content, old, new, str(rel))
        # Complete dry-preflight for all targets BEFORE changing anything.
        changes.append((path, raw, result.encode('utf-8'), changed))
    stamp = dt.datetime.now().strftime('%Y%m%d_%H%M%S')
    for path, raw, updated, changed in changes:
        if not changed:
            print(f'ALREADY CORRECT: {path.relative_to(root)}')
            continue
        backup = path.with_name(path.name + f'.before_b4_0_site_id_fix_{stamp}.bak')
        backup.write_bytes(raw)
        path.write_bytes(updated)
        print(f'PATCHED: {path.relative_to(root)}')
        print(f'  Backup: {backup.relative_to(root)}')
    print('B4.0 site-ID rules now accept descriptive canonical identifiers.')
    print('No resident shards or Minecraft saves were regenerated or modified.')


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=ROOT)
    args = parser.parse_args()
    install(args.repo.resolve())

if __name__ == '__main__':
    main()
