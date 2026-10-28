import csv, gzip, importlib.util, json, sys, tempfile, unittest
from pathlib import Path

BASE=Path(__file__).resolve().parents[3]
spec=importlib.util.spec_from_file_location('b40',BASE/'tools/geodata/b4_0_world_population.py')
b40=importlib.util.module_from_spec(spec);spec.loader.exec_module(b40)
sp=importlib.util.spec_from_file_location('hook',BASE/'tools/geodata/b4_0_install_hook.py')
hook=importlib.util.module_from_spec(sp);sp.loader.exec_module(hook)


def dump(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj),encoding='utf8')


def fixture(root):
    sids=[f'site_ne_{i:03d}' for i in range(1,249)]
    sites=[];jobs=[];resident={'site_ne_001','site_ne_003'}
    hh=[]
    ids=['Blacksmith','Clerk']+[f'Job {i:03d}' for i in range(202)]
    for idx,sid in enumerate(sids):
        pop,workers,households=(7,4,2) if idx==0 else (5,3,2) if idx==2 else (0,0,0)
        demand=([{'kind':'SMITHY','workersTarget':2,'requestedBuildings':1,'slotsPerBuilding':2},
                 {'kind':'ADMINISTRATION','workersTarget':2,'requestedBuildings':1,'slotsPerBuilding':2}]
                if idx==0 else [{'kind':'SMITHY','workersTarget':3,'requestedBuildings':2,'slotsPerBuilding':2}]
                if idx==2 else [])
        sites.append({'siteId':sid,'name':sid,'tier':'SMALL','markerIds':[f'NE-{idx+1:03d}'],
                      'mapAnchor':{'x':1000+idx,'z':2000+idx},'populationTarget':pop,
                      'workingPopulationTarget':workers,'householdsTarget':households,
                      'residentialBuildingsTarget':households,'residentialUnitsPerBuilding':1,
                      'buildingDemandByKind':demand,'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
                      'mainComplex':{'required':True}})
        jobs.append({'site_id':sid,'name':sid,'primary_marker':f'NE-{idx+1:03d}',
                     'all_markers':f'NE-{idx+1:03d}','tier':'SMALL','atlas_tag':'SMALL',
                     'blueprint_mode':'GENERAL_COMPACT_SETTLEMENT','working_x':'1000',
                     'working_z':'2000','planned_people':str(pop),'planned_workers':str(workers),
                     **{x:('2' if idx==0 else '3' if idx==2 else '0') if x=='Blacksmith' else
                        ('2' if idx==0 else '0') if x=='Clerk' else '0' for x in ids}})
        if idx==0:
            for j,n in enumerate((4,3),1):hh.append({'siteId':sid,'householdReservationId':f'{sid}:hh:{j:06d}','membersTarget':n})
        if idx==2:
            for j,n in enumerate((3,2),1):hh.append({'siteId':sid,'householdReservationId':f'{sid}:hh:{j:06d}','membersTarget':n})
    dump(root/b40.SOURCE/'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json',{'sites':sites})
    f=root/b40.SOURCE/'C4_9_HOUSEHOLD_RESERVATIONS.jsonl';f.write_text(''.join(json.dumps(h)+'\n' for h in hh),encoding='utf8')
    csvfile=root/b40.JOBS;csvfile.parent.mkdir(parents=True,exist_ok=True)
    with csvfile.open('w',encoding='utf8',newline='') as out:
        wr=csv.DictWriter(out,fieldnames=list(jobs[0]),delimiter=';');wr.writeheader();wr.writerows(jobs)
    dump(root/b40.C44,{'rows':[{'jobId':x,'building':'SMITHY' if x=='Blacksmith' else 'ADMINISTRATION','isDuplicate':False} for x in ids]})
    dump(root/b40.B2,{'physicalSites':[{'siteId':sid,'regionalInfluences':{'effectiveSourceLocationId':'the_north'}} for sid in sids],
        'regions':[{'regionId':'the_north','descendantPhysicalSiteIds':sids,'regionalInfluences':{
            'effectiveSourceLocationId':'the_north','cultureWeights':{'northman':.9},
            'religionWeights':{'old_gods':.85,'faith_of_the_seven':.15},
            'languageWeights':{'common_tongue':1.0}}}]})
    dump(root/'src/main/resources/data/knownworld/reference/regional_influences.json',[{'locationId':'the_north','cultures':{'northman':{'weight':.9}},'religions':{'old_gods':{'weight':.85}},'languages':{'common_tongue':{'weight':1.0}}}])
    dump(root/'src/main/resources/data/knownworld/reference/regional_influence_extensions.json',[])
    (root/b40.PALETTES).parent.mkdir(parents=True,exist_ok=True)
    import shutil;shutil.copyfile(BASE/b40.PALETTES,root/b40.PALETTES)
    return sites

