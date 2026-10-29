# AV2A — Persistent sparse numeric profiles for worldwide B4 identities

**Baseline:** GitHub `master` at `28a2e02d` + installed AV1 v1.1. **Scenario:** 298 AC. **Release gate:** source/check-only + `gradlew compileJava`; real Minecraft validation deferred to AV3/AV4 grouped test. **Authority:** B4.0 person IDs, no additional people, households, jobs or NPC registries.

## Why this is AV2A rather than a claim of fully completed AV2

Legacy `CharacterProfile`/`CharacterProfileManager` still hold authored `NpcId` state. The B4.1A.1 projection rebuilds deterministic profiles from B4.0 IDs, occupation and regional values. These are different identity domains. AV2A stores a sparse, versioned **per B4.0 person** overlay and connects it to both the right-click character sheet and K-key dialogue context. It **does not** convert B4 IDs to `NpcId` integers, duplicate the census, or claim that `SkillPracticeService` or `CharacterPsychologyService` are already invoked for worldwide people.

## Five writable AV1 domains (117 source entries)

- `CHARACTER_SKILL` — 20 dimensions, values in `[0,1]`, `skills` projection field.
- `NATURAL_APTITUDE` — 20 dimensions, values in `[0,1]`, `aptitudes` field. The canonical `LANGUAGES` dimension maps to the **existing** B4 projection key `language`. No extra aptitude is created.
- `DISPOSITION` — 26 dimensions, values in `[-1,1]`, `dispositions` field.
- `CHARACTER_VALUE` — 33 dimensions, values in `[-1,1]`, `characterValues` field.
- `SOCIAL_NORM` — 25 dimensions, values in `[-1,1]`, `socialNorms` field.

Everything else in AV1 remains preserved and tracked: 761 source rows, 706 definitions, 30 domains, including 97 untyped traits, cultures, religions, languages, education, legal, institutions, offices, building classifications and regional definitions. AV2A does **not** silently treat them as writable numeric stats. `PERSONALITY_SCALE` still needs a deliberate contract because the legacy temperament uses `[-1,1]` while the current B4.1A.1 projected baseline uses `[0,1]`.

## Persistence and safety

- Save path is **world save** `<save>/knownworld/npc/av2_character_deltas_v1/<siteId>.json`. It is never a resource pack or source-controlled scenario file.
- Each site file contains schema `knownworld.av2a.person_delta.v1`, a site ID, sparse changes by **canonical B4.0 ID + domain + AV1 ID**, provenance, campaign day, and SHA-256 checksum.
- Purely observational actions create no saves or new state; an OP-only debug command can set/clear a value **only for an existing, currently materialized verified B4 person**.
- A successful debug mutation immediately writes its site shard using exclusive `.pending` creation, `FileChannel.force(true)`, and atomic replacement of the published `.json` file. If interrupted, the pending file prevents further writes and requires independent investigation. Do not blindly delete a pending file.
- Six cached site shards maximum; 12,000 edited people per site and 8 MB per file. A complete global B4 roster is never loaded into memory; there is no per-tick profile iteration.
- Invalid, oversize or mismatched files refuse application. Profiles do not silently revert to a falsely claimed authoritative new value. Visual inspection reports `BLOCKED_CORRUPT_OR_PENDING_SAVE` when appropriate; commands fail closed.
- `NATURAL_APTITUDE` and other numeric values are intentionally edited only via a server OP test command. There is **no automatic** practice, inheritance, conversion or personality drift yet.
- This batch does not change Winterfell V3, old authored `NpcState`, inventories, towns, genealogies, occupations or existing save formats.

## Runtime connection

One minimal guarded change to `KnownWorldB41Activation`: import, `KnownWorldAv2CharacterState.register()` with the B4 registration, apply the sparse delta **at the end of `enrich`** (therefore both right-click and the K-chat `DialogueIdentity` inherit it), plus a read-only `av2VisibleSite` lookup that checks materialized entity UUID and actor tag. The new class owns all persistence and commands. No changes to `KnownWorld.java`, LLM networking or the original `CharacterProfile` fields.

## Commands and acceptance

- `/kwprofile status` — schema ready and retained AV1 definitions, no implicit writes.
- `/kwactors nearby` — find nearby B4 actors (for IDs, inspect/right-click one actor; the existing nearby command currently prints names/roles, not IDs).
- `/kwprofile inspect "b4:site_...:..."` — view saved changes for a **visible** B4 person; quoted because the person ID contains colon characters.
- `/kwprofile set "b4:site_...:..." CHARACTER_VALUE DUTY 0.55` — OP-only, explicit recorded change. Does not claim that it has driven any decision.
- `/kwprofile clear "b4:site_...:..." CHARACTER_VALUE DUTY` — OP-only, returns to deterministic baseline without reassigning the person.

Tests: create disposable world; find one projected B4 actor; read baseline; explicitly set one attribute; compare right-click sheet/K-key context; save and restart; inspect same stable ID; confirm only that value persisted; clear and verify baseline restoration. Check an unrelated NPC, Winterfell, different player and economy inventories remain unchanged. **Do not test admin mutations on valued saves without a backup.**

## Follow-up AV2B / AV3 / AV4

AV2B must expose a canonical immutable profile view to legacy psychology consumers without fabricating `NpcId`s. It must also resolve authored-character aliases, profile ownership, semantic scale conversion and per-person provenance; reconcile state across birth/death, migration and changed save schema. AV3 uses catalog/region/parents/upbringing to distribute starting state; AV4 consumes profiles to execute actual decisions, not just update LLM text. AV5+ handles real relationships, AV6 childhood, AV7 culture/religion. B4.2B economy continues separately.
