# Goal

## Objective

Add a conservative, locally attributable NoBreakDelay advisory detector and an
optional local `⚠` marker for players with repeated accepted attributable
alerts.

## Safety invariants

- NoBreakDelay emits only after repeated measured short inter-break intervals
  with complete actor, block-history, world-tick, and lag context. A missing or
  ambiguous input emits no evidence.
- Development mode uses a separate read-only local controller observation: a
  survival break that sets `blockHitDelay` to five must not reach zero before
  that window. Development self-evidence cannot create a Blacklist entry or a
  WDR action.
- The marker counts only `EvidencePolicy` decisions that were accepted and
  have a visible player UUID. Anonymous evidence, suppressed evidence, and
  cooldown rejections never count.
- Blacklist state is bounded and persistent by UUID; world changes reset only
  live detector timing. Rendering stays visible-player-only and never changes
  packets, chat input, targeting, or gameplay.
- Tab and NameTag rendering are read-only local suffixes and preserve the
  original text when no marker applies.

## Initial product decisions

- NoBreakDelay is default-disabled. The remote signal needs two confirmed,
  consecutive zero-tick-class block completions before it emits an advisory.
- The marker is default-disabled, uses a default threshold of three accepted
  attributable alerts, and applies the same setting to Tab and NameTag.
- A marker setting may apply immediately because it changes only local
  rendering; detector state, thresholds, and all other settings retain their
  documented boundaries.

## Stop conditions

- Pure-Java traces cover normal/missing/lagged mining observations and marker
  threshold/reset/anonymous cases.
- Java 8 tests and bootstrap-artifact verification pass.
- Tab and NameTag hooks have a separately documented Lunar manual gate.
