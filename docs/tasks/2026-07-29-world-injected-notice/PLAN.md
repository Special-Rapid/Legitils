# Plan

## Objective

Add a small local installation/readiness notice at the existing world-ready
Mixin boundary.

## Scope

- `MixinMinecraft` world lifecycle state and local chat rendering.
- A focused Java-only message-format test and manual smoke checklist.

## Non-goals

- Persistent HUD status, server chat, configuration UI, or detector changes.

## Steps

1. Define the fixed local notice text.
2. Emit it once after `theWorld` and `thePlayer` are available.
3. Reset the once-per-world guard on world loading/unloading.
4. Run Java 8 tests and document Lunar verification.

## Risks

- A world transition may have multiple client ticks; the guard must prevent
  duplicate messages.
