# Known World geographic research — Step 2.1 / SO (Sothoryos and the Basilisk Isles), 298 AC

**Status:** Source-grounded first-pass research and scenario-design document; **NOT** a final annotated-X reconciliation, exact placement file, population census, or Step 2.2 implementation.  
**Scope:** The `so.png` **Sothoryos** regional map (`MapZoneId.SO`, 2,248 × 792 regional pixels), including northern Sothoryos, the Basilisk Isles, Naath, and the Isle of Elephants/Zabhad where represented. It also covers unnamed northern coastal trade towns, indigenous communities, historical ruins, waterways, ecosystems, and sea lanes required to make the map behave like a populated world.  
**Scenario:** *A Song of Ice and Fire* **book continuity at the beginning of 298 AC** (royal arrival at Winterfell). Later *AFFC/ADWD* revelations can establish world lore, but their events must **not** automatically be applied as completed political changes in the 298 AC start.  
**Researched:** 2026-10-17.  
**Source cartography:** Repository: `authoring/maps/Realms/so.png`; compare `se.png`, `si.png` and master regional/continental borders in the combined verification pass.  
**Important ID distinction:** The `SO-R##` IDs below are **research rows, NOT the red-X detector IDs** used on earlier annotated submaps. The previous chat's exact SO handwritten-X output and image annotations are not available as a reliable text ledger in this conversation. No claim of having counted or matched every red X is made. Preserve the eventual raw `SO-NNN` observations separately and join these research rows to them during the user's planned all-map correction pass. **Never spawn from `SO-R##` alone.**

## 0. Source, provenance, and population contract

- **[C] CANON-SUPPORTED:** Described in a primary ASOIAF work or official atlas, with a cited secondary index pointing to the underlying source. Citations at the end name *The World of Ice & Fire* (TWOIAF), *The Lands of Ice and Fire* (TLOIAF), *A Dance with Dragons* (ADWD), and *Fire & Blood* (F&B).
- **[R] REPOSITORY:** Already present in project code/reference data. An existing feature ID or `SourceConfidence.CANON` label **does not prove** that its `GeographicFeatureType`, parent or date-specific population state is correct.
- **[I] SOURCE-INFERRED:** A modeling implication or plausible extension, never a fictional fact disguised as canon.
- **[M] MOD_ESTIMATE:** Provisional quantitative planning envelope, not a census, verified size, or canonical troop value. Numeric ranges are first-pass **site** envelopes and must be mutually calibrated later, not automatically summed into a known continental total.
- **[U] UNKNOWN:** Exact position, extent, current political control, faith, language distribution, 298 AC occupant count, or historical attribution is not securely established. Store null/unassigned, not an invented canon ruler or house.
- **[H] HISTORICAL ONLY:** A group, settlement, political actor or ruler existed at another time; current occupancy or sovereignty has **not** been established for 298 AC.

### Non-negotiable simulation invariants

1. Every person counted in Naath, Zabhad, a trade town, a pirate camp or a Sothoryi community must resolve to **one persistent identifiable human/person identity**. Cohorts are optional performance aggregations over people, not additional inhabitants; entering a settlement may materialize those identities as meetable NPCs.
2. Population, garrison, labor and captive counts must **not** double-count people. Ship crews, travelers, abducted captives and soldiers retain home affiliation and are not silently added to a port's resident census.
3. A *ruined* city can have visiting scavengers, explorers, traders, refugees or wildlife without receiving an invented civic population or producing goods from phantom farms.
4. `Government`, `owner`, `religious institution`, `culture`, `house/dynasty`, `warband`, `port operator` and `territorial jurisdiction` are different concepts. Do **not** attach a Westerosi feudal house or Lord Paramount to each island or indigenous village.
5. The terrible accounts of Sothoryi people in Westerosi and Ghiscari texts reflect **limited, frequently hostile outside viewpoints**; do not equate indigenous persons with animals, monsters, lawless enemies or universal cannibals. Their real institutions and beliefs remain poorly documented.
6. Hazardous terrain, disease, rainfall and soil suitability affect **real** travel, production, mortality and migration. A biome tag is not automatically an active plague; no disease should kill all entrants deterministically merely for crossing a polygon.
7. Historical Nymerian/Rhoynar settlements, Valyrian colonies, Ghiscari penal colonies and historical pirate rulers belong in **history/ruins**, not as functioning governments at campaign start unless a current existence is independently attested.
8. Distinguish map artwork extent, the known *northern coastal strip*, and the actual geographical extent of Sothoryos, whose southern limits are **not known**. A finite rectangular game raster is not lore proof of a southern coast.

## 1. Executive geographic findings

**[C] Sothoryos is neither wholly empty nor a mapped modern state.** The best-known northern shore has ruins at **Zamettar, Yeen and Gorosh**, and the separate ruined **Gogossos** stands on the **Isle of Tears**. But TWOIAF also refers to **about twenty small trade towns on Sothoryos's northern coast**. These are living, usually unnamed, unhealthy, vulnerable commercial settlements—not one giant city and not automatically twenty named capitals. Inland live **Sothoryi/Brindled Men**, some of whom trade with coast dwellers; further interior political and settlement patterns are not known. The offshore **Basilisk Isles** host intermittently occupied, shifting corsair towns and captive markets. **Naath** is inhabited by its own peaceful population, and **Zabhad** is a city on the **Isle of Elephants** in the Jade Sea, not on continental Sothoryos. [S1][S2][S3][S4][S5]

**[C] Terrain:** rivers, delta, swamp, flint hills, dense subtropical/tropical forest, the Green Hell, perilous freshwater and disease ecology all matter. The northern coast is the area most reliably placed in official cartography; the southern continent must remain geographically uncertain. The account of dragonrider **Jaenara Belaerys** supports a far larger landmass than early merchant maps depicted, not a precise size or coastline. [S1]

**[C] Governance:** no canon continent-wide king, native imperial capital, unified religion, complete population figure, fixed 298 AC pirate monarchy, or current named royal family is established for Sothoryos. The ruler of the Isle of Elephants bears the title **shan**; the personal name, family and real political reach are unknown. [S2][S5]

## 2. Research location ledger (source-to-world identities)

**Population** below means plausible permanent **site** residents including immediate contiguous dwellings, *not* all visitors or ships. **Organized defense** means resident guards/warriors in that same population. All numeric fields are [M] except `0 established`, which means **no documented stable civic residents**, not a claim that no human ever spends a night there. There are no canon population statistics for these sites.

