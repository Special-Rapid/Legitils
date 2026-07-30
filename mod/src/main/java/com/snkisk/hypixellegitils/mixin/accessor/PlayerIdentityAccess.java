package com.snkisk.hypixellegitils.mixin.accessor;

import java.util.UUID;

/**
 * Runtime-safe profile identity access supplied by the EntityPlayer mixin.
 *
 * Lunar's legacy classes do not expose Forge's reobfuscated getUniqueID method,
 * so callers must not invoke it directly.
 */
public interface PlayerIdentityAccess {
    UUID hypixelLegitils$getProfileId();
}
