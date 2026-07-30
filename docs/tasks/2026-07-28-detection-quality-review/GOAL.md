# Goal

## Objective

Record the observed anti-cheat quality problems before changing detector code:
overlong alert wording, AutoBlock false positives, unreliable Legit Scaffold
alerts, and BedNuke's block-in false positive.

## Decisions already made

- The four Meowtils-corresponding checks (AutoBlock, NoSlow, KillAura, and
  Legit Scaffold) use Meowtils' observable detection behaviour as their
  compatibility target. The current independently chosen millisecond/sample
  rules are not the target going forward.
- Compatibility means matching the observed conditions, world-tick windows,
  thresholds, state-reset rules and per-check cool-down semantics in a
  clean-room implementation. It does not mean copying source code, retaining
  an identified reference bug, or adopting blacklist, automatic WDR, or
  report behaviour.
- Alerts are local advisory output only. They must never punish or automate an
  action. The later, separately approved `[WDR]` component is a deliberate
  user click on an attributable alert, not a detector-driven report.
- The visible alert text should be short and use the detector/cheat label,
  rather than exposing internal confidence or evidence prose.
- A detector that is currently producing known false positives must be
  recalibrated or disabled by default before release; a passing synthetic unit
  test is not sufficient proof of normal-play accuracy.
- BedNuke remains anonymous. A block update alone does not reliably identify a
  breaker, so a name must not be guessed.
- BedNuke has no Meowtils reference implementation and remains an original,
  separately validated detector; it is not part of the four-check
  compatibility rewrite.

## Stop condition

This is a research and documentation task. It is complete when the current
behaviour, its evidence, and the validation gate for each proposed replacement
are recorded. It does not authorise detector or notification-code changes.
