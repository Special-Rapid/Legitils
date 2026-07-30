# Goal

## Objective

Show one local `Legitils Injected!` chat notice after a client world becomes
available, so the user can immediately confirm that the MOD's Minecraft Mixin
is active for that session.

## Safety invariants

- The notice is inserted locally with `addChatMessage`; it is never sent to a
  server.
- It appears at most once per loaded client world and resets only when that
  world is unloaded/replaced.
- It does not change detector, configuration, packet, or input behaviour.

## Stop condition

The Java 8 build passes and Lunar manual smoke shows one local notice after
joining a world.
