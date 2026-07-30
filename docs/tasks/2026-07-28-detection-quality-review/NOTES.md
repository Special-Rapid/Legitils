# Detection Quality Review

## Observed status — 2026-07-28

| Area | Observed result | Current status |
| --- | --- | --- |
| Alert text | Chat output formerly exposed confidence plus a long internal evidence sentence. | Replaced on 2026-07-29 by the mock-format local flag presentation. |
| AutoBlock | Normal play can flag repeatedly. | Not release-ready; recalibration required. |
| Legit Scaffold | Tick-accurate clean-room core and automated traces are complete; normal-bridging Lunar validation is pending. | Default disabled pending manual false-positive gate; player-name presentation remains separate work. |
| BedNuke | It has not received a controlled positive test. A normal block-in can trigger its sealed-volume condition. | Experimental; do not treat as a reliable default detector yet. |

## Compatibility decision

The four checks that have Meowtils references — **AutoBlock, NoSlow, KillAura,
and LegitScaffold** — will be unified to Meowtils' *observable behaviour*.
The current code is not a compatible implementation: all four differ in at
least their input contract, time unit/window, threshold, state machine, or
post-flag handling.

This is a clean-room compatibility target, not a source-copying target. The
product keeps its local-only boundary: no automatic WDR/report command,
blacklist,
punishment, packet operation, or gameplay automation. It also does not retain
the reference NoSlow implementation's missing previous-position update; the
equivalent intended movement comparison must be implemented correctly.

BedNuke has no corresponding Meowtils check in the supplied reference material,
so it remains an original experimental detector and is governed by its own
quality gate below.

### Compatibility matrix

| Detector | Meowtils behaviour to reproduce cleanly | Current incompatibility to remove |
| --- | --- | --- |
| AutoBlock | Tick-by-tick blocking plus attack-animation overlap, consecutive violation progression, reference threshold (>11 ticks). | Wall-clock sample continuity, swing-edge requirement, and preset overlap values (5–12) do not match. |
| NoSlow | Sprinting/item-use/non-riding movement with Speed adjustment and reference-length consecutive progression (>21 ticks). | Ground-only requirement, time-normalised `0.095 blocks/tick` threshold, 150 ms continuity, and 4–9-sample presets do not match. Preserve the intended previous-position comparison rather than the reference bug. |
| KillAura | Consumable-item-use attack sequence, item-type guard, use duration, recent-eating window, and violation threshold (8). | Completed clean-room rewrite; Lunar normal consumable-use/combat validation remains. |
| LegitScaffold | World-tick crouch start/end, 1–2 tick latest crouch, three crouches at most 3 ticks, swing 0–3 ticks after crouch end, 60-tick cool-down. | Completed clean-room rewrite; Lunar normal-bridging validation remains. |

The reference-side friend/team exclusions and player-facing violation handling
must be reviewed separately before adoption. They are not implicitly included
by detector compatibility.

## 1. Short alert presentation

Current `Evidence.advisoryText()` constructs output such as
`[HypixelLegitils] low confidence: repetitive scaffold timing anomaly`.
Confidence and raw observation prose are useful for tests/debugging, but they
are not useful as the normal player-facing notification.

### Selected presentation contract

- Attributed evidence: `§7[§fL§9e§1g§5i§dt§ci§6l§es§7] <visible formatted player name> §cflagged <coloured detector> §7| §4[WDR]`.
- Anonymous evidence: `§7[§fL§9e§1g§5i§dt§ci§6l§es§7] §cflagged <coloured detector>`.
- `[WDR]` is a separately clickable component only for a current visible player
  with a validated raw Minecraft name; a deliberate user click runs exactly
  `/wdr <name>`. It is not automatic reporting, and anonymous BedNuke has no
  button.
- The label is a short local warning name, not a factual verdict. Raw evidence
  and confidence stay internal and may be exposed later only through a
  diagnostic view.
- The display name must be resolved from the current visible-player observation
  at presentation time. If that mapping is absent or stale, omit the name; do
  not reconstruct or guess one from a UUID.

### Follow-up implementation and tests

1. Add a display-label mapping for `DetectorId` and make the alert sink use it.
2. Carry the evidence UUID only to presentation, then resolve it against the
   current visible-player list rather than storing names in evidence.
3. Test attributed, unknown-name, and anonymous BedNuke output. Verify that
   output never contains `null`, a UUID, confidence prose, or a guessed name.

## 2. AutoBlock false positives

### What the current detector actually does

`AutoBlockSignalCheck` counts every observed sample where `blocking` and
`swinging` are both true, then alerts at the configured overlap-sample and
swing-edge thresholds. The normal-play test only covers `blocking == false`.
It does **not** prove that ordinary sword-blocking combat cannot overlap with
the client swing animation.

This means the current signal establishes only animation/state overlap; it
does not establish an impossible attack/block sequence. That is consistent
with the reported false positives.

### Required decision before code work

The current AutoBlock rule must be replaced with the Meowtils-compatible
tick-level state progression and then evaluated against real normal-combat
traces. Raising the current numeric threshold alone is not a solution: it can
hide the reported false positives while retaining the wrong input contract.

### Replacement research gate

