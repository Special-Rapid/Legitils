package com.snkisk.hypixellegitils.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/** Persists detector choices; the bootstrap applies a successful update to the running detector set. */
public final class DetectorSettingsService {
    private final LegitilsConfigStore store;
    private final Path configPath;
    private LegitilsConfig savedConfig;

    public DetectorSettingsService(LegitilsConfigStore store, Path configPath, LegitilsConfig startupConfig) {
        if (store == null || configPath == null || startupConfig == null) throw new IllegalArgumentException("Detector settings require store, path, and config");
        this.store = store;
        this.configPath = configPath;
        this.savedConfig = startupConfig;
    }

    public synchronized LegitilsConfig savedConfig() {
        return savedConfig;
    }

    public synchronized Update setEnabled(DetectorId detector, boolean enabled) throws IOException {
        if (detector == null || !detector.isImplementedInCurrentBuild()) {
            throw new IllegalArgumentException("Detector is not configurable in this build");
        }
        EnumSet<DetectorId> enabledDetectors = copyEnabledDetectors();
        boolean changed = enabled ? enabledDetectors.add(detector) : enabledDetectors.remove(detector);
        return persist(changed ? updatedWithDetectors(enabledDetectors) : savedConfig, changed);
    }

    public synchronized Update setAllEnabled(boolean enabled) throws IOException {
        EnumSet<DetectorId> enabledDetectors = enabled
            ? EnumSet.copyOf(DetectorId.implementedInCurrentBuild())
            : EnumSet.noneOf(DetectorId.class);
        boolean changed = !enabledDetectors.equals(savedConfig.enabledDetectors);
        return persist(changed ? updatedWithDetectors(enabledDetectors) : savedConfig, changed);
    }

    public synchronized Update setMarkerEnabled(boolean enabled) throws IOException {
        MarkerSettings markers = new MarkerSettings(enabled, savedConfig.markerSettings.threshold);
        boolean changed = !markers.sameAs(savedConfig.markerSettings);
        return persist(changed ? updatedWithMarkers(markers) : savedConfig, changed);
    }

    public synchronized Update setMarkerThreshold(int threshold) throws IOException {
        MarkerSettings markers = new MarkerSettings(savedConfig.markerSettings.enabled, threshold);
        boolean changed = !markers.sameAs(savedConfig.markerSettings);
        return persist(changed ? updatedWithMarkers(markers) : savedConfig, changed);
    }

    public synchronized Update setNickDetectionEnabled(boolean enabled) throws IOException {
        NickDetectionSettings nickDetection = new NickDetectionSettings(enabled);
        boolean changed = !nickDetection.sameAs(savedConfig.nickDetectionSettings);
        return persist(changed ? updatedWithNickDetection(nickDetection) : savedConfig, changed);
    }

    public synchronized Update setPartyDetectionEnabled(boolean enabled) throws IOException {
        PartyDetectionSettings partyDetection = new PartyDetectionSettings(enabled);
        boolean changed = !partyDetection.sameAs(savedConfig.partyDetectionSettings);
        return persist(changed ? updatedWithPartyDetection(partyDetection) : savedConfig, changed);
    }

    public synchronized Update setNotificationEnabled(NotificationChannel channel, boolean enabled) throws IOException {
        if (channel == null) throw new IllegalArgumentException("Notification channel is required");
        NotificationSettings notifications = notificationSettingsWith(channel, enabled);
        boolean changed = notifications.chatEnabled != savedConfig.notifications.chatEnabled
            || notifications.overlayEnabled != savedConfig.notifications.overlayEnabled
            || notifications.soundEnabled != savedConfig.notifications.soundEnabled;
        return persist(changed ? updatedWithNotifications(notifications) : savedConfig, changed);
    }

    public synchronized Update setDebugEnabled(boolean enabled) throws IOException {
        boolean changed = savedConfig.debugEnabled != enabled;
        return persist(changed ? updatedWithDebugEnabled(enabled) : savedConfig, changed);
    }

    /** Persists one locally presented Stats field while preserving every unrelated setting. */
    public synchronized Update setStatsSettings(StatsSettings settings) throws IOException {
        if (settings == null) throw new IllegalArgumentException("Stats settings are required");
        boolean changed = !sameStatsSettings(settings, savedConfig.statsSettings);
        return persist(changed ? updatedWithStats(settings) : savedConfig, changed);
    }

    /**
     * Accepts an atomically replaced configuration created by the local
     * Companion only when it advances the persisted revision. Invalid files
     * and stale revisions leave the known-good running configuration intact.
     */
    public synchronized ExternalReload reloadFromDiskIfNewer() {
        ConfigLoadResult loaded = store.load(configPath);
        if (loaded.usedDefaults) return ExternalReload.invalidOrMissing();
        if (loaded.config.revision <= savedConfig.revision) return ExternalReload.unchanged();
        savedConfig = loaded.config;
        return ExternalReload.applied(loaded.config);
    }

