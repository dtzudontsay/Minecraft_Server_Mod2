"""Safety regression for uploaded-user-specific C4.16 stage patch."""
import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

def load(path, name):
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

PATCH=load(ROOT/'tools/geodata/c4_16_resolve_local_compat.py','c416_local_compat')
INSTALL=load(ROOT/'tools/geodata/c4_16_stage_apply.py','c416_original_installer')

class TestCompatibility(unittest.TestCase):
    def setUp(self):
        self.context=tempfile.TemporaryDirectory()
        self.repo=Path(self.context.name)
        shutil.copytree(ROOT/'C4_16_STAGE',self.repo/'C4_16_STAGE')
        shutil.copytree(ROOT/'C4_16_COMPAT_PAYLOAD',self.repo/'C4_16_COMPAT_PAYLOAD')
        path=self.repo/'tools/geodata/c4_16_stage_apply.py'
        path.parent.mkdir(parents=True)
        shutil.copy2(ROOT/'tools/geodata/c4_16_stage_apply.py',path)
        for rel in PATCH.RELS:
            source=self.repo/rel
            source.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(ROOT/'C4_16_COMPAT_PAYLOAD/baseline'/rel,source)
        for rel in INSTALL.FILES[:2]:
            source=self.repo/rel;source.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(ROOT/'C4_16_STAGE/baseline'/rel,source)
        scenarios=self.repo/INSTALL.RSL
        (scenarios/'world_population_b4_v1').mkdir(parents=True)
        (scenarios/'world_population_b4_v1/manifest.json').write_text(json.dumps({'peopleTotal':3760684,'householdsTotal':799343,'modeledWorkersTotal':1880341,'siteCount':248}))
        (scenarios/'universal_construction_c4_11_global_v1.json').write_text('{}')
        (scenarios/'world_projection_b4_1a1_v1').mkdir()
        (scenarios/'world_projection_b4_1a1_v1/manifest.json').write_text('{}')
        (scenarios/'world_daily_b4_2_v1').mkdir()
        (scenarios/'world_daily_b4_2_v1/manifest.json').write_text('{}')

    def tearDown(self):self.context.cleanup()

    def test_exact_stage_repairs_enable_original_installer(self):
        before=INSTALL.status(self.repo)
        # The fixture copies the user's current stage. That stage may be the
        # untouched original (two unknown local files), or may ALREADY be
        # repaired (both ready). Both are valid, and a second run must remain
        # safe and idempotent. Unexpected/mixed states still fail this test.
        unknown = [name for name, state in before.items()
                   if state == 'UNKNOWN_LOCAL_EDITS']
        self.assertIn(len(unknown), (0, 2))
        self.assertEqual(len(unknown), 0 if all(
            state == 'READY' for state in before.values()) else 2)
        self.assertTrue(all(state in ('READY', 'UNKNOWN_LOCAL_EDITS')
                            for state in before.values()))
        self.assertEqual(PATCH.install(self.repo), bool(unknown))
        self.assertTrue(all(s == 'READY' for s in INSTALL.status(self.repo).values()))
        backups = self.repo / PATCH.SNAP
        self.assertEqual(len(list(backups.iterdir())) if backups.exists() else 0,
                         1 if unknown else 0)
        self.assertFalse(PATCH.install(self.repo))
        INSTALL.apply(self.repo)
        self.assertTrue(all(s == 'ALREADY_INSTALLED'
                            for s in INSTALL.status(self.repo).values()))
        self.assertFalse(PATCH.install(self.repo))

    def test_reject_changed_local_before_editing_stage(self):
        source=self.repo/PATCH.RELS[0]
        source.write_bytes(source.read_bytes()+b'//untracked change\n')
        staged=self.repo/PATCH.STAGE/'baseline'/PATCH.RELS[1]
        original=staged.read_bytes()
        with self.assertRaises(ValueError):PATCH.install(self.repo)
        self.assertEqual(staged.read_bytes(),original)

    def test_reject_changed_staged_file(self):
        staged=self.repo/PATCH.STAGE/'updated'/PATCH.RELS[1]
        staged.write_bytes(staged.read_bytes()+b'//unknown stage change\n')
        with self.assertRaises(ValueError):PATCH.install(self.repo)

    def test_read_only_check_never_changes_staged_files(self):
        before=(self.repo/PATCH.STAGE/'baseline'/PATCH.RELS[0]).read_bytes()
        self.assertFalse(PATCH.install(self.repo,check=True))
        self.assertEqual(before,(self.repo/PATCH.STAGE/'baseline'/PATCH.RELS[0]).read_bytes())

if __name__=='__main__':unittest.main()
