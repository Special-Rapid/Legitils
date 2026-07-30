# Plan

## Objective

Replace restart-required detector configuration with an atomic local save plus
safe in-world detector-set transition.

## Scope

- `ObservationCoordinator` runtime config transition and detector state reset
- Bootstrap command response/status/list presentation
- Local command help wording
- Tests for immediate status, failed persistence, and transition reset
- Product and task documentation

## Non-goals

- No runtime Mixin configuration, loader changes, sensitivity changes, or
  Companion behavior changes.
- No config write occurs when the conflict guard rejects disk state.

## Steps

1. Add a coordinator method that swaps an immutable saved config only after
   persistence succeeded and resets detector patterns.
2. Apply that method from `anticheat on/off`, then report the active count.
3. Change help/list/docs from next-start/restart wording to immediate detector
   toggle wording.
4. Add pure-Java tests, build with Java 8, and review the stable result.

## Risks

Runtime enablement must not inherit partial timing state, and a failed config
save must not change live behavior.
