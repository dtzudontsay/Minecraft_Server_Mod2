# Known World — Step 2.1 Research: UL (Ulthos, Ulos and the Saffron Straits), 298 AC

**Research status:** completed first-pass *source-grounded regional research*, NOT completed marker-to-place reconciliation, named settlement census, exact build coordinate assignment, or Step 2.2 implementation.  
**Scenario:** book-continuity start in **298 AC**, around the royal visit to Winterfell.  
**Regional source:** `authoring/maps/Realms/ul.png`, `MapZoneId.UL` (1,083 × 607 pixels).  
**Companion/overlap:** `see.png` (Southeastern Essos), which contains the Shadow Lands, Saffron Straits, and Ulos subregion; both maps show the same Ulos, not two islands.  
**Research date:** 2026-10-17.  
**Repo inspected:** `dtzudontsay/Minecraft_Server_Mod2`, `master`.  
**Critical caution:** The exact red-X detector output and annotated UL image could not be inspected as a reliable per-marker ledger in this chat. The IDs `UL-R01`–`UL-R08` are **research identities only**, not original detected source marker IDs. Original `UL-NNN` observations must be joined later; do not fabricate positions or assert that every X has been inspected.

## 0. Evidence and generation conventions

- **[C] CANON / official cartography:** Published *The Lands of Ice and Fire*, *The World of Ice & Fire* or another published text actually supports the statement. A Wiki of Ice and Fire pages act as source indexes; map-derived details remain conditional on visual verification in the user's source artwork.
- **[R] REPO:** Current Java or JSON names, types, polygons or placeholders. Their presence does not prove correctness.
- **[I] INFERRED:** Conservative plausible implications derived from the landscape or historical context; never reported as attested places, indigenous customs or trade volumes.
- **[M] MOD-AUTHORED ESTIMATE:** Explicit speculative design choice, not a census, historical claim, production figure or canonical geography.
- **[U] UNKNOWN:** Nothing sufficient for a factual assignment. Store `null`/`UNKNOWN` in the eventually authored scenario data.
- **[H] HISTORICAL:** Historical exploration, ruins or quoted traditions; not proof of present-day 298 AC rule, occupancy or functioning economy.

The absence of described settlements on Ulthos **does not imply the landmass is uninhabited**. It means the sources do not establish a named settlement, a demographic count or a governmental system. Equally, the existence of a ruined city on Ulos does **not** justify attaching 298 AC active residents to it.

### System-wide invariants inherited from previous 2.1 batches

1. **Person identity:** The same person is used by the demographic model, household, NPC, labor, ship crew, military, kinship and on-screen entity. Cohorts may be an acceleration/cache mechanism, not a second population authority or an independent crowd.
2. **Authority:** Distinguish a mapped `WorldLocation`, a political `LandedHolding`, an operating government organization, a noble dynasty, a title, an individual holder and a physical `Settlement`. Do not invent an Ulthosi house to fill a blank.
3. **Conservation:** Resident counts, visitors, expedition parties, crews, workers, soldiers and captives must not be additive aliases of the same people. Birth/death/migration and trade consume or transfer authoritative records.
4. **Ruins:** A ruin may physically exist, attract explorers or be settled later by world events, without hosting an automatically generated 298 AC civic census.
5. **Provenance:** All uncertain climate, crop, military, architectural, economic and social statements must remain tagged [I]/[M]/[U], never silently upgraded to canon.
6. **Spatial state:** Keep the original source observation, calibrated master-map location, snapped world spawn point and building footprint as **separate fields**. User has deliberately postponed correction of false/missing/misaligned red X observations.
7. **Chronology:** Keep prior voyages and speculations in history or person-knowledge systems. Do not preload later discoveries, rulers, settlements or modern cartographic certainty.
8. **Perception/knowledge:** Westerosi lack of knowledge should affect the beliefs of Westerosi NPCs, not necessarily the truth/absence of things in the world.

## 1. Verified high-level findings

### 1.1 Ulthos: geography

