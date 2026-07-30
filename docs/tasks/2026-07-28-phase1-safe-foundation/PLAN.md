# Plan

## Objective

Build the reusable observation and evidence foundation required before adding individual anti-cheat detectors.

## Scope

- `mod/src/main/java/com/snkisk/hypixellegitils/config/`
- `mod/src/main/java/com/snkisk/hypixellegitils/observation/`
- `mod/src/main/java/com/snkisk/hypixellegitils/evidence/`
- `mod/src/main/java/com/snkisk/hypixellegitils/alert/`
- `HypixelLegitilsBootstrap`, `MixinMinecraft`, MOD resources, Gradle test configuration
- Pure-Java tests and Phase 1 documentation/checklist updates

## Non-goals

- No individual detector implementation.
- No block/entity scan, packet hook, proxy integration, Companion app, sound playback, or runtime configuration reload.
- No Lunar setting/cache modification by the implementation.

## Steps

1. Add versioned local configuration and runtime-status codecs with safe defaults and atomic write support.
2. Add bounded UUID observation storage plus world-reset lifecycle delegation.
3. Add immutable Evidence, fixed cool-down policy, global-lag/world-transition suppression, and local presentation queue.
4. Wire Java-only bootstrap/coordinator code to the existing Mixin, keeping Minecraft fields and UI methods inside the Mixin boundary.
5. Add pure-Java tests for configuration, storage, policy, and presentation; run build checks.
6. Document an Action Bar/world-reset Lunar smoke test and stop for user operation.

## Risks

- Lunar Ichor remaps Mixin members but not ordinary MOD class references; Action Bar and chat accesses require a live smoke test.
- `loadWorld` descriptor mapping must be verified in Lunar before later detector work relies on reset semantics.
- The active Ichor bake cache must be deleted when testing the changed Mixin configuration.
