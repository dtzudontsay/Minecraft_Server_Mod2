"""Isolated tests for the safe C4.17 stage-only compatibility repair."""
from __future__ import annotations
import importlib.util
import shutil
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location('c417_fix', ROOT/'tools/geodata/c4_17_fix_local_line_endings.py')
M = importlib.util.module_from_spec(spec)
spec.loader.exec_module(M)

class CompatTests(unittest.TestCase):
    def fixture(self, root: Path, live_crlf: bool = True):
        for rel in (M.REL, M.BASE, M.UPDATED):
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
        # Stage files were extracted at the real workspace root.
        shutil.copy2(ROOT/M.BASE, root/M.BASE)
        shutil.copy2(ROOT/M.UPDATED, root/M.UPDATED)
        pristine = (ROOT/M.BASE).read_bytes()
        (root/M.REL).write_bytes(pristine.replace(b'\n', b'\r\n') if live_crlf else pristine)
        return (root/M.REL).read_bytes()

    def test_patched_state_is_ready_and_live_file_untouched(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            original = self.fixture(root)
            self.assertEqual(M.run(root, check_only=True), 'READ_ONLY_PASS')
            self.assertEqual((root/M.BASE).read_bytes(), (ROOT/M.BASE).read_bytes())
            self.assertEqual(M.run(root), 'REPAIRED')
            self.assertEqual((root/M.REL).read_bytes(), original)
            self.assertEqual((root/M.BASE).read_bytes(), original)
            self.assertEqual(M.run(root), 'ALREADY_COMPATIBLE')
            backups = list((root/M.SNAPSHOTS).glob('*/baseline_B41Activation.java'))
            self.assertEqual(len(backups), 1)
            self.assertEqual(backups[0].read_bytes(), (ROOT/M.BASE).read_bytes())

    def test_unmodified_lf_state_requires_no_patch(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, False)
            self.assertEqual(M.run(root), 'ALREADY_COMPATIBLE')

    def test_real_local_edits_must_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            old = (root/M.BASE).read_bytes()
            (root/M.REL).write_bytes((root/M.REL).read_bytes() + b'// custom local source change\r\n')
            with self.assertRaises(ValueError):
                M.run(root)
            self.assertEqual((root/M.BASE).read_bytes(), old)

    def test_stage_baseline_custom_edits_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            (root/M.BASE).write_bytes((root/M.BASE).read_bytes() + b'// custom stage edit\n')
            with self.assertRaises(ValueError):
                M.run(root)

    def test_updated_stage_custom_edits_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            (root/M.UPDATED).write_bytes((root/M.UPDATED).read_bytes() + b'// altered update\n')
            with self.assertRaises(ValueError):
                M.run(root)

if __name__=='__main__':
    unittest.main()
