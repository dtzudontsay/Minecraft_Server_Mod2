#!/usr/bin/env python3
"""Guarded installation of C4.6 gallery, C3 live preview sink, and safe signs.
Default --check changes NOTHING. --install makes backups then updates guarded
source anchors; never modifies a Minecraft save or an unrelated class.
"""
from pathlib import Path
import argparse,os,shutil,sys
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/main/java/dev/dtzudontsay/knownworld'
PKG=BASE/'world/generation/settlement'
C3=PKG/'KnownWorldC3Architecture.java';SIG=PKG/'KnownWorldStructureSigns.java';MAIN=BASE/'KnownWorld.java'
GALLERY=PKG/'KnownWorldBlueprintGallery.java';DIM=PKG/'KnownWorldBlueprintGalleryDimensions.java'
TEMPLATES=ROOT/'tools/geodata/templates'
MARK='C4_6_LIVE_GALLERY_SINK'

def exactly(s,anchor,new,description):
    if s.count(anchor)!=1:raise ValueError(f'{description}: expected exactly one exact anchor, got {s.count(anchor)}: {anchor[:100]!r}')
    return s.replace(anchor,new,1)

def patch_c3(src):
    if MARK in src:
        print('C3 gallery sink already installed');return src
    src=exactly(src,'import net.minecraft.core.BlockPos;\n',
      'import net.minecraft.core.BlockPos;\nimport net.minecraft.server.level.ServerLevel;\n', 'C3 import')
    marker='    private static volatile Map<String, Template> catalog;\n'
    addition='''    // C4_6_LIVE_GALLERY_SINK: render the SAME C3 template into a loaded
    // superflat inspection world via ServerLevel.setBlock (client-sync safe).
    // Thread-local only, always cleared after the one gallery structure.
    private static final ThreadLocal<ServerLevel> GALLERY_LEVEL = new ThreadLocal<>();

    public static void buildLiveGalleryLot(ServerLevel level, Lot lot, int floor,
                                           String palette, String tier) {
        if (GALLERY_LEVEL.get() != null) throw new IllegalStateException("Nested gallery painter");
        GALLERY_LEVEL.set(level);
        try {
            int minCX=lot.x()>>4, maxCX=(lot.x()+lot.width()+2)>>4;
            int minCZ=lot.z()>>4, maxCZ=(lot.z()+lot.depth()+3)>>4;
            for (int cx=minCX;cx<=maxCX;cx++)
                for (int cz=minCZ;cz<=maxCZ;cz++)
                    buildLot(level.getChunk(cx,cz),lot,floor,palette,tier);
        } finally { GALLERY_LEVEL.remove(); }
    }
'''
    src=exactly(src,marker,marker+addition,'C3 state')
    src=exactly(src,
       '        if (template.requiresShore() || template.requiresMountainResource() || template.requiresPolicy()) {',
       '        if (GALLERY_LEVEL.get() == null && (template.requiresShore() || template.requiresMountainResource() || template.requiresPolicy())) {',
       'C3 site policy')
    src=exactly(src,
       '        KnownWorldStructureSigns.attach(c, lot.x(), lot.z(), lot.width(),\n                lot.depth(), floor, lot.face(), requested);',
       '        if (GALLERY_LEVEL.get() == null)\n            KnownWorldStructureSigns.attach(c, lot.x(), lot.z(), lot.width(),\n                    lot.depth(), floor, lot.face(), requested);',
       'C3 signs')
    src=exactly(src,
       '        c.setBlockState(new BlockPos(x, y, z), value, 0);',
       '        ServerLevel live = GALLERY_LEVEL.get();\n'
       '        if (live == null) c.setBlockState(new BlockPos(x, y, z), value, 0);\n'
       '        else live.setBlock(new BlockPos(x, y, z), value, 2);',
       'C3 put function')
    return src

