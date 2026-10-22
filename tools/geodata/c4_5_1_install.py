#!/usr/bin/env python3
"""Safe staged, logged C4.5.1 repair; no old save changes, pauses in BAT wrapper."""
from __future__ import annotations
import argparse, datetime, os, subprocess,sys,traceback
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
LOG=ROOT/'C4_5_1_INSTALL.log'

def main():
  ap=argparse.ArgumentParser();ap.add_argument('--scope',choices=('PILOTS','WORLD'),required=True);a=ap.parse_args()
  if os.environ.get('KNOWNWORLD_C45_CONFIRM')!='NEW_WORLD':raise SystemExit('Only with fresh-world confirmation')
  if not (ROOT/'gradlew.bat').exists():raise SystemExit('Extract the ZIP into repository root, beside gradlew.bat')
  geo=ROOT/'tools/geodata'
  mode='pilots' if a.scope=='PILOTS' else 'all-small-medium'
  env=dict(os.environ);env['PYTHONUNBUFFERED']='1'
  stages=[
    ('Update outdated C3 test expectation', [sys.executable,str(geo/'c4_5_1_fix_test_contract.py'),'--install']),
    ('Python geometry / job conservation', [sys.executable,str(geo/'test_c4_5_planner.py')]),
    ('Read-only Java patch compatibility', [sys.executable,str(geo/'c4_5_patch_java.py'),'--npc-visual-stress']),
    ('Generate and install '+a.scope+' SMALL/MEDIUM worldgen first',
      [sys.executable,str(geo/'c4_5_connected_rollout.py'),'--scope',mode,'--install']),
    ('Assign diverse primary occupations to existing Winterfell generated adults',
      [sys.executable,str(geo/'c4_5_connected_rollout.py'),'--only-winterfell','--winterfell','--install']),
    ('Move 24 castle-ward homes nearby; replace plots with 12 inner workplaces',
      [sys.executable,str(geo/'c4_5_1_castle_ward_swap.py'),'--install']),
    ('Add distributed Winterfell specialist venues',
      [sys.executable,str(geo/'c4_5_winterfell_venues.py'),'--install']),
    ('Install verified new-chunk building signs and Java generator guards',
      [sys.executable,str(geo/'c4_5_patch_java.py'),'--npc-visual-stress','--install']),
    ('Audit installed C4.5.1 source and geometry',
      [sys.executable,str(geo/'c4_5_1_verify.py')]),
    ('Gradle compile and regression suite', ['cmd','/c','gradlew.bat','build'])]
  stamp=datetime.datetime.now().astimezone().isoformat()
  with LOG.open('a',encoding='utf-8') as log:
    log.write(f'\n======== C4.5.1 {a.scope} run {stamp} ========\n')
    log.flush()
    for i,(label,command) in enumerate(stages,1):
      print(f'[{i}/{len(stages)}] {label}',flush=True)
      log.write(f'\n[{i}/{len(stages)}] {label}\nCommand: {command!r}\n');log.flush()
      with subprocess.Popen(command,cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
                            encoding='utf-8',errors='replace',bufsize=1) as p:
        for line in p.stdout:
          log.write(line)
          if 'ERROR' in line or 'FAILED' in line or 'INSTALLED' in line or 'BUILD SUCCESSFUL' in line:
            print(line.rstrip(),flush=True)
        result=p.wait()
      log.write(f'EXIT={result}\n');log.flush()
      if result!=0:
        print(f'\nSTOPPED IN STAGE {i}: {label}; exit code {result}',flush=True)
        print('Full error in:',LOG,flush=True)
        print('Prior changed source files have backup copies. Do not use the resulting files as a successful install.',flush=True)
        return result
  print('\nALL STAGES INCLUDING GRADLE TESTS SUCCEEDED.',flush=True)
  print('Create a fresh world; previously generated chunks are never replaced.',flush=True)
  print('Log:',LOG,flush=True)
  return 0
if __name__=='__main__':sys.exit(main())
