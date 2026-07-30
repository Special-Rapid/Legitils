# Summary

## Result

- Remote F3+T-like unsupported air stalls were already intentional `Timer`
  positives; no remote global-lag safety was removed.
- Added a separate `.l dev on`-only direct Timer test event when the observing
  client has a delayed tick. This makes the local F3+T test alert once while
  keeping its identity excluded from Blacklist markers and WDR.
- The local UUID is captured at the start of each client tick, so enabling dev
  mode or entering a world does not require a prior full visible-player frame.
- The direct event still respects the Timer detector toggle, world-transition
  safety, and 30-second Timer cooldown; only normal global-lag suppression is
  bypassed for this explicit self-test event.

## Verification

- Added coordinator coverage for wrong-self suppression, development self
  alerting, no self marker, Timer cooldown, and re-alert after 30 seconds.
- Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed.

## Manual gate

1. Fully quit and relaunch Lunar with the rebuilt JAR.
2. Run `.l anticheat on Timer` and `.l dev on`.
3. Join a world, press F3+T, and wait for the reload to finish.
4. Expect one local `Timer` chat alert. Repeating within 30 seconds must not
   produce another one; it must never create a Blacklist marker or WDR button.