- **[C]** A very poorly charted **large landmass** lies in the far southeast of the known world, south of Asshai and the Shadow Lands, across the **Saffron Straits**. It faces the Jade Sea to its northwest/west and unknown waters to its east. [S1][S2][S3]
- **[C]** It may be described as a continent or a very large island; even the distinction and full shape are intentionally unresolved by George R. R. Martin. In the mod, `CONTINENT` is an administrative/cartographic convenience, *not* proof of complete geographical survey. [S1][S5]
- **[C]** Only the **northern/northwestern margin** is substantially visible on published maps. The shore extends off the known map. A map boundary must **not** become an invented seacoast, cliff wall or inland nation boundary. [S1][S2]
- **[C]** Artist/cartographer Jonathan Roberts described a **purple-black jungle** in Ulthos. The maps show an **unnamed river with at least one tributary** through northwestern forested Ulthos. Neither river has an attested name, navigability or surveyed course beyond the published map. [S1][S6]
- **[U]** The extent, altitude of the interior, fauna, edible crops, climate subdivisions, soil fertility, exact monsoon regime, mineral resources, settlement density and disease repertoire are not documented sufficiently to assign canonical values.

### 1.2 Ulos: a separate island with one unnamed ruin

- **[C]** **Ulos** is a **mountainous island** in the Saffron Straits between the Shadow Lands and Ulthos and among the easternmost recorded islands. [S4]
- **[C]** An **unnamed ruined city** lies in its **northern part**. Its founders, construction style, dating, downfall, ownership, faith, political organization and former population are *unknown*. [S4]
- **[U]** Current stable inhabited town or garrison **not established**. Do not translate `unnamed_ruined_city` into a factual proper name, or assume it was built by Valyria, Asshai, Yi Ti or the makers of Yeen.
- **[M]** Expedition landings, temporary marooned sailors, isolated hermits or later colonization are *possible gameplay scenarios*, not initial 298 AC facts. Every added participant must be a real traveling person or an explicitly newly founded population.

### 1.3 Saffron Straits and Essos border

- **[C]** The **Saffron Straits** are the eastward exit of the Jade Sea, separating **Essos to the north** from **Ulthos to the south**; they widen toward the poorly known east. **Asshai** lies near the **northern side of the western entrance**. [S3]
- **[C]** The Shadow Lands and Asshai are **Essosi** places, **not** Ulthosi towns to duplicate on this map. The Manticore Isles likewise belong to the separate southeastern-Essos research and spatial catalog. [S3][S7]
- **[U]** The straits' name is sometimes explained by saffron trade in a *semi-canonical* account; that is not evidence that saffron is native to Ulthos, that Ulos has a spice plantation, or that the 298 AC route carries specific vessel counts. [S3]
- **[I]** Their coastal position supports *possible* maritime exploration and eastward passage. There is insufficient evidence for a regular, safely traveled Ulthos coastal trade network.

## 2. Place and feature research ledger

These are **stable research rows**, not the user's original detector numbers. Status refers to 298 AC factual establishment, not an authorization to create settlements.

| Research row | Place / feature | Classification and 298 AC status | Residents / defenders | Candidate stable ID | Existing repository link |
|---|---|---|---|---|---|
| UL-R01 | Ulthos, visible portion | Landmass / mostly unknown territory [C/U] | **Unknown / unknown**, NOT zero | `ulthos` | `locations.json` + `UlthosFeatureRegistry` + landmass polygon |
| UL-R02 | Northwest Ulthos forest | Broad visible forest/jungle sector [C/I] | **Unknown / unknown** | `northwest_ulthos` for geography only | Existing plausible region and territory polygon |
| UL-R03 | Northwestern unnamed river | River and >=1 tributary, named identity unknown [C] | Not a settlement | `ulthos_northwest_river` **proposed** | No dedicated named river reference verified |
| UL-R04 | Ulthos northern coastline | Mapped littoral / vague landings [C/U] | No separately known settlements | `ulthos_northern_littoral` **proposed** | Intersects visible UL landmass zone |
| UL-R05 | Ulos | Mountainous island [C] | Inhabitation **unknown** | `ulos` | Existing geographic feature and world location, but mismatched type |
| UL-R06 | **Unnamed city ruins on northern Ulos** | **Ruined city**, no confirmed current civic regime [C] | **No established civic census; not 0 humans guaranteed** | `ulos_northern_unnamed_ruins` **proposed** | Missing distinct ruins record |
| UL-R07 | Saffron Straits | Maritime geographic connector, not a city [C] | Non-residential | `saffron_straits` | Already in `SouthEastEssosFeatureRegistry`; do not register again |
| UL-R08 | Shadow Lands / Asshai at northern boundary | **Cross-map reference only**, located in Essos [C] | Use **SEE** dossier, no UL duplicate | `shadow_lands`, `asshai` | Existing Essos refs and SEE research |

