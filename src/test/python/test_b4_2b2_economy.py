import gzip,importlib.util,json,shutil,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
def module(name,rel):
 s=importlib.util.spec_from_file_location(name,ROOT/rel);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
M=module('b42b2','tools/geodata/b4_2b2_household_ownership.py')
I=module('b42b2_installer','tools/geodata/b4_2b2_guarded_install.py')
SC=M.SC

def dump(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj),encoding='utf8')
def people(path,rows):
 path.parent.mkdir(parents=True,exist_ok=True)
 with gzip.open(path,'wt',encoding='utf8') as f:
  for r in rows:f.write(json.dumps(r)+'\n')
def setup_fixture(root):
 sid='site_ne_001';ids=[sid]+['site_zz_%03d'%i for i in range(247)]
 popsites=[];projsites=[];worksites=[]
 for id in ids:
  selected=id==sid
  popsites.append({'siteId':id,'personCount':3 if selected else 0,'householdCount':2 if selected else 0,'modeledWorkerCount':2 if selected else 0})
  projsites.append({'siteId':id,'status':'C4_11_HOME_CANDIDATES' if selected else 'DEFERRED','tiles':{'0_0':3} if selected else {},'residentsIndexed':3 if selected else 0})
  worksites.append({'siteId':id,'people':3 if selected else 0,'workers':2 if selected else 0})
 dump(root/M.B40/'manifest.json',{'schemaVersion':'knownworld.b4_0.identity_baseline.v1','peopleTotal':3760684,'householdsTotal':799343,'modeledWorkersTotal':1880341,'siteCount':248,'sites':popsites})
 dump(root/M.B41/'manifest.json',{'schemaVersion':'knownworld.b4_1a1.household_projection.v1','sourcePopulationCount':3760684,'sites':projsites})
 dump(root/M.B42/'manifest.json',{'schemaVersion':'knownworld.b4_2.daily_worksites.v1','peopleTotal':3760684,'workersTotal':1880341,'sites':worksites})
 people(root/M.B41/sid/'0_0.jsonl.gz',[
  {'personId':'b4:'+sid+':00000001','siteId':sid,'householdId':'hh:1','homeLotId':'h1','householdRole':'HOUSEHOLD_HEAD','occupationId':'Master Smith','workplaceId':sid+':smithy:1'},
  {'personId':'b4:'+sid+':00000002','siteId':sid,'householdId':'hh:1','homeLotId':'h1','householdRole':'PARTNER_OR_CARER','occupationId':'Apprentice Smith','workplaceId':sid+':smithy:1'},
  {'personId':'b4:'+sid+':00000003','siteId':sid,'householdId':'hh:2','homeLotId':'h2','householdRole':'HOUSEHOLD_HEAD','occupationId':None,'workplaceId':None}])
 return sid

