#!/usr/bin/env python3
"""Read-only source and manifest regression checks for B4.2B.5 staging."""
from __future__ import annotations
import json
import csv
from pathlib import Path
import re
import sys

REPO=Path.cwd()
HERE=Path(__file__).resolve().parent
STAGE=HERE.parent
P=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
NEW=P/'KnownWorldB42B5HouseholdAuthority.java'
B2=P/'KnownWorldB42B2EconomicStanding.java'
B3=P/'KnownWorldB42B3StartingStock.java'
MAN=REPO/'src/main/resources/data/knownworld/scenarios/agot_298_ac/world_household_daily_b4_2b4_v1/manifest.json'
errors=[]
tracker=STAGE/'docs/architecture/VALUE_COVERAGE_ALL_SOURCE_ROWS.csv'
with tracker.open(encoding='utf-8-sig',newline='') as f:
    values=list(csv.DictReader(f))
def assert_(ok,message):
    if not ok:errors.append(message)
assert_(len(values)==761,'Coverage CSV must retain all 761 labeled rows from settlements.xlsx B2:B762')
assert_(sum(1 for r in values if r['row_kind']=='VALUE_OR_FIELD')==706,'Coverage CSV must track 706 value/field entries')
assert_(len({int(r['source_excel_row']) for r in values})==761,'Duplicate source spreadsheet rows')

m=json.loads(MAN.read_text(encoding='utf8'))
assert_(m['schemaVersion']=='knownworld.b4_2b4.household_daily_plan.v1','schema mismatch')
assert_((m['siteCount'],m['peopleTotal'],m['householdsTotal'],m['workersTotal']) == (248,3760684,799343,1880341),'census mismatch')
assert_(not m['dailyTransactionsActivated'],'transactions unexpectedly enabled')
assert_(m['indexedHouseholds'] + m['unresolvedHouseholds'] == 799343,'unresolved household mismatch')
assert_(len(m['sites'])==248,'incomplete sites')
assert_(len({s['siteId'] for s in m['sites']})==248,'duplicate sites')
assert_(sum(s['households'] for s in m['sites'])==799343,'site household sum mismatch')
assert_(sum(s['people'] for s in m['sites'])==3760684,'site people sum mismatch')
assert_(sum(s['workers'] for s in m['sites'])==1880341,'site worker sum mismatch')
assert_(sum(s['householdGrainFromPublicKg'] for s in m['sites'])==m['proposedGrainTransfersKg'],'grain proposals mismatch')
assert_(sum(s['householdCoinFromPublicUnits'] for s in m['sites'])==m['proposedCoinTransfersUnits'],'coin proposals mismatch')
assert_(sum(s['dailyHouseholdDemandGrams'] for s in m['sites'])==m['estimatedDailyGrainDemandGrams'],'daily grain mismatch')
assert_(sum(s['estimatedDailyGrossWagesCoinUnits'] for s in m['sites'])==m['estimatedDailyGrossWagesCoinUnits'],'wage proposals mismatch')
assert_((STAGE/NEW).is_file() or (REPO/NEW).is_file(),'new Java source missing')
source=(REPO/NEW if (REPO/NEW).exists() else STAGE/NEW).read_text(encoding='utf8')
for token in ['/kwbridge' if False else 'Commands.literal("kwbridge")', 'public static Audit audit(String siteId)', 'public static JsonObject findHousehold(', 'get("nativeInventoryId").isJsonNull()', 'NATIVE_SETTLEMENT_UNRESOLVED']:
    assert_(token in source,'Java implementation token missing: '+token)
assert_('manager.produce(' not in source and 'manager.transfer(' not in source and 'manager.create(' not in source,'read-only contract violated')
assert_('KnownWorldB42B3StartingStock.canonicalSettlement(siteId)' in source,'must use native canonical mapping')
if (REPO/NEW).is_file():
    assert_('KnownWorldB42B5HouseholdAuthority.register();' in (REPO/B2).read_text(encoding='utf8'),'registration absent')
    assert_(re.search(r'(?<!private )static SettlementId canonicalSettlement\(String siteId\)',(REPO/B3).read_text(encoding='utf8')) is not None,'canonical mapper not shared')
else:
    assert_('KnownWorldB42B4HouseholdDaily.register();' in (REPO/B2).read_text(encoding='utf8'),'old registration unexpectedly absent')
    assert_('private static SettlementId canonicalSettlement(String siteId)' in (REPO/B3).read_text(encoding='utf8'),'old canonical mapper missing')
if errors:
    for e in errors:print('FAIL:',e,file=sys.stderr)
    sys.exit(1)
print('PASS B4.2B.5: 248 unique sites, 3,760,684 persons, 799,343 households, 1,880,341 workers, conserved proposals, read-only source contract')
