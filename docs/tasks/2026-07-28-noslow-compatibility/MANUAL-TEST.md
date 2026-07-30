# Lunar NoSlow manual test

## Preconditions

- Launch Lunar with the current Java agent and a configuration in which
  `NO_SLOW` is enabled.
- Keep chat alerts enabled and Action Bar alerts optional/off so server UI does
  not hide the result.

## Normal-play no-alert trace

1. Join a normal Bed Wars match or an equivalent permitted server context.
2. Sprint while using ordinary usable items in the ways normal play permits.
3. Repeat across grounded and airborne moments, without using any client
   modification or packet manipulation.
4. Confirm that no `NoSlow` alert is emitted during the trace.
5. Record the map/mode, approximate duration, and whether an alert occurred in
   `RESULTS.md`.

## Controlled comparison trace

Only with every participant's consent and a permitted test environment, record
the same observation conditions for a known comparison client/fixture. This is
evidence-quality work only: do not add reporting, packet interception, input
automation, or gameplay control to the MOD.

## Stop/rollback

If Lunar crashes or normal play produces a NoSlow alert, stop the trace, retain
the relevant `latest.log` excerpt, and disable `NO_SLOW` in the configuration
before continuing ordinary play. Remove the JVM agent argument to roll back the
entire MOD.
