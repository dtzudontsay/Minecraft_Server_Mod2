# Known World — October 22, 2026 construction checkpoint

**Repo:** `dtzudontsay/Minecraft_Server_Mod2`, `master`, reviewed at source-tree SHA `522b06031a642c4c408eb4cadacc0ee75140521e` (as exposed by GitHub's tree API); GitHub is read-only to the assistant. This file is a continuation/handoff, NOT a new competing project roadmap.

## Current intended vision (user supersedes earlier design advice)

* Reuse **Winterfell-style major castle, inner/outer walls and expanding rings** for **every** MAJOR physical site at this development stage, varying actual house/workplace demand by the site. No realism-based exclusion merely because King's Landing eventually deserves special architecture.
* For SMALL and MEDIUM, adopt a corresponding manor/keep + walls and inward/outward connected-district archetype, not a scatter of generic houses without a main civic seat.
* Keep every C1 population and C4.4 job-demand number as the planning baseline unless explicitly revised by a demographic audit. No pilot caps on houses or workplaces. Residential *buildings* can hold several household units as modeled by C1.
* Preserve Winterfell's authored keep, walls, existing V3 lot positions where the current compact C4.6 layout applies, 5,000 people, 1,136 households and identity IDs. Generalizing the *algorithm* cannot overwrite existing worlds.
* Coastal/mountain places must get automatic retry attempts before failing: search sufficiently large dry connected land, orient inland; terrace and switchback where practical, never erase the authored relief; persist full reasoned failure reports.
* Planning reservations first, actual NPC IDs, household family graph, worker staffing and owners only through future authoritative B4/D managers. Do not double count populations or invent 3.76M live entities.

## Repo files and status inspected

* Active packages: `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/` including `KnownWorldUniversalWorldgenC2`, `KnownWorldC3Architecture`, `KnownWorldC4RoadPainter`, `WinterfellWorldgenV3`, `KnownWorldStructureSigns`, `KnownWorldC47ArchitecturalDetails`.
* Current C2/C4.5 manifest still limits concrete chunk decoration to a small pilot set; `KnownWorldUniversalWorldgenC2` loads `settlement_construction_c4_v1.json`, tests terrain before rendering C3 lots and skips unsafe individual plots. It does not global-enable all 248 sites or shift failed shore/mountain anchors.
* C4.6 compact Winterfell has 1,913 plot reservations (1,136 homes, 777 other sites), 24 decorative field annexes and the original 5,000-person V2 IDs. C4.7 review catalogue contains 61 named universal C3 template programs; a full castle plus wall archetype is not yet a reusable render contract.
* C4.8 reference directory supplies 248 physical site rows, 248×204 job matrix, 6,839 nonzero workplace-kind rows, 150,963 workplace buildings requested and audit summary. 3,760,684 people; 1,880,341 workers; 47 major sites, 38 general and nine exceptional; 198 inhabited.
* Earlier `KNOWN_WORLD_C4_PROJECT_CHECKPOINT_2026-10-21.md` is stale in its statement that C4.3 is pending. This newer checkpoint supersedes **only that implementation status claim** and must be read together with the authoritative long-term master roadmap and `KNOWN_WORLD_PROJECT_VISION_AND_ROADMAP_INDEX_2026-10-21.md`.

## C4.9 supplied by this answer

* `tools/geodata/c4_9_settlement_ledgers.py` uses the *unchanged* C4.8 sources to produce all-site JSON, household reservations (without duplicating Winterfell), workplace reservations (without duplicating Winterfell), and outstanding blueprint-policy exceptions. It conserves people and workers and includes the nonzero 204-job quotas per site.
* `KnownWorldC49TerrainPolicy.java` is a pure sampler-driven preflight module with closest sufficiently connected dry land search, inland vector, terrace/switchback advice and structured failure result; it has not yet been wired to worldgen.
* Python synthetic/unit tests succeeded in this assistant environment; standalone Java class was compiled and smoke-tested for coast, slope and failure. No full 26.3 Gradle build or Minecraft runtime visual test performed.
* The user must extract the supplied files and run `RUN_C4_9_PLAN_READONLY.bat` locally to materialize the complete 248-site ledgers against the checked-in CSVs. **Do not claim those generated ledger files already exist in GitHub.**

## Next verified task

Proceed to **C4.10 physical integration**: one authoritative universal layout pipeline for all three inhabited generic tiers, walls/main buildings as concrete building modules, a one-time authored-terrain anchor preflight and per-lot persistent failure register; do not regenerate Winterfell or change existing chunks. Use chunk-indexed/lazy pagination at city scale (King's Landing already demands 26,596 residential and 19,475 workplace buildings). Add 26.3 runtime Gradle tests and a new-world demonstration before default worldwide activation. Then enable the eligible 248-site catalogue under versioned worldgen manifests; connect B4/D populations and jobs after deduplication and conservation validation. Preserve the user's additional unshared roadmap as a separate later discussion.
