# NoSlow compatibility results

## Automated

- 2026-07-28: Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed.
- The pure-Java traces cover 21-tick progression, threshold/state boundaries,
  airborne eligibility, Speed adjustment, corrected prior-position handling,
  duplicate/skipped tick, unreliable/missing/world-reset cleanup, hard
  global-lag-frame invalidation, and bounded player-state cleanup.
- Static final review found no remaining blocking issue after the initial
  default-safety and test-strength follow-ups.

## Lunar manual gate

- Pending: normal item-use movement no-alert trace.
- Pending: controlled and consented comparison trace.

No release-quality conclusion is implied until both manual items are recorded.
