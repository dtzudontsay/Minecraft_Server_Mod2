# Known World geographic research — Step 2.1 / SE (Southern Essos), 298 AC

**Status:** Research and scenario-design draft; **NOT** an implementation-ready world-generation catalog.  
**Scope:** 31 numbered X/name observations from the user's `se.png` regional authoring map; southern Dothraki Sea, Slaver's Bay and Ghiscar, Lhazar, the Red Waste, Qarth, the Bone Mountains, the Jade Gates and Moraq, with two Sothoryos cross-map observations.  
**Scenario:** A Song of Ice and Fire **book-continuity starting year 298 AC**. Do **not** import subsequent Daenerys conquests, liberated-slave governments or wars into the initial scenario.  
**Color provenance:** red X and connecting handwriting `#ED1C24`.  
**Spatial provenance:** marker IDs `SE-001` to `SE-031` follow the *provisional* visual matching established in chat. Pixel centres, false positives, duplications and terrain snapping remain pending the later combined verification pass. **Never use an unverified SE marker ID directly to spawn a structure.**

## Research and confidence contract

- **[C] Canon-supported:** the named work, official map, or A Wiki of Ice and Fire synopsis actually supports the claim. Footnotes below link to A Wiki of Ice and Fire with citations to books, *The World of Ice & Fire* or *The Lands of Ice and Fire*. Wiki synthesis is a reference, not itself a replacement for verifying the primary text.
- **[R] Existing repository:** an already-authored ID, polygon, family or holding. This is **not** automatic evidence that an entry's value is historically correct.
- **[I] Inference:** plausible extrapolation from established geography, ecology, government or trade; do not describe as canon.
- **[M] Mod estimate:** expressly invented game-generation range or percentage. These are **planning envelopes**, not known historical population statistics. They must be jointly calibrated across the known world and converted into real, individually identifiable residents when population generation is enabled.
- **[U] Unresolved:** no reliable information, ambiguity, uncertain site status or missing map detail. Keep null/unassigned rather than fabricate a canonical ruler, noble house, faith or founding date.

### Critical architectural invariants

1. Every *human* resident, worker, merchant, guard, ruler, traveler or captive must ultimately have one persistent person identity resolvable into a physically meetable NPC. Population counts in this document are **targets**, not a new population authority or anonymous substitute for actual NPCs.
2. Jobs refer to employers and workplaces tied to real holdings, inventories and people; military personnel are an identifiable subset of the population, not additional people spawned from a troop statistic.
3. Historical ruins do not have an invented permanent population or tax economy. Itinerant visitors must be real people arriving from elsewhere, not permanent hidden cohorts.
4. Laws, hierarchy, slavery and coercion are modeled as historical/social circumstances of individual agents; avoid globally assuming that all places have a Westerosi feudal lord or ordinary noble house.
5. “Possible crops,” livestock, soils and resource availability are **suitability**. Actual output belongs to farmland/workplaces and assigned identifiable workers, constrained by weather, inputs and inventories.
6. Distinguish **stationed garrison**, **trained reserve**, **field army**, **mercenaries**, **fleet** and **visiting khalasars**. Never double-count the same soldiers.
7. The 298 AC baseline does not start with Daenerys as ruler of Meereen, Yunkai or Astapor. Later narrative events must arise dynamically or be added as authored scenario events, not silently pre-applied.

## 1. All 31 SE observations — status and numeric planning ranges

**Population:** permanent residents in the defined site plus its immediate contiguous built-up settlement; excludes distant villages, transient caravans and mobile khalasars. **Defenders:** initial locally stationed/organized land force *within the same population*; not total military levy or navy. Units marked `—` have **no attested active municipal garrison**. All numeric ranges `[M]`; no canon census was found. Some settlements have unknown site-level government despite known regional authority.

