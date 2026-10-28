#!/usr/bin/env python3
"""B4.2B: derived geographical discovery index for all projected residents.

Entries are *references* into existing B4.1A.1 household tiles, never new
people. Routes are calculated from existing home/work geometry, and public
venues are included for schedule-dependent leisure discovery. No saves touched.
"""
from __future__ import annotations
import argparse, collections, gzip, hashlib, json, math, os, re, shutil, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SC = Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
PROJ = SC/'world_projection_b4_1a1_v1'
WORK = SC/'world_daily_b4_2_v1'
C4 = SC/'universal_construction_c4_11_global_v1.json'
OUT = SC/'world_mobility_b4_2b_v1'
GRID=256
PARTS=16
MAX_LEGS=2500
SKIP={'site_nw_029','site_nw_031','site_sw_023'}


def read_json(path):
    with Path(path).open(encoding='utf8') as f:return json.load(f)
def jhash(text):
    value=0
    for c in text:value=(31*value+ord(c))&0xffffffff
    return value&0x7fffffff

def write_gzip(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('wb') as f:
        with gzip.GzipFile(fileobj=f,mode='wb',filename='',mtime=0,compresslevel=6) as gz:
            gz.write(json.dumps(obj,ensure_ascii=False,separators=(',',':')).encode('utf8'))

def cells(a,b):
    """Conservative tile-cover DDA, including endpoints and diagonal neighbors."""
    dx=b[0]-a[0];dz=b[1]-a[1]
    steps=math.ceil(max(abs(dx),abs(dz))/max(1,GRID//4))
    if steps>MAX_LEGS:raise ValueError('Unreasonably long local commute')
    for i in range(max(1,steps)+1):
        x=a[0]+dx*i/max(1,steps);z=a[1]+dz*i/max(1,steps)
        yield math.floor(x/GRID),math.floor(z/GRID)

def commute_points(home,work,fort):
    if not fort:return [home,work]
    cx,cz,outer,inner=fort
    for radius in [outer,inner]:
        if radius<=4:continue
        a=max(abs(home[0]-cx),abs(home[1]-cz))<radius-3
        b=max(abs(work[0]-cx),abs(work[1]-cz))<radius-3
        if a==b:continue
        gates=[(cx,cz-radius),(cx,cz+radius),(cx-radius,cz),(cx+radius,cz)]
        gate=min(gates,key=lambda g:math.dist(home,g)+math.dist(g,work))
        return [home,gate,work]
    return [home,work]

VENUE_GRID = 128
VENUE_RANGE = 120


class PublicVenueIndex:
    """Per-site spatial index; only scan venues within 120 taxicab blocks.

    Unlike repeatedly walking the whole public-venue catalog for every
    resident, this indexes a venue once, then inspects at most nine 128-block
    cells. The exact distance and tie-breaker from B4.2B.1 are preserved.
    """

    def __init__(self, public):
        cells = collections.defaultdict(list)
        for venue in public:
            kind = (venue.get('kind') or '').upper()
            x = int(venue['x']) + int(venue['w']) // 2
            z = int(venue['z']) + int(venue['d']) + 2
            ident = venue['id']
            entry = (x, z, ident, jhash_signed(ident) % 29,
                     any(token in kind for token in ('SCHOOL', 'MARKET', 'COURTYARD')))
            cells[(x // VENUE_GRID, z // VENUE_GRID)].append(entry)
        self.cells = dict(cells)
        self.count = len(public)

    def choose(self, person, home):
        h = jhash(person['personId'])
        if h % 4 == 0:
            return None
        child = 298 - int(person['birthYearAC']) < 16
        hx, hz = home
        gx, gz = hx // VENUE_GRID, hz // VENUE_GRID
        best = None
        for ix in (gx - 1, gx, gx + 1):
            for iz in (gz - 1, gz, gz + 1):
                for x, z, ident, hash29, child_ok in self.cells.get((ix, iz), ()):
                    if child and not child_ok:
                        continue
                    distance = abs(x - hx) + abs(z - hz)
                    if distance <= VENUE_RANGE:
                        candidate = (distance + ((h + hash29) % 29), ident, (x, z))
                        if best is None or candidate < best:
                            best = candidate
        return best[2] if best else None


def leisure_place(person, home, public):
    """Backwards-compatible helper, used by tests and standalone callers."""
    return PublicVenueIndex(public).choose(person, home)


def jhash_signed(s):
    h=0
    for c in s:h=(31*h+ord(c))&0xffffffff
    return h-0x100000000 if h>=0x80000000 else h

def fort_for(site,c4):
    s=c4.get(site)
    if not s:return None
    tier=s.get('physicalTier')
    radii={'SMALL':(56,0),'MEDIUM':(106,0),'MAJOR':(171,149)}
    if tier not in radii:return None
    return [int(s['originalX']),int(s['originalZ']),*radii[tier]]

def build(repo=ROOT,selection=None,output=None):
    repo=Path(repo).resolve();target=Path(output or repo/OUT).resolve()
    projection=read_json(repo/PROJ/'manifest.json')
    workplace=read_json(repo/WORK/'manifest.json')
    c4=read_json(repo/C4)
    if projection.get('schemaVersion')!='knownworld.b4_1a1.household_projection.v1' or projection.get('sourcePopulationCount')!=3760684 or len(projection.get('sites',[]))!=248:raise ValueError('Wrong B4.1A.1 source')
    if workplace.get('schemaVersion')!='knownworld.b4_2.daily_worksites.v1' or workplace.get('workersTotal')!=1880341 or len(workplace.get('sites',[]))!=248:raise ValueError('Wrong B4.2 worksite source')
    if c4.get('siteCount')!=248:raise ValueError('Wrong C4.11 geometry')
    bywork={s['siteId']:s for s in workplace['sites']}
    byc4={s['siteId']:s for s in c4['sites']}
    pending=target.with_name(target.name+'.pending')
    if pending.exists():raise FileExistsError('Pending index exists: '+str(pending))
    pending.mkdir(parents=True)
    sites=[];indexed=0;references=0
    try:
        for site in projection['sites']:
            sid=site['siteId'];ws=bywork.get(sid)
            if ws is None:raise ValueError('Site missing work information: '+sid)
            if selection and sid not in selection:sites.append({'siteId':sid,'parts':[],'indexed':0,'refs':0});continue
            if site['status']!='C4_11_HOME_CANDIDATES':
                sites.append({'siteId':sid,'parts':[],'indexed':0,'refs':0});continue
            path=(repo/WORK/(ws.get('file') or ''))
            works={}
            if ws.get('file'):
                with gzip.open(path,'rt',encoding='utf8') as f:doc=json.load(f)
                if doc.get('siteId')!=sid:raise ValueError('Foreign worksite: '+sid)
                works={w['id']:w for w in doc['workplaces']}
            public=[w for w in works.values() if any(x in (w.get('kind') or '').upper() for x in ('TAVERN','INN','MARKET','SCHOOL','TEMPLE','COURTYARD'))]
            venue_index=PublicVenueIndex(public)
            fort=fort_for(sid,byc4)
            # Source coords, same convention as existing household/worksite resources.
            buckets=[collections.defaultdict(list) for _ in range(PARTS)]
            npeople=0;nrefs=0
            for src_tile in site['tiles']:
                infile=repo/PROJ/sid/(src_tile+'.jsonl.gz')
                with gzip.open(infile,'rt',encoding='utf8') as f:
                    for line in f:
                        p=json.loads(line)
                        if p.get('siteId')!=sid or not p.get('personId','').startswith('b4:'+sid+':'):raise ValueError('Foreign person')
                        home=(int(p['x'])+int(p['w'])//2,int(p['z'])+int(p['d'])+2)
                        locations=[home]
                        w=works.get(p.get('workplaceId'))
                        if w:
                            dest=(int(w['x'])+int(w['w'])//2,int(w['z'])+int(w['d'])+2)
                            locations+=commute_points(home,dest,fort)[1:]
                        leisure=venue_index.choose(p,home)
                        if leisure:locations.append(leisure)
                        near=set()
                        for a,b in zip(locations,locations[1:]):near.update(cells(a,b))
                        near.add((math.floor(home[0]/GRID),math.floor(home[1]/GRID)))
                        if w:near.add((math.floor(dest[0]/GRID),math.floor(dest[1]/GRID)))
                        for tx,tz in near:
                            key=f'{tx}_{tz}'; part=((tx*73856093)^(tz*19349663))&(PARTS-1)
                            buckets[part][key].append([src_tile,p['personId']]);nrefs+=1
                        npeople+=1
                        if npeople % 50000 == 0:
                            print(f'Indexing {sid}: {npeople} people, {nrefs} geographical references, {venue_index.count} public venues',flush=True)
            part_names=[]
            for i,bucket in enumerate(buckets):
                if not bucket:continue
                name=f'p{i:02}.json.gz'
                write_gzip(pending/sid/name,{'siteId':sid,'part':i,'cells':dict(sorted(bucket.items()))})
                part_names.append(i)
            sites.append({'siteId':sid,'parts':part_names,'indexed':npeople,'refs':nrefs})
            indexed+=npeople;references+=nrefs
            if len(sites)%32==0:print(f'Indexed {len(sites)}/248 site records | {indexed} people | {references} mobility references',flush=True)
        if len(sites)!=248:raise ValueError('Missing site records')
        if selection is None and indexed!=projection.get('indexedResidents'):
            raise ValueError('Indexed residents differ from B4.1A.1 census; no publication')
        write=(pending/'manifest.json')
        write.write_text(json.dumps({'schemaVersion':'knownworld.b4_2b.spatial_discovery.v1','sourceProjectionPopulation':3760684,'projectionSchemaVersion':projection['schemaVersion'], 'sourceWorkforce':1880341,'cellSize':GRID,'partitionCount':PARTS,'peopleIndexed':indexed,'references':references,'sourceIsUnchanged':True,'scope':'SCHEDULE_COMMUTES_AND_KNOWN_LEISURE_ONLY','sites':sites},indent=2)+'\n',encoding='utf8')
        if target.exists():raise FileExistsError('Refusing to replace existing mobility index: '+str(target))
        pending.rename(target)
        return {'sites':len(sites),'residentsIndexed':indexed,'references':references,'path':str(target)}
    except BaseException:
        # KeyboardInterrupt and SIGTERM via normal Python shutdown must not
        # leave a misleading, half-written index that blocks the next run.
        shutil.rmtree(pending,ignore_errors=True)
        raise

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=ROOT);a.add_argument('--site',action='append');a.add_argument('--output',type=Path);opt=a.parse_args()
    print(json.dumps(build(opt.repo,set(opt.site) if opt.site else None,opt.output),indent=2))
