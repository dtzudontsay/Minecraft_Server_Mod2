# AV2B — Shared legacy / worldwide psychological tendencies (298 AC)

**Source review:** GitHub `dtzudontsay/Minecraft_Server_Mod2` at `964baba0e02bcd9cd9f688e701a7682c2a5ecab0`, plus AV2A local installation package. **Source install is guarded; no saved world is changed by the installer.**

## Authority and identity

- B4.0 remains the *only* canonical person/household starting snapshot for worldwide people, using `b4:site_id:suffix` string IDs. Its 3,760,684 people are not bulk-imported into `NpcRegistry`.
- Winterfell and previously authored `NpcId` characters remain in `NpcRegistry`, with their existing `CharacterProfileManager` and save authority.
- AV2A's sparse persisted numeric deltas are still the only dynamic B4 profile changes. AV2B derives tendencies *after* `KnownWorldAv2CharacterState.applyToSheet()`; the calculations create no new persistent stats.
- `CharacterPsychologyRules` now owns **exactly the existing twelve** `CharacterDerivedTendency` formulas. Legacy `CharacterPsychologyService.derivedTendencies` is delegated to it. Worldwide B4 via `KnownWorldAv2BPsychologyBridge` consumes the same evaluator, *without* creating synthetic `NpcId`, `NpcState`, `CharacterProfile`, inventories or memories.
- The B4.1A.1 generated personality representation stores **0 to 1**, whereas legacy `NpcPersonality` uses **-1 to 1**. Adapt by `2*x-1` at the B4 adapter. Do not modify legacy personality data to accommodate B4.

## Changes

1. Add `simulation/npc/psychology/CharacterPsychologyRules.java`: stateless, typed legacy/B4 input; twelve shared formulas; social susceptibility and persuasion resistance readouts. Required axes fail closed if missing or out of range. The evaluator is applicable to existing legacy profiles and AV2A-modified B4 numeric profile views.
2. Add `simulation/attributes/KnownWorldAv2BPsychologyBridge.java`: B4.0/AV2A JSON adapter; field normalization and full axis validation; `derivedTendencies` plus two resistance metrics on NPC inspection and K-chat profile. Block corrupt or invalid profiles; no fabricated default derived values.
3. Narrow changes to `CharacterPsychologyService.java` delegation, `KnownWorldB41Activation.java` registration/enrichment and `KnownWorldB41DialogueService.java` one LLM-context key. No changes to existing Java enum names or worldgen/population saves.
4. Add `/kwpsych status` informational command. No mutation command or automatic daily tick.

## Consistency with legacy implementation

The twelve named formulas retain the weights from legacy `CharacterPsychologyService.derivedTendencies`, including `socialSusceptibility` and `manipulationSusceptibility`. They return bounded 0–1 values. The original legacy calculation for `persuasionResistance` is also represented for the B4 adapter. The legacy service retains all its other stateful persuasion, conviction, influence and relationship operations; they are **not** redirected to B4 without a source-authority and event-validation bridge.

**Note:** AV2B's shared derived *readouts* are not yet independent decisions: `NpcDecisionService` remains limited; the `NpcActionProcessor` WORK branch has no universal real production. A high score for vengeance does not create a witnessed vengeance event or a reputation adjustment. K-chat text remains speech only, not proof of behavior.

## Validation

- AV1 lineage coverage: 706 definitions across 761 rows, all retained in `AV2B_ATTRIBUTE_COVERAGE.csv`; 55 headings excluded from numeric definitions.
- AV2A prerequisite: staged and applied `KnownWorldAv2CharacterState.java` and B4.1A.1 hooks must already exist.
- Twelve current enum constants matched and formulas found, no additional person manager or native economic grants.
- Run AV2B check-only; run install + `compileJava`, then later in a fresh/throwaway Minecraft world verify `/kwpsych status`, `/kwactors nearby`, right-click character values and K chat. Change an AV2A disposition with `/kwprofile set` and verify the derived readout updates after reopening the inspector; verify reload.
- Compare a legacy authored NPC's `derivedTendencies` scores before/after installation on a disposable world before calling parity independently verified. No live full-world validation has been done by this installer.

## Remaining AV program

- **AV2C:** bind observed, validated experiences and individual state changes to the same B4 identities; prevent AV2A admin-only deltas from being mistaken for organic psychology changes.
- **AV3:** culture/religion/region/parent/upbringing generation, with documented provenance and different laws/institutions. Must preserve B4 identity snapshots in existing saves.
- **AV4:** objective, executable NPC decisions with evidence and consequences. Integrate the twelve tendencies with needs/constraints and authorized world events, not an unbounded all-world tick loop.
- **AV5–AV12:** relationships, lifecycle, culture/faith/languages, economy, governance, safe knowledge propagation and world-scale stress tests.

Never treat inferred tendency as witnessed memory, actual social action, a legal office or real economic output.
