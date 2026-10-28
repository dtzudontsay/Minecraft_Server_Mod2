import hashlib,importlib.util,json,shutil,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
def imp(name,path):
 spec=importlib.util.spec_from_file_location(name,ROOT/path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
G=imp('b42b3gen','tools/geodata/b4_2b3_opening_reserves.py')
I=imp('b42b3install','tools/geodata/b4_2b3_guarded_install.py')

def dump(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x),encoding='utf8')
def fixture(root):
 ids=['site_ne_001']+['site_zz_%03d'%i for i in range(247)]
 p=[];e=[]
 for i,sid in enumerate(ids):
  big=(i==0)
  n=3760684 if big else 0;h=799343 if big else 0;w=1880341 if big else 0
  p.append(dict(siteId=sid,personCount=n,householdCount=h,modeledWorkerCount=w))
  e.append(dict(siteId=sid,people=n,households=h,workers=w,status='DERIVED_PROVISIONAL_NOT_ECONOMY_AUTHORITY'))
 dump(root/G.POP,dict(schemaVersion='knownworld.b4_0.identity_baseline.v1',siteCount=248,peopleTotal=3760684,householdsTotal=799343,modeledWorkersTotal=1880341,sites=p))
 dump(root/G.STANDING,dict(schemaVersion='knownworld.b4_2b2.worldwide_household_standing.v1',siteCount=248,populationTotal=3760684,householdsTotal=799343,workersTotal=1880341,authority='READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK',sites=e))
 return ids

def install_fixture(root):
 fixture(root)
 for rel in [I.STANDING]:
  for dst,source in [(root/rel,ROOT/I.STAGE/'baseline'/rel),
         (root/I.STAGE/'baseline'/rel,ROOT/I.STAGE/'baseline'/rel),
         (root/I.STAGE/'updated'/rel,ROOT/I.STAGE/'updated'/rel)]:
   dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dst)
 for rel in [I.NEW]:
  dest=root/I.STAGE/'updated'/rel;dest.parent.mkdir(parents=True,exist_ok=True)
  shutil.copy2(ROOT/I.STAGE/'updated'/rel,dest)
 patch=root/I.STAGE/'patches/EconomyRuntime_method.txt';patch.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(ROOT/I.STAGE/'patches/EconomyRuntime_method.txt',patch)
 eco=b'package x;\nclass EconomyRuntime {\n    private void saveEconomy() throws IOException {}\n}\n'
 eco_path=root/I.ECO;eco_path.parent.mkdir(parents=True,exist_ok=True);eco_path.write_bytes(eco)
 gen=root/"tools/geodata/b4_2b3_opening_reserves.py"
 gen.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(ROOT/"tools/geodata/b4_2b3_opening_reserves.py",gen)
 old=I.KNOWN_GIT_SHA;I.KNOWN_GIT_SHA=I.gitsha(eco)
 return old

class Economics(unittest.TestCase):
 def test_all_248_and_global_totals(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);s=G.build(root);self.assertEqual(s['people'],3760684)
   m=G.load(root/G.OUT/'manifest.json');self.assertEqual(len(m['sitePlans']),248)
   self.assertEqual(sum(x['population'] for x in m['sitePlans']),3760684)
   self.assertEqual(m['actualGrainCreditedKg'],0);self.assertEqual(m['actualCoinCreditedUnits'],0)
   self.assertFalse(m['autonomousConsumption']);self.assertFalse(m['wagesActivated'])
 def test_stocks_are_plans_not_household_property(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);G.build(root)
   m=G.load(root/G.OUT/'manifest.json');r=m['sitePlans'][0]
   self.assertEqual(r['grainKg'],3760684*75*90//100)
   self.assertIsNone(r['canonicalSettlementId']);self.assertFalse(r['individualHouseholdBalancesCreated'])
   self.assertEqual(r['stockPurpose'],'PUBLIC_GRANARY_AND_SETTLEMENT_TREASURY_ONLY')
 def test_no_overwrite_and_deterministic(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);G.build(root);first=(root/G.OUT/'manifest.json').read_bytes()
   with self.assertRaises(FileExistsError):G.build(root)
   shutil.rmtree(root/G.OUT);G.build(root);self.assertEqual(first,(root/G.OUT/'manifest.json').read_bytes())
 def test_refuses_population_mismatch(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);p=G.load(root/G.STANDING);p['sites'][0]['workers']=7;dump(root/G.STANDING,p)
   with self.assertRaisesRegex(ValueError,'count disagrees'):G.build(root)
   self.assertFalse((root/G.OUT).exists())
 def test_rollout_and_rollback(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);old=install_fixture(root)
   try:
    self.assertEqual(set(I.check(root).values()),{'READY','NEW'})
    I.install(root,run_gradle=False,run_tests=False)
    self.assertEqual(set(I.check(root).values()),{'ALREADY_INSTALLED'})
    I.rollback(root,'latest')
    self.assertEqual(set(I.check(root).values()),{'READY','NEW'})
   finally:I.KNOWN_GIT_SHA=old
 def test_unknown_changes_stop_and_automatic_restore(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);old=install_fixture(root)
   try:
    (root/I.ECO).write_text('modified without approval')
    self.assertEqual(I.check(root)[str(I.ECO)],'UNKNOWN_LOCAL_EDITS')
    with self.assertRaisesRegex(ValueError,'unknown local'):I.install(root,False,False)
    self.assertFalse((root/I.SNAP).exists())
    (root/I.ECO).write_bytes(b'package x;\nclass EconomyRuntime {\n    private void saveEconomy() throws IOException {}\n}\n')
    gen=root/'tools/geodata/b4_2b3_opening_reserves.py';gen.parent.mkdir(parents=True,exist_ok=True)
    gen.write_text('import sys\nsys.exit(7)\n')
    with self.assertRaises(Exception):I.install(root,False,False)
    self.assertEqual(set(I.check(root).values()),{'READY','NEW'})
   finally:I.KNOWN_GIT_SHA=old
 def test_receipt_and_snapshot_guard_is_not_an_autorun(self):
  j=(ROOT/I.STAGE/'updated'/I.NEW).read_text()
  self.assertIn('EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY',j)
  self.assertIn('StandardOpenOption.CREATE_NEW',j)
  self.assertIn('commitB42B3SnapshotNow()',j)
  self.assertIn('StandardCopyOption.ATOMIC_MOVE',j)
  self.assertNotIn('ServerTickEvents.END_SERVER_TICK',j)
if __name__=='__main__':unittest.main()
