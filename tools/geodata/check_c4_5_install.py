#!/usr/bin/env python3
"""C4.5 local installation diagnostic. Read-only; no Minecraft/world mutation.

Run from a repo checkout (tools/geodata/check_c4_5_install.py).
It inspects data, script patch markers, and built jars; never re-runs installer.
"""
from __future__ import annotations
import json,re,sys,zipfile
from pathlib import Path
from collections import Counter

ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
CASE=ROOT/'src/main/resources/data/knownworld/case_study'
JAVA=ROOT/'src/main/java/dev/dtzudontsay/knownworld'
GEN=ROOT/'tools/geodata/output/c4_5_connected_rollout'
E=[]

def say(level,message):
    print(f'[{level}] {message}')
    E.append((level,message))

def read_json(path):
    if not path.is_file():
        say('MISSING',f'{path.relative_to(ROOT)}')
        return None
    try:return json.loads(path.read_text(encoding='utf-8'))
    except Exception as exc:
        say('ERROR',f'Cannot parse {path.relative_to(ROOT)}: {exc}')
        return None

def marker_file(path,needles,title):
    if not path.exists():
        say('MISSING',f'{title}: {path.relative_to(ROOT)}')
        return
    body=path.read_text(encoding='utf-8',errors='replace')
    missing=[label for label,needle in needles if needle not in body]
    say('PASS' if not missing else 'FAIL',f'{title}: '+('patch present' if not missing else 'missing markers '+', '.join(missing)))

def latest_report(name):
    p=GEN/name
    if not p.exists():
        say('NOTE',f'No {p.relative_to(ROOT)} -- stage or opposite mode may not have run')
        return
    j=read_json(p)
    if j is None:return
    if 'sites' in j:
        say('PASS',f'{name}: {len(j["sites"])} sites, {j.get("totalBuildings", "?")} lots, {j.get("totalHomes","?")} houses')
    else:say('PASS',f'{name}: { {k:v for k,v in j.items() if isinstance(v,(int,str,bool))} }')

