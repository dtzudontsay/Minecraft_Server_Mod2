#!/usr/bin/env python3
"""Deterministic, NON-AUTHORITATIVE draft Winterfell 298 AC case-study.

Reads locally checked-out scenario character files, preserves authored IDs,
proposes profile expansions (NOT installed into scenario by this script),
creates conserved demographic/household/building ledgers, and optional
24-person standstill demonstration functions. Does not register simulation NPCs.
Python standard library only. Never overwrites the nine original named profiles.
"""
from __future__ import annotations
import csv, json, random, itertools, collections, hashlib, pathlib, copy
ROOT=pathlib.Path(__file__).resolve().parents[2]
BASE=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac/characters'
OUT=ROOT/'docs/step3/winterfell_case_study'
RESOURCE=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_roster.json'
FUNC=ROOT/'src/main/resources/data/knownworld/function/case_study'
SEED=298017
RNG=random.Random(SEED)
RESIDENTS=1200
ZONES={'castle':{'children':75,'elders':35,'workers':190,'households':55},
       'wintertown':{'children':70,'elders':25,'workers':145,'households':57},
       'hinterland':{'children':185,'elders':66,'workers':409,'households':145}}
ROLES={
 'castle':{'guard':80,'household_service':45,'administration':16,'artisan_smith':15,
           'food_service':20,'livestock_stable':5,'healer_religious':4,'other':5},
 'wintertown':{'guard':2,'household_service':25,'trade':30,'artisan_smith':20,
               'builder_carpenter':17,'food_service':30,'transport':10,
               'healer_religious':4,'forester_hunter':5,'other':2},
 'hinterland':{'farmer':310,'livestock_stable':38,'forester_hunter':30,
               'transport':15,'builder_carpenter':16}
}
STARKS=['eddard_stark','catelyn_stark','robb_stark','sansa_stark','arya_stark','bran_stark',
        'rickon_stark','jon_snow','theon_greyjoy']
VISITORS=['robert_baratheon','cersei_lannister','jaime_lannister','tyrion_lannister','benjen_stark']
# These are separate source-supported leads, not characters currently in the authored scenario.
# Placement/provenance must be verified in a separate review before merge.
STAFF=[('Luwin','maester',52),('Vayon Poole','steward',48),('Jeyne Poole','noble_attendant',11),
 ('Rodrik Cassel','master_at_arms',58),('Beth Cassel','dependent',9),('Jory Cassel','guard',30),
 ('Septa Mordane','septa',47),('Septon Chayle','septon',50),('Hullen','master_of_horse',49),
 ('Harwin','guard',24),('Joseth','stablehand',30),('Farlen','kennelmaster',40),('Palla','dependent',15),
 ('Old Nan','storyteller',82),('Hodor','stablehand',25),('Gage','cook',36),('Mikken','smith',36),
 ('Hallis Mollen','guard',32),('Desmond','guard',27),('Jacks','guard',28),('Porther','guard',27),
 ('Quent','guard',29),('Alyn','guard',22),('Tomard','guard',34),('Varly','guard',32),
 ('Heward','guard',26),('Cayn','guard',36),('Wyl','guard',24)]
STAFF_LINKS=[('Vayon Poole','Jeyne Poole'),('Rodrik Cassel','Beth Cassel'),
             ('Hullen','Harwin'),('Farlen','Palla'),('Old Nan','Hodor')]
