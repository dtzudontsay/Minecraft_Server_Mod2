# Known World — Step 2.1 Research: SWE (Southwestern Essos and the Valyrian approaches)

**Scenario:** 298 AC book-continuity starting state; avoid importing later-series wars or appointments into the starting date without explicit timeline data.  
**Source map:** `swe.png` annotated by the project author.  
**Status:** Researched **first-pass place and region profiles**, not a completed census, final location correction, or Step 2.2 code.  
**Last researched:** 2026-10-17.  
**Next stage:** integrate with geographical IDs, holdings, governments, real person IDs and regional generators **after** global cross-submap reconciliation.

## 0. Method and non-negotiable design rules

- `[C]` = source-supported historical/world fact; `[R]` = matching repository structure/ID; `[I]` = geographical/historical deduction; `[E]` = provisional mod numeric/design assumption. Citation links and the source list are at the end.
- Population envelopes, troop estimates, job mixes, soil scores, commodity proportions, ship/farm counts and exact ruler assignments **are not canonical numbers**. The data are not yet balanced to any specific full-world population total.
- Population counts must become **actual identifiable people** with persistent household, residence, birthplace, workplace, social position, culture/religion and current physical or abstract location. Aggregated work/economy calculations can optimize over those people but may not create fictional independent farmers, soldiers, traders, enslaved workers or consumers.
- An enslaved person's **legal/social status is not an occupation**. Enslaved workers, soldiers, artisans, servants and farmhands are all individual people; record employment separately from condition and ownership/household relations. Do not encode this as a faceless resource quantity.
- **Government and House are separate dimensions**: Myrish magisters, Tyroshi archon, Lysene magisters, Volantene triarchs, etc. are offices, not feudal rulers. Existing Lysene and Volantene families are candidates for **actual family IDs**, not automatic proprietors of the full city.
- Distinguish a **ruined city**, a **partly inhabited ruin**, a **living city**, and a **geographic region/river hazard**. Never map each red X to a vanilla village.
- `SWE-NNN` are provisional detector observation IDs, not permanent city IDs. User will provide corrected/missed/false readings in a separate consolidated pass after all submaps. Do not prematurely delete or merge any source observation.
- Geographic sources use broad cartographic placements. Original regional pixels will be retained separately from future snapped building positions and from local terrain-footprint geometry.

## 1. Map observation ledger (all 20 SWE detections)

| Marker | Read name | Site interpretation at 298 AC | Tentative identity rule |
|---|---|---|---|
| SWE-001 | The Sorrows | Dreaded **river region / ruined Chroyane**, not a generic settlement | Geographic hazard region; independent `chroyane` ruins if mapped |
| SWE-002 | Myr | Active Free City | `myr` |
| SWE-003 | Tyrosh | Active Free City, island harbor | `tyrosh` |
| SWE-004 | Selhorys | Active walled Volantene town | `selhorys` |
| SWE-005 | Bhorash | Ruined city, northern Slaver's Bay | `bhorash` |
| SWE-006 | Valysar | Active walled Volantene town | `valysar` |
| SWE-007 | Mantarys | **Inhabited** post-Doom city-state, dangerous/isolated | `mantarys` |
| SWE-008 | Sar Mell | Ancient Rhoynar ruined city | `sar_mell` |
| SWE-009 | Volon Therys | Active large Volantene river town; detector association previously questioned | `volon_therys` |
| SWE-010 | Lys | Active island Free City | `lys` |
| SWE-011 | Volantis | Active very large Free City | `volantis` |
| SWE-012 | Lys vicinity annotation | Uncertain potential duplicate/non-X | Keep source observation only |
| SWE-013 | Lys vicinity annotation | Uncertain potential duplicate/non-X | Keep source observation only |
| SWE-014 | Tolos | Active slave-dependent port city | `tolos` |
| SWE-015 | Elyria | Active island city | `elyria` |
| SWE-016 | Ghozai | Ruined Isle of Cedars port | `ghozai` |
| SWE-017 | Velos | Ruined Isle of Cedars city | `velos` |
| SWE-018 | Oros | Ruins **with reported unknown inhabitants** | `oros`; no ordinary city census without evidence |
| SWE-019 | Tyria | Ruins **with reported unknown inhabitants** | `tyria`; no ordinary city census without evidence |
| SWE-020 | Valyria | Ruined catastrophic former Freehold capital | `valyria` |

There are **20 detector observations** but **not 20 separately inhabited settlements**. This dossier profiles **17 distinct possible built sites** (ten currently active cities/towns, seven ruins including Oros/Tyria) plus the Sorrows region; SWE-012 and SWE-013 are not additional cities. This is provisional until the later shared marker correction.

## 2. Population, defense, government and source discipline

Population = provisional *urban permanent residents at/around the settlement*. In places with slavery, **include enslaved people within the total**, do not count them a second time. Defense = **actual organized local defenders at start**, not total theoretical recruitment. All ranges `[E]`.

