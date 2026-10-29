# Known World — All-Value Integration Contract (starting with 298 AC B4.2B.5)

Status: architecture and future acceptance specification, **not implemented NPC behavior**. The companion `VALUE_COVERAGE_ALL_SOURCE_ROWS.csv` enumerates the 761 labeled spreadsheet rows (706 values/fields, 24 major sections, 31 subsections); no row is silently discarded. Every status is deliberately `NOT YET CERTIFIED` pending per-value source/behavior verification, not a claim that implementation is absent.
Input authoring: `values.xlsx` column B enumerates intended characteristics; `settlements.xlsx` supplies potential place-level weights. The spreadsheets are authoring proposals; they do not presently drive the worldwide game runtime.

## Non-negotiable coverage rule

**Every canonical attribute or rule** authored in the spreadsheets must have a stable ID, type/units, applicability, owner, provenance, initial distribution, mutation source, persistence strategy, simulation consumers, dialogue consequences, tests, and fallback/default. Nothing silently disappears because it is rare, culture-specific, or temporarily unimplemented.

This does **not** mean every value changes every game system. A character trait may affect talk/social decisions but not building geometry. A settlement type may affect construction, laws and markets but is not a randomly rolled personality. Explicit `NOT_APPLICABLE` is allowed; `UNCONNECTED` remains a visible development backlog.

## Typed vocabulary instead of a universal random-weight table

| Type | Examples | Owning authority | Activation dependencies |
|---|---|---|---|
| APTITUDE / SKILL | stewardship, martial talent, languages | CharacterProfile/SkillPracticeService | birth/genesis, education, practice, actual outcomes |
| DISPOSITION / PERSONALITY | pride, adaptability, compassion | CharacterProfile and psychology | stable predisposition, events, social/decision/LLM effects |
| VALUES / NORMS | duty, social hierarchy, generosity | cultural upbringing + individual profile | layered distributions, norm compliance, social consequences |
| TRAIT | congenital, acquired, temporary, contextual | character traits/health/legal state | inheritance/childhood/event provenance and conflict rules |
| CULTURE / FAITH / LANGUAGE | Dothraki, Great Stallion, speech competence | culture/religion/language authority | family, region, migrations, exposure, conversion |
| OCCUPATION | smith, trader, artisan, herder | employment + verified work ledger | job availability, skill, contracts, resources, employer |
| STATUS / OFFICE | khal, bloodrider, dosh khaleen | membership, institutions, titles, law | political relationships, nomination, death/succession |
| ROUTINE / EDUCATION | apprentice, schooling, household work | daily life, family, mentor | campaign time and real location availability |
| SETTLEMENT / HOUSE / HOLDING TYPE | sacred gathering city, manor, ruin | site/holding and construction registries | stable physical-site hierarchy and building success |
| POLITICAL LAW / REGIONAL PROFILE | no drawn blades, market tolls | institutional rule engine | jurisdictions, exceptions, enforcement, witnesses |
| DERIVED MEASURE | economic standing, influence | source-specific calculators | measured world events and underlying balances |

Do **not** use the same color meaning for every type. Weight typical local culture/household formation for traits/values/faith/skills. For a role, law, institution, building or class status, use eligibility, capacity and explicit events instead of rolling prevalence.

## Distribution provenance and hierarchy

Default resolution: `world baseline -> continent -> realm/political jurisdiction -> region -> subregion -> settlement -> household/family -> culture/faith -> institutional role -> individual history`, with documented precedence, additive or multiplicative deltas, and explicit inapplicability. Political jurisdiction can diverge from geography; migration must retain natal traits and learned language/faith while updating exposure. For the author's colored Vaes Dothrak column, *green/red refer to typical Dothraki people*, not all merchants, visiting khalasars, enslaved foreigners or Dosh Khaleen members.

Store human-authored canon/culture claims separately from estimated statistics: `CANON`, `SOURCE_INFERRED`, `MOD_ESTIMATE`, `USER_AUTHORED`. Never backfill unknown populations with fake canon percentages.

## How attributes causally affect the game

