# Goal

## Objective

Make Nick markers render through the same `EntityPlayer#getDisplayName` path
used by the compatible reference mod, and make the development-only
NoBreakDelay observer prove whether its controller hook is receiving input.

## Stop conditions

- A UUID-v1 profile receives the red `[NICK]` suffix in the player name-tag
  path without writing identity data to the local Blacklist.
- With `.l dev on`, the Lunar log records that the NoBreakDelay controller
  hook has received a local mining call; a bypass remains an alert-only,
  local development observation.
- Unit tests and Java 8 artifact verification pass.

## Validation

- Run `./gradlew :mod:test verifyBootstrapArtifacts` with Java 8.
- Restart Lunar completely, join a world containing a known nick, and check
  the name tag and Tab list.
- Enable `.l dev on`, break a normal block and then exercise the test client;
  compare the one-time controller-hook log with the alert result.
