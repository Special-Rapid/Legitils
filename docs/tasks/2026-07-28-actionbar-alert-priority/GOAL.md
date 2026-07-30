# Goal

## Objective

Remove the always-on MOD Action Bar status. Keep server Action Bar text visible
in normal play, and make local chat the default detection notification. An
optional Action Bar alert can be enabled in configuration only when no server
Action Bar is active; Lunar's own GUI injection owns the server Action Bar draw
call. Defer an in-game status command until it has a separately validated Lunar
runtime integration path.

## Stop conditions

- No persistent MOD Action Bar is rendered while no alert is active.
- An optional local Action Bar alert renders its advisory text only while no
  server Action Bar is active.
- No packet interception, cancellation, delay, replay, or fabrication is added.
- Java 8 tests and the bootstrap artifact gate pass.

## Manual validation

- On a server with a visible Action Bar (for example `You are currently BUSY`),
  verify that idle play shows only the server text.
- With the optional Action Bar alert enabled, verify that an active server
  Action Bar has priority without a crash or overlapping text.
