# Known World — Step 2.1 Research: NE (Northern Essos, Ib and the northern Dothraki Sea)

**Scenario:** 298 AC, book-continuity starting state (do not prematurely apply later *A Dance with Dragons* developments).  
**Source region:** `ne.png` from user-authored geographic overlays.  
**Status:** First researched, versioned scenario-design dossier; **NOT** an authoritative human-population census, finalized coordinates, or executable scenario resource.  
**Last researched:** 2026-10-17.  
**Next stage:** Step 2.2, after cross-submap reconciliation and population-scale calibration.

## 0. Provenance and architectural constraints

- `CANON`: documented in ASOIAF books, *The World of Ice & Fire*, *The Lands of Ice and Fire*, or supported summaries thereof. References are at the end.
- `REPO`: identifier/structure present in `Minecraft_Server_Mod2` as inspected on the development branch/master; existence of an ID does **not** establish final placement or population.
- `INFERENCE`: deductions from climate, geography, history, local ecology or documented commerce.
- `ESTIMATE`: design ranges for later population/workforce/defense generation, not canon. **All numerical counts in this dossier are estimates unless explicitly noted otherwise.**
- Each permanent resident will become an identifiable persistent individual (same person encountered physically, with household/residence/employment/experiences). **No separate anonymous workforce or town population is permitted to act as an independent authoritative simulation.** Aggregate numbers here are *targets* that will be reconciled to those people.
- A city's owner/government is not necessarily a hereditary dynasty. Never insert a made-up House where the source describes councils, priests, dosh khaleen, etc.
- `NE-NNN` identifies a preliminary image observation. Preserve original pixel and edited world placement separately in the later place database. The later red-X/label correction pass is intentionally deferred.
- Religious or cultural trends are *probabilities for generation*, not compulsory attributes for all people. Nomadic visitors remain the same people when entering/leaving a settlement.

## 1. Map observations and 298 AC status

| Marker | Name / historical name | 298 AC state | Provisional permanent residents | Provisional standing defenders | Principal authority |
|---|---|---|---:|---:|---|
| NE-001 | Ib Nor | Active city | 4,000–12,000 | 150–600 | Ibbenese municipal officials under Shadow Council |
| NE-002 | Port of Ibben | Active major port | 35,000–90,000 | 1,000–4,000, plus ships | Shadow Council of Ibben |
| NE-003 | Ib Sar | Active town, Far Ib | 1,500–5,000 | 80–300 | Ibbenese appointed/local officials |
| NE-004 | New Ibbish | Active fortified colony | 3,000–12,000 | 200–800 | Ibbenese colonial officials |
| NE-005 | Vaes Aresak (Ibbish) | Ruined abandoned Ibbenese colony | 0 established | 0 established | No contemporary civic lord |
| NE-006 | Vaes Leisi | Ruins of Ifequevron culture | 0 established | 0 established | No contemporary civic lord |
| NE-007 | Vaes Dothrak | Sacred gathering city | **Unknown small permanent community**; testing envelope 300–3,000 attendants + dosh khaleen; transient crowds may be huge | No conventional standing garrison | Dosh khaleen; khalasars transient |
| NE-008 | Vaes Leqse (Gornath by the Lake) | Sarnori ruins | 0 established | 0 established | No contemporary civic lord |
| NE-009 | Kayakayanaya | Active Hyrkooni fortress city | 8,000–25,000 | 800–3,000 | Great Fathers; warrior-women institutions |
| NE-010 | Yalli Qamayi (Sathar) | Sarnori ruins | 0 established | 0 established | No contemporary civic lord |

**Important correction to earlier chat draft:** 5,000–20,000 was incorrectly characterized as *permanent* population of Vaes Dothrak. Canon distinguishes the dosh khaleen and their servants from transient traders and khalasars. Its permanent baseline remains **unverified**, and transient visitors must be represented by traveling persons, not newly generated local residents [NE-07].

**Scale rule:** The local envelopes above must be reconciled with total world population and supply systems before converting them into exact starting people. Defender ranges are *local organized personnel*, not populations of soldiers invented in a military-only simulation.

## 2. Settlement-specific dossiers
### NE-001 — Ib Nor (`ib_nor`; tentative alias)

