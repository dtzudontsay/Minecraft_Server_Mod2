# Known World — Research 2.1: SEE (Southeastern Essos), 298 AC

**Project:** `dtzudontsay/Minecraft_Server_Mod2` / Known World — ASOIAF, scenario 298 AC  
**Research prepared:** 2026-10-17  
**Scope:** Ten previously associated annotations on regional map `see.png`, plus relevant regional/subregional baselines.  
**Stage:** 2.1 — research only. **No Step 2.2 runtime changes or Minecraft world generation are authorized by this file.**  
**Companion dossiers:** `KNOWN_WORLD_RESEARCH_2_1_NE_298AC.md`, `..._NWE_298AC.md`, `..._SWE_298AC.md`, `..._SE_298AC.md`, `..._NEE_298AC.md`.

## 0. Interpretation, evidence, and modeling rules

- **CANON** = directly supported by the published books / *The World of Ice & Fire* / *The Lands of Ice and Fire*, as summarized by linked A Wiki of Ice and Fire entries. **Do not interpret a scholarly compilation's in-universe rumor as objective certainty.**
- **REPO** = verified existing 2026-10-17 repository JSON / Java. Describes the **current mod**, not necessarily canonical truth.
- **SOURCE-INFERRED** = reasoned from established climate, geography, trade and institutions; not expressly stated for that town.
- **MOD ESTIMATE** = invented **provisional initial generation envelope** or typical occupational share; not a canonical census, quantified army or current gameplay population.
- **UNKNOWN / CONDITIONAL** = evidence insufficient; leave unpopulated or feature-gated instead of secretly converting rumors into fact.
- **Map source:** user-marked `see.png`, regional dimensions **1609 × 1161 px**. Prior visual matches `SEE-001` … `SEE-010` are *candidate labels*, **not verified centred X positions**. Save exact source centre later; do not reinterpret this document as placement data. The user wants one consolidated marker correction pass when all submaps are complete.
- **Every human is one persistent, encounterable NPC identity.** A headcount is a *target* for generating unique people, not a second population manager. Military personnel are subsets of those people. Visiting merchants and crews remain their own persistent individuals, not generated new residents. Children, households, labor, owners, slaves, diplomats and rulers must use the same identity registry.
- **Population target = people normally resident in a locality and its immediate dependent settlement area** (unless explicitly noted); transient arrivals and military convoys are separately tracked. No claim that these specific figures follow from ASOIAF source material. The lower and upper ends are tuning candidates, **not uncertainty intervals from empirical data**.
- **Garrison figures** are defensive personnel already included in permanent population *when stationed long-term*. Army levies, militias, fleets, enslaved personnel and hired forces are distinct organizational statuses and must not be double counted.
- **Religion and culture:** individual identity, personal upbringing, present faith, organization memberships and region-level prevalence are separate concepts. Do not impose 100% homogeneous populations.
- **Economy:** soil/crop/livestock items are **potential production inputs**; working farms require actual land parcels, jobs, households, tools, seed, pasture rights and inventories.
- **Leadership:** preserve a known character and an office separately. Never fabricate a hereditary 'house' for a city where sources do not identify one. Do not promote a historical dynasty into office in 298 AC.

## 1. Executive overview and initial scenario envelopes

The largest contrast on this map is **Yin versus Asshai**: Yin is the functioning political and commercial capital of Yi Ti. Asshai physically sprawls across a vast area but supports no more people than a good-sized market town, with no children, no surviving imported animals, and complete dependence on imported food and fresh water. **Never size Asshai's population from its city footprint.** [S1][S2]

| Map ID | Place and status | Proposed resident population | Proposed permanent defenders | Scenario authority |
|---|---|---:|---:|---|
| SEE-001 | City of the Winged Men — legendary, **conditional** | UNKNOWN; optional 0–4,000 if materialized | UNKNOWN | Unknown |
| SEE-002 | Carcosa — claimed inhabited refuge/court, **low confidence** | Optional 500–8,000 | Optional 30–600 | Exiled claimant styling himself 69th Yellow Emperor, personal name unrecorded |
| SEE-003 | Jinqi — active YiTish city | 40,000–140,000 | 1,200–6,000 | Local YiTish governors / princes, names unknown; historically maroon-emperor court |
| SEE-004 | Leng Yi — active island city | 22,000–75,000 | 400–2,500 | God-empress of Leng; local official unknown |
| SEE-005 | Yin — active imperial capital | 200,000–600,000 | 6,000–25,000 | **Bu Gai**, 17th Azure Emperor (family Bu) |
| SEE-006 | Leng Ma — active island city | 18,000–65,000 | 350–2,200 | God-empress of Leng; local official unknown |
| SEE-007 | Turrani — active indigenous Lengii port | 20,000–80,000 | 500–2,500 | God-empress of Leng; local official unknown |
| SEE-008 | Ulo / Ulos — island with an unnamed ruined city; **not confirmed active town** | 0 permanent at ruin as default | 0 default | No established polity or current ruler |
| SEE-009 | Stygai — ruined 'corpse city' | 0 permanent default | 0 | No established living government |
| SEE-010 | Asshai — functioning extremely underpopulated port | 1,500–8,000 | 50–450 (local guards/retainers, speculative) | Government and actual city ruler **unknown** |

**Caution:** these ranges must be cross-calibrated against global 298 AC population totals and available land; particularly Yin and Jinqi are *not* canonically assigned precise populations. Asshai should retain very low occupancy over a huge ruins/structure footprint. The optional Carcosa and Winged Men settlement sizes must **not run automatically** under a conservative canon preset.

