"""C4.17/B4.2C meaningful source, geometry, and transaction contracts."""
from __future__ import annotations
import importlib.util,json,shutil,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[3]
def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod
I=load(ROOT/'tools/geodata/c4_17_guarded_install.py','installer417')
S=load(ROOT/'C4_17_STAGE/updated/tools/geodata/c4_10_shared_layout.py','geometry417')
T={k:{'id':k,'c1Width':w,'c1Depth':d,'requiresShore':False,'requiresMountainResource':False,'requiresPolicy':False}
   for k,w,d in [('HOUSE_SMALL',11,12),('HOUSE_MEDIUM',13,15),('HOUSE_URBAN',17,20),('SMITHY',13,11)]}
def site(tier='MAJOR',homes=440):
 return {'siteId':'site_sw_046','name':'Test Town','tier':tier,'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
 'mapAnchor':{'x':20000,'z':50000},'populationTarget':5000,'workingPopulationTarget':100,
 'householdsTarget':homes,'residentialBuildingsTarget':homes,'residentialUnitsPerBuilding':1,
 'workplaceBuildingsTarget':25,'occupationTargetsNonzero':{'Carpenter':100},
 'buildingDemandByKind':[{'kind':'SMITHY','requestedBuildings':25,'workersTarget':100,'slotsPerBuilding':4}],
 'mainComplex':{'reservedKeepWorkerSlots':0}}
