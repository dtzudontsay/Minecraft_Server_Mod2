# B4.1A – Non-destructive worldwide NPC physical projection

**Stage:** activation bridge, *not yet complete daily simulation, LLM conversations or event experience model*.

## Repository authority audit

The 298 AC B4.0 genesis output already has 3,760,684 stable person IDs, 799,343 logical household memberships and 1,880,341 modeled job assignments with culture/religion/language via B2/B3 regional profiles. It is *not* in `NpcRegistry`. Direct bulk import would collide with authored people and defeat spatial update budgets. `WinterfellV3Runtime` already owns Winterfell's 5,000 authored people and physical actors and must not be duplicated.

Existing `NpcRoutineService`, `NpcActionProcessor`, `NpcDecisionService`, `SkillPracticeService`, `NpcMemoryManager`, `NpcKnowledgeManager`, `SocialWitnessRuntime`, `KnownWorldPublicRumorRuntime`, world events, marriage, genealogy and lifecycle are valid future integration points. Generic `NpcActionProcessor.processWork` still has no economic side effects. `WinterfellConversationService` relies on live `NpcRegistry` `NpcId`s and `kwf3_actor` tags, so cannot be attached directly to B4.0 persons; doing so would silently bypass crucial knowledge/privacy guarantees.

## Runtime contract

- One canonical B4.0 identity record per individual. B4.1A projection candidates are read-only **views** (1-2 people/home), not a new population. All B4.0 identities remain intact.
- `b4_1a_projection_index.py` joins real resident homeLotId to **C4.11 tile source** available `HOUSE` lot IDs. It emits 194-ish site-specific gzipped indexes and counts them, with provenance to the original record. Do not fabricate village residents at an arbitrary map pin.
- The C4.11 runtime exports a **read-only terrain recheck / actual construction anchor** accessor. On a fresh session, it can recompute the deterministic approved placement even for chunks previously generated in a saved world. Do not re-run worldgen or `setBlock`; use the approved X/Z translation only.
- The B4.1A server-side spatial index is a 64-block-cell hash per loaded settlement. At most 2 site shards are resident in heap at once. Selection from existing B4.0 people near each player, globally bounded at 128 projected villagers. This is independent of Winterfell's existing V3 budget.
- Reuse `KnownWorldUiPackets.CharacterSheet`, preserving Winterfell's UI contract for the existing inspector. Only audited, actually stored fields are shown. Projections do not invent skills, social reputation, experiences or genealogy beyond B4.0 reservations.
- Minecraft villagers are not authoritative: they can be destroyed/rebuilt on world restart; no world identity is created or lost by that physical lifecycle. B4.1A actors carry `kwb41_actor`/`kwb41_id_` tags. Winterfell V3 uses different tags and retains its existing runtime.
- No offscreen simulation yet. K-key dialogue still routes to Winterfell's A1-A5 backend, which rejects B4.1A actors. Intentionally do not pretend that these actors know player conversations, rumors or current world events.

## Physical-reservation limitations

C4.11 has **site approval, but no confirmed per-lot success ledger** (lots can be suppressed by failed slope/entrance checks). B4.1A therefore projects people at verified safe loaded surfaces near *planned* home positions; not every projected house is guaranteed to exist yet. This must become a per-lot confirmation bridge before reliable home/work routines. Protected Ramsgate/Acorn Hall use the separate C2 scheme and need a C2-lot projection binding; Winterfell is already active.

## Test notes

Python 11 regression tests: descriptive IDs (`site_nw_missing_twins_01`), per-home cap, no cross-site pollution, duplicate prevention, C4.11 tile deduplication, authored Winterfell exclusion, synthetic 248-site manifest and actual gz resource emission, hook idempotence and UI title. Java 21 stub interface/syntax compilation passed. **Full Fabric Gradle build, actual world rendering, local-player latency and multi-player behavior remain unverified**. User should report first `compileJava` error and latest.log if any.

## Next integration

B4.1B: authenticated generic LLM UI for these projected persons; per-NPC/per-player dialogue provenance ledger, authoritative history/claims, event observer and rumor feeds. B4.2: offscreen daily trajectory, real workplace activity and gradual skills; world-save overlays for mutable character state. B4.3: travel itinerary clocks, encounter observation and propagated world changes. B4.0 ids never reallocated.
