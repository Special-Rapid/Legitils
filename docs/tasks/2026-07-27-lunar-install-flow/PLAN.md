# Plan

## Objective

Define a reproducible and reversible Lunar Client 1.8.9 installation path, delivered through a native macOS companion app plus anti-cheat MOD, that works for both direct Hypixel connections and local-proxy connections.

## Scope

- `app-plan/installation-and-update.md`
- `app-plan/companion-app.md`
- `app-plan/use-technology/build-and-packaging.md`
- `app-plan/README.md`
- `app-plan/implementation-checklist.md`

## Non-goals

- Do not implement an installer or modify Lunar Client in this task.
- Do not redistribute, fork, or bundle the third-party `curxxed-mc/lunar-agent` without an explicit license.
- Do not add a proxy dependency, proxy IPC, or packet access.

## Steps

1. Define a clean-room, first-party Java 8 loader and its packaging boundary.
2. Define the native macOS 13+ Companion, Developer ID signing/notarization, and no-injection UX.
3. Specify the manual alpha installation, verification, update, and uninstall flow.
4. Define a restart-only JSON settings contract between the macOS app and MOD.
5. Deliver all seven detectors and direct/local-proxy verification in the first release.

## Risks

- Lunar Client's current JVM-argument UI and runtime Java version are not confirmed by Lunar's own documentation.
- The external Agent is not licensed for redistribution and may change independently.
- A proxy can reduce observation fidelity; detector policy must suppress uncertain evidence.
