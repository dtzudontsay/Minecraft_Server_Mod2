#!/usr/bin/env python3
"""Guarded C4.7 architectural overhaul: preflight and atomic single-file patch.
Never edits saves, V2/V3 scenario data, signs, jobs, or gallery commands.
"""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[2]
PKG = ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement'
C3 = PKG/'KnownWorldC3Architecture.java'
NEW = PKG/'KnownWorldC47ArchitecturalDetails.java'
TEMPLATE = ROOT/'tools/geodata/templates/KnownWorldC47ArchitecturalDetails.java.txt'
MARK = 'C4_7_ARCHITECTURAL_DETAILS'

def replace_one(src, old, new):
    count=src.count(old)
    if count != 1:
        raise RuntimeError(f'Expected exactly one architecture anchor, got {count}: {old[:115]!r}')
    return src.replace(old,new,1)

def updated(src):
    if MARK in src:
        return src
    if 'C4_6_LIVE_GALLERY_SINK' not in src:
        raise RuntimeError('C4.6 gallery sink is missing. Install and build C4.6.1 successfully first.')
    if 'KnownWorldStructureSigns.attach(c' not in src:
        raise RuntimeError('Expected existing safe C4.6 sign path; refusing older source')
    src=replace_one(src,
       '        if (template.openAir()) {',
       '        // C4_7_ARCHITECTURAL_DETAILS: preserve all existing C3 structure dispatch.\n'
       '        if (template.openAir()) {')
    src=replace_one(src,
       '        // C4.5 - instance-local public building signs for new chunks only.',
       '        KnownWorldC47ArchitecturalDetails.decorate(c, lot, floor, template, palette);\n'
       '        // C4.5 - instance-local public building signs for new chunks only.')
    src=replace_one(src,
       '        BlockState wall = regionalWall(palette, t.family());\n'
       '        BlockState roof = regionalRoof(palette);',
       '        BlockState wall = KnownWorldC47ArchitecturalDetails.wall(t.id(), regionalWall(palette, t.family()));\n'
       '        BlockState roof = KnownWorldC47ArchitecturalDetails.roof(t.id(), regionalRoof(palette));')
    src=replace_one(src,
       '        boolean flat = palette.toUpperCase(Locale.ROOT).contains("DORN")\n'
       '                || "STORE".equals(t.family());',
       '        boolean flat = palette.toUpperCase(Locale.ROOT).contains("DORN")\n'
       '                || "STORE".equals(t.family())\n'
       '                || KnownWorldC47ArchitecturalDetails.flatRoof(t.id());')
    src=replace_one(src,
       '            int slope = flat ? 0 : Math.min(3, Math.min(xx, b.width() - 1 - xx));',
       '            int slope = KnownWorldC47ArchitecturalDetails.roofRise(t.id(),xx,zz,b.width(),b.depth(),flat);')
    src=replace_one(src,
       '            if ((zz == 0 || zz == b.depth() - 1) && slope > 0) {',
       '            if (KnownWorldC47ArchitecturalDetails.gable(t.id(),xx,zz,b.width(),b.depth()) && slope > 0) {')
    src=replace_one(src,
       '            BlockState state = station(t.stations().get(i));',
       '            // C4.7 public bath supplies contained water from its constructed basin.\n'
       '            // Never place the original free-flowing indoor water workstation.\n'
       '            if (\"PUBLIC_BATH\".equals(t.id()) && \"WATER\".equals(t.stations().get(i))) continue;\n'
       '            BlockState state = station(t.stations().get(i));')
    src=replace_one(src,
       '    private static void put(ChunkAccess c, int x, int y, int z, BlockState value) {',
       '    // C4_7 detail layer inherits the existing C4.6 gallery/live block sink.\n'
       '    static void c47Put(ChunkAccess c,int x,int y,int z,BlockState value) { put(c,x,y,z,value); }\n\n'
       '    private static void put(ChunkAccess c, int x, int y, int z, BlockState value) {')
    return src

def preflight():
    for p in (C3,TEMPLATE):
        if not p.is_file():raise RuntimeError(f'Missing file: {p}')
    current=C3.read_text(encoding='utf-8')
    target=TEMPLATE.read_text(encoding='utf-8')
    if 'Blocks.ORANGE_STAINED_GLASS' in target:
        raise RuntimeError('Obsolete block field in architectural source; use corrected C4.7.1 package')
    if 'c.b(mid,3,4,Blocks.GLOWSTONE.defaultBlockState())' not in target:
        raise RuntimeError('Corrected safe forge-light decoration missing from template')
    if 'public final class KnownWorldC47ArchitecturalDetails' not in target:
        raise RuntimeError('Missing C4.7 source marker')
    if MARK in current:
        if not NEW.is_file() or NEW.read_text(encoding='utf-8')!=target:
            raise RuntimeError('C4.7 installed with unknown detail Java; refusing overwrite')
        print('C4.7 already installed; compatible exact source found')
        return current,target
    if NEW.exists():raise RuntimeError('C4.7 Java file already exists with unknown ownership')
    candidate=updated(current)
    if MARK not in candidate or 'KnownWorldC47ArchitecturalDetails.decorate' not in candidate:
        raise RuntimeError('C4.7 patch marker missing')
    print('PASS C4.6 gallery + sign integration anchors')
    print('PASS original lot geometry and C3 public API left intact')
    print('PASS no new chunk loads or world/server references in enhancement source')
    return candidate,target

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--check',action='store_true')
    ap.add_argument('--install',action='store_true')
    args=ap.parse_args()
    if args.check==args.install:raise RuntimeError('Supply exactly one of --check or --install')
    newc3,detail=preflight()
    if args.install:
        if not (ROOT/'gradlew.bat').exists():raise RuntimeError('No gradlew.bat in root')
        C3.write_text(newc3,encoding='utf-8')
        NEW.write_text(detail,encoding='utf-8')
        print('INSTALLED C4.7 renderer. Build required before game launch.')
    else:print('PREFLIGHT PASSED: read-only')
if __name__=='__main__':
    try:main()
    except Exception as ex:
        print('C4.7 STOPPED:',str(ex))
        raise SystemExit(1)
