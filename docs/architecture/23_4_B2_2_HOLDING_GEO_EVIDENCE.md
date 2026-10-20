
# Known World — B2.2 Holding / Geographic Evidence

Scenario: AGOT 298 AC. Status: implementation, local acceptance pending.

## Source inventory

- 259 authored political holdings; 249 distinct linked world-location IDs.
- 253 **active red-X map observations** after false-positive removal and five recovered observations. They are NOT the same 259 things as the political holdings.
- Existing active CSV contains 64 named records representing 59 distinct canonical names. Additional named crosswalks provide labels for 202 marker IDs, raising source naming evidence coverage to 252/253 distinct marker IDs before conflicts/verification.
- Existing research dossiers contain historical, architectural, house, cultural, religious and population-range proposals. A research population estimate is not an instantiated person count.
- The marker reconciliation summary explicitly sets **zero final_approved_X/Y/Z coordinates**. Candidate world X/Z coordinates therefore must not be used to spawn buildings, people, caravans or structures yet.
- Some markers represent ruins, territorial or religious sites, or possibly overlapping atlas duplicates; they are not necessarily active settlements.

## What B2.2 does

`tools/geodata/build_b2_holding_links.py` performs a deterministic, source-derived join using only:

1. Authored holding ID -> worldLocationId in `landed_holdings.json`.
2. Geographic reference IDs and display names in both `locations*.json` resources.
3. Marker ID, X/Z and provenance in `marker_catalog_active_253.csv`.
4. Research name crosswalks `research_marker_name_index.csv` and `sw_marker_name_crosswalk.csv`.

A marker links to a holding only where the identity is **unambiguous** after exact ID/name normalization. No closest-place lookup, fuzzy spelling guess, geographic nearness heuristic, or forced unique matching is performed. Conflicting names and unlinked markers remain review entries. Slashed alternative names are deliberately not assumed to identify one site.

The tool generates three derived files:

- `src/main/resources/data/knownworld/scenarios/agot_298_ac/holding_geo_candidates.json`: full 259-holding candidate-evidence manifest bundled into the mod, containing no approved build coordinate and no Y coordinate.
- `tools/geodata/output/b2_holding_links/holdings_259_audit.csv`: one line per political holding, including house owner, holder ID, geographic reference and candidate markers.
- `tools/geodata/output/b2_holding_links/markers_253_audit.csv`: one line per existing map observation, with current name sources, conflicts, X/Z and land review status.

`KnownWorldHoldingGeoCandidates` reads the packaged candidate manifest without creating or relocating anything. It provides `find(authoredHoldingId)` for subsequent B3 authoring and read-only `/kwgeolinks` commands.

## Important non-conflations

- Map marker ≠ settlement ≠ holding ≠ dynasty ≠ NPC residence ≠ building plot.
- `REGION` and `TERRITORY` holdings are not automatically physical towns.
- `ownerDynastyId`, `holderNpcId`, `populationWeight`, `taxBase` and `militaryValue` are distinct facts; none is a settled local population count.
- A map marker's X/Z is a candidate anchor. Even `LAND_AT_RAW_X` does not establish navigability, plot boundaries, dry door access, appropriate elevation, or ownership.
- Uninstantiated cohort totals do not become separate unnamed actors driving events. B4 will reconcile identity and population ownership.

## Run

From the repository root:

```powershell
py tools\geodata\build_b2_holding_links.py
py tools\geodata\build_b2_holding_links.py --check
.\gradlew.bat clean build
```

In a world:

```text
/kwgeolinks status
/kwgeolinks holding winterfell_domain
/kwgeolinks holding north_domain
```

These are **inspection** commands. No migration or generation occurs.

## Next research and simulation connections

**B2.3:** resolve remaining ambiguous marker identities and shared-site observations with already-authored dossiers and confirmed corrections. Approve coordinates only after land, slope, building footprint and shoreline checks. Do not request all 250 coordinates from the user again.

**B3:** classify physical sites MAJOR/MEDIUM/SMALL/NO_SETTLEMENT/UNKNOWN, independently of political holding type. Regional house and occupation records guide architecture and function.

**B4:** establish exactly-one-population-authority semantics, then attach households, population distribution and job targets to canonical settlement IDs. `HoldingPopulationProjection` currently models aggregate inhabitants and is not an individual-NPC registry; migrate carefully.

**B5:** place authored named characters by canonical scenario state, not by matching house surname to an arbitrary castle.
