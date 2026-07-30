package com.snkisk.hypixellegitils.config;

/** A safe configuration load outcome that never prevents the MOD from starting. */
public final class ConfigLoadResult {
    public final LegitilsConfig config;
    public final boolean usedDefaults;
    public final String diagnostic;

    public ConfigLoadResult(LegitilsConfig config, boolean usedDefaults, String diagnostic) {
        this.config = config;
        this.usedDefaults = usedDefaults;
        this.diagnostic = diagnostic;
    }
}
