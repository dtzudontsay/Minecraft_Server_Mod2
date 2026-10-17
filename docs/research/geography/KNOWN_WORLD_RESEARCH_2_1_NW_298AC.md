# Known World — Step 2.1 / NW — Northern Westeros (298 AC)

**Status:** Comprehensive *recognizable-site and regional* research dossier; original individual red-X pixel annotations not authenticated and deliberately deferred to user's final Python mismatch pass. **No source marker numbers, pixel coordinates or runtime population were invented.**  
**Scenario:** Book timeline, 298 AC at Robert Baratheon's arrival in Winterfell.  
**Map:** `authoring/maps/Realms/nw.png` (2073 × 1966), calibrated by `MapZoneRegistry`; overlaps `sw.png` at the southern edges.  
**Repo:** `dtzudontsay/Minecraft_Server_Mod2`, `master` (October 17, 2026).  
**Research evidence:** Books, *The World of Ice & Fire*, *The Lands of Ice and Fire* and linked source indices, cross-checked with Java feature registry, scenario holdings, reference catalogs and simulation architecture.  
**Notation:** `[C]` book/official-cartography support; `[R]` actual project file; `[I]` environmental/social inference; `[M]` explicit non-canonical mod estimate; `[U]` unresolved; `[A]` adaptation or game-derived, not primary novel canon. A place can have a canonical name and an unknown current ruler.

## 0. Preservation and Step 2.2 rules

- One actual person has one persistent identity; civilian, farmer, lord, soldier, sailor, traveler, prisoner and household member are *roles or states of the same person*. Cohort counts must decrease when persons are individually materialized; no independently simulated ghost population.
- Geographic features, named world locations, active settlements, historic ruins, ownership holdings, governments, houses, title-holders and physical structures are different objects and must be linked by verified IDs rather than collapsed into one generated village.
- `[R]` `ownerDynastyId` does not prove a known named living lord. `[R]` `taxBase`, `populationWeight`, `militaryValue` are placeholders and **not** headcount, money, crops or troop strength.
- [M] Population/job/defense/acreage ranges are authoring envelopes and subject to North-wide and full-world food, labor, household and military calibration. Garrison troops are a *subset* of resident/posted individuals.
- Historic events are time-tagged: no Robb kingship, Bolton occupation, Greyjoy invasion, Red Wedding, or Jon Snow Lord Commandership preloaded at day 0.
- Coordinate data remain `null` until original detected X and hand-label are reconciled to a canonical location, map projection, final ground position and construction footprint. Do not automatically create 298 AC populations for ruins, empty Watch forts, or region/river names.
- The later shared corrections file retains missing, duplicate, ambiguous and false red-X matches. This NW document covers recognizable names and zones **without claiming the complete marked-image inspection**.

## 1. Northern Westeros institutional overview

**The North:** Largest of the Seven Kingdoms' constituent regions, ruled by House Stark from Winterfell. Eddard Stark is Lord of Winterfell and Warden of the North in the opening scenario. North's main city is White Harbor; Barrowton and Winterfell's winter town are significant towns. Notable bannermen include Bolton, Cerwyn, Dustin, Flint, Glover, Hornwood, Karstark, Manderly, Mormont, Reed, Ryswell, Tallhart and Umber. [C][S1][S4]

**Watch lands:** The Night's Watch holds the Gift south of the Wall, including Brandon's Gift and the New Gift. Its nineteen historically built Wall castles have dwindled to **three staffed in 298 AC**: Castle Black (roughly 600 brothers), Shadow Tower (roughly 200) and Eastwatch-by-the-Sea (fewer). Entire order fewer than 1,000: **do not add a separate 1,000-member army** to the Watch members or create garrisons for sixteen abandoned castles. Watch governance follows its Lord Commander, not Stark feudal succession. [C][S2][S5]

**Beyond the Wall:** Free Folk communities and diverse clans/bands beyond feudal southern jurisdiction, including mountain groups, forest households, nomadic war bands and remote shore populations. Mance Rayder has brought together followers around the story era, but his movement should not be modeled as a single permanent feudal city. Treat local leaders, law, beliefs and social structures individually; no automatic Stark taxation. [C][S6]

**Skagos:** Remote Northeastern island within nominal northern political sphere, with House Crowl at Deepdown, House Magnar at Kingshouse and House Stane at Driftwood Hall. Exact 298 AC heads unrecorded; do not invent them. Cannibalism/unicorn stories are **rumor/contested tradition**, not universal NPC behavior or guaranteed mobs. [C][S7]

**Bear Island:** House Mormont, northwestern Bay of Ice; Maege Mormont is 298 AC head of house. Wooded/rocky island, fish and maritime dependencies; castle seat's scenario ID is `hall_of_mormonts_domain`. Separate island population from the castle precinct. [C][S1][S8]

