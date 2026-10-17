# Known World — Northern Westeros (NW) — DEEP RESEARCH DOSSIER, Step 2.1, 298 AC

**Supersedes:** `KNOWN_WORLD_RESEARCH_2_1_NW_298AC.md`, earlier 38-KB outline. This revision replaces its incomplete and occasionally incorrect statistical baselines; it is intended as the **NW 2.1 expanded source**, not an appendix of generic text.  
**Research date:** 2026-10-17.  
**Scenario start:** `agot_298_ac/scenario_start.json`: Royal Party **has arrived** at Winterfell, **before** the Robert/Eddard crypt conversation, Sansa/Joffrey betrothal, Bran's fall, and Ned's acceptance of Hand. Long summer is ongoing. First-day-year `0` is a **simulation-relative** clock, not a known canonical calendar day.  
**Regional map:** `authoring/maps/Realms/nw.png`, 2073×1966, code `NW`; adjoining `sw.png`.  
**Coverage:** 45 existing `NorthFeatureRegistry` features; **25 NW raster subregions**, 28 Northern landed-holding records including root, **4 Three Sisters records** from the Vale hierarchy, 77 Northern dynasty records, all **19 Wall castles** and additional known castles, towns, ruins, natural sites, Free Folk cultural geographies and the population/economy/politics/worldgen dependencies required for Step 2.2.  
**Critical limitation:** Connected GitHub reads code and JSON, but did not expose binary annotated PNG *pixels* or the user's Python detector output. The research identifies **recognizable canonical named places from sources/catalogs**, NOT verified exact painted X centers. The user explicitly wants unreadable/uncertain map markers consolidated in a separate later review file. Do not manufacture `NW-X-001` marker identities, pixel coordinates or counted unidentified Xs.

## A. Research/architecture and reliability contract

`[C]` = canon text / published official atlas / indexed chapter or historically established lore.  
`[R]` = verified identity and fields in **current repository**; not itself independent canon proof.  
`[I]` = plausible ecological/economic/historical inference; NOT an attested economic census or exact practice.  
`[M]` = mod-authored hypothetical range, architecture, parameter or generated settlement; not source canon.  
`[U]` = genuinely unknown or disputed; leave null until sourced / user decision.  
`[H]` = historical period event, ruin or past owner; never overwrite the living 298 AC state.  
`[ADAPT]` = adaptation, fan-derived or author-created lore (e.g. some Telltale sites), not book canon by existence in the repository.

This dossier is **source-grounded first-pass geographical and simulation research**, not a file of authoritative exact census numbers. Site-specific working figures are consciously *draft worldbuilding envelopes* to be calibrated against every other region, productive land, food requirements and the user's population policy. Fewer named settlements on the map does NOT mean there are no commoners in the countryside; named structure markers cannot stand in for entire settlement geography.

### Systemwide identities that research must serve

1. **One human identity/accounting authority.** Every persistent person appears once: households, NPCs, unmaterialized cohorts, labor, Watch rosters, soldiers, sailors, hereditary lineages, prisoners and visitors are JOINED references, not independent independently summable people. A cohort count must decrease when individual people are materialized and vice versa. Individual NPCs retain IDs when traveling, moving for winter, joining a levy or serving a noble.
2. **Political authority is not a place.** `WorldLocation` (where), `GeographicFeature` (landmark/landform), `Settlement` (physically inhabited place), `LandedHolding` (legal landed rights), `Dynasty` (family), `Title` (office), `Organization` (Watch, household, clan), and `NpcId` (actual holder) are different objects. Avoid one generic “owner of NW”.
3. **Lore ≠ repo correctness.** Repo currently owns 259 *global* holdings; **28** are Northern-domain (including root) and **4** are Sisters-related. A known name in `*FeatureRegistry` is not automatically a correct world location/settlement or fully staffed castle. Some known houses have NO landed holding record and some available records are adaptation-only.
4. **298 AC historical freeze.** Robert is alive and king, Ned is Lord Stark, House Manderly runs White Harbor, Roose Bolton is Lord of Dreadfort, Jeor Mormont is Lord Commander of Watch; deaths, wars, betrayals, occupations and political offices from later books are not initial state.
5. **Production and government need actual agents.** The current `NpcActionProcessor.processWork` has no productive effect. Farms require fields/crops/seed/weather/worker time, armies consume stock and draw from demographic people, ships need crews and inventories, taxes from real households, and years/seasonal changes must impact harvest and stores.
6. **Evidence and source-of-claim per row.** If a house's exact head, income, agriculture or fort size is absent, keep `[U]`, or suggest an explicitly `[M]` simulation envelope. Do not choose a fictional lord and retroactively describe him as book character.
7. **No forced future plot.** Model pre-existing social hostility, blood feuds, succession disputes, marriage connections, religious differences and resource incentives so events may emerge; do not pre-script the War of Five Kings.
8. **Map and materialization:** original NW observation, world stable identity, raster geometry, master-map pixel and Minecraft build centroid/footprint are separate. Same road, city or island appearing in `NW`+`SW` must be one canonical location with several source observations.

### Books and high-quality references used

The principal narratives and retrospective histories are *A Game of Thrones* (especially Bran, Jon, Catelyn, Eddard POV); *A Clash of Kings*, *A Storm of Swords*, *A Feast for Crows*, *A Dance with Dragons* (as dated or retrospective evidence, NOT current-start outcomes); *The World of Ice & Fire* (The North, The Wall and Beyond, the Vale), and the officially published *The Lands of Ice and Fire* maps. Public linked A Wiki of Ice and Fire reference articles are **secondary source indexes** with chapter citations, not an invented primary source. Important anchors:

- [S01 North](https://awoiaf.westeros.org/index.php/North), [S02 Winterfell](https://awoiaf.westeros.org/index.php/Winterfell), [S03 Winter town](https://awoiaf.westeros.org/index.php/Winter_town), [S04 White Harbor](https://awoiaf.westeros.org/index.php/White_Harbor), [S05 New Castle](https://awoiaf.westeros.org/index.php/New_Castle).
- [S06 Wall](https://awoiaf.westeros.org/index.php/Wall), [S07 Night's Watch](https://awoiaf.westeros.org/index.php/Night%27s_Watch), [S08 Castle Black](https://awoiaf.westeros.org/index.php/Castle_Black), [S09 Shadow Tower](https://awoiaf.westeros.org/index.php/Shadow_Tower), [S10 Eastwatch](https://awoiaf.westeros.org/index.php/Eastwatch-by-the-Sea).
- [S11 Northern mountains](https://awoiaf.westeros.org/index.php/Northern_mountains), [S12 Northern mountain clans](https://awoiaf.westeros.org/index.php/Northern_mountain_clans), [S13 House Wull](https://awoiaf.westeros.org/index.php/House_Wull), [S14 House Liddle](https://awoiaf.westeros.org/index.php/House_Liddle).
- [S15 Beyond the Wall](https://awoiaf.westeros.org/index.php/Beyond_the_Wall), [S16 Hardhome](https://awoiaf.westeros.org/index.php/Hardhome), [S17 Fist of the First Men](https://awoiaf.westeros.org/index.php/Fist_of_the_First_Men), [S18 Whitetree](https://awoiaf.westeros.org/index.php/Whitetree), [S19 Mance Rayder](https://awoiaf.westeros.org/index.php/Mance_Rayder).
- [S20 Dreadfort](https://awoiaf.westeros.org/index.php/Dreadfort), [S21 Weeping Water](https://awoiaf.westeros.org/index.php/Weeping_Water), [S22 Barrowton](https://awoiaf.westeros.org/index.php/Barrowton), [S23 Barrow Hall](https://awoiaf.westeros.org/index.php/Barrow_Hall), [S24 Castle Cerwyn](https://awoiaf.westeros.org/index.php/Castle_Cerwyn), [S25 Medger Cerwyn](https://awoiaf.westeros.org/index.php/Medger_Cerwyn).
- [S26 Greywater Watch](https://awoiaf.westeros.org/index.php/Greywater_Watch), [S27 Moat Cailin](https://awoiaf.westeros.org/index.php/Moat_Cailin), [S28 Skagos](https://awoiaf.westeros.org/index.php/Skagos), [S29 House Manderly](https://awoiaf.westeros.org/index.php/House_Manderly), [S30 House Hornwood](https://awoiaf.westeros.org/index.php/House_Hornwood).
- [S31 Three Sisters](https://awoiaf.westeros.org/index.php/Three_Sisters), [S32 Sunderland](https://awoiaf.westeros.org/index.php/House_Sunderland), [S33 Sisterton](https://awoiaf.westeros.org/index.php/Sisterton), [S34 Borrell](https://awoiaf.westeros.org/index.php/House_Borrell), [S35 Night Lamp](https://awoiaf.westeros.org/index.php/Night_Lamp).
- [S36 Glass Gardens](https://awoiaf.westeros.org/index.php/Glass_Gardens), [S37 Godswood Winterfell](https://awoiaf.westeros.org/index.php/Godswood_of_Winterfell).

## B. Exact starting political/timeline audit

`scenario_start.json` is much more precise than “around AGOT”: royal party already at Winterfell **before** crypt conversation, appointment as Hand, planned Sansa–Joffrey marriage, Bran's discovery/fall, royal departure. Current state must preserve active stay/court supply/residency rather than placing Robert and Cersei in King's Landing at scenario tick zero.

| Actor/institution | Starting-day researched state | Must **not** already have happened |
|---|---|---|
| Eddard Stark | Lord Winterfell, Lord Paramount/Warden North, hosts Robert at Winterfell | Appointment as Hand; departure; death |
| Catelyn Stark née Tully | Resident Lady Stark, mother of children; ties Tully/Arryn | Her travel south and arrests |
| Robert Baratheon | King of Seven Kingdoms, visiting Winterfell with royal party | Death or succession |
| Cersei and Jaime Lannister | With royal visit in North | Tower incident, political crisis |
| Bran Stark | Alive and healthy at Winterfell; direwolf pup found | Fall/paralysis; future greenseer journeys |
| Robb Stark | Eddard's heir, not King in North | Crowning, marriages, wars, Red Wedding |
| Jon Snow | Raised at Winterfell, **not yet a sworn Watch brother** | Training/vows or Lord Commander status |
| Theon Greyjoy | Eddard's ward/hostage at Winterfell | Capture of Winterfell, abandonment of Stark household |
| House Bolton | Roose Bolton legitimate Stark bannerman at Dreadfort | Sack of Winterfell, northern lordship after Red Wedding |
| House Hornwood | Halys and Donella married, heir Daryn alive | Halys/Daryn deaths and ensuing contested claim |
| House Cerwyn | **Medger** Cerwyn lord; Cley heir | Cley succeeding and death; Jonelle's later succession |
| House Manderly | Wyman Manderly of New Castle/White Harbor | Major ADWD naval/war plots and later court actions |
| House Dustin | Lady Barbrey Dustin widow and ruling at Barrow Hall | Later Bolton winter military operations |
| House Mormont | Maege Mormont heads Bear Island; Jeor already Lord Commander of Watch | Jeor's death or later family deployments |
| House Stark northern mountain clans | Multiple proud clan leaders and small landowners | Clans acting as Stannis's northern host |
| Night's Watch | Jeor Mormont command, three manned Wall castles | Great ranging tragedy, Jon's command, mass reassignment |
| Free Folk/Mance | Mance organizing followers, mobile and politically diverse | Forced all-Free-Folk single fixed capital, later Wall battles |
| Three Sisters | Triston Sunderland under Arryn, Borrell at Sisterton | Political control transferring to Stark due to NW map inclusion |

**Important source corrections to former NW file:** winter town *summer occupancy* cannot be represented by simply simulating all winter housing as permanently full; semi-canonical fully occupied winter-town population may be about **15,000**, but not a reliable permanent summer census. At story's long-summer start **around four-fifths of houses are empty**. The 300–2,500 earlier blanket range was misleading. [S03] Also **Medger Cerwyn**, not his heir Cley, is starting lord [S25].


## C. All 25 color-raster NW subregions, their livelihoods and simulation meaning

The following **25** are the actual `NW` entries in `reference/subregion_masks.json`, not guessed newly invented map divisions. The 1:1 game terrain must sample `NW` pixels through the registered **affine map transform**, then clip by verified landmass and territory ownership. Subregion color is a **landscape descriptor**; culture/institution/holding associations remain separate. `[I]` indicates environmental plausibility, not proof of a local agricultural regime.


| # | Real zone ID | Approx source-map geography | Terrain | Food/product prior | Social/legal overlay | Required services and landmarks | Prohibitions | Sources |
|---|---|---|---|---|---|---|---|---|

| 1 | lands_of_always_winter | Far north beyond Wall / map upper edge | Ice, snow, stone, unreliable surveying | No ordinary estate food economy; no canonical census | UNKNOWN societies/forces | Unknown polar interior; no invented magical enemy spawn numbers | No automatic farm/kingdom/settlement | S14 S15 |

| 2 | the_frozen_shore | Far NW coast beyond Wall, west Frostfangs | Coast, gravel, ice floes, exposed shoals | Fish/marine gathering [I]; trading contacts uncertain | Free Folk coastal communities, decentralized | Coastal boats, seas ice, food drying and long travel | No uniform Frozen Shore dynasty | S15 |

| 3 | the_frostfangs | Western beyond-Wall mountain chain | High rock, narrow valleys, snowfields and passes | Grazing/foraging pockets [I], scarce grain | Mountain Free Folk, routes through passes | Skirling Pass, Giant’s Stair, Forktop landmarks | Avoid city-scale regular agriculture | S15 |

| 4 | the_haunted_forest | Beyond Wall east/south of Frostfangs | Dense northern woodland, streams, isolated clearings | Hunting, firewood, foraged food, small animal holding [I] | Free Folk villages/homesteads distinct groups | Whitetree, Craster, ringforts, ranger roads | No permanent Watch military ownership north of Wall | S15 S18 |

| 5 | bear_island | Bay of Ice west of north mainland | Rocky wooded island, coast and shallow farm patches | Fishing, forestry and limited animals/farms [I] | House Mormont and island families | Hall of the Mormonts; transport by ships, island garrison subset | Must not create generic populous continental metropolis | S01 |

| 6 | skagos | Shivering Sea east of Bay of Seals | High sea cliffs, mountainous island, dark stone | Fishing, foraged foods, meager fields; obsidian documented as export | Houses Crowl/Magnar/Stane, culturally Skagosi | Deepdown/Kingshouse/Driftwood Hall; distant ferries | Skagosi rumors ≠ confirmed behavior of all citizens | S28 |

| 7 | the_wall | East-west Wall barrier | Huge ice barrier, tunnels/gates, castles, tracks | No inherent farm production; Watch provisioning from Gift | Night’s Watch three active garrisons | One barrier and nineteen castle structures, finite men | Do not treat colored mask as habitable city | S06 S07 |

| 8 | brandons_gift | Narrow immediate south-Wall strip | Cold wooded fields, agricultural ruins | Former villages, timber and farming; currently thinly occupied | Institutional land rights Night’s Watch | Rents/rights and resupply, seasonal outmigration | Not a separate kingdom or lordly Stark tax domain | S07 |

| 9 | the_new_gift | Broader south-Wall endowed region | Cold forest/fields, roads, farms in decline | Sparse cropland, old homesteads, produce to Watch | Night’s Watch institutional landlord | Mole’s Town and dispersed villages, depopulation | Do not instantly fill with peasant families from potential carrying capacity | S07 |

| 10 | gray_cliffs | Northern east coast, above Widow’s Watch | Sheer coastal rocks, little flatland | Fisheries, pasture above cliffs [I] | Northern coast local lords/inhabitants | Landing suitability, sea hazards and seabird ecology | Registry spelling `grey_cliffs` differs from `gray_cliffs` region reference | S01 |

| 11 | sea_dragon_point | Northwest peninsula west of Wolfswood | Wooded rocky spit and remote shore | Timber, sparse fishing, hunting [I] | Sparse North subjects | Boat landings, cliff paths, difficult access | No densely generated cities | S01 |

| 12 | wolfswood | Northwest-central forest west of Winterfell | Huge forest with streams and clearings | Logging/woodwork, game, small pasture, farms in cleared land | Stark/Glover forest societies and adjacent clans | Deepwood Motte, Winterfell timber supply | No infinite tree products or instant clearing without work | S01 S02 |

| 13 | lonely_hills | East-centre between Last River and Dreadfort | Isolated hills, moor and upper streams | Pasture/wood/small agriculture [I] | Northern clan/hill farming households | Travel and defensible farmsteads, no large city | No auto-assignment to one canonical house | S20 |

| 14 | sheepshead_hills | Eastern North above White Knife coast | Hills, grazing and small streams | Sheep grazing, scattered fields/wood [I] | Wyman Manderly claims influence over areas | Wool/textile supply, coast roads | Do not equate Manderly claim with every field title | S29 |

| 15 | stony_shore | Western mainland coast south of Wolfswood | Exposed pebble shore, windswept land | Fishing, goat/sheep/limited crops [I] | Northern villages and minor holders | Shipping risk; raiding and garrison dispersal | No eastern temperate farm rates | S01 |

| 16 | the_rills | Western plains north Barrowlands | Open grazing/horse meadows/streams | Horse breeding, livestock and cultivation [I] | House Ryswell and subordinate smallholders | Horse training, fodder, inland traders | No literal realmwide worksite | S01 |

| 17 | barrowlands | Southwest North near Saltspear | Open hills/plains, ancient barrows, river confluence | Farms, pasture, regional markets | House Dustin; House Stout near Barrowton | Great Barrow/Barrow Hall, Barrowton town | Barrow/ruins not living villages by default | S22 S23 |

| 18 | cape_kraken | Southwestern coastal cape near Blazewater Bay | Rocky coast and western entrance to Ironman’s Bay | Fish and rocky coastal pasture [I] | Flint/Fisher historical coast traditions | Harbor/defense and sea raids, local clan distinction | Do not mistake cape polygon for Flint’s Finger castle footprint | S01 |

| 19 | the_neck | Far south marshland bordering Riverlands | Reeds, bogs, causeway, river channels, low visibility | Fishing, gathering, limited raised gardens [I] | Crannogmen; House Reed under Stark | Greywater Watch movable crannog; Moat Cailin north gateway | No standard farm-grass worldgen, no castle pinned to arbitrary point | S26 S27 |

| 20 | littlesister | Southern/eastern Bite island | Rocky windswept coastal terrain | Fishing, goat pasture, freight [I] | House Torrent → Sunderland → Arryn | Distinct Little Sister residences, ferry legs | Not part of North despite northern map | S31 S32 |

| 21 | longsister | Southern/eastern Bite island | Coastal hills and rough tidal edge | Fishing, pasture, maritime trade [I] | House Longthorpe → Sunderland → Arryn | Distinct island population and paths | Do not merge with Sweetsister | S31 |

| 22 | sweetsister | Three Sisters biggest harbor-related island | Coastal rock, fog, reefs and harbor | Crabs/fish, port freight, forage/goats | House Borrell under Sunderland | Sisterton, Breakwater and Night Lamp | Do not give Borrell municipal monopoly over other islands | S33 S34 |

| 23 | pebble | Smaller Vale island south/east of Bite | Small rock island and maritime corridors | Fisheries and garden patches only if terrain supports | House Pryor Vale | Small landing facilities, terrain and safety checks | Not a town marker | S31 |

| 24 | the_paps | Vale island group near Fingers | Rocky northern Vale coast | Fishing and rough pasture [I] | House Elesham of Paps | Inter-island ferries, cliffs and exposed islands | No double-island settlement creation | S31 |

| 25 | the_fingers | Northern Vale mainland fingers / SW overlap | Narrow peninsulas, wind-swept sparse grass | Coastal fisheries, sheep/goats [I] | Baelish and other Vale houses, not North | Coast access, smallholdings, remote Drearfort | SW physical overlap and legal parent Vale | S31 |



### Region-specific biophysical modeling contract (not invented canon measurements)

Maintain independent spatial layers for `meanTemperature`, long-summer vs winter `heatBudget`, `precipitation`, `waterTable`, `slope`, `substrate`, `soilDepth`, `freshwaterAccess`, `forestType`, `forestBiomass`, `arableSuitability`, `pastureCapacity`, `fishStock`, `wildlifeStock`, `winterFuelNeeds`, `seaHazard`, `roadCost`, `navigability` and `buildingFootprintSuitability`. These should be calibrated by terrain and simulation tests, **not assigned one numeric fertility rate to all of North**.

- **Cold/winter:** the 298 opening is a **long summer** [R scenario], so do not start with winter frost everywhere. Long winters later require crop calendars, preserved food, barns/fodder, firewood, wintertown resettlement, fuel price changes, household mortality and ruler response. A lore description of winter is a future seasonal contingency, not a map initial weather forecast.
- **Northwestern highlands:** compact seasonal pastoral households, small crofts, mountain clan structures, fragmented paths and transhumance to wintertown and lowlands; none of these imply standardized feudal acreages or a 300-person stone castle in every clan.
- **White Knife catchment:** water transport should connect fish markets, farms and White Harbor, but true navigable lengths/boat drafts require channel geometry; do not infer a deep-water port at every river bend.
- **The Gift:** legally Watch-controlled, low-density food production with demographic attrition. Vacant cropland is capacity, not production.
- **Skagos/Skane:** separate island footprints and populations; reported past devastation of Skane does not give a 298 AC census there. Shipping risks and family customs can differ from mainland North.
- **The Neck:** wetlands, narrow causeways, shifting channels, crannogs and lizard-lions where habitat exists [C]. Disable vanilla flat-plains villages. Greywater Watch needs a relocatable location abstraction or a scenario-authored special representation; its “position” cannot be a normal permanent stone tower at one fixed coordinate.
- **Beyond Wall:** fine-scale monster/supernatural sightings belong to scenario lore/encounters with epistemic confidence, not an ordinary climate multiplier that conjures canon enemy counts.
- **Three Sisters:** islands are economically tied to White Harbor and elsewhere by sea, legally Arryn/Sunderland; 1:1 traversable roads end at ports; schedules must use real boats/crews to cross water.


## D. Current northern and Three Sisters landed-holding records (full, exact IDs)

This section is a deliberate **exhaustive crosswalk for 32 existing landed-holding records** (28 North root/descendants; 4 Vale Sister group records). It does NOT assert that each was marked by a legible red X. Political, dynastic, physical, social and economic owners must be connected by IDs in Step 2.2. Named holder claims are subject to sources; `[R]` alone does not justify fictional lords. One regional domain represents a **territorial hierarchy** rather than an extra population.


| # | Holding name | Exact landed holding ID [R] | Current ownerDynastyId [R] | 298 holder/source evidence |
|---|---|---|---|---|

| 1 | The North | north_domain | house_stark | [C/R] Eddard holderNpcId=eddard_stark; political rights not taxBase count |

| 2 | Barrow Hall | barrow_hall_domain | house_dustin | [C] Barbrey; [H] ancient Barrow Kings |

| 3 | Blackpool | blackpool_domain | house_slate | [R/U] location and house; economy inferred |

| 4 | Breakstone Hill | breakstone_hill_domain | house_flint_breakstone_hill | [R] ownership; specific holder/site appearance unknown |

| 5 | Castle Cerwyn | castle_cerwyn_domain | house_cerwyn | [C] Medger [S24,S25]; physical location relative not exact coordinate |

| 6 | Deepdown | deepdown_domain | house_crowl | [R/C] dynasty and castle; holder identity unknown |

| 7 | Deepwood Motte | deepwood_motte_domain | house_glover | [C] house/seat; holder attribution supported AGOT-era sources |

| 8 | Dormand Hall | dormand_hall_domain | house_dormand | [R/U] |

| 9 | Dreadfort | dreadfort_domain | house_bolton | [C] Roose, setting [S20,S21] |

| 10 | Driftwood Hall | driftwood_hall_domain | house_stane | [R/C] seat/house; [U] holder |

| 11 | Flint's Finger | flints_finger_domain | house_flint_flints_finger | [R/C] house/seat; [U] head |

| 12 | Goldgrass | goldgrass_domain | house_stout | [C/R] location and house; starting personal holder needs verify |

| 13 | Greywater Watch | greywater_watch_domain | house_reed | [C] Howland [S26], exact point unknown by lore |

| 14 | Hall of the Mormonts | hall_of_mormonts_domain | house_mormont | [C] Maege; exact castle form unverified |

| 15 | Highpoint | highpoint_domain | house_whitehill | [R/ADAPT] holder unknown in book continuity |

| 16 | Hornwood | hornwood_domain | house_hornwood | [C] Halys, Donella, Daryn [S30] |

| 17 | Ironrath | ironrath_domain | house_forrester | [R/ADAPT] |

| 18 | Karhold | karhold_domain | house_karstark | [C] Rickard, Karstark family |

| 19 | Kingshouse | kingshouse_domain | house_magnar | [C/R] dynasty/seat; holder unknown |

| 20 | Last Hearth | last_hearth_domain | house_umber | [C] Jon Umber and border region |

| 21 | New Castle | new_castle_domain | house_manderly | [C] Wyman [S04,S05,S29] |

| 22 | Northern Mountains | northern_mountains_flint_domain | house_flint_mountains | [C/R] clan, unnamed actual holder |

| 23 | Oldcastle | oldcastle_domain | house_locke | [C/R] house/seat |

| 24 | The Rills | rills_domain | house_ryswell | [C/R] Rodrik Ryswell, broad geography |

| 25 | Rillwater Crossing | rillwater_crossing_domain | house_glenmore | [R/ADAPT] |

| 26 | Torrhen's Square | torrhens_square_domain | house_tallhart | [C/R] Helman, river/road vicinity |

| 27 | Widow's Watch | widows_watch_domain | house_flint_widows_watch | [C/R] Flint branch, unnamed head |

| 28 | Winterfell | winterfell_domain | house_stark | [C/R] Eddard holderNpcId=eddard_stark |

| 29 | Breakwater | breakwater_domain | house_borrell | [C] Godric [S33-S35] |

| 30 | Littlesister | littlesister_domain | house_torrent | [C/R] house/fealty, holder unknown |

| 31 | Longsister | longsister_domain | house_longthorpe | [C/R] house/fealty, holder unknown |

| 32 | Three Sisters | three_sisters_domain | house_sunderland | [C/R] Triston [S31-S32] |



### Mandatory full holding-specific dossiers

Each entry contains separate governance, landscape/food, economic roles, construction, person/military, genealogy/history and Step 2.2 migration notes. Where the books are silent, the project should preserve that silence rather than backfill it as canon. The following occupation splits are **scenario work-pool suggestions**, not a known demographic census. Adult workers **already include** guards and household servants; never add soldiers or servants on top of a town's population target.


### D.01 — The North (`north_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Eddard holderNpcId=eddard_stark; political rights not taxBase count. Political/historical specificity: Lord Eddard Stark, resident at Winterfell, feudal paramount under King Robert; many unequal bannermen. Repository associates `house_stark` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=north_domain`, `ownerDynastyId=house_stark`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Large diverse domain: central farms, forest timber, harbor commerce, highland grazing, coasts and marsh. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `NOT_APPLICABLE, this is an umbrella jurisdiction`; local defensive allocation `NOT_APPLICABLE`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **45%**, crafts/building/mines **20%**, commerce/transport **10%**, domestic/civic/religion **18%**, guards/armed roles **7%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Regional jurisdiction with several hundred potential villages and fortified seats, NOT an enormous castle on the centroid. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Liege orders travel, taxes and levies filter through subordinate holding; landmass realm data no extra residents. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.02 — Barrow Hall (`barrow_hall_domain`)

- **Research confidence, 298 office and tenure:** [C] Barbrey; [H] ancient Barrow Kings. Political/historical specificity: Lady Barbrey Dustin currently rules; widow of Lord Willam Dustin, sworn Stark bannerman. Repository associates `house_dustin` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=barrow_hall_domain`, `ownerDynastyId=house_dustin`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Barrowlands cultivated plains, grazing, grain/mill, Barrowton market links [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `3,000–11,000`; local defensive allocation `80–350`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **29%**, crafts/building/mines **23%**, commerce/transport **19%**, domestic/civic/religion **21%**, guards/armed roles **8%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Castle atop ancient Great Barrow, timber palisade, square towers and windmill, town below. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** One Barrowton + child castle Barrow Hall + adjacent Stout Goldgrass; interlinked workplace posts. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.03 — Blackpool (`blackpool_domain`)

- **Research confidence, 298 office and tenure:** [R/U] location and house; economy inferred. Political/historical specificity: House Slate northern bannerman; current individual holder not securely named. Repository associates `house_slate` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=blackpool_domain`, `ownerDynastyId=house_slate`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** North lowland mixed estate / forest and local farming [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Compact feudal keep of unknown material, nearby farms and barns [M]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** No invented named current Lord Slate or castle metrics; source footprint needs verifying. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.04 — Breakstone Hill (`breakstone_hill_domain`)

- **Research confidence, 298 office and tenure:** [R] ownership; specific holder/site appearance unknown. Political/historical specificity: Flint branch territory; regional hill clans and Flint branch distinction. Repository associates `house_flint_breakstone_hill` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=breakstone_hill_domain`, `ownerDynastyId=house_flint_breakstone_hill`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Mountain pasture, forest, limited grain [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `15–180 per clan site`; local defensive allocation `0–35`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **53%**, crafts/building/mines **17%**, commerce/transport **6%**, domestic/civic/religion **16%**, guards/armed roles **8%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Pastoral fastness / hill keep proposal, no fixed canon footprint. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Not interchangeable with Flint's Finger, Widow's Watch or Northern Mountains Flints. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.05 — Castle Cerwyn (`castle_cerwyn_domain`)

- **Research confidence, 298 office and tenure:** [C] Medger [S24,S25]; physical location relative not exact coordinate. Political/historical specificity: **Medger Cerwyn is lord**, with Cley and Jonelle his children; important Stark family neighbors. Repository associates `house_cerwyn` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=castle_cerwyn_domain`, `ownerDynastyId=house_cerwyn`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Central cold crops/pasture/river crossings; near western White Knife branch [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Keep approximately half day riding from Winterfell, supported by village/fields. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Explicit starting Medger/Cley heir order, source correction to old NW; later Cley lordship future. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.06 — Deepdown (`deepdown_domain`)

- **Research confidence, 298 office and tenure:** [R/C] dynasty and castle; holder identity unknown. Political/historical specificity: House Crowl of Skagos, a distinct local branch of northern authority. Repository associates `house_crowl` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=deepdown_domain`, `ownerDynastyId=house_crowl`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Rocky island sheep/fishing/obsidian potential [I/C]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Remote Skagosi hill stronghold, dark stone optional [M]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Do not give a mainland North standard climate and treat Crowls as a homogeneous Free Folk band. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.07 — Deepwood Motte (`deepwood_motte_domain`)

- **Research confidence, 298 office and tenure:** [C] house/seat; holder attribution supported AGOT-era sources. Political/historical specificity: House Glover seat, Galbart Glover head, Robett Glover a brother; household vassal of Starks. Repository associates `house_glover` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=deepwood_motte_domain`, `ownerDynastyId=house_glover`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Wolfswood timber, small clearings, hunting/forest transport [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 in actual site`; local defensive allocation `10–75`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **48%**, crafts/building/mines **24%**, commerce/transport **9%**, domestic/civic/religion **14%**, guards/armed roles **5%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Wooden palisade/ringworks **motte**; surrounding cabins, stables, logger depots. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Woodland extraction must deplete timber stock and employ identified workers. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.08 — Dormand Hall (`dormand_hall_domain`)

- **Research confidence, 298 office and tenure:** [R/U]. Political/historical specificity: House Dormand recorded in scenario; scarce book-era exact holder/site detail. Repository associates `house_dormand` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=dormand_hall_domain`, `ownerDynastyId=house_dormand`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Unknown [U]; use local sampled climate and land rather than fabricated export. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Castle/house seat type from repo, exact structure uncertain. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** No guaranteed printed map annotation or canon owner until provenance check. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.09 — Dreadfort (`dreadfort_domain`)

- **Research confidence, 298 office and tenure:** [C] Roose, setting [S20,S21]. Political/historical specificity: Lord Roose Bolton, legitimate 298 Stark bannerman; son Domeric already dead; Ramsay Snow illegitimate. Repository associates `house_bolton` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=dreadfort_domain`, `ownerDynastyId=house_bolton`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Weeping Water river land/forest, mills, cattle/crops [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `100–650 built site only`; local defensive allocation `30–180`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **57%**, crafts/building/mines **16%**, commerce/transport **8%**, domestic/civic/religion **12%**, guards/armed roles **7%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Large austere stone castle on north bank of Weeping Water, rooms/history of flaying, strong hall. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Bolton distrust/feuds are characters' memories/faction factors not automatic rebellion; future Winterfell ownership not initial. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.10 — Driftwood Hall (`driftwood_hall_domain`)

- **Research confidence, 298 office and tenure:** [R/C] seat/house; [U] holder. Political/historical specificity: House Stane of Skagos, cultural/kinship distinctions. Repository associates `house_stane` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=driftwood_hall_domain`, `ownerDynastyId=house_stane`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Rocky coast fishing, marginal fields and hides [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Remote small island seat, storms and exposed landing access. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** No confirmed personal ruler; Blackwood/Free Folk names not substitute Skagosi names. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.11 — Flint's Finger (`flints_finger_domain`)

- **Research confidence, 298 office and tenure:** [R/C] house/seat; [U] head. Political/historical specificity: House Flint of Flint's Finger, distinct line from other Flint branches. Repository associates `house_flint_flints_finger` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=flints_finger_domain`, `ownerDynastyId=house_flint_flints_finger`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Rocky western coast, fishing and pasture [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 built site`; local defensive allocation `5–90`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **50%**, crafts/building/mines **17%**, commerce/transport **21%**, domestic/civic/religion **8%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Coastal keep at/near Flint's Finger; sea watch function [I]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Keep and Cape Kraken region/cape polygon are separate features; verify exact location. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.12 — Goldgrass (`goldgrass_domain`)

- **Research confidence, 298 office and tenure:** [C/R] location and house; starting personal holder needs verify. Political/historical specificity: House Stout, linked to Barrowton/House Dustin; seat adjacent to Barrowton eastern entrance. Repository associates `house_stout` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=goldgrass_domain`, `ownerDynastyId=house_stout`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Barrowlands fields, granary, grain processing and pasture [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Small Stout keep near city gate, distinct from Barrow Hall [C]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Ownership chain likely Dustin sub-vassal even if current parentHoldingId north_domain; reconcile [S22,S23]. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.13 — Greywater Watch (`greywater_watch_domain`)

- **Research confidence, 298 office and tenure:** [C] Howland [S26], exact point unknown by lore. Political/historical specificity: Lord Howland Reed, father of Meera and Jojen, Stark loyal vassal, central crannogman society. Repository associates `house_reed` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=greywater_watch_domain`, `ownerDynastyId=house_reed`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Bog fish/reeds, marsh wildlife, small raised gardens and boats [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `20–150 per crannog site`; local defensive allocation `0–40`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **59%**, crafts/building/mines **11%**, commerce/transport **18%**, domestic/civic/religion **9%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** **Moving crannog castle** in The Neck, not fixed stone towers; no resident maester/knight trainer [C]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Runtime stable identity with changing spatial footprint; no phantom settlement if current lat/long unresolved. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.14 — Hall of the Mormonts (`hall_of_mormonts_domain`)

- **Research confidence, 298 office and tenure:** [C] Maege; exact castle form unverified. Political/historical specificity: Lady Maege Mormont commands Bear Island family; Jeor Mormont is Watch lord commander and **does not** simultaneously govern Bear Island. Repository associates `house_mormont` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=hall_of_mormonts_domain`, `ownerDynastyId=house_mormont`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Coastal fishing, livestock, forestry, island security [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Bear Island timber/stone castle, ship landing and watch posts. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Lady Maege one NPC; Jeor Watch membership independent from island ownership; avoid duplicate guards. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.15 — Highpoint (`highpoint_domain`)

- **Research confidence, 298 office and tenure:** [R/ADAPT] holder unknown in book continuity. Political/historical specificity: House Whitehill of Telltale adaptation; source hierarchy from game, NOT canon book guarantee. Repository associates `house_whitehill` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=highpoint_domain`, `ownerDynastyId=house_whitehill`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Northern highlands; ore/forges sometimes suggested by adaptation. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `15–180 per clan site`; local defensive allocation `0–35`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **53%**, crafts/building/mines **17%**, commerce/transport **6%**, domestic/civic/religion **16%**, guards/armed roles **8%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Game-derived castle reference, placement contingent mod author's inclusion. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** `[ADAPT]` must not silently enter canonical X-research or replace any established northern house. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.16 — Hornwood (`hornwood_domain`)

- **Research confidence, 298 office and tenure:** [C] Halys, Donella, Daryn [S30]. Political/historical specificity: Lord **Halys Hornwood** and Lady Donella (née Manderly) are living; heir Daryn is alive, Larence Snow natural son. Repository associates `house_hornwood` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=hornwood_domain`, `ownerDynastyId=house_hornwood`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Extensive forest, hunting, lumber, farms and grazing [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 in actual site`; local defensive allocation `10–75`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **48%**, crafts/building/mines **24%**, commerce/transport **9%**, domestic/civic/religion **14%**, guards/armed roles **5%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Castle with woodlands, hamlets, mills, outlying farmsteads. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Marriage/legitimate heir and later succession dispute preserved as future possibilities, not pre-sacked land. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.17 — Ironrath (`ironrath_domain`)

- **Research confidence, 298 office and tenure:** [R/ADAPT]. Political/historical specificity: Forrester family from Telltale adaptation, not established by primary book cartography. Repository associates `house_forrester` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=ironrath_domain`, `ownerDynastyId=house_forrester`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Forest ironwood lumber and small smithing in adaptation; do not call it book canon. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 in actual site`; local defensive allocation `10–75`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **48%**, crafts/building/mines **24%**, commerce/transport **9%**, domestic/civic/religion **14%**, guards/armed roles **5%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Adaptation-authored forest keep and local wooden infrastructure. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Enable only if game-derived lore permitted; source provenance and separate mod ID. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.18 — Karhold (`karhold_domain`)

- **Research confidence, 298 office and tenure:** [C] Rickard, Karstark family. Political/historical specificity: Lord Rickard Karstark, Stark cadet lineage, sons Harrion/Eddard/Torrhen and daughter Alys; later events not yet happened. Repository associates `house_karstark` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=karhold_domain`, `ownerDynastyId=house_karstark`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Far-northeast land, harder frost, livestock, smaller farming valleys [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `80–450 castle site`; local defensive allocation `15–120`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **60%**, crafts/building/mines **14%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **7%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Stone northern fortification, distant estate villages and defensive tracks. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Separate cadet genealogy and multiple heirs; later executions/captivity not at 298 start. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.19 — Kingshouse (`kingshouse_domain`)

- **Research confidence, 298 office and tenure:** [C/R] dynasty/seat; holder unknown. Political/historical specificity: House Magnar of Skagos, 'magnar' relates to old-tongue lordship; no confirmed initial incumbent. Repository associates `house_magnar` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=kingshouse_domain`, `ownerDynastyId=house_magnar`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Skagos rocky uplands, fishing/animal husbandry [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Island stronghold, build style not documented. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Don't give generic Stark marriage titles to local head; don't confuse Magnar clan with Thenn's Magnar. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.20 — Last Hearth (`last_hearth_domain`)

- **Research confidence, 298 office and tenure:** [C] Jon Umber and border region. Political/historical specificity: Lord Jon Umber ('Greatjon') active; Smalljon heir, House Umber northern frontier bannerman. Repository associates `house_umber` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=last_hearth_domain`, `ownerDynastyId=house_umber`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Northern plains/hills, sheep summer upland grazing, cold food storage [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `80–450 castle site`; local defensive allocation `15–120`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **60%**, crafts/building/mines **14%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **7%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Northern castle at extreme northern Stark domain; keep/outer fields [M]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Free Folk raids and Watch relations; soldiers drawn from Umber population, not new people. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.21 — New Castle (`new_castle_domain`)

- **Research confidence, 298 office and tenure:** [C] Wyman [S04,S05,S29]. Political/historical specificity: Lord Wyman Manderly, sons Wylis/Wendel, Manderly exiles from Reach and Faith of Seven. Repository associates `house_manderly` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=new_castle_domain`, `ownerDynastyId=house_manderly`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Urban port guilds and White Knife river/grain/fish trade; castle imports urban food. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `18,000–65,000 entire White Harbor city, **NOT New Castle alone**`; local defensive allocation `500–2,000 within city residents`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **12%**, crafts/building/mines **29%**, commerce/transport **29%**, domestic/civic/religion **22%**, guards/armed roles **8%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** New Castle is **within White Harbor**, with Merman's Court; city separate from family household holding. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** One city census, nested castle/ward/harbor inventories; do not spawn two independent White Harbors. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.22 — Northern Mountains (`northern_mountains_flint_domain`)

- **Research confidence, 298 office and tenure:** [C/R] clan, unnamed actual holder. Political/historical specificity: Flint mountain clan branch among ~40 northern clans, not the single ruler of all northern mountains. Repository associates `house_flint_mountains` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=northern_mountains_flint_domain`, `ownerDynastyId=house_flint_mountains`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Pastoral meadows, forest, highland game and limited crofts [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `15–180 per clan site`; local defensive allocation `0–35`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **53%**, crafts/building/mines **17%**, commerce/transport **6%**, domestic/civic/religion **16%**, guards/armed roles **8%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Small clan holdfasts, huddled wood/stone farms, wintertown migration. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** No unified castle/realm and no 100% old gods for each generated person. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.23 — Oldcastle (`oldcastle_domain`)

- **Research confidence, 298 office and tenure:** [C/R] house/seat. Political/historical specificity: House Locke southeastern North, often linked to Manderly sphere of influence. Repository associates `house_locke` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=oldcastle_domain`, `ownerDynastyId=house_locke`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Coastal/river fields, fisheries and walled village [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 built site`; local defensive allocation `5–90`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **50%**, crafts/building/mines **17%**, commerce/transport **21%**, domestic/civic/religion **8%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Ancient keep by coastal rural settlement; exact structure unknown. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Manderly claim over Locke land not necessarily transfer of all ownership. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.24 — The Rills (`rills_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Rodrik Ryswell, broad geography. Political/historical specificity: Lord Rodrik Ryswell and daughters; relationship with widow Barbrey Dustin and Bolton ancestry. Repository associates `house_ryswell` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=rills_domain`, `ownerDynastyId=house_ryswell`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Horse breeding, grazing, hay/fodder and agrarian villages [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `unknown region total; 30–350 per farmstead village`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **63%**, crafts/building/mines **14%**, commerce/transport **8%**, domestic/civic/religion **12%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Territorial plains with individual manors/horse ranches; **not** just one castle. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Households/person marriages define political ties; territory population separate from named Barrowlands. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.25 — Rillwater Crossing (`rillwater_crossing_domain`)

- **Research confidence, 298 office and tenure:** [R/ADAPT]. Political/historical specificity: Glenmore dynasty/game-associated holding, exact map/canon status uncertain. Repository associates `house_glenmore` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=rillwater_crossing_domain`, `ownerDynastyId=house_glenmore`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** River crossing, grazing/road trade if included [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Fortified crossing only when properly placed and explicitly allowed. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** [ADAPT] and source-evidence verification; cannot spawn as canon town by registry existence. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.26 — Torrhen's Square (`torrhens_square_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Helman, river/road vicinity. Political/historical specificity: Lord Helman Tallhart, children Benfred and Eddara; lake/road area west of Winterfell. Repository associates `house_tallhart` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=torrhens_square_domain`, `ownerDynastyId=house_tallhart`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Inland lake/fish, agriculture and northern forest edge [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–300 castle/attached village, rural jurisdiction separately`; local defensive allocation `10–80`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **64%**, crafts/building/mines **13%**, commerce/transport **7%**, domestic/civic/religion **12%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Rural castle with square towers, road/courier network, nearby crofts. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Future Ironborn capture not initial; offspring identities and inheritance remain alive. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.27 — Widow's Watch (`widows_watch_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Flint branch, unnamed head. Political/historical specificity: House Flint of Widow's Watch, northeast peninsular coastal seat; local house distinct from western Flints. Repository associates `house_flint_widows_watch` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=widows_watch_domain`, `ownerDynastyId=house_flint_widows_watch`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Near sea, fish/coastal trade and local fields [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `40–350 built site`; local defensive allocation `5–90`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **50%**, crafts/building/mines **17%**, commerce/transport **21%**, domestic/civic/religion **8%**, guards/armed roles **4%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Rock-coast castle and landing/beacon [I]. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Manderly regional claim must not erase separate Flint landholding. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.28 — Winterfell (`winterfell_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Eddard holderNpcId=eddard_stark. Political/historical specificity: Eddard and Catelyn, Stark children Robb/Sansa/Arya/Bran/Rickon, Jon Snow, ward Theon; active royal visit. Repository associates `house_stark` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=winterfell_domain`, `ownerDynastyId=house_stark`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Central estate farms, kitchens, winter food reserves, forests, hot spring glass gardens [C/I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `300–1,100 castle residents, separate winter town`; local defensive allocation `60–250`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **12%**, crafts/building/mines **20%**, commerce/transport **10%**, domestic/civic/religion **35%**, guards/armed roles **23%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Two granite walls, godswood, crypts, Great Keep/First Keep, glass gardens, guesthouse, broken tower, stables and winter town. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Starting-character roster needs actual temporary positions (royal party in Winterfell); winter town separate seasonal households; main scenario coordinates not calibrated. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.29 — Breakwater (`breakwater_domain`)

- **Research confidence, 298 office and tenure:** [C] Godric [S33-S35]. Political/historical specificity: Godric Borrell is Lord of Sweetsister and Keeper of Night Lamp; vassal to Sunderland. Repository associates `house_borrell` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=breakwater_domain`, `ownerDynastyId=house_borrell`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Sisterton seafood, seafaring and local dues. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `500–3,500 Sisterton town; castle household subset`; local defensive allocation `20–130`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **36%**, crafts/building/mines **22%**, commerce/transport **24%**, domestic/civic/religion **13%**, guards/armed roles **5%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Castle of House Borrell **within Sisterton on Sweetsister** alongside Night Lamp. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** One Sisterton harbor with castle, beacon and Borrell households; no second Breakwater town. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.30 — Littlesister (`littlesister_domain`)

- **Research confidence, 298 office and tenure:** [C/R] house/fealty, holder unknown. Political/historical specificity: House Torrent sworn to Sunderland, Lord/individual names not reliably established. Repository associates `house_torrent` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=littlesister_domain`, `ownerDynastyId=house_torrent`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Fisheries, boats, small goat/vegetable patches where viable [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Island landing/fishing villages, no named grand stone castle attested. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Isle zone is part of Vale, not North despite NW map; distinct resident census. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.31 — Longsister (`longsister_domain`)

- **Research confidence, 298 office and tenure:** [C/R] house/fealty, holder unknown. Political/historical specificity: House Longthorpe sworn to Sunderland, island jurisdiction distinct. Repository associates `house_longthorpe` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=longsister_domain`, `ownerDynastyId=house_longthorpe`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Coastal fisheries, possible pasture and trade [I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `25–300 per inhabited site`; local defensive allocation `5–60`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **62%**, crafts/building/mines **16%**, commerce/transport **12%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Exposed island shore villages and rural households. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** Vassal claims and actual homes separate from Sweetsister island. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


### D.32 — Three Sisters (`three_sisters_domain`)

- **Research confidence, 298 office and tenure:** [C/R] Triston [S31-S32]. Political/historical specificity: Lord Triston Sunderland over Three Sisters, subject to House Arryn of Vale, Borrell/Torrent/Longthorpe vassals. Repository associates `house_sunderland` with the holding [R]; this does **not** establish that a particular unreferenced adult has been named Lord/Lady at the starting date.
- **Existing ID joins [R]:** `holdingId=three_sisters_domain`, `ownerDynastyId=house_sunderland`; `worldLocationId` is the matching name specified in `landed_holdings.json` and must be looked up, not invented from English label. Domain root and liege parent must be validated against dynasty hierarchy and each actual feudal dependency. The raw X and world position are **UNVERIFIED**; do not generate a building at map centroid.
- **Climate/land use and supply [C/I/U]:** Maritime island economy with fish, crab, goat cheese, sailing and barter [C/I]. The listed products are plausible activity priors, not exact confirmed per-holding harvest volumes or known tax income. Record farmable vs cultivated patches, water rights, woodland/animal stock, grazing intensity, hay/seeds, seasonal food imports, grain bins and local rain/snow exposure. Absent hectares and labor intensity remain null until cross-regional 2.2 calibration.
- **People, jobs and law [M]:** Conditional **built-site** population target `aggregate calibration required`; local defensive allocation `shared local posts`. These are **not canonical numbers** and cannot be summed over domain hierarchies. Suggested percentage of *currently employed adults* (not all residents): primary food/forestry/fish **53%**, crafts/building/mines **17%**, commerce/transport **20%**, domestic/civic/religion **7%**, guards/armed roles **3%**. Women and men may work different jobs depending on actual culture/legal constraints; no universal fixed distribution is canonical. Children, seniors, apprentices, dependents and unemployed are separate census categories, not omitted people.
- **Building, route and on-screen generator:** Group-of-islands jurisdiction, not a single town; seat location uncertain, Sisterton important. Physically authored iconic structures and ordinary farmsteads/roads should have separate child-site IDs, with water/slope/forest clearance validated before construction. Do not spawn a universal vanilla village merely because `HoldingType=CASTLE` in the existing registry.
- **Relations/history, occupants and forces:** Use actual household marriage/parentage/living NPC state and Stark/Arryn vassal relationship; distinguish hereditary dynastic claimant, legal holder, castellan, guards, guest house, military captain and any current visitors. Each defense envelope is a **subset** of resident/posted people, with realistic work loss on mobilization. Non-book/unknown specific tenure stays UNKNOWN, not a random canonized noble.
- **Step 2.2 test/known trap:** One island group polity and subordinate islands, prevent duplicate Sweetsister residents. Link `WorldReferenceCatalog` → `LandedHoldingManager` → `DynastyManager`/`NpcRegistry` → residences and physical `Settlement` → `HoldingAgricultureCatalog`, workplaces/inventories, troop rosters and player-encounter NPC. Ensure no farmland/guards/tax without actual physical/property/agent data, and preserve one permanent person identity across zoom levels.
- **Individual unresolved fields:** exact 298 AC holding census, population age structure, precise adult labor shares, local crop-by-field areas, named unlisted household servants, proven garrison and muster, trade counts, income/tax unit prices, verified site image marker pixel and Minecraft structure footprint. Each is `[U]` until sourced or explicitly `[M]` and calibrated.


## E. FULL Northern dynasty coverage (all 77 dynasties in `westeros_north.json`)

**Why this is essential for Step 2.2:** holdings are only a *subset* of houses. The repository currently has **77** Northern dynasty records plus a `defaults` block, while there are **28** North-domain landed holding records including the regional root. A family can be a cadet branch, ruined/extinct house, mountain clan, crannog household, household officer family, illegitimate line, ancestor, adaptation house, or named family whose seat remains undocumented. The absence of a linked *current* `landed_holdings.json` ID **never proves that a House never held land** or that it does not exist in the setting. Do not instantiate a castle at a guessed point to get every family into 298 AC.

Categorize per dynasty into `[CURRENT_HOLDING_IN_SCENARIO]`, `[KNOWN_CLAN/SEAT_NOT_YET_AUTHORED]`, `[HISTORICAL/EXTINCT_TIMELINE_REVIEW]`, `[NON_LANDED_HOUSEHOLD/UNCERTAIN]`, `[ADAPT/PROVENANCE_CHECK]` or `[SEAT_UNKNOWN]`. **No replacement invented source data**; all families get an ID and handoff question, not all a fabricated lord.


| # | Dynasty ID (exact [R]) | 298 research disposition | Holding/rough associated area | Step 2.2 resolution task | Evidence |
|---|---|---|---|---|---|

| 1 | house_stark | CURRENT_HOLDING_IN_SCENARIO | winterfell_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 2 | house_ashwood | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 3 | house_blackmyre | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 4 | house_boggs_north | CRANNOG_HOUSE | Neck | Possible crannogman family, exact estate and lord unverified | [S26] |

| 5 | house_bole | BANNER_WITHOUT_CURRENT_HOLDING | Wolfswood | Northern minor house, no current holding record; avoid generic Bole castle | [S01] |

| 6 | house_bolton | CURRENT_HOLDING_IN_SCENARIO | dreadfort_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 7 | house_bolton_winterfell | FUTURE_BRANCH_REVIEW | Winterfell | Later Bolton of Winterfell political identity NOT part of scenario start | [R/H] |

| 8 | house_branch | BANNER_WITHOUT_CURRENT_HOLDING | Wolfswood | Minor northern house, exact seat uncertain | [S01] |

| 9 | house_burley | KNOWN_CLAN | northern mountains | Clan representative, pasture/holdfast | [S12] |

| 10 | house_cassel | HOUSEHOLD_SERVICE | Winterfell castle | Ser Rodrik/Jory Cassel in Stark service; holding not automatically bestowed | [S02] |

| 11 | house_cerwyn | CURRENT_HOLDING_IN_SCENARIO | castle_cerwyn_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 12 | house_condon | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 13 | house_cray | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 14 | house_crowl | CURRENT_HOLDING_IN_SCENARIO | deepdown_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 15 | house_dustin | CURRENT_HOLDING_IN_SCENARIO | barrow_hall_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 16 | house_fenn | CRANNOG_HOUSE | Neck marsh crannogs | Reed-allied distinct crannogmen, not all residents of Greywater Watch | [S26] |

| 17 | house_fisher_stony_shore | HISTORICAL_REVIEW | Stony Shore | Historical Fisher kings / possible lineage; cannot auto-confirm active 298 family | [S01] |

| 18 | house_flint | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 19 | house_flint_breakstone_hill | CURRENT_HOLDING_IN_SCENARIO | breakstone_hill_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 20 | house_flint_flints_finger | CURRENT_HOLDING_IN_SCENARIO | flints_finger_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 21 | house_flint_mountains | CURRENT_HOLDING_IN_SCENARIO | northern_mountains_flint_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 22 | house_flint_widows_watch | CURRENT_HOLDING_IN_SCENARIO | widows_watch_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 23 | house_forrester | CURRENT_HOLDING_IN_SCENARIO | ironrath_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 24 | house_frost | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 25 | house_glover | CURRENT_HOLDING_IN_SCENARIO | deepwood_motte_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 26 | house_greengood | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 27 | house_greenwood | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 28 | house_greystark | HISTORICAL | former Wolf’s Den/White Harbor region | Extinct rebellious Stark cadet house historically; no living 298 AC ruler | [S01,S29] |

| 29 | house_harclay | KNOWN_CLAN | foothills near Wolfswood / Winterfell | Local clan smallholdings, no single mapped public castle | [S11,S12] |

| 30 | house_holt_north | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 31 | house_hornwood | CURRENT_HOLDING_IN_SCENARIO | hornwood_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 32 | house_ironsmith | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 33 | house_karstark | CURRENT_HOLDING_IN_SCENARIO | karhold_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 34 | house_knott | KNOWN_CLAN | northern mountains high places | Mountain clan, named individual ruler/seat must be sourced | [S11,S12] |

| 35 | house_lake_north | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 36 | house_liddle | KNOWN_CLAN | high northern mountains north of wolfswood | The Liddle, Torren Liddle; Morgan Liddle heir; caution episode chronology | [S11,S14] |

| 37 | house_lightfoot | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 38 | house_locke | CURRENT_HOLDING_IN_SCENARIO | oldcastle_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 39 | house_long_north | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 40 | house_magnar | CURRENT_HOLDING_IN_SCENARIO | kingshouse_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 41 | house_manderly | CURRENT_HOLDING_IN_SCENARIO | new_castle_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 42 | house_marsh | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 43 | house_mollen | HOUSEHOLD_SERVICE | Winterfell military household | Stark guard/officer line, family ancestry and landed status check | [S02] |

| 44 | house_mormont | CURRENT_HOLDING_IN_SCENARIO | hall_of_mormonts_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 45 | house_moss | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 46 | house_norrey | KNOWN_CLAN | upper northern mountains nearest Gift | Clan friends of Night’s Watch; unknown exact castle | [S11,S12] |

| 47 | house_overton | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 48 | house_peat | CRANNOG_HOUSE | Neck swamp | Distinct crannog clan/house, local house seat unknown | [S26] |

| 49 | house_poole | HOUSEHOLD_SERVICE | Winterfell castle | Steward Vayon Poole and daughter Jeyne; do not invent current Poole castle | [S02] |

| 50 | house_quagg | CRANNOG_HOUSE | Neck marsh vicinity (candidate) | Potential regional minor family, verify history | [R/U] |

| 51 | house_redbeard | HISTORICAL_REVIEW | Beyond Wall Free Folk legend/lineage | Bael/Redbeard traditions not starting Northern feudal lordship | [S15] |

| 52 | house_reed | CURRENT_HOLDING_IN_SCENARIO | greywater_watch_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 53 | house_ryder | SEAT_UNKNOWN | Northern North, confirm | No safe exact holdfast or ruler to author | [U] |

| 54 | house_ryswell | CURRENT_HOLDING_IN_SCENARIO | rills_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 55 | house_slate | CURRENT_HOLDING_IN_SCENARIO | blackpool_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 56 | house_stane | CURRENT_HOLDING_IN_SCENARIO | driftwood_hall_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 57 | house_stout | CURRENT_HOLDING_IN_SCENARIO | goldgrass_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 58 | house_tallhart | CURRENT_HOLDING_IN_SCENARIO | torrhens_square_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 59 | house_thenn | NON_FEUDAL_REVIEW | Thenn valley beyond Wall | Thenn society/Magnar of Thenn distinct from House Magnar of Skagos; no 298 Stark feudal title | [S15] |

| 60 | house_towers_north | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 61 | house_umber | CURRENT_HOLDING_IN_SCENARIO | last_hearth_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 62 | house_waterman | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 63 | house_wells_north | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 64 | house_whitehill | CURRENT_HOLDING_IN_SCENARIO | highpoint_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 65 | house_woodfoot | HISTORICAL | Bear Island historical control | Earlier Bear Island line predating Mormonts; do not revive as 298 island lord | [S01] |

| 66 | house_woods | BANNER_WITHOUT_CURRENT_HOLDING | Wolfswood | Northern family; exact map pin unknown | [S01] |

| 67 | house_woolfield | BANNER_WITHOUT_CURRENT_HOLDING | near White Harbor / Manderly sphere | House Woolfield linked to Manderly authority; named seat/holder research required | [S29] |

| 68 | house_wull | KNOWN_CLAN | Bay of Ice / west Northern mountains | Lord Hugo Wull (Big Bucket) later named in books; clan chief | [S12,S13] |

| 69 | house_tuttle | ADAPT | Telltale northern storyline | Book-era geographical seat not established | [R/ADAPT] |

| 70 | house_brownbarrow | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 71 | house_dormand | CURRENT_HOLDING_IN_SCENARIO | dormand_hall_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 72 | house_elliver | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 73 | house_glenmore | CURRENT_HOLDING_IN_SCENARIO | rillwater_crossing_domain | Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege | [R] |

| 74 | house_grayson | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 75 | house_greenleaf | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 76 | house_mazin | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |

| 77 | house_degore | SEAT_UNKNOWN | Northern dynasty record, no trustworthy pinpoint | Do not generate unresearched canonical castle or named holder; review living/existing status | [R/U] |



### House identity and succession-specific constraints

- **Dustin/Stout/Ryswell/Bolton:** Marriage and older kinship relations of Barbrey Dustin (née Ryswell) influence loyalties, inherited wealth and succession; not a forced automatic allegiance after war. `Goldgrass` castle is distinct from Barrow Hall and its ownership hierarchy should be revised against House Dustin vassalage.
- **Manderly's subordinate houses:** House Locke, Woolfield and Flint of Widow’s Watch in Wyman's northern claims are not all direct Stark personal household guards. Use `DynastyRelationship` and `parentHoldingId` only when legal hierarchy is supported, with claims distinct from accepted tenure. [S29]
- **Clan heads Wull/Liddle:** `Wull` and `Liddle` are family/clan status and named individual office; the mountain community population is not an additional population of the North outside Stark realm.
- **Free Folk social line:** 298 AC Mance Rayder's rising coalition is a `NonDynasticSociety`, not an extension of `house_stark` or a false appointed Stark jarl; House Thenn/Skagosi Magnar naming collision must be detected by catalog validation.
- **Historical families:** House Greystark and Woodfoot have historical associations; do not fabricate surviving 298 lords or inherited castles without source proof. `house_bolton_winterfell` reflects later chronology, not start.
- **Adaptations:** Telltale Forrester/Whitehill/Glenmore/Tuttle and other adaptation records may be user-chosen alternative-canon content. They must remain explicitly `[ADAPT]` with opt-in compatibility instead of being cited as novel-sourced houses.
- **Unknown named house without holding:** Keep it **unlinked** and separately investigated; do not reclassify `NO HOLDING FOUND` as “NEVER HAD A HOLDING” just because the catalog lacks a seat.


## F. All 45 existing `NorthFeatureRegistry` geographical features and scene-level semantics

This table is **exactly the list of canonical source IDs the current Java registry uses**. Each requires a stable map reference; it must not be a second resident population or automatically own a noble holding. Note mismatches where world-location and subregion use alternate names: `grey_cliffs` Java vs `gray_cliffs` raster; `frostfangs` Java vs `the_frostfangs` raster; `haunted_forest` Java vs `the_haunted_forest` raster; `the_rills` same ID but may also be `rills_domain` holding; map-specific labels and existing IDs should remain aliases until migration tests.


| # | Feature ID [R] | Type | Rough location | Physical/cultural consequence |
|---|---|---|---|---|

| 1 | lands_of_always_winter | REGION | Farthest mapped north above forest and ice rivers | Unknown coast/interior and polar hazards; not a kingdom |

| 2 | frostfangs | MOUNTAIN_RANGE | Northwest Beyond Wall, continues northern mountains | Mountains with passes, river headwaters, cliffs and seasonal freefolk movements |

| 3 | haunted_forest | FOREST | Beyond Wall north/east of Castle Black | Forest villages, game and ranger roads; do not spawn Watch village north of Wall |

| 4 | milkwater | RIVER | Runs from Frostfangs toward Bay of Ice/Gorge corridor | River crossing and seasonal ice, movement along Mance camps |

| 5 | frozen_shore | SHORE | Far northwestern coast beyond Wall | Cold sea fisheries, walrus/seal, Free Folk clan routes and icy shoals |

| 6 | bay_of_ice | BAY | Western North and beyond-Wall south end | Bear Island ferry and rocky mainland coast; sea transport hazard |

| 7 | bear_island | ISLAND | Northwest coast in Bay of Ice | Mormont island, woodland, fishing and coastal defense |

| 8 | the_wall | WALL | Crossing northern Westeros from Gorge to Bay of Seals | 19 Watch castle sites, only three active, gates/tunnels |

| 9 | castle_black | CASTLE | Middle of Wall, north end of kingsroad | Jeor's command, Castle Black buildings and recruits; active |

| 10 | brandons_gift | REGION | Immediate south of Wall | Institutional Watch land, abandoned fields and villages |

| 11 | the_new_gift | REGION | Broader south-Wall region | Queen Alysanne Watch donation, depopulated farms, Mole's Town |

| 12 | skane | ISLAND | Near Skagos in Bay of Seals | Historical reported devastation, no guaranteed modern town |

| 13 | skagos | ISLAND | East of North in Shivering Sea | Remote Stane/Crowl/Magnar, rock and obsidian, long sea crossing |

| 14 | bay_of_seals | BAY | East Wall, south of Skagos | Ice/storm and trade routes north of North |

| 15 | sea_dragon_point | PENINSULA | Western forest promontory into Bay of Ice | Sparse rugged timber coast, no big ports |

| 16 | wolfswood | FOREST | West of Winterfell to Deepwood Motte | Forestry, small clearings, hunting, giant wood landscape |

| 17 | stony_shore | REGION | Western North below Wolfswood | Coastal fishing villages, rough shore and winter risk |

| 18 | the_rills | REGION | Southwest of Winterfell above Barrowlands | Ryswell horse ranches, pasture and grain farms |

| 19 | barrowtown | TOWN | At Great Barrow by rivers flowing to Saltspear | Dustin Barrowton, Great Barrow, Goldgrass nearby |

| 20 | blazewater_bay | BAY | Southwestern coast close to Cape Kraken | Western coasts and sea-passage hazards, bays and capes |

| 21 | saltspear | BAY | South of Barrowlands, NW of Neck | Saltwater inlet/estuary, river runoff, transport for Barrowton |

| 22 | cape_kraken | CAPE | Southwestern exposed cape | Coastal terrain/Flint and former Fisher history, distinct from castle |

| 23 | barrowlands | REGION | Southwest North, Barrowton center | Old barrows/historical kings, rural fields and town economy |

| 24 | kingsroad | ROAD | Wall–Winterfell–Neck toward Riverlands | Main north-south transport, couriers, inns, armies and variable upkeep |

| 25 | the_neck | REGION | Southern North wetland between rivers | Crannogs, Moat Cailin and hazardous causeway |

| 26 | winterfell | CASTLE | Central North east edge of Wolfswood | Stark capital, winter town, glass gardens, godswood and crypts |

| 27 | lonely_hills | HILLS | North of Dreadfort, east of winterfell | Eastern rural uplands and grazing, no city |

| 28 | last_river | RIVER | East North between Last Hearth and Dreadfort | River geometry, hydrologic crossings, upstream livestock |

| 29 | dreadfort | CASTLE | East North north bank Weeping Water | Bolton seat Roose, active castle and rural villages |

| 30 | weeping_water | RIVER | East North runs by Dreadfort to Shivering Sea | Ice fishing, mills and river farms where suitable |

| 31 | grey_cliffs | CLIFFS | North-eastern coast above Shivering Sea | CLIFFS feature vs `gray_cliffs` region spelling discrepancy |

| 32 | sheepshead_hills | HILLS | Eastern hinterland of White Harbor | Manderly claimed region, pastoral/woodland |

| 33 | broken_branch | RIVER | East of White Knife near Sheepshead Hills | River network/river crossings under Manderly sphere |

| 34 | white_knife | RIVER | From Long Lake/Winterfell branches to White Harbor | Northern artery, White Harbor fish and overland trade |

| 35 | white_harbor | CITY | Mouth White Knife on Bite, southeast North | Largest North city, new Castle and old Wolf's Den, Faith of Seven |

| 36 | moat_cailin | CASTLE | North boundary of Neck on kingsroad | Partial ruins, causeway gate, not functioning city |

| 37 | the_bite | BAY | South of White Harbor, around Three Sisters | Cross-Bite shipping and trade, fog/shoals |

| 38 | littlesister | ISLAND | Three Sisters / eastern Bite | Torrent island, Vale polity, not Stark |

| 39 | longsister | ISLAND | Three Sisters / eastern Bite | Longthorpe island, Vale polity |

| 40 | sweetsister | ISLAND | Three Sisters / eastern Bite | Borrell/Sisterton/Breakwater harbor area |

| 41 | pebble | ISLAND | Near Fingers and Sisters in northern Vale | Vale island with House Pryor, limited land |

| 42 | the_paps | ISLAND_GROUP | Northern Vale coastal island group | House Elesham; no individual “Paps city” |

| 43 | three_sisters | ISLAND_GROUP | Islands in Bite south of White Harbor | Sunderland island lordship under Arryn |

| 44 | the_fingers | PENINSULA | Northern Vale shore reaching into Bite | Baelish/other Vale territory, SW/NW duplicate |

| 45 | the_twins | CASTLE | Green Fork north Riverlands/south map border | House Frey fortified bridge/cross-map, NOT under Stark |



### F.1 Roads, streams, coast and navigation are geometry, not points

**Hydrology:** Milkwater and tributaries (Beyond Wall), Last River and Weeping Water (east North), White Knife with its branches and Long Lake source region (central/east), Broken Branch, Barrowlands streams feeding Saltspear, Green Fork headwaters in the Neck, and the Fever River nearby. Their official map courses are **not completely reproducible from list entries**. Final line geometries should be traced on master imagery, calibrated against actual authoring scale/elevation, and tested at every crossing. A river name in `GeographicFeatureRegistry` does not imply a stored polyline, navigable boat depth, or actual flowing Minecraft water already exists.

**Roads:** kingsroad from Wall by Winterfell to Moat Cailin/riverlands; subsidiary roads toward White Harbor, Barrowton, Torrhen's Square, Dreadfort, Karhold and coast; Watch road along south face of Wall; ranger roads north; smaller marsh causeways and boat-only paths in Neck. Some are known route relationships without exact path tracings. A `road` should reference traversable endpoints, grade limits, bridge/ferry, threat, winter speed and household/army/merchant needs.

**Maritime water:** Bay of Ice → Bear Island and western harbors; Shivering Sea → northern/eastern coast and Skagos/Skane; Bay of Seals to Eastwatch, White Harbor ↔ Three Sisters across The Bite, Vale/Fingers and North/Riverlands. All ferries and trade ships must possess real crews, cargo inventories, trip durations, storm/season risk and docking capacity. A `SHIP` is not a teleport between an island raster and mainland.

**Contested labels:** `barrowtown` registry's display Barrowtown often called **Barrowton** in book; `greycliffs/graycliffs` alternate spelling must not silently spawn two landforms; `the_twins` registered by NW because visible near southern edge but under Freys in **Riverlands**. Never reparent it to Stark domain.


## G. **All 19 Wall castles** — individual geographic/structural/occupancy dossiers

The Wall spans one hundred leagues / roughly 300 miles; fantasy published width/height estimates should be reconciled against the **1 block = 1 m** physical design before authoring the barrier. In book start **only Castle Black, Shadow Tower, Eastwatch-by-the-Sea remain garrisoned**. The Wall used to have nineteen castles, but never more than seventeen staffed simultaneously even historically. Castle Black has **nearly 600 brothers**, Shadow Tower **~200**, Eastwatch the remainder of the **under 1,000** total [S06,S07]. Exact individual daily rosters, health, age composition and materialized NPCs are not established. Each post below is ONE historic `WatchCastleSite` with `isActiveAt298` and attached physical settlement/institution data as applicable.


| West→East | Site | Wall section | 298 AC status | Lore/function | Scene and maintenance notes |
|---|---|---|---|---|---|

| 1 | Westwatch-by-the-Bridge | Gorge/west extreme | ABANDONED | bridge and western gorge approach | sealed gate, scattered ruins, dangerous cliffs |

| 2 | Shadow Tower | West near Gorge | ACTIVE | Commander Ser Denys Mallister, ~200 brothers | outer western patrols, cliff/ridge patrol posts, supply store |

| 3 | Sentinel Stand | West of central Wall | ABANDONED | Watch ghost castle, original gate location | partially collapsed wall buildings |

| 4 | Greyguard | Western Wall mountain slopes | ABANDONED | huge ice foundation and stone bases | historical towers, hollow gate and unsafe structures |

| 5 | Stonedoor | Western half | ABANDONED | abandoned tunnel/gate on castle list | stone doorway/ruin arch |

| 6 | Hoarfrost Hill | Western half | ABANDONED | cold raised terrain near Wall | watchtower ruins and snow-bound access |

| 7 | Icemark | West-central Wall | ABANDONED | historic Watch fort | stone/ice ruin remnants |

| 8 | Nightfort | Central-west Wall | ABANDONED | ancient largest original castle; Black Gate legend/special lore | enormous ruin, sealed passages and crypt-like spaces |

| 9 | Deep Lake | Near central Wall | ABANDONED | historic abandoned fort near named lake | ruined buildings with water and rotting timber |

| 10 | Queensgate / Snowgate | Near central Wall | ABANDONED | renamed for Queen Alysanne, historically Snowgate | ruin and historical naming layer |

| 11 | Castle Black | Central, at end of kingsroad | ACTIVE | Lord Commander Jeor Mormont, ~600 brothers | armory, common hall, towers, training courtyard, King’s Tower and lift |

| 12 | Oakenshield | Central-east Wall | ABANDONED | ghost castle; distinct from Reach island Oakenshield | remnant Watch tower and closed gate |

| 13 | Woodswatch-by-the-Pool | East of Castle Black | ABANDONED | wood/pool-related named Watch site | tree and frozen-pool feature if mapped |

| 14 | Sable Hall | East-central Wall | ABANDONED | named abandoned castle | old hall fragments and closed gate |

| 15 | Rimegate | Eastern half | ABANDONED | historic Watch ice gate | frozen tunnel or blocked passage |

| 16 | Long Barrow | Eastern half | ABANDONED | historic Watch castle near barrow landmark | ruins; possible cairn and historic Watch tombs [I] |

| 17 | The Torches | Eastern near coast | ABANDONED | historic signal/watch fort | former beacon towers [I] not active staffing |

| 18 | Greenguard | East near Eastwatch | ABANDONED | eastern ghost castle | outer ruined watch barracks |

| 19 | Eastwatch-by-the-Sea | Eastern end / Bay of Seals | ACTIVE | Commander Cotter Pyke; smallest of three garrisons | Watch port/naval boats, shore depot and sea supply |



### G.01 — Westwatch-by-the-Bridge (`westwatch_by_the_bridge` proposed feature identity)

- **Map/site identity [C]:** Gorge/west extreme. Source-supported historic function: bridge and western gorge approach. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** sealed gate, scattered ruins, dangerous cliffs. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=westwatch_by_the_bridge` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.02 — Shadow Tower (`shadow_tower` proposed feature identity)

- **Map/site identity [C]:** West near Gorge. Source-supported historic function: Commander Ser Denys Mallister, ~200 brothers. 298 AC state is **ACTIVE** and is a historically staffed **real** Watch site. [S06,S07]
- **Governance/persons:** **Shadow Tower**: Denys Mallister is the commander; c.200 Watch brothers out of the shared under-1,000. In western marches Watch rangers patrol the gorge; their traveling members retain residence and service identities here. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** Running active post requires rationing, fuel, arrow supply, work shifts, cooks, laundry, smithing, builders and raven/courier support, all accounted against known human Watch members and imported/local supplies. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** outer western patrols, cliff/ridge patrol posts, supply store. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=shadow_tower` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.03 — Sentinel Stand (`sentinel_stand` proposed feature identity)

- **Map/site identity [C]:** West of central Wall. Source-supported historic function: Watch ghost castle, original gate location. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** partially collapsed wall buildings. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=sentinel_stand` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.04 — Greyguard (`greyguard` proposed feature identity)

- **Map/site identity [C]:** Western Wall mountain slopes. Source-supported historic function: huge ice foundation and stone bases. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** historical towers, hollow gate and unsafe structures. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=greyguard` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.05 — Stonedoor (`stonedoor` proposed feature identity)

- **Map/site identity [C]:** Western half. Source-supported historic function: abandoned tunnel/gate on castle list. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** stone doorway/ruin arch. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=stonedoor` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.06 — Hoarfrost Hill (`hoarfrost_hill` proposed feature identity)

- **Map/site identity [C]:** Western half. Source-supported historic function: cold raised terrain near Wall. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** watchtower ruins and snow-bound access. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=hoarfrost_hill` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.07 — Icemark (`icemark` proposed feature identity)

- **Map/site identity [C]:** West-central Wall. Source-supported historic function: historic Watch fort. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** stone/ice ruin remnants. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=icemark` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.08 — Nightfort (`nightfort` proposed feature identity)

- **Map/site identity [C]:** Central-west Wall. Source-supported historic function: ancient largest original castle; Black Gate legend/special lore. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** enormous ruin, sealed passages and crypt-like spaces. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=nightfort` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.09 — Deep Lake (`deep_lake` proposed feature identity)

- **Map/site identity [C]:** Near central Wall. Source-supported historic function: historic abandoned fort near named lake. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** ruined buildings with water and rotting timber. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=deep_lake` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.10 — Queensgate / Snowgate (`queensgate_snowgate` proposed feature identity)

- **Map/site identity [C]:** Near central Wall. Source-supported historic function: renamed for Queen Alysanne, historically Snowgate. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** ruin and historical naming layer. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=queensgate_snowgate` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.11 — Castle Black (`castle_black` proposed feature identity)

- **Map/site identity [C]:** Central, at end of kingsroad. Source-supported historic function: Lord Commander Jeor Mormont, ~600 brothers. 298 AC state is **ACTIVE** and is a historically staffed **real** Watch site. [S06,S07]
- **Governance/persons:** **Castle Black**: Jeor Mormont is Lord Commander; first-ranger Benjen Stark, Lord Steward Bowen Marsh and Maester Aemon are known. About 600 members across stewards/builders/rangers, not 600 elite soldiers. Jon Snow has not yet taken the black. The 298 court visit at Winterfell does not transport him here prematurely. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** Running active post requires rationing, fuel, arrow supply, work shifts, cooks, laundry, smithing, builders and raven/courier support, all accounted against known human Watch members and imported/local supplies. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** armory, common hall, towers, training courtyard, King’s Tower and lift. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=castle_black` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.12 — Oakenshield (`oakenshield` proposed feature identity)

- **Map/site identity [C]:** Central-east Wall. Source-supported historic function: ghost castle; distinct from Reach island Oakenshield. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** remnant Watch tower and closed gate. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=oakenshield` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.13 — Woodswatch-by-the-Pool (`woodswatch_by_the_pool` proposed feature identity)

- **Map/site identity [C]:** East of Castle Black. Source-supported historic function: wood/pool-related named Watch site. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** tree and frozen-pool feature if mapped. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=woodswatch_by_the_pool` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.14 — Sable Hall (`sable_hall` proposed feature identity)

- **Map/site identity [C]:** East-central Wall. Source-supported historic function: named abandoned castle. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** old hall fragments and closed gate. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=sable_hall` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.15 — Rimegate (`rimegate` proposed feature identity)

- **Map/site identity [C]:** Eastern half. Source-supported historic function: historic Watch ice gate. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** frozen tunnel or blocked passage. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=rimegate` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.16 — Long Barrow (`long_barrow` proposed feature identity)

- **Map/site identity [C]:** Eastern half. Source-supported historic function: historic Watch castle near barrow landmark. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** ruins; possible cairn and historic Watch tombs [I]. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=long_barrow` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.17 — The Torches (`the_torches` proposed feature identity)

- **Map/site identity [C]:** Eastern near coast. Source-supported historic function: historic signal/watch fort. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** former beacon towers [I] not active staffing. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=the_torches` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.18 — Greenguard (`greenguard` proposed feature identity)

- **Map/site identity [C]:** East near Eastwatch. Source-supported historic function: eastern ghost castle. 298 AC state is **ABANDONED** and is an **abandoned** historical castle with no year-start stationed civic/garrison payroll. [S06,S07]
- **Governance/persons:** No permanent 298 AC active garrison or autonomous castle household is established. An occasional ranger patrol/expedition can visit and occupy buildings temporarily, but those people remain residents at their actual Watch station and must not be counted again. Any permanent restoration is an explicitly dated future institutional decision. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** No resident agricultural workforce or operating market in a ghost castle. Preservation/repair and guard expeditions require visiting named Watch people with material resources; empty buildings generate zero automatic output. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** outer ruined watch barracks. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=greenguard` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.19 — Eastwatch-by-the-Sea (`eastwatch_by_the_sea` proposed feature identity)

- **Map/site identity [C]:** Eastern end / Bay of Seals. Source-supported historic function: Commander Cotter Pyke; smallest of three garrisons. 298 AC state is **ACTIVE** and is a historically staffed **real** Watch site. [S06,S07]
- **Governance/persons:** **Eastwatch**: Cotter Pyke commands. Active small Watch harbor and boats should maintain actual crews/ships, supplies and maritime patrols. Its soldiers are the remaining share after Black/Shadow garrisons, never a separate phantom fleet crew. The Night’s Watch is an organization holding obligations and land, **not** a hereditary dynasty. Its Lord Commander is not “Lord of the gift” in the same way a Stark bannerman owns a hereditary estate.
- **Economy and workers [I/M]:** Running active post requires rationing, fuel, arrow supply, work shifts, cooks, laundry, smithing, builders and raven/courier support, all accounted against known human Watch members and imported/local supplies. The Castle Black/Shadow/Eastwatch total workforce and garrisons must add to the **single** Night’s Watch population.
- **Architecture and exact structure placement:** Watch port/naval boats, shore depot and sea supply. Represent an individual Watch fort with a Wall gate/tunnel/approach and exposed abandoned or functional barrack structures only as appropriate. Named site type does **not** justify an independent civilian village.
- **Step 2.2 integration:** propose a stable `geographicFeatureId=eastwatch_by_the_sea` after registry collision audit; link `WatchCastleSite` → `worldLocationId`/terrain/Wall slice → organization `nights_watch`/command chain → structure footprints → active workplaces/food inventory only where staffed. Confirm actual X and line geometry later. Test `sum(activeStationPersonnel) + patrolsOnRoute = activeWatchMembers`, with no extra population for ruins.


### G.20 — The three active posts are a SINGLE institution

- Castle Black: Lord Commander, infirmary/maester support, granaries and supply, barracks, armory, rookery and Castle Black's named active personnel. Members have their own lawful life histories, original families/regions, individual religious choices and legal status after vows.
- Shadow Tower: Western patrol and mountain passes; Denys Mallister's post and independent local watch captain; supplies transported from local stocks and southern patrons, winter costs rising.
- Eastwatch: Eastern maritime gateway. Cotter Pyke controls sea-facing operations; boats do not spawn unlimited goods, crew or transport capacity from a static port icon.
- All abandoned Watch castles: keep physical landscape/site memory, ruined status, decrepit infrastructure and possible patrol/restoration interactions, **zero permanently resident 'castle population' at scenario day 1** unless a source says there are caretakers (none assumed).
- The Wall/gates are infrastructure, not individually owned noble holdings; `brandons_gift`, `the_new_gift` are the Night's Watch's land endowments; verify current sparse settlements and night riders as explicit smallholders/institutional dependencies.


## H. Additional named settlements, buildings, strongholds, ruins and landforms **outside simple holding IDs**

The source map and full simulation need many more sites than the ~30 authored Northern holdings: a feudal castle is not its entire town or rural population; a ruined hall is not a working city; Free Folk locations and Night's Watch historical castle sites do not belong to a Stark banner. The entries below are **source-supported geographic/name leads** which must be checked against original annotated red-X positions in the separate correction pass. Listing a site here **does not assert a specific X is painted on `nw.png`**.


| # | Place / landmark | Type | Rough NW map location | 298 AC details or uncertainty | Source |
|---|---|---|---|---|---|

| 1 | Winter town | SEASONAL TOWN | Outside Winterfell walls beside kingsroad / central North | Seasonal winter housing, extensive summer vacancy; possible ~15k full winter capacity is semi-canon, not current residents | S03 |

| 2 | Smoking Log | INN/ALEHOUSE | Winter town just below Winterfell castle | One operating inn/business with named employees and supplies, no separate settlement census | S03 |

| 3 | Winterfell Great Keep | PALACE BUILDING | Inside Winterfell double granite walls | Stark quarters/administration, direct office access and servants | S02 |

| 4 | Winterfell First Keep | HISTORICAL BUILDING | Inside Winterfell, near crypt entrance | Oldest keep area; abandoned/old architecture unlike active Great Keep | S02 |

| 5 | Winterfell crypts | CRYPT/HISTORIC SITE | Under old Winterfell near First Keep | Living Stark family history; ancestral statues; not inhabited cemetery | S02 |

| 6 | Winterfell godswood | SACRED FOREST | Walled woodland inside Winterfell | Heart tree, old-gods worship, three-acre grove, sacred authority separate from sept | S37 |

| 7 | Winterfell glass gardens | GREENHOUSE | Inside castle hot-springs area | Active heat-assisted horticulture; crop/fruit/floral plots and labor even in winter | S36 |

| 8 | Broken Tower | RUINED CASTLE TOWER | Within Winterfell | Broken named tower, physical danger; Bran's later fall NOT occurred | S02 |

| 9 | Winterfell small sept | RELIGIOUS BUILDING | Within Winterfell | Built for Catelyn; septon Chayle and Faith of Seven minority institution | S02 |

| 10 | Winterfell guesthouse | CASTLE BUILDING | Within Winterfell near godswood | Hosts visiting royal party; record court household temporarily present | S02 |

| 11 | Winterfell armory/training yard | WORKSITE | Inside Winterfell | Rodrik Cassel arms training, Mikken blacksmithing; real supplies and recruits | S02 |

| 12 | Winterfell kennels/stables | WORKSITE | Castle precinct | Farlen kennelmaster/Hullen horse master; actual animals and fodder | S02 |

| 13 | Winterfell rookery/maester turret | BUILDING | Inside Winterfell | Maester Luwin, messenger raven travel tied to real flight/time delays | S02 |

| 14 | White Harbor | CITY | Mouth of White Knife on southern North shore of Bite | Only proper northern city, customs/markets and Faith of Seven density; New Castle child seat | S04 |

| 15 | Wolf's Den / Old Castle | HISTORIC CASTLE/PRISON | White Harbor city historical fortified area | Manderly old seat earlier than New Castle; currently old building/prison, not second separate White Harbor | S05 |

| 16 | New Castle / New Keep | CASTLE | Inside White Harbor | Wyman Manderly, Merman's Court; urban-castle separate footprint | S05 |

| 17 | Sept of the Snows | SEPT | White Harbor | Faith of Seven pilgrimage/chapel institution within city, not exclusive ideology of North | S04 |

| 18 | Old Mint | BUILDING/HISTORIC SITE | White Harbor | Historical currency/mint associated with city, actual current mint operation unclear | S04 |

| 19 | White Harbor Fishfoot Yard | PORT QUARTER | At or near harbor waterfront | Ship/fish/salt activity, exact ward names/footprints verify before addition | S04 |

| 20 | Barrowton / Barrowtown | TOWN | Barrowlands at streams toward Saltspear | Dustin town at Great Barrow, broad streets/wooden walls, regional farm market | S22 |

| 21 | Great Barrow | ANCIENT BARROW | In center of Barrowton under Barrow Hall | First King legend, archaeological protected hill, not populated tomb | S22 |

| 22 | Barrow Hall | CASTLE | On Great Barrow within Barrowton | Lady Barbrey, wooden palisade/square towers, windmill | S23 |

| 23 | Goldgrass / Stout keep | CASTLE | Under 1 mile from Barrowton east gate | Stout subordinate district, nearby rural agriculture, no duplicate Barrowton | S22 |

| 24 | Mole's Town | VILLAGE | South of Castle Black in the Gift | Watch-adjacent inhabited civilian trading/service village, burrows beneath houses | S07 |

| 25 | The Shadow Tower | ACTIVE WATCH CASTLE | At western Wall by Gorge | Denys Mallister ~200 Watch people, post can patrol mountain passes | S09 |

| 26 | Eastwatch-by-the-Sea | ACTIVE WATCH CASTLE/PORT | Wall eastern end on Bay of Seals | Cotter Pyke, ship/crew naval supply, fewer Watch brothers | S10 |

| 27 | The Nightfort | ABANDONED CASTLE | West-central Wall | Historic Night's Watch castle with Black Gate story, no operating initial garrison | S06 |

| 28 | Queenscrown | RUINED TOWER | Abandoned watchtower in Gift south of Wall | Old tower and hill/island/pond position, no stationed army at start | S07 |

| 29 | Hardhome | RUINED FREE FOLK PORT | Storrold's Point NE of the Wall on Shivering Sea | Destruction centuries ago, cave/harbor landscape, no viable 298 metropolis | S16 |

| 30 | Storrold's Point | PENINSULA | NE Beyond Wall, projects into Shivering Sea | Coastal cliffs, route to Hardhome, remote fish/harbor | S16 |

| 31 | Screaming Caves | CAVE SITE | Cliffs above Hardhome | Known caves, 298 absence of old town shelters except caves | S16 |

| 32 | Whitetree | ABANDONED FREE FOLK VILLAGE | Haunted Forest NW of Castle Black | Four tumble-down one-room houses around weirwood, fold and well; not active city | S18 |

| 33 | Craster's Keep | HOMESTEAD | Haunted Forest north of Wall | Craster's gated sod-roof hall, earthen berm, wives and daughters, animals; no medieval castle | S15 |

| 34 | Fist of the First Men | RUINED RINGFORT/HILL | Haunted Forest near Milkwater | Ancient concentric stones on defensible hill, not active village at start | S17 |

| 35 | Gorne's Way | LEGENDARY TUNNEL | Frostfangs/under Wall vicinity | Passage tradition uncertain/supernatural context, location not mapped as verified route | S15 |

| 36 | Bridge of Skulls | BRIDGE | Near Gorge west end of Wall | Ancient defensible crossing, Watch/Free Folk strategic choke | S06 |

| 37 | The Gorge | CANYON | West end of Wall, boundary of Gift | Steep physical barrier and ravine, rope bridges and Watch patrol restriction | S06 |

| 38 | Skirling Pass | MOUNTAIN PASS | Frostfangs north/west of Wall | Free Folk and ranger mountain travel, narrow viable crossing | S15 |

| 39 | Giant's Stair | MOUNTAIN PASS | Frostfangs / northern mountains beyond Wall | Alternate challenging crossing; exactly mapped path unverified | S15 |

| 40 | Forktop | MOUNTAIN PEAK | Frostfangs | High mountain iconic profile, not a named village | S15 |

| 41 | Thenn Valley | HIGHLAND VALLEY | Northern Frostfangs, hidden among peaks | Styr Magnar, Thenn society and mining of tin/copper, bronze working; not Skagos | S15 |

| 42 | Ice River valleys | RIVER REGION | North of Frozen Shore, below always winter | Several hostile to each other Free Folk clans, rumors not generic enemy faction | S15 |

| 43 | The Milkwater | RIVER | From Frostfangs to beyond-Wall lower valley | Free Folk movement and streams, Fist near river; verify water trace | S15 |

| 44 | The Haunted Forest ranger roads | FOREST TRACKS | Northern Wall frontier | Watch patrol mobility constrained by forest, changing enemies and winter | S15 |

| 45 | Crowns of the mountains / far northern hills | HIGHLAND | North of Wolfswood west of kingsroad | Clans Wull/Liddle/Norrey/Burley/Harclay/Flint/Knott, small fastnesses | S11 |

| 46 | Wull lands by Bay of Ice | CLAN TERRITORY | Western Northern mountains, coast | Lord Hugo Wull prominent; sea raids in clan history | S13 |

| 47 | Liddle high places | CLAN TERRITORY | Mountains north Wolfswood | Torren Liddle, Morgan Liddle heir; ancient clan houses, high meadows | S14 |

| 48 | Norrey high places | CLAN TERRITORY | Highlands near Gift | Close Watch relations; no canonical named Norrey city | S11 |

| 49 | Harclay foothills | CLAN TERRITORY | Near Winterfell/Wolfswood north | Clan pastoral/forestry upland hamlets | S11 |

| 50 | Knott highland holdings | CLAN TERRITORY | Northern mountain high places | Local clan settlements without confirmed castle names | S11 |

| 51 | Burley mountain community | CLAN TERRITORY | Northern mountains | Remote mountain houses, livestock/wintertown migrants | S11 |

| 52 | House Flint highland territory | CLAN TERRITORY | Northern mountains | Distinguish mountain Flints from Flint's Finger/Widow's Watch | S11 |

| 53 | Long Lake | LAKE | Northeast of Winterfell, west Last Hearth | White Knife catchment/headwater and fisheries, river roads | S01 |

| 54 | Broken Branch | RIVER | East of White Knife, flows near Sheepshead Hills | Fisheries, crossings, Manderly lordship and irrigation where feasible | S01 |

| 55 | Last River | RIVER | North of Dreadfort/near Last Hearth | Eastern North watercourse, river crossings and fisheries | S01 |

| 56 | Weeping Water | RIVER | At Dreadfort toward Shivering Sea | Bolton fortress bank, ice fishing and seasonal river behavior | S21 |

| 57 | White Knife | RIVER | Links northern interior and White Harbor on Bite | Navigability zones, river market access, fish and flood risks | S04 |

| 58 | Blazewater Bay | BAY | Southwestern coast west of Barrowlands | Storms and ironborn approach risks, maritime trade | S01 |

| 59 | Saltspear | BAY | South of Barrowlands, west of Neck | Estuary/inlet and Barrowton water connections | S22 |

| 60 | Cape Kraken | CAPE | SW North close to Blazewater Bay | Coastal watch and Flint territories; physical sea cliffs | S01 |

| 61 | Flint's Finger | CASTLE | SW northwest coast near Cape Kraken | Castle/territory NOT equal cape polygon | S01 |

| 62 | The Rills | GRAZING REGION | West/southwest central North | House Ryswell horses, hay, villages and farms | S01 |

| 63 | The Wolfswood | FOREST | West of Winterfell / around Deepwood Motte | Forest biomass, hunting and cleared hamlets; Glover and clans | S01 |

| 64 | Sea Dragon Point | PENINSULA | Northwest mainland into Bay of Ice | Remote sparse forest peninsula, coastal seafaring | S01 |

| 65 | Skagos / Skane channel | SEA CROSSING | Bay of Seals near eastern islands | Skagos inhabited, Skane rumored depopulated; islands not one landmass | S28 |

| 66 | Deepdown | SKAGOSI SEAT | Skagos | House Crowl castle, local 298 ruler unknown | S28 |

| 67 | Kingshouse | SKAGOSI SEAT | Skagos | House Magnar seat, NOT the same as Thenn leader title | S28 |

| 68 | Driftwood Hall | SKAGOSI SEAT | Skagos | House Stane seat, local ruler unknown | S28 |

| 69 | Obsidian sites of Skagos | RESOURCE REGION | Skagos volcanic/stone terrain | Export obsidian blades/horns noted, mine quantity/foundry unknown | S28 |

| 70 | Bear Island | ISLAND | Bay of Ice west North | Maege Mormont current authority, wooded island settlements | S01 |

| 71 | Hall of the Mormonts | ISLAND CASTLE | Bear Island | Mormont seat, separate from Jeor's Watch station | S01 |

| 72 | The Neck crannogs | MARSH COMMUNITY | Marsh north Riverlands around Green Fork sources | Reed/Fenn/Peat and other families, amphibious life, floating constructions | S26 |

| 73 | Greywater Watch | MOVING CRANNOG | Southwest of Moat Cailin in Neck | Howland Reed's moving seat, no normal fixed castle coordinate | S26 |

| 74 | Moat Cailin | MILITARIZABLE RUIN | Northern edge Neck on kingsroad | Three surviving basalt towers, causeway choke, ruined keep | S27 |

| 75 | Causeway through Neck | ROAD BRIDGE | From Moat Cailin south into Riverlands | Only safe bulk-army route; marsh floods and enemy ambushes | S27 |

| 76 | Sisterton | PORT TOWN | Sweetsister in Bite | Godric Borrell, town at Breakwater and Night Lamp, fish/crab/boat services | S33 |

| 77 | Breakwater | CASTLE | Within Sisterton, Sweetsister | Borrell lord seat, NOT another island port | S34 |

| 78 | Night Lamp | LIGHTHOUSE | Sisterton, Sweetsister | Beacon, false-light crimes as contingent events, Keeper Godric | S35 |

| 79 | Belly of the Whale | INN | Sisterton on Sweetsister | Named local service venue, harbor consumers and traveler NPCs | S33 |

| 80 | The Paps | ISLAND GROUP | Northern Vale coast in Bite | Elesham associated, no additional city automatically | S31 |

| 81 | Pebble | ISLAND | Eastern Bite/Vale near Fingers | Pryor authority, small coastline and villages unmeasured | S31 |

| 82 | The Fingers | PENINSULA | Northern Vale coast / southern NW map edge | Baelish smallholding and Vale fealty, SW duplicate | S31 |

| 83 | The Twins | CASTLE/BRIDGE | Far southern NW/riverlands border Green Fork | Frey crossing, physically in Riverlands, no Stark tax/settlement | S01 |

| 84 | The Bite | BAY | Southern North water between White Harbor and Vale | White Harbor–Sisterton routes and storm freight | S04 S31 |

| 85 | The Wolfs Den | HISTORICAL SITE | White Harbor | Alias Wolf's Den prison/old keep, avoids false two forts | S05 |

| 86 | Ramsgate | SETTLEMENT/SEAT | East North coast near Broken Branch / south Widow's Watch | House Woolfield, Manderly's area of claimed oversight, location needs exact audit | S29 |

| 87 | Widow's Watch | CASTLE | East coast north of White Harbor | Distinct House Flint line and Manderly political sphere | S29 |

| 88 | Oldcastle | CASTLE | Coast southeast White Harbor | House Locke; fishing and agricultural countryside [I] | S29 |

| 89 | Karhold | CASTLE | Far eastern North beyond Dreadfort | Lord Rickard Karstark and family, road/isolation | S01 |

| 90 | Last Hearth | CASTLE | Northern North near Gift east kingsroad | Greatjon Umber seat and northern frontier livestock | S01 |

| 91 | Torrhen's Square | CASTLE | West-southwest of Winterfell by water | Tallhart estate, nearby crofts and lakeside fields | S01 |

| 92 | Castle Cerwyn | CASTLE | Near Winterfell on White Knife western branch | Medger Cerwyn current lord (not Cley); half day ride | S24 S25 |

| 93 | Hornwood | CASTLE/WOODLAND | Eastern North between Dreadfort and White Harbor | Halys Hornwood, Donella, Daryn; 298 legitimate family | S30 |

| 94 | Dreadfort | CASTLE | North bank Weeping Water east North | Roose Bolton active lord; Domeric past death | S20 |

| 95 | Deepwood Motte | FORTIFIED MOTTE | Northwest near Wolfswood | Glover family, forests and ringwork | S01 |

| 96 | Barrowtown lands (Barrowlands) | REGION | North of Saltspear SW North | Ancient barrows and Dustin farms, avoid Barrowton duplicate | S22 |



### H.1 Urban nesting and archaeology layers that must remain **separate IDs but not separate people**

**Winterfell**: location of Stark castle, not same as the seasonally occupied **winter town** immediately outside. Castle contains Great Keep, First Keep, crypts, godswood and heart tree, glass gardens, sept, stables/kennels, armory, hall, inn-like guest service and broken tower. Building-site IDs do not acquire independent census count when they are part of castle; residents belong to households/workplaces. Wintertown has abandoned houses in summer and near-capacity housing in winter from **people migrating** out of other hamlets, not newly created 15,000-person ghost population. [S02,S03,S36,S37]

**White Harbor**: source-backed largest North city at White Knife river mouth. Urban place includes the New Castle of Wyman Manderly, older Wolf’s Den/Old Castle currently used as prison, multiple wards, streets and docks, septs and coin/civic institutions. The 259-holding catalog has New Castle but **does not have separate White Harbor CITY holding**, so civic population/port/market must be actual `Settlement` with its own ID and reference to nested castle. Be aware that the `white_harbor` geographic feature **does** exist; that does not imply an actual `Settlement` registration. The Faith of Seven is especially prominent here despite larger North following old gods. [S04,S05,S29]

**Barrowton**: town at Great Barrow, House Dustin's Barrow Hall on hill, nearby Stout keep at Goldgrass less than a mile from town's east gate. Existing holdings list Barrow Hall/Goldgrass but not autonomous town holding. Town shops/granary/hostel/dyers/millers and Stout estate peasants must be distinct actual residents without counting a horse trader once in both house domains. [S22,S23]

**Sisterton**: town on Sweetsister, with Breakwater (Borrell castle), Night Lamp (lighthouse) and inn Belly of the Whale. The island's Sunderland overlordship is not the same as Godric Borrell's legal rights and beacon duties. One port population; houses, ferry crews, officials, smugglers and lighthouse keepers are overlapping roles of the same people, not separate abstract populations. [S33-S35]

**Moat Cailin**: a ruined strategically militarizable fortress with three surviving towers, causeway, collapsed walls, marsh; source memory refers to far larger historical defenses. Its physical site is **RUINED/STRATEGIC** at 298 AC, not an ordinary active populated urban market; incoming Northern host may man it later via actual troops. [S27]

**Beyond Wall**: Hardhome long-destroyed ruined village/harbor and screaming caves, Whitetree abandoned village, ancient Fist of First Men, Craster's Keep *active* compound, and numerous unmarked Free Folk villages/camps. Each of these has a different chronological status and capacity. Use real or temporary individuals in camps, not permanent settlement multipliers. [S15-S18]


## I. Government, fealty, autonomous orders, Free Folk and crossmap political models

### I.1 North is a hierarchy of land-rights and people, not just a colored Stark polygon

- **King Robert Baratheon → Eddard Stark**: Iron Throne → Warden/Lord of North; Ned's individual title and household command are distinct from the castle walls. Robert temporarily visiting Winterfell should have `currentPosition` in Winterfell but `domicile` in King's Landing and realm office unchanged.
- **Stark → local titled houses**: Umber, Karstark, Bolton, Manderly, Dustin, Glover, Hornwood, Tallhart, Ryswell, Cerwyn, Mormont, Locke, Flint lines, Reed, and numerous smaller lords. Some have subordinate vassal houses; the repo currently simplifies many holding parents to `north_domain`. Step 2.2 must not equate this scaffold with exact real feudal dependencies.
- **Manderly sphere**: Lord Wyman's power around White Knife/White Harbor and East North, with claims to certain houses (Locke, Woolfield and Widow's Watch Flint), independent of the basic *Stark → all houses* structure. Political administration/territorial claim and actual landed ownership require separate fields. [S29]
- **Dustin/Goldgrass**: Stout's Barrowton-adjacent stronghold has separate landed rights, while Barbrey Dustin dominates Barrowton/Barrowlands; genealogy of Barbrey/Ryswell/Bolton shapes diplomacy, not fabricated “Lady Stout of Barrow Hall”. [S22,S23]
- **Northern clans**: about forty mountain clans, with Wull, Liddle, Norrey, Knott, Burley, Harclay and Flint among notable groups, partially autonomous pastoral people with small fastnesses and informal fealty. Generic equal-size castles or uniform vassal `popWeight` would misrepresent them. [S11,S12]
- **Neck crannogmen**: House Reed and other local families subject to Stark but employ swamp-based life, special mobility, marsh routes and differently sized settlements. Greywater Watch's moving site cannot be trivially represented by a fixed market/road anchor.
- **Night's Watch**: nonhereditary male sworn military/religious-adjacent institution with its own land, elections, command, obligations, houses and supply network. Brothers' pre-vow family identities persist; they do not inherit ordinary lands after taking vows. The Watch may receive taxes/donations/resources without passing them through Stark households as duplicates.
- **Free Folk**: no unifying hereditary feudal lord in ordinary period. Mance's coalition brings together different chieftains and peoples, with alliances, followers and different practices. It is a dynamic `NonDynasticSociety`, not a permanent royal bloodline or province with a feudal castle. Thenn Magnar discipline and Old Tongue make them particularly distinct.
- **Skagos**: nominal Stark suzerainty but historically rebellious and culturally distinctive. House Magnar of Skagos and Magnar of Thenn are **not the same dynasty/title** despite name. Crowl/Stane/Magnar island houses must have different family identity and local governing relationships.
- **Three Sisters**: islands geographically in north's map viewport but **Vale** jurisdiction: House Arryn → Sunderland → Borrell of Sweetsister/Breakwater, Longthorpe of Longsister, Torrent of Littlesister. Even if Sisterton trades heavily with White Harbor, House Stark does not gain its feudal taxes by proximity. [S31-S35]
- **South-map edge**: The Twins/Green Fork crossing and Fingers/pebble/Paps belong to Riverlands/Vale. A visually duplicated feature is one physical place with one governing hierarchy, not an additional NW “north territory”.

### I.2 Chronology / claims / current government resolution

`ScenarioOwnership(asOf=298AC)` should support `{legalOwnerDynastyId, deJureOwnerDynastyId, currentHolderNpcId, localGovernorNpcId, liegeHoldingId, titleId, officeOrganizationId, militaryDuty, taxesRights, currentControlParty, sourceConfidence}`. A site can be called a castle by historic atlas, controlled by a commander on service, but be owned by a dynasty in law; neither means the specific person was living there at the royal arrival. Historical Fealty and future occupation snapshots are future event records and **must not overwrite** starting state. For Mance/Watch/Skagos, adapt concept to their own legitimate society rather than forcing a generic lordship.

## J. Free Folk society deep research: communities, camps, law, labor and demographics

### J.1 The major communities are not one homogeneous “wildling” template

The Free Folk encompass families and shifting groups with distinct regional housing/materials, trade and laws. Published claims about some groups can be limited to frightened Westerosi narrators or the stories of other Free Folk. Capture **belief/rumor vs established practice**: reports of cannibal ice-river clans or strange supernatural threats do not establish that all beyond-Wall residents behave alike.


| # | Group | Institution/people | Rough map location | Governance / source caveat | Livelihood/identity | Game-model prohibition |
|---|---|---|---|---|---|---|

| 1 | Mance Rayder’s coalition | political coalition | mobile beyond Wall from Frostfangs and forest | Mance, former Watchman, gathering followers to escape northern peril, not a hereditary throne | multi-cultural coalition; followers already belong to real households/camps | camps change location and allegiance; no universal capital |

| 2 | Thenns | valley society | Hidden Thenn valley in northern Frostfangs | Magnar **Styr** at series-era, separate from Skagos Magnars; son Sigorn later succession | Own laws, metallurgy from local copper/tin and bronze arms; Old Tongue | bronze and mining production need actual ore/labor/forge, not fake “Magnar House Thenn” at start |

| 3 | Hornfoots | clan culture | Beyond Wall southern/northern regions, often near Frostfangs | Distinct foot-binding/hard feet customs (book accounts); named general chief unknown | mobile families, livestock/hunting; regional placement only rough | No point location for entire culture |

| 4 | Ice-river clans | clan group | north of Frozen Shore south of Always Winter | Former conflict with Frozen Shore people; reports of cannibalism | rivers/ice, seasonal food; kin-bands and camps | Do not make cannibalism required trait of every individual |

| 5 | Frozen Shore Free Folk | coastal groups | northwestern frozen coast | Communities distinct from ice-river clans; raiding/history and negotiated peace | maritime fish/seals or trade where source/suitability permits | No invented single lord of coastline |

| 6 | Cave dwellers | mobile/settled groups | Frostfangs and rocky beyond-Wall slopes | Caves/shelters documented as community setting; exact leaders unknown | cave access, fuel/food, hunting [I] | Do not collapse into separate species |

| 7 | Forest Free Folk | village/family | Haunted Forest region | Whitetree and other mapped villages; Craster is exceptional, not the baseline | hunting, limited sheep/pigs, firewood, shrines [I/C] | Some villages abandoned before hero visits; determine status date |

| 8 | Rattleshirt followers | warband | mobile at north of Wall, precise day-zero location unknown | Lord of Bones/Rattleshirt is named leader later; exact initial strength unverified | warband people have household and origin even while traveling | No static fortified metropolis |

| 9 | Harma Dogshead followers | warband | mobile Beyond Wall | Harma military/raider leadership later confirmed, precise opening-day camp unknown | nomadic camp and military roles not additive population | Do not pre-position at Wall siege |

| 10 | Tormund Giantsbane band | mobile group | Far north beyond Wall, exact location unknown | Tormund living Free Folk leader, family and followers where dated | hunter/herder/warrior jobs overlap | No giant population inferred from epithet |

| 11 | Varamyr Sixskins circle | individual followers | Beyond Wall | Skinchanger account and possessed animals requires unique actor/ability system | animal bond separate from population, no automatic species-wide gift | No generic “all wildlings are skinchangers” |

| 12 | Giants | nonhuman intelligent population | mountains and northward Beyond Wall | Separate species/social entities with extremely uncertain counts | food/movement and large architecture [M] distinct from humans | Never double count as human wildling village residents |

| 13 | Children of the forest | nonhuman tradition/disputed current sites | remote forests/caves | Reports and exceptional survivors, precise current location not map-ready | cultural memory/spiritual not generic old-gods NPCs | Do not globally spawn them by percentage |

| 14 | Others/wights | supernatural hostile phenomena | unknown northward and later sightings | Narrative events are separate from normal census/ecology | requires event/state, magical rules and lore knowledge filters | No random established army of Others at tick zero |

| 15 | Craster family | exceptional permanent homestead | Haunted Forest | Craster adult man with wives/daughters/family and livestock; personal harm is biography, not community norm | gated compound, sheep, pigs, Watch transactional contacts | Resident women are individuals with their own health/social status; no anonymous workforce |



### J.2 Free Folk population movement and legal realism

A single stable Free Folk NPC can move among forest huts, a cattle grazing camp, a large war camp, a cave and a trading rendezvous without being born or despawned each time. Household and social-affiliation references remain stable; `currentPosition` changes by travel, group roster follows actual commanders, food stock travels or is consumed, and reproduction/death occurs through the unified lifecycle model. There is **no canonical complete population total** for all beyond-Wall clans. A user-authored scenario might use 20–150 [M] people in a small household hamlet, 100–1,200 [M] in aggregated seasonal encampments, but these are **micro-site design trial envelopes** and never evidence for a region-wide census. Do not pre-place Mance's later mass migration or southern assaults at start.

The Free Folk and Watch communicate, trade, raid and form rivalries in the story, with significant differences by culture and individual. A character-specific belief in the Others, old gods, or Mance's right to lead should be generated from upbringing, knowledge, actual experiences and region, not a single continent-level “wildling aggression=100%”. Watch patrols that meet Craster should become known by **actual recurring person identities**. Traveling family members remain real people after reentering the simulation as fully-rendered Minecraft NPCs.


## K. Named character seed roster, kinship, offices, current whereabouts and future-event exclusions

The repository's `population_packs/north_core.json` currently initializes only a **small core** of North people, not the lords of every holding. The `characters/*.json` and `population_packs/*` include some royal/Watch names but **do not fully populate** this roster. These are *research identity reservations and dated office leads*, not a claim all listed NPC JSONs already exist. The actual starting `currentPosition` must obey the exact royal-arrival event and each person's known location **if specifically attested**; otherwise preserve `UNKNOWN` and place under an explicitly provisional residence policy. Historic dead persons belong in genealogy/history records but not alive census.


| # | Person | House/order | 298 residence/whereabouts | Known role at opening | Historical exclusion / integration |
|---|---|---|---|---|---|

| 1 | Eddard Stark | house_stark | Winterfell | Lord Stark, Warden North, Robert's host | repo authored eddard_stark; do not make Hand yet |

| 2 | Catelyn Stark | house_stark / house_tully | Winterfell | Lady Stark, Stark/Tully bridge | repo authored, active mother role |

| 3 | Robb Stark | house_stark | Winterfell | Heir, not King in North | repo authored, alive |

| 4 | Sansa Stark | house_stark | Winterfell | Daughter, unmarried | repo authored, betrothal not yet offered |

| 5 | Arya Stark | house_stark | Winterfell | Daughter, unmarried | repo authored, direwolf puppy already found |

| 6 | Bran Stark | house_stark | Winterfell | Son, healthy at royal arrival | repo authored, fall future |

| 7 | Rickon Stark | house_stark | Winterfell | Youngest son | repo authored, no Skagos future travels |

| 8 | Jon Snow | Stark household, Snow surname | Winterfell | Raised as Ned's acknowledged son, not yet Watch brother | repo authored, no vows/Watch service yet |

| 9 | Theon Greyjoy | house_greyjoy, ward | Winterfell | Hostage/ward, not a Greyjoy island governor | repo authored, resume same NPC if travels |

| 10 | Vayon Poole | house_poole | Winterfell | Stark household steward | create actual service NPC, daughter Jeyne distinct |

| 11 | Jeyne Poole | house_poole | Winterfell | Vayon's daughter and Stark household childhood companion | individual NPC, no 299 substitution |

| 12 | Maester Luwin | Citadel service | Winterfell | Maester, castle medical/library/raven role | person and maester organization, cannot inherit Stark lands |

| 13 | Ser Rodrik Cassel | house_cassel | Winterfell | Master-at-arms of Winterfell | same man trains guards/commands only when present |

| 14 | Jory Cassel | house_cassel | Winterfell | Captain Stark household guards | one garrison officer, future death not preapplied |

| 15 | Septa Mordane | Faith of Seven | Winterfell | Governess of Stark daughters | minority sept teacher; person and faith org |

| 16 | Septon Chayle | Faith of Seven | Winterfell | Septon of Winterfell small sept | religious NPC, not old-gods default |

| 17 | Hullen | Stark stables | Winterfell | Master of horse | horse inventory, stable workplace, fodder |

| 18 | Farlen | Stark kennels | Winterfell | Kennelmaster | dogs as animal entities with stock |

| 19 | Mikken | Stark smithy | Winterfell | Blacksmith and metalworker | real job and tools, no abstract extra smith |

| 20 | Gage | Stark kitchens | Winterfell | Cook | food inventory/meal prep, household staff |

| 21 | Hodor (Walder) | Stark household | Winterfell | Stable worker, known distinctive person | no duplicate as “giant” species |

| 22 | Old Nan | Stark household | Winterfell | Elder storyteller/servant | memory and cultural oral-history transmission |

| 23 | Barbrey Dustin | house_dustin, born Ryswell | Barrow Hall | Lady of Barrowton/Dustin widow | family relationships, estate stewardship |

| 24 | Rodrik Ryswell | house_ryswell | Rills | Lord Ryswell | father-in-law kinship/history |

| 25 | Medger Cerwyn | house_cerwyn | Castle Cerwyn | Lord of Cerwyn at 298 | critical correction: Cley is heir, not lord |

| 26 | Cley Cerwyn | house_cerwyn | Castle Cerwyn | Heir, young man | no lordship/299 death applied |

| 27 | Jonelle Cerwyn | house_cerwyn | Castle Cerwyn | Medger's daughter | later rule not initial |

| 28 | Roose Bolton | house_bolton | Dreadfort | Lord Bolton, Stark bannerman | no Warden title or Winterfell lordship yet |

| 29 | Ramsay Snow | house_bolton (illegitimate) | Bolton area near Weeping Water, exact residence audit | Recognized bastard, not Bolton of Winterfell | individual legal/parentage state matters |

| 30 | Domeric Bolton | house_bolton | dead before 298 | Deceased legitimate Bolton heir | NO living NPC counted; historical biography |

| 31 | Halys Hornwood | house_hornwood | Hornwood | Current lord | later Battle Green Fork death not initial |

| 32 | Donella Hornwood | house_hornwood, née Manderly | Hornwood | Lady Hornwood, Wyman kin | marriage and claim links |

| 33 | Daryn Hornwood | house_hornwood | Hornwood | Halys's legitimate heir, alive | later death not preapplied |

| 34 | Larence Snow | house_hornwood illegitimate line | North, location not verified | Halys's natural son | legal inheritance contested only if later events arise |

| 35 | Galbart Glover | house_glover | Deepwood Motte / initial location needs check | Head of Glover household | actual current garrison/travel as of scenario not guaranteed |

| 36 | Robett Glover | house_glover | Glover sphere, current location unknown | Galbart's brother | different person/war role |

| 37 | Rickard Karstark | house_karstark | Karhold | Current Lord Karhold | alive; no execution |

| 38 | Harrion Karstark | house_karstark | Karhold or location uncertain | Heir | alive, captive status future |

| 39 | Alys Karstark | house_karstark | Karhold / family location unknown | Daughter | later marriage to Sigorn future |

| 40 | Jon Umber (Greatjon) | house_umber | Last Hearth | Current lord of Last Hearth | no future Red Wedding imprisonment |

| 41 | Jon Umber (Smalljon) | house_umber | Last Hearth | Greatjon's eldest son/heir | alive, not 299 deceased |

| 42 | Mors Umber (Crowfood) | house_umber | Last Hearth area | Greatjon's uncle, future joint castellan role | don't install as day-zero lord if Greatjon present |

| 43 | Hother Umber (Whoresbane) | house_umber | Last Hearth area | Greatjon's uncle, future castellan role | independent person / different political later allegiance |

| 44 | Wyman Manderly | house_manderly | White Harbor / New Castle | Lord of White Harbor | house patriarch and Faith of Seven |

| 45 | Wylis Manderly | house_manderly | White Harbor or current locale uncertain | Wyman's son | later captivity not applied |

| 46 | Wendel Manderly | house_manderly | White Harbor or current locale uncertain | Wyman's son | later Red Wedding death not applied |

| 47 | Marlon Manderly | house_manderly | New Castle | Castle garrison command in later account | office exact 298 tenure verify, don't auto spawn excess guard |

| 48 | Maege Mormont | house_mormont | Bear Island | Lady Mormont | Jeor Mormont's separate sworn Watch life |

| 49 | Dacey Mormont | house_mormont | Bear Island/current location unknown | Maege's daughter | future death not applied |

| 50 | Alysane Mormont | house_mormont | Bear Island/current location unknown | Maege's daughter | person/family and military service if present |

| 51 | Helman Tallhart | house_tallhart | Torrhen's Square | Lord Tallhart | future Green Fork death not initial |

| 52 | Benfred Tallhart | house_tallhart | Torrhen's Square | Helman's son/heir | future ironborn violence not initial |

| 53 | Eddara Tallhart | house_tallhart | Torrhen's Square | Helman's daughter | not initial ruled Lady Tallhart |

| 54 | Howland Reed | house_reed | Greywater Watch / location mobile | Lord Reed, Eddard's past companion | fixed pixel invalid: moving marsh seat |

| 55 | Meera Reed | house_reed | Greywater Watch sphere | Howland's daughter | future Bran journey not at 298 tick 0 |

| 56 | Jojen Reed | house_reed | Greywater Watch sphere | Howland's son | future Bran journey not at 298 tick 0 |

| 57 | Jeor Mormont | Night's Watch, born house_mormont | Castle Black | Lord Commander Night's Watch | already abdicated Bear Island lordship |

| 58 | Benjen Stark | Night's Watch, born Stark | Castle Black / ranging location uncertain | First Ranger | Watch membership, not Stark regional lord |

| 59 | Bowen Marsh | Night's Watch | Castle Black | Lord Steward | real supply logistics/workforce |

| 60 | Maester Aemon | Night's Watch/Citadel, Targaryen birth | Castle Black | Maester, former prince | no present Targaryen kingship |

| 61 | Alliser Thorne | Night's Watch | Castle Black | Master-at-arms | active but office authority under Jeor |

| 62 | Yoren | Night's Watch | on recruiting mission, exact starting location uncertain | Wandering crow/recruiter | person moves/recruits without duplicating |

| 63 | Denys Mallister | Night's Watch, from House Mallister | Shadow Tower | Commander of western Watch castle | ~200-post roster subset |

| 64 | Cotter Pyke | Night's Watch | Eastwatch | Commander Eastwatch | crews/jobs same personnel |

| 65 | Qhorin Halfhand | Night's Watch | Shadow Tower sector | Senior ranger from Watch | later death/event future |

| 66 | Mance Rayder | Free Folk | Beyond Wall, day-zero camp unknown | King-Beyond-the-Wall claimant/coalition leader | not fixed royal city or named hereditary house |

| 67 | Tormund Giantsbane | Free Folk | Beyond Wall, location unknown | Free Folk leader/warrior | epithet does not imply giant identity |

| 68 | Styr | Thenn | Thenn valley north Frostfangs | Magnar of Thenn (leader) | distinct from House Magnar of Skagos |

| 69 | Sigorn | Thenn | Thenn valley | Styr's son | later lord and Alys Karstark marriage future |

| 70 | Craster | Free Folk | Craster's Keep | Patriarch of isolated household | abusive legal/family history not universal Free Folk norm |

| 71 | Gilly | Free Folk / Craster family | Craster's Keep | Individual young woman, daughter/partner in household | her future travels not initial |

| 72 | Hugo Wull | house_wull | Northern mountains west along Bay Ice | Wull clan chief, Big Bucket | actual known clan head, no castle ID guessed |

| 73 | Torren Liddle | house_liddle | Northern mountains | Head of clan Liddle | actual title vs personal name |

| 74 | Morgan Liddle | house_liddle | Northern mountains | Heir to clan head | future political action not initial |

| 75 | Triston Sunderland | house_sunderland | Three Sisters / seat exact uncertain | Lord Three Sisters under Arryn | Vale allegiance; do not double census across islands |

| 76 | Godric Borrell | house_borrell | Sisterton, Sweetsister | Lord Sweetsister and Keeper Night Lamp | false lights can be historical individual behavior, not every Sisterman trait |



### K.1 Household and court ecosystem — why additional named people matter

The royal party brings Robert/Cersei/Jaime/Tyrion and accompanying King's Landing guards, attendants, mounted escort and royal service workers. **None** becomes permanent Winterfell residents because the map tick observes them inside castle walls. Eddard and Catelyn host them via **temporary guest locations, household food demand, stable pasture/fodder and guard security**. Specific named Stark servants have jobs that must produce real service: Maester Luwin's letters/medical treatment, Vayon Poole management, Jory's guard schedules, Rodrik Cassel's training, Mikken's equipment production, Gage kitchen work, Hullen horse inventory, Farlen dogs/kennels, and Septa Mordane/Septon Chayle faith and instruction.

An authored noble NPC's house/residence is not synonymous with the household's headcount. `NpcIdentity` and genealogies must include marriage, blood relation, legal bastard status (Jon Snow, Ramsay Snow, Larence Snow), historical deceased heirs (Domeric Bolton), youths' ages/status and recognized ward status (Theon Greyjoy). Real household budgets and staff employment tie to actual inventories and land revenue, not decorative profiles. Knights, maesters, septons, smallfolk and old-gods priests have different authority systems.

### K.2 Date-scoped office risks

`Medger Cerwyn` is starting lord, Cley later inherits; `Halys Hornwood` is alive, Donella/Daryn's subsequent succession crisis is future; `Rickard Karstark` lives and has multiple living offspring; `Jon Umber` Greatjon is not captive and Smalljon is alive; Jeor leads Watch rather than Maege's Bear Island; `Jon Snow` is still **not a sworn Watch member** at tick zero; `Janos Slynt` and `Samwell Tarly` must not be automatically at Castle Black from later POV timelines. `Triston Sunderland` and `Godric Borrell` are of **the Vale**, though their islands lie in NW geography. `house_thenn` as a newly minted legal house is a *post-start future* and Styr's older tribal title is different. [S01,S06,S25,S28,S31]


## L. Civilian population, farm and labor model — research-to-2.2 authoring quantities

No published book gives an authoritative 298 AC census of The North, the entire Free Folk, all the islands or every keep's population. The existing `landed_holdings.json` uses **abstract** `populationWeight` and `militaryValue`; these are NOT census/army measurements. We must preserve a simultaneous calculation in which every individual exists in one household, at one official residence, employed for finite daily time, able to travel and change role. The following are **PROPOSED mod calibration envelopes** only and must be reconciled with regional farmable capacity, winter stock and world total before enabling runtime.

### L.1 Settlement-scale target templates [M]

| Archetype | Year-round settlement population **candidate** | Seasonal/visitor count rule | Local guard model | Why this is only a planning prior |
|---|---:|---|---|---|
| White Harbor full city incl New Castle neighborhoods | 18,000–65,000 | Foreign mariners/merchants temporary, not residents | 500–2,000 persons drawn from existing population and paid guards | No canon urban census; very different from Manderly realm population |
| Barrowton as town incl Barrow Hall footprint | 2,000–12,000 | Travelers and seasonal harvest merchants not extra residents | 60–350 [M] person subset | Historic built footprint and provisioning capacity need map geometry |
| Winterfell **castle** staff/household | 250–1,100 | Royal party temporaries separate | 50–220 [M] guarding subset | Castle staff only; **not** all North nor the winter town |
| Winter town immediately outside castle | Currently largely vacant in long summer, **not a constant full census** | **~15,000** when filled in winter is semi-canon/source-derived rough capacity; migrant villagers must be moved, not created | Shared Stark watch / small town guards, not a second army | Houses ~80% vacant in summer; seasonal occupancy and return essential [S03] |
| Major Northern castle seat and immediate attendant hamlet | 80–600 [M] at distinct site | Liege domain villages separate, visitors excluded | 20–150 [M] existing humans | This is *not* total holding domain, local climate huge effect |
| North rural inland hamlet | 25–400 [M] | Some seasonal grazing families | Mostly part-time watch, not trained professional garrison | Unnamed extra hamlets follow productive land, roads and water |
| North coast fishing village | 20–350 [M] | Fishing fleets out at sea remain resident | 0–35 [M] local watch | Catch and boat capacity seasonal; storm losses |
| Northern mountain clan fastness | 10–180 [M] per local hamlet | Winter migrants towards wintertown/castles | 0–40 [M] depending on militia, subset | 40 clans do not imply only 40 houses or 40 precise site dots |
| Crannog cluster in Neck | 10–130 [M] per cluster | Canoes/moving homes, census tied to actual households | 0–25 [M] per clan if organized | Moving crannogs require geographic dynamic location |
| Skagosi village | 20–220 [M] per real site | Trade voyages temporary | 0–40 [M] | Actual total Skagosi population unknown |
| Sister Island port (Sisterton) | 500–3,500 [M] in port footprint | Foreign crews and ships transient | 20–130 [M] local watch | Unlike Three Sisters entire region census |
| Gift surviving village | 5–120 [M] per actual site | Some migrate away, Watch work contracts | 0–15 [M] guard if justified | Land capable of cultivation can be empty and unproductive |
| Free Folk forest village | 10–150 [M] per site | mobile families, seasonal camps | no standing feudal garrison | One shared group, no fixed town if temporary |
| Watch Castle Black | nearly 600 sworn brothers [C] | rangers away count as **same people** | guard/ranger subset of ~600 | active Watch institutional census, not civilian town population |
| Watch Shadow Tower | about 200 sworn brothers [C] | ranger parties still counted here as posted | patrol subset of ~200 | not another 2000 by multiplying castle population |
| Watch Eastwatch | smaller remaining Watch garrison [C] | sea crew away count as posted | naval/guard subset | All three sum to fewer than 1000 brothers [S07] |
| 16 abandoned Wall castles | **no established active stationed population** | visiting patrols temporarily | **0 standing permanent Watch garrison** | Ruin existence ≠ civilian town |
| Hardhome/Whitetree ruins | no established active normal town census | potential irregular visitors | no town garrison | Whitetree abandoned; Hardhome historically destroyed |

**Calibration rule:** estimates for rural settlement archetypes are **per site**; they do not produce a total “North population” until actual site density, cultivable hectares, stock and birth/death/migration are jointly resolved. Non-resident visitors and armies must never be double-counted under both port and home town.

### L.2 Employment shares, by employed adults (not total population) [M]

| Archetype | Food production / fish / forestry | Skilled craft/construction/extraction | Trade, merchant and transport | Household, civic, teaching, health, religion | Military, policing, Watch patrol | Adult-worker total |
|---|---:|---:|---:|---:|---:|---:|
| North agricultural village | 64% | 12% | 5% | 15% | 4% | 100% |
| North castle and estate cluster | 42% | 17% | 8% | 20% | 13% | 100% |
| White Harbor dense urban | 12% | 29% | 29% | 22% | 8% | 100% |
| Barrowton regional market | 31% | 23% | 19% | 21% | 6% | 100% |
| Wintersupply town (winter seasonal) | 18% | 25% | 13% | 35% | 9% | 100% |
| Wolfswood forestry settlement | 51% | 24% | 10% | 12% | 3% | 100% |
| Western Rills pasture and horses | 61% | 16% | 10% | 10% | 3% | 100% |
| Northern mountain clan community | 60% | 13% | 6% | 15% | 6% | 100% |
| Neck crannogmen | 62% | 12% | 13% | 10% | 3% | 100% |
| Coastal fishing village | 61% | 16% | 14% | 6% | 3% | 100% |
| Sisterton island port | 35% | 22% | 25% | 13% | 5% | 100% |
| Watch Castle Black | 0% direct farm by default | 25% builders/repair | 5% transport/couriers | 35% stewards/kitchen/healer/training | 35% rangers/guards | 100% |
| Watch Shadow Tower | 0% direct farm by default | 20% | 8% | 27% | 45% | 100% |
| Watch Eastwatch | 0% direct farm by default | 20% | 21% sea/transport | 24% | 35% | 100% |
| Free Folk hunting/hamlet | 67% | 10% | 4% | 13% | 6% | 100% |
| Thenn valley specialist community | 50% | 27% metallurgy/other | 5% | 10% | 8% | 100% |

Every military or Watch job count is a **subset of the same population**, and the table percentage is not proof every worker is full-time every day. Jobs/childcare/elder care and seasonal employment require time-budget accounting. Men/women roles, church positions, guild memberships, child apprenticeships and conscript levies change across culture, person and event. No Canon shares are being asserted.

### L.3 Household conservation and demographic contracts

For any locality `L`, `residentPopulation(L)` equals **live individual persistent IDs domiciled in L** plus **unexpanded disjoint cohort members with primary domicile L**. It does not equal current physically visible entities. For a legal realm `R`, the total should be the sum of **distinct resident places**, not `SUM(holding.populationWeight)` and not repeated city/keep/region nested records.

An individual may have one `householdId`, one `homeSettlementId`, one `currentPosition`, one primary employed work schedule and multiple social memberships. A member of the Night's Watch may retain a birth family but not claim inherited holding after vows; an Island ship captain may be temporarily at sea but still belong to a Sisterton household; a northern hill clan resident who goes to winter town *migrates* rather than being born there. Death changes estate inheritance/succession and both food demand and labor; births require corresponding mothers/fathers, age and household changes.

## M. Farming, climate, resource stock, crop calendars and rural architecture

### M.1 Actual source evidence vs plausible crops

The map/lore establish strong regional differences, not a published Northwide table of yields. **Winterfell glass gardens** specifically support produce/flowers in winter due geothermal heat [S36]. Other North areas have cold climates with severe winters, sparse named crop details; **barley, oats, rye, vegetables, root crops, legumes and fodder/hay are ecological proposals** `[I]` to be admitted only after climate/soil suitability. Fish, crabs, timber, fur, sheep, horses, herring, forest game and pasturing are regionally plausible; source-backed specialities like Manderly trade and Skagosi obsidian should be cited by place. **Do not claim exact grams of grain per character in canon**. The existing agriculture file's `gramsPerPersonPerDay=800`, `grainKgPerHectare=700` for the North are `[M]` calibration inputs and may not represent full diet/meals.

### M.2 Source-to-economy chain for a real farm

1. Resolve correct `WorldLocation` and **actual finite field polygon**, avoiding marsh, cliffs, ruins, roads, settlement footprints and thick woods unless cleared.
2. Determine pasture/forest/crop suitability as a function of local biome, soil depth, water table, temperature, water availability and **long summer/winter**; land parcel/owner references link to field/holding not arbitrary castle marker.
3. Assign named crop/animal with seed reserve, planting day range and harvesting interval. Distinguish rain-fed, irrigated and greenhouse plots; the latter only where architecture/heat support it.
4. Recruit actual farmers and farmhands from the local census with property/tenant contracts and time schedules. Mobilized guards reduce labor capacity rather than clone new workers.
5. Use existing `HoldingAgricultureCatalog` → agricultural day enrollment/yield model and inventory/transaction authority. Mod-estimated productivity must be defended by test calculations of harvested kilograms, retained seed, fodder, spoilage and person-days.
6. Transfer harvest from field to real households, lordly tithe store and trader inventory *once*, conserving resources; reserves consumed in winter, death/starvation/emigration handled in demographics.
7. Allow policies (tax, road safety, requisition, building, war, plague, harsh winter) to change outcomes with actual changes in people, stocks, money and land condition.
8. Connect wood/fuel to chopping labor and sustainable stocks; hard northern winter often makes fuel a survival good as important as raw grain.

### M.3 Regional extraction and provisioning roles

| Area | Products and workplaces (tags) | Labor / land restrictions | Freight/export partners | Governance/economy trap |
|---|---|---|---|---|
| White Harbor | shipyards, markets, fish smoking, grain warehouses, timber/craft [C/I] | docks/riverbank real buildable frontage, ships + crew | River White Knife, southern ports, Sisterton | New Castle court population shares city census |
| Winterfell and central farmlands | glass gardens [C], livestock, hay, grain/vegetables/forest timber [I] | castle geothermal facilities tiny relative to regional farms | kingsroad and nearby farms; White Harbor | Glass gardens not infinite produce, seasonal town migrants counted once |
| Barrowton/Rills | grain/pasture, horses, wool, mills, regional craft [I] | winter hay and stables, seasonal rural workforce | inland to Winterfell, west coast | Stout/Dustin/Ryswell estates separate ownership |
| Wolfswood/Deepwood Motte | timber, hunting, charcoal/woodworkers, sparse crops [I] | forest must be cut/regrow; carrying capacity low | northern roads/Barrowton/Winterfell | No instant infinite logs, logging is employment |
| Dreadfort/Weeping Water | river fish, mixed cattle/fields, mills [I] | partial ice, river crossing, winter losses | northern roads/coastal ferries when possible | Roose's personal property not all forest and villages |
| Karhold/Last Hearth | cold crops, sheep, livestock, woods [I] | far north seasonal supply windows, possible raid insecurity | road links south, White Knife eastward routes | no army food created on muster |
| Bear Island | fish, forestry and small holdings [I] | storm crossings, livestock grazing pockets | Bay of Ice/Bite ports indirectly | Jeor Watch office not Bear Island authority |
| Skagos | pelts, obsidian and unicorn horns as reported [C]; fisheries [I] | dangerous rocks/crossings, limited crop soil | shipping to North when permitted | rumored cannibalism not labor commodity mechanic |
| Neck | marsh fish/reeds/boat crafts, raised gardens [I] | saturated soil, no bulk transport away from causeways | Green Fork/coastal border, Moat Cailin | floating crannog cannot pin single permanent coordinate |
| Three Sisters | fish/crab/goat cheese, sea services, smuggling possibilities [C/I] | reefs/fog, small farms, town freight | White Harbor, Vale coast | old pirate history not proof each merchant is criminal |
| Gift | derelict fields, surviving farms, Watch provisioning [C/I] | depopulation, little labor, northern climate | Wall posts from Kingsroad | all empty farmland does NOT generate revenue |
| Beyond Wall forest | hunting/traps, sheep/pig homesteads, timber/fuel [C/I] | seasonal camp mobility, no guaranteed grain fields | local barter, encounters, occasional Watch | Free Folk are not Westerosi tax serfs |
| Thenn valley | copper/tin/bronze [C], animal/herd/forage [I] | mines/smelter fuel, valley access/food supply | neighboring Free Folk / coalition | Thenn metallurgy cannot multiply infinite ore |

## N. Military, organization personnel, logistics and feudal levy limits

**Never collapse four different counts:** (1) theoretical adult fighting-age pool, (2) castle guards and household sworn men, (3) levies summoned/contracted and (4) field army currently deployed. A fifth is the Night's Watch, which is **not** an additive independent North levy under Eddard. The often-cited possible Northern muster in secondary materials is NOT the number of soldiers alive and marching on day 1; Nor is `militaryValue=0.78` a decimal count of soldiers.

### N.1 Northern contingents and real commanders

| Source of men | Day-zero command & relationship | Mobilization and production consequence | Special military architecture |
|---|---|---|---|
| Stark core and sworn household | Eddard Stark, household guard Jory Cassel, master-at-arms Rodrik Cassel | Real people guarding royal party and estate; some must stay on farms | Winterfell walls/stables/armory |
| Bolton | Roose Bolton, legitimate sworn Stark bannerman | Rural labor and political loyalties, no pre-scripted betrayal | Dreadfort on river bank |
| Manderly | Wyman Manderly and local captain; castle garrison/harbor | Urban workforce, traders, ship crews require separate employment | White Harbor New Castle, wall/harbor |
| Umber | Greatjon Umber, Smalljon heir; northern frontier militia | sheep and upland farmworkers mobilized/food impact | Last Hearth fort/roads |
| Karstark | Rickard Karstark and sons | northern long distances/delayed muster and farm labor loss | Karhold fortified seat |
| Glover | Galbart Glover family | woodcutters/farm labor at risk; travel through woods | Deepwood Motte palisade |
| Tallhart / Cerwyn / Hornwood | Helman/Medger/Halys alive at start | smaller households/estates, source dates for death and claim | named keeps and approach roads |
| Ryswell / Dustin / Stout | Rodrik/Barbrey/lesser lords | horse breeding and food production impacted by horses/levies | Barrowlands villages/fastnesses |
| Mormont | Maege Mormont | island seaborne mobilization requires ships with actual crew | Bear Island stronghold |
| Crannogmen / Reed | Howland Reed and kin | ambushes, swamp travel, difficult supply; marsh ecology | moving Greywater Watch, Moat Cailin causeway |
| Mountain clans | ~40 different clan groups, named Wull/Liddle/Norrey etc | fragmented travel; personal ties, highland subsistence rather than large feudal levy ledger | small holdfasts/pass defenses |
| Night's Watch | Jeor Mormont, Mallister, Pyke at 3 staffed forts | own finite <1000 members, food supply from Gift/support, not Stark levies | ice Wall, three active castles |
| Free Folk (Mance) | mobile coalition leaders with clan identities | cross-tribe alliance, supply forage/raids, population migration if event occurs | temporary camps and passes, not phantom towns |
| Three Sisters | Triston Sunderland, Godric Borrell etc under Arryn | insular fleets/harbor guards, not North levy manpower | Breakwater/Night Lamp, small ports |

### N.2 Military acceptance invariants

- `NpcId` must appear in one person census, regardless of household, armed force and current position. `enlistment` changes work allocation; it never materializes fresh population without debiting a cohort/person pool.
- A muster may collect tribute, horses, weapons, food, wagons and labor only from real owned inventories and available paths. An isolated mountain garrison cannot automatically share White Harbor's food as teleportation.
- A person can be a **former** peasant, current conscript soldier and member of a house; wage and profession history remains coherent. Demobilization updates farm reemployment and household reunification; casualties remove people, reduce food demand and create inheritance/memory events.
- Watch sworn brothers have distinct vows, chain of command, pre-Watch surnames and mobility; avoid enrolling Jon Snow before day zero vows or Jeor as simultaneous ruling Bear Island patriarch.
- Ruined forts at Moat Cailin, Nightfort/16 ghost Wall castles and ancient ringforts cannot simultaneously supply unproven battalions. A temporary unit may occupy one while remaining from its actual parent roster.
- Full-siege systems use buildings, food/water and road access, defenders, civilians, siege works and changing morale rather than castle battle values alone.
- Northern coastal ships and island boats need finite crew, repair/carpenter production and seasonal sail conditions. Do not initialize later Manderly naval expansion before it happens.

## O. Trade, travel, information and market-route research

Roads and waterways should become **edges with travel time, terrain exposure, access rules and capacity**, not teleport triggers. These are *route proposals* that need 1:1-map geometry checks and physical crossings; road endpoint evidence differs in reliability across minor roads.


| # | From | To | Proposed transport mode | Trade/social requirement | Geometry/risk |
|---|---|---|---|---|---|

| 1 | Castle Black | Winterfell | kingsroad | Watch supply, rangers and royal travel | snow/road decay |

| 2 | Winterfell | Moat Cailin | kingsroad | Royal couriers, grain stores, armies | Neck chokepoint |

| 3 | Moat Cailin | The Twins | southward kingsroad/Green Fork | march and long south trade | single canonical Twins in Riverlands |

| 4 | Winterfell | Castle Cerwyn | local north road | feudal visits, messenger, agricultural trade | half-day riding relative source |

| 5 | Winterfell | Torrhen’s Square | westward rural roads | cattle, local garrison and horse wagons | distance and path verification |

| 6 | Winterfell | Barrowton | southern/western roads | grain, pastoral, winter migration | multiple route options |

| 7 | Winterfell | White Harbor | central/eastern roads/White Knife crossings | major food/letters/maritime imports | rivers and marsh crossings |

| 8 | White Harbor | White Knife basin | riverboats where draft supports | fish, grain, wood and merchants | navigable geometry unknown |

| 9 | White Harbor | Dreadfort | eastward rural roads | Manderly–Bolton exchange / emissaries | rivers and seasonal floods |

| 10 | Dreadfort | Last Hearth | northward roads | livestock, garrison, politics | cold, long distances |

| 11 | Dreadfort | Karhold | eastward tracks | marriage/court and goods | far eastern North |

| 12 | Deepwood Motte | Winterfell | Wolfswood roads | timber, hides, hunting, nobility | forest travel cost |

| 13 | Deepwood Motte | Sea Dragon Point | remote trails/coastal options | sparse logging/fish movement | few developed roads |

| 14 | Barrowton | Goldgrass | very short road | Dustin/Stout trade | distinct holding nearby |

| 15 | Barrowton | The Rills | local estate paths | horses/wool/grain | rural smallholdings |

| 16 | Barrowton | Saltspear coast | rivers/estuaries | fishing and commerce | actual river mouth need tracing |

| 17 | Moat Cailin | Greywater Watch | crannog routes by reeds/water | Reed political/military support | moving location and secret paths |

| 18 | White Harbor | Sisterton | seaborne Bite | fish/crab/wood/wool/foreign boats | ships/crew, storm/fog |

| 19 | White Harbor | Vale coastal ports | seaborne Bite | grain/markets/visits | real shipping risk |

| 20 | White Harbor | Skagos | Shivering Sea/coastal legs | obsidian/hides trade when permitted | dangerous crossing and isolation |

| 21 | Bay of Ice coast | Bear Island | boats | island supplies and coastal warning | winter storms |

| 22 | Castle Black | Shadow Tower | Watch Wall track | orders/food/medical support | 19 ruins alongside, only 3 staffed |

| 23 | Castle Black | Eastwatch | Watch Wall track | raven/courier/food | no ghost garrisons on way |

| 24 | Shadow Tower | Bridge of Skulls | west Wall frontier route | ranger access to Gorge | choke/ambush |

| 25 | Castle Black | Craster’s Keep | ranger road north | patrol and provisioning contacts | forest travel, personal relationships |

| 26 | Castle Black | Whitetree | ranger trails | patrol/archeology | abandoned village not an inhabited Watch post |

| 27 | Craster’s Keep | Fist of First Men | haunted forest trails | ranger expedition route | steep old hill |

| 28 | Frostfangs | Thenn valley | mountain passes | clan trade/war/ore | snowbound, uncertain trail |

| 29 | Haunted Forest | Hardhome | coastal/broken forest paths | Free Folk travel if event | ruins with no functioning market |

| 30 | Sisterton | Littlesister | island craft | island family visits/market | crossing requires vessel |

| 31 | Sisterton | Longsister | island craft | fisheries/households | same crew not new passengers |

| 32 | Eastern Vale Fingers | The Twins/Riverlands | southern road boundary | cross-map people and politics | different territories |



### O.1 Information diffusion and economic links

A shipment from White Harbor to Winterfell consists of **real cargo inventoried at a real source** that must be loaded and transported by actual traders/boatmen/pack beasts; another town only gains it after arrival, spoilage/raids/fees and interdependent schedules. Household purchases consume real money or exchanged goods. Letters/rumors travel via raven/courier and affect `NpcKnowledgeManager` and player map discovery only for actual recipients. A global feast or deaths in a castle does not magically update thousands of distant peasants' memory in the same tick. War interrupts tolls and trade; long winters alter roads and stored grain. All travel/trade routes must be retested against calibrated river and road geometry.

## P. Social institutions, laws, cultural influence and individual variation

| Institution / social ecology | Actual prior supported by lore | Distinctions and exception handling | Implementation target |
|---|---|---|---|
| Stark feudal North | Predominantly Northern descendants of First Men, old gods worship | Manderly and White Harbor stronger Seven faith/knights; visitors from many realms | `northman`, `old_gods` probabilities via `regional_influences.json`; not absolute |
| Winterfell sept and ancient godswood | Both religious facilities coexist inside castle | Septon Chayle and Septa Mordane individually Seven followers, Ned prays in godswood | separate religious institution and office plus person-level faith |
| Manderly city | Historically Reach family, Faith of Seven and south trade | White Harbor multi-faith port, not uniform region conversion to Faith of Seven | local faith/culture override, actual sept workplaces |
| Northern mountain clans | Strong old gods and clan kinship, smallholdings | Wull/Norrey/Liddle/Burley/Harclay/Knott/Flint distinct houses; exceptional named persons | clan society/household membership + small land tenures |
| Crannogmen | People of Neck, swamp knowledge, old gods/kinship | Navigation/floating seat/terrain; no generic always-hostile residents | `crannogman` prior, spatial mobility, local laws/rite |
| Night's Watch | Sworn order, commands, vows, independent land grants | Order members born into different families/religions; Jon not yet sworn | organization membership/vow-based legal status and location |
| Free Folk | Freedom and clan-based kinship, old gods common | Some Thenn laws central discipline, island/coastal groups differ; named leaders | `wildling`/`thenn` priors; non-feudal authority |
| Skagosi | Isolated northern island cultures and houses | Rumors in other people's beliefs not automatic universal behaviors | `skagosi` specific personal probabilities, source confidence |
| Sistermen | Isles under Vale/Arryn/Sunderland, sea-centered | Distinct ship/market practices, Seven faith present, reef navigation/craft | `sisterman`, island faith and actual government mapping |
| Vale Fingers at SW edge | Valeman and small feudal family culture | Geographically appears in NW, legally Vale | `valeman` and political control from Vale, crossmap dedup |

Reference IDs confirmed in `reference/cultures.json`: `northman`, `first_men`, `crannogman`, `wildling`, `thenn`, `skagosi`, `sisterman`, `valeman` (among others). Confirmed religious IDs include `old_gods`, `faith_of_the_seven`, `unknown_religion`. Treat `regional_influences.json` numerical weights as **generator prior weights** not hard census shares. It already defines North vs Beyond-Wall cultural features and New Gift refinement; expand to island, mountain, White Harbor, individual societies as data after global calibration.

## Q. Architecture / world generation and building footprints (1 block = 1 m)

### Q.1 Hand-authored landmark priority

Priority unique builds: (1) Winterfell stone curtain walls/Great Keep/crypts/godswood/glass gardens/winter town/royal feast; (2) White Harbor street wards, harbor, New Castle, older Wolf’s Den and sept; (3) Greywater Watch relocatable crannog; (4) Moat Cailin ruin three towers/Neck causeway; (5) the Wall contiguous barrier with exactly nineteen historical castle footprints and actual garrison flags; (6) Lord Umber/Karstark/Bolton/Hornwood/Cerwyn/Glover/Tallhart/Mormont seats; (7) Barrowton/Great Barrow/Barrow Hall/Goldgrass; (8) Sisterton with Breakwater/Night Lamp; (9) Hardhome cliffs/screaming caves, Fist ringfort/Whitetree and Craster's sod-and-wattle homestead; (10) Skagos three house seats, island wilderness/volcanic stone where compatible.

### Q.2 Standard unnamed settlements should emerge from the economy, not random icon placement

For an authored rural holding, village/hamlet candidates must sample `LandmassZoneResolver` land, watershed/groundwater, flood risk, viable arable patches, slope cut/fill, distance to roads/market, local culture, agricultural carrying capacity, labor and food/winter access. Names may be locally generated but must be tagged `[M]` and never listed as independently canonical castle. Hamlet household sizes, crop fields, hay barns, cattle pens, woodland fuel lots and shrine buildings physically occupy actual surfaces. Northern estates have a narrower viable cropping season than the Reach and require food storage rather than endless farmland.

### Q.3 Geometry / world-engine checks

- No Vanilla random default mountains should overwrite pre-authored major elevation. The original coast/river/biome raster is authoritative until corrected, while generated lowlands may add microrelief only where the user permits.
- `NW` 2073×1966 raster has an affine map transform in `MapZoneRegistry`. Do not infer Minecraft X/Z by linear pixel box endpoints or use arbitrary estimated latitude; keep `rawPixel`, `masterMapPixel`, `worldCoord`, `terrainSnap`, `footprint` distinct.
- Island masks (Skagos, Skane, Bear Island, Sisters, Pebble/Paps) may overlap political/biome layers; verify water and shoreline before creating roads or villages. Sea lanes are shipping edges, not bridge blocks.
- The Wall is a special built feature across mountains and forests. Its starting state retains structural continuity and nineteen historic castle sites, but exactly three staffed posts; no invisible multithousand-man staffing per ruined castle.
- White Harbor docks and Winterfell glass gardens require unique physical structures; using a generic medieval village generator would discard their economic and historical uniqueness.
- A town attached to a castle must be constructed as a **child/neighbor** when physically distinct, not a merged generic holding polygon or duplicated global population.
- Dynamic Greywater Watch needs a special geography anchor system or uncertainty envelope; preserve historical “moving” fact and do not claim a fixed persistent pixel even if one overlay displays a dot.
- All roads, gates, causeways, rivers and cliffs must align with the sampleable map and realistic 1m scale. Test block spawn viability and water/flood volumes; do not trust a red X to be on flat land or above water automatically.

## R. Step 2.2 integration catalog, concrete repo entry points and contracts

All of these files are **current ownership starting points**, not requests to implement new competing managers. Source research Markdown stores evidence + candidates; the next phase writes validated JSON/Java migration and tests only after marker and ID reconciliation.


| # | Domain join | Existing code/file owner | Existing state | Missing research-to-runtime adaptation | Minimum validation |
|---|---|---|---|---|---|

| 1 | Geo feature identity | world/geography/regions/NorthFeatureRegistry.java | 45 features | 18 additional Watch castles, historic ruins, roads as polylines and famous separate sub-buildings require IDs | Collision audit; no duplicate IDs after NW+SW merge |

| 2 | Map calibration | world/geography/zones/MapZoneRegistry.java | NW affine 2073x1966 | raw marks/pixels, snapped safe Minecraft coordinates not yet verified | Preserve original pixel and world target separately, water mask validation |

| 3 | World reference | world/reference/WorldReferenceCatalog; reference/locations.json | core location entries | sites and aliases mostly missing; case/synonyms/parents | No unresolved reference foreign keys at migration |

| 4 | Holding references | reference/locations_holdings.json | many holding place IDs | must reconcile nested castle vs town/city and realm parent | city resident total not sum of household and region aggregates |

| 5 | Holdings legal | simulation/social/holding/LandedHoldingManager; landed_holdings.json | 259 global; 28 North, 4 Sisters | named living holders missing for most; intermediate sworn house hierarchy | one legal holding ID, current/de jure distinct |

| 6 | Dynasty registry | simulation/social/dynasty/DynastyManager; dynasties/westeros_north.json | 77 North dynasties | nonholding clan seats, extinct/game-derived status, actual living heads | do not create permanent house lords for U/ADAPT records |

| 7 | Vale hierarchy | dynasty_hierarchy.json, westeros_vale.json | Arryn/Sunderland/Borrell/Torrent/Longthorpe | Sister islands jurisdiction from Vale regardless of NW source map | crossmap dedup only one port and census |

| 8 | Organizations | simulation/social/OrganizationManager; organizations.json | household and basic noble orgs | Watch command, Mance coalition, regional clans, maester/faith | nonfeudal order/council not generic dynasty |

| 9 | Titles offices | simulation/social/title/TitleManager; succession/ClaimManager | Stark-related title definitions | lateral lordly offices/Watch ranks/wardens/house succession | dated office start and death transitions |

| 10 | Current named NPC | simulation/npc/NpcRegistry; NpcIdentity; character JSON | core household and few Watch/royal | dozens known named holders/stewards and family members still unmapped | stable persistent one person and correct 298 state |

| 11 | Households and demography | docs/architecture/20_3_POPULATION_AND_HOUSEHOLDS.md | design/cohort authority incomplete | north-wide seeded households, births, kinship and winter migration | cohort+materialized disjoint, home/current position separate |

| 12 | Culture/language | reference/cultures.json, languages.json | northman, wildling, crannogman, skagosi, sisterman etc | White Harbor, highland, Thenn, Watch individual exceptions | probabilities sum sensibly but need not equal census |

| 13 | Religion | reference/religions.json, religious_institutions.json | old_gods, faith_of_the_seven | Castle sept / White Harbor southern faith, mountain clans, Free Folk | person beliefs and shrine staff, no uniform provincial hardcode |

| 14 | Occupation | reference/occupations.json; roles.json | 136 general occupations and extensions | ranger/builder/steward, winter fuel and sail crew associations | no phantom jobs; work assignment to existing NpcId |

| 15 | Work implementation | simulation/npc/action/NpcActionProcessor.processWork | currently intentionally no economic effect | real labor scheduling linked to farms/shipping/manufacturing | never add second tick of production |

| 16 | Agriculture | simulation/economy/HoldingAgricultureCatalog; agricultural_capacity.json | North regional MOD_ESTIMATE grain/field template | enormous ecology diversity beyond current single North type | farm area not source census; same person labor |

| 17 | Food and inventory | EconomyRuntime, InventoryManager, SettlementCohortFoodIntegration | food/inventory baseline systems | actual wintertown migrations, castle stores, Watch rations, maritime fish | production/transport/consumption conserve stocks |

| 18 | Markets/prices | docs/architecture/20_3_ECONOMY_DOMAIN.md | future market/trade model | White Harbor/Gift/island routes and legal customs | no goods teleport, merchant actual person/vehicle |

| 19 | Military roster | master roadmap phase 5 | full autonomous war domain pending | Noble levies distinct from staff/Watch and camp allies | soldiers subset of census, ration/equip from stocks |

| 20 | Climate/biomes | world/biome, world/generation/KnownWorldChunkGenerator | raster biome and terrain systems | northern long winters, ocean ice, mountain ecology and marsh | no vanilla relief corruption or water villages |

| 21 | Settlement physical | simulation/world/settlement/SettlementManager; scenarios/agot_298_ac/settlements.json | only Winterfell active scenario settlement | White Harbor, Barrowton, Wall posts, ruined structures need authoring | build after coordinate terrain approval only |

| 22 | Ravens/travel | NPC goals/action, knowledge/memory and geographic routes | partial travel/knowledge systems | cross-realm and NW–SW roads, ships/ravens, info delay | current location changes only by travel, not duplicates |

| 23 | Law/crime | npc legal state, society and future civic engine | some legal/profile fields | Watch vows, clan customs, crannog rights, Sisters smuggling cases | enforcement based on actual witnesses and governments |

| 24 | Historical event | chronicle/event + scenario_start.json | specific royal arrival start anchor | prevent deaths, invasions and House Thenn marriage early | future political outcomes only if sim events cause |

| 25 | Persistence | economy snapshots/other saved managers | mixed domain checkpoints incomplete | world-scale multi-domain scene consistency | save/reload no double census or old owner rewinds |

| 26 | Minecraft NPC projection | NpcSpatialIndex and simulation levels | physical/abstract levels | same named NPC seen at farm, camp or market | no extra live entity on materialization |



## S. Automated and manual Step 2.2 acceptance tests before worldgen

Use these as actual runnable assertions/scenario exercises when authoring the data. They are prioritized because NW especially contains hierarchical/legal ambiguity, Watch/nonfeudal society, seasonal migration and multiple nested settlement sites.


| # | Test | Pass condition |
|---|---|---|

| 1 | 11 regional map coverage | all map zones present, exact 25 NW color zones, original marker table archived |

| 2 | No invented X IDs | research case records have `sourceMarkerId=null` where not visually verified |

| 3 | No doubled registry locations | `NW`/`SW` the_twins/the_fingers/the_neck converge to one stable ID each |

| 4 | Registered feature type ≠ active town | forest/wall/island/pass never auto-created as a census-bearing city |

| 5 | Thirty-two NW holdings joined | 28 North-domain + 4 Sister-domain rows all have valid worldLocation/correct parent |

| 6 | North vs Vale political border | Three Sisters and Fingers remain Vale; Twins Riverlands |

| 7 | Seventy-seven dynasties resolved | current scene, historical, adaptation, nonlanded/unknown categories explicit; no made-up seat |

| 8 | Correct Medger starting holder | Cley heir; subsequent transition only after death event |

| 9 | Correct Hornwood state | Halys/Donella/Daryn alive, no later Bolton seizure at tick zero |

| 10 | Correct Stark royal arrival | Robert, Cersei, Jaime, Tyrion physically visiting Winterfell, not permanent residents |

| 11 | Jon Snow vows chronology | Jon is Stark household, not a Watch brother before departure/vows |

| 12 | Watch unique population | active station posted/away patrols sum under 1000 without overlap |

| 13 | Nineteen Watch castle statuses | exactly three active, sixteen abandoned |

| 14 | Watch institutional land | Gift land rights not double-taxed as Stark smallholdings |

| 15 | No ghost castle markets | abandoned Watch forts output no daily food/provisions/taxes |

| 16 | No Moat Cailin town census | ruined partial towers, occupancy only if actual later garrison |

| 17 | No Greywater fixed pixel assertion | house seat keeps stable ID, location uncertainty/movement documented |

| 18 | Winter town migration conservation | moving 1000 villagers to winter town removes 1000 domiciled residents elsewhere or moves temporary presence; no +1000 global |

| 19 | Seasonal housing capacity | full wintertown capacity estimate not interpreted as 298 summer starting census |

| 20 | Nested White Harbor/New Castle | city + castle share one people accounting, castle staff subset |

| 21 | Nested Barrowton/Goldgrass | Stout keep close to Barrowtown; no duplicated town households |

| 22 | Sisterton / Breakwater / Night Lamp | one island harbor, three sites and different property/office types |

| 23 | Forest harvest constrained | wood inventory decreases by actual felled biomass and work |

| 24 | Crop suitability by biome | no high-yield grain on frostfang cliffs, in saltwater or intact bog |

| 25 | Geothermal site uniqueness | Glass Gardens local Winterfell only, not every village greenhouse |

| 26 | Manderly religion exception | mixed Seven/old gods relative to North old gods prior |

| 27 | Clan leader/household identities | Wull/Liddle/etc clans not one false feudal Kingdom of Northern Mountains |

| 28 | Thenn/Skagos name separation | Skagos House Magnar distinct from Thenn Styr office/new house later |

| 29 | Free Folk true society | Mance coalition not a dynasty-owned fixed city with NPC clones |

| 30 | Rumors vs facts | Skagos cannibal/unicorn stories and Others reports stored with provenance and beliefs |

| 31 | Labor only from living humans | dead Domeric Bolton never farms or commands troops; real living shifts sum to capacity |

| 32 | Military removes farm time | each mobilized resident reduces local farm or service labor, no new phantom soldiers |

| 33 | Ranger road trip accounting | departing a Watch post keeps same NpcIds and ration stock |

| 34 | White Harbor sea route | voyages across the Bite require real ship crews, weather and duration |

| 35 | Crossmap county ownership | the_twins registered NW feature not mis-parented under Stark |

| 36 | River geometry and flood | White Knife/Weeping Water/Milkwater routes match actual terrain/real water |

| 37 | Save restore person conservation | save/reload after migration/levy never doubles cohorts/NPCs |

| 38 | Future plot not initialized | no Bolton Warden, Stannis king, Robb King, ruined Winterfell, Jon LC |

| 39 | Long duration socioeconomic test | one North holding, then multi-holding decade: food, inheritance, work, needs and encounters coherent |

| 40 | Player encounter identity | NPC when rendered physically is same person as offscreen property/relationships/memories |



## T. Per-NW map-source observation / correction queue (NOT silently treated as matched)

The actual handwritten red-X PNG observations cannot be individually inspected in this connection; earlier report's assumption that the 45 registered features were “the NW markers” was therefore **unjustified**. This expanded report deliberately inventories independently named sites and repo IDs while preserving unverified source observations for the user's planned separate map-correction document.

**Specific high-priority NW candidates for later marker/label check:** Whitetree (northwest of Castle Black in Haunted Forest; abandoned), Craster's Keep (Haunted Forest, north Wall), Hardhome (far northeast Storrold's Point coast), Fist of the First Men (near Milkwater), Forktop/Skirling Pass/Giant's Stair (Frostfangs), all **18** non-Castle-Black Wall castles (Wall west-to-east, most abandoned), Mole's Town (just south Castle Black), Queenscrown (Gift south Wall), all major named mountain clans' seats (Northern mountains west kingsroad), Barrowton vs Barrow Hall vs Goldgrass (Barrowlands southwest), Ramsgate (East coast near White Harbor), Deepwood Motte (west Wolfswood), Greywater Watch (Neck west of Moat Cailin but no fixed precise location), Skagos three halls (far eastern island), Sisterton/Breakwater/Night Lamp (southern/eastern Bite), Twins (extreme south/edge NW, belongs Riverlands). Actual **unreadable labels** cannot be enumerated honestly without seeing PNG and/or original Python output; this is **candidate review**, not a reported unreadable-X count.

Cross-map review: The Neck, Cape Kraken, Fingers, Sisters near SW border and Twins, plus named roads and the White Knife reaching broader reference maps. Build one global site key with multiple source-observation links. Keep the user's false-positive and missed-X pass separate as requested.

## U. Completion and explicitly unsolved research

- [x] **Full** 45-entry registered feature inventory with physical identity/scene notes.
- [x] **Exact** 25 NW raster subregions and detailed ecology/economic/settlement rules.
- [x] **All 32 relevant landed-holding records** (28 North, 4 Three Sisters) individually profiled, with political/physical/social/economic/military joins and no-canon-fabrication evidence flags.
- [x] **All 77 Northern dynasty records** inventoried, including missing seats/clans, historical, adaptation and family/office distinctions.
- [x] **All nineteen Watch castles** individually identified/statused, with three-active/16-abandoned human conservation requirements.
- [x] **Supplemental named places** including full Winterfell/White Harbor/Barrowton/Sisterton subsite structure, Free Folk living sites/ruins, rivers, passes and clan territories.
- [x] **Named 298 roster** of dynasty/household/Watch/Free Folk/Three Sisters figures, with correct family and later plot exclusions.
- [x] **Occupation and demographic** templates, farming/environment/resources/ports/trade, architecture, governance, military and personal identity contracts, tied to verified code owners and future tests.
- [x] **Source index** with primary works, official cartography and indexed AWOIAF reference pages; explicit noncanonical mod assumptions and temporal state.
- [ ] **Exact X / handwriting** read for each marking in `nw.png`: GitHub binary image and Python detector results not accessible via text connector; must be joined through separate global map reconciliation document.
- [ ] Individual full manuscript/source verification for every one of 77 obscure houses and detailed exact current NPC head on 298 date: many are genuinely lore-unknown, and adaptation families have different provenance.
- [ ] Final world census, individual food/worker totals, field hectares, real military muster, all precise route polygons and 1:1 Minecraft build coordinates: not possible from lore and repo reference counts alone; Step 2.2/special placement checks.
- [ ] Comprehensive synthetic test and code changes: intentionally not authorized during research.

**Verdict:** NW now has a substantially expanded all-domain research base instead of treating the 45 map registry names as “everything in Northern Westeros.” The information is sufficient for **planning Step 2.2 schemas and population/zone authoring**, not an assertion that its uninspected PNG marks or every obscure family lord has been solved. Source-image reconciliation and a global population balance remain real preconditions for authoring the final world.


## V. Individual dynasty research-to-runtime handoff notes — all catalog families

The table in §E is a master index; this section retains one independent handoff contract for each family to avoid erasing small houses behind broad northern culture averages. For a family with no source-backed active holding, the entry describes exactly what must remain **unknown**, not fictional evidence. This list supports the user's former `19.0E.1` non-holding audit and Step 2.2 genealogy/claims without prematurely changing history.


### V.01 — `house_stark`

- **Geographical/temporal scope:** winterfell_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `winterfell_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.02 — `house_ashwood`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.03 — `house_blackmyre`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.04 — `house_boggs_north`

- **Geographical/temporal scope:** Neck; category `CRANNOG_HOUSE`. [S26]
- **Evidence-specific identity:** Possible crannogman family, exact estate and lord unverified. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `CRANNOG_HOUSE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.05 — `house_bole`

- **Geographical/temporal scope:** Wolfswood; category `BANNER_WITHOUT_CURRENT_HOLDING`. [S01]
- **Evidence-specific identity:** Northern minor house, no current holding record; avoid generic Bole castle. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `BANNER_WITHOUT_CURRENT_HOLDING`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.06 — `house_bolton`

- **Geographical/temporal scope:** dreadfort_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `dreadfort_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.07 — `house_bolton_winterfell`

- **Geographical/temporal scope:** Winterfell; category `FUTURE_BRANCH_REVIEW`. [R/H]
- **Evidence-specific identity:** Later Bolton of Winterfell political identity NOT part of scenario start. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `FUTURE_BRANCH_REVIEW`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.08 — `house_branch`

- **Geographical/temporal scope:** Wolfswood; category `BANNER_WITHOUT_CURRENT_HOLDING`. [S01]
- **Evidence-specific identity:** Minor northern house, exact seat uncertain. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `BANNER_WITHOUT_CURRENT_HOLDING`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.09 — `house_burley`

- **Geographical/temporal scope:** northern mountains; category `KNOWN_CLAN`. [S12]
- **Evidence-specific identity:** Clan representative, pasture/holdfast. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.10 — `house_cassel`

- **Geographical/temporal scope:** Winterfell castle; category `HOUSEHOLD_SERVICE`. [S02]
- **Evidence-specific identity:** Ser Rodrik/Jory Cassel in Stark service; holding not automatically bestowed. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HOUSEHOLD_SERVICE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.11 — `house_cerwyn`

- **Geographical/temporal scope:** castle_cerwyn_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `castle_cerwyn_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.12 — `house_condon`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.13 — `house_cray`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.14 — `house_crowl`

- **Geographical/temporal scope:** deepdown_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `deepdown_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.15 — `house_dustin`

- **Geographical/temporal scope:** barrow_hall_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `barrow_hall_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.16 — `house_fenn`

- **Geographical/temporal scope:** Neck marsh crannogs; category `CRANNOG_HOUSE`. [S26]
- **Evidence-specific identity:** Reed-allied distinct crannogmen, not all residents of Greywater Watch. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `CRANNOG_HOUSE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.17 — `house_fisher_stony_shore`

- **Geographical/temporal scope:** Stony Shore; category `HISTORICAL_REVIEW`. [S01]
- **Evidence-specific identity:** Historical Fisher kings / possible lineage; cannot auto-confirm active 298 family. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HISTORICAL_REVIEW`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.18 — `house_flint`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.19 — `house_flint_breakstone_hill`

- **Geographical/temporal scope:** breakstone_hill_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `breakstone_hill_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.20 — `house_flint_flints_finger`

- **Geographical/temporal scope:** flints_finger_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `flints_finger_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.21 — `house_flint_mountains`

- **Geographical/temporal scope:** northern_mountains_flint_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `northern_mountains_flint_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.22 — `house_flint_widows_watch`

- **Geographical/temporal scope:** widows_watch_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `widows_watch_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.23 — `house_forrester`

- **Geographical/temporal scope:** ironrath_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `ironrath_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.24 — `house_frost`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.25 — `house_glover`

- **Geographical/temporal scope:** deepwood_motte_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `deepwood_motte_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.26 — `house_greengood`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.27 — `house_greenwood`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.28 — `house_greystark`

- **Geographical/temporal scope:** former Wolf’s Den/White Harbor region; category `HISTORICAL`. [S01,S29]
- **Evidence-specific identity:** Extinct rebellious Stark cadet house historically; no living 298 AC ruler. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HISTORICAL`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.29 — `house_harclay`

- **Geographical/temporal scope:** foothills near Wolfswood / Winterfell; category `KNOWN_CLAN`. [S11,S12]
- **Evidence-specific identity:** Local clan smallholdings, no single mapped public castle. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.30 — `house_holt_north`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.31 — `house_hornwood`

- **Geographical/temporal scope:** hornwood_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `hornwood_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.32 — `house_ironsmith`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.33 — `house_karstark`

- **Geographical/temporal scope:** karhold_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `karhold_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.34 — `house_knott`

- **Geographical/temporal scope:** northern mountains high places; category `KNOWN_CLAN`. [S11,S12]
- **Evidence-specific identity:** Mountain clan, named individual ruler/seat must be sourced. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.35 — `house_lake_north`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.36 — `house_liddle`

- **Geographical/temporal scope:** high northern mountains north of wolfswood; category `KNOWN_CLAN`. [S11,S14]
- **Evidence-specific identity:** The Liddle, Torren Liddle; Morgan Liddle heir; caution episode chronology. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.37 — `house_lightfoot`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.38 — `house_locke`

- **Geographical/temporal scope:** oldcastle_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `oldcastle_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.39 — `house_long_north`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.40 — `house_magnar`

- **Geographical/temporal scope:** kingshouse_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `kingshouse_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.41 — `house_manderly`

- **Geographical/temporal scope:** new_castle_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `new_castle_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.42 — `house_marsh`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.43 — `house_mollen`

- **Geographical/temporal scope:** Winterfell military household; category `HOUSEHOLD_SERVICE`. [S02]
- **Evidence-specific identity:** Stark guard/officer line, family ancestry and landed status check. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HOUSEHOLD_SERVICE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.44 — `house_mormont`

- **Geographical/temporal scope:** hall_of_mormonts_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `hall_of_mormonts_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.45 — `house_moss`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.46 — `house_norrey`

- **Geographical/temporal scope:** upper northern mountains nearest Gift; category `KNOWN_CLAN`. [S11,S12]
- **Evidence-specific identity:** Clan friends of Night’s Watch; unknown exact castle. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.47 — `house_overton`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.48 — `house_peat`

- **Geographical/temporal scope:** Neck swamp; category `CRANNOG_HOUSE`. [S26]
- **Evidence-specific identity:** Distinct crannog clan/house, local house seat unknown. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `CRANNOG_HOUSE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.49 — `house_poole`

- **Geographical/temporal scope:** Winterfell castle; category `HOUSEHOLD_SERVICE`. [S02]
- **Evidence-specific identity:** Steward Vayon Poole and daughter Jeyne; do not invent current Poole castle. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HOUSEHOLD_SERVICE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.50 — `house_quagg`

- **Geographical/temporal scope:** Neck marsh vicinity (candidate); category `CRANNOG_HOUSE`. [R/U]
- **Evidence-specific identity:** Potential regional minor family, verify history. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `CRANNOG_HOUSE`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.51 — `house_redbeard`

- **Geographical/temporal scope:** Beyond Wall Free Folk legend/lineage; category `HISTORICAL_REVIEW`. [S15]
- **Evidence-specific identity:** Bael/Redbeard traditions not starting Northern feudal lordship. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HISTORICAL_REVIEW`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.52 — `house_reed`

- **Geographical/temporal scope:** greywater_watch_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `greywater_watch_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.53 — `house_ryder`

- **Geographical/temporal scope:** Northern North, confirm; category `SEAT_UNKNOWN`. [U]
- **Evidence-specific identity:** No safe exact holdfast or ruler to author. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.54 — `house_ryswell`

- **Geographical/temporal scope:** rills_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `rills_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.55 — `house_slate`

- **Geographical/temporal scope:** blackpool_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `blackpool_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.56 — `house_stane`

- **Geographical/temporal scope:** driftwood_hall_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `driftwood_hall_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.57 — `house_stout`

- **Geographical/temporal scope:** goldgrass_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `goldgrass_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.58 — `house_tallhart`

- **Geographical/temporal scope:** torrhens_square_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `torrhens_square_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.59 — `house_thenn`

- **Geographical/temporal scope:** Thenn valley beyond Wall; category `NON_FEUDAL_REVIEW`. [S15]
- **Evidence-specific identity:** Thenn society/Magnar of Thenn distinct from House Magnar of Skagos; no 298 Stark feudal title. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `NON_FEUDAL_REVIEW`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.60 — `house_towers_north`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.61 — `house_umber`

- **Geographical/temporal scope:** last_hearth_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `last_hearth_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.62 — `house_waterman`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.63 — `house_wells_north`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.64 — `house_whitehill`

- **Geographical/temporal scope:** highpoint_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `highpoint_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.65 — `house_woodfoot`

- **Geographical/temporal scope:** Bear Island historical control; category `HISTORICAL`. [S01]
- **Evidence-specific identity:** Earlier Bear Island line predating Mormonts; do not revive as 298 island lord. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `HISTORICAL`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.66 — `house_woods`

- **Geographical/temporal scope:** Wolfswood; category `BANNER_WITHOUT_CURRENT_HOLDING`. [S01]
- **Evidence-specific identity:** Northern family; exact map pin unknown. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `BANNER_WITHOUT_CURRENT_HOLDING`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.67 — `house_woolfield`

- **Geographical/temporal scope:** near White Harbor / Manderly sphere; category `BANNER_WITHOUT_CURRENT_HOLDING`. [S29]
- **Evidence-specific identity:** House Woolfield linked to Manderly authority; named seat/holder research required. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `BANNER_WITHOUT_CURRENT_HOLDING`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.68 — `house_wull`

- **Geographical/temporal scope:** Bay of Ice / west Northern mountains; category `KNOWN_CLAN`. [S12,S13]
- **Evidence-specific identity:** Lord Hugo Wull (Big Bucket) later named in books; clan chief. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `KNOWN_CLAN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.69 — `house_tuttle`

- **Geographical/temporal scope:** Telltale northern storyline; category `ADAPT`. [R/ADAPT]
- **Evidence-specific identity:** Book-era geographical seat not established. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `ADAPT`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.70 — `house_brownbarrow`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.71 — `house_dormand`

- **Geographical/temporal scope:** dormand_hall_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `dormand_hall_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.72 — `house_elliver`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.73 — `house_glenmore`

- **Geographical/temporal scope:** rillwater_crossing_domain; category `CURRENT_HOLDING_IN_SCENARIO`. [R]
- **Evidence-specific identity:** Active row in `landed_holdings.json`; confirm initial human holder and subordinate liege. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** Repo links an existing active holding `rillwater_crossing_domain`. Validate `worldLocationId`, the correct direct/indirect liege, de jure versus present dynasty control, the actual living 298 holder, and how its castle estate differs from attached villages; assign any workers, guards, tenant families and land parcels only after census calibration. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.74 — `house_grayson`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.75 — `house_greenleaf`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.76 — `house_mazin`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.


### V.77 — `house_degore`

- **Geographical/temporal scope:** Northern dynasty record, no trustworthy pinpoint; category `SEAT_UNKNOWN`. [R/U]
- **Evidence-specific identity:** Do not generate unresearched canonical castle or named holder; review living/existing status. Family culture/faith, exact hereditary property, location and living named holder are not universal northern defaults.
- **Domain-by-domain check:** No direct independently linked 298 AC North-root holding exists for this dynasty in inspected catalog. Status `SEAT_UNKNOWN`. **Do not infer the family never had land or is extinct**; investigate source-era head, land claim, title, possible parent bannerman and present existence, marking unknown where evidence is absent. Distinguish marriage/kin bloodline from hereditary title, local private property from governmental jurisdiction, and noble identity from a caste-wide social role. One named lord cannot stand in for the whole family population; non-noble kin, spouses, children, servants, tenants and allies need independent person/household records when generated.
- **Step 2.2 handoff:** connect stable DynastyId and any source-backed genealogy to the existing `DynastyManager`/`LandedHoldingManager`/`ClaimManager`, with null/explicitly estimated unsupported claims. Check household location against corrected `NW` pixels, source-matched seat, actual farm/workplace inventory, retainers, faith/social influence and armed personnel subsets. **No automatic castle insertion** merely because this dynasty has a registry row; no family “ghost characters” representing extra population.



---

## 2026-10-17 — AUTHORITATIVE USER MARKER CORRECTIONS (supersedes previous "unlinked" entries)

*These labels were provided by the map author. They are NOT inferred from a nearest-place lookup. Original detector Xs are preserved and final 3D build placements remain pending.*

| ID | User-supplied source-map name | Known book/reference label, if different | Baseline 298 AC | Location data status |
|---|---|---|---|---|
| NW-021 | Westwatch-by-the-Bridge | Westwatch-by-the-Bridge | Abandoned Night's Watch castle | Source X associated |
| NW-020 | Shadowtower | Shadow Tower | Inhabited; commander Denys Mallister | Source X associated |
| NW-019 | Shieldwatch | **Sentinel Stand** (possible cartographic/user-name variant, NOT silently substituted) | Abandoned | Source X associated |
| NW-018 | Greywatch | **Greyguard** (possible cartographic/user-name variant) | Abandoned | Source X associated |
| NW-012 | Stonegate | **Stonedoor** (possible cartographic/user-name variant) | Abandoned | Source X associated; original land-mask water warning to check |
| NW-015 | Hoarfrost Hill | Hoarfrost Hill | Abandoned | Source X associated |
| NW-013 | Icemark | Icemark | Abandoned | Source X associated |
| NW-016 | The Nightfort | Nightfort | Abandoned in 298 AC | Source X associated |
| NW-014 | Deep Lake | Deep Lake | Abandoned in 298 AC | Source X associated |
| NW-008 | Castle Black | Castle Black | **Active** | Old detector point (401.3997, 214.85223 master) wrongly north of Wall. New **approximate visual-only** atlas marker (409.70, 223.80), south of Wall between NW-014 Deep Lake and NW-009 Oakenshield; needs map/terrain approval. Source pixel unchanged. |

**Conflict unresolved:** user also wrote `NW-020: Strongson` after assigning NW-020 to Shadowtower. The available remaining unlinked marker is **NW-049**, in the Vale map margin near Heart's Home. The repo's `strongsong_domain` and book site **Strongsong** (House Belmore) are a compelling match, but this is explicitly **unconfirmed** until the user verifies NW-049. Never rename NW-020 to Strongsong, and never spawn two structures from this ambiguity.

**Interpretation rule:** book-valid Wall castles are Sentinel Stand, Greyguard, Stonedoor (among the historic nineteen); the user's Shieldwatch / Greywatch / Stonegate are preserved as user transcriptions, with book labels separately recorded. Before Step 3 Wall construction, inspect the original annotated Xs to choose final display aliases. Only Castle Black, Shadow Tower, Eastwatch are staffed in the 298 AC opening. Newly detected eastern Wall marks NW-MISSING-WALL-01..04 still have only hand-estimated pixel positions, not survey quality.

**Implementation source of truth:** `tools/geodata/atlas_user_confirmed_2026-10-17.json`, `marker_catalog_active_253.csv`, `src/main/resources/assets/knownworld/maps/atlas_markers.json`. These are **geographical observation coordinates**, never approved building anchors. `RegionalFeatureLocationRegistry.java` also has a provisional Castle Black location adjustment; the Winterfell anchor remains separate and unapproved.