| Map ID | Site / accepted alias | Status in 298 AC | Permanent people [M] | Stationed defenders [M] | Authority / association (evidentiary status) |
|---|---|---|---:|---:|---|
| SE-001 | Vaes Efe | Destroyed Ghiscari city | 0 established | — | Former Ghiscari, no surviving civic rule [C] |
| SE-002 | Vaes Diaf / Hazdahn Mo (map **Hazdahn No**) | Ruined Ghiscari city | 0 established | — | Former Ghiscari [C] |
| SE-003 | Vaes Jini / Yinishar | Ruined settlement | 0 established | — | Historical cultural connections with Samyriana [C] |
| SE-004 | **Vaes Mejhah** (map read “Meihah”) | Ruined Ghiscari city | 0 established | — | Former Ghiscari [C] |
| SE-005 | **Krazaaj Has** / Ghardaq (map sometimes Ghazdaq) | Ruined Ghiscari city | 0 established | — | Former Ghiscari [C] |
| SE-006 | Adakhakileki | Ruins, origin poorly known | 0 established | — | Unknown original ruling authority [U] |
| SE-007 | Samyriana / Shamyriana | Active Hyrkooni mountain fortress | 8,000–28,000 | 500–2,000 | Great Fathers + warrior women [C] |
| SE-008 | Meereen | Active major Ghiscari city | 180,000–360,000 | 6,000–17,000 | Great Masters; several named elite families [C] |
| SE-009 | Kosrak | Active Lhazareen city | 5,000–16,000 | 120–500 | Local Lhazareen leadership unknown [U] |
| SE-010 | Hesh | Active Lhazareen city | 5,000–17,000 | 120–550 | Local Lhazareen leadership unknown [U] |
| SE-011 | Yunkai | Active Ghiscari city | 95,000–190,000 | 4,000–9,000 | Wise Masters; named elite families [C] |
| SE-012 | Lhazosh | Active Lhazareen city | 8,000–24,000 | 180–650 | Local Lhazareen leadership unknown [U] |
| SE-013 | Bayasabhad | Active Hyrkooni fortress city | 7,000–23,000 | 400–1,800 | Great Fathers + warrior women [C] |
| SE-014 | Vaes Tolorro | Abandoned city of unclear origin | 0 established | — | Historical polity unknown [U] |
| SE-015 | Astapor | Active Ghiscari slave city | 65,000–140,000 | 1,000–5,000 stationed; military stock separate | Good Masters; named elite families [C] |
| SE-016 | Vaes Shirosi | Ruined Qaathi town | 0 established | — | Former Qaathi [C] |
| SE-017 | Vaes Qosar / Qolahn | Ruined Qaathi city | 0 established | — | Former Qaathi [C] |
| SE-018 | Vaes Orvik | Ruined Qaathi city | 0 established | — | Former Qaathi [C] |
| SE-019 | Qarkash | Active Qartheen town | 2,000–9,000 | 80–350 | Under Qartheen influence [C] |
| SE-020 | Asabhad | Active western Yi Ti/Jade Sea gateway | 8,000–30,000 | 350–1,400 | Local governor/sovereignty not securely identified [U] |
| SE-021 | Port Yhos | Active Qartheen-controlled port | 4,000–15,000 | 150–700 | Qarth [C] |
| SE-022 | Qarth | Active major city-state | 130,000–300,000 | 3,000–11,000 land; fleet separately | Pureborn, Civic Guard; competing merchant factions [C] |
| SE-023 | **Qal island and its two harbors** | Active island with Qartheen defenses | 2,000–9,000 *combined sites* | 200–900 *combined* | Qarth's fortified harbors [C]; **not one conventional town** |
| SE-024 | Old Ghis | Ancient devastated ruins | 0 established | — | Former Old Empire of Ghis [C] |
| SE-025 | “East Qal” | **Annotation / harbor-side lead; not independently verified third town** | **No independent population** | — | Resolve as eastern Qal harbor / label in later map review [U] |
| SE-026 | New Ghis | Active Ghiscari island city | 40,000–110,000 | 4,000–12,000 | Ghiscari administration and free iron legions [C] |
| SE-027 | Faros | Active city on Great Moraq | 5,000–18,000 | 150–700 | Local political authority not established [U] |
| SE-028 | Vahar | Active island spice-trade city | 7,000–25,000 | 200–900 | Local political authority not established [U] |
| SE-029 | Gorosh | **Ruined Sothoryos colony** | 0 established | — | Former Ghiscari penal colony [C]; **cross-map duplicate of SO** |
| SE-030 | Port Moraq | Active southern Great Moraq port | 6,000–22,000 | 180–850 | Local political authority not established [U] |
| SE-031 | Zamettar | **Ruined Sothoryos city** | 0 established | — | Former Ghiscari colony [C]; **cross-map duplicate of SO** |

**Balance constraint for eventual generation:** Meereen is described as roughly the size of Astapor and Yunkai combined. The ranges above admit different combinations; choose **joint** scenario values consistent with that relationship rather than independently sampling three random numbers. Actual military strengths are **not canonical figures**, and Yunkai's approximately 5,000-person field army reported in the source is not automatically the count of its permanent city garrison. [S1] [S2] [S3]

## 2. Occupation and class distribution baselines

These sector weights are **[M] provisional job-role targets for civilian-aged individuals**, not percentages of total including children, elderly and nonworkers; not every person is employed; no implied employment outside the individual population registry. Rows add to 100% across five **broad economic sectors**. Security includes identified professional guards and garrisons, not every possible militia member; the military ranges above are a separate cross-check, **not additive workforce**. Estate and captive labor are classified by **work actually performed**, while legal status/rights are orthogonal attributes.

| Site archetype | Food/animal/fishing | Craft/extraction/building | Trade/transport | Services/households/civic | Security | Economic/labor structure [C or I] |
|---|---:|---:|---:|---:|---:|---|
| Meereen, Yunkai, Astapor: slaver-city core | 8% | 25% | 23% | 34% | 10% | Strong coercive ownership, household/domestic service, training institutions, port commerce; many rural farms **outside** core counted separately [C/I] |
| New Ghis: military-industrial port | 12% | 26% | 20% | 25% | 17% | Disciplined *free* iron legion institutions rather than importing the Astapori Unsullied institution [C/I] |
| Qarth: great east-west port | 6% | 19% | 39% | 28% | 8% | Merchants/guilds, port authorities, civic guard, shipping; rural provisioning outside city [C/I] |
| Lhazareen city (Hesh/Kosrak/Lhazosh) | 35% | 16% | 19% | 25% | 5% | Shepherds and crop farmers important; animals and food stores extend into countryside [C/I] |
| Hyrkooni mountain fortress (Samyriana/Bayasabhad) | 14% | 25% | 22% | 20% | 19% | Sex-specific customary role structures and caravan-road defense; model individuals rather than abstract “female army” [C/I] |
| Qartheen coastal town (Qarkash/Port Yhos) | 27% | 19% | 25% | 23% | 6% | Fisherfolk, porters, food suppliers, caravan/sea trade [I] |
| Asabhad: Jade Sea caravan gateway | 18% | 19% | 30% | 24% | 9% | Trade between Qarth and Yi Ti; forested river mouth [C/I] |
| Moraq and Vahar port towns | 27% | 19% | 32% | 17% | 5% | Trade, fishing, forest goods, island food production; religious and language mix unresolved [C/I] |

**Within each group** additionally generate age structure, household types, wealth and legal status from explicit scenario rules. Do not assume enslaved residents are a separate unreal headcount; they are individually identified people with actual relationships, work assignments, and possible freedom/migration trajectories. For the ruins, there is **no resident occupational distribution**.

## 3. Individual place dossiers — one per SE observation

For all active places: the population and defender ranges are in §1; occupation archetypes in §2. Each entry below records political, cultural/religious, food/environmental and built-environment details. “Unassigned local head” means no invented canon ruler or noble surname; future generated officials must be explicitly tagged `[M]`.

### SE-001 — Vaes Efe
- **298 AC:** [C] Ruined Ghiscari site, destroyed in the Dothraki conquests; “City of Shackles”; south of Vaes Dothrak, north of Skahazadhan [S10].
- **People/politics/faith:** No permanent population, functioning government, or attested garrison. Historically Ghiscari; do not fabricate a contemporary noble house. Unknown ancient religious practice.
- **Environment/structures:** [I] Ruined brick market and civic compounds; possible former river road and caravan approach. [M] No automatic agricultural operation; passing Dothraki parties retain home populations elsewhere.