**Three Sisters:** Sweetsister, Longsister and Littlesister lie in the Bite, south of White Harbor. House Sunderland rules under *House Arryn of the Vale* (often loose political control), with Borrell, Longthorpe and Torrent as island vassals. Fishing, crab, goat cheese, beacons and ship traffic with White Harbor are sourced; **NW geographic presence does not mean Stark possession**. [C][S9]

## 2. Zone research: environmental constraints, regional social systems and Step 2.2 effects

**Zone envelopes below represent terrain/landscape, NOT independent resident cities or duplicate territorial owners.** [I] ecological suitability isn't evidence a specific farm already grows the listed crop.

| Zone | Geography/environment | Human distribution, ownership and 298 AC state | Livelihoods, construction, routes and generation constraints |
|---|---|---|---|
| Lands of Always Winter | Remote frozen/arctic landscape [C]; extent poorly known | [U] settlement census; no verified functioning kingdom | Ice, snow, high travel risk, extremely restricted cultivation; keep magic and beings separated from ordinary population |
| Frostfangs | High western mountain range [C] | Free Folk mountain clans and travelers | Valleys, grazing [I], rocky passes, winter blockade, caves; no universal feudal castles |
| Haunted Forest | Woodlands north of the Wall [C] | Diverse Free Folk families/bands and scattered homesteads | Hunting, timber, foraging [I], sparse camps, mobility constrained by trees/snow |
| Frozen Shore | Cold western northern coast | Shore communities under local leaders | Fishing/sealing [I], storm/ice hazards, few protected harbors |
| Milkwater / mountain approaches | River and valley network | Free Folk camp locations shift | Actual river crossing, passes, water and camp provisioning; river is not a town |
| Bay of Ice | Large northwestern bay | Northern island/coastal inhabitants | Ship travel, sea ice/storm risk, fish supply, coastal landing validations |
| The Wall | Massive engineered ice defensive structure | Night's Watch institution | Distinct segments/gates/manned forts, rations, patrols, buildings, ravens; no generic wall-aligned cities |
| Brandon's Gift | Old endowed Watch land | Watch legal holding, depleted farm settlements | Farming/abandonment history, winter supplies, no automatic Stark taxes |
| New Gift | Wider endowed land south of Wall | Watch jurisdiction and scattered residents | Sparse farms, villages, fallow land, road supply to Watch forts |
| Northern Mountains | Highlands south of Wall | Flint of mountain branches; Wull/Liddle/Norrey and other clans | Pasture, valleys, slate/stone and timber [I], slow recruitment, clan law |
| Wolfswood | Vast Northwestern forest | Stark/House Glover dependencies | Timber, hunting, logging workplaces, road clearance; restrict farm clearing |
| Sea Dragon Point | Rugged wooded peninsula | Low-density Northern territory | Forest/coastland, no large automatic cities, sea approach |
| Stony Shore | Rugged west coast | Fishing villages and Northern minor localities | Fisheries, poor farm pockets, exposure to coastal raids |
| The Rills | Western plains/grasslands | House Ryswell domain | Horses, grazing, farming [I], dispersed estates and track routes |
| Barrowlands / Barrowton | Ancient barrows and western lowlands | House Dustin; active market town | Agricultural villages [I], cattle/horses, markets, burial mounds as protected archaeology |
| The Neck | Marshland and bogs, narrow causeways | House Reed and crannogmen; Northern frontier | Raised crannogs, bog ecology, constrained farming, trails, defensive choke at Moat Cailin |
| Central North / Winterfell | Fields/forests and inland roads | Stark domain and rural household network | Winter grain/storage, winter town, castle hot springs, kingsroad and stables |
| White Knife basin / Long Lake | Waterways and river valleys | Rural northern households, Manderly zone to south | Rivers as real lines, fishing, farms where soil allows, goods moving to White Harbor |
| Hornwood forest | Woodland and inland rivers | House Hornwood | Wood, estates, hunting/forestry and small fields [I] |
| Dreadfort / Weeping Water / Last River | Eastern river valleys and estate landscapes | House Bolton and adjacent northern houses | Riverine farms, forest extraction, castle and routes, feudal dues and historical Stark–Bolton rivalries |
| Grey Cliffs / Sheepshead Hills / Broken Branch | Northeastern cliffs, hill river systems | Locke/Flint and smaller households | Exposed coast, pasture/hill farming [I], river mouths, harbors only on actual suitable shore |
| Skagos and Skane | Treacherous rocky remote islands | Crowl/Magnar/Stane; Skane status uncertain | Small fishing/foraging/animal husbandry [I], hazardous shipping, distinctive culture; island masks separate |
| Bear Island | Northwestern island forests/coast | Mormont households | Fish, timber, limited agriculture [I], sea logistics, defensive organization |
| The Bite and Three Sisters | Northern maritime islands in sea between Vale/North | **Vale** Sunderland and vassals | Seafood/goats/ship traffic, nighttime navigation and wreck risks, low island arability |
| The Fingers / Paps / Pebble | Vale's coastal headlands/islets visible on northern map | Vale, not North; exact local jurisdiction varied | Coastal elevation and travel, no automatic Stark settlement |
| Kingsroad and major routes | Long north-south road through North | Mixed holdings and Watch access | Actual edge/path graph, trade/military itineraries, road-maintenance/inn network, no duplicate travelers |
| Southern map edge / The Twins | Riverlands bridge-fortress border overlap | House Frey under Riverlands, not Northern bannerman | Duplicate with SW resolved to one crossing/castle, trade tolls and road control |

