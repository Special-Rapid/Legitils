package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.SimpleJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Strict, key-free local endpoint descriptor written by the macOS Companion. */
public final class StatsBridgeDescriptor {
    public static final int SCHEMA_VERSION = 2;
    private static final long MAXIMUM_LIFETIME_MILLIS = 10L * 60L * 1000L;
    private static final int MAXIMUM_DESCRIPTOR_BYTES = 4096;
    private static final Pattern CAPABILITY = Pattern.compile("[A-Za-z0-9_-]{32,128}");

    public final int port;
    public final String capability;
    public final long expiresAtEpochMillis;

    private StatsBridgeDescriptor(int port, String capability, long expiresAtEpochMillis) {
        this.port = port;
        this.capability = capability;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    public static Optional<StatsBridgeDescriptor> read(Path path, long nowMillis) {
        try {
            if (path == null || !Files.isRegularFile(path) || Files.size(path) > MAXIMUM_DESCRIPTOR_BYTES) {
                return Optional.empty();
            }
            Object parsed = SimpleJson.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            if (!(parsed instanceof Map)) return Optional.empty();
            Map<?, ?> object = (Map<?, ?>) parsed;
            if (object.size() != 4) return Optional.empty();
            Number schema = number(object.get("schemaVersion"));
            Number rawPort = number(object.get("port"));
            Object rawCapability = object.get("capability");
            Number expiry = number(object.get("expiresAtEpochMillis"));
            if (schema == null || schema.intValue() != SCHEMA_VERSION
                || rawPort == null || rawPort.longValue() < 1L || rawPort.longValue() > 65535L
                || !(rawCapability instanceof String) || !CAPABILITY.matcher((String) rawCapability).matches()
                || expiry == null) {
                return Optional.empty();
            }
            long expiresAt = expiry.longValue();
            if (expiresAt <= nowMillis || expiresAt - nowMillis > MAXIMUM_LIFETIME_MILLIS) return Optional.empty();
            return Optional.of(new StatsBridgeDescriptor(rawPort.intValue(), (String) rawCapability, expiresAt));
        } catch (IOException exception) {
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Number number(Object value) {
        return value instanceof Number ? (Number) value : null;
    }
}