    private Update persist(LegitilsConfig updated, boolean changed) throws IOException {
        try {
            if (!changed) {
                store.ensureUnchanged(configPath, savedConfig);
                return new Update(savedConfig, false);
            }
            if (savedConfig.revision == Long.MAX_VALUE) throw new IOException("Configuration revision cannot be increased");
            store.writeIfUnchangedAtomically(configPath, savedConfig, updated);
        } catch (LegitilsConfigStore.ConfigChangedException exception) {
            throw new ConfigWriteRefusedException();
        }
        savedConfig = updated;
        return new Update(updated, true);
    }

    private LegitilsConfig updatedWithDetectors(EnumSet<DetectorId> enabledDetectors) throws IOException {
        return new LegitilsConfig(
            savedConfig.schemaVersion, nextRevision(), enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, savedConfig.markerSettings, savedConfig.nickDetectionSettings, savedConfig.partyDetectionSettings,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithMarkers(MarkerSettings markers) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, markers, savedConfig.nickDetectionSettings, savedConfig.partyDetectionSettings,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithNickDetection(NickDetectionSettings nickDetection) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, savedConfig.markerSettings, nickDetection, savedConfig.partyDetectionSettings,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithPartyDetection(PartyDetectionSettings partyDetection) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, savedConfig.markerSettings, savedConfig.nickDetectionSettings, partyDetection,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithNotifications(NotificationSettings notifications) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, savedConfig.markerSettings, savedConfig.nickDetectionSettings, savedConfig.partyDetectionSettings,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithDebugEnabled(boolean debugEnabled) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            debugEnabled, savedConfig.markerSettings, savedConfig.nickDetectionSettings, savedConfig.partyDetectionSettings,
            savedConfig.statsSettings
        );
    }

    private LegitilsConfig updatedWithStats(StatsSettings stats) throws IOException {
        return new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, nextRevision(), savedConfig.enabledDetectors, savedConfig.sensitivity,
            savedConfig.notifications, savedConfig.normalCooldownMillis, savedConfig.airStallCooldownMillis,
            savedConfig.debugEnabled, savedConfig.markerSettings, savedConfig.nickDetectionSettings,
            savedConfig.partyDetectionSettings, stats
        );
    }

    private static boolean sameStatsSettings(StatsSettings left, StatsSettings right) {
        return left.enabled == right.enabled && left.tabEnabled == right.tabEnabled
            && left.starsEnabled == right.starsEnabled && left.fkdrEnabled == right.fkdrEnabled
            && left.winStreakEnabled == right.winStreakEnabled && left.chatEnabled == right.chatEnabled;
    }

    private NotificationSettings notificationSettingsWith(NotificationChannel channel, boolean enabled) {
        boolean chat = savedConfig.notifications.chatEnabled;
        boolean overlay = savedConfig.notifications.overlayEnabled;
        boolean sound = savedConfig.notifications.soundEnabled;
        if (channel == NotificationChannel.CHAT) chat = enabled;
        else if (channel == NotificationChannel.ACTION_BAR) overlay = enabled;
        else if (channel == NotificationChannel.SOUND) sound = enabled;
        return new NotificationSettings(chat, overlay, sound);
    }

    private long nextRevision() throws IOException {
        if (savedConfig.revision == Long.MAX_VALUE) throw new IOException("Configuration revision cannot be increased");
        return savedConfig.revision + 1L;
    }

    private EnumSet<DetectorId> copyEnabledDetectors() {
        return savedConfig.enabledDetectors.isEmpty()
            ? EnumSet.noneOf(DetectorId.class)
            : EnumSet.copyOf(savedConfig.enabledDetectors);
    }

    public static final class ConfigWriteRefusedException extends IOException {
        ConfigWriteRefusedException() {
            super("Configuration changed or is invalid on disk");
        }
    }

    public static final class Update {
        public final LegitilsConfig config;
        public final boolean changed;

        Update(LegitilsConfig config, boolean changed) {
            this.config = config;
            this.changed = changed;
        }
    }

    public static final class ExternalReload {
        public final LegitilsConfig config;
        public final boolean applied;
        public final boolean invalidOrMissing;

        private ExternalReload(LegitilsConfig config, boolean applied, boolean invalidOrMissing) {
            this.config = config;
            this.applied = applied;
            this.invalidOrMissing = invalidOrMissing;
        }

        static ExternalReload applied(LegitilsConfig config) {
            return new ExternalReload(config, true, false);
        }

        static ExternalReload unchanged() {
            return new ExternalReload(null, false, false);
        }

        static ExternalReload invalidOrMissing() {
            return new ExternalReload(null, false, true);
        }
    }
}