| Built site | Occupation state | Working envelope: residents | Standing urban defenders | 298 AC political / leading authority |
|---|---|---:|---:|---|
| Myr | ACTIVE | 150,000–450,000 | 2,000–8,000 plus contracted free companies | Magisters, no established singular hereditary city-lord |
| Tyrosh | ACTIVE | 140,000–350,000 | 2,000–8,000 plus fleet and hired forces | Elected archon / wealthy conclave |
| Selhorys | ACTIVE | 350,000–900,000* | 2,000–12,000 | Subordinate to triarchs of Volantis |
| Valysar | ACTIVE | 350,000–900,000* | 2,000–12,000 | Subordinate to triarchs of Volantis |
| Mantarys | ACTIVE, unusual | 10,000–50,000 | 200–2,500 uncertain | Independent local authority unknown |
| Volon Therys | ACTIVE | 350,000–900,000* | 2,000–12,000 | Subordinate to triarchs of Volantis |
| Lys | ACTIVE | 120,000–400,000 | 1,500–5,000 plus hired companies/ships | Conclave of magisters; First Magister; military gonfaloniere |
| Volantis | ACTIVE | 1,200,000–3,500,000 | 12,000–55,000 plus fleets and provincial forces | Three annually elected triarchs, Old Blood eligibility |
| Tolos | ACTIVE | 30,000–120,000 | 800–4,000 (skilled slingers among defenders) | Local polity; ruler not established |
| Elyria | ACTIVE | 12,000–65,000 | 300–2,500 plus harbor ships | Local polity; ruler not established |
| Bhorash | RUIN | 0 established | 0 established | No known surviving city government |
| Sar Mell | RUIN | 0 established | 0 established | No contemporary Rhoynar king |
| Ghozai | RUIN | 0 established | 0 established | No contemporary city government |
| Velos | RUIN | 0 established | 0 established | No contemporary city government |
| Oros | INHABITED RUINS? | Unknown; do not initialize an ordinary urban population | Unknown | Unknown and potentially hostile residents; reports are limited |
| Tyria | INHABITED RUINS? | Unknown; do not initialize an ordinary urban population | Unknown | Unknown and potentially hostile residents; reports are limited |
| Old Valyria | RUIN / extreme hazard | No established organized civilian population | No known organized garrison | Freehold destroyed by Doom |

**\* Critical canonical relative-size constraint:** Selhorys, Valysar and Volon Therys are expressly described as being larger than King's Landing and Oldtown, despite being administratively called *towns* [SWE-05][SWE-06][SWE-07]. The provisional 350–900k values are **not guaranteed compatible** with any fixed Westerosi urban census. **The final generator must enforce relative population/footprint ordering** when final canonical interpretation and world numbers are chosen. Do not shrink these towns to five thousand residents just because their category says TOWN. The status as Volantene dependencies is political, not a population-size indicator.

**Volantis scale:** Five enslaved persons per one free person is stated for Volantis [SWE-04]. Myr and Tyrosh are described as roughly three enslaved per freeborn [SWE-02][SWE-03], and Lys about three to one [SWE-11]. These are lore-reported ratios, not a license to model humans as commodities without identities, or to assume identical conditions in surrounding countryside and every satellite community. Scenario-scale corrections must be decided consistently.

## 3. Active settlement dossiers
### SWE-002 — Myr (`myr`; target **ACTIVE**)

- **Political and leading-person research [C]:** Walled Free City, on fertile continental coast; magisters govern [SWE-02]. No confirmed current individual named as sole ruler in 298 AC.
- **Cultural, religious and demographic factors [C/I]:** `myrish` dominant, varied slave and mercantile migrant origins. Religious diversity; `rhllor` attested among several temples. Slavery ~3:1 enslaved:freeborn reported.
- **Economy and occupational mix [C/E]:** Specialty glass, lenses, lace, carpets, dyes, painting, miniatures, wines, ships, banking. Employment [E] active adults: primary food 8%, workshops/artisans 38%, trade/transport 24%, services/households 21%, civic/defense 9%.
- **Fertility, farming, crops, livestock [I/E]:** Fertile heel: vineyards, orchards, vegetables, cereals and sheep/poultry as hypotheses. Fertility index 0.55–0.80 in viable lowlands; urban workers fed partly from hinterlands.
- **Settlement generation archetype [C + DESIGN]:** City walls, glass kilns and lensmakers, tight artisan quarters, workshops, textile districts, bazaar, diverse temples, mills and agricultural satellite villages.
- **Integration and alignment:** Not a Westerosi manor with a hereditary Lord Myr. Existing `myr` reference; no completed holding to invent.

### SWE-003 — Tyrosh (`tyrosh`; target **ACTIVE**)

