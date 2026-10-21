"""C4.2 source patch tests without Minecraft or a local Java toolchain."""
import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SCRIPT = Path(__file__).with_name('install_c4_terrain_roads.py')
spec = importlib.util.spec_from_file_location('c42_patch', SCRIPT)
patcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patcher)

SOURCE = '''
class KnownWorldUniversalWorldgenC2 {
    private static final String RESOURCE = "settlement_construction_c4_v1.json";
    private static void decorate() {
            for (int index = cp.roads.nextSetBit(0); index >= 0; index = cp.roads.nextSetBit(index + 1)) {
                road(chunk, x, z, p);
            }
                Survey terrain = survey(lot);
                if ("FIELD".equals(lot.kind())) {
                    buildLot(chunk, lot, terrain.floor, p.palette, wx, wz);
                }
    }
    private static void road(ChunkAccess c, int x, int z, Pilot p) {
        // ancient road that assumed every street joined p.z
    }
    private static void clearHazards(ChunkAccess c, int x, int z, int terrainY) {
    }
}
'''

class C42Tests(unittest.TestCase):
    def test_source_patch_is_idempotent(self):
        first = patcher.build_patch(SOURCE)
        self.assertEqual(first, patcher.build_patch(first))
        self.assertIn('KnownWorldC4RoadPainter.paint(', first)
        self.assertIn('entrancesNear(p, wx, wz)', first)
        self.assertIn('approvedSurvey(lot)', first)
        self.assertIn('road(chunk, x, z, p, entrances)', first)
        self.assertNotIn('ancient road', first)

    def test_rejects_missing_c4_manifest_hook(self):
        with self.assertRaisesRegex(ValueError, 'manifest'):
            patcher.build_patch(SOURCE.replace('settlement_construction_c4_v1.json',
                                              'settlement_construction_c2_v1.json'))

    def test_rejects_missing_field_dispatch(self):
        with self.assertRaisesRegex(ValueError, 'FIELD'):
            patcher.build_patch(SOURCE.replace('if ("FIELD".equals(lot.kind())) {', 'if (false) {'))

    def test_never_mutates_worlds_or_winterfell(self):
        patched = patcher.build_patch(SOURCE)
        self.assertNotIn('ServerLevel', patched)
        self.assertNotIn('WinterfellWorldgenV3', patched)
        self.assertNotIn('setBlockAndUpdate', patched)

    def test_bad_install_fails_without_modifying_java(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td)/'KnownWorldUniversalWorldgenC2.java'
            path.write_text('unknown incompatible source')
            with patch.object(patcher, 'JAVA', path):
                with self.assertRaises(ValueError):
                    patcher.build_patch(path.read_text())
            self.assertEqual(path.read_text(), 'unknown incompatible source')

if __name__ == '__main__':
    unittest.main()
