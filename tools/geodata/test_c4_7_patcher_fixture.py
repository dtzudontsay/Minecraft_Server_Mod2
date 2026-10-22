#!/usr/bin/env python3
"""Synthetic C4.6 anchor contract and idempotence test.
Run on package files only, never edits user project.
"""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from c4_7_install_architecture import updated,MARK
sample='''// C4_6_LIVE_GALLERY_SINK
void f() {
        if (template.openAir()) {
        }
        // C4.5 - instance-local public building signs for new chunks only.
        BlockState wall = regionalWall(palette, t.family());
        BlockState roof = regionalRoof(palette);
        boolean flat = palette.toUpperCase(Locale.ROOT).contains("DORN")
                || "STORE".equals(t.family());
            int slope = flat ? 0 : Math.min(3, Math.min(xx, b.width() - 1 - xx));
            if ((zz == 0 || zz == b.depth() - 1) && slope > 0) {
            BlockState state = station(t.stations().get(i));
    private static void put(ChunkAccess c, int x, int y, int z, BlockState value) {
        KnownWorldStructureSigns.attach(c,0,0,0,0,0,"NORTH","TEMPLE");
}'''
patched=updated(sample)
assert MARK in patched
assert 'KnownWorldC47ArchitecturalDetails.decorate' in patched
assert 'c47Put' in patched
assert '"PUBLIC_BATH".equals(t.id())' in patched
assert patched==updated(patched), 'Idempotence failed'
print('PASS C4.6 source anchors and idempotent patch test')
