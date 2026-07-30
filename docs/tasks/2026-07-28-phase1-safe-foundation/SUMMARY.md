# Phase 1 Summary

## Result

Phase 1 — Safe foundation is complete. The MOD now has a dependency-free local
configuration contract, bounded UUID observation state, conservative evidence
policy, local notifications, and a persistent status HUD. It remains
advisory-only: it does not implement detector logic, change gameplay, or alter
network traffic.

## Delivered

- Versioned configuration and runtime status under
  `~/Library/Application Support/HypixelLegitils/`.
- Immutable detector, notification and sensitivity configuration contracts.
- Bounded UUID observation storage and reset on every world load.
- Central evidence/cool-down policy: normal evidence is one second and
  air-stall evidence is thirty seconds; global-lag, transition, insufficient
  history and disabled detectors suppress alerts.
- Persistent HUD status with local-only alert presentation. It is independent
  of the vanilla record-notification state, which takes precedence while shown.
- Pure-Java tests covering configuration, state cleanup, full cooldown-key
  capacity, transition suppression, and local presentation preferences.

## Verification

- Java 8 offline build, artifact gate and all 12 unit tests passed on
  2026-07-28.
- Development LaunchWrapper smoke reached the Mixin launch stage without a
  Mixin validation error; the known non-interactive macOS AWT boundary remains
  outside gameplay verification.
- Lunar 1.8.9 manual test confirmed bootstrap, persistent status, vanilla
  record-notification coexistence, repeated world-lifecycle resets, client
  usability and a clean JVM-argument/Ichor-cache rollback.

## Next

Phase 2 adds AutoBlock, NoSlow, KillAura and Legit Scaffold as conservative
evidence producers using these shared contracts.
