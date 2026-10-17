# Winterfell 298 AC — playable population and architecture case study, first vertical slice

**Status: WORKING DATA GENERATOR + TEST-ONLY NPC PROXIES; NOT PRODUCTION NPC MIGRATION OR FINAL CASTLE BUILD.**

## A. NW source dossier findings

The authoritative repo research input is `docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_NW_298AC_EXPANDED.md` § D.28 and L.1–L.3. It says Winterfell's **castle built-site** target is **250–1,100** residents [M], distinct from the seasonal winter town and rural jurisdiction. Defenses need 50–220 drawn from the same real population, and it specifies the two granite walls, godswood, glass gardens, crypts, towers, winter town, guesthouse, hot springs, supply and farmland. The holding is `winterfell_domain`, held by Eddard Stark at day zero. All quoted numbers are explicitly *mod planning estimates*, **not canon censuses**.

See [Winterfell](https://awoiaf.westeros.org/index.php/Winterfell), [Winter town](https://awoiaf.westeros.org/index.php/Winter_town), *A Game of Thrones* chapters Bran I, Catelyn I and associated Winterfell material. Winter town is mostly empty during the long summer: less than 1/5 of houses occupied. A proposed winter capacity around 15,000 is not a verified simultaneous day-zero census. Castle and town distinct; visiting royal entourage not residents. Old gods dominant but sept/Septa Mordane/Septon Chayle demonstrate diversity.

**Scenario anchor:** `scenario_start.json` states the royal entourage **has arrived** at Winterfell but the crypt conversation, Bran's fall, Eddard's Hand appointment and later war have **not** happened.

## B. Selected exact *mod calibration* for the single case-study estate cluster

This is a 1,200-resident **test area** of castle, summer wintertown, and adjacent rural hamlets. It is *not* the North-wide census, all of Stark's domain, the winter town when full, or a historical population number.

| Primary residence zone | Children 0–15 | Working-age 16–60 | Elders 61+ | Permanent residents | Occupied households |
|---|---:|---:|---:|---:|---:|
| Winterfell castle + outer service complex | 75 | 190 | 35 | **300** | **55** |
| Summer winter town outside walls | 70 | 145 | 25 | **240** | **57** |
| Associated rural hamlets and isolated homesteads | 185 | 409 | 66 | **660** | **145** |
| **TOTAL** | **330** | **744** | **126** | **1,200** | **257** |

The 320 proposed winter town cottage/house plots are mostly vacant in long summer: 57 active (17.81%), 263 vacant. This **does not add 263 × nominal capacity to residentPopulation**. In later winter arrivals, people must move house/residence from existing rural IDs rather than new people being created.

### C. Worker roles: all 744 employed-adult slots counted once

| Role | Castle | Winter town | Rural | Total |
|---|---:|---:|---:|---:|
| Guard | 80 | 2 | 0 | 82 |
| Household service | 45 | 25 | 0 | 70 |
| Administration | 16 | 0 | 0 | 16 |
| Artisan and smith | 15 | 20 | 0 | 35 |
| Food preparation and service | 20 | 30 | 0 | 50 |
| Stable, stock and animal husbandry | 5 | 0 | 38 | 43 |
| Healer, clergy, religious institution | 4 | 4 | 0 | 8 |
| Trade | 0 | 30 | 0 | 30 |
| Builder and carpenter | 0 | 17 | 16 | 33 |
| Transport | 0 | 10 | 15 | 25 |
| Forester, hunter | 0 | 5 | 30 | 35 |
| Farmer | 0 | 0 | 310 | 310 |
| Other workers | 5 | 2 | 0 | 7 |
| **TOTAL** | **190** | **145** | **409** | **744** |

**Not canon:** every number is an explicit design parameter. Roles are primary occupations, not an assertion of uninterrupted working hours; farm and stable households additionally include children, elders and domestic labor. The 82 guards are a subset of the 1,200 people, **never 82 additional soldiers**. Crafts, food service and agricultural work do not produce inventory until Step 2.2 economy owns labor and goods.

## D. Named characters at this exact start

Existing authored characters **permanent residents** (preserve original authored IDs and *existing* runtime NpcIds):
`eddard_stark`, `catelyn_stark`, `robb_stark`, `sansa_stark`, `arya_stark`, `bran_stark`, `rickon_stark`, `jon_snow`, `theon_greyjoy`. Jon is NOT yet sworn to the Night's Watch; Bran has NOT fallen; Theon is a ward/hostage, not an independent landed lord.

Existing authored **present but not Winterfell residents:**
`robert_baratheon`, `cersei_lannister`, `jaime_lannister`, `tyrion_lannister`, `benjen_stark`. The first four are part of the royal visit, Benjen is a Night's Watch member visiting. They use **guest beds** and their real home institutional/household IDs, and contribute **zero** to the 1,200 permanent Winterfell resident count.

Book-attested Winterfell household / guard persons not yet in the authored nine-person scenario pack have provisional book-named IDs `wf_named_*` in the **DRAFT roster**: Maester Luwin, Vayon Poole, Jeyne Poole, Rodrik Cassel, Beth Cassel, Jory Cassel, Septa Mordane, Septon Chayle, Hullen, Harwin, Joseth, Farlen, Palla, Old Nan, Hodor, Gage, Mikken, Hallis Mollen, and a selection of named guards (Desmond, Jacks, Porther, Quent, Alyn, Tomard, Varly, Heward, Cayn and Wyl). Sources: [Winterfell's household](https://awoiaf.westeros.org/index.php/Winterfell), *A Game of Thrones*, and associated character pages. **Provisional age estimates and unsourced intimate biographical detail are not claimed as canon.** A few guard names, spellings and exact attendance must still be verified against individual book chapters before promotion into primary scenario data.

### E. Richest schema and profile parity

Of the current starting party, **Eddard** has the richest relevant authored character record. He provides 20 `skills`, 20 `aptitudes`, 26 `dispositions`, 33 `characterValues`, 25 `socialNorms`, motivations/goals/fears/desires, family and political ties, appearance, dialogue persona, aliases, offices, social identity, belief and private/public knowledge. Eddard's authored profile must be preserved **exactly**.

The build script creates **14 full-shape character proposals** under `docs/step3/winterfell_case_study/profile_proposals/`; it does NOT replace `characters/*.json`. Existing values and relations are never overwritten. Added numerical dimensions for children are schema-completion **mod estimates**, not lore, and are deliberately conservative for abilities that develop with age. Adult orientation and private secrets are never invented; unknown is left `UNSPECIFIED` or empty. Users should review proposed diffs before migrating via the existing `AuthoredCharacterProfileOverlayService` and authoringVersion pathway.

### F. Role, genealogy, household and housing commitments

- A household is a consumption/residential unit, not a dynasty, social house, government, structure or one person per bed.
- `257` occupied primary residences meet all 1,200 people's home IDs. A 9-person Stark household is preserved. Some quarters should become physically shared barracks, staff wings and castle family rooms rather than 55 detached castle houses.
- Known family associations from books are kept together: Vayon/Jeyne, Rodrik/Beth, Hullen/Harwin, Farlen/Palla, Old Nan/Hodor. **These are relation hints**, not automatically assigned biological father/mother in every case.
- Each synthetic child may have realistic same-household parents/guardian, but the *biological relation remains provisional* until the genealogy system owns the record. No 8-year-old becomes a professional guard.
- Each working adult receives one identified workplace slot. The case draft allocates **111** planned workplace buildings/units based on capacity; future road/soil audits and physical placement determine the final layouts.
- Total plan contains **631 building/plot units**: 257 occupied residence slots + 263 vacant seasonal wintertown houses + 111 workplace buildings. Every unit is **PLANNED** (`placed=false`).
- The generator must be idempotent for identical inputs and seed; the future physical construction planner requires ground survey, connected roads, buildings with working doors/beds, work facilities, farm plot areas, season-appropriate materials, and no collision with castle walls.

### G. Existing-system ownership and migration gate

Do **not** register any `wf298_*` draft person into a second runtime character manager. These are candidate authored IDs. When approved, a **one-time migration** must:
1. Check `AuthoredIdRegistry.findNpc(candidateId)` before any creation, reuse 9 existing Stark/ward IDs, preserve guest IDs.
2. Create each missing person **once** via `NpcRegistry.create`, register mapping via `AuthoredIdRegistry.registerNpc`, create one `CharacterProfile` in `CharacterProfileManager`, and use existing religion/culture, legal, genealogy and affiliation owners. Do not store parallel mutable copies of these fields.
3. Bind each person to one `SettlementId` and household; create the three or more physical settlement IDs rather than placing 1,200 people in the tiny castle `Settlement`.
4. Save via `NpcSimulation.save()` and run save/reload **twice** to establish zero additional IDs. Validate no conflicting existing save or scenario authoringVersion migrations.
5. Make temporary villager proxies resolve back to that `NpcId`; only then turn on role routines, wages, production, inventories, levies, consumption, birth/death or NPC conversational actions.

Existing reference documents: `docs/architecture/20_3_POPULATION_AND_HOUSEHOLDS.md`, `20_3_ECONOMY_DOMAIN.md`, `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`, `NpcRegistry`, `AuthoredIdRegistry`, `CharacterProfileManager`, `GenealogyManager`, `NpcAffiliationManager`, `SettlementManager`.

**Current interactive scope:** The generated `/function knownworld:case_study/winterfell_npcs` summons 24 *no-AI test villager proxies* in a disposable flat world. Right-clicking a tagged proxy displays culture, faith, job, home, lord, traits and 3 skills in chat, **read from the stable draft identity record**. It is NOT yet a full GUI, does NOT register new simulation NPCs, and does NOT construct the 631 planned properties physically. This prevents a demonstration from silently corrupting a permanent save. Avoid spawning 1,200 Minecraft entities at once.

### Pass/fail before promoting this case study

- [x] One resident list; 1,200 unique IDs, 1,200 household memberships, 744 job/workplace assignments.
- [x] Nine existing resident authored IDs reused in draft; visitors explicitly excluded.
- [x] Character proposals supplement 14 existing records without overwriting old values.
- [x] JSON, housing/occupation/census generator and proxy summoning function exist.
- [ ] In-game Minecraft 26.3 Java compile and actual interaction tested; may require API fixes.
- [ ] Real castle and rural housing **constructed** and surveyed; current winterfell_test is a structural dummy only.
- [ ] Physical roads, doors/beds, farmland ownership, household migration and save/load persistence completed.
- [ ] All 1,200 identities promoted once into actual NpcRegistry with authoritative NpcIds and persistence.
- [ ] Income, food production, age progression, memory/relationships and military logistics connected to the real simulation.
