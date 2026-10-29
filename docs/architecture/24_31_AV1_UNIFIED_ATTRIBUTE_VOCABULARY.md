# AV1 — Unified attribute vocabulary & canonical type contracts

Baseline: `dtzudontsay/Minecraft_Server_Mod2`, `master` at `28a2e02db9e8dd9a3adb15158672c5c71b8f85d5` (2026-10-29).

## Why AV1 exists

All 762 rows of the user's `values(1).xlsx` / AV0 master audit must stay traceable. Row 1 is the workbook header. Among rows 2–762, 706 are value/field entries and 55 are headings. They map to 30 **typed domain labels** in the AV1 resource. This is a source-coverage count, **not** 706 operational personality modifiers or real-time NPC effects.

The existing `CharacterProfile`, `CharacterPsychologyService`, `SocialReactionService`, `ChildFormationService`, `CultureService`, `ReligionService`, scenario reference catalogs, political and legal managers remain the only authoritative owners of their respective types/state. AV1 creates **no alternative profile store, culture manager, person ID, house registry, inventory, or ruler**.

## Actual deliverables

- `src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json` — immutable schema, all source-row links and canonical definitions.
- `KnownWorldAv1Vocabulary.java` — read-only resolver and startup-compatible debug command registered through the existing `KnownWorld` entrypoint.
- `tools/av1/test_av1_vocabulary.py` — source data, enum and catalog cross-checks. Missing enum/catalog entries fail closed.
- `docs/architecture/AV1_VOCABULARY_CROSSWALK.csv` and `AV1_Attribute_Coverage.xlsx` — one-to-one human-readable coverage, including original row numbers.
- Guarded installer and check-only Windows batch files. Installation snapshots the changed `KnownWorld.java`, runs Python source checks, applies two narrow registration lines, and invokes Gradle `compileJava`. Any failed build restores the entrypoint and removes newly created files.

## Canonical/legacy policy

1. Existing exact enum and reference-catalog IDs are canonical. Typos or former free-form labels may be accepted **only as explicit read-time aliases** for imports/debug lookups; they are never persisted or emitted as canonical identifiers.
2. `EGALITARISM` -> `EGALITARIANISM`, `MINORITY_ILL` -> `MINORLY_ILL`, `rope_marker` -> `rope_maker`, etc., are deterministic, domain-scoped resolutions. There is no global untyped string rewrite. See the CSV for all aliases and authoritative ownership.
3. `RUIN` is not a fifth ordinary `SMALL/MEDIUM/MAJOR/SPECIAL` settlement tier. It resolves to existing C4.11 `RUIN_OR_LANDMARK_ONLY` blueprint mode, with **no new settlement or structures**.
4. The 97 existing loose trait tags remain `LEGACY_UNTYPED_TRAIT`, not all 97 newly coded psychological modifiers. AV2–AV5 must decide which belong to dispositions, health, role, skill, learned trauma/experience, situational effects, or relational facts. The tags are never dropped or auto-applied.
5. Ten education identifiers remain `PROPOSED_EDUCATION_ID`, not verified completed educational paths. Misspellings `noble_courtley` and `noble_scholary` have corrected **proposed** canonical IDs `noble_courtly` and `noble_scholarly` with legacy read aliases; AV6 must reconcile these with actual education code/data.
6. Sixteen `PROFILE_FIELD` rows are composite descriptors, not sixteen new mutable fields. They must dispatch through already-authoritative subfields/managers; e.g. secrets, dialogue persona, legal state and interpersonal relationships have separate ownership.
7. Culture/religion custom keys and their dynamic `<culture_id>` suffixes are **namespaces** governed by `CultureService` and `ReligionService`; they do not create independent identity stores.
8. Each definition has `runtimeAssignable=false` in AV1. `typeResolvable=true` means only that its type is defined/recognized; it does **not** imply autonomous decisions, persistence or player-visible consequences.

## Runtime debug commands

- `/kwattributes status` — validates all 706 definitions against live Java enum constants and reference JSON catalogs; prints counts and activation boundary.
- `/kwattributes domain CHARACTER_VALUE` — reads the size of the domain covered in the user's source workbook.
- `/kwattributes row 93` — traces the original Excel spelling to canonical ID and existing owner.
- `/kwattributes resolve CHARACTER_VALUE EGALITARISM` — prints `EGALITARIANISM` with explicit `LEGACY ALIAS` notice.
- `/kwattributes resolve RELIGION great_shepard` — prints `great_shepherd` (read-only).
- `/kwattributes resolve HEALTH_STATE MINORLY_ILL` — canonical exact match.

The commands never assign traits, modify characters or invent work/economic effects.

## AV2 handoff (not implemented by AV1)

AV2 must reconcile legacy `NpcId`/`CharacterProfile` and the B4.0 compressed worldwide person IDs **without duplication**, and create sparse save-compatible per-person deltas with explicit ownership of mutations. It must preserve Winterfell authored characters; verify generation, inheritance and determinism; and reject unknown/renamed resource IDs gracefully. Profile mutation must only be admitted after the correct owner accepts it. AV4 adds decision hooks, AV5 social effects, AV6 child development, AV7 group identity, AV8 work, AV9 political institutions, AV10 grounded LLM context, AV11 world events, AV12 long-run validation.

## Acceptance gates

**AV1 complete only if:** the local check-only script passes, the guarded installer builds via Fabric Gradle, the above Minecraft commands validate all entries, old profiles/saves remain unchanged, all 762 original worksheet rows remain represented and no typo is emitted as a new canonical ID.

**Not an AV1 acceptance criterion:** 706 independent behavioral rules, live B4 profile mutation, offscreen millions-of-people psychology updates, economic productivity, political AI, canon-truthful dialogue, childhood inheritance or war strategy. Those remain explicitly planned.

**Open audit risks:** the B4.0 worldwide compressed population still uses provisional profiles rather than fully authoritative `NpcRegistry` profiles; `NpcDecisionService` mainly addresses needs, `NpcActionProcessor` productive WORK remains an empty economy effect, and B4.2B.4 economic transfers have no autonomous daily coordinator. The C4 204-job distribution and the 195 reference occupation catalog are separate models requiring an AV8/B4 economy crosswalk. No `knownworld` save must be modified during AV1.
