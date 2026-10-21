#!/usr/bin/env python3
"""C4.3 read-only data-contract inventory. No Minecraft/runtime/source mutations.

Reads checked-in sources and emits optional local review CSV/Markdown files only.
Use this BEFORE changing job catalogues or switching on global world generation.
"""
from __future__ import annotations
import argparse
import collections
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = ROOT / 'src/main/resources/data/knownworld/reference'
SCENARIO = ROOT / 'src/main/resources/data/knownworld/scenarios/agot_298_ac'
GEODATA = ROOT / 'tools/geodata'
DEFAULT_OUT = GEODATA / 'output/c4_3_readonly_audit'


def read(path: Path):
    if not path.is_file():
        raise FileNotFoundError(f'Required repository input missing: {path.relative_to(ROOT)}')
    return json.loads(path.read_text(encoding='utf-8-sig'))


def write_csv(path, fields, records):
    with path.open('w', encoding='utf-8-sig', newline='') as fp:
        writer = csv.DictWriter(fp, fieldnames=fields, delimiter=';', extrasaction='ignore')
        writer.writeheader()
        writer.writerows(records)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Read + validate; do not create files')
    parser.add_argument('--out', type=Path, default=DEFAULT_OUT)
    opt = parser.parse_args()
    occupations = read(REFERENCE / 'occupations.json')
    extensions = read(REFERENCE / 'occupations_extensions.json')
    c1 = read(SCENARIO / 'settlement_presets_c1_v1.json')
    c3 = read(SCENARIO / 'architecture_templates_c3_v1.json')
    c4 = read(SCENARIO / 'settlement_construction_c4_v1.json')
    winterfell = read(ROOT / 'src/main/resources/data/knownworld/case_study/winterfell_v2.json')
    authored_dir = SCENARIO / 'characters'
    authors = [read(f) for f in sorted(authored_dir.glob('*.json'))]
    catalog = occupations + extensions
    if len({x['id'] for x in catalog}) != len(catalog):
        raise ValueError('Duplicate occupation IDs across reference catalogues')
    if len({x['id'] for x in c3['templates']}) != len(c3['templates']):
        raise ValueError('Duplicate C3 physical template IDs')
    all_sites = c1['physicalSitePresets']
    roles = c1['occupationToWorkplace']
    buildings = {x['id']: x for x in c3['templates']}
    missing = {role: name for role, name in roles.items() if name not in buildings}
    if missing:
        raise ValueError(f'Role-to-building mapping references nonexistent physical template: {missing}')
    if int(c4['summary']['pilotCount']) != len(c4['pilots']):
        raise ValueError('C4 pilot count and manifests disagree')
    pilot_ids = {x['siteId'] for x in c4['pilots']}
    sites_by_id = {x['siteId']: x for x in all_sites}
    if not pilot_ids <= set(sites_by_id):
        raise ValueError('C4 constructed pilots absent from C1 plan')
    named_profiles = []
    for p in authors:
        named_profiles.append(dict(id=p['id'], occupations=list(p.get('profile', {}).get('occupations') or []),
                                   routine_role=(p.get('routine') or {}).get('role', '')))
    wf_people = winterfell['residents']
    wf_roles = collections.Counter(p.get('job', '') for p in wf_people)
    role_ids = set(roles)
    wf_ids = set(wf_roles)
    catalog_ids = {x['id'] for x in catalog}
    cross_reference = collections.defaultdict(set)
    for p in named_profiles:
        for occ in p['occupations']:
            cross_reference[occ].add(p['id'])
    catalog_rows = []
    for origin, entries in [('occupations.json', occupations), ('occupations_extensions.json', extensions)]:
        for p in entries:
            occ = p['id']
            catalog_rows.append({
                'source': origin, 'occupation_id': occ, 'name': p.get('displayName', ''),
                'category': p.get('category', ''), 'source_provenance': p.get('provenance', ''),
                'direct_c1_role': occ if occ in role_ids else '',
                'c1_building_if_exact_role': roles.get(occ, ''),
                'winterfell_exact_job_count': wf_roles.get(occ.upper(), 0),
                'named_profile_examples': ', '.join(sorted(cross_reference.get(occ, set()))[:6]),
                'KEEP_CHANGE_REMOVE': '', 'assign_to_npcs_NOW_OR_LATER': '',
                'desired_building': '', 'region_culture_conditions': '', 'reviewer_notes': '',
            })
    named_rows = []
    for p in named_profiles:
        named_rows.append({'source': 'authored_character_profile', 'identifier': p['id'],
                           'occupation_or_role': ', '.join(p['occupations']), 'person_count': 1,
                           'status': 'profile metadata, not proof of an actual staffed workplace'})
    for role, count in sorted(wf_roles.items()):
        named_rows.append({'source': 'winterfell_v2_roster', 'identifier': 'winterfell',
                           'occupation_or_role': role, 'person_count': count,
                           'status': 'roster plan; native NPC import may depend on opted-in runtime state'})
    building_rows = []
    for b in sorted(c3['templates'], key=lambda x: x['id']):
        possible = sorted(role for role, building in roles.items() if building == b['id'])
        building_rows.append({
            'building_id': b['id'], 'family': b.get('family', ''),
            'width_blocks': b.get('c1Width', ''), 'depth_blocks': b.get('c1Depth', ''),
            'max_staff_planning': b.get('maxStaff', 0), 'floors_physical': b.get('floors', ''),
            'requires_shore': b.get('requiresShore', False),
            'requires_mountain': b.get('requiresMountainResource', False),
            'requires_policy': b.get('requiresPolicy', False),
            'c1_roles_direct': ', '.join(possible),
            'desired_footprint': '', 'desired_staff': '',
            'economic_area_needed': '', 'reviewer_notes': ''})
    actual_buildings = collections.defaultdict(collections.Counter)
    for pilot in c4['pilots']:
        actual_buildings[pilot['siteId']].update(x['kind'] for x in pilot['lots'])
    site_rows = []
    for s in sorted(all_sites, key=lambda x: x['siteId']):
        d = s.get('buildingDemand', {})
        a = s.get('workingAnchor') or {}
        site_rows.append({
            'site_id': s['siteId'], 'name': s.get('name', ''), 'tier': s.get('tier', ''),
            'mode': s.get('blueprintMode', ''), 'x_candidate': a.get('x', ''),
            'z_candidate': a.get('z', ''), 'population_plan': d.get('peopleTarget', 0),
            'worker_plan': d.get('workforceTarget', 0),
            'households_plan': d.get('householdsEstimated', 0),
            'housing_building_demand': d.get('housingBuildingsDemand', 0),
            'housing_first_phase': d.get('firstPhaseHousingPlots', 0),
            'housing_backlog': d.get('housingBuildingBacklog', 0),
            'workplaces_json': json.dumps(d.get('workplaceCounts', {}), sort_keys=True),
            'c4_pilot': s['siteId'] in pilot_ids,
            'c4_planned_lots': sum(actual_buildings[s['siteId']].values()),
            'c4_planned_homes': actual_buildings[s['siteId']].get('HOUSE', 0),
            'terrain_approved': 'NOT_ESTABLISHED',
            'reviewed_job_distribution': 'NO',
            'reviewer_notes': '',
        })
    cross_rows = []
    for role in sorted(role_ids | {x.lower() for x in wf_ids}):
        building = roles.get(role, '')
        label = ('ATLAS_ROLE' if role in role_ids else 'WINTERFELL_ROLE_ONLY')
        cross_rows.append({
            'role_key': role, 'source': label,
            'exact_reference_occupation_exists': role in catalog_ids,
            'winterfell_roster_count': wf_roles.get(role.upper(), 0),
            'atlas_workplace': building, 'physical_template_exists': building in buildings if building else False,
            'desired_reference_occupation_id': '', 'desired_building': '',
            'assign_npcs_now_or_later': '', 'reviewer_notes': ''})
    counts = collections.Counter(x.get('tier', '') for x in all_sites)
    modes = collections.Counter(x.get('blueprintMode', '') for x in all_sites)
    summaries = [
        '# C4.3 read-only job / settlement reconciliation', '',
        '**Status: REVIEW ONLY. No construction activation, NPC mutation, or job assignment.**', '',
        f'- Occupation references: {len(catalog)} ({len(occupations)} core, {len(extensions)} extension).',
        f'- Atlas detailed job-role keys: {len(roles)}.',
        f'- Winterfell V2 roster: {len(wf_people)} planned residents across {len(wf_roles)} job/status labels.',
        f'- Named character files: {len(named_profiles)}.',
        f'- Physical architectural templates: {len(buildings)}.',
        f'- C1 physical sites: {len(all_sites)}; tiers: {dict(sorted(counts.items()))}.',
        f'- C1 generation modes: {dict(sorted(modes.items()))}.',
        f'- C4 currently enabled pilot site IDs: {sorted(pilot_ids)}.',
        '', '## Blocking mismatches', '',
        '- Catalog occupations, atlas role keys, Winterfell assigned job labels, and building types are separate vocabularies.',
        '- Reference occupation existence does not imply NPC assignment, and a rendered workstation does not imply productive labor.',
        '- C1 computes estimates; C4 caps these estimates to two physical pilots.',
        '- Neither active worker reassignment nor global construction is authorized by this audit.',
        '- Land/rural area capacity and income/production still need independent integration.',
        '', '## Review exports', '',
        '- `01_occupation_catalog.csv`: all reference professions, editable review columns.',
        '- `02_assigned_npc_occupations.csv`: authored-profile fields and Winterfell roster job counts.',
        '- `03_physical_building_catalog.csv`: 43 dimensions/staff values and planning role links.',
        '- `04_role_building_crosswalk.csv`: atlas roles, Winterfell labels, direct matches and editable decisions.',
        '- `05_site_readiness.csv`: all C1 sites, demand and existing pilot-lot counts.',
        '', 'Do not treat this as C4.3 completion or authorize worldwide construction from these files.',
    ]
    print('\n'.join(summaries[:11]))
    if opt.check:
        print('CHECK PASSED; repository files unchanged; no reports written.')
        return
    out = opt.out if opt.out.is_absolute() else ROOT / opt.out
    out.mkdir(parents=True, exist_ok=True)
    write_csv(out / '01_occupation_catalog.csv', list(catalog_rows[0]), catalog_rows)
    write_csv(out / '02_assigned_npc_occupations.csv', list(named_rows[0]), named_rows)
    write_csv(out / '03_physical_building_catalog.csv', list(building_rows[0]), building_rows)
    write_csv(out / '04_role_building_crosswalk.csv', list(cross_rows[0]), cross_rows)
    write_csv(out / '05_site_readiness.csv', list(site_rows[0]), site_rows)
    (out / 'C4_3_READONLY_AUDIT.md').write_text('\n'.join(summaries) + '\n', encoding='utf-8')
    print(f'Review-only reports created at: {out}')


if __name__ == '__main__':
    main()
