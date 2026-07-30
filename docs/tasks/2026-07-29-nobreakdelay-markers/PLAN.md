# Plan

## Objective

Deliver a bounded NoBreakDelay advisory signal and optional visible-player
warning marker without turning incomplete client observations into an alert or
a player label.

## Scope

- Detector identifier, immutable NoBreakDelay observation and pure-Java
  remote-cadence and local post-break-delay checks.
- Read-only Minecraft 1.8.9 remote and development-local adapters only if safe
  observation hooks are confirmed.
- Accepted-alert marker state, configuration, and read-only Tab/NameTag
  rendering adapters.
- Tests, local command/config documentation, and Lunar manual checklist.

## Non-goals

- Any packet mutation, automatic report, cheat verdict, persistent watchlist,
  targeting, or gameplay intervention.
- Guessing a miner from anonymous world updates.

## Steps

1. Map the existing observation, alert, configuration, and rendering paths.
2. Confirm version-specific hooks and reject unsupported attribution paths.
3. Implement the smallest conservative signal: only a remote S25 progress
   sequence that is matched to a server-applied block removal, with a resolved
   visible UUID and two confirmed immediate block completions, may produce evidence.
4. Add a dev-mode-only local `PlayerControllerMP.blockHitDelay` observer so a
   bypassed five-tick post-break delay can be self-tested without reusing the
   remote S25 protocol.
5. Integrate accepted attributable alerts with the default-off persistent
   Blacklist marker for both Tab and NameTag.
6. Add deterministic tests and Java 8 artifact checks.
7. Review the stable result and record Lunar manual gates.

## Risks

- Minecraft 1.8.9 world block updates do not inherently identify the breaker;
  the detector must stay silent unless an independently reliable association
  exists.
- Lunar can replace Tab/NameTag rendering, so Mixin success requires an
  explicit manual compatibility gate.