- **Political and leading-person research [C]:** Island Free City guarding maritime passage; archon selected from wealthy/noble conclave [SWE-03]. Incumbent at exact 298 AC unverified.
- **Cultural, religious and demographic factors [C/I]:** `tyroshi` dominant, foreign traders/sellswords; many faiths including `trios`, `rhllor`, `pattern`. ~3:1 enslaved:freeborn reported for city.
- **Economy and occupational mix [C/E]:** Sea-snail dyes, colored fabrics, trade, fine armor/helms, pear brandy, shipbuilding, mercenaries. Active workforce [E]: primary fishing 8%, craft/dye 31%, trade/ships 31%, service 20%, defense/civic 10%.
- **Fertility, farming, crops, livestock [I/E]:** Rocky island scarce farm acreage; fishing/shellfish, seawater dye resources, imported grain, offshore orchards and hinterland provisioning. Fertility index on urban island 0.10–0.30.
- **Settlement generation archetype [C + DESIGN]:** Bleeding Tower at harbor entrance, fortified sea walls, dye vats near water, shipyards, ornate towers, densely packed colorful market districts.
- **Integration and alignment:** Military service and mercenary companies represent actual people; known archon is an office, not House Tyrosh.

### SWE-004 — Selhorys (`selhorys`; target **ACTIVE**)

- **Political and leading-person research [C]:** Huge but administratively subordinate walled Volantene town at Selhoru/Rhoyne confluence, on east bank; towers and domes [SWE-05].
- **Cultural, religious and demographic factors [C/I]:** `volantene` majority plus mixed river trade, slaves, Volantene institutions, minority `rhoynar` influences. Religion varied, with `rhllor` and `old_gods_of_valyria` regionally possible, not fixed percentages.
- **Economy and occupational mix [C/E]:** River cargo, timber from Qohor, ship repair, grain markets, food processing, trades. Workers [E]: primary 12%, crafts 25%, transport/trade 34%, services 20%, civic/defense 9%.
- **Fertility, farming, crops, livestock [I/E]:** Floodplains and tributary irrigation: grain, vegetable gardens, orchards, livestock feed, river fisheries; fertility 0.60–0.90 where flood control supports cultivation.
- **Settlement generation archetype [C + DESIGN]:** Enormous walled river town with harbor, market wharves, towers/domes, gates, floodplain agriculture and bridge/ferry logistics.
- **Integration and alignment:** Volantene taxes/army authority; high Dothraki exposure because eastern bank. Relative size constraint overrides TOWN category.

### SWE-006 — Valysar (`valysar`; target **ACTIVE**)

- **Political and leading-person research [C]:** Large Volantene-controlled walled town on west bank of Rhoyne [SWE-06].
- **Cultural, religious and demographic factors [C/I]:** `volantene` base; transit merchants and captive/migrant populations, river corridor influences. Multi-faith as above.
- **Economy and occupational mix [C/E]:** Forest of Qohor timber, river transport, woodworking, mills, commerce. Adult workforce [E]: primary 15%, crafts 27%, transport/trade 31%, services 18%, civic/defense 9%.
- **Fertility, farming, crops, livestock [I/E]:** River floodplain fields, grazing margins, freshwater fish, grain/crops and orchard plots inferred. Fertility 0.60–0.85 near managed lowlands.
- **Settlement generation archetype [C + DESIGN]:** Huge walled town, docks, timber stacks, gristmills, markets, mixed districts and country estates.
- **Integration and alignment:** Subordinate to Volantis; do not create a separate reigning dynasty. Relative-size constraint essential.

### SWE-007 — Mantarys (`mantarys`; target **ACTIVE**)

- **Political and leading-person research [C]:** Living city-state to north of Lands of Long Summer, on Demon Road. Founded as Valyrian dependency; survives Doom; reputation for monstrosity [SWE-12].
- **Cultural, religious and demographic factors [C/I]:** No clearly established contemporary people-group/religion percentages. Candidate `valyrian` heritage; do **not** make all residents mutants or assert fabricated biology. Faith unresolved; use `unknown_religion` where necessary.
- **Economy and occupational mix [C/E]:** Speculative survival economy: passage tolls, scavenging, metal/stone, guarded caravan services, irrigated plots. Adult workforce [E]: primary 27%, crafts 22%, transport/trade 16%, services 19%, guards/authority 16%.
- **Fertility, farming, crops, livestock [I/E]:** Dry hazardous margin of Painted Mountains; oasis/river valley patchy cultivation; grains, goats and fodder only if actual land/water viability. Fertility 0.10–0.45.
- **Settlement generation archetype [C + DESIGN]:** Threatening walled town, old Valyrian roads, damaged structures, fortresslike compounds, irregular rebuilding; **not** a demon carnival unless justified by implementation/lore.
- **Integration and alignment:** 298 AC local ruler unknown; later ADWD diplomatic acts must not be pre-applied to starting date.

### SWE-009 — Volon Therys (`volon_therys`; target **ACTIVE**)

