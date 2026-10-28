# 24.21 — B4.1B: Worldwide dialogue bridge on one population

## Authority map

- **B4.0** authoritative starting people, household ID, occupation assignment, culture/religion/language, stable personal ID.
- **C4.11 / C4.15** placement/architecture/commute planning. Buildings/workplace reservations do not imply completion or recorded labor.
- **B4.1A.1** materialization and deterministic provisional trait/skill profiles, with a global limit of 128 nearby physical worldwide NPCs.
- **B4.1B** conversation presentation and private per-(NPC,player) ledger. Does not create `NpcState`, economic events, observations, or beliefs.
- **Winterfell V3/A1–A5** retains pre-existing authored `NpcState`, genealogy, profile, witnessed-public-information model, its own LLM and social actions.

## Event and packet path

Existing `WinterfellClientUi` still sends K-key `OpenRequest`, `ChatRequest`, `CloseRequest` over the same packet types. The server's common packet receiver recognizes a validated `kwb41_actor` tag and branches to `KnownWorldB41DialogueService`. Otherwise it takes the pre-existing Winterfell route.

The generic service asks the physical projector for a guarded immutable person snapshot; the projector checks the actual UUID of the current person projection, not merely a fake scoreboard tag. Both services reuse packet records and the existing UI. Actor movement locks apply to either actor type and release on closing, range loss, or player disconnect.

## Personal context and anti-hallucination policy

The global prompt contains only B4 facts: name, culture, faith, region, language, job, logical household/home/work reservations; a seed-derived personality and regional values; current calendar and *only if live title data exists*, royal title. It is explicit that these do not establish eyewitness experiences or the spread of news.

Conversation provenance is from `WinterfellConversationLedger` (per individual and player). `WinterfellDialogueGuard` blocks a small high-risk historical contradiction set; `KnownWorldB41DialogueTruth` rejects some invented firsthand player witnessing or prior meetings. Filtered responses are replaced with a noncommittal in-character correction. This is a selective guard, **not** perfect factual validation. A3 raw dialogue is not upgraded to NpcKnowledgeManager, NpcMemoryManager, or WorldEventManager truth.

The server does not call Ollama for every resident. It only makes a request when a nearby player actively sends a message, and limits simultaneous worldwide requests. The only persistent files created are for NPCs actually spoken to or socially interacted with.

## Lifecycle safety

- Stable person ID and player UUID form the persisted ledger and relationship key.
- Server thread owns session and file state; HTTP callbacks schedule delivery onto the server thread.
- The client session token, live actor identity, proximity and server-world identity are rechecked before accepting completed generation.
- When player closes, disconnects, changes person, or leaves proximity, late responses are discarded and do not produce phantom history.
- Ledger writes use existing atomic writes, bounded recent history and corruption checks.

## Future upgrades

**B4.2** must convert/attach provisional B4 attributes to authoritative, changeable profiles and daily routines using persisted identity reconciliation, simulated work schedules and employer-workplace verification. **B4.3** must supply per-person knowledge provenance from observed events, social encounters, travel, news, rulers/succession changes and family deaths. Only then may such facts be disclosed by a global NPC on a per-person basis. Avoid bulk-loading 3.76M `NpcState` objects into the active registry.
