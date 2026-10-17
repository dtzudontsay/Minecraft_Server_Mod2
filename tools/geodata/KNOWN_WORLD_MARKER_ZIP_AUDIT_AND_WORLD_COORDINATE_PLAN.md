# Known World — Authored marker ZIP audit and coordinate-finalization plan

**Audit date:** 2026-10-17. **Input:** `city output.zip` supplied by the user, equivalent to `D:\GameOfThronesMod2\tools\geodata\output\authored_places`. **Scenario:** 298 AC. **Status:** verified raw detector output / spatial math; site names, snaps and spawn positions **not yet approved**.

## A. What is already accomplished

1. The existing `tools/geodata/extract_authored_places.py` reads the **eleven annotated source images** (`tools/geodata/input/cities/*.png`), detects red-X candidates, and writes `marker_candidates.csv/json` and `*_review.png` overlays.
2. Each row already contains a **regional pixel centre** (`pixel_x,pixel_y`), **logical master-map pixel position** (`master_x,master_y`), and **1:1 Minecraft coordinate** (`minecraft_x,minecraft_z`).
3. The region-to-master transforms are **identical to those in `MapZoneRegistry.java`** (mirrored in `tools/geodata/zone_transforms.py`). The master origin is `(1024, 678.5)`, scale is **about 6,000.69 metres per logical master pixel**, and current `WorldDefinition.MINECRAFT_WORLD_SCALE = 1.0` with `HORIZONTAL_METRES_PER_BLOCK = 1.0`.
4. The ZIP has **259 candidate rows**, matching the earlier screenshot's counts exactly. All 259 master positions are inside the logical master-image bounds and their derived X/Z values pass re-computation (rounding within 10 metres).
5. **All 259 `name` entries are blank**, and **all 259 `status` entries equal `NEEDS_REVIEW`**. Thus previous manual `234 / 259` match work is *not encoded* here; don't regard this as a regression, but no validated catalog yet exists in the uploaded output.
6. The ZIP contains **14 template-based detections**: **13 on NW** and **SWE-009**. Template hits require manual X-versus-line/artifact review. Remaining 245 are connected-component detections; those are still not automatically proven correct.

## B. Actual detector counts and prior manually reported name matches