### 2.1 Terrain suitability envelopes — [M] *calibration proposals only*

| Archetype | Farmable suitability (0–1) | Pasture suitability | Forestry suitability | Economic constraint |
|---|---:|---:|---:|---|
| Always Winter / deep ice | 0–0.05 | 0–0.10 | 0–0.05 | Winter starvation/travel; no unconditional crops |
| Frostfangs / mountains | 0.02–0.20 | 0.10–0.50 | 0.05–0.40 | Limited traversable valley pockets |
| Dense Northern forest | 0.05–0.30 | 0.05–0.35 | 0.65–0.95 | Transport and clearing cost |
| Stony west coast | 0.05–0.30 | 0.15–0.45 | patchy | Fisheries and sea transport |
| Rills / Barrowlands | 0.30–0.70 | 0.50–0.90 | 0.10–0.35 | Seasonal transport and harvest |
| Winterfell and central North | 0.25–0.65 | 0.30–0.65 | 0.25–0.65 | Winter stores and fuel |
| White Knife floodplain | 0.35–0.75 | 0.30–0.65 | 0.20–0.60 | Flood/water quality and navigability |
| The Neck | 0.02–0.25 | 0.05–0.20 | 0.10–0.55 | Waterlogged bog/causeway transport |
| Rugged northern islands | 0.05–0.35 | 0.15–0.50 | variable | Shipping and shelter |

This table is **not** `agricultural_capacity.json`, not real hectares, and not a crop/yield schedule. Rye/oats/barley, pulses and hardy roots are *proposed ecological candidates* for some cultivated northern regions. Do not treat their exact prevalence as canon. Actual yields must be produced from specific land, farmers, tools/seeds, weather, season and inventories. Northern seasonal winter impacts can persist for years; do not impose fixed vanilla crop seasons.

## 3. Existing Northern holdings: precise repository association and 298 AC research

The current `scenarios/agot_298_ac/landed_holdings.json` already contains these **Northern and geographically overlapping Three Sisters** records. The `ownerDynastyId` entries are **[R] scenario links**, while holder names/status need separate source verification. Not all are printed X sites. Name recognition is not exact image-label reconciliation.

