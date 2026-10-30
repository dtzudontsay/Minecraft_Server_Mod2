# AV3 — Regional Correlation & Family-Linked Starting Profiles (2026-10-29)

**Audited starting point:** GitHub `dtzudontsay/Minecraft_Server_Mod2` at `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6`; upstream `master` contains AV2A, and this package requires separately installed **AV2B v1.1 + AV2C + AV2D**. AV3 does not modify scenario JSON, saves or original Java enums. It is a **read-time starting-profile enrichment**, not a child-birth engine or verified behavior event.

## Canonical vocabulary and complete source lineage

- Preserve the user's **761 original Excel rows** (706 canonical/other definitions as previously audited) in `AV3_ATTRIBUTE_COVERAGE.csv`; no aliases become extra runtime attributes. Preserve the actual **124 Java numeric enum dimensions** in `AV3_RUNTIME_AXIS_INVENTORY.csv`: 20 skills, 20 aptitudes, 26 dispositions, 33 values, 25 norms. Of these, AV3 modifies **104** starting axes; the **20 learned skills** remain the B4 existing age/occupation-based starting estimates. **Seven** canonical Java character-value axes do not appear among the original Excel value rows: `WEALTH_SEEKING`, `MARTIAL_GLORY`, `POWER_SEEKING`, `PRESTIGE_SEEKING`, `KNOWLEDGE_SEEKING`, `FAMILY_FORMATION`, `ROMANTIC_LOVE`. All remain included, not fabricated as spreadsheet lines.
- Original B4.1A.1 `Person` object and `b4:site_...:` ID remain the sole B4 person authority. AV3 does not import a duplicate `NpcState` and does not change `CharacterProfileManager` for older actors.
- Data inputs: `src/main/resources/data/knownworld/reference/{cultures,religions,regional_influences,regional_influence_extensions}.json` (existing user-controlled authoritative catalogs), not an invented set of new culture/faith labels. Current repository has **52 culture IDs, 33 religion IDs, 37 base regional profiles plus 16 extensions**; the extensions merge by `locationId`, producing **42 distinct regions**.

## Exact active effects

AV3 adds `KnownWorldAv3RegionalPriors` and `KnownWorldAv3Formation`:

1. On first read, validate culture/religion catalogs, enum axes, weights, finite limits and region records. Combine base regional influences and extensions keyed by location (extension entries update fields, do **not** count as additional regions). Build a read-only lookup of **weighted-average regional value/norm correlations** for each canonically named culture/faith **only if an authored regional profile actually contains that group exposure**. Groups with no such evidence receive **no** culture/faith modifier.
2. Starting personal character values and social norms are adjusted by a modest *culture correlation* coefficient `0.18` and a *faith correlation* coefficient `0.09`. These are **mod balancing estimates**, not factual religious doctrines, group determinism or descriptions of every individual. They are computed from existing signed regional influences, not newly invented biases.
3. The **B4 site-level regional values and norms are already included by `KnownWorldB41Activation.enrich(...)`** with weights `0.70/0.72`; AV3 explicitly does not add those values again. A culture or faith correlation is a *separate, smaller influence* from other locations sharing that category. Future AV7 must replace these crude priors with actual regional/institutional/cultural rules where appropriate.
4. Give shared, deterministic baseline family-context variation to **20 natural aptitudes**, **26 dispositions**, **33 values**, and **25 norms**. Use the canonical mother/father ID pair when supplied; otherwise use the household ID as shared *social environment*, NOT implied genetic relation; if neither exists use the person ID. Each dimension gets a bounded stable family-linked offset: aptitude scale `0.055`, disposition `0.085`, value `0.035`, norm `0.035` times `[-1,1]` seed. **A parent's actual measured profile is NOT yet looked up or inherited**; AV3 family effects are intentionally labeled a *shared statistical prior*, with real parental development reserved for AV3B/AV6.
5. AV3 adjusts the **generated starting snapshot only**. It does **not** award skills, experience, money, pregnancy, legal status, faith conversion, language fluency, employment, title, relationship or traits; it does not rewrite the original B4 identity and does not emit an AV2D verified event. It may affect AV2B-derived tendencies through dispositions, values, norms, and aptitudes, and the resulting snapshot appears in right-click and K-key dialogue.
6. AV3 validates the complete profile in a separate JSON copy before applying any group; invalid/missing axes, malformed references, or mismatched `personId`/`siteId` result in `av3State=BLOCKED_UNVERIFIED_STARTING_PRIORS` **without partial profile changes**. The original B4 snapshot remains available; no AV3 bias is claimed as verified if blocked. This is a starting-state adjustment, not an observation of individual personality.
7. The index records a **SHA-256 fingerprint of the four authored resource arrays**; right-click profile metadata exposes `regionalSourceDigest`. This helps identify changes in the starting-input basis. **Important limitation:** the index isn't pinned per save. If the game's reference files change between versions, generated starting priors can also change. AV3 does not silently migrate persisted AV2A manual overrides or AV2D receipts; the existing read-time precedence remains well defined.

