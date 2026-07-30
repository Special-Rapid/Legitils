# Summary

## Delivered

- Added local detector settings commands:
  - `.legitils anticheat list`
  - `.legitils anticheat on <AutoBlock|NoSlow|KillAura|LegitScaffold|BedNuke|all>`
  - `.legitils anticheat off <AutoBlock|NoSlow|KillAura|LegitScaffold|BedNuke|all>`
- Each update atomically writes the existing `config.json` schema with a
  monotonic revision and preserves all unrelated settings.
- After an atomic write succeeds, the enabled-detector set is also applied in
  the current session. Partial detector state is cleared so observations from
  before and after a toggle cannot combine. Other configuration stays
  restart-owned.

## Validation

- Java 8 `:mod:test` passed on 2026-07-29, including command parsing and
  runtime persistence and immediate application tests.

## Remaining manual gate

- Run a command in Lunar, confirm it stays local, then verify `.legitils status` immediately reports the saved active count.