class PopulationB40Tests(unittest.TestCase):
    def test_full_204_matrix_sharded_and_family_conservation(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);fixture(root)
            out=root/'out';result=b40.generate(root,out,{'site_ne_001','site_ne_003'},strict=False)
            self.assertEqual((result['peopleTotal'],result['householdsTotal'],result['modeledWorkersTotal']),(12,4,7))
            self.assertEqual(result['siteCount'],2)
            allp=[]
            for sid in ('site_ne_001','site_ne_003'):
                with gzip.open(out/(sid+'.jsonl.gz'),'rt',encoding='utf8') as stream:
                    people=[json.loads(s) for s in stream];allp.extend(people)
                expected=4 if sid=='site_ne_001' else 3
                self.assertEqual(sum(bool(p['occupationId']) for p in people),expected)
                self.assertTrue(all(298-p['birthYearAC']>=17 for p in people if p['occupationId']))
                self.assertTrue(all(p['householdId'].startswith(sid+':hh:') for p in people))
                self.assertTrue(all(p['homeLotId'].startswith(sid+':home:') for p in people))
            self.assertEqual(len({p['personId'] for p in allp}),12)
            self.assertEqual(len({p['householdId'] for p in allp}),4)
            self.assertEqual(sum(1 for p in allp if p['workplaceId'] is None and p['occupationId']),0)
            snapshot={p['personId']:p['displayName'] for p in allp}
            b40.generate(root,out,{'site_ne_001','site_ne_003'},strict=False)
            with gzip.open(out/'site_ne_001.jsonl.gz','rt',encoding='utf8') as f:
                self.assertEqual(snapshot['b4:site_ne_001:00000001'],json.loads(next(f))['displayName'])
    def test_winterfell_authority_preserved_not_recreated(self):
        site={'populationTarget':5000,'workingPopulationTarget':2500}
        roster={'residents':[{'id':f'wf2_gen_{i:05d}','name':f'Original {i}',
            'householdId':f'old_hh_{i%1136:04d}','homeId':f'old_home_{i%1136:04d}',
            'job':'FARMER','culture':'northman','religion':'old_gods','birthYear':260,'sex':'MALE'}
            for i in range(5000)], 'households':[{} for _ in range(1136)]}
        out=[]
        summ=b40.generate_winterfell(site,roster,out.append)
        self.assertEqual(len(out),5000)
        self.assertEqual(summ['households'],1136)
        self.assertEqual(out[0]['personId'],'wf2_gen_00000')
        self.assertEqual(out[-1]['personId'],'wf2_gen_04999')
        self.assertEqual(out[100]['identityProvenance'],
            'EXISTING_WINTERFELL_V2_REUSED_NOT_IMPORTED_AGAIN')
    def test_employee_age_and_household_parentage(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);fixture(root)
            out=root/'out';b40.generate(root,out,{'site_ne_001'},strict=False)
            with gzip.open(out/'site_ne_001.jsonl.gz','rt',encoding='utf8') as f:
                people=[json.loads(line) for line in f]
            byid={p['personId']:p for p in people}
            for p in people:
                if p['occupationId']:self.assertGreaterEqual(298-p['birthYearAC'],17)
                for parent in (p['motherId'],p['fatherId']):
                    if parent:
                        self.assertIn(parent,byid)
                        self.assertGreaterEqual((298-byid[parent]['birthYearAC'])-(298-p['birthYearAC']),16)
                if p['spouseId']:
                    self.assertEqual(byid[p['spouseId']]['spouseId'],p['personId'])
    def test_missing_job_source_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);fixture(root)
            path=root/b40.C44
            job=b40.jread(path);job['rows']=job['rows'][:-1];dump(path,job)
            with self.assertRaisesRegex(ValueError,'204 unique'):
                b40.generate(root,root/'out',{'site_ne_001'},strict=False)
    def test_household_overflow_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);fixture(root)
            path=root/b40.SOURCE/'C4_9_HOUSEHOLD_RESERVATIONS.jsonl'
            lines=path.read_text().splitlines();first=json.loads(lines[0]);first['membersTarget']=1000
            lines[0]=json.dumps(first);path.write_text('\n'.join(lines)+'\n')
            with self.assertRaisesRegex(ValueError,'household reservations'):
                b40.generate(root,root/'out',{'site_ne_001'},strict=False)
    def test_installer_is_idempotent_preserves_existing_hooks(self):
        source='package x;\nimport dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellV3Runtime;\nclass X {\n        WinterfellV3Runtime.register();\n}\n'
        patched=hook.patch(source)
        self.assertEqual(hook.patch(patched),patched)
        self.assertIn('KnownWorldB40PopulationIndex.register()',patched)
    def test_reject_partial_install(self):
        with self.assertRaises(ValueError):hook.patch('file '+hook.IMPORT)
    def test_repeated_profiles_and_unknowns_are_explicit(self):
        r={'physicalSites':[{'siteId':'site_ne_001','regionalInfluences':{'effectiveSourceLocationId':'the_north'}}],
            'regions':[{'descendantPhysicalSiteIds':['site_ne_001'],'regionalInfluences':{'cultureWeights':{'northman':{'weight':.9}},'religionWeights':{'old_gods':{'weight':.85}}}}]}
        p=b40.resolve_profiles(r,[],[])
        self.assertAlmostEqual(p['site_ne_001']['cultures']['unknown_culture'],.1)
        self.assertAlmostEqual(p['site_ne_001']['religions']['unknown_religion'],.15)

if __name__=='__main__':unittest.main()
