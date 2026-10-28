"""Run: python -m unittest discover -s src/test/python -p test_c4_9_settlement_ledgers.py"""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SRC = Path(__file__).resolve().parents[3] / 'tools/geodata/c4_9_settlement_ledgers.py'
spec = importlib.util.spec_from_file_location('kw_c49', SRC)
c49 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(c49)

COLUMNS = ('site_id name primary_marker all_markers tier atlas_tag blueprint_mode '
           'working_x working_z planned_people planned_workers other_people '
           'estimated_households houses_requested units_per_house_building '
           'nonzero_job_classes workplace_buildings_requested anchor_status build_approved').split()


def site(sid, name, tier, people, workers, households, houses, units, workplaces):
    values = [sid,name,'NW-029' if sid == c49.WINTERFELL else 'NW-999',
              'NW-029' if sid == c49.WINTERFELL else 'NW-999',tier,tier,
              'GENERAL_COMPACT_SETTLEMENT','0','0',str(people),str(workers),
              str(people-workers),str(households),str(houses),str(units),
              '2',str(workplaces),'ACCEPTED_FOR_PLANNING','False']
    return dict(zip(COLUMNS,values))


class C49Test(unittest.TestCase):
    def test_households_exact_conservation_and_determinism(self):
        p = c49.household_sizes(1000, 210, 1234)
        self.assertEqual(len(p),210)
        self.assertEqual(sum(p),1000)
        self.assertEqual(p,c49.household_sizes(1000,210,1234))
        self.assertGreater(len(set(p)),2)
        self.assertTrue(all(1 <= x <= 10 for x in p))

    def test_winterfell_authored_and_ordinary_record(self):
        sites = [site('site_nw_029','Winterfell','MAJOR',5000,2500,1064,266,4,2),
                 site('site_fake','Sample','SMALL',10,4,2,2,1,1)]
        work = [{'site_id':'site_nw_029','building_type':'KEEP','workers':'1',
                 'requested_workplaces':'0','capacity_per_unit':'0'},
                {'site_id':'site_nw_029','building_type':'SMITHY','workers':'2499',
                 'requested_workplaces':'2','capacity_per_unit':'1250'},
                {'site_id':'site_fake','building_type':'SMITHY','workers':'4',
                 'requested_workplaces':'1','capacity_per_unit':'6'}]
        jobs = [{'site_id':'site_nw_029','planned_people':'5000','planned_workers':'2500',
                 'A':'1','B':'2499'},
                {'site_id':'site_fake','planned_people':'10','planned_workers':'4',
                 'A':'0','B':'4'}]
        with tempfile.TemporaryDirectory() as temp:
            total=c49.build(sites,work,jobs,Path(temp),include_slot_files=True)
            records=json.loads((Path(temp)/'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json').read_text())['sites']
            self.assertEqual(total['people'],5010)
            self.assertEqual(records[0]['householdsTarget'],1136)
            self.assertEqual(records[0]['residentialBuildingsTarget'],1136)
            self.assertEqual(records[0]['mainComplex']['reservedKeepWorkerSlots'],1)
            self.assertEqual(records[0]['householdSlotSource'],'WINTERFELL_V2_AUTHORED_IDS_DO_NOT_RECREATE')
            self.assertEqual(records[1]['mainComplex']['template'],'MANOR_HALL')
            hh=(Path(temp)/'C4_9_HOUSEHOLD_RESERVATIONS.jsonl').read_text().strip().splitlines()
            self.assertEqual(len(hh),2)  # no conflicting new Winterfell IDs
            self.assertEqual(records[0]['workplaceSlotSource'], 'WINTERFELL_V3_AUTHORED_LOT_IDS_RECONCILE_BEFORE_ANY_CREATION')
            wp=(Path(temp)/'C4_9_WORKPLACE_RESERVATIONS.jsonl').read_text().strip().splitlines()
            self.assertEqual(len(wp),1)  # 1 ordinary, zero fabricated Winterfell workplaces
            self.assertEqual(sum(json.loads(x)['membersTarget'] for x in hh),10)

    def test_keeps_zero_population_landmarks_unbuilt(self):
        s=site('ruin','Ruins','SPECIAL',0,0,0,0,1,0)
        s['blueprint_mode']='RUIN_OR_LANDMARK_ONLY'
        with tempfile.TemporaryDirectory() as temp:
            c49.build([s],[],[{'site_id':'ruin','planned_workers':'0'}],Path(temp))
            entry=json.loads((Path(temp)/'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json').read_text())['sites'][0]
            self.assertFalse(entry['mainComplex']['required'])
            self.assertEqual(entry['residentialBuildingsTarget'],0)

if __name__=='__main__':unittest.main()
