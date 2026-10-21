#!/usr/bin/env python3
"""Zero dependencies; synthetic 253 marker/259 holding coverage tests."""
import importlib.util
import pathlib
import sys
import unittest

HERE=pathlib.Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('kw_c1',HERE/'build_c1_settlement_presets.py')
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

ROLES={k:0 for k in mod.ROLE_WORKPLACE}
ROLES.update({'grain_farmer':38,'blacksmith':12,'cook':10,'guard':10,'merchant':15,
              'administrator':5,'religious_attendant':5,'gardener':5})
assert sum(ROLES.values())==100

def fixture():
    all_sites=[];markers=[]
    for i in range(253):
        mid=('NW-029' if i==0 else 'NEE-005' if i==1 else 'SEE-009' if i==2
             else 'SW-046' if i==3 else 'SE-008' if i==4 else f'SI-{i+100:03d}')
        sid='site_'+mid.lower().replace('-','_')
        tier=('MAJOR' if i in (0,3,4) else 'SPECIAL' if i in (1,2) else 'SMALL')
        kind=('ABANDONED_RUIN' if i==2 else 'MONUMENTAL_FRONTIER_FORT' if i==1
              else 'MAJOR_CITY' if tier=='MAJOR' else 'VILLAGE')
        archetype=('STYGAI_RUINS' if i==2 else 'FIVE_FORTS_MONUMENTAL' if i==1
                   else 'NORTHERN_CASTLE_TOWN' if i==0 else 'ROYAL_CAPITAL_CITY'
                   if i==3 else 'GHISCARI_PYRAMID_CITY' if i==4 else 'VILLAGE')
        pop=0 if i==2 else (1500 if i==1 else 5000 if i==0 else 500000 if i==3
              else 95000 if i==4 else 350)
        anchor={'x':-4000000+i*1000,'z':-2000000+i*1000,'y':None,
                'isBuildApproved':False,'status':'ACCEPTED_FOR_PLANNING'}
        markers.append({'markerId':mid,'siteId':sid,'workingAnchor':anchor})
        all_sites.append({'siteId':sid,'name':mid,'tier':tier,'archetype':archetype,
            'workingAnchor':anchor,'markerIds':[mid],'primaryMarkerId':mid,'zone':mid.split('-')[0],
            'geographicId':'known_'+str(i),'materialPalette':'NORTHERN',
            'physicalType':{'value':kind,'evidence':'CONTEXT'},
            'habitation':{'value':'RUIN' if i==2 else 'INHABITED','evidence':'CONTEXT'},
            'linkedPoliticalHoldingIds':[],
            'populationPlanning':{'baseline':pop,'rangeEvidence':'INVENTED'},
            'laborParticipation':{'value':.5,'evidence':'INVENTED'},
            'occupationDistribution':{'workerRolesPercent':dict(ROLES) if pop else {k:0 for k in ROLES}}})
    holdings=[]
    for i in range(259):
        h={'holdingId':f'h_{i}','name':f'Holding {i}',
           'worldLocationId':'known_0' if i==0 else 'known_'+str(i),
           'tier':'MAJOR' if i==0 else 'MEDIUM','archetype':'CASTLE_TOWN',
           'placementStatus':'LINKED_TO_MAP_SITE' if i==0 else 'UNPLACED_SEAT',
           'linkedSiteIds':['site_nw_029'] if i==0 else [],
           'parentHoldingId':None,'parentCitySiteIds':[],
           'ownerDynastyId':'house_stark' if i==0 else None,
           'holderNpcId':'eddard_stark' if i==0 else None,'isPoliticalDomain':False}
        holdings.append(h)
    return {'schemaVersion':1,'summary':{'markerCount':253,'holdingCount':259},
            'physicalSites':all_sites,'markers':markers,'holdings':holdings}

class TestC1(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.reg=fixture()
        cls.payload=mod.build(cls.reg,{'permitBrothel':False})
        cls.sites={x['primaryMarkerId']:x for x in cls.payload['physicalSitePresets']}

    def test_complete_coverage(self):
        self.assertEqual(self.payload['summary']['markerCount'],253)
        self.assertEqual(self.payload['summary']['physicalSiteCount'],253)
        self.assertEqual(self.payload['summary']['holdingCount'],259)
        self.assertEqual(len({m for s in self.payload['physicalSitePresets'] for m in s['markerIds']}),253)

    def test_no_builds_anywhere(self):
        for s in self.payload['physicalSitePresets']:
            self.assertFalse(s['generation']['physicalConstructionEnabled'])
            self.assertFalse(s['generation']['terrainApproved'])
            self.assertTrue(s['constructionSafety']['noLavaLakesWithinFootprintOrFeatureMargin'])
            self.assertTrue(s['constructionSafety']['noTreesWithinFootprintOrFeatureMargin'])

    def test_known_specials(self):
        self.assertEqual(self.sites['NEE-005']['blueprintMode'],'MONUMENTAL_FORTRESS_REQUIRE_AUTHORED_BLUEPRINT')
        self.assertEqual(self.sites['SEE-009']['blueprintMode'],'RUIN_OR_LANDMARK_ONLY')
        self.assertEqual(self.sites['SEE-009']['buildingDemand']['peopleTarget'],0)
        self.assertTrue(self.sites['NW-029']['generation']['preserveWinterfellV3'])
        self.assertEqual(self.sites['SE-008']['blueprintMode'],'PYRAMID_CITY_REQUIRE_AUTHORED_BLUEPRINT')

    def test_job_capacity_and_compactness(self):
        city=self.sites['SW-046']
        d=city['buildingDemand'];self.assertEqual(d['peopleTarget'],500000)
        self.assertEqual(sum(d['roleWorkerTargets'].values()),d['workforceTarget'])
        self.assertEqual(sum(d['staffByWorkplace'].values()),d['workforceTarget'])
        self.assertEqual(d['workplaceCounts']['FARM'],mod.qceil(d['staffByWorkplace']['FARM'],12))
        self.assertGreater(d['housingBuildingBacklog'],0)
        self.assertGreater(d['firstPhaseHousingPlots'],0)
        self.assertLessEqual(d['firstPhaseHousingPlots'],mod.TIERS['MAJOR']['plotLimit'])
        self.assertTrue(all(max(abs(x['localX']),abs(x['localZ']))<=mod.TIERS['MAJOR']['radius']
                            for x in city['compactLayoutPreview']))
        self.assertTrue(all(not x['hasTerrainApproval'] for x in city['compactLayoutPreview']))

    def test_no_brothel_without_policy(self):
        for s in self.payload['physicalSitePresets']:
            self.assertEqual(s['buildingDemand']['workplaceCounts'].get('BROTHEL',0),0)

    def test_double_marker_group(self):
        reg=fixture();merge=reg['markers'].pop()
        removed=reg['physicalSites'].pop()
        reg['physicalSites'][-1]['markerIds'].append(merge['markerId'])
        merge['siteId']=reg['physicalSites'][-1]['siteId']
        reg['markers'].append(merge)
        self.assertEqual(mod.build(reg,{'permitBrothel':False})['summary']['physicalSiteCount'],252)

    def test_duplicate_marker_rejected(self):
        reg=fixture();reg['markers'][1]['markerId']=reg['markers'][0]['markerId']
        with self.assertRaises(ValueError):mod.build(reg,{'permitBrothel':False})

if __name__=='__main__':unittest.main()
