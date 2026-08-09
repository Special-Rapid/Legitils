package com.snkisk.hypixellegitils.stats;

import java.util.Collections;
import java.util.List;

/** Normalized display data only; no provider payload is retained. */
public final class StatsBridgePlayerResult {
    public final String name;
    public final NickStatus nickStatus;
    public final Integer stars;
    public final Double finalKillDeathRatio;
    public final Integer modeWinStreak;
    public final List<CommunityTag> communityTags;

    public StatsBridgePlayerResult(
        String name,
        NickStatus nickStatus,
        Integer stars,
        Double finalKillDeathRatio,
        Integer modeWinStreak,
        List<CommunityTag> communityTags
    ) {
        this.name = name;
        this.nickStatus = nickStatus;
        this.stars = stars;
        this.finalKillDeathRatio = finalKillDeathRatio;
        this.modeWinStreak = modeWinStreak;
        this.communityTags = Collections.unmodifiableList(communityTags);
    }

    public enum NickStatus {
        KNOWN,
        NICKED,
        UNAVAILABLE
    }

    public static final class CommunityTag {
        public final String source;
        public final String label;
        /** Sanitised, bounded provider explanation used solely by the local Chat hover component. */
        public final String tooltip;

        public CommunityTag(String source, String label) {
            this(source, label, null);
        }

        public CommunityTag(String source, String label, String tooltip) {
            this.source = source;
            this.label = label;
            this.tooltip = tooltip;
        }
    }
}