| Holding/seat | Existing holding ID | House/current 298 AC information | Economic/cultural/physical Step 2.2 guidance |
|---|---|---|---|
| North overall | `north_domain` | Stark; Eddard Stark `holderNpcId` [R/C] | Regional jurisdiction, NOT separate castle resident population |
| Winterfell | `winterfell_domain` | Stark; Eddard Stark [R/C] | Double defensive walls, great hall, godswood/crypts, hot springs, castle stores; separate winter town |
| Barrow Hall / Barrowton | `barrow_hall_domain` | Dustin; Barbrey Dustin [C] | Market/burial mounds, fields and horses [I], separate town population |
| Blackpool | `blackpool_domain` | Slate [R], head [U] | Minor Northern estate/seat, location status verify |
| Breakstone Hill | `breakstone_hill_domain` | Flint of Breakstone Hill [R], head [U] | Highland territory and pasture, not entire mountain range |
| Castle Cerwyn | `castle_cerwyn_domain` | Cerwyn; incumbent chronology requires check | Central North farmland and nearby winter/supply road |
| Deepwood Motte | `deepwood_motte_domain` | Glover [C], individual holder [U] | Timber and local forest hamlets; clearing/outworks |
| Dreadfort | `dreadfort_domain` | Bolton; Roose Bolton [C] | Castle and larger feudal eastern estate, historic feud vs Stark |
| Dormand Hall | `dormand_hall_domain` | Dormand [R], head [U] | Minor castle; little evidence about demography |
| Flint's Finger | `flints_finger_domain` | Flint of Flint's Finger [R/C] | West rocky coast defense and fishing [I] |
| Goldgrass | `goldgrass_domain` | Stout [R], head [U] | Dustin-related minor vassal to verify; farms/grazing [I] |
| Greywater Watch | `greywater_watch_domain` | Reed; Howland Reed [C] | Reed floating/moving crannog tradition; swamp rather than square stone fort |
| Hall of Mormonts / Bear Island | `hall_of_mormonts_domain` | Mormont; Maege Mormont [C] | Rocky/wooded island, sea travel, fishing and defense |
| Hornwood | `hornwood_domain` | Hornwood; Halys Hornwood [C] | Forest, local villages, estate farming and retainers |
| Karhold | `karhold_domain` | Karstark; Rickard Karstark [C] | Eastern Northern stronghold, long travel and militia |
| Last Hearth | `last_hearth_domain` | Umber; Greatjon Umber [C] | Uppermost major northern seat, cold/livestock/muster |
| New Castle (White Harbor) | `new_castle_domain` | Manderly; Wyman Manderly [C] | Separate castle within/over city; city market/harbor population independent |
| Northern Mountains | `northern_mountains_flint_domain` | Mountain Flint [R], head [U] | One clan territory, no monopoly over all highlands |
| Oldcastle | `oldcastle_domain` | Locke [C], head [U] | Southeast North; farms and potential coastal supply |
| Rills | `rills_domain` | Ryswell; Rodrik Ryswell [C] | Horse and pasture region with separate dispersed farms |
| Torrhen's Square | `torrhens_square_domain` | Tallhart; Helman Tallhart [C] | Fortified small urban castle/lakeside estates |
| Widow's Watch | `widows_watch_domain` | Flint of Widow's Watch [C], head [U] | Coastal peninsula, fishing and cliffs |
| Deepdown, Skagos | `deepdown_domain` | Crowl [C], head unknown | Isolated Skagosi local settlement, remote coastal economy |
| Driftwood Hall, Skagos | `driftwood_hall_domain` | Stane [C], head unknown | Rocky fishing island settlement |
| Kingshouse, Skagos | `kingshouse_domain` | Magnar [C], head unknown | Skagosi `magnar` lordship, no fictional named head |
| Ironrath | `ironrath_domain` | Forrester [A/R], book status not established | **Telltale/adaptation association**, not book-confirmed X site |
| Highpoint | `highpoint_domain` | Whitehill [A/R], book status not established | **Telltale/adaptation association**, cannot silently canonize |
| Rillwater Crossing | `rillwater_crossing_domain` | Glenmore [A/R], book status not established | **Telltale/adaptation association**, map-match unverified |
| Three Sisters overall | `three_sisters_domain` | Sunderland; Triston Sunderland [C] | **Vale** hierarchy `vale_domain` in repo, not `north_domain` |
| Littlesister | `littlesister_domain` | Torrent [C] | Island subholding, its residents included in 3 Sisters total |
| Longsister | `longsister_domain` | Longthorpe [C] | Island subholding, fish/ship culture |
| Breakwater / Sweetsister | `breakwater_domain` | Borrell; Godric Borrell [C] | Night Lamp, Sisterton proximity, shipping defenses |

**Audit protection:** `landed_holdings.json` records do not establish the exact site X, a census, current physical population or an actually instanced workplace. Place types like CASTLE/TERRITORY refer to the holding, not the number of buildings or people. House heads in the table where named are historical source associations; names should be date-checked at the exact first tick before binding to `holderNpcId`. The Isles' current parent holdings are Vale. [S9]

## 4. Active and historic settlements/landmarks not exhausted by the existing holding list

