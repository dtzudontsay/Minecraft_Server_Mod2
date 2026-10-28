"""C4.16 + B4.2A: executable contracts, not assertions of runtime path success."""
from pathlib import Path
import importlib.util,unittest,json,hashlib,tempfile,shutil
ROOT=Path(__file__).resolve().parents[3]
LAYOUT_PATH=ROOT/'C4_16_STAGE/updated/tools/geodata/c4_10_shared_layout.py'
def mod(path):
 spec=importlib.util.spec_from_file_location('c416_layout_test',path)
 module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
S=mod(LAYOUT_PATH)
T={k:{'id':k,'c1Width':w,'c1Depth':d,'requiresShore':False,'requiresMountainResource':False,'requiresPolicy':False}
   for k,w,d in [('HOUSE_SMALL',11,12),('HOUSE_MEDIUM',13,15),('HOUSE_URBAN',17,20),('SMITHY',13,11),('KEEP',49,41)]}
def site(homes=450,tier='MAJOR'):
 return {'siteId':'site_sw_046','name':'Test Town','tier':tier,'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
 'mapAnchor':{'x':20000,'z':50000},'markerIds':['SW-046'],'populationTarget':5000,
 'workingPopulationTarget':100,'householdsTarget':homes,'residentialBuildingsTarget':homes,
 'residentialUnitsPerBuilding':1,'workplaceBuildingsTarget':25,
 'occupationTargetsNonzero':{'Carpenter':100},
 'buildingDemandByKind':[{'kind':'SMITHY','requestedBuildings':25,'workersTarget':100,'slotsPerBuilding':4}],
 'mainComplex':{'reservedKeepWorkerSlots':0}}
class Tests(unittest.TestCase):
 def test_preserves_every_home_and_job_slot(self):
  lots=[];greens=[]
  info=S.plan(site(),T,lots.append,greens.append)
  self.assertEqual(len(lots),475)
  self.assertEqual(sum(x['reservedWorkerSlots'] for x in lots),100)
  self.assertEqual(info['linkedHouseholdReservations'],450)
  self.assertEqual(len({x['lotId'] for x in lots}),475)
  self.assertTrue(greens)
  self.assertTrue(all((p['width'],p['depth'])==(19,19) for p in greens))
 def test_building_reservations_no_overlapping_grid_cells(self):
  lots=[];S.plan(site(),T,lots.append)
  cells=set()
  for lot in lots:
   g=lot['grid']
   if not g:continue
   gx,gz,sx,sz=g
   block={(gx+u,gz+v) for u in range(sx) for v in range(sz)}
   self.assertFalse(cells&block)
   cells|=block
 def test_individual_variation_in_major_city(self):
  lots=[];S.plan(site(900),T,lots.append)
  homes=[l for l in lots if l['kind']=='HOUSE' and l['grid']]
  self.assertGreaterEqual(len({(l['width'],l['depth']) for l in homes}),5)
  self.assertGreaterEqual(len({((l['x']-20000)%31,(l['z']-50000)%31) for l in homes}),10)
 def test_determinism(self):
  a=[];b=[];S.plan(site(220),T,a.append);S.plan(site(220),T,b.append)
  self.assertEqual(a,b)
 def test_winterfell_protected(self):
  a=site();a['siteId']='site_nw_029';lots=[]
  self.assertIn('PROTECTED',S.plan(a,T,lots.append)['status']);self.assertEqual(lots,[])
 def test_no_chunk_loads_or_forced_teleportation_at_work(self):
  root=ROOT/'C4_16_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld'
  daily=(root/'simulation/population/KnownWorldB42DailyLife.java').read_text()
  actors=(root/'simulation/population/KnownWorldB41Activation.java').read_text()
  self.assertIn('hasActiveWorkGoal(',daily)
  self.assertIn('routeAttempts++',daily)
  self.assertIn('if(!goingToWork && dx*dx+dz*dz>HOME_WANDER_LIMIT',actors)
  self.assertIn('unloadedWaypoints',daily)
  self.assertNotIn('getDayTime()',daily)
 def test_source_snapshot_apply_and_rollback(self):
  install=mod(ROOT/'tools/geodata/c4_16_stage_apply.py')
  with tempfile.TemporaryDirectory() as td:
   r=Path(td)
   shutil.copytree(ROOT/'C4_16_STAGE',r/'C4_16_STAGE')
   for rel in install.FILES:
    dst=r/rel;dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(r/'C4_16_STAGE/baseline'/rel,dst)
   sc=r/install.RSL
   (sc/'world_population_b4_v1').mkdir(parents=True)
   (sc/'world_population_b4_v1/manifest.json').write_text(json.dumps({'peopleTotal':3760684,'householdsTotal':799343,'modeledWorkersTotal':1880341,'siteCount':248}))
   (sc/'universal_construction_c4_11_global_v1.json').write_text('{"version":1}')
   (sc/'world_projection_b4_1a1_v1').mkdir(parents=True)
   (sc/'world_projection_b4_1a1_v1/manifest.json').write_text('{}')
   (sc/'world_daily_b4_2_v1').mkdir(parents=True)
   (sc/'world_daily_b4_2_v1/manifest.json').write_text('{}')
   (sc/'c4_11_global_tiles/site_test').mkdir(parents=True)
   (sc/'c4_11_global_tiles/site_test/0_0.json').write_text('unchanged')
   status=install.status(r)
   self.assertTrue(all(x=='READY' for x in status.values()))
   install.apply(r)
   self.assertTrue(all(x=='ALREADY_INSTALLED' for x in install.status(r).values()))
   stamps=list((r/install.SNAPROOT).iterdir());self.assertEqual(len(stamps),1)
   (sc/'c4_11_global_tiles/site_test/0_0.json').write_text('regenerated')
   install.rollback(r,stamps[0].name)
   self.assertEqual((sc/'c4_11_global_tiles/site_test/0_0.json').read_text(),'unchanged')
   self.assertTrue(all(x=='READY' for x in install.status(r).values()))
 def test_region_grove_and_connector_guards_present(self):
  java=(ROOT/'C4_16_STAGE/updated/src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java').read_text()
  for name in ('paintBeltSpines(', 'touchesBeltSpine(', 'Blocks.SHORT_GRASS', 'Blocks.FERN', 'Blocks.DANDELION'):
   self.assertIn(name,java)
  self.assertIn('p.approved',java)
 def test_install_rollback_snapshots_all_derived_indices(self):
  txt=(ROOT/'tools/geodata/c4_16_stage_apply.py').read_text()
  for field in ('world_projection_b4_1a1_v1','world_commutes_c4_15_v1','world_daily_b4_2_v1','c4_11_global_tiles'):
   self.assertIn(field,txt)
  self.assertIn('UNKNOWN_LOCAL_EDITS',txt)
if __name__=='__main__':unittest.main()