- **Historical / 298 AC status [CANON]:** Active Ibbenese north-coast city; primary lore confirms location but little else [NE-01].
- **Government and leading people [CANON/UNKNOWN]:** Council-derived municipal administration. Name of governor is unknown; do not generate a canonical surname.
- **Population influences:** ibbenese dominant; visiting mariners and merchants. Ibbenese local religions are not sufficiently documented to assign an invented named deity; `unknown_religion` fallback until better evidence.
- **Employment / trade [ESTIMATE unless noted]:** INFERENCE: cold-water fishing, forestry, pelts, animal products, coastal transport, whaling-related processing, limited hardy cultivation and trade. Adult active-worker template: 35% fishing/farming/forestry, 25% crafts/extraction, 25% trade/transport, 10% service, 5% government/defense.
- **Land, crops and livestock [INFERENCE]:** INFERENCE: northern sea winds, constrained cultivable pockets, hardy grains where suitable, root crops, grazing sheep/goats, timber, fish and whale products. Proposed relative fertility low–moderate (0.25–0.50); never treat a modeled crop as canon.
- **Future structure palette [CANON + DESIGN]:** Stone-and-timber waterfront; wharf, fish/smokehouses, timber yards, steep or rocky street segments, defensive shoreworks.
- **Step 2.2 integration guard:** Keep settlement distinct from Port of Ibben; sea trade requires actual merchant/crew identities.

### NE-002 — Port of Ibben (`port_of_ibben`; tentative alias)

- **Historical / 298 AC status [CANON]:** Largest Ibbenese port; former God-King palace; whaling, ships, maritime commerce [NE-02][NE-03].
- **Government and leading people [CANON/UNKNOWN]:** Shadow Council chosen by the Thousand; guild/noble/priest representation; no living God-King in 298 AC. Officials unknown.
- **Population influences:** ibbenese core; foreign sailors/traders concentrated around dock precincts, with restricted access elsewhere. Local faith uncertain.
- **Employment / trade [ESTIMATE unless noted]:** INFERENCE/LORE: whaling, seal products, shipbuilding, whale oil, amber, pelts, timber, metal imports/exports. Adult workforce template: 12% primary extraction/fishing, 30% processing/crafts/shipyards, 35% trade/transport, 15% service, 8% civic/defense.
- **Land, crops and livestock [INFERENCE]:** Mostly port provisioning; nearby farms must use land capability. City likely imports part of its food. Cattle, sheep, fishing, cold-climate grains/root produce as **inference**; fertility 0.20–0.45 on steep island terrain.
- **Future structure palette [CANON + DESIGN]:** Large steep harbor city, whale-oil lights, warehouses, shipyards, cobbles, slopes, waterfront customs districts; monumental **ruined** God-King palace.
- **Step 2.2 integration guard:** No independent royal House; fleet crew and workshop workers must be encounterable people.

### NE-003 — Ib Sar (`ib_sar`; tentative alias)

- **Historical / 298 AC status [CANON]:** Only town on Far Ib; historic place of penal exile, practice ended after God-Kings [NE-04].
- **Government and leading people [CANON/UNKNOWN]:** Ibbenese municipal authority; individual governor unrecorded.
- **Population influences:** ibbenese dominant; historical exiles should shape backstories, not imply all current residents are criminals.
- **Employment / trade [ESTIMATE unless noted]:** INFERENCE: fisheries, coastal shipping, wood/stone works, local hunting. Adult workforce template: 40% primary production, 25% craft/extraction, 20% shipping/trade, 10% service, 5% civic/defense.
- **Land, crops and livestock [INFERENCE]:** Rocky island, marine resources, scant arable pockets; grazing goats/sheep; hardy vegetables and grain in sheltered sites. Fertility 0.15–0.40.
- **Future structure palette [CANON + DESIGN]:** Compact exposed town with harsh-weather roofs, protected landing, old penal architecture, small watch fort.
- **Step 2.2 integration guard:** Existing Far Ib geographic reference should determine location affinity, not nearest larger city.

### NE-004 — New Ibbish (`new_ibbish`; tentative alias)

- **Historical / 298 AC status [CANON]:** Post-Ibbish mainland Ibbenese settlement, fortified colony on forested peninsula [NE-05][NE-06].
- **Government and leading people [CANON/UNKNOWN]:** Ibbenese colonial command subordinate to Shadow Council; no invented king or house.
- **Population influences:** ibbenese strong; frontier peoples/visitors possible, but affiliation should reflect actual family migration.
- **Employment / trade [ESTIMATE unless noted]:** INFERENCE: timber, forestry, pelts, fishing, coastal supplies, small homesteads, frontier fortification. Workforce template: 40% forestry/fishing/farming, 23% crafts, 20% trade/transport, 8% services, 9% defense/administration.
- **Land, crops and livestock [INFERENCE]:** Cold coast/forest; some small farms and pastures, variable soil; fertility 0.20–0.50 depending on cleared land.
- **Future structure palette [CANON + DESIGN]:** Distinct timber defensive palisade/rampart, watchtowers, ditch, sheltered inner settlement, harbor and logging outposts.
- **Step 2.2 integration guard:** Do not duplicate inhabitants of destroyed Ibbish.

