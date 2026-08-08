package com.snkisk.hypixellegitils.config;

/** Live local presentation choices for normalized Stats Bridge data. */
public final class StatsSettings {
    public final boolean enabled;
    public final boolean tabEnabled;
    public final boolean starsEnabled;
    public final boolean fkdrEnabled;
    public final boolean winStreakEnabled;
    public final boolean chatEnabled;
    public final boolean nametagEnabled;
    public final double nametagFkdrThreshold;

    public StatsSettings(boolean enabled, boolean tabEnabled, boolean starsEnabled, boolean fkdrEnabled, boolean winStreakEnabled, boolean chatEnabled) {
        this(enabled, tabEnabled, starsEnabled, fkdrEnabled, winStreakEnabled, chatEnabled, false, 1D);
    }

    public StatsSettings(
        boolean enabled, boolean tabEnabled, boolean starsEnabled, boolean fkdrEnabled, boolean winStreakEnabled, boolean chatEnabled,
        boolean nametagEnabled, double nametagFkdrThreshold
    ) {
        if (Double.isNaN(nametagFkdrThreshold) || Double.isInfinite(nametagFkdrThreshold)
            || nametagFkdrThreshold < 0D || nametagFkdrThreshold > 1000D) {
            throw new IllegalArgumentException("Nametag FKDR threshold must be between 0 and 1000");
        }
        this.enabled = enabled;
        this.tabEnabled = tabEnabled;
        this.starsEnabled = starsEnabled;
        this.fkdrEnabled = fkdrEnabled;
        this.winStreakEnabled = winStreakEnabled;
        this.chatEnabled = chatEnabled;
        this.nametagEnabled = nametagEnabled;
        this.nametagFkdrThreshold = nametagFkdrThreshold;
    }

    public static StatsSettings defaults() {
        return new StatsSettings(true, true, true, true, true, true, false, 1D);
    }
}
