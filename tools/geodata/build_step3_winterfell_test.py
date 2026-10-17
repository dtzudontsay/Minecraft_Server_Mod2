#!/usr/bin/env python3
"""Build an EXPLICITLY INVOKED, TEST-WORLD-ONLY Winterfell voxel pilot.
Minecraft 26.3 functions are data/knownworld/function/*.mcfunction (singular).
No auto spawning, no NPCs, no claimed terrain survey, no overwrite protection.
The produced functions must ONLY be used in a disposable FLAT CREATIVE TEST world.
Run: py tools/geodata/build_step3_winterfell_test.py
"""
from __future__ import annotations
from pathlib import Path
import json, random, math
ROOT=Path(__file__).resolve().parents[2]
FUNCS=ROOT/'src/main/resources/data/knownworld/function/step3/winterfell'
REPORT=ROOT/'docs/step3'
FUNCS.mkdir(parents=True,exist_ok=True)
REPORT.mkdir(parents=True,exist_ok=True)
P={k:[] for k in ['00_test_pad','01_footings','02_outer_defenses','03_inner_defenses','04_great_hall_and_keep','05_godswood_gardens','06_service_buildings','07_winter_town','08_finishing']}
plots=[]
random.seed(298)

def emit(phase,text): P[phase].append(text)
def rel(v):return '~' if v==0 else f'~{v}'
def f(phase,x1,y1,z1,x2,y2,z2,block):
    # split volumes into ~8000-block slabs along x/z, below vanilla 32768 limit
    a,b=sorted([int(x1),int(x2)]);c,d=sorted([int(y1),int(y2)]);e,g=sorted([int(z1),int(z2)])
    dy=d-c+1;dz=g-e+1
    for xx in range(a,b+1,max(1,8000//max(1,dy*dz))):
        xend=min(b,xx+max(1,8000//max(1,dy*dz))-1)
        if (xend-xx+1)*dy*dz<=8000:
            emit(phase,f'fill {rel(xx)} {rel(c)} {rel(e)} {rel(xend)} {rel(d)} {rel(g)} minecraft:{block}')
        else:
            step=max(1,8000//dy)
            for zz in range(e,g+1,step):
                zend=min(g,zz+step-1)
                for xi in range(xx,xend+1):
                    emit(phase,f'fill {rel(xi)} {rel(c)} {rel(zz)} {rel(xi)} {rel(d)} {rel(zend)} minecraft:{block}')
def s(phase,x,y,z,block):emit(phase,f'setblock {rel(int(x))} {rel(int(y))} {rel(int(z))} minecraft:{block}')

def plot(ident,kind,rect,style,capacity=None,status='ACTIVE_STRUCTURE_TEST'):
    a,c,b,d=rect
    plots.append(dict(id='winterfell_'+ident,kind=kind,bounds=dict(xmin=a,xmax=b,zmin=c,zmax=d),style=style,
                      estimated_capacity=capacity,status=status,source='AUTHORING_ESTIMATE_NOT_CANON'))

def wall_path(phase,points,height,thickness=5):
    # axis-aligned deliberate stepped rampart, varied outline; apertures cut afterward
    for ((x1,z1),(x2,z2)) in zip(points,points[1:]):
        if x1!=x2 and z1!=z2:raise ValueError('rampart path needs orthogonal segment')
        o=thickness//2
        f(phase,min(x1,x2)-o,0,min(z1,z2)-o,max(x1,x2)+o,height-1,max(z1,z2)+o,'stone_bricks')
        f(phase,min(x1,x2)-o, height, min(z1,z2)-o,max(x1,x2)+o,height,max(z1,z2)+o,'smooth_stone')
        n=abs(x1-x2) or abs(z1-z2)
        for i in range(0,n+1,4):
            x=x1+((x2-x1)//n*i if n else 0)
            z=z1+((z2-z1)//n*i if n else 0)
            f(phase,x-o,height+1,z-o,x+o,height+2,z+o,'stone_bricks')

def tower(phase,x,z,rad=8,height=38,broken=False):
    f(phase,x-rad,0,z-rad,x+rad,height-1,z+rad,'stone_bricks')
    f(phase,x-rad+2,3,z-rad+2,x+rad-2,height-2,z+rad-2,'air')
    for y in range(4,height-5,8):
        f(phase,x-rad+2,y,z-rad+2,x+rad-2,y,z+rad-2,'spruce_planks')
        f(phase,x-rad+2,y+2,z-rad,x+rad-2,y+4,z-rad,'air')
    roofh=height-(6 if broken else 0)
    if broken:
        for off in range(0,2*rad,3):
            s(phase,x-rad+off,roofh+(off%5),z+rad,'cobbled_deepslate')
    else:
        f(phase,x-rad-1,height,z-rad-1,x+rad+1,height,z+rad+1,'deepslate_tiles')
        for h in range(1,6):
            r=max(0,rad+1-h)
            f(phase,x-r,height+h,z-r,x+r,height+h,z+r,'deepslate_tiles')
    # arrow slit distinctive medieval detail
    for xx,zz in [(x,z-rad),(x,z+rad),(x-rad,z),(x+rad,z)]:
        for y in (12,21):s(phase,xx,y,zz,'air')

def house(phase,ident,x,z,w,dep,h,kind='RESIDENTIAL_SEASONAL',quality='NORTHERN_TIMBER',roof='spruce'):
    x2=x+w-1;z2=z+dep-1
    f(phase,x,0,z,x2,0,z2,'cobblestone')
    f(phase,x,1,z,x2,h,z2,'stone_bricks' if quality=='NORTH_STONE' else 'spruce_planks')
    f(phase,x+1,1,z+1,x2-1,h-1,z2-1,'air')
    f(phase,x+1,1,z+1,x2-1,1,z2-1,'spruce_planks')
    f(phase,x+w//2,1,z2,x+w//2,3,z2,'air')
    for yy in (3,4):
        s(phase,x,yy,z+dep//2,'glass_pane');s(phase,x2,yy,z+dep//2,'glass_pane')
    f(phase,x-1,h+1,z-1,x2+1,h+1,z2+1,roof+'_planks')
    if w>=10:
        for step in (1,2,3):
            f(phase,x+step-1,h+step+1,z-1,x2-step+1,h+step+1,z2+1,roof+'_planks')
    # frame lintel + ridge
    f(phase,x,1,z,x,h+2,z,'stripped_spruce_log')
    f(phase,x2,1,z,x2,h+2,z,'stripped_spruce_log')
    plot(ident,kind,(x,z,x2,z2),quality,{'bed_space_max_estimate':(w*dep)//11})

# 00: elevated disposable Creative test pad at executing player's Y-1.
# Strong guardrail: deliberately very visible flat platform; NOT production terrain.
f('00_test_pad',-169,-2,-169,169,-2,330,'dirt')
f('00_test_pad',-169,-1,-169,169,-1,330,'grass_block')

# 01: Footings and roads on the disposable test platform.
f('01_footings',-14,0,144,14,0,315,'cobblestone')
f('01_footings',-10,0,-154,10,0,150,'cobblestone')
f('01_footings',-135,0,18,135,0,27,'gravel')
f('01_footings',-120,0,-30,125,0,-24,'gravel')
f('01_footings',-12,0,193,14,0,222,'cobblestone')
plot('entrance_causeway','ROAD',(-12,146,12,314),'STARK_NORTH')

outer=[(-145,-136),(124,-136),(124,-114),(148,-114),(148,62),(128,62),(128,140),(-102,140),(-102,125),(-150,125),(-150,-100),(-145,-100),(-145,-136)]
wall_path('02_outer_defenses',outer,24,5)
# towers carefully aligned to rampart corners
for tx,tz in [(-145,-136),(124,-136),(148,-114),(148,62),(128,140),(-102,140),(-150,125),(-150,-100),(-60,140),(65,140)]:tower('02_outer_defenses',tx,tz,7,30)
# outer gate opening and flanking keepers
f('02_outer_defenses',-7,1,137,7,12,144,'air')
for x in [-17,17]:tower('02_outer_defenses',x,140,9,36)
f('02_outer_defenses',-9,13,138,9,16,142,'dark_oak_planks')
plot('outer_ramparts','DEFENSIVE_ENVELOPE',(-150,-145,148,151),'NORTHERN_GREY_GRANITE')

inner=[(-116,-100),(101,-100),(101,-80),(112,-80),(112,95),(92,95),(92,108),(-92,108),(-92,90),(-120,90),(-120,-90),(-116,-90),(-116,-100)]
wall_path('03_inner_defenses',inner,31,6)
for tx,tz in [(-116,-100),(101,-100),(112,-80),(112,95),(92,108),(-92,108),(-120,90),(-120,-90)]:tower('03_inner_defenses',tx,tz,8,38)
f('03_inner_defenses',-7,1,105,7,15,113,'air')
for x in [-19,19]:tower('03_inner_defenses',x,108,10,44)
f('03_inner_defenses',-10,16,108,10,19,112,'dark_oak_planks')
# low moat strip near south gate; not full filled moat (geothermal drainage needs survey)
f('03_inner_defenses',-90,-1,118,-18,-1,123,'water')
f('03_inner_defenses',19,-1,118,91,-1,123,'water')
plot('inner_ramparts','DEFENSIVE_ENVELOPE',(-123,-110,123,118),'NORTHERN_GREY_GRANITE')

# Great Keep: complex rather than one big empty box
house('04_great_hall_and_keep','great_keep',34,-18,48,38,16,'LORDLY_RESIDENCE','NORTH_STONE','dark_oak')
for yy in [6,11]:f('04_great_hall_and_keep',36,yy,-16,79,yy,17,'spruce_planks')
for tx,tz in [(34,-18),(81,-18),(34,19),(81,19)]:tower('04_great_hall_and_keep',tx,tz,5,27)
f('04_great_hall_and_keep',54,1,19,61,5,19,'air')
# Greathall dimensions + raised dais, fireplace and long tables
house('04_great_hall_and_keep','great_hall',-45,28,61,38,13,'FEASTING_HALL','NORTH_STONE','dark_oak')
f('04_great_hall_and_keep',-37,2,54,4,2,58,'spruce_planks')
for zz in (37,43,49):
    for xx in (-31,-11,9):f('04_great_hall_and_keep',xx,2,zz,xx+12,2,zz+1,'dark_oak_planks')
f('04_great_hall_and_keep',-43,2,29,-42,5,29,'magma_block')
# Guard hall, First Keep and broken tower preexisting ruin
house('04_great_hall_and_keep','guardhall',-89,49,30,22,9,'WATCH_BARRACK','NORTH_STONE')
house('04_great_hall_and_keep','first_keep',-101,12,28,30,12,'ANCIENT_KEEP','NORTH_STONE')
tower('04_great_hall_and_keep',-93,-94,9,33,broken=True)
plot('broken_tower','RUINED_ANCIENT_TOWER',(-103,-103,-83,-85),'NORTH_STONE',status='RUINED_298AC')
# real subterranean crypt chambers in sacrificial/test-flat environment only
f('04_great_hall_and_keep',-50,-13,-22,-4,-7,5,'stone_bricks')
f('04_great_hall_and_keep',-47,-12,-19,-7,-8,2,'air')
f('04_great_hall_and_keep',-30,-12,-19,-26,-8,2,'stone_bricks')
f('04_great_hall_and_keep',-28,-11,-19,-28,-8,2,'air')
for xx in [-45,-39,-33,-21,-15,-9]:
    f('04_great_hall_and_keep',xx,-10,-13,xx+1,-8,-11,'polished_andesite')
    s('04_great_hall_and_keep',xx,-7,-12,'stone_brick_stairs')
plot('crypts','ANCIENT_CRYPT',(-50,-22,-4,5),'NORTH_STONE',status='ANCIENT_ACTIVE_FAMILY_BURIAL')

# Godswood ~three acres intended in canon; here *prototype footprint* 78x82, below canon target; phase1 only
f('05_godswood_gardens',-111,0,-84,-25,0,-5,'podzol')
for _ in range(35):
    x=random.randint(-105,-30);z=random.randint(-80,-10)
    if abs(x+72)<11 and abs(z+42)<11:continue
    h=random.randint(9,16)
    f('05_godswood_gardens',x,1,z,x,h,z,'spruce_log')
    f('05_godswood_gardens',x-3,h-2,z-3,x+3,h+3,z+3,'spruce_leaves')
f('05_godswood_gardens',-80,0,-52,-64,0,-36,'water')
f('05_godswood_gardens',-75,1,-50,-75,18,-50,'birch_log')
f('05_godswood_gardens',-79,15,-54,-71,22,-46,'oak_leaves')
s('05_godswood_gardens',-75,3,-49,'red_concrete')
plot('godswood','SACRED_GODS_WOOD',(-111,-84,-25,-5),'NORTH_FOREST',status='PERMANENT_PROTECTED_NATURAL_COMPOUND')
# glass gardens and geothermal pools
f('05_godswood_gardens',42,0,-87,89,0,-41,'stone_bricks')
f('05_godswood_gardens',42,1,-87,89,10,-41,'glass')
f('05_godswood_gardens',44,1,-85,87,9,-43,'air')
for x in range(45,88,4):f('05_godswood_gardens',x,1,-85,x,9,-85,'spruce_log')
for z in range(-82,-42,4):f('05_godswood_gardens',45,1,z,86,1,z,'farmland')
f('05_godswood_gardens',41,11,-88,90,11,-40,'lime_stained_glass')
f('05_godswood_gardens',62,2,-87,69,4,-87,'air')
f('05_godswood_gardens',30,0,-92,38,0,-83,'water')
f('05_godswood_gardens',27,0,-93,29,0,-81,'magma_block')
plot('glass_gardens','HEATED_GREENHOUSE',(42,-87,89,-41),'GLASS_GEOTHERMAL',status='GROWING_SPACE_CAPACITY_ONLY')
plot('hot_springs','GEOTHERMAL_POOL',(27,-93,38,-81),'WARM_WATER')

# civic: chapel/forge/stable/kennels/food stores and service access
house('06_service_buildings','smithy',68,53,28,21,8,'METALWORKING_SHOP','NORTH_STONE')
for x,z in [(78,62),(80,62),(81,64)]:s('06_service_buildings',x,2,z,'furnace')
house('06_service_buildings','stables',-106,72,34,25,8,'HORSE_STABLE','NORTHERN_TIMBER')
for x in range(-102,-75,7):f('06_service_buildings',x,2,75,x,3,92,'oak_fence')
house('06_service_buildings','kennels',-146,72,20,20,7,'ANIMAL_SHELTER','NORTHERN_TIMBER')
house('06_service_buildings','kitchens',25,64,29,25,8,'FOOD_SERVICE_AND_STORAGE','NORTH_STONE')
house('06_service_buildings','winterfell_sept',-18,-11,23,22,11,'SMALL_SEPT','NORTH_STONE')
house('06_service_buildings','guesthouse',-13,-83,22,25,10,'GUEST_LODGING','NORTH_STONE')

# Winter town, separated by gate, seasonal use: not 1200 spawnable NPCs
# Neighbourhood blocks avoid main road x[-14..14].
# Street-following but irregular Northern winter-town plots, no uniform copied grid.
occupied=[(-50,181,-16,207),(16,159,45,184)]  # inn / granary protection
rng=random.Random(11498)
for i in range(34):
    # Lots concentrate around winding roads, asymmetrically and with random spacing.
    for attempt in range(2000):
        left=(i%3!=0)
        x=rng.randint(-152,-38) if left else rng.randint(32,135)
        z=rng.randint(186,303)
        w=rng.randint(10,21);dep=rng.randint(12,20);h=rng.randint(5,10)
        box=(x,z,x+w-1,z+dep-1)
        if box[2]>154 or box[3]>327:continue
        if any(not(box[2]+5 < old[0] or old[2]+5 < box[0] or box[3]+5 < old[1] or old[3]+5 < box[1]) for old in occupied):continue
        # Larger dwellings and small lanes around the inn/market; varied heights.
        occupied.append(box)
        quality='NORTH_STONE' if i in (2,13,21,28) else 'NORTHERN_TIMBER'
        house('07_winter_town',f'wintertown_cottage_{i+1:02d}',x,z,w,dep,h,
              'SEASONALLY_OCCUPIED_HOUSE',quality=quality,
              roof='spruce' if i%4 else 'dark_oak')
        break
    else:
        raise RuntimeError(f'Cannot place wintertown cottage {i} without overlap')
# Secondary lanes are reserved at main street; no grid road blankets cottage floors.
for bx,bz in [(-12,210),(-12,260),(-12,301),(14,232),(14,282)]:
    f('07_winter_town',bx,0,bz,bx+8,0,bz+2,'cobblestone')
house('07_winter_town','wintertown_inn',-47,183,28,22,11,'INN_TAVERN','NORTH_STONE')
house('07_winter_town','wintertown_granary',18,162,24,20,10,'FOOD_STORE','NORTH_STONE')
plot('wintertown_road','SETTLEMENT_ROAD',(-16,160,16,325),'WINTER_TOWN')

# surface articulation and structural story, no infinite decoration
for x in [-122,-107,-90,92,112,131]:
    for z in [-102,-78,72,97]:
        if random.random()>0.4:s('08_finishing',x,2,z,'mossy_stone_bricks')
for x,z in [(-20,115),(20,115),(-70,20),(60,-10)]:
    s('08_finishing',x,1,z,'lantern')

# Output into datapack namespace. Single unambiguous manual trigger with a disclaimer.
for phase, commands in P.items():
    (FUNCS/f'{phase}.mcfunction').write_text('\n'.join(commands)+'\n',encoding='utf8')
launcher=ROOT/'src/main/resources/data/knownworld/function/step3/winterfell_test.mcfunction'
launcher.parent.mkdir(parents=True,exist_ok=True)
launcher.write_text('# TEST-WORLD ONLY. Flat prepared 420x500 block build pad at execution point. Do not run on your real world!\n'+
  '\n'.join(f'function knownworld:step3/winterfell/{p}' for p in P)+'\n'+
  'tellraw @s {"text":"Winterfell TEST prototype complete. Buildings contain physical capacity ONLY. No NPCs or final coordinates.","color":"gold"}\n', encoding='utf8')
summary={'version':'step3_winterfell_test_v1','status':'EXPLICIT_MANUAL_TEST_ONLY_NOT_REAL_WORLDGEN','type':'DATA_PACK_FUNCTIONS','entrypoint':'knownworld:step3/winterfell_test',
         'function_count':len(P)+1,'command_counts_by_phase':{k:len(v) for k,v in P.items()},'total_commands':sum(len(x) for x in P.values()),'plot_count':len(plots),
         'layout_coordinate_system':'relative to player execution position (x=0,z=0), north is negative z','size_envelope_test_metres':{'xmin':-166,'xmax':166,'zmin':-169,'zmax':330},
         'original_in_world_site_NW_029':{'x':-4050173,'y':None,'z':-1976496,'approved':False},
         'rules':['No automatic world edits','Do not use on permanent world','All structures are first bespoke geometry test, not exact book footprint','Creative flat test pad required','No player edit protection, no undo','Run in world copy or disposable test world','No NPC generation']}
(REPORT/'WINTERFELL_TEST_GENERATION_MANIFEST.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf8')
(REPORT/'WINTERFELL_TEST_BUILDING_PLOTS.json').write_text(json.dumps({'site':'winterfell','date':'298 AC','plots':plots},indent=2)+'\n',encoding='utf8')
print(json.dumps({'total_commands':summary['total_commands'],'per_phase':summary['command_counts_by_phase'],'plots':len(plots)},indent=2))
