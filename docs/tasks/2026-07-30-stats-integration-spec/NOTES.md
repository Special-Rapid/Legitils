# Research notes and specification draft

## Provider roles

| Provider | Intended data | Requirement | MVP status |
| --- | --- | --- | --- |
| Hypixel | Bed Wars level, FKDR, WLR, finals, wins, mode winstreak | Approved server-side API design; no distributed key or user-entered Hypixel key | Blocked pending approval/design |
| Urchin | Community tags/reports | User-owned or approved application key; provider rate limits | Optional |
| Seraph | Community blacklist/tag metadata only unless separate stats access is confirmed | Seraph API access, endpoint, rate limit, and terms confirmation | Blocked pending access |

Urchin and Seraph tags are not proof of cheating and must never directly add a player to the local blacklist.

## Recommended Bed Wars match flow

1. Detect an in-game Bed Wars transition.
2. Wait for a short roster stabilization window, then collect unique real UUIDs from Tab.
3. Exclude self and UUID-v1 Nick profiles from every remote request.
4. Start one request group per remaining UUID and provider. A request carries the current match generation and is ignored if the world changed.
5. Present Tab information as each result returns. Do not make rendering start HTTP work.
6. After all high-priority requests finish or a bounded deadline elapses, send at most one local summary plus up to three high-stats rows.

## Safety and rate controls

- Persistent cache: at least 24 hours for Hypixel and provider-specific TTL for community tags.
- In-flight requests de-duplicate by `provider + UUID`.
- Source queues are independent, bounded, and cancel stale match results.
- 429/5xx responses back off; errors are silent except for a manual lookup.
- No continuous polling during a match and no server chat output.

## Display draft

### Tab

Append only compact Bed Wars values after the existing local markers:

`Name [NICK] [! ]  §8| §b✫120 §8| §e4.2 FKDR §8| §aWS 7`

- Nick: no remote data; keep the existing red `[NICK]` marker only.
- Local blacklist: keep the existing yellow marker.
- Unknown/loading/error: omit the stats suffix rather than showing misleading values.
- Fields should be individually configurable; default is level, FKDR, and mode winstreak.

### Local chat at match start

One session header, then no more than three high-stats rows:

`[Legitils] Bed Wars stats: 12/15 profiles loaded (3 unavailable)`

`[Legitils] High stats: Name — ✫420 | FKDR 12.4 | WS 18`

Community data is separate and source-labelled:

`[Legitils] Urchin tag: Name — <tag>`

No message should claim that a community tag or high stats proves cheating.

## Proposed high-stats policy

Notify only when a player meets one of these conditions, using their selected Bed Wars mode where available:

- Elite: level >= 300 and FKDR >= 10; or winstreak >= 20.
- Strong: level >= 100 and FKDR >= 5; or winstreak >= 10.

Chat defaults to Elite only. Strong can be enabled in settings. Results are sorted by severity, then FKDR, then level. This is a profile summary, not a warning or flag.

## Commands draft

- `.l stats on/off`
- `.l stats status`
- `.l stats display <tab|chat> on/off`
- `.l stats provider <urchin|seraph> on/off`
- `.l stats threshold <elite|strong>`
- `.l stats <player>` (manual lookup, once approved data access exists)

## Decisions required before implementation

1. Hypixel data architecture: obtain an approved backend/application arrangement, or omit Hypixel stats from the first release.
2. Seraph: obtain API access and record the approved base URL, access scope, and rate limits.
3. Chat threshold: keep `Elite only` as default, or notify `Strong` too.
4. Tab fields: default `level + FKDR + mode winstreak`, or use a smaller default.
5. Community tag visibility: show all source-labelled tags, or only configured severities.