### NE-005 — Vaes Aresak / Ibbish (`vaes_aresak`; tentative alias)

- **Historical / 298 AC status [CANON]:** Former white-walled harbor city with Whalebone Gates, burned/abandoned under Dothraki pressure [NE-06].
- **Government and leading people [CANON/UNKNOWN]:** No normal city authority or garrison; historians may distinguish past Ibbenese government.
- **Population influences:** Historical ibbenese, not a current default living culture.
- **Employment / trade [ESTIMATE unless noted]:** RUIN: no standing economy. Salvagers, temporary camps or visitors need actual person identities and event justification.
- **Land, crops and livestock [INFERENCE]:** Former port and timber resource landscape; avoid automatically planting productive farms inside city ruins.
- **Future structure palette [CANON + DESIGN]:** Collapsed white walls, whalebone monumental gates, ruined harbor, temples/treasuries, overgrowth.
- **Step 2.2 integration guard:** `status=RUIN`, `canGeneratePopulation=false` until explicit event.

### NE-006 — Vaes Leisi (`vaes_leisi`; tentative alias)

- **Historical / 298 AC status [CANON]:** Ruined carved-tree/grotto settlement of the Ifequevron [NE-07].
- **Government and leading people [CANON/UNKNOWN]:** No known present-day municipality.
- **Population influences:** Historical Ifequevron culture is **not** equivalent to modern `ibbenese`; no matching current culture ID must be invented as established fact.
- **Employment / trade [ESTIMATE unless noted]:** RUIN: no autonomous living village economy.
- **Land, crops and livestock [INFERENCE]:** Dense forest, wildlife, potential timber in surrounding zone; preserve sacred ruins.
- **Future structure palette [CANON + DESIGN]:** Ancient carved tree structures, grottos, overgrown walks, forest ruins.
- **Step 2.2 integration guard:** No dummy farmer families simply because the map has a red X.

### NE-007 — Vaes Dothrak (`vaes_dothrak`; tentative alias)

- **Historical / 298 AC status [CANON]:** Sacred unwalled meeting city; dosh khaleen permanently dwell with attendants; visiting khalasars [NE-08][NE-09].
- **Government and leading people [CANON/UNKNOWN]:** Dosh khaleen religious authority, not a crowned king. Each visiting khal authority follows their actual khalasar.
- **Population influences:** dothraki dominant in rituals and visitors; foreign market traders and enslaved attendants are individuals. `great_stallion` core faith; multiple foreign faiths among travelers.
- **Employment / trade [ESTIMATE unless noted]:** Permanent work: attendants, markets, animal care, religious functions; temporary trade and military movement drive changes. Do **not** attach a fixed cavalry garrison or thousands of permanent farmer records.
- **Land, crops and livestock [INFERENCE]:** Steppe grazers, horses, sheep/goats and imported market food; no standardized fixed wheat-farm village sprawl. Local growing potential limited and highly location-dependent.
- **Future structure palette [CANON + DESIGN]:** Vast unwalled plan, Horse Gate, Godsway, eastern/western markets, widely spaced eclectic pavilions and imported styles, Womb of the World access.
- **Step 2.2 integration guard:** Track residence vs current location vs temporary encampment; prohibit permanent fictitious khals.

### NE-008 — Vaes Leqse / Gornath (`vaes_leqse`; tentative alias)

- **Historical / 298 AC status [CANON]:** Destroyed Sarnori canal city on small lake [NE-10].
- **Government and leading people [CANON/UNKNOWN]:** No active King of Gornath.
- **Population influences:** Historical `sarnori`.
- **Employment / trade [ESTIMATE unless noted]:** RUIN: no normal farm/market/garrison. River and lake outlying hamlets require distinct identities.
- **Land, crops and livestock [INFERENCE]:** Former canal-fed cultivation possible; nearby survivable patches should be evaluated separately.
- **Future structure palette [CANON + DESIGN]:** Ruined canal grid, embankments, lakefront docks, overgrowth and abandoned masonry.
- **Step 2.2 integration guard:** Crossmap duplicate observation in NWE; single canonical place ID.

