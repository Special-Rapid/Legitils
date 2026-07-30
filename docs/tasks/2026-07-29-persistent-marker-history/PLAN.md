# Plan

## Objective

Replace session-only marker counts with a bounded local UUID Blacklist without
weakening configuration parsing. Manual edits may resolve a Java profile name
to UUID through Mojang only after an explicit user command.

## Scope

- New history path/store with strict JSON parsing and atomic replacement.
- Auto/manual Blacklist restoration, persistence, and world-reset behavior.
- `.l` alias, local Blacklist commands, asynchronous Mojang UUID resolution,
  tests, and product documentation.

## Non-goals

- Evidence logs, cross-device sync, automatic reporting, background lookup,
  or any non-Mojang network communication.

## Steps

1. Keep strict configuration separate from history data.
2. Restore history at bootstrap and persist after each accepted attributable
   alert, automatic Blacklist registration, manual edit, or explicit clear.
3. Keep rendering visibility checks unchanged while retaining UUID history over
   world changes and launches.
4. Add malformed-file, atomic-write, restore, eviction, and clear tests.
5. Resolve a manually supplied name asynchronously only when the current world
   cannot supply its UUID; persist the returned UUID and cache its canonical
   Mojang name for the local Blacklist list.
6. Cancel stale pending lookups when the user clears the local Blacklist and
   bound/deduplicate outstanding requests.
7. Treat UUID-version-1 nicked profiles as a separate current-world state:
   show red `[NICK]` without a separate chat notice; never persist or de-nick
   the identity.
8. Add a schema-3 Nick-detection toggle that applies immediately, clears the
   current session marker on disable, and appears with detector and Blacklist
   state in `.l status`.
9. Add immediate, persisted alert-channel and development self-observation
   commands. Development samples may alert but must never self-Blacklist or
   expose a WDR action.

## Risks

- Persistent blacklist entries may outlive a false positive, so the erasure command is
  required.
- The history file must never be treated as authoritative server information.
- The Mojang lookup can be unavailable or rate-limited, so it must never run
  on the client thread or retry automatically.
- Nick detection must not become identity disclosure: only the server-presented
  display name may be shown, and it must not be written to history.