`UL-R03`, `UL-R04` and `UL-R06` are **proposed IDs only**. Do not create them until Step 2.2 ID review. `UL-R02` is an existing **mod-authored** geographic region, not a canonical political province.

### Population / military baseline (critical)

There is **no canon 298 AC population census** for Ulthos, Ulos or the Ulos ruins. The correct data-level value is `UNKNOWN`/`null`, not a fabricated 10,000-person city, not an arbitrary percentage of Yi Ti, and not `zero`. A future scenario creator might **explicitly authorize** `[M]` population envelopes for unknown Ulthosi communities. Until then:

- `ulthos`: `populationTarget = null`, `militaryTarget = null`, `government = null`.
- `northwest_ulthos`: same.
- `ulos`: island-wide permanent population unknown; no named current polity or sovereign.
- `ulos_northern_unnamed_ruins`: `siteStatus = RUIN`; `civicPopulationEstablished = false`; no initial payroll, tax collection, standing garrison, farming workforce or default functioning palace.
- `saffron_straits`: no permanent population attribute; vessel travelers retain other residence identities.

This explicit abstention is intentional. It avoids fake specificity when sources are almost entirely silent.

## 3. Place dossiers and generation policy

### UL-R01–R02 — Ulthos / Northwest Ulthos

**Government, political control, dynasties and houses [U]:** Nothing supports a unified kingdom, claimed throne, recognized prince, noble House Ulthos, vassal hierarchy, appointed governor or imperial administration in 298 AC. `northwest_ulthos` is solely a geography/simulation subdivision. An external power's hypothetical exploration cannot become ownership without an explicitly dated change event.

**Population, cultures and religions [U]:** No attested Ulthosi ethnic identity, language distribution, social organization, worship, names, surname patterns, aging patterns, legal systems or specific rights/captive regimes can be supplied. Do not copy cultures/religions from Asshai, the Jogos Nhai or Sothoryos. Do not populate the forest with a universal "barbarian" faction or standard Minecraft hostile villages by default.

**Physical environment [C/I]:** Dark purple-black jungle and an unnamed branched river are attested as *cartographic clues*. Plausible terrain layers: dense mixed-height canopy, humid coastal forest, an alluvial river corridor, flood-prone banks, hill transitions, restricted visibility and natural clearings. Exact annual temperature, rain, fertility and disease penalties are **unknown**; any numeric biome weights or seasons are author choices requiring explicit tagging and terrain testing.

**Agriculture, resources and industry [U/I]:** Canon does not identify regional staple grains, rice, fruit, spice, livestock, domestication, fisheries, timber industries, mines, metals or active markets. Habitat suitability might allow foraging, fishing, forestry or riverine gardens *if authored*, but no output should be produced without actual inhabitants, workplaces, land, inventory, labor and ecological constraints. Do **not** make `ulthos` inherit the grain yields of temperate Westeros.

**Settlement form [U/I]:** No canon house plans, urban street patterns, settlement names, roofing methods, bridges, walls or temples. Source-derived environments may be generated; speculative communities/architecture require a user-authored creative layer clearly marked separate from the canonical 298 AC record.