The replacement must first verify that the reference's tick-level blocking and
attack-animation inputs have the same 1.8.9/Lunar semantics in local samples.
It must use the reference-compatible consecutive progression, add reset guards
for missing state/world transition/global lag, and have tests for normal sword
blocking **with** swing animation. If those inputs cannot be observed
reliably, the detector stays disabled rather than using a looser substitute.

## 3. Legit Scaffold — current implementation versus sample behaviour

### Current clean implementation

The product currently uses millisecond samples: holding a block, on ground,
pitch 55–90 degrees, then multiple sneak state toggles no more than 400 ms
apart plus swing rising edges. It requires neither an individual crouch
duration nor a tight swing-to-crouch relationship. Its synthetic positive
trace is deliberately constructed from alternating 50 ms values.

That is materially different from the reference behaviour, so it is expected
that it can flag a different set of players and fail to match the reference's
practical results.

### Reference behaviour to use as a clean-room behavioural target

`sample/anticheat/checks/LegitScaffoldCheck.java` uses **world ticks**, not
loose millisecond toggle spacing. Its observable rule is:

1. visible non-local player holds a block, is grounded, and pitches down at
   least 60 degrees;
2. record the start and end of each crouch;
3. require a just-finished crouch of 1–2 ticks;
4. require the last three crouches to be at most 3 ticks each;
5. require a swing beginning from the crouch end through three ticks later,
   with the swing still recent; and
6. apply a 60-world-tick per-player cool-down.

This is a behavioural description only. Product code must be written from
first-party requirements and verified data; no reference source is copied.

### Clean compatibility rewrite plan

1. Verify that Lunar's visible-player sampling can provide a stable world tick,
   crouch edge, and swing-progress edge with the reference semantics above.
2. Implement a bounded per-visible-player tick state machine with world-reset,
   despawn, missing-sample and global-lag invalidation.
3. Require the tight crouch-duration and swing timing relationship; do not use
   broad toggle cadence as a proxy.
4. Add trace tests for each near miss: long crouch, three inconsistent crouches,
   early/late swing, no block, low pitch, airborne state, missing sample, and
   cool-down.
5. Perform controlled Lunar comparison traces before enabling it by default.

## 3a. NoSlow and KillAura compatibility work

NoSlow and KillAura are also currently distinct from their Meowtils references,
even though they have not yet been the reported false-positive focus. They
must be included in the same rewrite rather than left on the current generic
sample rules.

- **NoSlow:** Change only after the intended reference movement behaviour,
  Speed adjustment and >21-tick progression have been stated as a testable
  clean contract. Keep the correctly maintained previous position and add
  missing-state/global-lag resets.
- **KillAura:** Add verified consumable type and use-duration inputs before
  implementing the reference-compatible eating/attack sequence. Do not treat a
  nearby hurt animation as equivalent to an eating attack.
- Both checks require normal and adversarial comparison traces before their
  default enablement is restored.

## 4. Why player names are currently absent

`PlayerSample` carries a UUID and the detector correctly attaches that UUID to
ordinary visible-player evidence. `Evidence.advisoryText()`, however, has no
display-name resolver, so it outputs only generic text. This is a presentation
gap, not evidence that the detector has no player identity.

BedNuke is different: a server-applied local block-state update contains no
reliable breaker identity. It deliberately carries no UUID and must remain an
anonymous message even after normal alerts gain display names.

## 5. BedNuke and legitimate block-in

### Current false-positive mechanism

The current BedNuke detector snapshots the defense when the first bed half
changes from bed, waits for both halves to disappear, then alerts if no open
six-direction path leads from the snapshot exterior to the former bed. A
legitimate player can enter the defense, close the entrance with blocks, and
break the bed while inside. The final sealed geometry is then indistinguishable
from the current detector's target pattern, which explains the replayed
block-in false positive.

### Rejected as the sole fix

Suppressing every event after a recent `OPEN -> SOLID` block update is safe
against this false positive but too broad: it would also conceal a BedNuke
whenever ordinary players place blocks near the defense. It can be a fallback
ambiguity guard, not the only detector logic.

### Preferred next design: temporal entry proof plus geometry

BedNuke should alert only if all of these independent conditions hold:

1. a complete, loaded, bounded defense history covers the relevant interval;
2. the normal exterior-to-bed route is closed at break time;
3. the defense history contains the target discontinuity (outer defense
   changes while the bed is removed without a normal internal route); and
4. no currently visible player has a plausible legitimate occupancy history:
   entering/crossing the defense boundary while an opening existed, then being
   inside or immediately adjacent to the defense when the bed breaks.

If a player may have entered, if their position history is missing, if the
volume is partial, or if a chunk/world/global-lag transition occurs, the result
is **no alert**. This preserves the anonymous output decision: position history
is only an ambiguity guard, never breaker attribution.

### Test gate before shipping

Record and test, in a controlled private scenario:

- an ordinary open-route bed break: no alert;
- a player enters, block-ins, and breaks the bed: no alert;
- an outside player places unrelated blocks near the defense: no alert caused
  solely by placement;
- a fully observed target-like obstructed break with no plausible internal
  occupant: anonymous alert, if the event can be reproduced safely;
- missing player history, partial cuboid, delayed halves, chunk/world changes,
  and global lag: no alert.

Until that gate passes, BedNuke remains an unverified experimental advisory.
