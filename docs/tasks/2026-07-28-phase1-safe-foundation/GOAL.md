# Goal

## Objective

Implement Phase 1 of Hypixel Legitils: startup-loaded local configuration, bounded UUID observation state, Evidence/cool-down/global-lag policy, local alert presentation, and persistent Action Bar status.

## Stop condition

- Complete pure-Java unit and packaging checks.
- Prepare an exact Lunar smoke procedure for Action Bar status, lifecycle reset, and rollback.
- Stop for the user before modifying the Lunar JVM argument or cache again.

## Safety boundaries

- No packet hooks, synthetic input, gameplay changes, external network access, or auto-reporting.
- Ordinary MOD classes must not directly access Minecraft members; only the existing Mixin boundary may do so.
- Configuration is local, startup-loaded, and contains no player/evidence history.