### SE-002 — Vaes Diaf / Hazdahn Mo
- **298 AC:** [C] Ruined Ghiscari slave-market city. **Hazdahn Mo** is the supported name; “Hazdahn No” is a noted map misspelling [S11].
- **People/politics/faith:** No established residents; former Ghiscari rulers unknown. No canon current house or governor.
- **Environment/structures:** [I] Broken exchange yards, abandoned brick buildings, former caravan approaches, ruined retaining walls; no live trading market.

### SE-003 — Vaes Jini / Yinishar
- **298 AC:** [C] Ruined city on the Stone Road near the western Bone Mountains; former Yinishar, culturally related to Samyriana; “City of Goats” [S12].
- **People/politics/faith:** No active polity, population or municipal troops. Former Hyrkooni-related cultural inheritance; don't ascribe ongoing authority of Samyriana without a separate source.
- **Environment/structures:** [I] Broken pass road, stone foundations, former caravan yards; arid stony soil with viable grazing only where water is available.

### SE-004 — Vaes **Mejhah** (annotation read “Meihah”)
- **298 AC:** [C] Ruined Ghiscari settlement between the southern Dothraki Sea and Lhazar, on an eastern Skahazadhan tributary [S13].
- **People/politics/faith:** No permanent urban community; historic Ghiscari. The older source alias spelling is to be preserved as an **annotation alias**, not a second settlement.
- **Environment/structures:** [I] Ruined riverside town, irrigated terraces or abandoned farmland only where water and soil allow; no assumed active harvest.

### SE-005 — **Krazaaj Has** / Ghardaq
- **298 AC:** [C] Ruined Ghiscari city; former **Ghardaq** (also found as **Ghazdaq** in a map error). Name “Sharp Mountains” refers to former pyramids [S14].
- **People/politics/faith:** No existing elite house or garrison; historic Ghiscari context.
- **Environment/structures:** [C/I] Ruined pyramid complex and former tributary-side settlement; broken road to Meereen/Lhazar. Monument silhouette should differ from ordinary Ghiscari city walls.

### SE-006 — Adakhakileki
- **298 AC:** [C] Ruined city on the northern Poison Sea margin near the Stone Road and Bone Mountain foothills; name translated as “The Cannibals” [S15]. **Original inhabitants unknown**.
- **People/politics/faith:** No attested residents, culture, ruler, military or religious institutions; leave unknown rather than assigning all to Dothraki.
- **Environment/structures:** [I] Harsh arid foothills and abandoned stone compounds; source contains little architectural detail, so use a **generic ruined unknown-origin** archetype with explicit low confidence.

### SE-007 — Samyriana / Shamyriana
- **298 AC:** [C] Functioning fortress city built/carved into gray rock above the **Stone Road**, one of the three surviving Hyrkooni fortress cities [S16].
- **Rulers/house:** **Great Fathers**, not a Westerosi House; individual names not established. Warrior women form the distinctive military class; cultural lineage `hyrkooni` [R]. Do not randomly assign one of the Ghiscari dynasties.
- **Religion:** [U] Hyrkooni religious traditions are tied to the city's social history; no confirmed single current catalog religion. Use `unknown_religion` pending research rather than incorrectly forcing Great Stallion.
- **Jobs and food:** [I] Caravan tolls, pass guarding, masonry, smithing, irrigated plots in usable ravines, goats/sheep, stored grains. [C] Servant/scholar/farmer roles within the city's unusual gender hierarchy [S16].
- **Architecture:** **Mountainside-carved city**, terracing, defended pass, cliff-integrated buildings, stone gates, storage cisterns. Standalone 5× vanilla-village clone would be unsuitable.

### SE-008 — Meereen
- **298 AC:** [C] Biggest Ghiscari Slaver City, ruled by the **Great Masters** (the Daenerys takeover belongs later, not at starting year) [S1].
- **Houses/families:** [R/C] Already in `essos_slavers_bay.json`: Dhazak, Galare, Ghazeen, Hazkar, Kandaq, Loraq, Merreq, Naqqan, Pahl, Quazzar, Reznak, Rhazdar, Uhlez, Yherizan, Zhak. These are elite lineages **not** proof that any one family owns all Meereen. Known named rulers at the 298 AC instant need chronology-specific authoring, not a made-up single king.
- **Culture/religion:** Ghiscari-descended `meereenese` [R] city identity, with High Valyrian dialect and varied imported people. The old Ghiscari gods are not a safely specified active pantheon. Priesthood of the Graces is documented; individual faiths must not all be generated as one fictional “Ghiscari religion.”
- **Food/economy:** [C] Slave markets, fighting pits, pyramid estates, farming wells, wheat, **olives**, **grapes**, **sheep and goats**, **copper and salt** extraction in the hills [S1]. [I] Irrigation, cisterns, coastal gardens, caravan depots; not uniform high fertility across all surrounding hills.
- **Military/architecture:** [C] Massive multi-colored brick pyramid city, Great Pyramid, Temple of the Graces, fighting pits, brick wall with defensive fixtures, markets, canals/drainage; protect central monuments from procedural replacement. Maintain household and captive records as individuals.
- **Scale:** [C] Approximate relative size equal to Astapor + Yunkai combined [S2]; jointly calibrate numbers across all three, not independent samples.

### SE-009 — Kosrak
- **298 AC:** [C] Active city in eastern Lhazar on a tributary of the Skahazadhan, on routes toward Vaes Tolorro and Adakhakileki [S17].
- **Government/people:** Specific head of city unknown [U]; Lhazareen predominance is regional inference, not city-wide proven percentage. `lhazareen` culture [R]; worship of **Great Shepherd** broadly applies in Lhazar [C].
- **Production:** [C/I] Sheep/pasture, vegetables, dryland grain in watered patches, caravan supplies, wool and leather. River-dependent productivity and raiding risks.
- **Built form:** Mudbrick or stone town near stream, animal compounds, pastoral markets, small temples, guarded route crossings. No invented local dynasty.