### Suggested settlement generation archetypes

- **Imperial port/metropolis:** Yin. Large palace complex, royal precinct, water transport, warehouses, outer fields.
- **Fortified frontier port:** Jinqi. Historic military court, layered walls, barracks, delta/quays and customs infrastructure.
- **YiTish-derived tropical island city:** Leng Yi and Leng Ma. Local cultural blending and island river/sea trading.
- **Indigenous Lengii port:** Turrani. A different settlement layout/style from the northern YiTish-founded towns.
- **Remote mythical outpost:** City of the Winged Men / Carcosa; conditional and never a generic vanilla village.
- **Mountain/island ruins:** Ulos, Stygai. No standard houses with invented inhabitants.
- **Ancient near-empty port city:** Asshai. Dense *built fabric in selected quarters*, vast abandoned districts, black stone, **import logistics**.

## 2. Detailed location dossiers (all ten map observations)

### SEE-001 — City of the Winged Men

- **Status/provenance:** **CANON as a named legendary location**, not verified as a conventional occupied 298 AC settlement. A map locates it on the **northwestern shore of the Hidden Sea**, within the **Mountains of the Morn**; Carcosa is on the southeastern shore. Reports say its inhabitants have leather wings and can fly, but this is **reported folklore**. [S3]
- **Political authority / leading house/person:** UNKNOWN. **Do not generate a known ruling house or a canon winged species.**
- **Culture / religion:** UNKNOWN. Could contain frontier YiTish or unique fictional community *only as explicitly authored mod invention*. Use `unknown_religion` for unspecified persons; do not assign `yi_tish` by name alone.
- **Population / jobs / military:** **No asserted canon count**. Conservative default: no functional community until opt-in. Experimental speculative preset: **0–4,000** residents, e.g. caravan exchange, fishing, gathering, stoneworking, herding, ritual specialists; **no numeric garrison claim**.
- **Land/agriculture:** Hidden Sea shores, highland valleys; small terraces, irrigated gardens and goats/sheep **possible, not sourced**; check lake water quality and surrounding slopes.
- **Construction:** Largely inaccessible lakeshore/canyon settlement with cliff terraces, stone access paths, docks, caves; if mystical traits are enabled later, treat them as a separate verified scenario feature, not ordinary biome spawning.
- **Step 2.2:** create a `LEGENDARY_CONDITIONAL` site; avoid default NPC births, taxes and recruitment.

### SEE-002 — Carcosa

- **Status:** CANON as a named city by the Hidden Sea on the **southeastern shore**, with an exiled sorcerer lord claiming to be the **69th Yellow Emperor** of Yi Ti. He is in **Carcosa**, not an incumbent emperor ruling the entirety of Yi Ti. [S4][S5]
- **Person / dynasty:** **Claimant is known by title, not a confirmed personal given name**; family name **Chai** belongs to the historical Yellow Emperors, and the repo has `dynasty_yiti_yellow`. The current code records `carcosa_yellow_domain` with de-jure/owner = that dynasty; this must represent the **claimant's local enclave** rather than recognized imperial sovereignty. Current dynasty status `DISPOSSESSED` is compatible with an exiled claimant, but reconcile with local holding ownership semantics.
- **Culture/religion:** plausible YiTish exiles, retainers, frontier peoples, with unknown other communities. `yi_tish` where known; religious mix unknown; Lion of Night cult or private sorcerous sect only where authored, **not automatic**.
- **Population/military:** No canon numbers. Use **500–8,000** and **30–600 retainers** only as conditional fictional campaign targets, or start as limited personal court with 0 surrounding city generated until the source existence/scale decision is made. Do not inflate based on the title 'emperor'.
- **Economy:** likely precarious: fishing from the Hidden Sea, trade with mountain paths, caravan provisioning, elite retainer consumption; agriculture only on surveyed lake terraces (SOURCE-INFERRED).
- **Construction:** Secluded lakeside court/compound; unusual imperial pretender architecture, occult academy or watch towers *only if mod-authored*; no forced grand capital.
- **Step 2.2:** create a persistent claimant NPC **without fabricating a given name**; personal claim separate from actual control of other YiTish territories.

### SEE-003 — Jinqi

- **Status:** CANON active city by a delta on the Jade Sea, north of Leng. A river reaches it from the **Bleeding Sea** to the north. The **maroon emperors historically kept their martial court** there to guard against raiders from the Shadow Lands. No evidence of an active maroon dynasty ruling Jinqi in 298 AC. [S6][S7]
- **Leader / affiliation:** Yi Ti; **contemporary governor unnamed**. It may be loyal to Bu Gai, another local prince or exercise significant autonomy—**DO NOT assign a fixed sovereign purely from the imperial history**.
- **Population:** MOD ESTIMATE **40,000–140,000** permanent persons, diverse port population; **1,200–6,000** locally stationed defenders, excluding field campaigns.
- **Culture/religion:** majority `yi_tish` by initial generator prior; minority foreign sailors and merchants; YiTish faith complex and imperial cult—repo `lion_of_night` available, **`maiden_made_of_light` missing** and should be added only with appropriate canon data; not all citizens worship a single cult.
- **Occupations:** porters, sailors, customs officials, merchants, artisans, military, shipwrights, fishing families, delta farmers, warehouse labor, priests and scholars. Trade sector emphasized; rural surrounding workforce for grain, vegetables and livestock.
- **Agriculture:** alluvial delta farmland, possible **wet-rice irrigation (MODEL INFERENCE)**, millet/wheat, vegetables, river fisheries; monsoon/flood controls need terrain and hydrology rules.
- **Architecture:** wharf wards, old maroon dynastic citadel/martial court, frontier barracks, flood defenses, canals and delta settlements; historic *maroon* architectural remnants distinct from present government.
- **Step 2.2:** legitimate present authority not necessarily a maroon emperor; create local governor office, military institution and customs ledger.

