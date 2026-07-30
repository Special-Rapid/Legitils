# Goal

## Objective

Allow a user-entered `.legitils anticheat` command to inspect and persist the
five current detector enablement choices without editing JSON manually.

## Command contract

- `.legitils anticheat list` shows the current-session active count and the
  current detector configuration.
- `.legitils anticheat on <detector|all>` and `off <detector|all>` update only
  the local configuration file.
- Supported detector tokens are `AutoBlock`, `NoSlow`, `KillAura`,
  `LegitScaffold`, `BedNuke`, and `all`.
- Every successful update persists and immediately applies the detector set after clearing partial detector timing state. All non-detector configuration remains restart-owned.

## Safety invariants

- Only manually entered `.legitils` text may change the local file.
- No command is sent to the server and no packet/input/gameplay behaviour is
  changed.
- The existing strict schema, atomic replacement write, revision increment,
  and implemented-detector allowlist remain in use.

## Non-goals

- Detector tuning, notification setting changes, server commands, or Companion-app replacement.

## Stop condition

Parser, atomic persistence, immediate runtime application, and Java 8 artifact tests pass; Lunar manual smoke proves local-only command output and immediate status update.
