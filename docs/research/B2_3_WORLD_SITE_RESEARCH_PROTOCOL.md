# Known World — B2.3 world research inventory, 298 AC

**Date:** 2026-10-20  
**Status:** Source-labeled, provisional, and **non-authoritative**. This is a research/authoring package, not a worldgen or population migration.  
**Coverage:** all **253** active map observations and all **259** authored political holdings, including records without matching map markers.

## 1. How to run

Place these files in the corresponding directories of your existing `Minecraft_Server_Mod2` checkout:

- `tools/geodata/build_b2_3_world_profiles.py`
- `tools/geodata/b2_3_world_research_overrides.json`
- `tools/geodata/b2_3_research_html_template.html`
- `docs/research/B2_3_WORLD_SITE_RESEARCH_PROTOCOL.md`

Run from `D:\GameOfThronesMod2`:

```powershell
py tools\geodata\build_b2_3_world_profiles.py
```

**No dependencies** beyond Python's standard library; **no Minecraft or Gradle build**.

It reads the **existing pushed files**:

- Active marker corpus: `tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv`.
- B2 marker-link report: `tools/geodata/output/b2_holding_links/markers_253_audit.csv`.
- Names: `tools/geodata/research_marker_name_index.csv`, `tools/geodata/sw_marker_name_crosswalk.csv`.
- Political scenario: `src/main/resources/data/knownworld/scenarios/agot_298_ac/landed_holdings.json`.
- Geographic reference catalogs: `src/main/resources/data/knownworld/reference/locations*.json`.
- Prior regional research: `docs/research/geography/KNOWN_WORLD_RESEARCH_2_1_{ZONE}_298AC.md`.

Outputs (new, only under `tools/geodata/output/b2_3_world_profiles/`):

1. `WorldResearchAtlas_253.html` — **the main browsable overview**. Open this in a web browser, search/filter, read source tier and notes, enter reviews, export reviews to `;`-delimited Excel-compatible CSV.
2. `site_profiles_253.json` — all mapped observations with independent evidence grades per field.
3. `holding_profiles_259.json` — all political holdings, even unmapped, with independent/no-independent population rules.
4. `site_profiles_253_excel_de.csv` — semicolon-separated and UTF-8-BOM for German Excel.
5. `holding_profiles_259_excel_de.csv` — semicolon-separated and UTF-8-BOM for German Excel.
6. `README_RESEARCH_STATUS.md` — generated counters and safeguards.

No overwrites to existing source CSV files, scenario files, world save, Java code, houses, NPC identity/registry, physical blocks, or atlas marker positions.

## 2. Evidence tiers — **per value, not per file**

- **CONFIRMED:** supported by an explicitly identified published setting source **or** directly copied from an authored project record with its provenance clearly named. A project scenario fact is not necessarily independently confirmed book canon.
- **CONTEXT:** plausible geographic, historical, political or cultural inference, or a **candidate** join. The B2 marker-to-holding association remains a candidate, not an authenticated location.
- **INVENTED:** a transparent, revisable mod design estimate (especially demographic quantities, job splits, household sizes, labor rates). The source field is intentionally blank rather than faked.

The exact **~500,000 inhabitants of King's Landing** is a book-attributed *approximate* estimate, not a precise census; its invented low/high planning bounds are flagged separately. All broad job-distribution templates are invented even for historically well-attested settlements.

`pop.baselineEvidence` and `pop.planningRangeEvidence` must be read separately. A confirmed city identity does **not** certify our made-up local labor percentages.

## 3. Research invariants

1. **A holding is not a city and a map marker is not a holding.** An inhabited place can lack a noble owner (Five Forts); several holdings can share one site (Gulltown, Saltpans).
2. **No person is created by research estimates.** Population targets are for B4-authorized, uniquely identified individuals in a later stage. The old anonymous cohort mechanisms cannot independently create phantom population beside identified NPCs.
3. **No doubles for duplicate pixels.** Repeated geographic IDs across overlapping map subzones retain research records but have **one primary population planning observation**. Shared city subholdings do not each get city population counted again.
4. **Ruin / unverified existence:** default independent resident count is `0` to prevent unsafe automatic seeding. That is an *admission of no approved permanent civic census*, not a claim that absolutely no travelers can visit.
5. **Fort capacity ≠ active garrison.** The Five Forts are individually capable of housing **10,000 men**, but each receives a separate invented 700–3,500 range for its proposed current resident/service population. This range is *not* a verified unit roster.
6. **Unique polity vs named ruler:** `ownerDynastyId` is not enough to invent an individual contemporary ruler. Keep absent `holderNpcId` null.
7. **Year 298 AC only.** Do not preload Daenerys's conquest, the Red Wedding, Bolton occupation, future commanders or city destructions into the opening AGOT setting.
8. **Do not promote coordinates to build-approved.** Candidate X/Z says only where the red X was observed. Y, slope, safe footprint and user review remain unresolved.
9. **Occupations are percentages of workers**, not percentages of all individuals. The labor participation estimate is separate. A guard who farms on the side is *one individual* with multiple jobs or affiliations, not two persons.
10. **Do not sum parent and child jurisdictions**, or a city's nested castle and its city population.