### SEE-004 — Leng Yi

- **Status:** CANON active city on the **northern coast of Leng**, resembling a YiTish city in culture and design. [S8][S9]
- **Government:** **God-empress of Leng**, whose current personal name is not specified; regional administrator unknown. Do **not** substitute Bu Gai as the city’s sovereign.
- **Culture:** dominant **YiTish-descended** inhabitants of northern Leng; retain Lengii and foreign merchant minorities. The existing culture IDs `yi_tish`, `lengi` can be used. Avoid inventing a 'House Leng Yi'.
- **Religion:** imperial god-empress veneration alongside YiTish-derived religious practices; local percentages unknown. Local worship is not automatically loyalty to the Azure Emperor.
- **Population:** MOD ESTIMATE **22,000–75,000**; long-term resident defenders **400–2,500** (part of island command).
- **Economy:** maritime commerce, fishing, spices/gems transported from the island interior, wood and timber, ship repair; imported grains and island field production coexist.
- **Agriculture:** tropical coastal suitability where topography permits—**rice-like wet crops are plausible but unconfirmed**, vegetables, orchards, fruit, island livestock and water buffalo-like draft animals only if ecologically justified and explicitly mod-authored; do not assert exact crops from lore.
- **Construction:** YiTish-inspired palace architecture, courtyards, canals, market streets, coastal quays, terraced settlements. Distinguish from indigenous southern Lengii style.
- **Step 2.2:** create governor seat under `dynasty_leng_khiara`, not a YiTish governor title by default.

### SEE-005 — Yin

- **Status:** CANON great port city, current **capital of Yi Ti**, and seat of **Bu Gai, seventeenth Azure God-Emperor**, family **Bu**. Connected to Asabhad by trade route and to forest hinterlands by river. A palace of the reigning emperor is **said to be larger than all of King's Landing**—this describes the **palace**, not population. [S1][S10][S11]
- **Political nuance:** despite imperial titulature, the current emperor has weak direct control outside the capital; many local princes, generals and tax-collectors hold practical power. Carcosa's Yellow claimant and Trader Town's Pol Qo compete politically. Do not apply a unified absolute monarchy over every YiTish settlement. [S1][S11]
- **Repo IDs:** `yin`, `yin_azure_domain`, `dynasty_yiti_azure`; `yin_azure_domain` already `ownerDynastyId = dynasty_yiti_azure`. Preserve. Canon individual `Bu Gai` should be reserved and authored; not replaced by generated ruler.
- **Population:** MOD ESTIMATE **200,000–600,000**; permanent city guards/imperial troops **6,000–25,000**, subject to finance, food and wider army model. No canonical census.
- **Culture/religion:** dominant `yi_tish`, mixed foreign visitors; imperial ancestor worship and many gods, including Lion of Night and Maiden-Made-of-Light; the exact majority percentages are **unknown**.
- **Occupations:** court officials, scribes and tax collectors, domestic service, shipping, silk/textile processing, jade/gems work, crafts, food trade, construction, clerics, army, nobles, rural farmers and fishers in dependent districts.
- **Agriculture:** prosperous river basin and forest fringe. Potential rice/irrigated grains, other cereals, market vegetables, orchards and fish. **Specific crop mixes are MOD ESTIMATES**; terrain/water availability overrides generic rice fantasy.
- **Trade:** Jade Sea shipping, Asabhad route, silk, jade, spices, cloth, royal consumption; ships' crews must be actual traveling NPCs with inventories and home ports. [S10][S12]
- **Architecture:** enormous imperial palace precinct, extensive dense urban wards, river docks, canals/bridges where geography allows, tax/customs houses, religious precincts, wealthy estates, workers' quarters and cultivated hinterland. Landmark palace requires a special designed complex; do not just multiply vanilla villages tenfold.
- **Step 2.2:** separate emperor office, dynasty, city government, local city garrison, imperial external diplomacy, claimants, tax-vassals and temple organizations.

### SEE-006 — Leng Ma

- **Status:** CANON city on Leng's **central western coast**, near a river mouth, sharing YiTish architectural/cultural influence with Leng Yi. [S9]
- **Government:** god-empress of Leng via local magistrates; **no named incumbent city lord**.
- **Culture/religion:** predominant YiTish-descended Leng population, with indigenous Lengii and foreign coastal visitors; veneration of god-empress, diverse inherited practices.
- **Population:** MOD ESTIMATE **18,000–65,000**; **350–2,200** defenders.
- **Economy:** river-to-sea shipping, port crafts, fishing, lumber, interior spices/gems, trade brokerage.
- **Agriculture:** river plain/lowland fertility potentially good (verify gradient), orchards, mixed gardens and wetland crops as MOD ESTIMATE; interior forest should not become fields wholesale.
- **Construction:** harbor at river mouth, bridge/canal infrastructure where viable, YiTish-style built districts, coastal warehouses, farms on accessible land.
- **Step 2.2:** register island-wide navy/army assignments and ports, instead of generating a separate independent state.

### SEE-007 — Turrani