def main():
    print('='*72)
    print('C4.5 INSTALLATION AUDIT (READ ONLY)')
    print('Repository:',ROOT)
    print('='*72)
    if not (ROOT/'gradlew.bat').is_file():
        say('ERROR','This script is not extracted under the repository root beside gradlew.bat')
        return 2
    c4=SC/'settlement_construction_c4_v1.json'
    if c4.exists():
        nbytes=c4.stat().st_size
        # The WORLD manifest can be huge; examine its short header instead of loading all indices.
        with c4.open('rb') as f: head=f.read(30000).decode('utf-8','replace')
        c45='C4_5_NEW_CHUNK_GENERATION_C4_4_DISTRIBUTIONS' in head
        pc=re.search(r'"pilotCount"\s*:\s*(\d+)',head)
        count=int(pc.group(1)) if pc else None
        say('PASS' if c45 else 'FAIL',f'C4 worldgen manifest: {nbytes/1048576:.1f} MB, purpose C4.5={c45}, site count={count}')
        if c45 and count is not None:
            if count>2: say('PASS',f'WORLD mode appears installed ({count} indexed sites; only SMALL/MEDIUM normal types)')
            elif count==2:say('NOTE','PILOTS mode installed (NOT WORLD). Ramsgate + Acorn Hall only.')
        if not c45:say('NOTE','C4.5 settlement resource was not installed, or another generator overwrote it.')
    else:say('MISSING','C4 settlement construction resource')

    v2=read_json(CASE/'winterfell_v2.json')
    if v2 is not None:
        residents=v2.get('residents',[]);homes=v2.get('homes',[])
        affected=[p for p in residents if p.get('occupationPlanVersion')==45]
        uniq=len({p.get('job') for p in affected})
        allworkers=sum(p.get('job') not in ('CHILD','DEPENDENT','WARD','LORD','LADY') for p in residents)
        say('PASS' if affected else 'FAIL',f'Winterfell NPC roster: residents={len(residents)}, homes={len(homes)}, C4.5 reassigned={len(affected)}, distinct reassigned jobs={uniq}, employed-labelled residents={allworkers}')
        if affected: say('NOTE',f'Example assigned job: {affected[0].get("job", "?")} / {affected[0].get("occupationBuildingC45", "?")}')
        else: say('NOTE','No C4.5 jobs in Winterfell roster -- changed NPC job diversity will not appear in fresh-world V3 import.')

    v3=read_json(CASE/'winterfell_v3_worldgen.json')
    if v3 is not None:
        buildings=v3.get('buildings',[])
        new=[b for b in buildings if str(b.get('t','')).startswith('C3:')]
        marker=v3.get('c45')
        say('PASS' if marker and new else 'FAIL',f'Winterfell physical V3 index: total plots={len(buildings)}, C4.5 specialized plots={len(new)}, C4.5 manifest={bool(marker)}')
        if marker:
            say('NOTE',f'Original buildings preserved={marker.get("original1514Preserved", "?")}; unplaced workplaces={sum(marker.get("unplacedByGeometry",{}).values())}')
        if new:say('NOTE',f'First new venues: {Counter(b.get("t") for b in new).most_common(8)}')
        elif len(buildings)==1514:say('NOTE','This is exactly the old 1,514-plot layout; no new specialty workplaces were appended.')

    marker_file(JAVA/'world/generation/settlement/KnownWorldUniversalWorldgenC2.java',
                [('variable site count','C4.5 site index count disagrees with manifest')], 'C4.5 universal worldgen Java')
    marker_file(JAVA/'world/generation/settlement/KnownWorldC3Architecture.java',
                [('sign support','KnownWorldStructureSigns.attach')], 'C4.5 C3 signs Java')
    marker_file(JAVA/'world/generation/settlement/WinterfellWorldgenV3.java',
                [('special V3 templates','b.template.startsWith("C3:")'),('legacy labels','b.label')], 'C4.5 Winterfell V3 Java')
    marker_file(JAVA/'world/generation/settlement/KnownWorldStructureSigns.java',
                [('text handler','SignBlockEntity')], 'C4.5 sign helper Java')
    npc_file=JAVA/'simulation/casestudy/WinterfellV3Runtime.java'
    if npc_file.exists():
        body=npc_file.read_text(encoding='utf8',errors='replace')
        if 'FULL_BUDGET = 384;' in body:say('PASS','Winterfell projection stress budget = 384 (NOT NPC census limit)')
        else:say('NOTE','Winterfell projection budget = original/other value (not evidence of job failure)')

    print('\nREPORTS FROM THE LAST RUN:')
    for f in ('winterfell_job_report.json','winterfell_c45_venues_report.json',
              'C4_5_all-small-medium_report.json','C4_5_pilots_report.json'):
        latest_report(f)

    print('\nGRADLE OUTPUT JARS (existence does NOT prove build succeeded after changes):')
    jars=sorted((ROOT/'build/libs').glob('*.jar'),key=lambda p:p.stat().st_mtime,reverse=True) if (ROOT/'build/libs').exists() else []
    if not jars:say('NOTE','No build/libs/*.jar found -- use RUN_C4_5_BUILD_AND_CAPTURE.bat')
    for jar in jars[:4]:
        try:
            with zipfile.ZipFile(jar) as z:
                names=set(z.namelist())
            found=any(x.endswith('KnownWorldStructureSigns.class') for x in names)
            say('NOTE',f'{jar.relative_to(ROOT)} ({jar.stat().st_size/1048576:.1f} MB): compiled sign helper={found}')
        except Exception as ex:say('ERROR',f'Could not read {jar}: {ex}')

    print('\n'+'='*72)
    print('SUMMARY / NEXT STEP')
    errors=[x for x in E if x[0] in ('ERROR','FAIL','MISSING')]
    if errors:
        print(f'{len(errors)} failure/missing markers. C4.5 appears incomplete or mixed.')
        print('Do not repeat WORLD install yet. Share C4_5_INSTALL_AUDIT.log + C4_5_BUILD_CHECK.log.')
    else:
        print('All installed-resource and source markers found (NOT a runtime terrain test).')
        print('Check Gradle build status and a genuinely NEW world; use /kwinterfell3 status.')
    print('NOTE: Winterfell V3 retains 1,136 homes and its original walls by design.')
    print('='*72)
    return 0 if not errors else 1

if __name__=='__main__':sys.exit(main())