### SE-010 — Hesh
- **298 AC:** [C] Active western Lhazar city, trade route through **Khyzai Pass** to Meereen [S18].
- **Government/people:** Named ruler and formal state unknown; predominantly Lhazareen likely [I], Great Shepherd broad regional baseline [C]. No forced Ghiscari sovereignty despite trade dependency.
- **Production:** [I] Sheep/goats, vegetable crops, canal-fed/dryland wheat or barley where rainfall supports them; wool, passage trade, pack animals.
- **Built form:** Trade-route approach, pasture edges, small streets and pens, water storage, pack-animal marketplace, defensible waystation structures.

### SE-011 — Yunkai
- **298 AC:** [C] Functioning city ruled by **Wise Masters**, associated with slave training and commercial slavery; distinctive yellow-brown bricks and stepped pyramids [S3].
- **Families:** [R] Existing Yunkai lineages Ahlaq, Eraz, Faez, Myraq, Qaggaz, Rhaezn, Yunzak, Zherzyn; don't treat all as simultaneous ruling monarchs.
- **Culture/religion:** `yunkish` [R], High Valyrian-derived Ghiscari dialect; local priesthood and merchants, `unknown_religion` for unresolved household-specific faiths rather than universal invented cult.
- **Economy/food:** [C] Expansive commerce in enslaved persons, labor-intensive estates and city crafts; [I] coastal olives, grain, sheep/goats, caravan fodder, gardens sustained by local water.
- **Military:** [C] Source describes an approximately **5,000-person army**, slave-based, and archers/slingers on defenses; this is a field-force reference, **not** definitive standing garrison [S3].
- **Architecture:** Yellow-brick pyramids and walls, broad gates, markets, rich enclosed compounds, poorer residential quarters, nearby estates.

### SE-012 — Lhazosh
- **298 AC:** [C] Active city in southern Lhazar with a road toward the Red Waste and Qarth's ancient ruins [S19].
- **Government/people:** No named leader attested; Lhazareen settlement, Great Shepherd influence. Do not anoint a fictional King of Lhazar.
- **Production:** Sheep, goats, wool, vegetables, irrigated grain where feasible; transport and caravan provisions [C/I].
- **Architecture:** Low-rise town, animal pens, shaded markets, storehouses, water points, settlement outskirts blending into pasture.

### SE-013 — Bayasabhad
- **298 AC:** [C] Active Hyrkooni city defending the **Sand Road** through southern Bone Mountains. One of three sister cities ruled by the Great Fathers [S20].
- **Leadership/culture:** `hyrkooni`, Great Fathers; rule from established Hyrkoon institutions, no known ruling family surnames. Gendered military/social roles as documented; avoid generalizing to unrelated cities.
- **Religion:** Specific active catalog mapping still unknown; do not default to Great Stallion or R'hllor.
- **Economy/food:** Caravan commerce and defenses; scarce arable ground, cisterns, terraced watering areas, goats/sheep [I].
- **Build:** Fortified desert mountain pass, monumental gate, terraced garrison quarters, hidden cisterns, caravanserai, stone military structures.

### SE-014 — Vaes Tolorro
- **298 AC:** [C] Abandoned ruins in the Red Waste. Once occupied but origin/culture unknown. Description includes abandoned alleys, palace and plaza, wells and surviving garden fruit trees [S21].
- **People:** No permanent city population; later travelers can take water/food from wells and abandoned gardens without conjuring local residents.
- **Environment/build:** Ruined white-stone or marble urban complex, dry plazas, viable deep wells, hidden figs, grapes and peaches, scorpions; **no routine active farms**.

### SE-015 — Astapor
- **298 AC:** [C] Active *Red City*, ruled by **Good Masters**, known for training **Unsullied** and its slave trade [S4]. Future uprisings and conquest are not pre-applied.
- **Families:** [R] `family_nakloz` and `family_ullhor` already registered; others should only be added where substantiated or marked generated, never invented as a canonical House.
- **Culture/religion:** `astapori` [R], historic Ghiscari-Valyrian mixture, varied enslaved and free backgrounds. Historic Ghiscari gods explicitly largely forgotten; don't force a revived canon pantheon.
- **Economy:** [C] Unsullied training and export, warehouses, trading plazas, domestic crafts, ports. [I] surrounding grain/animal estates and gardens, with dryness/water restrictions.
- **Military:** [C] Decaying city wall and under-defended towers, but **Unsullied training cohorts are valuable export inventory** and not all automatically a free city garrison [S4]. Track every soldier/trainee as an identifiable person and their legal/organizational ties.
- **Build:** Distinct red-brick pyramids, Plaza of Pride/Punishment, crumbling harbor harpy, training compounds, fighting pits, canals/Worm River, warehouses.

### SE-016 — Vaes Shirosi
- **298 AC:** [C] Ruined Qaathi town destroyed by the Dothraki, “City of Scorpions” [S22].
- **People/government:** None established; historic Qaathi. No active Qartheen tax/political structure for ruins by default.
- **Build:** Sparse urban ruins in Red Waste, abandoned trading posts, dune-buried roadside structures; no full-time local farmers.

### SE-017 — Vaes Qosar / Qolahn
- **298 AC:** [C] Ruined Qaathi city northwest of Qarth, “City of Spiders”; lies along caravan routes [S23].
- **People/government:** No permanent city population and no local garrison. Historic Qaathi association.
- **Build:** Desert-stone ruins, former caravan streets, partially buried gateways; do not silently spawn active merchants there.

### SE-018 — Vaes Orvik
- **298 AC:** [C] Ruined Qaathi settlement near Qarkash/Port Yhos, “City of the Whip” from the taking of captives [S24].
- **People/government:** No active city population; no functioning local lord or market.
- **Build:** Ruined roads and town blocks with heat-scoured masonry; historic civic identity Qaathi.

### SE-019 — Qarkash
- **298 AC:** [C] Active Qartheen town on Summer Sea coast west of Qarth, east of Port Yhos [S25].
- **Leadership:** Qarth-associated settlement; named administrator unknown, should hold an appointed/municipal office not hereditary invented noble House.
- **Culture/religion:** Qartheen coastal mix probable; avoid assumed homogeneous religion.
- **Food/build:** Fish and seafood, gardens or dryland farms near water, port trade and camel provisioning [I]. Smaller walled coastal town, harbor, shore warehouses and caravan station.

