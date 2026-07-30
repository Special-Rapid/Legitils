# Architecture

## Runtime path

```text
-javaagent:hypixel-legitils-loader.jar=loader-config.json
  -> First-party loader adds `mixins.hypixellegitils.json`
  -> MixinBootstrap observes Minecraft lifecycle and ticks
  -> ObservationCoordinator updates local state
  -> Signal checks emit Evidence
  -> EvidencePolicy decides whether to notify AlertSink
  -> Chat / sound / notification is shown locally
```

The MOD must initialise through its own Mixin bootstrap rather than relying on the old Meowtils module system or FML lifecycle discovery. This narrows the runtime surface and avoids taking a dependency on the reference artifact.

## Connection-independent operation

Minecraft owns the connection; Hypixel Legitils does not observe or alter the transport. Once a world is loaded, its observation path is identical whether the client joined `mc.hypixel.net` directly or joined a loopback local proxy that forwards to Hypixel.

```text
Minecraft world/entity/block state
  -> ObservationCoordinator
  -> detector and global-lag policy
  -> local advisory alert
```

The coordinator may reset observations on world/disconnect transitions, but it must not use the remote address as a detector gate. No proxy IPC, packet hook, proxy configuration, or proxy-specific module is permitted.

## Source layout

```text
src/main/java/com/snkisk/hypixellegitils/
  HypixelLegitilsBootstrap.java
  config/LegitilsConfig.java
  mixin/MixinMinecraft.java
  mixin/MixinWorldClient.java                 # only if block-update observation is required
  observation/ObservationCoordinator.java
  observation/PlayerObservation.java
  observation/PlayerObservationStore.java
  observation/WorldSnapshot.java
  detection/SignalCheck.java
  detection/AutoBlockSignalCheck.java
  detection/NoSlowSignalCheck.java
  detection/KillAuraSignalCheck.java
  detection/LegitScaffoldSignalCheck.java
  detection/BedNukeSignalCheck.java
  detection/CombatDesyncSignalCheck.java
  detection/AirStallSignalCheck.java
  evidence/Evidence.java
  evidence/EvidencePolicy.java
  alert/AlertSink.java
  alert/LocalAlertSink.java

src/main/resources/
  mixins.hypixellegitils.json
  hypixellegitils.default.json
```

## Data flow and lifetime

- `MixinMinecraft` calls bootstrap once after `startGame`; it emits pre/post client-tick observations and clears state on world unload.
- `ObservationCoordinator` reads only locally visible entities/world state and converts it to small immutable samples.
- `PlayerObservationStore` uses player UUID as the key. It has a bounded size and clears state on world change, player disappearance timeout, and explicit reset.
- `SignalCheck` implementations are pure state transitions as far as possible. Minecraft/Mixin calls belong at the coordinator edge, not inside evidence policy.
- `EvidencePolicy` is the only component permitted to decide whether an alert may be emitted. It applies cool-downs, minimum evidence counts, and global-lag suppression.
- `AlertSink` has no network capability; it only sends a local client message, local sound, optional alert-only Action Bar text, or overlay notification. Idle configuration status belongs to the future Companion/runtime-status path, not an in-game persistent HUD. During an active server Action Bar, the renderer leaves Lunar's server draw path untouched and suppresses the optional advisory Action Bar.

## Minimal Mixins

| Mixin | Purpose | Forbidden behaviour |
| --- | --- | --- |
| `MixinMinecraft` | startup, tick, world lifecycle observation | cancellable input hooks, timer changes |
| `MixinWorldClient` | optional local block-update observation | block mutation, packet interception |

Do not inherit the reference `MixinNetworkManager`, renderer, entity-raycast, keybinding, player-controller, or GUI accessor Mixins.

## Configuration

Use one local JSON configuration file named for the new project, not Meowtils. Settings include enabled detectors, supplementary alert modality, sensitivity preset, per-detector cool-downs, and debug evidence output. Chat is enabled by default; the Action Bar is used only for a local alert when optional overlay notification is enabled. The Companion/runtime-status path will show the on-demand status summary. The normal cool-down is one second and air-stall alone is thirty seconds. Debug mode must remain local and redact no data because no data leaves the device.

## Reuse boundary

Separate the code into a Minecraft-independent `detection`/`evidence` layer and a Minecraft 1.8.9 adapter layer. The former receives immutable observation samples and returns evidence; the latter owns Mixins, entity/world reads, configuration integration, and alerts. This makes a later integration with another client MOD possible without coupling its UI, feature modules, or broad Mixin set to the anti-cheat logic.
