# B4.3A design contract

- **Authoritative person:** `b4:<site-id>:<person-suffix>` B4.0 shard (unchanged).
- **Active physical proxy:** B4.1A.1 `kwb41_actor`, global maximum 256, optional lower JVM property. It does not become a second person or alter the B4.0 census.
- **Time authority:** `NpcSimulation.campaignCalendar()`: absolute campaign day and tick remainder (same clock as B4.2).
- **Default offscreen position:** pure formula from work/rest schedule, home coordinate, assigned workplace and estimated gate-aware polyline. Cost per queried resident O(1), no all-world per-tick scan.
- **Observed handoff:** sparse per-world JSON (max 4096 observations, max 14 simulated days), atomic saves, fail closed on damage. This is a projection position continuity hint, not an event log or precise route progress.
- **Candidate discovery:** existing nearby home tiles plus bounded lookups by home tile for recently seen handoffs; worldwide road-intersection spatial indexing is not yet implemented.
- **Physical pathing:** remains B4.2C controller, no teleporting or forced chunk loading. The offscreen estimate may be refined when live Minecraft pathfinding resumes.
- **Privacy:** K-chat ledger and relationship stores untouched; no fabricated knowledge from estimated travel.
- **Data immutability:** person IDs, households, jobs, salaries, worldgen geometry and C4.15 commute allocations untouched.
- **Next:** B4.2B productive work and observed personal events; B4.3 full intersettlement travel/migration and event propagation.
