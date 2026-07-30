# Phase 1 verification checklist

## Automated checks

- [x] Config codec defaults safely for a missing or malformed file and round-trips valid configuration atomically.
- [x] UUID observation state evicts the oldest entry, expires stale entries, and clears on explicit reset.
- [x] Evidence policy allows normal evidence after one second and air-stall evidence after thirty seconds, including all seven detector keys for 256 observed players.
- [x] Global lag, world transition, missing history, and disabled detectors suppress alerts; a fresh player observation is required to leave a world-transition suppression state.
- [x] Local alert presentation temporarily replaces the persistent Action Bar status without any Minecraft dependency, and honours the overlay notification preference.
- [x] Full Java 8 build, Mixin refmap generation, and artifact checks pass.
- [x] Development LaunchWrapper smoke reaches the Mixin launch stage without a Mixin validation error; the headless macOS process still exits at the known AWT boundary.

## Manual Lunar gate

- [x] Add the rebuilt Java agent argument, delete the active Ichor `bake.zip`, and start Lunar 1.8.9. (Confirmed by user 2026-07-28 after the dedicated HUD Mixin change.)
- [x] Confirm the persistent Action Bar text `Hypixel Legitils: 7/7 detectors active` without a crash, and verify that a vanilla record-notification is not replaced while it is visible. (Confirmed by user 2026-07-28.)
- [x] Confirm `Bootstrap Mixin reached Minecraft.startGame; ...` and `World lifecycle reset.` in the current 1.8 profile log. (Confirmed by user 2026-07-28.)
- [x] Enter and leave a local world, then confirm another `World lifecycle reset.` line and that the client remains usable. (Confirmed by user 2026-07-28.)
- [x] Remove the JVM argument, delete the active Ichor `bake.zip`, restart Lunar, and confirm no Hypixel Legitils output. (Confirmed by user 2026-07-28.)
