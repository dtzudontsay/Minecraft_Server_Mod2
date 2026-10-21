# C4.1 — Connected physical settlement districts

## Authority and compatibility

B2/B3 remains the geographic and political authority (253 markers / 259 holdings).
C1 defines speculative housing and workplace demand. C2 remains the chunk-local
physical constructor; C3 renders each building. C4.1 changes only which *lots*
and roads are fed to C2 at two existing authorized pilot sites.

- **Ramsgate** (`NW-031`): small northern pilot.
- **Acorn Hall** (`SW-023`): medium Riverlands pilot.
- No new locations, rulers, NPCs, households or workplaces are instantiated.
- No previously generated chunks are modified, and Winterfell V3 is untouched.
- `settlement_construction_c2_v1.json` is preserved as the baseline.
- `settlement_construction_c4_v1.json` is the new construction manifest.

## Implemented in C4.1

- Compact, connected multi-street cores with a predictable per-site core size.
- Housing and workplace lot demand from C1, restricted by transparent phase caps.
- Streets, building door paths, and outside-road connectors are pre-indexed per chunk.
- Farms with physical irrigated field annexes on a separate rural belt.
- The legacy C2 farmland painter handles `FIELD`; the C3 building renderer handles
  all other authorized construction module types.
- Structural variation comes from the 43 C3 procedural architectural templates.
- C2's exact per-lot geographic land/elevation safety survey is still authoritative;
  its existing slope/water and vanilla decoration guards are preserved.
- Plans fail if plots overlap, streets enter footprints, or the street network
  has more than one disconnected component.

## Explicitly not completed by C4.1

- The planning census is **not** an existing NPC census; unbuilt housing remains
  counted as a backlog. `housingCapacityEstimate` describes planned capacity,
  not guaranteed beds or actual residents.
- Unsafe real-terrain plots are skipped by C2 and may leave disconnected **actual**
  roads despite a connected 2D planning grid. Cliff routing and retaining walls
  still require a terrain-aware C4 follow-up.
- Major-city castle compounds, walled capitals, Ghiscari pyramids, the Five
  Forts and ruins require separate approved authored templates, not generic lots.
- Individual family signs require B4 real household linkage.
- Real staffing, production, routines, birth/death and economic effects require B4/D.
- C5 worldwide activation and C6 persistent world expansion are **not** enabled.

## Install

1. Back up the test world. Extract these project-relative files into the repo.
2. Run `INSTALL_AND_VALIDATE_C4_SETTLEMENTS.bat`.
3. Verify C4 data generation + Gradle build. Query `/kwc2 status`, `/kwc2 sites`.
4. Observe **unvisited chunks only**. Existing pilot chunks do not regenerate.

`py tools/geodata/build_c4_settlement_layouts.py --check` checks deterministic output.
`py tools/geodata/install_c4_settlement_layouts.py --check` checks the Java hook.

Running the old C2 installer still generates only its original manifest, but C4
uses a *separate* resource path, so C4 output does not get overwritten. If the
old C2 installer replaces the Java file, rerun the C4 installer to restore its
resource pointer and FIELD dispatch.