- **Political and leading-person research [C]:** Large dependent Volantene town on west bank opposite Sar Mell [SWE-07]. Has high white walls and riverfront; formerly damaged by Rhoynar floods.
- **Cultural, religious and demographic factors [C/I]:** `volantene` with trade/legacy Rhoynar influences; multi-faith distribution unresolved.
- **Economy and occupational mix [C/E]:** Major river traffic; Qohor timber, mills, agriculture, river storage and canal access. Adult workers [E]: primary 14%, crafts 26%, trade/transport 32%, services 19%, civic/defense 9%.
- **Fertility, farming, crops, livestock [I/E]:** Fertile margins plus significant flood hazard: grain, orchards, grazing, river fish; site fertility 0.50–0.85 when protected.
- **Settlement generation archetype [C + DESIGN]:** High white walls; long crowded wharves, reclaimed flood areas, robust foundations, canal remains, bridges/ferries and satellite farms.
- **Integration and alignment:** Authority directly under triarchs of Volantis; mandatory relative population rule.

### SWE-010 — Lys (`lys`; target **ACTIVE**)

- **Political and leading-person research [C]:** Valyrian-descended island Free City, conclave of magisters, First Magister and gonfaloniere [SWE-11].
- **Cultural, religious and demographic factors [C/I]:** `lysene` majority among longstanding urban residents; many imported/enslaved cultural backgrounds; diverse shrines including `rhllor`, `yndros_of_twilight`, `saagael`, `pantera`, `bakkalon`. Reported enslaved:freeborn ~3:1.
- **Economy and occupational mix [C/E]:** Maritime commerce, perfumes, fine tapestries, wine, fruit, shipbuilding, pleasure gardens and slavery. Adult workers [E]: primary 7%, craft/processing 25%, transport/trade 30%, services/households 30%, civic/defense 8%.
- **Fertility, farming, crops, livestock [I/E]:** Fertile island horticulture: fruit, grapes, palms/fish; some grain imports. Fertility 0.50–0.75 on available island fields (not over all sea/rock).
- **Settlement generation archetype [C + DESIGN]:** Dense mercantile terraces, harbors, pleasure gardens, temples, white/colorful stone, fortified walls and villas.
- **Integration and alignment:** Existing Lysene families in repo are **family references**, not permanent ruling house or fixed current officeholders.

### SWE-011 — Volantis (`volantis`; target **ACTIVE**)

- **Political and leading-person research [C]:** Capital of its powerful freehold at Rhoyne mouth; three annually elected Old Blood triarchs. Tiger/Elephant political factions; Black Walls and Long Bridge [SWE-04][SWE-08].
- **Cultural, religious and demographic factors [C/I]:** `volantene` politically core, but large multi-origin population. Within Black Walls old Valyrian rites; outside `rhllor` significant; other faiths tolerated/represented. City-reported ~5 slaves:1 free person.
- **Economy and occupational mix [C/E]:** Maritime and river shipping, spice/silk imports, glass, sweet red wine, beet crops, docks, fisheries, finance, crafts, slave labor. Adults [E]: primary 7%, craft/processing 30%, trade/transport 31%, services/households 23%, civic/defense 9%.
- **Fertility, farming, crops, livestock [I/E]:** Humid fertile Rhoyne delta: beets attested, vineyards, horticulture, grain fields and freshwater aquaculture inferred; fertility 0.65–0.95 where controlled, severe flood/disease hazards.
- **Settlement generation archetype [C + DESIGN]:** Massive multi-district river metropolis; Black Walls inner aristocratic core; Long Bridge shops; enormous R'hllor temple, docks, triarchal venues, east/west administrative differences.
- **Integration and alignment:** `volantis_domain` is active in landed holdings. Existing Old Blood families in repo include Maegyr, Paenymion, Qhaedar, Staegone, Tagaros, Vaelaros, Vhassar. Never auto-assign all as triarchs.

### SWE-014 — Tolos (`tolos`; target **ACTIVE**)

- **Political and leading-person research [C]:** Inhabited port on Black Cliffs; noted lead-pellet slingers; slave-dependent, and Ghiscari connections [SWE-13].
- **Cultural, religious and demographic factors [C/I]:** `tolosi` culture already in repo, interaction with neighboring Ghiscari (`ghiscari`) and Valyrian heritage uncertain. Religion undocumented; refrain from asserted percentages.
- **Economy and occupational mix [C/E]:** Harbor trade, ship repair, commercial slave labor, metal/lead supply for slingers, local crafts, goods from Ghiscari coast. Workforce [E]: primary 13%, craft/extraction 27%, trade/ships 30%, services 20%, civic/defense 10%.
- **Fertility, farming, crops, livestock [I/E]:** Steep coastal position: terraces, port provisioning, goats/sheep, grain from hinterland or imports, some vineyards; fertility 0.20–0.55.
- **Settlement generation archetype [C + DESIGN]:** Black Cliffs fortifications, cliffside quay, drill yards for slingers, mercantile districts and old Valyrian roads.
- **Integration and alignment:** Hizdahr zo Loraq later has ties/kin here: do not assign him rule of city; later Yunkish alliance not assumed at day 0.

