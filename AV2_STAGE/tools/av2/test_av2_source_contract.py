#!/usr/bin/env python3
"""AV2A offline source/data regression tests. No world/Java mutation."""
import argparse, csv, json, re
from collections import Counter
from pathlib import Path


def check(root: Path, stage: Path):
    av1 = root/'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json'
    if not av1.exists():
        raise AssertionError('AV1 not installed. Install AV1 v1.1 first.')
    data=json.loads(av1.read_text(encoding='utf-8-sig'))
    definitions=data['definitions']; source=data['sourceRows']
    assert len(definitions)==706 and len(source)==761 and data['headingRows']==55
    assert set(row['row'] for row in source)==set(range(2,763))
    seen={row['definitionId'] for row in source if row['definitionId']}
    assert seen=={d['id'] for d in definitions}, 'AV2 lost an AV1 source definition'
    allow={'CHARACTER_SKILL','NATURAL_APTITUDE','DISPOSITION','CHARACTER_VALUE','SOCIAL_NORM'}
    expected={x['id'] for x in definitions if x['domain'] in allow}
    assert all(x['kind']=='ENUM' and x['typeResolvable'] for x in definitions if x['domain'] in allow)
    staged_java=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2CharacterState.java').read_text(encoding='utf-8')
    b41=(root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java').read_text(encoding='utf-8')
    assert 'record Person(' in b41 and 'world_projection_b4_1a1_v1/' in b41
    assert 'sheet.add("motivations",motivations);' in b41
    assert 'public static void register() {' in b41
    assert 'private static Person forActor(Entity actor)' in b41
    assert 'private static final Map<String,UUID> projected' in b41
    assert 'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);' in b41 or 'sheet.add("motivations",motivations);' in b41
    assert 'knownworld.av2a.person_delta.v1' in staged_java
    for marker in ('KnownWorldAv1Vocabulary.lookup','ADMIN_VERIFIED_PROJECTION','StandardOpenOption.CREATE_NEW',
                   'StandardCopyOption.ATOMIC_MOVE','checkRange(', 'validateSubject(',
                   'public static void applyToSheet(', 'public static void register(',
                   'KnownWorldB41Activation.av2VisibleSite'):
        assert marker in staged_java,marker
    # Each in-scope dimension remains an AV1 definition and all other domains
    # still belong to their later AV workstreams. We do not fabricate mechanics.
    ledger=stage/'docs/architecture/AV2A_ATTRIBUTE_COVERAGE.csv'
    records=list(csv.DictReader(ledger.open(encoding='utf-8-sig',newline='')))
    assert len(records)==761
    assert {int(r['Excel row']) for r in records}==set(range(2,763))
    writable={r['Domain']+':'+r['Canonical ID'] for r in records if r['AV2A state']=='SPARSE_NUMERIC_DELTA_READY'}
    assert writable==expected, (len(writable),len(expected))
    assert Counter(r['AV2A state'] for r in records)['SPARSE_NUMERIC_DELTA_READY']==len(expected)
    assert all(r['AV2A state']!='SPARSE_NUMERIC_DELTA_READY' for r in records if r['Domain']=='TRAIT_TAG')
    assert 'new CharacterProfileManager(' not in staged_java and 'new NpcId(' not in staged_java
    print('AV2A TEST PASS: 761 Excel rows / 706 AV1 definitions retained;'
          f' {len(expected)} typed numeric dimensions; no second identity manager')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',required=True);p.add_argument('--stage',default='AV2_STAGE')
    args=p.parse_args()
    check(Path(args.repo).resolve(),Path(args.repo).resolve()/args.stage)
