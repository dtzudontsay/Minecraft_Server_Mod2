#!/usr/bin/env python3
"""Idempotently merge C1 entrypoint and occupations, without clobbering local files."""
import argparse
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MOD=ROOT/'src/main/resources/fabric.mod.json'
OCC=ROOT/'src/main/resources/data/knownworld/reference/occupations_extensions.json'
ADD=ROOT/'tools/geodata/c1_occupations_additions.json'
ENTRY='dev.dtzudontsay.knownworld.simulation.integration.KnownWorldSettlementPresets'
BACKUP=ROOT/'tools/geodata/output/c1_settlement_presets/backups'

def process(check=False):
    if not MOD.exists() or not OCC.exists():
        raise SystemExit('Missing existing fabric.mod.json or occupations_extensions.json; stop safely')
    mod=json.loads(MOD.read_text(encoding='utf-8-sig'))
    old=json.loads(OCC.read_text(encoding='utf-8-sig'))
    additions=json.loads(ADD.read_text(encoding='utf-8-sig'))
    main=mod.get('entrypoints',{}).get('main')
    if not isinstance(main,list) or 'dev.dtzudontsay.knownworld.KnownWorld' not in main:
        raise SystemExit('Cannot safely identify KnownWorld main entrypoints')
    if not isinstance(old,list) or not isinstance(additions,list):
        raise SystemExit('Occupation records must be JSON lists')
    beforeCount=len(old)
    if len({p['id'] for p in old})!=len(old):raise SystemExit('Existing occupation ID duplicates')
    by_id={p['id']:p for p in old}
    changed_entries=ENTRY not in main
    if changed_entries:main.append(ENTRY)
    added=0
    for new in additions:
        id=new['id']
        if id in by_id:
            if by_id[id]!=new:
                raise SystemExit('Existing occupation '+id+' differs from the C1 proposal; resolve manually, no changes made')
            continue
        old.append(new);by_id[id]=new;added+=1
    if check:
        if changed_entries or added:
            raise SystemExit('C1 integration missing: run without --check')
        print('C1 integration CHECK PASSED: entrypoint + '+str(len(additions))+' job IDs present')
        return
    # Validate both documents BEFORE touching either file. They are additive
    # and idempotent; local custom occupations and entrypoints are preserved.
    mods=json.dumps(mod,ensure_ascii=False,indent=2)+'\n'
    jobs=json.dumps(old,ensure_ascii=False,indent=2)+'\n'
    # Backup once under tools/output so no .bak is packaged into the mod jar.
    BACKUP.mkdir(parents=True,exist_ok=True)
    if changed_entries:
        backup=BACKUP/'fabric.mod.json.before_c1.bak'
        if not backup.exists():backup.write_bytes(MOD.read_bytes())
        MOD.write_text(mods,encoding='utf-8')
    if added:
        backup=BACKUP/'occupations_extensions.json.before_c1.bak'
        if not backup.exists():backup.write_bytes(OCC.read_bytes())
        OCC.write_text(jobs,encoding='utf-8')
    print(f'C1 installed: main entrypoint added={changed_entries}; occupations added={added}; preserved existing={beforeCount}')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--check',action='store_true')
    process(parser.parse_args().check)
