
# Known World - Economy Domain Architecture

Batch: 20.3
Status: Domain design

## 1. Objective

Define a persistent, scalable medieval economy
connected to population, geography and political power.

The economy must create real consequences for:

- Households
- Settlements
- Dynasties
- Holdings
- Governments
- Armies
- Trade networks
- Individual characters

The economy must run independently of loaded
Minecraft chunks and physical NPC entities.

## 2. Authoritative State

The economy must establish explicit ownership of:

- Resource definitions
- Inventories
- Currency balances
- Asset ownership
- Production capacity
- Consumption demand
- Employment
- Economic obligations
- Transfers and transactions
- Market conditions

Reference catalogs define what resources are.

Runtime economic state defines how much exists,
who controls it and how it changes.

Reference definitions must not double as mutable stocks.

## 3. Economic Actors

Supported owner types should eventually include:

- Individual characters
- Households
- Businesses or workplaces
- Holdings and estates
- Settlements or public stores
- Governments
- Military organizations
- Religious institutions
- Other organizations

Ownership identifiers must be explicitly typed.

Do not infer ownership from unrelated references
such as a character's geographic position.

## 4. Resources

Initial resource categories:

FOOD:
Grain, meat, fish and other staples.

RAW MATERIALS:
Timber, stone, ore, wool and hides.

PROCESSED GOODS:
Tools, textiles, equipment and construction materials.

LUXURIES:
High-value traded goods.

MILITARY SUPPLIES:
Weapons, provisions and other army requirements.

CURRENCY:
Coin and monetary balances.

Resource definitions should provide:

- Stable resource ID
- Display name
- Unit of measurement
- Category
- Storage properties
- Spoilage rules where applicable
- Transport characteristics
- Reference metadata

Resource lists must be data-driven.

Do not embed all resources as Java enum constants.

## 5. Quantities

Quantities must have explicit units.

Examples:

- Grain in weight units
- Timber in volume or standard units
- Livestock in headcounts
- Currency in integer minor units
- Labor in person-days or defined work units

The initial implementation should avoid uncontrolled
floating-point transfers.

Fixed-point integers or another explicit precision
policy should be selected by resource type.

No negative resource inventory is permitted
unless the domain explicitly represents debt
or an authorized obligation.

## 6. Inventory Model

An inventory is an authoritative resource holding.

Potential fields:

- Inventory identity
- Owner reference
- Location reference
- Resource balances
- Capacity
- Last update time
- Schema version

A person may own multiple inventories.

A household may own stores in different locations.

Ownership and physical location must be separate.

## 7. Resource Accounting

Every state-changing economic operation must specify
the cause of a resource increase or decrease.

Core operations:

PRODUCTION
CONSUMPTION
TRANSFER
LOSS
DISCOVERY
CORRECTION

TRANSFER:

The source balance decreases by an amount.

The destination balance increases by the same amount.

A valid transfer does not change total world stock.

PRODUCTION:

Increases resources through a defined production process.

Inputs, labor and production constraints must be applied.

CONSUMPTION:

Reduces inventory for a defined purpose.

LOSS:

Reduces inventory through spoilage, destruction or theft.

Theft is normally a transfer if resources remain
owned by another simulated actor.

## 8. Transaction Invariants

Required invariants:

- No negative transfer quantity
- No transfer from insufficient available inventory
- No silent resource duplication
- No invalid owner reference
- No invalid resource definition
- No partial committed transfer
- Deterministic transaction ordering
- Explicit economic event provenance

Transfer operations must have atomic success/failure
semantics within authoritative simulation state.

Future multi-domain transactions require coordinated
commit or recovery rules.

## 9. Production

A production process may require:

- Workplace
- Input resources
- Workers
- Tools
- Land or infrastructure
- Skill
- Season
- Environmental conditions
- Available time

It produces:

- Output goods
- Byproducts
- Wages or obligations
- Experience or skill progression
- Environmental effects where relevant

Initial production should prioritize:

- Agriculture
- Basic food collection
- Timber production
- Simple toolmaking

Detailed manufacturing can follow once the
core economic loop is validated.

## 10. Consumption

Demand derives from:

- Population size
- Age and life stage
- Cultural diet
- Household wealth
- Occupation
- Health
- Seasonal conditions
- Military requirements

Consumption must affect authoritative inventories.

Failure to satisfy basic needs may influence:

- Health
- Mortality
- Migration
- Productivity
- Social unrest
- Political legitimacy
- Military morale

Individual and aggregate consumption must not
count the same person twice.

## 11. Labor

Labor supply derives from demographic state.

An employed person or cohort must not supply
more labor time than its schedule permits.

Required dimensions include:

