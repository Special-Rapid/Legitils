# Notes

## 2026-07-28 Lunar command compatibility finding

The ForgeGradle development classpath exposes deobfuscated
`ClientCommandHandler.registerCommand`. Lunar's shipped `Forge_v1_8.jar`
contains an obfuscated `ClientCommandHandler` API instead. Registering the
development command from bootstrap is not a safe Lunar runtime dependency and
is removed from this build after the startup regression. It is replaced by the
separately scoped `.legitils status` local GUI command adapter, which does not
use Forge command registration. Its Lunar manual smoke remains tracked in the
[local command task](../2026-07-29-local-status-command/MANUAL-TEST.md).
