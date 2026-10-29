# AV2C — Verified Personal Experience & Source Provenance (first evidence type)

**Audited baseline:** `dtzudontsay/Minecraft_Server_Mod2`, master `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6` (October 29, 2026), **plus** the AV2B installer stage (not required to be pushed before local installation). AV1, AV2A, AV2B must all be installed and compiled before installing AV2C. GitHub is read-only for this batch.

## Why this stage exists

- B4.0 owns the 298 AC starting population: 3,760,684 **fixed identity records**, not 3.76m Minecraft entities or live `NpcState`s.
- AV2A saves sparse administrator-approved numerical profile overrides, currently with provenance `ADMIN_VERIFIED_PROJECTION`. This is **not proof of earned experience**, and AV2C does not alter its schema.
- AV2B computes twelve derived psychology tendencies for B4 profiles using the existing legacy formulas. Those derived readouts are neither distinct persisted attributes nor proof of behavior.
- B4.2B `KnownWorldB42BPersonalEvents.observedAtWork` already accepts **physical presence during a scheduled work shift** (once per person/campaign day). It explicitly does not assert production, income, training, coworkers, or personal memories beyond that presence.

AV2C introduces a **first strict fact/provenance bridge** for this existing observation, while avoiding another character registry, mutable value store, economic authority, or hallucinated event.

## Files and source hooks

1. New `simulation/attributes/KnownWorldAv2CEventPolicy.java`: validates canonical B4 person/site IDs and same-site workplace, limits event kind to `WORKPLACE_PRESENCE`, source to `B4_2B_PHYSICAL_WORK_ARRIVAL`, day >= 0; produces deterministic SHA-256 event identity.
2. New `simulation/attributes/KnownWorldAv2CExperienceJournal.java`: per-person, checksummed, write-through JSON evidence mirror with exclusive `.pending` files and atomic replacement. It stores the most recent 32 daily observations, a lifetime count and a rolling hash chain of receipt IDs; no per-tick person scan.
3. `KnownWorldB42BPersonalEvents.java`: after **already accepting** a new real work-presence record, notify AV2C. Never call this from an abstract schedule, an LLM statement, modeled offscreen arrival or a general command.
4. `KnownWorldB41Activation.java`: register `/kwexperience` and enrich the *same* existing right-click inspection sheet. No extra entity or new census head.
5. `KnownWorldB41DialogueService.java`: supply a narrow verified-presence statement to K-key LLM context. It explicitly refuses claims about goods, wages, coworkers, witnessed crimes or actual skill improvement.
6. Carry forward *all* 761 source-lineage rows and all 706 AV1 vocabulary definitions in the new `AV2C_ATTRIBUTE_COVERAGE.csv`, noting that no attribute is automatically mutated by this batch.

## Authority and safety

- **Do not** import B4 IDs into `NpcRegistry`; no second live-person universe.
- **Do not** auto-raise `AGRICULTURE`, `TRADE`, `CRAFTING`, etc. A workplace arrival is not successful practice. Later AV3/AV4/AV8 will require an approved activity outcome and bounded skill-practice rules.
- **Do not** modify AV2A admin edit provenance or existing characters' saved profile values. Existing AV2A edits are not retrospectively relabeled as earned experience.
- **Do not** write wages, production, household assets, relationship values, reputation, NPC knowledge/rumors or historical chronicles.
- Each AV2C person evidence file is independently persisted. The older B4 attendance journal and the AV2C mirror are **not jointly transactional**. A crash may leave one ahead of the other. AV2C must be treated as *additional conservative evidence*, not a new canonical day-count authority or an economic trigger. Cross-domain reconciliation is a separate future task.
- A checksum detects accidental corruption, not malicious modification. The rolling hash chain is a compact continuity indicator, not cryptographic attestation from an external trusted witness. Pruned events are **not individually recoverable**.
- If a file is corrupt, oversized, linked or has an interrupted `.pending`, AV2C refuses to publish its history; the existing B4 subsystem is unaffected. Never delete `.pending` in a valued save merely to resume simulation without inspection.
- AV2C is bounded by 32 recent entries/person, 24 KB maximum per file and 64 cached people. No millions-of-people startup preload or new per-tick loop.

## Acceptance and validation

- Run `RUN_AV2C_CHECK_ONLY.bat` first; must not edit user source or saves.
- Run `RUN_AV2C_INSTALL_AND_TEST.bat`; must run full `gradlew compileJava` and restore changed source if it fails. No GitHub push or world editing.
- On a **disposable** Minecraft save: `/kwexperience status` and `/kwexperience person "b4:site_...:..."` (use an actual B4 ID), then observe a physically projected worker reach their assigned work shift and inspect again. Verify exactly one accepted source observation/day, stable count after save/restart, appearance in right-click sheet and K dialogue; verify absence of fictional production and earned XP.
- Tamper a **copy of the disposable save**'s AV2C JSON or leave `.pending`: AV2C must report unavailable evidence and must not publish an unverified value.
- Existing 3,760,684 residents and AV1 definitions are immutable. No automatic mutations to 117 AV2A numeric dimensions.

## AV next steps

- **AV2D / AV3:** validated change-intent and source attribution for future experience-driven updates; verify a single canonical save authority before permitting automatic skill gain. Regional, family, cultural and religious profile generation must use the AV1 typed vocabulary.
- **AV4:** actual autonomous choices using AV2B derived psychology, bounded events and explicit outcomes; no fabricated witnesses or all-world tick.
- **AV5–AV12:** meaningful relationship, upbringing, cultural, economic, political and knowledge effects. Each Excel value must have a typed owner and a tested consumer or explicit non-applicability.

**Status:** AV2C implements *a first narrow real experience source*, not completion of the full human experiential or behavioral simulation. Do not call AV2 fully complete until multiple event types, source provenance, cross-domain recovery and profile-state integration are tested.