| Research row | Name / group | 298 AC site status | Resident people, first-pass [M] | Resident organized defenders [M] | Political evidence | Required representation |
|---|---|---|---:|---:|---|---|
| SO-R01 | **Naath** | Inhabited, inhabited settlements unnamed [C] | **15,000–65,000** island-wide; not one city | **0 formal military [I]** | Peaceful People, Lord of Harmony worship; no named king [C/U] | Multi-village island society, farms/orchards, refuge sites |
| SO-R02 | **Basilisk Isles**, whole group | Inhabited corsair archipelago [C] | **5,000–25,000** across *all* active bases/isles combined | **400–3,500** embedded in population | Competing transient corsair chiefs; no 298 AC supreme ruler proven [C/U] | Aggregate only, never stack over island subcounts |
| SO-R03 | **Gogossos / Gorgai**, Isle of Tears | **Ruined city** after Red Death [C] | **0 established civic** | — | Former Ghiscari and Valyrian penal colony [H] | Large layered ancient/colonial ruin |
| SO-R04 | **Isle of Tears** apart from Gogossos | Corsair landing/captive holding activity attested [C] | **250–1,800** camps/settlements, included in R02 | 50–350 included | Variable warbands; **not Gogossos revived** [U] | Ports, cages, camp structures, wetlands |
| SO-R05 | **Talon / Barter Beach** | Corsair exchange place on island [C] | **300–2,000** island-wide, included in R02 | 80–500 included | Local corsair traders/captains, not hereditary lord [U] | Beach market, captive pens, moorings |
| SO-R06 | **Ax Isle** | Island with old ruins, historical corsair fort [C/H] | **150–1,500** optional active ports, included in R02 | 30–350 included | Xandarro Xhore's black fort is historical, **not a 298 AC ruler** [H] | Old fort + conditional occupied waterfront |
| SO-R07 | **Isle of Toads / Toad Stone** | Island and ancient monument [C] | **50–750** if contemporary fishing/corsair use is authored, included in R02 | 0–150 conditional | No current island sovereign proven [U] | Forty-foot black-stone idol, limited settlement |
| SO-R08 | **Skull Isle** | Corsair skull-deposit site [C] | **0–250**; no established town census | 0–60 conditional | Religious meaning and current leadership unknown [U] | Landmark/temporary anchorage, not city |
| SO-R09 | **Howling Mountain** | Island with historical corsair royal activity [C/H] | **100–950** optional active camp, included in R02 | 25–250 included | Nymeria-era corsair king is historical only [H] | Difficult cliffs, anchorages, watchposts |
| SO-R10 | **Isle of Flies** | Island with Brotherhood of Bones history [C/H] | **100–950** possible camps, included in R02 | 20–240 included | Brotherhood's 298 AC existence is unverified [U/H] | Disease-prone island, uncertain ruins/camps |
| SO-R11 | **Other Basilisk corsair settlements** (Black Pudding, Port Plunder, Sty, Whore's Gash) | Reused, temporary settlement names [C] | **100–1,500 per current camp**, all within R02 | 20–350 per occupied camp, within R02 | A named site is **not** evidence all four exist simultaneously [C/U] | Ephemeral settlements with dated identities |
| SO-R12 | **Zamettar**, Zamoyos delta | **Ruined / abandoned Ghiscari walled city** [C] | **0 established civic** | — | Old Ghiscari colony; former Valyrian and Rhoynar occupation [H] | Major walled delta ruin, not modern port |
| SO-R13 | **Yeen**, Zamoyos upstream | **Ancient abandoned black-stone city** [C] | **0 established civic** | — | Builders and institutions unknown [U] | Unique colossal oily-black-stone site |
| SO-R14 | **Gorosh**, Wyvern Point | **Ruined Ghiscari penal colony** [C] | **0 established civic** | — | Ancient Old Ghiscari colony, no 298 AC garrison [H] | Coastal ruins, not operational fortress-city |
| SO-R15 | **Northern Sothoryos trade towns (≈20)** | Inhabited small trading towns, names unknown [C] | **200–2,000 per extant town**, **4,000–40,000** combined [M] | 0–120 per town, included | Individual government unidentified [U] | Flexible anchor-limited trading-port network |
| SO-R16 | **Coastal Sothoryi communities** | Inhabited indigenous groups [C] | **50–600 per generated community** [M]; total unknown | 5–80 if a group organizes defense | Indigenous authorities/kin groups unknown [U] | Villages or seasonal occupation, actual society |
| SO-R17 | **Interior Sothoryi communities** | Inhabited, poorly mapped [C/I] | **30–350 per generated community** [M]; total unknown | Local defense unknown | No canon kingdom, census or surveyed roads [U] | Sparse network with explicit source uncertainty |
| SO-R18 | **Basilisk Point colonial remains** | Historical failed Valyrian and Rhoynar settlements [C/H] | **0 as historical colony**; contemporary separate camps possible | — | Ruined colonies, **not present-day Valyrian rule** [H] | Multiple small archaeological footprints |
| SO-R19 | **Zabhad**, Isle of Elephants | **Active city / chief island port** [C] | **7,000–25,000** [M] | **150–900** [M] | Island ruled by a **shan**, personal identity unknown [C/U] | Separate Jade Sea city/culture/trading polity |
| SO-R20 | **Isle of Elephants** beyond Zabhad | Inhabited island with elephants [C] | **3,000–20,000 additional** rural people [M] | 0–350 island-wide outside Zabhad | Same shan's island authority; limits unknown [C/U] | Rural districts, herds, transport, island interior |
| SO-R21 | **Naathi Valyrian fort / Volantene trade-town remains** | Historical foreign ruins [C/H] | **0 separate active foreign colony** | — | Fort and Volantene town from an earlier era [H] | Point-of-interest ruins, not automatic new villages |
| SO-R22 | **Unlocated ancient inland cities** | Legendary/unsurveyed ruins [C as report; U exact location] | **0 established** | — | Founder/date unknown [U] | Discovery sites only after deliberate authoring |

### Counting guardrails

- **R02 is an aggregate**: R04–R11 must fit *within* it; never add their people to R02 a second time. Conditional camp estimates are site budgets, not assertions that every listed island is currently settled.
- **R15 includes actual Sothoryos coastal towns** and must not overlap the ruins R12–R14, separate native communities R16–R17, or Basilisk port camps R04–R11. If an indigenous household lives inside a trade town, assign it **once** to that town.
- **R01 is Naath-wide**; do not invent a single city of 65,000 inhabitants.
- **R19 is Zabhad urban; R20 is its additional countryside.** Only this pair can be summed for an island population envelope; it is **not** a Sothoryos mainland total.
- **A source description of 'a score' of trade towns does not identify their names or exact locations.** Resolve their eventual procedural identities from legal coast cells, river mouths and ports without pretending they appear individually in atlas labels.

## 3. Site profiles: political history, architecture, livelihood and gameplay

### 3.1 SO-R01 — Naath: the Peaceful People, villages, orchards and refuges

**Evidence and starting state [C]:** Naath lies **west of the Basilisk Isles**, east of the Summer Isles and off northwestern Sothoryos. The Naathi are a living people, not an extinct ruin population. Their tradition emphasizes nonviolence and worship of the **Lord of Harmony**, associated with the island's butterflies. Repeated slaver raids have driven many Naathi from exposed shores into wooded hills; Naath's once-important silk exports have dwindled. The Valyrian dragonstone fort, short-lived Volantene trade town and earlier Ghiscari conquests belong to **history**. The local disease called *butterfly fever* has repeatedly limited long-lived foreign occupation, but raids can still succeed because slavers land and leave quickly. [S5][S6]

- **Government:** no documented individual Naathi monarch or noble house for 298 AC. For simulation [I], use village/community elders or councils, plus sanctuary/temple authority as *optional* mod-modeled offices clearly labeled unverified; do **not** create a fictional canon dynasty.
- **Culture/religion:** `naathi` [R] is defined in `reference/cultures.json`. Model Peaceful People's teachings as a region-linked belief/faith tradition, not `faith_of_seven` or `drowned_god`. The Naathi statement about eating fruit rather than animal flesh is a strong baseline for voluntary food choices, but should not prevent individual variation or external trade.
- **Economy [I/M]:** sheltered orchard horticulture and fruit drying **32%**; silk and fine plant/fiber work **18%**; other garden tending and storage **16%**; craft/building **11%**; local trade/transport **8%**; spiritual, communal and household services **15%**. These are occupational shares of active adult workers, not all residents; no separate soldiers are generated. Total **100%**.
- **Agricultural compatibility [I]:** fruit-bearing tropical/subtropical groves, garden plots, water collection, food drying, storage. The exact species mix, soil productivity, field sizes and silk yield are unknown. Avoid automatic giant grain monocultures or intensive slaughterhouses.
- **Settlement forms [I]:** modest distributed family dwellings; small communal meeting and worship areas; orchard paths; storerooms; workshops; hidden inward retreats. Ruined foreign fort and shoreline posts should be physically distinct from present Naathi homes.
- **Disease and defense:** endemic infection risks are **host/duration/exposure-specific**, not a blanket magical permanent immunity mechanic; the Naathi are said to be unaffected while outsiders are vulnerable. No invented Naathi professional army. Separate short pirate attacks from extended foreign garrisons, allow hiding/rescue/diplomacy.
- **298 AC identities:** Missandei and her brothers' Naathi origins are book canon, but their later-revealed experiences do not prove they are personally on Naath at exact scenario initialization; timeline reconciliation is required before creating their current residences. [S5]

### 3.2 SO-R02 and R04–R11 — Basilisk Isles: distributed corsair archipelago

**Evidence [C]:** The largest islands and landmarks include **Isle of Tears, Isle of Toads, Isle of Flies, Talon, Skull Isle, Ax Isle and Howling Mountain**. The group is a refuge for corsairs, slavers, fugitives, merchants, sellswords and more. Ancient ruins on the Isle of Tears, Isle of Toads and Ax Isle predate modern pirate settlements. Settlements such as **Black Pudding, Port Plunder, Sty and Whore's Gash** are shifting, short-lived port names, not four permanently surveyed independent cities. [S2]

- **Government [I]:** each 298 AC harbor can have a **warband/corsair captain** or coalition of captains. Do not start a unified King of the Basilisk Isles simply because Saathos Saan or older corsair kings ruled at earlier dates. A rumor about a *new* corsair king during *AFFC* is future relative to 298 AC, not baseline fact.
- **Resident job mix [M], adult workers for *currently occupied* ports:** crews/harbor labor/navigation **27%**; provisioning, fishing and animal tending **18%**; barter/fencing/warehousing **18%**; construction and ship repair **16%**; household/tavern/other services **11%**; standing shore guards **10%**. **100%**. Captive persons keep **their own actual occupation and status** and are included in one population authority; captives in temporary transit are not permanent residents.
- **Economic mechanics:** cash/barter, pirate provisioning, water/fresh-food stores, stolen goods, recruitment, slave trafficking as a coercive institution of actual agents, taxes/tolls demanded by warbands, occasional fleets sent by external powers. None of this should grant every pirate infinite loot or infinite victim NPC spawns.
- **Settlement footprint:** hot muddy shacks, timber walls, warehouses, beach landings, repair slips, corrals, market clearings, palisade camps, disease-ridden bogs, temporary defensive positions. Keep wealth rare and insecure; a fort can remain visible after a group abandons it.
- **Temporal identity:** an active camp may change name, occupants, captain and allegiance across time; the **physical ruin/landing** remains, while `settlement_identity`, `warband_id` and `occupation_interval` are separate. Pirate eradication by a player does not permanently delete all island settlement potential; repopulation requires actual migrants and ships.

**Island-specific points:**

| Island / site | Source-backed special features | Implementation caution |
|---|---|---|
| **Talon / Barter Beach** | Known meeting/exchange market among corsairs and captive holding areas [C] | Market is **not** a unique royal city, and the whole island is not one market footprint. |
| **Isle of Tears** | Rugged flint hills, black bogs, ruined Gogossos; corsair pens [C] | Gogossos's once-great city population **is not alive in 298 AC**. |
| **Ax Isle** | Ancient ruins and historical black fort built by Xandarro Xhore [C/H] | Do not spawn Xhore as present ruler absent a dated source. |
| **Isle of Toads** | Ancient **Toad Stone**, roughly forty feet high and made of greasy black stone [C] | Unique monument, not a generic vanilla altar or active god NPC. |
| **Skull Isle** | Distant northern isle to which corsairs deliver skulls [C] | Historical ritual/usage is documented; permanent priests/city are **not**. |
| **Howling Mountain** | Island linked to a Nymeria-era corsair ruler [C/H] | Historical king is not an identifiable living 298 AC NPC. |
| **Isle of Flies** | Brotherhood of Bones settled there after Red Death era [C/H] | Contemporary order status unknown; do not resurrect it as proven current rule. |
| **Black Pudding / Port Plunder / Sty / Whore's Gash** | Recurring names for short-lived corsair towns [C] | Names should be drawn from an attested **name pool**, not four always-active global coordinates. |

### 3.3 SO-R03 — Gogossos (Gorgai), the plague-abandoned city

- **Lore [C]:** Old Ghis built **Gorgai** on the **southern coast of the Isle of Tears**. Valyria captured the city in the Third Ghiscari War, renamed it **Gogossos** and used it as a penal colony. Its terrible experiments and slavery are historical context; in the Century of Blood, the **Red Death** devastated Gogossos and the Basilisk Isles, leaving the city ruined. [S2][S7]
- **298 AC:** `ruined_city`; civic population **0 established**. Pirate camps elsewhere on the island remain possible, as do short-term entrants into ruins; neither makes Gogossos a functioning giant Valyrian city.
- **Architecture [I]:** distinct original Ghiscari foundations, later Valyrian masonry/compounds, collapsed warehouses and quay walls, plague grave areas or abandoned facilities **only where suitably documented/designed**. Do not represent a generalized fantasy plague as a currently permanent instant-death zone.
- **Political/story content:** difficult archaeology, foreign claims to relics, competing salvagers, risky anchorage, environmental threats, unknown exact ruined footprints. There is no current queen/governor or tax-base output.

### 3.4 SO-R12 — Zamettar, walled city at the mouth of the Zamoyos

- **Lore [C]:** Ghiscari-founded **walled** city on the large **Zamoyos delta**. Its control changed under the Valyrians; much later Princess Nymeria's Rhoynar tried to live among its long-abandoned ruins. They ultimately fled Sothoryos after disease, predation, raids and the disappearance of their people at Yeen. By the story era the jungle is reclaiming abandoned Zamettar. [S1][S8]
- **298 AC:** source-supported **ruins**, not a surviving Ghiscari port or a Nymerian city. The old gates and road/wharf traces can exist, but large-scale farming and resident households should not automatically function.
- **Layout [I]:** substantial ruined walls, former gate passages, sediment-clogged quays, delta distributaries, marshy approach roads, former market/street grid, encroaching jungle. No fixed pre-Aegon census or canonical ruler.
- **Gameplay:** river-mouth expedition and archaeological access; protect against automatically generating a second modern trade town on the **same site** without explicit settlement-over-ruins flag. In earlier `SE-031`, Zamettar is already flagged as a **cross-map duplicate**.

### 3.5 SO-R13 — Yeen, the exceptional black-stone ruins

- **Lore [C]:** Yeen lies **up the Zamoyos from Zamettar**. Its builders, age and purpose are unknown. Its immense fitted **oily black stone** has resisted encroachment by the surrounding jungle; Rhoynar colonists vanished, and attempts to inhabit the ruins are associated with disaster. [S1][S9]
- **298 AC:** uninhabited major **ancient ruin**, not an evil faction's capital or a densely occupied Sothoryi city. Rumors are **rumors**; do not assert the cause of disappearances.
- **Layout [I]:** monumental enigmatic blocks, huge geometric terraces/courtyards, minimal rooted vegetation **inside the ancient masonry**, dense growth immediately outside. Do not freely copy the entire Valyrian/Ghiscari architecture set; Yeen's builders are not established to be either.
- **Exploration:** preservation, research and hazards should rely on real navigation, environmental dangers and lore uncertainty. Do not invent a confirmed Lovecraftian god, magical boss or canonical resident caste.

### 3.6 SO-R14 — Gorosh, Wyvern Point

- **Lore [C]:** Old Ghis founded **Gorosh** as a penal colony on the **northern coast of Wyvern Point**. The city is ruined by the story era. [S1][S10]
- **298 AC:** stable civic inhabitants and garrison **0 established**; no present Good Masters, Ghiscari tax administration or production industry. Avoid rendering `GeographicFeatureType.CITY` as evidence of live function.
- **Layout [I]:** coastal defensive ruins, prison/foundation remains, dock traces and tropical overgrowth; structural detail mostly unknown.
- **Cross-map:** same place as **SE-029**, not a second settlement. Its identity should resolve once with both regional observation pointers.

### 3.7 SO-R15 — The twenty or so living northern trade towns

**Why this matters [C]:** TWOIAF describes **a score of small trade towns** on Sothoryos's northern coast. Treating the entire mainland as nothing but abandoned ruins would contradict this. Their individual place names, administrations, exact coordinates and distinct populations are **not** given. [S1]

- **Site-count policy:** use `~20` as the lore-level scale target across the **known northern littoral**, not twenty precisely measured coordinates in `so.png`. Start with some tentative landing/river-mouth candidates and defer final count, coast bounds, access and overlaps to the all-map pass.
- **Population [M]:** 200–2,000 per inhabited trade town as a *planning range*; 4,000–40,000 aggregate if all ~20 survive. Risk-driven seasonal decline can be severe; visitors and transient ships excluded. A few could be smaller or bigger once locally authored.
- **Adult employed-role mix [M]:** coast fishing/food/water collection **25%**; trade, port labor, packing/transport **29%**; building/boatsmith/crafts **20%**; gathering/hunting/wood products **13%**; household/service/medical tasks **8%**; guards **5%**. Total **100%**, guards included in residents. Legal status is orthogonal.
- **Economic context [C/I]:** foreign fortune seekers, coastal trading opportunities, corsair predation, hazardous supply routes. Possible cargo such as gemstones, gold, exotic spices and forest products should **only** be produced after an actual source/local deposit and assigned workers are confirmed; reported exploratory finds do not guarantee a rich mine at each town.
- **Physical design [I]:** river landing, muddy causeways, raised storehouses, dock timbers, rain shelters, improvised palisades, wetland/water storage, short buildings rather than stone city walls, a few inland paths to trading contacts, hazard-adapted construction.
- **Government [U]:** no continent-wide colonial charter or named houses. A town can have a merchant committee, harbor boss, expedition captain or mercenary protection under **explicit MOD_ESTIMATE authority**, capable of changing through play.
- **Population dynamics:** high turnover from disease, raids and departure; resident migrants remain real identities, not fresh copies on each spawn. Settlements can be abandoned, reclaimed, recolonized or renamed with historical event records.

### 3.8 SO-R16–R17 — Sothoryi/Brindled Men and the unknown interior

- **Lore [C]:** Brindled Men/Sothoryi are indigenous inhabitants. Some of the northern coastal people speak **Trade Talk** and trade with outsiders. Accounts of more distant groups, worship and warfare largely come from hostile or uncertain foreign reports. Other supposed ancient populations or creatures such as lizard men and eyeless cave-dwellers have **no reliable confirmation**. [S1][S11]
- **Demography [U/M]:** **no canonical village count, polity count, inhabitants-per-tribe estimate, territory extent or language atlas**. A generated community (for initial playtesting) may be 30–600 residents, but this is *not* a bound on total Sothoryi population. Do not fabricate one continent-wide statistical total from that per-village number.
- **Society [I]:** provision community identity, kinship, ancestry, languages, elders/leaders, local conflict resolution, intergroup treaties, labor, trading partners and changing alliances. **Do not automatically set all communities to hostile**, give them the same religion, or make them someone's labor resource.
- **Food/environment [I]:** forest gathering, fishing, freshwater adaptation, local plant cultivation and hunting where ecological data permit; choose actual agriculture only after biome and crop suitability checks. No claim about specific staple species or continent-wide domestication patterns.
- **Architecture [I]:** adaptable village scale, shade, ventilation, rainy-season flood protection, clustered homes, locally available organic materials, river crossings; **provisional artistic direction**, not verified canonical architecture.
- **Physical/abstract interface:** all actual persons remain persistent; a player's passage through unfamiliar jungle should reveal people, noncombat encounters, trade opportunities, migration and territorial diplomacy, not a hostile-mob-only biome.
- **Interior ruins [U]:** reports of further inland ancient cities support *unknown discovery locations*, not machine-generated thousands of black-stone Yeen copies.

### 3.9 SO-R18 — Basilisk Point and its failed overseas colonies

- **Canon [C]:** three Valyrian colonies were successively lost—one to the Brindled Men, one to disease and one abandoned after Valyrians seized Zamettar. Nymeria's Rhoynar later founded coastal towns; two were sacked and the Rhoynar eventually left. This is a **historical ruin chain**, not five named occupied contemporary towns. [S1][S12]
- **Map effect [I]:** multiple potentially small archaeological layers on the peninsula; ruined pathways, landing beaches, occasional modern scavengers or Sothoryi camp. The **number, exact position and footprint** of individual colonial sites are not charted.
- **Safety:** never assign a modern Valyrian lord, claim or garrison on the basis of historical colonial presence. Colonization can be player/NPC-driven in the simulation **after 298 AC** with actual settlers, supplies and disease risk.

### 3.10 SO-R19–R20 — Isle of Elephants and the city of Zabhad

- **Canon [C]:** **Zabhad** is the chief port on the **northern shore of the Isle of Elephants**, which lies in the **Jade Sea**, south of Great Moraq and northeast of Sothoryos. The island's ruler is called a **shan** and is said to have an ivory palace. Its elephants are a major distinct environmental/economic feature. **Zabhad is not a ruined city, not part of the Basilisk Isles, and not literally a district of continental North Sothoryos.** [S4][S13]
- **Government [C/U]:** `shan` is a canonical sovereign title; ruler's personal name, dynasty, succession law and exact jurisdiction are not established. Implement a local ruling office with unknown person identity, **not** an invented 298 AC king or named house.
- **People [M]:** urban Zabhad 7k–25k + additional island countryside 3k–20k. Site-specific numbers have no official basis. Defense 150–900 within Zabhad residents plus at most 350 elsewhere, not additive to workers. Calibrate total island land area and exports later.
- **Jobs [M] for **Zabhad adult workers**:** harbor trade/ship and haulage **26%**; food cultivation/fishing **23%**; artisans/carvers/builders **22%**; local pack-animal/elephant husbandry **8%**; services/temple/palace administration **15%**; security **6%**. Total **100%**. Elephant use and ivory extraction are **not proof of a canon hunting industry, headcount or sustainability practice**.
- **Food/trade [I]:** island agriculture, coastal fisheries, port exchange with the surrounding Jade Sea and spice-trade ports. Specific species yields, merchants and routes must be recorded as inferred until separately sourced.
- **Architecture [C/I]:** an **ivory palace** is attested by Corlys Velaryon's report. A warm-water chief port may have merchant warehouses, shipyards, elephant-related roads and gardens, but style and size are unrecorded.
- **Map parentage:** the regional **image** may include this island; that does not transfer its political/landmass ownership to Sothoryos. Maintain a distinct island location parent and territorial region.

### 3.11 SO-R21–R22 — Historical foreign ruins and unreliably mapped sites

- **Naathi Valyrian fort [C/H]:** melted/fused dragonstone remains on Naath; **not** a live Valyrian garrison.
- **Volantene Naathi trading colony [C/H]:** former short-lived palisaded trade town; evidence does not support modern Volantene sovereignty or site-level stable town identity in 298 AC.
- **Yeen-like unknown ruin reports [C/U]:** other ancient cities are rumored in the interior; their names and map positions are unknown. Only add unique authored ruins under `unknown_ruin` archetypes; no factual politics, dates or builders.
- **Old pirate/Valyrian forts [H]:** retain temporal historical occupancy; a player may discover abandoned walls or a new group may occupy an old fort, but these are different ownership events.

## 4. Full environmental and landscape zoning matrix

| Geographic zone | Canon-supported geography | Generate [I] | Restrictions / unresolved |
|---|---|---|---|
| `sothoryos_northern_littoral` | Northern shore trade towns, hot humidity [C] | Coastal forest, wet trade landings, mouths, fishing | Coastline must come from map/mask; no uniform continuous towns |
| `zamoyos_delta` | Zamettar at broad river delta [C] | Delta branches, wetlands, brackish soil, silt banks, ruins | Branch positions/widths not measured; do not assume potable river |
| `zamoyos_corridor` | Zamoyos passes Yeen inland [C] | Navigable/partly navigable corridor, rapids where inferred, dense riverside canopy | Yeen–Zamettar ordered connection yes; exact river course pending |
| `basilisk_point` | Peninsula with basilisks, old colonies [C] | Coastal capes, dense growth, reptile habitat, expedition ruins | Do not create a modern Valyrian province |
| `wyvern_point` | Northeastern promontory and Gorosh [C] | Seaward bluffs/coastal jungle, colony ruins | Wildlife density unknown; port frontage to check |
| `green_hell` | Dangerous deeper jungle, reported unusual fauna [C] | High-canopy ecology, strong humidity/risk, wildlife and hidden communities | Does not mean entire Sothoryos is flat swamp or permanently impossible to inhabit |
| `sothoryos_interior_unknown` | Very poor geographic knowledge [C] | Reserved/uncertain biome blend; authored exploration discoveries | No invented full outline, precise rivers, mega-empires or cities |
| `basilisk_isles_rock_and_bog` | Isle of Tears flint hills, black bogs [C] | Rugged coastlines, swamps, short pirate piers, historical ruins | Distinct islands, not a single continuous mainland polygon |
| `basilisk_isles_small_islands` | Multiple named islands/ruins [C] | Hot, humid micro-ecosystems, caves, intermittent camps | Coastal positions differ by map; don't place all on one `isle_of_tears` |
| `naath_orchards_and_hills` | Inland Naathi retreat, butterflies and historic fort [C] | Mixed forest/hills/orchards/paths | No automatic foreign cities; pacifist policy not the absence of conflict |
| `isle_of_elephants_jade_sea` | Warm-water Jade Sea island, elephants and Zabhad [C] | Coastal port, countryside, elephant ecology | Not part of Sothoryos mainland; wildlife abundance is not automatically harvest rate |
| `corsairs_road` | Sea route off northern Sothoryos [C] | Maritime graph; pirate intercepts, provisioning links | Not an overland road and not a static pirate capital |

### Disease implementation rules

- **Naath:** butterfly fever targets susceptible non-native visitors based on long-term exposure; immunity/susceptibility should be modeled carefully as a *specific setting claim*, not a shortcut that all Naathi resist every illness.
- **Sothoryos:** blood boils, green fever, dancing plague, sweetrot, bronze pate, Red Death and additional named ailments are reported. They are **possible diseases**, not proof they are simultaneously raging throughout the map in 298 AC. Add reservoirs, contact routes, incubation, resistance, medical care, migration, survival and contagion states, each sourced or tagged inferred.
- **Rivers:** polluted/parasite-laden freshwater may reduce safe supply; survival must depend on filtration/boiling or other plausible mitigation, not permanent absolute inaccessibility.
- **Ecological balance:** predator populations and dangerous creatures are finite persistent wildlife, not newly created mobs every tick; use risk maps that reflect actual habitats, not universal hostile spawn rolls.

## 5. Political, cultural and faith/authority policies for 298 AC

| Region/group | Political baseline | Culture/language | Religion | 'House' / government policy |
|---|---|---|---|---|
| Naath | Autonomous indigenous island society; rulers unnamed [C/U] | Naathi [C/R] | Lord of Harmony tradition [C] | No canon noble dynasty or formal army |
| Basilisk Isles | Multiple shifting corsair authorities [C/I] | Mixed regional sailors, Sothoryi, freed/captive persons [C] | Diverse/unknown; Skull Isle rituals not uniform faith [C/U] | Port captain/warband; no proven 298 AC king |
| Mainland Sothoryos coastal trade towns | Small fragmented trade enclaves [C] | Multi-origin visitors/local communities [C/I] | Mixed/unknown [U] | No global colony governor or automatic Free City sovereignty |
| Coastal Sothoryi | Indigenous communities with some Trade Talk speakers [C] | Sothoryi [C]; Trade Talk for coastal contacts [C] | Unrecorded [U] | Community/kin organization [I], **no** imported feudal baronies |
| Interior Sothoryi | Diverse groups likely, not surveyed [I/U] | Specific speech/kinship unknown [U] | External hostile rumors must not be canonized [U] | Local identities need authoring; not universal hostility |
| Isle of Elephants | Ruled by a *shan* [C] | Specific local culture/language not pinned down [U] | Unknown [U] | Sovereign office `shan`, **unnamed**; distinct Jade Sea polity |
| Ancient Gorosh/Zamettar/Gogossos | No functioning Ghiscari/Valyrian government [C] | Former colonial affiliations historical [C] | No current civic religion [U] | **Ruins**, no living dynastic holding assigned |
| Ancient Yeen | No functioning city administration [C] | Builders unknown [U] | Unknown [U] | Ancient ruin, never auto-assign a creator race/faction |

### NPC, status and knowledge design

- **Individuals, not stereotypes:** pirates, Naathi, Sothoryi and merchant-adventurers have individual motives, affiliations, family and law constraints. Being from the Basilisk Isles does not automatically make an NPC enslaver or criminal.
- **Raids:** aggressors, defenders, captives and rescuers are unique persistent people; capture transitions social/legal position and household/whereabouts, **not** a reduction to trade goods or a disappearance from the census.
- **Personal knowledge:** inland ruins, stories of monster species and supposed curses should be rumors with speaker, time, reliability and belief state. Character knowledge must not become universal authoritative map revelation.
- **No giant fixed armies:** pirates can field ships with real captains, crew and stores. At most the organized defenders within small populations are initially stationed; offshore raiding flotillas retain their own owners/personnel.

## 6. Farming, ecology and local livelihood constraints

**Important:** The following suitability suggestions are **[I]**, not crops or yields asserted by GRRM. Numbers in this section are **planning ranges only**. No auto-production without actual owned land/sea space, tools, seed/stock, transport and assigned workers.

| Area | Candidate food/resource systems [I] | Agricultural/transport bottleneck | Design prohibition |
|---|---|---|---|
| Naath | Fruit orchards, kitchen gardens, silk-related fiber work, preserved fruit, sheltered water | Raids, shoreline abandonment, insect ecology | No broad militarized slavery-based plantations by default |
| Basilisk harbor camps | Shallow-water fishing, imported grain/fruit, rain collection, repairs and salvage | Poor soils, disease, corsair disruption, limited farmland | No self-sustaining megacities or endless free supplies |
| Mainland littoral | Fishing, gathered timber/fiber/spices, riverbank gardens, opportunistic mineral gathering | Swamp, disease, insects, predation, unstable roads | No rich gems at every marker merely because explorers reported gems |
| Sothoryi villages | Place-specific gathering, hunting/fishing, local cultivated plots if suitable | Unknown crops/cultural preferences, ecology and floods | No unsupported universal agricultural tech/crop uniformity |
| Zamoyos delta | Fertile **potential** silts, fish and reeds | Flooding, waterborne parasites, unsafe access | Ruined Zamettar does not produce crops without workers |
| Isle of Elephants | Island farming/fishing, trade, elephant management | Island carrying capacity, feed and transport logistics | Ivory ≠ unlimited mining node; wild elephants are not endless consumables |

**Workforce percent tables above describe jobs among working adults**; their denominators are not the whole population, and the shares must be assigned to unique persons. Each disease outbreak, pirate raid, migration, crop failure or shipping blockade must affect production through **these same agents** and their inventories.

## 7. Topology, communications, access and settlement placement

### Canon-backed spatial relations (do not confuse with map-pixel coordinates)

1. **Naath** lies west of the Basilisk Isles and northwest of Sothoryos, east of the Summer Isles.
2. **Basilisk Point** is on northwestern Sothoryos; **Isle of Tears** lies **east** of it. **Talon** lies north of the Isle of Tears; **Isle of Toads** to the northeast.
3. **Gogossos** is on the **southern coast of Isle of Tears**.
4. **Zamettar** is on the **mouth/delta of Zamoyos**, **Yeen** is upstream to its south.
5. **Ax Isle** is northeast of Zamettar and southeast of Skull Isle.
6. **Gorosh** lies on the **northern coast of Wyvern Point**, in northeastern Sothoryos, near the Lesser Moraq approaches.
7. **Isle of Elephants** is an **island of the Jade Sea** south of Great Moraq; **Zabhad** is on its **north coast**.
8. **Corsair's road** is a maritime route past northern Sothoryos, linking pirate anchorages and wider merchant crossings.

### Approximate position status

- **Do not assign block coordinates yet.** The regional `SO` raster uses a transform to the 2,048 × 1,357 master map; exact icon points, marker centres, correction of false X detections, and coastal snapping are deferred to the user's planned combined validation pass.
- Store **distinct** fields: `regionalMap=SO`, `sourcePixel=null`, `masterPixel=null`, `buildWorldX=null`, `buildWorldZ=null`, `footprint=null`, `placementConfidence=UNVERIFIED`, `identityStatus=RESEARCHED`.
- For actual 1 block = 1 metre world conversion use the existing authoritative map calibration, not a standalone scale guessed from an online screenshot.
- A single site's political identity is allowed to have **multiple source-map observations** (`SE-029` and SO reference for Gorosh; `SE-031` and SO reference for Zamettar). Never generate two copies.
- Keep **border-clipped areas** separate from the real coast of the continent. The apparent southern image edge is not evidence of a real shore or habitable island limit.

## 8. Repository audit: what exists and what to flag for later correction

**Repository inspected:** `dtzudontsay/Minecraft_Server_Mod2` on `master`, specifically `MapZoneRegistry.java`, `SothoryosFeatureRegistry.java`, `locations.json`, `territory_zones.json`, `landmass_zones.json`, `cultures.json`, `locations_holdings.json`, 298 AC `landed_holdings.json`, and the SE research document. These are **audit leads**, not a request to apply code corrections during research.

| Existing repo object | Finding | Later implementation / cleanup guidance |
|---|---|---|
| `MapZoneId.SO` / `registerSothoryos()` | Registered **2248 × 792** with regional-to-master transform [R] | Keep authoritative mapping; verify X/pixel extraction in global pass |
| `SothoryosFeatureRegistry.zamettar` | Currently `GeographicFeatureType.CITY` [R] | 298 AC site should be **ruined city**; canonical historical settlement can remain in feature catalog but active-state flag must not imply residents |
| `SothoryosFeatureRegistry.gorosh` | Currently `GeographicFeatureType.CITY` [R] | **Ruined city** in 298 AC; see `SE-029` |
| `SothoryosFeatureRegistry.zabhad` | `CITY` [R] | Correct active city **type**, but parent must be Isle of Elephants / Jade Sea |
| `SothoryosFeatureRegistry.gogossos`, `.yeen` | Both typed `RUINED_CITY` [R] | Correct status direction; merge with source identity graph |
| `SothoryosFeatureRegistry.skull_island` | Display name 'Skull Island' [R] | Keep stable ID if referenced; 'Skull Isle' is the frequent canon label |
| `locations.json:ax_ilse` | Typo **`ax_ilse`** / display **`Ax Ilse`**, parented `north_sothoryos` [R] | Canon **Ax Isle**, a Basilisk Isle; correct carefully with aliases/migration |
| `locations.json:isle_of_elephants` | Parented under `north_sothoryos` [R] | Move to **Jade Sea / own island** geographic parent, no sudden save-breaking ID rename |
| `locations.json:isle_of_whips` | Also parented under `north_sothoryos` [R] | Audit against real island / Jade Sea geography, not mainland |
| `locations.json:basilisk_isles` | Parented under `sothoryos` [R] | Geographic grouping acceptable as nearby research scope, **not mainland political/landmass membership** |
| `locations.json:naath` | Already separate world-level region [R] | Preserve independence from Basilisk Isles and Sothoryos mainland |
| `locations.json:yeen`, `locations.json:gogossos` | Existing settlement IDs [R] | Use as canonical stable names; maintain ruined state; reparent Gogossos specifically to Isle of Tears where appropriate |
| Missing in `locations.json` | No **Zamettar, Gorosh, Zabhad** as standalone settlement location IDs in inspected section; registry features are present [R] | Add location/settlement catalog objects in Step 2.2 if no other owner already covers them |
| Missing subregions in inspected feature registry | **Talon, Isle of Flies, Howling Mountain, Barter Beach, Zamoyos, Green Hell, Toad Stone** not registered there [R] | Add geographic/landmark types after full repository deduplication and map check |
| 298 AC holdings/reference assignments | No visible SO-specific landed holdings in inspected `landed_holdings.json` or `locations_holdings.json` [R] | **Do not invent canon nobles** to fill table; add suitable non-feudal offices and real holding owners later |
| `territory_zones.json:north_sothoryos` | Broad mod polygon [R] | Treat as simulation region approximation, not exact indigenous borders or full southern limit |
| `landmass_zones.json:sothoryos` | Broad map mask/polygon [R] | Check against actual land, islands and coastline; do not label all water as a province |
| SE research `SE-029` and `SE-031` | Gorosh and Zamettar intentionally tagged SO cross-map duplicates [R] | Merge by stable site key after full detector reconciliation |

### No changes to the existing code made by this research

The existing source repository remains unchanged. This file is meant to be committed to `docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_SO_298AC.md`. Type corrections, new locations, historical statuses, regional transforms and feature deduplication stay for Step 2.2/global review, as the user requested.

## 9. Step 2.2 implementation contract and follow-up checklist

**Identity and data model**

- [ ] Maintain one stable `location_id` for each canonical named site. Add `site_kind={ACTIVE_CITY, ACTIVE_TOWN, TEMPORARY_CAMP, INDIGENOUS_COMMUNITY, RUIN, LANDMARK, REGION, ISLAND, WATERWAY}` and **date-specific** occupation/civic status instead of relying on timeless feature type.
- [ ] Use `government_office`, `polity`, `dynasty`, `temporary_warband`, `community`, `religion`, `language` as separate relations. Leave unknown current holders null.
- [ ] Distinguish place name pools from currently active settlements; dynamic pirate towns preserve **temporal and physical identities** through rename/abandonment.
- [ ] Allow a source map observation table linked many-to-one to stable sites across SE, SO and potentially SI.
- [ ] Add Sothoryi culture/language and Naathi religion only after audit against existing reference catalogs; reuse current `naathi` culture ID.

**Population, jobs, shipping and coercion**

- [ ] No extra hidden cohort populations. Reconcile households, NPC registry, naval crews, itinerant traders, soldiers, captives, deaths and migrations in the single campaign population authority.
- [ ] Add small-town templates and procedural candidates for northern coastal towns with **source-level count target**, not named-canon city list.
- [ ] Model island shipping by finite real ships and identifiable crew; raiders cannot teleport prisoners from Naath or Sothoryos to markets without travel.
- [ ] Distinguish permanent residents, seasonal/mobile settlement occupants and temporary visitors in population accounting.
- [ ] Tie jobs and crop production to eligible land, tools, real workplaces, actual workers and consistent inventory accounting.

**Geography and hazards**

- [ ] Add Zamoyos river corridor/delta, Sothoryos littoral, Green Hell and northern island ecology without pretending unknown far south is charted.
- [ ] Make Naath's butterfly fever and Sothoryos disease ecology distinct from each other and from generic biome danger.
- [ ] Assign unique Yeen oily-black-stone ruins and Toad Stone landmark; only geographically attested giant ruins, not widespread copies.
- [ ] Ensure landmass ownership of **Isle of Elephants** and **Isle of Whips** is correct before territorial jurisdiction code is used.
- [ ] Review `ax_ilse` ID migration separately; preserve save compatibility and lookup aliases.

**Global final map review (explicitly deferred)**

- [ ] Import the user's saved script output for **every** SO red X / handwritten name; preserve false positives, uncertain labels and raw source coordinates.
- [ ] Inspect matching on original `so.png` and master atlas, then align detector rows to SO-R research entities.
- [ ] Reconcile **SE-029 Gorosh**, **SE-031 Zamettar**, overlapping regional/shore labels and any Naath/SI duplicate placements.
- [ ] Validate full map projection, coast snapping, spawn-safe terrain elevation, historical ruins, waterways and place footprints.
- [ ] Produce a separate combined `MISSED_FALSE_UNKNOWN_LOCATIONS` batch after the rest of the submaps, rather than prematurely discarding ambiguous SO source points.

## 10. Selected source index (can be rechecked at implementation)

> The links below are a research gateway to named places and footnoted primary works, **not** a substitute for the books/official atlas. The project should retain each entry's provenance and uncertainty when turning this research into data.

- **[S1] Sothoryos** — https://awoiaf.westeros.org/index.php/Sothoryos — cites TWOIAF, TLOIAF, ADWD, F&B; northern trade towns, ruins, Jaenara's journey, ecology, Sothoryi and history.
- **[S2] Basilisk Isles** — https://awoiaf.westeros.org/index.php/Basilisk_Isles — named islands, short-lived pirate town names, Talon/Barter Beach, pirate history, Gogossos and Red Death.
- **[S3] Isle of Tears** — https://awoiaf.westeros.org/index.php/Isle_of_Tears — Gogossos site, bogs/flint hills and historic plague.
- **[S4] Isle of Elephants** — https://awoiaf.westeros.org/index.php/Isle_of_Elephants — Zabhad, elephants, and *shan* with ivory palace.
- **[S5] Naath** — https://awoiaf.westeros.org/index.php/Naath — Peaceful People, Lord of Harmony, coastal raids, silk, fort and historic towns.
- **[S6] Butterfly fever** — https://awoiaf.westeros.org/index.php/Butterfly_fever — Naath-specific disease.
- **[S7] Gogossos** — https://awoiaf.westeros.org/index.php/Gogossos — historic Gorgai, Valyrian penal colony and ruin.
- **[S8] Zamettar** — https://awoiaf.westeros.org/index.php/Zamettar — Ghiscari ruins, Zamoyos delta, Nymeria and jungle growth.
- **[S9] Yeen** — https://awoiaf.westeros.org/index.php/Yeen — black-stone architecture, Zamoyos and unknown disappearance.
- **[S10] Gorosh** — https://awoiaf.westeros.org/index.php/Gorosh — ruined former Ghiscari penal colony on Wyvern Point.
- **[S11] Sothoryi** — https://awoiaf.westeros.org/index.php/Sothoryi — native inhabitants, limited outsider accounts and Trade Talk.
- **[S12] Basilisk Point** — https://awoiaf.westeros.org/index.php/Basilisk_Point — failed Valyrian/Rhoynar settlement history.
- **[S13] Zabhad** — https://awoiaf.westeros.org/index.php/Zabhad — active city, chief Jade Sea port.
- **[S14] Barter Beach** — https://awoiaf.westeros.org/index.php/Barter_Beach — Talon corsair market.
- **[S15] Toad Stone** — https://awoiaf.westeros.org/index.php/Toad_Stone — forty-foot greasy black-stone idol.
- **[S16] Isle of Flies** — https://awoiaf.westeros.org/index.php/Isle_of_Flies — historical Brotherhood of Bones.
- **[S17] Howling Mountain** — https://awoiaf.westeros.org/index.php/Howling_Mountain — historic Nymeria-era pirate activity.
- **[S18] Jade Sea** — https://awoiaf.westeros.org/index.php/Jade_Sea — Isle of Elephants, not continental Sothoryos, and map context.

**Primary material to verify before final implementation:** *The World of Ice & Fire*, chapters “Beyond the Free Cities: Sothoryos,” “The Basilisk Isles,” “Naath,” “The Summer Isles,” and “Ancient History: Ten Thousand Ships”; *The Lands of Ice and Fire* (“The Known World,” “Central Essos,” “The East”); *A Dance with Dragons* (“The Iron Suitor” and book maps); *Fire & Blood* (Ser Eustace Hightower's expedition account).

## 11. Completion status

- [x] Canonical named cities, landmarks, islands, dangerous regions, historical colonies and unnamed inhabited settlement **classes** cataloged.
- [x] Source-supported **298 AC active vs ruined vs historical** distinctions documented.
- [x] Naath, Basilisk Isles, Sothoryos and Zabhad kept politically/economically distinct.
- [x] First-pass [M] population/defense, occupations, agriculture, government and architectural guidelines recorded explicitly as **not canon**.
- [x] Existing repository structures audited for common misclassifications and missing canon sites without editing code prematurely.
- [x] SE cross-map duplicates and future all-map correction tasks documented.
- [ ] Full original SO handwritten-X / detector association, pixel centres and user-script false-positive/missing-point audit — **deferred**.
- [ ] Comprehensive named 298 AC island/tribal/corsair rulers — **not established in sources**, must remain unknown or intentionally mod-authored.
- [ ] Final 1:1 Minecraft coordinates, biome mask, river traces, coastline and structure footprints — **deferred**.
- [ ] Global population balancing, actual residents and implementation Step 2.2 — **future work**.

**Research conclusion:** SO must contain **living Naathi and indigenous Sothoryi communities, shifting pirate economies, roughly a score of hazardous coastal trade towns, the active island port Zabhad, and several distinct monumental ruins**. It must **not** portray all of Sothoryos as abandoned, invent a single Sothoryi empire, populate devastated Ghiscari cities, merge the Isle of Elephants into the mainland, or treat the unknown far-southern coast as surveyed. This document preserves researched worldbuilding for the eventual geographically verified, persistent simulation.
