# Known World Research 2.1 — Northeastern Essos (NEE), 298 AC

**Project:** Minecraft Known World — A Song of Ice and Fire / A Game of Thrones 298 AC scenario  
**Research batch:** 2.1-NEE  
**Source image:** `authoring/maps/Realms/nee.png` / user-annotated `tools/geodata/input/cities/nee.png`  
**Research date:** 2026-10-17  
**Destination:** `docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_NEE_298AC.md`  
**Status:** RESEARCH DRAFT — NOT YET IMPLEMENTED; pixel/name validation deferred to the global review batch.

## A. Non-negotiable architectural and research rules

1. **One civilization; one authoritative population.** All inhabitants, nomads, priests, commanders, garrison soldiers, work crews, traders, family members, refugees and temporary visitors must ultimately refer to identifiable, persistent people. Population numbers below are *initialization targets* or planning envelopes, never a parallel anonymous human simulation. Physical Minecraft NPCs represent those same persistent identities.
2. **298 AC and book-based chronology first.** Preserve attested characters and offices; do not generate an unrelated replacement for a known living ruler. Other continuities must be explicitly tagged, not silently mixed into book canon.
3. **Distinguish four evidence levels:** **CANON** = attested in the cited work/its reliable sourced compilation; **REPO** = an existing reference or scenario assertion in the mod, which need not itself be canonical; **INFERENCE** = extrapolation from geography, institutions and historical comparators; **MOD_ESTIMATE** = invented, provisional balancing assumption. The fact that a city is canonical does *not* make its estimated population canonical.
4. **Uncertainty is an actual scenario state.** If a town's existence is legendary, its presence, population and buildings should be controlled by an explicit scenario policy. Do not silently convert literary speculation into a confirmed city. `unknown_religion` is preferable to assigning an unsupported cult. No invented canonical dynasty names.
5. **Geography is not yet finalized.** `NEE-001` etc. are *provisional annotation IDs*, not permanent place IDs. Preserve source pixels even when final build coordinates are shifted onto traversable land. In particular, individual Five Forts have NO known canonical names.
6. **Infrastructure is attributable.** Stores, farmland, crafts, fortifications and economic actions must connect to real holdings, organizations, workers and households. Even a garrison's rations must be supplied by identifiable people and institutions.
7. **Military totals count people already in the population.** Capacity, stationed soldiers, mobilizable people, levies and visiting armies are separate quantities and must not be summed without individual membership reconciliation.

### Population notation and non-canonical numbers

- **Permanent residents** include soldiers stationed long-term, dependents and institutional servants if they live at the site. **Transient visitors** are tracked separately but remain real people, never added as freshly generated headcounts on arrival.
- **Organized defenders** = contemporaneously present military personnel, *already within the resident or visitor population*. They are NOT an additional number of residents. Capacity (particularly for the Five Forts) is different again.
- All numeric bands below are **MOD_ESTIMATE** ranges for the first 298 AC scenario. They are not derived from a canonical census and should be compared against whole-world population planning before generation.
- **Employment shares** are percentages of *working-age employed residents*, not shares of total people, and include serving soldiers/garrison labor when indicated. Exclude dependents; model real households, aged residents, children and employment absences through NPC records. For sites without attested urban continuity, no mandatory fixed employment shares are supplied.
- The ranges are deliberately broad where evidence is weak; `0 / conditional` is the correct default for entirely uncertain site existence.

## B. Complete NEE place register (ten marker observations)