- **Status:** CANON **Lengii-founded port city**, on the **southern coast** of Leng near a river mouth, **wealthy in spices and gems**. Unlike the YiTish-founded northern cities, it is culturally indigenous Lengii. [S13][S8]
- **Government:** ruling dynasty of Khiara the Great and **current unnamed god-empress**; local civic leader not known. The god-empress traditionally takes two husbands, one Lengii and one YiTish; one oversees armies and the other the fleet. **Which specific spouse commands which is not established for 298 AC** without further scenario authoring. [S8]
- **Population:** MOD ESTIMATE **20,000–80,000**; **500–2,500** local defenders, plus island fleet capacity tracked separately.
- **Culture/religion:** strong `lengi` majority; god-empress veneration and indigenous practices, specifics otherwise UNKNOWN; avoid making the whole city YiTish because of earlier colonization.
- **Economy:** spice and gem shipping, forest gathering, wood and ship repair, fishing, crafts and marketplace services.
- **Agriculture:** wetter tropical conditions possible, mixed tropical gardens, fruit, possibly specialized spice orchards; cultivation conflicts with protected/unknown jungle and ruins; treat crop types as **MOD estimates**.
- **Construction:** local Lengii courtyard/domestic forms rather than copy-pasted YiTish architecture, abundant shade/water-management, coastal warehouses, merchant compounds, river mouth structures; dense outer jungle retained.
- **Step 2.2:** cultural boundary within the same sovereign island; mixed marriage rules should not erase indigenous languages or identities.

### SEE-008 — Ulo / Ulos (island and unnamed ruin)

- **Status:** The earlier annotation was read **'Ulo'**. **CANON refers to the mountainous island `Ulos`**, in the Saffron Straits, with an **unnamed ruined city in its north**. There is **no sourced active town named 'Ulo'**. Preserve annotation as unresolved source label until collective map correction. [S14]
- **Government/ruler/house:** UNKNOWN; no evidence for current polity, landlord, official garrison or descendants.
- **Population:** **0 permanent residents at the ancient ruined city by default**; shipwreck survivors, explorers or transient camps could arise as named people via simulation later. No invented 298 AC city population.
- **Culture/religion:** UNKNOWN; do not assign Lengii, Asshai'i or 'Ulthosi' automatically. 
- **Economy:** no established organized present-day economy; fisheries, coastal salvage or passing ships only if NPCs actually perform these actions. Fertility/livestock entirely unknown.
- **Architecture:** mountains, coastal cliffs, a distinctive old ruined urban footprint on the north, maritime approaches/strait navigation; not a living 5× village.
- **Repo gap:** `subregion_masks.json` has `"locationId": "ulos"` but `SouthEastEssosFeatureRegistry` explicitly defers Ulos to Ulthos and the current locations reference list does not clearly define `ulos`. Validate the entire chain before Step 2.2.

### SEE-009 — Stygai

- **Status:** **CANON ruined corpse city**, the City of the Night, in the perpetually shadowed **Vale of Shadows**, by the **Ash** river northeast of Asshai. Reports of demons, dragons or haunted phenomena are **in-universe testimony**, not evidence of functioning public institutions. [S15][S16]
- **Government/house/culture/religion:** no established living sovereign, permanent household distribution or occupation system.
- **Population/military:** **0 ordinary permanent residents / 0 ordinary garrison by default**. Optional unique encounters only when authored and consistent with scenario magic/lore flags; do not spawn a town of 2,000 people.
- **Economic/agricultural:** no farm economy. The Ash is polluted; ordinary water extraction and fisheries should be invalid without specialized rules.
- **Construction:** enormous ruined black/dark architecture, steep valley, rare shafts of sunlight, forbidding passages, collapsed bridges, poisoned waterways; site-of-interest rather than village.
- **Step 2.2:** no automatic births, inns, food production, taxes or normal village inhabitants; extraordinary creatures are separately implemented, not implied by legend.

### SEE-010 — Asshai-by-the-Shadow

- **Status:** CANON inhabited port at mouth of Ash river, with a **gigantic city footprint** but **market-town-sized permanent population**. Only a small fraction of its buildings are lit at night. Black oily stone structures; markets, halls, temples, palaces. [S2]
- **Authority/House:** **No canonical sitting ruler or hereditary House established.** Do **not** invent King of Asshai or a generic Westerosi lord. For 298 AC use an unknown civic/guild/temple institutional model until researched further; local contacts can be generated, but marked `MOD_ESTIMATE`.
- **Population:** MOD ESTIMATE **1,500–8,000** resident adults; **no children born or resident within the city**, per source. This should be modeled as **zero resident children and disabled local birth placement**, not a universal magical claim that all Asshai'i anywhere cannot reproduce. Individuals with Asshai'i culture may live elsewhere and have families. Resident influx can sustain population. Stationed security/retainers maybe **50–450**, not a large garrison by wall circumference.
- **Religion/culture:** `asshaii` available; mixed practices, blood magic, alchemy, shadowbinding, R'hllor followers, Lion of Night and others; canonical source says religious practice is eclectic. No strong percentages justified. Existing `religions.json` has `rhllor`, `lion_of_night`, `black_goat`, `bakkalon`, `church_of_starry_wisdom`, `unknown_religion`, but **no evidence that a given one dominates Asshai**.
- **Food/water/ecology:** CRITICAL CANON RULE: **all foodstuffs and fresh water arrive by ship**; introduced animals die; river fish are deformed and unfit for ordinary consumption. Assign `local_farming_capacity=0`, `safe_drinking_water_from_Ash=0`, `ordinary_livestock=0`. Treat this as a location-specific exception even if surrounding biome is fertile by rainfall. Cargo disruption can rapidly cause shortages. [S2]
- **Trade:** long-distance port for amber, dragonglass, gold, gems and rare materials; visiting merchant convoys, harbor crews, ship supply, caravan traffic. Resource extraction origins need separate site evidence, **do not assume the minerals are mined inside the city**.
- **Jobs:** traders, boat crews, stevedores, warehouse staff, guards, personal servants/enslaved people, occult practitioners, crafts and transport. No resident farmers or ordinary animal husbandry. Strange magical specialists can exist as real persons, but their actions need validated game mechanics.
- **Construction:** very large exterior walls and near-empty black-stone districts; harbor, piers, temples, abandoned mansions, palanquins and markets, a handful of occupied neighborhoods. Do NOT place a dense mega-metropolis population over the entire footprint.
- **Step 2.2:** create major import dependency in settlement inventory and regional trading routes. Childless local demographic rule. No safe river water. Distinct city area from populated district set. Physical NPCs only near player, persistent identities elsewhere.

