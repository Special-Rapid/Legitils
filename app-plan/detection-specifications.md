# Detection Specifications

## Common rules

- Every detector returns evidence, never a cheat verdict.
- No detector automatically causes a report, blacklist change, packet operation,
  or gameplay action. An accepted attributable chat alert may offer a manual
  `[WDR]` button; only a deliberate user click runs `/wdr <validated raw name>`.
  Anonymous evidence, including BedNuke, has no such button.
- Evidence is invalidated or reduced during global lag, world transition, unknown player state, or insufficient observation history.
- Normal alerts use a short detector label (`AutoBlock`, `LegitScaffold`,
  `BedNuke`, and so on), not confidence or raw evidence prose. This is a local
  advisory label, not a factual cheat verdict. Attributed evidence may prepend
  the currently visible player name; anonymous evidence must not invent one.
- A normal alert may reappear after one second for the same detector/player. Air-stall alone has a thirty-second re-alert cool-down.
- The Action Bar is empty while idle. Chat is the default local alert output. If the optional Action Bar alert is enabled, a newly emitted advisory observation is shown only while no server Action Bar is active; Lunar's server Action Bar remains untouched.
- `.l notify <chat|actionbar|sound> on/off` changes each alert channel immediately and persists it. Sound is the vanilla `gui.button.press` sound, played once for each alert selected for display on the client thread. When several alerts arrive in one client tick, the existing presentation model intentionally shows only the newest one. `.l dev on/off` is a development-only self-observation switch: it can generate an advisory for the local player, but never creates a Blacklist entry or a `[WDR]` button for that player.
- All MOD chat groups begin with the mock-up's rainbow `§7[§fL§9e§1g§5i§dt§ci§6l§es§7]` prefix; subsequent lines use an indented continuation, so settings and help output do not become one wrapped line.

## Blacklist visual marker

The MOD offers an optional yellow `§e⚠` suffix beside a visible player's Tab-list entry and in-world name tag. It is a Blacklist indicator, never a verdict or report. While Auto Blacklist is enabled, an accepted attributable alert increments that UUID's count; when it reaches the configured threshold, the UUID is automatically added to the Blacklist. Suppressed/global-lag/incomplete/cool-down-rejected evidence and anonymous events cannot count, so the current anonymous BedNuke signal cannot create an automatic entry. A user may manually add or remove any valid Java player name: the MOD first uses a UUID already loaded in the current world, otherwise it makes one asynchronous request to Mojang's profile API. The lookup sends only the explicitly entered name, does not retry automatically, and 404/rate-limit/service errors leave the Blacklist unchanged. The bounded 256-entry history persists UUID, count, blacklist state, timestamp, and optional non-nick cached canonical/server-presented names in atomically written `marker-history.json`; it stores no nick aliases, evidence text, chat, or packets and is never sent to the Minecraft server. Cached names make `.l blacklist list [page]` readable without calling Mojang again; a name can therefore be stale until the player is next resolved or seen. World changes reset only live detection timing. Rendering remains current-visible-player-only. It is default-disabled with a threshold of three accepted alerts, one count per accepted alert, and no detector weighting. Use `.legitils blacklist …` or its `.l …` alias; legacy `.legitils marker …` remains a compatibility alias. `.l blacklist clear all` erases every stored entry.

## Nicked-profile session marker

When the server-presented GameProfile UUID is version 1, the MOD treats it as a nicked profile using the same client-visible rule as the reference implementation and appends a red `§c[NICK]` to that currently visible profile's Tab entry and NameTag. It does not print a separate Nick chat notice. `.l nickdetect on/off` persists and applies this setting immediately; disabling it clears existing Nick markers without changing Blacklist data. The marker lasts only for the current world; nicked profiles are not stored in `marker-history.json`, do not enter the persistent Blacklist from automatic flags, and are never de-nicked or mapped to a real identity. `.l status` displays Anti-cheat, Nick-detect, Developer self-detect, Blacklist, and Chat/Action Bar/Sound notification state together.

## Existing four check families

### Meowtils behavioural compatibility policy

