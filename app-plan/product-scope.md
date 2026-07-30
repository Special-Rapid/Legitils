# Product Scope

## Target environment

- Minecraft Java Edition 1.8.9.
- Lunar Client launched through the clean first-party Hypixel Legitils loader mechanism.
- Java 8-compatible bytecode.
- Local client use only. The first release does not promise standalone Forge distribution.
- A direct Lunar Agent MOD installation is the complete anti-cheat path; no local proxy, packet capture, or proxy authentication setup is required.

## Connection compatibility

The anti-cheat has one client-side runtime and must work with either supported connection topology:

```text
Direct:  Lunar Client + Hypixel Legitils MOD -> mc.hypixel.net
Proxied: Lunar Client + Hypixel Legitils MOD -> local proxy -> mc.hypixel.net
```

- The MOD does not branch its detector logic on the configured server host or port. In particular, `127.0.0.1` must not disable checks merely because a local proxy is in use.
- It reads only the resulting local client world, entity, block, and tick state after Minecraft has processed it. It does not connect to, configure, or exchange data with the proxy.
- A proxy that delays, drops, or rewrites normal play-state updates can reduce observation quality. The evidence policy must treat this as uncertainty/global-lag evidence and suppress alerts rather than attributing it to a player.
- Direct and proxied connections both need their own smoke-test traces. The feature is anti-cheat only in the first release; cosmetics, stats overlays, and proxy controls are not part of this MOD MVP.

## User value

The player receives a clear local alert when observations meet a conservative evidence threshold. Alerts explain the observed pattern, confidence level, and why it is not a definitive server-side verdict.

## Explicitly retained concepts

- startup after `Minecraft.startGame`;
- client tick and world lifecycle observation;
- UUID-keyed per-player state;
- persisted user settings;
- local chat, sound, optional alert-only Action Bar output, an on-demand local
  status command, and an optional user-clicked WDR button on attributable chat
  alerts.

These are concepts only. Do not copy Meowtils source or its package names.

## Detector scope

The first product scope is exactly the following eight local-warning checks:

1. AutoBlock
2. NoSlow
3. KillAura
4. Legit Scaffold
5. BedNuke
6. Blink (combat-correlated desync)
7. Timer (air-stall)
8. NoBreakDelay cheat detection through a locally observed mining-cadence
   anomaly (deferred until after the higher-priority detector phases)

The Meowtils implementations are behavioural references only. Each check is redesigned against the new observation/evidence interfaces and must pass its own false-positive tests. In particular, the old NoSlow implementation is not portable because it never updates its prior-position fields.

## Reference-only inventory

| Current path | Role in this project | Packaging rule |
| --- | --- | --- |
| `sample/Meowtils-Lunar/` | deployed reference bundle | never package |
| `sample/Meowtils-2.0.1.jar_Decompiler.com/` | decompiled behavioural reference | never compile or package |
| `sample/net/curxxed/dev/agent/` | Agent loading reference | do not fork into the MOD |
| `sample/anticheat/` | examples of stateful checks, including defects | redesign only |
| `sample/client-side-anti-cheat/` | examples of alert orchestration | redesign only |
| `sample/mappings/` | naming reference | never package |

## Excluded source behaviours

The original reference contains code that automates input/actions or changes client interaction rules. The new project excludes it entirely, including GhostHand, DelayRemover, AutoChest, AutoSwap, AutoStairs, Sprint, packet event interception, extension JAR loading, ESP variants, Freelook, ViewClip, auto-command features, and server-side/gameplay-affecting blacklist or report paths. The sole allowed exception is the bounded local UUID Blacklist: it only changes this MOD's local Tab/NameTag suffix, never sends an in-game command, packet, report, or enforcement action.

## Acceptance rules

- The deliverable contains no `wtf.tatp.meowtils` classes or `meowtils` Mixin config.
- The deliverable contains no code path that sends, cancels, delays, replays,
  or fabricates network packets. The sole server-command exception is a
  deliberate click on an attributable chat alert's `[WDR]` button, which uses
  Minecraft's ordinary command UI to run only `/wdr <validated raw name>`.
- The deliverable contains no synthetic key, mouse, inventory, movement, or chat action.
- A manually typed `.legitils` diagnostic command may be consumed by the local
  GUI before an outbound chat packet exists. It is not packet interception:
  no packet is constructed, sent, canceled, delayed, replayed, or fabricated;
  all non-matching input passes through unchanged. Its documented `anticheat`
  subcommands may atomically persist and immediately apply only the detector enable set after clearing partial detector timing state. All other configuration remains restart-owned.
- A documented manually typed `.legitils anticheat` command may atomically update the active detector set after a successful write. It never sends server chat and cannot change sensitivity, notifications, Mixin configuration, or any gameplay behavior.
- `[WDR]` is never automatic: it appears only for a current visible player with
  a valid raw Minecraft name, and the player must deliberately click it. The
  anonymous BedNuke signal, missing identity metadata, invalid names, report
  queues, and report history have no WDR path.
- Every alert is local-only and labels its output as a suspicion or observed anomaly.
- Each detector has deterministic unit-test traces for positive, negative, and global-lag cases.
