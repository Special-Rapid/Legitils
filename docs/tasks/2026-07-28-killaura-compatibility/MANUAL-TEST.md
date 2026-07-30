# Lunar KillAura manual test

## Preconditions

- Launch Lunar with the current agent and an explicit configuration containing
  `KILL_AURA`.
- Keep chat alerts enabled; Action Bar output is optional.

## Normal-play no-alert trace

1. In a permitted normal gameplay context, eat food, drink normal potions and
   use milk buckets as ordinary play allows.
2. Include ordinary combat before and after those uses, without any client
   modification, packet manipulation, or automation.
3. Confirm no `KillAura` local alert and record the outcome in `RESULTS.md`.

## Controlled comparison trace

Record a known comparison only with every participant's consent and in a
permitted test environment. This is local evidence-quality validation; it must
not add reporting, interception, input automation, or gameplay control.

## Stop/rollback

If normal play alerts or Lunar crashes, stop, retain the relevant `latest.log`
excerpt, disable `KILL_AURA`, and remove the Java agent argument to roll back
the MOD if necessary.
