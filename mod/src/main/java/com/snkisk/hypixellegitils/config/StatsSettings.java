package com.snkisk.hypixellegitils.config;

/** Live local presentation choices for normalized Stats Bridge data. */
public final class StatsSettings {
    public final boolean enabled;
    public final boolean tabEnabled;
    public final boolean starsEnabled;
    public final boolean fkdrEnabled;
    public final boolean winStreakEnabled;
    public final boolean chatEnabled;

    public StatsSettings(boolean enabled, boolean tabEnabled, boolean starsEnabled, boolean fkdrEnabled, boolean winStreakEnabled, boolean chatEnabled) {
        this.enabled = enabled;
        this.tabEnabled = tabEnabled;
        this.starsEnabled = starsEnabled;
        this.fkdrEnabled = fkdrEnabled;
        this.winStreakEnabled = winStreakEnabled;
        this.chatEnabled = chatEnabled;
    }

    public static StatsSettings defaults() {
        return new StatsSettings(true, true, true, true, true, true);
    }
}
