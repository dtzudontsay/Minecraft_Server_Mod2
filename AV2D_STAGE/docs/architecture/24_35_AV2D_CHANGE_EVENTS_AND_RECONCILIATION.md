# AV2D — Verified Attribute-Change Contract & Source Reconciliation

**Audited source:** GitHub `dtzudontsay/Minecraft_Server_Mod2` at `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6`, plus AV2B v1.1 and AV2C local installer stages. The current GitHub master includes AV2A but not necessarily AV2B/AV2C; **install and compile AV2B and AV2C before AV2D**. This bundle includes no upstream GitHub writes and alters no saved Minecraft world at install time.

## Purpose

AV1 canonicalized 706 definitions across 761 Excel lineage rows; AV2A introduced write-through *admin-edited* sparse B4 character values; AV2B shared twelve **derived**, nonpersisted psychological formulas with legacy NPCs; AV2C recorded **physically observed workplace presence only**. AV2D defines the missing contract for confirmed *outcome* changes without equating presence, schedules or dialogue with an achievement. AV2D is a small, isolated state owner for **earned/confirmed event deltas**. No AV2D write is currently called by active gameplay code.

### Implemented contract and actual effect

1. `KnownWorldAv2DChangePolicy` defines a canonical B4 person/site ID, exact `CHARACTER_SKILL`, `NATURAL_APTITUDE`, `DISPOSITION`, `CHARACTER_VALUE`, `SOCIAL_NORM` domains, numeric change magnitude of at most 0.025 per authenticated event/axis, deterministic SHA-256 idempotency key, and one of three future completed-event authorities (`VERIFIED_COMPLETED_PRACTICE`, `VERIFIED_FORMATION_OUTCOME`, `VERIFIED_SOCIAL_OUTCOME`). All other event sources, especially `WORKPLACE_PRESENCE`, are rejected.
2. `KnownWorldAv2DChangeJournal` is a **single atomic per-person record for approved AV2D event changes**. Every receipt carries its outcome authority, external source event ID, campaign day, canonical domain/axis and signed delta. It refuses duplicate or conflicting receipt IDs and does **not compact receipts**. At 512 lifetime events per person it stops accepting changes until a deliberate migration. Up to 290 KB per person; LRU caches at most 48 records. Starting census and physical projection budgets are unchanged.
3. An *internal, package-scope* API `acceptVerifiedOutcome(server, intent, verifier)` exists for **future trusted and actual gameplay event producers**. A verifier must inspect an authoritative completed-event record for the exact subject, day, event ID, and outcome. A fabricated/constant-true verifier would violate this contract. **No current system calls this API**; no ordinary work attendance, dialogue, abstract action, administrator command or scheduled work can submit a new receipt. Source event references are not a cryptographic witness or remotely secure signature.
4. Profile read order is explicitly **immutable B4.0 baseline → AV2A manual overrides → AV2D confirmed-event deltas → AV2B psychology derived tendencies**. Only these five numeric maps can be altered by AV2D; it does not edit `NpcRegistry`, B4 census, offices, cultures, religions, economy, relationships, or a legacy profile. The original AV2A site-shard save schema and its `ADMIN_VERIFIED_PROJECTION` provenance remain unchanged. Changes from any genuine future outcome would appear in right-click and K-dialogue's common profile and influence the existing AV2B derived readout.
5. `kwchanges reconcile <id>` performs a **read-only advisory** comparison between the old B4 attendance count and the separately stored AV2C presence mirror. The result distinguishes equal counts, B4 ahead, AV2C ahead, absent old record and corrupt/unavailable mirror. It **does not merge, repair, backfill, delete pending files or certify atomic agreement**. Equal counts are not cross-domain atomic proof. No source attendance record becomes an attribute-change receipt.
6. Diagnostic commands: `/kwchanges status`, `/kwchanges person "b4:site_ne_001:..."`, `/kwchanges reconcile "b4:site_ne_001:..."`.

## Safety, durability and trust boundaries

- AV2D stores one validated per-person JSON blob in `world/knownworld/npc/av2d_confirmed_changes_v1/<siteId>/<sha256(personId)>.json`, guarded by SHA-256 checksum, `CREATE_NEW` `.pending`, file `force(true)`, and `ATOMIC_MOVE`. A pending file, symlink, bad record, duplicate or unrecognized canonical value blocks the record instead of silently generating new stats. Its receipt log stays intact after restart, so compacted-id replay is impossible **within this version's capacity**.
- AV2D's checksum protects against accidental corruption, **not against an attacker who controls the filesystem**. The source verifier is a Java trust boundary, not a network signature or authorization token. The absence of actual completed-event producers means no AV2D write is currently authorized by gameplay. Java smoke checks exercise policy and API compilation, not actual Fabric/MC runtime or crash-consistency with AV2A/AV2C.
- AV2D's per-person journal write is internally atomic, but **the upstream event-producing domain and AV2D are not one joint transaction**. Future producer integration must settle commit ordering, replay after crash and canonical evidence lookup. Do not claim multi-domain crash recovery is solved by this batch.
- Current AV2A manual overrides and AV2D event delta records are **separate owner domains and files**, not two competing sets of baseline NPC identities. Read layering has a defined order. A manual override changes AV2D's effective starting point without rewriting receipts; no retroactive earnings are invented.
- Replay of the same outcome/axis is rejected even when another delta is submitted. Different domains/axes for one actual event are separate receipts. Old accepted receipts are never dropped to preserve idempotency. Future growth requires a versioned rework, not deleting event history.
- `kwchanges reconcile` is an operator's diagnostic, not an automatic repair or legal/financial settlement. A missing B4 record is NOT proof that AV2C's record is fraudulent, and equal counts are not a proof of perfect source alignment.

## Source changes

- Add `simulation/attributes/KnownWorldAv2DChangePolicy.java`.
- Add `simulation/attributes/KnownWorldAv2DChangeJournal.java`.
- Patch only `KnownWorldB41Activation.java`: register AV2D beside AV2C and apply confirmed event deltas **after** AV2A and **before** AV2B.
- Add installer with backups, check-only, idempotency, full Gradle `compileJava` and rollback.
- Add Java 21 compile/policy test and exact 761-row coverage checker.
- Preserve all AV1/AV2A/B/C sources, all preexisting save formats and population identities.

## Acceptance and next implementation

1. Run `RUN_AV2D_CHECK_ONLY.bat` from the repo root **after** AV2C installed. This should check all local source prerequisites without editing files.
2. Run `RUN_AV2D_INSTALL_AND_TEST.bat`; its real Fabric compile must pass. It does not run Minecraft or make commits.
3. Optional *disposable-world* test: `/kwchanges status`, `/kwchanges person "<real B4 ID>"`, `/kwchanges reconcile "<real B4 ID>"`; confirm no automatic skills/wages have been credited. Observe if AV2C and B4 counts disagree, but do not fix files manually without reviewing evidence.
4. **AV3:** world/region/household/culture/faith generation rules for the 117 numeric dimensions and the rest of the 706 definitions. **AV4:** introduce validated active action outcomes and an audited gameplay verifier as the first real AV2D producers. **AV6/AV8:** trusted education and completed-work sources can submit their own structured outcomes; never mere work presence.

**Completion statement:** AV2D installs executable, tested event-policy, durable receipt handling and read-only cross-domain comparison, but **does not activate any autonomous attribute XP/reward**. Full two-domain transactionality, actual verifier adapters, active behavior and performance tests are still outstanding.
