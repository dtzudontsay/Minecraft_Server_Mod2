import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch as patch_var
from install_atlas_teleport_c412 import patched, MARKER
import install_atlas_teleport_c412 as installer

class TestPatch(unittest.TestCase):
    def test_original_to_new_and_idempotent(self):
        original = ('x\n    private static void teleport(\n'
                    ' findNaturalLanding(\n makeRescuePlatform(\n player.teleportTo(\n'
                    ' lastTeleportNanos LEVEL_GAMEMASTERS\n'
                    '    private static boolean withinBorder(\ny\n')
        changed = patched(original)
        self.assertIn(MARKER, changed)
        self.assertIn('surfaceY + 5', changed)
        self.assertIn('requestedX + 0.5', changed)
        self.assertEqual(changed, patched(changed))

    def test_fail_closed_unknown_source(self):
        with self.assertRaises(ValueError):
            patched('unknown')

    def test_root_resolves_to_installer_parent(self):
        # Regression: installer in tools/ must resolve its parent repository,
        # *not* the parent of that repository (original D:\src failure).
        self.assertEqual(installer.ROOT, Path(installer.__file__).resolve().parents[1])
        self.assertEqual(installer.TARGET, installer.ROOT /
                         'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/KnownWorldUiPackets.java')

    def test_cli_check_and_install_inside_a_fake_repo(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / 'GameOfThronesMod2'
            target = root / 'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/KnownWorldUiPackets.java'
            target.parent.mkdir(parents=True)
            original = ('x\n    private static void teleport(\n'
                        ' findNaturalLanding(\n makeRescuePlatform(\n player.teleportTo(\n'
                        ' lastTeleportNanos LEVEL_GAMEMASTERS\n'
                        '    private static boolean withinBorder(\ny\n')
            target.write_text(original, encoding='utf-8')
            with patch_var.object(installer, 'ROOT', root), patch_var.object(installer, 'TARGET', target):
                with patch_var('sys.argv', ['install_atlas_teleport_c412.py', '--check']):
                    installer.main()
                self.assertEqual(target.read_text(encoding='utf-8'), original)
                with patch_var('sys.argv', ['install_atlas_teleport_c412.py']):
                    installer.main()
                    installer.main()
                self.assertIn(MARKER, target.read_text(encoding='utf-8'))
                self.assertTrue(target.with_suffix('.java.before_c4_12_atlas.bak').is_file())

if __name__ == '__main__':
    unittest.main()