## 3. Regional and subregional profiles — inherited influences, **not laws for every resident**

**Fields:** `land & water` → `culture/politics` → `livelihood/agriculture` → `generation and special rules`; all invented details labeled `MODEL`.

### R01 — Yi Ti imperial heartland and Yin river basin

- **References:** `yi_ti` (region/realm), `yin`. Existing polygon `territory_zones.json: yi_ti`; insufficient on its own for farm-scale differentiation.
- **CANON:** wealthy imperial history, current weak central authority, silk/spice/jade trading zone, Yin river and great port. [S1][S10]
- **MODEL:** high agricultural potential in drained, irrigable plains; millet, grains, vegetables, fruit and possible paddies; higher urban/port density on roads and river mouths, rural villages around farmable land.
- **Culture:** `yi_tish` baseline, religious diversity; `lion_of_night` and an as-yet-missing `maiden_made_of_light` reference; many local gods, imperial veneration.
- **Political:** local princedoms can be de facto autonomous despite imperial claim; don't auto-spawn an unbroken unified imperial chain.

### R02 — Jinqi delta and northeastern frontier of Yi Ti

- **Reference:** `jinqi` geographic feature; new subpolygon required.
- **CANON:** river connecting Bleeding Sea and Jade Sea, historical military court and frontier defense. [S6]
- **MODEL:** alluvial farmland with periodic floods; water transport, mills, shipworks, troops and river customs; cultivate only outside flood-prone and urban footprints. Frontier conflict and trade risks differentiated from Yin.

### R03 — Eastern YiTish wooded/rural hinterland

- **Reference:** `yi_ti` polygon with additional high-resolution forests / rivers / passes.
- **MODEL:** settlements concentrated near verified river valleys, plains, roads and valleys; forest clearing and specialist orchards; reduced density on steep inaccessible slopes and protected temples.
- **Culture/politics:** local YiTish political affinities vary by governor/temple/road, not just straight-line distance to Yin.

### R04 — Hidden Sea and Mountains of the Morn basin

- **Existing references:** `mountains_of_the_morn` named subregion; `city_of_the_winged_men`, `carcosa`; future lake-zone boundary needed.
- **CANON:** Hidden Sea has two legendary cities on opposite shores. [S3][S4]
- **MODEL:** steep rocky shores, uncertain potable water, narrow passes, terrace agriculture possible only where terrain supports; **very low spontaneous rural settlement density**. Seasonal caravan access and isolation modifiers.
- **Religion and authority:** UNKNOWN outside Carcosa claimant; don't spread Chai imperial worship across this wilderness by default.

### R05 — Northern Leng YiTish-descended coastal communities

- **References:** `leng` and `leng_yi`; current subregion-mask `leng` lacks a north/south cultural split.
- **CANON:** northern Leng's people descended from Yi Ti and maintain similar customs; god-empress of Leng is sovereign. [S8]
- **MODEL:** coastal fishing, imported luxury goods, hill gardens/orchards, possible valley rice, docks and custom houses. Mix `yi_tish` and `lengi` on an explicit local basis rather than applying a universal number.

### R06 — Leng western river and Leng Ma basin

- **CANON:** Leng Ma lies near mouth of west-coast river. [S9]
- **MODEL:** river and floodplain crops, boats, logging and forest-to-port cargo; medium-high settlement density near navigable river, lower inland jungle.
- **Cultural:** primarily YiTish-founded urban traditions with real island-wide Leng imperial authority.

### R07 — Southern Lengii territory around Turrani

- **CANON:** indigenous Lengii stronger in south; Turrani Lengii-founded spice/gem port. [S13]
- **MODEL:** tropical spice groves, fruit, forestry and mining/extraction, maritime economy; high wet-season infrastructure need. Rural settlements possible along the coast and access rivers, not randomly throughout jungle.
- **Culture/religion:** `lengi` as primary cultural template, god-empress veneration; avoid forcing YiTish building style onto all houses.

### R08 — Leng interior jungles and sealed ancient ruins

- **CANON:** vast overgrown ruins and underground tunnels, with entrances historically sealed under penalty of death. Accounts of Old Ones are legendary. [S8][S17]
- **MODEL:** very low inhabited density, high rainforest cover, guarded sacred/forbidden heritage sites, jungle products and dangerous route difficulty; no automatic ordinary villagers inside the labyrinths.

### R09 — Marahai and western Jade Sea island approaches

- **Existing reference:** `marahai` subregion and `jade_sea` geographic feature.
- **CANON:** island geography, maritime route between Moraq/Qarth and Leng/Yi Ti. [S12]
- **MODEL:** island fishing, sheltered anchorages, boats, limited inland farming; no forced specific kingship, culture or occupation of the island without evidence. Maritime encounter/resupply logic.

