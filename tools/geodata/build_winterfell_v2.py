#!/usr/bin/env python3
"""Winterfell V2 test-world planner. Deterministic, test-only, no Minecraft blocks modified.
Run from repository root: py tools/geodata/build_winterfell_v2.py
Output: data/knownworld/case_study/winterfell_v2.json (5000 residents + all plots).
No external dependencies; authoritative named profiles remain in existing scenario resources.
"""
from pathlib import Path
import json, math, random, hashlib, collections, heapq

ROOT=Path(__file__).resolve().parents[2]
OUTPUT=ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
REPORT=ROOT/'docs/step3/WINTERFELL_V2_MANIFEST.json'
SEED=2985000
POPULATION=5000
SEASONAL_UNUSED_HOMES=0 # PROTOTYPE: only assigned occupied homes are physically planned; winter reserves future phase
NAMED=['eddard_stark','catelyn_stark','robb_stark','sansa_stark','arya_stark','bran_stark','rickon_stark','jon_snow','theon_greyjoy']
VISITORS=['robert_baratheon','cersei_lannister','jaime_lannister','tyrion_lannister','benjen_stark']
STAFF=[('Maester Luwin','HEALER'),('Rodrik Cassel','GUARD'),('Jory Cassel','GUARD'),('Vayon Poole','ADMIN'),('Jeyne Poole','DEPENDENT'),('Septa Mordane','HEALER'),('Septon Chayle','HEALER'),('Hullen','STABLE'),('Harwin','GUARD'),('Hodor','STABLE'),('Old Nan','DEPENDENT'),('Gage','COOK'),('Mikken','SMITH')]
FIRST_M='Brandon Jonos Torrhen Harl Eddric Ronnel Willam Ethan Gared Lew Robyn Garth Pate Joss Hugh Rickard Harwood Osmund Rodrik Lorren Lymond Tomard Wyl Donnel Hother Halys Benjen Hullen Brynden Joram Joseth Cregan Theo Cedric Corben Walder Olyvar Parmen Timmard Bennard'.split()
FIRST_F='Alys Beth Jeyne Mira Wylla Mara Sarra Dacey Roslin Marya Lyra Ellyn Rowena Bessa Tansy Erena Nella Willa Wynna Selyse Myranda Walda Sybelle Gella Catelyn Talla Lysa Wenna Elyn Myrielle Janna Elysa Kyra Alysanne Leona'.split()
SURNAMES='Northwood Flintwell Ashbank Hearth Wool Moss Northfield Fenbrook Dale Oakham Hollow Snowbrook Timber Rill Hale Briar Holt Millward Beck Sedge Winterton Barley Tallow Thorn Ashgrove Wick Underhill Longford Fenn Croft Mead Farrow Greystone Harrow Mander Claye Yarrow Ward Hay Branwell Warren Blackbriar Rowan Brooks Snowden Fenwick Marshbank Flint Brook Ford Claye Reedbank Greycliff Norwood Stonehill Coldwell Fallow Hoar Frostwell Hemming Harrowby Woolcott Bramble'.split()
# Household distributions: 76 + 240 + 4*115 + 15*24 = 1136 occupied units.
SITES=[{'id':'castle','tier':'CASTLE','cx':0,'cz':0,'homes':76,'occupation':'MIXED'} ,
       {'id':'wintertown','tier':'10X','cx':520,'cz':0,'homes':240,'occupation':'MIXED'}]
for i,(x,z) in enumerate([(1800,-800),(2200,500),(1450,1500),(-1200,1600)],1):
    SITES.append({'id':f'village5_{i:02}','tier':'5X','cx':x,'cz':z,'homes':115,'occupation':'MIXED'})
# Fifteen sparse 1x settlements distributed beyond major roads; fixed centers are local relative offsets.
for i in range(15):
    a=2*math.pi*i/15
    rad=2700+350*(i%3)
    SITES.append({'id':f'hamlet_{i+1:02}','tier':'1X','cx':int(math.cos(a)*rad),'cz':int(math.sin(a)*rad),'homes':24,'occupation':'RURAL'})

WORKS={'FARMER':(0.39,'FARMHOUSE'),'LIVESTOCK':(.09,'FARMHOUSE'),'FORESTER':(.06,'FARMHOUSE'),
       'SMITH':(.035,'WORKSHOP'),'CARPENTER':(.045,'WORKSHOP'),'MASON':(.025,'WORKSHOP'),
       'MERCHANT':(.075,'MERCHANT'),'TRANSPORT':(.055,'MERCHANT'),
       'GUARD':(.09,'GUARD'),'SERVANT':(.10,'COTTAGE'),
       'COOK':(.04,'COTTAGE'),'HEALER':(.025,'COTTAGE'),'ADMIN':(.03,'MERCHANT')}