| Map | Raw candidates | Previous manual matched/associated (from user's screenshot) | Previous unmatched/uncertain | Template detections in this ZIP |
|---|---:|---:|---:|---:|
| NW | 49 | 27 | 22 | 13 |
| SW | 104 | 104 | 0 | 0 |
| SI | 4 | 4 | 0 | 0 |
| NWE | 16 | 16 | 0 | 0 |
| SWE | 20 | 18 | 2 | 1 |
| NE | 10 | 10 | 0 | 0 |
| SE | 31 | 30 | 1 | 0 |
| NEE | 10 | 10* | 0* | 0 |
| SEE | 10 | 10 | 0 | 0 |
| SO | 5 | 5 | 0 | 0 |
| UL | 0 | 0 | 0 | 0 |
| **Total** | **259** | **234** | **25** | **14** |

`*`: NEE's five forts have **five distinct Xs but no canonical individual proper names**; numerical associations were considered complete in earlier review, not final site identity.

**Important:** This table preserves earlier manually claimed associations. The uploaded rows do not identify WHICH 22 NW records were left unmatched. We must compare the actual `nw_review.png` numbered crosses and red leader lines to a per-X name ledger to resolve them; a made-up list would be inaccurate. The image makes the very dense **Wall castle cluster** and **Southeast NW / the Fingers** plausible focus areas, but not proof that every template hit is invalid.

### Strong crossmap same-place candidates discovered through *actual coordinates*

| Observations | Research-based same place | Separation between raw projected points | Required action |
|---|---|---:|---|
| NWE-014 / NE-010 | Yalli Qamayi / Sathar | ~2.0 km | One ruin ID, two map observations |
| SE-031 / SO-003 | Zamettar | ~2.9 km | One ruin ID, two source Xs |
| SE-029 / SO-002 | Gorosh | ~7.1 km | One ruin ID, two source Xs |
| SE-028 / SO-001 | Vahar | ~7.7 km | One site ID, two Xs |
| NWE-008 / NE-008 | Vaes Leqse / Gornath | ~9.7 km | One ruin ID, two observations |

**Proximity is NOT duplicate proof.** Nearby distinct houses or ports must remain separate. Conversely, duplicate icon positions can be more than 10 km apart because the images are stylized and independent affine fits differ. Canonical location IDs + visual source associations decide identity; distance only flags candidates.

### Local source coordinate examples

| Name read from NW overlay | Marker ID | Region pixels | Master pixels | Calculated Minecraft X/Z | Approved? |
|---|---|---|---|---|---|
| Winterfell | `NW-029` | `(1242, 1218)` | `(349.94763,349.56061)` | `(-4050173,-1976496)` | **NO** |
| White Harbor | `NW-034` | `(1401.5,1525.5)` | `(392.38252,431.33878)` | `(-3795195,-1485116)` | **NO** |
| Moat Cailin | `NW-035` | `(1245.5,1594)` | `(350.89077,449.55901)` | `(-4044506,-1375636)` | **NO** |
| The Dreadfort | `NW-028` | `(1548,1161.5)` | `(431.33781,334.52948)` | `(-3561125,-2066813)` | **NO** |

**Pre-existing coordinate mismatch:** The Java `RegionalFeatureLocationRegistry.java` separately stores Winterfell at NW `(1230.12,1198.65)` instead of detector `NW-029=(1242,1218)`; the resulting world-space difference is roughly **36 km**. Castle Black also has an old manually-authored point. Before generating structures, establish one authoritative reviewed position and migrate old references deliberately. Neither coordinate is automatically true simply because it is present in code.

## C. What is NOT done yet

1. **Name/identity linkage:** a reviewed row must have `canonicalLocationId`, source map ID and any aliases, actual `WorldLocation`, `Settlement`, `LandedHolding`/organization references, provenance and known historic occupancy. Empty name fields imply there is *no* final mapping in this ZIP.
2. **Cross-map deduplication:** create one canonical place and multiple independent `sourceObservations`. Do not delete source X records or run duplicate settlements.
3. **Land/water validation:** use the actual runtime `src/main/resources/assets/knownworld/geodata/land_mask.png`. Its pixels are 9050×6000, **not** the logical master map size (2048×1357). Scale logical coordinates into that raster in the same direction as `KnownWorldGeoSampler` and threshold `>=128` for land.
4. **Snap rules:** nearest land search *only proposes candidates*. Verify same shore/island and named site, do not jump across a bay to an unrelated island or move inland ports arbitrarily. Record original raw point, proposed point, displacement, reason, mask version, approval person/date. If no plausible land nearby, mark `MANUAL_PLACEMENT_REQUIRED`, not “spawn in water.”
5. **Terrain/footprint:** at the 1-block metre scale, point quality is limited by original cartography: a source-map pixel can represent kilometers, and a printed red X covers a broad area. Choose a deterministic nearby buildable site using terrain heightmap, rivers, vegetation, slope, cliff, coastline and authored city requirements. Y is determined by the actual terrain/structure design—not the X's cartographic pixel or a fixed Y=180.
6. **Ownership and residences:** `winterfell` site / Stark holding / winter town / independent castle buildings are separate entities and must refer to consistent IDs. One settlement isn't one house and one X may represent an entire urban built-up area.
7. **Final outputs:** validated one-per-place master dataset, raw observations, aliases, *approved* final `(X,Y,Z)` or explicitly unresolved Y, spawn plan, and `/tp` list. An unresolved site must never default to `(0,0)`.
8. **Non-X settlements:** not every researched settlement/ruin has an authored red X; the 259 detector rows are **not the universe of all known places**. Other named sites require approved placement or later generated position rules.

## D. Proposed exact pipeline for Step 2.2

`annotated *.png → X candidates with region px → master px → calculated world X/Z → source-name review → canonical-ID resolution → crossmap merge → land mask check → candidate coastal snap → local height/slope/footprint validation → final approved (X,Y,Z) → settlement and structure generator → real persistent NPC households/economy.`

### Safeguards / authoritative data model

- `placeId` stays stable and represents a named *physical* site or spatial feature, not the detector's `NW-029` ID.
- Each place can have `sourceObservations[] = {regionalMapId, observationId, regionPx, masterPx, convertedXZ, confidence}`.
- Place can have `placedFootprint`, `entrances`, `roads`, `harbor`, `districts`, `structureTemplateIds`; a city needs more than one point to spawn correctly.
- `placementStatus`: `RAW`, `NAMED`, `DE_DUPED`, `LAND_CHECKED`, `SNAP_REVIEW`, `TERRAIN_VALIDATED`, `APPROVED`, `GENERATED` (with separate `UNKNOWN`/`REJECTED` states).
- Store `rawX/Z`, `proposedX/Z`, and `approvedX/Y/Z` separately; never overwrite evidence to conceal a correction.
- If two accepted observations disagree by kilometers, choose according to actual coastline, physical source location and terrain, not blindly average their coordinates.
- Spawn generation must be idempotent (`placeId`/dimension/structure chunk) and never duplicate after reload or exploration.
- Port/castle special cases require configurable site policies and buildable-footprint criteria; nearest-land is not always safest for canals, bridges or river-mouth towns.

## E. Downloadable tool: read-only georeference/land/snap audit

Place **`audit_authored_places.py`** at `D:\GameOfThronesMod2\tools\geodata\audit_authored_places.py` and run from repo root:

```powershell
cd D:\GameOfThronesMod2
py tools\geodata\audit_authored_places.py
```

The script reads the existing `output/authored_places/marker_candidates.csv` plus the existing 9050×6000 runtime land mask. It emits **only separate audit files**, leaving the originals and simulation files untouched:

- `tools/geodata/output/authored_places/georeference_audit.csv` (all 259, original+proposal+status).
- `tools/geodata/output/authored_places/crossmap_near_pairs.csv` (spatial duplicate candidates).
- `tools/geodata/output/authored_places/georeference_audit_summary.json` (counts, consistency and land classes).

It does not fill names, approve snaps, calculate Y or spawn settlements. If the land mask is missing, all land checks are `NO_LAND_MASK`; it still verifies projection and pair proximity. Example command customization:

```powershell
py tools\geodata\audit_authored_places.py --max-snap-metres 15000 --crossmap-radius-metres 10000
```

**Run this audit locally and return its resulting summary and CSV (or ZIP)**. We can then compare actual water hits, choose land-safe sites, and merge the earlier per-map name research into a reliable canonical coordinate catalogue.

### Important production limitation

The runtime land mask pixel spans around **1.36 km**, not one Minecraft block. The audit's proposed landing coordinate is only a first-stage candidate. Real village generation still needs a **fine-resolution terrain examination and settlement-footprint placement pass** within the game world. Otherwise a point can technically be on land but be on a cliff, mountain face, road, beach or within a river.
