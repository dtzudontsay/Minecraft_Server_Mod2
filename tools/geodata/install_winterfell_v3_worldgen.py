#!/usr/bin/env python3
"""Install V3 chunk-worldgen hook into the exact current Known World repo.

Safe, narrow modification of existing files; creates .v3-backup on first run.
Does not change the existing 5,000 resident roster, characters or settlement list.
Do NOT run in an existing built world expecting modified chunks to regenerate.
"""
from pathlib import Path
import re
import sys

ROOT=Path(__file__).resolve().parents[2]
GEN=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/KnownWorldChunkGenerator.java'
LEGACY=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellV2.java'
ENTRY=ROOT/'src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
HOOK='import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;'
CALL='WinterfellWorldgenV3.decorate(level, chunk);'


def patch(path:Path, original:str, updated:str):
    if original==updated:
        print(f'Already installed: {path.relative_to(ROOT)}')
        return
    backup=path.with_suffix(path.suffix+'.v3-backup')
    if not backup.exists():backup.write_text(original,encoding='utf8')
    path.write_text(updated,encoding='utf8')
    print(f'Updated: {path.relative_to(ROOT)} (backup: {backup.name})')


def main():
    if not GEN.exists() or not LEGACY.exists() or not ENTRY.exists():
        sys.exit('Run inside the current Minecraft_Server_Mod2 repo; generator/legacy class missing.')
    base=GEN.read_text(encoding='utf8')
    updated=base
    if HOOK not in updated:
        sentinel='import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerators;'
        # In the target class we are inside the SAME world.generation package;
        # insert after a known import from this source instead.
        match=re.search(r'^import dev\.dtzudontsay\.knownworld\.world\.data\.GeographicDataManager;\s*$',updated,re.M)
        if not match:
            sys.exit('KnownWorldChunkGenerator.java imports changed. No file modified; inspect before patching.')
        updated=updated[:match.end()]+'\n'+HOOK+updated[match.end():]
    if CALL not in updated:
        pattern=r'(super\.applyBiomeDecoration\(\s*level\s*,\s*chunk\s*,\s*structureManager\s*\)\s*;)'
        matches=list(re.finditer(pattern,updated))
        if len(matches)!=1:
            sys.exit('Expected exactly one super.applyBiomeDecoration call. No file modified.')
        m=matches[0]
        updated=updated[:m.end()]+'\n\n        // Chunk-local, deterministic Winterfell V3 settlement construction.\n        '+CALL+updated[m.end():]
    previous=LEGACY.read_text(encoding='utf8')
    replacement=previous
    # V2's timer-based builder would overwrite V3 buildings. Disable the
    # entire old test controller until Stage 3 reuses its actor materializer.
    if 'public static final boolean ENABLED = true;' in replacement:
        replacement=replacement.replace('public static final boolean ENABLED = true;',
                                        'public static final boolean ENABLED = false; // V3 chunk-worldgen replaces live V2 construction.',1)
    elif 'public static final boolean ENABLED = false;' not in replacement:
        sys.exit('V2 ENABLED switch differs; refusing to change controller.')
    newclass=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/WinterfellWorldgenV3.java'
    data=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v3_worldgen.json'
    if not newclass.exists() or not data.exists():
        sys.exit('V3 Java class or data not present; extract entire ZIP into repo root first.')
    entry=ENTRY.read_text(encoding='utf8')
    modified=entry
    entry_import='import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;'
    if entry_import not in modified:
        marker='import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerators;'
        if marker not in modified:sys.exit('KnownWorld.java initialization imports changed; no files modified.')
        modified=modified.replace(marker,marker+'\n'+entry_import,1)
    if 'WinterfellWorldgenV3.registerDebugCommands();' not in modified:
        marker='KnownWorldChunkGenerators.register();'
        if marker not in modified:sys.exit('KnownWorld.java initialization changed; no files modified.')
        modified=modified.replace(marker,marker+'\n\n        WinterfellWorldgenV3.registerDebugCommands();',1)
    patch(GEN,base,updated)
    patch(LEGACY,previous,replacement)
    patch(ENTRY,entry,modified)
    print('V3 worldgen installed. Existing already-generated chunks will NOT be changed.')

if __name__=='__main__':main()
