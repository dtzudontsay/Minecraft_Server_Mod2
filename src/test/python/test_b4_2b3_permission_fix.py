import hashlib,importlib.util,shutil,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
SCRIPT=ROOT/'tools/geodata/b4_2b3_permission_fix.py'
spec=importlib.util.spec_from_file_location('b42b3permfix',SCRIPT)
F=importlib.util.module_from_spec(spec);spec.loader.exec_module(F)

class PermissionFix(unittest.TestCase):
    def test_stage_matches_old_or_new_only(self):
        state=F.check(ROOT)
        self.assertIn(state['staged_permission'],('READY_TO_PATCH','ALREADY_PATCHED'))
        self.assertEqual(state['operator_security'],'LEVEL_OWNERS')
    def test_old_unsafe_check_is_replaced_by_exact_owner_permission(self):
        p=ROOT/F.REL;b=p.read_bytes()
        if F.digest(b)==F.OLD_SHA:
            b=b.replace(F.OLD,F.NEW)
        self.assertEqual(F.digest(b),F.NEW_SHA)
        self.assertEqual(b.count(F.NEW),1)
        self.assertNotIn(F.OLD,b)
        self.assertIn(b'Commands.hasPermission(Commands.LEVEL_OWNERS)',b)
    def test_install_idempotent_and_rollback(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);target=root/F.REL;target.parent.mkdir(parents=True)
            current=(ROOT/F.REL).read_bytes()
            if F.digest(current)==F.NEW_SHA: current=current.replace(F.NEW,F.OLD)
            self.assertEqual(F.digest(current),F.OLD_SHA)
            target.write_bytes(current)
            self.assertEqual(F.check(root)['staged_permission'],'READY_TO_PATCH')
            F.apply(root);self.assertEqual(F.check(root)['staged_permission'],'ALREADY_PATCHED')
            F.apply(root);self.assertEqual(F.check(root)['staged_permission'],'ALREADY_PATCHED')
            F.rollback(root,'latest');self.assertEqual(F.check(root)['staged_permission'],'READY_TO_PATCH')
    def test_unknown_and_partial_install_refuse_changes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);target=root/F.REL;target.parent.mkdir(parents=True)
            target.write_bytes((ROOT/F.REL).read_bytes()+b'UNKNOWN EDIT')
            with self.assertRaisesRegex(ValueError,'Unknown staged'):F.apply(root)
            self.assertFalse((root/F.BACK).exists())
            target.write_bytes((ROOT/F.REL).read_bytes())
            live=root/F.LIVE;live.parent.mkdir(parents=True);live.write_bytes(b'anything')
            with self.assertRaisesRegex(ValueError,'already exists'):F.apply(root)
            self.assertFalse((root/F.BACK).exists())

if __name__=='__main__':unittest.main()