### R10 — Manticore Isles and southeastern Jade Sea

- **Existing reference:** `manticore_isles` subregion.
- **CANON:** island group south of Leng, near the Shadow coast; precise living settlements unestablished. [S12]
- **MODEL:** limited landing sites, hazardous fauna as special ecological feature if implemented, sparse or no village generation. Do not assume a kingdom or export product merely from the name.

### R11 — Shadow Lands coast and Asshai logistics enclave

- **Existing polygon:** `shadow_lands`; **exception mask required for Asshai**.
- **CANON:** unnatural wildlife/ghost grass stories, strange black stone, imports required for Asshai's sustenance. [S2][S18][S19]
- **Economy:** the city has **zero local food-and-water production**, even if biome-based algorithms predict agricultural fertility. Seaborne imported calories/freshwater are essential; no ordinary farm, animal husbandry or fishery inventory created locally. Outside the city, assume no reliable agricultural baseline without distinct evidence.
- **Culture/religion:** mixed `asshaii` and visiting traders; diverse cults; do not treat the whole Shadow Lands as one uniform faith.

### R12 — Ash river watershed and Vale of Shadows

- **Geography:** Ash travels from Mountains of the Morn through deep Vale of Shadows to Asshai; Stygai lies upstream; rivers polluted, light obstructed. [S16]
- **MODEL:** hazard corridors, no freshwater/food production from river, near-zero ordinary settlement density; travel only through difficult paths. Potential mine/cave/ruin sites as individually authored resources, not generic farms.

### R13 — Stygai ruins and restricted interior

- **Existing geographic ID:** `stygai` is registered as `RUINED_CITY`.
- **CANON:** no current functioning corpse-city settlement, feared by even experienced shadowbinders. [S15]
- **MODEL:** ban spontaneous villagers, animal pens, market stalls and garrisons; keep scenery ruin generation independent of NPC generation; optional event/expedition parties remain identifiable travelers.

### R14 — Saffron Straits / Ulos / northwestern Ulthos approaches

- **References:** `ulos` in `subregion_masks.json`; `ulthos` separate continent; `Saffron Straits` registered in SEE features; Ulos missing adequate unified world-reference entry.
- **CANON:** Ulos is a mountainous island with an unnamed ruin; a strait island and maritime passage, not a named inhabited town. [S14][S20]
- **MODEL:** sparse/no settlement spawning, navigational hazards, small anchorages only where geography supports, no assumed owner; future UL research should refine continental land/jungle ecological data separately.

## 4. Proposed adult employment profiles, illustrative only

These percentages allocate **employed resident adults** to high-level sectors; they do **not** state percentages of the whole population and must not directly create workers in `PopulationCohortManager`. Children, elders, non-employed adults and legally unfree persons remain distinct individuals. Percentages are **MOD ESTIMATES** and sum to 100 per active site.

| City | Food primary (farming/fishing) | Craft/extraction/construction | Trade/transport | Household & general services | Civic/religious/education | Defense/security |
|---|---:|---:|---:|---:|---:|---:|
| Yin (urban workforce) | 6 | 23 | 25 | 29 | 10 | 7 |
| Jinqi | 10 | 22 | 29 | 22 | 8 | 9 |
| Leng Yi | 13 | 25 | 29 | 23 | 5 | 5 |
| Leng Ma | 16 | 25 | 27 | 22 | 5 | 5 |
| Turrani | 13 | 30 | 30 | 18 | 5 | 4 |
| Asshai | **0** | 13 | 40 | 27 | 15 | 5 |
| Carcosa **only if populated** | 18 | 15 | 18 | 28 | 14 | 7 |
| Winged Men city **only if populated** | 23 | 17 | 15 | 25 | 13 | 7 |

**Asshai's 0** food-primary employment also includes fishing. Its craft sector means urban artisans and processing of *imported* inputs, not local crop production. The bulk of Yin's food should come from **separate rural communities with their own real farmer NPCs**, not farming within a dense palace district. Visiting ship crews are not resident workforce unless they actually settle.

## 5. Initial settlement-size and architecture rules

1. **Place role beats cartographic label size.** A palace whose claimed footprint exceeds King's Landing is not a measure of city population. A giant near-empty city like Asshai must have a low occupancy ratio.
2. **Yin:** imperial capital / major port / landmark palace; complex hierarchy of urban districts, supply network and rural satellites.
3. **Jinqi:** coastal delta frontier city, prior military capital; flood-resilient building plan and old fortifications.
4. **Leng Yi/Leng Ma:** large urban ports with YiTish-derived architecture, blended local influences, river/cove siting.
5. **Turrani:** indigenous southern Lengii architecture and harbor, spice/gem trade, different building-family kit.
6. **Carcosa/Winged Men:** opt-in speculative; never silently place five-times-vanilla towns at legend markers.
7. **Asshai:** hugely spread out, few active residences, cargo imports, no safe crops/livestock, deep black-stone visual kit, high abandonment.
8. **Stygai/Ulos:** *ruin* archetype; **zero** automatic houses, jobs, tax yield, garrison or NPC births.
9. **Satellite villages:** only in real suitable farmland/river/road corridors, respecting slopes, water, political risk, woods and material style. Guard against crowding/overlap between reference zones.
10. Preserve physical **sourceX/sourceY**, **geographic projected X/Z**, and **resolved safe-building X/Z** as separate fields during Step 2.2; no coordinate fixes in this research batch.

## 6. Repo integration audit — confirmed existing data and known gaps

