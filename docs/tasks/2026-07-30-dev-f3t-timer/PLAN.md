# Plan

## Objective

Provide a deterministic local F3+T test signal without treating a general
observing-client freeze as remote player evidence.

## Scope

- Bootstrap development-self identity cache and delayed-tick adapter path.
- A coordinator-only direct development Timer evidence route.
- Focused tests and validation notes.

## Non-goals

- Distinguishing a remote Timer/Fly client from a remote F3+T reload.
- Removing global-lag suppression for remote observations.
- Persistent Blacklist, WDR, packet, or gameplay changes.

## Steps

1. Cache the current dev self UUID while normal visible-player sampling is
   available.
2. On a delayed client tick, reset all normal detector state as before, then
   submit one development-only Timer evidence item through the usual detector
   enablement and Timer cooldown policy while bypassing only the global-lag
   context for that direct local test event.
3. Preserve the existing self-marker/WDR exclusion and add deterministic
   tests.

## Risks

- In dev mode any client-tick stall over the existing threshold is deliberately
  treated as the requested Timer test signal; this is why the route must never
  activate outside `.l dev on`.
