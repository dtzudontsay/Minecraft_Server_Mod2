"""B4.2B source preservation, geographic index and guarded rollback tests."""
import gzip,importlib.util,json,shutil,tempfile,unittest
from pathlib import Path

ME=Path(__file__).resolve()
ROOT=ME.parents[3]

def module(path,name):
    spec=importlib.util.spec_from_file_location(name,str(path));obj=importlib.util.module_from_spec(spec);spec.loader.exec_module(obj);return obj

class TestB42B(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.a=module(ROOT/'tools/geodata/b4_2b_guarded_install.py','b42b_guard')
        cls.mob=module(ROOT/'tools/geodata/b4_2b_mobility_index.py','b42b_mobility')
        cls.eco=module(ROOT/'tools/geodata/b4_2b_economy_readiness.py','b42b_economy')
    def setUp(self):
        self.t=tempfile.TemporaryDirectory();self.repo=Path(self.t.name)
        a=self.a
        for src in a.EXISTING:
            live=self.repo/src;live.parent.mkdir(parents=True,exist_ok=True)
            seed=ROOT/a.STAGE/'baseline'/src
            shutil.copy2(seed,live)
        for script in ('b4_2b_mobility_index.py','b4_2b_economy_readiness.py'):
            dst=self.repo/'tools/geodata'/script
            dst.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(ROOT/'tools/geodata'/script,dst)
        for src in a.EXISTING+a.NEW:
            for kind in (('baseline',) if src in a.EXISTING else ()) + ('updated',):
                source=ROOT/a.STAGE/kind/src;dest=self.repo/a.STAGE/kind/src
                dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
        sc=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
        proj=sc/'world_projection_b4_1a1_v1';work=sc/'world_daily_b4_2_v1';c4=sc/'universal_construction_c4_11_global_v1.json'
        sites=[];worksites=[];geometry=[]
        for i in range(1,249):
            sid=f'site_nw_{i:03}'
            sites.append({'siteId':sid,'status':'C4_11_HOME_CANDIDATES' if i==1 else 'DEFERRED_EXISTING_OR_NONBUILDABLE',
                          'tiles':{'0_0':2} if i==1 else {}})
            worksites.append({'siteId':sid,'file':sid+'.json.gz' if i==1 else None,
                              'people':3760684 if i==1 else 0,'workers':1880341 if i==1 else 0})
            geometry.append({'siteId':sid,'originalX':0,'originalZ':0,'physicalTier':'SMALL'})
        self.put(proj/'manifest.json',{'schemaVersion':'knownworld.b4_1a1.household_projection.v1','sourcePopulationCount':3760684,'sourceHouseholdCount':799343,'physicalProjectionLimit':256,'indexedResidents':2,'sites':sites})
        self.put(work/'manifest.json',{'schemaVersion':'knownworld.b4_2.daily_worksites.v1','peopleTotal':3760684,'workersTotal':1880341,'siteCount':248,'sites':worksites})
        self.put(c4,{'siteCount':248,'sites':geometry})
        with gzip.open(self.repo/work/'site_nw_001.json.gz','wt',encoding='utf8') as f:
            json.dump({'siteId':'site_nw_001','workplaces':[{'id':'site_nw_001:work1','kind':'SMITHY','x':1020,'z':0,'w':8,'d':10}]},f)
        p0={'personId':'b4:site_nw_001:1','siteId':'site_nw_001','x':0,'z':0,'w':8,'d':8,
            'birthYearAC':260,'workplaceId':'site_nw_001:work1'}
        p1=dict(p0,personId='b4:site_nw_001:2',workplaceId=None)
        path=self.repo/proj/'site_nw_001/0_0.jsonl.gz';path.parent.mkdir(parents=True,exist_ok=True)
        with gzip.open(path,'wt',encoding='utf8') as f:
            for p in (p0,p1):f.write(json.dumps(p)+'\n')
    def tearDown(self):self.t.cleanup()
    def put(self,path,data):
        file=self.repo/path;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(json.dumps(data),encoding='utf8')
    def test_precheck_matches_exact_b43_sources(self):
        states=self.a.check(self.repo)
        self.assertEqual(list(states.values()).count('READY'),3)
        self.assertEqual(list(states.values()).count('NEW'),5)
    def test_remote_workplace_is_discoverable_without_home_nearby(self):
        summary=self.mob.build(self.repo)
        self.assertEqual(summary['residentsIndexed'],2)
        root=self.repo/self.mob.OUT/'site_nw_001'
        matching=[]
        for gz in root.glob('*.gz'):
            with gzip.open(gz,'rt',encoding='utf8') as f:
                data=json.load(f)
                matching.extend(data['cells'].get('4_0',[]))
        self.assertIn(['0_0','b4:site_nw_001:1'],matching)
        self.assertNotIn(['0_0','b4:site_nw_001:2'],matching)
    def test_gate_route_uses_intermediate_indexed_tile(self):
        route=self.mob.commute_points((0,0),(230,0),[0,0,106,0])
        self.assertTrue(route)
        self.assertEqual(route[-1],(230,0))
    def test_readiness_never_credits_grain(self):
        info=self.eco.build(self.repo)
        self.assertTrue(info['readOnly'])
        data=json.loads((self.repo/self.eco.OUT).read_text())
        self.assertEqual(data['initialGrainActuallyCreditedKg'],0)
        self.assertEqual(data['sites'][0]['indicativeGrainReserveKg'],3760684*90)
    def test_snapshot_rollback_new_files_and_resources(self):
        self.a.install(self.repo,run_gradle=False,run_tests=False)
        self.assertTrue(all(x=='ALREADY_INSTALLED' for x in self.a.check(self.repo).values()))
        self.a.rollback(self.repo,'latest')
        self.assertTrue(all(x in ('READY','NEW') for x in self.a.check(self.repo).values()))
        self.assertFalse((self.repo/self.a.MOB).exists())
    def test_generator_failure_rolls_back_sources(self):
        # The installer writes Java before regenerating data. A missing required
        # household tile must trigger an automatic restore of those Java files.
        broken=self.repo/self.mob.PROJ/'site_nw_001/0_0.jsonl.gz'
        broken.unlink()
        with self.assertRaises(Exception):
            self.a.install(self.repo,run_gradle=False,run_tests=False)
        self.assertEqual(self.a.check(self.repo)[self.a.EXISTING[0]],'READY')
        self.assertFalse((self.repo/self.a.MOB).exists())
    def test_mobility_index_does_not_mutate_projected_people(self):
        path=self.repo/self.mob.PROJ/'site_nw_001/0_0.jsonl.gz'
        before=path.read_bytes()
        self.mob.build(self.repo)
        self.assertEqual(path.read_bytes(),before)
    def test_unknown_edits_fails_before_backup(self):
        live=self.repo/self.a.EXISTING[0]
        live.write_text(live.read_text()+'\n// a local edit\n')
        with self.assertRaises(ValueError):self.a.install(self.repo,run_gradle=False,run_tests=False)
        self.assertFalse((self.repo/self.a.SNAP).exists())
    def test_immutable_b40_resources(self):
        self.a.install(self.repo,run_gradle=False,run_tests=False)
        self.assertFalse((self.repo/'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1').exists())
        self.assertEqual(self.a.check(self.repo)[self.a.EXISTING[0]],'ALREADY_INSTALLED')
    def test_single_authority_and_position_source_guards(self):
        java=(ROOT/self.a.STAGE/'updated'/self.a.EXISTING[0]).read_text()
        self.assertIn('KnownWorldB42BSpatialDiscovery.nearby',java)
        self.assertIn('KnownWorldB42DailyLife.virtualArrival',java)
        self.assertIn('if(d>PLAYER_RADIUS*PLAYER_RADIUS',java)
        handoff=(ROOT/self.a.STAGE/'updated'/self.a.EXISTING[2]).read_text()
        self.assertIn('return modeled;',handoff)
        events=(ROOT/self.a.STAGE/'updated'/self.a.NEW[1]).read_text()
        self.assertNotIn('.produce(',events)
        self.assertNotIn('.transfer(',events)

if __name__=='__main__':unittest.main()
