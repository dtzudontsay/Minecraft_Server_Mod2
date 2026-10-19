#!/usr/bin/env python3
"""Winterfell V3 finishing patch: read-only structure/census regression check."""
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[2]
INPUT = ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json'
INDEX = ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v3_worldgen.json'
JAVA = ROOT/'src/main/java/dev/dtzudontsay/knownworld'
V3 = JAVA/'world/generation/settlement/WinterfellWorldgenV3.java'
UI = JAVA/'client/casestudy/WinterfellCharacterScreen.java'
RUNTIME = JAVA/'simulation/casestudy/WinterfellV3Runtime.java'

def check(cond,msg):
    if not cond:raise AssertionError(msg)

def main():
    a=json.loads(INPUT.read_text(encoding='utf-8'))
    b=json.loads(INDEX.read_text(encoding='utf-8'))
    check(len(a['residents'])==5000,'5000 resident IDs required')
    check(len(a['households'])==len(a['homes'])==1136,'1136 households and homes required')
    check(len(a['workplaces'])==378,'378 workplaces required')
    check(len(b['buildings'])==1514,'V3 worldgen plot list changed')
    check(b['anchorX']==-4050173 and b['anchorZ']==-1976496,'Winterfell marker changed')
    check(len(b['chunks'])==6669,'V3 chunk-plan index changed')
    check(len(set(x['id'] for x in a['residents']))==5000,'Duplicate people')
    check(len(set(h['id'] for h in a['homes']))==1136,'Duplicate homes')
    match={p['id']:(p['x'],p['z'],p['w'],p['d']) for p in a['homes']+a['workplaces']}
    indexed={p['id']:(p['x'],p['z'],p['w'],p['d']) for p in b['buildings']}
    check(match==indexed,'V3 plotted building positions differ from household plan')
    roads=set(map(tuple,a['roads']))
    for gate in [(-171,15),(171,15),(171,-23),(158,-171),(158,171),(-149,15),(149,15),(149,94),(149,118)]:
        # A crossing may be represented by the central road within +/-3m.
        check(any(abs(x-gate[0])<=3 and abs(z-gate[1])<=3 for x,z in roads),
              f'No matching planned road near castle gate {gate}')
    src=V3.read_text(encoding='utf-8')
    check('cleanseSurfaceHazards' in src,'No lava footprint protection')
    check('Blocks.LANTERN' in src,'No interior lighting')
    check('Blocks.STONE_BRICK_STAIRS' in src,'No keep staircase')
    check('DoorBlock.HINGE' in src,'No double keep doors')
    check('Blocks.DEEPSLATE_TILES' in src,'No pitched dark-slate turret cap')
    screen=UI.read_text(encoding='utf-8')
    check('addRenderableWidget(Button.builder' in screen, 'UI needs real widgets')
    check('gm' in screen and 'secrets' in screen,'GM-only profile categories missing')
    runtime=RUNTIME.read_text(encoding='utf-8')
    check('profile.aptitudes()' in runtime and 'profile.socialNorms()' in runtime, 'profile categories incomplete')
    check('sheet.addProperty("gamemaster",gm)' in runtime,'GM filtering missing')
    print('PASS: 5000 residents, 1136 homes, 378 workplaces, 1514 unchanged plots, 6669 indexed chunks')
    print('PASS: protected original winterfell centre and all plotted building coordinates')
    print('PASS: existing 9 castle-road gate crossings; lights, stairs, entrance doors, UI widgets')
    print('NOTICE: offline check, NOT a Minecraft 26.3 Gradle/game test.')

if __name__=='__main__':
    try:main()
    except (FileNotFoundError,KeyError,AssertionError) as e:
        print('FAIL:',e,file=sys.stderr);sys.exit(1)