**Travel and interaction [I]:** River mouth/coast offers potential landfall; interior travel should be obstructed by vegetation, waterways and imperfect route knowledge. Travel penalties should depend on physical terrain and actual character movement. Unknown lands should remain discoverable and map exploration should enrich *character knowledge*, not silently reveal an invented world history.

**Economy, conflict, disease [U]:** No fixed export trade, monetary system, military levy, raiding frequency, plague or armed territorial conflicts can be sourced. Generic mechanics may run for any *explicitly instantiated* residents in a mod-authored scenario; do not fabricate regional baseline dynamics and then present them as lore.

### UL-R03 — Unnamed northern Ulthos river and tributary

- **[C] Geography:** at least a main river and a tributary are depicted on the northeastern/far-eastern maps of the northwestern visible region. Original source-map river lines should be digitized once and projected into the master/terrain map. [S1]
- **[U] Names, flow direction away from traced source, tributary branching, water quality, depth and navigability:** unknown; use source tracing where visible, otherwise interpolation tags.
- **[I] Simulation role:** freshwater proximity, soils/floodplains, stream crossings, boat feasibility (only after size checks), erosion/road/path constraints, wildlife corridors, potential local settlement bias **without requiring** any town to exist.
- **[M] Spatial object proposal:** a geometry/river entry, **not** a `Settlement`, with `featureType = RIVER` and `canonicalName = null`; map-render label should remain “unnamed river” or be omitted.
- **Test:** main branch intersects traced coast at river mouth; tributary joins upstream; never flows uphill according to final elevation; no visual-only river that water simulation treats as a dry road.

### UL-R04 — Northern littoral and off-map continuation

- **[C]** Mapped northern shoreline against the Saffron Straits and Jade Sea; continent/island size unresolved. [S1][S3]
- **[I]** Rocky beaches, jungle-backed inlets and river mouth estuary *when supported by map elevation*; no invented named port, beach market or realm.
- **[U]** Safe landing, trade patterns, reefs, tides and historic contacts unknown. Marine routes get risk/range based on world geometry, not invented historical naval volume.
- **Map edge:** no abrupt south/east ocean, coast or magical wall when the source map ends. Generated land may extend beyond the authored extent only as deliberately unknown/mod-authored continuation; such extension cannot be called canonical.

### UL-R05–R06 — Ulos and its unnamed northern ruined city

**Geography [C]:** Ulos has mountainous terrain. An ancient unnamed ruined city lies somewhere in its northern part. Both should be represented as separate entities: **island** and **site of ruins**. [S4]

**Settlement state [C/U]:** Ruined city. Construction era, stone type, architects, scale, dates and fate are unknown. Avoid a canonical 298 AC governor, throne, tax base, military post, functioning city market or urban population. `RUIN` must not be conflated with `ABANDONED_STRUCTURE` unless the existing domain defines those statuses appropriately.

**Architecture [M]:** A unique **authored mystery-ruins template** is appropriate: broken street blocks, collapsed walls, fragments of large buildings and vegetation/rock encroachment. Nothing specifically attests black stone, Valyrian fused masonry, Asshai shadow architecture, pyramids, particular monuments, necromancy or an underground city. Such styling is optional fiction, not source fact.

**Current users [I]:** A player or traveling party may stop, camp, scavenge, study or reoccupy the ruin. These are *events* with existing NPC IDs and equipment/inventories; they do not imply a permanently manned 298 AC garrison or default labor market.

**Military, governance, culture, faith [U]:** All unknown. If factions appear, their source/home culture follows their **actual provenance** (e.g., an existing expedition of Essosi sailors); Ulos itself must not automatically turn them into a fabricated “Ulthosi culture.”

**Agriculture [I]:** Mountainous island terrain and limited arable patches suggest constrained land farming. Ocean fishing and tiny sheltered gardens are only options for any future settlers; whether Ulos currently supports them is unknown. No non-existent city's production.