## 4. Researched scenario examples

| Marker | Status at 298 AC | Authority/culture | Population basis |
|---|---|---|---|
| `SW-046` King's Landing | Inhabited major city | King Robert; crown bureaucracy, City Watch, trade, Faith of Seven | Approx. 500,000 **confirmed book estimate**; occupational shares **invented** |
| `NW-029` Winterfell | Inhabited castle and Winter Town | House Stark; old gods predominant | Working simulation ~5,000 vs older authored 1,200-person census: **requires reconciliation, not a replacement** |
| `NW-034` White Harbor | Inhabited major northern port | House Manderly, New Castle, southern trading and Seven influences | Inhabitants/occupations **invented** |
| `NW-039` Greywater Watch | Occupied moving crannog | House Reed, Howland Reed, old gods | **No fixed approved construction footprint**; mod baseline invented |
| `NW-008` Castle Black | Inhabited active Night's Watch fort | Watch officers/stewards/builders, sworn brothers | Staffing envelope **invented**, not an extra village |
| `NE-007` Vaes Dothrak | Inhabited sacred city | Dosh khaleen, servants and visiting khalasars | **Small resident base** despite huge capacity; estimate invented |
| `NWE-001` Braavos | Inhabited maritime Free City | Sealord, Iron Bank, plural faiths, no slavery | Estimates invented; finance/seafaring supported |
| `NWE-005` Saath | Surviving Sarnori settlement | Surviving Sarnori inhabitants | Inhabited is historical, count invented |
| `NWE-013` Sarnath | Ruined | Former Sarnori city destroyed by Dothraki | **No contemporary civic population seeded** |
| `NEE-001` Nefer | Inhabited | N'ghai city; extensive subterranean built environment | Population invented |
| `NEE-005`–`009` Five Forts | Five extant frontier fortresses | Yi Ti frontier; individual commanders unknown | Each **10,000 capacity ≠ 10,000 soldiers**; current occupancy estimates invented |
| `SE-008` Meereen | Inhabited Ghiscari city | Great Masters, pre-Daenerys slavery | All workforce distributions invented |
| `SE-024` Old Ghis | Ruined | Ancient Ghis destroyed millennia ago | No modern city seeded |
| `SE-026` New Ghis | Inhabited separate city | New Ghiscari with free legions | Invented demographic target, distinct from Old Ghis |
| `SEE-009` Stygai | Ruined, fearsome location | No ordinary active municipal government | No ordinary city population |
| `SEE-010` Asshai | Inhabited, extremely low density | Shadowbinding/trade, diverse faiths | **Market-town population** within huge area; no children; numbers invented |
| `SI-003` Tall Trees Town | Inhabited sacred/commercial town | Summer Islander religious and civic tradition | Invented demographic/job distribution |
| `SWE-020` Valyria | Ruined since the Doom | No living Freehold government | No live Valyrian-era city population |

## 5. Supporting source register

These are site/context references for the **specified** fields, *not proof of every number in the dataset*:

- King's Landing population and government — https://awoiaf.westeros.org/index.php/King%27s_Landing
- Five Forts' physical nature and **capacity** — https://awoiaf.westeros.org/index.php/Five_Forts
- Asshai's low density and absence of children — https://awoiaf.westeros.org/index.php/Asshai
- Stygai's ruined status — https://awoiaf.westeros.org/index.php/Stygai
- Braavos: Iron Bank, multicultural faith, maritime trade — https://awoiaf.westeros.org/index.php/Braavos
- Sarnori ruined cities; Saath's survival — https://awoiaf.westeros.org/index.php/Kingdom_of_Sarnor
- Old Ghis — https://awoiaf.westeros.org/index.php/Old_Ghis
- Valyria — https://awoiaf.westeros.org/index.php/Valyria
- Vaes Dothrak — https://awoiaf.westeros.org/index.php/Vaes_Dothrak
- Greywater Watch — https://awoiaf.westeros.org/index.php/Greywater_Watch
- White Harbor — https://awoiaf.westeros.org/index.php/White_Harbor
- Summer Isles — https://awoiaf.westeros.org/index.php/Summer_Isles
- Tall Trees Town — https://awoiaf.westeros.org/index.php/Tall_Trees_Town
- Yi Ti — https://awoiaf.westeros.org/index.php/Yi_Ti
- Nefer — https://awoiaf.westeros.org/index.php/Nefer
- Qarth Pureborn — https://awoiaf.westeros.org/index.php/Pureborn
- New Ghis — https://awoiaf.westeros.org/index.php/New_Ghis
- Storm's End — https://awoiaf.westeros.org/index.php/Storm%27s_End
- Volantis — https://awoiaf.westeros.org/index.php/Volantis
- Astapor Unsullied — https://awoiaf.westeros.org/index.php/Unsullied

Also consult **all ten existing region-specific** `KNOWN_WORLD_RESEARCH_2_1_...` dossiers; they contain many site-by-site notes on canon/source location, material palette, law, religion, trade and especially provisional regional population and profession proposals. The generator records the dossier path for each zone and does not overwrite it.

## 6. What needs reviewing later

- **111** markers have explicit authored site overrides; **142** get cautious location/type/region templates pending more per-site sources. Thus **coverage is complete**, but the historical depth is intentionally uneven. Never call the generic 142 "researched and confirmed".
- Many holdings have no red-X marker; we still generate a **259-holding political research** file separately. Do not give them invented marker coordinates.
- Some canonical cities have nested noble seats. `holding_profiles_259.json` marks candidate building-inside-city cases such as the Red Keep, New Castle, Hightower and Great Pyramids.
- Some markers have image-visible identities but no political holding (e.g., Five Forts). They retain their own physical site profile.
- **Red-X/atlas coordinate confidence is independent from a source-confirmed historical identity**. All physical placements still need the user's map review, elevation and footprint checks.

## 7. Future architecture: scale Winterfell without copying it 252 times

Extend current *authority*, do not create parallel population engines:

### B3: placement and site archetypes

- `GeographicSiteId` references stable authored marker/geography ID with independent `holdingIds[]` (zero, one or many).
- A single real settlement can hold many political assets. One political domain can contain many physical sites.
- Site state includes `siteType`, `MAJOR/MEDIUM/SMALL/NO_SETTLEMENT`, ruined/seasonal/inhabited flag and approved footprint.
- Regional environment/material influence obtained from real zone/masks; architecture type separately.

### B4: unique individual residence

- `NpcId` is the sole identity; `SettlementId` residence and `HouseholdId` membership link to **one actual site**.
- Demographic *targets* are authoring input, not extra invisible people.
- Migrating old aggregate `PopulationCohortManager` requires explicit conservation tests; no automatic background cohort added.
- Expose low-fidelity remote simulation views without generating Minecraft villagers for every individual.

### C: reusable physical generator

- Winterfell V3 must become one archetype plugin, not a duplicated full generator file per settlement.
- `site_profile` feeds building-density, house/workshop/garrison/temple demand, cultural materials and terrain rules **after** site approval.
- Permanent service NPC roles and trade jobs map to real employed persons and real physical buildings.
- Forested/lava/water intersections, open doors/ladders/lantern supports, roads, mountain slope and safe Y are per-plot validators.

### D–I: economy and character identity integration

- A person can work, buy, trade, travel, marry, inherit and die; property changes transfer to **the same** individual and actual household.
- Settlement production and food deficits are derived from real workers, resource inventories and soil, not static guessed occupation percentages once the simulation is live.
- Political control is derived from the current `LandedHoldingManager`, which may change by succession or war, **never from static research overrides**.
- The current culture/religion/political context can be used as fallback *starting profile*, but authoritative per-NPC profiles still override it.

## 8. Review incorporation

When user review CSV arrives, import approved name corrections back into the **source marker catalog/reconciliation process**, not directly into world saves. Then update research override IDs and rerun the generator. Keep all manual changes auditable with provenance and date. Never update `CONFIRMED` automatically merely because the user marked a pixel in the browser: there are separate *identity*, *position* and *historical evidence* checks.