### SWE-015 — Elyria (`elyria`; target **ACTIVE**)

- **Political and leading-person research [C]:** Inhabited island city preserved after Doom; links to Ghiscari cities, later political involvement in Meereen conflicts [SWE-14].
- **Cultural, religious and demographic factors [C/I]:** Cultural admixture of Valyrian-descended and Ghiscari-connected populations plausible; precise `valyrian`/`ghiscari` proportions unsupported. Faith unresolved.
- **Economy and occupational mix [C/E]:** Maritime trade, warehouses, fishing and shipping, trade with Tolos and Slaver's Bay. Adult workers [E]: primary 15%, craft/shipbuilding 25%, maritime trade 34%, service 19%, civic/defense 7%.
- **Fertility, farming, crops, livestock [I/E]:** Small island: fishing and salt, modest horticulture, livestock, food imported by sea; fertility 0.20–0.55.
- **Settlement generation archetype [C + DESIGN]:** Island harbor, merchant palaces and warehouses, wind-sheltered streets, old Valyrian masonry, offshore docks.
- **Integration and alignment:** Local leader unknown; kinship with Hizdahr later is not automatic Ghiscari rule.

## 4. Ruins, partial survivors and non-city observations

### SWE-005 — Bhorash (`bhorash`; **RUIN / UNCERTAIN**)

- **Known state:** Destroyed city on northern Slaver's Bay coast between former road routes; founding attribution to Freehold uncertain [SWE-15].
- **298 AC resident/authority rule:** 0 urban residents/defenders; no living government or dynasty assumed. Temporary trespassers/scavengers possible **only** if modeled.
- **Economic/agricultural rule:** Ruined coast and former Valyrian road intersections; no automatic food production, fortified active city, or ordinary housing.
- **Generation profile:** Collapsed harbor, overrun road and ruined coastal masonry.
- **Identity and historical culture:** Historical city culture and religion largely unknown.

### SWE-008 — Sar Mell (`sar_mell`; **RUIN / UNCERTAIN**)

- **Known state:** Ruined Rhoynar city east of Rhoyne opposite Volon Therys, devastated in Rhoynish Wars [SWE-09].
- **298 AC resident/authority rule:** 0 urban residents/defenders; no valid contemporary Rhoynar king.
- **Economic/agricultural rule:** Nearby fertile Rhoyne margins do **not** imply active Sar Mell households; generate separate country settlements if geography supports them.
- **Generation profile:** Pale abandoned river city, destroyed monumental flower courts, flood-scarred ruins.
- **Identity and historical culture:** Historical `rhoynar`; faith possibly `mother_rhoyne` as heritage, not living population allocation.

### SWE-016 — Ghozai (`ghozai`; **RUIN / UNCERTAIN**)

- **Known state:** Ruined northern Isle of Cedars slaver port destroyed by tsunami during Doom; broken pyramids, monkeys [SWE-16].
- **298 AC resident/authority rule:** No established permanent community.
- **Economic/agricultural rule:** Surrounding island forest/fish may sustain *other* sites. No reactivated slaver port without explicit event.
- **Generation profile:** Slaver-port ruins, crumbled brick stepped pyramids, monkey-infested jungle.
- **Identity and historical culture:** Historical material culture Ghiscari-related, details unresolved.

### SWE-017 — Velos (`velos`; **RUIN / UNCERTAIN**)

- **Known state:** Ruined southern Isle of Cedars city destroyed by same tsunami; historical pink marble and cedar palaces [SWE-17].
- **298 AC resident/authority rule:** No established permanent urban community.
- **Economic/agricultural rule:** Survivor fisherfolk at sea at time of Doom do not imply 298 AC continuation of the same city.
- **Generation profile:** Broken cedar/pink-marble manses and shattered sea walls, coastal overgrowth.
- **Identity and historical culture:** Historical culture not safely reduced to living `ghiscari` people.

### SWE-018 — Oros (`oros`; **RUIN / UNCERTAIN**)

- **Known state:** Post-Doom ruin on Smoking Sea coast, formerly extraordinarily grand; some unknown inhabitants reportedly survive [SWE-18].
- **298 AC resident/authority rule:** Unknown number/type; **do not initialize 0 as factual** and do not invent a normal urban census/garrison.
- **Economic/agricultural rule:** Unreliable hostile environment; avoid ordinary farmland/merchant traffic without exceptional safety model.
- **Generation profile:** Collapsed monumental terraces, broken dragonroad, ashen/cursed waterfront and ruined walls.
- **Identity and historical culture:** Identity of surviving residents and religion unknown; future bespoke quest/exploration data.

### SWE-019 — Tyria (`tyria`; **RUIN / UNCERTAIN**)

- **Known state:** Post-Doom ruin across Smoking Sea from Oros; some inhabitants reportedly survive [SWE-19].
- **298 AC resident/authority rule:** Unknown; no evidence for functioning city government or regular garrison.
- **Economic/agricultural rule:** Volcanic/ash danger, no normal crop regime.
- **Generation profile:** Broken old Valyrian towers, severed roads, ruined island town.
- **Identity and historical culture:** Do not turn reported survivors into a generic Valyrian noble dynasty.

