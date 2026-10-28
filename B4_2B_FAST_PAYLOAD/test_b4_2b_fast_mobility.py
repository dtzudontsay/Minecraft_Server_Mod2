"""Regression tests for a safe optimization of the B4.2B.1 mobility index."""
import importlib.util
import json
import random
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent

def mod(path, name):
    spec = importlib.util.spec_from_file_location(name, str(path))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result

ORIGINAL = mod(HERE / 'original_b4_2b_mobility_index.py', 'mob_original')
OPTIMIZED = mod(HERE / 'b4_2b_mobility_index.py', 'mob_optimized')
PATCHER = mod(ROOT / 'tools/geodata/b4_2b_fast_mobility_hotfix.py', 'mob_patcher')

class Tests(unittest.TestCase):
    def setUp(self):
        self.rng = random.Random(1903)

    def venues(self, n):
        kinds = ['TAVERN', 'INN', 'MARKET', 'SCHOOL', 'TEMPLE', 'COURTYARD']
        return [{'id': f'site_nw_001:venue_{i}',
                 'x': self.rng.randrange(-2000, 2000),
                 'z': self.rng.randrange(-2000, 2000),
                 'w': self.rng.randrange(3, 20), 'd': self.rng.randrange(3, 20),
                 'kind': kinds[i % len(kinds)]} for i in range(n)]

    def test_original_generator_sha_is_known(self):
        self.assertEqual(PATCHER.sha(HERE / 'original_b4_2b_mobility_index.py'), PATCHER.BASE_HASH)
        self.assertEqual(PATCHER.sha(HERE / 'b4_2b_mobility_index.py'), PATCHER.PATCH_HASH)

    def test_indexed_destinations_match_original_for_children_and_adults(self):
        public = self.venues(400)
        index = OPTIMIZED.PublicVenueIndex(public)
        for i in range(800):
            p = {'personId': f'b4:site_nw_001:{i}', 'birthYearAC': self.rng.choice([230, 260, 284, 286, 293, 296, 301])}
            home = (self.rng.randrange(-2100, 2100), self.rng.randrange(-2100, 2100))
            self.assertEqual(ORIGINAL.leisure_place(p, home, public), index.choose(p, home))

    def test_empty_and_boundary_venues_match_original(self):
        public = self.venues(20)
        for home in ((0, 0), (-128, -128), (-1, -1), (128, 128), (120, 120), (4096, 4096)):
            for i in range(85):
                person = {'personId': f'b4:test:{i}', 'birthYearAC': 298 if i % 2 else 270}
                self.assertEqual(ORIGINAL.leisure_place(person, home, public), OPTIMIZED.leisure_place(person, home, public))
        p = {'personId': 'b4:test:1', 'birthYearAC': 260}
        self.assertIsNone(OPTIMIZED.leisure_place(p, (0, 0), []))

    def fixture(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        repo = Path(self.temp.name)
        generator = repo / PATCHER.REL
        generator.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(HERE / 'original_b4_2b_mobility_index.py', generator)
        dst = repo / PATCHER.PAYLOAD
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(HERE / 'b4_2b_mobility_index.py', dst)
        return repo

    def test_pending_is_archived_not_lost(self):
        repo = self.fixture()
        pending = PATCHER.pending_path(repo)
        pending.mkdir(parents=True)
        child = pending / 'site_nw_001' / 'p00.json.gz'
        child.parent.mkdir()
        child.write_bytes(b'incomplete and harmless')
        PATCHER.apply(repo)
        self.assertEqual(PATCHER.status(repo)['generator'], 'ALREADY_PATCHED')
        self.assertFalse(pending.exists())
        backups = list((repo / PATCHER.SNAP).glob('*/incomplete_previous_index/site_nw_001/p00.json.gz'))
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0].read_bytes(), b'incomplete and harmless')
        self.assertEqual(PATCHER.sha(repo / PATCHER.REL), PATCHER.PATCH_HASH)

    def test_reject_unrecognized_pending_data_without_live_changes(self):
        repo = self.fixture()
        pending = PATCHER.pending_path(repo)
        pending.mkdir(parents=True)
        (pending / 'personal_world_save').write_text('user data')
        with self.assertRaises(ValueError):
            PATCHER.apply(repo)
        self.assertEqual(PATCHER.sha(repo / PATCHER.REL), PATCHER.BASE_HASH)
        self.assertEqual((pending / 'personal_world_save').read_text(), 'user data')

    def test_unknown_live_edit_is_not_overwritten(self):
        repo = self.fixture()
        (repo / PATCHER.REL).write_bytes(b'custom local changes')
        with self.assertRaises(ValueError):
            PATCHER.apply(repo)
        self.assertEqual((repo / PATCHER.REL).read_bytes(), b'custom local changes')

    def test_hotfix_can_be_rolled_back_with_original_pending(self):
        repo = self.fixture()
        pending = PATCHER.pending_path(repo)
        (pending / 'site_nw_001').mkdir(parents=True)
        (pending / 'site_nw_001' / 'p15.json.gz').write_bytes(b'checkpoint data')
        PATCHER.apply(repo)
        PATCHER.rollback(repo, 'latest')
        self.assertEqual(PATCHER.sha(repo / PATCHER.REL), PATCHER.BASE_HASH)
        self.assertEqual((pending / 'site_nw_001' / 'p15.json.gz').read_bytes(), b'checkpoint data')

    def test_repeat_apply_does_not_make_new_backup(self):
        repo = self.fixture()
        PATCHER.apply(repo)
        before = list((repo / PATCHER.SNAP).iterdir())
        PATCHER.apply(repo)
        self.assertEqual(before, list((repo / PATCHER.SNAP).iterdir()))

if __name__ == '__main__':
    unittest.main()
