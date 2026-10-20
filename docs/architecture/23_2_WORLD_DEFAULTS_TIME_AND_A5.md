
# Known World — Roadmap Addendum 23.2

Status: Active
Date: 2026-10-20
Scope: A5, B0, B1, generation and projection acceptance gates

Companion:
- 23_0_CONNECTED_SIMULATION_ROADMAP.md
- 23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md

This addendum makes previously implicit development requirements
explicit. It does not replace the main implementation roadmap.

## A5 — Player/NPC relationships

Status: implementation files supplied; local validation pending.

Existing:
- WinterfellConversationService
- WinterfellConversationLedger
- WinterfellConversationPackets
- NPC character profiles and public knowledge context

New:
- WinterfellPlayerRelationshipStore
- Player relationship persistence tests

Immediate behavior:
- Completed conversations gradually increase familiarity.
- Explicit praise improves respect and warmth.
- Insults lower affection, trust and respect.
- Threats increase fear and lower trust.
- Actions require an active nearby NPC dialogue.
- Action cooldown prevents unbounded immediate repetition.
- State persists by stable authored character ID and player UUID.
- The next LLM response receives the individual relationship state.

Commands typed inside the K-key dialogue:
- !relations
- !praise
- !insult
- !threaten

Deferred A5 expansion:
- Automatic evaluation of natural-language social intentions.
- Conversation promises and obligations.
- Giving actual goods or money.
- Reputation effects observed by third parties.
- Relationship changes based on completed quests and real deeds.
- Player/NPC friendship, rivalry, employment and loyalty bonds.
- NPC-controlled initiation of player interactions.
- Integration with law, politics and family decisions.

Critical:
Player/NPC relationships are distinct from NPC/NPC relationships.
Do not interpret freeform player text as a validated factual event.
Do not permit LLM-generated answers to mutate domain state.

## B0 — Known World creation defaults

Status: planned, immediately before B1.

Requirements:
1. Detect the selected Known World dimension/generator reliably.
2. On NEW Known World saves, set Peaceful difficulty by default.
3. Disable natural hostile mob spawning by default.
4. Keep farm animals, villagers and custom NPC projections enabled.
5. Do not silently change existing worlds' user settings.
6. Provide a config option to restore hostile mobs/difficulty.
7. Verify singleplayer and dedicated-server behavior.
8. Never confuse peaceful difficulty with absence of simulation war.

Acceptance:
- Fresh Known World starts peaceful without player commands.
- Vanilla worlds using the same mod retain their normal defaults.
- Winterfell projection and animals still spawn.
- Existing saves only change when explicitly opted in.

## B1 — One Minecraft day equals one campaign day

Status: accepted design target; migration not implemented.

Authoritative time:
- One Minecraft day = 24,000 overworld daylight ticks.
- One campaign year = 365 campaign days.
- Starting scenario remains in year 298 AC.
- Calendar time must not advance twice for the same day boundary.

Consequences:
- Daily needs/routines advance over one Minecraft day.
- Agriculture and household consumption process once per day.
- Pregnancy, disease, mortality and fertility use campaign days.
- NPC birthday/year transition occurs after 365 such days.
- The world time and historical simulation calendar stay consistent.

Important edge cases:
- Sleeping through night.
- /time commands and backward jumps.
- doDaylightCycle false.
- Save/reload midway through a day.
- Admin fast-forward and test harness.
- Paused singleplayer versus running dedicated servers.
- Lag and low TPS.
- Existing campaign saves configured for 1,200 ticks/day.

Required implementation:
1. Version the calendar-time authority.
2. Explicitly synchronize calendar to overworld day progression.
3. Define backward-time and forward-time jump policy.
4. Run catch-up through one ordered daily event scheduler.
5. Reuse the same scheduler for normal and fast-forward execution.
6. Perform safe migrations for existing saves.
7. Add reproducibility and save/reload tests.

Do not change only the DEFAULT_TICKS_PER_CAMPAIGN_DAY
constant and assume that all subsystems are synchronized.

## B2-B4 — Identity and population completion

Additional explicit requirement:
- All simulated residents possess individual identities.
- Aggregation is an optimization, not a second invisible population.
- Every generated NPC has exactly one regional residence.
- Transient travelers and temporary garrisons are not duplicated.
- Existing population-cohort saves require migration tests.
- No high-frequency Java object or physical entity is necessary
  for every person in the wider Known World.

## C0 — Existing physical-world bug gates

Before mass settlement generation, fix and test:

- Atlas teleport always searches for a safe dry ground landing.
- Never place a player in water when teleporting to valid land.
- Castle and household doorways must be actually accessible.
- Ladders must be supported and climbable.
- Lanterns must attach to valid supports.
- Lava, vegetation and structures must not intersect.
- Settlement terrain must accommodate authored mountains.
- Steep terrain requires safe graded construction or rejection.
- First-load NPC projection must reliably appear.
- NPCs must retain their stable simulation identity on reload.
- NPC conversation locks must release on close or disconnect.
- Existing generated chunks need explicit migration/version handling.

## C2-C4 — Physical settlement detail gates

- MAJOR/MEDIUM/SMALL settlement-size classification.
- All 259 holding records reviewed, including non-settlement
  domains which must not be incorrectly assigned buildings.
- Density and placement rules allow compact town cores.
- Regional construction materials and settlement palettes.
- Family/business signage based on current occupancy.
- Bakery, forge, inn, market, armory, worship, stables,
  military training, farms, housing and storage.
- Local employment counts drive workplace construction.
- Buildings must have valid entries, circulation and capacity.

## D-I — System interconnection checks

- Jobs drive real production and income.
- Needs and inventories drive actual personal decisions.
- Family events update households, law and succession.
- Player deeds influence relationships through validated events.
- Trade requires actual merchants, routes and resource transfers.
- Remote events move through messengers or communications.
- Death, marriage, war and rulership changes update dialogue context.
- Factions can recruit, establish camps and claim territory.
- Camps and forts require labor, equipment and resources.
- Quests originate from real needs and provide real rewards.
- Simulation works while chunks are unloaded.
- No system silently double-processes a daily event.

## Remaining architecture decisions

- Whether to permit hostile fantasy creatures in special regions
  independently of vanilla difficulty.
- Which circumstances allow NPCs to disclose secrets.
- How fast-forward should handle skipped physical time.
- How many fully detailed active individuals fit the CPU budget.
- Which population data need a one-time migration.

## Release discipline

A batch is complete only after:
- Build passes.
- Automated tests pass.
- Live in-game behavior is verified.
- Save/reload behavior is verified.
- No identity or resource duplication occurs.
- Performance remains within a measured budget.