| Site | 298 AC status / connection | Economic and physical implications |
|---|---|---|
| White Harbor | Active North's single city, ruled by Manderlys [C] | Major port on White Knife/Bite; docks, markets, merchants, ships, guild artisans, Faith of Seven and old gods, actual seafreight |
| Winter town | Seasonal town adjacent to Winterfell [C] | Independent built-up footprint but economically attached to Stark castle, winter residents/markets and special heating/storage |
| Barrowton | Active Northern town under Dustin [C] | Houses and shops, horses and roads, river/barrow historic sites |
| Moat Cailin | Ruined ancient fortress guarding Neck [C] | Defensible choke/causeway, does not default to city workforce or standing 298 AC garrison |
| Mole's Town | Inhabited village south of Castle Black [C] | Village market, inn/trade, civilians not Watch brothers; stores and roads |
| Craster's Keep | Remote homestead beyond Wall [C] | Small exceptional household under Craster, Free Folk cultural and legal specificity; not standardized clan form |
| Fist of the First Men | Ancient hilltop ringfort [C] | Historic ruin, temporary expedition camp possible, no automatic permanent 298 settlement |
| Hardhome | Destroyed abandoned Free Folk settlement [C] | Historical ruins and eerie traditions, not a bustling port or neutral Westerosi barony |
| Queenscrown | Abandoned ruined tower [C] | Site/legend landmark, no independent town income |
| Bridge of Skulls | Remote crossing/pass near western Wall [C] | Actual crossing feature and patrol/military supply bottleneck |
| Sisterton | Inhabited town on Sweetsister [C] | Island trade, fisheries, lodgings, beacons, House Sunderland sphere |
| Night Lamp | Beacon maintained by Borrell [C] | Operating maritime warning infrastructure; potential deceptive individual criminal acts (not universal culture) |
| Ramsgate | Northern eastern coastal locality [C] | Eastern North place/holding match still unresolved; avoid invented lord |
| Long Lake | Northern inland lake [C] | White Knife source, lakeshore water/fish and freshwater routing |
| The Twins | Riverlands House Frey castle/bridge [C] | Geographically visible NW boundary, physically deduplicate with SW, North does not own it |
| The Fingers / Pebble / The Paps | Vale peninsulas and islands [C] | Terrain labels, not Northern settlements or Stark taxes |
| Gorne's Way | Legendary passage below Wall [C as legend] | Belief vs verified geography, no automatic tunnel built if the map cannot establish it |

### 4.1 All existing 45 official-map geographic IDs in `NorthFeatureRegistry`

`lands_of_always_winter`, `frostfangs`, `haunted_forest`, `milkwater`, `frozen_shore`, `bay_of_ice`, `bear_island`, `the_wall`, `castle_black`, `brandons_gift`, `the_new_gift`, `skane`, `skagos`, `bay_of_seals`, `sea_dragon_point`, `wolfswood`, `stony_shore`, `the_rills`, `barrowtown`, `blazewater_bay`, `saltspear`, `cape_kraken`, `barrowlands`, `kingsroad`, `the_neck`, `winterfell`, `lonely_hills`, `last_river`, `dreadfort`, `weeping_water`, `grey_cliffs`, `sheepshead_hills`, `broken_branch`, `white_knife`, `white_harbor`, `moat_cailin`, `the_bite`, `littlesister`, `longsister`, `sweetsister`, `pebble`, `the_paps`, `three_sisters`, `the_fingers`, `the_twins`.

Additional river/coastal features must remain geometries rather than resident placeholders. `blazewater_bay`, `saltspear`, `cape_kraken`, `lonely_hills`, `last_river`, `broken_branch`, `grey_cliffs`, `sheepshead_hills`, `the_bite` and `the_paps` are individually recognized geography but do not justify inventing same-named civic rulers or garrisons. All 45 IDs are explicitly preserved for data joins.

## 5. All nineteen Night's Watch castles, west to east

The 19 were historically raised along the Wall; **only three** remain manned. [C][S2][S5]

| # | Historical castle | Scenario 298 AC initial occupation |
|---:|---|---|
| 01 | Westwatch-by-the-Bridge | Abandoned |
| 02 | Shadow Tower | **ACTIVE**, ~200 Watch brothers; Commander Denys Mallister |
| 03 | Sentinel Stand | Abandoned |
| 04 | Greyguard | Abandoned |
| 05 | Stonedoor | Abandoned |
| 06 | Hoarfrost Hill | Abandoned |
| 07 | Icemark | Abandoned |
| 08 | Nightfort | Abandoned, ancient castle, Black Gate |
| 09 | Deep Lake | Abandoned |
| 10 | Queensgate / Snowgate | Abandoned |
| 11 | Castle Black | **ACTIVE**, ~600 brothers; Lord Commander Jeor Mormont |
| 12 | Oakenshield | Abandoned |
| 13 | Woodswatch-by-the-Pool | Abandoned |
| 14 | Sable Hall | Abandoned |
| 15 | Rimegate | Abandoned |
| 16 | Long Barrow | Abandoned |
| 17 | Torches | Abandoned |
| 18 | Greenguard | Abandoned |
| 19 | Eastwatch-by-the-Sea | **ACTIVE**, smaller garrison; Commander Cotter Pyke |

**Staffing and population:** The Watch comprises actual individuals serving as rangers, builders, stewards, officers, maesters and guards; it is not 3× invented town populations + independent 1,000-member army. Current repo already authors Jeor Mormont, Benjen Stark, Bowen Marsh, Aemon Targaryen, Alliser Thorne, Denys Mallister, Qhorin Halfhand and others; preserve existing NPC IDs, chain of command and parent relationships. All active castles maintain distinct inventories of food, stables, weapons and tools, with actual post-specific schedules. Former castles remain physical structures without producing labor, taxes or Watch salaries. Reoccupation should be a dated, resource-using event. [S2][S5]

