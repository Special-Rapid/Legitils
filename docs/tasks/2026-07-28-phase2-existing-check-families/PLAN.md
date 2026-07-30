# Plan

## Objective

Deliver the four existing check families as conservative local evidence
producers while retaining the Phase 1 clean Lunar/Ichor boundary.

## Scope

- `mod/src/main/java/com/snkisk/hypixellegitils/detection/`
- Phase 1 observation coordinator and Mixin adapters
- `mixins.hypixellegitils.json` only if a minimal additional observation Mixin
  is required
- Pure-Java tests under `mod/src/test/java/`
- Phase 2 documentation and checklist updates

## Non-goals

- Packet/input interception, any client gameplay modification, Meowtils
  integration, proxy integration, or later detector families.

## Steps

1. Map supported 1.8.9 client-visible fields and choose conservative sample contracts.
2. Implement pure detectors with bounded state and explicit reset methods.
3. Add a Mixin-owned adapter that produces samples only from visible entities.
4. Test positive, normal, missing-input, and reset traces for every detector.
5. Build, inspect generated Mixin mappings, review the stable diff, then stop
   for the Lunar manual gate.

## Risks

- Remote-player use/block state may be absent or unreliable; absence must
  suppress evidence rather than infer it.
- Lunar/Ichor mappings permit Minecraft members only inside Mixins.
- Swing and movement observations are not proof of an external modification;
  thresholds must require repeated patterns and state decay.
