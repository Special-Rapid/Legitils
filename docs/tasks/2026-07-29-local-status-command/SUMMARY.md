# Summary

The approved narrow input-path exception is implemented as `.legitils status`.
`MixinGuiChat` redirects only the Enter-triggered one-argument manual-send
call before Forge command routing and chat packet creation; all other text
passes through unchanged. The clickable two-argument RUN_COMMAND path is not
intercepted. It displays the existing bounded local status string and cannot
mutate configuration, evidence, gameplay, or server state.

Java 8 compilation, tests and refmap generation pass. Lunar manual smoke is
still required because Ichor mapping/application cannot be proven statically.