FIRST_M=['Ben','Donnel','Hal','Jonos','Ronnel','Willam','Harl','Torrhen','Brandon','Edrick','Tom','Alyn','Ramsen','Ethan','Gared','Lew','Robyn','Garth','Pate','Joss','Hugh','Rickard','Harwood','Willis','Osmund','Cedric','Corben','Rodrik','Theo','Walder','Harrion','Edmyn','Lorren','Lymond']
FIRST_F=['Lysa','Mara','Alys','Beth','Jeyne','Gella','Wylla','Lynesse','Sarra','Dacey','Roslin','Marya','Lyra','Ellyn','Mira','Wynafryd','Sybelle','Janna','Myranda','Rowena','Bessa','Helly','Tansy','Elyn','Nella','Erena','Myrielle','Wenna','Celia','Willa']
FAMILY=['Ashford','Brackenfield','Northwood','Frost','Wool','Hornbrook','Moss','Tanner','Miller','Snowbrook','Hearth','Timber','Rowan','Barley','Reedbank','Whitebark','Flintwell','Hale','Briar','Oakham','Marsh','Brook','Nettle','Hollow','Mander','Dale','Harrow','Greystone','Coldwell','Ash','Tallow','Farrow','Hunt','Snowden','Wick','Rill','Holt','Warren','Hill','Hay','Fen','Grove','Ward','Beck','Underhill','Kerr','Longford','Ford','Whittle','Borne','Fenn','Claye','Rowe','Blackbriar','Thorn','Yarrow','Wells','Croft','Sedge','Barn','Mead']
# Inherit EXACT keys from richer Eddard profile whenever possible; calculated values are [M].
SKILLS=['martial','diplomacy','stewardship','intrigue','learning','combat','leadership','riding',
'sailing','navigation','trade','crafting','agriculture','hunting','medicine','religious_study',
'language','performance','command','logistics']
APTITUDES=SKILLS[:-5]+['religious_study','languages','performance','command','logistics']
DISPOSITIONS=['independence','assertiveness','impulsivity','discipline','curiosity','risk_tolerance',
'emotional_stability','suspiciousness','empathy','aggression','adaptability','persistence','pride',
'humility','greed','generosity','forgiveness','vindictiveness','deceitfulness','gullibility',
'conformity','dominance','obedience','jealousy','romanticism','sensuality']
CHARACTER_VALUES=['duty','family_loyalty','house_loyalty','dynastic_loyalty','clan_loyalty',
'friendship_loyalty','personal_freedom','authority_respect','hierarchy_acceptance','egalitarianism',
'traditionalism','innovation','religiosity','religious_tolerance','cultural_tolerance',
'honor_culture','hospitality','mercy','vengeance','justice','law_obedience','wealth_seeking',
'prestige_seeking','power_seeking','knowledge_seeking','martial_glory','pacifism','family_formation',
'romantic_love','political_stability','social_mobility','communal_solidarity','individualism']
SOCIAL_NORMS=['martial','courtly','mercantile','naval','raiding','scholarly','equestrian',
'clan_based','dynastic','religious','hospitality','vengeance','bureaucratic','urban','rural',
'cosmopolitan','isolationist','collectivist','individualist','social_mobility','authority',
'traditional','foreign_exposure','political_exposure','migratory']
PERSONALITY=['courage','ambition','compassion','honor','patience','sociability']


def save_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def read_named(id):
    p=BASE/f'{id}.json'
    if not p.is_file():raise FileNotFoundError(f'Missing local authored character: {p}')
    return json.loads(p.read_text('utf8'))

def bounded(v):return round(max(-1.0,min(1.0,v)),3)

def unit(v):return round(max(0.0,min(1.0,v)),3)