**Unique gameplay [I]:** Archaeological investigation, gradual discovery, personal rumors, map exploration, navigation hazard and exploration logistics. Discovery should distinguish an observed structure from a theory about its builders.

### UL-R07–R08 — Saffron Straits; Asshai and Shadow Lands overlap

- **[C]** The straits connect the eastern Jade Sea toward unknown waters; the Shadow Lands and Asshai are on the **Essos** side. [S3][S7]
- **[R]** The source-code `SouthEastEssosFeatureRegistry` already owns the `saffron_straits` geographic feature; UL must reference it, **not** add a duplicate feature.
- **[R]** UL and SEE subregion raster references both label Ulos (`#677024`), reflecting geographical overlap. One stable island identity must survive however many masks overlap.
- **[U]** Do not transfer Asshai's government/religion/trade or supernatural lore onto Ulos or Ulthos. These require separate evidence and SEE-specific records.
- **[I]** Maritime route graph may connect boats between Asshai/Shadow-side waters and future southern landfalls, subject to known coastlines, actual vessel crews, supply and fog-of-war. A route is not proof of scheduled trade.

## 4. Geographic zones / environment generation contract

| Proposed simulation layer | Source-backed content | Suggested behavior [I/M] | Known prohibition |
|---|---|---|---|
| `ulthos_visible_landmass` | Land visible on official maps [C] | Apply calibrated landmask; assign continent/localization tag [R] | Do not assert mapped edge is landmass edge |
| `northwest_ulthos` | Existing [R] geographic zone; visible jungle [C] | Subdivide only as needed for terrain and gameplay | No fictional political ruler/ethnicity |
| `ulthos_purple_black_jungle` | Purple-black jungle art direction [C] | Tall forest, dense understory, low visibility [M] | No fixed canon species/poison biome |
| `ulthos_northwest_river` | At least one main branch and tributary [C] | Hydrology, crossing costs and floodplain [I] | Do not assign map-unverified widths |
| `ulthos_coastal_transition` | Partial northern shore [C] | Shoreline, estuary, coast-to-forest gradients [I] | No default recurring merchant city |
| `ulos_island` | Mountainous isle [C] | Rocky relief, maritime separation, limited arability [I] | Not a political subregion of main landmass |
| `ulos_northern_ruins` | Unnamed ancient ruined city [C] | Physical nonproductive ruin; archaeological content [I] | No auto city population or named builders |
| `saffron_straits` | Attested geographic waterway [C] | Maritime route/sea domain shared with SEE [I] | No independent territorial government |
| `off_map_ulthos` | Remainder unmapped [C/U] | Unsurveyed terrain handling / blocked bounds by explicit map policy | Do not fake official coastline or settlements |

### Climate, fertility, crops and numerical policy

For **all** UL terrestrial layers, retain separate fields for:
`vegetationType`, `forestDensity`, `elevationBand`, `annualRainfall`, `temperatureRange`, `fertility`, `cropSuitability`, `freshwater`, `navigability`, `diseaseHazard`, `predatorHazard`, `passability`, `constructionSuitability` and `confidence`.

Of those, the source supports only broad dark-jungle vegetation, mountains on Ulos and a northern river. All exact percentages, hectares, troop values, species and weather constants are `[M]` or `[U]`. No numeric values are prescribed in this dossier; derive them later from finalized physical terrain and calibrated agriculture, not from lore silence.

## 5. Repository audit — exact current integration points

### 5.1 Existing geography is present but not the full lore feature set

