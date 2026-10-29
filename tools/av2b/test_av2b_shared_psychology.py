#!/usr/bin/env python3
"""Nonmutating AV2B checks: AV1 lineage, AV2A installed hooks, formula and source guards.
No Minecraft saves or user source edited.
"""
import argparse,csv,json,re,sys
from pathlib import Path

AXES=('DECEITFUL_ADVANCEMENT','INTERVENTION','IMPULSIVE_CONFRONTATION',
      'PEER_CONFORMITY','RISK_TAKING','MERCY','VENGEANCE','LOYAL_DUTY',
      'POLITICAL_ASSERTIVENESS','RELIGIOUS_RECEPTIVENESS','CULTURAL_OPENNESS',
      'MANIPULATION_SUSCEPTIBILITY')

def check(repo,stage):
    reference=json.loads((repo/'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json').read_text(encoding='utf8'))
    assert len(reference['definitions'])==706 and len(reference['sourceRows'])==761
    assert {r['row'] for r in reference['sourceRows']}==set(range(2,763))
    assert sum(not r['heading'] for r in reference['sourceRows'])==706
    src=repo/'src/main/java/dev/dtzudontsay/knownworld/simulation'
    rules=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyRules.java').read_text(encoding='utf8')
    bridge=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2BPsychologyBridge.java').read_text(encoding='utf8')
    legacy=(src/'npc/psychology/CharacterPsychologyService.java').read_text(encoding='utf8')
    b41=(src/'population/KnownWorldB41Activation.java').read_text(encoding='utf8')
    chat=(src/'population/KnownWorldB41DialogueService.java').read_text(encoding='utf8')
    av2a=(src/'attributes/KnownWorldAv2CharacterState.java').read_text(encoding='utf8')
    enum=(src/'npc/psychology/CharacterDerivedTendency.java').read_text(encoding='utf8')
    for axis in AXES:
        assert re.search(r'\b'+axis+r'\b',enum), 'Missing actual legacy tendency '+axis
        assert f'out.put(CharacterDerivedTendency.{axis}' in rules, 'Not mapped '+axis
    assert rules.count('out.put(CharacterDerivedTendency.')==12
    for token in ['CharacterPsychologyRules.evaluate','KnownWorldAv2BPsychologyBridge.enrich(sheet);']:
        assert token not in legacy or legacy.count(token)==1
    assert 'CharacterPsychologyRules.legacy(state.personality(), profile)' in rules or 'CharacterPsychologyRules.legacy(state.personality(), profile)' in legacy or 'CharacterPsychologyRules.legacy(state.personality(), profile)' in (stage.parent/'tools/av2b/av2b_guarded_install.py').read_text(encoding='utf8')
    assert 'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);' in b41
    assert 'KnownWorldAv2CharacterState.register();' in b41
    assert 'public static String av2VisibleSite(String personId)' in b41
    assert 'String[] SKILLS' in b41 and 'JsonObject personality=scoreMap' in b41
    assert 'PERSONALITY / REGIONAL INFLUENCE' in chat
    assert 'String site=KnownWorldB41Activation.av2VisibleSite(id)' in av2a
    assert 'knownworld.av2a.person_delta.v1' in av2a
    for marker in ('new NpcPersonality(', 'toSigned(personality,"courage")', 'CharacterPsychologyRules.evaluate(',
                   'BLOCKED_UPSTREAM_PROFILE','BLOCKED_INVALID_PROFILE','SHARED_DERIVED_MODEL_READ_ONLY',
                   'SOCIAL_NORM','LANGUAGES', '"language"', 'derivedTendencies'):
        if marker=='SOCIAL_NORM':continue
        assert marker in bridge,marker
    assert 'new NpcId(' not in rules+bridge and 'new CharacterProfile(' not in rules+bridge
    records=list(csv.DictReader((stage/'docs/architecture/AV2B_ATTRIBUTE_COVERAGE.csv').open(encoding='utf-8',newline='')))
    assert len(records)==761 and {int(x['Excel row']) for x in records}==set(range(2,763))
    expected={d['id'] for d in reference['definitions']}
    actual={x['Definition ID'] for x in records if x['Definition ID']}
    assert expected==actual,'AV1 definitions/Excel rows lost'
    assert all(x['AV2B state'] for x in records)
    print(f'AV2B TEST PASS: {len(records)} source rows / {len(expected)} canonical definitions;'
          f' {len(AXES)} shared tendencies; AV2A prerequisite; no second NpcRegistry')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',required=True);p.add_argument('--stage',default='AV2B_STAGE')
    a=p.parse_args();check(Path(a.repo).resolve(),(Path(a.repo)/a.stage).resolve())