class C417Tests(unittest.TestCase):
 def test_dense_plots_and_all_housing_classes_every_tier(self):
  self.assertEqual(S.PITCH,25)
  for tier in ('SMALL','MEDIUM','MAJOR'):
   lots=[];info=S.plan(site(tier,900),T,lots.append)
   homes=[x for x in lots if x['kind']=='HOUSE']
   self.assertEqual(len(homes),900)
   self.assertEqual(set(x['template'] for x in homes),{'HOUSE_SMALL','HOUSE_MEDIUM','HOUSE_URBAN'})
   self.assertEqual(info['linkedHouseholdReservations'],900)
   self.assertEqual(sum(x['reservedWorkerSlots'] for x in lots),100)
 def test_site_layout_deterministic(self):
  for tier in ('SMALL','MEDIUM','MAJOR'):
   a=[];b=[];S.plan(site(tier,170),T,a.append);S.plan(site(tier,170),T,b.append)
   self.assertEqual(a,b)
 def test_cell_and_physical_footprints_do_not_overlap(self):
  lots=[];S.plan(site('MAJOR',1250),T,lots.append)
  used=set();boxes=[]
  for lot in lots:
   grid=lot['grid']
   if not grid:continue
   gx,gz,sx,sz=grid
   cells={(gx+dx,gz+dz) for dx in range(sx) for dz in range(sz)}
   self.assertFalse(used&cells);used|=cells
   x,z,w,d=(lot[k] for k in ('x','z','width','depth'))
   # Physical building must be fully inside the selected grid-cell envelope.
   self.assertLessEqual(w+4,sx*S.PITCH)
   self.assertLessEqual(d+4,sz*S.PITCH)
 def test_winterfell_still_protected(self):
  x=site();x['siteId']='site_nw_029';built=[]
  self.assertIn('PROTECTED',S.plan(x,T,built.append)['status'])
  self.assertEqual(built,[])
 def test_stable_lot_identifiers_across_new_tier_mix(self):
  for tier in ('SMALL','MEDIUM','MAJOR'):
   lots=[];S.plan(site(tier,300),T,lots.append)
   homes=[x['lotId'] for x in lots if x['kind']=='HOUSE']
   self.assertEqual(homes,[f'site_sw_046:home:{n:07d}' for n in range(1,301)])
 def test_no_physical_despawn_recenter_or_huge_speed(self):
  f=(ROOT/'C4_17_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java').read_text()
  m=(ROOT/'C4_17_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42PhysicalRoutine.java').read_text()
  self.assertNotIn('HOME_WANDER_LIMIT',f)
  self.assertNotIn('v.snapTo(back.',f)
  self.assertIn('virtualArrival(',f)
  self.assertIn('ROUTE_STILL_IN_PROGRESS',m)
  self.assertIn('RETARGET_TICKS = 160',m)
  self.assertIn('WALK_SPEED = 0.48',m)
  self.assertIn('isConversationActor(',m)
 def test_gate_and_work_and_leisure_targets(self):
  j=(ROOT/'C4_17_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java').read_text()
  m=(ROOT/'C4_17_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42PhysicalRoutine.java').read_text()
  self.assertIn('leisurePlace(',j);self.assertIn('publicPlaces(',j)
  self.assertIn('KnownWorldB42PhysicalRoutine.steer',j)
  self.assertIn('portalWaypoint(',m);self.assertIn('KnownWorldC411GlobalWorldgen.initialTerrainHeight',m)
  self.assertIn('C4.10',m)
 def test_staged_baselines_known_source_shapes(self):
  for rel in I.FILES:
   source=(ROOT/'C4_17_STAGE/baseline'/rel).read_text()
   updated=(ROOT/'C4_17_STAGE/updated'/rel).read_text()
   self.assertNotEqual(source,updated)
   self.assertTrue(len(updated)>len(source)//2)
 def fixture(self,root):
  shutil.copytree(ROOT/'C4_17_STAGE',root/'C4_17_STAGE')
  for rel in I.FILES:
   p=root/rel;p.parent.mkdir(parents=True,exist_ok=True)
   shutil.copy2(root/'C4_17_STAGE/baseline'/rel,p)
  pop=root/I.POP;pop.parent.mkdir(parents=True,exist_ok=True)
  pop.write_text(json.dumps({'peopleTotal':3760684,'householdsTotal':799343,
    'modeledWorkersTotal':1880341,'siteCount':248}),encoding='utf-8')
  for rel in I.REQUIRED_GENERATORS:
   f=root/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text('# existing generator')
  for rel in I.RESOURCES:
   f=root/rel
   if f.suffix=='.json':f.parent.mkdir(parents=True,exist_ok=True);f.write_text('before',encoding='utf-8')
   else:f.mkdir(parents=True,exist_ok=True);(f/'old_resource.txt').write_text('before')
 def test_guard_refuses_unrecognized_java_edits(self):
  with tempfile.TemporaryDirectory() as tmp:
   r=Path(tmp);self.fixture(r)
   target=r/I.FILES[2];target.write_text(target.read_text()+'\n// manual user addition\n')
   statuses=I.check(r)
   self.assertEqual(statuses[I.FILES[2]],'UNKNOWN_LOCAL_EDITS')
 def test_snapshot_restores_sources_resources_and_absent_new_file(self):
  with tempfile.TemporaryDirectory() as tmp:
   r=Path(tmp);self.fixture(r)
   before=I.check(r);self.assertTrue(all(v in ('NEW','READY') for v in before.values()))
   snap=I.snapshot(r)
   for rel in I.FILES+[I.NEW]:
    dest=r/rel;dest.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(r/I.STAGE/'updated'/rel,dest)
   for rel in I.RESOURCES:
    p=r/rel
    if p.is_dir():shutil.rmtree(p);p.mkdir();(p/'new.txt').write_text('after')
    else:p.write_text('after')
   I.rollback(r,snap.name)
   self.assertFalse((r/I.NEW).exists())
   self.assertTrue(all(v in ('NEW','READY') for v in I.check(r).values()))
   for rel in I.RESOURCES:
    p=r/rel
    self.assertTrue((p/'old_resource.txt').exists() if p.is_dir() else p.read_text()=='before')
 def test_install_auto_restore_on_failure(self):
  with tempfile.TemporaryDirectory() as tmp:
   r=Path(tmp);self.fixture(r)
   def fail(repo,args):
    if 'tools/geodata/c4_11_global_rollout.py' in ' '.join(args):
     raise RuntimeError('simulated resource compiler failure')
   with patch.object(I,'run',side_effect=fail):
    with self.assertRaises(RuntimeError):I.install(r)
   self.assertTrue(all(v in ('NEW','READY') for v in I.check(r).values()))
 def test_global_runtime_resource_contract_remains_b40_unchanged(self):
  text=(ROOT/'tools/geodata/c4_17_guarded_install.py').read_text()
  self.assertIn('world_population_b4_v1/manifest.json',text)
  self.assertNotIn('b4_0_world_population.py',text)
  self.assertIn('cmd',text)
if __name__=='__main__':unittest.main()
