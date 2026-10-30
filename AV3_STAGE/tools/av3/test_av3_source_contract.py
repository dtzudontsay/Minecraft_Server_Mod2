#!/usr/bin/env python3
"""AV3 source/lineage preflight; read-only and independent of Gradle."""
import argparse,csv,json
from pathlib import Path

DOMAINS={'NATURAL_APTITUDE':20,'DISPOSITION':26,'CHARACTER_VALUE':26,'SOCIAL_NORM':25,'CHARACTER_SKILL':20}
JAVA_COUNTS={'CharacterSkill':20,'CharacterAptitude':20,'CharacterDisposition':26,'CharacterValue':33,'CharacterSocialNorm':25}

def check(repo,stage):
    root=repo/'src/main/java/dev/dtzudontsay/knownworld'
    ref=repo/'src/main/resources/data/knownworld/reference'
    paths=['cultures.json','religions.json','regional_influences.json','regional_influence_extensions.json']
    data=[]
    for name in paths:
        obj=json.loads((ref/name).read_text(encoding='utf-8-sig'))
        if not isinstance(obj,list) or not obj:raise AssertionError('Missing/empty catalog '+name)
        data.append(obj)
    cultures,religions,regions,extensions=data
    cs={x['id'] for x in cultures};rs={x['id'] for x in religions}
    if len(cs)!=len(cultures) or len(rs)!=len(religions):raise AssertionError('Duplicate culture/faith ID')
    if len(cs)<45 or len(rs)<30 or len(regions)<30:raise AssertionError('Unexpectedly small canonical source catalogs')
    keys=set()
    for region in [*regions,*extensions]:
        if not isinstance(region,dict) or not region.get('locationId'):raise AssertionError('Bad regional record')
        keys.add(region['locationId'])
        for group,ids in [('cultures',cs),('religions',rs)]:
            for identifier,weight in region.get(group,{}).items():
                if identifier not in ids:raise AssertionError('Regional source refers to missing '+group+' ID '+identifier)
                v=weight.get('weight')
                if not isinstance(v,(int,float)) or not 0<=v<=1:raise AssertionError('Out-of-range group exposure')
        for group in ('values','socialNorms'):
            for axis,payload in region.get(group,{}).items():
                if not isinstance(payload,dict) or not isinstance(payload.get('value'),(int,float)) or not -1<=payload['value']<=1:
                    raise AssertionError('Malformed regional axis '+axis)
    c=stage/'docs/architecture/AV3_ATTRIBUTE_COVERAGE.csv'
    rows=list(csv.DictReader(c.open(encoding='utf-8-sig',newline='')))
    if len(rows)!=761 or len({x['Excel row'] for x in rows})!=761:
        raise AssertionError('AV3 lineage is not the original 761 rows')
    canon=[r for r in rows if r.get('Canonical ID') and r.get('Definition ID')]
    if len(canon)<600:raise AssertionError('AV3 coverage lost original canonical definitions')
    source=root/'simulation/population/KnownWorldB41Activation.java'
    text=source.read_text(encoding='utf-8')
    anchors=('KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);',
             'KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);',
             'KnownWorldAv2BPsychologyBridge.enrich(sheet);',
             'KnownWorldAv2DChangeJournal.register();')
    for anchor in anchors:
        if text.count(anchor)!=1:raise AssertionError('Missing or duplicated AV2 prerequisite integration '+anchor)
    if not (text.index(anchors[0])<text.index(anchors[1])<text.index(anchors[2])):
        raise AssertionError('AV2 effective profile read order changed')
    if 'private static void enrich(JsonObject sheet,Person p,Site site)' not in text:
        raise AssertionError('B41 profile generation signature changed')
    for need in ('String mother,','String father,','String household,','String religion,','String language,'):
        if need not in text:raise AssertionError('B41 family/identity source changed: '+need)
    # Cross-check all 104 influenced dimensions against authoritative enum files.
    vocab={r['Domain']:[x['Canonical ID'] for x in rows if x['Domain']==r['Domain'] and x.get('Canonical ID') and x.get('Definition ID')] for r in rows if r['Domain'] in DOMAINS}
    for k,expected in DOMAINS.items():
        identifiers={x['Canonical ID'] for x in rows if x['Domain']==k and x.get('Canonical ID') and x.get('Definition ID')}
        if len(identifiers)!=expected:raise AssertionError(f'Wrong count {k}: {len(identifiers)} != {expected}')
    for name,count in JAVA_COUNTS.items():
        source=(root/'simulation/npc/profile'/ (name+'.java')).read_text(encoding='utf-8')
        import re
        fields=re.findall(r'(?m)^[ \t]*([A-Z][A-Z0-9_]*)[ \t]*(?:,|;|(?=\r?$))',source)
        if len(fields)!=count or len(set(fields))!=count:raise AssertionError(f'Enum runtime dimension count differs: {name} {len(fields)} != {count}')
    inventory=list(csv.DictReader((stage/'docs/architecture/AV3_RUNTIME_AXIS_INVENTORY.csv').open(encoding='utf-8-sig',newline='')))
    if len(inventory)!=124:raise AssertionError('Not all 124 Java enum axes accounted for')
    from collections import defaultdict
    axes=defaultdict(set)
    for item in inventory:
        if item['Canonical ID'] in axes[item['Java enum']]:raise AssertionError('Duplicate runtime axis')
        axes[item['Java enum']].add(item['Canonical ID'])
    for name in JAVA_COUNTS:
        import re
        source=(root/'simulation/npc/profile'/(name+'.java')).read_text(encoding='utf-8')
        fields=set(re.findall(r'(?m)^[ \t]*([A-Z][A-Z0-9_]*)[ \t]*(?:,|;|(?=\r?$))',source))
        if axes[name]!=fields:raise AssertionError('AV3 runtime inventory and authoritative '+name+' differ')
    print(f'AV3 SOURCE CHECK PASS: {len(cs)} culture IDs; {len(rs)} faith IDs; {len(keys)} unique regional profiles; 104 influenced axes; 761 lineage rows; AV2A/B/C/D order intact')

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--repo',required=True);a.add_argument('--stage',default='AV3_STAGE')
    args=a.parse_args();repo=Path(args.repo);check(repo,repo/args.stage)
