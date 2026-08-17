package com.snkisk.hypixellegitils.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public final class ProviderKeyChangeEventReaderTest {
    @Test
    public void baselinesExistingEventsThenReturnsNewProvidersOnce() throws Exception {
        Path directory = Files.createTempDirectory("legitils-provider-key-events");
        Path events = directory.resolve("provider-key-change-events.json");
        try {
            write(events, "{\"schemaVersion\":1,\"events\":[{\"sequence\":3,\"provider\":\"hypixel\"}]}");
            ProviderKeyChangeEventReader reader = new ProviderKeyChangeEventReader(events);
            reader.baseline();
            assertArrayEquals(new String[0], reader.poll());

            write(events, "{\"schemaVersion\":1,\"events\":[{\"sequence\":3,\"provider\":\"hypixel\"},{\"sequence\":4,\"provider\":\"urchin\"},{\"sequence\":5,\"provider\":\"seraph\"}]}");
            assertArrayEquals(new String[] { "urchin", "seraph" }, reader.poll());
            assertArrayEquals(new String[0], reader.poll());
        } finally {
            Files.deleteIfExists(events);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void ignoresInvalidOrUnsupportedPayloads() throws Exception {
        Path directory = Files.createTempDirectory("legitils-provider-key-events");
        Path events = directory.resolve("provider-key-change-events.json");
        try {
            ProviderKeyChangeEventReader reader = new ProviderKeyChangeEventReader(events);
            reader.baseline();
            write(events, "{\"schemaVersion\":1,\"events\":[{\"sequence\":1,\"provider\":\"unknown\"}]}");
            assertArrayEquals(new String[0], reader.poll());
            write(events, "{\"schemaVersion\":1,\"events\":[{\"sequence\":1,\"provider\":\"hypixel\",\"key\":\"forbidden\"}]}");
            assertArrayEquals(new String[0], reader.poll());
        } finally {
            Files.deleteIfExists(events);
            Files.deleteIfExists(directory);
        }
    }

    private static void write(Path path, String source) throws Exception {
        Files.write(path, source.getBytes(StandardCharsets.UTF_8));
    }
}
