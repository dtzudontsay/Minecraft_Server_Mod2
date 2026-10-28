"""Regression tests for recovery of B4.2B pending index folders with real site IDs."""
import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent


def load_hotfix():
    spec = importlib.util.spec_from_file_location('b42b_pending_hotfix', ROOT / 'tools/geodata/b4_2b_fast_mobility_hotfix.py')
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


H = load_hotfix()


class PendingRecovery(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = Path(self.tmp.name)
        for source, dest in [
            (HERE / 'original_b4_2b_mobility_index.py', self.repo / H.REL),
            (HERE / 'b4_2b_mobility_index.py', self.repo / H.PAYLOAD),
        ]:
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, dest)
        self.site_ids = ['site_nw_missing_twins_01'] + [f'site_sw_{i:03}' for i in range(1, 248)]
        manifest = self.repo / H.PROJECTION_MANIFEST
        manifest.parent.mkdir(parents=True, exist_ok=True)
        manifest.write_text(json.dumps({'schemaVersion': H.PROJECTION_SCHEMA, 'sites': [{'siteId': x} for x in self.site_ids]}), encoding='utf8')
        self.pending = H.pending_path(self.repo)

    def create_fragment(self, site='site_nw_missing_twins_01'):
        dest = self.pending / site / 'p00.json.gz'
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(b'original interrupted index bytes')
        return dest

    def test_unusual_site_id_validates_from_authoritative_manifest(self):
        self.create_fragment()
        self.assertEqual(H.status(self.repo)['incomplete_mobility_index'], 'PRESENT_SAFE_TO_ARCHIVE')
        H.apply(self.repo)
        self.assertEqual(H.status(self.repo)['generator'], 'ALREADY_PATCHED')
        self.assertFalse(self.pending.exists())
        archives = list((self.repo / H.SNAP).glob('*/incomplete_previous_index/site_nw_missing_twins_01/p00.json.gz'))
        self.assertEqual(len(archives), 1)
        self.assertEqual(archives[0].read_bytes(), b'original interrupted index bytes')

    def test_manual_rollback_preserves_unusual_site_id_and_bytes(self):
        self.create_fragment()
        H.apply(self.repo)
        H.rollback(self.repo, 'latest')
        self.assertEqual(H.sha(self.repo / H.REL), H.BASE_HASH)
        self.assertEqual((self.pending / 'site_nw_missing_twins_01/p00.json.gz').read_bytes(), b'original interrupted index bytes')

    def test_reject_non_project_site_id_even_if_looks_normal(self):
        self.create_fragment('site_nw_001')
        with self.assertRaisesRegex(ValueError, 'Unknown data'):
            H.apply(self.repo)
        self.assertEqual(H.sha(self.repo / H.REL), H.BASE_HASH)
        self.assertTrue((self.pending / 'site_nw_001/p00.json.gz').exists())

    def test_reject_unknown_files_but_leave_them_untouched(self):
        self.create_fragment()
        other = self.pending / 'site_nw_missing_twins_01' / 'my_saves.dat'
        other.write_bytes(b'important data')
        with self.assertRaisesRegex(ValueError, 'Unknown file'):
            H.apply(self.repo)
        self.assertEqual(other.read_bytes(), b'important data')
        self.assertEqual(H.sha(self.repo / H.REL), H.BASE_HASH)

    def test_reject_corrupted_manifest_without_modifying_live_or_pending(self):
        source = self.repo / H.PROJECTION_MANIFEST
        doc = json.loads(source.read_text(encoding='utf8'))
        doc['sites'].append({'siteId': 'unexpected_extra'})
        source.write_text(json.dumps(doc), encoding='utf8')
        self.create_fragment()
        with self.assertRaisesRegex(ValueError, 'Wrong B4.1A.1 projection manifest'):
            H.apply(self.repo)
        self.assertEqual(H.sha(self.repo / H.REL), H.BASE_HASH)
        self.assertTrue((self.pending / 'site_nw_missing_twins_01/p00.json.gz').exists())

    def test_real_world_path_is_not_permitted_in_pending(self):
        self.create_fragment()
        foreign = self.pending / 'level.dat'
        foreign.write_bytes(b'no touching')
        with self.assertRaisesRegex(ValueError, 'Unknown data'):
            H.apply(self.repo)
        self.assertEqual(foreign.read_bytes(), b'no touching')


if __name__ == '__main__':
    unittest.main()