# Weighted occupation prior is an inference, not canon. Includes only adults with primary jobs.
WEIGHTS=[(k,w) for k,(w,_) in WORKS.items()]
WEIGHT_TOTAL=sum(w for _,w in WEIGHTS)

def stable_rng(*parts):
    seed=int.from_bytes(hashlib.sha256(('|'.join(map(str,parts))).encode()).digest()[:8],'big')
    return random.Random(seed)
def name_for(person_id,family,sex,used):
    r=stable_rng(SEED,person_id);first=r.choice(FIRST_F if sex=='FEMALE' else FIRST_M)
    name=f'{first} {family}'
    if name.casefold() in used:
        middle=r.choice(['of the Frost','of Winterfell','the Younger','the Elder','the Quiet','of the Wolfswood','the Red','of the Rills'])
        name=f'{first} {family} {middle}'
        idx=1
        while name.casefold() in used:
            idx+=1;name=f'{first} {family} {middle} {idx}'
    used.add(name.casefold())
    return name

def choose_job(r,site):
    pairs=WEIGHTS[:]
    if site['tier']=='CASTLE':
        boost={'GUARD':2.8,'SERVANT':3.5,'ADMIN':2.8,'COOK':2.2,'FARMER':.2,'LIVESTOCK':.5,'FORESTER':.2}
    elif site['tier']=='1X': boost={'FARMER':2,'LIVESTOCK':1.7,'ADMIN':.1,'MERCHANT':.35,'GUARD':.4}
    else: boost={'FARMER':.65,'MERCHANT':1.45,'SMITH':1.3,'CARPENTER':1.3}
    return r.choices([k for k,_ in pairs],[w*boost.get(k,1) for k,w in pairs],k=1)[0]

