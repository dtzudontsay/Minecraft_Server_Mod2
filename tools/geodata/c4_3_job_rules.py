"""C4.3 occupation and workplace policy. Scenario estimates, not canonical census.

Exactly one primary work assignment per worker; offices, background roles and
criminal activities can instead be secondary identities or scenario-authored.
"""
from __future__ import annotations
import re

# Job names separated by semicolons. Each job appears in precisely one group.
WORKPLACE_GROUPS = {
 'ADMINISTRATION': '''Administrator;Bridge Warden;Castellan;Clerk;Customs Officer;Market Inspector;Messenger;Public Notary;Road Warden;Scribe;Steward;Tax Collector;Tax Farmer;Toll Collector;Land Surveyor;Interpreter''',
 'KEEP': '''Courtier;Ruler;Merchant Prince;Dosh Khaleen;Maester;Grandmaester''',
 'JUSTICE_HALL': '''Executioner;Torturer''',
 'BARRACKS': '''Archer;Captain|MILITARY;Caravan Guard;Crossbowman;Drillmaster;Gatekeeper;Guard;Knight;Man-at-Arms;Mercenary;Outrider;Scout;Sellsword;Siege Carrier;Siege Engineer;Soldier;Spearman;Standard-Bearer;Weapons Master;Quartermaster;Dothraki Bloodrider;Unsullied Soldier''',
 'ARMORY': '''Armory Keeper;Armorer''',
 'DOCK': '''Captain|MARITIME;Corsair;Dockworker;Galley Master;Harbormaster;Navigator;Oarsman;Pilot;Pirate;Reaver;Sailor''',
 'FERRY_LANDING': '''Ferryman''',
 'FISHERY': '''Fisherman;Fishmonger''',
 'SHIPYARD': '''Shipwright''',
 'HEALER': '''Apothecary;Healer;Herbalist;Midwife;Eunuch Healer''',
 'SCHOLASTIC_HALL': '''Scholar;Tutor''',
 'BOOKBINDERY': '''Bookbinder;Parchment Maker''',
 'TEMPLE': '''Bell Ringer;Drowned Man;Moonsinger;Red Priest;Septa;Septon;religious_attendant;Silent Sister;Godswife;Gravedigger''',
 'BANK': '''Banker;Moneychanger;Moneylender''',
 'MARKET': '''Caravan Master;Factor;Market Seller;Merchant;Trader;Wine Factor;Wool Factor;Slaver;Slave Trader''',
 'INN': '''Innkeeper''',
 'CART_DEPOT': '''Wagon Driver;carter;Cartwright;Wheelwright''',
 'WAREHOUSE': '''Purser;Warehouse Keeper''',
 'SMITHY': '''Blacksmith;Farrier;Locksmith''',
 'CARPENTRY': '''Carpenter;Bowyer;Fletcher;Cooper;Rope Maker;Lumber Yard Keeper''',
 'BRICKWORKS': '''Brickmaker;Lime Burner;Plasterer;Roofer;Thatcher''',
 'MASONRY': '''Mason;Stonecutter''',
 'MINE': '''miner;Mine Overseer;Stone Quarryman;Salt Worker''',
 'CHANDLERY': '''Candle Maker;Soap Maker''',
 'LEATHERWORKS': '''Cobbler;Leatherworker;Tanner''',
 'POTTERY': '''Potter;Glassworker;Glazier''',
 'JEWELER': '''Goldsmith;Jeweler;Silversmith''',
 'WEAVERY': '''Dyer;Seamstress;Tailor;Weaver''',
 'FOREST_CAMP': '''Charcoal Burner;Forester;Woodcutter;Gamekeeper;Hunter''',
 'FARM': '''Farmer;grain_farmer;Grain Thresher;Harvest Overseer;Irrigation Keeper''',
 'GARDEN': '''Beekeper;Gardener;Orchardist''',
 'PASTURE': '''Animal Trainer;Dairyman;Herder;LIVESTOCK;Shepard;Camel Rider;Elephant Handler''',
 'MILL': '''Miller''',
 'BAKERY': '''Baker''',
 'BREWERY': '''Brewer;Vintner''',
 'BUTCHERY': '''Butcher;Smokehouse Keeper''',
 'GRANARY': '''Granary Keeper''',
 'KITCHEN': '''Cook''',
 'SERVICE_HALL': '''Handmaid;Servant;Wet Nurse;Water Carrier''',
 'PUBLIC_BATH': '''Bathhouse Keeper''',
 'CANAL_WORKS': '''Canal Keeper''',
 'STABLE': '''Falconer;Stable Master;Stablehand''',
 'CITY_SERVICES': '''Fire Warden;Lamplighter;Street Sweeper;Waste Collector''',
 'BROTHEL': '''Courtesan;Prostitute''',
 'ENTERTAINMENT_HALL': '''Bard;Dancer;Fool;Mummer;Musician;Singer;Storyteller''',
 'NONE_PRIVATE': '''Assasin;Bandit;Beggar;Fence;Poacher;Smuggler;Thief;Spy''',
 'ARCANE_STUDY': '''Alchemist;Bloodmage;Fortune Teller;Necromancer;Pyromancer;Shadowbinder;Warlock;Woods Witch;Dothraki Herbwoman;Jaqqa Rhan''',
 'FIGHTING_PIT': '''Pit Fighter''',
}
# Legacy existing executable C3 template IDs. New types require C3 building grammar.
EXECUTABLE_C3 = set('ADMINISTRATION ARMORY BAKERY BARRACKS BROTHEL CAMP CARPENTRY CART_DEPOT COURTYARD DOCK FARM FISHERY FOREST_CAMP GARDEN GRANARY GREAT_HALL GUARD_TOWER HEALER HOUSE_MEDIUM HOUSE_SMALL HOUSE_URBAN INN KEEP KITCHEN MARKET MASONRY MILL MINE MONUMENT PASTURE PUBLIC_BATH RITUAL_GROUND RUIN SCHOOL SERVICE_HALL SMITHY STABLE TAVERN TEMPLE TEMPORARY_CAMP WAREHOUSE WEAVERY WELL'.split())
# Rarer means smaller weight and a greater population threshold for first generated worker.
COMMON = set('Farmer grain_farmer Herder LIVESTOCK Gardener Servant Cook Soldier Guard Merchant market_seller Weaver Carpenter Blacksmith Woodcutter Forester'.split())
UNIQUE = {'Grandmaester', 'Dosh Khaleen', 'Ruler', 'Merchant Prince'}
EXTRAORDINARY = {'Bloodmage','Necromancer','Shadowbinder','Warlock','Pyromancer','Alchemist','Godswife','Fortune Teller','Woods Witch','Spy','Assasin','Torturer','Dothraki Bloodrider','Dosh Khaleen'}
UNDERGROUND = {'Assasin','Bandit','Fence','Poacher','Smuggler','Thief','Spy'}
SLAVERY = {'Slaver','Slave Trader'}
DOTHRAKI = {'Dosh Khaleen','Dothraki Bloodrider','Dothraki Herbwoman','Jaqqa Rhan'}
ESSOS_SPECIAL = {'Camel Rider','Elephant Handler','Eunuch Healer','Pit Fighter','Unsullied Soldier','Godswife'}
PORT = set('Captain|MARITIME Corsair Dockworker Galley_Master Harbormaster Navigator Oarsman Pilot Pirate Reaver Sailor Shipwright Ferryman Fisherman Fishmonger'.split())
PORT = {x.replace('_',' ') for x in PORT} | {'Salt Worker'}
GOVERNING = {'Ruler','Courtier','Castellan','Merchant Prince'}
FAITH_SEVEN = {'Septon','Septa','Silent Sister','Grandmaester','Maester'}