| Project owner | Existing item | Finding / required Step 2.2 action |
|---|---|---|
| `world/geography/zones/MapZoneRegistry.java` | `MapZoneId.UL`, **1083 × 607** | Preserve affine transform; never convert marker coordinates with an arbitrary rectangle scale |
| `world/geography/regions/UlthosFeatureRegistry.java` | `ulthos` = `CONTINENT`; `ulos` = `ISLAND` | These are only two registered UL-specific features; **missing named/unnamed ruin feature** |
| `reference/locations.json` | `ulthos` continent | Reuse stable location ID, but mark true geographic extent uncertain |
| `reference/locations.json` | `ulos` = `REGION`, parent `ulthos` | **Type discrepancy:** feature is `ISLAND` but world location `REGION`; add `geographicFeatureId: "ulos"` when schema migration allows; assess separate island containment and political neutrality |
| `reference/locations.json` | `northwest_ulthos` = `REGION`, parent `ulthos`, provenance `PLAUSIBLE` | This is an **authored simulation division**, not a source-defined realm |
| `reference/territory_zones.json` | `northwest_ulthos` polygon | Only an approximate mask for visible NW segment; must be clipped by actual landmask |
| `reference/landmass_zones.json` | `ulthos` polygon | Traced master-map envelope; **not** evidence for the full continent's borders |
| `reference/subregion_masks.json` | `UL` and `SEE` color `#677024` -> `ulos` | Deduplicate overlapping Ulos source annotations and confirm mask color priority |
| `reference/regional_influences.json` | `northwest_ulthos` low-information profile | Correctly cautious; do not turn unspecified values into invented society facts |
| `SouthEastEssosFeatureRegistry.java` | `saffron_straits`, `asshai`, `the_shadow` | These remain single SEE-owned geographic feature definitions; UL only references |
| `scenarios/agot_298_ac/landed_holdings.json` | **No Ulthos/ Ulos named lordship attested** in inspected catalog | Do not create a holding merely because a WorldLocation exists |
| `scenarios/agot_298_ac/settlements.json` | Only `winterfell` in main scenario | Do not create automatic civic Ulos settlement while marked `RUIN` |
| `reference/cultures.json` / `religions.json` | No evidence-backed Ulthosi baseline | Do not silently reuse another ethnicity or faith as “Ulthosi” |

**Specific bug candidate:** `ulos` as `WorldLocationKind.REGION` is not the same concept as its `GeographicFeatureType.ISLAND`. This may be tolerated by existing schemas, but the eventual map-location contract should distinguish **landform**, **territorial/political jurisdiction** and **settlement**, instead of using mismatched kinds as an implicit linkage.

### 5.2 Step 2.2 joins, with absent relations explicitly left null

```text
ResearchPlaceId UL-R05
    -> canonicalWorldLocationId: ulos                 [R]
    -> geographicFeatureId: ulos                     [R; location crosslink needs review]
    -> sourceObservations: [UL-NNN?, SEE-NNN?]       [U: deferred]
    -> sourceImage: authoring/maps/Realms/ul.png     [R]
    -> masterPixel / worldPosition: null            [U: deferred]
    -> landmass/territory: island in Saffron Straits [C]
    -> currentGovernmentId: null                    [U]
    -> ownerDynastyId: null                         [U]
    -> holderNpcId: null                            [U]
    -> residentPopulationTarget: null               [U]
    -> archetype: mountainous_island                [C/I]
    -> ruinChildId: ulos_northern_unnamed_ruins      [PROPOSED]
```

```text
ResearchPlaceId UL-R06
    -> canonicalWorldLocationId: ulos_northern_unnamed_ruins [PROPOSED]
    -> geographicFeature: ruin, northern Ulos                 [C]
    -> statusAt298AC: RUIN                                     [C]
    -> currentCivicPopulationKnown: false                     [C/U]
    -> initialCivicGovernment: null                            [U]
    -> agricultureWorkers / garrison / taxes: none instantiated
    -> playerDiscoverable: true                                [M]
    -> spawnStructureNow: false until verified position/footprint
```

### 5.3 Existing system consequences (do not create parallel authorities)

