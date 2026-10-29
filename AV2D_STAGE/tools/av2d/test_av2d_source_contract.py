#!/usr/bin/env python3
"""Read-only source crosswalk and AV2C prerequisite contract."""
import argparse,csv,json
from pathlib import Path


def check(root,stage):
    av1=root/'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json'
    idx=json.loads(av1.read_text(encoding='utf8'))
    assert len(idx['definitions'])==706 and len(idx['sourceRows'])==761
    assert {r['row'] for r in idx['sourceRows']}==set(range(2,763))
    f=stage/'docs/architecture/AV2D_ATTRIBUTE_COVERAGE.csv'
    with f.open(encoding='utf8',newline='') as source:rows=list(csv.DictReader(source))
    assert len(rows)==761 and {int(r['Excel row']) for r in rows}==set(range(2,763))
    assert {r['Definition ID'] for r in rows if r['Definition ID']}=={x['id'] for x in idx['definitions']}
    assert sum(x['AV2D event-change eligibility']=='POTENTIAL_VERIFIED_EVENT_TARGET' for x in rows)==117
    b=root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java'
    s=b.read_text(encoding='utf8')
    for key in ('KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);',
                'KnownWorldAv2BPsychologyBridge.enrich(sheet);',
                'KnownWorldAv2CExperienceJournal.register();'):
        assert s.count(key)==1,key
    for q in ('KnownWorldAv2DChangeJournal.register();','KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);'):
        assert s.count(q)<=1,q
    policy=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2DChangePolicy.java').read_text(encoding='utf8')
    ledger=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2DChangeJournal.java').read_text(encoding='utf8')
    for q in ('MAX_RECEIPTS_PER_PERSON = 512','MAX_ABSOLUTE_CHANGE = 0.025','VERIFIED_COMPLETED_PRACTICE','VERIFIED_FORMATION_OUTCOME','VERIFIED_SOCIAL_OUTCOME'):
        assert q in policy,q
    for q in ('CREATE_NEW','ATOMIC_MOVE','sourceEventId','VerifiedOutcomeSource','checksum','receiptId','reconcile','read(site,id)','applyToSheet'):
        assert q in ledger,q
    assert 'KnownWorldB42BPersonalEvents.observedAtWork' not in ledger
    assert 'NpcRegistry' not in ledger and 'InventoryManager' not in ledger
    print('AV2D SOURCE PASS: AV1 706 definitions/761 rows, AV2A/B/C hooks, 117 outcome-capable typed dimensions, zero automatic skill rewards')

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--repo',required=True);ap.add_argument('--stage',default='AV2D_STAGE')
    args=ap.parse_args();check(Path(args.repo),Path(args.repo)/args.stage)
