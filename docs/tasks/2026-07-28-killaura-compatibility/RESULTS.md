# KillAura compatibility results

## Automated

- 2026-07-28: Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed.
- The tests cover consumable classification, strict use duration, recent-end
  boundary, active-animation requirement, VL decay, alert reset, unreliable
  and discontinuous observation cleanup, riding exclusion, global-lag frame
  invalidation, and bounded player-state cleanup.

## Lunar manual gate

- Pending: normal consumable-use and ordinary-combat no-alert trace.
- Pending: controlled and consented comparison trace.

KillAura is disabled in new/default configurations until both items are
recorded.