### SE-020 — Asabhad
- **298 AC:** [C] Active forested-river-mouth town/city on Jade Sea, western boundary of Yi Ti, caravan links with Qarth and Yin [S26].
- **Leadership/culture:** Specific ruling person and jurisdiction **not confirmed**; potentially Yi Ti-linked / mixed caravan influences [I], but don't assume an invented Yi Tish governor as canon.
- **Religion:** [U] mixed, use existing `yi_tish` cultural influence where appropriate and unknown faith if unresolved; trade visitors retain their personal backgrounds.
- **Economy/build:** Trade port, freshwater river access, forest products, rice or wet-field crops only where local wet climate and land suitability supports them [I]. River wharves, woods, caravanserais, guarded market district.

### SE-021 — Port Yhos
- **298 AC:** [C] Active town and westernmost settlement controlled by Qarth, south of Vaes Orvik, west of Qarkash [S27].
- **Leadership/culture:** Qarth's dependent port; local officials unknown, no fabricated noble dynasty. Merchants, fishers and caravanners [I].
- **Economy/build:** Port cargo handling, seafood, travel stores, ships and coastal trading road; dry coastal stonework and compact waterfront.

### SE-022 — Qarth
- **298 AC:** [C] Major trading city at the Jade Gates. **Pureborn** rule formally; **Thirteen**, **Tourmaline Brotherhood**, **Ancient Guild of Spicers** contest influence; Civic Guard and fleet serve civic institutions [S5].
- **Families:** [R] `family_deeth`, `family_emeros`, `family_mallarawan`, and qualified `family_daxos` in Qarth dynasty data. The latter is noted in repository as plausible family grouping rather than automatically a noble House. Xaro Xhoan Daxos is an important merchant prince, **not** the single city's hereditary ruler.
- **Culture/religion:** `qartheen` [R] with visiting global traders; warlocks/House of the Undying are a special organization, **not a universal civic faith**. Diverse shrines and merchants plausible; unresolved people use individualized faith.
- **Production:** Overseas trading guilds, spices, silk, luxury objects, port warehousing, shipbuilding, animal trains/camels, island harbor levies [C/I]. Gardens where water exists; surrounding Red Waste cannot be converted into blanket fertile fields.
- **Architecture:** **Three massive city walls**, celebrated gates, wealthier districts, palaces, Hall of a Thousand Thrones, Temple of Memory, House of the Undying, separate merchant quays, great port. **Major-city custom archetype**, not repeated 10× vanilla villages.
- **Military:** Civic Guard, camelry, **fleet** and overseas merchant shipping must be tracked separately; guild fleets aren't automatically government ships.

### SE-023 — Qal island (western and eastern fortified harbors)
- **298 AC:** [C] **Island**, not a single city; Qarth controls fortified harbor cities on **both sides** of Qal at the Jade Gates [S5] [S28].
- **People/politics:** [M] combined population/defenders in §1 are one provisional envelope to be divided between **two distinct harbor-site IDs** later, contingent on marker review. Both owe authority to Qarth; do not create a fictitious “King of Qal”.
- **Economy/build:** Channel tolls, pilots, fishing, navies, ship repair, dock fortifications, watchtowers; very little presumed arable land. Keep island geography and port orientation in final layout.

### SE-024 — Old Ghis
- **298 AC:** [C] Destroyed ancient capital of Old Empire of Ghis, burned by Valyrian dragons; land sowed with salt and sulfur [S7].
- **People/politics:** **No active Old Ghiscari monarchy, garrison or independent tax base.** Historical prestige and present ruins are different concepts.
- **Economy/build:** Broken brick towers, pyramids, monumental walls, archaeological landscape, inhospitable soil. Generate ruined terrain and occasional identifiable visitors, not normal fertile satellite villages near the center.

### SE-025 — East Qal (unresolved observation)
- **Map semantics:** [U] Existing cross-name list identifies **East Qal** near the island/harbor channel. Literature confirms Qal's **two** opposite fortified harbor settlements; it does **not** substantiate a third independent city called “East Qal” [S5].
- **Provisional handling:** Alias/observation for Qal's eastern harbor. **Do not spawn** a separate village or assign separate population until the later combined marker pass resolves its exact X and relation to SE-023.

### SE-026 — New Ghis
- **298 AC:** [C] Active Ghiscari city founded after Old Ghis, separated from mainland by Ghiscari Strait and known for disciplined **free iron legions** [S8].
- **Government/people:** Contemporary exact ruler/council and names not securely resolved; treat as civic elite + military command to be researched. Culture `new_ghiscari` [R], not necessarily identical to Meereen/Yunkai elites.
- **Economy:** Marine trade, fishing, metalcraft, disciplined military establishment, provisioning and agriculture where usable [C/I].
- **Build:** Newer organized island urban plan, walls, ship facilities and legion barracks. Do not replace the existing ancient ruined Old Ghis site with New Ghis.

### SE-027 — Faros
- **298 AC:** [C] Active city at river mouth on northwestern side of **Great Moraq** [S29].
- **Government/people:** Unknown named ruler and exact cultural/religious mix. Historic Yi Ti reach in Moraq does **not** prove present Yi Tish sovereignty. Leave location-specific authority open.
- **Economy:** River and coast fishing, wet-forest products, maritime exchange; fertile river-flat gardens, fruit and rice suitability to be checked by actual biome/water [I].
- **Build:** Port-and-river city on drier high bank, mixed warehouse and waterfront settlements, rainy-season drainage, wooden/stone architecture appropriate to humid coast.

### SE-028 — Vahar
- **298 AC:** [C] Active city on island of same name between Great Moraq and Lesser Moraq; wealthy through **spice trade** [S30].
- **Leadership/culture:** Specific ruler, house, official religion **unknown**. Diverse seafaring influences are plausible; don't make it automatically Qartheen or Yi Tish territory without source.
- **Food/economy:** Spice commerce, maritime carriers, fish, tropical island cultivation suitable to land and rain [C/I].
- **Build:** Harbor terraces, spice bazaars, dry storage, docks, coastal watchposts; notable wealth without automatically gigantic population.

