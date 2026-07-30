# Summary

## Delivered

- `.legitils anticheat on/off` now atomically saves and immediately applies
  the enabled-detector set in the running client.
- `status`, `list`, help, and success text describe the active state rather
  than asking for a Lunar restart.
- A successful detector-set transition clears both the per-player timing
  engine and BedNuke's independent partial bed history.
- Sensitivity, notification settings, cooldowns, Mixins, and loader settings
  remain restart-owned.

## Validation

- Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed on 2026-07-29.
- Unit coverage proves the active count updates immediately, a non-detector
  runtime change is rejected, timer progress is reset, and a pre-toggle
  partial BedNuke trace cannot complete after the toggle.

## Remaining manual gate

- Restart Lunar once to load this rebuilt JAR, then use `.legitils anticheat
  on Blink` and `.legitils status` in the same session. The status count must
  change without another restart and the command must remain local chat.