1. `GENESIS`: regional and family priors, childbirth, inherited aptitudes, culturally weighted childhood.
2. `LIFE`: upbringing, apprenticeship, practice, health, trauma, legal/relationship changes; use sparse per-person events to avoid daily writes for 3.76 million people.
3. `SOCIAL`: affinity, social norms, reputation, trust, faction belonging, marriages, conflicts and prejudice; modifiers tied to explicit decisions.
4. `ACTION`: needs, priorities, skill success, work productivity, contract compliance, trade and crime; no universal indiscriminate stat multiplier.
5. `INSTITUTION`: role eligibility, law, titles, political votes/appointments, succession, disputes and governance.
6. `INFORMATION`: witnessed acts, authorized beliefs, uncertainty, travel-bounded rumors and private relationship memory.
7. `DIALOGUE`: context from authoritative world facts + person's traits/norms/skills/knowledge; LLM explains/proposes action but server validates the outcome.
8. `PERSISTENCE`: source version, sparse personality/event deltas, deterministic background replay, old-save migrations, no double counting.

## Milestones added across the existing roadmap

- **B4.2B.5–B4.2B.6:** register canonical value taxonomy and coverage audit; economic/social class, occupations, and ability-to-work effects; do not tie LLM words to fictional coin balances.
- **B4.2B production/consumption:** skills, aptitudes, needs, work ethic, health, age, class, household culture influence job/productivity/rewards; only verified work causes skill growth.
- **B4.3B travel/migration:** class, faith, language, birthplace, custom and personality affect destination and interaction; retain persistent identity and changed affinities.
- **B4.4 government/regency:** office-specific traits, realm laws, succession, kinship, faction loyalty, culture/faith conflicts; rare offices are institutional memberships, never random professions.
- **B4.5 social/knowledge:** generalized disposition/norm effects, romance, kinship, rivalries, personal memories, rumors and relationship feedback; source-backed dialogue.
- **Later family/lifecycle and education:** parent/guardian traits, inheritance rules, non-genetic cultural transmission, instruction and developmental windows for all newborn generations.
- **War/player systems:** leadership, tactics, morale, courage, culture, personal wounds, loyalty and player reputation; authoritative simulation events.
- **J/K parallel gate every batch:** review coverage registry, add behavior and unit/long-run tests for touched values, record still-unconnected attributes and update the authoring sheet.

## Scalable implementation contract

Use one **typed registry** for value definitions and multiple domain-owned behaviors, not one global `Map<String, Double>` driving everything. Each definition has metadata (`ID`, `type`, `units`, `valid range`, `exclusive group`, `provenance`, `inheritable`, `mutable`, `legal scope`, `consumers`). Behavior adapters operate on focused events (work shift, birth, social encounter, vote, crime, witness, journey), with deterministic order and bounded costs. NPCs not currently loaded retain baseline distributions plus sparse event deltas, not O(N×values×days) updates.

When a new value is authored, CI should fail its **coverage gate** if the definition has no declared behavior contract or explicit `NOT_YET_IMPLEMENTED` development ticket. Only mark a value *operational* after a server-visible consequence, persistence, replay and tests are proven. Do not claim LLM mention alone is a simulation effect.

## Vaes Dothrak-specific test case (future)

A visitor from a foreign culture keeps that culture's priors while in the city. The Dosh Khaleen council is a persistent membership/institution; a khal and bloodrider are elected/related offices; settlement peace laws are jurisdictional, not traits. Gathering of all khalasars is user-authored optional experiment, subject to travel time, capacity, supply and social/economic consequences. Validate that an NPC's conversation, work and decisions reflect these facts without inventing personal experiences.

## Exit condition for the entire values vision

For every row of the source value catalog, show an auditable status: `DEFINED`, `GENERATED`, `PERSISTED`, `DECISIONS_ACTIVE`, `SOCIAL_ACTIVE`, `LLM_CONTEXT_ACTIVE`, `INSTITUTION_ACTIVE`, `WORLD_CONSEQUENCE_TESTED`, with `NOT_APPLICABLE` where justified. An **end-to-end scenario test** must demonstrate a change to a trait or an institution changing later events in a reproducible way. This is a multi-phase product criterion, not one next-batch feature.
