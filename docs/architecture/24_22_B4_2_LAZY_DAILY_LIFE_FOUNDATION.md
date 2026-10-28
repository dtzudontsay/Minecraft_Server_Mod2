# Known World B4.2 — Lazy Daily Life and Employment

## Source-of-truth boundaries

- B4.0 identity/occupation/household catalog is immutable; no duplicated people, altered jobs, regenerated names or family links.
- C4.15 optional commute overlay supplies preferred `workplaceId`; no assertion of navigable path or constructed/active workplace.
- B4.1A.1 bounded 128-villager physical projections still own entity materialization and original home lots.
- B4.1B per-NPC/player conversation ledger still owns spoken history and relationship changes; it is never treated as a world-event journal.
- Winterfell V3 remains independently authored and controlled. Ramsgate and Acorn Hall C2 structures are not overwritten.
- `NpcSimulation.campaignCalendar().absoluteDay()` and overworld day phase are the ONLY simulation time authority. Minecraft server restart doesn't gain time.

## Batch B4.2 core algorithm

For each B4.0 person, the active daily routine is a deterministic function of stable ID, occupation, birth year, campaign day and day phase, whether a **planned** matching workplace is available, and estimated commute distance. Age <16 cannot be assigned work by the runtime model. Work hours differ by broad occupational category. One rest day per seven is staggered across stable IDs. An unavailable workplace creates an explicit `UNVERIFIED_WORKPLACE_OR_HOME` state, **not** simulated productivity.

The campaign-day store checkpoints only global boundaries (one O(1) save per day), with idempotent restart semantics. Per-person practice is derived from the number of modeled workdays since first B4.2 observation and capped at 0.18, **not** written as verified real-world work. The open-ended growth model can be replaced by authoritative `SkillPracticeService` events when work execution is real. The ordinary personality/region values remain the B4.1A.1 deterministic baseline, not duplicated here.

## Scalability

No daily O(N) loop over all 3,760,684 people. Runtime requests one site shard at a time; workplace caches retain at most 6 sites. The compiled resource index enumerates existing C4.11 work lots but does not embed every person's full identity again. The persistent daily checkpoint is a small JSON file per save. Existing B4.1A.1 512-block person tiles continue to determine which nearby actors can be materialized.

Targeted 128-actor navigation operates only in already loaded chunks and only within 48 blocks; it checks safe ground near the street entrance and honors the conversation lock. It cannot magically move NPCs across an unloaded town or validate an actual street network. B4.3 should add road graph waypoints and timed travel segments with interceptable position projection.

## Future authority bridges

1. Verify `workplaceId` against live C4.11 placement completion and actual navigable street entrances; audit missing buildings and commuting exceptions.
2. Feed genuinely completed work intervals, labor skill and local resource access into the authoritative economy journal, not directly invent coins/goods.
3. Commit sparse durable life events only where simulation rules actually resolve something (injury, marriage, wage payment, witnessed action, religious conversion).
4. Reconcile generated worldwide identities with authored characters and B4.0 cohorts before linking lifecycle births/deaths so nobody is counted twice.
5. Persist changed skills, needs and per-person event cursors as sparse deltas with an explicit schema/upgrade path.

## Acceptance boundaries

Pass B4.2 if it compiles, does not change B4.0/C4.15/B4.1B world counts, has a restart-safe campaign day checkpoint, returns consistent work/home/rest activity for nearby worldwide NPCs, exposes the limited work-site authority clearly, and leaves Winterfell/LLM memories unaffected. Do **not** interpret success as proof that millions of physical NPCs walked to work or automatically created resources.