def patch_main(src):
    if 'KnownWorldBlueprintGallery.register();' in src:
        print('Gallery startup already installed');return src
    import_anchor='import dev.dtzudontsay.knownworld.world.generation.settlement.WinterfellWorldgenV3;\n'
    src=exactly(src,import_anchor,
       'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldBlueprintGallery;\n'+import_anchor,
       'mod main gallery import')
    src=exactly(src,'        WinterfellWorldgenV3.registerDebugCommands();',
        '        WinterfellWorldgenV3.registerDebugCommands();\n\n        KnownWorldBlueprintGallery.register();',
        'mod main gallery command hook')
    if src.count('KnownWorldStructureSigns.register();')!=1:
        raise ValueError('C4.5.2+ safe signs must have registered startup hook')
    return src

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--check',action='store_true');ap.add_argument('--install',action='store_true')
    a=ap.parse_args()
    if a.install and os.environ.get('KNOWNWORLD_C46_CONFIRM')!='NEW_WORLD':raise ValueError('C4.6 install requires NEW_WORLD confirmation')
    if not (ROOT/'gradlew.bat').is_file():raise ValueError('Must be in project root with Gradle wrapper')
    for p in (C3,SIG,MAIN):
        if not p.is_file():raise ValueError('Missing core mod file '+str(p))
    source=SIG.read_text(encoding='utf8')
    if 'public final class KnownWorldStructureSigns' not in source or 'C4_5_2_DEFERRED_SIGNS' not in source:
        raise ValueError('Expected C4.5.3/C4.5.4 prior sign implementation; refusing unknown source')
    # Additional strictness: avoid overwriting a later edited/custom sign helper.
    if 'C4_6_BOUNDED' in source:raise ValueError('Unexpected preexisting C4.6 sign implementation')
    c3=C3.read_text(encoding='utf8');main=MAIN.read_text(encoding='utf8')
    gallery_source=TEMPLATES/'KnownWorldBlueprintGallery.java.txt'
    dim_source=TEMPLATES/'KnownWorldBlueprintGalleryDimensions.java.txt'
    if not gallery_source.is_file() or not dim_source.is_file():
        raise ValueError('Missing the C4.6 gallery Java templates under tools/geodata/templates')
    for dest,source_file in ((GALLERY,gallery_source),(DIM,dim_source)):
        if dest.exists() and dest.read_text(encoding='utf8') != source_file.read_text(encoding='utf8'):
            raise ValueError('An unexpected gallery Java file already exists; refusing overwrite: '+str(dest))
    changes={C3:patch_c3(c3),MAIN:patch_main(main)}
    # Sign Java is overwritten separately only after checking its known old marker.
    sign_template=ROOT/'tools/geodata/c4_6_sign_source.txt'
    if not sign_template.is_file():raise ValueError('Missing C4.6 safe signage source template')
    changes[SIG]=sign_template.read_text(encoding='utf8')
    changes[GALLERY]=gallery_source.read_text(encoding='utf8')
    changes[DIM]=dim_source.read_text(encoding='utf8')
    if '.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))' not in changes[GALLERY] or 'import net.minecraft.server.permissions.Permissions;' not in changes[GALLERY]:
        raise ValueError('C4.6.1 gallery missing verified Minecraft 26.3 permission guard')
    if 'source.hasPermission(2)' in changes[GALLERY]:
        raise ValueError('Outdated Minecraft permission API in gallery template')
    if 'C4.6 BOUNDED' not in changes[SIG]:raise ValueError('Unrecognized sign template content')
    for path,content in changes.items():
        print(('NEW' if not path.exists() else 'CHANGE' if path.read_text(encoding='utf8')!=content else 'UNCHANGED'),path.relative_to(ROOT))
    if not a.install:print('PREFLIGHT PASSED (read-only Java check)');return
    for path,content in changes.items():
        if path.is_file() and path.read_text(encoding='utf8')==content:continue
        backup=Path(str(path)+'.before_c46.bak')
        if path.exists() and not backup.exists():shutil.copy2(path,backup)
        temporary=Path(str(path)+'.c46.tmp')
        temporary.write_text(content,encoding='utf8');os.replace(temporary,path)
    print('C4.6 Java safely installed. Run Gradle build before opening Minecraft.')
if __name__=='__main__':
    try:main()
    except Exception as exc:
        print('STOPPED:',type(exc).__name__,str(exc),file=sys.stderr);sys.exit(1)