### SWE-020 — Valyria (`valyria`; **RUIN / UNCERTAIN**)

- **Known state:** Destroyed capital of Freehold, Doom in 102 BC; Smoking Sea and volcanic shattered archipelago [SWE-20][SWE-21].
- **298 AC resident/authority rule:** No established ordinary civilian population, farm economy or governmental continuity at start.
- **Economic/agricultural rule:** Extreme volcanic, airborne and supernatural hazards; routine agriculture/merchant logistics forbidden in ruins.
- **Generation profile:** Unique iconic ruined capital, topless towers, sphinxes, surviving old dragonroads, volcanic terrain. Requires hand-authored landmark templates.
- **Identity and historical culture:** Known old dragonlord families are historical—not resident ruling houses in 298 AC.

### SWE-001 — The Sorrows (region, not a city)

- The Sorrows is a fog-shrouded stretch of the Rhoyne around the **ruins of Chroyane**, including Bridge of Dream, Palace of Sorrow, stone men afflicted by greyscale, and river hazards [SWE-01].
- **Not** an additional conventional town. `chroyane` can be a separate ruin reference; `the_sorrows` a hazard/river zone spanning the archaeological landscape.
- Local civic population: **no ordinary resident town target**. Stone men are individuals with a persistent identity, condition and location; if the NPC system cannot support this safely yet, leave exploration actor generation disabled rather than counting faceless monsters as townspeople.
- Travel model: fog, ship navigation, river hazards, greyscale exposure, dangerous islands; building model: half-submerged Rhoynar monuments, broken arches, tangled canals and the Bridge of Dream.
- Do not automatically extrapolate the Sorrows' extreme environment to all agricultural lowlands up- or downriver.

### SWE-012 and SWE-013 — Near Lys

These are unverified detector observations in the Lys annotation cluster. They are **not** two additional Lys settlements. Preserve for the consolidated marked-map audit; do not create new political entities, invented houses or population from them.

## 5. Zone research — starting from repository zones and finer proposed geography

The repo's existing `reference/territory_zones.json` has geographic polygons for `disputed_lands`, `lands_of_long_summer` and nearby major regions such as `ghiscar`. `reference/locations.json` also contains named features `lands_of_old_valyria` and `isle_of_cedars`. **Do not assume every named subregion has a validated polygon.** Minor coastlands, deltas, floodplains, islands and volcanic areas require geometry and geographic priority rules in Step 2.2.

| Zone / desired subzone | Culture/religion tendencies (conditional, not universal) | Land and production potential | Political / security | Density & generator controls |
|---|---|---|---|---|
| Sea of Myrth / Myr hinterland | `myrish`, many foreign artisans/merchants; `rhllor` among other faiths | Fertile fields, vineyards, workshops, glass/ceramics, routes to inland towns | Myrish city-state commercial reach | High agricultural villages near cultivable lowlands/roads; no extra free-city governments |
| Tyroshi island / Stepstones approaches | `tyroshi`, diverse visiting mariners | Dense harbor, fisheries, dye snails, imported grain | Tyroshi maritime, mercenaries and piracy risk offshore | Very dense main port, few arable islands; small fishing ports, piracy camps where appropriate |
| Lysene island and nearby waters | `lysene` with multi-origin enslaved/workforce population, multiple cults | Horticulture, fruit/wine, fish, shipping, perfumes | Magisterial merchant oligarchy | Dense coastal city/gardens, limited farmland, satellite isles only if coastal land |
| Disputed Lands | Overlapping Myrish/Tyroshi/Lysene influences; contested, not uniform single culture | Patchy cultivation, pastures, salt/coastal access | Border raids, free-company camps, no permanent shared lord | Security suppresses undefended villages; fortified agrarian sites and military roads |
| Rhoyne / lower river towns | `volantene`, trade migrants, historical `rhoynar` influence, diverse faiths | Alluvial high-fertility floodplains, canals, inland riverboats, timber transport | Volantene control with town garrisons; river pirates nearer Sorrows | Dense large riverside towns; floodplain villages with levees and drainage |
| The Sorrows / Chroyane ruins | Historical `rhoynar`, modern individual sufferers/passersby only | Fog/greyscale/ruined canals | Extremely dangerous, no ordinary law or garrison | Suppress ordinary residential placement inside hazard polygon |
| Orange Shore / Volantene coast | `volantene`, traders, mixed agrarian people | Humid coastal agriculture, ships, orchards, salt and food processing | Volantene hegemony | Dense rural farm hamlets where soil, water and security allow |
| Painted Mountains / Demon Road | Uncertain between Mantarys and former Valyrian dependency sphere | Pass-trade, marginal irrigated enclaves, upland livestock | Dangerous caravan corridor, ruined roads | Sparse guarded caravan stops and forts; avoid indiscriminate farm villages |
| Slaver's Bay west / Black Cliffs | `tolosi`, Ghiscari and Valyrian-descended influences; no unverified universal proportions | Cliff ports, trade, fishing, lead/stone, imported food | Independent local polities tied to Ghiscari trade | Port-driven settlements, steep-site rejection, some defensible upland villages |
| Isle of Cedars | Historical diverse trading colonies; no claim of surviving ancient cities | Forest/cedar, fisheries, coastal ecological resources | No established active city authority at Ghozai/Velos | Protect two ruin footprints; rare distinct living hamlets only if separately supported |
| Lands of the Long Summer / Old Valyria | Historic `valyrian`; do not auto-populate ancient capital | Ash, volcanic rock, shattered roads, toxic/hazardous terrain | Destroyed Freehold; uncertain surviving bands near Oros/Tyria | Suppress standard settlement villages, farming, normal patrols; special ruin generation |

