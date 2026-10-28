"""Run with: py -3 -m unittest discover -s src/test/python -p test_c4_10_shared_layout.py -v"""
import collections
import importlib.util
import pathlib
import tempfile
import json
import unittest
import subprocess
import sys

ROOT=pathlib.Path(__file__).resolve().parents[3]
PATH=ROOT/'tools/geodata/c4_10_shared_layout.py'
spec=importlib.util.spec_from_file_location('c410',PATH)
c410=importlib.util.module_from_spec(spec)
spec.loader.exec_module(c410)

TEMPLATES={name:{'id':name,'c1Width':w,'c1Depth':d,'requiresShore':False,
                 'requiresMountainResource':False,'requiresPolicy':False}
           for name,w,d in [
           ('HOUSE_SMALL',11,12),('HOUSE_MEDIUM',13,15),('HOUSE_URBAN',17,20),
           ('SMITHY',13,11),('ADMINISTRATION',17,14),('FARM',17,15),
           ('GARDEN',11,11),('KEEP',49,41)]}


def site(sid='site_demo',tier='SMALL',homes=8,hh=8,units=1,mode='GENERAL_COMPACT_SETTLEMENT',
         workers=12,people=40,demands=None):
    if demands is None:
        demands=[{'kind':'SMITHY','workersTarget':7,'requestedBuildings':2,'slotsPerBuilding':6},
                 {'kind':'KEEP','workersTarget':5,'requestedBuildings':0,'slotsPerBuilding':0},
                 {'kind':'FARM','workersTarget':0,'requestedBuildings':1,'slotsPerBuilding':12}]
    return {'siteId':sid,'name':'Test Settlement','tier':tier,
            'blueprintMode':mode,'mapAnchor':{'x':100000,'z':-200000},
            'populationTarget':people,'workingPopulationTarget':workers,
            'occupationTargetsNonzero':{'Blacksmith':workers},
            'residentialBuildingsTarget':homes,'householdsTarget':hh,
            'residentialUnitsPerBuilding':units,
            'workplaceBuildingsTarget':sum(x['requestedBuildings'] for x in demands),
            'buildingDemandByKind':demands,
            'mainComplex':{'required':True,'reservedKeepWorkerSlots':5}}