| Map observation | Location / permanent site identifier proposal | 298 AC standing | Residents, MOD_ESTIMATE | Stationed defenders, MOD_ESTIMATE | Political status | Main source(s) |
|---|---|---|---:|---:|---|---|
| NEE-001 | Nefer (`nefer`) | Real inhabited port / capital | 12,000–45,000 | 300–1,500 | Remaining kingdom of N'Ghai; ruler unknown | [S1], [S2] |
| NEE-002 | K'Dath (`kdath`) | **Semi-legendary; existence unconfirmed** | 0 by default; if enabled, exploratory 2,000–20,000 | unknown | No documented government or owner | [S3] |
| NEE-003 | Trader Town (`trader_town`) | Active military-commercial city | 25,000–90,000 | 2,000–10,000 (including Pol Qo's contingent where present) | Pol Qo's contested Orange Emperor regime | [S4], [S5] |
| NEE-004 | Bonetown (`bonetown`) | **Rumored** bone-trading town | 0 by default; if enabled, 600–6,000 | unknown; hypothetical 30–350 | Unconfirmed; no attested lord | [S6], [S7] |
| NEE-005 | Five Forts site 1 (`five_forts_site_01`) | Massive ancient frontier fort | 1,000–5,000 | 700–3,500 | Yi Ti frontier; individual commander unknown | [S8] |
| NEE-006 | Five Forts site 2 (`five_forts_site_02`) | Massive ancient frontier fort | 1,000–5,000 | 700–3,500 | As above | [S8] |
| NEE-007 | Five Forts site 3 (`five_forts_site_03`) | Massive ancient frontier fort | 1,000–5,000 | 700–3,500 | As above | [S8] |
| NEE-008 | Five Forts site 4 (`five_forts_site_04`) | Massive ancient frontier fort | 1,000–5,000 | 700–3,500 | As above | [S8] |
| NEE-009 | Five Forts site 5 (`five_forts_site_05`) | Massive ancient frontier fort | 1,000–5,000 | 700–3,500 | As above | [S8] |
| NEE-010 | Tiqui (`tiqui`) | Active ancient market city | 25,000–100,000 | 600–3,500 | Yi Ti city; modern governor unknown | [S9], [S10] |

**Critical Five Forts distinction:** Each fort is said to be able to house **10,000 men**; the lore does not say each *currently stations* 10,000 men. Five sites imply a claimed maximum accommodation of **50,000** men combined, not an automatic active army of 50,000. Nor does it settle whether each fort houses civilian families. [S8]

**NEE scope:** These ten observations cover six named places or groups (Nefer, K'Dath, Trader Town, Bonetown, the Five Forts, Tiqui). They become ten site observations because the Five Forts are five individual physical sites. Do not conflate marker observations with unique named cities. The five fort markers are separate physical sites but *not* separately named in canon.

### B.1 NEE-001 — Nefer, the Secret City

- **Lore (CANON):** Nefer is the capital and last surviving city of the once wider kingdom of N'Ghai. It stands at a river mouth on the Shivering Sea, surrounded by high chalk cliffs and fog; most of the city is reportedly *underground*. Its sinister reputation as a home of necromancers and torturers reflects outsider reports rather than a census of actual practitioners. Continuous Jogos Nhai pressure reduced N'Ghai to Nefer and its hinterland. Corlys Velaryon visited it. [S1][S2]
- **298 AC identity/governance:** `nefer`; kingdom/realm `n_ghai`. The sovereign's personal name and dynasty are **unknown**. Avoid an invented hereditary house name. Model an unresolved royal/urban office populated by a generated individual whose name is explicitly provisional, once the canonical-character reservation layer is ready.
- **Residents/defense:** 12k–45k residents, 300–1.5k defenders are MOD_ESTIMATE. No source gives fixed figures. Coastal defenses, river-control duties and fear of raids can justify a substantial watch, but it cannot be made a major imperial army without evidence.
- **People:** Local N'Ghai kinship is reportedly related to Jogos Nhai, but the population must not automatically receive all nomadic Jogos Nhai customs. `jogos_nhai` is an imperfect existing cultural proxy; **ADD** a distinct `n_ghai` cultural identity only after checking whether it is already supported elsewhere. The religion is not documented; use `unknown_religion` rather than mandatory moonsinger or necromancy worship. A minority of foreign merchants and sailors is plausible, explicitly inferred.
- **Occupation distribution (MOD_ESTIMATE, employed adults):** marine fishing/shipping 20%; local farming/food processing 18%; quarrying/construction/craft 17%; trade/porters 13%; civil administration/ritual/specialized knowledge 9%; guards and military 10%; household and other services 13%. **Total 100%.** Distinguish the underground-city building/maintenance economy from a flat normal port.
- **Agriculture/resources (INFERENCE):** river-bottom garden plots, hardy cereals (barley/rye analogues as *plausible* not attested crops), tubers/vegetables in protected plots, goats/sheep, fish, timber imports, chalk/stone extraction, cisterns and imported grain. Low-to-medium potential overall, locally higher along river terraces; fog and cliff slopes reduce large-field suitability.
- **Construction requirements:** chalk-cliff harbor entrance and compact visible docks; enormous connected subterranean boroughs, stairways, light wells, ventilation and underground stores; fortified land approaches, river and marine piers, chalk quarries. Underground proportion is a lore description, not proof of exact inhabitants or volumes. An ordinary overworld village template is unacceptable.
- **Social/military hooks:** individual harbor masters, customs officers, tunnel wardens, sentries, craftspeople, fishers, royal servants, occasional refugees. Frontier threats influence personal memories and trade/security behavior. Location's visible size must not be scaled from total population because the city is partly subterranean.
- **Verification:** 298 AC ruler/dynasty not recorded; extent of underground works and crop mix are unknown.

### B.2 NEE-002 — K'Dath

- **Lore (CANON as reported legend):** K'Dath is *semi-legendary*. Its inhabitants allegedly claim it is the first city, while terrifying rites are rumored. The source specifically questions whether it exists and what its nature is. It is on the far side of the Five Forts, near the Grey Waste, Land of the Shrykes and Bonetown. [S3]
- **Existence policy:** Recommended default `LEGENDARY_UNCONFIRMED`. Keep a named geographical point/rumor even when no functioning structure is spawned. An optional exploratory/sandbox worldgen setting may instantiate a city explicitly labeled `MOD_SPECULATIVE`; do not silently classify it as an attested active settlement in 298 AC.
- **If player opts into speculative realization:** 2k–20k residents is an intentionally broad scenario range; defending forces and political culture unknown. Use tightly clustered isolated compounds, monumental ancient structures, ritual buildings, narrow internal passages, burial/monumental complexes and unpredictable maintenance conditions rather than a Westerosi castle-and-village. None of these exact features are established canon.
- **Culture/religion:** Both UNKNOWN. Do not treat literary “mad gods” as confirmed theological institutions. `unknown_religion` is the appropriate existing fallback; no canonical ruling House or leader is known.
- **Economy/agriculture:** None documented. Optional city would require actual logistical model for desert water, food import, traders/foragers and protected supply lines. Prohibit automatic surrounding villages and normal farmland without demonstrated land/water resources.
- **Narrative hooks:** Travelers may possess conflicting rumors. Their knowledge of K'Dath is individual and reliability-dependent, not a globally verified factual NPC memory.

### B.3 NEE-003 — Trader Town

- **Lore (CANON):** A rude, sprawling northern YiTish garrison city at the intersection of important road networks, east of the Great Sand Sea and west of the Shrinking Sea, south of the Jogos Nhai plains. The Steel Road reaches it and the Sand Road connects it with Tiqui and Bayasabhad. It is currently the seat of **General Pol Qo**, Hammer of the Jogos Nhai, who styles himself the first Orange Emperor against the Azure Emperor **Bu Gai** at Yin. [S4][S5][S11]
- **Identity/governance:** `trader_town`; `orange_emperor_domain` is **REPO/mod-authored political polygon**; the already-defined `trader_town_orange_domain` holding, `dynasty_yiti_orange` and its Pol family association must be reused. Pol Qo should be a reserved canonical identity, not overwritten by a randomly generated lord. His territorial ambitions do not prove control of all Yi Ti. The Orange Emperor claim is contested and distinct from de jure YiTish legitimacy.
- **Residents/defense:** 25k–90k stable residents; stationed 2k–10k as a first balancing range, **not** a canonical army count. Units can be posted, transferred or camped; their people must stay identifiable in the soldier/household system. Visiting mercenaries, caravans, armies and temporary camps are additional **existing** individuals, not residents conjured upon arrival.
- **Culture/religion:** primarily `yi_tish`, with actual traders from other cultures and possible Jogos Nhai captives/migrants. YiTish religious identity is heterogeneous and not safely inferred from a western stereotype; baseline `unknown_religion`, optional explicitly sourced cults later. Pol Qo's political title must not be mistaken for a religious census.
- **Employment (MOD_ESTIMATE, employed adults):** caravan trade/transport/animal care 23%; food farming/herding and processing 18%; crafts/metalworking/construction 19%; soldier/guard/quartermaster 18%; state/civic administration 8%; household/personal services 14%. **Total 100%.** Garrison personnel are included, not added a second time.
- **Agriculture (INFERENCE):** irrigated river/valley pockets and oasis gardens, dryland grains where feasible, legumes, goats/sheep, zorse or horse feed via fodder supply and grazing at the steppe margin. Water stress and local security should meaningfully restrict cropland. Farmland beyond the city must be tied to safe routes and actual farmer households.
- **Structures:** scattered quarters along long trade and military roads, caravanserais, courtyards, warehouses, corrals, armories, barracks, administrative palace/command compound, outer encampments and irregular walls/earthworks as warranted. Do not put the same residential grid over camp areas. Build a true junction city, not a single enlarged vanilla village.
- **Politics/gameplay:** imperial rival claim, road tariffs, border patrols, negotiation with YiTish local princes, raiding threat from Jogos Nhai. For future interactive NPCs, military transfers, trade shortage and faction conflicts must change their personal assignments and loyalties.

### B.4 NEE-004 — Bonetown

- **Lore (CANON as rumor):** Bonetown is *rumored* to stand just north of the Dry Deep, east of the Land of the Shrykes, near the Cannibal Sands. It may trade ancient odd bones from the canyon; it is said to be made entirely of bones. Neither its current size nor continuous occupation is firmly attested. [S6][S7]
- **Recommended status:** `RUMORED_UNCONFIRMED`, `0` generated permanent residents by default pending explicit worldbuilding selection. Alternatively opt into a `MOD_SPECULATIVE` active bone-trading outpost of 600–6k people. Proposed 30–350 guards **only** if actually generated and economically sustainable; military organization unknown.
- **Culture/religion/authority:** UNKNOWN; do not invent a verified Bone Lord, a noble House Bone, or a confirmed cannibal cult. Cannibal Sands is a separate region whose inhabitants are subject to *allegations*, not evidence about every Bonetown resident.
- **If enabled, livelihoods (MOD_ESTIMATE):** bone-extraction/salvage/transport 35%; commerce/porters/caravans 25%; water and food procurement 18%; crafts/repair 12%; guarding and administration 10%. **Total 100%.** No automatic surrounding intensive farms; imported food and finite water supply are mandatory.
- **Structures:** carved bone and fossil elements, shelters adapted to dry canyon edges, water cisterns, excavation stores and trading yard. Literal buildings completely constructed from bones remain a storyteller's claim, not an absolute authoring requirement.
- **Risk:** treat species and provenance of ancient bones as unknown; do not spontaneously introduce canonical creatures or proven fossil trades beyond the rumored material.

### B.5–B.9 NEE-005..009 — The Five Forts (five distinct structures, NO canonical individual names)

- **Lore (CANON):** Five enormous fortresses mark the northeastern defenses of Yi Ti between the Bleeding Sea and Mountains of the Morn. Each can supposedly house 10,000 men, with near-thousand-foot walls made of fused black stone. Their origin predates the empire; attribution to the Pearl Emperor and defense against demons of the Lion of Night are legends. The forts guard against raiders out of the Grey Waste; the specific architectural layout, fort names, commanders and deployed troop counts are not known. [S8][S12]
- **Shared identity:** group ID `the_five_forts` is already in `NorthEastEssosFeatureRegistry`; five separate holding/workplace/fortress identities should be added, retaining original `NEE-005`..`NEE-009` source pixels for later verification. Use neutral technical IDs `five_forts_site_01`..`05`, **not** invented proper names.
- **Shared headcount policy:** capacity 10,000 soldiers per fort is attested lore; **actual stationed soldiers 700–3,500 per fort** and stable residents 1k–5k per fort are MOD_ESTIMATE placeholders. The five-site garrison must be simulated as actual units with rosters and logistics, not an independent 5× garrison number used by military AI. Local civilians/retainers, suppliers, engineers and dependents cannot be counted again under separate workforce cohorts.
- **Authority:** Yi Ti frontier institution, but **do not assume** every fort has sworn to Pol Qo just because he controls Trader Town. Contested control and actual fort commanders are research gaps; use `UNRESOLVED_CURRENT_COMMANDER`, `UNRESOLVED_ALLEGIANCE` until explicit 298 AC data or defensible mod-authored political decisions are made. No canonical families for individual sites have been identified.
- **Culture/religion:** likely predominantly `yi_tish` among imperial garrison personnel (**INFERENCE**), with personnel who may vary by source territory, state allegiance and recruitment. No attested compulsory fort-specific religion. Visiting pilgrims or frontier traders remain individuals.
- **Supply/economy:** Stores of cereals and salted provisions, weapons, armor, repair stone, timber, water, fuel, fodder, pack animals, workshops. Efficient survival requires supply chains back to actual populated towns and nearby defensible farmlands, not automatic grain production inside stone walls. Assume low local cultivation outside suitable river and protected pockets.
- **Construction:** Dedicated *monumental* templates: fused black stone, vertically exceptional ramparts, gateways, watch and signal network, barracks, cisterns, covered stores, service courtyards, extensive approach roads and surrounding support settlements *only where safe*. Literal fully detailed ~300m structures may be heavy at 1:1 scale: use terrain-aware phased geometry and structure-safe chunk sequencing. Never upscale a vanilla keep 10× and call it one of the Five Forts.
- **Defense/gameplay:** choke points, signal systems, road supply, sortie parties, muster and patrol contracts; attacks from Grey Waste must be composed of actual modeled entities (or a clearly explicit scripted/nonhuman threat), with casualties attributed to existing soldiers.

**Separate observations / per-site authoring records:**

| Observation | Neutral technical ID | Distinguishing data already known | Later requirement |
|---|---|---|---|
| NEE-005 | `five_forts_site_01` | Same canonical fort group | Verify red X centre; choose relative position; fort footprint and access route |
| NEE-006 | `five_forts_site_02` | Same canonical fort group | As above; no individual name invented |
| NEE-007 | `five_forts_site_03` | Same canonical fort group | As above; no individual name invented |
| NEE-008 | `five_forts_site_04` | Same canonical fort group | As above; no individual name invented |
| NEE-009 | `five_forts_site_05` | Same canonical fort group | As above; no individual name invented |

**Important:** These site numbers follow earlier detector order and are NOT asserted to be west-to-east order, command hierarchy or authentic numbering. Actual relative layout awaits the consolidated red-X validation pass.

### B.10 NEE-010 — Tiqui

- **Lore (CANON):** City in western hilly Yi Ti, southeast of the Great Sand Sea. It is an ancient imperial capital and former seat of the Purple Emperors, but **currently a market city** linked by the Sand Road to Trader Town, Bayasabhad and Qarth. [S9][S10]
- **Government:** Yi Ti polity; the present city's leader/house is UNKNOWN. Do **not** generate an active Purple Emperor in 298 AC merely because an older dynasty ruled here. Its de jure claim vs Pol Qo's local sphere may require explicit scenario conflict policy. Pol Qo is based at Trader Town, not Tiqui; Bu Gai's court is at Yin.
- **Residents/defense:** 25k–100k permanent city residents, 600–3.5k organized defenders are MOD_ESTIMATE. Tiqui could be large by Westerosi standards; actual canonical numbers are absent.
- **Culture/religion:** `yi_tish` dominant as probable; other merchant communities optional, supported through individual migration. Local beliefs largely undocumented; `unknown_religion` baseline, with known YiTish traditions assigned only where actual source support emerges.
- **Employment (MOD_ESTIMATE, employed adults):** agriculture/food processing/hillside herding 28%; trade/logistics 23%; crafts/stonework/textiles 21%; civic/palace/ceremonial services 9%; garrison/guards 7%; household/personal/other 12%. **Total 100%.**
- **Agriculture (INFERENCE):** hillside terracing, valley grains (wheat/millet analogues subject to environmental checks), legumes, orchards and vegetables under suitable climate, goats/sheep, draft animals, water channels. Southern slopes and accessible valleys may support significantly higher fertility than the desert to the northwest.
- **Structures:** ancient imperial precinct and repurposed palatial buildings (historical Purple court), monumental gates, multi-period street grid, terraces, market streets, commercial inns, shrines, old paved roads, workshops and surrounding lower-density orchard districts. Historic precinct is not proof of a present Purple Emperor's household.
- **Economic/geopolitical hooks:** road tariffs, desert caravans, agricultural hinterland, faction rivalry, merchants trading between Qarth and the YiTish interior. Each transaction requires actual merchants, inventories and workplaces.

## C. NEE broader zone / environmental influence profiles

**Zone precedence:** Existing polygons in `territory_zones.json` are imperfect political/geographical baselines; marked subregions without polygons currently cannot support reliable pixel-to-zone attribution. Do not set NPC religion, culture or allegiance solely from the nearest city. A region should supply probabilistic priors constrained by individual household, birthplace, settlement, occupation and political affiliation.

| Area / profile ID (proposal) | Current repo mapping | Geography, crops and environment | Society, economy, settlements and conflict | Source level |
|---|---|---|---|---|
| `n_ghai_coast_and_nefer` | Named `n_ghai` reference, but no dedicated polygon; `n_ghai` currently incorrectly nested under `jogos_nhai` | Shivering Sea river mouth, fog, chalk cliffs, limited river plots, fisheries | Distinct N'Ghai culture; Nefer, sheltered satellite hamlets, hard frontier; border raids | Lore/inference [S1][S2] |
| `thousand_islands_fringe` | `the_thousand_islands` reference, no separate territory-zone polygon | Cold, windy islands, marine resource, low arability | Distinct islander society; unusual contact rules. Do not assume Nefer governs the archipelago | Lore/inference [S19] |
| `jogos_nhai_windswept_plains` | `jogos_nhai` polygon exists | Dry sparse grassland, weeds/devilgrass, zorse husbandry, low reliable rainfall | Mobile jhat + moonsinger-led blood-related bands, raiding, no automatic permanent farming towns | Lore [S13][S14] |
| `yi_ti_northern_frontier` | `yi_ti` polygon exists, `orange_emperor_domain` mod-authored priority polygon exists | Patches of farmland, infrastructure and fortified border roads | Competing claimants; Traders/imperial militias; locations need independent actual allegiance | Lore/REPO [S11][S12] |
| `trader_town_road_corridor` | `orange_emperor_domain` (REPO, `PLAUSIBLE`) | Dusty transport corridor, water stops and caravan fodder | Roadside service villages, caravans and fortified storehouses only where viable | Inference [S4][S17] |
| `tiqui_western_hills` | `yi_ti` polygon only; no named local polygon | Hilly arable pockets and terraces; orchard/irrigation where justified | Market villages, ancient palace/roads; craftsmen and merchant networks | Lore/inference [S9][S10] |
| `five_forts_defensive_marches` | `yi_ti` polygon; feature group `the_five_forts` | Frontier mountains/plains; variable water, limited safe farmland | Five forts + exact unit rosters, supply tracks, sparse service communities | Lore/inference [S8] |
| `grey_waste_cold_desert` | `grey_waste` named reference, **no** dedicated territory polygon | Freezing desert, extremely low agricultural value | Raiders reported; no random villages and no automatic human mass spawning | Lore [S15] |
| `land_of_the_shrykes` | Named reference, no mapped polygon | Dangerous, poorly documented eastern plain | Alleged monsters, no proven human settlement system; ecology unknown | Lore/legend [S16] |
| `kdath_remote_plain` | Feature `kdath`, but no verified active city reference | Remote land between Shryke territory and Grey Waste, highly uncertain | Legendary city if scenario option enables it; otherwise rumors/wilderness | Legend [S3] |
| `dry_deep_and_bonetown` | `the_dry_deep` reference, no polygon | Waterless canyon per legend, severe food/water scarcity | Conditional bone-trading outpost; no normal village density or reliable agriculture | Lore/legend [S6][S7] |
| `cannibal_sands` | `cannibal_sands` reference, no polygon | Separate desert areas near Grey Waste | Alleged cannibalism cannot be asserted as universal inhabitant culture; sparse uncertain habitation | Rumor [S18] |
| `great_sand_sea_northeast` | `great_sand_sea` reference, no fine local polygon | Hotter to south and colder to north, former inland sea theories; low productivity | Oasis caravan stops, isolated fortlets, no homogeneous continuous farming | Lore/inference [S17] |
| `mossovy_southern_edge` | `mossovy` polygon exists | Forest/wilderness bordering far northeastern Essos | Separate polity/inhabitants; never auto-attach to Nefer or K'Dath | Lore + existing repo |

**Required new geographic fields (later implementation):** `region_profile_id`, `parent_zone_ids`, `territory_polygon_ref`, `environment_class`, `fertility_base_permille`, `water_security`, `supported_crops` with provenance, `pasture_potential`, `settlement_density_policy`, `nomadic_band_policy`, `road_access`, `security_threats`, `civil_administration`, `religion_prior`, `culture_prior`, `climate_variability`, `source_refs`. These are not arbitrary immutable NPC traits.

## D. Crop, soil and animal suitability — carefully bounded proposals

**This is environmental modelling, not crop canon.** Canon confirms some environments and economies but rarely agricultural species at specific sites; any crop listed below is an agronomic *possibility* requiring climate, soil, irrigation and seed/import validation.

| Site/area | Fertility proposal | Potential produce / husbandry (MOD_ESTIMATE/INFERENCE) | Important limits |
|---|---|---|---|
| Nefer lowlands / N'Ghai | low–medium with river-local high pockets | barley/rye-like grains, roots, hardy greens, small livestock, fish | High cliffs, fog, northern temperatures, finite open soil; fish ≠ farm harvest |
| Trader Town periphery | low overall; moderate in watered corridors | dry cereals, legumes, garden produce, sheep/goats, fodder | Supply security, irrigation, water deficit and caravan demand |
| Tiqui hills and valleys | medium in accessible terraces; low on bare slopes | terraces of grains and legumes, orchards/vegetables where suitable, sheep/goats | Crop mix climate-sensitive; rocky slopes cannot be ordinary farms |
| Five Forts | low local production; imported staples crucial | localized gardens, livestock pens and forage where safe | Forts are defense/supply nodes, not five gigantic farms |
| Jogos Nhai plain | low cropland, variable grazing | hardy zorses, pack herds, grasses, small mobile stocks | Nomadic people: herd/pasture must connect to mobile bands; no dense crop fields |
| Grey Waste / Cannibal Sands | near-zero barring genuine water points | only transient forage, no default stable agriculture | Avoid infinite food generated by a supposed desert village |
| K'Dath / Bonetown | UNKNOWN | none until explicit speculative choice | No real climate or soil evidence detailed enough for production balance |

For future code, agricultural yields should continue through the single authoritative harvest model with actual farms and resident workers. Derive zone potential from terrain plus proven resources; do not grant a site a resource without holdings, households and an active supply chain.

## E. Existing repository audit (2026-10-17)

**Verified existing records:**

- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/NorthEastEssosFeatureRegistry.java`: registers `nefer`, `nghai`, `kdath`, `bonetown`, `the_five_forts`, `trader_town`, `tiqui`, `land_of_the_shrykes`, `the_grey_waste`, `the_dry_deep`, `cannibal_sands`, `the_thousand_islands`, `the_shrinking_sea` and more *as geographic features*; geographic feature type does not prove an active city exists at 298 AC.
- `src/main/resources/data/knownworld/reference/locations.json`: already includes `yi_ti`, `orange_emperor_domain`, `jogos_nhai`, `plains_of_the_jogos_nhai`, `n_ghai`, `mossovy`, `the_thousand_islands`, `great_sand_sea`, `the_shrinking_sea`, `grey_waste`, `cannibal_sands`, `land_of_the_shrykes`, `the_dry_deep` and other broad references. **Issue:** `n_ghai` is nested under `jogos_nhai`, even though N'Ghai is a distinct neighboring polity and kin culture; should be evaluated in Step 2.2 instead of blindly inherited.
- `src/main/resources/data/knownworld/reference/territory_zones.json`: implemented polygons for `jogos_nhai`, `orange_emperor_domain`, `yi_ti`, `mossovy`. `orange_emperor_domain` is explicitly `PLAUSIBLE` and mod-authored; no claim its boundary is canonical. Gaps: dedicated sub-polygons for Nefer/N'Ghai, Five Forts, Tiqui hills, Grey Waste, Shryke lands, K'Dath, Bonetown and Dry Deep.
- `src/main/resources/data/knownworld/reference/locations_holdings.json`: already has a named `trader_town` settlement and `yi_ti` reference. Nefer, Bonetown, K'Dath, Tiqui and the five individual fort sites are not equivalently registered there at the time of this audit.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`: `yi_ti_domain` and `trader_town_orange_domain` already exist. `trader_town_orange_domain` is a `CITY`, `ACTIVE`, its `worldLocationId=trader_town`, `parentHoldingId=yi_ti_domain`, `deJureDynastyId=ownerDynastyId=dynasty_yiti_orange`. **Political caveat:** keeping a parent de jure hierarchy and a disputed owner requires distinct legal vs effective allegiance; `Pol Qo` is a claimant, not universally accepted emperor.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_yi_ti_leng.json`: `dynasty_yiti_orange` is present, notes family name **Pol** and General **Pol Qo**. Reuse that record. Do not invent or assert separate hereditary noble families at Nefer, K'Dath, Bonetown, each fort or Tiqui.
- `src/main/resources/data/knownworld/reference/cultures.json`: `yi_tish` and `jogos_nhai` exist. An explicitly distinct `n_ghai` culture is not in the audited list; do not silently treat all Nefer residents as Jogos Nhai. No established K'Dath/Bonetown ethnographic definition.
- `src/main/resources/data/knownworld/reference/religions.json`: `moonsingers`, `lion_of_night`, `unknown_religion` exist. `moonsingers` is an appropriate Jogos Nhai-prior, not a mandatory Nefer/Trader Town/Tiqui religion; `lion_of_night` is associated with distant lore, not a certified population fraction among the Five Forts.

**Names and actors to reserve in identity system:**

| Canonical person | Location / office in 298 AC | Identity policy |
|---|---|---|
| **Pol Qo** | General and rival Orange Emperor at Trader Town | RESERVE exact person, clan/family Pol, claimed title, actual command and alliances; no generated replacement. |
| **Bu Gai** | Azure God-Emperor at Yin (outside this submap) | RESERVE person; distinguish imperial de jure legitimacy from Pol Qo’s effective territory. |
| Individual Five Forts commanders | Not named | Generate placeholder office-holders from appropriate YiTish pools **only if** scenario demands live command, marked `MOD_GENERATED`, with identities retained for replacement if future canon emerges. |
| Tiqui contemporary governor | Not named | Same, no invented house surname or revival of Purple Emperor lineage as fact. |
| Nefer king/queen | Not named | Reserve unresolved office; don't invent a canon dynasty. |

## F. Quantitative group allocation rules for NPC generation

1. **Counts are goals:** actual count is computed from generated/persisted people; the site profile describes starting capacity/target distribution. Never separately add an anonymous cohort population on top of these individuals.
2. **Initialize institutions BEFORE people:** ruling offices, civic authorities, garrison units, markets, farms, roads, temples, workplaces, traveling bands and households are assigned identities; residents then fill jobs with attributes and relationships.
3. **Don't flatten nomads:** the Jogos Nhai live in mobile, kin-linked bands led by a `jhat` and a `moonsinger`, not random static villages; animals and equipment remain attached to their real owners. [S13][S14]
4. **Cohort indexes derive from the person ledger:** population age/sex/culture/religion counts, occupational statistics and military readiness are derived views, never independently authoritative humans.
5. **No duplicated military:** fort capacities and actual men-at-arms are separate; guards are real NPCs (or identically traceable compact person records) inside site population. Visiting units have a home residence separate from current physical location.
6. **Site-existence policy:** K'Dath and Bonetown are not default confirmed active cities; if mod generates them, retain `MOD_SPECULATIVE` provenance and real generated residents.
7. **Canonical identity substitution:** Pol Qo and Bu Gai never receive unrelated random replacement NPCs. Their roles are reserved from the start; later authored profiles update same person or validated canonical ID slot.
8. **No city-size-by-font-size:** even a monumental fort should not create unnecessary generic town NPCs; population is determined by credible logistics and the type of site, not map text typography.
9. **Generation determinism:** project seed + zone + stable site ID + household/office index produce reproducible identities, but *persisted* identities and altered histories supersede regeneration when the player returns.

## G. Settlement architectural archetypes and performance considerations

| Archetype | Used by | Required template/planner capabilities |
|---|---|---|
| `subterranean_cliff_port` | Nefer | Cliff anchor, underground volume reserve, docks, stairwells, access shafts, services, surface/underground ownership; coherent full-scale above-/below-ground generation. |
| `imperial_garrison_trade_city` | Trader Town | Military compounds, caravan junction, civic infrastructure, markets, logistics depots, stratified neighborhoods and outer camps. |
| `yi_tish_ancient_hill_market_city` | Tiqui | Terraces, old palace precinct, multiple historical building layers, trade roads, structured streets. |
| `monumental_blackstone_fort` | Five Forts ×5 | Distinct monumental superstructures, fort walls, lodgings, stores, internal transport, approach causeways, terrain/supporting settlement separation. |
| `rumored_remote_city` | K'Dath, conditional | Generation feature flag; if disabled, geographic rumor only; if enabled, explicit speculative architecture and sustainability constraints. |
| `remote_bone_trader_outpost` | Bonetown, conditional | Scarce water, fossil storage, trade yard, dry canyon infrastructure and low-density approach roads. |
| `mobile_pastoral_band` | Jogos Nhai region | Camps migrate with real member roster and herds; no permanently generated fake village for each band. |

**Visual/performance precautions:** Five Forts walls can be huge in Minecraft 1:1 terrain; require stable multi-chunk planning (idempotent writes, chunk-border stitching, no worldgen thread-unsafe mutation), foundations and terrain adaptation, roads that do not cut through housing, partial-detail distant projection and configurable template density. A complete city/castle planner should not be a single monolithic Java class.

## H. Step 2.2 integration backlog (NO changes in this research batch)

1. **World/place reference:** Add confirmed sites to authoritative location catalog, avoid duplicate `Nefer`/`nefer` or group-site identity confusion; maintain canonical vs speculative flags.
2. **Geographical polygons:** Refine zone hierarchy; especially split N'Ghai from Jogos Nhai and create eastern frontier subzones with source-derived uncertainty.
3. **Position import:** Bind every source `NEE-xxx` pixel to an exact regional-to-master transform and Minecraft X/Z; save adjusted terrain-safe footprint separately; perform duplicate/uncertain marker review in the single later consolidated pass.
4. **Government/dynasty:** Map Pol Qo to `dynasty_yiti_orange` and `trader_town_orange_domain`; explicitly model contested authority and the Bu Gai claimant elsewhere. Never invent noble surnames for the other sites.
5. **NPC / population:** Initialize permanent individual people and households using city generation targets with diverse age, sex, family, residence, work, culture, religion, education, personality, health, wealth and memory profiles.
6. **Economy/employment:** Instantiate work assignments for real merchants, caravans, workshops, quarries, fishing crews and farmers; build inventories/ownership under existing `InventoryManager` and organizations.
7. **Food/agriculture:** Use microregional climate/soil water resources, farm plots and actual farmworker identities; establish supply/import routes especially at Five Forts, Bonetown and K'Dath (if enabled). Retire parallel anonymous farmer authority.
8. **Military/warfare:** Actual soldiers belong to rostered organizations and fort posts; storage, attrition, pay, rations and muster tied to the same NPCs. Site fort capacity is not force size.
9. **Religion/culture:** Use probability priors plus household/upbringing and actual migration, preserve local minorities. Do not produce imaginary fixed percentages from scarce lore.
10. **Travel/trade:** Physically meaningful Sand/Steel Roads, northern supply links, caravan storage, border patrols, trader NPC schedules, horses/zorses and ownership.
11. **Worldgen:** Dedicated archetype planner; don't apply ordinary 10× vanilla village template to Five Forts, Nefer or ancient Tiqui.
12. **Dialogue/knowledge:** Civilians discuss their actually experienced raids, goods prices, family losses and fort supply; K'Dath rumors require speaker-specific certainty. Nothing should be scripted from an unrelated background population.
13. **Testing:** identity/population conservation, no duplicate generated rulers, no invented permanent K'Dath/Bonetown inhabitants in canon-conservative preset, save/reload identity, no garrison > cap, proper location assignments and conflict resolution between de jure and de facto rulers.

## I. Explicit unresolved questions for future batch

- Is the K'Dath location physically present in the chosen simulation continuity? What level of mystical content is allowed? Recommended conservative default: geographic point/rumor only.
- Is Bonetown actually inhabited in 298 AC? Recommended conservative default: rumored point, optional speculative economy.
- Who rules Nefer? What are N'Ghai's exact religious and cultural distinctions? No supported name identified.
- How many soldiers actually occupy each Five Fort and which rival YiTish political faction commands each? No canonical answer; do not infer Pol Qo's control.
- Are the Five Forts all the same architecture? Lore gives material and enormous scale, not individual internal floor plans.
- Who governs Tiqui today? The Purple Emperors are historical, not current.
- Exact source-image markers/leader-line connections, including NEE-005..009 ordering, await final user-requested global correction pass. Do not spend this batch moving crosses.
- The `n_ghai` parent `jogos_nhai` in `locations.json` requires evaluation against geopolitical and cultural structure before any inheritance logic uses it.
- Crop species, garrisons, monthly rainfall and occupation percentages have no reliable canonical numerical coverage; model sensitivity and regional constraints explicitly.

## J. Source register

The **primary worldbuilding works** for these subjects are George R. R. Martin, *The World of Ice & Fire* (especially **The Bones and Beyond: Yi Ti** and **The Plains of the Jogos Nhai**, and **Beyond the Free Cities: East of Ib**), and *The Lands of Ice and Fire* (**The Known World** / **The East**). These are the source works cited by the location articles below. The hyperlinks here are secondary compiled summaries that identify their book/map references; we did not independently inspect the entire primary works in this batch.

- **[S1]** Nefer: https://awoiaf.westeros.org/index.php/Nefer
- **[S2]** N'Ghai: https://awoiaf.westeros.org/index.php/N%27ghai
- **[S3]** K'Dath: https://awoiaf.westeros.org/index.php/K%27Dath
- **[S4]** Trader Town: https://awoiaf.westeros.org/index.php/Trader_Town
- **[S5]** Pol Qo: https://awoiaf.westeros.org/index.php/Pol_Qo
- **[S6]** Bonetown: https://awoiaf.westeros.org/index.php/Bonetown
- **[S7]** Dry Deep: https://awoiaf.westeros.org/index.php/Dry_Deep
- **[S8]** Five Forts: https://awoiaf.westeros.org/index.php/Five_Forts
- **[S9]** Tiqui: https://awoiaf.westeros.org/index.php/Tiqui
- **[S10]** Yi Ti: https://awoiaf.westeros.org/index.php/Yi_Ti
- **[S11]** Bu Gai / Golden Empire: https://awoiaf.westeros.org/index.php/Bu_Gai and https://awoiaf.westeros.org/index.php/Golden_Empire_of_Yi_Ti
- **[S12]** Grey Waste: https://awoiaf.westeros.org/index.php/Grey_Waste
- **[S13]** Jogos Nhai: https://awoiaf.westeros.org/index.php/Jogos_Nhai
- **[S14]** Plains of the Jogos Nhai: https://awoiaf.westeros.org/index.php/Plains_of_the_Jogos_Nhai
- **[S15]** Grey Waste: https://awoiaf.westeros.org/index.php/Grey_Waste
- **[S16]** Land of the Shrykes: https://awoiaf.westeros.org/index.php/Land_of_the_Shrykes
- **[S17]** Great Sand Sea: https://awoiaf.westeros.org/index.php/Great_Sand_Sea
- **[S18]** Cannibal Sands: https://awoiaf.westeros.org/index.php/Cannibal_Sands
- **[S19]** Thousand Islands: https://awoiaf.westeros.org/index.php/Thousand_Islands
- **[S20]** Sand Road: https://awoiaf.westeros.org/index.php/Sand_Road
- **[S21]** Government and god-emperors: https://awoiaf.westeros.org/index.php/God-emperor

### Internal repo sources inspected

- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/NorthEastEssosFeatureRegistry.java`
- `src/main/resources/data/knownworld/reference/locations.json`
- `src/main/resources/data/knownworld/reference/locations_holdings.json`
- `src/main/resources/data/knownworld/reference/territory_zones.json`
- `src/main/resources/data/knownworld/reference/cultures.json`
- `src/main/resources/data/knownworld/reference/religions.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_yi_ti_leng.json`

## K. Completion criteria for this research batch

- [x] All ten source map observations have a named/group association and own record; Five Forts individual names remain explicitly unknown.
- [x] Every site has 298 AC status, government/authority state, culture/religion treatment, provisional population/garrison policy, economic/agricultural and construction guidance.
- [x] Eastern N'Ghai, Jogos Nhai, YiTish and wilderness zone baselines documented, with repo polygon gaps identified.
- [x] Existing scenario political and dynasty IDs audited; Pol Qo reserved, no fabricated canon house.
- [x] Explicit provenance, uncertainty, source links, human identity invariants and Step 2.2 migration tasks.
- [ ] Precise annotation verification / corrected Minecraft build positions — deferred by user to global map-review phase.
- [ ] Canonical population census and garrison tables — not available; keep estimates provisional.
- [ ] Step 2.2 code implementation — future batch.

**Research conclusion:** The NEE region is not just ten villages. It is a contested imperial frontier, a major trade-road garrison city, a former imperial capital, five colossal forts, a surviving isolated northern kingdom and two largely legendary remote cities. The eventual world generator must represent these distinct institutions, their geography and their **actual persistent people** without fabricating independent background populations.
