# Known World — Research 2.1: SI (Summer Isles), 298 AC

**Project:** `dtzudontsay/Minecraft_Server_Mod2` / Known World (book-forward ASOIAF scenario)  
**Prepared:** 2026-10-17  
**Map:** `si.png`, regional source size **2952 × 1714** pixels, annotated X colour `#ED1C24`.  
**Scope:** Four user-marked towns/cities, their associated political and economic hinterlands, and fourteen region/transport-zone baselines.  
**Stage:** **2.1 RESEARCH ONLY.** No Java/JSON implementation, village spawning, or final X-coordinate adjudication is performed here.  
**Companions:** `KNOWN_WORLD_RESEARCH_2_1_NE_298AC.md`, `..._NWE_298AC.md`, `..._SWE_298AC.md`, `..._SE_298AC.md`, `..._NEE_298AC.md`, `..._SEE_298AC.md`.

## 0. Evidence and modeling rules

- **CANON**: Book/official-cartography statements as summarized by linked *A Wiki of Ice and Fire* references. In-universe rumors must remain rumors. Map locations derive principally from *The Lands of Ice and Fire*, and island history from *The World of Ice & Fire*.
- **REPO**: Existing repository data, verified from the current main branch on 2026-10-17. REPO data does not imply a claim is canon or that a feature is already operational.
- **SOURCE-INFERRED**: Plausible conclusion from established history and geographical circumstances, not direct text.
- **MOD ESTIMATE**: Deliberately provisional tunable headcount, defensive personnel, occupation shares, crop potential or building footprint. No numerical settlement census, harvest yield or garrison for these four towns has been established by the sources.
- **UNKNOWN**: No supported person, religion, troop count, area size or legal relationship identified. Preserve unknown rather than substitute a plausible statement as an established fact.
- **All people = persistent individuals**, physically encounterable when their region is activated. Population numbers are **generation targets for actual NPC identities**. A soldier, ship captain, farmer or visiting merchant is not counted again in a parallel anonymous human ledger.
- The figures here refer to **permanent residents of the settlement and its locally dependent hinterland**, not all residents of the surrounding major island. Economic-sector shares apply to **working people, not the total population**; children, elderly residents, dependents and nonworking adults must remain real residents with household identity.
- Stationed guards, sailors and marines are **subsets** of those residents where locally based. Offshore vessels and their crews may be transient; do not duplicate them at the port.
- Do not use geographical `populationWeight`, `militaryValue`, `taxBase`, or social-influence weights as measured population, troop counts or percentages.
- Preserve the input annotations **SI-001 through SI-004** pending the user's **single consolidated marker correction pass** after all submaps. The matched names are reviewed observations, not yet final pixel centres, duplicates or build coordinates.
- **Critical naming finding:** the location marked/known as **Lotus Port** on *The Lands of Ice and Fire* is corrected by the reference to **Lotus Point**. Keep `lotus_port` as a map/source alias and existing geographic feature ID; do not create two cities or discard the name written on the map. [S3]

## 1. Executive overview

The Summer Isles are an archipelago of over fifty islands. **Walano, Omboru and Jhala contain over nine tenths of their population; Walano is the most populous, Jhala the largest by area.** Society is not governed by one permanent centralized king. Island principalities, especially multiple rival princely domains on large islands, are significant. Slavery is prohibited. International trade and seafaring are important, and islanders are noted for excellent archery, swan ships, tropical foods, spices and feathered garments. Local land disputes can be settled by unusually ritualized warfare; defense against slavers and pirates is more conventional. [S1][S4][S5]

**Top scenario constraints:**