class LayoutTests(unittest.TestCase):
    def test_three_core_tiers_and_workplace_capacity(self):
        for tier,h,units in [('SMALL',8,1),('MEDIUM',4,2),('MAJOR',2,4)]:
            with self.subTest(tier=tier):
                s=site(tier=tier,homes=h,hh=8,units=units)
                lots=[]
                result=c410.plan(s,TEMPLATES,lots.append)
                self.assertEqual(result['core']['template'],c410.CORE[tier]['building'])
                self.assertEqual(result['core']['workplaceStaffSlots'],5)
                self.assertEqual(result['lotCounts']['SMITHY'],2)
                self.assertEqual(result['lotCounts']['FARM'],1)
                self.assertEqual(result['lotCounts']['HOUSE'],h)
                smiths=[l for l in lots if l['kind']=='SMITHY']
                self.assertEqual(sum(l['reservedWorkerSlots'] for l in smiths),7)
                self.assertEqual(sum(len(l['householdReservationIds']) for l in lots),8)
                self.assertEqual({l['template'] for l in lots if l['kind']=='HOUSE'},
                                 {'HOUSE_'+('URBAN' if tier=='MAJOR' else tier)})

    def test_unbounded_major_city(self):
        # Explicitly cross the old C4.6 maximum radius and pilot plot-count limits.
        s=site(tier='MAJOR',homes=6000,hh=24000,units=4,
               people=85000,workers=12)
        counters=collections.Counter()
        ids=set();regions=collections.Counter()
        def sink(lot):
            self.assertNotIn(lot['lotId'],ids)
            ids.add(lot['lotId'])
            counters[lot['kind']]+=1
            regions[lot['region']]+=1
        result=c410.plan(s,TEMPLATES,sink)
        self.assertEqual(counters['HOUSE'],6000)
        self.assertEqual(result['linkedHouseholdReservations'],24000)
        self.assertGreater(result['outerRadiusCells'],35)
        self.assertEqual(regions['OUTER_RURAL_BELT'],1)
        self.assertGreater(regions['MIXED_URBAN_RING'],0)

    def test_kings_landing_scale_targets_uncapped(self):
        # Exact C4.8 King's Landing buildings/households; synthetic job-class
        # distribution tests scale only, NOT authoritative King's Landing jobs.
        d=[{'kind':'SMITHY','workersTarget':8492,
            'requestedBuildings':1416,'slotsPerBuilding':6},
           {'kind':'ADMINISTRATION','workersTarget':241508,
            'requestedBuildings':18059,'slotsPerBuilding':14}]
        s=site(tier='MAJOR',homes=26596,hh=106383,units=4,
               people=500000,workers=250000,demands=d)
        s['mainComplex']['reservedKeepWorkerSlots']=0
        count=0;staff=0;last_radius=0
        def sink(lot):
            nonlocal count,staff,last_radius
            count+=1
            staff+=lot['reservedWorkerSlots']
            last_radius=max(last_radius,lot['ring'])
        result=c410.plan(s,TEMPLATES,sink)
        self.assertEqual(count,26596+19475)
        self.assertEqual(staff,250000)
        self.assertEqual(result['linkedHouseholdReservations'],106383)
        self.assertGreater(last_radius,35)

    def test_deterministic_layout_and_rural_outside(self):
        s=site(homes=40,hh=40)
        a=[];b=[]
        ra=c410.plan(s,TEMPLATES,a.append)
        rb=c410.plan(s,TEMPLATES,b.append)
        self.assertEqual(a,b)
        self.assertEqual(ra,rb)
        urban=max(l['ring'] for l in a if l['region']=='MIXED_URBAN_RING')
        rural=min(l['ring'] for l in a if l['region']=='OUTER_RURAL_BELT')
        self.assertGreater(rural,urban)

    def test_special_major_retains_demand_but_cannot_build(self):
        s=site(tier='MAJOR',mode='COASTAL_REQUIRE_SHORE_LAYOUT')
        items=[]
        info=c410.plan(s,TEMPLATES,items.append)
        self.assertEqual(info['status'],'GEOMETRY_PREVIEW_EXCEPTIONAL_SITE_NO_CONSTRUCTION')
        self.assertEqual(len(items),s['residentialBuildingsTarget']+s['workplaceBuildingsTarget'])
        self.assertFalse(info['terrainApproved'])

    def test_winterfell_preserved(self):
        s=site(sid='site_nw_029',tier='MAJOR')
        lots=[]
        out=c410.plan(s,TEMPLATES,lots.append)
        self.assertEqual(out['status'],'EXISTING_WINTERFELL_V3_PROTECTED_NO_REBUILD')
        self.assertEqual(lots,[])

    def test_special_tier_not_silently_generic(self):
        s=site(tier='SPECIAL')
        lots=[]
        out=c410.plan(s,TEMPLATES,lots.append)
        self.assertEqual(out['status'],'SPECIAL_SITE_REQUIRES_DISTINCT_CORE_POLICY')
        self.assertEqual(lots,[])

    def test_unknown_template_is_visible_exception_not_dropped(self):
        s=site(demands=[{'kind':'ARCANE_STUDY','workersTarget':12,
                         'requestedBuildings':3,'slotsPerBuilding':4}])
        s['mainComplex']['reservedKeepWorkerSlots']=0
        lots=[]
        info=c410.plan(s,TEMPLATES,lots.append)
        self.assertEqual(info['lotCounts']['ARCANE_STUDY'],3)
        self.assertEqual(info['exceptionCounts']['TEMPLATE_MISSING'],3)
        self.assertEqual(sum(x['reservedWorkerSlots'] for x in lots),12)

    def test_unsatisfied_worker_slots_rejected(self):
        s=site(demands=[{'kind':'SMITHY','workersTarget':12,
                         'requestedBuildings':1,'slotsPerBuilding':6}])
        with self.assertRaisesRegex(ValueError,'uncapped worker demand'):
            c410.plan(s,TEMPLATES,lambda x:None)

    def test_invalid_household_capacity_rejected(self):
        s=site(tier='MEDIUM',homes=2,hh=8,units=2)
        with self.assertRaisesRegex(ValueError,'insufficient household units'):
            c410.plan(s,TEMPLATES,lambda x:None)

    def test_command_line_summary_only(self):
        # Regression for argparse --architecture incorrectly read as args.arch.
        with tempfile.TemporaryDirectory() as t:
            t=pathlib.Path(t)
            src=t/'sites.json'
            templates=t/'templates.json'
            out=t/'out'
            src.write_text(json.dumps({'sites':[site()]}),encoding='utf8')
            templates.write_text(json.dumps({'templates':list(TEMPLATES.values())}),encoding='utf8')
            cmd=[sys.executable,str(PATH),'--source',str(src),
                 '--architecture',str(templates),'--output',str(out),
                 '--site','site_demo','--summary-only']
            result=subprocess.run(cmd,capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stdout+'\n'+result.stderr)
            self.assertTrue((out/'C4_10_SUMMARY.json').exists())
            self.assertEqual(json.loads((out/'C4_10_SUMMARY.json').read_text())
                             ['totals']['newPhysicalLotsProposed'],11)
            self.assertFalse((out/'C4_10_ALL_PLANNED_LOTS.jsonl').exists())

    def test_end_to_end_with_temporary_inputs(self):
        with tempfile.TemporaryDirectory() as t:
            t=pathlib.Path(t)
            src=t/'sites.json';templates=t/'templates.json';out=t/'output'
            src.write_text(json.dumps({'sites':[site()]}),encoding='utf8')
            templates.write_text(json.dumps({'templates':list(TEMPLATES.values())}),encoding='utf8')
            result=c410.run(src,templates,out,site_filter={'site_demo'})
            self.assertEqual(result['newPhysicalLotsProposed'],11)
            self.assertEqual(len((out/'C4_10_ALL_PLANNED_LOTS.jsonl').read_text().splitlines()),11)
            self.assertEqual(json.loads((out/'C4_10_SUMMARY.json').read_text())['terrainApproved'],0)

if __name__=='__main__':unittest.main()