Verified against repository master tree as of **2026-10-17**, commit `3772a401bf0fd7c6c7507b0528b7502725988e06`. No modifications made in this batch.

| Repo component | Existing identifiers/features | Needs in Step 2.2 |
|---|---|---|
| `reference/locations.json` | `yi_ti`, `leng`, `shadow_lands`, `asshai`, `mountains_of_the_morn`, `marahai`, `manticore_isles`, `ghost_grass` | Add/validate world locations for `jinqi`, `yin` if supplied via extensions, `leng_yi`, `leng_ma`, `turrani`, `stygai`, `city_of_the_winged_men`, `ulos`; unique IDs, hierarchy and historical status |
| `geography/regions/SouthEastEssosFeatureRegistry.java` | `yin`, `jinqi`, `leng_yi`, `leng_ma`, `turrani`, `carcosa`, `city_of_the_winged_men`, `stygai`, `asshai`, `saffron_straits` | Useful feature names are **not necessarily placed settlements**; link validated source pixels, site status and geometry; Ulos deferred intentionally |
| `reference/subregion_masks.json` | SEE 1609×1161; colours include `yi_ti`, `leng`, `marahai`, `manticore_isles`, `ghost_grass`, `mountains_of_the_morn`, `shadow_lands`, `ulos` | Fine subzones for Leng north/south, Jinqi delta, Asshai's zero-farming exclusion and Hidden Sea; validate missing `ulos` location resolution |
| `reference/territory_zones.json` | `yi_ti`, `shadow_lands`, `moraq_region` polygons | Existing polygon boundaries too coarse for city-specific crops, religions or cultural differences; do not overwrite authored masks |
| `reference/cultures.json` | `yi_tish`, `lengi`, `asshaii` | Compatible parent/neighbor influences and local mixtures; avoid false homogeneity |
| `reference/religions.json` | `lion_of_night`, `rhllor`, `unknown_religion`, `black_goat`, `bakkalon` | **Missing** specific `maiden_made_of_light`, divine god-empress veneration identifiers/organizations if religion model requires them; do not conflate personal faith with loyalty |
| `dynasties/essos_yi_ti_leng.json` | `dynasty_yiti_azure` (family Bu), `dynasty_yiti_yellow` (family Chai, dispossessed), `dynasty_leng_khiara` (current Leng dynasty) | Reserve Bu Gai, Carcosa claimant (name unknown) and unnamed contemporary god-empress; don't fabricate family surnames |
| `landed_holdings.json` | `yin_azure_domain`, `carcosa_yellow_domain`, `yi_ti_domain`, `leng_domain` | Hierarchy/sovereignty audit: competing claimant vs actual control, separately local town authorities |
| `NpcRegistry`/profile/affiliation/household systems | individual identity, culture, faith, employment and genealogy foundations exist | Population target should create **named/persistent residents** with actual households, jobs, age, sex, real political office, and travel; no independent anonymous headcount |
| food/economy/agriculture | holding and settlement agriculture abstractions | **Asshai exception**: all food/water imports; no local farming/normal fishery/livestock; avoid denominator inconsistencies and anonymous farmworkers |
| world generator | terrain, biome and map sampler | region/style architecture templates, terrain-safe placement, roads, local farms only at suitable sites, ruins vs cities, huge Asshai footprint with few residents |

**Repository conflict worth future audit:** `yin_azure_domain` and `carcosa_yellow_domain` both have `capital: true`, but this may be intentional—separate competing royal courts. Do not interpret this as *two capitals of a single unified legitimate polity* without explicit claim/sovereignty context.

**Scenario timestamp caution:** characters known during *A Song of Ice and Fire* should not be shifted to a different reign without chronology checks. Set start date 298 AC and preserve the current novel-era Bu Gai / Pol Qo / Carcosa-claimant situation explicitly.

## 7. Step 2.2 acceptance tests (deferred)

- [ ] All ten source observations have a stable review status and marker pixels later; **source image unchanged**.
- [ ] One place ID per actual place; duplicate map observations do not create duplicate settlements.
- [ ] Asshai: `child_residents=0`, `local_food_production=0`, `local_freshwater_production=0`, `ordinary_livestock=0`; food and water cargo must be provided by actual traders and inventories. No automatic child births in Asshai.
- [ ] Asshai: low active residence density despite colossal wall/structure footprint.
- [ ] Stygai and Ulos unnamed ruin: 0 ordinary households and 0 ordinary garrisons in default 298 AC preset.
- [ ] Winged Men city and Carcosa have explicit `CANONICALLY_ATTESTED_LOCATION` versus `CONFIRMED_INHABITED` flags; do not conflate.
- [ ] Imperial authority: Bu Gai's local real rule and his wider disputed claim represented separately; Pol Qo and Yellow claimant have claim objects, not substituted kings.
- [ ] Leng island's current god-empress governs Leng Yi, Leng Ma and Turrani; indigenous and YiTish-descended populations receive distinct cultures; her named historical ancestress Khiara is **not** the unnamed 298 AC empress.
- [ ] Military member IDs refer to real, persistent NPCs; generals and guards are not separate cohort-based ghost populations.
- [ ] Job totals reconcile exactly with assigned living NPCs; all farm and trade production traced to actual people or groups of such people.
- [ ] Regional profile resolution uses regional polygons + local terrain/land masks + local settlement overrides; does not assign Shadow Lands zero fertility to all of Yi Ti.
- [ ] Population constraints and empire-wide manpower/employment make sense under an eventual consistent global census.
- [ ] Generation completes reproducibly across all confirmed marked locations and rural satellite villages; unique architecture kits and special landmark slots.