- Available workers
- Age and legal eligibility
- Occupation
- Skill or aggregate productivity
- Seasonal availability
- Employment commitments
- Compensation

The eventual labor model must integrate with
NpcRoutineService and NpcActionProcessor.

WORK goals must not independently create economic
output if the economy already accounts for the work.

## 12. Property and Land

Landed holdings, title control and economic property
are related but distinct.

An economic asset may be:

- Owned by one entity
- Managed by another
- Taxed by a government
- Located inside a holding
- Subject to competing legal claims

Political titles must not automatically be treated as
direct ownership of every economic asset in a region.

Existing LandedHoldingManager remains authoritative
for its currently modeled holding relationships.

Economic property requires its own ownership contract.

## 13. Markets

Markets coordinate supply and demand.

Initial implementation:

- Local resource availability
- Local demand
- Basic prices
- Household transactions
- Merchant transactions

Future systems:

- Merchant networks
- Credit
- Transport costs
- Regional arbitrage
- Market regulation
- Embargoes
- Currency exchange
- Maritime commerce

Markets must not create goods merely because
demand exceeds supply.

## 14. Taxation and Obligations

Political obligations consume real resources,
money or labor.

Potential instruments:

- Household taxes
- Land rents
- Tithes
- Trade tariffs
- Military levies
- Labor services
- Tribute
- Debt repayments

Collected taxes must move to an identified owner
or treasury.

Taxes are transfers, not resource creation.

Government expenditure must draw from real balances
or explicitly created liabilities.

## 15. Trade and Transportation

Movement of resources requires:

- Origin
- Destination
- Available inventory
- Transport capacity
- Travel duration
- Cost
- Route viability
- Loss risks

A future shipment must not be counted as freely
available at both its origin and destination.

Goods in transit must have explicit ownership
and inventory state.

Transport can initially be abstract before
physical caravans and ships are implemented.

## 16. Time and Scheduling

The economy uses historical campaign time for
long-term production and consumption.

Campaign speed remains configurable.

When historical time is paused:

- Long-term economic production does not advance.
- Population consumption does not advance.
- Wages and taxes do not accrue through paused
  campaign-time cycles.

Immediate physical interactions may still occur.

Player trades must still move real inventory balances.

The implementation must prevent repeated physical
WORK actions from duplicating historical production.

Offline progression remains disabled.

## 17. Determinism

Random production shocks and economic choices must
use SimulationRandom contexts.

Stable ordering is required when processing:

- Settlements
- Households
- Inventories
- Workplaces
- Transactions

Economic outcomes should be reproducible given:

- Equivalent starting state
- Equivalent actions
- Equivalent scheduling
- Equivalent random context
- Equivalent algorithm version

## 18. Persistence

Economic state requires:

- Versioned resource definitions
- Versioned runtime records
- Referential integrity
- Save/load validation
- Transaction consistency
- Safe migration
- Recovery after interrupted saves

Existing save data must remain readable.

Do not reinterpret old fields or files silently.

## 19. First Integrated Economy Loop

A minimal functioning settlement should:

1. Contain households and people.
2. Have an initial stock of grain.
3. Produce grain through labor.
4. Transfer grain to household inventories.
5. Consume grain according to household needs.
6. Record remaining stock.
7. Update wellbeing based on food availability.
8. Record relevant outcomes.
9. Preserve all state through saving/loading.

A grain shortage must produce meaningful consequences,
rather than merely displaying a warning.

## 20. Future Political Integration

Economic data will supply inputs for:

- Tax revenue
- House wealth
- Political influence
- Public order
- Levy capacity
- Army provisioning
- War exhaustion
- Regional stability

The political engine must consume authoritative
economic information rather than maintaining
separate invented economic totals.

## 21. Design Boundaries

This design does not yet implement:

- Production simulation
- Markets
- Prices
- Household inventory
- Taxation
- Trade
- Army supply

It defines the boundaries and invariants
those implementations must satisfy.

## 22. Implementation Order

A. Stable economic actor and resource identifiers
B. Inventory state and conserved transfers
C. Household consumption
D. Population-driven labor availability
E. Basic production
F. Scarcity and wellbeing
G. Local transactions and prices
H. Government obligations
I. Regional trade
J. Military logistics

Each implementation stage must have regression tests.

## 23. Acceptance Criteria

Core economy completion requires:

- Authoritative inventory state
- Conserved resource transfers
- Working production
- Working consumption
- Population and household integration
- Economic effects on NPC decisions
- Working persistence
- Deterministic replay tests
- Long-duration simulation tests
- Acceptable scale performance

Do not declare the economy complete based on data
definitions or manager classes alone.