### Quantitative environmental baselines — provisional knobs, not geographic truth

- `relativeCultivability` recommended *initial test intervals*, conditional on actual biome/soils and not used as production values by themselves: alluvial Rhoyne 0.65–0.95; Myrish hinterland 0.55–0.80; Lysene viable island patches 0.50–0.75; Tyroshi rocky island 0.10–0.30; Black Cliffs 0.20–0.55; volcanic old Valyria 0.00–0.05; dangerous Sorrows core = `settlementPermitted=false`.
- Add hydrology (floodplains/river width), salinity, slope, soil organic productivity, irrigation, temperature and security. A categorical polygon alone cannot know real site-level farm suitability.
- Historical ruin status and political ownership **must override** generic high-fertility/population-density rules. The same good soil may support *other* hamlets without resurrecting a ruined historical city.
- Cultural distributions should derive from provenance of **individual households and actual migration histories**, not overwrite every person's identity each tick based on their current residence.

## 6. Existing repository overlap / known IDs

**Inspected current master:**

- `src/main/resources/data/knownworld/reference/locations.json`: `myr`, `tyrosh`, `lys`, `volantis`, `disputed_lands`, `lands_of_long_summer`, `lands_of_old_valyria`, `isle_of_cedars`, `ghiscar`, and related region IDs. Many smaller towns/ruins in this dossier are not yet individually established there; Step 2.2 should *extend*, not duplicate, canonical IDs.
- `src/main/resources/data/knownworld/reference/territory_zones.json`: includes `disputed_lands`, `lands_of_long_summer`, `ghiscar`; do not infer site-specific polygons where none are present.
- `src/main/resources/data/knownworld/reference/cultures.json`: existing relevant IDs include `valyrian`, `myrish`, `tyroshi`, `lysene`, `volantene`, `tolosi`, `ghiscari`, `rhoynar`. There is no generic validated culture `mantarysi` yet; do not fabricate one as canon.
- `src/main/resources/data/knownworld/reference/religions.json`: documented usable IDs include `rhllor`, `old_gods_of_valyria`, `trios`, `yndros_of_twilight`, `pantera`, `saagael`, `bakkalon`, `mother_rhoyne`, `unknown_religion`. Religious shares are unknown and must be provisional.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`: existing `volantis_domain` holding with `worldLocationId=volantis`. No standalone feudal ownership invented for other Free Cities.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_free_cities.json`: existing Lysene families `family_haen`, `family_dagareon`, `family_moraqos`, `family_orthys`, `family_pendaerys`, `family_rogare`, `family_saan`; Volantene Old Blood `family_maegyr`, `family_paenymion`, `family_qhaedar`, `family_staegone`, `family_tagaros`, `family_vaelaros`, `family_vhassar`. These can seed **family memberships** but are not municipal owner assignments.
- Some later-known characters have office/kinship facts in later novels. Only place them as 298 AC rulers when the timeline actually establishes the same role at scenario start. Do not backdate ADWD coalition choices or triarch appointments by assumption.

## 7. Step 2.2 contracts: how systems actually connect

1. **Geography:** One unique `WorldLocationDefinition` per true place, world conversion tied to source map + final site. Create shape/footprint references for special monuments, farms, ports, roads and ruins.
2. **Population:** Generate identifiable people (`NpcId` or compatible scalable person IDs), assign exact residence and birth locality, culture, religion, household, kinship and legal standing. Aggregate forecasts are planning constraints only.
3. **Economy:** Actual people work specific estates, ships, workshops and markets. Output/input stocks owned by extant organizations, people/households/holdings. Track work, ownership and food consumption without duplicate fictional workers.
4. **Politics and law:** Triarchs, archons, magisters and governors are office roles filled by real people. Titles/organizations cannot be reduced to feudal House ownership. Add real authority relationships between Volantis and its three major towns.
5. **Military:** Garrisons, slingers, mercenaries, fleets, urban watch and rural militias consist of real members (NPC identities), equipment and geographic stations. A defense estimate is a recruitment target, not an army generated from nowhere during siege.
6. **Agriculture:** Distinguish region viability from placed farm businesses. Map actual cultivable plots, livestock holdings, actual workers and household ration needs. Slave labor remains individual people with full identities and persistent legal status, not workforce count only.
7. **Worldgen:** Different archetypes for gigantic riverside towns, densely walled cities, island ports, old ruins, inhabited anomaly cities, sacred/ritual or civic landmarks. Procedural architecture must use building ownership/residence/job slots that will actually be occupied.
8. **Travel/dialogue:** An NPC met in Lys or an enslaved craftsperson in Myr must be the identical individual from economy and social history; local price and famine information derived from real inventories/transactions, not fake scripts.
9. **Persistence:** Prevent duplicate population when switching from old anonymous cohort system. Reserve known book characters; upgrade existing roles through stable identity reconciliation.

