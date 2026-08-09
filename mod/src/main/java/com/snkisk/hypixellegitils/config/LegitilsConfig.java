package com.snkisk.hypixellegitils.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable startup configuration shared later with the macOS Companion. */
public final class LegitilsConfig {
    public static final int SCHEMA_VERSION = 8;
    public static final int STATS_SCHEMA_VERSION = 5;
    public static final int STATS_NAMETAG_SCHEMA_VERSION = 6;
    public static final int STATS_TAB_SORT_SCHEMA_VERSION = 7;
    public static final int STATS_AUTO_WHO_SCHEMA_VERSION = 8;
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int MARKER_SCHEMA_VERSION = 2;
    public static final int NICK_DETECTION_SCHEMA_VERSION = 3;
    public static final int PARTY_SCHEMA_VERSION = 4;
    public static final long DEFAULT_NORMAL_COOLDOWN_MILLIS = 1000L;
    public static final long DEFAULT_AIR_STALL_COOLDOWN_MILLIS = 30000L;

    public final int schemaVersion;
    public final long revision;
    public final Set<DetectorId> enabledDetectors;
    public final SensitivityPreset sensitivity;
    public final NotificationSettings notifications;
    public final long normalCooldownMillis;
    public final long airStallCooldownMillis;
    public final boolean debugEnabled;
    public final MarkerSettings markerSettings;
    public final NickDetectionSettings nickDetectionSettings;
    public final PartyDetectionSettings partyDetectionSettings;
    public final StatsSettings statsSettings;

    public LegitilsConfig(
        int schemaVersion,
        long revision,
        Set<DetectorId> enabledDetectors,
        SensitivityPreset sensitivity,
        NotificationSettings notifications,
        long normalCooldownMillis,
        long airStallCooldownMillis,
        boolean debugEnabled
    ) {
        this(
            schemaVersion, revision, enabledDetectors, sensitivity, notifications,
            normalCooldownMillis, airStallCooldownMillis, debugEnabled,
            MarkerSettings.defaults(), NickDetectionSettings.defaults(), PartyDetectionSettings.defaults()
        );
    }

    public LegitilsConfig(
        int schemaVersion,
        long revision,
        Set<DetectorId> enabledDetectors,
        SensitivityPreset sensitivity,
        NotificationSettings notifications,
        long normalCooldownMillis,
        long airStallCooldownMillis,
        boolean debugEnabled,
        MarkerSettings markerSettings
    ) {
        this(
            schemaVersion, revision, enabledDetectors, sensitivity, notifications,
            normalCooldownMillis, airStallCooldownMillis, debugEnabled,
            markerSettings, NickDetectionSettings.defaults(), PartyDetectionSettings.defaults()
        );
    }

    public LegitilsConfig(
        int schemaVersion,
        long revision,
        Set<DetectorId> enabledDetectors,
        SensitivityPreset sensitivity,
        NotificationSettings notifications,
        long normalCooldownMillis,
        long airStallCooldownMillis,
        boolean debugEnabled,
        MarkerSettings markerSettings,
        NickDetectionSettings nickDetectionSettings
    ) {
        this(
            schemaVersion, revision, enabledDetectors, sensitivity, notifications,
            normalCooldownMillis, airStallCooldownMillis, debugEnabled,
            markerSettings, nickDetectionSettings, PartyDetectionSettings.defaults()
        );
    }

    public LegitilsConfig(
        int schemaVersion,
        long revision,
        Set<DetectorId> enabledDetectors,
        SensitivityPreset sensitivity,
        NotificationSettings notifications,
        long normalCooldownMillis,
        long airStallCooldownMillis,
        boolean debugEnabled,
        MarkerSettings markerSettings,
        NickDetectionSettings nickDetectionSettings,
        PartyDetectionSettings partyDetectionSettings
    ) {
        this(schemaVersion, revision, enabledDetectors, sensitivity, notifications, normalCooldownMillis,
            airStallCooldownMillis, debugEnabled, markerSettings, nickDetectionSettings, partyDetectionSettings,
            StatsSettings.defaults());
    }

