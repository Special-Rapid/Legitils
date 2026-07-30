# Goal

## Objective

When `.l dev on` is enabled, make the observing client's own F3+T-style
client-tick stall emit one local `Timer` alert, while retaining global-lag
suppression for every normal remote observation.

## Stop conditions

- A development self player receives a Timer alert for a detected client-tick
  stall.
- The alert cannot create a Blacklist entry or WDR action.
- Remote Timer/Blink/global-lag behaviour remains unchanged.

## Validation

- Add a coordinator test for the dev-only Timer alert and run Java 8 tests.
- Manually enable `.l dev on`, use F3+T in-world, and confirm one Timer chat
  alert with the normal 30-second Timer cooldown. The first Tick after enabling
  dev mode also captures the local UUID before it evaluates a stall.
