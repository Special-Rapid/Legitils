# Goal

## Objective

Replace KillAura's generic nearby-hurt/score detector with a clean-room,
Meowtils-compatible, POST-world-tick consumable-use and attack-animation state
machine.

## Contract

For each visible non-local, non-riding player on each continuous world tick:

1. Treat only food, potion and milk-bucket items as consumable.
2. Count continuous consumable-use ticks.
3. When use ends, record the end world tick.
4. Increment violation level only when the player has used the currently held
   consumable for more than six ticks, has an active attack animation, and is
   within 33 ticks of a completed consumable-use sequence.
5. Decay violation level by one on every other tick; emit on the eighth
   violating tick, then clear the detector pattern while retaining the
   product's separate local-alert policy/cooldown.

## Non-goals

- Copying source code, importing friend/team filters, WDR/reporting,
  blacklist/punishment behaviour, packet operations, input automation, or any
  gameplay control.
- Treating a local alert as a factual cheat verdict.

## Stop condition

The runtime captures the exact observable item/animation inputs, Java 8 tests
cover the state and reset boundaries, and a final static review passes. Lunar
normal-play and controlled/consented comparison traces remain release gates.
