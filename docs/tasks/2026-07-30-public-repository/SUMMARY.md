# Summary

Prepared the first public Legitils repository without altering MOD behaviour.

- Added a root README that states the observation-only boundary, alpha status, build command, and manual validation limit.
- Excluded local reference material, Gradle caches, generated output, logs, macOS metadata, and unrelated MirrorProxy logo assets from Git.
- Corrected publication documentation to distinguish the implemented bootstrap from planned Companion, detector, and direct/proxied release validation.
- Ran `./gradlew build` with Java 8 and the repository-local Gradle cache; the build and artifact checks passed.
- Checked staged files for whitespace errors, personal absolute paths, known credential patterns, and excluded paths; all checks passed.
- Final review found no remaining publication blocker.
- Published the `main` branch to the public `Special-Rapid/Legitils` GitHub repository.

The public repository is source and documentation only. Real Lunar normal-play, direct/proxied, and release verification remain future gates.
