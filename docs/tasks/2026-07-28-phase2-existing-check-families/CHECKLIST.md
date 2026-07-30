# Phase 2 Verification Checklist

## Automated checks

- [x] AutoBlock positive, normal-play, missing-state, and reset traces.
- [x] NoSlow positive, normal-play, potion-adjustment, missing-state, reset, and global-lag suppression traces.
- [x] KillAura positive, normal-play decay, missing-state, and reset traces.
- [x] Legit Scaffold positive, normal-play, missing-state, and reset traces.
- [x] Full Java 8 build, artifact gate, generated refmap, and source boundary check pass.
- [x] Development LaunchWrapper smoke reaches the Mixin launch stage without a Mixin validation error; non-interactive macOS still exits at the known AWT boundary.

## Manual Lunar gate

- [x] Add the rebuilt Java agent argument, delete the active Ichor `bake.zip`, and start Lunar 1.8.9. (Confirmed by user 2026-07-28.)
- [x] Confirm the persistent status HUD remains visible and no client crash occurs in a world with at least one other visible player. (Confirmed by user 2026-07-28.)
- [x] Confirm `Visible-player observation active.` and `World lifecycle reset.` in the current 1.8 profile log after entering/leaving a world. (Observed at 18:27:16 and 18:27:17.)
- [x] Confirm the client remains usable after several world transitions; no automatic gameplay action or packet behaviour occurs. (Confirmed by user 2026-07-28.)
- [x] Remove the JVM argument, delete the active Ichor `bake.zip`, restart Lunar, and confirm no Hypixel Legitils output. (Confirmed by user 2026-07-28.)

`NoBreakDelay` is documented as a deferred eighth detector only. It is not yet
in the MOD's active detector enum or current `4/4` status count. BedNuke,
combat desync and air-stall are likewise unavailable until their own phases.