### Exact profile read sequence

`B4 deterministic person/age/occupation + site region baseline → AV3 culture/faith/regional-correlation + family-linked starting prior → AV2A sparse authored override → AV2D verified outcome changes (if any) → AV2B shared derived psychology`.

Never modify B4's 3.76m source records or journalled economic assets when evaluating an AV3 profile. AV2A manual overrides take precedence; AV2D verified outcome delta changes occur later. Current legitimate AV2D producers remain absent; an AV3 prior never becomes a completed event.

## Changes included

- **Add:** `KnownWorldAv3RegionalPriors.java` — validates reference files, merges extensions, computes cached immutable weighted priors.
- **Add:** `KnownWorldAv3Formation.java` — deterministic starting adjustments, diagnostics `/kwformation status`, status metadata.
- **Patch only:** `KnownWorldB41Activation.java` — one import, one registration call, and one call inside the existing `enrich(JsonObject sheet, Person p, Site site)` method **immediately before AV2A's `applyToSheet`**, so both NPC inspection and K chat share the adjusted profile.
- Add guarded installer, source/regression checks, Java21 smoke, complete 761-row CSV and separate 124-axis Java enum inventory. Existing saves and resources untouched.

## What AV3 does *not* claim

- No real inherited traits from biological parents, genealogy resolution, spouse-relation inference, or live upbringing from recorded childhood events. AV6 will use older `ChildFormationService` and worldwide B4 family relations once a verified common identity adapter exists.
- No actual cultural or religious adherence rules, doctrinal checks, location-specific laws, linguistic mutual intelligibility, conversion or education changes. The index is a regional correlation estimator, not a cultural/faith authority engine.
- No autonomous goals, economic production or social decisions. These are AV4/AV5/AV7/AV8 responsibilities.
- No complete every-character evaluation. Only B4 physical projected person inspections/conversations generate AV3 snapshots on demand. 3.76m individuals are never expanded into RAM.
- No guaranteed stable historical starting values if authoritative reference files are edited later. They are deterministic **for the same inputs** and schema.

## Validation and acceptance

1. Extract archive into repo root, run `RUN_AV3_CHECK_ONLY.bat`. It verifies all AV2 prerequisite source hooks, enum definitions, all 761 original lineage rows, reference catalog membership, and Java 21 smoke. It makes no Java changes.
2. Run `RUN_AV3_INSTALL_AND_TEST.bat`; an exact source patch is backed up under the Git directory; compile the **real Fabric project** with Gradle. Any compile failure rolls back modified source and newly copied files. No Minecraft saves are opened during install.
3. In a disposable test save later, run `/kwformation status`, inspect two B4 projected people in same household, and verify `av3Formation`, `av3State`, 104 expected axes, AV2B tendencies, AV2A manual override priority, AV2D overlay order, and values across restart.
4. Change a reference file only in a disposable environment; verify the source digest changes and no duplicate regional-profile count is introduced by the overlay file. Record and approve any intentional baseline migration before adopting into a long-lived save.
5. Before AV4, implement and verify named, executable action choices; AV3 priors alone are **not real autonomous NPC behavior**.

## Next decisions & roadmap

- **AV3B / AV6:** actual measured parental profile effects and validated child development; decide relative weights of biological aptitude, household, guardian, peers, experience and regional assimilation before enabling persistent live formation.
- **AV4:** context-driven decisions using new starting profiles and existing 12 derived psychology tendencies, with reproducible explanations and verified, durable world effects. Keep explicit `SourceEvent` proof before calling AV2D; don't reward workplace presence.
- **AV7:** move from regional correlations to authored cultural norms and faith doctrine/institutions; avoid assigning false ASOIAF universals.
- **AV8 onward:** productive work, social mobility, law and political systems use typed attributes only after actual action authority exists.
