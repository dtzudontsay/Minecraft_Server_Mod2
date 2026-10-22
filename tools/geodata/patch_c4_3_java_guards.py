#!/usr/bin/env python3
"""Bounded idempotent patch of C3 manifest-size and C2 building geometry guards.

Does not remove two-pilot / Winterfell protection. Prints exact changes.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement'
PATCHES={
 'KnownWorldC3Architecture.java':[(
    '|| root.get("templateCount").getAsInt() != 43',
    '|| root.get("templateCount").getAsInt() < 43'),(
    'if (templates.size() != 43) throw new IllegalStateException("Missing C3 architecture programs");',
    'if (templates.size() != root.get("templateCount").getAsInt()) throw new IllegalStateException("Missing C3 architecture programs");')],
 'KnownWorldUniversalWorldgenC2.java':[(
    'if (w < 5 || d < 5 || w > 33 || d > 33)',
    'if (w < 5 || d < 5 || w > 64 || d > 64)')]
}

def main():
 for fn,changes in PATCHES.items():
  p=BASE/fn;src=p.read_text(encoding='utf8');updated=src
  for before,after in changes:
   if before in updated:
    if updated.count(before)!=1:raise ValueError('Ambiguous Java search: '+fn+' '+before)
    updated=updated.replace(before,after)
   elif after in updated:pass
   else:raise ValueError('Unexpected Java version: '+fn+'; did not patch')
  if updated!=src:
   backup=p.with_suffix('.java.c4_3.bak')
   if not backup.exists():backup.write_text(src,encoding='utf8')
   p.write_text(updated,encoding='utf8');print('PATCHED',p)
  else:print('ALREADY PATCHED',p)
if __name__=='__main__':main()
