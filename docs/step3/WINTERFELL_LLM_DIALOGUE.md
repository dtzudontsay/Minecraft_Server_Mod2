# Winterfell V3 — On-demand LLM Conversations (Prototype)

## Status

Feature branch: feature/winterfell-llm-dialogue. The integration has not been
compiled or tested in a Minecraft 26.3 client. Review and test before merging.

The right-click Winterfell six-tab character sheet and GM tab are unchanged.
The K-key conversation uses separate packets and a separate screen.

All physically projected Winterfell V3 actors bearing the server-side
kwf3_actor and kwf3_id_* tags can be spoken to. The simulation currently
contains 5,000 persistent resident records. Existing V3 projection budgets
limit the number of visible/clickable actors. Unrendered NPCs do not require
any active LLM process.

## Operation

1. Look directly at a Winterfell villager from within 6 blocks and press K.
2. The portrait panel renders the nearby villager's live 3D model (when client-side entity data is available). Type your words and press Enter or click Say. Mouse-wheel scrolls history.
3. Esc, K or X closes the dialogue. Right-click still opens the profile screen.
4. M still opens the Atlas, independently of conversation.

With no API key, the screen opens and explains what needs configuring,
without generating API charges.

## Server-only API configuration

Set the environment variable KNOWNWORLD_OPENAI_API_KEY on the **server**.
OPENAI_API_KEY is supported as a fallback. Set KNOWNWORLD_LLM_MODEL to override
the default gpt-4.1-mini model. Endpoint is the OpenAI Responses API, using
HTTPS. OpenAI API billing is separate from a ChatGPT subscription.

Example: In the PowerShell terminal that will launch the client/server, run:

    $env:KNOWNWORLD_OPENAI_API_KEY="your-key-here"
    $env:KNOWNWORLD_LLM_MODEL="gpt-4.1-mini"
    .\gradlew.bat runClient

For a dedicated server, configure variables in its startup environment
instead. Never commit an API key into GitHub or embed it in a distributed JAR.

API requests specify store:false. Conversations still send the player's
dialogue and NPC character context to the chosen external model; users of
the server should be informed. This prototype does not persist entire
chat transcripts or have a text-moderation/action-approval pipeline.

## Persona and memory

An individual request uses the selected NPC's live simulation identity
and CharacterProfile. It supplies name, role, household, age, culture,
religion, traits, six stable personality dimensions, DialoguePersona
speech guidance, strongest skills, values, social norms, politics, goals,
fears, selected public facts and six salient subjective memories.
**Secrets, known secrets, and private facts are not sent to the model** until
an explicit server-side trust/knowledge disclosure system exists. It also sends
at most 12 rolling player/assistant dialogue
messages. The selected actor's own personality affects interpretation and
speech; no template is shared between different characters.

World events and private facts must be decided by the simulation, not
fabricated by the language model. The instructions forbid treating speech
as a world-changing action, claiming knowledge of future book events, or
claiming the NPC has disclosed private character secrets. These constraints
guide a stochastic model but are NOT deterministic permission enforcement.
Do not add gameplay tool execution until explicit server-side authorization
and simulation state mutation exist.

The model knows the character's recorded culture and religion. Rich
doctrines, cultural traditions and historical world lore are future
retrieval layers; the current code does not imply that all teachings
of a faith or culture are already loaded.

## Guardrails and limits

- The server resolves the targeted entity and stable NpcId; never trust the
  client-provided target ID as an identity.
- Server checks that the projection exists and the player is within 6 blocks
  both on opening and each chat submission.
- Random sessions are tied to one player, entity UUID and NPC identity.
- Messages are limited to 400 characters; replies to 1700 characters.
- One request per conversation in flight, 1.2 s minimum message spacing,
  max 8 requests globally in flight, 40 s provider timeout.
- Network I/O is asynchronous to avoid blocking server simulation ticks.
- Sessions clear on disconnect/server stop and idle sessions are pruned.
- No API request is made during terrain generation, NPC simulation ticks,
  or simply because an NPC becomes physically visible.
- Provider errors show short user-facing statuses without API-key exposure.
- Set API-side budgets/rate limits before inviting more players.

## Manual acceptance checklist

- [ ] Build mod with Fabric 26.3; launch integrated and dedicated servers.
- [ ] Right-click Eddard Stark, a guard and a commoner: inspection/GM intact.
- [ ] Press K on each: different dialogue screen with live 3D NPC preview and matching persona.
- [ ] Press K on an ordinary non-Winterfell mob: no conversation.
- [ ] Open dialogue, then walk away: further messages rejected by server.
- [ ] Unset API key: clear status and no provider calls.
- [ ] Set valid API key: messages produce readable, different in-character replies.
- [ ] Two players talking to different NPCs have isolated conversations.
- [ ] Close, reconnect and restart world: no NPC identity/data corruption.
- [ ] Disable Atlas M independently and confirm dialogue K still registers.

## Global rollout

The packet/provider/session/prompt layers can be reused anywhere. Future
NpcDialogueService should resolve every physical world NPC to its stable NpcId
without hardcoding Winterfell tags. Future additions: religion doctrine and
culture lore retrieval, player-specific relationship states, faction loyalties,
persistent conversation summary memory, local news, and a validated action
channel for genuine effects on the simulation. Never run one language model
instance continuously per resident.
