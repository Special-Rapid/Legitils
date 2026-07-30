package com.snkisk.hypixellegitils.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Strict, bounded, local-only UUID marker history stored separately from config.json. */
public final class MarkerHistoryStore {
    public static final int SCHEMA_VERSION = 2;
    public static final int MAXIMUM_ENTRIES = 256;

    public Map<UUID, MarkerHistoryEntry> load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return new LinkedHashMap<UUID, MarkerHistoryEntry>();
        try {
            Object parsed = SimpleJson.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            if (!(parsed instanceof Map)) throw new IllegalArgumentException("root must be object");
            Map<?, ?> root = (Map<?, ?>) parsed;
            requireKeys(root, "schemaVersion", "entries");
            long schemaVersion = number(root.get("schemaVersion"), "schemaVersion", SCHEMA_VERSION);
            if (schemaVersion != 1L && schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("unsupported schema");
            if (!(root.get("entries") instanceof List)) throw new IllegalArgumentException("entries must be array");
            List<?> entries = (List<?>) root.get("entries");
            if (entries.size() > MAXIMUM_ENTRIES) throw new IllegalArgumentException("too many entries");
            Map<UUID, MarkerHistoryEntry> result = new LinkedHashMap<UUID, MarkerHistoryEntry>();
            for (Object raw : entries) {
                if (!(raw instanceof Map)) throw new IllegalArgumentException("entry must be object");
                Map<?, ?> entry = (Map<?, ?>) raw;
                if (schemaVersion == 1L) {
                    requireKeys(entry, "playerId", "acceptedCount", "blacklisted", "updatedAtEpochMillis");
                } else {
                    requireKeys(entry, "playerId", "acceptedCount", "blacklisted", "updatedAtEpochMillis",
                        "mojangResolvedName", "mojangResolvedAtEpochMillis", "observedServerName", "observedServerNameAtEpochMillis");
                }
                if (!(entry.get("playerId") instanceof String) || !(entry.get("blacklisted") instanceof Boolean)) {
                    throw new IllegalArgumentException("invalid entry types");
                }
                UUID playerId = UUID.fromString((String) entry.get("playerId"));
                if (result.containsKey(playerId)) throw new IllegalArgumentException("duplicate playerId");
                int acceptedCount = (int) number(entry.get("acceptedCount"), "acceptedCount", 1000000L);
                boolean blacklisted = ((Boolean) entry.get("blacklisted")).booleanValue();
                long updatedAt = number(entry.get("updatedAtEpochMillis"), "updatedAtEpochMillis", Long.MAX_VALUE);
                if (schemaVersion == 1L) {
                    result.put(playerId, new MarkerHistoryEntry(acceptedCount, blacklisted, updatedAt));
                } else {
                    result.put(playerId, new MarkerHistoryEntry(
                        acceptedCount,
                        blacklisted,
                        updatedAt,
                        optionalName(entry.get("mojangResolvedName"), "mojangResolvedName"),
                        number(entry.get("mojangResolvedAtEpochMillis"), "mojangResolvedAtEpochMillis", Long.MAX_VALUE),
                        optionalName(entry.get("observedServerName"), "observedServerName"),
                        number(entry.get("observedServerNameAtEpochMillis"), "observedServerNameAtEpochMillis", Long.MAX_VALUE)
                    ));
                }
            }
            return result;
        } catch (Exception ignored) {
            return new LinkedHashMap<UUID, MarkerHistoryEntry>();
        }
    }

    public void writeAtomically(Path path, Map<UUID, MarkerHistoryEntry> history) throws IOException {
        if (path == null || history == null || history.size() > MAXIMUM_ENTRIES) throw new IOException("Invalid marker history");
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schemaVersion", Long.valueOf(SCHEMA_VERSION));
        List<Object> entries = new ArrayList<Object>();
        for (Map.Entry<UUID, MarkerHistoryEntry> entry : history.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) throw new IOException("Invalid marker history entry");
            MarkerHistoryEntry value = entry.getValue();
            Map<String, Object> encoded = new LinkedHashMap<String, Object>();
            encoded.put("playerId", entry.getKey().toString());
            encoded.put("acceptedCount", Long.valueOf(value.acceptedCount));
            encoded.put("blacklisted", Boolean.valueOf(value.blacklisted));
            encoded.put("updatedAtEpochMillis", Long.valueOf(value.updatedAtEpochMillis));
            encoded.put("mojangResolvedName", value.mojangResolvedName);
            encoded.put("mojangResolvedAtEpochMillis", Long.valueOf(value.mojangResolvedAtEpochMillis));
            encoded.put("observedServerName", value.observedServerName);
            encoded.put("observedServerNameAtEpochMillis", Long.valueOf(value.observedServerNameAtEpochMillis));
            entries.add(encoded);
        }
        root.put("entries", entries);
        Path parent = path.getParent();
        if (parent == null) throw new IOException("History path requires parent");
        Files.createDirectories(parent);
        Path temporary = parent.resolve(path.getFileName().toString() + ".tmp-" + UUID.randomUUID().toString());
        try {
            Files.write(temporary, SimpleJson.write(root).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void requireKeys(Map<?, ?> value, String... keys) {
        if (value.size() != keys.length) throw new IllegalArgumentException("unknown or missing key");
        for (String key : keys) if (!value.containsKey(key)) throw new IllegalArgumentException("missing key");
    }

    private static long number(Object value, String name, long maximum) {
        if (!(value instanceof Long)) throw new IllegalArgumentException(name + " must be integer");
        long number = ((Long) value).longValue();
        if (number < 0L || number > maximum) throw new IllegalArgumentException(name + " out of range");
        return number;
    }

    private static String optionalName(Object value, String name) {
        if (value == null) return null;
        if (!(value instanceof String) || !((String) value).matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException(name + " must be a Minecraft name or null");
        }
        return (String) value;
    }
}