1. **Four mapped locations are known inhabited places**, not ruins: Last Lament, Lotus Point (= map's Lotus Port), Tall Trees Town, Ebonhead.
2. **Named known ruling family does not imply today's monarch.** House Qo is historically of Sweet Lotus Vale, with **no currently documented person**; House Xho's named Prince **Jalabhar Xho is in exile in King's Landing at the scenario start**, *not* reigning on Jhala. [S6][S7]
3. **Koj is the foremost swan-ship construction center**, despite having no red X on this particular map. Its known Pearl Palace/princes and port activity justify future geographically placed sites, but we must not invent an X coordinate. [S8]
4. **Tall Trees Town is a world-unique sacred historical site**, centered on the Talking Trees, their carved laws and songs, and the priestess custodians. No general-purpose '10× village' generator may erase or casually replace those trees. [S9]
5. **The island pantheon has roughly twenty deities**, favoring a god and goddess associated with love, beauty and fertility. **There is no canonical single named `summer_islander_religion` in the current repo**. The existing `unknown_religion` is unsuitable as a final default for the whole population; Step 2.2 should add an umbrella reference with explicit provenance and preserve cult variation. [S1][S10]
6. **Goldenheart comes from Jhala and Omboru, not every island, and export is forbidden**. It can be used for local bows and defense fleets, but must not automatically become an ordinary unregulated trade commodity. [S11]

### 1.1 Initial economic and demographic envelopes — not canon

| Map marker | Canonical location | Status/type | Permanent resident target | Locally based defenders | Current leader/authority |
|---|---|---|---:|---:|---|
| SI-001 | **Last Lament**, NW Walano | Active coastal **town** | **3,000–12,000** | **80–450** | Local prince/princess or their delegate; **not named** |
| SI-002 | **Lotus Point** (marked **Lotus Port**), SW Walano | Active **city/port** | **20,000–70,000** | **350–2,000** | Local prince/princess or council; **not named** |
| SI-003 | **Tall Trees Town**, SW Walano | Active **town** and sacred center | **9,000–35,000** | **100–650** | Temporal ruler **unknown**; priestesses are established custodians of the Talking Trees |
| SI-004 | **Ebonhead**, mouth of Sweet Lotus Vale, Jhala | Active **city/port** | **18,000–65,000** | **300–1,800** | Present ruler **unknown**; House Qo has historical title of Sweet Lotus Vale; present possession unproven |

These are **tuning bands**, not evidence-backed statistical intervals. They make no claim about the archipelago's total population, which must be calibrated from the usable island areas, agriculture, and the global scenario in Step 2.2. The coastal settlements do not exhaust the populations of Walano or Jhala. Since Walano is canonically most populous, **global region calibration must preserve Walano > Omboru and Walano > Jhala by total population**, although the ranking or respective ratios are not specified by lore.

**Not established for any of these four places:** 298 AC census, actual garrison number, named contemporary reigning prince, named town administrator, exact crops farmed, port tariff schedules, per-household wealth, exact settlement footprint and count of masonry/wood structures. Avoid inventing a canon claimant to fill these blanks.

### 1.2 Working-adult occupational compositions (MOD ESTIMATE; percentages sum to 100)

| Locality / dependent hinterland | Food crops & horticulture | Fishing & sea harvesting | Timber/forestry/other crafts | Commerce, shipping & port labor | Government, learning & religious services | Security/maritime defense |
|---|---:|---:|---:|---:|---:|---:|
| Last Lament | 37% | 25% | 15% | 12% | 5% | 6% |
| Lotus Point | 19% | 13% | 21% | 30% | 9% | 8% |
| Tall Trees Town | 26% | 12% | 18% | 16% | 22% | 6% |
| Ebonhead | 33% | 13% | 18% | 23% | 7% | 6% |

**Sector boundaries:** Fishing includes fishers, netmakers and shore processing. Craft includes woodworking, textiles, bowmaking (where material available), pottery and maintenance. Commerce includes mariners, porters, warehouses and market workers. Religious services are especially prominent at Tall Trees Town **because of its documented cultural role**, but the percentage is a mod guess. Security includes locally employed guards and bowmen, not every potential militia member or a ship visiting from elsewhere. Allocate each **real adult** to one primary occupational sector initially, with secondary seasonal roles explicitly represented rather than making phantom labor equivalents.

### 1.3 Broad culture, religion and language proposals

- Default **culture**: existing `summer_islander` for most local births and upbringing. Foreign sailors, exiles and merchants can have their own persistent cultures; do not generate a 100% homogeneous island by rule. Distinct **Rhoynish-descended community** persists on the **Isle of Women**, even if that island has no named X; `rhoynar` already exists as a heritage identifier. [S12]
- Default **language**: `summer_tongue` (existing). `trade_talk` is common at commercial ports; `common_tongue` may be known by individual cosmopolitan merchants. The existing repo's 0.95 and 0.24 influence **weights** are not census percentages. [S1]
- Default **faith**: island pantheon with multiple individual cult traditions; the god and goddess of love/beauty/fertility prominent. Exact deity names and city percentages **UNKNOWN**. In Step 2.2 consider `summer_islander_pantheon` as a **HYBRID_CANON grouping** distinct from individual named cults, and make it compatible with the existing religious-practice framework. [S1][S10]
- **Political**: local princedoms/queendoms, with equal legitimacy for female rulers/warriors; no fictitious single summer-island king. [S1][S13]
- **Law**: slavery forbidden, ritualized inter-principality conflict; differentiate ritual battlefield contests from organized maritime anti-slaver defense. [S1]
- **Diet**: fruit and fish established; tropical roots, vegetables, beans, poultry, goats and household gardens are plausible **MOD ECOLOGICAL OPTIONS**, not reported crop lists. [S1]

## 2. Detailed settlement dossiers

### SI-001 — Last Lament (northwestern Walano)

**Status / source:** **CANON inhabited town**, located in northwest Walano, south of Stone Head island. The cited map source documents its name and position but provides no named ruler, scale, garrison or specific trades. [S2]

**Political title / allegiance:** Summer Islander local principality within Walano. A ruling prince/princess or local magistrate is **SOURCE-INFERRED**, not named. No extant named house in the repository should be assigned solely because it is summer-islander.

**Population:** **3,000–12,000 permanent residents (MOD ESTIMATE)** across town and small dependent coastal hamlets. Example midpoint ~6,000–7,000 is **not a canon fact**; record a target configuration, then materialize unique persons in households. Small resident foreign merchant minority is possible but not guaranteed.

**Military:** **80–450 locally organized defenders (MOD ESTIMATE)** within the resident headcount: coast watch, port guards, skilled bowmen, occasional patrol-boat crew. Additional seasonal militia not assumed permanently mobilized. Sea-lane defense and raiding threats should be event-driven, not an automatic standing war fleet.

**People, culture, faith, and language:** Mostly `summer_islander`, `summer_tongue`, island pantheon (proposed later); travelers using `trade_talk`. Local noble/priest lineages not individually attested. Household formation and professional diversity should match a working maritime town.

**Livelihoods:** Coastal line fishing, shore nets, salt or smoked fish (processing techniques inferred), boat repair, modest mercantile wharves, fruit gardens, plantation/orchard operations where terrain permits; artisans and carpenters. **Do not automatically place Koj's major swan-ship industry here.**

**Soils, food and animals:** Conditional well-drained coastal garden soil and sheltered fertile patches; wetlands or salt-spray-exposed beaches are unsuitable for intensive crops. Tropical fruits; roots and garden crops possible by inference. Fish, small household poultry and draft/pasture animals only where supported by ecological carrying capacity. Freshwater must be determined from water-layer/hydrology rather than invented springs.

**Building generator:** Mid-sized irregular harbor town with a protected small quay, jetties, fish drying/storage areas, markets, sun-shaded homes, thatched/timber/hardwood structures on raised foundations in wet areas, winding coastal paths, limited watchtowers. Stone masonry on suitable rocky sites only. Avoid giant uniform castle walls and European village churches.

**Integration dependencies:** Port access / local boats, coastal trade, fisherman occupation, stable ruling office unresolved, local farms and households; maritime defenses sourced from identified people. Birthplace and residence stable location ID `last_lament` should be added or linked in Step 2.2.

**Unknowns:** Administrative seat, actual harbor depth, precise river mouths, relative status compared with nearby Walano towns, troop inventory, local guilds and crops. Preserve original source X pending later coast adjustment.

### SI-002 — Lotus Point, alias **Lotus Port** (southwestern Walano)

**Status / source:** **CANON city** on southwest Walano, north of Koj. The reference specifically identifies *Lotus Port* on the published map as a mistake for **Lotus Point**. For this project, `lotus_point` is the proposed canonical place ID, while `lotus_port` remains a first-class **alias for the user mark and the existing geographic feature**, never a second settlement. [S3][S14]

**Political title / allegiance:** Summer Islander prince/princess or municipal official, **identity unrecorded**. Its proximity to Koj means commercial links, **not** that Lotus Point belongs to the Prince of Koj or to House Xaq. Territorial authority must not be transferred across the water by proximity.

**Population:** **20,000–70,000 permanent residents (MOD ESTIMATE)**, including an urban center and attached agricultural settlements. Temporary merchants and sailors, including arrivals from foreign ports, remain separate itinerary-based NPCs; do not add them to the permanent population or spawn replacement duplicates every visit.

**Military:** **350–2,000 locally stationed defenders (MOD ESTIMATE)**, port watch, bowmen, city guards, patrol service. Crews on swan ships home-ported at Lotus Point may be based there, but most shipbuilding occurs across the water on **Koj**. A shipping census must not claim every ship at the docks is owned by Lotus Point.

**People, culture, faith, and language:** Predominantly `summer_islander`; maritime variety may create a larger `trade_talk`/`common_tongue`-speaking minority than rural Walano; religions diverse at individual level within the Summer pantheon and visiting traders. Female ship captains and religious officials are normal by cultural rule; avoid restricting leadership by Westerosi customs. [S13]

**Economy:** Harbor logistics, inter-island trade to Koj and Omboru, oceanic merchant traffic, dock warehouses, boat maintenance, retail markets, fruit and fish processing, feather craft, hardwood furnishings, textiles, food suppliers; limited local agriculture plus imported goods. Imported iron/tin and metals are essential supply bottlenecks because the island economy lacks plentiful sources. [S1][S8]

**Soils / agriculture:** Dense urban footprint reduces cultivated area locally; gardens/orchards inland, possible fertile lower slopes and well-watered lowland farming outside immediate built district. Common produce: fruits and fish strongly grounded; roots/vegetables/legumes, goats/poultry **only speculative ecological templates**. No automatic goldenheart harvest on **Walano**: its confirmed range is on **Jhala and Omboru**. [S11]

**Construction:** Substantial port city with multiple quays, customs/stores, maritime markets, sailors' lodging, ferry points toward Koj, harbor administration, shaded public courts, tropical houses, religious sites, warehousing, water cisterns, residential wards and gardens. Buildings should follow coast and topography; no perfectly rectangular Western castle-city and no endless vanilla farm plots.

**Integration dependencies:** Stable ID alias migration (`lotus_port` -> canonical `lotus_point` after audited reference resolution), trade capacity, import/export infrastructure, household payroll, merchant organizations, ownership of actual vessels and their named crews, island-linked ferries, local ruler office unresolved.

**Unknowns:** Actual population rank among Walano cities, incumbent prince, city plan, commercial institutions, naval force count, legal status of harbor guilds.

### SI-003 — Tall Trees Town (southwestern Walano)

**Status / source:** **CANON inhabited town** on southwest Walano. It is culturally/religiously special: ancient, enormous **Talking Trees**, shaded avenues and priestesses who preserve carved histories, songs, commandments and laws on living trunks. It was sacked in **19 AC** by pirates/slavers; that event is **historical**, not a sign the town remains destroyed in 298 AC. [S4][S9]

**Political title / allegiance:** Current temporal prince/princess **unknown**. Priestesses have documented custodial/religious-historical authority; **do not automatically claim that they possess sole secular kingship**. Distinguish municipal ruler, priesthood, sacred grove caretakers, archivists/teachers and legitimate local security offices.

**Population:** **9,000–35,000 resident persons (MOD ESTIMATE)** in town and dependent farms. The sacred grove physically occupies prominent space rather than being demolished for density. Pilgrims and outside scholars/merchants remain visitors, not additional unanchored residents.

**Military:** **100–650 standing/local defenders (MOD ESTIMATE)**, especially protection of sacred trees and harbor approaches; larger emergency levies may be possible but should be mobilized from actual households. Religious priests' ability to schedule ceremonial conflicts does not make every priest a soldier.

**People, culture, faith:** `summer_islander`, `summer_tongue`, strong local pantheon, individual cult preferences; religious priestesses, songkeepers, carvers, teachers, artisans, traders, fishers, farmers and ordinary families. Non-Summer Islander merchants possible; no evidence for a specific 298 AC individual head priestess.

**Economy:** Religious/cultural visitors, ritual services, carving and woodcraft, local teaching or recitation of histories, boat/ship supplies, fish, fruit, market goods, coastal trade. No automatic high-volume logging of the **Talking Trees**; they must be protected named historical monuments. Treat history carving as a productive institution with appointed real persons, materials and social influence.

**Soil and agriculture:** Fertile natural pockets in Walano are plausible, but preserve groves, large roots, floodplains and protected landscape. Orchards, fishing and small hinterland farms rather than plowing sacred tree precincts. Do not replace the historical woodland with mass monoculture.

**Building generator:** **Unique manually governed landmark plan:** preserve multiple giant Talking Trees and a sacred grove; shade-covered paths, carved monuments, temples and teaching courts integrated into tree bases, modest living quarters, artisan alleys, port/ferry infrastructure if shore access permits. Existing base terrain trees are not substitutes for the canonical Talking Trees. Respect tree roots when grading terrain. Individual residences have inhabitants; temples have custodians; no phantom population.

**Historical chronology:** Pirate/slaver destruction and mass abduction in **19 AC** must be recorded as a past historical event. The scenario should start with an **active restored town** unless separately sourced otherwise. No eternally active 19 AC invasion.

**Integration dependencies:** Explicit sacred-location status, unique landmark generator/schematic, historical records, religious organization membership, conservation/forest exclusion masks, dialogue historical knowledge, cultural identity and actual priestesses. The existing `tall_trees_town` geographic feature needs a location/settlement record.

**Unknowns:** Name and extent of grove, number of priestesses, current ruling house, shape of restored defenses, local economic taxes, exact population and garrison.

### SI-004 — Ebonhead (mouth of Sweet Lotus Vale, Jhala)

**Status / source:** **CANON coastal city**, at the river mouth of Sweet Lotus Vale in south/southwestern Jhala. To the east are Xon and Parrot Bay. Geographic placement and the vale are clear; contemporary city's lord and scale are not. [S5][S15]

**Political title / allegiance:** Ebonhead participates in the **Sweet Lotus Vale** political/economic sphere by geography. Historic **House Qo** of the vale includes **Xanda Qo**, who led resistance to slaving, and **Chatana Qo**; **no known Qo currently appears in the main-series time span**. The repository sets `sweet_lotus_vale_domain` de-jure and owner to `house_qo`. Preserve its authored status as a *scenario decision under review*, **not proof that House Qo's known ancient members are alive or its unnamed descendants presently rule Ebonhead**. [S6]

**Population:** **18,000–65,000 permanent residents (MOD ESTIMATE)** in city and connected fertile lower-valley villages. Distinguish the population of the entire Sweet Lotus Vale from the Ebonhead catchment to avoid double counting when additional hamlets are created.

**Military:** **300–1,800 locally based defenders (MOD ESTIMATE)** in city/river-mouth and maritime watch, plus potential archery militia drawn from local households. Trained bows from local goldenheart are supported at the *island* level; scarcity and management must be modeled. Foreign sailors and ship-based guards are not automatically local garrison members.

**People/culture/faith:** `summer_islander`, native pantheon, `summer_tongue`; rural and urban vocational mix. No inherited surname assumption for everyone: noble house identity and family names of common people are separate. Can host foreign trading groups; no asserted citywide fixed foreign proportion.

**Economy:** River and coast trade, fish, fruit, vineyards or other wine-growing sites (island wine canon, exact growing locality inferred), spice horticulture (regional suitability only), food processing, timber/hardwood crafts, archery/bowmaking with controlled goldenheart sourcing, markets, shipping to northern islands and overseas. Goldenheart harvesting and wood trade need **prohibition** and controlled legitimate use; do not set it as an unrestricted export. [S1][S11]

**Soil/fertility:** Lower river valley/alluvial or volcanic-looking tropical conditions could be productive **only after terrain and soil survey**. Proposed suitable patches: rain-fed orchards, gardens, tropical roots, pepper/spices where microclimate allows, possible upland pasture. High flood-risk mouth requires drainage and elevated buildings. Jhala's mountainous forest/goldenheart landscape creates permanent conservation and logging constraints.

**Construction:** Large river-mouth harbor town/city, quays and timber landing places, fish markets, broad fruit/spice markets, stone-and-wood/prismatic roofed tropical neighborhoods (style to be authored), river crossings and bridges, upstream village roads, hillside gardens/orchards, princely compound **only if actual authority and style are separately established**; sunset-facing wharves, wetlands preservation and flood-safe foundations.

**Integration dependencies:** Anchor to `sweet_lotus_vale_domain`, reconcile Ebonhead town vs territory economic coverage and any House Qo incumbent NPC, connect river trade to farms and distinct household ownership. Center marked `SI-004` will remain source coordinate until alignment/suitability pass.

**Unknowns:** Current ruling person and whether Qo still de-facto governs, crop proportions, market taxes, actual militarized fleet, coastal hydrology, historical fortification style.

## 3. Regional influence and settlement-geography dossiers (14)

**Important:** Some entries below have existing **color-coded subregion masks** but **no detailed cultural/economic distributions**; map zones are not the same as principality boundaries. The scope is a starter profile for remote villagers and settlement suitability, not a declaration of exact legal ownership.

### SI-R01 — Walano: main inhabited island

- **CANON:** Northernmost of the three largest islands and the **most populated**, containing Last Lament, Lotus Point and Tall Trees Town. [S1][S14]
- **Influence proposal:** Strong `summer_islander`, `summer_tongue`, varied religious affiliations under island pantheon; urban/coastal trade concentrated south and northwest.
- **Ecology:** Tropical forests, coast, upland relief; forests and river corridors protected from indiscriminate tilling. Use water availability and terrain for fertile land decisions.
- **Rural villages:** Fishing hamlets/coastal farms inland of secure harbors; decreased density in steep jungle uplands. A rural farmer is an actual assigned adult living in an actual household, not a cohort worker-equivalent.
- **Politics:** Multiple rival local principalities possible. **No automatic 'king of Walano'** controlling the entire island.

### SI-R02 — Northwest Walano / Stone Head approaches

- **Anchor:** Last Lament, nearby Stone Head island.
- **Livelihood:** Fishing, shore trade, boat carpentry, orchards where soil supports; MOD INFERENCE.
- **Risks:** Storms, exposure, pirate threat, shallow rocky landing; true coast and bathymetry need sample.
- **Settlement layout:** Dispersed coast villages + one secondary town. No major shipyard monopoly.

### SI-R03 — Southwest Walano / Lotus Point commercial coast

- **Anchor:** Lotus Point and neighboring Koj sea crossings.
- **Livelihood:** Overseas and inter-island trade, warehousing, fishing, port crafts, imported metals and grain as needed.
- **Culture:** Strongest plausible trade-language minority among the four marked sites; must be represented via actual merchants and families, not imposed per NPC.
- **Construction:** Tiered harbor city, quays, customs, roads from farms. Respect coastal suitability.

### SI-R04 — Tall Trees cultural precinct and Walano hinterland

- **CANON:** Sacred Talking Trees, priestesses, recorded laws/histories. [S9]
- **Legal environmental layer:** Exclusion/protection zones around monumental trees; avoid generic logging, crop fields, roads and building overlap.
- **Jobs:** Temple staff, story/carving custodians, artisans, pilgrimage support; no hypothetical novel god names.
- **Historical behavior:** NPC cultural memories and historical knowledge may refer to the 19 AC raid, without implying current devastation.

### SI-R05 — Koj shipbuilding principality

- **CANON:** Small island whose shipyards construct most swan ships; historical Prince Malthar Xaq, princely **Pearl Palace**. [S8]
- **Economic baseline:** Skilled shipwrights, carpenters, ropes/sail textiles, ship maintenance and supply boats. Wood likely must be imported from other islands or managed local stockpiles (**INFERRED**, timber ownership TBD).
- **Political:** Repo `house_xaq`, `koj_domain`; **current incumbent unknown**. Malthar Xaq is ancient history, not a ruler in 298 AC.
- **Worldgen:** High-priority **unmarked** principality/shipyard site. **Do not create a fake source X**; map/hand-author coordinates later.
- **Labor:** Every swan ship requires actual identified crew and archers; every dock job is attributable to a persistent person.

### SI-R06 — Isle of Women (Abulu) and Rhoynar descendants

- **CANON:** Nymeria's Rhoynar refugees lived here; a few thousand followers remained and descendants continue to inhabit the island. [S12]
- **Culture:** `summer_islander` with explicit Rhoynish heritage (`rhoynar`) as a local historical contributor. Present native language persistence **unknown**; don't automatically require every resident to speak Rhoynish.
- **Economy:** Fishing and limited agriculture on the small island; **mod-inferred**. Beware historic evidence that refugees had food difficulties; do not force high fertility.
- **Settlement:** A distinct named island community with actual households, though the user's SI map did not include a marked X for a named town.

### SI-R07 — Omboru

- **CANON:** Middle large island, populated, separated from Walano by Smiling Sea and Jhala by Indigo Straits; goldenheart habitat. The "old red men" apes are described here. [S1][S11]
- **Influence:** Island pantheon, summer-islander social customs, multiple local principalities. Crops and population targets need terrain calibration.
- **Forestry:** Managed goldenheart forests must be conservation-limited and must not create an open export economy; other local hardwood crafts possible.
- **Villages:** Ports at suitable shore pockets, island markets and inland orchard/garden hamlets; forest interior lower settlement density.

### SI-R08 — Smiling Sea, Isle of Birds and intra-island routes

- **CANON:** Named water and small islands between Walano and Omboru. [S1]
- **Transport:** Ferry and swan-ship waypoints; travel time by actual route and vessel, not flat Euclidean distance.
- **Livelihood:** Fishing, small safe anchorages, bird habitat and maritime navigation (site-specific occupations uncertain).
- **Physical constraints:** Water tile cannot receive ordinary structures except docks/islets; seabird habitat and coast buffers.

### SI-R09 — Jhala: broad large-island baseline

- **CANON:** Largest island by **area**, smaller population than Walano, with forests, mountains, wildlife, valleys and goldenheart. [S5]
- **Culture:** Summer Islander; multiple principalities. Religion broadly island pantheon, with local variation.
- **Economy:** River-valley farms, forest products, tropical fruit and spices where sustainable, inland wildlife; managed high-value goldenheart.
- **Settlement density:** Major valley mouths and rivers, smaller inland orchards/pastoral hamlets, broad low-density jungle and mountains. Never uniformly populate the entire giant island.

### SI-R10 — Sweet Lotus Vale and Ebonhead catchment

- **CANON:** Settled western/southwestern Jhala river valley; Ebonhead at its mouth; historically House Qo principality. [S5][S6][S15]
- **Economy:** River transport, coastal port, horticulture, hinterland farms, forest-edge craft production (specific soil/crops MODEL ESTIMATE).
- **Politics:** `sweet_lotus_vale_domain` exists; **house_qo** is historical and repository owner, but current prince is unknown. Keep title-holder and administrative owner separate.
- **Geography:** River path, lowland flood model, freshwater, ford/bridge sites and orchard suitability need traced layers.

### SI-R11 — Red Flower Vale / eastern Jhala

- **CANON:** Rival Jhala principality historically belonging to House Xho; **Jalabhar Xho is exiled at Robert Baratheon's court**. [S7]
- **Political status:** Repo currently declares `house_xho` de-jure/owner. Distinguish **claimant and former dynastic title** from de-facto current controller, who is **UNKNOWN**. This is not an invitation to randomly materialize Jalabhar as local ruler.
- **Economy:** Valley farming, orchards, river/port outlets, timber and artisanal trade (**inferred**).
- **Worldgen:** Include principle that region may support non-user-marked settlements, but do not invent a named regional capital or date-specific ruler until researched.

### SI-R12 — Indigo Straits / Jhala–Omboru crossing

- **CANON:** Water separating the two largest southern islands. [S1]
- **Economy:** Inter-island shipping, ferry passages, fishery and sail/ship services.
- **War:** Trade and anti-slaver protection, no permanent naval blockade without a simulated political decision; swan ships defend themselves with archers rather than generic galley war fleets.
- **Worldgen:** Island strait crossings and plausible ports, avoid placing farms on water.

### SI-R13 — Parrot Bay, Xon, Doquu and the Bones

- **CANON:** Southern Jhala coastal/island names (map-proven). [S5]
- **Livelihood:** Fishing, maritime stops, small settlements only where coast, soil and freshwater allow (**MOD INFERENCE**).
- **Ecology:** Tropical bird/reef/coastal habitat can contribute feathers and food; do not destroy all habitat with settlement density.
- **Population:** Mostly small communities, generally lower than three main islands; do not automatically choose a named ruling house from a nearby port.

### SI-R14 — Western outliers: Singing Stones, Three Exiles, Isle of Love, Moluu / northern Stone Head

- **CANON:** Small island groups west/north of the larger islands; their inhabitants, rulers and populations are generally **not established**. [S1]
- **Environment:** Patchy freshwater, island exposure, terrain/landmask-first; many land patches should remain uninhabited or carry only seasonal use.
- **Economic possibilities:** Sea foraging, fishery, birdlife, passing anchorage, low-level island trade; MOD INFERENCE, not canonical agricultural crops or villages.
- **Procedural controls:** Small land area limits houses, fields and resident target; explicit density suppression for near-sterile offshore rock. No forced castle per island.

## 4. Whole-isles political, social and economic foundations

### 4.1 Government, dynasties and living individuals

| Existing identity | Canon / repo status | Required simulation handling |
|---|---|---|
| `house_qo` / Xanda Qo and Chatana Qo | Canon **historic** Sweet Lotus Vale princely lineage. Repo active dynasty and owner of vale territory. | Historical members must **not spawn living in 298 AC**. Current princely incumbent not named; reconcile repo current owner as a mod-authored assumption. |
| `house_xho` / Jalabhar Xho | Canon exile of the Red Flower Vale at court in King's Landing. Repo owner/de-jure of Red Flower Vale. | Create/reserve one persistent **Jalabhar Xho** at court as of 298 AC, preserve homeland/birthplace and claimant status; do not duplicate him in Jhala or declare he controls the valley. Actual controller unknown. |
| `house_xaq` / Malthar Xaq | Ancient prince/shipbuilder of Koj, not a known living 298 AC leader. Repo says Koj held by `house_xaq`. | Preserve dynastic history; current Koj ruler **unnamed**; don't create the historical Malthar as a contemporary. |
| Numerous other princely families | Canon says multiple rivals and male/female rulers; names and location allocations **unknown**. | Generate new names only as explicitly **MOD AUTHORED** when a location needs a court, not as apparent confirmed nobility; avoid a single fictional imperial ruler. |
| `summer_isles_domain` holding | Existing umbrella **REGION** holding. | Treat as **geographic aggregation** initially, not proof of centralized direct lordship or a standing Summer Isles royal army. |

**Exile gameplay:** a deposed ruler can possess a historical title claim while living and working in another realm; military support, political lobbying, language, memories and inherited kinship persist. Territorial control of the Red Flower Vale is a separate question. This should exercise all existing NPC / noble / claims / residence systems together.

### 4.2 Religion and values

- Canon gods: multiple deities, notably love/beauty/fertility; temple/priesthood functions and distinctive funerary customs; female and male civic and martial roles. [S1][S10][S13]
- Existing repo has `summer_islander` culture and `summer_tongue` language, but **no Summer Islander pantheon religion ID**. Suggested reference: `summer_islander_pantheon` (HYBRID_CANON umbrella label), then optional local deity/practice details only where evidenced or overtly authored.
- Do **not** blindly set all residents' religion to `unknown_religion`, nor copy over `faith_of_the_seven` from Westerosi settlements. The actual practice remains individualized by person and household.
- Religious significance at Tall Trees Town should influence learning, historic memory, festival calendars, legitimacy, mediation, temple services and maybe ritual-war scheduling; none of those generate anonymous priests.
- Existing repo `CULTURAL_TOLERANCE:0.55`, `PERSONAL_FREEDOM:0.45`, etc. are **mod scores**, not 55% culturally tolerant citizens or 45% personally free persons. Review their values in the context of slavery prohibition and local equality; do not map an invented generic Westerosi social class distribution onto the islands.

### 4.3 Military and war

- Islander ritual disputes: priests select times/locations; spears/slings, no goldenheart bows; defeated surviving warriors commonly exiled, not massacred. [S1]
- External defense: exceptional goldenheart bowmen, escorts aboard merchant **swan ships**, port watches and anti-slaver maritime security; no standard foreign-conquest navies or independently assumed permanent warship fleets. [S1][S16]
- Force model must distinguish **stationed guards**, **local militia**, **ritual dispute participants**, **merchant crews with guard assignments**, and **true expeditionary fighters**. They may overlap in individual membership but count **once** in human totals.
- Do not transform regional troops into a single centrally ruled 100,000-man Summer Isles army. Local principality political relations and coastal threat conditions constrain musters.
- Goldenheart resource distribution must respect Jhala/Omboru plus export ban. Fleet defense without unlimited goldenheart bow stockpiles.

### 4.4 Agriculture, ecology and food security

**Canonical products/baselines:** Fruit and fish feature in the diet; wine, tropical spices (including cinnamon, nutmeg and pepper), timber/hardwoods, feathers and gemstones/pearls appear as economic products; imported metal resources are significant. [S1] **Canon does not provide town-by-town soil chemistry, exact hectare allocation, harvest calendars, crop varieties or livestock headcount.**

| Ecological site | Likely capacity model | Agricultural plan (ALL provisional) | Exclusions and risks |
|---|---|---|---|
| Walano sheltered lowlands | Moderate–high if freshwater and slopes verified | Orchards, tropical gardens, root crops/vegetables and small stock | Sacred grove, salt flats, forest conservation, floods |
| Walano steep rainforest | Low–moderate | Small clearings, forest garden products | Soil erosion, flooding, no indiscriminate slash-and-burn |
| Koj | Low–moderate farmland, strong craft/port economy | Market gardens and imported foods | No broad crop carpet across tiny shipbuilding island |
| Omboru forest and valleys | High variation | Fruit, small valley farms, fishing | Protect goldenheart woodlots and watershed |
| Sweet Lotus Vale | Potential higher river-valley suitability | Orchard/spice/garden mixes with irrigation as validated | Flood plains and salinity at Ebonhead mouth |
| Jhala interior mountains/jungle | Generally low for intensive flat farming | Patch farms/managed forest harvest | Mountain and goldenheart habitat; wildlife; access costs |
| Tiny offshore islets | Often little/no agriculture | Fishing and perhaps seasonal gardens | Small area, absent freshwater, storms |

- Spices are not substitutes for staples. Foods must be produced, gathered, fished or imported, with appropriate food inventories and consumption.
- Each growable crop has a land, water, season and labor requirement. Assign it to **real farms/holdings**, not unrestricted region-wide production.
- Woods for ordinary carpentry and regulated goldenheart conservation are separate inventory resources.
- Port food resilience depends on actual routes and merchant vessels, not global instant stock transfer. Maritime storms/piracy can disrupt resupply.

### 4.5 Trade routes and economic network

- Internal coastal links: Last Lament ↔ Lotus Point ↔ Tall Trees Town (Walano), Walano ↔ Koj, Walano ↔ Omboru across the Smiling Sea, Omboru ↔ Jhala across the Indigo Straits, Ebonhead ↔ inland Sweet Lotus Vale farms.
- Overseas routes may connect to Oldtown, King's Landing, the Free Cities, Qarth and elsewhere: exact assignments per ship are scenario gameplay outcomes. [S1]
- Imports: Iron, tin and metals, perhaps specific food deficits if island production fails; quantities unknown. Exports: legitimate hardwoods (excluding forbidden goldenheart), wine, spices, feathered goods, gemstone/pearls, maritime shipping services; harvest and goods must have owners.
- **Koj's shipyards** should be supply-chain integrated even though no city X was marked. Timber from forests, sailors from households and metal imports must flow to ship production.

## 5. Repo audit and targeted integration plan for Step 2.2

**Verified relevant locations/files (existing, not newly created):**

1. `src/main/resources/data/knownworld/reference/locations.json` already contains `summer_isles`, `walano`, `koj`, `omboru`, `jhala`, `sweet_lotus_vale`, `red_flower_vale`, `isle_of_women`, and smaller islands. **But the four map-marked towns are not currently declared there as ordinary named location entries.**
2. `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/SummerIslesFeatureRegistry.java` contains geographic feature `lotus_port`, but **does not contain** Last Lament, Tall Trees Town and Ebonhead. Its `lotus_port` identifier must be reconciled with canonical **Lotus Point** without breaking persisted/legacy IDs.
3. `src/main/resources/data/knownworld/reference/subregion_masks.json` has an `SI` mask: **2952×1714 px**, covering geographic areas such as Walano, Koj, Omboru, Jhala, Sweet Lotus Vale, Red Flower Vale, Xon etc. The color codes are classification keys, not occupant ethnic percentages, and must not be overwritten by marker annotations. `#ED1C24` is the user's **X annotation** color in source-map authoring, a different purpose.
4. `src/main/resources/data/knownworld/reference/landmass_zones.json` and `.../territory_zones.json` provide the broad `summer_isles` envelope/polygon. Island and vale refinements require actual subregion resolution; don't assert they are fully independent per-island political polygons.
5. `src/main/resources/data/knownworld/reference/cultures.json` contains `summer_islander`.
6. `src/main/resources/data/knownworld/reference/languages.json` contains `summer_tongue` and `trade_talk`.
7. `src/main/resources/data/knownworld/reference/religions.json` contains **no** specific Summer Isles faith umbrella. Add one explicitly in Step 2.2 with research provenance.
8. `src/main/resources/data/knownworld/reference/regional_influences.json` has baseline `summer_isles` cultural weight 0.95, language 0.95, plus values/social norms; `regional_influence_extensions.json` adds `trade_talk` 0.24. **These are weights, not demographic percentages**. Review cultural/freedom values against researched facts; implement smaller island/port/sacred-site overrides.
9. `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/wider_world_summer_isles.json`: `house_qo`, `house_xaq`, `house_xho`.
10. `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`: `summer_isles_domain`, `koj_domain` (house_xaq), `red_flower_vale_domain` (house_xho), `sweet_lotus_vale_domain` (house_qo). **The current owner fields of Red Flower Vale contradict using Jalabhar Xho as a present in-place ruler**; preserve him as claimant/exile. Historical House Qo ownership may be an intended mod assumption, not a source-confirmed incumbent.
11. `src/main/resources/data/knownworld/scenarios/agot_298_ac/settlements.json` remains effectively sandbox-sized and cannot be taken as the number of canonical Summer Isles settlements.

**Implementation contract for Step 2.2** (not carried out here):

| Requirement | Authoritative intended owner / integration |
|---|---|
| Source X, alias, canonical name, landmark status | Geographic feature/location registry + eventual unified place catalog; source observation retained alongside adjusted build position |
| Island/vale geographic influence | Existing raster subregion / reference resolver, with refined island geography and provenance |
| Scenario occupation, ruler and claims | Existing holding/dynasty/title/office systems; exiled person physically at King's Landing, not duplicated |
| Permanent population | Unified persistent individual identity store; initial counts only generate the set of real residents |
| Families, education, birthplace and citizenship | NPC identity, character profile, family/genealogy and affiliation managers using actual place/region IDs |
| Jobs and operating firms/farms | Existing economic inventory, labor, farm/holding subsystems; **no anonymous labor cohorts** |
| Agriculture, food, fish and wood | Land and water opportunity layers + named worker/fisher/resource-owner entities; conservation laws |
| Market and swan-ship traffic | Persistent vessel/crew/shipping organizations, routes, port warehouses and goods inventories |
| Military and ritual conflict | Citizen recruitment, guard affiliations, ships and army organization; no double counts or fictional central king |
| Religion and civic institutions | Faith definitions, real priestess/priests, Talking Trees authority, ritual laws, local variation |
| Site construction | Regional cultural template library, terrain fitting, unique Tall Trees Town monument, Koj Pearl Palace when later mapped |
| Daily/yearly simulation and persistence | Population, household, trade, agricultural, political and physical projections all read same live entities and share deterministic saves |

### Suggested geography/political audits before world generation

- **Alias test:** `lotus_port` and `lotus_point` resolve to **one** actual site; source map label preserved, no duplicated village.
- **No fake historical ruler:** Xanda/Chatana Qo and Malthar Xaq appear in history, not alive in 298 AC. Jalabhar Xho appears once at King's Landing, with disputed Red Flower Vale title and kinship.
- **No fictional global island monarchy:** separate local princes/households and island/vale jurisdictions.
- **Koj without a red X:** can be geographically referenced and simulated as a principality; no fabricated user-marked point.
- **Tall Trees protected:** town planned around designated unique trees, immutable cultural landmark and carving institutions.
- **No slave estates:** island customs prohibit slavery, despite foreign slavers and the historic Slavers' Wars.
- **Goldenheart controlled:** gather only from suitable Jhala/Omboru stock; no open exports and no supply by anonymous logging.
- **Isle of Women:** descendants of Rhoynish refugees retain a local heritage influence; religion/language individually varied.
- **Geographic check:** ports on navigable land coast; wetlands and crop soils surveyed; no city anchored in water after final terrain correction.
- **Headcount reconciliation:** sum of local residents across all spawned/simulated households equals declared actual active individuals; garrisons, ships and farm workers count as subsets, temporary visitors do not become extra citizens.
- **Village generation:** harbor towns and hinterland farm villages can generate worldwide, but each building's eventual inhabitants and workplaces must map to persistent identities. Empty houses may remain empty and should not generate fake human activity.

## 6. Open questions retained for later validation

- Local names and 298 AC sovereigns for the four settlements: **unknown**.
- Whether House Qo still holds Sweet Lotus Vale, and who **currently controls** Red Flower Vale after Jalabhar Xho's exile.
- How to choose a consistent worldwide 298 AC island population calibration and working-age fraction; no published town figures.
- Exact named god/goddess of love/fertility and individual local faith distribution: **not canonically supplied**.
- Agricultural species and yields per port/valley; rain patterns, soil distribution, existing reefs/mangroves and fresh water.
- True positions, boundaries and sizes of Walano's coastal ports and Tall Trees sacred groves.
- Koj shipyard city/port and Pearl Palace physical site not among the four red Xs; future separate placement should not masquerade as a marked annotation.
- The names, genders, succession histories, claims and allegiances of the dozens of otherwise unnamed local princes.
- Distinct children/young adults per locality, household sizes, ship crew ratios, temple staff and military commitments.
- The remaining user map-marker and legend/leader-line uncertainties: **deliberately deferred** to the single consolidated review after all submaps.

## 7. Sources and provenance ledger

These links are research references to A Wiki of Ice and Fire (AWOIAF), which in turn cites the specified original books, *The World of Ice & Fire*, and *The Lands of Ice and Fire*. AWOIAF is a secondary compilation, not a source of accurate quantitative census data.

- **[S1]** [Summer Isles](https://awoiaf.westeros.org/index.php/Summer_Isles) — geography, demographics qualitative, island politics, religions, military, economy, trade, slavery prohibition, historical figures; cites *The World of Ice & Fire*.
- **[S2]** [Last Lament](https://awoiaf.westeros.org/index.php/Last_Lament) — named NW Walano town; map from *The Lands of Ice and Fire*.
- **[S3]** [Lotus Point](https://awoiaf.westeros.org/index.php/Lotus_Point) — explicitly notes "Lotus Port" is a map labeling error; southwest Walano city.
- **[S4]** [Tall Trees Town](https://awoiaf.westeros.org/index.php/Tall_Trees_Town) — location, Talking Trees, sack in 19 AC.
- **[S5]** [Jhala](https://awoiaf.westeros.org/index.php/Jhala) — area/population contrast, Sweet Lotus and Red Flower vales, Ebonhead, goldenheart forests.
- **[S6]** [House Qo](https://awoiaf.westeros.org/index.php/House_Qo) — Xanda and Chatana are historical princesses; **no named current known member**.
- **[S7]** [House Xho](https://awoiaf.westeros.org/index.php/House_Xho) and [Jalabhar Xho](https://awoiaf.westeros.org/index.php/Jalabhar_Xho) — exiled claimant at Robert's court, Red Flower Vale; do not appoint him at home.
- **[S8]** [Koj](https://awoiaf.westeros.org/index.php/Koj) — shipyards, Pearl Palace and historic Xaq line.
- **[S9]** [Talking Trees](https://awoiaf.westeros.org/index.php/Talking_Trees) — sacred carved laws, histories, songs and priestesses.
- **[S10]** [Religion](https://awoiaf.westeros.org/index.php/Religion) — many gods of the islands, favored fertility/love pair.
- **[S11]** [Goldenheart](https://awoiaf.westeros.org/index.php/Goldenheart) — wood restricted to Jhala and Omboru, export forbidden.
- **[S12]** [Isle of Women](https://awoiaf.westeros.org/index.php/Isle_of_Women) — Rhoynish descendants living there in modern period.
- **[S13]** [Gender and sexuality](https://awoiaf.westeros.org/index.php/Sexuality_and_Gender) — equal social status/roles for women and men, sacred cultural customs.
- **[S14]** [Walano](https://awoiaf.westeros.org/index.php/Walano) — relative position of northern/southern ports and islands.
- **[S15]** [Ebonhead](https://awoiaf.westeros.org/index.php/Ebonhead) and [Sweet Lotus Vale](https://awoiaf.westeros.org/index.php/Sweet_Lotus_Vale) — Jhala city and valley location.
- **[S16]** [Swan ship](https://awoiaf.westeros.org/index.php/Swan_ship) — nature of defended merchant vessels; faster sailing but vulnerable when becalmed.
- **[S17]** [Summer Tongue](https://awoiaf.westeros.org/index.php/Summer_Tongue) — local language.

**Repository basis** (checked 2026-10-17):

- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/SummerIslesFeatureRegistry.java`
- `src/main/resources/data/knownworld/reference/locations.json`
- `src/main/resources/data/knownworld/reference/locations_holdings.json`
- `src/main/resources/data/knownworld/reference/landmass_zones.json`
- `src/main/resources/data/knownworld/reference/territory_zones.json`
- `src/main/resources/data/knownworld/reference/subregion_masks.json`
- `src/main/resources/data/knownworld/reference/cultures.json`
- `src/main/resources/data/knownworld/reference/languages.json`
- `src/main/resources/data/knownworld/reference/religions.json`
- `src/main/resources/data/knownworld/reference/regional_influences.json`
- `src/main/resources/data/knownworld/reference/regional_influence_extensions.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/wider_world_summer_isles.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`

**End of Step 2.1 SI dossier.** Everything labeled MOD ESTIMATE / SOURCE-INFERRED is provisional scenario-design input. Future Step 2.2 must integrate it with the existing model's single person, household, holding, political office, economy and military authority structures rather than creating a second simulation.