def full_profile(name,age,role,sex,faith='old_gods',culture='northman',existing=None):
    """Produce complete typed profile candidate WITHOUT pretending estimates are canon."""
    profile=copy.deepcopy((existing or {}).get('profile',{}))
    profile.setdefault('culture',culture)
    profile.setdefault('religion',faith)
    profile.setdefault('education','northern_commoner' if role not in ('child','dependent') else 'childhood')
    profile.setdefault('birthplaceLocationId','unknown')
    profile.setdefault('upbringingLocationId','winterfell')
    profile.setdefault('orientation','UNSPECIFIED')
    profile.setdefault('healthState','HEALTHY')
    profile.setdefault('legalStatus','LEGITIMATE')
    profile.setdefault('wealth',unit(RNG.gauss(.25,.12)))
    profile.setdefault('socialStatus',unit(RNG.gauss(.30,.12)))
    profile.setdefault('reputation',unit(RNG.gauss(.42,.08)))
    skilled={'farmer':'agriculture','livestock_stable':'agriculture','guard':'combat',
        'artisan_smith':'crafting','builder_carpenter':'crafting','trade':'trade',
        'transport':'navigation','healer_religious':'religious_study',
        'forester_hunter':'hunting','administration':'stewardship',
        'household_service':'stewardship','food_service':'crafting'}
    existing_skill=profile.get('skills',{})
    profile['skills']={k:unit(existing_skill.get(k,(0.0 if age<6 else RNG.uniform(.08,.36) if age<16 else RNG.uniform(.13,.56)) +(.24 if k==skilled.get(role) and age>=16 else 0))) for k in SKILLS}
    for field,keys,low,hi in [('aptitudes',APTITUDES,0.25,0.78),('dispositions',DISPOSITIONS,-0.65,.65),
                              ('characterValues',CHARACTER_VALUES,-.65,.65),('socialNorms',SOCIAL_NORMS,-.6,.6)]:
        base=profile.get(field,{})
        profile[field]={k:base.get(k,round(RNG.uniform(low,hi),3)) for k in keys}
    profile.setdefault('values',{'family':.65,'honor':.50,'duty':.45,'faith':.6})
    profile.setdefault('politicalPreferences',{})
    profile.setdefault('traits', ['curious','young'] if age<16 else [RNG.choice(['dutiful','patient','gregarious','reserved','brave','cautious']), RNG.choice(['industrious','frugal','generous','observant','stubborn','gentle'])])
    profile.setdefault('aliases',[])
    profile.setdefault('occupations',[] if role in ('child','dependent','retired') else [role])
    profile.setdefault('offices',[])
    profile.setdefault('courtRoles',[])
    profile.setdefault('militaryRoles',[])
    profile.setdefault('combatSpecialties',[])
    profile.setdefault('languages',{'common_tongue':1})
    profile.setdefault('motivations', ['Support the people of the household and live securely.'] if age>=16 else ['Learn and play while helping the household.'])
    profile.setdefault('goals', [])
    profile.setdefault('fears', [])
    profile.setdefault('desires', [])
    profile.setdefault('secrets', [])
    profile.setdefault('knownSecrets', [])
    profile.setdefault('publicFacts', {})
    profile.setdefault('privateFacts', {})
    profile.setdefault('appearance', {'description':'Northern resident; appearance not attested.'})
    profile.setdefault('dialogue', {'formality':0,'verbosity':0,'warmth':0,'directness':0,'preferredAddress':name,'guidance':'Speak according to occupation, age and local custom.'})
    return profile


def distribute(n,groups,minimum=1):
    if n<groups*minimum:raise ValueError('not enough persons to distribute')
    out=[minimum]*groups
    for _ in range(n-groups*minimum):
        # allow extended households; no impossible huge family unless communal assignment
        choices=[i for i,s in enumerate(out) if s<9]
        out[RNG.choice(choices)]+=1
    RNG.shuffle(out)
    return out