AutoBlock, NoSlow, KillAura, and Legit Scaffold must follow the supplied
Meowtils checks' observable conditions, world-tick timing, thresholds,
state-reset and detector cool-down semantics through a clean-room
implementation. The current sample/millisecond implementations are not
behaviourally compatible and require replacement. Compatibility does not copy
source code, inherit a reference bug, or include automatic WDR, blacklist,
reporting, or punishment behaviour. The separately approved manual WDR chat
button is limited to a user click on an attributable alert. BedNuke has no
supplied Meowtils counterpart and remains an original, separately validated
signal.

### AutoBlock

Track eleven uninterrupted client-world tick samples of visible swing state and
blocking state. Reset when either state is absent or the local observation
stream is missing/stalled. This is the clean-room core equivalent of the
Meowtils signal; the product's global-lag/world-transition and local-alert
cooldown guards remain separate.

**Quality gate (open):** The identical visible overlap can occur during normal
sword blocking. AutoBlock is disabled in new/default configurations until a
recorded Lunar normal-play trace proves its false-positive rate acceptable. An
existing configuration that explicitly enables it remains an explicit opt-in.

### NoSlow

For each visible non-local player, compare the horizontal distance between
consecutive client-world ticks. While the player is sprinting, using an item,
and not riding, a sample is anomalous only when it is strictly above
`0.05 * Speed-adjustment`; emit after 21 uninterrupted anomalous ticks and
then reset the streak. Airborne observations remain eligible, matching the
reference contract. The implementation stores the previous accepted position on
every tick before evaluating the next one, deliberately correcting the
reference's missing prior-position update.

**Quality gate (open):** NoSlow is disabled in new/default configurations
until normal Lunar item-use movement and a controlled, consented comparison
trace are recorded. An existing configuration that explicitly enables it
remains an explicit opt-in. See [the compatibility
task](../docs/tasks/2026-07-28-noslow-compatibility/PLAN.md).

### KillAura

For each visible non-local player on continuous world ticks, count use duration
only for food, normal potion, and milk-bucket items. A violation tick requires
an active reference-equivalent attack animation (`swingProgressInt > 0`), more
than six ticks of current consumable use, and a completed consumable-use
sequence fewer than 33 ticks ago. Violation level rises by one only on that
combined condition, decays by one on every other tick, and alerts on level
eight before clearing the pattern. Nearby hurt-animation and generic
item-use/combat proxies are not used.

**Quality gate (open):** KillAura is disabled in new/default configurations
until normal consumable use/ordinary combat and a controlled, consented
comparison trace are recorded. An existing explicit configuration remains
opt-in. See [the compatibility
task](../docs/tasks/2026-07-28-killaura-compatibility/PLAN.md).

### Legit Scaffold

Observe only visible sneak transitions, swing timing, block holding, pitch, and ground context. Require multiple short, consistent sneak cycles within a scaffold-like context and apply a cool-down. The alert describes a `repetitive scaffold timing anomaly`; it must not treat ordinary bridging as a verdict.

The clean-room implementation uses verified world-tick crouch durations, tight
swing-to-crouch timing, bounded per-player state and a 60-tick cool-down.

**Quality gate (open):** LegitScaffold is disabled in new/default
configurations until controlled Lunar normal-bridging traces confirm an
acceptable false-positive rate. An existing configuration that explicitly
enables it remains an explicit opt-in. See [the compatibility task](../docs/tasks/2026-07-28-legitscaffold-compatibility/PLAN.md).

## NoBreakDelay cheat-detection signal

### Priority and goal

This is a default-disabled anti-cheat detector. Its purpose is to detect and
locally notify on a repeated, locally attributable
NoBreakDelay-style block-breaking cadence that is inconsistent with
conservatively observed normal break-delay behaviour. It sends the result
through `EvidencePolicy` and the existing local alert path, and describes a
`mining cadence anomaly`, never a definitive cheat verdict.

### Working behaviour model

The implemented remote target is a NoBreakDelay-style pattern in which two
successive new break animations begin within one world tick of the preceding
confirmed break. Each animation must reach stage 9 and be matched to a
server-applied air update at the same block. This is deliberately stricter
than the reported approximately-five-tick local delay and remains default-off
until tool, enchantment/effect, game-mode, server and block-specific traces
are measured in Lunar.

