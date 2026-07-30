package com.snkisk.hypixellegitils.config;

/** Minimal, non-sensitive status written by the MOD for a future Companion. */
public final class RuntimeStatus {
    public final String modVersion;
    public final long configRevision;
    public final boolean configUsedDefaults;

    public RuntimeStatus(String modVersion, long configRevision, boolean configUsedDefaults) {
        this.modVersion = modVersion;
        this.configRevision = configRevision;
        this.configUsedDefaults = configUsedDefaults;
    }
}