- **NPC and households:** `NpcRegistry`, `NpcIdentity`, `NpcState`, demographic/household architecture. Unknown settlers must not be generated as canon citizens. Expedition parties must use actual individual identities.
- **Holding, government and dynasty:** `LandedHoldingManager`, `AuthorityService`, `DynastyManager`, `TitleManager`, `SuccessionService`. No UL political assignments are validated by lore.
- **Geography and physical world:** `WorldReferenceCatalog`, `RegionalSubregionResolver`, `TerritoryZoneResolver`, `LandmassZoneResolver`, `RegionalInfluenceResolver`, `KnownWorldGeoSampler`, chunk/biome/elevation generation. Distinguish a `Feature` landmark from a polity or active `Settlement`.
- **Economy and agriculture:** `HoldingAgricultureCatalog`, `EconomyRuntime`, inventories, agricultural day coordinator. No production without real land, employer/worker capacity and ownership.
- **Social/religion/culture:** Keep population distributions unknown. Mark rumors as NPC belief/knowledge rather than facts; exploration can create observations and personal memories.
- **Military and travel:** No fixed garrison or army; visitors and maritime parties remain identifiable existing actors, with supply, location and travel time.
- **Timeline:** All links initially 298 AC; newly established outposts, claims, trades and exploration must become dated events and persist through save/reload.

## 6. Proposed Step 2.2 data record shape (illustrative ONLY)

This is a **research-to-authoring handoff**, not an existing Java/Gson schema or a new competing simulation owner.

```yaml
schemaProposal: geographic_research_place_v1
researchId: UL-R06
mapZones: [UL, SEE]
displayName: "Unnamed ruined city of Ulos"  # descriptive working label, not canonical proper name
canonicalName: null
canonicalWorldLocationId: null               # proposed ulos_northern_unnamed_ruins
parentLocationId: ulos
sourceObservations: []                       # fill with raw detector IDs later
originalRegionalPixel: null
calibratedMasterPixel: null
minecraftWorldPosition: null
structureFootprint: null
sourceStatus: CANON
evidenceReferences: [S4]
date: 298_AC
siteKind: RUIN
siteStatus: RUIN
population:
  permanentResidents: null
  civilianCityCensusEstablished: false
  provenance: UNKNOWN
government:
  institutionId: null
  titleId: null
  dynastyId: null
  rulerNpcId: null
economy:
  operationalWorkplaces: []
  activeProduction: false
geography:
  islandId: ulos
  regionId: null  # island-specific zone unassigned; mainland northwest_ulthos is not valid here
  terrainTags: [mountainous_island, archaeological_ruins]
history:
  founders: null
  foundingDate: null
  destructionDate: null
generation:
  archetype: unique_ruin
  placementApproved: false
  provenance: MOD_ESTIMATE
crossMap:
  deduplicateAgainst: [SEE]
review:
  missingOrFalseMarkerReconciliation: DEFERRED_BY_USER
```

**Island parenting guard:** `northwest_ulthos` is a mainland territorial reference and must **not** be inherited by Ulos ruins. The separate island-specific geographical zone may be created later, or `regionId` stays `null` until mapped.

## 7. Cross-map reconciliation queue (intentionally NOT performed now)

| Issue | Known basis | Final review action |
|---|---|---|
| Ulos appears as a UL and SEE reference | `subregion_masks.json` uses `ulos` in both | Join any physical markers to **one** site/island identity |
| Sea vs land polygon boundary | Existing master landmass/territory polygons are approximate | Test precise island outline and water classification |
| Ulos ruins site not separately represented | Source supports ruin, registry only has island | Create unique child landmark **after** marker/footprint verification |
| Ulthos river geometry lacks stable feature ID | River in published map; not UL Java feature | Trace actual line and tributary geometry from artwork |
| Mainland hidden beyond bottom/right map edge | Canon knowledge ends | Preserve unknown boundary policy, do not invent coastline |
| Asshai / Shadow Lands / Manticore Isles | SEE-sourced references | One physical/political identity; no UL duplication |
| Original handwritten markers | Detector output unavailable to this chat | Preserve all raw detection IDs, even false positives, in separate correction file |

## 8. Step 2.2 implementation checklist for UL