While `.l dev on` is active, the local player is tested through a separate,
read-only `PlayerControllerMP.blockHitDelay` observer rather than the remote
S25 path. A normal survival completion sets this value to five; reaching zero
before that five-tick window is a direct self-test signal. This development
path never adds the local account to the Blacklist and never offers `[WDR]`.

### Required evidence inputs

- actor-resolved S25 block-break progress and resulting server-applied
  block-state transitions at the same position;
- a bounded block-history snapshot around the observed break;
- reliable actor attribution and the actor's visible tool/context when the
  client can establish it;
- world-tick continuity and a global-lag baseline.

### Safety conditions

The detector must not alert from anonymous block updates, a single fast break,
one apparent zero-delay interval, unknown block history, incomplete actor
attribution, server lag, world transitions, creative-mode-like mechanics, or
expected tool/enchantment/effect behaviour. It must require repeated intervals
that fall below the measured conservative baseline. If the client cannot
reliably associate the cadence with one visible player, it may record local
debug context but emits no evidence. The initial implementation also excludes
creative state, Haste, enchanted tools, non-full blocks, unloaded blocks, and
a player farther than the packet's normal local visibility range.

## BedNuke signal

### Goal

Warn about an **unassigned blocked-bed-break anomaly** when a locally observed
bed destruction occurs while a complete, bounded 3D defense snapshot has no
open route from its exterior to the bed. The alert does not name or infer a
breaker.

### Evidence inputs

- a complete, loaded local block-state cuboid around both bed halves, captured
  before the first server-applied bed-removal state;
- subsequent server-applied block updates for that cuboid;
- a bounded-time pair of non-bed states for both halves;
- a six-direction flood-fill from the cuboid exterior after a short settling
  window.

### Alert threshold

Require all of the following: the cuboid is completely loaded, both bed halves
change from bed within the short window, no chunk/world transition or global
lag occurs, and flood-fill finds no open exterior-to-bed route after the
settling window. An unavailable/partial cuboid, late second half, or any
transition ambiguity produces no alert. Attribution is deliberately absent,
not guessed.

### Known limitations

Explosions, chunk visibility, delayed block updates, and server-side custom
mechanics can resemble this anomaly. This is advisory-only and is not a cheat
verdict or a player accusation.

**Quality gate (open):** A normal block-in can create the same final sealed
geometry as the current rule. The replacement must combine complete 3D history
with a temporal entry/occupancy ambiguity guard and must prove no-alert for a
controlled block-in before it is enabled by default. BedNuke is disabled in
new/default configurations until then. It remains anonymous.

## Blink signal (combat-correlated desync)

### Goal

Warn about repeated player-specific update stalls that occur during combat and are not explained by global lag. This describes the observation; it does not distinguish Blink from F3+T, client freeze, or other causes.

### Evidence inputs

- per-player observed update cadence and position deltas;
- rolling non-combat baseline for that same player;
- combat-context markers such as proximity plus visible swing/damage context;
- nearby-player cadence median as a global-lag baseline.

### Alert threshold

Require repeated episodes where one player stalls or resumes with an anomalous delta primarily during combat, while nearby players continue updating normally and the same player has a materially lower non-combat anomaly score. A single episode never alerts.

### Known limitations

Client interpolation, server correction, packet loss, and resource reloads can look similar. The alert wording must say `combat-correlated desync anomaly`.

## Timer signal (air-stall)

### Goal

Warn when a player remains unexpectedly stationary in mid-air for a sustained observed interval.

### Evidence inputs

- recent position samples;
- locally visible support blocks, liquids, ladders, vehicles, and ground state;
- global lag baseline and world tick continuity.

### Alert threshold

Require continuous samples over a configurable duration with no visible support explanation, no global-lag condition, and no world transition. It is a low-confidence signal by default and needs a longer cool-down than the other checks.

### Known limitations

This cannot establish use of a Timer or Flight client. It only reports an observed air-stall pattern, including patterns that may be externally similar to F3+T/resource-reload stalls.

**F3+T policy:** A remote player's F3+T-like visible air stall is an intended
`Timer` advisory positive, not a false positive to filter out. The detector
must still suppress a freeze/reload of the observing client itself, global lag,
world transitions, unloaded support, and ambiguous local support state.
