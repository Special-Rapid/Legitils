# Goal

## Objective

Replace the current AutoBlock rule with a clean-room implementation of the
Meowtils-observable tick rule, then establish whether that rule can be safely
enabled for normal Lunar play.

## Compatibility contract

For each visible non-local player, count consecutive client-world ticks where
both visible `isBlocking()` and `isSwingInProgress` are true. Reset the count
when either condition is false. The Meowtils threshold is strictly greater than
10, meaning the 11th consecutive qualifying tick is the first failure.

The product retains its global safety boundary: no packet/input inspection,
mutation, WDR/reporting, blacklist, punishment, or gameplay action.

## Decision gate

The two visible states in the contract can also occur during ordinary sword
blocking. Therefore the reference-compatible detector must remain disabled by
default until controlled normal-play traces demonstrate that the 11-tick
sequence does not produce unacceptable false positives. If it does, the
feature remains an optional experimental advisory rather than adding a looser
non-reference proxy.

## Stop condition

Automated tests prove exact 10/11 tick, interruption, reset, cooldown and
global-safety behaviour. A Lunar private-world trace then decides whether
default enablement is safe; no claim of normal-play discrimination is made
before that trace exists.
