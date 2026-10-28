import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'tools/geodata/install_c4_13_worldgen_diagnostics.py'
spec=importlib.util.spec_from_file_location('c413_installer',SRC)
p=importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)

class PatchTests(unittest.TestCase):
    def test_main_idempotent(self):
        source=('import dev.dtzudontsay.knownworld.debug.KnownWorldGeoDebugCommand;\n'
                '        WinterfellWorldgenV3.registerDebugCommands();\n')
        changed=p.edit_main(source)
        self.assertIn('KnownWorldC413ConstructionDiagnostics.register();',changed)
        self.assertEqual(p.edit_main(changed),changed)
    def test_atlas_snap_idempotent(self):
        src=('                    teleportTargetX=(int)Math.round((mx-KnownWorldAtlasConfig.MASTER_ORIGIN_X)\n'
             '                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);\n'
             '                    teleportTargetZ=(int)Math.round((mz-KnownWorldAtlasConfig.MASTER_ORIGIN_Y)\n'
             '                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);')
        got=p.edit_atlas(src)
        self.assertIn('pin.masterX()',got)
        self.assertIn('pin.masterY()',got)
        self.assertIn('C4_13_MARKER_SNAP',got)
        self.assertEqual(p.edit_atlas(got),got)
    def test_bad_anchor_fails_without_writes(self):
        with self.assertRaises(ValueError):p.edit_main('class KnownWorld {}')
        with self.assertRaises(ValueError):p.edit_atlas('class Atlas {}')
    def test_installer_dry_run_leaves_source_untouched(self):
        with tempfile.TemporaryDirectory() as temporary:
            base=Path(temporary)
            main=base/'src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
            atlas=base/'src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java'
            main.parent.mkdir(parents=True);atlas.parent.mkdir(parents=True)
            main.write_text('import dev.dtzudontsay.knownworld.debug.KnownWorldGeoDebugCommand;\n        WinterfellWorldgenV3.registerDebugCommands();\n')
            atlas.write_text('''                    teleportTargetX=(int)Math.round((mx-KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
                    teleportTargetZ=(int)Math.round((mz-KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);''')
            before=main.read_bytes(),atlas.read_bytes()
            p.execute(base,True)
            self.assertEqual(before,(main.read_bytes(),atlas.read_bytes()))

if __name__=='__main__':unittest.main()