class WorldwideEconomicIndexTests(unittest.TestCase):
 def test_fixture_all_248_sites_and_workgroups(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);sid=setup_fixture(root)
   r=M.build(root,selection={sid})
   self.assertEqual((r['siteCount'],r['people'],r['households'],r['workers'],r['workplaceGroups']),(248,3,2,2,1))
   m=json.loads((root/M.OUT/'manifest.json').read_text())
   self.assertEqual(m['actualCoinCredited'],0);self.assertEqual(m['actualGrainCreditedKg'],0)
   parts=list((root/M.OUT/sid).glob('*.json.gz'))
   hh={};work={}
   for f in parts:
    with gzip.open(f,'rt') as g:j=json.load(g);hh.update(j['households']);work.update(j['workplaces'])
   self.assertEqual(hh['hh:1']['members'],2)
   self.assertTrue(hh['hh:1']['standing'] in {'MODEST','PRECARIOUS','COMFORTABLE','PROSPEROUS'})
   self.assertEqual(work[sid+':smithy:1']['assignedWorkers'],2)
   self.assertIn(work[sid+':smithy:1']['coordinatorPersonId'],['b4:'+sid+':00000001','b4:'+sid+':00000002'])
   self.assertIsNone(work[sid+':smithy:1']['legalOwnerPersonId'])
 def test_deterministic_and_fails_on_unknown_source(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);sid=setup_fixture(root)
   M.build(root,selection={sid})
   first={(x.relative_to(root/M.OUT)).as_posix():x.read_bytes() for x in (root/M.OUT).rglob('*') if x.is_file()}
   with self.assertRaises(FileExistsError):M.build(root,selection={sid})
   shutil.rmtree(root/M.OUT);M.build(root,selection={sid})
   second={(x.relative_to(root/M.OUT)).as_posix():x.read_bytes() for x in (root/M.OUT).rglob('*') if x.is_file()}
   self.assertEqual(first,second)
   x=root/M.B40/'manifest.json';doc=json.loads(x.read_text());doc['peopleTotal']=1;dump(x,doc)
   shutil.rmtree(root/M.OUT)
   with self.assertRaisesRegex(ValueError,'B4.0 input census mismatch'):M.build(root,selection={sid})
 def test_household_inconsistent_home_fails_closed(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);sid=setup_fixture(root)
   file=root/M.B41/sid/'0_0.jsonl.gz'
   with gzip.open(file,'rt') as f:docs=[json.loads(line) for line in f]
   docs[1]['homeLotId']='wrong';people(file,docs)
   with self.assertRaisesRegex(ValueError,'Household split'):M.build(root,selection={sid})
   self.assertFalse((root/M.OUT).exists())
 def test_skill_and_currency_guardrails_are_unchanged(self):
  c=(ROOT/'B4_2B2_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42B2EconomicStanding.java').read_text()
  self.assertIn('actualCoinCredited',c)
  self.assertIn('UNRESOLVED_NO_INVENTED_HOLDER', (ROOT/'tools/geodata/b4_2b2_household_ownership.py').read_text())
  self.assertNotIn('EconomyRuntime.getNullable().inventories().produce',c)
 def test_snapshot_restore_handles_new_resources(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d)
   for rel in I.REQUIRE: (root/rel).parent.mkdir(parents=True,exist_ok=True);(root/rel).write_text('{}')
   for rel in I.EXISTING:
    b=ROOT/I.STAGE/'baseline'/rel;u=ROOT/I.STAGE/'updated'/rel
    for path,data in [(root/rel,b),(root/I.STAGE/'baseline'/rel,b),(root/I.STAGE/'updated'/rel,u)]:
     path.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(data,path)
   for rel in I.NEW:
    path=root/I.STAGE/'updated'/rel;path.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/I.STAGE/'updated'/rel,path)
   self.assertSetEqual(set(I.check(root).values()),{'READY','NEW'})
   snap=I.backup(root)
   for rel in I.EXISTING+I.NEW:
    path=root/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text('altered')
   dump(root/I.RES[0]/'manifest.json',{'schemaVersion':'knownworld.b4_2b2.worldwide_household_standing.v1'})
   I.rollback(root,snap.name)
   self.assertSetEqual(set(I.check(root).values()),{'READY','NEW'})
 def test_generator_failure_restores_source_automatically(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d)
   for rel in I.REQUIRE:
    path=root/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text('{}')
   for rel in I.EXISTING:
    for dst,src in [(root/rel,ROOT/I.STAGE/'baseline'/rel),
      (root/I.STAGE/'baseline'/rel,ROOT/I.STAGE/'baseline'/rel),
      (root/I.STAGE/'updated'/rel,ROOT/I.STAGE/'updated'/rel)]:
     dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
   for rel in I.NEW:
    dst=root/I.STAGE/'updated'/rel;dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(ROOT/I.STAGE/'updated'/rel,dst)
   gen=root/'tools/geodata/b4_2b2_household_ownership.py'
   gen.parent.mkdir(parents=True,exist_ok=True)
   gen.write_text('import sys\nsys.exit(7)\n')
   with self.assertRaises(Exception):I.install(root,gradle=False,tests=False)
   self.assertSetEqual(set(I.check(root).values()),{'READY','NEW'})
   self.assertFalse((root/I.RES[0]).exists())

 def test_unknown_source_edits_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d)
   for rel in I.REQUIRE:(root/rel).parent.mkdir(parents=True,exist_ok=True);(root/rel).write_text('{}')
   rel=I.EXISTING[0]
   for dest,ref in [(root/rel,ROOT/I.STAGE/'baseline'/rel),(root/I.STAGE/'baseline'/rel,ROOT/I.STAGE/'baseline'/rel),(root/I.STAGE/'updated'/rel,ROOT/I.STAGE/'updated'/rel)]:
    dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ref,dest)
   for rel in I.NEW:
    dst=root/I.STAGE/'updated'/rel;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/I.STAGE/'updated'/rel,dst)
   self.assertEqual(I.check(root)[str(I.EXISTING[0])],'READY')
   (root/I.EXISTING[0]).write_text('unrelated local work')
   self.assertEqual(I.check(root)[str(I.EXISTING[0])],'UNKNOWN_LOCAL_EDITS')
   with self.assertRaisesRegex(ValueError,'Unknown local modifications'):I.install(root,gradle=False,tests=False)

if __name__=='__main__':unittest.main()
