# Summary

## Delivered

- Added default-disabled `NoBreakDelay`: an actor-resolved remote S25 mining
  sequence must reach stage 9, match a server-applied air update at the same
  position, and complete two immediate consecutive breaks to alert.
- Added a separate dev-mode local controller observer: after a survival break
  sets `blockHitDelay` to five, an early zero is a direct NoBreakDelay
  self-test signal. It cannot persist a Blacklist entry or expose WDR.
- Excluded incomplete/unresolved events, unloaded/non-full blocks, creative,
  Haste, enchanted tools, and out-of-range actors.
- Added optional yellow `§e⚠` Blacklist markers to Tab and NameTag. They are
  default-disabled and require the configured accepted-alert threshold.
- Added the `blacklist` command surface (with legacy `marker` aliases) for
  persistent UUID Blacklist settings. Changing its runtime settings applies
  immediately without clearing stored Blacklist history.

## Validation

- Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed on 2026-07-29.
- Pure-Java tests cover the two-break remote cadence threshold, direct local
  post-break-delay bypass, missing confirmation, reset, accepted-only marker
  counting, anonymous evidence, cooldown suppression, global-lag suppression,
  and world reset.
- Final review confirmed that competing miner UUIDs at one position suppress
  NoBreakDelay attribution, schema 1-to-2 marker migration applies live, and
  Tab markers require a currently visible world player.

## Remaining manual gate

- Restart Lunar once to load this JAR, then verify normal mining does not flag
  and test controlled remote mining before enabling NoBreakDelay normally.
- For the local development gate, enable `.l dev on` and `.l anticheat on
  NoBreakDelay`; normal survival mining must not alert and a consented bypass
  must alert only the local client.
- In a world with accepted attributable alerts, use `.l blacklist on` and
  confirm a yellow `§e⚠` appears in both Tab and NameTag without breaking Lunar
  UI formatting; confirm it remains for that UUID after a later world change.
