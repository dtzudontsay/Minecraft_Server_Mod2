"""C4.15 full-global invariant, footprint and commute regressions."""
import collections,importlib.util,json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
def mod(name,file):
    spec=importlib.util.spec_from_file_location(name,ROOT/'tools/geodata'/file)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
layout=mod('layout415','c4_10_shared_layout.py')
roll=mod('roll415','c4_11_global_rollout.py')
commute=mod('commute415','c4_15_commute_assignment.py')
install=mod('install415','c4_15_local_install.py')

def source(tier='MAJOR',homes=400):
    return {'siteId':'site_sw_046','name':'Large Test City','tier':tier,
            'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
            'mapAnchor':{'x':20000,'z':50000},'markerIds':['SW-046'],
            'populationTarget':5000,'workingPopulationTarget':100,
            'householdsTarget':homes,'residentialBuildingsTarget':homes,
            'residentialUnitsPerBuilding':1,'workplaceBuildingsTarget':25,
            'occupationTargetsNonzero':{'Carpenter':100},
            'buildingDemandByKind':[{'kind':'SMITHY','requestedBuildings':25,'workersTarget':100,'slotsPerBuilding':4}],
            'mainComplex':{'reservedKeepWorkerSlots':0}}
T={k:{'id':k,'c1Width':w,'c1Depth':d,'requiresShore':False,'requiresMountainResource':False,'requiresPolicy':False}
   for k,w,d in [('HOUSE_SMALL',11,12),('HOUSE_MEDIUM',13,15),('HOUSE_URBAN',17,20),('SMITHY',13,11),('KEEP',49,41)]}

class RevisionTests(unittest.TestCase):
    def test_exact_lot_id_reservations_unchanged(self):
        s=source();out=[];result=layout.plan(s,T,out.append)
        self.assertEqual(len(out),425)
        self.assertEqual(result['linkedHouseholdReservations'],400)
        self.assertEqual(sum(o['reservedWorkerSlots'] for o in out),100)
        self.assertEqual(len(set(o['lotId'] for o in out)),425)
        self.assertEqual({v['face'] for v in out},{'SOUTH'})
        self.assertEqual(out[0]['lotId'],'site_sw_046:home:0000001')

    def test_irregular_compact_cells_and_no_overlap(self):
        out=[];layout.plan(source(),T,out.append)
        outer=[x for x in out if x['grid'] is not None]
        self.assertTrue(outer)
        self.assertEqual(layout.PITCH,31)
        self.assertLess(layout.PITCH,56)
        # Spatial collision invariant for all lots, including group reservations.
        cells=set()
        for l in outer:
            gx,gz,sx,sz=l['grid']
            allocated={(gx+dx,gz+dz) for dx in range(sx) for dz in range(sz)}
            self.assertFalse(cells & allocated)
            cells.update(allocated)
            self.assertLessEqual(l['width']+11,layout.PITCH*sx)
            self.assertLessEqual(l['depth']+11,layout.PITCH*sz)

    def test_green_pocket_demand_not_counted(self):
        lots=[];features=[]
        meta=layout.plan(source(homes=1000),T,lots.append,features.append)
        self.assertEqual(len(lots),1025)
        self.assertGreater(len(features),0)
        self.assertEqual(meta['greenPocketsReserved'],len(features))
        self.assertTrue(all(x['kind']=='GREEN_POCKET' for x in features))
        self.assertEqual(len({f['lotId'] for f in features}),len(features))

    def test_protected_winterfell_no_lots(self):
        s=source();s['siteId']='site_nw_029';out=[]
        v=layout.plan(s,T,out.append)
        self.assertEqual(v['status'],'EXISTING_WINTERFELL_V3_PROTECTED_NO_REBUILD')
        self.assertFalse(out)

    def test_large_facility_reserves_multiple_cells(self):
        s=source(homes=5);s['buildingDemandByKind']=[{'kind':'KEEP','requestedBuildings':25,'workersTarget':100,'slotsPerBuilding':4}]
        t={**T,'KEEP':{**T['KEEP']}};out=[]
        layout.plan(s,t,out.append)
        big=[l for l in out if l['kind']=='KEEP' and l['grid']]
        self.assertTrue(big)
        self.assertTrue(all(v['grid'][2]>1 and v['grid'][3]>1 for v in big))

    def test_generated_green_tiles_excluded_from_quota(self):
        rec,tiles=roll.site_record(source(homes=500),T,'WESTEROSI_INTERIOR',roll.layout_module_cached(),set())
        self.assertEqual(rec['physicalAvailableLots'],525)
        green={x['id'] for b in tiles.values() for x in b if x['kind']=='GREEN_POCKET'}
        self.assertTrue(green)
        self.assertEqual(len(green),rec['greenPocketsReserved'])

    def test_nearby_job_consume_exact_slots(self):
        wid=(0,0,'w:1')
        grid={'SMITHY':{(0,0):[wid]}};cap={'w:1':2}
        one=commute.allocate_worker((2,3),'SMITHY',grid,cap)
        two=commute.allocate_worker((2,3),'SMITHY',grid,cap)
        three=commute.allocate_worker((2,3),'SMITHY',grid,cap)
        self.assertEqual(one[0],'w:1')
        self.assertEqual(two[0],'w:1')
        self.assertEqual(three[2],'NO_CAPACITY_WITHIN_PROXY_LIMIT')
        self.assertEqual(cap['w:1'],0)

    def test_far_jobs_reported_not_falsely_claimed(self):
        cap={'far':10};grid={'FARM':{(20,20):[(2500,2500,'far')]}}
        a=commute.allocate_worker((0,0),'FARM',grid,cap,limit=600)
        self.assertIsNone(a[0]);self.assertIn('NO_CAPACITY',a[2]);self.assertEqual(cap['far'],10)

    def test_regional_brick_rule_exact_and_idempotent(self):
        raw='''class C3 {\n'''+install.LOOKUP+'''\n}'''
        once=install.patch_palettes(raw)
        self.assertEqual(once,install.patch_palettes(once))
        self.assertIn('"URBAN_HOME"',once)
        self.assertIn('Blocks.STONE_BRICKS',once)
        with self.assertRaises(ValueError):install.patch_palettes('missing anchor')

    def test_household_roof_spawn_guard(self):
        src=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java').read_text()
        self.assertIn('dz=(choice/10)',src)
        self.assertIn('initialTerrainHeight(xx,zz)',src)
        self.assertIn('y-nativeY>9',src)

    def test_fortification_tier_designs(self):
        src=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC410Fortifications.java').read_text()
        for val in ('fortifiedHall(', 'minorKeep(', 'majorKeep(', 'watchTower(', 'entrance(', 'SOLID PITCHED ROOF'):
            self.assertIn(val,src)
        self.assertIn('171,149',src);self.assertNotIn('private static void keepCell(',src)

    def test_b41_placement_bridge_preserved(self):
        src=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java').read_text()
        self.assertIn('b41ApprovedPlacementForInspection(',src)
        self.assertIn('private static void streetsForLot(',src)
        self.assertIn('"GREEN_POCKET"',src)

if __name__=='__main__':unittest.main()
