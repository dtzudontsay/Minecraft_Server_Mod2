#!/usr/bin/env python3
"""Idempotent patch: register runtime diagnostics and snap right-click atlas teleports to nearby marker.

Reads local working tree, makes backups and only alters two existing Java files.
No GitHub writes, world/save edits, forced chunk generation or population changes.
"""
from __future__ import annotations
import argparse
import datetime
import difflib
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
ATLAS = ROOT / 'src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java'
IMPORT = 'import dev.dtzudontsay.knownworld.debug.KnownWorldC413ConstructionDiagnostics;'
CALL = '        KnownWorldC413ConstructionDiagnostics.register();'


def edit_main(source: str) -> str:
    if IMPORT in source and CALL in source:
        return source
    if (IMPORT in source) != (CALL in source):
        raise ValueError('C4.13 partial install in KnownWorld.java, inspect it first')
    marker = 'import dev.dtzudontsay.knownworld.debug.KnownWorldGeoDebugCommand;'
    registration = '        WinterfellWorldgenV3.registerDebugCommands();'
    if source.count(marker) != 1 or source.count(registration) != 1:
        raise ValueError('KnownWorld.java registration markers missing/ambiguous')
    source = source.replace(marker, marker + '\n' + IMPORT)
    return source.replace(registration, registration + '\n\n' + CALL)


def edit_atlas(source: str) -> str:
    # No change to free placement away from any marker; only snap nearby named places.
    if 'C4_13_MARKER_SNAP' in source:
        return source
    original = '''                    teleportTargetX=(int)Math.round((mx-KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
                    teleportTargetZ=(int)Math.round((mz-KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);'''
    replacement = '''                    // C4_13_MARKER_SNAP: source-image pixels cover kilometres.
                    // A right click near a pin must use that marker's exact X/Z;
                    // clicking elsewhere still targets the arbitrary map coordinate.
                    KnownWorldAtlasMarkers.Marker pin=nearestMarker(px,pz);
                    double targetMx=pin==null ? mx : pin.masterX();
                    double targetMz=pin==null ? mz : pin.masterY();
                    teleportTargetX=(int)Math.round((targetMx-KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
                    teleportTargetZ=(int)Math.round((targetMz-KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);'''
    if source.count(original) != 1:
        raise ValueError('Atlas right-click coordinate handler changed; no unsafe blind edit')
    return source.replace(original, replacement)


def execute(root: Path, dry_run=False):
    for path, transform in ((root / MAIN.relative_to(ROOT), edit_main),
                            (root / ATLAS.relative_to(ROOT), edit_atlas)):
        if not path.is_file():
            raise FileNotFoundError(f'Missing local repository file: {path}')
        before = path.read_text(encoding='utf-8')
        after = transform(before)
        if before == after:
            print('ALREADY APPLIED',path.relative_to(root))
            continue
        if dry_run:
            print(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),
                                               fromfile=str(path),tofile='C4.13 revised')))
            continue
        stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
        backup=path.with_name(path.name+'.before_c4_13_'+stamp+'.bak')
        shutil.copy2(path,backup)
        temporary=path.with_name(path.name+'.c413_tmp')
        temporary.write_text(after,encoding='utf-8')
        temporary.replace(path)
        print('PATCHED',path.relative_to(root),'backup',backup.name)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dry-run',action='store_true')
    parser.add_argument('--root',type=Path,default=ROOT)
    args=parser.parse_args()
    execute(args.root,args.dry_run)

if __name__=='__main__':
    main()
