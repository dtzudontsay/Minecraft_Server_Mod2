#!/usr/bin/env python3
"""AV1 read-only source/canonical-ID regression tests, no Minecraft launch."""
from pathlib import Path
import argparse, collections, json, re, sys

DOMAIN_ENUMS={
 'CHARACTER_SKILL':'simulation/npc/profile/CharacterSkill.java',
 'NATURAL_APTITUDE':'simulation/npc/profile/CharacterAptitude.java',
 'DISPOSITION':'simulation/npc/profile/CharacterDisposition.java',
 'CHARACTER_VALUE':'simulation/npc/profile/CharacterValue.java',
 'REGIONAL_CHARACTER_VALUE':'simulation/npc/profile/CharacterValue.java',
 'SOCIAL_NORM':'simulation/npc/profile/CharacterSocialNorm.java',
 'REGIONAL_SOCIAL_NORM':'simulation/npc/profile/CharacterSocialNorm.java',
 'HEALTH_STATE':'simulation/npc/profile/CharacterHealthState.java',
 'LEGAL_STATUS':'simulation/npc/profile/CharacterLegalStatus.java',
 'ORIENTATION':'simulation/npc/profile/CharacterOrientation.java',
 'ROUTINE_ROLE':'simulation/npc/routine/NpcRoleType.java',
 'SETTLEMENT_TYPE':'world/settlement/SettlementType.java',
 'HOLDING_TYPE':'simulation/social/holding/HoldingType.java',
 'TITLE_TYPE':'simulation/social/title/TitleType.java',
 'SUCCESSION_LAW':'simulation/social/succession/SuccessionLaw.java',
 'SOCIETY_TYPE':'simulation/social/society/NonDynasticSocietyType.java',
 'RELIGIOUS_INSTITUTION_TYPE':'simulation/npc/religion/ReligiousOrderCategory.java'}
CATALOG_FILES={
 'CULTURE':['data/knownworld/reference/cultures.json'],
 'RELIGION':['data/knownworld/reference/religions.json'],
 'LANGUAGE':['data/knownworld/reference/languages.json'],
 'OCCUPATION':['data/knownworld/reference/occupations.json','data/knownworld/reference/occupations_extensions.json'],
 'HOUSE_TEMPLATE':['data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json']}

def parse_java_enum_members(source, path):
    """Read Java enum constant names, including constants with constructor args.

    The AV1 audit previously assumed enum constants were plain comma-separated
    words. NonDynasticSocietyType uses KHALASAR(OrganizationType.NOMADIC_HOST),
    which is valid Java and must not be rejected as a missing enum entry.
    This intentionally validates names, not Java expressions or method bodies.
    """
    source = re.sub(r'/\*.*?\*/', '', source, flags=re.S)
    source = re.sub(r'//[^\n]*', '', source)
    m = re.search(r'\benum\s+\w+\s*\{', source)
    if not m:
        raise AssertionError(('no enum declaration', path))
    body = source[m.end():]
    names = []
    start = 0
    parens = 0
    braces = 0
    quote = None
    escaped = False

    def add(segment):
        part = segment.strip()
        if not part: return
        found = re.match(r'^([A-Z][A-Z0-9_]*)\b', part)
        if not found:
            raise AssertionError(('unrecognized enum constant declaration', path, part[:100]))
        suffix = part[found.end():].lstrip()
        if suffix and not suffix.startswith(('(', '{')):
            raise AssertionError(('unexpected enum constant syntax', path, part[:100]))
        names.append(found.group(1))

    for i, char in enumerate(body):
        if quote is not None:
            if escaped: escaped = False
            elif char == '\\': escaped = True
            elif char == quote: quote = None
            continue
        if char in ('"', "'"):
            quote = char
        elif char == '(':
            parens += 1
        elif char == ')':
            parens -= 1
            if parens < 0: raise AssertionError(('bad enum parentheses', path))
        elif char == '{':
            braces += 1
        elif char == '}':
            if braces == 0 and parens == 0:
                add(body[start:i])
                break
            braces -= 1
        elif char in ',;' and parens == 0 and braces == 0:
            add(body[start:i])
            start = i + 1
            if char == ';': break
    if not names:
        raise AssertionError(('no parsed enum entries', path))
    if len(set(names)) != len(names):
        raise AssertionError(('duplicate Java enum constants', path))
    return set(names)

