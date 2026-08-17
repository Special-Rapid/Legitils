package com.snkisk.hypixellegitils.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Reads Companion's bounded key-change notifications. The wire format contains only a
 * monotonically increasing sequence and the provider name; API keys never leave Keychain.
 */
public final class ProviderKeyChangeEventReader {
    private static final int SCHEMA_VERSION = 1;
    private static final int MAXIMUM_EVENTS = 16;

    private final Path path;
    private boolean initialized;
    private long lastObservedSequence;

    public ProviderKeyChangeEventReader(Path path) {
        this.path = path;
    }

    /** Baselines existing events so Companion saves while Minecraft is closed never replay. */
    public synchronized void baseline() {
        if (initialized) return;
        lastObservedSequence = maximumSequence(readEvents());
        initialized = true;
    }

    /** Returns each newly appended supported provider once, ordered by sequence. */
    public synchronized String[] poll() {
        List<Event> events = readEvents();
        if (!initialized) {
            lastObservedSequence = maximumSequence(events);
            initialized = true;
            return new String[0];
        }

        List<String> providers = new ArrayList<String>();
        for (Event event : events) {
            if (event.sequence > lastObservedSequence) providers.add(event.provider);
        }
        lastObservedSequence = Math.max(lastObservedSequence, maximumSequence(events));
        return providers.toArray(new String[providers.size()]);
    }

    private List<Event> readEvents() {
        try {
            if (!Files.isRegularFile(path)) return Collections.emptyList();
            Object parsed = SimpleJson.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            if (!(parsed instanceof Map)) return Collections.emptyList();
            Map<?, ?> root = (Map<?, ?>) parsed;
            if (!hasOnlyKeys(root, "schemaVersion", "events")) return Collections.emptyList();
            Object rawSchemaVersion = root.get("schemaVersion");
            if (!(rawSchemaVersion instanceof Long) || ((Long) rawSchemaVersion).longValue() != SCHEMA_VERSION) {
                return Collections.emptyList();
            }
            Object rawEvents = root.get("events");
            if (!(rawEvents instanceof List)) return Collections.emptyList();
            List<?> source = (List<?>) rawEvents;
            if (source.size() > MAXIMUM_EVENTS) return Collections.emptyList();

            List<Event> events = new ArrayList<Event>();
            long previous = 0L;
            for (Object rawEvent : source) {
                if (!(rawEvent instanceof Map)) return Collections.emptyList();
                Map<?, ?> event = (Map<?, ?>) rawEvent;
                if (!hasOnlyKeys(event, "sequence", "provider")) return Collections.emptyList();
                Object rawSequence = event.get("sequence");
                Object rawProvider = event.get("provider");
                if (!(rawSequence instanceof Long) || !(rawProvider instanceof String)) return Collections.emptyList();
                long sequence = ((Long) rawSequence).longValue();
                String provider = (String) rawProvider;
                if (sequence <= previous || !("hypixel".equals(provider) || "urchin".equals(provider) || "seraph".equals(provider))) {
                    return Collections.emptyList();
                }
                previous = sequence;
                events.add(new Event(sequence, provider));
            }
            Collections.sort(events, new Comparator<Event>() {
                @Override
                public int compare(Event left, Event right) {
                    return Long.compare(left.sequence, right.sequence);
                }
            });
            return events;
        } catch (IOException | IllegalArgumentException ignored) {
            return Collections.emptyList();
        }
    }

    private static boolean hasOnlyKeys(Map<?, ?> object, String first, String second) {
        return object.size() == 2 && object.containsKey(first) && object.containsKey(second);
    }

    private static long maximumSequence(List<Event> events) {
        long maximum = 0L;
        for (Event event : events) maximum = Math.max(maximum, event.sequence);
        return maximum;
    }

    private static final class Event {
        private final long sequence;
        private final String provider;

        private Event(long sequence, String provider) {
            this.sequence = sequence;
            this.provider = provider;
        }
    }
}
