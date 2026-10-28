import gzip,hashlib,importlib.util,json,shutil,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
def loadmod(name,p):
 s=importlib.util.spec_from_file_location(name,ROOT/p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
G=loadmod('b42b4gen','tools/geodata/b4_2b4_household_daily_plan.py')
I=loadmod('b42b4ins','tools/geodata/b4_2b4_guarded_install.py')
def save(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value),encoding='utf-8')
def fixture(root):
 ids=['site_ne_001','site_nw_029']+[f'site_zz_{i:03d}' for i in range(246)]
 sites=[];stock=[]
 for sid in ids:
  if sid=='site_ne_001':people,households,workers,status,parts=4,2,2,'DERIVED_PROVISIONAL_NOT_ECONOMY_AUTHORITY',[0]
  elif sid=='site_nw_029':people,households,workers,status,parts=3760680,799341,1880339,'WINTERFELL_V3_SEPARATELY_AUTHORED_NO_BOOTSTRAP',[]
  else:people,households,workers,status,parts=0,0,0,'EMPTY_NO_RESIDENTS',[]
  sites.append(dict(siteId=sid,people=people,households=households,workers=workers,status=status,parts=parts))
  stock.append(dict(siteId=sid,population=people,households=households,modeledWorkers=workers,grainKg=people*75*90//100,treasuryCoinUnits=people*12+workers*45))
 save(root/G.HOUSE/'manifest.json',dict(schemaVersion='knownworld.b4_2b2.worldwide_household_standing.v1',siteCount=248,populationTotal=3760684,householdsTotal=799343,workersTotal=1880341,authority='READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK',sites=sites))
 save(root/G.RESERVES,dict(schemaVersion='knownworld.b4_2b3.opening_public_reserves.v1',siteCount=248,peopleTotal=3760684,householdsTotal=799343,workersTotal=1880341,actualGrainCreditedKg=0,actualCoinCreditedUnits=0,sitePlans=stock))
 G.save_gz(root/G.HOUSE/'site_ne_001'/'p00.json.gz',{'households':{
  'hh:one':dict(members=3,workers=1,headPersonId='person1',prosperityIndex=350,standing='MODEST'),
  'hh:two':dict(members=1,workers=1,headPersonId='person4',prosperityIndex=680,standing='PROSPEROUS')}})
 return ids

def installer_fixture(root):
 fixture(root)
 for rel in (I.STANDING,I.DAILY,I.NATIVE):
  updated=root/I.STAGE/'updated'/rel;updated.parent.mkdir(parents=True,exist_ok=True)
  shutil.copy2(ROOT/I.STAGE/'updated'/rel,updated)
 orig=root/I.STAGE/'baseline'/I.STANDING;orig.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(ROOT/I.STAGE/'baseline'/I.STANDING,orig)
 live=root/I.STANDING;live.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(orig,live)
 ec=root/I.ECON;ec.parent.mkdir(parents=True,exist_ok=True);ec.write_bytes(I.COMMIT_ANCHOR+b' {}\n')
 p=root/I.B43;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(I.B43_PERMISSION+b'\n')
 g=root/'tools/geodata/b4_2b4_household_daily_plan.py';g.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/'tools/geodata/b4_2b4_household_daily_plan.py',g)

class B42B4Test(unittest.TestCase):
 def test_248_sites_full_census_and_deferred_winterfell(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);res=G.build(root)
   self.assertEqual(res['sites'],248);self.assertEqual(res['people'],3760684)
   m=G.load(root/G.OUT/'manifest.json')
   self.assertEqual(m['indexedHouseholds'],2)
   self.assertEqual(m['unresolvedHouseholds'],799341)
   self.assertFalse(m['dailyTransactionsActivated'])
   self.assertEqual(m['actualCoinDebitedUnits'],0)
 def test_no_double_count_resources_and_daily_demand(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);G.build(root);m=G.load(root/G.OUT/'manifest.json')
   site=next(x for x in m['sites'] if x['siteId']=='site_ne_001')
   self.assertEqual(site['dailyHouseholdDemandGrams'],3000)
   self.assertEqual(site['estimatedDailyGrossWagesCoinUnits'],16)
   self.assertEqual(site['householdGrainFromPublicKg'],43)
   self.assertEqual(site['householdGrainFromPublicKg']+site['publicGrainRemainingKg'],4*75*90//100)
   self.assertEqual(site['householdCoinFromPublicUnits']+site['publicCoinRemainingUnits'],4*12+2*45)
   self.assertEqual(site['householdCoinFromPublicUnits'],(4*12+2*45)*40//100)
 def test_household_shard_corresponds_to_hash(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);G.build(root)
   for hid in ('hh:one','hh:two'):
    n=int.from_bytes(hashlib.sha256(hid.encode()).digest()[:2],'big')%16
    v=G.read_gz(root/G.OUT/'site_ne_001'/f'p{n:02d}.json.gz')['households'][hid]
    self.assertIsNone(v['nativeHouseholdOwnerId']);self.assertFalse(v['authoritativeTransactions'])
    self.assertGreater(v['grainAllocationFromPublicKg'],0)
 def test_determinism_and_no_overwrite(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);G.build(root)
   original=(root/G.OUT/'manifest.json').read_bytes()
   with self.assertRaises(FileExistsError):G.build(root)
   shutil.rmtree(root/G.OUT);G.build(root)
   self.assertEqual(original,(root/G.OUT/'manifest.json').read_bytes())
 def test_invalid_source_fails_without_publication(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);fixture(root);m=G.load(root/G.RESERVES);m['sitePlans'][0]['households']=900;save(root/G.RESERVES,m)
   with self.assertRaisesRegex(ValueError,'census mismatch'):G.build(root)
   self.assertFalse((root/G.OUT).exists())
 def test_source_precheck_and_successful_rollback(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);installer_fixture(root)
   self.assertEqual(list(I.check(root).values()).count('READY'),1)
   I.install(root,False,False)
   self.assertTrue(all(v=='ALREADY_INSTALLED' for v in I.check(root).values()))
   I.rollback(root,'latest')
   self.assertEqual(set(I.check(root).values()),{'READY','NEW'})
 def test_unknown_local_file_safe_stop(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);installer_fixture(root)
   (root/I.STANDING).write_text('user-edited')
   self.assertEqual(I.check(root)[str(I.STANDING)],'UNKNOWN_LOCAL_EDITS')
   with self.assertRaises(ValueError):I.install(root,False,False)
   self.assertFalse((root/I.SNAPS).exists())
 def test_generator_failure_auto_rollback(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);installer_fixture(root)
   g=root/'tools/geodata/b4_2b4_household_daily_plan.py'
   g.write_text('raise RuntimeError("deliberate simulated generator failure")\n')
   with self.assertRaises(Exception):I.install(root,False,False)
   self.assertEqual(set(I.check(root).values()),{'READY','NEW'})
 def test_native_journal_guard_no_autonomous_tick(self):
  j=(ROOT/I.STAGE/'updated'/I.NATIVE).read_text()
  self.assertIn('manager.transfer(',j)
  self.assertIn('StandardOpenOption.CREATE_NEW',j)
  self.assertIn('commitB42B3SnapshotNow()',j)
  self.assertIn('EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY',j)
  self.assertNotIn('ServerTickEvents',j)
  self.assertNotIn('manager.produce(',j)
if __name__=='__main__':unittest.main()
