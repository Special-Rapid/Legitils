# Local Proxy Relationship

## Decision

Hypixel Legitils is MOD-first. It is the complete product for anti-cheat observation, evidence scoring, UI, and local alerts; it has no runtime, build, installation, or release dependency on the local-proxy project.

The local proxy remains a separate product for its existing self-cosmetics, authentication, and packet-rewrite experiments. It must not receive new anti-cheat responsibilities.

## Why this is the correct boundary

- The MOD can observe the local world, player/entity state, block state, tick cadence, and lifecycle directly. Those are the evidence sources needed for conservative BedNuke and air-stall signals.
- A proxy primarily sees network traffic. Requiring it for the anti-cheat would entangle the product with encrypted-login/MITM setup and make normal Lunar installation harder.
- The anti-cheat needs a narrow observation-only Mixin surface. Proxy packet interception must not become a route for delaying, cancelling, replaying, fabricating, or otherwise controlling gameplay traffic.
- Independent packaging makes the main safety rule testable: a user can install the MOD alone and it cannot alter gameplay or network traffic.
- The only UI-input exceptions are a manually typed `.legitils` diagnostic
  command consumed before outbound chat packet creation, and a deliberate user
  click on an attributable alert's `[WDR]` chat component. The former only
  renders local status/usage text; the latter uses the normal Minecraft command
  UI for exactly `/wdr <validated raw name>`. Neither exception makes the MOD a
  packet-interception, automatic-report, or proxy-control surface.

## Responsibility split

| Area | Hypixel Legitils MOD | Local proxy |
| --- | --- | --- |
| Anti-cheat checks and evidence | owns all eight planned advisory checks | out of scope |
| Alerts, configuration, and future in-game UI | owns | out of scope |
| Client/world observation | owns through minimal Mixins | out of scope |
| Self-only cosmetic rewrites and proxy authentication | out of scope | retains existing scope |
| Runtime dependency or local bridge | none | none |

## Explicit non-integration

Do not merge the repositories, copy runtime classes, expose a listener/port, share a live evidence store, or add a MOD-to-proxy protocol for this plan. Each Minecraft client instance keeps its own local configuration and UUID-scoped observations; no cross-account or cross-process evidence sharing is planned.

If a genuinely separate use case later requires interoperability, write a new specification and threat model first. It must not change the MOD's standalone installation path or its observation-only safety boundary.