## 6. Quantitative initial-world planning (not canonical censuses)

**Population ranges are `[M]`; they apply to each site's *permanently resident civilian/household community* and exclude visiting traders, field armies, temporary king's procession and surrounding rural lordship. They must be balanced jointly against full Northern agricultural capacity, season, migration and known relations.** They are not recovered historical figures.

| Site archetype | Resident target [M] | Local organized armed posts [M], subset of people | Notes |
|---|---:|---:|---|
| Winterfell castle household and precinct | 250–900 | 75–250 | NOT winter town or overall Winterfell domain |
| Winter town | 300–2,500 season-variable | patrol from existing authority, not new army | Population swells in prolonged winter; origin residents tracked |
| White Harbor urban municipality | 18,000–65,000 | 500–2,000 | NEW Castle's household and Manderly urban domain must NOT be counted twice |
| Barrowton built-up town | 3,000–10,000 | 100–400 | Exclude rural barrowlands |
| Great Northern castles, castle precinct only | 100–550 | 40–180 | Dreadfort / Last Hearth / Karhold / Deepwood / Torrhen etc., not domain total |
| Lesser Northern castles/holdfasts | 40–250 | 10–70 | Adjacent villages counted separately |
| Rural farming/forest villages | 30–450 | 0–15 | Farm economy/worksites determine viability |
| Northern mountain/Skagos hamlets | 15–180 | no automatic feudal garrison | Sparse communities, locally distinctive households |
| Sisterton/island port settlements | 500–3,500 per port | 20–140 | Island subpopulations add to, not supplement, Three Sisters total |
| Night's Watch | Canon **<1,000 total**, across all 3 active castles | Not additive — Watch brothers are the garrison | About 600 Castle Black, 200 Shadow Tower, remainder Eastwatch [C] |
| Abandoned Watch castles, Hardhome, isolated ruins | no established civic census | 0 standing 298 AC | Visitors/patrols remain registered elsewhere |
| Free Folk nomadic camps | `[U]` size for particular groups | no generic professional garrison | Actual named bands/families with person identity, not towns |

### Employed adult sector allocation templates [M], each row totals 100%

| Archetype | Farm/food/forestry | Craft/extraction/construction | Trade/transport | Civic/service/household/religion | Guards/military |
|---|---:|---:|---:|---:|---:|
| Rural northern village | 64% | 12% | 5% | 15% | 4% |
| Castle + surrounding village (shared local accounting) | 35% | 18% | 8% | 23% | 16% |
| Winterfell castle precinct | 10% | 20% | 8% | 38% | 24% |
| White Harbor city | 13% | 27% | 25% | 27% | 8% |
| Forest village | 48% | 24% | 10% | 14% | 4% |
| Remote island fishing village | 63% | 17% | 8% | 9% | 3% |
| Three Sisters port | 42% | 18% | 23% | 13% | 4% |
| Gift village with Watch provisioning | 58% | 12% | 10% | 18% | 2% |

**All figures require adjustment:** unemployed, children and elderly excluded from employed denominators. Garrison people occur *inside* military job shares, not as separately generated characters. Island port counts must reconcile with rural fishing communities and ships. Day 0 armed personnel stationed in a castle may not equal military power stored on the `LandedHolding` object.

## 7. Culture, religion, family, law, diplomacy and healthcare

**Core North:** First Men heritage and old gods prominent, but not a universal faith. Manderlys at White Harbor retain Faith of the Seven and produce culturally mixed mercantile urban communities. Named persons should keep individual culture, religion, language, occupation, household and social station; Faith and old gods institutions have separate physical assets and membership. [S1][S3]

**Free Folk:** Different bands, leaders, local norms and kinship. “Wildling” is an external category, not a monolithic ruler, urban police force, standardized surname culture or universal faith. Mance Rayder's leadership is politically contingent and emergent. Craster's practices are specific to his homestead. [S6]

**Skagos:** Separate island cultures and legal/historical traditions. Crowl/Magnar/Stane names exist, but contemporary living heads unknown. Contested reports of cannibalism and unicorn-like animals belong as individual beliefs or uncertainty, not guaranteed village templates. Rickon/Osha's subsequent flight not already true in 298 AC. [S7]

**Three Sisters:** Maritime Sistermen, local beacon culture, seafood economy and politically loose Arryn suzerainty. False-light wreck schemes should be possible decisions by individual people, not automatic universal criminality. The North–Sisters maritime trading route is real; goods and merchants must be materially transferred. [S9]

**Night's Watch:** Institution-specific oath and organization membership across cultures; it is not a hereditary military House. Jeor Mormont is an individual Lord Commander, and Aemon's Targaryen birth is not an active claim to own the Wall. Vows, discipline, burial, provisioning, patrols and promotion run through existing NPC, organizations, memory, law, and title/authority systems. [S2]