    public LegitilsConfig(
        int schemaVersion, long revision, Set<DetectorId> enabledDetectors, SensitivityPreset sensitivity,
        NotificationSettings notifications, long normalCooldownMillis, long airStallCooldownMillis, boolean debugEnabled,
        MarkerSettings markerSettings, NickDetectionSettings nickDetectionSettings,
        PartyDetectionSettings partyDetectionSettings, StatsSettings statsSettings
    ) {
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        EnumSet<DetectorId> detectorCopy = enabledDetectors.isEmpty()
            ? EnumSet.noneOf(DetectorId.class)
            : EnumSet.copyOf(enabledDetectors);
        this.enabledDetectors = Collections.unmodifiableSet(detectorCopy);
        this.sensitivity = sensitivity;
        this.notifications = notifications;
        this.normalCooldownMillis = normalCooldownMillis;
        this.airStallCooldownMillis = airStallCooldownMillis;
        this.debugEnabled = debugEnabled;
        if (markerSettings == null) throw new IllegalArgumentException("Marker settings are required");
        if (nickDetectionSettings == null) throw new IllegalArgumentException("Nick detection settings are required");
        if (partyDetectionSettings == null) throw new IllegalArgumentException("Party detection settings are required");
        if (statsSettings == null) throw new IllegalArgumentException("Stats settings are required");
        this.markerSettings = markerSettings;
        this.nickDetectionSettings = nickDetectionSettings;
        this.partyDetectionSettings = partyDetectionSettings;
        this.statsSettings = statsSettings;
    }

    public static LegitilsConfig defaults() {
        EnumSet<DetectorId> defaultDetectors = EnumSet.copyOf(DetectorId.implementedInCurrentBuild());
        // AutoBlock's Meowtils-compatible visible-state heuristic is still
        // awaiting normal sword-blocking validation, so new configurations
        // must opt in explicitly.
        defaultDetectors.remove(DetectorId.AUTO_BLOCK);
        // NoSlow has a complete static compatibility trace, but normal
        // item-use movement still needs a Lunar false-positive gate.
        defaultDetectors.remove(DetectorId.NO_SLOW);
        // LegitScaffold is likewise awaiting normal-bridging validation after
        // its tick-accurate compatibility rewrite.
        defaultDetectors.remove(DetectorId.LEGIT_SCAFFOLD);
        // BedNuke is still experimental because a normal block-in can match
        // its sealed-volume signal; it must be an explicit opt-in.
        defaultDetectors.remove(DetectorId.BED_NUKE);
        // KillAura now has a clean-room static contract, but normal consumable
        // use/combat still needs a Lunar false-positive gate.
        defaultDetectors.remove(DetectorId.KILL_AURA);
        // CombatDesync is a conservative visible stall-and-snap advisory
        // signal. It remains explicit opt-in until direct and proxied normal
        // combat traces establish an acceptable false-positive rate.
        defaultDetectors.remove(DetectorId.COMBAT_DESYNC);
        // AirStall intentionally includes remote F3+T-like visible stalls, but
        // requires Lunar support-state validation before default enablement.
        defaultDetectors.remove(DetectorId.AIR_STALL);
        defaultDetectors.remove(DetectorId.NO_BREAK_DELAY);
        return new LegitilsConfig(
            SCHEMA_VERSION,
            0L,
            defaultDetectors,
            SensitivityPreset.BALANCED,
            new NotificationSettings(true, false, false),
            DEFAULT_NORMAL_COOLDOWN_MILLIS,
            DEFAULT_AIR_STALL_COOLDOWN_MILLIS,
            false,
            MarkerSettings.defaults(),
            NickDetectionSettings.defaults(),
            PartyDetectionSettings.defaults()
        );
    }

    public boolean isDetectorEnabled(DetectorId detector) {
        return enabledDetectors.contains(detector);
    }
}
