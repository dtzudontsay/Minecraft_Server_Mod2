import importlib.util, json, pathlib, tempfile, unittest
ROOT=pathlib.Path(__file__).resolve().parents[3]
def load(name,rel):
    spec=importlib.util.spec_from_file_location(name,ROOT/rel)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
M=load('work','tools/geodata/b4_2_workplace_index.py')
I=load('install','tools/geodata/b4_2_install.py')
class Integration(unittest.TestCase):
    def test_worldwide_manifest_and_exact_totals(self):
        with tempfile.TemporaryDirectory() as d:
            root=pathlib.Path(d);sc=root/M.SC
            ids=['site_ne_001','site_nw_missing_twins_01']+[f'site_sw_{n:03d}' for n in range(246)]
            assert len(set(ids))==248
            population={'peopleTotal':3760684,'modeledWorkersTotal':1880341,'householdsTotal':799343,'siteCount':248,'sites':[]}
            construction={'siteCount':248,'globalEnabled':True,'sites':[]};projection={'sites':[]}
            for i,sid in enumerate(ids):
                population['sites'].append({'siteId':sid,'personCount':3760684 if i==0 else 0,
                                            'modeledWorkerCount':1880341 if i==0 else 0})
                construction['sites'].append({'siteId':sid,'tiles':['0_0'] if i==0 else [],'status':'GLOBAL_TERRAIN_SURVEY_PENDING'})
                projection['sites'].append({'siteId':sid,'status':'C4_11_HOME_CANDIDATES' if i==0 else 'DEFERRED_EXISTING_OR_NONBUILDABLE'})
            samples={M.POP:population,M.C4:construction,M.PROJECTION:projection,M.COMMUTES:{'version':1,'employmentTotalsPreserved':True}}
            for rel,raw in samples.items():
                p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(raw))
            lotdir=root/M.TILES/ids[0];lotdir.mkdir(parents=True)
            (lotdir/'0_0.json').write_text(json.dumps({'siteId':ids[0],'lots':[{'id':ids[0]+':work:SMITHY:000001',
              'kind':'SMITHY','permission':'C3_MODULE_AVAILABLE','x':12,'z':13,'w':11,'d':11,'s':6}]}))
            summary=M.build(root)
            self.assertEqual(summary['siteCount'],248)
            self.assertEqual(summary['peopleTotal'],3760684)
            self.assertEqual(summary['workersTotal'],1880341)
            self.assertEqual(summary['workplacesIndexed'],1)
            self.assertEqual(summary['sites'][0]['status'],'READ_ONLY_PLANNED_WORKSITES')
            self.assertIsNone(summary['sites'][1]['file'])
            self.assertTrue((root/M.OUT/(ids[0]+'.json.gz')).is_file())
            self.assertFalse((root/M.OUT/(ids[1]+'.json.gz')).exists())
            self.assertTrue((root/M.OUT/'manifest.json').is_file())
    def test_marker_idempotency_is_guarded(self):
        self.assertRaises(ValueError,I.patch_activation,'// B4_1B_WORLDWIDE_DIALOGUE_ROUTING')
        self.assertRaises(ValueError,I.patch_dialogue,'public class Dummy {}')
        self.assertRaises(ValueError,I.patch_ui,'public class Dummy {}')
    def test_atomic_checkpoint_required(self):
        txt=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java').read_text()
        self.assertIn('ATOMIC_MOVE',txt)
        self.assertIn('current<lastDay',txt)
        self.assertIn('SCHEDULE_ONLY_NO_AUTO_ECONOMY_OR_OBSERVED_MEMORIES',txt)
        self.assertIn('WinterfellConversationPackets.isConversationActor',txt)
if __name__=='__main__':unittest.main()