def run(repo,stage):
    # Regression: valid Java enum constants may have constructor arguments.
    sample = ('enum Example { KHALASAR(OrganizationType.NOMADIC_HOST), '
              'OTHER(OrganizationType.OTHER); private final int x; }')
    assert parse_java_enum_members(sample, 'AV1_ENUM_PARSER_REGRESSION') == {'KHALASAR', 'OTHER'}
    resource=stage/'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json'
    obj=json.loads(resource.read_text(encoding='utf8'))
    assert obj['schema']=='knownworld.av1.attribute_vocabulary.v1'
    assert obj['sourceRowsTotal']==762 and obj['includedRows']==761 and obj['headingRows']==55
    assert obj['definitionsCount']==len(obj['definitions'])==706
    defs=obj['definitions'];srcrows=obj['sourceRows']
    assert len(srcrows)==761 and len({x['row'] for x in srcrows})==761
    assert sorted(x['row'] for x in srcrows)==list(range(2,763))
    assert sum(x['heading'] for x in srcrows)==55
    by_row={x['sourceRow']:x for x in defs};ids=set();aliases={};kinds=collections.Counter()
    for x in defs:
        key=x['domain']+':'+x['canonicalId'];assert key==x['id'] and key not in ids,key;ids.add(key)
        assert x['sourceRow'] in by_row and by_row[x['sourceRow']] is x
        assert x['runtimeAssignable'] is False,'AV1 must never activate mutations'
        assert x['sourceLabel'] and x['owner']
        kinds[x['kind']]+=1
        for a in x['aliases']:
            k=(x['domain'],a.lower());assert k not in aliases,('ambiguous alias',k);aliases[k]=x['canonicalId']
    assert len(by_row)==706 and len(ids)==706 and len({d['domain'] for d in defs})==30
    assert set(by_row)=={s['row'] for s in srcrows if not s['heading']}
    for s in srcrows:
        assert bool(s['heading'])==(s['definitionId'] is None)
        if not s['heading']:assert s['definitionId']==by_row[s['row']]['id']

    # Match the ACTUAL repository declarations; do not silently introduce new enum members.
    java_root=repo/'src/main/java/dev/dtzudontsay/knownworld'
    enum_ids={}
    for dom,rel in DOMAIN_ENUMS.items():
        source=(java_root/rel).read_text(encoding='utf8')
        enum_ids[dom]=parse_java_enum_members(source,rel)
    resources_root=repo/'src/main/resources'
    refs={}
    for dom,paths in CATALOG_FILES.items():
        values=set()
        for p in paths:
            content=json.loads((resources_root/p).read_text(encoding='utf8'))
            items=content['templates'] if isinstance(content,dict) else content
            for entry in items:
                code=entry['id'];assert code not in values,('duplicate source catalog id',dom,code)
                values.add(code)
        refs[dom]=values
    for d in defs:
        dom,canon,kind=d['domain'],d['canonicalId'],d['kind']
        if dom in enum_ids:
            assert kind=='ENUM' and canon in enum_ids[dom],('enum mismatch',d['sourceRow'],dom,canon)
        elif dom in refs:
            assert kind=='REFERENCE' and canon in refs[dom],('catalog mismatch',d['sourceRow'],dom,canon)
        elif dom=='PERSONALITY_SCALE':
            assert canon in {'courage','ambition','compassion','honor','patience','sociability'}
        elif dom=='PLANNER_TIER':
            assert canon in {'SMALL','MEDIUM','MAJOR','SPECIAL'}
        elif dom=='BLUEPRINT_MODE':
            assert canon=='RUIN_OR_LANDMARK_ONLY'
        elif dom=='TRAIT_TAG':assert kind=='LEGACY_UNTYPED_TRAIT'
        elif dom=='EDUCATION':assert kind=='PROPOSED_EDUCATION_ID'
        elif dom=='PROFILE_FIELD':assert kind=='COMPOSITE_FIELD_FAMILY'
        elif dom in {'FAITH_ATTRIBUTE','CULTURE_ATTRIBUTE'}:assert kind=='CUSTOM_FIELD'
        else:raise AssertionError(('unrecognized domain',dom))
    checks={
        ('CHARACTER_VALUE','EGALITARISM'):'EGALITARIANISM',
        ('SOCIAL_NORM','SCHOLARY'):'SCHOLARLY',
        ('CULTURE','seroni'):'sarnori',
        ('CULTURE','meerenese'):'meereenese',
        ('RELIGION','great_shepard'):'great_shepherd',
        ('OCCUPATION','rope_marker'):'rope_maker',
        ('OCCUPATION','shepard'):'shepherd',
        ('HEALTH_STATE','MINORITY_ILL'):'MINORLY_ILL',
        ('REGIONAL_CHARACTER_VALUE','RELIGIOUSITY'):'RELIGIOSITY',
        ('REGIONAL_SOCIAL_NORM','COURTLEY'):'COURTLY',
        ('EDUCATION','noble_courtley'):'noble_courtly',
        ('EDUCATION','noble_scholary'):'noble_scholarly',
        ('BLUEPRINT_MODE','RUIN'):'RUIN_OR_LANDMARK_ONLY',
    }
    for (dom,alias),expect in checks.items():assert aliases[dom,alias.lower()]==expect
    assert len([x for x in defs if x['kind']=='LEGACY_UNTYPED_TRAIT'])==97
    assert len([x for x in defs if x['kind']=='PROPOSED_EDUCATION_ID'])==10
    print(f'AV1 PASS: {len(defs)} definitions; {len(srcrows)} source lineage rows; {len(enum_ids)} enum domains; {len(refs)} catalog domains; {len(aliases)} historic label aliases; 0 gameplay mutations')
    print('Kinds: '+', '.join(f'{k}={v}' for k,v in sorted(kinds.items())))

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--repo',type=Path,default=Path.cwd());ap.add_argument('--stage',type=Path,default=Path('AV1_STAGE'))
    a=ap.parse_args()
    try:run(a.repo.resolve(),(a.repo/a.stage).resolve())
    except (ValueError,KeyError,AssertionError,OSError) as e:
        print('AV1 FAIL:',str(e),file=sys.stderr);sys.exit(1)
