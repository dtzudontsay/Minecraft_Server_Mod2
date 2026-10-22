#!/usr/bin/env python3
"""C4.8: optional, guarded atlas UI update. Only edits one client screen and adds one helper."""
from pathlib import Path
import shutil,sys
ROOT=Path(__file__).resolve().parents[2]
PACKAGE_TEMPLATE=ROOT/'tools/geodata/c4_8_atlas_site_tags.java.txt'
SCREEN=ROOT/'src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java'
HELPER=ROOT/'src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasSiteTags.java'
OLD='gfx.text(font, info.name(), tx + 8, ty + 8, WHITE, true);'
NEW='gfx.text(font, info.name() + KnownWorldAtlasSiteTags.suffix(info.id()), tx + 8, ty + 8, WHITE, true);'

def patch(old):
    if old.count(NEW)==1 and OLD not in old:return old
    if old.count(OLD)!=1 or NEW in old:
        raise ValueError('Atlas tooltip source differs from C4.8 preflight; no files changed')
    return old.replace(OLD,NEW)

def install():
    old=SCREEN.read_text(encoding='utf-8')
    fresh=patch(old)
    helper=PACKAGE_TEMPLATE.read_text(encoding='utf-8')
    if HELPER.exists() and HELPER.read_text(encoding='utf-8')!=helper:
        raise ValueError('Existing atlas tag class differs; refusing to overwrite')
    # A rollback backup is created by the caller before any write.
    SCREEN.write_text(fresh,encoding='utf-8')
    HELPER.write_text(helper,encoding='utf-8')
    print('Installed read-only atlas tooltip tier tags')

def check():
    patch(SCREEN.read_text(encoding='utf-8'))
    helper=PACKAGE_TEMPLATE.read_text(encoding='utf-8')
    if HELPER.exists() and HELPER.read_text(encoding='utf-8')!=helper:
        raise ValueError('Unexpected atlas tag class: '+str(HELPER))
    print('C4.8 atlas UI anchors compatible')

if __name__=='__main__':
    if sys.argv[1:]==['--check']:check()
    elif sys.argv[1:]==['--install']:install()
    else:raise SystemExit('Usage: --check | --install')