## 8. Unresolved research and later geography review queue

1. SEE-001: whether City of the Winged Men has an occupied city, and its actual cultural/religious form; flying inhabitants are a **legend**.
2. SEE-002: Carcosa's built scale, demographics and sovereignty; Yellow Emperor's personal name unknown.
3. SEE-003: Jinqi's current named local governor and present political allegiance unknown; former maroon imperial court is **historical**.
4. SEE-004/006/007: contemporary god-empress's name unknown; named island administrative heads and garrisons unknown.
5. SEE-005: no canonical city census or garrison headcount; very large palace does not establish 600,000 inhabitants.
6. SEE-008: original annotated name `Ulo` versus cartographic `Ulos` **and whether its X denotes the island or unnamed ruined city**. Resolve in final user-map pass, not by guess.
7. SEE-009: Stygai is an abandoned ruin, reported fantastical dangers remain speculative unless separately authored.
8. SEE-010: personal identity of Asshai's sitting civic administrators unknown; religious percentages unknowable; exact meaning of reported universal absence of children should be applied conservatively to **resident city demographics**, not world heredity.
9. Leng interior: map the forbidden labyrinth entrances separately from town sites, with provenance and scale.
10. Existing `locations.json` and `locations_holdings.json` may overlap world-reference IDs: validate combined catalog lookups, maintain one ID per site.
11. Exact coordinate extraction and coastline/water snapping **held** until all regional map research is finished by user agreement.

## 9. Sources and provenance index

The following pages cite published material including *The World of Ice & Fire* and *The Lands of Ice and Fire*. The mod-specific numeric choices above are **not** taken from these pages.

- **[S1]** Yi Ti — political fragmentation, Bu Gai, Pol Qo, Carcosa: https://awoiaf.westeros.org/index.php/Yi_Ti
- **[S2]** Asshai — city scale, population, imports, zero children and animals, religion: https://awoiaf.westeros.org/index.php/Asshai
- **[S3]** City of the Winged Men — legendary claim and Hidden Sea geography: https://awoiaf.westeros.org/index.php/City_of_the_Winged_Men
- **[S4]** Carcosa — location and 69th Yellow Emperor claimant: https://awoiaf.westeros.org/index.php/Carcosa
- **[S5]** Yellow emperors and family Chai: https://awoiaf.westeros.org/index.php/Yellow_emperors
- **[S6]** Jinqi — location and historical maroon military court: https://awoiaf.westeros.org/index.php/Jinqi
- **[S7]** Maroon emperors — former rulers, not today's rulers: https://awoiaf.westeros.org/index.php/Maroon_emperors
- **[S8]** Leng — cultures, god-empress dynasty, two spouses/army-fleet offices, jungles, trade and subterranean ruins: https://awoiaf.westeros.org/index.php/Leng
- **[S9]** Leng Yi and Leng Ma: https://awoiaf.westeros.org/index.php/Leng_Yi ; https://awoiaf.westeros.org/index.php/Leng_Ma
- **[S10]** Yin — active capital and trade route: https://awoiaf.westeros.org/index.php/Yin
- **[S11]** Bu Gai — imperial claim and palace: https://awoiaf.westeros.org/index.php/Bu_Gai
- **[S12]** Jade Sea — international sea routes and regional ports: https://awoiaf.westeros.org/index.php/Jade_Sea
- **[S13]** Turrani — indigenous Lengii origin, spice/gem port: https://awoiaf.westeros.org/index.php/Turrani
- **[S14]** Ulos — mountainous island with northern unnamed ruined city: https://awoiaf.westeros.org/index.php/Ulos
- **[S15]** Stygai — ruined, haunted City of the Night: https://awoiaf.westeros.org/index.php/City_of_the_Night
- **[S16]** Ash river — unsafe water and valley geography: https://awoiaf.westeros.org/index.php/Ash_(river)
- **[S17]** Old Ones — claims and forbidden ancient tunnels: https://awoiaf.westeros.org/index.php/Old_Ones
- **[S18]** Shadow Lands — landscape and supernatural rumors: https://awoiaf.westeros.org/index.php/The_Shadow
- **[S19]** Ghost grass — inedible and ecologically invasive: https://awoiaf.westeros.org/index.php/Ghost_Grass
- **[S20]** Ulthos — relationship to Ulos and Saffron Straits: https://awoiaf.westeros.org/index.php/Ulthos
- **[S21]** YiTish Lion of Night: https://awoiaf.westeros.org/index.php/Lion_of_Night
- **[S22]** Maiden-Made-of-Light: https://awoiaf.westeros.org/index.php/Maiden-Made-of-Light
- **[S23]** Empires/dynasties overview: https://awoiaf.westeros.org/index.php/Golden_Empire_of_Yi_Ti

### Repositories inspected (not a substitute for canon)

- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/SouthEastEssosFeatureRegistry.java`
- `src/main/resources/data/knownworld/reference/locations.json`
- `src/main/resources/data/knownworld/reference/locations_holdings.json`
- `src/main/resources/data/knownworld/reference/territory_zones.json`
- `src/main/resources/data/knownworld/reference/subregion_masks.json`
- `src/main/resources/data/knownworld/reference/cultures.json`
- `src/main/resources/data/knownworld/reference/religions.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_yi_ti_leng.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`

**Next stage:** Continue 2.1 with other submaps. Preserve this document under `docs/research/geography/`. Create a single collective map-marker correction batch *after* 2.1 coverage, then integrate vetted geography via Step 2.2.
