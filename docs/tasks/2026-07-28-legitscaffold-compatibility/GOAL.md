# Goal

## Objective

Replace the current broad millisecond sneak-toggle detector with a clean-room,
Meowtils-compatible LegitScaffold state machine based on client world ticks,
crouch duration, and swing timing.

## Compatibility contract

For each visible non-local player, observed once after a client tick:

1. record sneak start and end world ticks;
2. retain the most recent five completed crouch durations;
3. record a swing only when visible `swingProgressInt == 1`;
4. in a block-held, grounded, downward-looking (`pitch >= 60`) context, match
   only when the latest crouch is 1–2 ticks, the latest three crouches are each
   at most three ticks, and the last swing is from crouch end through three
   ticks later and remains at most ten ticks old;
5. emit at most once per player per 60 world ticks.

## Product safety rules

The implementation remains local observation only: no packet/input operation,
reporting, blacklist, punishment, or gameplay action. Duplicate, skipped,
unreliable, world-transition, or global-lag observations invalidate partial
state rather than being bridged.

## Stop condition

Pure-Java tests cover every timing boundary and reset condition, Java 8
artifact verification passes, and a private Lunar trace confirms ordinary
bridging does not create an unacceptable alert rate before default enablement.
