#!/usr/bin/env python3
"""Read-only B4.0 manifest + representative compressed resource verifier.

Does not modify game saves or generated population, and does not download anything.
Optional --deep scans every resident for identity and exact count conservation.
"""
from __future__ import annotations
import argparse, gzip, json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REL = Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1')
SCHEMA = 'knownworld.b4_0.identity_baseline.v1'
TOTALS = (248, 3_760_684, 799_343, 1_880_341)
SITE_PATTERN = re.compile(r'site_[a-z0-9]+(?:_[a-z0-9]+)+\Z')


def validate(root:Path, deep:bool=False) -> dict:
    root = Path(root)
    base = root / REL
    manifest = base / 'manifest.json'
    if not manifest.exists():
        raise FileNotFoundError(f'Missing B4.0 manifest: {manifest}')
    data = json.loads(manifest.read_text(encoding='utf-8-sig'))
    if data.get('schemaVersion') != SCHEMA:
        raise ValueError(f'Unexpected B4.0 schemaVersion {data.get("schemaVersion")!r}')
    sites = data.get('sites')
    if not isinstance(sites, list): raise ValueError('Manifest sites is not a list')
    measured = (len(sites), sum(s['personCount'] for s in sites),
                sum(s['householdCount'] for s in sites),
                sum(s['modeledWorkerCount'] for s in sites))
    declared = (data.get('siteCount'),data.get('peopleTotal'),
                data.get('householdsTotal'),data.get('modeledWorkersTotal'))
    if measured != TOTALS or declared != TOTALS:
        raise ValueError(f'Population totals incorrect: calculated={measured} declared={declared} expected={TOTALS}')
    ids=set();shards=0;records_checked=0
    for site in sites:
        sid=site['siteId']
        if not SITE_PATTERN.fullmatch(sid) or sid in ids:
            raise ValueError(f'Duplicate/malformed site id {sid!r}')
        ids.add(sid)
        count = site['personCount']
        fname=site.get('file')
        if count==0:
            if fname is not None: raise ValueError(f'Uninhabited {sid} unexpectedly has a shard')
            continue
        if fname != sid+'.jsonl.gz':
            raise ValueError(f'Invalid resource filename: {sid}: {fname!r}')
        path = base / fname
        if not path.is_file(): raise FileNotFoundError(f'Missing population shard: {path}')
        shards+=1
        with gzip.open(path,'rt',encoding='utf8') as f:
            if deep:
                actual=0
                for actual, line in enumerate(f,1):
                    try: person=json.loads(line)
                    except Exception as e: raise ValueError(f'Malformed person {sid} line {actual}') from e
                    if person.get('siteId')!=sid or not person.get('personId'):
                        raise ValueError(f'Wrong owner/identity: {sid} line {actual}')
                if actual!=count: raise ValueError(f'Incomplete shard {sid}: {actual} != {count}')
                records_checked+=actual
            else:
                line=f.readline()
                if not line:raise ValueError(f'Empty population shard: {sid}')
                person=json.loads(line)
                if person.get('siteId')!=sid or not person.get('personId'):
                    raise ValueError(f'First record wrong: {sid}')
                records_checked+=1
    return {'sites':len(sites),'inhabitedShards':shards,'people':measured[1],
            'households':measured[2],'workers':measured[3],
            'recordsActuallyRead':records_checked,'mode':'deep' if deep else 'first-line smoke test'}


def main():
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=ROOT)
    p.add_argument('--deep',action='store_true',help='Read and validate every compressed individual; more expensive')
    opts=p.parse_args()
    result=validate(opts.repo,opts.deep)
    print(json.dumps(result,indent=2))
    print('B4.0 RESOURCES OK. If chat command fails, use /kwpopulation diagnose and logs/latest.log.')

if __name__=='__main__':main()