### SE-029 — Gorosh
- **298 AC:** [C] Ruined penal colony established by Old Ghis at **Wyvern Point, Sothoryos**, not an active Ghiscari overseas garrison [S7] [S31].
- **People/land:** No regular permanent city population. Wyvern-danger/jungle hazards, dense overgrowth. All future visitors individually simulated.
- **Mapping:** **Duplicate observation with SO map**. One eventual `gorosh` location identity and authoritative source coordinates, later.

### SE-030 — Port Moraq
- **298 AC:** [C] Active port on south coast of Great Moraq at Cinnamon Straits, near rainforest to north; frequented by east-west traders [S32].
- **People/government:** Local head not identified; no fabricated noble dynasty or “King of Moraq” [U].
- **Food/economy:** Portage, imported luxury goods, fishing, jungle goods, coastal vegetable/fruit cropping under real terrain conditions [I].
- **Build:** Warm humid docks, warehouses, crew housing, ropeworks and harbor security with drainage and rainforest transition.

### SE-031 — Zamettar
- **298 AC:** [C] Ruined Ghiscari-founded colony on **Sothoryos** at mouth of Zamoyos River; historically occupied/abandoned again, not a functioning port city in 298 AC [S7] [S31].
- **Environment/build:** Collapsed river port, sprawling wet ruins, disease ecology, jungle encroachment, former canal/walled quarters. No automatic settlement economy.
- **Mapping:** **Duplicate observation with SO map**; defer coordinate choice and alias treatment.

## 4. Regional zone dossiers: ecology, people, sovereignty and settlement generation

These are zone-level *default influences* for rural communities **not specifically on the map**. They do **not** overrule an authored city dossier, and they never create a separate anonymous population. A region can overlap a trade route, hydrologic basin, biome and jurisdiction; treat these as separate dimensions rather than mutually exclusive polygons.

| Zone / reference ID | Land/climate/fertility [C/I] | Dominant cultural/religious tendencies [C/I/U] | Economy and rural settlement policy [M/I] | Authority / territorial caution |
|---|---|---|---|---|
| Southern Dothraki Sea — `dothraki_sea_region` [R] | Grasslands, water-dependent grazing; parts transition to ruined Ghiscari sites | `dothraki`, `great_stallion` for mobile groups; ethnic mix at frontiers | **Mobile camps, herds and caravan corridors**; never blanket-spawn wheat villages | Khalasars are traveling groups of real NPCs, not fixed city populations |
| Ghiscari coastal cities — `ghiscar` + `slavers_bay_region` [R] | Irrigated coastal estates, olive/grain conditions; saline/dry hills inland | Astapori/Yunkish/Meereenese city variation; faith diverse or uncertain | Coastal orchards, wheat/olives/grapes where supported, copper/salt in hills, market satellites | Distinct Good/Wise/Great Master administrations; not one automatic Slaver's Bay kingdom |
| Skahazadhan + Khyzai Pass | River corridor and mountain passage; differentiated irrigated vs dry land | Ghiscari/Lhazareen/Dothraki contact | Farms on river flats, toll posts, caravan stations, guarded bridges | Raids and trade cross political boundaries; no arbitrary regional sovereign |
| Lhazar — `lhazar` [R] | Semi-arid rolling plains, grazable hills, tributary agriculture | **Lhazareen**, **Great Shepherd** [C] | Sheep, wool, villages near water and tracks, trade towns, defensive granaries | Local leaders/structures poorly named, do not invent a canonical dynasty |
| Painted Mountains & southern Ghiscari foothills | Variable highland/valley vegetation, stone, copper, salt potential | Ghiscari frontier mixed, tribal/route visitors | Terraces/grazing/mining where appropriate; passes and small hamlets, not uniform density | Use topography for environmental modifiers |
| Stone Road / western Bones | Arid rocky passes; strong settlement chokepoints | `hyrkooni` around Samyriana; historical ruined Yinishar | Fortress city, passes, caravans, cisterns, grazing pockets | Great Fathers distinct from Ghiscari Masters |
| Sand Road / Dry Bones / eastern Bones | Hot dry passes, limited local water | Hyrkooni at Bayasabhad; traders from Qarth/Yi Ti | Fortified gates, wells, caravan stopping points, sparse highland villages | Don't confuse historic Patrimony with extant centralized nation |
| Red Waste — `red_waste_region` [R] | Severe aridity, dangerous water scarcity, isolated wells | Ruined ancient Qaathi; current transients retain origin culture | **Extremely sparse permanent settlements**, caravan and water points, ruin sites | No automatic modern Qaathi crown or functional farms at Vaes Tolorro |
| Qarth coastal jurisdiction — `qarth_region` [R] | Arid coast with managed water access; islands and port corridors | `qartheen` near city, cosmopolitan in port | Port Yhos, Qarkash and satellite fishing/trade hamlets; no arbitrary infinite crop fertility | Pureborn/civic and guild influence; ports may be dependencies |
| Qal / Jade Gates maritime zone | Rocky channel island and trade strait | Qartheen-administered, diverse sailors | Two fortified harbor sites, pilots, tolls, coast guards; no mass rural inland village chain | One island, two fortifications, one geographic feature; don't double-count SE-025 |
| Ghiscar and Ghiscari Strait | Ruined Old Ghis, salt/sulfur in historic core; islands and marine trade | `new_ghiscari` around New Ghis; unknown at ruins | Fishery/island farms, legion towns, avoid ruined soils around Old Ghis | New Ghis is distinct from ancient Old Ghis |
| Moraq Islands — `moraq_region` [R] | Great Moraq north plains/hills/small forests; south wet rainforest | Contemporary religious/cultural mix **unknown**; historical Yi Ti reach | River ports, fishers, jungle-edge villages, timber/spices, occasional farmland where viable | Unknown local political ownership; do not assert current Yi Tish rule |
| Sothoryos northern outposts — `north_sothoryos` [R] | Jungle, dangerous disease conditions, old ruined Ghiscari ports | Historical colonial Ghiscari; contemporary people highly uncertain | Ruins/exploration, almost no automatically generated permanent colony without research | One Gorosh and one Zamettar despite two maps |

