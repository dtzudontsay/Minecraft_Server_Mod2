from __future__ import annotations
import sys,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/geodata'))
from b4_1a_install import main_patch,global_patch,ui_patch,PLACEMENT

class B41Installer(unittest.TestCase):
    def test_main_idempotence(self):
        raw=('import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB40PopulationIndex;\n'
             'public class KnownWorld {\n        KnownWorldB40PopulationIndex.register();\n}')
        patched=main_patch(raw)
        self.assertIn('KnownWorldB41Activation.register();',patched)
        self.assertEqual(main_patch(patched),patched)
    def test_main_missing_b40_rejected(self):
        with self.assertRaisesRegex(ValueError,'B4.0'):main_patch('public class KnownWorld {}')
    def test_global_idempotence(self):
        src='public class X {\n    public static void decorate(WorldGenLevel level,ChunkAccess chunk) {\n}}'
        patched=global_patch(src)
        self.assertIn('b41ApprovedPlacementForInspection',patched)
        self.assertEqual(global_patch(patched),patched)
        self.assertIn('placements.computeIfAbsent',PLACEMENT)
    def test_ui_title_for_knownworld_only(self):
        base='gfx.text(font,"WINTERFELL  •  CHARACTER RECORD",x+13,y+8,GOLD,true);'
        out=ui_patch(base)
        self.assertIn('value("id").startsWith("b4:")',out)
        self.assertEqual(ui_patch(out),out)
    def test_bad_partial_main_rejected(self):
        with self.assertRaisesRegex(ValueError,'Partial'):main_patch('        KnownWorldB41Activation.register();')

if __name__=='__main__':unittest.main()
