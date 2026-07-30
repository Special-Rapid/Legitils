# Goal

## Objective

Persist a local UUID Blacklist so a manually or automatically blacklisted
player receives the same yellow `§e⚠` Tab/NameTag suffix after a later world join
or a later client launch.

## Safety invariants

- Store UUID, accepted-count, manual/auto blacklist state, update time, and an
  optional cached non-nick canonical/server-presented name in a separate local
  history file; never store nick aliases, chat, packets, or evidence prose. A
  manually entered name may be sent once to Mojang's profile API only after the
  user explicitly invokes add/remove.
- Keep the bounded persistent history separate from strict `config.json`.
- A malformed or unavailable history file starts empty and must never block
  Minecraft startup.
- World changes clear only live observation timing, not persistent blacklist
  entries. The user can clear every stored entry explicitly.
- Blacklist rendering remains local-only, read-only, and visible-player-only.
- Nick detection is a separate setting. Disabling it immediately removes its
  current-world Nick markers without changing Blacklist data.

## Initial decisions

- Use an atomically written `marker-history.json` sibling under HypixelLegitils
  Application Support.
- Retain at most 256 UUID entries, evicting the least recently updated entry.
- Automatically add an entry after the configured number of accepted flags;
  manual and automatic entries use the same local yellow suffix.
- Add `.legitils` and `.l` aliases. Use `.l blacklist clear all` as an
  explicit erasure command.
- Add immediate `.l nickdetect on/off`, `.l notify <channel> on/off`, and
  `.l dev on/off` controls. `.l status` shows detector, Nick, development,
  Blacklist, and notification state.