def job_key(raw, category=''):
    v = str(raw).split(' (')[0].split(' / ')[0].strip()
    # An accidental parenthetical is not part of the occupation key.
    v = re.sub(r'\s*\(.*$', '', v).strip()
    if v.upper() == 'ADMINISTRATOR': v='Administrator'
    v={'GUARD':'Guard','HEALER':'Healer','FARMER':'Farmer','FORESTER':'Forester','SMITH':'Blacksmith',
       'CARPENTER':'Carpenter','MASON':'Mason','MERCHANT':'Merchant','TRANSPORT':'Wagon Driver',
       'COOK':'Cook','SERVANT':'Servant','LIVESTOCK':'LIVESTOCK','Assasin':'Assasin',
       'market_seller':'Market Seller','religious_attendant':'religious_attendant'}.get(v,v)
    # 'Captain' in both military and maritime sections; semantically different.
    if v == 'Captain':return v + ('|MILITARY' if category == 'Military' else '|MARITIME')
    return v


def build_index(source_rows):
    rev={}
    for building,names in WORKPLACE_GROUPS.items():
        for name in names.split(';'):
            name=name.strip()
            if name in rev: raise ValueError(f'Duplicate job policy for {name}')
            rev[name]=building
    result=[]
    seen={}
    missing=[]
    for source in source_rows:
        key=job_key(source['raw'],source['section'])
        b=rev.get(key)
        if not b:
            missing.append((source['row'],key));continue
        original_count=seen.get(key,0)
        seen[key]=original_count+1
        policy='STANDARD'
        if key in UNDERGROUND:policy='SOCIAL_ACTIVITY_ONLY'
        elif key in EXTRAORDINARY:policy='AUTHOR_OR_EXCEPTION_ONLY'
        elif key in SLAVERY:policy='REGIONAL_SLAVERY'
        elif key in DOTHRAKI:policy='DOTHRAKI_ONLY'
        elif key in ESSOS_SPECIAL:policy='ESSOS_SPECIAL'
        elif key in FAITH_SEVEN:policy='REGIONAL_FAITH'
        elif key in PORT:policy='SHORE_AND_PORT_REQUIRED'
        elif key in GOVERNING:policy='HOLDING_AUTHORITY'
        if b in ('MINE','FISHERY','FERRY_LANDING','DOCK','SHIPYARD'):
            if b=='MINE':policy='MOUNTAIN_RESOURCE_REQUIRED'
            elif policy=='STANDARD':policy='SHORE_AND_PORT_REQUIRED'
        # Duplicates retain source row but count one canonical job.
        if original_count:policy='DUPLICATE_SOURCE_ROW'
        rarity=('UNIQUE' if key in UNIQUE else 'RESTRICTED' if key in EXTRAORDINARY or key in SLAVERY
                or key in ESSOS_SPECIAL else 'COMMON' if key in COMMON else 'SPECIALIST')
        result.append({'row':source['row'],'section':source['section'],'raw':source['raw'],'jobId':key,
                       'building':b,'workspace':'CASTLE_INTERIOR' if b in ('KEEP','ADMINISTRATION') else 'BUILDING',
                       'existingC3':b in EXECUTABLE_C3,'rarity':rarity,'rule':policy,
                       'isDuplicate':bool(original_count),
                       'canonicalSource':'USER_WORKBOOK_2026_10_22',
                       'provenance':'USER_AUTHORED_JOB; BUILDING_AND_WEIGHTS_MOD_ESTIMATE'})
    if missing:raise ValueError(f'Unmapped user jobs: {missing}')
    return result
