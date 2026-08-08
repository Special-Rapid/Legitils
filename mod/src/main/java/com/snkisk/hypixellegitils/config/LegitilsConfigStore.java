package com.snkisk.hypixellegitils.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Strict reader and atomic writer for the local configuration. */
public final class LegitilsConfigStore {
    public ConfigLoadResult load(Path path) {
        if (!Files.isRegularFile(path)) {
            return new ConfigLoadResult(LegitilsConfig.defaults(), true, "configuration file is missing");
        }
        try {
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return new ConfigLoadResult(fromJson(source), false, "configuration loaded");
        } catch (Exception exception) {
            return new ConfigLoadResult(LegitilsConfig.defaults(), true, "invalid configuration: " + exception.getMessage());
        }
    }

    public void writeAtomically(Path path, LegitilsConfig config) throws IOException {
        Path parent = path.getParent();
        if (parent == null) throw new IOException("Configuration path requires a parent directory");
        Files.createDirectories(parent);
        try (FileChannel lockChannel = FileChannel.open(lockPath(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = lockChannel.lock()) {
            writeRawAtomically(path, toJson(config));
        }
    }

    /** Compare-and-replace under the shared config lock used by the Companion. */
    public void writeIfUnchangedAtomically(Path path, LegitilsConfig expected, LegitilsConfig replacement) throws IOException {
        Path parent = path.getParent();
        if (parent == null) throw new IOException("Configuration path requires a parent directory");
        if (expected == null || replacement == null) throw new IllegalArgumentException("Expected and replacement configuration are required");
        Files.createDirectories(parent);
        try (FileChannel lockChannel = FileChannel.open(lockPath(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = lockChannel.lock()) {
            ensureExpectedConfiguration(path, expected);
            writeRawAtomically(path, toJson(replacement));
        }
    }

    /** Verifies a no-op command still observes the current on-disk revision under the shared lock. */
    public void ensureUnchanged(Path path, LegitilsConfig expected) throws IOException {
        Path parent = path.getParent();
        if (parent == null) throw new IOException("Configuration path requires a parent directory");
        if (expected == null) throw new IllegalArgumentException("Expected configuration is required");
        Files.createDirectories(parent);
        try (FileChannel lockChannel = FileChannel.open(lockPath(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = lockChannel.lock()) {
            ensureExpectedConfiguration(path, expected);
        }
    }

    public void writeRuntimeStatusAtomically(Path path, RuntimeStatus status) throws IOException {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schemaVersion", Long.valueOf(LegitilsConfig.SCHEMA_VERSION));
        root.put("modVersion", status.modVersion);
        root.put("configRevision", Long.valueOf(status.configRevision));
        root.put("configUsedDefaults", Boolean.valueOf(status.configUsedDefaults));
        writeRawAtomically(path, SimpleJson.write(root));
    }

    private void writeRawAtomically(Path path, String json) throws IOException {
        Path parent = path.getParent();
        if (parent == null) throw new IOException("Status path requires a parent directory");
        Files.createDirectories(parent);
        Path temporary = parent.resolve(path.getFileName().toString() + ".tmp-" + UUID.randomUUID().toString());
        try {
            Files.write(temporary, json.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path lockPath(Path configPath) {
        String normalized = configPath.toAbsolutePath().normalize().toString();
        return Paths.get("/tmp", "hypixellegitils-config-" + Integer.toHexString(normalized.hashCode()) + ".lock");
    }

    private boolean sameConfig(LegitilsConfig left, LegitilsConfig right) {
        return toJson(left).equals(toJson(right));
    }

    private void ensureExpectedConfiguration(Path path, LegitilsConfig expected) throws ConfigChangedException {
        ConfigLoadResult current = load(path);
        if (current.usedDefaults) {
            if (Files.isRegularFile(path) || expected.revision != 0L) throw new ConfigChangedException();
        } else if (!sameConfig(current.config, expected)) {
            throw new ConfigChangedException();
        }
    }

    private LegitilsConfig fromJson(String source) {
        Object parsed = SimpleJson.parse(source);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("root must be an object");
        Map<?, ?> root = (Map<?, ?>) parsed;
        int schemaVersion = intValue(root.get("schemaVersion"), "schemaVersion");
        if (schemaVersion != LegitilsConfig.LEGACY_SCHEMA_VERSION
            && schemaVersion != LegitilsConfig.MARKER_SCHEMA_VERSION
            && schemaVersion != LegitilsConfig.NICK_DETECTION_SCHEMA_VERSION
            && schemaVersion != LegitilsConfig.PARTY_SCHEMA_VERSION
            && schemaVersion != LegitilsConfig.STATS_SCHEMA_VERSION
            && schemaVersion != LegitilsConfig.SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schemaVersion");
        }
        if (schemaVersion == LegitilsConfig.LEGACY_SCHEMA_VERSION) {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug");
        } else if (schemaVersion == LegitilsConfig.MARKER_SCHEMA_VERSION) {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug", "markers");
        } else if (schemaVersion == LegitilsConfig.NICK_DETECTION_SCHEMA_VERSION) {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug", "markers", "nickDetection");
        } else if (schemaVersion == LegitilsConfig.PARTY_SCHEMA_VERSION) {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug", "markers", "nickDetection", "partyDetection");
        } else if (schemaVersion == LegitilsConfig.STATS_SCHEMA_VERSION) {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug", "markers", "nickDetection", "partyDetection", "stats");
        } else {
            requireOnlyKeys(root, "schemaVersion", "revision", "enabledDetectors", "sensitivity", "notifications", "cooldowns", "debug", "markers", "nickDetection", "partyDetection", "stats");
        }
        long revision = longValue(root.get("revision"), "revision");
        if (revision < 0L) throw new IllegalArgumentException("revision must be non-negative");

        Set<DetectorId> enabled = EnumSet.noneOf(DetectorId.class);
        Object rawDetectors = root.get("enabledDetectors");
        if (!(rawDetectors instanceof List)) throw new IllegalArgumentException("enabledDetectors must be an array");
        for (Object rawDetector : (List<?>) rawDetectors) {
            if (!(rawDetector instanceof String)) throw new IllegalArgumentException("enabledDetectors must contain strings");
            DetectorId detector;
            try {
                detector = DetectorId.valueOf((String) rawDetector);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("unknown detector: " + rawDetector);
            }
            if (detector.isImplementedInCurrentBuild()) enabled.add(detector);
        }

        Object rawSensitivity = root.get("sensitivity");
        if (!(rawSensitivity instanceof String)) throw new IllegalArgumentException("sensitivity must be a string");
        SensitivityPreset sensitivity;
        try {
            sensitivity = SensitivityPreset.valueOf(((String) rawSensitivity).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown sensitivity");
        }

        if (!(root.get("notifications") instanceof Map)) throw new IllegalArgumentException("notifications must be an object");
        Map<?, ?> notifications = (Map<?, ?>) root.get("notifications");
        requireOnlyKeys(notifications, "chat", "overlay", "sound");
        NotificationSettings notificationSettings = new NotificationSettings(
            booleanValue(notifications.get("chat"), "notifications.chat"),
            booleanValue(notifications.get("overlay"), "notifications.overlay"),
            booleanValue(notifications.get("sound"), "notifications.sound")
        );

        if (!(root.get("cooldowns") instanceof Map)) throw new IllegalArgumentException("cooldowns must be an object");
        Map<?, ?> cooldowns = (Map<?, ?>) root.get("cooldowns");
        requireOnlyKeys(cooldowns, "normalMillis", "airStallMillis");
        long normalCooldown = longValue(cooldowns.get("normalMillis"), "cooldowns.normalMillis");
        long airStallCooldown = longValue(cooldowns.get("airStallMillis"), "cooldowns.airStallMillis");
        if (normalCooldown < 1000L || normalCooldown > 300000L) throw new IllegalArgumentException("normal cooldown outside allowed range");
        if (airStallCooldown < 30000L || airStallCooldown > 600000L) throw new IllegalArgumentException("air-stall cooldown outside allowed range");

        MarkerSettings markers = MarkerSettings.defaults();
        if (schemaVersion >= LegitilsConfig.MARKER_SCHEMA_VERSION) {
            if (!(root.get("markers") instanceof Map)) throw new IllegalArgumentException("markers must be an object");
            Map<?, ?> rawMarkers = (Map<?, ?>) root.get("markers");
            requireOnlyKeys(rawMarkers, "enabled", "threshold");
            markers = new MarkerSettings(
                booleanValue(rawMarkers.get("enabled"), "markers.enabled"),
                intValue(rawMarkers.get("threshold"), "markers.threshold")
            );
        }

        NickDetectionSettings nickDetection = NickDetectionSettings.defaults();
        if (schemaVersion >= LegitilsConfig.NICK_DETECTION_SCHEMA_VERSION) {
            if (!(root.get("nickDetection") instanceof Map)) throw new IllegalArgumentException("nickDetection must be an object");
            Map<?, ?> rawNickDetection = (Map<?, ?>) root.get("nickDetection");
            requireOnlyKeys(rawNickDetection, "enabled");
            nickDetection = new NickDetectionSettings(booleanValue(rawNickDetection.get("enabled"), "nickDetection.enabled"));
        }

        PartyDetectionSettings partyDetection = PartyDetectionSettings.defaults();
        if (schemaVersion >= LegitilsConfig.PARTY_SCHEMA_VERSION) {
            if (!(root.get("partyDetection") instanceof Map)) throw new IllegalArgumentException("partyDetection must be an object");
            Map<?, ?> rawPartyDetection = (Map<?, ?>) root.get("partyDetection");
            requireOnlyKeys(rawPartyDetection, "enabled");
            partyDetection = new PartyDetectionSettings(booleanValue(rawPartyDetection.get("enabled"), "partyDetection.enabled"));
        }
        StatsSettings stats = StatsSettings.defaults();
        if (schemaVersion >= LegitilsConfig.STATS_SCHEMA_VERSION) {
            if (!(root.get("stats") instanceof Map)) throw new IllegalArgumentException("stats must be an object");
            Map<?, ?> rawStats = (Map<?, ?>) root.get("stats");
            if (schemaVersion == LegitilsConfig.STATS_SCHEMA_VERSION) {
                requireOnlyKeys(rawStats, "enabled", "tab", "stars", "fkdr", "winStreak", "chat");
            } else {
                requireOnlyKeys(rawStats, "enabled", "tab", "stars", "fkdr", "winStreak", "chat", "nametag", "nametagFkdrThreshold");
            }
            stats = new StatsSettings(booleanValue(rawStats.get("enabled"), "stats.enabled"),
                booleanValue(rawStats.get("tab"), "stats.tab"), booleanValue(rawStats.get("stars"), "stats.stars"),
                booleanValue(rawStats.get("fkdr"), "stats.fkdr"), booleanValue(rawStats.get("winStreak"), "stats.winStreak"),
                booleanValue(rawStats.get("chat"), "stats.chat"),
                schemaVersion >= LegitilsConfig.STATS_NAMETAG_SCHEMA_VERSION && booleanValue(rawStats.get("nametag"), "stats.nametag"),
                schemaVersion >= LegitilsConfig.STATS_NAMETAG_SCHEMA_VERSION ? doubleValue(rawStats.get("nametagFkdrThreshold"), "stats.nametagFkdrThreshold") : 1D);
        }

        return new LegitilsConfig(
            schemaVersion,
            revision,
            enabled,
            sensitivity,
            notificationSettings,
            normalCooldown,
            airStallCooldown,
            booleanValue(root.get("debug"), "debug"),
            markers,
            nickDetection,
            partyDetection, stats
        );
    }

    private String toJson(LegitilsConfig config) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schemaVersion", Long.valueOf(config.schemaVersion));
        root.put("revision", Long.valueOf(config.revision));
        List<Object> detectors = new ArrayList<Object>();
        for (DetectorId detector : DetectorId.values()) {
            if (detector.isImplementedInCurrentBuild() && config.enabledDetectors.contains(detector)) detectors.add(detector.name());
        }
        root.put("enabledDetectors", detectors);
        root.put("sensitivity", config.sensitivity.name().toLowerCase(java.util.Locale.ROOT));
        Map<String, Object> notifications = new LinkedHashMap<String, Object>();
        notifications.put("chat", Boolean.valueOf(config.notifications.chatEnabled));
        notifications.put("overlay", Boolean.valueOf(config.notifications.overlayEnabled));
        notifications.put("sound", Boolean.valueOf(config.notifications.soundEnabled));
        root.put("notifications", notifications);
        Map<String, Object> cooldowns = new LinkedHashMap<String, Object>();
        cooldowns.put("normalMillis", Long.valueOf(config.normalCooldownMillis));
        cooldowns.put("airStallMillis", Long.valueOf(config.airStallCooldownMillis));
        root.put("cooldowns", cooldowns);
        root.put("debug", Boolean.valueOf(config.debugEnabled));
        if (config.schemaVersion >= LegitilsConfig.MARKER_SCHEMA_VERSION) {
            Map<String, Object> markers = new LinkedHashMap<String, Object>();
            markers.put("enabled", Boolean.valueOf(config.markerSettings.enabled));
            markers.put("threshold", Long.valueOf(config.markerSettings.threshold));
            root.put("markers", markers);
        }
        if (config.schemaVersion >= LegitilsConfig.NICK_DETECTION_SCHEMA_VERSION) {
            Map<String, Object> nickDetection = new LinkedHashMap<String, Object>();
            nickDetection.put("enabled", Boolean.valueOf(config.nickDetectionSettings.enabled));
            root.put("nickDetection", nickDetection);
        }
        if (config.schemaVersion >= LegitilsConfig.PARTY_SCHEMA_VERSION) {
            Map<String, Object> partyDetection = new LinkedHashMap<String, Object>();
            partyDetection.put("enabled", Boolean.valueOf(config.partyDetectionSettings.enabled));
            root.put("partyDetection", partyDetection);
        }
        if (config.schemaVersion >= LegitilsConfig.STATS_SCHEMA_VERSION) {
            Map<String, Object> stats = new LinkedHashMap<String, Object>();
            stats.put("enabled", Boolean.valueOf(config.statsSettings.enabled));
            stats.put("tab", Boolean.valueOf(config.statsSettings.tabEnabled));
            stats.put("stars", Boolean.valueOf(config.statsSettings.starsEnabled));
            stats.put("fkdr", Boolean.valueOf(config.statsSettings.fkdrEnabled));
            stats.put("winStreak", Boolean.valueOf(config.statsSettings.winStreakEnabled));
            stats.put("chat", Boolean.valueOf(config.statsSettings.chatEnabled));
            if (config.schemaVersion >= LegitilsConfig.STATS_NAMETAG_SCHEMA_VERSION) {
                stats.put("nametag", Boolean.valueOf(config.statsSettings.nametagEnabled));
                stats.put("nametagFkdrThreshold", Double.valueOf(config.statsSettings.nametagFkdrThreshold));
            }
            root.put("stats", stats);
        }
        return SimpleJson.write(root);
    }

    private static void requireOnlyKeys(Map<?, ?> object, String... keys) {
        Set<String> allowed = new java.util.HashSet<String>();
        for (String key : keys) allowed.add(key);
        if (object.size() != allowed.size()) throw new IllegalArgumentException("missing or unknown JSON keys");
        for (Object key : object.keySet()) if (!(key instanceof String) || !allowed.contains(key)) throw new IllegalArgumentException("unknown JSON key: " + key);
        for (String key : allowed) if (!object.containsKey(key)) throw new IllegalArgumentException("missing JSON key: " + key);
    }

    private static int intValue(Object value, String name) {
        long number = longValue(value, name);
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new IllegalArgumentException(name + " outside integer range");
        return (int) number;
    }

    private static long longValue(Object value, String name) {
        if (!(value instanceof Long)) throw new IllegalArgumentException(name + " must be an integer");
        return ((Long) value).longValue();
    }

    private static double doubleValue(Object value, String name) {
        if (!(value instanceof Number)) throw new IllegalArgumentException(name + " must be a number");
        double number = ((Number) value).doubleValue();
        if (Double.isNaN(number) || Double.isInfinite(number)) throw new IllegalArgumentException(name + " must be finite");
        return number;
    }

    private static boolean booleanValue(Object value, String name) {
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(name + " must be a boolean");
        return ((Boolean) value).booleanValue();
    }

    public static final class ConfigChangedException extends IOException {
        ConfigChangedException() {
            super("Configuration changed or is invalid on disk");
        }
    }
}
