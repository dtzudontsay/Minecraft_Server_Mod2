#!/usr/bin/env python3
"""Update only the obsolete C3 template-count expectation (read/write guarded)."""
from pathlib import Path
import argparse,shutil
ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/'src/test/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC3ArchitectureTest.java'
OLD='assertEquals(43, KnownWorldC3Architecture.allTemplates().size());'
NEW='assertTrue(KnownWorldC3Architecture.allTemplates().size() >= 43, "Original C1 templates must remain after extension");'
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--install',action='store_true');a=ap.parse_args()
    s=FILE.read_text(encoding='utf8')
    if NEW in s:
        print('PASS: C3 template-count test already updated');return
    if s.count(OLD)!=1:raise SystemExit('Refusing to rewrite unexpected C3 test version')
    print('FOUND obsolete hard-coded 43-template assertion; will preserve existing C1 template contract')
    if a.install:
        backup=FILE.with_name(FILE.name+'.before_c4_5_1.bak')
        if not backup.exists():shutil.copy2(FILE,backup)
        FILE.write_text(s.replace(OLD,NEW),encoding='utf8')
        print('UPDATED',FILE,'backup',backup)
if __name__=='__main__':main()