### Proposed numerical environmental modifiers [M] (not canon)

Use **relative suitability** as initial weights to be calibrated against world terrain and the existing `HoldingAgricultureCatalog`, **not fixed yields**. Illustrative baseline from 0 to 100: Skahazadhan irrigated flats 70–85; well-watered Lhazar valleys 50–70; Lhazar uplands 25–45; Ghiscari inland hill terraces 35–60; Dothraki steppe grazing 65–85 but settled crop suitability 20–40; Red Waste without irrigation 0–10; Qarth urban fringe with reliable water 30–55; Bone mountain pass 0–25; Moraq coastal/river fertile pockets 60–80, rainforest clearing conditional and ecologically constrained; Old Ghis ruined salinated core 0–5. **These are investigation starting points**, not values to write blindly into runtime catalogs. Soil fertility ≠ rainfall ≠ water access ≠ cultivated area.

### Missing or under-resolved spatial influence polygons

`territory_zones.json` currently has polygons for `dothraki_sea_region`, `ghiscar`, `lhazar`, `red_waste_region`, `qarth_region`, `moraq_region` [R], but named subregions such as rivers, passes, coast strips and island harbors may need separate shape/line layers. A polygon isn't a political title; ownership needs independently authored polity/holding relations. Crosses are **points**, not precise cadastral footprints.

## 5. Repository integration audit (Step 2.2 backlog; no files changed here)

Verified relevant source files:

- `src/main/resources/data/knownworld/reference/locations.json`: references for `meereen`, `yunkai`, `astapor`, `qarth`, `lhazar`, `ghiscar`, `bone_mountains`, `red_waste_region`, `qarth_region`, `moraq_region`, `great_moraq`; most smaller marker sites require new canonical-location references or alignment to `locations_holdings.json`.
- `src/main/resources/data/knownworld/reference/territory_zones.json`: broader spatial zone polygons already exist; add zone-level fertility/settlement influences without rewriting the topology blindly.
- `src/main/resources/data/knownworld/reference/cultures.json`: `astapori`, `yunkish`, `meereenese`, `new_ghiscari`, `ghiscari`, `dothraki`, `lhazareen`, `hyrkooni`, `qartheen`, `yi_tish` can be reused **if the referenced ID is present in the live repo** (validate at implementation time). No invented canon religious identifier.
- `src/main/resources/data/knownworld/reference/religions.json`: `great_shepherd`, `great_stallion`, `unknown_religion` are useful baselines; do not treat ancient Ghiscari gods as currently well documented; Qarth's faiths vary.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_slavers_bay.json`: existing Ahlaq, Eraz, Faez, Myraq, Qaggaz, Rhaezn, Yunzak, Zherzyn (Yunkai); Nakloz, Ullhor (Astapor); Dhazak, Galare, Ghazeen, Hazkar, Kandaq, Loraq, Merreq, Naqqan, Pahl, Quazzar, Reznak, Rhazdar, Uhlez, Yherizan, Zhak (Meereen). Check scenarios for duplicate variants (e.g. `family_zo_loraq` in the seed file) before character spawning.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/dynasties/essos_valyria_qarth.json`: Deeth, Emeros, Mallarawan, Daxos; **do not** create unrelated noble houses in the Three Slaver Cities or Hyrkooni fortresses.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`: world holdings exist for `qarth_domain` and for many other centers; attach researched places to **their current holdings** or explicitly create needed holdings with provenance. Don't create duplicate estates for old ruins.
- `simulation/population/PopulationCohortManager`, `AnnualCohortDemography`, `SettlementAgriculturalWorkforce`, existing inventories and military managers: **must ultimately operate on persistent persons and actual household/farm/army membership**, not on a separate fictional population. Research target sizes initialize actual population, not runtime `anonymous` people.

### Future contracts

1. **`PlaceIdentity`** → stable canonical ID, aliases, marker observations, status by scenario date, physical region and future build-site geometry. Record Qal island plus two child fortified harbors.
2. **`EnvironmentalZoneProfile`** → spatial baseline climate/soil/water/crops/animal/settlement density from verified polygons and biome layers; provenance and uncertainty per field.
3. **`PopulationGenerationPlan`** → *seed targets*, age/family/status and occupation tendencies. Must produce **unique persistent NPC IDs**, their families, employers, birthplaces and residences; never permanently abstract-only “anonymous inhabitants.”
4. **`Workplace/holding assignment`** → identifiable farms, merchants, workshops, dockyards, temples, government bodies and their real occupants; an individual can work, travel, migrate or die without creating duplicate counts.
5. **`Polity/office structure`** → named existing elite family when supported; otherwise seat held by generated person tied to **appropriate local institution**, not automatically a Westerosi hereditary House.
6. **`Army membership`** → named/regiment-linked individual soldiers, guards, Unsullied trainees, Hyrkooni warrior women, naval crews; duty, skill, pay, supply and residence. Don't double count training stock as city guard or sailors as infantry.
7. **`Structure generation`** → archetypes: Ghiscari pyramid city (3 variants), Qartheen triple-walled city, Hyrkooni cliff-pass fortress, Lhazareen pastoral town, Qartheen minor ports, Moraq tropical river/harbor town, ruin-specific templates. Never replace unique monumental sites with tenfold vanilla villages.
8. **`World event projection`** → a farmer, guard or freed captive the player meets is the **same persistent person** who contributed to labor, food shortage, defense or slavery in the remote simulation.

## 6. Cross-map and future reconciliation queue (intentionally deferred)

