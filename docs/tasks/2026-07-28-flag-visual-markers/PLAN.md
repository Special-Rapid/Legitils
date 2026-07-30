# Plan

## Objective

Add a future optional local `⚠` marker after repeated advisory alerts for one visible player.

## Scope

- A bounded UUID-keyed accepted-alert counter next to observation state.
- A restart-required threshold and enable/disable configuration.
- Read-only client rendering adapters for Tab text and visible player name tags.
- Deterministic policy, lifecycle, and rendering-selection tests.

## Non-goals

- Marking a player from anonymous evidence.
- Persisting a watchlist, communicating flags, automatic reporting, or server moderation.
- Packet interception/mutation, entity targeting, or gameplay effects.

## Steps

1. Measure false-positive traces and choose default threshold, range, and whether count decays or remains session-scoped.
2. Add one bounded per-player `acceptedAlertCount` only after `EvidencePolicy.shouldAlert` is true.
3. Reset/prune that state with world transitions and visible-player expiry.
4. Expose a read-only marker query to two rendering adapters.
5. Add optional Tab and NameTag suffix rendering, preserving existing client formatting and suppressing output for unknown/stale players.
6. Validate direct and proxied worlds, plus Tab/name-tag coexistence with Lunar UI modules.

## Open decisions

- Default threshold and permitted range (for example 2–10 accepted alerts).
- Whether different detector families should have equal weight.
- Whether the user can enable Tab and NameTag separately or only together.
- Whether counts reset only on world change or also decay after inactivity.

## Risks

- NameTag/Tab render hooks are client-version and Lunar-module sensitive, requiring a separate compatibility check before implementation.
- A marker can be misread as a verdict; its text and settings must call it a local advisory indicator.
