# Plan

## Objective

Make AutoBlock's core detection rule behaviourally compatible with Meowtils,
without importing source code or unsafe downstream actions.

## Scope

- `AutoBlockSignalCheck` and its trace tests.
- AutoBlock's threshold/config/default-enable contract.
- Detection specifications and this task's manual validation checklist.

## Non-goals

- Rewriting NoSlow, KillAura, Legit Scaffold, or BedNuke in this task.
- Changing packets, combat, input, server communication, or player state.
- Claiming that an alert proves cheating.

## Steps

1. Replace the broad sample/preset rule with the reference-compatible 11
   consecutive qualifying world-tick rule.
2. Keep bounded UUID state, world/global-lag reset, and local EvidencePolicy
   suppression as product-level safety guards.
3. Add tests for ten ticks, eleven ticks, interruption, world reset, lag reset,
   and presentation cooldown.
4. Remove AutoBlock from default enabled detectors while its normal-play gate
   is open; it remains user-configurable for controlled testing.
5. Run the Java 8 test/artifact gate and obtain a private Lunar normal
   sword-blocking trace before deciding whether to re-enable it by default.

## Risks

- Exact Meowtils compatibility is not equivalent to a reliable cheat verdict.
  Its two visible input states can be reproduced by normal sword blocking.
- A detector-level 11-tick count and the product's one-second alert cooldown
  are different concepts. The former is the compatible signal; the latter
  only controls local notification repetition.