- SE-029 Gorosh and SE-031 Zamettar duplicate Sothoryos observations (`so.png`). Retain until consolidated final matching.
- SE-023 Qal is the island and two fortified harbors. SE-025 East Qal may be a harbor annotation, not an independent city. Preserve source X; hold additional population/worldgen.
- SE-004 alias **Vaes Mejhah** versus hand-labeled “Meihah”. SE-002 **Hazdahn Mo**, atlas's **Hazdahn No** misspelling. SE-005 former **Ghardaq**, atlas's **Ghazdaq** misspelling. Do not turn orthographic variants into duplicate population.
- SE-007 **Samyriana / Shamyriana** same city; link NE's Kayakayanaya for shared Hyrkooni institutional model but do not merge them.
- Qarth / Qal / island port positions; snap only after land/shoreline and regional-master calibration tests.
- All permanent census and garrison numbers `[M]` need worldwide joint calibration and a memory/performance benchmark before full-world population initialization.
- The SE list may miss individual true Xs or contain label-derived false positives; output of the user's detector is pending later combined pass. Do not “correct” unverified points by guessed nearest-label assignment.

## 7. Sources (direct links usable in repository)

Sources are predominantly A Wiki of Ice and Fire (AWOIAF), which cites primary ASOIAF text, *The World of Ice & Fire*, and *The Lands of Ice and Fire*. Where an article only gives a name/location, additional economics, soil or architecture are explicitly **[I]/[M]**, not attributable to the article.

- [S1] [Meereen — AWOIAF](https://awoiaf.westeros.org/index.php/Meereen): Great Masters, families, pyramids, farms, animals and minerals.
- [S2] [Meereen comparative size — AWOIAF](https://awoiaf.westeros.org/index.php/Feature_article/57): description of city size relative to Astapor/Yunkai.
- [S3] [Yunkai — AWOIAF](https://awoiaf.westeros.org/index.php/Yunkai): Wise Masters, walls, army, commerce.
- [S4] [Astapor — AWOIAF](https://awoiaf.westeros.org/index.php/Astapor): Good Masters, red bricks, Unsullied, plazas, chronology.
- [S5] [Qarth — AWOIAF](https://awoiaf.westeros.org/index.php/Qarth): Pureborn/guilds, walls, Jade Gates, island Qal harbors.
- [S6] [Lhazar — AWOIAF](https://awoiaf.westeros.org/index.php/Lhazar), [Lhazareen](https://awoiaf.westeros.org/index.php/Lhazareen): sheep herding, religion, three cities, regional ecology.
- [S7] [Old Empire of Ghis — AWOIAF](https://awoiaf.westeros.org/index.php/Old_Empire_of_Ghis): Old Ghis, New Ghis context, colonies, southern Ghiscari ruins.
- [S8] [New Ghis — AWOIAF](https://awoiaf.westeros.org/index.php/New_Ghis): free iron legions, location.
- [S9] [Great Moraq — AWOIAF](https://awoiaf.westeros.org/index.php/Great_Moraq): rivers, north/south vegetation, Faros and Port Moraq.
- [S10] [Vaes Efe](https://awoiaf.westeros.org/index.php/Vaes_Efe).
- [S11] [Vaes Diaf](https://awoiaf.westeros.org/index.php/Vaes_Diaf): Hazdahn Mo / map alias.
- [S12] [Vaes Jini](https://awoiaf.westeros.org/index.php/Vaes_Jini): Yinishar / Stone Road.
- [S13] [Vaes Mejhah](https://awoiaf.westeros.org/index.php/Vaes_Mejhah).
- [S14] [Krazaaj Has](https://awoiaf.westeros.org/index.php/Krazaaj_Has): Ghardaq / Ghazdaq.
- [S15] [Adakhakileki](https://awoiaf.westeros.org/index.php/Adakhakileki).
- [S16] [Shamyriana](https://awoiaf.westeros.org/index.php/Shamyriana), [Great Fathers](https://awoiaf.westeros.org/index.php/Great_Fathers): city and political/military practices.
- [S17] [Kosrak](https://awoiaf.westeros.org/index.php/Kosrak).
- [S18] [Hesh](https://awoiaf.westeros.org/index.php/Hesh).
- [S19] [Lhazosh](https://awoiaf.westeros.org/index.php/Lhazosh).
- [S20] [Bayasabhad](https://awoiaf.westeros.org/index.php/Bayasabhad), [Patrimony of Hyrkoon](https://awoiaf.westeros.org/index.php/Patrimony_of_Hyrkoon).
- [S21] [Vaes Tolorro](https://awoiaf.westeros.org/index.php/Vaes_Tolorro): actual gardens and wells in otherwise abandoned town.
- [S22] [Vaes Shirosi](https://awoiaf.westeros.org/index.php/Vaes_Shirosi).
- [S23] [Vaes Qosar](https://awoiaf.westeros.org/index.php/Vaes_Qosar).
- [S24] [Vaes Orvik](https://awoiaf.westeros.org/index.php/Vaes_Orvik).
- [S25] [Qarkash](https://awoiaf.westeros.org/index.php/Qarkash).
- [S26] [Asabhad](https://awoiaf.westeros.org/index.php/Asabhad).
- [S27] [Port Yhos](https://awoiaf.westeros.org/index.php/Port_Yhos).
- [S28] [Qal](https://awoiaf.westeros.org/index.php/Qal).
- [S29] [Faros](https://awoiaf.westeros.org/index.php/Faros), [Great Moraq](https://awoiaf.westeros.org/index.php/Great_Moraq).
- [S30] [Vahar](https://awoiaf.westeros.org/index.php/Vahar).
- [S31] [Sothoryos](https://awoiaf.westeros.org/index.php/Sothoryos): Gorosh/Zamettar and northern shore.
- [S32] [Port Moraq](https://awoiaf.westeros.org/index.php/Port_Moraq).
- [S33] [Dothraki sea](https://awoiaf.westeros.org/index.php/Dothraki_sea), [Century of Blood](https://awoiaf.westeros.org/index.php/Century_of_Blood): extent of ruined cities.

## 8. Research verdict and handoff

**Done for Step 2.1:** all 31 SE candidate observations addressed; 298 AC operating status; 10+ regional ecological/political influences; provisional population/garrison envelopes; sector composition; governance, cultures/faith, livelihoods, structures, lore/assumption provenance; repo cross-check; future integration contracts.

**Not done:** marker verification, source-perfect pixel coordinates, geography-to-Minecraft terrain snapping, final census figures, generation of individual NPCs, code changes, settlement worldgen or automatic farming. These intentionally follow in the agreed consolidation/Step 2.2/3 phases.
