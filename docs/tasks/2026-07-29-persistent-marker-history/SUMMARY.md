# Summary

## Result

Implemented a bounded, persistent Blacklist. A visible player's
UUID is automatically blacklisted after the configured number of accepted
flags while Auto Blacklist is enabled, or manually via a chat command.
Blacklisted visible players receive the yellow `§e⚠` Tab/NameTag suffix.

## Commands

- `.legitils …` and `.l …` are equivalent command prefixes.
- `.l blacklist on/off`
- `.l blacklist threshold <2-10>`
- `.l blacklist add <player>` / `.l blacklist remove <player>` — current-world
  UUID is used when available; otherwise the explicitly entered name is looked
  up asynchronously through Mojang before the edit.
- `.l blacklist status` / `.l blacklist list [page]`
- `.l blacklist clear all`
- `.l nickdetect on/off` — persist and immediately toggle the red `[NICK]`
  heuristic. Turning it off also clears Nick markers already shown in the
  current world; it does not print a separate Nick chat notice.
- `.l notify <chat|actionbar|sound> on/off` — immediately persist and apply an
  alert channel. Sound uses the built-in GUI press once per displayed alert.
- `.l dev on/off` — include the local player in checks for development. A self
  sample can alert, but cannot self-Blacklist or show `[WDR]`.
- `.l status` — show Anti-cheat detector count, Nick-detect, developer,
  Blacklist, and Chat/Action Bar/Sound notification state together.

`marker` commands remain a backward-compatible alias, but help and user-facing
messages now call the feature Blacklist.

Nicked UUID-version-1 profiles receive a red `§c[NICK]` in Tab and NameTag for
that world only; no separate chat notice, real-name recovery, or persistence
is performed.

Nick detection is stored separately in schema-3 `config.json`; schema-1 and
schema-2 configurations retain the earlier enabled behavior until a setting is
saved. Its setting never changes persistent Blacklist data.

## Data boundary

`~/Library/Application Support/HypixelLegitils/marker-history.json` is written
atomically and stores UUID, accepted count, blacklist state, update time, and
an optional cached non-nick Mojang/server-presented name for the local list. It
has a 256-entry limit; nick aliases, evidence text, chat, packets, and server
data are not persisted. A manual unavailable-player edit sends its entered name
once to Mojang's profile API; no automatic lookup/retry occurs.
At most eight different lookup operations may be pending; duplicate requests
are coalesced, and `blacklist clear all` cancels results that have not yet been
applied.

## Verification

- Java 8: `./gradlew :mod:test verifyBootstrapArtifacts` passed.
- Added restoration, write rollback, malformed-history, capacity-eviction,
  clear-and-reload, alias, local-command parsing, Mojang-response, Nick-setting
  migration, and Nick-setting conflict coverage.
- A real Lunar manual smoke remains pending.
