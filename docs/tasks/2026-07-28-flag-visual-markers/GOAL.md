# Goal

## Objective

Plan a deferred local-only visual marker for a visible player who crosses a
user-configured number of accepted anti-cheat alerts. The marker is a `⚠`
suffix in the Tab list and beside the in-world name tag.

## Product contract

- The marker is advisory and local-only; it is never sent to a server or other players.
- Count only evidence that `EvidencePolicy` allowed to alert. Suppressed, incomplete, and cool-down-rejected evidence never increments a count.
- Count only evidence with a non-null player UUID. Anonymous local events, including the current BedNuke signal, cannot mark a player.
- The threshold is a restart-required configuration option. Default and allowed range must be chosen with false-positive traces before implementation.
- Counts are bounded, reset on world/session transitions, and are not persisted or shown as a player watchlist.

## Stop conditions for a later implementation

- Tab and NameTag rendering read the same bounded local marker state.
- A player below the threshold has no marker; a player at/above it has one marker in each enabled surface.
- Disconnect/world reset, stale-player expiry, global lag, and anonymous evidence cannot leave a stale or false marker.
- The implementation adds no packet, input, targeting, or gameplay behaviour.