def create():
    OUT.mkdir(parents=True,exist_ok=True)
    named={id:read_named(id) for id in STARKS+VISITORS}
    default_eddard=named['eddard_stark']
    names_used=set(); people=[]; houses=[]; buildings=[]
    role_counts=collections.Counter(); birth_names=collections.Counter()
    identities_named={x['id']:x for x in named.values()}
    # Explicit canon named first; their profile upgrades are in a separately reviewed folder.
    for id,data in named.items():
        prof=full_profile(data['identity']['givenName'],298-data['identity']['birthYear'],
                          'visitor' if id in VISITORS else ('lord' if id=='eddard_stark' else 'dependent'),
                          data['identity']['sex'],existing=data)
        assert all(prof['skills'][k]==v for k,v in data['profile'].get('skills',{}).items())
        proposal=copy.deepcopy(data)
        proposal['profile']=prof
        # No changes to legacy schema and no silent post-canon event or secret additions.
        proposal['authoringVersion']=data.get('authoringVersion',1)+1
        save_json(OUT/'profile_proposals'/(id+'.json'),proposal)
    # Fill each zone from exactly specified demographics and worker occupation slots.
    slot_counter=itertools.count(1)
    for zone,spec in ZONES.items():
        age_slots=(['child']*spec['children']+['elder']*spec['elders']+
                   list(itertools.chain.from_iterable([role]*n for role,n in ROLES[zone].items())))
        assert len(age_slots)==sum(spec[k] for k in ('children','elders','workers'))
        RNG.shuffle(age_slots)
        # Reserve canonical Winterfell residents FIRST. Counts are no extra population.
        reserved=[]
        if zone=='castle':
            for id in STARKS:
                record=named[id]
                years=298-record['identity']['birthYear']
                age_cat='child' if years<=15 else 'elder' if years>=61 else 'worker'
                if age_cat=='worker':
                    for prefer in ({'eddard_stark':['administration'],
                                    'catelyn_stark':['household_service'],
                                    'theon_greyjoy':['other']}.get(id,['other'])):
                        if prefer in age_slots:age_slots.remove(prefer);break
                    else:raise AssertionError('worker capacity')
                elif age_cat=='elder':age_slots.remove('elder')
                else:age_slots.remove('child')
                reserved.append((id,record,years,prefer if age_cat=='worker' else 'child' if age_cat=='child' else 'retired'))
            for label,job,age in STAFF:
                group='child' if age<=15 else 'elder' if age>=61 else 'worker'
                if group=='worker':
                    priorities={'guard':'guard','stablehand':'livestock_stable','master_of_horse':'livestock_stable',
                      'smith':'artisan_smith','cook':'food_service','steward':'administration',
                      'maester':'healer_religious','septon':'healer_religious','septa':'healer_religious',
                      'master_at_arms':'guard','kennelmaster':'livestock_stable','storyteller':'other',
                      'noble_attendant':'household_service'}
                    pref=priorities.get(job,'household_service')
                    for target in [pref,'household_service','guard','other','food_service']:
                        if target in age_slots: age_slots.remove(target);break
                    else:raise AssertionError('staff worker capacity')
                else:age_slots.remove(group)
                reserved.append(('wf_named_'+''.join(c.lower() if c.isalnum() else '_' for c in label).strip('_'),
                    {'identity':{'givenName':label,'familyName':'',
                     'sex':'FEMALE' if label in ('Jeyne Poole','Beth Cassel','Septa Mordane','Palla','Old Nan') else 'MALE',
                     'birthYear':298-age}},age,
                     target if group=='worker' else 'child' if group=='child' else 'retired'))
        # Predeclare household sizes; Stark family/wards are a single named household, other
        # authored staff each gets separate deterministic primary household unless known relative.
        sizes=distribute(len(age_slots)+len(reserved),spec['households'])
        if zone=='castle':
            # Stark household consumes 9 slots; one special record.
            sizes[0]=max(9,sizes[0])
            # Restore exact population count via adjusting others with bounded minimum of 1.
            while sum(sizes)>spec['children']+spec['elders']+spec['workers']:
                idx=next(i for i in range(1,len(sizes)) if sizes[i]>1)
                sizes[idx]-=1
        zone_people=[]
        for i,slots in enumerate(sizes):
            household_id=f'wf_{zone}_h{i+1:03d}'
            family=FAMILY[(i+({'castle':0,'wintertown':17,'hinterland':33}[zone]))%len(FAMILY)]
            house={'id':household_id,'zone':zone,'primaryMembers':[],'status':'PLANNED','familyName':family,
                   'type':'noble_household' if zone=='castle' and i==0 else 'farm_household' if zone=='hinterland' else 'mixed_household',
                   'residenceBuildingId':f'wf_{zone}_b{i+1:03d}','population':slots,
                   'religion':'faith_of_the_seven' if RNG.random()<(.11 if zone=='wintertown' else .035) else 'old_gods'}
            houses.append(house)
            buildings.append({'id':house['residenceBuildingId'],'zone':zone,'purpose':'castle_residence' if zone=='castle' else 'farmstead' if zone=='hinterland' else 'townhouse',
                              'capacity':max(slots,slots+1),'occupants':slots,'householdId':household_id,'placed':False,
                              'buildingTemplate':'NORTHERN_STONE' if zone=='castle' else 'NORTH_RURAL_FARM' if zone=='hinterland' else 'NORTH_LOG_AND_STONE'})
        # special Stark family household, related staff households where supported.
        pos=0
        for id,record,age,role in reserved:
            if id in STARKS:
                hi=0
            else:
                relatives={b:a for a,b in STAFF_LINKS}
                known_relative=relatives.get(record['identity']['givenName'])
                located=next((i for i,c in enumerate(houses[-len(sizes):]) if any(
                    pp['name']==known_relative for pp in zone_people if pp['homeHouseholdId']==c['id'])),None)
                if located is not None and len(houses[-len(sizes)+located]['primaryMembers'])<sizes[located]:
                    hi=located
                else:
                    needs_pair=any(a==record['identity']['givenName'] for a,b in STAFF_LINKS)
                    hi=next(i for i in range(1,len(sizes)) if
                            sizes[i]-len(houses[-len(sizes)+i]['primaryMembers']) >= (2 if needs_pair else 1))
            h=houses[-len(sizes)+hi]
            sex=record['identity'].get('sex','UNSPECIFIED')
            given=record['identity']['givenName'];family=record['identity'].get('familyName','')
            nm=(given+' '+family).strip()
            profile=named[id]['profile'] if id in named else full_profile(nm,age,role,sex)
            role_actual='child' if age<16 else 'retired' if age>=61 else role
            person={'id':id,'name':nm,'sex':sex,'birthYear':298-age,'age':age,'zone':zone,
                    'status':'AUTHORED_SCENARIO' if id in named else 'BOOK_NAMED_PROPOSED_NOT_IN_SCENARIO',
                    'ageGroup':'child' if age<16 else 'elder' if age>=61 else 'worker',
                    'job':role_actual,'employerId':'house_stark' if zone=='castle' else None,
                    'homeHouseholdId':h['id'],'homeBuildingId':h['residenceBuildingId'],'lordAuthoredId':'eddard_stark',
                    'culture':profile.get('culture','northman'),'religion':profile.get('religion','old_gods'),
                    'traits':profile.get('traits',[]),'skills':profile.get('skills',{}),'personality':record.get('personality',{}),
                    'profile':profile if id in named else full_profile(nm,age,role,sex),
                    'biologicalFamilyUnverified':id not in named}
            h['primaryMembers'].append(id);zone_people.append(person);names_used.add(nm.lower())
        unfilled=[x for x in age_slots]
        # assign age types keeping at least one adult worker in every non-Stark household
        remaining_households=houses[-len(sizes):]
        for house in remaining_households:
            if len(house['primaryMembers'])>=house['population']:continue
            if 'child' in unfilled and len(unfilled)>0 and all(x=='child' for x in unfilled):break
            # first member is an adult, unless institution-like child staff household already exists.
            for a in list(unfilled):
                if a not in ('child','elder'):
                    unfilled.remove(a)
                    _add_commoner(zone,house,a,names_used,zone_people,slot_counter)
                    break
        for h in remaining_households:
            while len(h['primaryMembers'])<h['population']:
                a=unfilled.pop()
                _add_commoner(zone,h,a,names_used,zone_people,slot_counter)
        assert not unfilled
        assert len(zone_people)==spec['children']+spec['elders']+spec['workers']
        people.extend(zone_people)
    # Visitors: use existing authored NPCs, no home or resident population added.
    visit=[]
    for id in VISITORS:
        j=named[id]
        visit.append({'id':id,'name':j['identity']['givenName']+' '+j['identity']['familyName'],
                      'presentAtWinterfell':True,'residentOfWinterfell':False,'homeHouseholdId':j.get('social',{}).get('household'),
                      'origin':'AUTHORED_SCENARIO','housing':'guest_quarters' if id!='benjen_stark' else 'watch_guest_quarters'})
    assert len(people)==RESIDENTS and len({p['id'] for p in people})==RESIDENTS
    for z in ZONES:
        for role,quota in ROLES[z].items():
            got=sum(p['zone']==z and p['ageGroup']=='worker' and p['job']==role for p in people)
            assert got==quota,(z,role,got,quota)
    assert sum(len(h['primaryMembers']) for h in houses)==RESIDENTS
    assert all(len(h['primaryMembers'])==h['population'] for h in houses)
    assert sum(h['population'] for h in houses)==sum(ZONES[z][k] for z in ZONES for k in ('children','elders','workers'))
    assert len(set(p['homeBuildingId'] for p in people))==len(buildings)
    assert all(b['capacity']>=b['occupants'] for b in buildings)
    people_by_id={p['id']:p for p in people}
    for parent_name,child_name in STAFF_LINKS:
        parent=next(p for p in people if p['name']==parent_name)
        child=next(p for p in people if p['name']==child_name)
        assert parent['homeHouseholdId']==child['homeHouseholdId'],(parent_name,child_name)
    # Authoritative genealogy for existing scenario members remains in the genealogy service;
    # the following relations are export hints, not automatic mutation of live genealogy.
    for p in people:
        p['motherId']=None;p['fatherId']=None;p['guardianId']=None
        if p['id'] in ('robb_stark','sansa_stark','arya_stark','bran_stark','rickon_stark'):
            p['motherId']='catelyn_stark';p['fatherId']='eddard_stark'
        if p['id']=='jon_snow':
            p['publicAcknowledgedFatherId']='eddard_stark' # not biological claim
        if p['id']=='theon_greyjoy':p['guardianId']='eddard_stark'
        if p['name'] in [b for _,b in STAFF_LINKS]:
            known=next(a for a,b in STAFF_LINKS if b==p['name'])
            parent=next(q for q in people if q['name']==known)
            p['kinOrGuardianId']=parent['id']
        # Synthetic child parentage is offered only if plausible in the home household.
        if p['ageGroup']=='child' and p['status']=='GENERATED_CANDIDATE':
            companions=[people_by_id[q] for q in next(h['primaryMembers'] for h in houses if h['id']==p['homeHouseholdId']) if q!=p['id']]
            adults=[a for a in companions if a['age']>=p['age']+16 and a['age']<=66]
            mother=next((a for a in adults if a['sex']=='FEMALE'),None)
            father=next((a for a in adults if a['sex']=='MALE'),None)
            if mother:p['motherId']=mother['id']
            if father:p['fatherId']=father['id']
            if not mother and not father and adults:p['guardianId']=adults[0]['id']
    # Workplace allocations are real construction targets: no work is yet simulated.
    workbuilding_count=0
    capacities={'farmer':6,'guard':24,'livestock_stable':8,'forester_hunter':6,
      'artisan_smith':5,'builder_carpenter':7,'food_service':8,'household_service':12,
      'administration':14,'healer_religious':5,'other':8,'trade':6,'transport':8}
    for z in ZONES:
        for role in ROLES[z]:
            employed=[p for p in people if p['zone']==z and p['ageGroup']=='worker' and p['job']==role]
            cap=capacities[role]
            for i in range(0,len(employed),cap):
                wid=f'wf_work_{z}_{role}_{i//cap+1:03d}'
                group=employed[i:i+cap]
                buildings.append({'id':wid,'zone':z,'purpose':'workplace:'+role,'capacity':0,
                    'occupants':0,'workplaceCapacity':cap,'assignedWorkers':len(group),
                    'placed':False,'buildingTemplate':'NORTH_ROLE_'+role.upper(),'householdId':None})
                for worker in group:worker['workBuildingId']=wid
                workbuilding_count+=1
    assert sum(b.get('assignedWorkers',0) for b in buildings)==744
    assert all(p.get('workBuildingId') for p in people if p['ageGroup']=='worker')
    # Wintertown has unused seasonal stock rather than creating a phantom winter population.
    for i in range(len([b for b in buildings if b['purpose']=='townhouse'])+1,321):
        buildings.append({'id':f'wf_wintertown_seasonal_{i:03d}','zone':'wintertown',
                          'purpose':'seasonal_residence_vacant','capacity':5,'occupants':0,'householdId':None,
                          'placed':False,'buildingTemplate':'NORTH_LOG_AND_STONE'})
    living_town=sum(b['purpose']=='townhouse' for b in buildings)
    assert living_town/320<.2
    # Role ledger is exact, named staff roles are categorized separately until occupational mapping.
    census=collections.Counter((p['zone'],p['ageGroup']) for p in people)
    # Case-study output is only a candidate roster; NEVER silently bootstrap full 1,200 into NpcRegistry.
    payload={'schemaVersion':1,'scenario':'agot_298_ac','seed':SEED,'status':'DRAFT_DO_NOT_BOOTSTRAP',
             'populationMethod':'MOD_CALIBRATION_NOT_CANON','residentTotal':RESIDENTS,'residents':people,'visitors':visit,
             'households':houses,'buildings':buildings,'populationConservation':{'residentIndividuals':len(people),'cohorts':0,'visitorsExcluded':len(visit)}}
    save_json(RESOURCE,payload)
    save_json(OUT/'CENSUS_AND_CAPACITY.json',{'people':RESIDENTS,'residentialUnitsOccupied':len(houses),
              'allBuildingSlots':len(buildings),'townSummerOccupiedHomes':living_town,'townTotalHomes':320,
              'zoneDemographics':{z:{k:sum(p['zone']==z and p['ageGroup']==k for p in people) for k in ('child','worker','elder')} for z in ZONES},
              'status':'AUTHORING_CALIBRATION_NOT_CANON'})
    # Command demo: 12 displayed actual source characters + proposed named staff/commoners.
    selections=[*people[:9]]
    for p in people:
        if p['status']=='BOOK_NAMED_PROPOSED_NOT_IN_SCENARIO' and len(selections)<18:selections.append(p)
    for p in people:
        if p['status']=='GENERATED_CANDIDATE' and p['zone']=='wintertown' and len(selections)<22:selections.append(p)
    for p in people:
        if p['status']=='GENERATED_CANDIDATE' and p['zone']=='hinterland' and len(selections)<24:selections.append(p)
    assert len(selections)==24
    # All test villagers have stable authored tags, never a duplicated NPC registry.
    lines=['# TEST-ONLY: call once in a disposable superflat world.','say Winterfell case study: placing 24 non-authoritative actor PROXIES.']
    for i,p in enumerate(selections):
        tag='kwfid_'+p['id']
        dx=(i%6)*4; dz=(i//6)*5
        # Visible proxy: interaction handler reads kwfid_* and the associated stable draft JSON record.
        # Repeated invocations don't duplicate at same local location within 64 blocks.
        display=json.dumps({'text':p['name']},separators=(',',':'))
        snbt=display.replace("'","\\'")
        lines.append(f'execute unless entity @e[type=minecraft:villager,tag={tag},distance=..80] run summon minecraft:villager ~{dx} ~ ~{dz} '+
                     '{Tags:["kwf_case","'+tag+'"],PersistenceRequired:1b,NoAI:1b,CustomName:\''+snbt+'\',CustomNameVisible:1b}')
    FUNC.mkdir(parents=True,exist_ok=True)
    (FUNC/'winterfell_npcs.mcfunction').write_text('\n'.join(lines)+'\n','utf8')
    with (OUT/'residents.csv').open('w',newline='',encoding='utf8') as fd:
        w=csv.DictWriter(fd,fieldnames=['id','name','zone','age','ageGroup','job','homeHouseholdId','homeBuildingId','culture','religion','status']);w.writeheader();w.writerows({k:p.get(k) for k in w.fieldnames} for p in people)
    with (OUT/'households.csv').open('w',newline='',encoding='utf8') as fd:
        w=csv.DictWriter(fd,fieldnames=['id','zone','population','type','residenceBuildingId']);w.writeheader();w.writerows({k:h.get(k) for k in w.fieldnames} for h in houses)
    print(json.dumps({'residents':len(people),'named_main_scenario':len(STARKS),'visitors_excluded':len(visit),
                     'households':len(houses),'buildings':len(buildings),'workplaces':workbuilding_count,'winterTownOccupiedHomes':living_town,
                     'showcaseProxies':len(selections),'zoneDemographics':{z:dict((a,census[z,a]) for a in ('child','worker','elder')) for z in ZONES}},indent=2))


def _add_commoner(zone,h,ageclass,used,result,count):
    id=f'wf298_{next(count):05d}'
    male_prior={'guard':.9,'builder_carpenter':.78,'artisan_smith':.76,
        'forester_hunter':.74,'transport':.73,'livestock_stable':.66,
        'administration':.65,'household_service':.25,'food_service':.42,
        'trade':.58,'healer_religious':.40,'farmer':.5,'other':.5}
    sex='MALE' if RNG.random()<male_prior.get(ageclass,.5) else 'FEMALE'
    if ageclass=='child':age=RNG.randint(0,15);role='child'
    elif ageclass=='elder':age=RNG.randint(61,82);role='retired'
    else:age=RNG.randint(16,60);role=ageclass
    given_pool=FIRST_M if sex=='MALE' else FIRST_F
    for attempt in range(1000):
        first=RNG.choice(given_pool)
        surname=h['familyName']
        name=f'{first} {surname}'
        if name.lower() not in used:break
    else:raise RuntimeError('name pool exhausted')
    used.add(name.lower())
    faith=h['religion']
    prof=full_profile(name,age,role,sex,faith)
    record={'id':id,'name':name,'sex':sex,'birthYear':298-age,'age':age,'zone':zone,'status':'GENERATED_CANDIDATE',
      'ageGroup':'child' if ageclass=='child' else 'elder' if ageclass=='elder' else 'worker','job':role,
      'employerId':None,'homeHouseholdId':h['id'],'homeBuildingId':h['residenceBuildingId'],
      'lordAuthoredId':'eddard_stark','culture':prof['culture'],'religion':prof['religion'],
      'traits':prof['traits'],'skills':prof['skills'],'personality':{k:bounded(RNG.gauss(0,.4)) for k in PERSONALITY},
      'profile':prof,'biologicalFamilyUnverified':True}
    h['primaryMembers'].append(id);result.append(record)

if __name__=='__main__':create()