### NE-009 — Kayakayanaya (`kayakayanaya`; tentative alias)

- **Historical / 298 AC status [CANON]:** Fortress city on Steel Road; eastern Bone Mountains. Great Fathers and warrior-women institutions [NE-11].
- **Government and leading people [CANON/UNKNOWN]:** Great Fathers govern the sister cities. Individual officeholders unknown.
- **Population influences:** `hyrkooni` predominant; caravan minorities; faith uncertain: use `unknown_religion` unless supported by later sources. Do not map every resident to old-gods or Seven.
- **Employment / trade [ESTIMATE unless noted]:** INFERENCE: mountain caravan tariffs, metal/stone craft, trade services, defense, constrained farming; workforce: 22% primary, 25% craft/extraction, 25% trade/caravan, 12% services, 16% civic/defense.
- **Land, crops and livestock [INFERENCE]:** Narrow valley/terrace plots, pack animals and managed fodder, mountain goats/sheep and hardy grains; fertility 0.15–0.40.
- **Future structure palette [CANON + DESIGN]:** Monumental black basalt/iron/bone fortifications, steep terracing, guarded caravan gates, secure markets and mountain passes.
- **Step 2.2 integration guard:** Warrior roles belong to actual people and cultural institutions; do not make all defenders male.

### NE-010 — Yalli Qamayi / Sathar (`yalli_qamayi`; tentative alias)

- **Historical / 298 AC status [CANON]:** Ruined Sarnori Waterfall City destroyed during Dothraki conquest [NE-12].
- **Government and leading people [CANON/UNKNOWN]:** No living Sarnori king established at the ruins.
- **Population influences:** Historical `sarnori`; traveling Dothraki may pass.
- **Employment / trade [ESTIMATE unless noted]:** RUIN: no automatic population, active town economy or settled garrison.
- **Land, crops and livestock [INFERENCE]:** Rivers/confluence, potential fertile riparian valleys. Nearby farms must be distinct authored/generated settlements.
- **Future structure palette [CANON + DESIGN]:** Destroyed monumental river city with waterfall, bridges, damaged waterways and collapsed terraces.
- **Step 2.2 integration guard:** Crossmap duplicate observation in NWE; preserve one identity.

## 3. Regional influence dossiers (not a replacement for geography masks)

| Zone or proposed subzone | Current geographic basis | Culture/religion model | Land/economy | Procedural settlement policy |
|---|---|---|---|---|
| Ib main island | `ib_region` polygon + coast/topography | `ibbenese` dominant; local faith unresolved | Fisheries/whales, furs, woodland, gold/iron/tin [NE-03] | Fishing ports, logging hamlets, rare cold-climate farms in viable valleys |
| Far Ib | `far_ib` location (polygon must be verified/refined) | `ibbenese` dominant | Rocky shore, fisheries, small pastures | Tiny towns, sheltered coves; don't project Ib Nor's urban density |
| Ibbenese mainland peninsula | New Ibbish geographic feature; requires smaller polygon | Ibbenese enclave within varied frontier | Woodland, fishing, limited farming | Fortified colonies and dispersed forestry sites |
| Former Ifequevron forests | Geographic research needed, not a blanket cultural polygon | Historical Ifequevron; current faith unknown | Dense woodland, uncertain habitation | Mostly wilderness, isolated historical ruins, no invented present-day national government |
| Northern Dothraki steppe | `dothraki_sea_region` polygon | `dothraki` and `great_stallion` as *defaults for khalasar members*, not all fixed residents | Pastoralism, horse herds, moving camps | Mobile groups, seasonal tent circles and trading camps; not vanilla field villages |
| Former Sarnori lake/river cities | `kingdom_of_sarnor` location; boundaries require review | `sarnori` historic; no automatic contemporary urban population | Fertile river margins but extensive ruins | Protect ruins; generate tiny separate living enclaves only where justified |
| Kayakayanaya/Bone frontier | `bone_mountains` and Steel Road corridor | `hyrkooni`; belief-specific proportions unresolved | Caravan tariffs, stone/metal, mountain pasture | Fortified pass towns, guarded inns, scarce terraced villages |

