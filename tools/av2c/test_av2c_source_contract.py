#!/usr/bin/env python3
"""AV2C static/coverage assertions; no source/write side effects."""
import argparse,csv,json
from pathlib import Path

def test(root,stage):
    vocab=root/'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json'
    a=json.loads(vocab.read_text(encoding='utf-8'))
    assert len(a['definitions'])==706 and len(a['sourceRows'])==761
    assert {r['row'] for r in a['sourceRows']}==set(range(2,763))
    rows=list(csv.DictReader((stage/'docs/architecture/AV2C_ATTRIBUTE_COVERAGE.csv').open(encoding='utf-8',newline='')))
    assert len(rows)==761 and {int(r['Excel row']) for r in rows}==set(range(2,763))
    assert {r['Definition ID'] for r in rows if r['Definition ID']}=={d['id'] for d in a['definitions']}
    assert all(r['AV2C provenance effect'] for r in rows)
    folder=stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes'
    p=(folder/'KnownWorldAv2CEventPolicy.java').read_text(encoding='utf8')
    j=(folder/'KnownWorldAv2CExperienceJournal.java').read_text(encoding='utf8')
    assert 'public static final int RECENT_LIMIT = 32;' in p
    for fragment in ['B4_2B_PHYSICAL_WORK_ARRIVAL','WORKPLACE_PRESENCE','Cross-site AV2C person ID',
                     'String eventId','SHA-256']:
        assert fragment in p, fragment
    for fragment in ['ATOMIC_MOVE','CREATE_NEW','chain','checksum','validate(value,site,id)',
                     'KnownWorldAv2CEventPolicy.observedAtWork','Verified workplace-presence evidence',
                     'read(site,personId)','no profile/economy mutation','B4.2BPersonalEvents']:
        assert fragment.lower() in j.lower(), fragment
    assert 'manager.transfer' not in j.lower() and 'processWork(' not in j
    assert 'new NpcId(' not in j and 'KnownWorldAv2CharacterState.set(' not in j
    b4=(root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42BPersonalEvents.java').read_text(encoding='utf8')
    b41=(root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java').read_text(encoding='utf8')
    chat=(root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java').read_text(encoding='utf8')
    assert 'eventCount++;dirty=true;' in b4
    assert 'if(previous!=null && day<=previous.latestDay())return;' in b4
    assert 'KnownWorldAv2BPsychologyBridge.enrich(sheet);' in b41
    assert 'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);' in b41
    assert '"derivedTendencies"' in chat
    print('AV2C SOURCE PASS: 706 definitions, 761 lineage rows; no second NPC population, no wages/skill changes')

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--repo',required=True);ap.add_argument('--stage',default='AV2C_STAGE')
    args=ap.parse_args();test(Path(args.repo),Path(args.repo)/args.stage)
