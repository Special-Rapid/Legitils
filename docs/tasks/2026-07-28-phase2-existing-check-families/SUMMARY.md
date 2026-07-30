# Phase 2 Summary

## Result

Phase 2 — Existing check families is complete. AutoBlock, NoSlow, KillAura and
Legit Scaffold now produce only local, advisory evidence from immutable,
client-visible player samples. No detector changes gameplay, inputs, packets or
server state.

## Delivered

- Bounded pure-Java detector state for up to 256 visible players.
- AutoBlock evidence for repeated blocking/swing overlap.
- NoSlow evidence for repeated sprint/item-use movement anomalies, with potion
  adjustment and zero/long sample-gap suppression.
- KillAura evidence for repeated ambiguous item-use/swing/combat timing
  sequences with normal-state score decay.
- Legit Scaffold evidence for repeated short sneak cycles in a narrow visible
  block-holding context.
- A Mixin-owned adapter which reads only client-visible player state and emits
  immutable samples; all Minecraft references remain inside Mixin classes.
- Current-build configuration and HUD expose only the four implemented checks
  (`4/4`), while preserving valid settings from older configurations that list
  future detector IDs.

## Verification

- Java 8 offline build, artifact gate and pure-Java test suite passed.
- Tests include positive, normal-play, missing-state and reset traces for every
  Phase 2 detector, plus potion, global-lag, zero-elapsed-time and
  configuration-migration regressions.
- Development LaunchWrapper reached the Mixin launch stage without validation
  errors; the known non-interactive AWT boundary remains outside gameplay
  verification.
- Lunar 1.8.9 manual test logged bootstrap at 18:26:50, world resets at
  18:27:16, and visible-player observation at 18:27:17. The user also confirmed
  HUD, usability and clean rollback.

## Deferred work

NoBreakDelay is documented as a later anti-cheat detector for repeated absence
of the expected inter-break delay. BedNuke, combat desync and air-stall remain
subsequent phases.

## Next

Phase 3 defines conservative block history and actor-attribution contracts for
BedNuke evidence.