- [x] Identify the two primary UL source-backed geographic locations (`ulthos`, `ulos`) and additionally known unnamed ruin, river and littoral.
- [x] Establish 298 AC **unknown/ruined** political and residential states without fabricated headcounts.
- [x] Establish Ulos/SEE duplicate and type mismatch; separate landform from political region and inhabited site.
- [x] Record environment, construction, economy, exploration and travel **constraints**, with provenance.
- [x] Record existing repository files and explicit proposed missing reference links.
- [x] Preserve sources, IDs and crossmap review.
- [ ] Review **original annotated UL image** and Python red-X detector output; map `UL-NNN` IDs.
- [ ] Confirm map-to-master affine placement and raster/zone geometry.
- [ ] Decide whether to author any non-canon populated settlements in Ulthos; separate such choice from canon baseline.
- [ ] Write reference/ruin/river authoring data and tests after global reconciliation.
- [ ] Wire physical structures only after approved placement.
- [ ] Integrate optional future settlements with authoritative person/labor/economy/politics systems; no duplicate state.
- [ ] Test region boundary behavior, Ulos duplicate matching, unknown `null` defaults, no ghost population, save/reload and remote simulation.

**Verdict:** UL has very little canon-defined *human* data. Its research completion therefore consists mainly of **defensible unknowns** plus precise geography/ruin/identity safeguards. Adding dozens of invented rulers, cultures, harvests or populated towns would make the historical scenario *less* accurate, not more complete.

## 9. Sources and repo trace

### Published / secondary indexing of primary works
- **[S1]** [Ulthos — A Wiki of Ice and Fire](https://awoiaf.westeros.org/index.php/Ulthos); indexes *The Lands of Ice and Fire*, **The East / The Known World**, and *The World of Ice & Fire*, **Other Lands**.
- **[S2]** [Known world — A Wiki of Ice and Fire](https://awoiaf.westeros.org/index.php/Known_world); description of cartographic extent and geographical uncertainty.
- **[S3]** [Saffron Straits — A Wiki of Ice and Fire](https://awoiaf.westeros.org/index.php/Saffron_Straits); maps the Essos–Ulthos waterway, Asshai and Ulos.
- **[S4]** [Ulos — A Wiki of Ice and Fire](https://awoiaf.westeros.org/index.php/Ulos); mountainous island, unnamed ruined city in northern part. Underlying sources: *The Lands of Ice and Fire*, *The World of Ice & Fire*.
- **[S5]** [G.R.R. Martin, “Maps! Maps!! Maps!!!” (2012)](https://georgerrmartin.com/notablog/2012/10/30/); explicitly explains intentional gaps of the published map.
- **[S6]** Cartographer Jonathan Roberts, “[The East](https://www.fantasticmaps.com/2013/03/the-east/)” (2013), as indexed by [S1]; dark jungle art direction, not a scientific biome classification.
- **[S7]** [Shadow Lands — A Wiki of Ice and Fire](https://awoiaf.westeros.org/index.php/The_Shadow); distinction between Essos and Ulthos.

### Inspected project files
- `authoring/maps/Realms/ul.png`, `authoring/maps/Realms/see.png` (paths and dimensions verified in GitHub; annotated image **pixels not visually inspected**).
- `src/main/java/dev/dtzudontsay/knownworld/world/geography/zones/MapZoneRegistry.java`
- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/UlthosFeatureRegistry.java`
- `src/main/java/dev/dtzudontsay/knownworld/world/geography/regions/SouthEastEssosFeatureRegistry.java`
- `src/main/resources/data/knownworld/reference/locations.json`
- `src/main/resources/data/knownworld/reference/landmass_zones.json`
- `src/main/resources/data/knownworld/reference/territory_zones.json`
- `src/main/resources/data/knownworld/reference/subregion_masks.json`
- `src/main/resources/data/knownworld/reference/regional_influences.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/settlements.json`
- `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`
- `docs/architecture/20_3_POPULATION_AND_HOUSEHOLDS.md`
- `docs/architecture/20_3_ECONOMY_DOMAIN.md`

*This file is source research only. No GitHub files, server simulation state, or world-generation code were changed.*
