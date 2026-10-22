#!/usr/bin/env python3
"""Offline conservation and policy regression checks against real repo resources."""
from __future__ import annotations
import collections, json
from build_c4_3_job_distribution import ROOT, SCENARIO, CATALOG, INPUT, MACRO, site_record, eligible, build_plan
from c4_3_job_rules import build_index, EXECUTABLE_C3

def main():
 catalog=json.loads(CATALOG.read_text(encoding='utf8'))['rows']
 assert len(catalog)==206,(len(catalog),'original workbook rows')
 assert len({x['jobId'] for x in catalog})==204
 assert [(x['row'],x['jobId']) for x in catalog if x['isDuplicate']]==[(134,'Salt Worker'),(216,'Slaver')]
 assert {'Captain|MILITARY','Captain|MARITIME'}.issubset({x['jobId'] for x in catalog})
 sites=json.loads(INPUT.read_text(encoding='utf8'))['physicalSitePresets']
 assert len(sites)==248
 tt={t['id']:t for t in json.loads((SCENARIO/'architecture_templates_c3_v1.json').read_text(encoding='utf8'))['templates']}
 plans=[build_plan(site_record(site),catalog,tt) for site in sites]
 for p,s in zip(plans,sites):
  assert sum(p['occupationCounts'].values())==p['workforceTarget'],p['name']
  for role,n in p['occupationCounts'].items():
   key=next(j for j in catalog if j['jobId']==role and not j['isDuplicate'])
   assert eligible(key,site_record(s))[0],(p['name'],role)
  if p['peopleTarget']==0:assert not p['occupationCounts']
  if p['name']=='Winterfell':assert p['peopleTarget']==5000 and p['householdsEstimated']==1136
  if p['name']=='Ramsgate':assert p['peopleTarget']==550
  if p['name']=='Acorn Hall':assert p['peopleTarget']==2100
 lookup={p['name']:p for p in plans}
 assert lookup['King\'s Landing']['peopleTarget']==500000
 assert lookup['Braavos']['occupationCounts'].get('Slaver',0)==0
 for p in plans:
  if p['zone'] in ('NW','SW'):
   assert p['occupationCounts'].get('Slaver',0)==0
 print('C4.3 VERIFIED: 206 source rows / 204 distinct occupations / 248 sites')
 print('C4.3 VERIFIED: each site conserves every planned worker, including zero-population sites')
 print('C4.3 VERIFIED: Winterfell 5,000 population / 1,136 households protected in planning')
 print('C4.3 VERIFIED: Westeros and Braavos exclude automatically generated slaver jobs')
 print('NOTE: runtime NPCs and physical Minecraft blocks are NOT tested here')
if __name__=='__main__':main()
