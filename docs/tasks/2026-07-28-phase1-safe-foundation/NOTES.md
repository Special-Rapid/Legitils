# Notes

## Phase 1 decisions

- The MOD reads `~/Library/Application Support/HypixelLegitils/config.json` once at startup. Missing or invalid configuration falls back to immutable defaults; it never prevents Minecraft startup.
- The configuration writer exists for the future Companion's atomic replacement workflow. The MOD itself writes only minimal `runtime-status.json`; it never persists player UUIDs, evidence, alerts, packets, or account data.
- Evidence policy is the sole alert gate. Default normal cooldown is 1000 ms; air-stall is 30000 ms. Global lag, world transition, insufficient history, disabled detectors, and cooldown suppress rather than alert.
- Minecraft field/method access stays in `MixinMinecraft`. The Java-only bootstrap, config, observation, evidence, and alert packages must never import Minecraft classes because Lunar Ichor does not remap ordinary MOD class member references.
- `Minecraft.loadWorld(WorldClient,String)` maps to `func_71353_a` in the generated refmap and resets bounded UUID observation/evidence state before a world changes.

## Manual test boundary

The Action Bar shadows and lifecycle injection compile and pass the development Mixin launch stage, but Lunar Ichor mapping of these newly added Mixin members remains an explicit user-operated smoke test. Enabling or disabling the Mixin requires deleting the active Ichor `bake.zip` as established by the bootstrap task.
