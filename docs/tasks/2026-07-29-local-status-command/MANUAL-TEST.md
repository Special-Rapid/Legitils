# Lunar local command smoke

1. Build the current JAR and fully quit Lunar Client.
2. Restart Lunar using the existing Java agent argument; do not add a Forge
   command registration or change the Wrapper/Pre-Launch fields.
3. Join a server and enter `.legitils status` in chat.
4. Confirm that exactly one `Hypixel Legitils: anti-cheat X/Y detectors active`
   line appears locally and no server-visible message or unknown-command reply
   appears.
5. Enter ordinary chat, an unknown dot-prefixed message, and a normal server
   slash command. Confirm all three retain normal behavior.
   Clickable server-chat commands are also outside the local namespace and
   must retain their normal behavior.
6. Record the result and relevant `latest.log` excerpt in `RESULTS.md`.

## Rollback

If Lunar does not start or normal chat is affected, remove the Java agent JVM
argument to restore stock behavior. Do not try to work around a failed Mixin by
adding `ClientCommandHandler` registration.