### Regional generation algorithm (design, not yet code)
1. Determine true geographical cell: landmass + territory polygon + local features/biome/river access. If the needed finer polygon does not exist, mark its influence as `UNRESOLVED` rather than pretend it exists.
2. Identify nearest **viable site** (not nearest named city by straight-line distance). Roads, water, terrain and trade access modify settlement viability.
3. Generate households and **individually persistent people** from location/region conditional distributions; generate culture/religion per person and keep exceptions and migration.
4. Assign farming, fishing, whaling, mining, defense and markets to real holdings/organizations and actual person IDs. No shadow numerical workforce.
5. Treat military as actual service membership, not a second population; travel/nomad groups carry identities and change positions, not counts.

## 4. Initial repo integration audit

**Inspected resources:** `reference/locations.json`, `reference/territory_zones.json`, `reference/cultures.json`, `reference/religions.json`, `scenarios/agot_298_ac/landed_holdings.json`.

- Relevant extant culture IDs: `ibbenese`, `dothraki`, `sarnori`, `hyrkooni`.
- Relevant extant religion IDs: `great_stallion`, `unknown_religion`. There is no dedicated canonically substantiated Ibbenese local deity in the inspected list: do not fabricate one under the guise of known lore.
- Relevant extant places/zones: `ib_region`, `far_ib`, `bone_mountains`, `kingdom_of_sarnor`, `dothraki_sea_region`. Not all minor places have complete authored location/holding IDs; check `locations.json` before creating new identifiers.
- Settlement roles must be separate from map-marker category: Vaes Dothrak is sacred/gathering, ruins remain ruins, and Kayakayanaya is a fortress city.
- Do not assign a current hereditary noble family to the Shadow Council, dosh khaleen or Great Fathers unless a separate canonical fact supports it.

## 5. Outstanding research / later shared correction pass

- [ ] Verify image X centres, text association, NE/NWE duplicate observations and local buildable terrain only during consolidated image-correction stage.
- [ ] Calibrate Ibbenese total population and maritime fleet capacity against full-world estimates; numbers here are generation envelopes only.
- [ ] Establish individually identifiable members of Shadow Council, dosh khaleen and Great Fathers *where known*; otherwise generate explicitly provisional officeholders without invented house claims.
- [ ] Distinguish Vaes Dothrak's permanent attendants from traveling merchant convoys/khalasars with an actual travel system.
- [ ] Validate island fertility, local climate and agriculturally usable hectares before crop and livestock production.
- [ ] Preserve ruined monuments as special architecture; never place generic inhabited villages in active ruin footprints.

## 6. Sources / research trace

Primary works cited through A Wiki of Ice and Fire's summaries: *The World of Ice & Fire*, *The Lands of Ice and Fire*, *A Game of Thrones* and *A Dance with Dragons*. Entries are secondary summaries; do not treat this as an exhaustive primary-text verification.

- [NE-01] [Ib Nor](https://awoiaf.westeros.org/index.php/Ib_Nor)
- [NE-02] [Port of Ibben / Ib](https://awoiaf.westeros.org/index.php/Ib)
- [NE-03] [Ibben: politics and natural resources](https://awoiaf.westeros.org/index.php/Kingdom_of_Ib)
- [NE-04] [Ib Sar](https://awoiaf.westeros.org/index.php/Ib_Sar)
- [NE-05] [New Ibbish](https://awoiaf.westeros.org/index.php/New_Ibbish)
- [NE-06] [Vaes Aresak / Ibbish](https://awoiaf.westeros.org/index.php/Vaes_Aresak)
- [NE-07] [Vaes Leisi](https://awoiaf.westeros.org/index.php/Vaes_Leisi)
- [NE-08] [Vaes Dothrak](https://awoiaf.westeros.org/index.php/Vaes_Dothrak)
- [NE-09] [Dosh khaleen; permanent residency](https://awoiaf.westeros.org/index.php/Dosh_khaleen)
- [NE-10] [Vaes Leqse / Gornath](https://awoiaf.westeros.org/index.php/Vaes_Leqse)
- [NE-11] [Kayakayanaya](https://awoiaf.westeros.org/index.php/Kayakayanaya)
- [NE-12] [Kingdom of Sarnor and Sathar](https://awoiaf.westeros.org/index.php/Kingdom_of_Sarnor)
- [NE-13] [Steel Road](https://awoiaf.westeros.org/index.php/Steel_Road)
- [NE-14] [God-King of Ib and later Shadow Council](https://awoiaf.westeros.org/index.php/God-King_of_Ib)
