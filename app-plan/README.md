# Hypixel Legitils App Plan

## Product decision

Hypixel Legitils is a new, clean Minecraft 1.8.9 client-side advisory project for Lunar Client. Its planned architecture has three parts: a first-party Java Agent loader starts the in-game MOD, the MOD observes local client state and presents local alerts, and a future native macOS Companion will own installation, settings, repair, updates, and status. The current repository implements the loader and MOD foundation; the Companion is not implemented, and the detector signals remain default-disabled or await controlled normal-play validation. The product is intended to help the local player notice suspicious gameplay patterns without modifying gameplay, sending automated commands, or claiming that another player is cheating. It is designed to use the same configuration when connecting directly to `mc.hypixel.net` or through a local proxy, but direct/proxied compatibility remains a release-validation gate. It does not depend on, load, or integrate with Meowtils or the separate local-proxy project.

The project is a new implementation. `sample/` is immutable reference material and must never be compiled into, copied into, or packaged with the product.

## MVP

1. A Lunar Agent-loadable JAR with a minimal Mixin bootstrap.
2. Local configuration and local chat/notification alerts.
3. UUID-scoped observation storage with safe world-reset behaviour.
4. Eight advisory signal detectors:
   - AutoBlock
   - NoSlow
   - KillAura
   - Legit Scaffold
   - BedNuke-like obstructed-bed-break evidence
   - Blink-like combat-correlated position-update stalls
   - Timer-like air-stall evidence
   - deferred NoBreakDelay cheat detection and local mining-cadence notification
5. Automated unit tests for pure evidence-scoring code, JAR-content checks, and a Lunar smoke-test checklist.
6. A clean first-party Java Agent loader, compiled as Java 8 bytecode and separate from the MOD.
7. A Developer ID-signed and notarized native macOS 13+ companion-app installation, settings, and recovery flow; it never injects into a running Minecraft process.

## Product boundary

This MOD may observe the local client world and show local warnings. It must not:

- intercept, delay, cancel, replay, or fabricate packets;
- automate movement, clicks, inventory operations, hotbar switching, attacks, chat, reports, or commands;
- alter reach, raycasts, cooldowns, entity targeting, game time, camera collision, or rendering to reveal hidden information;
- include ESP, free-look, view clipping, arbitrary extension loading, remote telemetry, or server-side/gameplay-affecting automatic blacklists;
- identify a player as definitively cheating or punish/report them automatically.

There are exactly three user-driven input exceptions:

- A user-entered `.legitils` (or `.l`) local diagnostic command may be consumed
  in the client GUI before any outbound chat packet exists, solely to print status/help, atomically persist settings, or edit the UUID Blacklist. It never sends, cancels, delays, replays, or fabricates a Minecraft network packet; it never changes gameplay or server state. For an explicit `blacklist add/remove <name>` whose UUID is not currently loaded, it makes one asynchronous request to Mojang's profile API containing that entered name; it does not retry automatically and never contacts the Minecraft server. Successful `anticheat`, `nickdetect`, `notify`, `dev`, and `blacklist on/off/threshold` changes are narrow runtime exceptions: they immediately apply only their detector, Nick display, alert channel, self-observation switch, or Blacklist state respectively. Other configuration changes remain restart-owned.
- A user-entered `.legitils anticheat list` or `anticheat on/off` command may inspect or atomically update the local detector set. A successful write immediately changes the active detector set after its partial timing state is cleared; it does not alter any other runtime setting.
- An accepted, attributable chat alert may show a `[WDR]` button. Only a
  deliberate user click on that button uses Minecraft's normal command path to
  run exactly `/wdr <validated-raw-player-name>`. The MOD never clicks it,
  queues reports, chooses a target, or gives the anonymous BedNuke signal a
  button.

Every other input passes through unchanged.

## Reading order

1. [Product scope](product-scope.md)
2. [Architecture](use-technology/architecture.md)
3. [Build and packaging](use-technology/build-and-packaging.md)
4. [macOS Companion app](companion-app.md)
5. [Installation and updates](installation-and-update.md)
6. [Detection specifications](detection-specifications.md)
7. [Integration policy](integration-policy.md)
8. [Implementation checklist](implementation-checklist.md)

## Current roadmap

| Phase | Status | Delivery | Exit criterion |
| --- | --- | --- | --- |
| 0. Bootstrap proof | Complete | Clean Java 8 loader and minimal MOD/Mixin bootstrap | Lunar 1.8.9 startup/tick proof and rollback both tested. See [bootstrap summary](../docs/tasks/2026-07-27-bootstrap-loader/SUMMARY.md). |
| 1. Safe foundation | Complete | Configuration, local alerts, bounded UUID observations, Evidence policy, cool-downs, global-lag suppression | Pure-Java tests and the Lunar manual gate proved reset, cool-down, evidence policy, alert rendering, and rollback. Persistent status was subsequently replaced with chat-default alerts and optional Action Bar alerts. |
| 2. Meowtils-compatible check rewrite | Static implementation complete | AutoBlock, NoSlow, KillAura, Legit Scaffold | All four now have clean-room tick-level static cores and remain default-disabled pending Lunar normal-play traces. |
| 3. BedNuke signal | Deferred experimental work | Conservative, unassigned obstructed-bed-break evidence | Block-in ambiguity requires a later temporal entry/occupancy redesign; it remains default-disabled. |
| 4. Blink and Timer | In progress | Blink-like combat-correlated desync and Timer-like air-stall evidence | Local/global freezes and ambiguous support states suppress; a remote F3+T-like air-stall remains an advisory Timer positive. |
| 5. NoBreakDelay anti-cheat | Static implementation complete | Conservative locally attributable mining-cadence detection and local notification | Default-disabled pending controlled Lunar traces; missing attribution, progress/state correlation, or ordinary mining context never alerts. |
| 6. macOS Companion | Planned | Native installation, settings, recovery, and update UI | No runtime attach or Lunar setting mutation; restart-required configuration flow tested. |
| 7. Release verification | Planned | Direct/proxied smoke, policy review, signed/notarized Companion | All eight detectors are available and conservative in both connection topologies. |

Detector quality work is currently open across **AutoBlock**, **NoSlow**,
**KillAura**, **Legit Scaffold**, and **BedNuke**. See the
[detection quality review](../docs/tasks/2026-07-28-detection-quality-review/NOTES.md)
before enabling or expanding those signals.

```text
Bootstrap proof (complete)
  -> observation/evidence core
  -> AutoBlock, NoSlow, KillAura, Legit Scaffold
  -> Blink and Timer evidence
  -> BedNuke evidence redesign
  -> NoBreakDelay controlled-world validation
  -> macOS Companion
  -> release and false-positive verification
```
