#!/usr/bin/env python3
"""AV4 read-only continuity test. No mutations, no whole-world NPC scan."""
import argparse, csv, re
from pathlib import Path

def check(root,stage):
    java=root/'src/main/java/dev/dtzudontsay/knownworld'
    b41=(java/'simulation/population/KnownWorldB41Activation.java').read_text(encoding='utf-8')
    daily=(java/'simulation/population/KnownWorldB42DailyLife.java').read_text(encoding='utf-8')
    chat=(java/'simulation/population/KnownWorldB41DialogueService.java').read_text(encoding='utf-8')
    for need in ('KnownWorldAv3Formation.register();',
                 'KnownWorldAv3Formation.applyToSheet(sheet,p.site,p.id,',
                 'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);',
                 'KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);',
                 'KnownWorldAv2BPsychologyBridge.enrich(sheet);'):
        if b41.count(need)!=1:raise AssertionError('Missing/double AV3 profile pipeline '+need)
    names=['KnownWorldAv3Formation.applyToSheet(sheet,p.site,p.id,',
           'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);',
           'KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);',
           'KnownWorldAv2BPsychologyBridge.enrich(sheet);']
    if [b41.index(x) for x in names]!=sorted(b41.index(x) for x in names):
        raise AssertionError('AV3 > AV2A > AV2D > AV2B profile authority changed')
    for need in ('private static void enrich(JsonObject sheet,Person p,Site site)',
                 'private static void inspect(ServerPlayer player,Entity actor)',
                 'public static DialogueIdentity dialogueIdentity(ServerPlayer player,Entity actor)'):
        if b41.count(need)!=1:raise AssertionError('B41 API changed '+need)
    for need in ('public static void applyProjection(Villager villager,String personId,String siteId,String workId,',
                 'KnownWorldB42PhysicalRoutine.steer(villager,role,tx,tz,fort,when);',
                 'KnownWorldB42BPersonalEvents.observedAtWork('):
        if daily.count(need)!=1:raise AssertionError('B42 daily-life/movement authority changed '+need)
    old='Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,'
    new='var av4=KnownWorldAv4Decisions.choose(personId,siteId,day());'
    if (old in daily)==(new in daily):
        raise AssertionError('Expected exactly one original or AV4 free-time selection hook')
    oldNeighbor='if(Math.floorMod(personId.hashCode()+(int)day(),7)==0 &&'
    newNeighbor='if((av4!=null && av4.action()=='
    if (oldNeighbor in daily)==(newNeighbor in daily):
        raise AssertionError('Expected exactly one original or AV4 neighbor decision hook')
    for f in ['KnownWorldAv2CharacterState.java','KnownWorldAv2DChangeJournal.java',
              'KnownWorldAv2BPsychologyBridge.java','KnownWorldAv3Formation.java']:
        if not (java/'simulation/attributes'/f).is_file():raise AssertionError('Missing prerequisite '+f)
    a=list(csv.DictReader((stage/'docs/architecture/AV4_ATTRIBUTE_COVERAGE.csv').open(encoding='utf-8-sig',newline='')))
    if len(a)!=761 or len(set(row['Excel row'] for row in a))!=761:
        raise AssertionError('Loss or duplication of Excel lineage')
    required=('AV4 activity effect','AV4 mechanics state','AV4 owner','AV4 test focus')
    if not all(key in a[0] for key in required):raise AssertionError('Coverage columns incomplete')
    if sum(bool(row.get('Definition ID')) for row in a)!=706:raise AssertionError('AV1 definition lineage changed')
    if not ('"derivedTendencies"})' in chat or '"derivedTendencies","av4FreeTimeDecision"' in chat):
        raise AssertionError('K-chat profile context changed unexpectedly')
    rules=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv4DecisionRules.java').read_text(encoding='utf-8')
    service=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv4Decisions.java').read_text(encoding='utf-8')
    assert 'HOUSEHOLD_ACTIVITY' in rules and 'NEIGHBOR_VISIT' in rules and 'PUBLIC_LEISURE' in rules
    assert 'AV4 input outside [0,1]' in rules and 'av4ProfileFor' in service
    assert 'observedAtWork(' not in service and 'InventoryManager' not in service
    print('AV4 SOURCE CHECK PASS: 761 Excel rows / 706 definitions; AV3+AV2 pipeline; real B4 free-time movement; no fake economics')

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--repo',required=True);ap.add_argument('--stage',default='AV4_STAGE')
    args=ap.parse_args();check(Path(args.repo),Path(args.repo)/args.stage)