def slot_homes(site):
    """Connected rectilinear roads: each front porch faces a row lane; west spine joins all row lanes.
    Not a vanilla Jigsaw village. Templates are role-aware vanilla BLOCKS.
    """
    n=site['homes']
    if site['tier']=='CASTLE':
        # 76 apartments/sleeping rooms in the extended castle wards: 4 wings of 19;
        # plot locations are portal-to-room metadata rather than 76 detached houses.
        return [{'id':f'{site["id"]}_home_{i+1:04}', 'site':site['id'],'template':'CASTLE_QUARTER',
                 'x':site['cx']-135+(i%19)*14,'z':site['cz']-98+(i//19)*44,
                 'w':11,'d':12,'accessX':site['cx']-135+(i%19)*14+5,
                 'accessZ':site['cz']-98+(i//19)*44+12,'status':'PLANNED'} for i in range(n)]
    cols=max(4,int(math.ceil(math.sqrt(n)*1.45)))
    startx=site['cx']-(cols*18)//2
    rows=math.ceil(n/cols)
    startz=site['cz']-(rows*24)//2
    plots=[]
    for i in range(n):
        col=i%cols;row=i//cols
        x=startx+col*18; z=startz+row*24
        plots.append({'id':f'{site["id"]}_home_{i+1:04}', 'site':site['id'],
                      'template':'COTTAGE','x':x,'z':z,'w':12,'d':11,'accessX':x+6,'accessZ':z+12,'status':'PLANNED'})
    return plots

def line_road(road,x1,z1,x2,z2):
    if x1!=x2 and z1!=z2: raise ValueError('roads must be orthogonal')
    if x1==x2:
        for zz in range(min(z1,z2),max(z1,z2)+1):road.add((x1,zz))
    else:
        for xx in range(min(x1,x2),max(x1,x2)+1):road.add((xx,z1))

def plan():
    plots=[];site_by_id={s['id']:s for s in SITES}
    roads=set()
    for site in SITES:
        group=slot_homes(site)
        plots.extend(group)
        if site['tier']=='CASTLE':
            # Castle has an internal courtyard/wing roads and a gateway to the town.
            line_road(roads,site['cx']-142,site['cz']+15,site['cx']+158,site['cz']+15)
            line_road(roads,site['cx']+142,site['cz']+15,site['cx']+142,site['cz']+70)
            line_road(roads,site['cx']+158,site['cz']+15,site['cx']+158,site['cz'])
            # All four rows of castle sleeping quarters have frontage lanes.
            for row in range(4):
                lane_z=-98+row*44+13
                line_road(roads,-142,lane_z,142,lane_z)
            line_road(roads,-142,-85,-142,70)
            continue
        cols=max(4,int(math.ceil(math.sqrt(site['homes'])*1.45)))
        rows=math.ceil(site['homes']/cols)
        sx=group[0]['x']; sz=group[0]['z']
        spine_x=sx-6
        for row in range(rows):
            lanez=sz+row*24+13
            line_road(roads,spine_x,lanez,sx+(cols-1)*18+9,lanez)
        line_road(roads,spine_x,sz+13,spine_x,sz+(rows-1)*24+13)
        for plot in group:
            # Door is on the south wall (z + depth - 1). Connect its outside
            # step directly to the row lane; do not leave two grass gaps.
            line_road(roads,plot['accessX'],plot['z']+plot['d'],plot['accessX'],plot['accessZ']+1)
    # All inter-site roads connect via a house-avoiding route to the castle gateway.
    # Straight L-routes are preferred; A* detours only when an L-route intersects a house.
    blocked=set()
    for home in plots:
        for xx in range(home['x']-1,home['x']+home['w']+1):
            for zz in range(home['z']-1,home['z']+home['d']+1):
                blocked.add((xx,zz))
    # Permit the deliberate front-door approach through the one-cell
    # pathfinding buffer that otherwise keeps major roads off house walls.
    for plot in plots:
        if plot['site']!='castle':
            for zz in range(plot['z']+plot['d'],plot['accessZ']+2):
                blocked.discard((plot['accessX'],zz))
    def simple_path(a,b,orientation):
        ax,az=a;bx,bz=b
        mid=(bx,az) if orientation=='horizontal' else (ax,bz)
        pts=[]
        for (x1,z1),(x2,z2) in ((a,mid),(mid,b)):
            if x1==x2:pts.extend((x1,z) for z in range(min(z1,z2),max(z1,z2)+1))
            else:pts.extend((x,z1) for x in range(min(x1,x2),max(x1,x2)+1))
        return pts
    def avoid_houses(start,goal):
        for orientation in ('horizontal','vertical'):
            route=simple_path(start,goal,orientation)
            if not any(pt in blocked for pt in route):return route
        # A* with 4-neighbour movement; bound search to a modest corridor.
        # A slightly higher cost for new road gently reuses prior track segments.
        pad=150
        xmin=min(start[0],goal[0])-pad;xmax=max(start[0],goal[0])+pad
        zmin=min(start[1],goal[1])-pad;zmax=max(start[1],goal[1])+pad
        queue=[(abs(start[0]-goal[0])+abs(start[1]-goal[1]),0,start)]
        dist={start:0};previous={}
        while queue:
            _,g,pos=heapq.heappop(queue)
            if g!=dist.get(pos):continue
            if pos==goal:
                result=[pos]
                while pos!=start:pos=previous[pos];result.append(pos)
                return result[::-1]
            x,z=pos
            for nxt in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                nx,nz=nxt
                if nx<xmin or nx>xmax or nz<zmin or nz>zmax or nxt in blocked:continue
                ng=g+1
                if ng<dist.get(nxt,10**15):
                    dist[nxt]=ng;previous[nxt]=pos
                    f=ng+abs(nx-goal[0])+abs(nz-goal[1])
                    heapq.heappush(queue,(f,ng,nxt))
        raise AssertionError(f'No feasible road route: {start} -> {goal}')
    gate=(158,15)
    assert gate not in blocked
    for site in SITES[1:]:
        group=next(p for p in plots if p['site']==site['id'])
        spine=(group['x']-6,group['z']+13)
        # Use a remote spine starting point; each village road is linked by its local rows.
        for point in avoid_houses(gate,spine):roads.add(point)
    assert not any(pt in blocked for pt in roads), 'Road crosses a physical home footprint'
    # Edges and plot invariants: all remote homes are connected to this road graph.
    # 5000 people allocated to households, no phantom pop; the roster does not count visitors.
    assert len(plots)==sum(x['homes'] for x in SITES)
    assert len({p['id'] for p in plots})==len(plots)
    # Assign role-differentiated housing and 5000 exact residents.
    household_sizes=[4]*len(plots)
    r=stable_rng(SEED,'households')
    rest=POPULATION-sum(household_sizes)
    assert rest>0
    while rest:
        options=[i for i in range(len(plots)) if household_sizes[i]<7]
        i=r.choice(options);household_sizes[i]+=1;rest-=1
    # Reserve a 9-person Stark household by redistributing member slots.
    first=0
    additional=9-household_sizes[first]
    household_sizes[first]=9
    for i in range(1,len(plots)):
        if additional<=0:break
        if household_sizes[i]>3:household_sizes[i]-=1;additional-=1
    assert additional==0 and sum(household_sizes)==POPULATION
    household_data=[]; people=[];used=set();gen_id=1
    named_data={}
    for key in NAMED:
        p=ROOT/f'src/main/resources/data/knownworld/scenarios/agot_298_ac/characters/{key}.json'
        if p.exists():named_data[key]=json.loads(p.read_text('utf8'))
    if len(named_data)!=len(NAMED):
        raise FileNotFoundError('Expected all nine original authored Stark household character JSON files in your repo. Do NOT overwrite them.')
    for hidx,plot in enumerate(plots):
        site=site_by_id[plot['site']];rng=stable_rng(SEED,'home',plot['id']);n=household_sizes[hidx]
        kind=('NOBLE' if hidx==0 else 'GUARD' if site['tier']=='CASTLE' and hidx%5==0
              else 'FARM' if site['tier']=='1X' or (site['tier']=='5X' and hidx%3==0)
              else rng.choices(['FARM','CRAFT','MERCHANT','LABOR'],[3,2,1,3])[0])
        template={'NOBLE':'CASTLE_QUARTER','GUARD':'GUARD_QUARTER','FARM':'FARMHOUSE','CRAFT':'WORKSHOP','MERCHANT':'MERCHANT','LABOR':'COTTAGE'}[kind]
        plot['template']=template if site['tier']!='CASTLE' else 'CASTLE_QUARTER'
        plot['capacity']=max(n,9 if hidx==0 else 7)
        plot['occupants']=n
        home_id=plot['id']
        house_id=f'wf2_h{hidx+1:04}'
        family=('Stark' if hidx==0 else SURNAMES[(hidx*41)%len(SURNAMES)])
        if hidx==0:
            # First nine slots are EXISTING authored persons, not generated duplicates.
            ids=NAMED[:]
            for key in ids:
                jd=named_data[key];ident=jd['identity'];age=298-int(ident['birthYear'])
                pr=jd.get('profile',{})
                people.append({'id':key,'name':ident['givenName']+' '+ident['familyName'],'sex':ident['sex'],
                               'birthYear':ident['birthYear'],'age':age,'householdId':house_id,
                               'homeId':home_id,'site':site['id'],'status':'AUTHORED_EXISTING',
                               'job':('LORD' if key=='eddard_stark' else 'LADY' if key=='catelyn_stark'
                               else 'WARD' if key=='theon_greyjoy' else 'DEPENDENT'),
                               'culture':pr.get('culture','northman'),'religion':pr.get('religion','old_gods'),
                               'traits':[],'personality':{},'skills':{},
                               'profileSource':'LIVE_EXISTING_AUTHORED_PROFILE',
                               'workId':'castle_keep'})
        else:
            ids=[]
            # plausible household: 1-2 adults, children/elders, same inherited surname.
            adults=min(n,2 if rng.random()<.78 else 3)
            # 3+ adults can be extended relatives; do NOT assert marriages without evidence.
            for m in range(n):
                person_id=f'wf2_gen_{gen_id:05d}';gen_id+=1
                staff_entry=STAFF[hidx-1] if site['tier']=='CASTLE' and m==0 and 1<=hidx<=len(STAFF) else None
                if staff_entry:person_id='wf2_book_'+''.join(c.lower() if c.isalnum() else '_' for c in staff_entry[0]).strip('_')
                sex='FEMALE' if (m==1 or rng.random()<.48) else 'MALE'
                if m<adults:age=rng.randint(19,59)
                elif rng.random()<.17:age=rng.randint(60,86)
                else:age=rng.randint(0,18)
                job=choose_job(rng,site) if 17<=age<=64 else 'CHILD' if age<17 else 'DEPENDENT'
                name=name_for(person_id,family,sex,used)
                if staff_entry:
                    name=staff_entry[0];job=staff_entry[1]
                    if name in ('Jeyne Poole','Septa Mordane','Old Nan'):sex='FEMALE'
                    if name=='Old Nan':age=82
                    if name=='Jeyne Poole':age=11
                personality={trait:round(max(-1,min(1,rng.gauss(0,.37))),3) for trait in ('courage','ambition','compassion','honor','patience','sociability')}
                traits=rng.sample(['brave','dutiful','cautious','patient','frugal','generous','industrious','reserved','curious','protective','sociable','stubborn','pious','worldly','honorable','suspicious'],3)
                key_skill={'FARMER':'agriculture','LIVESTOCK':'agriculture','FORESTER':'hunting','SMITH':'crafting','CARPENTER':'crafting','MASON':'crafting','MERCHANT':'trade','TRANSPORT':'navigation','GUARD':'combat','SERVANT':'stewardship','COOK':'crafting','HEALER':'medicine','ADMIN':'stewardship'}
                skill_map={k:round(rng.uniform(.08,.45),3) for k in ('martial','diplomacy','stewardship','intrigue','learning','combat','leadership','riding','trade','crafting','agriculture','hunting','medicine','navigation','logistics')}
                if age>16 and job in key_skill:skill_map[key_skill[job]]=round(rng.uniform(.48,.88),3)
                # Stable deterministic family links, unconfirmed biology marked inferred.
                parent_candidates=ids[:2] if m>=adults else []
                member={'id':person_id,'name':name,'sex':sex,'birthYear':298-age,'age':age,
                        'householdId':house_id,'homeId':home_id,'site':site['id'],'job':job,
                        'status':'BOOK_NAMED_PROPOSAL' if staff_entry else 'GENERATED_MOD_CALIBRATION','culture':'northman',
                        'religion':('faith_of_the_seven' if rng.random()<.1 else 'old_gods'),
                        'traits':traits,'personality':personality,'skills':skill_map,
                        'parentCandidates':parent_candidates,'parentageEvidence':'INFERRED_ONLY',
                        'workId':('castle_keep' if job in ('GUARD','SERVANT','ADMIN','COOK','LORD','LADY') and site['tier']=='CASTLE'
                        else f'{site["id"]}_{job.lower()}_work')}
                people.append(member);ids.append(person_id)
        household_data.append({'id':house_id,'site':site['id'],'residence':home_id,'type':kind,'members':ids,'capacity':plot['capacity']})
    # Generate only AGE-PLAUSIBLE parentage links within each household.
    # These are *inferred test-family relationships*, not book canon.
    by_id={person['id']:person for person in people}
    for household in household_data[1:]:
        members=[by_id[ident] for ident in household['members']]
        for child in members:
            if child['age']>=17 or child['status']!='GENERATED_MOD_CALIBRATION':continue
            possible=[p for p in members if p['age']>=child['age']+16 and p['age']<=74]
            mother=next((p for p in possible if p['sex']=='FEMALE'),None)
            father=next((p for p in possible if p['sex']=='MALE'),None)
            child['motherId']=mother['id'] if mother else None
            child['fatherId']=father['id'] if father else None
            child['parentageEvidence']='MODELED_INFERRED_TEST_ONLY'
            child['parentCandidates']=[i for i in (child['motherId'],child['fatherId']) if i]
    # Add physical workplace plots, on road-connected service rows; one worker is
    # assigned to one named workshop, stable or field depot (12 workers maximum).
    workplaces=[]
    for site in SITES:
        members=sorted((p for p in people if p['site']==site['id'] and p['job'] not in ('CHILD','DEPENDENT','WARD','LORD','LADY')),key=lambda p:p['id'])
        groups={}
        for person in members:groups.setdefault(person['job'],[]).append(person)
        workunits=[]
        for role,workers in sorted(groups.items()):
            for start in range(0,len(workers),12):workunits.append((role,workers[start:start+12]))
        if not workunits:continue
        if site['tier']=='CASTLE':
            coords=[(x,z) for z in (81,105,129,153) for x in (-134,-116,-98,-80,-62,62,80,98,116,134)]
            assert len(workunits)<=len(coords)
            work_positions=coords[:len(workunits)]
        else:
            home_group=[p for p in plots if p['site']==site['id']]
            cols=max(4,int(math.ceil(math.sqrt(site['homes'])*1.45)))
            startx=home_group[0]['x'];maxz=max(p['z']+p['d'] for p in home_group)
            work_positions=[(startx+(i%cols)*18,maxz+23+(i//cols)*24) for i in range(len(workunits))]
        for i,((role,workers),(wx,wz)) in enumerate(zip(workunits,work_positions)):
            wid=f'wf2_{site["id"]}_work_{i+1:03d}'
            if site['tier']=='CASTLE':
                # Castle workrooms connect to the western interior road and keep doorway.
                if wx<0:
                    line_road(roads,-142,wz+13,wx+6,wz+13)
                    line_road(roads,-142,15,-142,wz+13)
                else:
                    line_road(roads,156,wz+13,wx+6,wz+13)
                    line_road(roads,156,15,156,wz+13)
            else:
                home_group=[p for p in plots if p['site']==site['id']]
                sx=home_group[0]['x']-6; basez=home_group[0]['z']+13
                lane_z=wz+13
                line_road(roads,sx,basez,sx,lane_z)
                line_road(roads,sx,lane_z,wx+6,lane_z)
            template={'SMITH':'WORKSHOP','CARPENTER':'WORKSHOP','MASON':'WORKSHOP',
                      'MERCHANT':'MERCHANT','TRANSPORT':'MERCHANT','FARMER':'FARMHOUSE',
                      'LIVESTOCK':'FARMHOUSE','FORESTER':'FARMHOUSE',
                      'GUARD':'GUARD_QUARTER','HEALER':'COTTAGE',
                      'ADMIN':'MERCHANT','SERVANT':'COTTAGE','COOK':'COTTAGE'}.get(role,'COTTAGE')
            # Connect the workplace's entrance to its dedicated service lane.
            line_road(roads,wx+6,wz+11,wx+6,wz+13)
            workplaces.append({'id':wid,'site':site['id'],'template':template,
                  'x':wx,'z':wz,'w':12,'d':11,'capacity':0,'occupants':0,
                  'workforceCapacity':12,'assignedWorkers':len(workers),'role':role})
            for person in workers:person['workId']=wid
    assert sum(p['job'] not in ('CHILD','DEPENDENT','WARD','LORD','LADY') for p in people)==sum(w['assignedWorkers'] for w in workplaces)
    all_buildings=plots+workplaces
    assert len({b['id'] for b in all_buildings})==len(all_buildings)
    # Collision validation includes workshops/forges as well as dwellings.
    cells=set()
    for b in all_buildings:
        for px in range(b['x'],b['x']+b['w']):
            for pz in range(b['z'],b['z']+b['d']):
                assert (px,pz) not in cells,(b['id'],px,pz)
                cells.add((px,pz))
    # Doors/roads remain free of building footprints.
    roads.difference_update(cells)
    assert len(people)==POPULATION and len({p['id'] for p in people})==POPULATION
    assert len(household_data)==len(plots)
    assert sum(len(h['members']) for h in household_data)==POPULATION
    # Avoid treating book-named staff as random: future authored addition after review.
    # Visitor positions/rooms planned in castle, without adding to 5k census.
    visitor_recs=[{'id':k,'residence':'external','presence':'TEMPORARY_ROYAL_VISIT','homeCountedHere':False} for k in VISITORS]
    # Precompute flattened road list; main roads one tile wide, rendered 3 wide by runtime.
    road_coords=sorted(roads)
    manifest={'schemaVersion':2,'scenario':'agot_298_ac','seed':SEED,
              'residentCount':POPULATION,'householdCount':len(household_data),'residentialPlotCount':len(plots),
              'castleCount':1,'village1xCount':15,'village5xCount':4,'village10xCount':1,
              'roadPoints':len(road_coords),'workplacePlots':len(workplaces),'seasonalWintertownReserve':'NOT_INSTALLED_REQUIRES_SEASONAL_MIGRATION_PLAN',
              'placement':'RELATIVE_TO_EXPLICIT_TEST_ORIGIN_NOT_GEOGRAPHIC_WINTERFELL',
              'status':'TEST_VERTICAL_SLICE_CANDIDATE_NOT_CANON','activeNpcBudget':128,'reducedNpcBudget':128,
              'sitePopulations':{site['id']:sum(len(h['members']) for h in household_data if h['site']==site['id']) for site in SITES}}
    payload={'manifest':manifest,'sites':SITES,'homes':plots,'roads':[[a,b] for a,b in road_coords],
             'households':household_data,'workplaces':workplaces,'residents':people,'visitors':visitor_recs}
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    OUTPUT.write_text(json.dumps(payload,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf8')
    REPORT.parent.mkdir(parents=True,exist_ok=True)
    REPORT.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
    print(json.dumps(manifest,indent=2))

if __name__=='__main__':plan()
