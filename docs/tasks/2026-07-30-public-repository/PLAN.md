# Plan

## Objective

Prepare the clean-room Legitils source checkout for its first public GitHub repository.

## Scope

- Root README and ignore rules
- Initial Git repository, commit, and public GitHub repository creation
- Publication-focused review and build verification

## Non-goals

- Changing the MOD, loader, detector behaviour, or release status
- Redistributing local reference material or build caches

## Steps

1. Review the tree for files unsafe or inappropriate for a public repository.
2. Exclude local reference material, generated outputs, and unrelated assets.
3. Add an accurate README that states product boundaries and current verification limits.
4. Run publication and build checks, then perform a stable final review.
5. Create the initial commit and public `Legitils` repository only if blocking issues are resolved.

## Risks

- The checkout contains third-party reference material in `sample/`.
- The legacy ForgeGradle build requires Java 8 and a real Lunar Client smoke remains manual.