## 8. Priority unknowns (not blocking the later consolidated marker pass)

- [ ] Confirm the exact centers and whether SWE-009 / SWE-012 / SWE-013 represent separate marker shapes. Cross-map duplicates solved after all submaps as requested.
- [ ] Calibrate a shared urban-population ordering across the Free Cities, the Volantene three towns and King's Landing/Oldtown; **don't force 2 million as entire world census**.
- [ ] Determine known starting 298 AC triarch incumbents, Tyroshi archon, Myrish magistrates, Lysene First Magister if reliably sourced; otherwise leave unknown; **do not invent noble surnames**.
- [ ] Research detailed crops, grazing and irrigation from maps/biome/climate; current suggestions are inference, not in-universe crop lists.
- [ ] Determine whether the Oros/Tyria survivors in sources justify encounterable human communities, dangerous nonhuman populations, or uncertain sightings. Do not fabricate unverified permanent families.
- [ ] Create workable fine subzone polygons: Rhoyne banks, floodplains, old city danger zones, island farmland, Mantarys/Painted Mountain corridor, Isle of Cedars, and actual roads.
- [ ] Reconcile city slave/freeborn ratios with each individual's legal status and the starting world demographic generation; ratio applies to the **city**, not every neighboring town.
- [ ] Validate historical ruin status in generator before applying 10×/5× village rules; preserved ancient structures may exceed live settlement footprint in ruins.

## 9. Sources and audit links

A Wiki of Ice and Fire is a community-maintained index that cites *The World of Ice & Fire*, *The Lands of Ice and Fire*, *A Dance with Dragons* and other book sources. It supplies traceable secondary research; it is not a substitute for verifying every specific quote in primary text. Dates and later-story changes should be independently checked before 298 AC scenario code.

- [SWE-01] [The Sorrows](https://awoiaf.westeros.org/index.php/Sorrows); [Chroyane](https://awoiaf.westeros.org/index.php/Chroyane)
- [SWE-02] [Myr](https://awoiaf.westeros.org/index.php/Myr)
- [SWE-03] [Tyrosh](https://awoiaf.westeros.org/index.php/Tyrosh); [Archon](https://awoiaf.westeros.org/index.php/Archon_of_Tyrosh)
- [SWE-04] [Volantis](https://awoiaf.westeros.org/index.php/Triarchy_of_Volantis)
- [SWE-05] [Selhorys](https://awoiaf.westeros.org/index.php/Selhorys)
- [SWE-06] [Valysar](https://awoiaf.westeros.org/index.php/Valysar)
- [SWE-07] [Volon Therys](https://awoiaf.westeros.org/index.php/Volon_Therys)
- [SWE-08] [Volantis Black Walls](https://awoiaf.westeros.org/index.php/Black_Walls)
- [SWE-09] [Sar Mell](https://awoiaf.westeros.org/index.php/Sar_Mell)
- [SWE-10] [Rhoyne](https://awoiaf.westeros.org/index.php/Rhoyne)
- [SWE-11] [Lys](https://awoiaf.westeros.org/index.php/Lys)
- [SWE-12] [Mantarys](https://awoiaf.westeros.org/index.php/Mantarys)
- [SWE-13] [Tolos](https://awoiaf.westeros.org/index.php/Tolos)
- [SWE-14] [Elyria](https://awoiaf.westeros.org/index.php/Elyria)
- [SWE-15] [Bhorash](https://awoiaf.westeros.org/index.php/Bhorash)
- [SWE-16] [Ghozai](https://awoiaf.westeros.org/index.php/Ghozai)
- [SWE-17] [Velos](https://awoiaf.westeros.org/index.php/Velos)
- [SWE-18] [Oros](https://awoiaf.westeros.org/index.php/Oros)
- [SWE-19] [Tyria](https://awoiaf.westeros.org/index.php/Tyria)
- [SWE-20] [Old Valyria city](https://awoiaf.westeros.org/index.php/Valyria_(city))
- [SWE-21] [Doom of Valyria](https://awoiaf.westeros.org/index.php/Doom_of_Valyria)