**Health/climate:** Winter hunger, cold exposure, accidents, river contamination and crowded winter town disease are plausible consequences `[I]`, not precisely measured lore epidemics at all Northern settlements. Link incidents to household food/medicine/temperature and actual people. Do not fabricate a fixed Northern disease prevalence.

## 8. Political history and pre-298 relations to store

- Old Kings in the North and Stark dominance, Bolton rivalries and historic wars belong in title/house/chronicle relations as **historical** facts.
- House Manderly's historic expulsion from the Reach and settlement in White Harbor has current culture/religion/trade implications; historic origins do not make White Harbor a current Reach dependency.
- Foundation of the Night's Watch, gifting of Brandon's Gift and the New Gift, Wall decline and abandonment timeline explain empty Watch forts and sparsely occupied estates; formerly manned does not mean 298 active.
- Greyjoy's rebellion, imprisonment/fostering of Theon at Winterfell and recent royal Baratheon/Stark relationships matter for initial character ties and trust, not forced future conquest.
- House Sunderland's past independent kingship is **historical**, not an active king of the Sisters in 298 AC.
- Skagos past uprisings explain fraught relations with Winterfell; do not preset the late-book status of the North.
- At story start no forced future Red Wedding, Stark defeats, Lord Commander Jon, Winterfell sack, or later naval Manderly buildup. Later events should be simulated, not applied at initialization.

## 9. Existing architecture and Step 2.2 exact join/backlog

