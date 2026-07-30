# Goal

## Objective

Implement AutoBlock, NoSlow, KillAura, and Legit Scaffold as conservative,
local-only evidence producers on top of the Phase 1 observation and evidence
contracts.

## Required evidence

- Each detector has immutable pure-Java input samples and state transitions.
- Each detector has a positive trace, a normal-play trace, a missing-state
  trace, and a world-reset trace.
- Minecraft reads remain inside narrowly scoped Mixins/adapters; detection
  classes must not import Minecraft classes or modify client behaviour.
- The MOD remains connection-independent and advisory-only.

## Stop condition

Stop before modifying Lunar settings or deleting the Ichor cache. Ask the user
to perform the rebuilt-agent Lunar smoke test only after automated verification
and review are complete.

## Non-goals

- BedNuke, combat desync, air-stall, block history, packet handling, proxy IPC,
  gameplay automation, reporting, or player verdicts.
