from __future__ import annotations
import json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/geodata'))
import b4_1a1_install as mod

class Installer(unittest.TestCase):
    def setup_project(self,root):
        def put(path,text):
            f=root/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
        put(mod.MAIN,'class X { KnownWorldB41Activation.register(); }')
        put(mod.GLOBAL,'public int[] b41ApprovedPlacementForInspection(String s){return null;}')
        put(mod.CLIENT,'value("id").startsWith("b4:")')
        put(mod.ACTOR,'private static final int MAX_ACTORS = 128; world_projection_b4_1a1_v1')
        put(mod.SOURCE_B4,json.dumps({'peopleTotal':3760684,'householdsTotal':799343,
            'modeledWorkersTotal':1880341,'siteCount':248}))
        put(mod.SOURCE_C4,json.dumps({'siteCount':248,'globalEnabled':True}))
        put(mod.SOURCE_B2,'{}')
    def test_good_local_install(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.setup_project(root)
            self.assertEqual(mod.audit(root)['physicalBudget'],128)
    def test_missing_hook_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.setup_project(root)
            (root/mod.MAIN).write_text('class X {}')
            with self.assertRaisesRegex(ValueError,'onInitialize'):mod.audit(root)
    def test_old_runtime_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.setup_project(root)
            (root/mod.ACTOR).write_text('B4.1A v1')
            with self.assertRaisesRegex(ValueError,'Wrong worldwide'):mod.audit(root)
if __name__=='__main__':unittest.main()