| Data system | Existing repository owner | Step 2.2 join / missing requirements |
|---|---|---|
| Regional named features | `NorthFeatureRegistry.java` | 45 feature IDs, but missing 18 Watch castle identities, some named villages/holdings |
| Calibrated map zones | `MapZoneRegistry`, `RegionalMapTransform`, `KnownWorldGeoSampler` | original NW pixel, master-map pixel, world coords separate; never invent marker ID |
| Known places/aliases | `reference/locations.json`, `locations_holdings.json` | source alias → stable WorldLocation, avoid overlapping castle/town names |
| Land/territory/subregions | `landmass_zones.json`, `territory_zones.json`, `subregion_masks.json` | terrain and political boundaries; Watch Gifts vs North vs Vale Sisters |
| Social/environment priors | `regional_influences.json`, cultures/religions/occupations | correct probabilities and exceptions for Free Folk, Manderlys, Sistermen, Skagosi, Watch |
| Holding hierarchy | `LandedHoldingManager`, `landed_holdings.json` | owner vs de jure vs named holder/title/parent; rectify adapter-only entries; currently 259 global holdings |
| Dynasties/characters | `DynastyManager`, `NpcRegistry`, genealogy/relationship JSON and population packs | avoid random actual house heads where sources silent; keep Eddard/Jeor named NPC IDs |
| Watch / Free Folk institutions | Organization and NonDynasticSociety managers | vows, headship, clan offices, political and legal rules separate from House Stark |
| Households and census | `20_3_POPULATION_AND_HOUSEHOLDS.md` | stable people, cohorts not double-counted, residence vs posting vs current travel |
| Employment/work | `NpcActionProcessor.processWork` | currently no-op: workplaces/farms/crew jobs require actual labor dispatch |
| Land and food | `HoldingAgricultureCatalog`, agricultural_capacity.json`, `EconomyRuntime` | winter temperature/suitability, acreage, stock, seed reserves, real farmers |
| Inventories/trade/tax | `InventoryManager` and economy accounting | merchants/ships/ravens, river roads and customs/tolls/household wealth |
| Military | existing person/order refs, future unit/muster domain | Watch <1k vs Northern feudal levy and garrisons; no double count |
| Scenario settlements | `scenarios/agot_298_ac/settlements.json` | **currently Winterfell only**; proper source-snapped new positions must be authored |
| World generation | known-world biome, terrain and chunk services | iconic keep layouts, villages, Wall and rivers; preserve 1 block = 1 metre |
| Save/calendar | time, NPC and economy lifecycle/journals | death/war/work/migration and physical projection consistent after fast-forward/reload |

### 9.1 Particular schema/ownership issues

1. `three_sisters_domain` is parented to `vale_domain`: **correct**; do not “repair” it to a North holding merely because NW map shows Sisters.
2. `the_twins` in North geographical registry is a *Riverlands* House Frey seat; one feature/settlement after SW review.
3. Physical `winterfell` exists in `settlements.json` but only as one currently authored settlement position. Its current coordinates must be audited before assuming map-accurate placement; name is not proof of terrain snapping.
4. Some `landed_holdings.json` entries (Forrester / Whitehill / Glenmore) derive from adaptations/game lore, not primary-book canon. Tag `[A]` and keep as a separate mod-authored option.
5. Existing registries do not contain nineteen separate Wall castle markers; correctly distinguish historical castle assets vs staffed Watch posts.
6. The North's `north_domain` `populationWeight`/`taxBase`/`militaryValue` are abstract; numerical generation cannot treat them as canonical people, silver or soldiers.
7. Castle, market town, and district nearby are distinct footprints: Winterfell vs winter town; White Harbor vs New Castle; Sisterton vs Breakwater.
8. Northern mountain clan homes, Free Folk sites and Gift settlements must not automatically become hereditary holdings with made-up House IDs.

## 10. Cross-map and final correction backlog (deferred per user)

| Problem | Status and future correction |
|---|---|
| NW actual red-X detections/handwriting | Not visually inspectable in this chat; preserve later Python results and compare all recognized IDs with source pixels |
| False detections, aliases, missed sites | Put only in subsequent global correction file, without changing researched statuses here |
| NW/SW overlap | The Twins, river border, Fingers, major through-roads require single canonical identity |
| River geometry | White Knife, Last River, Milkwater, Broken Branch, Weeping Water and other named lines require actual polyline digitization |
| Watch sites absent in feature registry | Add 18 distinct castle features with active/abandoned status in Step 2.2, then align physical footprints |
| House-holder gaps | Preserve unknown holder NPCs; source check exact start-day lord chronology and cadet vassalship |
| Minor households/hamlets | Populate constrained unnamed settlements by land, roads and economy with `[M]` labels; no invented *canon names* |
| Map accuracy | Source pixel, master calibration, terrain/water, slope and ground point must be checked before construction |
| National population | Individually estimate, *then jointly calibrate* North, Westeros and rest of world, crop demand/defense/migration |
| Fast-forward and persistence | No ghost NPCs, duplicate garrisons, disappearing merchants or food created by unreal labor |

## 11. Status and sources

**This is a substantive Step 2.1 regional research dossier**, not just an inventory of code labels: geography and environmental zoning, all available distinct holding associations, all 19 Wall forts, settlement archetypes, resident and professional role estimate envelopes, culture/religion, history, agriculture, urban economy, port/road networks, military/Watch staffing, known NPC relationship constraints and Step 2.2 joins are documented. However, it **cannot certify completion of raw NW image marker matching** until the user-provided Python ledger and annotated map can be read, and individual obscure lordships remain deliberately unknown. The remaining SW research is independent.

**Source index (secondary pages linking to the books / official maps):**

- [S1] North: https://awoiaf.westeros.org/index.php/North
- [S2] Night's Watch: https://awoiaf.westeros.org/index.php/Night%27s_Watch
- [S3] Winterfell: https://awoiaf.westeros.org/index.php/Winterfell
- [S4] Houses of the North and social geography: https://awoiaf.westeros.org/index.php/North
- [S5] The Wall: https://awoiaf.westeros.org/index.php/Wall
- [S6] Beyond the Wall: https://awoiaf.westeros.org/index.php/Beyond_the_Wall
- [S7] Skagos: https://awoiaf.westeros.org/index.php/Skagos
- [S8] House Mormont: https://awoiaf.westeros.org/index.php/House_Mormont
- [S9] Three Sisters: https://awoiaf.westeros.org/index.php/Three_Sisters
- [S10] House Sunderland: https://awoiaf.westeros.org/index.php/House_Sunderland
- [S11] Shadow Tower: https://awoiaf.westeros.org/index.php/Shadow_Tower
- [S12] White Harbor: https://awoiaf.westeros.org/index.php/White_Harbor
- [S13] House Crowl: https://awoiaf.westeros.org/index.php/House_Crowl
- [S14] House Magnar: https://awoiaf.westeros.org/index.php/House_Magnar
- [S15] House Stane: https://awoiaf.westeros.org/index.php/House_Stane
- [S16] Military strength (semi-canon global planning): https://awoiaf.westeros.org/index.php/Military_strength

**Primary source directions:** George R. R. Martin, *A Game of Thrones* (particularly Bran, Eddard, Catelyn and Jon viewpoints), later books solely for backward-looking facts with dates; *The World of Ice & Fire* (North, Vale, Wall and lands beyond); *The Lands of Ice and Fire* printed maps. No claims should be treated as supported solely because a Telltale title or current mod registry contains the name.

**Repository files inspected:** `NorthFeatureRegistry.java`, `MapZoneRegistry.java`, `reference/locations_holdings.json`, `scenarios/agot_298_ac/landed_holdings.json`, `scenarios/agot_298_ac/settlements.json`, master roadmap, population/economy architecture and previous research documents. **No code or GitHub files modified.**
